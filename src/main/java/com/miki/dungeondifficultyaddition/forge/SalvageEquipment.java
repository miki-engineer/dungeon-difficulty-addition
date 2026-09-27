package com.miki.dungeondifficultyaddition.forge;

import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class SalvageEquipment {
    private SalvageEquipment() {}
    public static GemKind kind(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof LevelGemItem
                || !ForgeRules.validLevel(ItemScaling.getScaleFactor(stack))
                || !AccessoryItemScaling.isForgeEquipment(stack)) return null;
        // Pack makers can explicitly classify supported equipment without Java changes.
        for (var kind : GemKind.values()) if (stack.isIn(TagKey.of(RegistryKeys.ITEM,
                Identifier.of("dungeon_difficulty_addition", "salvage/" + kind.id())))) return kind;
        if (stack.getItem() instanceof ArmorItem || stack.getItem() instanceof ShieldItem) return GemKind.ARMOR;
        if (AccessoryItemScaling.isSupportedAccessory(stack, AccessoryScalingConfig.get())) return GemKind.ACCESSORY;
        return GemKind.WEAPON;
    }
}
