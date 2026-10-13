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

import de.sean.blockprot.nbt.stats.ListStatisticItem;
import de.sean.blockprot.nbt.stats.StatisticOnClickAction;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A statistic that can keep a list of items.
 *
 * @since 1.0.0
 */
public abstract class ListStatistic<V extends ListStatisticItem<IV, Block>, IV>
    extends Statistic<List<V>> {
    public abstract void add(@NotNull IV object);

    public abstract void remove(int index);

    public abstract void remove(@NotNull IV object);

    @Override
    public @NotNull StatisticOnClickAction getClickAction() {
        return StatisticOnClickAction.LIST_MENU;
    }

    protected @NotNull ListTag getList() {
        if (container.get(this.getKey()) instanceof ListTag list) return list;
        final ListTag list = new ListTag();
        container.put(this.getKey(), list);
        return list;
    }

    @Override
    public @NotNull String getTitle() {
        return getStatisticName();
    }
}
