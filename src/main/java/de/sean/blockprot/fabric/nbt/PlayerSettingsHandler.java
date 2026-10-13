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

package de.sean.blockprot.fabric.nbt;

import de.sean.blockprot.fabric.BlockProt;
import de.sean.blockprot.fabric.inventories.InventoryConstants;
import de.sean.blockprot.util.BlockProtUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A simple handler to get a player's BlockProt settings.
 *
 * @since 0.2.3
 */
public final class PlayerSettingsHandler extends FriendSupportingHandler {
    static final String LOCK_ON_PLACE_ATTRIBUTE = "splugin_lock_on_place";
    static final String DEFAULT_FRIENDS_ATTRIBUTE = "blockprot_default_friends";
    static final String PLAYER_SEARCH_HISTORY = "blockprot_player_search_history";

    /**
     * Flag saved in NBT to check if the player has ever interacted with a
     * menu from BlockProt and if any hints should be sent to them.
     */
    static final String PLAYER_HAS_INTERACTED_WITH_MENU = "blockprot_player_has_interacted_with_menu";

    private static final int MAX_HISTORY_SIZE = InventoryConstants.tripleLine - 2;

    /**
     * The player that this settings handler is getting values
     * for.
     *
     * @since 0.2.3
     */
    public final ServerPlayer player;

    /**
     * Create a new settings handler.
     *
     * @since 0.2.3
     */
    public PlayerSettingsHandler(@NotNull final ServerPlayer player) {
        super(DEFAULT_FRIENDS_ATTRIBUTE);
        this.player = player;
        this.container = player.getAttachedOrCreate(BlockProtAttachments.PLAYER_SETTINGS, CompoundTag::new);
    }

    @Override
    protected void markDirty() {
        player.setAttached(BlockProtAttachments.PLAYER_SETTINGS, container);
    }

    /**
     * Check if the player wants their blocks to be locked when
     * placed.
     *
     * @since 0.2.3
     */
    public boolean getLockOnPlace() {
        // We will default to the config value.
        if (!container.contains(LOCK_ON_PLACE_ATTRIBUTE))
            return BlockProt.getDefaultConfig().lockOnPlaceByDefault();
        return container.getBooleanOr(LOCK_ON_PLACE_ATTRIBUTE, true);
    }

    public void setLockOnPlace(final boolean lockOnPlace) {
        container.putBoolean(LOCK_ON_PLACE_ATTRIBUTE, lockOnPlace);
        markDirty();
    }

    /**
     * Get the current search history for this player.
     *
     * @since 0.3.0
     */
    public List<String> getSearchHistory() {
        if (!container.contains(PLAYER_SEARCH_HISTORY)) return new ArrayList<>();
        else {
            return BlockProtUtil
                .parseStringList(container.getStringOr(PLAYER_SEARCH_HISTORY, ""));
        }
    }

    /**
     * Clears the search history by removing the list completely.
     *
     * @since 0.3.0
     */
    public void clearSearchHistory() {
        if (container.contains(PLAYER_SEARCH_HISTORY)) {
            container.remove(PLAYER_SEARCH_HISTORY);
            markDirty();
        }
    }

    /**
     * Add a player to the search history.
     *
     * @since 1.1.16
     */
    public void addPlayerToSearchHistory(@NotNull final UUID player) {
        List<String> history = getSearchHistory();
        if (!history.contains(player.toString())) {
            // We want the list to not be bigger than MAX_HISTORY_SIZE,
            // therefore we remove the first entry if we would exceed that size.
            if (history.size() == MAX_HISTORY_SIZE) {
                history.remove(0);
            }
            history.add(player.toString());
            container.putString(PLAYER_SEARCH_HISTORY, history.toString());
            markDirty();
        }
    }

    /**
     * Get whether this player has interacted with any of the mod's
     * menus before.
     *
     * @since 1.0.0
     */
    public boolean hasPlayerInteractedWithMenu() {
        return container.getBooleanOr(PLAYER_HAS_INTERACTED_WITH_MENU, false);
    }

    public void setHasPlayerInteractedWithMenu(boolean bool) {
        container.putBoolean(PLAYER_HAS_INTERACTED_WITH_MENU, bool);
        markDirty();
    }

    @Override
    public void mergeHandler(@NotNull NBTHandler handler) {
        if (!(handler instanceof final PlayerSettingsHandler playerSettingsHandler)) return;
        this.setLockOnPlace(playerSettingsHandler.getLockOnPlace());
        this.setFriends(playerSettingsHandler.getFriends());
    }
}
