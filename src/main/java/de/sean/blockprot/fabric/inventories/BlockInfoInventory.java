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

import de.sean.blockprot.fabric.ProfileService;
import de.sean.blockprot.fabric.TranslationKey;
import de.sean.blockprot.fabric.Translator;
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.nbt.RedstoneSettingsHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockInfoInventory extends BlockProtInventory {
    private final int maxSkulls = getSize() - InventoryConstants.singleLine;

    @Override
    int getSize() {
        return InventoryConstants.sextupletLine;
    }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO);
    }

    @Override
    public void onClick(@NotNull MenuClick event, @NotNull InventoryState state) {
        final ServerPlayer player = event.player();
        final ItemStack item = event.currentItem();
        if (item.is(GuiItems.BLACK_STAINED_GLASS_PANE)) {
            if (state.getBlock() != null) {
                state.currentPageIndex = 0;
                BlockNBTHandler handler = getNbtHandlerOrNull(state.getBlock());
                closeAndOpen(
                    player,
                    handler == null
                        ? null
                        : new BlockLockInventory().fill(player, state.getBlock().getType(), handler)
                );
            }
        } else if (item.is(GuiItems.CYAN_STAINED_GLASS_PANE)) {
            if (state.getBlock() == null) return;
            if (state.currentPageIndex >= 1) {
                state.currentPageIndex--;
                BlockNBTHandler handler = getNbtHandlerOrNull(state.getBlock());
                closeAndOpen(
                    player,
                    handler == null
                        ? null
                        : this.fill(player, handler)
                );
            }
        } else if (item.is(GuiItems.BLUE_STAINED_GLASS_PANE)) {
            if (state.getBlock() == null) return;
            final ItemStack lastFriendInInventory = inventory.getItem(InventoryConstants.singleLine + maxSkulls - 1);
            if (!lastFriendInInventory.isEmpty()) {
                // There's an item in the last slot => The page is fully filled up, meaning we should go to the next page.
                state.currentPageIndex++;
                BlockNBTHandler handler = getNbtHandlerOrNull(state.getBlock());
                closeAndOpen(
                    player,
                    handler == null
                        ? null
                        : this.fill(player, handler)
                );
            }
        }
    }

    @Override
    public void onClose(@NotNull ServerPlayer player, @NotNull InventoryState state) {
    }

    @NotNull
    public BlockInfoInventory fill(@NotNull ServerPlayer player, @NotNull BlockNBTHandler handler) {
        final InventoryState state = InventoryState.get(player.getUUID());
        if (state == null) return this;

        String owner = handler.getOwner();
        var friends = handler.getFriends();

        state.friendResultCache.clear();
        this.inventory.clearContent();
        final Map<Integer, UUID> skulls = new HashMap<>();
        var pageOffset = maxSkulls * state.currentPageIndex;
        for (int i = 0; i < Math.min(friends.size() - pageOffset, maxSkulls); i++) {
            final var uuid = friends.get(pageOffset + i).getName();
            if (friends.get(pageOffset + i).doesRepresentPublic()) {
                this.setItemStack(InventoryConstants.singleLine + i, Items.PLAYER_HEAD, TranslationKey.INVENTORIES__FRIENDS__THE_PUBLIC);
            } else {
                this.setItemStack(InventoryConstants.singleLine + i, Items.SKELETON_SKULL, uuid);
            }
            try {
                final UUID id = UUID.fromString(uuid);
                state.friendResultCache.add(id);
                skulls.put(InventoryConstants.singleLine + i, id);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (!owner.isEmpty()) {
            try {
                final UUID ownerUuid = UUID.fromString(owner);
                setPlayerSkull(0, ownerUuid, ProfileService.getIfPresent(ownerUuid));
                skulls.put(0, ownerUuid);
            } catch (IllegalArgumentException e) {
                logProfileFailure(e);
            }
        }
        setItemStack(
            1,
            Items.OAK_SIGN,
            handler.getName()
        );
        setItemStack(
            InventoryConstants.lineLength - 3,
            GuiItems.CYAN_STAINED_GLASS_PANE,
            TranslationKey.INVENTORIES__LAST_PAGE
        );
        setItemStack(
            InventoryConstants.lineLength - 2,
            GuiItems.BLUE_STAINED_GLASS_PANE,
            TranslationKey.INVENTORIES__NEXT_PAGE
        );

        RedstoneSettingsHandler redstoneSettingsHandler = handler.getRedstoneHandler();
        setEnchantedOptionItemStack(
            2,
            Items.REDSTONE,
            TranslationKey.INVENTORIES__REDSTONE__REDSTONE_PROTECTION,
            redstoneSettingsHandler.getCurrentProtection()
        );
        setEnchantedOptionItemStack(
            3,
            Items.HOPPER,
            TranslationKey.INVENTORIES__REDSTONE__HOPPER_PROTECTION,
            redstoneSettingsHandler.getHopperProtection()
        );
        setEnchantedOptionItemStack(
            4,
            Items.PISTON,
            TranslationKey.INVENTORIES__REDSTONE__PISTON_PROTECTION,
            redstoneSettingsHandler.getPistonProtection()
        );
        setBackButton(InventoryConstants.lineLength - 1);

        fillPlayerSkullsAsync(skulls);
        return this;
    }
}
