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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A statistic that keeps track of many {@link StatLocation}s.
 *
 * @since 1.0.0
 */
public abstract class LocationListStatistic extends ListStatistic<LocationListEntry, StatLocation> {
    private @NotNull StatLocation parseLocationFromCompound(@NotNull CompoundTag c) {
        return new StatLocation(
            c.getStringOr("name", ""),
            c.getDoubleOr("x", 0),
            c.getDoubleOr("y", 0),
            c.getDoubleOr("z", 0));
    }

    private @NotNull CompoundTag writeLocationToCompound(@NotNull StatLocation location) {
        final CompoundTag compound = new CompoundTag();
        compound.putString("name", location.world());
        compound.putDouble("x", location.x());
        compound.putDouble("y", location.y());
        compound.putDouble("z", location.z());
        return compound;
    }

    @Override
    public @NotNull String toString() {
        return this.get().toString();
    }

    @Override
    public @NotNull List<LocationListEntry> get() {
        final List<LocationListEntry> ret = new ArrayList<>();
        for (Tag tag : getList()) {
            if (tag instanceof CompoundTag compound)
                ret.add(new LocationListEntry(parseLocationFromCompound(compound)));
        }
        return ret;
    }

    @Override
    public void set(@NotNull List<LocationListEntry> value) {
        container.remove(this.getKey());
        value.forEach(v -> getList().add(writeLocationToCompound(v.get())));
        markDirty();
    }

    @Override
    public void add(@NotNull StatLocation vector) {
        getList().add(writeLocationToCompound(vector));
        markDirty();
    }

    @Override
    public void remove(@NotNull StatLocation object) {
        getList().removeIf(c -> c instanceof CompoundTag compound && parseLocationFromCompound(compound).equals(object));
        markDirty();
    }

    @Override
    public void remove(int index) {
        getList().remove(index);
        markDirty();
    }
}
