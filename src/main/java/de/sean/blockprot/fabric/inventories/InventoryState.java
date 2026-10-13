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

package de.sean.blockprot.fabric.inventories;

import de.sean.blockprot.fabric.events.BlockAccessMenuEvent;
import de.sean.blockprot.fabric.util.BlockRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Storage for the current state and location of each player's
 * interactions with this mod's menus.
 *
 * @since 0.2.3
 */
public final class InventoryState {
    /**
     * HashMap containing the current InventoryState of each player.
     * The keys are the String representation of the player's UUID.
     */
    private static final HashMap<String, InventoryState> players = new HashMap<>();

    /**
     * A local cache of players for this state.
     */
    @NotNull
    public final ArrayList<UUID> friendResultCache = new ArrayList<>();

    @Nullable
    private final BlockRef block;

    /**
     * The current state of the friend search mechanism.
     * When adding default friends this should be {@link FriendSearchState#DEFAULT_FRIEND_SEARCH},
     * which will then add the new friends to the current players NBT.
     * When adding friends for a single block this should be {@link FriendSearchState#FRIEND_SEARCH},
     * which will then add the new friends to {@link #block}'s NBT.
     */
    @NotNull
    public FriendSearchState friendSearchState = FriendSearchState.FRIEND_SEARCH;

    /**
     * The current index of the page, if the inventory
     * has any multi-page capabilities.
     */
    public int currentPageIndex = 0;

    /**
     * The friend we currently want to modify with {@link FriendDetailInventory}.
     */
    @Nullable
    public UUID currentFriend = null;

    /**
     * The current cached menu permissions for this player.
     */
    @NotNull
    public Set<BlockAccessMenuEvent.MenuPermission> menuPermissions = new HashSet<>();

    public InventoryState(@Nullable BlockRef block) {
        this.block = block;
    }

    public static void set(UUID player, InventoryState state) {
        players.put(player.toString(), state);
    }

    @Nullable
    public static InventoryState get(UUID player) {
        return players.get(player.toString());
    }

    public static void remove(UUID player) {
        players.remove(player.toString());
    }

    public static void clear() {
        players.clear();
    }

    /**
     * The block that backs the values of the inventory state.
     * If this inventory state is for a user settings inventory, this
     * block will be null.
     */
    @Nullable
    public BlockRef getBlock() {
        return this.block;
    }

    /**
     * The current search state of the friend menu. Indicates
     * whether we're searching for default friends or for friends
     * to be added directly to a block.
     */
    public enum FriendSearchState {
        /**
         * This search is currently for a single block.
         */
        FRIEND_SEARCH,

        /**
         * This search is currently for the default friends
         * of a player.
         */
        DEFAULT_FRIEND_SEARCH,
    }
}
