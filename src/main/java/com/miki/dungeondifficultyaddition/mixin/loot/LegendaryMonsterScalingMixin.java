package com.miki.dungeondifficultyaddition.mixin.loot;

import com.miki.dungeondifficultyaddition.compat.LegendaryMonsterAttributes;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

/** Shared by DD loot scaling, commands, fixed levels, and anvil upgrades. */
@Mixin(value = ItemScaling.class, remap = false)
public abstract class LegendaryMonsterScalingMixin {
    @Inject(method = "applyModifiersForItemStack", at = @At("HEAD"), remap = false)
    private static void prepare(List<AttributeModifierSlot> slots, String id, ItemStack stack,
                                List<Config.AttributeModifier> modifiers, int level, CallbackInfo ci) {
        if (level > 0 && !modifiers.isEmpty()) LegendaryMonsterAttributes.prepare(stack);
    }

    @Inject(method = "applyModifiersForItemStack", at = @At("TAIL"), remap = false)
    private static void mark(List<AttributeModifierSlot> slots, String id, ItemStack stack,
                             List<Config.AttributeModifier> modifiers, int level, CallbackInfo ci) {
        LegendaryMonsterAttributes.markRepaired(stack);
    }
}
