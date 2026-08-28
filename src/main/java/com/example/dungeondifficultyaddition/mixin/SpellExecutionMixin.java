package com.example.dungeondifficultyaddition.mixin;

import com.example.dungeondifficultyaddition.RelicEffectScaling;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.target.SpellTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpellExecution.class, remap = false)
public abstract class SpellExecutionMixin {
    @Inject(method = "performSpell", at = @At("HEAD"), remap = false)
    private static void ddJewelryCompat$captureRelicLevel(
            World world,
            PlayerEntity player,
            RegistryEntry<Spell> spellEntry,
            SpellTarget.SearchResult target,
            SpellCast.Action action,
            float progress,
            CallbackInfo ci
    ) {
        RelicEffectScaling.push(player, spellEntry);
    }

    @Inject(method = "performSpell", at = @At("RETURN"), remap = false)
    private static void ddJewelryCompat$clearRelicLevel(
            World world,
            PlayerEntity player,
            RegistryEntry<Spell> spellEntry,
            SpellTarget.SearchResult target,
            SpellCast.Action action,
            float progress,
            CallbackInfo ci
    ) {
        RelicEffectScaling.pop();
    }
}
