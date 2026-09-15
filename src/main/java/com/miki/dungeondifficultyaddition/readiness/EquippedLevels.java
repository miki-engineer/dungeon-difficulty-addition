package com.miki.dungeondifficultyaddition.readiness;

import com.miki.dungeondifficultyaddition.OptionalModSupport;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.List;

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

    public static List<Slot> slots(PlayerEntity player) {
        var slots = new ArrayList<Slot>();
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            add(slots, slot.getName(), player.getEquippedStack(slot));
        }
        if (OptionalModSupport.isLoaded("curios")) CuriosLevels.append(player, slots);
        return List.copyOf(slots);
    }

    public static int readiness(List<Slot> slots) {
        return ReadinessMath.readiness(slots.stream().map(Slot::level).toList(),
                EncounterConfig.get().player_readiness.required_equipped_items, EncounterConfig.get().max_level);
    }
}
