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

import de.sean.blockprot.fabric.BlockProt;
import de.sean.blockprot.fabric.TranslationKey;
import de.sean.blockprot.fabric.Translator;
import de.sean.blockprot.fabric.nbt.stats.ListStatistic;
import de.sean.blockprot.fabric.util.BlockUtil;
import de.sean.blockprot.nbt.stats.ListStatisticItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 1.0.0
 */
public final class StatisticListInventory extends BlockProtInventory {
    /** Stores the statistic for pages to use. */
    private ListStatistic<? extends ListStatisticItem<?, Block>, ?> statistic;

    @Override
    int getSize() {
        return InventoryConstants.sextupletLine;
    }

    @Override
    @NotNull String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__STATISTICS__STATISTICS);
    }

    @Override
    public void onClick(@NotNull MenuClick event, @NotNull InventoryState state) {
        ItemStack item = event.currentItem();
        if (item.is(GuiItems.CYAN_STAINED_GLASS_PANE)) {
            if (state.currentPageIndex >= 1) {
                state.currentPageIndex--;
                closeAndOpen(event.player(), this.fill(event.player(), null));
            }
        } else if (item.is(GuiItems.BLUE_STAINED_GLASS_PANE)) {
            state.currentPageIndex++;
            closeAndOpen(event.player(), fill(event.player(), null));
        } else if (item.is(GuiItems.BLACK_STAINED_GLASS_PANE)) {
            state.currentPageIndex = 0;
            closeAndOpen(event.player(), new StatisticsInventory().fill(event.player()));
        }
        // Ignore clicks on statistic items.
    }

    @Override
    public void onClose(@NotNull ServerPlayer player, @NotNull InventoryState state) {
    }

    /**
     * @param stat The statistic to show. If null, uses the cached statistic.
     */
    @NotNull
    public StatisticListInventory fill(@NotNull final ServerPlayer player,
                                       @Nullable final ListStatistic<? extends ListStatisticItem<?, Block>, ?> stat)
        throws RuntimeException {
        if (stat != null) this.statistic = stat;
        if (this.statistic == null) throw new RuntimeException("No cached statistic available.");
        final InventoryState state = InventoryState.get(player.getUUID());
        if (state == null) return this;

        // Verify that all block locations are valid.
        List<? extends ListStatisticItem<?, Block>> list = this.statistic.get().stream()
            .filter((item) -> BlockProt.getDefaultConfig().isLockable(item.getItemType()))
            .collect(Collectors.toList());

        this.inventory.clearContent();
        final int max = this.getSize() - 3;
        int offset = max * state.currentPageIndex;
        for (int i = 0; i < Math.min(list.size() - offset, max); ++i) {
            final ListStatisticItem<?, Block> item = list.get(offset + i);
            setItemStack(
                i,
                BlockUtil.getProperItem(item.getItemType()),
                item.getTitle()
            );
        }

        if (list.size() - offset > max || state.currentPageIndex > 0) {
            setItemStack(
                max,
                GuiItems.CYAN_STAINED_GLASS_PANE,
                TranslationKey.INVENTORIES__LAST_PAGE);
        }
        if (list.size() - offset > max) {
            setItemStack(
                max + 1,
                GuiItems.BLUE_STAINED_GLASS_PANE,
                TranslationKey.INVENTORIES__NEXT_PAGE);
        }
        setBackButton();
        return this;
    }
}
