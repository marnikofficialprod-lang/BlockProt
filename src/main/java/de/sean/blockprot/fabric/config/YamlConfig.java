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

package de.sean.blockprot.fabric.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A tiny read-only replacement for Bukkit's YamlConfiguration. Keys are separated by dots,
 * so "inventories.back" reads the "back" key inside the "inventories" section.
 */
public final class YamlConfig {
    private final Map<?, ?> root;

    private YamlConfig(@NotNull final Map<?, ?> root) {
        this.root = root;
    }

    @NotNull
    public static YamlConfig empty() {
        return new YamlConfig(Collections.emptyMap());
    }

    @NotNull
    public static YamlConfig load(@NotNull final Reader reader) {
        final Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
        return loaded instanceof Map<?, ?> map ? new YamlConfig(map) : empty();
    }

    @NotNull
    public static YamlConfig load(@NotNull final InputStream stream) {
        return load(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    @NotNull
    public static YamlConfig load(@NotNull final Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    @Nullable
    public Object get(@NotNull final String path) {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = map.get(part);
            if (current == null) return null;
        }
        return current;
    }

    public boolean contains(@NotNull final String path) {
        return get(path) != null;
    }

    @Nullable
    public String getString(@NotNull final String path) {
        final Object value = get(path);
        return value == null || value instanceof Map<?, ?> || value instanceof List<?> ? null : value.toString();
    }

    public boolean getBoolean(@NotNull final String path) {
        return get(path) instanceof Boolean b && b;
    }

    public int getInt(@NotNull final String path) {
        return get(path) instanceof Number n ? n.intValue() : 0;
    }

    public long getLong(@NotNull final String path) {
        return get(path) instanceof Number n ? n.longValue() : 0L;
    }

    public double getDouble(@NotNull final String path) {
        return get(path) instanceof Number n ? n.doubleValue() : 0.0;
    }

    @Nullable
    public List<?> getList(@NotNull final String path) {
        return get(path) instanceof List<?> list ? list : null;
    }

    @NotNull
    public List<String> getStringList(@NotNull final String path) {
        final List<?> list = getList(path);
        if (list == null) return new ArrayList<>();
        final List<String> ret = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof String || o instanceof Number || o instanceof Boolean) ret.add(o.toString());
        }
        return ret;
    }
}
