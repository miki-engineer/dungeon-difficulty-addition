package com.miki.dungeondifficultyaddition.mixin;

import com.miki.dungeondifficultyaddition.RelicEffectScaling;
import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellTriggers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpellTriggers.class, remap = false)
public abstract class SpellTriggersMixin {
    @Inject(method = "evaluateTrigger", at = @At("HEAD"), remap = false)
    private static void dungeonDifficultyAddition$captureTriggerRelicLevel(
            RegistryEntry<Spell> spellEntry,
            Spell.Trigger trigger,
            SpellTriggers.Event event,
            CallbackInfoReturnable<Boolean> cir
    ) {
        RelicEffectScaling.push(event.player, spellEntry);
    }

    @Inject(method = "evaluateTrigger", at = @At("RETURN"), remap = false)
    private static void dungeonDifficultyAddition$clearTriggerRelicLevel(
            RegistryEntry<Spell> spellEntry,
            Spell.Trigger trigger,
            SpellTriggers.Event event,
            CallbackInfoReturnable<Boolean> cir
    ) {
        RelicEffectScaling.pop();
    }

    @Redirect(
            method = "evaluateTrigger",
            at = @At(value = "FIELD", target = "Lnet/spell_engine/api/spell/Spell$Trigger;chance:F"),
            remap = false
    )
    private static float dungeonDifficultyAddition$scaleProcChance(Spell.Trigger trigger) {
        return RelicEffectScaling.scaledProcChance(trigger.chance);
    }
}
