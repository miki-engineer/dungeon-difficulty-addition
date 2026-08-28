package com.example.dungeondifficultyaddition.mixin;

import com.example.dungeondifficultyaddition.RelicEffectScaling;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.client.gui.SpellTooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpellTooltip.class, remap = false)
public abstract class SpellTooltipMixin {
    @Inject(method = "createDescription", at = @At("HEAD"), remap = false)
    private static void ddJewelryCompat$captureTooltipLevel(
            RegistryEntry<Spell> spellEntry,
            PlayerEntity player,
            ItemStack sourceStack,
            Spell spell,
            @Coerce Object power,
            CallbackInfoReturnable<String> cir
    ) {
        RelicEffectScaling.push(RelicEffectScaling.level(sourceStack));
    }

    @Inject(method = "createDescription", at = @At("RETURN"), remap = false)
    private static void ddJewelryCompat$clearTooltipLevel(
            RegistryEntry<Spell> spellEntry,
            PlayerEntity player,
            ItemStack sourceStack,
            Spell spell,
            @Coerce Object power,
            CallbackInfoReturnable<String> cir
    ) {
        RelicEffectScaling.pop();
    }

    @Redirect(
            method = "createDescription",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/spell_engine/api/spell/Spell$Impact$Action$StatusEffect;duration:F"
            ),
            remap = false
    )
    private static float ddJewelryCompat$scaleDisplayedEffectDuration(Spell.Impact.Action.StatusEffect statusEffect) {
        return RelicEffectScaling.scaledValue(RelicEffectScaling.DURATION, statusEffect.duration);
    }

    @Redirect(
            method = "createDescription",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/spell_engine/api/spell/Spell$Delivery$Cloud;time_to_live_seconds:F"
            ),
            remap = false
    )
    private static float ddJewelryCompat$scaleDisplayedCloudDuration(Spell.Delivery.Cloud cloud) {
        return RelicEffectScaling.scaledValue(RelicEffectScaling.DURATION, cloud.time_to_live_seconds);
    }

    @Redirect(
            method = "createDescription",
            at = @At(value = "FIELD", target = "Lnet/spell_engine/api/spell/Spell$Trigger;chance:F"),
            remap = false
    )
    private static float dungeonDifficultyAddition$scaleDisplayedProcChance(Spell.Trigger trigger) {
        return RelicEffectScaling.scaledProcChance(trigger.chance);
    }

    @Inject(method = "percent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dungeonDifficultyAddition$compactRelicPercent(
            float value,
            CallbackInfoReturnable<String> cir
    ) {
        if (RelicEffectScaling.currentLevel() > 0) {
            cir.setReturnValue(RelicEffectScaling.compactNumber(value * 100D) + "%");
        }
    }
}
