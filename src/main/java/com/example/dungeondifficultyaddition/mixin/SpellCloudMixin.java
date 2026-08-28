package com.example.dungeondifficultyaddition.mixin;

import com.example.dungeondifficultyaddition.RelicEffectScaling;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.delivery.CloudPlacer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.spell_engine.internals.SpellExecution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CloudPlacer.class, remap = false)
public abstract class SpellCloudMixin {
    @Inject(method = "placeCloud", at = @At("HEAD"), remap = false)
    private static void dungeonDifficultyAddition$captureCloudRelicLevel(
            World world,
            LivingEntity caster,
            Entity target,
            Vec3d position,
            RegistryEntry<Spell> spellEntry,
            SpellExecution.ImpactContext context,
            CallbackInfo ci
    ) {
        RelicEffectScaling.push(caster instanceof PlayerEntity player ? player : null, spellEntry);
    }

    @Inject(method = "placeCloud", at = @At("RETURN"), remap = false)
    private static void dungeonDifficultyAddition$clearCloudRelicLevel(
            World world,
            LivingEntity caster,
            Entity target,
            Vec3d position,
            RegistryEntry<Spell> spellEntry,
            SpellExecution.ImpactContext context,
            CallbackInfo ci
    ) {
        RelicEffectScaling.pop();
    }

    @Redirect(
            method = "placeCloud",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/spell_engine/api/spell/Spell$Delivery$Cloud;time_to_live_seconds:F"
            ),
            remap = false
    )
    private static float ddJewelryCompat$scaleCloudDuration(Spell.Delivery.Cloud cloud) {
        return RelicEffectScaling.scaledValue(RelicEffectScaling.DURATION, cloud.time_to_live_seconds);
    }
}
