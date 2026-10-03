package com.miki.dungeondifficultyaddition.mixin.item;

import com.miki.dungeondifficultyaddition.compat.AttributeProbeScope;
import net.minecraft.entity.EquipmentSlot;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** LM treats NeoForge's nullable slot override as the actual hand being queried. */
@Mixin(value = IItemStackExtension.class, remap = false)
public interface AttributeProbeSlotMixin {
    @Inject(method = "getEquipmentSlot", at = @At("RETURN"), cancellable = true, remap = false)
    private void dungeonDifficultyAddition$probeMainhand(CallbackInfoReturnable<EquipmentSlot> cir) {
        if (cir.getReturnValue() == null && AttributeProbeScope.contains(this)) {
            cir.setReturnValue(EquipmentSlot.MAINHAND);
        }
    }
}
