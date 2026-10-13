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

package de.sean.blockprot.fabric.events;

import de.sean.blockprot.fabric.util.BlockRef;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Called when a player is trying to access a blocks lock menu.
 * Can be cancelled to prevent the inventory from opening up.
 *
 * @since 0.4.0
 */
public final class BlockAccessMenuEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> event -> {
        for (Listener listener : listeners) listener.onBlockAccessMenu(event);
    });

    @NotNull
    private final BlockRef block;
    @NotNull
    private final ServerPlayer player;
    @NotNull
    private final Set<MenuPermission> permissions = new HashSet<>();
    private boolean isCancelled = false;

    public BlockAccessMenuEvent(@NotNull final BlockRef block,
                                @NotNull final ServerPlayer player) {
        this.block = block;
        this.player = player;
    }

    @NotNull
    public BlockRef getBlock() {
        return block;
    }

    @NotNull
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * Get the permissions the player has when opening the
     * menu.
     */
    @NotNull
    public Set<MenuPermission> getPermissions() {
        return permissions;
    }

    public void addPermission(@NotNull MenuPermission permission) {
        this.permissions.add(permission);
    }

    public void addPermissions(@NotNull MenuPermission... permissions) {
        this.permissions.addAll(Arrays.asList(permissions));
    }

    public void removePermission(@NotNull MenuPermission permission) {
        this.permissions.remove(permission);
    }

    /**
     * If this event is cancelled, the inventory will not be opened.
     */
    public boolean isCancelled() {
        return this.isCancelled;
    }

    public void setCancelled(final boolean cancel) {
        this.isCancelled = cancel;
    }

    @NotNull
    public BlockAccessMenuEvent call() {
        EVENT.invoker().onBlockAccessMenu(this);
        return this;
    }

    @FunctionalInterface
    public interface Listener {
        void onBlockAccessMenu(@NotNull BlockAccessMenuEvent event);
    }

    /**
     * The different permissions a player can have when opening
     * the menu. Each permission is specific to one element of the
     * menus and permission does not imply another unless explicitly
     * stated.
     *
     * @since 0.4.0
     */
    public enum MenuPermission {
        /* Allows a player to see the information of a block */
        INFO,
        /* Allows a player to lock or unlock a block */
        LOCK,
        /* Allows a player to manage redstone settings and friends */
        MANAGER
    }
}
