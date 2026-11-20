package com.miki.dungeondifficultyaddition.mixin.accessory;

import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;
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

@Mixin(targets = "net.jewelry.neoforge.compat.curios.JewelryCurioItem", remap = false)
public abstract class JewelryCurioItemMixin {
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
        var scaled = AccessoryItemScaling.scaledCuriosModifiers(stack, cir.getReturnValue());
        if (scaled != null) {
            cir.setReturnValue(scaled);
        }
    }
}
