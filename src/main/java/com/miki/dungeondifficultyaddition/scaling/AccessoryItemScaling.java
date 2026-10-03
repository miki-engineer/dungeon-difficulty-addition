package com.miki.dungeondifficultyaddition.scaling;

import com.miki.dungeondifficultyaddition.DungeonDifficultyAddition;
import com.miki.dungeondifficultyaddition.compat.OptionalModSupport;
import com.miki.dungeondifficultyaddition.compat.AccessoryFamilies;
import com.miki.dungeondifficultyaddition.compat.LegendaryMonsterAttributes;
import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import net.dungeon_difficulty.logic.ItemScaling;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class AccessoryItemScaling {
    private static final String CURIOS_ROLL_MARKER = "dungeon_difficulty_addition.curios_roll";
    private static final String FIXED_MODIFIER_PREFIX = "fixed/";
    private static final Set<String> BUILT_IN_EXCLUSIONS = Set.of(
            "jewelry:ruby",
            "jewelry:topaz",
            "jewelry:citrine",
            "jewelry:jade",
            "jewelry:sapphire",
            "jewelry:tanzanite",
            "jewelry:jewelers_kit",
            "jewelry:gem_vein",
            "jewelry:deepslate_gem_vein"
    );

    private AccessoryItemScaling() {
    }

    public static boolean isSupportedAccessory(ItemStack stack, AccessoryScalingConfig config) {
        var namespace = Registries.ITEM.getId(stack.getItem()).getNamespace();
        return (config.scale_jewelry && AccessoryFamilies.jewelry(namespace))
                || (config.scale_relics && AccessoryFamilies.relics(namespace));
    }

    public static boolean isBuiltInExcluded(ItemStack stack) {
        var id = Registries.ITEM.getId(stack.getItem()).toString();
        return BUILT_IN_EXCLUSIONS.contains(id) || AccessoryFamilies.material(id);
    }

    public static void applyLootLevel(ItemStack stack, int level) {
        var config = AccessoryScalingConfig.get();
        if (level <= 0 || ItemScaling.isScaled(stack) || isBuiltInExcluded(stack) || !config.enabled
                || !isSupportedAccessory(stack, config)) {
            return;
        }

        if (OptionalModSupport.isLoaded("accessories") && !usesCuriosAccessory(stack)) {
            OptionalAccessoryAttributeScaling.scaleAttributes(stack, config, level, false);
        }
        ensureCuriosRoll(stack);
        ItemScaling.markAsScaled(stack, level);
    }

    public static int applyCommandLevel(ItemStack stack, int requestedLevel) {
        var config = AccessoryScalingConfig.get();
        if (stack == null || stack.isEmpty() || requestedLevel <= 0 || !config.enabled || isBuiltInExcluded(stack)) {
            return 0;
        }

        var fixedLevel = config.fixedLevel(stack);
        if (fixedLevel > 0) {
            if (!applyLevel(stack, fixedLevel)) {
                return 0;
            }
            ItemLevelData.markFixedLevel(stack, fixedLevel);
            ItemLevelData.clearAnvilLevel(stack);
            return fixedLevel;
        }

        if (!applyLevel(stack, requestedLevel)) {
            return 0;
        }
        ItemLevelData.markManualLevel(stack, requestedLevel);
        ItemLevelData.clearAnvilLevel(stack);
        return requestedLevel;
    }

    /** Called only for a validated anvil preview copy, never the input stack. */
    public static int applyAnvilLevel(ItemStack stack, int target) {
        if (stack == null || stack.isEmpty()) return 0;
        int fixed = AccessoryScalingConfig.get().fixedLevel(stack);
        if (!AnvilLevelPolicy.canAdvance(ItemScaling.getScaleFactor(stack), target, fixed,
                ItemLevelData.anvilLevelMarker(stack)) || !applyLevel(stack, target)) return 0;
        ItemLevelData.markFixedLevel(stack, target);
        ItemLevelData.markAnvilLevel(stack, target);
        return target;
    }

    private static boolean applyLevel(ItemStack stack, int level) {
        var config = AccessoryScalingConfig.get();
        if (stack == null || stack.isEmpty() || level <= 0 || !config.enabled || isBuiltInExcluded(stack)) {
            return false;
        }

        removeFixedModifiers(stack);
        if (ItemScaling.isScaled(stack)) {
            ItemScaling.removeScaling(stack);
        }

        if (isSupportedAccessory(stack, config)) {
            scaleVanillaAttributes(stack, config, level);
            if (OptionalModSupport.isLoaded("accessories") && !usesCuriosAccessory(stack)) {
                OptionalAccessoryAttributeScaling.scaleAttributes(stack, config, level, true);
            }
            ensureCuriosRoll(stack);
            ItemScaling.markAsScaled(stack, level);
        } else if (!DungeonDifficultyNativeScaling.apply(stack, level)) {
            scaleVanillaAttributes(stack, config, level);
            ItemScaling.markAsScaled(stack, level);
        }
        return true;
    }

    public static void enforceFixedLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        var config = AccessoryScalingConfig.get();
        if (!config.enabled) {
            return;
        }

        // Repair old loot/manual/fixed stacks once, retaining their earned level and metadata.
        // This precedes the manual/fixed shortcuts that otherwise keep broken old rolls forever.
        repairLegendaryAttributes(stack, config);

        if (config.fixedLevel(stack) <= 0 && ItemLevelData.anvilLevelMarker(stack) <= 0
                && ItemLevelData.manualLevelMarker(stack) > 0) {
            return;
        }

        if (isBuiltInExcluded(stack)) {
            removeFixedModifiers(stack);
            if (ItemScaling.isScaled(stack)) {
                ItemScaling.removeScaling(stack);
            }
            return;
        }

        var level = AnvilLevelPolicy.enforcedLevel(config.fixedLevel(stack), ItemLevelData.anvilLevelMarker(stack));
        if (level <= 0) {
            // A floor, not a fixed override: preserve dungeon/manual levels and
            // do not reroll attributes on subsequent inventory ticks.
            if (needsMinimumLevel(stack)) {
                applyLevel(stack, 1);
            }
            return;
        }
        if (ItemLevelData.fixedLevelMarker(stack) == level
                && ItemScaling.getScaleFactor(stack) == level
                && ItemLevelData.fixedRevisionMarker(stack) == ItemLevelData.FIXED_REVISION
                && !hasNegativeFixedModifier(stack)) {
            return;
        }

        if (!applyLevel(stack, level)) {
            return;
        }
        ItemLevelData.markFixedLevel(stack, level);
    }

    private static void repairLegendaryAttributes(ItemStack stack, AccessoryScalingConfig config) {
        if (!LegendaryMonsterAttributes.needsRepair(stack)) return;
        int current = ItemScaling.getScaleFactor(stack);
        int floor = AnvilLevelPolicy.enforcedLevel(config.fixedLevel(stack), ItemLevelData.anvilLevelMarker(stack));
        if (applyLevel(stack, Math.max(current, floor))) LegendaryMonsterAttributes.markRepaired(stack);
    }

    public static boolean needsMinimumLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty() || ItemScaling.getScaleFactor(stack) > 0
                || isBuiltInExcluded(stack)) {
            return false;
        }
        if (OptionalModSupport.isLoaded(DungeonDifficultyAddition.SPELL_ENGINE_MOD_ID)
                && OptionalSpellEngineSupport.isSpellBookOrScroll(stack)) {
            return false;
        }
        var config = AccessoryScalingConfig.get();
        return config.enabled && config.minimum_equipment_level_enabled
                && (isSupportedAccessory(stack, config)
                || DungeonDifficultyNativeScaling.isEquipment(stack));
    }

    /** Workstation eligibility is independent of the automatic minimum-level switch. */
    public static boolean isForgeEquipment(ItemStack stack) {
        if (stack == null || stack.isEmpty() || isBuiltInExcluded(stack)) return false;
        if (OptionalModSupport.isLoaded(DungeonDifficultyAddition.SPELL_ENGINE_MOD_ID)
                && OptionalSpellEngineSupport.isSpellBookOrScroll(stack)) return false;
        var config = AccessoryScalingConfig.get();
        return config.enabled && (isSupportedAccessory(stack, config)
                || DungeonDifficultyNativeScaling.isEquipment(stack));
    }

    private static void removeFixedModifiers(ItemStack stack) {
        var vanilla = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (vanilla != null && vanilla.modifiers().stream().anyMatch(entry -> isFixedModifier(entry.modifier()))) {
            var builder = AttributeModifiersComponent.builder();
            for (var entry : vanilla.modifiers()) {
                if (!isFixedModifier(entry.modifier())) {
                    builder.add(entry.attribute(), entry.modifier(), entry.slot());
                }
            }
            stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    builder.build().withShowInTooltip(vanilla.showInTooltip()));
        }

        if (OptionalModSupport.isLoaded("accessories")) {
            OptionalAccessoryAttributeScaling.removeFixedModifiers(stack);
        }
    }

    public static boolean isScalingModifier(EntityAttributeModifier modifier) {
        return (DungeonDifficultyAddition.MOD_ID.equals(modifier.id().getNamespace())
                || DungeonDifficultyAddition.LEGACY_MOD_ID.equals(modifier.id().getNamespace()))
                && (modifier.id().getPath().startsWith(FIXED_MODIFIER_PREFIX)
                || modifier.id().getPath().startsWith("scale/"));
    }

    static boolean isFixedModifier(EntityAttributeModifier modifier) {
        return isScalingModifier(modifier)
                && modifier.id().getPath().startsWith(FIXED_MODIFIER_PREFIX);
    }

    private static boolean hasNegativeFixedModifier(ItemStack stack) {
        var vanilla = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (vanilla != null) {
            for (var entry : vanilla.modifiers()) {
                if (isFixedModifier(entry.modifier()) && entry.modifier().value() < 0D) {
                    return true;
                }
            }
        }

        return OptionalModSupport.isLoaded("accessories")
                && OptionalAccessoryAttributeScaling.hasNegativeFixedModifier(stack);
    }

    private static void scaleVanillaAttributes(ItemStack stack, AccessoryScalingConfig config, int level) {
        var attributes = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attributes == null || attributes.modifiers().isEmpty()) {
            return;
        }

        var itemId = Registries.ITEM.getId(stack.getItem()).toString();
        var builder = AttributeModifiersComponent.builder();
        for (var entry : attributes.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
            var attributeId = entry.attribute().getKey()
                    .map(key -> key.getValue().toString())
                    .orElse("");
            var bonus = configuredBonus(entry.modifier().value(), attributeId, config, level);
            if (bonus == 0D) {
                continue;
            }

            var modifier = new EntityAttributeModifier(
                    fixedModifierId(itemId, attributeId, entry.modifier().id().toString()),
                    bonus,
                    entry.modifier().operation()
            );
            builder.add(entry.attribute(), modifier, entry.slot());
        }
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                builder.build().withShowInTooltip(attributes.showInTooltip()));
    }

    static double configuredBonus(
            double baseValue,
            String attributeId,
            AccessoryScalingConfig config,
            int level
    ) {
        var bonus = 0D;
        for (var modifier : config.modifiers()) {
            if (!matchesAttribute(attributeId, modifier.attribute)) {
                continue;
            }
            bonus += switch (modifier.operation) {
                case ADDITION -> modifier.randomizedValue(level);
                // Do not scale penalties such as attack-speed reductions.
                case MULTIPLY_BASE -> baseValue > 0D
                        ? baseValue * modifier.randomizedValue(level)
                        : 0D;
            };
        }
        return bonus;
    }

    // Returns the base value and its scaling as one Curios modifier.
    public static double configuredCuriosValue(
            ItemStack stack,
            double baseValue,
            String attributeId,
            int level
    ) {
        if (level <= 0) {
            return baseValue;
        }

        var result = baseValue;
        var roll = curiosRoll(stack, level);
        for (var modifier : AccessoryScalingConfig.get().modifiers()) {
            if (!matchesAttribute(attributeId, modifier.attribute)) {
                continue;
            }

            var scaling = modifier.value * level + modifier.offset + modifier.randomness * roll;
            result += switch (modifier.operation) {
                case ADDITION -> scaling;
                case MULTIPLY_BASE -> baseValue > 0D ? baseValue * scaling : 0D;
            };
        }
        return result;
    }

    // Match the precision shown in the tooltip.
    public static double compactCuriosValue(
            double value,
            EntityAttributeModifier.Operation operation
    ) {
        var decimals = operation == EntityAttributeModifier.Operation.ADD_VALUE ? 1 : 3;
        return BigDecimal.valueOf(value)
                .setScale(decimals, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static boolean usesCuriosAccessory(ItemStack stack) {
        if (!OptionalModSupport.isLoaded("curios")) {
            return false;
        }
        var namespace = Registries.ITEM.getId(stack.getItem()).getNamespace();
        return AccessoryFamilies.accessory(namespace);
    }

    private static void ensureCuriosRoll(ItemStack stack) {
        if (!usesCuriosAccessory(stack)) {
            return;
        }
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null && customData.contains(CURIOS_ROLL_MARKER)) {
            return;
        }

        var roll = ThreadLocalRandom.current().nextDouble(-1D, 1D);
        stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, roll,
                (data, value) -> data.apply(nbt -> nbt.putDouble(CURIOS_ROLL_MARKER, value)));
    }

    private static double curiosRoll(ItemStack stack, int level) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null && customData.contains(CURIOS_ROLL_MARKER)) {
            return customData.getNbt().getDouble(CURIOS_ROLL_MARKER);
        }

        // Stable fallback for items created before rolls were stored.
        var itemId = Registries.ITEM.getId(stack.getItem()).toString();
        var hash = 31 * itemId.hashCode() + level;
        return ((hash & 0x7fffffff) / (double) Integer.MAX_VALUE) * 2D - 1D;
    }

    public static boolean matchesAttribute(String attributeId, String pattern) {
        if (pattern == null || pattern.isEmpty() || "*".equals(pattern)) {
            return true;
        }
        return attributeId.equals(pattern) || PatternMatching.regexMatches(attributeId, pattern);
    }

    static Identifier fixedModifierId(String itemId, String attributeId, String originalModifierId) {
        return Identifier.of(
                DungeonDifficultyAddition.MOD_ID,
                FIXED_MODIFIER_PREFIX + sanitize(itemId) + "/" + sanitize(attributeId)
                        + "/" + sanitize(originalModifierId)
        );
    }

    static String sanitize(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    }
}
