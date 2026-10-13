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

import de.sean.blockprot.fabric.util.LegacyText;
import de.sean.blockprot.fabric.util.Scheduler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * A text input through the anvil rename field, the replacement for the AnvilGUI library.
 * Clicking the output slot completes the input.
 */
public final class AnvilInputMenu extends AnvilMenu {
    @NotNull
    private String text;
    @NotNull
    private final BiConsumer<ServerPlayer, String> onComplete;

    private AnvilInputMenu(final int containerId, @NotNull final Inventory inventory, @NotNull final String initialText,
                           @NotNull final BiConsumer<ServerPlayer, String> onComplete) {
        super(containerId, inventory, ContainerLevelAccess.NULL);
        this.text = initialText;
        this.onComplete = onComplete;
        final ItemStack paper = new ItemStack(Items.PAPER);
        paper.set(DataComponents.CUSTOM_NAME, Component.literal(initialText));
        this.inputSlots.setItem(0, paper);
        this.createResult();
    }

    /**
     * Opens a new text input for the player.
     *
     * @param title       The title of the anvil, may contain legacy colour codes.
     * @param initialText The text the input field starts with.
     * @param onComplete  Called on the server thread with the text, when the output slot was clicked.
     */
    public static void open(@NotNull final ServerPlayer player, @NotNull final String title, @NotNull final String initialText,
                            @NotNull final BiConsumer<ServerPlayer, String> onComplete) {
        player.openMenu(new SimpleMenuProvider(
            (id, inventory, p) -> new AnvilInputMenu(id, inventory, initialText, onComplete),
            LegacyText.parse(title)));
    }

    @Override
    public void createResult() {
        // The text field might not be initialized yet while the super constructor runs.
        @SuppressWarnings("ConstantValue") final String name = this.text == null ? "" : this.text;
        final ItemStack result = new ItemStack(Items.PAPER);
        result.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        this.resultSlots.setItem(0, result);
    }

    @Override
    public boolean setItemName(@NotNull final String name) {
        this.text = name;
        return super.setItemName(name);
    }

    @Override
    public void clicked(final int slotIndex, final int buttonNum, @NotNull final ContainerInput containerInput, @NotNull final Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (slotIndex != AnvilMenu.RESULT_SLOT) return;
        if (containerInput != ContainerInput.PICKUP && containerInput != ContainerInput.QUICK_MOVE) return;
        final String result = this.text;
        Scheduler.runLater(() -> {
            if (serverPlayer.containerMenu != this) return;
            onComplete.accept(serverPlayer, result);
        });
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull final Player player, final int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull final Player player) {
        return true;
    }

    @Override
    public void removed(@NotNull final Player player) {
        // The paper items are never given to the player.
        this.inputSlots.clearContent();
        this.resultSlots.clearContent();
        super.removed(player);
    }
}
