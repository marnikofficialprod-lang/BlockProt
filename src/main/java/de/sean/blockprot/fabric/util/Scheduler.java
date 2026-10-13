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

import de.sean.blockprot.fabric.BlockProt;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Replacement for the Bukkit scheduler: runs tasks on the server thread at the end of a tick.
 */
public final class Scheduler {
    private static final Queue<Runnable> tasks = new ArrayDeque<>();

    private Scheduler() {
    }

    /**
     * Runs the task at the end of the current tick. Must be called from the server thread.
     */
    public static void runLater(@NotNull final Runnable task) {
        tasks.add(task);
    }

    /**
     * Runs the task on the server thread. Safe to call from any thread.
     */
    public static void runSync(@NotNull final Runnable task) {
        final var server = BlockProt.getServer();
        if (server != null) server.execute(() -> run(task));
    }

    public static void tick() {
        Runnable task;
        // Tasks may schedule other tasks, those also run in this tick.
        while ((task = tasks.poll()) != null) {
            run(task);
        }
    }

    public static void clear() {
        tasks.clear();
    }

    private static void run(@NotNull final Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            BlockProt.LOGGER.error("Error while running a BlockProt task", t);
        }
    }
}
