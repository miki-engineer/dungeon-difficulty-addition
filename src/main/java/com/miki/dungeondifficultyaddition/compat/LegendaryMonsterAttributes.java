package com.miki.dungeondifficultyaddition.compat;

import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.common.extensions.IItemExtension;

import java.util.ArrayList;
import java.util.HashSet;

/** Resolve the mod's live config-backed attributes before DD applies its own scaling formula. */
public final class LegendaryMonsterAttributes {
    private static final String MOD_ID = "legendary_monsters";
    private static final String REVISION = "dungeon_difficulty_addition.legendary_attributes";
    private static final int CURRENT_REVISION = 6;
    private static final Identifier PROBE = Identifier.of("dungeon_difficulty_addition", "attribute_probe");

    private LegendaryMonsterAttributes() {}

    public static boolean applies(ItemStack stack) {
        return !stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).getNamespace().equals(MOD_ID);
    }

    public static boolean needsRepair(ItemStack stack) {
        if (!applies(stack) || !ItemScaling.isScaled(stack)) return false;
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                .getNbt().getInt(REVISION) != CURRENT_REVISION;
    }

    /** Recognise plain Item subclasses such as Withered Scythe without mutating the queried stack. */
    public static boolean hasWeaponAttributes(ItemStack stack) {
        if (!applies(stack) || !AccessoryScalingConfig.get().enabled) return false;
        return readBaseAttributes(stack).modifiers().stream().anyMatch(entry ->
                entry.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                        && (entry.slot() == AttributeModifierSlot.MAINHAND || entry.slot() == AttributeModifierSlot.HAND));
    }

    public static void markRepaired(ItemStack stack) {
        if (AccessoryScalingConfig.get().enabled && needsRepair(stack)) {
            stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT,
                    data -> data.apply(nbt -> nbt.putInt(REVISION, CURRENT_REVISION)));
        }
    }

    public static void prepare(ItemStack stack) {
        if (!applies(stack) || !AccessoryScalingConfig.get().enabled) return;
        var resolved = readBaseAttributes(stack);
        if (resolved.modifiers().isEmpty()) return;

        var existing = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, mergeBaseAttributes(existing, resolved));
    }

    private static AttributeModifiersComponent readBaseAttributes(ItemStack stack) {
        // Never feed previously scaled attributes back into a dynamic defaults method.
        var probe = stack.copy();
        var defaults = stack.getItem().getComponents().getOrDefault(
                DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        if (defaults.modifiers().isEmpty()) {
            // Soul Great Sword queries the stack's effective attributes inside its defaults method.
            // A nonempty, zero-value seed avoids recursively querying that method again.
            defaults = AttributeModifiersComponent.builder().add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                    new EntityAttributeModifier(PROBE, 0, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND).build();
        }
        probe.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, defaults);
        // LM registers its live weapon stats through ItemAttributeModifierEvent, including
        // Chorus Blade (which has no attributes in its item class). Read that effective path
        // first: some legacy defaults methods disagree with the event's current config values.
        // The seed also prevents Soul Great Sword from recursively asking for its defaults.
        var effective = normalize(AttributeProbeScope.read(probe, probe::getAttributeModifiers));
        // Retain support for older/custom LM items which only supply a defaults method.
        return LegendaryAttributePolicy.effectiveOrFallback(effective, value -> value.modifiers().isEmpty(),
                () -> normalize(AttributeProbeScope.read(probe,
                        () -> ((IItemExtension) stack.getItem()).getDefaultAttributeModifiers(probe))));
    }

    private static AttributeModifiersComponent mergeBaseAttributes(
            AttributeModifiersComponent existing, AttributeModifiersComponent resolved) {
        var replaced = new HashSet<BaseModifierKey>();
        for (var entry : resolved.modifiers()) replaced.add(BaseModifierKey.of(entry));
        var merged = AttributeModifiersComponent.builder();
        // Preserve unrelated stack-specific modifiers. Replace matching base IDs even when the
        // upstream entry used the wrong attribute (attack speed registered as attack damage).
        for (var entry : existing.modifiers()) {
            if (!replaced.contains(BaseModifierKey.of(entry))) {
                merged.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        for (var entry : resolved.modifiers()) merged.add(entry.attribute(), entry.modifier(), entry.slot());
        return merged.build().withShowInTooltip(existing.showInTooltip());
    }

    static AttributeModifiersComponent normalize(AttributeModifiersComponent source) {
        var corrected = new ArrayList<AttributeModifiersComponent.Entry>();
        for (var entry : source.modifiers()) {
            if (entry.modifier().id().equals(PROBE)) continue;
            var attribute = entry.attribute();
            if (LegendaryAttributePolicy.misplacedSpeed(
                    attribute.getKey().map(key -> key.getValue().toString()).orElse(""),
                    entry.modifier().id().toString())) {
                attribute = EntityAttributes.GENERIC_ATTACK_SPEED;
            }
            corrected.add(new AttributeModifiersComponent.Entry(attribute, entry.modifier(), entry.slot()));
        }
        // Soul Great Sword can return both a valid speed entry and one under attack damage.
        // Correct first, then deduplicate: they become the same (attribute, ID, slot) key.
        var result = AttributeModifiersComponent.builder();
        for (var entry : LegendaryAttributePolicy.uniqueByKey(corrected,
                ModifierKey::of)) {
            result.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        return result.build().withShowInTooltip(source.showInTooltip());
    }

    private record ModifierKey(RegistryEntry<EntityAttribute> attribute,
                               Identifier id, AttributeModifierSlot slot) {
        static ModifierKey of(AttributeModifiersComponent.Entry entry) {
            return new ModifierKey(entry.attribute(), entry.modifier().id(), entry.slot());
        }
    }

    /** Deliberately excludes attribute so a misplaced base modifier gets replaced, not retained. */
    private record BaseModifierKey(Identifier id, AttributeModifierSlot slot) {
        static BaseModifierKey of(AttributeModifiersComponent.Entry entry) {
            return new BaseModifierKey(entry.modifier().id(), entry.slot());
        }
    }
}
