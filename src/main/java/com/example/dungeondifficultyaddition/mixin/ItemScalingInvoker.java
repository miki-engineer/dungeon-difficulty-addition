package com.example.dungeondifficultyaddition.mixin;

import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(value = ItemScaling.class, remap = false)
public interface ItemScalingInvoker {
    @Invoker(value = "applyModifiersForItemStack", remap = false)
    static void ddJewelryCompat$applyModifiersForItemStack(
            List<AttributeModifierSlot> slots,
            String itemId,
            ItemStack stack,
            List<Config.AttributeModifier> modifiers,
            int level
    ) {
        throw new AssertionError();
    }
}
