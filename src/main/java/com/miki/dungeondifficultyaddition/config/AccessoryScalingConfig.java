package com.miki.dungeondifficultyaddition.config;

import net.dungeon_difficulty.config.Config;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import java.util.List;

/** The scaling section of settings.json. Fixed item lists remain in their own file. */
public final class AccessoryScalingConfig {
    public boolean enabled = true;
    public boolean minimum_equipment_level_enabled = true;
    public boolean scale_jewelry = true;
    public boolean scale_relics = true;
    public boolean merge_accessory_modifiers = true;
    public boolean show_compat_tooltip = true;
    public List<Config.AttributeModifier> attributes = List.of(defaultAttributeModifier());

    public static AccessoryScalingConfig get() { return ModSettings.get().scaling(); }
    public static void reload() { ModSettings.reload(); }
    public List<Config.AttributeModifier> modifiers() { return attributes != null ? attributes : List.of(); }
    public int fixedLevel(ItemStack stack) {
        return ModSettings.get().fixedLevels().levelFor(Registries.ITEM.getId(stack.getItem()).toString());
    }
    private static Config.AttributeModifier defaultAttributeModifier() {
        var modifier = new Config.AttributeModifier(".*", 0.1F);
        modifier.operation = Config.Operation.MULTIPLY_BASE;
        modifier.randomness = 0.05F;
        return modifier;
    }
}
