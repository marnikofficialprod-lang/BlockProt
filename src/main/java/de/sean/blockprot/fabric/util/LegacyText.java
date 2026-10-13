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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Converts legacy "§" formatted strings, which are used by the translation files, into components.
 * Behaves like Spigot's CraftChatMessage: a colour code resets all other formatting, so
 * coloured item names are not italic, while names without colour codes are.
 */
public final class LegacyText {
    private static final Style RESET = Style.EMPTY
        .withBold(false)
        .withItalic(false)
        .withUnderlined(false)
        .withStrikethrough(false)
        .withObfuscated(false);

    private LegacyText() {
    }

    @NotNull
    public static MutableComponent parse(@NotNull final String text) {
        final MutableComponent root = Component.empty();
        Style style = Style.EMPTY;
        final StringBuilder current = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c != '§' || i + 1 >= text.length()) {
                current.append(c);
                continue;
            }

            final char code = Character.toLowerCase(text.charAt(i + 1));
            // Spigot's hex colour format: §x§r§r§g§g§b§b
            if (code == 'x' && isHexSequence(text, i + 2)) {
                final StringBuilder hex = new StringBuilder();
                for (int j = 0; j < 6; j++) hex.append(text.charAt(i + 3 + j * 2));
                flush(root, current, style);
                style = RESET.withColor(TextColor.fromRgb(Integer.parseInt(hex.toString(), 16)));
                i += 13;
                continue;
            }

            final ChatFormatting format = ChatFormatting.getByCode(code);
            if (format == null) {
                current.append(c);
                continue;
            }
            flush(root, current, style);
            if (format == ChatFormatting.RESET) {
                style = RESET;
            } else if (format.ordinal() <= ChatFormatting.WHITE.ordinal()) {
                style = RESET.withColor(format);
            } else {
                style = style.applyFormat(format);
            }
            i++;
        }
        flush(root, current, style);
        return root;
    }

    private static boolean isHexSequence(@NotNull final String text, final int start) {
        if (start + 12 > text.length()) return false;
        for (int j = 0; j < 6; j++) {
            if (text.charAt(start + j * 2) != '§') return false;
            if (Character.digit(text.charAt(start + j * 2 + 1), 16) < 0) return false;
        }
        return true;
    }

    private static void flush(@NotNull final MutableComponent root, @NotNull final StringBuilder current, @NotNull final Style style) {
        if (current.isEmpty()) return;
        root.append(Component.literal(current.toString()).setStyle(style));
        current.setLength(0);
    }
}
