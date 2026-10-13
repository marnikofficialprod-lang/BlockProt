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

import de.sean.blockprot.fabric.BlockProt;
import de.sean.blockprot.fabric.nbt.stats.BlockCountStatistic;
import de.sean.blockprot.fabric.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.fabric.nbt.stats.StatLocation;
import de.sean.blockprot.fabric.nbt.stats.Statistic;
import de.sean.blockprot.fabric.util.BlockRef;
import de.sean.blockprot.fabric.util.BlockUtil;
import de.sean.blockprot.nbt.stats.StatisticType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * StatHandler for statistic related NBT data. Keeps track
 * of every statistic and periodically saves it to disk.
 *
 * @since 1.0.0
 */
public final class StatHandler extends NBTHandler {
    static final String STAT_FILE_NAME = "blockprot_stats.nbt";
    static final String STAT_BACKUP_FILE_NAME = "blockprot_stats.nbt_old";
    static final String PLAYER_SUB_KEY = "player_stats";
    static final String SERVER_SUB_KEY = "server_stats";

    /** 5 minutes * 60 seconds * 20 ticks */
    private static final int SAVE_INTERVAL = 5 * 60 * 20;

    private static @Nullable Path baseFile;
    private static @Nullable Path backupFile;
    private static @Nullable Path temporarySwapFile;
    private static @Nullable CompoundTag nbtFile;
    private static boolean dirty = false;
    private static int ticksSinceSave = 0;

    /** Internal constructor to copy the NBT compound. */
    private StatHandler(@NotNull final CompoundTag compound) {
        super();
        this.container = compound;
    }

    /** Update the NBT compound of given statistic. */
    public void updateStatistic(final @NotNull Statistic<?> statistic) {
        statistic.updateContainer(this.container);
    }

    public static void markStatsDirty() {
        dirty = true;
    }

    @Nullable
    private static CompoundTag tryRead(@NotNull final Path file) {
        if (!Files.exists(file)) return null;
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    public static void enable(@NotNull final MinecraftServer server) {
        if (nbtFile != null) return;
        final Path worldFolder = server.getWorldPath(LevelResource.ROOT);
        baseFile = worldFolder.resolve(STAT_FILE_NAME);
        backupFile = worldFolder.resolve(STAT_BACKUP_FILE_NAME);
        temporarySwapFile = worldFolder.resolve(STAT_FILE_NAME + ".tmp");

        nbtFile = tryRead(baseFile);
        if (nbtFile == null && Files.exists(baseFile)) {
            // The base file has been corrupted and couldn't be loaded. We'll try reading from the backup.
            nbtFile = tryRead(backupFile);
            if (nbtFile == null) {
                BlockProt.LOGGER.warn("The statistics file and its backup are corrupted!");
            }
        }
        if (nbtFile == null) {
            nbtFile = new CompoundTag();
            dirty = true;
        }
        ticksSinceSave = 0;
    }

    /**
     * Called every tick. Saves the statistics file every 5 minutes.
     */
    public static void tick() {
        if (++ticksSinceSave < SAVE_INTERVAL) return;
        ticksSinceSave = 0;
        if (!dirty || nbtFile == null) return;
        final CompoundTag copy = nbtFile.copy();
        dirty = false;
        BlockProt.EXECUTOR.execute(() -> {
            try {
                saveFile(copy);
            } catch (IOException e) {
                BlockProt.LOGGER.warn("Failed to save statistics file: " + e.getMessage());
            }
        });
    }

    /**
     * We will always first save to a temporary file and then swap it with the actual file.
     * This ensures that the write process on the actual file does not get interrupted, thus
     * never corrupting it. The previous file is kept as a backup.
     */
    private static synchronized void saveFile(@NotNull final CompoundTag compound) throws IOException {
        if (baseFile == null || backupFile == null || temporarySwapFile == null) return;
        NbtIo.writeCompressed(compound, temporarySwapFile);
        if (tryRead(temporarySwapFile) == null) {
            Files.deleteIfExists(temporarySwapFile);
            throw new IOException("Failed to re-read statistic NBT File!");
        }
        if (Files.exists(baseFile)) {
            Files.move(baseFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temporarySwapFile, baseFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void disable() {
        if (nbtFile == null) return;
        try {
            saveFile(nbtFile.copy());
        } catch (IOException e) {
            BlockProt.LOGGER.warn("Failed to save statistics file: " + e.getMessage());
        }
        nbtFile = null;
    }

    /**
     * Adds given block to given player's block statistic, while also incrementing the
     * global block count.
     *
     * @since 1.0.0
     */
    public static void addBlock(@NotNull final UUID player, @NotNull final BlockRef block) {
        BlockCountStatistic countStatistic = new BlockCountStatistic();
        PlayerBlocksStatistic containersStatistic = new PlayerBlocksStatistic();
        StatHandler.getStatistic(countStatistic);
        StatHandler.getStatistic(containersStatistic, player);

        countStatistic.increment();
        containersStatistic.add(StatLocation.of(block));
    }

    /**
     * Removes given block from the player's statistic, while also decrementing
     * the global block count.
     */
    private static void removeContainer(@NotNull final UUID player, @NotNull final StatLocation block) {
        BlockCountStatistic countStatistic = new BlockCountStatistic();
        PlayerBlocksStatistic containersStatistic = new PlayerBlocksStatistic();
        StatHandler.getStatistic(countStatistic);
        StatHandler.getStatistic(containersStatistic, player);

        countStatistic.decrement();
        containersStatistic.remove(block);
    }

    /**
     * Removes given block from the player's statistic, while also decrementing
     * the global block count. Also takes care of special blocks, like doors.
     *
     * @since 1.1.5
     */
    public static void removeContainer(@NotNull final UUID player, @NotNull final BlockRef block) {
        /* Remove the other half of the door from the statistics as well */
        if (BlockProt.getDefaultConfig().isLockableDoor(block.getType())) {
            final BlockRef otherDoor = BlockUtil.getOtherDoorHalf(block);
            if (otherDoor != null)
                StatHandler.removeContainer(player, StatLocation.of(otherDoor));
        }
        removeContainer(player, StatLocation.of(block));
    }

    /**
     * Get a statistic. To query player specific statistics please
     * use {@link #getStatistic(Statistic, UUID)}.
     *
     * @since 1.0.0
     */
    public static void getStatistic(@NotNull Statistic<?> statistic) {
        if (statistic.getType() == StatisticType.PLAYER) {
            throw new RuntimeException("StatHandler#getStatistic with player statistic called without player");
        }
        getStatistic(statistic, null);
    }

    public static void getStatistic(@NotNull Statistic<?> statistic, @Nullable UUID player) {
        switch (statistic.getType()) {
            case ALL:
            case PLAYER:
                // Only get the statistic if player is not null.
                if (player != null) {
                    getStatsForPlayer(player.toString()).updateStatistic(statistic);
                }
                // Let "ALL" fallthrough.
                if (statistic.getType() == StatisticType.PLAYER) break;
            case GLOBAL:
                getServerStats().updateStatistic(statistic);
                break;
        }
    }

    @NotNull
    private static CompoundTag file() {
        if (nbtFile == null) throw new RuntimeException("nbtFile was null.");
        return nbtFile;
    }

    private static @NotNull StatHandler getStatsForPlayer(@NotNull final String id) {
        final CompoundTag players = getOrCreateCompound(file(), PLAYER_SUB_KEY);
        if (players.get(id) instanceof CompoundTag compound) return new StatHandler(compound);
        // Add the stats NBT to be sure that we always return a value.
        final CompoundTag compound = getOrCreateCompound(players, id);
        compound.putString("id", id);
        markStatsDirty();
        return new StatHandler(compound);
    }

    private static @NotNull StatHandler getServerStats() {
        return new StatHandler(getOrCreateCompound(file(), SERVER_SUB_KEY));
    }
}
