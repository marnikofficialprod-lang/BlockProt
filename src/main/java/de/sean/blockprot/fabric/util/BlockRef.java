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

package de.sean.blockprot.fabric.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block inside a level, the replacement for Bukkit's Block.
 */
public record BlockRef(@NotNull ServerLevel level, @NotNull BlockPos pos) {
    public BlockRef {
        pos = pos.immutable();
    }

    @NotNull
    public BlockState getState() {
        return level.getBlockState(pos);
    }

    @NotNull
    public Block getType() {
        return getState().getBlock();
    }

    @Nullable
    public BlockEntity getBlockEntity() {
        return level.getBlockEntity(pos);
    }

    @NotNull
    public BlockRef relative(final int x, final int y, final int z) {
        return new BlockRef(level, pos.offset(x, y, z));
    }
}
