package com.miki.dungeondifficultyaddition;

import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.container.SpellContainerSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;

public final class RelicEffectScaling {
    public static final String DAMAGE = "spell_engine:damage";
    public static final String HEALING = "spell_engine:healing";
    public static final String DURATION = "spell_engine:duration";
    public static final String COOLDOWN = "spell_engine:cooldown";
    public static final String PROC_CHANCE = "spell_engine:proc_chance";
    public static final String RANGE = "spell_engine:range";
    public static final String RADIUS = "spell_engine:radius";

    private static final ThreadLocal<Deque<Integer>> ACTIVE_RELIC_LEVELS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private RelicEffectScaling() {
    }

    public static void push(PlayerEntity player, RegistryEntry<Spell> spellEntry) {
        ACTIVE_RELIC_LEVELS.get().push(relicLevel(player, spellEntry));
    }

    public static void push(int level) {
        ACTIVE_RELIC_LEVELS.get().push(Math.max(0, level));
    }

    public static void pop() {
        var levels = ACTIVE_RELIC_LEVELS.get();
        if (!levels.isEmpty()) {
            levels.pop();
        }
        if (levels.isEmpty()) {
            ACTIVE_RELIC_LEVELS.remove();
        }
    }

    public static int currentLevel() {
        var levels = ACTIVE_RELIC_LEVELS.get();
        return levels.isEmpty() ? 0 : levels.peek();
    }

    public static double scaledValue(String attributeId, double baseValue, int level) {
        if (level <= 0) {
            return baseValue;
        }

        var config = AccessoryScalingConfig.get();
        var scaledValue = baseValue;

        for (var modifier : config.modifiers()) {
            if (!AccessoryItemScaling.matchesAttribute(attributeId, modifier.attribute)) {
                continue;
            }

            // Keep active-effect runtime values and tooltips identical.
            var scaling = modifier.value * level + modifier.offset;
            scaledValue += modifier.operation == Config.Operation.ADDITION
                    ? scaling
                    : baseValue * scaling;
        }

        return scaledValue;
    }

    public static float scaledValue(String attributeId, float baseValue) {
        return (float) scaledValue(attributeId, baseValue, currentLevel());
    }

    public static float scaledCooldown(float baseValue, int level) {
        var potency = scaledValue(COOLDOWN, 1D, level);
        return potency > 0D ? (float) (baseValue / potency) : baseValue;
    }

    public static float scaledProcChance(float baseValue) {
        var scaled = scaledValue(PROC_CHANCE, baseValue, currentLevel());
        return (float) Math.max(0D, Math.min(1D, scaled));
    }

    public static String compactNumber(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    public static int level(PlayerEntity player, RegistryEntry<Spell> spellEntry) {
        return relicLevel(player, spellEntry);
    }

    public static int level(ItemStack stack) {
        return scaledRelicLevel(stack);
    }

    private static int relicLevel(PlayerEntity player, RegistryEntry<Spell> spellEntry) {
        var config = AccessoryScalingConfig.get();
        if (player == null || !config.enabled || !config.scale_relics) {
            return 0;
        }

        var spellId = spellEntry.getKey()
                .map(key -> key.getValue())
                .orElse(null);
        if (spellId == null || !DungeonDifficultyAddition.RELICS_MOD_ID.equals(spellId.getNamespace())) {
            return 0;
        }

        var source = SpellContainerSource.getFirstSourceOfSpell(spellId, player);
        if (source == null) {
            return 0;
        }

        return scaledRelicLevel(source.itemStack());
    }

    private static int scaledRelicLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        var itemId = Registries.ITEM.getId(stack.getItem());
        if (!DungeonDifficultyAddition.RELICS_MOD_ID.equals(itemId.getNamespace())) {
            return 0;
        }

        return Math.max(0, ItemScaling.getScaleFactor(stack));
    }
}
