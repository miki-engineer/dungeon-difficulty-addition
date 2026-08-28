package com.example.dungeondifficultyaddition.mixin;

import com.example.dungeondifficultyaddition.RelicEffectScaling;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.impact.SpellImpacts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

@Mixin(value = SpellImpacts.class, remap = false)
public abstract class SpellImpactsMixin {
    private static final String AREA_IMPACT_METHOD =
            "lookupAndPerformAreaImpact(Lnet/spell_engine/api/spell/Spell$AreaImpact;"
                    + "Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;"
                    + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;"
                    + "Ljava/util/List;Lnet/spell_engine/internals/SpellExecution$ImpactContext;"
                    + "ZLjava/lang/Float;)Z";

    @Inject(method = AREA_IMPACT_METHOD, at = @At("HEAD"), remap = false)
    private static void dungeonDifficultyAddition$captureAreaRelicLevel(
            Spell.AreaImpact areaImpact,
            RegistryEntry<Spell> spellEntry,
            LivingEntity caster,
            Entity target,
            Entity aoeSource,
            List<Spell.Impact> impacts,
            SpellExecution.ImpactContext context,
            boolean direct,
            Float radiusOverride,
            CallbackInfoReturnable<Boolean> cir
    ) {
        RelicEffectScaling.push(caster instanceof PlayerEntity player ? player : null, spellEntry);
    }

    @Inject(method = AREA_IMPACT_METHOD, at = @At("RETURN"), remap = false)
    private static void dungeonDifficultyAddition$clearAreaRelicLevel(
            Spell.AreaImpact areaImpact,
            RegistryEntry<Spell> spellEntry,
            LivingEntity caster,
            Entity target,
            Entity aoeSource,
            List<Spell.Impact> impacts,
            SpellExecution.ImpactContext context,
            boolean direct,
            Float radiusOverride,
            CallbackInfoReturnable<Boolean> cir
    ) {
        RelicEffectScaling.pop();
    }

    @Inject(method = "performImpact", at = @At("HEAD"), remap = false)
    private static void ddJewelryCompat$captureImpactRelicLevel(
            World world,
            LivingEntity caster,
            Entity target,
            RegistryEntry<Spell> spellEntry,
            Spell.Impact impact,
            SpellExecution.ImpactContext context,
            Collection<ServerPlayerEntity> trackers,
            CallbackInfoReturnable<Boolean> cir
    ) {
        RelicEffectScaling.push(caster instanceof PlayerEntity player ? player : null, spellEntry);
    }

    @Inject(method = "performImpact", at = @At("RETURN"), remap = false)
    private static void ddJewelryCompat$clearImpactRelicLevel(
            World world,
            LivingEntity caster,
            Entity target,
            RegistryEntry<Spell> spellEntry,
            Spell.Impact impact,
            SpellExecution.ImpactContext context,
            Collection<ServerPlayerEntity> trackers,
            CallbackInfoReturnable<Boolean> cir
    ) {
        RelicEffectScaling.pop();
    }

    @Redirect(
            method = "performImpact",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/spell_engine/api/spell/Spell$Impact$Action$Damage;spell_power_coefficient:F"
            ),
            remap = false
    )
    private static float ddJewelryCompat$scaleDamage(Spell.Impact.Action.Damage damage) {
        return RelicEffectScaling.scaledValue(RelicEffectScaling.DAMAGE, damage.spell_power_coefficient);
    }

    @Redirect(
            method = "performImpact",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/spell_engine/api/spell/Spell$Impact$Action$Heal;spell_power_coefficient:F"
            ),
            remap = false
    )
    private static float ddJewelryCompat$scaleHealing(Spell.Impact.Action.Heal heal) {
        return RelicEffectScaling.scaledValue(RelicEffectScaling.HEALING, heal.spell_power_coefficient);
    }

    @Redirect(
            method = "performImpact",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/spell_engine/api/spell/Spell$Impact$Action$StatusEffect;duration:F"
            ),
            remap = false
    )
    private static float ddJewelryCompat$scaleEffectDuration(Spell.Impact.Action.StatusEffect statusEffect) {
        return RelicEffectScaling.scaledValue(RelicEffectScaling.DURATION, statusEffect.duration);
    }
}
