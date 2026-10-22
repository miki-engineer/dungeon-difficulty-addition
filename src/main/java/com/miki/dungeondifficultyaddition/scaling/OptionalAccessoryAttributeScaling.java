package com.miki.dungeondifficultyaddition.scaling;

import com.miki.dungeondifficultyaddition.DungeonDifficultyAddition;
import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryItemAttributeModifiers;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/** Loaded only when Accessories is installed. */
final class OptionalAccessoryAttributeScaling {
    private OptionalAccessoryAttributeScaling() {
    }

    /**
     * Strips fixed/ and scale/ bonuses. A merged loot bonus keeps the original modifier ID, so
     * restore those values from the item's default component (Jewelry registers its attributes
     * there) before re-scaling; otherwise the scaled value becomes the next base.
     */
    static void removeScalingModifiers(ItemStack stack, boolean restoreMergedLoot) {
        var attributes = stack.get(AccessoriesDataComponents.ATTRIBUTES);
        if (attributes == null) {
            return;
        }
        var defaults = restoreMergedLoot
                ? stack.getItem().getComponents().get(AccessoriesDataComponents.ATTRIBUTES)
                : null;
        // Excluded gems pass through here every inventory tick; skip the rebuild when idle.
        if (defaults == null && !hasScalingModifier(attributes)) {
            return;
        }
        var builder = AccessoryItemAttributeModifiers.builder().showInTooltip(attributes.showInTooltip());
        var changed = false;
        for (var entry : attributes.modifiers()) {
            if (AccessoryItemScaling.isScalingModifier(entry.modifier())) {
                changed = true;
                continue;
            }
            var modifier = defaults == null ? entry.modifier() : defaultModifier(defaults, entry);
            changed |= !modifier.equals(entry.modifier());
            builder.addForSlot(entry.attribute(), modifier, entry.slotName(), entry.isStackable());
        }
        if (changed) {
            stack.set(AccessoriesDataComponents.ATTRIBUTES, builder.build());
        }
    }

    private static boolean hasScalingModifier(AccessoryItemAttributeModifiers attributes) {
        for (var entry : attributes.modifiers()) {
            if (AccessoryItemScaling.isScalingModifier(entry.modifier())) return true;
        }
        return false;
    }

    private static EntityAttributeModifier defaultModifier(
            AccessoryItemAttributeModifiers defaults,
            AccessoryItemAttributeModifiers.Entry entry
    ) {
        for (var candidate : defaults.modifiers()) {
            if (candidate.attribute().equals(entry.attribute())
                    && candidate.slotName().equals(entry.slotName())
                    && candidate.modifier().id().equals(entry.modifier().id())) {
                return candidate.modifier();
            }
        }
        return entry.modifier();
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

        var itemId = ItemTraits.of(stack).id();
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
