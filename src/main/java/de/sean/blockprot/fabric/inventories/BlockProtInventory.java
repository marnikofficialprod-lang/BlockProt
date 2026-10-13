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
import de.sean.blockprot.fabric.Permissions;
import de.sean.blockprot.fabric.TranslationKey;
import de.sean.blockprot.fabric.Translator;
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.nbt.FriendHandler;
import de.sean.blockprot.fabric.nbt.FriendSupportingHandler;
import de.sean.blockprot.fabric.nbt.PlayerSettingsHandler;
import de.sean.blockprot.fabric.util.BlockRef;
import de.sean.blockprot.fabric.util.LegacyText;
import de.sean.blockprot.nbt.FriendModifyAction;
import de.sean.blockprot.nbt.LockReturnValue;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A base inventory holder for each of the mod's menus. The contents live in a
 * {@link SimpleContainer} that is shown to the player through a {@link GuiMenu}.
 *
 * @since 0.2.3
 */
public abstract class BlockProtInventory {
    protected SimpleContainer inventory;

    @NotNull
    private Component title = Component.translatable("container.chest");

    @NotNull
    private MenuType<?> menuType = MenuType.GENERIC_9x3;

    /**
     * Creates a new BlockProtInventory object, that optionally also has its inventory initialized directly.
     *
     * @since 1.0.0
     */
    public BlockProtInventory(boolean createInventory) {
        if (createInventory)
            inventory = createInventory();
    }

    public BlockProtInventory() {
        this(true);
    }

    /**
     * Gets this holder's inventory. Does not create a new one.
     */
    @NotNull
    public final SimpleContainer getInventory() {
        return inventory;
    }

    /**
     * Get the size of this inventory, which should be a multiple of 9 with a maximum value of 6 *
     * 9.
     */
    abstract int getSize();

    /**
     * Gets the translated name of this inventory, or an empty String if not translatable.
     */
    @Nullable
    abstract String getTranslatedInventoryName();

    @Nullable
    public final String getDefaultInventoryName() {
        final var inventoryName = getTranslatedInventoryName();
        if (inventoryName == null)
            return null;
        return inventoryName.isEmpty() ? this.getClass().getSimpleName() : inventoryName;
    }

    /**
     * Handles clicks in this inventory.
     */
    public abstract void onClick(@NotNull final MenuClick event, @NotNull final InventoryState state);

    /**
     * Callback when this inventory gets closed, so that the holders can save their NBT or state.
     */
    public abstract void onClose(@NotNull final ServerPlayer player, @NotNull final InventoryState state);

    final void handleClick(@NotNull final MenuClick event) {
        final InventoryState state = InventoryState.get(event.player().getUUID());
        if (state == null) return;
        onClick(event, state);
    }

    final void handleClose(@NotNull final ServerPlayer player) {
        final InventoryState state = InventoryState.get(player.getUUID());
        if (state == null) return;
        onClose(player, state);
    }

    @NotNull
    private static MenuType<?> chestMenuType(final int size) {
        return switch ((size + 8) / 9) {
            case 0, 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    private static int chestSize(final int size) {
        return Math.min(Math.max((size + 8) / 9, 1), 6) * 9;
    }

    /**
     * Create this current inventory. If {@link BlockProtInventory#getTranslatedInventoryName()}
     * returns an empty String, this class's simple name will be used. If it returns null, this uses
     * the default 'Chest' title.
     */
    @NotNull
    protected final SimpleContainer createInventory() {
        var name = getDefaultInventoryName();
        if (name != null) {
            return createInventory(name);
        } else {
            this.title = Component.translatable("container.chest");
            this.menuType = chestMenuType(getSize());
            return new SimpleContainer(chestSize(getSize()));
        }
    }

    /**
     * Creates a new inventory with given {@code title}.
     */
    @NotNull
    protected final SimpleContainer createInventory(@NotNull String title) {
        this.title = LegacyText.parse(title);
        this.menuType = chestMenuType(getSize());
        return new SimpleContainer(chestSize(getSize()));
    }

    /**
     * Creates a new inventory with the given menu type, e.g. a hopper.
     */
    @NotNull
    protected final SimpleContainer createInventory(@NotNull MenuType<?> menuType, int size, @NotNull Component title) {
        this.title = title;
        this.menuType = menuType;
        return new SimpleContainer(size);
    }

    /**
     * Opens this inventory for the player. Like the InventoryOpenEvent listener of the plugin,
     * this double-checks that the player may still access the block of the current state.
     *
     * @return Whether the inventory has been opened.
     */
    public boolean open(@NotNull final ServerPlayer player) {
        final InventoryState state = InventoryState.get(player.getUUID());
        if (state != null && state.getBlock() != null) {
            final String playerUuid = player.getUUID().toString();
            try {
                BlockNBTHandler handler = new BlockNBTHandler(state.getBlock());
                Optional<FriendHandler> friend = handler.getFriend(playerUuid);
                if (!(handler.isNotProtected()
                    || handler.isOwner(playerUuid)
                    || (friend.isPresent() && friend.get().isManager())
                    || Permissions.ADMIN.check(player)
                    || Permissions.INFO.check(player))) {
                    player.closeContainer();
                    player.sendOverlayMessage(LegacyText.parse(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION)));
                    return false;
                }
            } catch (RuntimeException ignored) {
            }
        }

        final GuiMenuFactory factory = new GuiMenuFactory(this, menuType);
        player.openMenu(new SimpleMenuProvider(factory::create, title));
        return true;
    }

    private record GuiMenuFactory(@NotNull BlockProtInventory holder, @NotNull MenuType<?> menuType) {
        GuiMenu create(int containerId, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
            return new GuiMenu(menuType, containerId, inventory, holder);
        }
    }

    /**
     * Modifies given {@code friend} for given {@code action}.
     *
     * @since 1.1.16
     */
    protected final void modifyFriendsForAction(
            @NotNull final ServerPlayer player,
            @NotNull final UUID friend,
            @NotNull final FriendModifyAction action
    ) {
        applyChanges(
            player,
            (handler) -> handler.modifyFriends(
                player.getUUID().toString(),
                friend.toString(),
                action
            ),
            (handler) -> {
                switch (action) {
                    case ADD_FRIEND -> handler.addFriend(friend.toString());
                    case REMOVE_FRIEND -> handler.removeFriend(friend.toString());
                }
            }
        );
    }

    /**
     * Creates a temporary {@link BlockNBTHandler} or {@link PlayerSettingsHandler}
     * depending on {@link InventoryState#friendSearchState}.
     *
     * @since 0.4.6
     */
    protected void applyChanges(
        @NotNull final ServerPlayer player,
        @Nullable final Function<BlockNBTHandler, LockReturnValue> onBlockChanges,
        @Nullable final Consumer<PlayerSettingsHandler> onSettingsChanges) {
        InventoryState state = InventoryState.get(player.getUUID());
        if (state == null) return;
        switch (state.friendSearchState) {
            case FRIEND_SEARCH -> {
                if (onBlockChanges == null) return;
                BlockNBTHandler nbtHandler = getNbtHandlerOrNull(state.getBlock());
                if (nbtHandler == null) return;
                LockReturnValue ret = onBlockChanges.apply(nbtHandler);
                if (ret.success)
                    nbtHandler.applyToOtherContainer();
            }
            case DEFAULT_FRIEND_SEARCH -> {
                if (onSettingsChanges == null) return;
                PlayerSettingsHandler settingsHandler = new PlayerSettingsHandler(player);
                onSettingsChanges.accept(settingsHandler);
            }
        }
    }

    /**
     * Sets the back button to the last item in the inventory.
     */
    public void setBackButton() {
        setBackButton(inventory.getContainerSize() - 1);
    }

    /**
     * Sets the back button to the [index] in the inventory.
     */
    public void setBackButton(int index) {
        setItemStack(index, GuiItems.BLACK_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__BACK);
    }

    public void setItemStack(int index, Item item, TranslationKey key) {
        setItemStack(index, item, Translator.get(key));
    }

    public void setItemStack(int index, Item item, TranslationKey key, List<String> lore) {
        setItemStack(index, item, Translator.get(key), lore);
    }

    public void setItemStack(int index, Item item, String text) {
        setItemStack(index, item, text, Collections.emptyList());
    }

    /**
     * Sets a ItemStack with the type {@code item} and the name as {@code text} with {@code lore}
     * at {@code index}. Both name and lore may contain legacy colour codes.
     */
    public void setItemStack(int index, Item item, String text, List<String> lore) {
        final ItemStack stack = new ItemStack(item, 1);
        stack.set(DataComponents.CUSTOM_NAME, LegacyText.parse(text));
        if (!lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore.stream().map(line -> (Component) LegacyText.parse(line)).toList()));
        }
        inventory.setItem(index, stack);
    }

    /**
     * Set a player skull to {@code index}. The client loads the skin of the given player.
     */
    public void setPlayerSkull(int index, @NotNull UUID uuid, @Nullable String name) {
        final ItemStack stack = new ItemStack(Items.PLAYER_HEAD, 1);
        stack.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(uuid));
        if (name != null)
            stack.set(DataComponents.CUSTOM_NAME, LegacyText.parse(name));
        inventory.setItem(index, stack);
    }

    /**
     * Closes the player's current inventory and then opens the new
     * inventory. If inventory is null, we clear the inventory state.
     */
    protected void closeAndOpen(@NotNull final ServerPlayer player, @Nullable final BlockProtInventory inventory) {
        if (inventory != null) {
            inventory.open(player);
        } else {
            player.closeContainer();
            InventoryState.remove(player.getUUID());
        }
    }

    /**
     * This gets the NBT Handler for the block requested by the player. If
     * a {@link RuntimeException} is triggered due to the block being invalid,
     * possibly by another player breaking it, we return null, indicating the
     * caller should close the inventory.
     */
    @Nullable
    protected BlockNBTHandler getNbtHandlerOrNull(@Nullable BlockRef block) {
        if (block == null) return null;
        try {
            return new BlockNBTHandler(block);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Gets the handler for the block opened/edited by the player.
     */
    protected @Nullable FriendSupportingHandler getFriendSupportingHandler(@NotNull InventoryState.FriendSearchState state,
                                                                           @Nullable ServerPlayer player,
                                                                           @Nullable BlockRef block) {
        return switch (state) {
            case FRIEND_SEARCH -> block == null ? null : getNbtHandlerOrNull(block);
            case DEFAULT_FRIEND_SEARCH -> player == null ? null : new PlayerSettingsHandler(player);
        };
    }

    /**
     * Updates the title of this inventory. As inventory titles cannot
     * be updated directly, this instead re-creates and re-opens the
     * inventory.
     */
    protected void updateTitle(@NotNull ServerPlayer player, @NotNull String title) {
        this.inventory = this.createInventory(title);
        this.closeAndOpen(player, this);
    }

    private static boolean isEnchanted(@NotNull ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    /**
     * Sets an enchanted version of the item into the index. Based on the value of {@code value},
     * the string returned for the translation key will be suffixed with an ": Enabled", or
     * ": Disabled", as per the respective translation key for each.
     *
     * @since 1.0.0
     */
    public void setEnchantedOptionItemStack(int index, Item item, TranslationKey key, boolean value) {
        setItemStack(index, item, Translator.get(key) + ": " +
            (value ? Translator.get(TranslationKey.ENABLED) : Translator.get(TranslationKey.DISABLED)));
        ItemStack stack = inventory.getItem(index);
        inventory.setItem(index, toggleOption(stack, value));
    }

    /**
     * Toggles the option name, if it's found at the end of the display name, and the enchantment
     * status of the given itemstack.
     *
     * @param toggle The new value. If null, the current value gets inverted.
     * @since 1.0.0
     */
    @NotNull
    protected ItemStack toggleOption(@NotNull ItemStack stack, final @Nullable Boolean toggle) {
        stack = stack.copy();
        final Component customName = stack.get(DataComponents.CUSTOM_NAME);
        var name = customName == null ? "" : customName.getString();
        final var pos = name.lastIndexOf(':');
        if (pos != -1) {
            name = name.substring(0, pos);
        }

        if (isEnchanted(stack) && (toggle == null || !toggle)) {
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            stack.set(DataComponents.CUSTOM_NAME, LegacyText.parse(name + ": " + Translator.get(TranslationKey.DISABLED)));
        } else if (!isEnchanted(stack) && (toggle == null || toggle)) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            stack.set(DataComponents.CUSTOM_NAME, LegacyText.parse(name + ": " + Translator.get(TranslationKey.ENABLED)));
        }
        return stack;
    }

    /**
     * Logs a failed profile lookup like the plugin did.
     */
    protected static void logProfileFailure(@NotNull Exception e) {
        BlockProt.LOGGER.warn("Failed to update PlayerProfile: " + e.getMessage());
    }

    /**
     * Counts how often this inventory has been (re-)filled, so that player heads which
     * were loaded asynchronously don't end up on a different page.
     */
    private int fillGeneration = 0;

    /**
     * Resolves the names of the players asynchronously and then replaces the slots with
     * their player heads, like the asynchronous tasks of the plugin did.
     *
     * @param slots The players by slot index.
     */
    protected void fillPlayerSkullsAsync(@NotNull final java.util.Map<Integer, UUID> slots) {
        final int generation = ++fillGeneration;
        final var uuids = slots.values().stream()
            .filter(uuid -> !uuid.equals(FriendSupportingHandler.publicUuid))
            .distinct()
            .toList();
        if (uuids.isEmpty()) return;
        de.sean.blockprot.fabric.ProfileService.findAllByUuid(uuids).whenComplete((names, error) -> {
            if (error != null) {
                BlockProt.LOGGER.warn("Failed to update PlayerProfile: " + error.getMessage());
                return;
            }
            if (generation != fillGeneration) return;
            slots.forEach((slot, uuid) -> {
                if (uuid.equals(FriendSupportingHandler.publicUuid)) return;
                final String name = names.get(uuid);
                setPlayerSkull(slot, uuid, name == null ? uuid.toString() : name);
            });
        });
    }

    /**
     * Invalidates any pending asynchronous skull updates.
     */
    protected void invalidatePendingSkulls() {
        ++fillGeneration;
    }
}
