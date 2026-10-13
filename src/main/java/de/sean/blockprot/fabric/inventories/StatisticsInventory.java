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

import de.sean.blockprot.fabric.TranslationKey;
import de.sean.blockprot.fabric.Translator;
import de.sean.blockprot.fabric.nbt.StatHandler;
import de.sean.blockprot.fabric.nbt.stats.BlockCountStatistic;
import de.sean.blockprot.fabric.nbt.stats.ListStatistic;
import de.sean.blockprot.fabric.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.fabric.nbt.stats.Statistic;
import de.sean.blockprot.nbt.stats.StatisticOnClickAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @since 1.0.0
 */
public final class StatisticsInventory extends BlockProtInventory {
    private final List<Statistic<?>> playerStatistics = List.of(new PlayerBlocksStatistic());

    private final List<Statistic<?>> serverStatistics = List.of(new BlockCountStatistic());

    @Override
    int getSize() {
        return InventoryConstants.sextupletLine;
    }

    @Override
    @NotNull String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__STATISTICS__PLAYER_STATISTICS); /* We default to player statistics. */
    }

    @Override
    public void onClick(@NotNull MenuClick event, @NotNull InventoryState state) {
        ItemStack item = event.currentItem();
        if (item.is(GuiItems.BLUE_STAINED_GLASS_PANE)) {
            state.currentPageIndex ^= 1; // Toggles the first bit, switching between 0 and 1.
            this.inventory = this.createInventory(state.currentPageIndex == 0
                ? Translator.get(TranslationKey.INVENTORIES__STATISTICS__PLAYER_STATISTICS)
                : Translator.get(TranslationKey.INVENTORIES__STATISTICS__GLOBAL_STATISTICS));
            this.fill(event.player());
            this.closeAndOpen(event.player(), this);
        } else if (item.is(GuiItems.BLACK_STAINED_GLASS_PANE)) {
            closeAndOpen(event.player(), null);
        } else if (!item.isEmpty()) {
            final List<Statistic<?>> statistics = state.currentPageIndex == 0 ? playerStatistics : serverStatistics;
            if (event.slot() < statistics.size())
                openStatInventory(statistics.get(event.slot()), event.player(), state);
        }
    }

    @Override
    public void onClose(@NotNull ServerPlayer player, @NotNull InventoryState state) {
    }

    @SuppressWarnings("unchecked")
    public void openStatInventory(@NotNull final Statistic<?> stat, @NotNull final ServerPlayer player, @NotNull final InventoryState state) {
        if (stat.getClickAction() == StatisticOnClickAction.NONE) return;
        if (player.containerMenu instanceof GuiMenu menu && menu.getHolder() instanceof StatisticListInventory) return;

        if (stat.getClickAction() == StatisticOnClickAction.LIST_MENU && stat instanceof ListStatistic<?, ?> listStatistic) {
            state.currentPageIndex = 0;
            closeAndOpen(player, new StatisticListInventory().fill(player,
                (ListStatistic<? extends de.sean.blockprot.nbt.stats.ListStatisticItem<?, net.minecraft.world.level.block.Block>, ?>) listStatistic));
        }
    }

    @NotNull
    public StatisticsInventory fill(@NotNull final ServerPlayer player) {
        final InventoryState state = InventoryState.get(player.getUUID());
        if (state == null || state.currentPageIndex >= 2) return this; // We only have 2 pages.

        final List<Statistic<?>> statistics = state.currentPageIndex == 0
            ? playerStatistics
            : serverStatistics;

        for (int i = 0; i < statistics.size() && i < getSize() - 2; ++i) {
            Statistic<?> stat = statistics.get(i);
            StatHandler.getStatistic(stat, player.getUUID());
            setItemStack(
                i,
                stat.getItemType(),
                stat.getTitle()
            );
        }

        setItemStack(
            getSize() - 2,
            GuiItems.BLUE_STAINED_GLASS_PANE,
            state.currentPageIndex == 0
                ? TranslationKey.INVENTORIES__STATISTICS__GLOBAL_STATISTICS
                : TranslationKey.INVENTORIES__STATISTICS__PLAYER_STATISTICS
        );
        setBackButton();
        return this;
    }
}
