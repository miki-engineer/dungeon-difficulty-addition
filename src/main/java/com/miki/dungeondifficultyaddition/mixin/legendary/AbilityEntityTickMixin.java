package com.miki.dungeondifficultyaddition.mixin.legendary;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.miki.dungeondifficultyaddition.compat.legendary.AbilityDamageScope;
import com.miki.dungeondifficultyaddition.compat.legendary.LegendaryAbilityDamage;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerWorld.class)
public abstract class AbilityEntityTickMixin {
    @WrapMethod(method = "tickEntity")
    private void restoreCast(Entity entity, Operation<Void> original) {
        var cast = LegendaryAbilityDamage.savedCast(entity);
        if (cast == null) { original.call(entity); return; }
        AbilityDamageScope.run(cast, () -> original.call(entity));
    }
}
