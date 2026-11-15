package com.miki.dungeondifficultyaddition.mixin.loot;

import com.miki.dungeondifficultyaddition.scaling.LevelBonus;
import net.dungeon_difficulty.logic.PatternMatching;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A spawner in a level-1 zone keeps vanilla settings. Dungeon Difficulty's spawner mixin is the only
 * reader of this accessor (six multipliers × level), so shifting it here covers every spawner setting.
 */
@Mixin(value = PatternMatching.SpawnerScaleResult.class, remap = false)
public abstract class SpawnerLevelBonusMixin {
    @Inject(method = "level", at = @At("RETURN"), cancellable = true, remap = false)
    private void dungeonDifficultyAddition$bonusLevels(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(LevelBonus.bonusLevels(cir.getReturnValueI()));
    }
}
