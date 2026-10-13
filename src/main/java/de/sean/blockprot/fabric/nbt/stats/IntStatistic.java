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

import org.jetbrains.annotations.NotNull;

/**
 * A base statistic backed by a single integer.
 *
 * @since 1.0.0
 */
public abstract class IntStatistic extends Statistic<Integer> {
    @Override
    public @NotNull String toString() {
        return this.get().toString();
    }

    @Override
    public @NotNull Integer get() {
        return this.container.getIntOr(this.getKey(), 0);
    }

    @Override
    public void set(@NotNull Integer value) {
        this.container.putInt(this.getKey(), value);
        markDirty();
    }

    @Override
    public int compareTo(@NotNull Statistic<Integer> o) {
        return this.get().compareTo(o.get());
    }

    /** Increments the value. */
    public void increment() {
        if (this.get() < Integer.MAX_VALUE)
            this.set(this.get() + 1);
    }

    /** Decrements the value. The result cannot be negative. */
    public void decrement() {
        if (this.get() > 0)
            this.set(this.get() - 1);
    }
}
