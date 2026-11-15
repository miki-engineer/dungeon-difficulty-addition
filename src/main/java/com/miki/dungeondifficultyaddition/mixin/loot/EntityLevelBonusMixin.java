package com.miki.dungeondifficultyaddition.mixin.loot;

import com.miki.dungeondifficultyaddition.scaling.LevelBonus;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.EntityScaling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Through LevelBonus: with the level-1 rule on, a level-1 mob has vanilla stats, matching equipment. */
@Mixin(value = EntityScaling.class, remap = false)
public abstract class EntityLevelBonusMixin {
    // Dungeon Difficulty's only level-to-stat step for mobs: every attribute bonus is rolled here.
    @Redirect(
            method = "apply",
            at = @At(value = "INVOKE",
                    target = "Lnet/dungeon_difficulty/config/Config$AttributeModifier;randomizedValue(I)F"),
            remap = false
    )
    private static float dungeonDifficultyAddition$levelOneIsBase(Config.AttributeModifier modifier, int level) {
        return LevelBonus.randomizedValue(modifier, level);
    }
}
