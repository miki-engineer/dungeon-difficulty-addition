package com.example.dungeondifficultyaddition;

import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryItemAttributeModifiers;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Loaded only when Accessories is installed. */
final class OptionalAccessoryAttributeScaling {
    private OptionalAccessoryAttributeScaling() {
    }

    static void removeFixedModifiers(ItemStack stack) {
        var attributes = stack.get(AccessoriesDataComponents.ATTRIBUTES);
        if (attributes == null || attributes.modifiers().isEmpty()) {
            return;
        }
        var builder = AccessoryItemAttributeModifiers.builder().showInTooltip(attributes.showInTooltip());
        for (var entry : attributes.modifiers()) {
            if (!AccessoryItemScaling.isFixedModifier(entry.modifier())) {
                builder.addForSlot(entry.attribute(), entry.modifier(), entry.slotName(), entry.isStackable());
            }
        }
        stack.set(AccessoriesDataComponents.ATTRIBUTES, builder.build());
    }

    static boolean hasNegativeFixedModifier(ItemStack stack) {
        var attributes = stack.get(AccessoriesDataComponents.ATTRIBUTES);
        if (attributes == null) {
            return false;
        }
        for (var entry : attributes.modifiers()) {
            if (AccessoryItemScaling.isFixedModifier(entry.modifier()) && entry.modifier().value() < 0D) {
                return true;
            }
        }
        return false;
    }

    static void scaleAttributes(
            ItemStack stack,
            AccessoryScalingConfig config,
            int level,
            boolean fixedOverride
    ) {
        var attributes = stack.get(AccessoriesDataComponents.ATTRIBUTES);
        if (attributes == null || attributes.modifiers().isEmpty()) {
            return;
        }

        var itemId = Registries.ITEM.getId(stack.getItem()).toString();
        var builder = AccessoryItemAttributeModifiers.builder().showInTooltip(attributes.showInTooltip());
        var changed = false;
        for (var entry : attributes.modifiers()) {
            var attributeId = entry.attribute().getKey()
                    .map(key -> key.getValue().toString())
                    .orElse("");
            var bonus = AccessoryItemScaling.configuredBonus(
                    entry.modifier().value(), attributeId, config, level
            );

            if (bonus == 0D) {
                builder.addForSlot(entry.attribute(), entry.modifier(), entry.slotName(), entry.isStackable());
            } else if (!fixedOverride && config.merge_accessory_modifiers) {
                builder.addForSlot(
                        entry.attribute(),
                        new EntityAttributeModifier(
                                entry.modifier().id(),
                                entry.modifier().value() + bonus,
                                entry.modifier().operation()
                        ),
                        entry.slotName(),
                        entry.isStackable()
                );
                changed = true;
            } else {
                builder.addForSlot(entry.attribute(), entry.modifier(), entry.slotName(), entry.isStackable());
                var modifierId = fixedOverride
                        ? AccessoryItemScaling.fixedModifierId(
                                itemId, attributeId, entry.modifier().id().toString()
                        )
                        : Identifier.of(
                                DungeonDifficultyAddition.MOD_ID,
                                "scale/" + AccessoryItemScaling.sanitize(itemId)
                                        + "/" + AccessoryItemScaling.sanitize(attributeId)
                        );
                builder.addForSlot(
                        entry.attribute(),
                        new EntityAttributeModifier(modifierId, bonus, entry.modifier().operation()),
                        entry.slotName(),
                        entry.isStackable()
                );
                changed = true;
            }
        }
        if (changed) {
            stack.set(AccessoriesDataComponents.ATTRIBUTES, builder.build());
        }
    }
}
