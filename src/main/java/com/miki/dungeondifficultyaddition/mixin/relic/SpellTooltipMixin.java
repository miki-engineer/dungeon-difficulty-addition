package com.miki.dungeondifficultyaddition.mixin.relic;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.miki.dungeondifficultyaddition.compat.AccessoryFamilies;
import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import com.miki.dungeondifficultyaddition.relic.RelicEffectScaling;
import com.miki.dungeondifficultyaddition.relic.TooltipNumbers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.SpellDataComponents;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.api.spell.tooltip.TooltipTokens;
import net.spell_engine.client.gui.SpellTooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mixin(value = SpellTooltip.class, remap = false)
public abstract class SpellTooltipMixin {
    @Inject(method = "createDescription", at = @At("HEAD"), remap = false)
    private static void dungeonDifficultyAddition$captureTooltipLevel(
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
    private static void dungeonDifficultyAddition$clearTooltipLevel(
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
    private static float dungeonDifficultyAddition$scaleDisplayedEffectDuration(Spell.Impact.Action.StatusEffect statusEffect) {
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
    private static float dungeonDifficultyAddition$scaleDisplayedCloudDuration(Spell.Delivery.Cloud cloud) {
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
            // One decimal instead of truncating to whole percents, in the player's number format.
            cir.setReturnValue(TooltipTokens.formattedNumber(value * 100F) + "%");
        }
    }

    // The details section prints range from the raw field for uncharged spells. Charged spells go
    // through SpellParameters.getRange, which SpellParametersMixin already scales.
    @Inject(method = "addSpellDetails", at = @At("HEAD"), remap = false)
    private static void dungeonDifficultyAddition$captureDetailsLevel(
            RegistryEntry<Spell> spellEntry,
            PlayerEntity player,
            ItemStack sourceStack,
            int indent,
            ArrayList<Text> lines,
            CallbackInfo ci
    ) {
        RelicEffectScaling.push(RelicEffectScaling.level(sourceStack));
    }

    @Inject(method = "addSpellDetails", at = @At("RETURN"), remap = false)
    private static void dungeonDifficultyAddition$clearDetailsLevel(
            RegistryEntry<Spell> spellEntry,
            PlayerEntity player,
            ItemStack sourceStack,
            int indent,
            ArrayList<Text> lines,
            CallbackInfo ci
    ) {
        RelicEffectScaling.pop();
    }

    @Redirect(
            method = "addSpellDetails",
            at = @At(value = "FIELD", target = "Lnet/spell_engine/api/spell/Spell;range:F"),
            remap = false
    )
    private static float dungeonDifficultyAddition$scaleDisplayedRange(Spell spell) {
        return RelicEffectScaling.scaledValue(RelicEffectScaling.RANGE, spell.range);
    }

    /**
     * Effect stat bonuses are the only spell numbers not scaled where Spell Engine reads them:
     * its effect-token text is cached per token, not per item level. Rewrite just those numbers,
     * only in the lines this call added, formatted exactly as Spell Engine prints them.
     */
    @WrapMethod(method = "addSpellLines", remap = false)
    private static void dungeonDifficultyAddition$scaleEffectNumbers(
            ItemStack stack,
            TooltipType type,
            List<Text> lines,
            Operation<Void> original
    ) {
        int start = lines.size();
        original.call(stack, type, lines);
        if (lines.size() > start && AccessoryScalingConfig.get().show_compat_tooltip) {
            dungeonDifficultyAddition$rewriteEffectNumbers(stack, lines, start);
        }
    }

    private static void dungeonDifficultyAddition$rewriteEffectNumbers(ItemStack stack, List<Text> lines, int start) {
        var level = RelicEffectScaling.level(stack);
        var container = stack.get(SpellDataComponents.SPELL_CONTAINER);
        var world = MinecraftClient.getInstance().world;
        if (level <= 0 || container == null || container.spell_ids().isEmpty() || world == null) {
            return;
        }

        var replacements = new HashMap<String, String>();
        var spellRegistry = SpellRegistry.from(world);
        for (var spellIdString : container.spell_ids()) {
            var spellId = Identifier.tryParse(spellIdString);
            if (spellId == null || !AccessoryFamilies.relics(spellId.getNamespace())) {
                continue;
            }
            var spell = spellRegistry.get(spellId);
            if (spell == null) {
                continue;
            }
            if (spell.impacts != null) {
                for (var impact : spell.impacts) {
                    if (impact != null && impact.action != null && impact.action.status_effect != null) {
                        var effect = impact.action.status_effect;
                        dungeonDifficultyAddition$addEffectReplacements(replacements, effect.effect_id, effect.amplifier, level);
                    }
                }
            }
            if (spell.deliver != null && spell.deliver.stash_effect != null) {
                var stash = spell.deliver.stash_effect;
                dungeonDifficultyAddition$addEffectReplacements(replacements, stash.id, stash.amplifier, level);
            }
        }
        if (replacements.isEmpty()) {
            return;
        }
        for (int index = start; index < lines.size(); index++) {
            lines.set(index, dungeonDifficultyAddition$rewriteLine(lines.get(index), replacements));
        }
    }

    // Same source and formatter as Spell Engine's effect tokens, and the same scaling and
    // namespace check StatusEffectMixin applies when the effect lands.
    private static void dungeonDifficultyAddition$addEffectReplacements(
            Map<String, String> replacements,
            String effectId,
            int amplifier,
            int level
    ) {
        var id = effectId == null ? null : Identifier.tryParse(effectId);
        if (id == null || !AccessoryFamilies.relics(id.getNamespace())) {
            return;
        }
        var effect = Registries.STATUS_EFFECT.get(id);
        if (effect == null) {
            return;
        }
        // Description tokens may name any amplifier up to the spell's own.
        for (int tier = 0; tier <= Math.max(0, amplifier); tier++) {
            effect.forEachAttributeModifier(tier, (attribute, modifier) -> {
                var attributeId = attribute.getKey().map(key -> key.getValue().toString()).orElse("");
                var scaled = RelicEffectScaling.scaledValue(attributeId, modifier.value(), level);
                var originalText = TooltipTokens.bonus((float) modifier.value(), modifier.operation());
                var scaledText = TooltipTokens.bonus((float) scaled, modifier.operation());
                if (!originalText.isEmpty() && !originalText.equals(scaledText)) {
                    replacements.putIfAbsent(originalText, scaledText);
                }
            });
        }
    }

    // Rewrite each styled segment separately so colors and formatting inside the line survive.
    private static Text dungeonDifficultyAddition$rewriteLine(Text line, Map<String, String> replacements) {
        var rewritten = Text.empty();
        var changed = new boolean[1];
        line.visit((style, part) -> {
            var replaced = TooltipNumbers.replaceAll(part, replacements);
            changed[0] |= !replaced.equals(part);
            rewritten.append(Text.literal(replaced).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return changed[0] ? rewritten : line;
    }
}
