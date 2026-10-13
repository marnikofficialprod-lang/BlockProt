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

import de.sean.blockprot.fabric.BlockProt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The default config of BlockProt. Wraps the user's config.yml and falls back to the
 * config.yml inside the jar for every scalar value.
 *
 * @since 0.2.3
 */
public final class DefaultConfig {
    @NotNull
    private final YamlConfig config;
    @NotNull
    private final YamlConfig defaults;

    /**
     * A list of all lockable tile entities.
     */
    private final Set<Block> lockableTileEntities = new HashSet<>();

    /**
     * A list of all available shulker boxes, so we
     * can save the protection state even after breaking.
     */
    private final Set<Block> shulkerBoxes = new HashSet<>();

    /**
     * Blocks without block entity. Doors are separately listed inside of [lockableDoors].
     */
    private final Set<Block> lockableBlocks = new HashSet<>();

    /**
     * Doors are separate for BlockNBTHandler#applyToOtherContainer.
     */
    private final Set<Block> lockableDoors = new HashSet<>();

    private final List<String> excludedWorlds;

    public DefaultConfig(@NotNull final YamlConfig config, @NotNull final YamlConfig defaults) {
        this.config = config;
        this.defaults = defaults;
        this.excludedWorlds = config.contains("excluded_worlds")
            ? config.getStringList("excluded_worlds")
            : defaults.getStringList("excluded_worlds");
        this.loadBlocksFromConfig();
    }

    private boolean contains(@NotNull final String key) {
        return config.contains(key) || defaults.contains(key);
    }

    @NotNull
    private YamlConfig source(@NotNull final String key) {
        return config.contains(key) ? config : defaults;
    }

    /**
     * Resolves a Bukkit material name (e.g. "OAK_DOOR") or a block id (e.g. "minecraft:oak_door")
     * to a block.
     */
    @Nullable
    public static Block parseBlock(@NotNull final String name) {
        final Identifier id = Identifier.tryParse(name.trim().toLowerCase(Locale.ROOT));
        if (id == null) return null;
        return BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }

    private void loadBlockListFromConfig(@NotNull final String key, @NotNull final Set<Block> list, @NotNull final Predicate<Block> validateCallback) {
        // Like the Spigot plugin, the block lists are never taken from the defaults.
        List<?> configList = config.getList(key);
        if (configList == null) return;
        final List<String> unknown = new ArrayList<>();
        configList.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .distinct() // Remove duplicates
            .forEach(name -> {
                final Block block = parseBlock(name);
                if (block == null) {
                    unknown.add(name);
                } else if (!validateCallback.test(block)) {
                    BlockProt.LOGGER.warn("Caught invalid value passed to " + key + ": " + name);
                } else {
                    list.add(block);
                }
            });
        if (!unknown.isEmpty()) {
            BlockProt.LOGGER.warn("Failed to map following values to blocks: " + unknown);
        }
    }

    /**
     * Loads all the different lists from the config.yml file.
     */
    private void loadBlocksFromConfig() {
        loadBlockListFromConfig("lockable_tile_entities", this.lockableTileEntities,
            block -> block instanceof EntityBlock);
        loadBlockListFromConfig("lockable_shulker_boxes", this.shulkerBoxes,
            block -> block instanceof ShulkerBoxBlock);
        loadBlockListFromConfig("lockable_blocks", this.lockableBlocks,
            block -> !(block instanceof EntityBlock));
        loadBlockListFromConfig("lockable_doors", this.lockableDoors,
            block -> block instanceof DoorBlock);
        lockableBlocks.addAll(lockableDoors);
    }

    /**
     * Get the filename of the language file we use.
     * This file should be located in config/blockprot/lang/.
     */
    @Nullable
    public String getLanguageFile() {
        return source("language_file").getString("language_file");
    }

    public boolean shouldReplaceTranslations() {
        if (!contains("replace_translations")) return true;
        return source("replace_translations").getBoolean("replace_translations");
    }


    public boolean disallowRedstoneOnPlace() {
        if (contains("redstone_disallowed_by_default")) {
            return source("redstone_disallowed_by_default").getBoolean("redstone_disallowed_by_default");
        } else {
            return true;
        }
    }

    /**
     * Checks if given {@code level} should be excluded from any
     * block protection functionality. Accepts the dimension id, its path and
     * the Bukkit world folder names of the three vanilla dimensions.
     */
    public boolean isWorldExcluded(@Nullable final Level level) {
        if (level == null) return true;
        if (excludedWorlds.isEmpty()) return false;
        final Identifier id = level.dimension().identifier();
        final List<String> names = new ArrayList<>(List.of(id.toString(), id.getPath()));
        if (level.dimension() == Level.OVERWORLD) names.add("world");
        else if (level.dimension() == Level.NETHER) names.add("world_nether");
        else if (level.dimension() == Level.END) names.add("world_the_end");
        for (String excluded : excludedWorlds) {
            for (String name : names) {
                if (excluded.equalsIgnoreCase(name)) return true;
            }
        }
        return false;
    }

    public boolean lockOnPlaceByDefault() {
        if (!contains("lock_on_place_by_default")) return true;
        return source("lock_on_place_by_default").getBoolean("lock_on_place_by_default");
    }

    public boolean publicIsFriendByDefault() {
        if (!contains("public_is_friend_by_default")) return false;
        return source("public_is_friend_by_default").getBoolean("public_is_friend_by_default");
    }

    @Nullable
    public String getTranslationFallbackString() {
        if (!contains("fallback_string")) return "";
        return source("fallback_string").getString("fallback_string");
    }

    /**
     * Gets the maximum amount of blocks a player is allowed to
     * lock globally.
     *
     * @return The max block count, or null if there is no limit.
     */
    @Nullable
    public Integer getMaxLockedBlockCount() {
        if (!contains("player_max_locked_block_count"))
            return null;
        int val = source("player_max_locked_block_count").getInt("player_max_locked_block_count");
        return val > 0 ? val : null;
    }

    public long getLockHintCooldown() {
        if (!contains("lock_hint_cooldown_in_seconds")) {
            return 10;
        }
        return source("lock_hint_cooldown_in_seconds").getLong("lock_hint_cooldown_in_seconds");
    }

    public double getFriendSearchSimilarityPercentage() {
        if (!contains("friend_search_similarity")) {
            return 0.5;
        }
        return source("friend_search_similarity").getDouble("friend_search_similarity");
    }

    public boolean isFriendFunctionalityDisabled() {
        if (!contains("disable_friend_functionality")) {
            return false;
        }
        return source("disable_friend_functionality").getBoolean("disable_friend_functionality");
    }

    /**
     * Whether the given {@code type} is either a lockable block or a lockable tile entity.
     */
    public boolean isLockable(@NotNull Block type) {
        return isLockableBlock(type) || isLockableTileEntity(type);
    }

    public boolean isLockableBlock(@NotNull Block type) {
        return lockableBlocks.contains(type);
    }

    public boolean isLockableTileEntity(@NotNull Block type) {
        return lockableTileEntities.contains(type) || shulkerBoxes.contains(type);
    }

    public boolean isLockableDoor(@NotNull Block type) {
        return lockableDoors.contains(type);
    }

    public boolean isLockableShulkerBox(@NotNull Block type) {
        return shulkerBoxes.contains(type);
    }
}
