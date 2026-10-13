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
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The backbone of any handler that supports adding a list of friends/players.
 *
 * @since 1.0.0
 */
public abstract class FriendSupportingHandler extends NBTHandler {
    private final @NotNull String friendNbtKey;

    /** Mojang recently started requiring zeroed UUIDs to *not* be used, so we instead use an invalid UUID */
    public static final UUID publicUuid = new UUID(~0, ~0);

    public FriendSupportingHandler(@NotNull String friendNbtKey) {
        this.friendNbtKey = friendNbtKey;
    }

    private CompoundTag compound() {
        return getOrCreateCompound(container, friendNbtKey);
    }

    /**
     * Gets a {@link Stream} of {@link FriendHandler} for this block.
     *
     * @since 1.0.0
     */
    public Stream<FriendHandler> getFriendsStream() {
        if (!(this.container.get(friendNbtKey) instanceof CompoundTag compound)) return Stream.empty();
        if (BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) return Stream.empty();

        return compound
            .keySet()
            .stream()
            .filter(k -> compound.get(k) instanceof CompoundTag)
            .map((k) -> new FriendHandler(k, compound.getCompoundOrEmpty(k), this::markDirty))
            // Guarantees that the entry representing the public is at the front of the stream.
            .sorted((a, b) -> Boolean.compare(b.doesRepresentPublic(), a.doesRepresentPublic()));
    }

    /**
     * Gets a {@link List} of {@link FriendHandler} for this block.
     *
     * @since 1.0.0
     */
    public List<FriendHandler> getFriends() {
        return this.getFriendsStream().collect(Collectors.toList());
    }

    /**
     * Set a new list of FriendHandler for the friends list.
     *
     * @since 1.0.0
     */
    public void setFriends(@NotNull final List<FriendHandler> friends) {
        container.remove(friendNbtKey);
        friends.forEach(this::addFriend);
        markDirty();
    }

    /**
     * Filters the results of {@link #getFriends()} for any entry whose
     * UUID qualifies for {@link String#equals(Object)} with given {@code id}.
     * If the given {@code id} is not a friend of this block, this might return
     * the {@link FriendHandler} which represents all players.
     *
     * @since 1.0.0
     */
    @NotNull
    public Optional<FriendHandler> getFriend(@NotNull final String id) {
        return getFriendsStream()
            .filter(f -> f.getName().equals(id) || f.doesRepresentPublic())
            // Guarantees that the entry representing the public is only used as a fallback.
            .min((a, b) -> Boolean.compare(a.doesRepresentPublic(), b.doesRepresentPublic()));
    }

    /**
     * Adds a new friend to the NBT.
     *
     * @since 1.0.0
     */
    public void addFriend(@NotNull final String friend) {
        getOrCreateCompound(compound(), friend).putString("id", friend);
        markDirty();
    }

    /**
     * Adds a new {@link FriendHandler} to this NBT data.
     *
     * @since 1.0.0
     */
    public void addFriend(@NotNull final FriendHandler friend) {
        getOrCreateCompound(compound(), friend.getName()).merge(friend.container.copy());
        markDirty();
    }

    /**
     * Add "everyone" as a friend to this handler.
     *
     * @since 1.0.0
     */
    public void addEveryoneAsFriend() {
        addFriend(publicUuid.toString());
    }

    /**
     * Removes a friend from the NBT.
     *
     * @since 1.0.0
     */
    public void removeFriend(@NotNull final String friend) {
        compound().remove(friend);
        markDirty();
    }

    public boolean containsFriend(@NotNull final String friendUuid) {
        return containsFriend(getFriendsStream(), friendUuid);
    }

    public boolean containsFriend(@NotNull final Stream<FriendHandler> friends, @NotNull final String friendUuid) {
        return friends
            .anyMatch((f) -> f.getName().equals(friendUuid));
    }
}
