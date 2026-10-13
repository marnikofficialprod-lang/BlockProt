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

import de.sean.blockprot.fabric.BlockProt;
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.nbt.BlockProtAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes the protection of a block without block entity (doors, trapdoors, ...) once it gets
 * replaced by a block that cannot be locked, so a new block placed there isn't locked already.
 * Changes between lockable blocks, like oxidizing copper doors, keep the protection.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void blockprot$removeStaleData(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        final LevelChunk self = (LevelChunk) (Object) this;
        if (!self.hasAttached(BlockProtAttachments.CHUNK_BLOCKS)) return;
        if (self.getBlockState(pos).getBlock() == state.getBlock()) return;
        if (BlockProt.getDefaultConfig().isLockableBlock(state.getBlock())) return;
        BlockNBTHandler.removeChunkData(self, pos);
    }
}
