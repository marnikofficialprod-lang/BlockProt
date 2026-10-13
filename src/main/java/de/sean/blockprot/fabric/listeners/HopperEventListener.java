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
import de.sean.blockprot.fabric.util.BlockUtil;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class HopperEventListener {
    private HopperEventListener() {
    }

    /**
     * Whether a hopper (block or minecart) may move items from {@code source} into {@code destination}.
     * One of the two is always a hopper.
     */
    public static boolean isMoveAllowed(@Nullable final Container source, @Nullable final Container destination) {
        if (source == null || destination == null) return true;
        // This is a hopper trying to pull from something or push into something.
        final BlockRef sourceBlock = BlockUtil.getBlock(source);
        if (sourceBlock == null) return true;
        if (BlockProt.getDefaultConfig().isWorldExcluded(sourceBlock.level())) return true;
        if (!BlockProt.getDefaultConfig().isLockable(sourceBlock.getType())) return true;

        final BlockNBTHandler sourceHandler;
        try {
            sourceHandler = new BlockNBTHandler(sourceBlock);
        } catch (RuntimeException e) {
            return true;
        }
        if (!sourceHandler.isProtected()) return true;

        // The source chest is owned by someone. Check if the hopper block is also owned by
        // the same player and if so, allow this event to happen, regardless of the hopper
        // protection.
        if (destination instanceof BlockEntity || destination instanceof CompoundContainer) {
            // The destination is a block of some sorts, chest, hopper, ...
            final BlockRef destinationBlock = BlockUtil.getBlock(destination);
            if (destinationBlock != null && BlockProt.getDefaultConfig().isLockable(destinationBlock.getType())) {
                final BlockNBTHandler destinationHandler;
                try {
                    destinationHandler = new BlockNBTHandler(destinationBlock);
                } catch (RuntimeException e) {
                    return !sourceHandler.getRedstoneHandler().getHopperProtection();
                }
                if (destinationHandler.isProtected()
                        && !destinationHandler.isOwner(sourceHandler.getOwner())
                        && sourceHandler.getRedstoneHandler().getHopperProtection()) {
                    // The hopper and chest are NOT owned by the same person and the chest has
                    // the hopper protection enabled.
                    return false;
                } else if (destinationHandler.isNotProtected()
                        && sourceHandler.getRedstoneHandler().getHopperProtection()) {
                    // The hopper isn't protected, whereas the source block is, and it has the
                    // hopper protection enabled. It's trying to access a locked container.
                    return false;
                }
            } else {
                /* The destination block cannot be locked and is therefore considered public */
                return !sourceHandler.getRedstoneHandler().getHopperProtection();
            }
        } else if (destination instanceof Entity) {
            // As Minecarts are not lockable (yet?), we will disallow the move
            // if hopper protection is enabled.
            return !sourceHandler.getRedstoneHandler().getHopperProtection();
        }
        return true;
    }
}
