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
import de.sean.blockprot.fabric.BlockProtAPI;
import de.sean.blockprot.fabric.Permissions;
import de.sean.blockprot.fabric.TranslationKey;
import de.sean.blockprot.fabric.Translator;
import de.sean.blockprot.fabric.events.BlockAccessEvent;
import de.sean.blockprot.fabric.inventories.BlockProtInventory;
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.nbt.FriendHandler;
import de.sean.blockprot.fabric.nbt.PlayerSettingsHandler;
import de.sean.blockprot.fabric.util.BlockRef;
import de.sean.blockprot.fabric.util.BlockUtil;
import de.sean.blockprot.fabric.util.LegacyText;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public final class InteractEventListener {
    private InteractEventListener() {
    }

    /**
     * UseBlockCallback: checks the access to protected blocks and opens the lock menu when
     * crouching and right-clicking with an empty hand.
     */
    public static InteractionResult onUseBlock(@NotNull final Player p, @NotNull final Level level,
                                               @NotNull final InteractionHand hand, @NotNull final BlockHitResult hitResult) {
        if (!(p instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (player.isSpectator()) return InteractionResult.PASS;
        if (BlockProt.getDefaultConfig().isWorldExcluded(level)) return InteractionResult.PASS;

        final BlockRef block = new BlockRef(serverLevel, hitResult.getBlockPos());
        if (!BlockProt.getDefaultConfig().isLockable(block.getType())) return InteractionResult.PASS;

        if (!player.isShiftKeyDown()) {
            final BlockAccessEvent accessEvent = new BlockAccessEvent(block, player).call();
            if (accessEvent.isCancelled()) {
                return deny(player, block);
            }

            final BlockNBTHandler handler;
            try {
                handler = new BlockNBTHandler(block);
            } catch (RuntimeException e) {
                return InteractionResult.PASS;
            }
            final String playerUuid = player.getUUID().toString();
            final boolean bypass = accessEvent.shouldBypassProtections() || Permissions.BYPASS.check(player);
            if (!(handler.canAccess(playerUuid) || bypass)) {
                return deny(player, block);
            }

            if (!bypass && handler.isProtected() && !handler.isOwner(playerUuid)) {
                final Optional<FriendHandler> friend = handler.getFriend(playerUuid);
                final boolean canWrite = friend.isPresent() && friend.get().canWrite();
                if (block.getType() instanceof LecternBlock && !block.getState().getValue(LecternBlock.HAS_BOOK) && !canWrite) {
                    // With Lecterns you place the books by interacting with the block. canAccess will return true because the
                    // player has the READ permission, but this should not be allowed in this case.
                    return deny(player, block);
                }
                // Friends without the write permission may look into, but not alter the inventory.
                InventoryEventListener.setReadOnlyAccess(player, !canWrite);
            } else {
                InventoryEventListener.setReadOnlyAccess(player, false);
            }

            if (!(new PlayerSettingsHandler(player).hasPlayerInteractedWithMenu())) {
                sendLockHint(player);
            }
            return InteractionResult.PASS;
        } else {
            // Only enter the menu with an empty hand.
            if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;

            if (!Permissions.LOCK.check(player)) {
                return deny(player, block);
            }

            final BlockProtInventory inv;
            try {
                inv = BlockProtAPI.getInstance().getLockInventoryForBlock(block, player);
            } catch (RuntimeException e) {
                return InteractionResult.PASS;
            }
            if (inv == null) {
                return deny(player, block);
            }
            new PlayerSettingsHandler(player).setHasPlayerInteractedWithMenu(true);
            inv.open(player);
            return InteractionResult.SUCCESS;
        }
    }

    /**
     * Cancels the interaction and makes sure the client shows the actual state again.
     */
    private static InteractionResult deny(@NotNull final ServerPlayer player, @NotNull final BlockRef block) {
        sendMessage(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
        final BlockRef otherDoor = BlockUtil.getOtherDoorHalf(block);
        if (otherDoor != null) {
            player.connection.send(new ClientboundBlockUpdatePacket(otherDoor.level(), otherDoor.pos()));
        }
        player.containerMenu.sendAllDataToRemote();
        return InteractionResult.FAIL;
    }

    private static void sendLockHint(@NotNull final ServerPlayer player) {
        Long timestamp = LockHintMessageCooldown.getTimestamp(player.getUUID());
        if (timestamp == null || timestamp < System.currentTimeMillis() - (BlockProt.getDefaultConfig().getLockHintCooldown() * 1000)) {
            // If they can access the block we'll notify them that they could
            // potentially lock their blocks.
            String message = Translator.get(TranslationKey.MESSAGES__LOCK_HINT);
            if (!message.isEmpty()) {
                LockHintMessageCooldown.setTimestamp(player.getUUID());
                var tooltip = Translator.get(TranslationKey.MESSAGES__HINT_HOVER_TEXT);
                final MutableComponent component = LegacyText.parse(message.strip());
                component.withStyle(style -> {
                    style = style.withClickEvent(new ClickEvent.RunCommand("/blockprot disablehints"));
                    if (!tooltip.isEmpty())
                        style = style.withHoverEvent(new HoverEvent.ShowText(LegacyText.parse(tooltip)));
                    return style;
                });
                player.sendSystemMessage(component);
            }
        }
    }

    static void sendMessage(@NotNull final ServerPlayer player, @NotNull final String message) {
        player.sendOverlayMessage(LegacyText.parse(message));
    }

    private static final class LockHintMessageCooldown {
        private static final HashMap<UUID, Long> timestamps = new HashMap<>();

        public static void setTimestamp(final @NotNull UUID player) {
            timestamps.put(player, System.currentTimeMillis());
        }

        public static Long getTimestamp(final @NotNull UUID player) {
            return timestamps.get(player);
        }
    }
}
