package com.miki.dungeondifficultyaddition.mixin;

import com.miki.dungeondifficultyaddition.RelicEffectScaling;
import net.spell_engine.api.spell.Spell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Spell.AreaImpact.class, remap = false)
public abstract class SpellAreaImpactMixin {
    @Inject(method = "combinedRadius", at = @At("RETURN"), cancellable = true, remap = false)
    private void dungeonDifficultyAddition$scaleRelicRadius(
            double power,
            CallbackInfoReturnable<Float> cir
    ) {
        var level = RelicEffectScaling.currentLevel();
        if (level > 0) {
            cir.setReturnValue((float) RelicEffectScaling.scaledValue(
                    RelicEffectScaling.RADIUS, cir.getReturnValue(), level
            ));
        }
    }
}
