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
 * A NBT handler specific to all redstone settings.
 *
 * @since 1.0.0
 */
public class RedstoneSettingsHandler extends NBTHandler {
    public static final boolean DEFAULT_PROTECTION_VALUE = true;

    // Redstone-Current protection, so e.g. doors do not react to buttons etc.
    static final String CURRENT_PROTECTION_ATTRIBUTE = "current_protection";
    static final String PISTON_PROTECTION_ATTRIBUTE = "piston_protection";
    static final String HOPPER_PROTECTION_ATTRIBUTE = "hopper_protection";

    @NotNull
    private final Runnable onChange;

    RedstoneSettingsHandler(@NotNull final CompoundTag compound, @NotNull final Runnable onChange) {
        super();
        this.container = compound;
        this.onChange = onChange;
    }

    @Override
    protected void markDirty() {
        onChange.run();
    }

    public boolean getCurrentProtection() {
        return container.getBooleanOr(CURRENT_PROTECTION_ATTRIBUTE, DEFAULT_PROTECTION_VALUE);
    }

    public void setCurrentProtection(final boolean value) {
        container.putBoolean(CURRENT_PROTECTION_ATTRIBUTE, value);
        markDirty();
    }

    public boolean getPistonProtection() {
        return container.getBooleanOr(PISTON_PROTECTION_ATTRIBUTE, DEFAULT_PROTECTION_VALUE);
    }

    public void setPistonProtection(final boolean value) {
        container.putBoolean(PISTON_PROTECTION_ATTRIBUTE, value);
        markDirty();
    }

    public boolean getHopperProtection() {
        return container.getBooleanOr(HOPPER_PROTECTION_ATTRIBUTE, DEFAULT_PROTECTION_VALUE);
    }

    public void setHopperProtection(final boolean value) {
        container.putBoolean(HOPPER_PROTECTION_ATTRIBUTE, value);
        markDirty();
    }

    /**
     * Resets all the protections to their default value.
     *
     * @since 1.0.0
     */
    public void reset() {
        this.setAll(DEFAULT_PROTECTION_VALUE);
    }

    /**
     * Resets all different redstone settings to given value.
     *
     * @since 1.0.0
     */
    public void setAll(final boolean value) {
        this.setCurrentProtection(value);
        this.setPistonProtection(value);
        this.setHopperProtection(value);
    }

    @Override
    public void mergeHandler(@NotNull NBTHandler handler) {
        if (!(handler instanceof final RedstoneSettingsHandler redstoneHandler)) return;
        this.setCurrentProtection(redstoneHandler.getCurrentProtection());
        this.setPistonProtection(redstoneHandler.getPistonProtection());
        this.setHopperProtection(redstoneHandler.getHopperProtection());
    }
}
