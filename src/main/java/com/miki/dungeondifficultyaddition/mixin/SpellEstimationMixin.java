package com.miki.dungeondifficultyaddition.mixin;

import com.miki.dungeondifficultyaddition.RelicEffectScaling;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.impact.SpellEstimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpellEstimation.class, remap = false)
public abstract class SpellEstimationMixin {
    @Inject(method = "estimate", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dungeonDifficultyAddition$scaleTooltipEstimates(
            Spell spell,
            PlayerEntity player,
            ItemStack sourceStack,
            CallbackInfoReturnable<SpellEstimation.EstimatedOutput> cir
    ) {
        var level = RelicEffectScaling.level(sourceStack);
        if (level <= 0) {
            return;
        }

        var original = cir.getReturnValue();
        var damage = original.damage().stream()
                .map(value -> new SpellEstimation.EstimatedValue(
                        RelicEffectScaling.scaledValue(RelicEffectScaling.DAMAGE, value.min(), level),
                        RelicEffectScaling.scaledValue(RelicEffectScaling.DAMAGE, value.max(), level)
                ))
                .toList();
        var healing = original.heal().stream()
                .map(value -> new SpellEstimation.EstimatedValue(
                        RelicEffectScaling.scaledValue(RelicEffectScaling.HEALING, value.min(), level),
                        RelicEffectScaling.scaledValue(RelicEffectScaling.HEALING, value.max(), level)
                ))
                .toList();
        cir.setReturnValue(new SpellEstimation.EstimatedOutput(damage, healing));
    }
}
