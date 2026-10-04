package com.miki.dungeondifficultyaddition.scaling;

import com.miki.dungeondifficultyaddition.DungeonDifficultyAddition;
import com.miki.dungeondifficultyaddition.compat.OptionalModSupport;
import com.miki.dungeondifficultyaddition.compat.LegendaryMonsterAttributes;

import com.miki.dungeondifficultyaddition.mixin.loot.ItemScalingInvoker;
import net.dungeon_difficulty.DungeonDifficulty;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.ItemScaling;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ToolItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.common.extensions.IItemExtension;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class DungeonDifficultyNativeScaling {
    private static final ClassValue<Boolean> HAS_DYNAMIC_DEFAULT_ATTRIBUTES = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> itemClass) {
            try {
                return itemClass
                        .getMethod("getDefaultAttributeModifiers", ItemStack.class)
                        .getDeclaringClass() != IItemExtension.class;
            } catch (NoSuchMethodException exception) {
                return false;
            }
        }
    };

    private DungeonDifficultyNativeScaling() {
    }

    static boolean isEquipment(ItemStack stack) {
        var path = Registries.ITEM.getId(stack.getItem()).getPath().replace("_", "").replace("-", "");
        if (path.contains("spellbook") || path.contains("spellscroll")) {
            return false;
        }
        return isNativeEquipment(stack)
                // A spell container alone is not equipment (e.g. spell scrolls).
                // Keep the broader compatibility inference for explicit scaling only.
                || inferKindAndSlots(stack, false) != null
                || LegendaryMonsterAttributes.hasWeaponAttributes(stack);
    }

    static boolean apply(ItemStack stack, int level) {
        boolean nativeEquipment = isNativeEquipment(stack);
        // Capture stack-specific defaults before ItemScaling replaces them.
        if (LegendaryMonsterAttributes.applies(stack)) {
            // Plain Item weapons must expose their attributes before kind/slot inference.
            // Native equipment is prepared by the shared DD hook. Only inferred equipment
            // needs its attributes materialized here to discover its kind and slots.
            if (!nativeEquipment) LegendaryMonsterAttributes.prepare(stack);
        } else if (HAS_DYNAMIC_DEFAULT_ATTRIBUTES.get(stack.getItem().getClass())) {
            var dynamicDefaults = ((IItemExtension) stack.getItem()).getDefaultAttributeModifiers(stack);
            stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, dynamicDefaults);
        }

        // Use Dungeon Difficulty's public scaler for every item type it recognizes
        // natively. Reserve the inferred compatibility path for nonstandard items.
        if (nativeEquipment) {
            ItemScaling.scale(stack, level);
            return ItemScaling.isScaled(stack);
        }

        var inferred = inferKindAndSlots(stack);
        if (inferred == null) {
            return false;
        }

        var itemData = new PatternMatching.ItemData(
                inferred.kind(),
                Identifier.of("minecraft", "none"),
                stack.getRegistryEntry(),
                stack.getRarity().toString()
        );
        var result = PatternMatching.getItemScaleResult(
                itemData,
                DungeonDifficulty.config.value.loot_scaling,
                level
        );
        var modifiers = applicableModifiers(stack, inferred, result.modifiers());
        ItemScalingInvoker.dungeonDifficultyAddition$applyModifiersForItemStack(
                inferred.slots(),
                Registries.ITEM.getId(stack.getItem()).toString(),
                stack,
                modifiers,
                result.level()
        );
        return ItemScaling.isScaled(stack);
    }

    private static boolean isNativeEquipment(ItemStack stack) {
        return stack.getItem() instanceof ToolItem
                || stack.getItem() instanceof RangedWeaponItem
                || stack.getItem() instanceof ArmorItem
                || stack.getItem() instanceof ShieldItem;
    }

    private static InferredItem inferKindAndSlots(ItemStack stack) {
        return inferKindAndSlots(stack, true);
    }

    private static InferredItem inferKindAndSlots(ItemStack stack, boolean allowSpellContainer) {
        var attributes = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        var armorSlots = new LinkedHashSet<AttributeModifierSlot>();
        var weapon = false;
        var handSlot = false;
        if (attributes != null) {
            for (var entry : attributes.modifiers()) {
                var attributeId = attributeId(entry);
                if (attributeId.endsWith("attack_damage")) {
                    weapon = true;
                    handSlot |= entry.slot() == AttributeModifierSlot.HAND;
                }
                if (attributeId.endsWith(":armor") || attributeId.endsWith("armor_toughness")) {
                    armorSlots.add(entry.slot());
                }
            }
        }

        if (stack.getItem() instanceof ToolItem || weapon) {
            return new InferredItem(
                    PatternMatching.ItemKind.WEAPONS,
                    List.of(handSlot ? AttributeModifierSlot.HAND : AttributeModifierSlot.MAINHAND),
                    false
            );
        }
        if (stack.getItem() instanceof RangedWeaponItem) {
            return new InferredItem(
                    PatternMatching.ItemKind.WEAPONS,
                    List.of(AttributeModifierSlot.MAINHAND),
                    true
            );
        }
        if (isMagicWeapon(stack, allowSpellContainer)) {
            return new InferredItem(
                    PatternMatching.ItemKind.WEAPONS,
                    List.of(AttributeModifierSlot.MAINHAND),
                    false
            );
        }
        if (!armorSlots.isEmpty()) {
            return new InferredItem(PatternMatching.ItemKind.ARMOR, List.copyOf(armorSlots), false);
        }
        if (stack.getItem() instanceof ShieldItem) {
            return new InferredItem(
                    PatternMatching.ItemKind.ARMOR,
                    List.of(AttributeModifierSlot.HAND),
                    false
            );
        }
        return null;
    }

    private static List<Config.AttributeModifier> applicableModifiers(
            ItemStack stack,
            InferredItem inferred,
            List<Config.AttributeModifier> original
    ) {
        if (inferred.kind() != PatternMatching.ItemKind.WEAPONS || hasSpellPower(stack)) {
            return original;
        }

        var modifiers = new ArrayList<Config.AttributeModifier>();
        var removedPowerRule = false;
        for (var modifier : original) {
            var pattern = modifier.attribute == null
                    ? ""
                    : modifier.attribute.toLowerCase(Locale.ROOT);
            if (pattern.contains("power") && !pattern.contains("attack")) {
                removedPowerRule = true;
                continue;
            }
            modifiers.add(modifier);
        }
        if (!removedPowerRule) {
            return original;
        }
        if (inferred.ranged()) {
            var movementSpeed = new Config.AttributeModifier(
                    "minecraft:generic.movement_speed",
                    0.02F
            );
            movementSpeed.operation = Config.Operation.ADDITION;
            movementSpeed.randomness = 0F;
            modifiers.add(movementSpeed);
        }
        return modifiers;
    }

    private static boolean hasSpellPower(ItemStack stack) {
        var attributes = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attributes == null) {
            return false;
        }
        for (var entry : attributes.modifiers()) {
            var attributeId = attributeId(entry).toLowerCase(Locale.ROOT);
            if (attributeId.contains("spell_power") || attributeId.contains("spellpower")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMagicWeapon(ItemStack stack, boolean allowSpellContainer) {
        if (allowSpellContainer
                && OptionalModSupport.isLoaded(DungeonDifficultyAddition.SPELL_ENGINE_MOD_ID)
                && OptionalSpellEngineSupport.hasSpellContainer(stack)) {
            return true;
        }

        // Some caster weapons expose their type only through the registry name.
        var path = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        return path.contains("staff")
                || path.contains("wand")
                || path.contains("scepter")
                || path.contains("spellbook")
                || path.contains("grimoire")
                || path.contains("tome");
    }

    private static String attributeId(net.minecraft.component.type.AttributeModifiersComponent.Entry entry) {
        return entry.attribute().getKey()
                .map(key -> key.getValue().toString())
                .orElse("");
    }

    private record InferredItem(
            PatternMatching.ItemKind kind,
            List<AttributeModifierSlot> slots,
            boolean ranged
    ) {
    }
}
