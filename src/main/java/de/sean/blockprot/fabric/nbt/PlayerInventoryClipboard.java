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

package de.sean.blockprot.fabric.nbt;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * Serves as a clipboard database of configurations a player has copied
 * and could paste to some other block.
 *
 * @since 0.4.6
 */
public final class PlayerInventoryClipboard {
    @NotNull
    private static final HashMap<String, CompoundTag> players = new HashMap<>();

    private PlayerInventoryClipboard() {}

    @Nullable
    public static CompoundTag get(@NotNull String player) {
        return players.get(player);
    }

    public static void set(@NotNull String player, @NotNull CompoundTag compound) {
        players.put(player, compound);
    }

    public static void remove(@NotNull String player) {
        players.remove(player);
    }

    public static boolean contains(@NotNull String player) {
        return players.containsKey(player);
    }
}
