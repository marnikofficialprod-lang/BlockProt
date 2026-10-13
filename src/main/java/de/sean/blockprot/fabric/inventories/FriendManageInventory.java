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
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.nbt.FriendSupportingHandler;
import de.sean.blockprot.nbt.LockReturnValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FriendManageInventory extends BlockProtInventory {
    private final int maxSkulls = getSize() - InventoryConstants.singleLine;

    @Override
    public int getSize() {
        return InventoryConstants.sextupletLine; // 6 Lines of inventory go brr
    }

    @NotNull
    @Override
    public String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE);
    }

    /**
     * Exits this inventory depending on {@code state}'s FriendSearchState back to the {@link BlockLockInventory}
     * or the {@link UserSettingsInventory} respectively.
     *
     * @since 0.3.0
     */
    public void exitModifyInventory(@NotNull final ServerPlayer player, @NotNull final InventoryState state) {
        BlockProtInventory newInventory;
        switch (state.friendSearchState) {
            case FRIEND_SEARCH -> {
                if (state.getBlock() == null) return;
                BlockNBTHandler handler = getNbtHandlerOrNull(state.getBlock());
                if (handler == null) {
                    newInventory = null;
                } else {
                    newInventory = new BlockLockInventory()
                            .fill(
                                player,
                                state.getBlock().getType(),
                                handler);
                }
            }
            case DEFAULT_FRIEND_SEARCH -> newInventory = new UserSettingsInventory().fill(player);
            default -> {
                return;
            }
        }
        closeAndOpen(player, newInventory);
    }

    @Override
    public void onClick(@NotNull MenuClick event, @NotNull InventoryState state) {
        final ServerPlayer player = event.player();
        final ItemStack item = event.currentItem();
        if (item.is(GuiItems.BLACK_STAINED_GLASS_PANE)) {
            // Exit the friend modify inventory and return to the base lock inventory.
            state.currentPageIndex = 0;
            exitModifyInventory(player, state);
        } else if (item.is(GuiItems.CYAN_STAINED_GLASS_PANE)) {
            if (state.currentPageIndex >= 1) {
                state.currentPageIndex--;
                closeAndOpen(player, fill(player));
            }
        } else if (item.is(GuiItems.BLUE_STAINED_GLASS_PANE)) {
            ItemStack lastFriendInInventory = inventory.getItem(maxSkulls - 1);
            if (!lastFriendInInventory.isEmpty()) {
                // There's an item in the last slot => The page is fully filled up, meaning
                // we should go to the next page.
                state.currentPageIndex++;
                closeAndOpen(player, fill(player));
            }
        } else if ((item.is(Items.SKELETON_SKULL) || item.is(Items.PLAYER_HEAD)) && event.slot() < maxSkulls) {
            // Get the clicked player head and open the detail inventory.
            final var index = event.slot();
            if (index >= 0 && index < state.friendResultCache.size()) {
                state.currentFriend = state.friendResultCache.get(index);
                var inv = new FriendDetailInventory().fill(player);
                closeAndOpen(player, inv);
            }
        } else if (item.is(Items.MAP)) {
            FriendSearchInventory.openAnvilInventory(player);
        } else if (item.is(Items.BOOK)) {
            closeAndOpen(player, new FriendSearchHistoryInventory().fill(player));
        } else if (item.is(Items.WITHER_SKELETON_SKULL)) {
            applyChanges(
                player,
                (handler) -> {
                    handler.addEveryoneAsFriend();
                    return new LockReturnValue(true, null);
                },
                FriendSupportingHandler::addEveryoneAsFriend
            );
            fill(player); // Essentially rebuilds the inventory.
        } else {
            closeAndOpen(player, null); // Unexpected, exit the inventory.
        }
    }

    @Override
    public void onClose(@NotNull ServerPlayer player, @NotNull InventoryState state) {
    }

    @Nullable
    public FriendManageInventory fill(@NotNull ServerPlayer player) {
        final InventoryState state = InventoryState.get(player.getUUID());
        if (state == null) return this;

        final @Nullable FriendSupportingHandler handler =
            getFriendSupportingHandler(state.friendSearchState, player, state.getBlock());
        if (handler == null) return null;

        var friends = handler.getFriends();

        // Fill the first page inventory with skeleton skulls.
        state.friendResultCache.clear();
        this.inventory.clearContent();
        final Map<Integer, UUID> skulls = new HashMap<>();
        var hasAddedPublic = false;
        for (final var friend : friends) {
            if (friend.doesRepresentPublic())
                hasAddedPublic = true;
        }

        var pageOffset = maxSkulls * state.currentPageIndex;
        for (int i = 0; i < Math.min(friends.size() - pageOffset, maxSkulls); i++) {
            final var uuid = friends.get(pageOffset + i).getName();
            if (friends.get(pageOffset + i).doesRepresentPublic()) {
                this.setItemStack(i, Items.PLAYER_HEAD,
                    TranslationKey.INVENTORIES__FRIENDS__THE_PUBLIC,
                    List.of(Translator.get(TranslationKey.INVENTORIES__FRIENDS__THE_PUBLIC_DESC)));
            } else {
                this.setItemStack(i, Items.SKELETON_SKULL, uuid);
            }
            try {
                final UUID id = UUID.fromString(uuid);
                state.friendResultCache.add(id);
                skulls.put(i, id);
            } catch (IllegalArgumentException ignored) {
            }
        }

        setItemStack(
            maxSkulls,
            GuiItems.CYAN_STAINED_GLASS_PANE,
            TranslationKey.INVENTORIES__LAST_PAGE);
        setItemStack(
            maxSkulls + 1,
            GuiItems.BLUE_STAINED_GLASS_PANE,
            TranslationKey.INVENTORIES__NEXT_PAGE);
        if (!hasAddedPublic) {
            setItemStack(
                getSize() - 4,
                Items.WITHER_SKELETON_SKULL,
                TranslationKey.INVENTORIES__FRIENDS__MAKE_PUBLIC);
        }
        setItemStack(
            getSize() - 3,
            Items.BOOK,
            TranslationKey.INVENTORIES__FRIENDS__SEARCH_HISTORY);
        setItemStack(
            getSize() - 2,
            Items.MAP,
            TranslationKey.INVENTORIES__FRIENDS__SEARCH);
        setBackButton();

        fillPlayerSkullsAsync(skulls);
        return this;
    }
}
