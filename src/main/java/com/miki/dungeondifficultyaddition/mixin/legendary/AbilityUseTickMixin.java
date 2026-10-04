package com.miki.dungeondifficultyaddition.mixin.legendary;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.miki.dungeondifficultyaddition.compat.legendary.AbilityDamageScope;
import com.miki.dungeondifficultyaddition.compat.legendary.LegendaryAbilityDamage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = {
        "net.miauczel.legendary_monsters.item.custom.WitheredScytheItem"
}, remap = false)
public abstract class AbilityUseTickMixin {

    @WrapMethod(method = {"onUseTick", "usageTick"}, remap = false)
    private void cast(World world, LivingEntity actor, ItemStack stack, int remaining, Operation<Void> original) {
        AbilityDamageScope.run(LegendaryAbilityDamage.cast(actor, stack),
                () -> original.call(world, actor, stack, remaining));
    }
}
