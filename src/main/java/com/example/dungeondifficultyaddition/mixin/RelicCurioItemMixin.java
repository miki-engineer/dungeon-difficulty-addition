package com.example.dungeondifficultyaddition.mixin;

import com.example.dungeondifficultyaddition.AccessoryItemScaling;
import com.google.common.collect.LinkedHashMultimap;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.relics_rpgs.neoforge.compat.curios.RelicCurioItem", remap = false)
public abstract class RelicCurioItemMixin {
    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true, remap = false)
    private void dungeonDifficultyAddition$scaleCuriosModifiers(
            @Coerce Object slotContext,
            Identifier slotIdentifier,
            ItemStack stack,
            CallbackInfoReturnable<
                    com.google.common.collect.Multimap<
                            RegistryEntry<EntityAttribute>, EntityAttributeModifier
                            >
                    > cir
    ) {
        var level = ItemScaling.getScaleFactor(stack);
        var original = cir.getReturnValue();
        if (level <= 0 || original == null || original.isEmpty()) {
            return;
        }

        var scaled = LinkedHashMultimap.<RegistryEntry<EntityAttribute>, EntityAttributeModifier>create();
        for (var entry : original.entries()) {
            var attributeId = entry.getKey().getKey()
                    .map(key -> key.getValue().toString())
                    .orElse("");
            var modifier = entry.getValue();
            var value = AccessoryItemScaling.configuredCuriosValue(
                    stack, modifier.value(), attributeId, level
            );
            scaled.put(entry.getKey(), new EntityAttributeModifier(
                    modifier.id(),
                    AccessoryItemScaling.compactCuriosValue(value, modifier.operation()),
                    modifier.operation()
            ));
        }
        cir.setReturnValue(scaled);
    }
}
