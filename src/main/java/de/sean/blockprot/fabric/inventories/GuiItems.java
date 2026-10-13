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

package de.sean.blockprot.fabric.inventories;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The coloured items used by the menus. Since 26.1 the dyed items are grouped in colour collections.
 */
final class GuiItems {
    static final Item BLACK_STAINED_GLASS_PANE = Items.STAINED_GLASS_PANE.pick(DyeColor.BLACK);
    static final Item BLUE_STAINED_GLASS_PANE = Items.STAINED_GLASS_PANE.pick(DyeColor.BLUE);
    static final Item CYAN_STAINED_GLASS_PANE = Items.STAINED_GLASS_PANE.pick(DyeColor.CYAN);
    static final Item RED_STAINED_GLASS_PANE = Items.STAINED_GLASS_PANE.pick(DyeColor.RED);
    static final Item GREEN_STAINED_GLASS_PANE = Items.STAINED_GLASS_PANE.pick(DyeColor.GREEN);

    private GuiItems() {
    }
}
