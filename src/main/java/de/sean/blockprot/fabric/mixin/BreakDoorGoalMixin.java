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

import de.sean.blockprot.fabric.listeners.ProtectionListener;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * EntityBreakDoorEvent: zombies cannot break protected doors.
 */
@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalMixin {
    @Inject(method = {"canUse", "canContinueToUse"}, at = @At("RETURN"), cancellable = true)
    private void blockprot$protectDoor(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        final DoorInteractGoalAccessor goal = (DoorInteractGoalAccessor) this;
        if (!ProtectionListener.canEntityChangeBlock(goal.blockprot$getMob().level(), goal.blockprot$getDoorPos())) {
            cir.setReturnValue(false);
        }
    }
}
