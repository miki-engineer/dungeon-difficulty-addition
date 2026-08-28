package com.example.dungeondifficultyaddition.mixin;

import com.example.dungeondifficultyaddition.RelicEffectScaling;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StatusEffect.class)
public abstract class StatusEffectMixin {
    @Redirect(
            method = "onApplied(Lnet/minecraft/entity/attribute/AttributeContainer;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/attribute/EntityAttributeInstance;addPersistentModifier(Lnet/minecraft/entity/attribute/EntityAttributeModifier;)V"
            )
    )
    private void ddJewelryCompat$scaleRelicEffectModifier(
            EntityAttributeInstance attributeInstance,
            EntityAttributeModifier modifier
    ) {
        var level = RelicEffectScaling.currentLevel();
        var effectId = Registries.STATUS_EFFECT.getId((StatusEffect) (Object) this);
        if (level > 0 && "relics_rpgs".equals(effectId.getNamespace())) {
            var attributeId = attributeInstance.getAttribute().getKey()
                    .map(key -> key.getValue().toString())
                    .orElse("");
            var scaledValue = RelicEffectScaling.scaledValue(attributeId, modifier.value(), level);
            modifier = new EntityAttributeModifier(modifier.id(), scaledValue, modifier.operation());
        }

        attributeInstance.addPersistentModifier(modifier);
    }
}
