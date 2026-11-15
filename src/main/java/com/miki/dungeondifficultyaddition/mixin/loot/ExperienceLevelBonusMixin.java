package com.miki.dungeondifficultyaddition.mixin.loot;

import com.miki.dungeondifficultyaddition.scaling.LevelBonus;
import net.dungeon_difficulty.logic.ExperienceScaling;
import net.dungeon_difficulty.logic.PatternMatching;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Mob XP is multiplied by (1 + multiplier × level); with the level-1 rule on, level 1 drops vanilla XP. */
@Mixin(value = ExperienceScaling.class, remap = false)
public abstract class ExperienceLevelBonusMixin {
    // The level is read exactly once, inside the XP multiplier.
    @Redirect(
            method = "scale",
            at = @At(value = "INVOKE",
                    target = "Lnet/dungeon_difficulty/logic/PatternMatching$EntityScaleResult;level()I"),
            remap = false
    )
    private static int dungeonDifficultyAddition$bonusLevels(PatternMatching.EntityScaleResult result) {
        return LevelBonus.bonusLevels(result.level());
    }
}
