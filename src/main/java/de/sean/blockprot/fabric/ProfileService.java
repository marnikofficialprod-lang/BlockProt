/*
 * Copyright (C) 2021 - 2025 spnda
 * This file is part of BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.fabric;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.services.ProfileResult;
import de.sean.blockprot.fabric.util.Scheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Resolves player names from UUIDs. First looks at the online players and the server's
 * usercache.json, then at our own cache and only then asks the Mojang session server.
 * Replaces the SquirrelID based CachedProfileService of the plugin.
 */
public final class ProfileService {
    private static final String CACHE_FILE_NAME = "blockprot_usercache.json";
    private static final Map<UUID, String> cache = new ConcurrentHashMap<>();
    private static @Nullable Path cacheFile;
    private static volatile boolean cacheDirty = false;

    private ProfileService() {
    }

    public static void load(@NotNull final MinecraftServer server) {
        cache.clear();
        cacheFile = server.getServerDirectory().resolve(CACHE_FILE_NAME);
        if (!Files.exists(cacheFile)) return;
        try (Reader reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8)) {
            final JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            for (var entry : object.entrySet()) {
                try {
                    cache.put(UUID.fromString(entry.getKey()), entry.getValue().getAsString());
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException | RuntimeException e) {
            BlockProt.LOGGER.warn("Failed to read " + CACHE_FILE_NAME + ": " + e.getMessage());
        }
    }

    public static synchronized void save() {
        if (cacheFile == null || !cacheDirty) return;
        cacheDirty = false;
        final JsonObject object = new JsonObject();
        cache.forEach((uuid, name) -> object.addProperty(uuid.toString(), name));
        try (Writer writer = Files.newBufferedWriter(cacheFile, StandardCharsets.UTF_8)) {
            new Gson().toJson(object, writer);
        } catch (IOException e) {
            BlockProt.LOGGER.warn("Failed to save " + CACHE_FILE_NAME + ": " + e.getMessage());
        }
    }

    public static void put(@NotNull final UUID uuid, @NotNull final String name) {
        if (!name.equals(cache.put(uuid, name))) cacheDirty = true;
    }

    /**
     * Gets the name of a player without any network requests. Must be called on the server thread.
     */
    @Nullable
    public static String getIfPresent(@NotNull final UUID uuid) {
        final MinecraftServer server = BlockProt.getServer();
        if (server != null) {
            final ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) return player.getGameProfile().name();
            final Optional<NameAndId> cached = server.services().nameToIdCache().get(uuid);
            if (cached.isPresent()) return cached.get().name();
        }
        return cache.get(uuid);
    }

    /**
     * Asks the Mojang session server for the name of the player. This blocks, so never call this
     * on the server thread.
     */
    @Nullable
    private static String fetch(@NotNull final UUID uuid) {
        final MinecraftServer server = BlockProt.getServer();
        if (server == null) return null;
        try {
            final ProfileResult result = server.services().sessionService().fetchProfile(uuid, false);
            if (result == null || result.profile() == null) return null;
            final String name = result.profile().name();
            if (name != null) put(uuid, name);
            return name;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Resolves the names of all given players. The returned future is completed on the server
     * thread. Players whose name could not be found are missing from the map.
     */
    @NotNull
    public static CompletableFuture<Map<UUID, String>> findAllByUuid(@NotNull final Collection<UUID> uuids) {
        final Map<UUID, String> found = new LinkedHashMap<>();
        final Set<UUID> missing = new HashSet<>();
        for (UUID uuid : uuids) {
            final String name = getIfPresent(uuid);
            if (name != null) found.put(uuid, name);
            else missing.add(uuid);
        }
        final CompletableFuture<Map<UUID, String>> future = new CompletableFuture<>();
        if (missing.isEmpty()) {
            future.complete(found);
            return future;
        }
        BlockProt.EXECUTOR.execute(() -> {
            final Map<UUID, String> fetched = new LinkedHashMap<>();
            for (UUID uuid : missing) {
                final String name = fetch(uuid);
                if (name != null) fetched.put(uuid, name);
            }
            save();
            Scheduler.runSync(() -> {
                found.putAll(fetched);
                future.complete(found);
            });
        });
        return future;
    }

    /**
     * All players that have ever joined this server, the replacement for Bukkit#getOfflinePlayers.
     * Must be called on the server thread.
     */
    @NotNull
    public static Set<UUID> getKnownPlayers(@NotNull final MinecraftServer server) {
        final Set<UUID> players = new HashSet<>(cache.keySet());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) players.add(player.getUUID());
        for (LevelResource resource : new LevelResource[]{LevelResource.PLAYER_DATA_DIR, LevelResource.PLAYER_OLD_DATA_DIR}) {
            final Path folder = server.getWorldPath(resource);
            if (!Files.isDirectory(folder)) continue;
            try (Stream<Path> files = Files.list(folder)) {
                files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".dat"))
                    .forEach(name -> {
                        try {
                            players.add(UUID.fromString(name.substring(0, name.length() - 4)));
                        } catch (IllegalArgumentException ignored) {
                        }
                    });
            } catch (IOException e) {
                BlockProt.LOGGER.warn("Failed to list player data: " + e.getMessage());
            }
        }
        return players;
    }
}
