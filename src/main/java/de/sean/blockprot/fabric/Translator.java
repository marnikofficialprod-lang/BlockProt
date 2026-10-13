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

package de.sean.blockprot.fabric;

import de.sean.blockprot.fabric.config.YamlConfig;
import de.sean.blockprot.nbt.LockReturnValue;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Helper to quickly obtain translations from a config by a enum key.
 *
 * @since 0.2.2
 */
public final class Translator {
    /**
     * The list of all included translation files.
     *
     * @since 0.3.0
     */
    public static final List<String> DEFAULT_TRANSLATION_FILES = List.of(
        "translations_cs.yml", "translations_de.yml", "translations_en.yml", "translations_es.yml",
        "translations_fr.yml", "translations_it.yml", "translations_ja.yml", "translations_ko.yml",
        "translations_pl.yml", "translations_pt-br.yml", "translations_ru.yml", "translations_sk.yml",
        "translations_tr.yml", "translations_zh-CN.yml", "translations_zh-TW.yml");

    /**
     * The default locale we use for default translation
     * values.
     *
     * @since 0.2.2
     */
    @NotNull
    public static final Locale defaultLocale = Locale.UK;

    /**
     * A HashMap of all possible translation values by key, loaded through
     * {@link Translator#loadFromConfigs(YamlConfig, YamlConfig)}.
     *
     * @since 0.2.2
     */
    @NotNull
    private static final HashMap<TranslationKey, TranslationValue> values = new HashMap<>();

    @NotNull
    private static Locale locale = defaultLocale;

    static String DEFAULT_FALLBACK = "";

    private Translator() {
    }

    /**
     * Initialize the translations from given configuration and sets the
     * internal locale to a locale, which is read from the configs themselves.
     *
     * @param defaultConfig The default config, which is the English translation inside the jar.
     * @param config        The config of the wanted language.
     * @since 0.2.2
     */
    public static void loadFromConfigs(@NotNull final YamlConfig defaultConfig, @NotNull final YamlConfig config) {
        String locale = config.getString("locale");
        Translator.locale = (locale == null)
            ? Locale.ROOT
            : Locale.of(locale);

        long missingValues = 0, totallyMissingValues = 0;
        TranslationKey[] translations = TranslationKey.values();
        for (TranslationKey translation : translations) {
            String translationKey = translation.toString();

            // Directly set keys to unknown values when both configs don't
            if (!defaultConfig.contains(translationKey) &&
                !config.contains(translationKey)) {
                values.put(translation, TranslationValue.UNKNOWN_TRANSLATION_VALUE);
                ++totallyMissingValues;
                continue;
            }

            // Get the default config value. Set it to
            // UNKNOWN_TRANSLATION if we don't have it.
            Object defaultValue = defaultConfig.get(translationKey);
            TranslationValue translatedValue =
                (defaultValue instanceof String)
                    ? new TranslationValue((String) defaultValue)
                    : TranslationValue.UNKNOWN_TRANSLATION_VALUE;

            Object newTranslatedValue = config.get(translationKey);
            if (newTranslatedValue instanceof String) {
                translatedValue.setTranslatedValue((String) newTranslatedValue);
            } else {
                ++missingValues;
            }

            values.put(translation, translatedValue);
        }

        if (missingValues > 0)
            BlockProt.LOGGER.warn("At least " + missingValues
                + " translations have not been found in config: " + BlockProt.getDefaultConfig().getLanguageFile());
        if (totallyMissingValues > 0)
            BlockProt.LOGGER.warn("At least " + totallyMissingValues
                + " translations have not been found in any config.");
    }

    /**
     * Get the translated String by translation key. This will use
     * {@link TranslationValue#getValue()}, so values that are not
     * translated still use their default value.
     *
     * @since 0.2.2
     */
    @NotNull
    public static String get(@NotNull final TranslationKey key) {
        TranslationValue value = values.get(key);
        return value == null
            ? DEFAULT_FALLBACK
            : value.getValue();
    }

    /**
     * Gets the appropriate translation for given {@link LockReturnValue.Reason}.
     *
     * @since 1.0.3
     */
    @NotNull
    public static String get(@NotNull final LockReturnValue.Reason reason) {
        return switch (reason) {
            case NO_PERMISSION -> get(TranslationKey.MESSAGES__NO_PERMISSION);
            case EXCEEDED_MAX_BLOCK_COUNT -> get(TranslationKey.MESSAGES__EXCEEDED_MAX_BLOCK_COUNT);
        };
    }

    @NotNull
    public static Locale getLocale() {
        return locale;
    }

    /**
     * Clears all translations.
     *
     * @since 0.4.7
     */
    public static void resetTranslations() {
        values.clear();
    }
}
