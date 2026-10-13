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

package de.sean.blockprot.fabric.listeners;

import de.sean.blockprot.fabric.BlockProt;
import de.sean.blockprot.fabric.ProfileService;
import de.sean.blockprot.fabric.nbt.PlayerSettingsHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import org.jetbrains.annotations.NotNull;

public final class JoinEventListener {
    private JoinEventListener() {
    }

    public static void onJoin(@NotNull final ServerPlayer player) {
        ProfileService.put(player.getUUID(), player.getGameProfile().name());

        // A player that has never left the game before is joining for the first time.
        final boolean hasPlayedBefore = player.getStats().getValue(Stats.CUSTOM.get(Stats.LEAVE_GAME)) > 0;
        if (BlockProt.getDefaultConfig().publicIsFriendByDefault() && !hasPlayedBefore) {
            new PlayerSettingsHandler(player).addEveryoneAsFriend();
        }
    }
}
