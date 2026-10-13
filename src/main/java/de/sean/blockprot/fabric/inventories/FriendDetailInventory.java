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
import de.sean.blockprot.fabric.ProfileService;
import de.sean.blockprot.fabric.TranslationKey;
import de.sean.blockprot.fabric.Translator;
import de.sean.blockprot.fabric.nbt.BlockAccessFlag;
import de.sean.blockprot.fabric.nbt.FriendHandler;
import de.sean.blockprot.fabric.nbt.FriendSupportingHandler;
import de.sean.blockprot.nbt.FriendModifyAction;
import de.sean.blockprot.nbt.LockReturnValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The detail inventory for managing a single friend and their permission.
 *
 * @since 0.3.0
 */
public final class FriendDetailInventory extends BlockProtInventory {
    @NotNull
    private static final List<EnumSet<BlockAccessFlag>> accessFlagCombinations =
        Arrays.asList(
            EnumSet.of(BlockAccessFlag.READ),
            EnumSet.of(BlockAccessFlag.READ, BlockAccessFlag.WRITE),
            EnumSet.of(BlockAccessFlag.READ, BlockAccessFlag.WRITE, BlockAccessFlag.MANAGER));

    @Nullable
    private EnumSet<BlockAccessFlag> curFlags = EnumSet.noneOf(BlockAccessFlag.class);

    @Nullable
    private String friendId = null;

    @Override
    public int getSize() {
        return InventoryConstants.singleLine;
    }

    @NotNull
    @Override
    public String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__FRIENDS__EDIT);
    }

    @Override
    public void onClick(@NotNull MenuClick event, @NotNull InventoryState state) {
        final ServerPlayer player = event.player();
        final ItemStack item = event.currentItem();
        if (item.is(GuiItems.BLACK_STAINED_GLASS_PANE)) {
            closeAndOpen(player, new FriendManageInventory().fill(player));
        } else if (item.is(GuiItems.RED_STAINED_GLASS_PANE)) {
            final var friend = state.currentFriend;
            if (friend == null) return;
            modifyFriendsForAction(player, friend, FriendModifyAction.REMOVE_FRIEND);
            // We remove the friend, so the player does not exist anymore either.
            this.friendId = null;
            closeAndOpen(player, new FriendManageInventory().fill(player));
        } else if (item.is(Items.ENDER_EYE)) {
            if (friendId == null || curFlags == null) return;
            int curIndex = 0;
            for (; curIndex < accessFlagCombinations.size(); curIndex++) {
                if (curFlags.equals(accessFlagCombinations.get(curIndex))) break;
            }
            if (curIndex + 1 >= accessFlagCombinations.size()) curIndex = 0;
            else curIndex += 1;
            curFlags = accessFlagCombinations.get(curIndex);
            setItemStack(
                2,
                Items.ENDER_EYE,
                BlockAccessFlag.toBaseString(),
                BlockAccessFlag.accumulateAccessFlagLore(curFlags)
            );
        } else if (!item.is(Items.PLAYER_HEAD)) {
            closeAndOpen(player, null);
        }
    }

    @Override
    public void onClose(@NotNull ServerPlayer player, @NotNull InventoryState state) {
        if (this.friendId == null || curFlags == null) return;
        final EnumSet<BlockAccessFlag> flags = curFlags;
        final String id = friendId;
        // Write the flags onto the current data and also onto the other half of doors or double chests.
        applyChanges(
            player,
            handler -> {
                handler.getFriend(id).filter(f -> f.getName().equals(id)).ifPresent(f -> f.setAccessFlags(flags));
                return new LockReturnValue(true, null);
            },
            settings -> settings.getFriend(id).filter(f -> f.getName().equals(id)).ifPresent(f -> f.setAccessFlags(flags))
        );
    }

    @Nullable
    public FriendDetailInventory fill(@NotNull ServerPlayer player) {
        final InventoryState state = InventoryState.get(player.getUUID());
        if (state == null) return this;

        final var uuid = state.currentFriend;
        if (uuid == null) return this;

        if (!uuid.equals(FriendSupportingHandler.publicUuid)) {
            final String name = ProfileService.getIfPresent(uuid);
            setPlayerSkull(0, uuid, name);
            if (name == null) fillPlayerSkullsAsync(Map.of(0, uuid));
        } else {
            setItemStack(0, Items.PLAYER_HEAD,
                    TranslationKey.INVENTORIES__FRIENDS__THE_PUBLIC,
                    List.of(Translator.get(TranslationKey.INVENTORIES__FRIENDS__THE_PUBLIC_DESC)));
        }

        setItemStack(
            1, GuiItems.RED_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__FRIENDS__REMOVE);

        final @Nullable FriendSupportingHandler handler =
            getFriendSupportingHandler(state.friendSearchState, player, state.getBlock());
        if (handler == null) return null;
        final Optional<FriendHandler> friendHandler =
            handler.getFriend(uuid.toString());
        if (friendHandler.isEmpty()) {
            BlockProt.LOGGER.warn(
                    "Tried to open a " + this.getClass().getSimpleName() + " with a unknown player.");
            return null;
        }

        final FriendHandler playerHandler = friendHandler.get();
        friendId = playerHandler.getName();
        /* Read the current access flags */
        curFlags = playerHandler.getAccessFlags();

        setItemStack(
            2,
            Items.ENDER_EYE,
            BlockAccessFlag.toBaseString(),
            BlockAccessFlag.accumulateAccessFlagLore(curFlags)
        );
        setBackButton();
        return this;
    }
}
