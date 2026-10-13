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

package de.sean.blockprot.fabric.nbt.stats;

import de.sean.blockprot.fabric.BlockProt;
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.util.BlockRef;
import de.sean.blockprot.fabric.util.BlockUtil;
import de.sean.blockprot.nbt.stats.ListStatisticItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

/**
 * @since 1.0.0
 */
public class LocationListEntry extends ListStatisticItem<StatLocation, Block> {
    public LocationListEntry(@NotNull StatLocation value) {
        super(value);
    }

    public @Nullable BlockRef getBlock() {
        return this.get().toBlock(BlockProt.getServer());
    }

    @Override
    public @NotNull Block getItemType() {
        final BlockRef block = getBlock();
        return block == null ? Blocks.AIR : block.getType();
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public String getTitle() {
        final BlockRef block = getBlock();
        String name;
        try {
            name = block == null ? "" : new BlockNBTHandler(block).getName();
        } catch (RuntimeException e) {
            name = BlockUtil.getMaterialName(block.getType());
        }
        var coordinates = new StringJoiner(", ", "[", "]")
            .add(String.valueOf(this.value.getBlockX()))
            .add(String.valueOf(this.value.getBlockY()))
            .add(String.valueOf(this.value.getBlockZ()))
            .toString();
        return name + " " + coordinates;
    }
}
