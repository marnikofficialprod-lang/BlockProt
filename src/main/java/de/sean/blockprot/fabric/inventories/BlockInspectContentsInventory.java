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

import de.sean.blockprot.fabric.util.BlockRef;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inventory to mirror the contents of another inventory. Works for any inventory type, including
 * furnaces, hoppers, etc.
 *
 * @since 1.1.4
 */
public class BlockInspectContentsInventory extends BlockProtInventory {
    @NotNull
    private final Container sourceInventory;

    private BlockInspectContentsInventory(@NotNull final Container source, @NotNull final BlockEntity blockEntity) {
        super(false);
        this.sourceInventory = source;
        if (blockEntity.getBlockState().getBlock() instanceof ChestBlock) {
            this.inventory = createInventory();
        } else if (blockEntity instanceof HopperBlockEntity) {
            this.inventory = createInventory(MenuType.HOPPER, source.getContainerSize(), titleOf(blockEntity));
        } else if (blockEntity instanceof DispenserBlockEntity && source.getContainerSize() == 9) {
            this.inventory = createInventory(MenuType.GENERIC_3x3, 9, titleOf(blockEntity));
        } else {
            this.inventory = createInventory(titleOf(blockEntity).getString());
        }
    }

    /**
     * Creates the inventory for the block of the player's current inventory state.
     *
     * @return The inventory, or null if the block has no inventory.
     */
    @Nullable
    public static BlockInspectContentsInventory create(@NotNull final ServerPlayer player) {
        final var state = InventoryState.get(player.getUUID());
        if (state == null || state.getBlock() == null) return null;
        final BlockRef block = state.getBlock();
        final BlockEntity blockEntity = block.getBlockEntity();
        if (!(blockEntity instanceof Container container)) return null;

        Container source = container;
        if (block.getState().getBlock() instanceof ChestBlock chestBlock) {
            // Double chests show both halves.
            final Container combined = ChestBlock.getContainer(chestBlock, block.getState(), block.level(), block.pos(), true);
            if (combined != null) source = combined;
        }
        return new BlockInspectContentsInventory(source, blockEntity).fill();
    }

    @NotNull
    private static Component titleOf(@NotNull final BlockEntity blockEntity) {
        if (blockEntity instanceof Nameable nameable) return nameable.getDisplayName();
        return Component.translatable("container.chest");
    }

    @Override
    int getSize() {
        return sourceInventory.getContainerSize();
    }

    @Override
    @Nullable
    final String getTranslatedInventoryName() {
        return null;
    }

    @Override
    public void onClick(@NotNull MenuClick event, @NotNull InventoryState state) {
        // This inventory is only to inspect the contents, not for taking items out.
    }

    @Override
    public void onClose(@NotNull ServerPlayer player, @NotNull InventoryState state) {
    }

    @NotNull
    public BlockInspectContentsInventory fill() {
        final int size = Math.min(this.inventory.getContainerSize(), sourceInventory.getContainerSize());
        for (int i = 0; i < size; i++) {
            this.inventory.setItem(i, sourceInventory.getItem(i).copy());
        }
        return this;
    }
}
