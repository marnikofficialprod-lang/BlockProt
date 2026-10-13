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
import de.sean.blockprot.fabric.nbt.PlayerSettingsHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class UserSettingsInventory extends BlockProtInventory {
    @Override
    int getSize() {
        return InventoryConstants.singleLine;
    }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS);
    }

    @Override
    public void onClick(@NotNull MenuClick event, @NotNull InventoryState state) {
        ServerPlayer player = event.player();
        ItemStack item = event.currentItem();
        if (item.is(Items.BARRIER)) {
            // Lock on place button, default value is true
            PlayerSettingsHandler settingsHandler = new PlayerSettingsHandler(player);
            settingsHandler.setLockOnPlace(!settingsHandler.getLockOnPlace());
            inventory.setItem(0, toggleOption(item, null));
        } else if (item.is(Items.PLAYER_HEAD)) {
            state.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            closeAndOpen(player, new FriendManageInventory().fill(player));
        } else {
            closeAndOpen(player, null); // This also includes the black stained glass pane
        }
    }

    @Override
    public void onClose(@NotNull ServerPlayer player, @NotNull InventoryState state) {
    }

    @NotNull
    public UserSettingsInventory fill(@NotNull ServerPlayer player) {
        PlayerSettingsHandler settingsHandler = new PlayerSettingsHandler(player);
        boolean lockOnPlace = settingsHandler.getLockOnPlace();
        setEnchantedOptionItemStack(
            0,
            Items.BARRIER,
            TranslationKey.INVENTORIES__LOCK_ON_PLACE,
            lockOnPlace
        );
        if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
            setItemStack(
                1,
                Items.PLAYER_HEAD,
                TranslationKey.INVENTORIES__FRIENDS__MANAGE
            );
        }
        setBackButton();
        return this;
    }
}
