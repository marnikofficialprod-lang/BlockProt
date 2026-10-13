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

package de.sean.blockprot.fabric.util;

import de.sean.blockprot.fabric.mixin.CompoundContainerAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various utilities for manipulating and getting block
 * related data.
 *
 * @since 0.2.3
 */
public final class BlockUtil {
    private BlockUtil() {
    }

    /**
     * Get the other half of the double chest of given {@code block}.
     *
     * @return The other half or null, if the chest is a single chest.
     * @since 0.2.3
     */
    @Nullable
    public static BlockRef getDoubleChest(@NotNull final BlockRef block) {
        final BlockState state = block.getState();
        if (!(state.getBlock() instanceof ChestBlock)) return null;
        if (!state.hasProperty(ChestBlock.TYPE) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return null;
        final BlockRef other = new BlockRef(block.level(), block.pos().relative(ChestBlock.getConnectedDirection(state)));
        return other.getType() instanceof ChestBlock ? other : null;
    }

    /**
     * Gets the other half of the door, as per the block given.
     *
     * @return The other half or null if the given block is not a door.
     * @since 0.2.3
     */
    @Nullable
    public static BlockRef getOtherDoorHalf(@NotNull final BlockRef block) {
        final BlockState state = block.getState();
        if (!(state.getBlock() instanceof DoorBlock) || !state.hasProperty(DoorBlock.HALF)) return null;
        return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER
            ? block.relative(0, -1, 0)
            : block.relative(0, 1, 0);
    }

    /**
     * Gets the block that holds the given container, if it is a block container. Double chests
     * return one of their halves.
     */
    @Nullable
    public static BlockRef getBlock(@Nullable final Container container) {
        if (container instanceof BlockEntity blockEntity) {
            if (blockEntity.getLevel() instanceof ServerLevel level) return new BlockRef(level, blockEntity.getBlockPos());
            return null;
        } else if (container instanceof CompoundContainer compound) {
            return getBlock(((CompoundContainerAccessor) compound).blockprot$getContainer1());
        }
        return null;
    }

    public static @NotNull String capitalizeFirstLetters(@NotNull String str) {
        String n = (str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase());
        Matcher m = Pattern.compile("_[a-z]").matcher(n);
        while (m.find()) {
            n = n.substring(0, m.end() - 2) // -2 as we also match the underscore.
                + n.substring(m.start(), m.end()).toUpperCase()
                + n.substring(m.end());
        }
        return n.replaceAll("_", " ");
    }

    /**
     * Gets the Bukkit-like material name of a block, e.g. "OAK_DOOR".
     */
    public static @NotNull String getMaterialName(@NotNull Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath().toUpperCase(Locale.ROOT);
    }

    /**
     * Gets the human-readable name of the given {@code block}. Note that this is not
     * translated and is merely based of the block id.
     *
     * @since 1.1.6
     */
    public static @NotNull String getHumanReadableBlockName(@NotNull Block block) {
        return capitalizeFirstLetters(getMaterialName(block));
    }

    /**
     * Gets the item for a block. For example, wall signs have no item of their own,
     * so the item of the standing sign is used.
     */
    public static @NotNull Item getProperItem(@NotNull Block block) {
        final Item item = block.asItem();
        return item == Items.AIR ? Items.BARRIER : item;
    }
}
