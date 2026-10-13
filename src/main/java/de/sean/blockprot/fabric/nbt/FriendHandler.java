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

package de.sean.blockprot.fabric.nbt;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The friend handler used by {@link BlockNBTHandler} to handle
 * each of the {@link CompoundTag} used in the "friends" sub-tag of each block.
 * A single {@link FriendHandler} itself only handles a *single friend*
 * in the list of friends.
 *
 * @since 0.3.0
 */
public final class FriendHandler extends NBTHandler {
    static final String ACCESS_FLAGS_ATTRIBUTE = "blockprot_access_flags";

    @NotNull
    private final String name;

    @NotNull
    private final Runnable onChange;

    public FriendHandler(@NotNull final String name, @NotNull final CompoundTag compound, @NotNull final Runnable onChange) {
        super();
        this.name = name;
        this.container = compound;
        this.onChange = onChange;
    }

    /**
     * {@inheritDoc}
     *
     * @return The UUID of this friend.
     */
    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    protected void markDirty() {
        onChange.run();
    }

    /**
     * A single friend handler can represent the whole player-base, giving access
     * to anyone on the server with specific access flags. We represent everyone by
     * using an invalid UUID.
     *
     * @since 1.0.0
     */
    public boolean doesRepresentPublic() {
        return getName().equals(FriendSupportingHandler.publicUuid.toString());
    }

    private int getAccessFlagsBitset() {
        return container.getIntOr(ACCESS_FLAGS_ATTRIBUTE, BlockAccessFlag.READ.getFlag() | BlockAccessFlag.WRITE.getFlag());
    }

    private void setAccessFlagsBitset(final int flagsBitset) {
        container.putInt(ACCESS_FLAGS_ATTRIBUTE, flagsBitset);
        markDirty();
    }

    /**
     * Read the access flags of this block.
     *
     * @since 0.3.0
     */
    @NotNull
    public EnumSet<BlockAccessFlag> getAccessFlags() {
        if (!container.contains(ACCESS_FLAGS_ATTRIBUTE)) return EnumSet.of(BlockAccessFlag.READ, BlockAccessFlag.WRITE);
        else return BlockAccessFlag.parseFlags(container.getIntOr(ACCESS_FLAGS_ATTRIBUTE, 0));
    }

    /**
     * Sets the access flags for this block. ORs all flags together to one integer, then
     * writes all of them to ACCESS_FLAGS_ATTRIBUTE.
     *
     * @since 0.3.0
     */
    public void setAccessFlags(@NotNull final EnumSet<BlockAccessFlag> flags) {
        setAccessFlagsBitset(flags.stream().mapToInt(BlockAccessFlag::getFlag).sum());
    }

    public boolean canRead() {
        return getAccessFlags().contains(BlockAccessFlag.READ);
    }

    public boolean canWrite() {
        return getAccessFlags().contains(BlockAccessFlag.WRITE);
    }

    /**
     * A manager is allowed to edit redstone settings and remove/add
     * and edit friends of a block they have this permission on.
     *
     * @since 1.1.0
     */
    public boolean isManager() {
        return getAccessFlags().contains(BlockAccessFlag.MANAGER);
    }

    /**
     * Only merges the access flags.
     */
    @Override
    public void mergeHandler(@NotNull NBTHandler handler) {
        if (handler instanceof FriendHandler friendHandler) {
            this.setAccessFlagsBitset(friendHandler.getAccessFlagsBitset());
        }
    }
}
