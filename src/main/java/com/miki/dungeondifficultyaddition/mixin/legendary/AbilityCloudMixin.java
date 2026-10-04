package com.miki.dungeondifficultyaddition.mixin.legendary;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.miki.dungeondifficultyaddition.compat.legendary.AbilityDamageScope;
import com.miki.dungeondifficultyaddition.compat.legendary.LegendaryAbilityDamage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.WorldAccess;
import net.neoforged.bus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.miauczel.legendary_monsters.item.custom.CustomItemEvents.WandOfCloudsRightClickEvent", remap = false)
public abstract class AbilityCloudMixin {
    @WrapMethod(method = "execute(Lnet/neoforged/bus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V", remap = false)
    private static void cast(Event event, WorldAccess world, Entity target, Entity source, Operation<Void> original) {
        var cast = source instanceof LivingEntity actor ? LegendaryAbilityDamage.cast(actor, actor.getMainHandStack()) : null;
        AbilityDamageScope.run(cast, () -> original.call(event, world, target, source));
    }
}
