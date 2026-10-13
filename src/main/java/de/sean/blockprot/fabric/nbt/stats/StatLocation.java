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

import de.sean.blockprot.fabric.util.BlockRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block location inside a dimension, as saved in the statistics file. Replaces Bukkit's Location.
 */
public record StatLocation(@NotNull String world, double x, double y, double z) {
    @NotNull
    public static StatLocation of(@NotNull BlockRef block) {
        return new StatLocation(block.level().dimension().identifier().toString(),
            block.pos().getX(), block.pos().getY(), block.pos().getZ());
    }

    public int getBlockX() {
        return (int) Math.floor(x);
    }

    public int getBlockY() {
        return (int) Math.floor(y);
    }

    public int getBlockZ() {
        return (int) Math.floor(z);
    }

    @Nullable
    public BlockRef toBlock(@Nullable MinecraftServer server) {
        if (server == null) return null;
        final Identifier id = Identifier.tryParse(world);
        if (id == null) return null;
        final ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (level == null) return null;
        return new BlockRef(level, new BlockPos(getBlockX(), getBlockY(), getBlockZ()));
    }
}
