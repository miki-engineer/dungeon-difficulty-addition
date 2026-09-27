package com.miki.dungeondifficultyaddition.mixin.relic;

import com.miki.dungeondifficultyaddition.relic.RelicEffectScaling;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.delivery.SpellDelivery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Consumer;

@Mixin(value = SpellDelivery.class, remap = false)
public abstract class SpellDeliveryMixin {
    // Class literals are remapped with the JAR, unlike hard-coded production descriptors.
    @Inject(target = @Desc(value = "deliver", args = {World.class, RegistryEntry.class, LivingEntity.class,
            List.class, SpellExecution.ImpactContext.class, Vec3d.class, Consumer.class, boolean.class, boolean.class},
            ret = boolean.class), at = @At("HEAD"), remap = false)
    private static void dungeonDifficultyAddition$captureDeliveredRelicLevel(
            World world,
            RegistryEntry<Spell> spellEntry,
            LivingEntity caster,
            List<SpellExecution.DeliveryTarget> targets,
            SpellExecution.ImpactContext context,
            Vec3d origin,
            Consumer<SpellExecution.DeliveryCompletion> completion,
            boolean direct,
            boolean batch,
            CallbackInfoReturnable<Boolean> cir
    ) {
        RelicEffectScaling.push(caster instanceof PlayerEntity player ? player : null, spellEntry);
    }

    @Inject(target = @Desc(value = "deliver", args = {World.class, RegistryEntry.class, LivingEntity.class,
            List.class, SpellExecution.ImpactContext.class, Vec3d.class, Consumer.class, boolean.class, boolean.class},
            ret = boolean.class), at = @At("RETURN"), remap = false)
    private static void dungeonDifficultyAddition$clearDeliveredRelicLevel(
            World world,
            RegistryEntry<Spell> spellEntry,
            LivingEntity caster,
            List<SpellExecution.DeliveryTarget> targets,
            SpellExecution.ImpactContext context,
            Vec3d origin,
            Consumer<SpellExecution.DeliveryCompletion> completion,
            boolean direct,
            boolean batch,
            CallbackInfoReturnable<Boolean> cir
    ) {
        RelicEffectScaling.pop();
    }
}
