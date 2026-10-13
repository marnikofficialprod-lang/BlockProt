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

package de.sean.blockprot.fabric.inventories;

import de.sean.blockprot.fabric.BlockProt;
import de.sean.blockprot.fabric.ProfileService;
import de.sean.blockprot.fabric.TranslationKey;
import de.sean.blockprot.fabric.Translator;
import de.sean.blockprot.fabric.nbt.PlayerSettingsHandler;
import de.sean.blockprot.nbt.FriendModifyAction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FriendSearchResultInventory extends BlockProtInventory {
    private final int maxResults = getSize() - 1;

    private boolean closed = false;

    @Override
    int getSize() {
        return InventoryConstants.tripleLine;
    }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__FRIENDS__RESULT);
    }

    private static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    // Optimized version of the Levenshtein Distance from here:
    // https://stackoverflow.com/a/13564498/9156308
    private static int levenshteinDistance(@NotNull CharSequence a, @NotNull CharSequence b) {
        if (a.isEmpty())
            return b.length();
        if (b.isEmpty())
            return a.length();

        int[] mem = new int[b.length()];
        for (int i = 0; i < b.length(); ++i)
            mem[i] = i;

        for (int i = 1; i < a.length(); ++i) {
            int[] cur = new int[b.length()];
            cur[0] = i;
            for (int j = 1; j < b.length(); ++j) {
                int d1 = mem[j] + 1;
                int d2 = cur[j - 1] + 1;
                int d3 = mem[j - 1];
                if (a.charAt(i - 1) != b.charAt(j - 1))
                    d3 += 1;
                cur[j] = min(d1, d2, d3);
            }
            mem = cur;
        }
        return mem[b.length() - 1];
    }

    @Override
    public void onClick(@NotNull MenuClick event, @NotNull InventoryState state) {
        ServerPlayer player = event.player();
        ItemStack item = event.currentItem();
        if (item.is(GuiItems.BLACK_STAINED_GLASS_PANE)) {
            // As in the anvil inventory we cannot differentiate between
            // pressing Escape to go back, or closing it to go to the result
            // inventory, we won't return to the anvil inventory and instead
            // go right back to the FriendAddInventory.
            closeAndOpen(
                player,
                new FriendManageInventory().fill(player)
            );
        } else if (item.is(Items.PLAYER_HEAD) || item.is(Items.SKELETON_SKULL)) {
            final int index = event.slot();
            if (index >= 0 && index < state.friendResultCache.size()) {
                final var id = state.friendResultCache.get(index);
                modifyFriendsForAction(player, id, FriendModifyAction.ADD_FRIEND);
                closeAndOpen(player, new FriendManageInventory().fill(player));

                // Update the search history
                PlayerSettingsHandler settingsHandler = new PlayerSettingsHandler(player);
                settingsHandler.addPlayerToSearchHistory(id);
            }
        } else {
            closeAndOpen(player, null);
        }
    }

    @Override
    public void onClose(@NotNull ServerPlayer player, @NotNull InventoryState state) {
        closed = true;
    }

    /**
     * Compare two strings by the levenshtein distance, returning a value between 0,
     * being totally unrelated strings, and 1, being identical or if both are empty.
     *
     * @since 0.3.0
     */
    private static double compareStrings(String str1, String str2) {
        String longer = str1;
        String shorter = str2;
        if (str1.length() < str2.length()) {
            longer = str2;
            shorter = str1;
        }
        final int longerLength = longer.length();
        if (longerLength == 0) return 1.0; // They match 100% if both Strings are empty
        else return (longerLength - levenshteinDistance(longer, shorter)) / (double) longerLength;
    }

    @Nullable
    public FriendSearchResultInventory fill(@NotNull ServerPlayer player, @NotNull String searchQuery) {
        InventoryState state = InventoryState.get(player.getUUID());
        if (state == null) return this;
        final MinecraftServer server = BlockProt.getServer();
        if (server == null) return null;

        state.friendResultCache.clear();
        for (int i = 0; i < maxResults; i++) {
            this.setItemStack(i, Items.SKELETON_SKULL, "Loading...");
        }
        setBackButton();

        final double minimumSimilarity = BlockProt.getDefaultConfig().getFriendSearchSimilarityPercentage();
        // Other mods might use other UUID versions for NPCs or other players.
        final var candidates = ProfileService.getKnownPlayers(server).stream()
            .filter(uuid -> uuid.version() == 3 || uuid.version() == 4 || uuid.version() == 0)
            .filter(uuid -> !uuid.equals(player.getUUID()))
            .toList();

        // This loads all possible players and filters them based on the search criteria.
        ProfileService.findAllByUuid(candidates).whenComplete((profiles, error) -> {
            if (closed) return;
            for (int i = 0; i < maxResults; i++) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            if (error != null) {
                BlockProt.LOGGER.warn("Failed to search and filter players during friend search: " + error.getMessage());
                return;
            }

            final List<Map.Entry<UUID, String>> results = profiles.entrySet().stream()
                .map(p -> Map.entry(p, compareStrings(p.getValue(), searchQuery)))
                .filter(p -> p.getValue() >= minimumSimilarity)
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .limit(maxResults)
                .toList();

            state.friendResultCache.clear();
            int playersIndex = 0;
            for (Map.Entry<UUID, String> profile : results) {
                state.friendResultCache.add(profile.getKey());
                setPlayerSkull(playersIndex++, profile.getKey(), profile.getValue());
            }
        });
        return this;
    }
}
