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

package de.sean.blockprot.fabric.listeners;

import de.sean.blockprot.fabric.inventories.GuiMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Friends that only have the read permission may open protected containers, but cannot move
 * any items, nor take books from lecterns. The menu a player opens right after interacting with
 * such a block gets marked as read-only.
 */
public final class InventoryEventListener {
    private record PendingAccess(int tick, boolean readOnly) {
    }

    private static final Map<UUID, PendingAccess> pending = new HashMap<>();
    private static final Set<AbstractContainerMenu> readOnlyMenus = Collections.newSetFromMap(new WeakHashMap<>());

    private InventoryEventListener() {
    }

    static void setReadOnlyAccess(@NotNull final ServerPlayer player, final boolean readOnly) {
        if (readOnly) {
            pending.put(player.getUUID(), new PendingAccess(player.level().getServer().getTickCount(), true));
        } else {
            pending.remove(player.getUUID());
        }
    }

    /**
     * Called after {@link ServerPlayer#openMenu} opened a new menu.
     */
    public static void onMenuOpened(@NotNull final ServerPlayer player, @NotNull final AbstractContainerMenu menu) {
        final PendingAccess access = pending.remove(player.getUUID());
        if (access == null || !access.readOnly()) return;
        if (access.tick() != player.level().getServer().getTickCount()) return;
        if (menu instanceof GuiMenu) return;
        readOnlyMenus.add(menu);
    }

    public static boolean isReadOnly(@NotNull final AbstractContainerMenu menu) {
        return !readOnlyMenus.isEmpty() && readOnlyMenus.contains(menu);
    }

    public static void onDisconnect(@NotNull final ServerPlayer player) {
        pending.remove(player.getUUID());
    }
}
