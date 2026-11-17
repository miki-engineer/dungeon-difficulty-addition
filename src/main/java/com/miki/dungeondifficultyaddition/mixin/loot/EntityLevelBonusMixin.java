package com.miki.dungeondifficultyaddition.mixin.loot;

import com.miki.dungeondifficultyaddition.scaling.LevelBonus;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.EntityScaling;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Through LevelBonus: with the level-1 rule on, a level-1 mob has vanilla stats, matching equipment. */
@Mixin(value = EntityScaling.class, remap = false)
public abstract class EntityLevelBonusMixin {
    /** Name Dungeon Difficulty gives the result of PerPlayerDifficulty.getAttributeModifiers. */
    private static final String PER_PLAYER = "per_player";

    // Dungeon Difficulty's only level-to-stat step for mobs: every attribute bonus is rolled here.
    // apply() also runs for per-player difficulty, whose level is already min(players, cap) - 1:
    // one player is no bonus. Shifting it again would cost every multiplayer group one step.
    @Redirect(
            method = "apply",
            at = @At(value = "INVOKE",
                    target = "Lnet/dungeon_difficulty/config/Config$AttributeModifier;randomizedValue(I)F"),
            remap = false
    )
    private static float dungeonDifficultyAddition$levelOneIsBase(
            Config.AttributeModifier modifier,
            int level,
            PatternMatching.EntityScaleResult result,
            LivingEntity entity
    ) {
        if (PER_PLAYER.equals(result.name())) {
            return modifier.randomizedValue(level);
        }
        return LevelBonus.randomizedValue(modifier, level);
    }
}
