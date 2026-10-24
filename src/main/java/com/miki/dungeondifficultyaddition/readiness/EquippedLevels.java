package com.miki.dungeondifficultyaddition.readiness;

import com.miki.dungeondifficultyaddition.compat.OptionalModSupport;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EquippedLevels {
    public record Slot(String name, String item, int level) {}
    private EquippedLevels() {}

    public static int level(ItemStack stack) {
        var config = EncounterConfig.get();
        if (stack.isEmpty()) return 0;
        return ReadinessMath.clamp(ItemScaling.isScaled(stack) ? ItemScaling.getScaleFactor(stack)
                : config.player_readiness.unmarked_item_level, config.max_level);
    }

    static void add(List<Slot> slots, String name, ItemStack stack) {
        if (!stack.isEmpty()) slots.add(new Slot(name, Registries.ITEM.getId(stack.getItem()).toString(), level(stack)));
    }

    /** All four armor slots (empty ones as level 0) plus occupied active Curios slots. */
    public static List<Slot> slots(PlayerEntity player) {
        var slots = new ArrayList<Slot>();
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            var stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) slots.add(new Slot(slot.getName(), "empty", 0));
            else add(slots, slot.getName(), stack);
        }
        if (OptionalModSupport.isLoaded("curios")) CuriosLevels.append(player, slots);
        return List.copyOf(slots);
    }

    public static double gearScore(List<Slot> slots) {
        return ReadinessMath.gearScore(slots.stream().map(Slot::level).toList(), EncounterConfig.get().max_level);
    }

    public static String format(double gearScore) {
        return String.format(Locale.ROOT, "%.1f", gearScore);
    }
}
