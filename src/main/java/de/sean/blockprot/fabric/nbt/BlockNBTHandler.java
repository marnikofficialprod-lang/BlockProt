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

import de.sean.blockprot.fabric.BlockProt;
import de.sean.blockprot.fabric.Permissions;
import de.sean.blockprot.fabric.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.fabric.util.BlockRef;
import de.sean.blockprot.fabric.util.BlockUtil;
import de.sean.blockprot.nbt.FriendModifyAction;
import de.sean.blockprot.nbt.LockReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A block handler to get values and settings from a single lockable
 * block.
 *
 * @since 0.2.3
 */
public final class BlockNBTHandler extends FriendSupportingHandler {
    static final String OWNER_ATTRIBUTE = "splugin_owner";
    static final String LOCK_ATTRIBUTE = "blockprot_friends";
    static final String REDSTONE_ATTRIBUTE = "blockprot_redstone";
    static final String NAME_ATTRIBUTE = "blockprot_name";

    /**
     * The backing block this handler handles.
     *
     * @since 0.2.3
     */
    @NotNull
    public final BlockRef block;

    @NotNull
    private final Storage storage;

    /**
     * Create a new handler for given {@code block}.
     *
     * @throws RuntimeException if {@code block} is not a lockable block
     *                          or lockable tile entity.
     * @since 0.2.3
     */
    public BlockNBTHandler(@NotNull final BlockRef block) throws RuntimeException {
        super(LOCK_ATTRIBUTE);
        this.block = block;

        if (BlockProt.getDefaultConfig().isLockableBlock(this.block.getType())) {
            storage = new ChunkStorage(block);
        } else if (BlockProt.getDefaultConfig().isLockableTileEntity(this.block.getType())) {
            final BlockEntity blockEntity = block.getBlockEntity();
            if (blockEntity == null)
                throw new RuntimeException("Given block " + block.getType() + " has no block entity");
            storage = new BlockEntityStorage(blockEntity);
        } else {
            throw new RuntimeException("Given block " + block.getType() + " is not a lockable block/tile entity");
        }
        container = storage.read();
    }

    /**
     * Saves the container. The container only gets attached to the block or chunk once
     * something has been written, so that reading never creates any data.
     */
    @Override
    protected void markDirty() {
        container = storage.commit(container);
    }

    /**
     * Reads the current owner from the NBT container.
     *
     * @return The UUID of the owner, or an empty String.
     * @since 0.2.3
     */
    @NotNull
    public String getOwner() {
        return container.getStringOr(OWNER_ATTRIBUTE, "");
    }

    /**
     * Set the current owner of this block.
     *
     * @since 0.2.3
     */
    public void setOwner(@NotNull final String owner) {
        container.putString(OWNER_ATTRIBUTE, owner);
        markDirty();
    }

    /**
     * Gets the redstone settings handler for this block.
     *
     * @since 0.4.0
     */
    public @NotNull RedstoneSettingsHandler getRedstoneHandler() {
        return new RedstoneSettingsHandler(
            getOrCreateCompound(container, REDSTONE_ATTRIBUTE), this::markDirty);
    }

    /**
     * Whether or not this block is protected. This is evaluated by checking
     * if an owner exists and if any friends have been added to the block.
     *
     * @since 0.2.3
     */
    public boolean isNotProtected() {
        return getOwner().isEmpty() && getFriends().isEmpty();
    }

    public boolean isProtected() {
        return !isNotProtected();
    }

    public boolean isOwner(@NotNull final String player) {
        return getOwner().equals(player);
    }

    public boolean isOwner(@NotNull final UUID player) {
        return getOwner().equals(player.toString());
    }

    public @NotNull String getName() {
        return container.getStringOr(NAME_ATTRIBUTE, BlockUtil.getMaterialName(block.getType()));
    }

    public void setName(@NotNull String name) {
        container.putString(NAME_ATTRIBUTE, name);
        markDirty();
    }

    /**
     * Checks whether given {@code player} can access this block.
     *
     * @since 0.2.3
     */
    public boolean canAccess(@NotNull final String player) {
        Optional<FriendHandler> friend = getFriend(player);
        return isNotProtected() || getOwner().equals(player) || (friend.isPresent() && friend.get().canRead());
    }

    public boolean canAccess(@NotNull final FriendHandler friend) {
        return isNotProtected() || friend.canRead();
    }

    /**
     * Locks this block for given {@code player} as the owner.
     *
     * @return A {@link LockReturnValue} whether or not the block was successfully locked,
     * else there might have been issues with permissions.
     * @since 0.2.3
     */
    @NotNull
    public LockReturnValue lockBlock(@NotNull final ServerPlayer player) {
        String owner = getOwner();
        final String playerUuid = player.getUUID().toString();
        if (owner.isEmpty()) {
            // Check if the player is not exceeding their max block count.
            if (exceedsMaxBlockCount(player)) {
                return new LockReturnValue(false, LockReturnValue.Reason.EXCEEDED_MAX_BLOCK_COUNT);
            }

            // This block is not owned by anyone, this user can claim this block
            owner = playerUuid;
            setOwner(owner);
            this.applyToOtherContainer();
            StatHandler.addBlock(player.getUUID(), block);
            return new LockReturnValue(true, null);
        } else if (owner.equals(playerUuid) || Permissions.isOp(player) || Permissions.ADMIN.check(player)) {
            try {
                StatHandler.removeContainer(UUID.fromString(owner), block);
            } catch (IllegalArgumentException ignored) {
            }
            this.clear();
            this.applyToOtherContainer();
            return new LockReturnValue(true, null);
        }
        return new LockReturnValue(false, LockReturnValue.Reason.NO_PERMISSION);
    }

    /**
     * Whether the player has already locked as many blocks as they're allowed to.
     */
    public static boolean exceedsMaxBlockCount(@NotNull final ServerPlayer player) {
        Integer maxBlockCount = BlockProt.getDefaultConfig().getMaxLockedBlockCount();
        if (maxBlockCount == null) return false;

        PlayerBlocksStatistic playerBlocksStatistic = new PlayerBlocksStatistic();
        StatHandler.getStatistic(playerBlocksStatistic, player.getUUID());
        if (Permissions.has(player, Permissions.LOCK_MAX)) {
            final Integer limit = Permissions.getLockLimit(player);
            return limit != null && playerBlocksStatistic.get().size() >= limit;
        }
        return playerBlocksStatistic.get().size() >= maxBlockCount;
    }

    /**
     * Modifies the friends of this block for given {@code action}.
     *
     * @since 0.4.6
     */
    @NotNull
    public LockReturnValue modifyFriends(@NotNull final String player, @NotNull final String friend, @NotNull final FriendModifyAction action) {
        // This theoretically shouldn't happen, though we will still check for it just to be sure
        if (!isOwner(player)) return new LockReturnValue(
            false, null
        );

        switch (action) {
            case ADD_FRIEND -> {
                if (containsFriend(friend)) {
                    return new LockReturnValue(false, null);
                } else {
                    addFriend(friend);
                    this.applyToOtherContainer();
                    return new LockReturnValue(true, null);
                }
            }
            case REMOVE_FRIEND -> {
                if (containsFriend(friend)) {
                    removeFriend(friend);
                    this.applyToOtherContainer();
                    return new LockReturnValue(true, null);
                } else {
                    return new LockReturnValue(false, null);
                }
            }
            default -> {
                return new LockReturnValue(false, null);
            }
        }
    }

    /**
     * @see #applyToOtherContainer(Predicate, Consumer)
     * @since 0.4.6
     */
    public void applyToOtherContainer() {
        this.applyToOtherContainer(handler -> true, handler -> {
        });
    }

    /**
     * This applies any changes to this container to a possible other
     * half. For example doors consist from two blocks, as do double
     * chests. Without this call, all methods will modify only the local,
     * current block.
     *
     * @since 0.4.6
     */
    public void applyToOtherContainer(@NotNull Predicate<BlockNBTHandler> condition, @NotNull Consumer<BlockNBTHandler> orElse) {
        BlockRef other = null;
        if (BlockProt.getDefaultConfig().isLockableDoor(block.getType())) {
            other = BlockUtil.getOtherDoorHalf(block);
        } else if (this.block.getType() instanceof ChestBlock) {
            other = BlockUtil.getDoubleChest(this.block);
        }
        if (other == null || !BlockProt.getDefaultConfig().isLockable(other.getType())) return;

        final BlockNBTHandler otherHandler;
        try {
            otherHandler = new BlockNBTHandler(other);
        } catch (RuntimeException e) {
            return;
        }
        if (condition.test(otherHandler)) {
            otherHandler.mergeHandler(this);
        } else {
            orElse.accept(otherHandler);
        }
    }

    /**
     * Clears all values from this block and resets it to the
     * defaults.
     *
     * @since 0.3.0
     */
    public void clear() {
        this.setOwner("");
        this.setFriends(Collections.emptyList());
        this.getRedstoneHandler().reset();
    }

    /**
     * Merges this handler with another {@link NBTHandler}.
     *
     * @since 0.4.6
     */
    @Override
    public void mergeHandler(@NotNull NBTHandler handler) {
        if (!(handler instanceof final BlockNBTHandler blockNBTHandler)) return;
        this.setOwner(blockNBTHandler.getOwner());
        this.setFriends(blockNBTHandler.getFriends());
        this.getRedstoneHandler().mergeHandler(blockNBTHandler.getRedstoneHandler());
        this.setName(blockNBTHandler.getName());
    }

    @Override
    public void pasteNbt(@NotNull CompoundTag container) {
        // We remove the owner key for security reasons.
        container.remove(OWNER_ATTRIBUTE);
        super.pasteNbt(container);
        this.applyToOtherContainer();
    }

    /**
     * Removes all data of a block without block entity from its chunk.
     */
    public static void removeChunkData(@NotNull final LevelChunk chunk, @NotNull final BlockPos pos) {
        final CompoundTag blocks = chunk.getAttached(BlockProtAttachments.CHUNK_BLOCKS);
        if (blocks == null || blocks.remove(Long.toString(pos.asLong())) == null) return;
        if (blocks.isEmpty()) chunk.removeAttached(BlockProtAttachments.CHUNK_BLOCKS);
        else chunk.setAttached(BlockProtAttachments.CHUNK_BLOCKS, blocks);
        chunk.markUnsaved();
    }

    private interface Storage {
        @NotNull CompoundTag read();

        /** Saves the container and returns the container that is attached from now on. */
        @NotNull CompoundTag commit(@NotNull CompoundTag container);
    }

    /** Stores the data on the block entity, like the PersistentDataContainer of a TileState. */
    private record BlockEntityStorage(@NotNull BlockEntity blockEntity) implements Storage {
        @Override
        public @NotNull CompoundTag read() {
            final @Nullable CompoundTag existing = blockEntity.getAttached(BlockProtAttachments.BLOCK_ENTITY);
            return existing == null ? new CompoundTag() : existing;
        }

        @Override
        public @NotNull CompoundTag commit(@NotNull CompoundTag container) {
            final @Nullable CompoundTag existing = blockEntity.getAttached(BlockProtAttachments.BLOCK_ENTITY);
            if (existing != null && existing != container) {
                // Someone else attached data since we've read it.
                existing.merge(container);
                container = existing;
            }
            blockEntity.setAttached(BlockProtAttachments.BLOCK_ENTITY, container);
            blockEntity.setChanged();
            return container;
        }
    }

    /** Stores the data inside the chunk, keyed by the position, like NBT-API's NBTBlock. */
    private record ChunkStorage(@NotNull LevelChunk chunk, @NotNull String key) implements Storage {
        ChunkStorage(@NotNull BlockRef block) {
            this(block.level().getChunkAt(block.pos()), Long.toString(block.pos().asLong()));
        }

        @Override
        public @NotNull CompoundTag read() {
            final @Nullable CompoundTag blocks = chunk.getAttached(BlockProtAttachments.CHUNK_BLOCKS);
            if (blocks != null && blocks.get(key) instanceof CompoundTag existing) return existing;
            return new CompoundTag();
        }

        @Override
        public @NotNull CompoundTag commit(@NotNull CompoundTag container) {
            @Nullable CompoundTag blocks = chunk.getAttached(BlockProtAttachments.CHUNK_BLOCKS);
            if (blocks == null) blocks = new CompoundTag();
            if (blocks.get(key) instanceof CompoundTag existing && existing != container) {
                existing.merge(container);
                container = existing;
            }
            blocks.put(key, container);
            chunk.setAttached(BlockProtAttachments.CHUNK_BLOCKS, blocks);
            chunk.markUnsaved();
            return container;
        }
    }
}
