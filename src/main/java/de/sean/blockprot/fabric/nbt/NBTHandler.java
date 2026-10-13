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

/**
 * The base NBT Handler. Every handler works on a {@link CompoundTag} and has to call
 * {@link #markDirty()} after modifying it, so that the change gets saved.
 *
 * @since 0.2.3
 */
public abstract class NBTHandler {
    /**
     * The NBT container for this handler.
     *
     * @since 0.2.3
     */
    protected CompoundTag container = new CompoundTag();

    /**
     * Create a new base NBTHandler.
     *
     * @since 0.2.3
     */
    protected NBTHandler() {
    }

    /**
     * Get the name of the nbt container, which is the key of this container inside its parent.
     *
     * @return The name, or an empty String.
     * @since 0.3.0
     */
    @NotNull
    public String getName() {
        return "";
    }

    /**
     * Called after the container has been modified.
     */
    protected void markDirty() {
    }

    /**
     * Copies all values of the other handler to this handler.
     *
     * @since 0.4.6
     */
    public void mergeHandler(@NotNull final NBTHandler handler) {}

    /**
     * Get's a copy of this NBT.
     *
     * @since 0.4.6
     */
    @NotNull
    public CompoundTag getNbtCopy() {
        return this.container.copy();
    }

    /**
     * Pastes given NBT into this container, potentially
     * overriding everything.
     *
     * @since 0.4.6
     */
    public void pasteNbt(@NotNull CompoundTag container) {
        this.container.merge(container);
        markDirty();
    }

    /**
     * Gets a sub compound or creates it, if it does not exist yet.
     */
    @NotNull
    protected static CompoundTag getOrCreateCompound(@NotNull final CompoundTag parent, @NotNull final String key) {
        if (parent.get(key) instanceof CompoundTag compound) return compound;
        final CompoundTag compound = new CompoundTag();
        parent.put(key, compound);
        return compound;
    }
}
