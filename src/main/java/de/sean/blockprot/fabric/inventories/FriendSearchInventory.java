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
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class FriendSearchInventory {
    private FriendSearchInventory() {
    }

    public static void openAnvilInventory(@NotNull final ServerPlayer requestingPlayer) {
        requestingPlayer.closeContainer();
        AnvilInputMenu.open(
            requestingPlayer,
            Translator.get(TranslationKey.INVENTORIES__FRIENDS__SEARCH),
            "Name",
            FriendSearchInventory::onCompleteCallback);
    }

    private static void onCompleteCallback(@NotNull final ServerPlayer player, @NotNull final String text) {
        final FriendSearchResultInventory inventory = new FriendSearchResultInventory().fill(player, text);
        if (inventory == null) {
            player.closeContainer();
        } else {
            inventory.open(player);
        }
    }
}
