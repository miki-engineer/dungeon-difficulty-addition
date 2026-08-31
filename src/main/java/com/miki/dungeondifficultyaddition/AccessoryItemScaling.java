package com.miki.dungeondifficultyaddition;

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
    private static final String FIXED_LEVEL_MARKER = "dungeon_difficulty_addition.fixed_level";
    private static final String FIXED_REVISION_MARKER = "dungeon_difficulty_addition.fixed_revision";
    private static final String LEGACY_FIXED_LEVEL_MARKER = "dd_jewelry_compat.fixed_level";
    private static final String LEGACY_FIXED_REVISION_MARKER = "dd_jewelry_compat.fixed_revision";
    private static final String CURIOS_ROLL_MARKER = "dungeon_difficulty_addition.curios_roll";
    private static final int FIXED_REVISION = 13;
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
        return (config.scale_jewelry && DungeonDifficultyAddition.JEWELRY_MOD_ID.equals(namespace))
                || (config.scale_relics && DungeonDifficultyAddition.RELICS_MOD_ID.equals(namespace));
    }

    public static boolean isBuiltInExcluded(ItemStack stack) {
        return BUILT_IN_EXCLUSIONS.contains(Registries.ITEM.getId(stack.getItem()).toString());
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

    public static void enforceFixedLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        var config = AccessoryScalingConfig.get();
        if (!config.enabled) {
            return;
        }

        if (isBuiltInExcluded(stack)) {
            removeFixedModifiers(stack);
            if (ItemScaling.isScaled(stack)) {
                ItemScaling.removeScaling(stack);
            }
            return;
        }

        var level = config.fixedLevel(stack);
        if (level <= 0 || fixedLevelMarker(stack) == level
                && ItemScaling.getScaleFactor(stack) == level
                && fixedRevisionMarker(stack) == FIXED_REVISION
                && !hasNegativeFixedModifier(stack)) {
            return;
        }

        removeFixedModifiers(stack);
        if (ItemScaling.isScaled(stack)) {
            // Clear old generated scaling first.
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
            // Handles configured items without standard equipment attributes.
            scaleVanillaAttributes(stack, config, level);
            ItemScaling.markAsScaled(stack, level);
        }
        stack.apply(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT,
                level,
                (data, fixedLevel) -> data.apply(nbt -> {
                    nbt.putInt(FIXED_LEVEL_MARKER, fixedLevel);
                    nbt.putInt(FIXED_REVISION_MARKER, FIXED_REVISION);
                    nbt.remove(LEGACY_FIXED_LEVEL_MARKER);
                    nbt.remove(LEGACY_FIXED_REVISION_MARKER);
                })
        );
    }

    private static int fixedLevelMarker(ItemStack stack) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null && customData.contains(FIXED_LEVEL_MARKER)
                ? customData.getNbt().getInt(FIXED_LEVEL_MARKER)
                : 0;
    }

    private static int fixedRevisionMarker(ItemStack stack) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null && customData.contains(FIXED_REVISION_MARKER)
                ? customData.getNbt().getInt(FIXED_REVISION_MARKER)
                : 0;
    }

    private static void removeFixedModifiers(ItemStack stack) {
        var vanilla = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (vanilla != null && !vanilla.modifiers().isEmpty()) {
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
        return DungeonDifficultyAddition.JEWELRY_MOD_ID.equals(namespace)
                || DungeonDifficultyAddition.RELICS_MOD_ID.equals(namespace);
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

    static boolean matchesAttribute(String attributeId, String pattern) {
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
