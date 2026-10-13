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
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.nbt.FriendHandler;
import de.sean.blockprot.fabric.nbt.RedstoneSettingsHandler;
import de.sean.blockprot.nbt.LockReturnValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

/**
 * The settings inventory for all redstone relevant settings.
 *
 * @since 1.0.0
 */
public class RedstoneSettingsInventory extends BlockProtInventory {
    private boolean currentProtection;
    private boolean hopperProtection;
    private boolean pistonProtection;
    private boolean loaded = false;

    private static final int SETTINGS_COUNT = 3;

    @Override
    int getSize() {
        return InventoryConstants.doubleLine;
    }

    @Override
    @NotNull String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__REDSTONE__SETTINGS);
    }

    @Override
    public void onClick(@NotNull MenuClick event, @NotNull InventoryState state) {
        ItemStack item = event.currentItem();
        ServerPlayer player = event.player();
        if (item.is(Items.REDSTONE)) {
            currentProtection = !currentProtection;
            inventory.setItem(0, toggleOption(item, null));
        } else if (item.is(Items.HOPPER)) {
            hopperProtection = !hopperProtection;
            inventory.setItem(1, toggleOption(item, null));
        } else if (item.is(Items.PISTON)) {
            pistonProtection = !pistonProtection;
            inventory.setItem(2, toggleOption(item, null));
        } else if (item.is(GuiItems.RED_STAINED_GLASS_PANE)) {
            overrideAllSettings(false);
        } else if (item.is(GuiItems.GREEN_STAINED_GLASS_PANE)) {
            overrideAllSettings(true);
        } else {
            BlockNBTHandler handler = getNbtHandlerOrNull(state.getBlock());
            closeAndOpen(
                player,
                handler == null || state.getBlock() == null
                    ? null
                    : new BlockLockInventory().fill(player, state.getBlock().getType(), handler)
            );
        }
    }

    @Override
    public void onClose(@NotNull ServerPlayer player, @NotNull InventoryState state) {
        if (!loaded) return;
        if (state.friendSearchState == InventoryState.FriendSearchState.FRIEND_SEARCH && state.getBlock() != null) {
            applyChanges(
                player,
                (handler) -> {
                    // Apply new redstone values
                    RedstoneSettingsHandler redstoneHandler = handler.getRedstoneHandler();
                    redstoneHandler.setCurrentProtection(currentProtection);
                    redstoneHandler.setHopperProtection(hopperProtection);
                    redstoneHandler.setPistonProtection(pistonProtection);
                    return new LockReturnValue(true, null);
                },
                null
            );
        }
    }

    private void overrideAllSettings(final boolean value) {
        currentProtection = value;
        pistonProtection = value;
        hopperProtection = value;
        for (int i = 0; i < SETTINGS_COUNT; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                inventory.setItem(i, toggleOption(stack, value));
            }
        }
    }

    @NotNull
    public RedstoneSettingsInventory fill(@NotNull ServerPlayer player, @NotNull InventoryState state) {
        if (state.getBlock() == null) return this;
        BlockNBTHandler nbtHandler = getNbtHandlerOrNull(state.getBlock());
        if (nbtHandler == null) return this;

        // Make sure the player owns this, or manages it as a friend.
        final String playerUuid = player.getUUID().toString();
        if (!nbtHandler.isOwner(playerUuid)
            && !nbtHandler.getFriend(playerUuid).map(FriendHandler::isManager).orElse(false)) return this;

        RedstoneSettingsHandler redstoneHandler = nbtHandler.getRedstoneHandler();
        currentProtection = redstoneHandler.getCurrentProtection();
        pistonProtection = redstoneHandler.getPistonProtection();
        hopperProtection = redstoneHandler.getHopperProtection();
        loaded = true;

        setEnchantedOptionItemStack(
            0,
            Items.REDSTONE,
            TranslationKey.INVENTORIES__REDSTONE__REDSTONE_PROTECTION,
            currentProtection
        );
        setEnchantedOptionItemStack(
            1,
            Items.HOPPER,
            TranslationKey.INVENTORIES__REDSTONE__HOPPER_PROTECTION,
            hopperProtection
        );
        setEnchantedOptionItemStack(
            2,
            Items.PISTON,
            TranslationKey.INVENTORIES__REDSTONE__PISTON_PROTECTION,
            pistonProtection
        );
        setItemStack(
            InventoryConstants.doubleLine - 3,
            GuiItems.RED_STAINED_GLASS_PANE,
            TranslationKey.INVENTORIES__REDSTONE__DISABLE_ALL
        );
        setItemStack(
            InventoryConstants.doubleLine - 2,
            GuiItems.GREEN_STAINED_GLASS_PANE,
            TranslationKey.INVENTORIES__REDSTONE__ENABLE_ALL
        );
        setBackButton();
        return this;
    }
}
