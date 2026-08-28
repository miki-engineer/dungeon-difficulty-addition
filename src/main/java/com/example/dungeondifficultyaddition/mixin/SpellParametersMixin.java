package com.example.dungeondifficultyaddition.mixin;

import com.example.dungeondifficultyaddition.RelicEffectScaling;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpellParameters.class, remap = false)
public abstract class SpellParametersMixin {
    @Inject(
            method = "getCooldownDuration(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemStack;)F",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void ddJewelryCompat$scaleCooldown(
            LivingEntity caster,
            RegistryEntry<Spell> spellEntry,
            ItemStack sourceStack,
            CallbackInfoReturnable<Float> cir
    ) {
        var level = sourceStack != null && !sourceStack.isEmpty()
                ? RelicEffectScaling.level(sourceStack)
                : RelicEffectScaling.level(caster instanceof PlayerEntity player ? player : null, spellEntry);
        if (level > 0) {
            cir.setReturnValue(RelicEffectScaling.scaledCooldown(cir.getReturnValue(), level));
        }
    }

    @Inject(method = "getRangeCurved", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dungeonDifficultyAddition$scaleRelicRange(
            LivingEntity caster,
            RegistryEntry<Spell> spellEntry,
            float progress,
            CallbackInfoReturnable<Float> cir
    ) {
        var level = RelicEffectScaling.level(caster instanceof PlayerEntity player ? player : null, spellEntry);
        if (level > 0) {
            cir.setReturnValue((float) RelicEffectScaling.scaledValue(
                    RelicEffectScaling.RANGE, cir.getReturnValue(), level
            ));
        }
    }
}
