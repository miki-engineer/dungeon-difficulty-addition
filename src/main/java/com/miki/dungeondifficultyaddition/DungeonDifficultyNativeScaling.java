package com.miki.dungeondifficultyaddition;

import com.miki.dungeondifficultyaddition.mixin.ItemScalingInvoker;
import net.dungeon_difficulty.DungeonDifficulty;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.ItemScaling;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ToolItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.common.extensions.IItemExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class DungeonDifficultyNativeScaling {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DungeonDifficultyAddition.MOD_ID);
    private static final Set<String> DIAGNOSTIC_ITEMS = Set.of(
            "legendary_monsters:soul_great_sword",
            "legendary_monsters:the_tesseract"
    );
    private static final Set<String> LOGGED_DIAGNOSTICS = java.util.Collections.synchronizedSet(new HashSet<>());
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

    static boolean apply(ItemStack stack, int level) {
        var itemId = Registries.ITEM.getId(stack.getItem()).toString();
        var hasDynamicDefaults = HAS_DYNAMIC_DEFAULT_ATTRIBUTES.get(((Object) stack.getItem()).getClass());
        // Capture stack-specific defaults before ItemScaling replaces them.
        if (hasDynamicDefaults) {
            var dynamicDefaults = ((IItemExtension) stack.getItem()).getDefaultAttributeModifiers(stack);
            if (dynamicDefaults.modifiers().isEmpty()) {
                dynamicDefaults = knownBrokenDynamicDefaults(itemId);
            }
            stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, dynamicDefaults);
        }

        var beforeScaling = describeAttributes(stack);

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
        if (DIAGNOSTIC_ITEMS.contains(itemId) && LOGGED_DIAGNOSTICS.add(itemId)) {
            LOGGER.info(
                    "Fixed scaling diagnostic: item={}, class={}, level={}, dynamic_defaults={}, rules={}, before={}, after={}, marked_scaled={}",
                    itemId,
                    ((Object) stack.getItem()).getClass().getName(),
                    level,
                    hasDynamicDefaults,
                    result.modifiers().stream()
                            .map(modifier -> modifier.attribute + ":" + modifier.operation)
                            .toList(),
                    beforeScaling,
                    describeAttributes(stack),
                    ItemScaling.isScaled(stack)
            );
        }
        return ItemScaling.isScaled(stack);
    }

    private static AttributeModifiersComponent knownBrokenDynamicDefaults(String itemId) {
        var attackDamage = switch (itemId) {
            case "legendary_monsters:soul_great_sword" -> 12D;
            case "legendary_monsters:the_tesseract" -> 14D;
            default -> 0D;
        };
        if (attackDamage == 0D) {
            return AttributeModifiersComponent.DEFAULT;
        }

        // These items return no defaults, so restore their weapon attributes.
        return AttributeModifiersComponent.builder()
                .add(
                        EntityAttributes.GENERIC_ATTACK_DAMAGE,
                        new EntityAttributeModifier(
                                Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,
                                attackDamage,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.MAINHAND
                )
                .add(
                        EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier(
                                Item.BASE_ATTACK_SPEED_MODIFIER_ID,
                                -2.8D,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.MAINHAND
                )
                .build();
    }

    private static List<String> describeAttributes(ItemStack stack) {
        var attributes = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attributes == null) {
            return List.of("<none>");
        }
        return attributes.modifiers().stream()
                .map(entry -> attributeId(entry)
                        + "|" + entry.modifier().id()
                        + "|" + entry.modifier().value()
                        + "|" + entry.modifier().operation()
                        + "|" + entry.slot())
                .toList();
    }

    private static InferredItem inferKindAndSlots(ItemStack stack) {
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
        if (isMagicWeapon(stack)) {
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

    private static boolean isMagicWeapon(ItemStack stack) {
        if (OptionalModSupport.isLoaded(DungeonDifficultyAddition.SPELL_ENGINE_MOD_ID)
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
