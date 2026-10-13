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
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.util.BlockRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The checks of the explosion, entity, piston and redstone listeners of the plugin.
 */
public final class ProtectionListener {
    private ProtectionListener() {
    }

    /**
     * Whether the block at the position is a lockable block that someone protects.
     */
    public static boolean isProtected(@NotNull final ServerLevel level, @NotNull final BlockPos pos) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(level)) return false;
        final BlockRef block = new BlockRef(level, pos);
        if (!BlockProt.getDefaultConfig().isLockable(block.getType())) return false;
        try {
            return new BlockNBTHandler(block).isProtected();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * EntityExplodeEvent/BlockExplodeEvent: protected blocks don't get destroyed by explosions.
     */
    public static List<BlockPos> filterExplodedBlocks(@NotNull final ServerLevel level, @NotNull final List<BlockPos> blocks) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(level)) return blocks;
        // Someone owns this block, block its destroying.
        final List<BlockPos> filtered = new java.util.ArrayList<>(blocks);
        filtered.removeIf(pos -> isProtected(level, pos));
        return filtered;
    }

    /**
     * EntityChangeBlockEvent: entities (withers, the ender dragon, zombies breaking doors) cannot
     * destroy protected blocks.
     */
    public static boolean canEntityChangeBlock(@NotNull final Level level, @NotNull final BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return true;
        return !isProtected(serverLevel, pos);
    }

    /**
     * BlockPistonExtendEvent/BlockPistonRetractEvent: pistons cannot move or destroy protected
     * blocks which have the piston protection enabled. Protected shulker boxes are never moved.
     *
     * @return Whether the piston has to be stopped.
     */
    public static boolean isPistonBlocked(@NotNull final Level level, @NotNull final List<BlockPos> toPush, @NotNull final List<BlockPos> toDestroy) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (BlockProt.getDefaultConfig().isWorldExcluded(level)) return false;
        return isPistonBlocked(serverLevel, toPush) || isPistonBlocked(serverLevel, toDestroy);
    }

    private static boolean isPistonBlocked(@NotNull final ServerLevel level, @NotNull final List<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            final BlockRef block = new BlockRef(level, pos);
            // Check if the block is lockable
            if (BlockProt.getDefaultConfig().isLockableShulkerBox(block.getType())) {
                // Shulker boxes drop right away. Those will therefore not have
                // to be configured to disable redstone, but they will never be
                // pushable if locked.
                try {
                    if (new BlockNBTHandler(block).isProtected()) return true;
                } catch (RuntimeException ignored) {
                }
            } else if (BlockProt.getDefaultConfig().isLockable(block.getType())) {
                try {
                    BlockNBTHandler nbtHandler = new BlockNBTHandler(block);
                    // We check if the block is protected, because we only care
                    // about locked blocks that have redstone disabled.
                    if (nbtHandler.isProtected() && nbtHandler.getRedstoneHandler().getPistonProtection()) return true;
                } catch (RuntimeException ignored) {
                }
            }
        }
        return false;
    }

    /**
     * BlockRedstoneEvent: protected doors, trapdoors and fence gates with the redstone protection
     * enabled don't receive any redstone current.
     */
    public static boolean isRedstoneBlocked(@NotNull final Level level, @NotNull final BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (BlockProt.getDefaultConfig().isWorldExcluded(level)) return false;
        final BlockRef block = new BlockRef(serverLevel, pos);
        if (!BlockProt.getDefaultConfig().isLockableBlock(block.getType())) return false;
        try {
            final BlockNBTHandler handler = new BlockNBTHandler(block);
            return handler.isProtected() && handler.getRedstoneHandler().getCurrentProtection();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
