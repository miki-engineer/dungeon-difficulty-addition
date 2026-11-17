package com.miki.dungeondifficultyaddition.gametest;

import com.miki.dungeondifficultyaddition.DungeonDifficultyAddition;
import com.miki.dungeondifficultyaddition.readiness.EquippedLevels;
import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Gear score weapon slot behaviour, with a fake player scrolling through a real hotbar. */
@GameTestHolder(DungeonDifficultyAddition.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReadinessGameTests {
    private ReadinessGameTests() {}

    private static ItemStack levelled(Item item, int level) {
        var stack = new ItemStack(item);
        AccessoryItemScaling.applyCommandLevel(stack, level);
        return stack;
    }

    private static String countedWeapon(PlayerEntity player) {
        return EquippedLevels.slots(player).stream().filter(slot -> slot.name().equals("weapon"))
                .findFirst().map(EquippedLevels.Slot::item).orElse("missing");
    }

    private static void select(PlayerEntity player, int hotbarSlot) {
        player.getInventory().selectedSlot = hotbarSlot;
        EquippedLevels.trackWeapon(player);
    }

    @GameTest(templateName = "test_empty")
    public static void toolsOnlyRaiseTheWeaponSlot(TestContext context) {
        var player = FakePlayerFactory.getMinecraft(context.getWorld());
        var inventory = player.getInventory();
        for (int slot = 0; slot < 9; slot++) inventory.setStack(slot, ItemStack.EMPTY);
        inventory.setStack(0, levelled(Items.IRON_SWORD, 3));
        inventory.setStack(1, levelled(Items.DIAMOND_PICKAXE, 5));
        inventory.setStack(2, levelled(Items.IRON_SHOVEL, 2));
        inventory.setStack(3, levelled(Items.IRON_HOE, 3));
        inventory.setStack(4, levelled(Items.IRON_AXE, 1));
        inventory.setStack(5, new ItemStack(Items.BREAD));

        select(player, 0);
        expect(context, player, "minecraft:iron_sword", "Held sword counts");
        select(player, 1);
        expect(context, player, "minecraft:diamond_pickaxe", "A level 5 pickaxe raises it above the level 3 sword");
        select(player, 2);
        expect(context, player, "minecraft:diamond_pickaxe", "A level 2 shovel never lowers it");
        select(player, 5);
        expect(context, player, "minecraft:diamond_pickaxe", "Food keeps the remembered pickaxe");
        select(player, 0);
        expect(context, player, "minecraft:iron_sword", "Scrolling onto a weapon counts it again, even when lower");
        select(player, 3);
        expect(context, player, "minecraft:iron_sword", "A tool at the same level keeps the sword");
        select(player, 4);
        expect(context, player, "minecraft:iron_axe", "A weaker level 1 axe is a weapon, so it counts");
        select(player, 3);
        expect(context, player, "minecraft:iron_hoe", "A level 3 hoe now raises it above the level 1 axe");
        context.complete();
    }

    @GameTest(templateName = "test_empty")
    public static void toolsCountWithoutAnyWeapon(TestContext context) {
        var player = FakePlayerFactory.getMinecraft(context.getWorld());
        var inventory = player.getInventory();
        for (int slot = 0; slot < 9; slot++) inventory.setStack(slot, ItemStack.EMPTY);
        inventory.setStack(0, levelled(Items.IRON_PICKAXE, 2));
        inventory.setStack(1, levelled(Items.DIAMOND_SHOVEL, 4));
        inventory.setStack(2, new ItemStack(Items.TORCH));

        select(player, 0);
        expect(context, player, "minecraft:iron_pickaxe", "With no weapon, a held tool counts");
        select(player, 1);
        expect(context, player, "minecraft:diamond_shovel", "A higher tool replaces it");
        select(player, 0);
        expect(context, player, "minecraft:diamond_shovel", "A lower tool never pushes it down");
        select(player, 2);
        expect(context, player, "minecraft:diamond_shovel", "Holding a torch keeps the best tool counted");
        context.complete();
    }

    private static void expect(TestContext context, PlayerEntity player, String item, String rule) {
        context.assertTrue(countedWeapon(player).equals(item), rule + ": expected " + item + ", got " + countedWeapon(player));
    }
}
