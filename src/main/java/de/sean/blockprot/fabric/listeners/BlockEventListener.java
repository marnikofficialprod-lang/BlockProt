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
import de.sean.blockprot.fabric.Permissions;
import de.sean.blockprot.fabric.Translator;
import de.sean.blockprot.fabric.events.BlockLockOnPlaceEvent;
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.nbt.PlayerSettingsHandler;
import de.sean.blockprot.fabric.nbt.StatHandler;
import de.sean.blockprot.fabric.util.BlockRef;
import de.sean.blockprot.fabric.util.BlockUtil;
import de.sean.blockprot.fabric.util.LegacyText;
import de.sean.blockprot.fabric.util.Scheduler;
import de.sean.blockprot.nbt.LockReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class BlockEventListener {
    /**
     * The key inside the custom data of shulker box items, holding the protection of the box.
     */
    public static final String SHULKER_DATA_KEY = "blockprot:shulker_data";

    private BlockEventListener() {
    }

    /**
     * Whether a protected block should not burn.
     */
    public static boolean isBurnProtected(@NotNull final Level level, @NotNull final BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (BlockProt.getDefaultConfig().isWorldExcluded(level)) return false;
        final BlockRef block = new BlockRef(serverLevel, pos);
        if (!BlockProt.getDefaultConfig().isLockable(block.getType())) return false;
        try {
            // If the block is protected by any user, prevent it from burning down.
            return new BlockNBTHandler(block).isProtected();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * PlayerBlockBreakEvents.BEFORE: Prevent unauthorized players from breaking locked blocks.
     *
     * @return false to cancel the block break.
     */
    public static boolean onBlockBreak(@NotNull final Level level, @NotNull final Player p, @NotNull final BlockPos pos,
                                       @NotNull final BlockState state, @Nullable final BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || !(p instanceof ServerPlayer player)) return true;
        if (BlockProt.getDefaultConfig().isWorldExcluded(level)) return true;
        if (!BlockProt.getDefaultConfig().isLockable(state.getBlock())) return true; // We only want to check for lockable blocks.

        final BlockRef block = new BlockRef(serverLevel, pos);
        final BlockNBTHandler handler;
        try {
            handler = new BlockNBTHandler(block);
        } catch (RuntimeException e) {
            return true;
        }
        if (!handler.isOwner(player.getUUID()) && handler.isProtected()) {
            // Prevent unauthorized players from breaking locked blocks.
            return false;
        }

        if (handler.isProtected()) {
            try {
                StatHandler.removeContainer(UUID.fromString(handler.getOwner()), block);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Owners keep the protection of their shulker boxes, which gets applied again when placing it.
        if (BlockProt.getDefaultConfig().isLockableShulkerBox(state.getBlock())
            && handler.isOwner(player.getUUID())
            && !player.isCreative()
            && serverLevel.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.BLOCK_DROPS)) {
            final List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, blockEntity, player, player.getMainHandItem());
            if (!drops.isEmpty()) {
                // Shulker blocks should only have a single drop anyway
                final ItemStack item = drops.getFirst();
                final CompoundTag customData = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                customData.put(SHULKER_DATA_KEY, handler.getNbtCopy());
                item.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
                Block.popResource(serverLevel, pos, item);
                for (int i = 1; i < drops.size(); i++) Block.popResource(serverLevel, pos, drops.get(i));
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                return false; // So that the box doesn't drop twice.
            }
        }

        // For blocks, we want to clear the NBT data, as that lives
        // independently of the actual block state.
        if (handler.isProtected()) handler.clear();
        return true;
    }

    /**
     * Called before a block item gets placed. Cancels the placement if the player could not
     * lock the block, because they exceeded their block limit.
     *
     * @return false to cancel the placement.
     */
    public static boolean canPlace(@NotNull final ServerPlayer player, @NotNull final Level level, @NotNull final Block block, @NotNull final ItemStack stack) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(level)) return true;
        if (!BlockProt.getDefaultConfig().isLockable(block)) return true;
        if (!Permissions.LOCK.check(player)) return true;
        if (getShulkerData(stack) != null) return true; // Already locked.
        if (!new PlayerSettingsHandler(player).getLockOnPlace()) return true;
        if (BlockNBTHandler.exceedsMaxBlockCount(player)) {
            player.sendOverlayMessage(LegacyText.parse(Translator.get(LockReturnValue.Reason.EXCEEDED_MAX_BLOCK_COUNT)));
            return false;
        }
        return true;
    }

    @Nullable
    private static CompoundTag getShulkerData(@NotNull final ItemStack stack) {
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        return customData.copyTag().get(SHULKER_DATA_KEY) instanceof CompoundTag tag ? tag : null;
    }

    /**
     * Called after a player placed a block.
     */
    public static void onBlockPlace(@NotNull final ServerPlayer player, @NotNull final Level level, @NotNull final BlockPos pos, @NotNull final ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (BlockProt.getDefaultConfig().isWorldExcluded(level)) return;

        final BlockRef block = new BlockRef(serverLevel, pos);
        if (!BlockProt.getDefaultConfig().isLockable(block.getType())) return;

        final BlockNBTHandler handler;
        try {
            handler = new BlockNBTHandler(block);
        } catch (RuntimeException e) {
            return;
        }

        // Shulker boxes might already be locked, from previous placing.
        final CompoundTag shulkerData = getShulkerData(stack);
        if (shulkerData != null && BlockProt.getDefaultConfig().isLockableShulkerBox(block.getType())) {
            handler.pasteNbt(shulkerData.copy());
            final String owner = shulkerData.getStringOr("splugin_owner", "");
            if (!owner.isEmpty()) {
                handler.setOwner(owner);
                try {
                    StatHandler.addBlock(UUID.fromString(owner), block);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (!Permissions.LOCK.check(player)) return;
        final String playerUuid = player.getUUID().toString();

        // We only try to lock the block if it isn't locked already.
        if (handler.isNotProtected()) {
            PlayerSettingsHandler settingsHandler = new PlayerSettingsHandler(player);
            // Lock the block instantly if the setting is enabled.
            if (settingsHandler.getLockOnPlace()) {
                final BlockLockOnPlaceEvent lockOnPlaceEvent = new BlockLockOnPlaceEvent(block, player).call();
                if (!lockOnPlaceEvent.isCancelled()) {
                    LockReturnValue lock = handler.lockBlock(player);
                    if (!lock.success) {
                        // The limit is normally checked before placing, see canPlace.
                        if (lock.reason != null) {
                            player.sendOverlayMessage(LegacyText.parse(Translator.get(lock.reason)));
                        }
                        return;
                    }

                    settingsHandler.getFriendsStream()
                        .forEach(handler::addFriend);
                }

                if (BlockProt.getDefaultConfig().disallowRedstoneOnPlace()) {
                    handler.getRedstoneHandler().setAll(false);
                }
            }

            Scheduler.runLater(() -> {
                if (!BlockProt.getDefaultConfig().isLockable(block.getType())) return;
                if (block.getType() instanceof ChestBlock) {
                    // We cannot use BlockNBTHandler#applyToOtherContainer, because we want the
                    // data to be copied to this new chest, instead of the old chest being effectively
                    // cleared.
                    final BlockRef doubleChest = BlockUtil.getDoubleChest(block);
                    if (doubleChest != null) {
                        final BlockNBTHandler newHandler;
                        final BlockNBTHandler doubleChestHandler;
                        try {
                            newHandler = new BlockNBTHandler(block);
                            doubleChestHandler = new BlockNBTHandler(doubleChest);
                        } catch (RuntimeException e) {
                            return;
                        }
                        // Remove the container from the stats, as the double chest is only counted once.
                        if (newHandler.isOwner(playerUuid))
                            StatHandler.removeContainer(player.getUUID(), block);
                        if (doubleChestHandler.isNotProtected() || doubleChestHandler.isOwner(playerUuid)) {
                            newHandler.mergeHandler(doubleChestHandler);
                        } else {
                            // We can't cancel the placement anymore. We'll just need to destroy the block and drop it.
                            newHandler.clear();
                            serverLevel.destroyBlock(block.pos(), true, player);
                        }
                    }
                } else {
                    try {
                        final BlockNBTHandler newHandler = new BlockNBTHandler(block);
                        newHandler.setName(BlockUtil.getHumanReadableBlockName(block.getType()));
                        newHandler.applyToOtherContainer();
                    } catch (RuntimeException ignored) {
                    }
                }
            });
        }
    }

    /**
     * BlockPhysicsEvent for anvils: protected lockable blocks never fall.
     */
    public static boolean isFallProtected(@NotNull final ServerLevel level, @NotNull final BlockPos pos, @NotNull final BlockState state) {
        if (!BlockProt.getDefaultConfig().isLockableBlock(state.getBlock())) return false;
        if (BlockProt.getDefaultConfig().isWorldExcluded(level)) return false;
        try {
            return new BlockNBTHandler(new BlockRef(level, pos)).isProtected();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * SignChangeEvent: only the owner may edit protected signs.
     */
    public static boolean canEditSign(@NotNull final Player p, @NotNull final Level level, @NotNull final BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return true;
        final BlockRef block = new BlockRef(serverLevel, pos);
        if (!BlockProt.getDefaultConfig().isLockable(block.getType())) return true;
        try {
            final var handler = new BlockNBTHandler(block);
            return !(handler.isProtected() && !handler.isOwner(p.getUUID()));
        } catch (RuntimeException e) {
            return true;
        }
    }
}
