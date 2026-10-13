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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.sean.blockprot.fabric.listeners.ProtectionListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * EntityExplodeEvent/BlockExplodeEvent: explosions don't destroy protected blocks.
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @ModifyExpressionValue(
        method = "explode",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;")
    )
    private List<BlockPos> blockprot$protectBlocks(List<BlockPos> original) {
        return ProtectionListener.filterExplodedBlocks(this.level, original);
    }
}
