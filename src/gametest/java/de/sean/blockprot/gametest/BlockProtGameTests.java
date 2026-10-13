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

package de.sean.blockprot.gametest;

import de.sean.blockprot.fabric.inventories.BlockLockInventory;
import de.sean.blockprot.fabric.inventories.GuiMenu;
import de.sean.blockprot.fabric.inventories.RedstoneSettingsInventory;
import de.sean.blockprot.fabric.nbt.BlockAccessFlag;
import de.sean.blockprot.fabric.nbt.BlockNBTHandler;
import de.sean.blockprot.fabric.nbt.BlockProtAttachments;
import de.sean.blockprot.fabric.util.BlockRef;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

public class BlockProtGameTests {
    private static final UUID SOMEONE = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 8; x++)
            for (int z = 0; z < 8; z++)
                helper.setBlock(x, 0, z, Blocks.STONE);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        @SuppressWarnings("removal") final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return player;
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos relative) {
        final BlockPos pos = helper.absolutePos(relative);
        return new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
    }

    private static BlockNBTHandler handler(GameTestHelper helper, BlockPos relative) {
        return new BlockNBTHandler(new BlockRef(helper.getLevel(), helper.absolutePos(relative)));
    }

    private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, relative));
    }

    @GameTest
    public void lockOnPlaceAndProtect(GameTestHelper helper) {
        floor(helper);
        final ServerPlayer owner = player(helper);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST));
        use(helper, owner, new BlockPos(1, 0, 1));
        final BlockPos chest = new BlockPos(1, 1, 1);
        helper.assertBlockPresent(Blocks.CHEST, chest);
        helper.assertTrue(handler(helper, chest).isOwner(owner.getUUID()), "The placed chest was not locked by its owner");

        final ServerPlayer stranger = player(helper);
        helper.assertFalse(stranger.gameMode.destroyBlock(helper.absolutePos(chest)), "A stranger broke the locked chest");
        helper.assertBlockPresent(Blocks.CHEST, chest);
        helper.assertTrue(use(helper, stranger, chest) == InteractionResult.FAIL, "A stranger could use the locked chest");
        helper.assertFalse(stranger.containerMenu instanceof ChestMenu, "A stranger opened the locked chest");

        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        use(helper, owner, chest);
        helper.assertTrue(owner.containerMenu instanceof ChestMenu, "The owner could not open the chest");
        owner.closeContainer();

        helper.assertTrue(owner.gameMode.destroyBlock(helper.absolutePos(chest)), "The owner could not break the chest");
        helper.succeed();
    }

    @GameTest
    public void readOnlyFriend(GameTestHelper helper) {
        floor(helper);
        final BlockPos chest = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        final ServerPlayer friend = player(helper);
        final BlockNBTHandler handler = handler(helper, chest);
        handler.setOwner(SOMEONE.toString());
        handler.addFriend(friend.getUUID().toString());
        handler.getFriend(friend.getUUID().toString()).orElseThrow().setAccessFlags(EnumSet.of(BlockAccessFlag.READ));
        final ChestBlockEntity chestEntity = helper.getBlockEntity(chest, ChestBlockEntity.class);
        chestEntity.setItem(0, new ItemStack(Items.DIAMOND, 5));

        use(helper, friend, chest);
        helper.assertTrue(friend.containerMenu instanceof ChestMenu, "A friend could not open the chest");
        friend.containerMenu.clicked(0, 0, ContainerInput.QUICK_MOVE, friend);
        helper.assertTrue(chestEntity.getItem(0).getCount() == 5, "A read-only friend took items");
        friend.closeContainer();

        handler(helper, chest).getFriend(friend.getUUID().toString()).orElseThrow()
            .setAccessFlags(EnumSet.of(BlockAccessFlag.READ, BlockAccessFlag.WRITE));
        use(helper, friend, chest);
        friend.containerMenu.clicked(0, 0, ContainerInput.QUICK_MOVE, friend);
        helper.assertTrue(chestEntity.getItem(0).isEmpty(), "A friend with write access could not take items");
        friend.closeContainer();
        helper.succeed();
    }

    @GameTest
    public void lockMenu(GameTestHelper helper) {
        floor(helper);
        final BlockPos chest = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        final ServerPlayer owner = player(helper);
        handler(helper, chest).setOwner(owner.getUUID().toString());

        owner.setShiftKeyDown(true);
        helper.assertTrue(use(helper, owner, chest) == InteractionResult.SUCCESS, "Crouching did not open the menu");
        helper.assertTrue(owner.containerMenu instanceof GuiMenu menu && menu.getHolder() instanceof BlockLockInventory,
            "The lock menu did not open");
        helper.assertTrue(owner.containerMenu.getSlot(1).getItem().is(Items.REDSTONE), "The redstone button is missing");

        // Open the redstone settings.
        owner.containerMenu.clicked(1, 0, ContainerInput.PICKUP, owner);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(owner.containerMenu instanceof GuiMenu menu && menu.getHolder() instanceof RedstoneSettingsInventory,
                "The redstone menu did not open");
            // Toggle the redstone protection and go back.
            owner.containerMenu.clicked(0, 0, ContainerInput.PICKUP, owner);
            helper.runAfterDelay(1, () -> {
                owner.containerMenu.clicked(17, 0, ContainerInput.PICKUP, owner);
                helper.runAfterDelay(1, () -> {
                    helper.assertFalse(handler(helper, chest).getRedstoneHandler().getCurrentProtection(),
                        "The redstone setting was not saved");
                    helper.assertTrue(owner.containerMenu instanceof GuiMenu menu && menu.getHolder() instanceof BlockLockInventory,
                        "The back button did not return to the lock menu");
                    // Unlock the chest.
                    owner.containerMenu.clicked(0, 0, ContainerInput.PICKUP, owner);
                    helper.runAfterDelay(1, () -> {
                        helper.assertTrue(handler(helper, chest).isNotProtected(), "The unlock button did not unlock the chest");
                        helper.assertTrue(owner.containerMenu == owner.inventoryMenu, "The menu did not close after unlocking");
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(maxTicks = 100)
    public void hopperProtection(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(1, 1, 1, Blocks.HOPPER);
        helper.setBlock(1, 2, 1, Blocks.CHEST);
        helper.setBlock(4, 1, 1, Blocks.HOPPER);
        helper.setBlock(4, 2, 1, Blocks.CHEST);
        handler(helper, new BlockPos(1, 2, 1)).setOwner(SOMEONE.toString());
        helper.getBlockEntity(new BlockPos(1, 2, 1), ChestBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND));
        helper.getBlockEntity(new BlockPos(4, 2, 1), ChestBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND));

        helper.runAfterDelay(40, () -> {
            helper.assertFalse(helper.getBlockEntity(new BlockPos(1, 2, 1), ChestBlockEntity.class).getItem(0).isEmpty(),
                "A hopper took items out of a locked chest");
            helper.assertTrue(helper.getBlockEntity(new BlockPos(4, 2, 1), ChestBlockEntity.class).getItem(0).isEmpty(),
                "A hopper did not take items out of an unlocked chest");
            helper.assertFalse(helper.getBlockEntity(new BlockPos(4, 1, 1), HopperBlockEntity.class).isEmpty(),
                "The hopper below the unlocked chest is empty");
            helper.succeed();
        });
    }

    @GameTest
    public void explosionProtection(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(2, 1, 2, Blocks.CHEST);
        helper.setBlock(4, 1, 2, Blocks.CHEST);
        handler(helper, new BlockPos(2, 1, 2)).setOwner(SOMEONE.toString());
        final BlockPos center = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.getLevel().explode(null, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.TNT);
        helper.assertBlockPresent(Blocks.CHEST, new BlockPos(2, 1, 2));
        helper.assertBlockNotPresent(Blocks.CHEST, new BlockPos(4, 1, 2));
        helper.succeed();
    }

    @GameTest
    public void doorRedstoneAndCleanup(GameTestHelper helper) {
        floor(helper);
        for (int x : new int[]{1, 4}) {
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            helper.setBlock(new BlockPos(x, 2, 1), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }
        final BlockNBTHandler lockedDoor = handler(helper, new BlockPos(1, 1, 1));
        lockedDoor.setOwner(SOMEONE.toString());
        lockedDoor.applyToOtherContainer();
        helper.assertTrue(handler(helper, new BlockPos(1, 2, 1)).isProtected(), "The upper door half is not protected");

        helper.setBlock(2, 1, 1, Blocks.REDSTONE_BLOCK);
        helper.setBlock(5, 1, 1, Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(2, () -> {
            helper.assertFalse(helper.getBlockState(new BlockPos(1, 1, 1)).getValue(DoorBlock.OPEN), "Redstone opened a locked door");
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, 1)).getValue(DoorBlock.OPEN), "Redstone did not open an unlocked door");

            // Removing the door removes its data, so a new door at the same place isn't locked.
            helper.setBlock(1, 2, 1, Blocks.AIR);
            helper.setBlock(1, 1, 1, Blocks.AIR);
            final CompoundTag blocks = helper.getLevel().getChunkAt(helper.absolutePos(new BlockPos(1, 1, 1)))
                .getAttached(BlockProtAttachments.CHUNK_BLOCKS);
            helper.assertTrue(blocks == null || !blocks.contains(Long.toString(helper.absolutePos(new BlockPos(1, 1, 1)).asLong())),
                "The data of a removed door was kept");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void pistonAndFallingProtection(GameTestHelper helper) {
        floor(helper);
        // Anvils are immovable in vanilla, so pistons are tested with fence gates.
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(2, 1, 1, Blocks.OAK_FENCE_GATE);
        handler(helper, new BlockPos(2, 1, 1)).setOwner(SOMEONE.toString());

        helper.setBlock(5, 3, 1, Blocks.ANVIL);
        handler(helper, new BlockPos(5, 3, 1)).setOwner(SOMEONE.toString());

        // The same setup without protection, which has to move.
        helper.setBlock(new BlockPos(1, 1, 4), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(2, 1, 4, Blocks.OAK_FENCE_GATE);
        helper.setBlock(5, 3, 4, Blocks.ANVIL);

        helper.setBlock(0, 1, 1, Blocks.REDSTONE_BLOCK);
        helper.setBlock(0, 1, 4, Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(30, () -> {
            helper.assertBlockPresent(Blocks.OAK_FENCE_GATE, new BlockPos(2, 1, 1));
            helper.assertBlockPresent(Blocks.ANVIL, new BlockPos(5, 3, 1));
            helper.assertBlockPresent(Blocks.OAK_FENCE_GATE, new BlockPos(3, 1, 4));
            // Landing can randomly damage the anvil into a chipped one.
            helper.assertTrue(helper.getBlockState(new BlockPos(5, 1, 4)).is(BlockTags.ANVIL),
                "The unprotected anvil did not fall");
            helper.succeed();
        });
    }

    @GameTest
    public void blockEntityDataIsSaved(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(1, 1, 1, Blocks.CHEST);
        handler(helper, new BlockPos(1, 1, 1)).setOwner(SOMEONE.toString());
        final CompoundTag saved = helper.getBlockEntity(new BlockPos(1, 1, 1), ChestBlockEntity.class)
            .saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(saved.toString().contains(SOMEONE.toString()), "The protection is not part of the saved chest: " + saved);
        helper.succeed();
    }
}
