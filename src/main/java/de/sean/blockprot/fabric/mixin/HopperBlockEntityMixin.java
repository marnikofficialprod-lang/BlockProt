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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import de.sean.blockprot.fabric.listeners.HopperEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * InventoryMoveItemEvent: hoppers cannot take items out of protected containers.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    /**
     * A hopper (block or minecart) pulling from the container above it.
     */
    @WrapOperation(
        method = "suckInItems",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getSlots(Lnet/minecraft/world/Container;Lnet/minecraft/core/Direction;)[I")
    )
    private static int[] blockprot$protectSource(Container container, Direction direction, Operation<int[]> original,
                                                 @Local(argsOnly = true) Hopper hopper) {
        if (!HopperEventListener.isMoveAllowed(container, hopper)) return new int[0];
        return original.call(container, direction);
    }

    /**
     * A hopper block pushing its items into the container it faces.
     */
    @WrapOperation(
        method = "ejectItems",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getAttachedContainer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)Lnet/minecraft/world/Container;")
    )
    private static Container blockprot$protectHopper(Level level, BlockPos pos, HopperBlockEntity self, Operation<Container> original) {
        final Container destination = original.call(level, pos, self);
        if (destination != null && !HopperEventListener.isMoveAllowed(self, destination)) return null;
        return destination;
    }
}
