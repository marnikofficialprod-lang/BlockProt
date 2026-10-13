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

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * The persistent data BlockProt stores in the world. This replaces the Bukkit
 * PersistentDataContainers the Spigot plugin used.
 */
public final class BlockProtAttachments {
    /**
     * Data of a lockable block entity (chests, furnaces, ...). Removed together with the block entity.
     */
    public static final AttachmentType<CompoundTag> BLOCK_ENTITY = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("blockprot", "block"),
        builder -> builder.persistent(CompoundTag.CODEC));

    /**
     * Data of lockable blocks without a block entity (doors, trapdoors, ...), stored in the chunk and
     * keyed by the packed block position.
     */
    public static final AttachmentType<CompoundTag> CHUNK_BLOCKS = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("blockprot", "blocks"),
        builder -> builder.persistent(CompoundTag.CODEC));

    /**
     * The BlockProt settings of a player.
     */
    public static final AttachmentType<CompoundTag> PLAYER_SETTINGS = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("blockprot", "player_settings"),
        builder -> builder.persistent(CompoundTag.CODEC).copyOnDeath());

    private BlockProtAttachments() {
    }

    public static void init() {
        // Loads the class and therefore registers all attachment types.
    }
}
