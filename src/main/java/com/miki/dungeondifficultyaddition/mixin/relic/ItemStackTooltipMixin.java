package com.miki.dungeondifficultyaddition.mixin.relic;

import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import com.miki.dungeondifficultyaddition.compat.AccessoryFamilies;
import com.miki.dungeondifficultyaddition.relic.RelicEffectScaling;
import com.miki.dungeondifficultyaddition.relic.TooltipNumbers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.neoforged.fml.loading.FMLPaths;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.SpellDataComponents;
import net.spell_engine.api.spell.registry.SpellRegistry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {
    private static final Map<String, JsonObject> dungeonDifficultyAddition$relicEffects = new LinkedHashMap<>();
    private static final Map<String, Long> dungeonDifficultyAddition$nextEffectsRead = new LinkedHashMap<>();

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void dungeonDifficultyAddition$appendScalingTooltip(
            Item.TooltipContext context,
            @Nullable PlayerEntity player,
            TooltipType type,
            CallbackInfoReturnable<List<Text>> cir
    ) {
        var config = AccessoryScalingConfig.get();
        if (!config.enabled || !config.show_compat_tooltip) {
            return;
        }

        var stack = (ItemStack) (Object) this;
        var itemId = Registries.ITEM.getId(stack.getItem());
        if (!AccessoryFamilies.accessory(itemId.getNamespace())) {
            return;
        }

        var level = ItemScaling.getScaleFactor(stack);
        if (level <= 0) {
            return;
        }

        dungeonDifficultyAddition$rewriteActiveRelicTooltip(cir.getReturnValue(), stack, player, level);
    }

    private static boolean dungeonDifficultyAddition$rewriteActiveRelicTooltip(
            List<Text> tooltip,
            ItemStack stack,
            @Nullable PlayerEntity player,
            int level
    ) {
        if (player == null) {
            return false;
        }

        var spellContainer = stack.get(SpellDataComponents.SPELL_CONTAINER);
        if (spellContainer == null || spellContainer.spell_ids().isEmpty()) {
            return false;
        }

        var replacements = new LinkedHashMap<String, String>();
        var spellRegistry = SpellRegistry.from(player.getWorld());

        for (var spellIdString : spellContainer.spell_ids()) {
            var spellId = Identifier.of(spellIdString);
            if (!AccessoryFamilies.relics(spellId.getNamespace())) {
                continue;
            }

            var spell = spellRegistry.get(spellId);
            if (spell == null) {
                continue;
            }

            for (var effectId : dungeonDifficultyAddition$effectIds(spell)) {
                dungeonDifficultyAddition$addEffectBonusReplacements(replacements, effectId, level);
            }
            dungeonDifficultyAddition$addSpellValueReplacements(replacements, spell, level);
        }

        if (replacements.isEmpty()) {
            return false;
        }

        var changed = false;
        var itemLevelLine = Text.translatable("item.power.level", level).getString();
        for (var index = 0; index < tooltip.size(); index++) {
            var original = tooltip.get(index);
            // Never rewrite Dungeon Difficulty's level line.
            if (original.getString().equals(itemLevelLine)) {
                continue;
            }
            var rewritten = dungeonDifficultyAddition$replaceFirstKnownBonus(original.getString(), replacements);
            if (rewritten.equals(original.getString())) {
                continue;
            }

            tooltip.set(index, Text.literal(rewritten).setStyle(original.getStyle()));
            changed = true;
        }

        return changed;
    }

    private static List<String> dungeonDifficultyAddition$effectIds(Spell spell) {
        var ids = new ArrayList<String>();
        if (spell.impacts != null) {
            for (var impact : spell.impacts) {
                if (impact != null
                        && impact.action != null
                        && impact.action.status_effect != null
                        && impact.action.status_effect.effect_id != null
                        && !impact.action.status_effect.effect_id.isEmpty()) {
                    ids.add(impact.action.status_effect.effect_id);
                }
            }
        }

        if (spell.deliver != null
                && spell.deliver.stash_effect != null
                && spell.deliver.stash_effect.id != null
                && !spell.deliver.stash_effect.id.isEmpty()) {
            ids.add(spell.deliver.stash_effect.id);
        }

        return ids;
    }

    private static void dungeonDifficultyAddition$addSpellValueReplacements(
            Map<String, String> replacements,
            Spell spell,
            int level
    ) {
        // Spell Engine renders range outside createDescription.
        dungeonDifficultyAddition$addNumberReplacement(
                replacements,
                RelicEffectScaling.RANGE,
                spell.range,
                level
        );

        if (spell.impacts != null) {
            for (var impact : spell.impacts) {
                if (impact == null || impact.action == null) {
                    continue;
                }
                if (impact.action.damage != null) {
                    dungeonDifficultyAddition$addPercentReplacement(
                            replacements,
                            RelicEffectScaling.DAMAGE,
                            impact.action.damage.spell_power_coefficient,
                            level
                    );
                }
                if (impact.action.heal != null) {
                    dungeonDifficultyAddition$addPercentReplacement(
                            replacements,
                            RelicEffectScaling.HEALING,
                            impact.action.heal.spell_power_coefficient,
                            level
                    );
                }
                if (impact.action.status_effect != null) {
                    dungeonDifficultyAddition$addNumberReplacement(
                            replacements,
                            RelicEffectScaling.DURATION,
                            impact.action.status_effect.duration,
                            level
                    );
                }
            }
        }

        if (spell.cost != null && spell.cost.cooldown != null) {
            var base = spell.cost.cooldown.duration;
            var scaled = RelicEffectScaling.scaledCooldown(base, level);
            dungeonDifficultyAddition$putReplacement(
                    replacements,
                    dungeonDifficultyAddition$formatNumber(base),
                    dungeonDifficultyAddition$formatNumber(scaled)
            );
        }
    }

    private static void dungeonDifficultyAddition$addPercentReplacement(
            Map<String, String> replacements,
            String attribute,
            float base,
            int level
    ) {
        dungeonDifficultyAddition$putReplacement(
                replacements,
                dungeonDifficultyAddition$formatNumber(base * 100D) + "%",
                dungeonDifficultyAddition$formatNumber(
                        RelicEffectScaling.scaledValue(attribute, base, level) * 100D
                ) + "%"
        );
    }

    private static void dungeonDifficultyAddition$addNumberReplacement(
            Map<String, String> replacements,
            String attribute,
            float base,
            int level
    ) {
        dungeonDifficultyAddition$putReplacement(
                replacements,
                dungeonDifficultyAddition$formatNumber(base),
                dungeonDifficultyAddition$formatNumber(
                        RelicEffectScaling.scaledValue(attribute, base, level)
                )
        );
    }

    private static void dungeonDifficultyAddition$putReplacement(
            Map<String, String> replacements,
            String original,
            String scaled
    ) {
        if (!original.equals(scaled)) {
            replacements.putIfAbsent(original, scaled);
        }
    }

    private static void dungeonDifficultyAddition$addEffectBonusReplacements(
            Map<String, String> replacements,
            String effectId,
            int level
    ) {
        var effects = dungeonDifficultyAddition$relicEffects(effectId);
        if (effects == null || !effects.has(effectId)) {
            return;
        }

        var effect = effects.getAsJsonObject(effectId);
        if (!effect.has("attributes") || !effect.get("attributes").isJsonArray()) {
            return;
        }

        for (var attributeElement : effect.getAsJsonArray("attributes")) {
            if (!attributeElement.isJsonObject()) {
                continue;
            }
            var attribute = attributeElement.getAsJsonObject();
            var attributeId = attribute.has("attribute") ? attribute.get("attribute").getAsString() : "";
            var baseValue = attribute.has("value") ? attribute.get("value").getAsDouble() : 0D;
            var original = dungeonDifficultyAddition$formatValue(baseValue);
            var scaled = dungeonDifficultyAddition$formatValue(
                    RelicEffectScaling.scaledValue(attributeId, baseValue, level)
            );
            if (original.equals(scaled)) {
                continue;
            }

            replacements.putIfAbsent(original, scaled);
        }
    }

    private static String dungeonDifficultyAddition$replaceFirstKnownBonus(String line, Map<String, String> replacements) {
        return TooltipNumbers.replaceFirst(line, replacements);
    }

    private static JsonObject dungeonDifficultyAddition$relicEffects(String effectId) {
        var id = Identifier.tryParse(effectId);
        var directory = id == null ? null : AccessoryFamilies.effectsDirectory(id.getNamespace());
        if (directory == null) return null;
        var cached = dungeonDifficultyAddition$relicEffects.get(directory);
        if (cached != null) return cached;

        long now = System.nanoTime();
        var nextRead = dungeonDifficultyAddition$nextEffectsRead.get(directory);
        if (nextRead != null && now - nextRead < 0) return null;
        dungeonDifficultyAddition$nextEffectsRead.put(directory, now + java.util.concurrent.TimeUnit.SECONDS.toNanos(5));

        var path = FMLPaths.CONFIGDIR.get().resolve(directory).resolve("effects.json");
        try (var reader = Files.newBufferedReader(path)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            var effects = root.getAsJsonObject("effects");
            if (effects != null) dungeonDifficultyAddition$relicEffects.put(directory, effects);
            return effects;
        } catch (IOException | IllegalStateException | com.google.gson.JsonParseException exception) {
            return null;
        }
    }

    private static String dungeonDifficultyAddition$formatValue(double value) {
        if (Math.abs(value) < 1) {
            return dungeonDifficultyAddition$formatNumber(value * 100D) + "%";
        }
        return dungeonDifficultyAddition$formatNumber(value);
    }

    private static String dungeonDifficultyAddition$formatNumber(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
