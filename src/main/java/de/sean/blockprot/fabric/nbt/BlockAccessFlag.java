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

import de.sean.blockprot.fabric.TranslationKey;
import de.sean.blockprot.fabric.Translator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * @since 0.3.0
 */
public enum BlockAccessFlag {
    /**
     * The user is allowed to see, but not alter, the contents of the inventory.
     *
     * @since 0.3.0
     */
    READ(TranslationKey.INVENTORIES__FRIENDS__PERMISSION__READ),

    /**
     * The user is allowed to add and remove contents of the inventory.
     *
     * @since 0.3.0
     */
    WRITE(TranslationKey.INVENTORIES__FRIENDS__PERMISSION__WRITE),

    /**
     * The user is allowed to manage redstone settings and manage friends.
     *
     * @since 1.1.0
     */
    MANAGER(TranslationKey.INVENTORIES__FRIENDS__PERMISSION__MANAGER);

    @NotNull
    private final TranslationKey descriptionKey;

    BlockAccessFlag(@NotNull final TranslationKey description) {
        this.descriptionKey = description;
    }

    /**
     * Parse flags from a single int value. This is done at a binary level.
     *
     * @since 0.3.0
     */
    public static EnumSet<BlockAccessFlag> parseFlags(final int value) {
        EnumSet<BlockAccessFlag> flags = EnumSet.noneOf(BlockAccessFlag.class);
        final BlockAccessFlag[] values = BlockAccessFlag.values();
        String digits = Integer.toString(value, 2);
        for (int i = digits.length() - 1; i >= 0; --i) {
            char j = digits.charAt(i); // the bit to check
            if (j == '0') {
                // only add the flag if it is 1
                continue;
            }
            int k = digits.length() - 1 - i; // the ordinal of the enum, as per #getFlag
            if (k < values.length) {
                flags.add(values[k]);
            }
        }
        return flags;
    }

    /**
     * Gets a user-friendly name of the permissions item title.
     *
     * @since 0.3.2
     */
    public static String toBaseString() {
        return Translator.get(TranslationKey.INVENTORIES__FRIENDS__PERMISSIONS);
    }

    /**
     * Accumulate the access flag details into a list of strings, where each entry
     * represents a line. Used for item lore in the GUI.
     *
     * @since 0.3.2
     */
    @NotNull
    public static List<String> accumulateAccessFlagLore(@NotNull final EnumSet<BlockAccessFlag> flags) {
        if (flags.isEmpty()) return Collections.singletonList("No access");
        ArrayList<String> ret = new ArrayList<>();
        for (BlockAccessFlag flag : flags) {
            // ChatColor.RESET + ChatColor.YELLOW
            ret.add("§r§e" + flag.getDescription());
        }
        return ret;
    }

    @NotNull
    public String getDescription() {
        return Translator.get(descriptionKey);
    }

    /**
     * Gets this enum as a bit flag.
     *
     * @since 0.3.0
     */
    public int getFlag() {
        return 1 << ordinal();
    }
}
