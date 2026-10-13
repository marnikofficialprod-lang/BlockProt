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

package de.sean.blockprot.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.sean.blockprot.fabric.BlockProt;
import de.sean.blockprot.fabric.Permissions;
import de.sean.blockprot.fabric.TranslationKey;
import de.sean.blockprot.fabric.Translator;
import de.sean.blockprot.fabric.inventories.InventoryState;
import de.sean.blockprot.fabric.inventories.StatisticsInventory;
import de.sean.blockprot.fabric.inventories.UserSettingsInventory;
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.nbt.PlayerSettingsHandler;
import de.sean.blockprot.fabric.util.BlockRef;
import de.sean.blockprot.fabric.util.LegacyText;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

/**
 * The "/blockprot" command, aliased to "/bp", with all of its sub commands.
 *
 * @since 0.3.3
 */
public final class BlockProtCommand {
    private BlockProtCommand() {
    }

    public static void register(@NotNull final CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("blockprot")
            .executes(BlockProtCommand::usage)
            .then(Commands.literal("stats").executes(BlockProtCommand::statistics))
            .then(Commands.literal("statistics").executes(BlockProtCommand::statistics))
            .then(Commands.literal("settings").executes(BlockProtCommand::settings))
            .then(Commands.literal("about").executes(BlockProtCommand::about))
            .then(Commands.literal("reload")
                .requires(Permissions::isOp)
                .executes(BlockProtCommand::reload))
            .then(Commands.literal("disablehints").executes(BlockProtCommand::disableHints))
            .then(Commands.literal("debug")
                .requires(source -> Permissions.has(source, Permissions.DEBUG))
                .then(Commands.literal("placeDebugChest").executes(ctx -> placeDebugBlock(ctx, Blocks.CHEST)))
                .then(Commands.literal("placeDebugShulker").executes(ctx -> placeDebugBlock(ctx, Blocks.SHULKER_BOX)))
                .then(Commands.literal("clearSearchHistory").executes(BlockProtCommand::clearSearchHistory)));

        final LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);
        dispatcher.register(Commands.literal("bp").executes(BlockProtCommand::usage).redirect(node));
    }

    private static int usage(@NotNull final CommandContext<CommandSourceStack> ctx) {
        // Red error message
        ctx.getSource().sendFailure(LegacyText.parse("§c/blockprot [settings|stats|reload|about]"));
        return 0;
    }

    private static int statistics(@NotNull final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command is only executable as a player!"));
            return 0;
        }
        final InventoryState state = new InventoryState(null);
        state.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
        InventoryState.set(player.getUUID(), state);
        new StatisticsInventory().fill(player).open(player);
        return 1;
    }

    private static int settings(@NotNull final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        InventoryState state = new InventoryState(null);
        state.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
        InventoryState.set(player.getUUID(), state);
        new UserSettingsInventory().fill(player).open(player);
        new PlayerSettingsHandler(player).setHasPlayerInteractedWithMenu(true);
        return 1;
    }

    private static int about(@NotNull final CommandContext<CommandSourceStack> ctx) {
        final MutableComponent message = Component.empty()
            .append(LegacyText.parse("§x§a§3§c§6§e§bBlockProt v" + BlockProt.getVersion() + " - Fabric Mod\n"))
            .append(Component.literal("Author: spnda, Fabric port: mariotto\n"))
            .append(createUrlComponent("§x§c§3§e§e§a§7Click here to report issues or for suggestions",
                "https://github.com/marnikofficialprod-lang/BlockProt/issues", "Report issues with the Fabric version here!"));
        ctx.getSource().sendSystemMessage(message);
        return 1;
    }

    private static @NotNull MutableComponent createUrlComponent(@NotNull String text, @NotNull String url, String hoverText) {
        final MutableComponent component = LegacyText.parse(text);
        component.withStyle(style -> {
            style = style.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)));
            if (hoverText != null)
                style = style.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText)));
            return style;
        });
        return component;
    }

    private static int reload(@NotNull final CommandContext<CommandSourceStack> ctx) {
        BlockProt.reloadConfigAndTranslations();
        ctx.getSource().sendSystemMessage(Component.literal("Finished reloading BlockProt!"));
        return 1;
    }

    private static int disableHints(@NotNull final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        var settings = new PlayerSettingsHandler(player);
        if (!settings.hasPlayerInteractedWithMenu()) {
            settings.setHasPlayerInteractedWithMenu(true);
            player.sendSystemMessage(LegacyText.parse(Translator.get(TranslationKey.MESSAGES__DISABLED_HINTS)));
        }
        return 1;
    }

    private static int placeDebugBlock(@NotNull final CommandContext<CommandSourceStack> ctx, @NotNull final Block block) {
        final ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        final ServerLevel level = player.level();
        level.setBlockAndUpdate(player.blockPosition(), block.defaultBlockState());
        try {
            final var handler = new BlockNBTHandler(new BlockRef(level, player.blockPosition()));
            handler.setOwner("069a79f4-44e9-4726-a5be-fca90e38aaf5"); // Notch's UUID.
        } catch (RuntimeException e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
        return 1;
    }

    private static int clearSearchHistory(@NotNull final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        new PlayerSettingsHandler(player).clearSearchHistory();
        return 1;
    }
}
