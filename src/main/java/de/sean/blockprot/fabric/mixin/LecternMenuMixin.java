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

package de.sean.blockprot.fabric.mixin;

import de.sean.blockprot.fabric.listeners.InventoryEventListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.LecternMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PlayerTakeLecternBookEvent: friends without the write permission cannot take the book.
 */
@Mixin(LecternMenu.class)
public abstract class LecternMenuMixin {
    /** The button of the "Take Book" action. */
    private static final int TAKE_BOOK_BUTTON = 3;

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void blockprot$protectBook(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        if (buttonId == TAKE_BOOK_BUTTON && InventoryEventListener.isReadOnly((AbstractContainerMenu) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
