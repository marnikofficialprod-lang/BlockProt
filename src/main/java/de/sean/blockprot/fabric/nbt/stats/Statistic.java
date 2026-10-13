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

import de.sean.blockprot.fabric.nbt.NBTHandler;
import de.sean.blockprot.fabric.nbt.StatHandler;
import de.sean.blockprot.nbt.stats.StatisticOnClickAction;
import de.sean.blockprot.nbt.stats.StatisticType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

/**
 * A statistic.
 *
 * @since 1.0.0
 */
public abstract class Statistic<V> extends NBTHandler implements Comparable<Statistic<V>> {
    /** The key for getting the NBT for this statistic. */
    public abstract @NotNull String getKey();

    /** The type of this statistic. */
    public abstract @NotNull StatisticType getType();

    /** Get the (translated) name of this statistic. */
    public abstract String getStatisticName();

    public abstract @NotNull V get();

    public abstract void set(@NotNull V value);

    /** Update the NBT container from which this statistic shall be read. */
    public void updateContainer(@NotNull CompoundTag container) {
        this.container = container;
    }

    @Override
    protected void markDirty() {
        StatHandler.markStatsDirty();
    }

    public @NotNull Item getItemType() {
        return Items.DIRT;
    }

    /** Get what should happen when the user clicks on this statistic. */
    public @NotNull StatisticOnClickAction getClickAction() {
        return StatisticOnClickAction.NONE;
    }

    /** Returns the formatted statistic name with the value of this statistic. */
    public @NotNull String getTitle() {
        return getStatisticName() + ": " + get();
    }

    @Override
    public int compareTo(@NotNull Statistic<V> o) {
        return o.getKey().equals(this.getKey()) ? 0 : -1;
    }
}
