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

import de.sean.blockprot.fabric.util.Scheduler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A server-side only menu which displays a {@link BlockProtInventory}. The client sees a normal
 * chest (or hopper/dispenser) screen. No item can be moved, every click on the top inventory
 * is forwarded to {@link BlockProtInventory#onClick(MenuClick, InventoryState)} instead.
 */
public final class GuiMenu extends AbstractContainerMenu {
    @NotNull
    private final BlockProtInventory holder;
    private final int containerSize;

    GuiMenu(@NotNull final MenuType<?> menuType, final int containerId, @NotNull final Inventory playerInventory,
            @NotNull final BlockProtInventory holder) {
        super(menuType, containerId);
        this.holder = holder;
        final Container container = holder.getInventory();
        this.containerSize = container.getContainerSize();
        for (int i = 0; i < containerSize; i++) {
            this.addSlot(new Slot(container, i, 0, 0) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(@NotNull Player player) {
                    return false;
                }
            });
        }
        this.addStandardInventorySlots(playerInventory, 0, 0);
    }

    @NotNull
    public BlockProtInventory getHolder() {
        return holder;
    }

    @Override
    public void clicked(final int slotIndex, final int buttonNum, @NotNull final ContainerInput containerInput, @NotNull final Player player) {
        // Never let vanilla move any items. The client gets resynced after the click.
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (slotIndex < 0 || slotIndex >= containerSize) return;
        if (containerInput == ContainerInput.QUICK_CRAFT || containerInput == ContainerInput.PICKUP_ALL) return;

        final ItemStack item = holder.getInventory().getItem(slotIndex).copy();
        // Handle the click after the packet has been processed, so that opening other menus is safe.
        Scheduler.runLater(() -> {
            if (serverPlayer.containerMenu != this) return;
            holder.handleClick(new MenuClick(serverPlayer, slotIndex, item, containerInput, buttonNum));
        });
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull final Player player, final int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull final Player player) {
        return true;
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull final ItemStack carried, @NotNull final Slot target) {
        return false;
    }

    @Override
    public boolean canDragTo(@NotNull final Slot slot) {
        return false;
    }

    @Override
    public void removed(@NotNull final Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) {
            holder.handleClose(serverPlayer);
        }
    }
}
