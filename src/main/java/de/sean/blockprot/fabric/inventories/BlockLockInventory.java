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

import de.sean.blockprot.fabric.BlockProt;
import de.sean.blockprot.fabric.Permissions;
import de.sean.blockprot.fabric.TranslationKey;
import de.sean.blockprot.fabric.Translator;
import de.sean.blockprot.fabric.events.BlockAccessMenuEvent;
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.nbt.PlayerInventoryClipboard;
import de.sean.blockprot.fabric.util.BlockRef;
import de.sean.blockprot.fabric.util.BlockUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockLockInventory extends BlockProtInventory {
    @Override
    int getSize() {
        return InventoryConstants.doubleLine;
    }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__BLOCK_LOCK);
    }

    @Override
    public void onClick(@NotNull MenuClick event, @NotNull InventoryState state) {
        BlockRef block = state.getBlock();
        if (block == null) return;
        ItemStack item = event.currentItem();
        ServerPlayer player = event.player();

        if (BlockProt.getDefaultConfig().isLockable(block.getType()) && event.slot() == 0) {
            applyChanges(
                player,
                (handler) -> handler.lockBlock(player),
                null
            );
            closeAndOpen(player, null);
        } else if (item.is(Items.PLAYER_HEAD)) {
            closeAndOpen(player, new FriendManageInventory().fill(player));
        } else if (item.is(Items.REDSTONE)) {
            var handler = getNbtHandlerOrNull(block);
            closeAndOpen(
                player,
                handler == null
                    ? null
                    : new RedstoneSettingsInventory().fill(player, state)
            );
        } else if (item.is(Items.OAK_SIGN)) {
            var handler = getNbtHandlerOrNull(block);
            closeAndOpen(
                player,
                handler == null
                    ? null
                    : new BlockInfoInventory().fill(player, handler)
            );
        } else if (item.is(Items.KNOWLEDGE_BOOK)) {
            // Paste
            var handler = getNbtHandlerOrNull(block);
            var container = PlayerInventoryClipboard.get(player.getUUID().toString());
            if (handler != null && container != null)
                handler.pasteNbt(container.copy());
        } else if (item.is(Items.PAPER)) {
            // Copy
            var handler = getNbtHandlerOrNull(block);
            if (handler != null) {
                PlayerInventoryClipboard.set(player.getUUID().toString(), handler.getNbtCopy());
                // The player probably doesn't want to paste the data onto the same container.
                closeAndOpen(player, null);
            }
        } else if (item.is(Items.NAME_TAG)) {
            player.closeContainer();
            AnvilInputMenu.open(player, Translator.get(TranslationKey.INVENTORIES__SET_BLOCK_NAME), "Block name", (p, text) -> {
                var invState = InventoryState.get(p.getUUID());
                if (invState == null || invState.getBlock() == null) {
                    closeAndOpen(p, null);
                    return;
                }
                var handler = getNbtHandlerOrNull(invState.getBlock());
                if (handler == null) {
                    closeAndOpen(p, null);
                    return;
                }
                handler.setName(text);
                handler.applyToOtherContainer();
                closeAndOpen(p, new BlockLockInventory().fill(p, invState.getBlock().getType(), handler));
            });
        } else if (item.is(Items.SPYGLASS)) {
            closeAndOpen(player, BlockInspectContentsInventory.create(player));
        } else {
            closeAndOpen(player, null);
        }
    }

    @Override
    public void onClose(@NotNull ServerPlayer player, @NotNull InventoryState state) {
    }

    @Nullable
    public BlockLockInventory fill(@NotNull ServerPlayer player, @NotNull Block material, @NotNull BlockNBTHandler handler) {
        final InventoryState state = InventoryState.get(player.getUUID());
        if (state == null) return this;

        var isNotProtected = handler.isNotProtected();

        // This means the user only has INFO permissions but the block is not locked and can
        // therefore not provide any information.
        if (isNotProtected && state.menuPermissions.size() == 1
            && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.INFO))
            return null;

        if (state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.LOCK)) {
            setItemStack(
                0,
                BlockUtil.getProperItem(material),
                isNotProtected
                    ? TranslationKey.INVENTORIES__LOCK
                    : TranslationKey.INVENTORIES__UNLOCK
            );
        }

        // We insert a 'inspect contents' button if the player does not own the block and has admin permissions.
        var rightItemOffset = !isNotProtected
            && !handler.isOwner(player.getUUID())
            && state.getBlock() != null
            && state.getBlock().getBlockEntity() instanceof Container
            && Permissions.ADMIN.check(player) ? 1 : 0;
        if (rightItemOffset == 1) {
            setItemStack(
                getSize() - 2,
                Items.SPYGLASS,
                TranslationKey.INVENTORIES__INSPECT_CONTENTS
            );
        }

        if (!isNotProtected && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.MANAGER)) {
            var offset = 1;
            setItemStack(
                offset++,
                Items.REDSTONE,
                TranslationKey.INVENTORIES__REDSTONE__SETTINGS
            );
            if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
                setItemStack(
                    offset++,
                    Items.PLAYER_HEAD,
                    TranslationKey.INVENTORIES__FRIENDS__MANAGE
                );
            }
            setItemStack(
                offset,
                Items.NAME_TAG,
                TranslationKey.INVENTORIES__SET_BLOCK_NAME
            );
            if (PlayerInventoryClipboard.contains(player.getUUID().toString())) {
                setItemStack(
                    getSize() - 4 - rightItemOffset,
                    Items.KNOWLEDGE_BOOK,
                    TranslationKey.INVENTORIES__PASTE_CONFIGURATION
                );
            }
            setItemStack(
                getSize() - 3 - rightItemOffset,
                Items.PAPER,
                TranslationKey.INVENTORIES__COPY_CONFIGURATION
            );
        }
        if (!isNotProtected && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.INFO)) {
            setItemStack(
                getSize() - 2 - rightItemOffset,
                Items.OAK_SIGN,
                TranslationKey.INVENTORIES__BLOCK_INFO
            );
        }
        setBackButton();
        return this;
    }
}
