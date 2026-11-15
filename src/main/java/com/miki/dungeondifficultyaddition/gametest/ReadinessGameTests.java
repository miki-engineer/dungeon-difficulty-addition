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
    public static void scrollingPastToolsKeepsTheLastWeapon(TestContext context) {
        var player = FakePlayerFactory.getMinecraft(context.getWorld());
        var inventory = player.getInventory();
        inventory.setStack(0, levelled(Items.IRON_SWORD, 3));
        inventory.setStack(1, levelled(Items.DIAMOND_PICKAXE, 5));
        inventory.setStack(2, levelled(Items.IRON_SHOVEL, 5));
        inventory.setStack(3, levelled(Items.IRON_HOE, 5));
        inventory.setStack(4, levelled(Items.IRON_AXE, 4));

        select(player, 0);
        context.assertTrue(countedWeapon(player).equals("minecraft:iron_sword"), "Held sword counts");
        for (int tool = 1; tool <= 3; tool++) {
            select(player, tool);
            context.assertTrue(countedWeapon(player).equals("minecraft:iron_sword"),
                    "Scrolling to " + inventory.getStack(tool).getItem() + " keeps the sword, got " + countedWeapon(player));
        }
        select(player, 4);
        context.assertTrue(countedWeapon(player).equals("minecraft:iron_axe"), "Axes are weapons, got " + countedWeapon(player));
        context.complete();
    }
}
