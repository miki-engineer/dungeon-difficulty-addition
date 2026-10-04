package com.miki.dungeondifficultyaddition.mixin.legendary;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.miki.dungeondifficultyaddition.compat.legendary.AbilityDamageScope;
import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class AbilityDamageMixin {
    @WrapMethod(method = "damage")
    private boolean scaleActiveHit(DamageSource source, float amount, Operation<Boolean> original) {
        var cast = AbilityDamageScope.current();
        if (cast == null) return original.call(source, amount);
        var victim = (LivingEntity) (Object) this;
        var attacker = source.getAttacker();
        float damage = !victim.getWorld().isClient && AccessoryScalingConfig.get().enabled
                && attacker != null && cast.owns(attacker.getUuid(), victim.getUuid()) ? cast.apply(amount) : amount;
        // Modify the original hit, not a second hit. Suppress the scope during damage callbacks
        // so thorns, reflected damage and other mods' nested procs do not receive this bonus.
        return AbilityDamageScope.call(null, () -> original.call(source, damage));
    }
}
