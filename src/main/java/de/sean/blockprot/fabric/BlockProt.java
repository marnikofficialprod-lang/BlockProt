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

import de.sean.blockprot.fabric.commands.BlockProtCommand;
import de.sean.blockprot.fabric.config.DefaultConfig;
import de.sean.blockprot.fabric.config.YamlConfig;
import de.sean.blockprot.fabric.inventories.GuiMenu;
import de.sean.blockprot.fabric.inventories.InventoryState;
import de.sean.blockprot.fabric.listeners.BlockEventListener;
import de.sean.blockprot.fabric.listeners.InteractEventListener;
import de.sean.blockprot.fabric.listeners.InventoryEventListener;
import de.sean.blockprot.fabric.listeners.JoinEventListener;
import de.sean.blockprot.fabric.nbt.BlockProtAttachments;
import de.sean.blockprot.fabric.nbt.StatHandler;
import de.sean.blockprot.fabric.util.Scheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The main mod instance of BlockProt.
 */
public final class BlockProt implements ModInitializer {
    public static final String MOD_ID = "blockprot";

    public static final Logger LOGGER = LoggerFactory.getLogger("BlockProt");

    /**
     * Runs the asynchronous work (player name lookups, update checks, saving files).
     */
    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new java.util.concurrent.ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            final Thread thread = new Thread(runnable, "BlockProt Worker #" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    });

    /**
     * The name of the default language file, which is the english
     * translation file.
     */
    public static final String defaultLanguageFile = "translations_en.yml";

    @Nullable
    private static DefaultConfig defaultConfig = null;

    @Nullable
    private static MinecraftServer server = null;

    /**
     * Gets the default config.
     *
     * @since 0.4.0
     */
    @NotNull
    public static DefaultConfig getDefaultConfig() {
        if (defaultConfig == null) throw new IllegalStateException("The BlockProt config has not been loaded yet.");
        return defaultConfig;
    }

    /**
     * The running server, or null if no server is running.
     */
    @Nullable
    public static MinecraftServer getServer() {
        return server;
    }

    @NotNull
    public static Path getConfigFolder() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    }

    @NotNull
    public static String getVersion() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    }

    @Override
    public void onInitialize() {
        BlockProtAttachments.init();
        reloadConfigAndTranslations();

        /* Register Listeners */
        UseBlockCallback.EVENT.register(InteractEventListener::onUseBlock);
        PlayerBlockBreakEvents.BEFORE.register(BlockEventListener::onBlockBreak);
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> BlockProtCommand.register(dispatcher));

        // Blocks of other mods are only registered after all mods have been initialized.
        ServerLifecycleEvents.SERVER_STARTING.register(startingServer -> reloadConfigAndTranslations());
        ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
            server = startedServer;
            StatHandler.enable(startedServer);
            ProfileService.load(startedServer);

            if (Boolean.getBoolean("blockprot.mixinAudit")) {
                MixinEnvironment.getCurrentEnvironment().audit();
                LOGGER.info("Mixin audit finished.");
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> {
            for (ServerPlayer player : stoppingServer.getPlayerList().getPlayers()) {
                if (player.containerMenu instanceof GuiMenu) player.closeContainer();
            }
            LOGGER.info("Saving statistic file...");
            StatHandler.disable();
            ProfileService.save();
            Scheduler.clear();
            InventoryState.clear();
            server = null;
        });
        ServerTickEvents.END_SERVER_TICK.register(tickingServer -> {
            Scheduler.tick();
            StatHandler.tick();
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, joinedServer) -> JoinEventListener.onJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, disconnectedServer) -> {
            InventoryEventListener.onDisconnect(handler.player);
            InventoryState.remove(handler.player.getUUID());
        });
    }

    /**
     * Reloads the config and the translation files (possibly changed through config).
     *
     * @since 1.0.0
     */
    public static void reloadConfigAndTranslations() {
        final Path configFolder = getConfigFolder();
        final Path langFolder = configFolder.resolve("lang");
        try {
            Files.createDirectories(langFolder);
            saveResource("config.yml", configFolder.resolve("config.yml"), false);
        } catch (IOException e) {
            LOGGER.error("Failed to create the BlockProt config folder", e);
        }

        YamlConfig userConfig;
        try {
            userConfig = YamlConfig.load(configFolder.resolve("config.yml"));
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to read config/blockprot/config.yml, using the default config", e);
            userConfig = YamlConfig.empty();
        }
        defaultConfig = new DefaultConfig(userConfig, loadResource("config.yml"));

        Translator.resetTranslations();
        final String fallback = defaultConfig.getTranslationFallbackString();
        Translator.DEFAULT_FALLBACK = fallback == null ? "" : fallback;

        // Ensure that all translation files have been saved properly.
        for (String resource : Translator.DEFAULT_TRANSLATION_FILES) {
            try {
                saveResource("lang/" + resource, langFolder.resolve(resource), defaultConfig.shouldReplaceTranslations());
            } catch (IOException e) {
                LOGGER.warn("Failed to save translation file " + resource + ": " + e.getMessage());
            }
        }

        // We always use the resource within the JAR as the default language file, in case the
        // file in the config folder has not been updated yet.
        final YamlConfig defaultLanguageConfig = loadResource("lang/" + defaultLanguageFile);

        // Get the wanted language file and load its config.
        final String fileName = defaultConfig.getLanguageFile() == null
            ? defaultLanguageFile
            : defaultConfig.getLanguageFile();
        YamlConfig wantedConfig = YamlConfig.empty();
        final Path wantedFile = langFolder.resolve(fileName);
        if (Files.exists(wantedFile)) {
            try {
                wantedConfig = YamlConfig.load(wantedFile);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Failed to read the language file " + fileName + ": " + e.getMessage());
            }
        } else {
            LOGGER.warn("The configured language file does not exist: " + fileName);
        }
        Translator.loadFromConfigs(defaultLanguageConfig, wantedConfig);
    }

    @NotNull
    private static YamlConfig loadResource(@NotNull final String name) {
        try (InputStream stream = BlockProt.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                // The JAR has been modified or is corrupt and the files are missing.
                throw new RuntimeException("Failed to get " + name + ". Possibly corrupt mod?");
            }
            return YamlConfig.load(stream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + name, e);
        }
    }

    private static void saveResource(@NotNull final String name, @NotNull final Path target, final boolean replace) throws IOException {
        if (Files.exists(target) && !replace) return;
        try (InputStream stream = BlockProt.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) throw new IOException("The resource " + name + " does not exist.");
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
