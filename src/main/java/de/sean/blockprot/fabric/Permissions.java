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

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the permission keys for BlockProt. Checked through the fabric-permissions-api,
 * so LuckPerms (or any other permission mod) can grant them. Without a permission mod the
 * defaults of the original plugin.yml are used.
 * @since 1.1.7
 */
public enum Permissions {
    LOCK("blockprot.lock"),
    INFO("blockprot.info"),
    ADMIN("blockprot.admin"),
    BYPASS("blockprot.bypass");

    public static final String LOCK_MAX = "blockprot.lockmax";
    public static final String LOCK_LIMIT_PREFIX = "blockprot.locklimit.";
    public static final String DEBUG = "blockprot.debug";

    private final String text;

    Permissions(final String text) {
        this.text = text;
    }

    @Override
    public final String toString() {
        return text;
    }

    public final String key() {
        return text;
    }

    /**
     * Checks this permission with the default value defined in the original plugin.yml.
     */
    public boolean check(@NotNull final ServerPlayer player) {
        return has(player, text);
    }

    /**
     * Checks a permission node. The default values mirror the plugin.yml of the Spigot plugin:
     * blockprot.lock is granted to everyone, blockprot.info to operators and everything else
     * to nobody.
     */
    public static boolean has(@NotNull final ServerPlayer player, @NotNull final String node) {
        final boolean def = switch (node) {
            case "blockprot.lock" -> true;
            case "blockprot.info" -> isOp(player);
            default -> false;
        };
        return me.lucko.fabric.api.permissions.v0.Permissions.check(player, node, def);
    }

    public static boolean has(@NotNull final CommandSourceStack source, @NotNull final String node) {
        final ServerPlayer player = source.getPlayer();
        if (player == null) return true; // The console has every permission.
        return has(player, node);
    }

    public static boolean isOp(@NotNull final ServerPlayer player) {
        return player.level().getServer().getPlayerList().isOp(player.nameAndId());
    }

    public static boolean isOp(@NotNull final CommandSourceStack source) {
        final ServerPlayer player = source.getPlayer();
        return player == null || isOp(player);
    }

    /**
     * Finds the highest "blockprot.locklimit.&lt;number&gt;" value of a player. The permission
     * api has no way of listing all nodes of a player, so we first look for a "blockprot.locklimit"
     * option (LuckPerms meta), then ask LuckPerms directly for the permission nodes, if it is
     * installed.
     *
     * @return The highest found limit, or null if the player has none.
     */
    public static Integer getLockLimit(@NotNull final ServerPlayer player) {
        Integer highest = null;
        try {
            final var option = me.lucko.fabric.api.permissions.v0.Options.get(player, "blockprot.locklimit");
            if (option.isPresent()) highest = Integer.parseInt(option.get().trim());
        } catch (NumberFormatException ignored) {
        }

        for (Map.Entry<String, Boolean> entry : LuckPermsBridge.getPermissionMap(player.getUUID()).entrySet()) {
            final String permission = entry.getKey().toLowerCase();
            if (!entry.getValue() || !permission.startsWith(LOCK_LIMIT_PREFIX)) continue;
            try {
                final int value = Integer.parseInt(permission.substring(LOCK_LIMIT_PREFIX.length()));
                if (value > (highest == null ? 0 : highest)) highest = value;
            } catch (NumberFormatException ignored) {
            }
        }
        return highest;
    }

    /**
     * Soft dependency on the LuckPerms API through reflection, used to list permission nodes.
     */
    private static final class LuckPermsBridge {
        private static boolean failed = false;

        @SuppressWarnings("unchecked")
        static Map<String, Boolean> getPermissionMap(@NotNull final UUID uuid) {
            if (failed) return Map.of();
            try {
                final Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
                final Object api = provider.getMethod("get").invoke(null);
                final Class<?> apiClass = Class.forName("net.luckperms.api.LuckPerms");
                final Object userManager = apiClass.getMethod("getUserManager").invoke(api);
                final Class<?> userManagerClass = Class.forName("net.luckperms.api.model.user.UserManager");
                final Object user = userManagerClass.getMethod("getUser", UUID.class).invoke(userManager, uuid);
                if (user == null) return Map.of();
                final Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
                final Object cachedData = userClass.getMethod("getCachedData").invoke(user);
                final Class<?> cachedDataClass = Class.forName("net.luckperms.api.cacheddata.CachedDataManager");
                final Object permissionData = cachedDataClass.getMethod("getPermissionData").invoke(cachedData);
                final Class<?> permissionDataClass = Class.forName("net.luckperms.api.cacheddata.CachedPermissionData");
                final Method getPermissionMap = permissionDataClass.getMethod("getPermissionMap");
                return (Map<String, Boolean>) getPermissionMap.invoke(permissionData);
            } catch (ClassNotFoundException e) {
                failed = true;
                return Map.of();
            } catch (ReflectiveOperationException | RuntimeException e) {
                return Map.of();
            }
        }
    }
}
