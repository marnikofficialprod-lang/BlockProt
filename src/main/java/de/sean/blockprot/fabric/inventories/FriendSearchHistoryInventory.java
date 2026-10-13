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

import de.sean.blockprot.fabric.TranslationKey;
import de.sean.blockprot.fabric.Translator;
import de.sean.blockprot.fabric.nbt.PlayerSettingsHandler;
import de.sean.blockprot.nbt.FriendModifyAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Basic inventory showing the last 25 players that the player
 * searched for. This can be used to easily add a player that they
 * had just searched for again.
 *
 * @since 0.3.0
 */
public class FriendSearchHistoryInventory extends BlockProtInventory {
    private final int maxSkulls = getSize() - 2;

    @Override
    int getSize() {
        return InventoryConstants.tripleLine;
    }

    @Override
    @NotNull String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__FRIENDS__SEARCH_HISTORY);
    }

    @Override
    public void onClick(@NotNull MenuClick event, @NotNull InventoryState state) {
        final ServerPlayer player = event.player();
        final ItemStack item = event.currentItem();
        if (item.is(GuiItems.BLACK_STAINED_GLASS_PANE)) {
            closeAndOpen(player, new FriendManageInventory().fill(player));
        } else if (item.is(Items.PLAYER_HEAD) || item.is(Items.SKELETON_SKULL)) {
            final int index = event.slot();
            if (index >= 0 && index < state.friendResultCache.size()) {
                final var id = state.friendResultCache.get(index);
                modifyFriendsForAction(player, id, FriendModifyAction.ADD_FRIEND);
                closeAndOpen(player, new FriendManageInventory().fill(player));
            }
        } else {
            closeAndOpen(player, null);
        }
    }

    @Override
    public void onClose(@NotNull ServerPlayer player, @NotNull InventoryState state) {
    }

    @NotNull
    public FriendSearchHistoryInventory fill(@NotNull ServerPlayer player) {
        final InventoryState state = InventoryState.get(player.getUUID());
        if (state == null) return this;

        PlayerSettingsHandler settingsHandler = new PlayerSettingsHandler(player);
        final List<String> searchHistory = settingsHandler.getSearchHistory();
        state.friendResultCache.clear();
        final Map<Integer, UUID> skulls = new HashMap<>();
        int slot = 0;
        for (int i = 0; i < searchHistory.size() && slot < maxSkulls; i++) {
            try {
                final UUID uuid = UUID.fromString(searchHistory.get(i));
                this.setItemStack(slot, Items.SKELETON_SKULL, searchHistory.get(i));
                state.friendResultCache.add(uuid);
                skulls.put(slot, uuid);
                slot++;
            } catch (IllegalArgumentException ignored) {
            }
        }

        setBackButton();
        fillPlayerSkullsAsync(skulls);
        return this;
    }
}
