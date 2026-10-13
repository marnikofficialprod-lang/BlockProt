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

package de.sean.blockprot.fabric.events;

import de.sean.blockprot.fabric.util.BlockRef;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player is trying to access a block and its contents.
 * If cancelled, the access to the block is blocked.
 *
 * @since 0.4.0
 */
public final class BlockAccessEvent {
    /**
     * Listen to this to cancel accesses or to let players bypass protections.
     */
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> event -> {
        for (Listener listener : listeners) listener.onBlockAccess(event);
    });

    @NotNull
    private final BlockRef block;
    @NotNull
    private final ServerPlayer player;
    private boolean isCancelled;
    private boolean bypassProtections;

    public BlockAccessEvent(@NotNull final BlockRef block,
                            @NotNull final ServerPlayer player) {
        this.block = block;
        this.player = player;
    }

    @NotNull
    public BlockRef getBlock() {
        return block;
    }

    /**
     * The player that is trying to access this block.
     */
    @NotNull
    public ServerPlayer getPlayer() {
        return player;
    }

    public boolean isCancelled() {
        return this.isCancelled;
    }

    public void setCancelled(final boolean cancel) {
        this.isCancelled = cancel;
    }

    /**
     * Whether the mod should still check if the requesting player
     * actually owns this block, or that the player has some privileges
     * to be able to ignore any protections.
     *
     * @since 1.0.0
     */
    public boolean shouldBypassProtections() {
        return this.bypassProtections;
    }

    public void setBypassProtections(final boolean bypass) {
        this.bypassProtections = bypass;
    }

    /**
     * @return The event after all listeners have been called.
     */
    @NotNull
    public BlockAccessEvent call() {
        EVENT.invoker().onBlockAccess(this);
        return this;
    }

    @FunctionalInterface
    public interface Listener {
        void onBlockAccess(@NotNull BlockAccessEvent event);
    }
}
