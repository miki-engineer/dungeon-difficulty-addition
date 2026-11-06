package com.miki.dungeondifficultyaddition.mixin.loot;

import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;
import com.miki.dungeondifficultyaddition.scaling.LevelBonus;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies to every item roll, so unlike the accessory hooks it must not depend on Jewelry or Relics. */
@Mixin(value = ItemScaling.class, remap = false)
public abstract class LevelBonusMixin {
    // Every item roll (loot, fixed, manual, anvil, Legendary Monsters) passes through here; mob
    // scaling uses EntityScaling and keeps Dungeon Difficulty's own level formula.
    @Redirect(
            method = "applyModifiersForItemStack",
            at = @At(value = "INVOKE",
                    target = "Lnet/dungeon_difficulty/config/Config$AttributeModifier;randomizedValue(I)F"),
            remap = false
    )
    private static float dungeonDifficultyAddition$levelOneIsBase(Config.AttributeModifier modifier, int level) {
        return LevelBonus.randomizedValue(modifier, level);
    }

    // Dungeon Difficulty marks a stack only right after rolling it (rescale removes, then re-rolls).
    @Inject(method = "markAsScaled", at = @At("TAIL"), remap = false)
    private static void dungeonDifficultyAddition$markCurrentFormula(ItemStack stack, int level, CallbackInfo ci) {
        AccessoryItemScaling.markCurrentFormula(stack);
    }
}
