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

package de.sean.blockprot.fabric;

import de.sean.blockprot.fabric.events.BlockAccessMenuEvent;
import de.sean.blockprot.fabric.inventories.BlockLockInventory;
import de.sean.blockprot.fabric.inventories.BlockProtInventory;
import de.sean.blockprot.fabric.inventories.InventoryState;
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.nbt.FriendHandler;
import de.sean.blockprot.fabric.nbt.PlayerSettingsHandler;
import de.sean.blockprot.fabric.util.BlockRef;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * BlockProt's class for external API methods.
 *
 * @author spnda
 * @since 0.4.7
 */
public final class BlockProtAPI {
    private static final BlockProtAPI instance = new BlockProtAPI();

    private BlockProtAPI() {
    }

    @NotNull
    public static BlockProtAPI getInstance() {
        return instance;
    }

    /**
     * Get the handler for given blocks. This is used to get information
     * about the owner, the friends and the redstone protection.
     *
     * @throws RuntimeException if the block is not lockable.
     * @since 0.4.7
     */
    @NotNull
    public BlockNBTHandler getBlockHandler(@NotNull final BlockRef block) {
        return new BlockNBTHandler(block);
    }

    /**
     * Get the player settings handler for given player. This is used
     * to retrieve default friends or other globally applicable settings
     * for the player.
     *
     * @since 0.4.7
     */
    @NotNull
    public PlayerSettingsHandler getPlayerSettings(@NotNull final ServerPlayer player) {
        return new PlayerSettingsHandler(player);
    }

    /**
     * Get the lock inventory for given block and player. This call
     * triggers the {@link BlockAccessMenuEvent} event and checks
     * if it succeeded and what permissions the player has and bases the
     * inventory on that information.
     *
     * @return The inventory or null, if the request was denied, possibly
     * due to permissions.
     * @since 0.4.7
     */
    @Nullable
    public BlockProtInventory getLockInventoryForBlock(@NotNull final BlockRef block, @NotNull final ServerPlayer player) {
        final BlockAccessMenuEvent event = new BlockAccessMenuEvent(block, player);
        final String playerUuid = player.getUUID().toString();

        final BlockNBTHandler handler = new BlockNBTHandler(block);
        if (Permissions.isOp(player) || Permissions.ADMIN.check(player)) {
            event.addPermissions(
                    BlockAccessMenuEvent.MenuPermission.LOCK,
                    BlockAccessMenuEvent.MenuPermission.INFO);
        } else if (Permissions.INFO.check(player)) {
            event.addPermission(BlockAccessMenuEvent.MenuPermission.INFO);
        }

        Optional<FriendHandler> friend;
        if (handler.isOwner(playerUuid)) {
            event.addPermissions(
                    BlockAccessMenuEvent.MenuPermission.LOCK,
                    BlockAccessMenuEvent.MenuPermission.INFO,
                    BlockAccessMenuEvent.MenuPermission.MANAGER);
        } else if (handler.isNotProtected()) {
            event.addPermission(BlockAccessMenuEvent.MenuPermission.LOCK);
        } else if ((friend = handler.getFriend(playerUuid)).isPresent() && friend.get().isManager()) {
            event.addPermission(BlockAccessMenuEvent.MenuPermission.MANAGER);
        }

        // Call the event and let the listeners remove/add more permissions.
        event.call();

        if (event.isCancelled() || event.getPermissions().isEmpty()) {
            return null;
        }

        InventoryState state = new InventoryState(block);
        state.menuPermissions = event.getPermissions();
        state.friendSearchState = InventoryState.FriendSearchState.FRIEND_SEARCH;
        InventoryState.set(player.getUUID(), state);

        return new BlockLockInventory().fill(player, block.getType(), handler);
    }
}
