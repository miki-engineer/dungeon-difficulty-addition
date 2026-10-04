package com.miki.dungeondifficultyaddition.mixin.legendary;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.miki.dungeondifficultyaddition.compat.legendary.AbilityDamageScope;
import com.miki.dungeondifficultyaddition.compat.legendary.LegendaryAbilityDamage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = {
        "net.miauczel.legendary_monsters.item.custom.MonstrousAnchorItem"
}, remap = false)
public abstract class AbilityPostHitMixin {

    // Only the separate splash damage is inside postHit; the ordinary melee hit is outside.
    @WrapMethod(method = {"hurtEnemy", "postHit"}, remap = false)
    private boolean cast(ItemStack stack, LivingEntity target, LivingEntity actor, Operation<Boolean> original) {
        return AbilityDamageScope.call(LegendaryAbilityDamage.cast(actor, stack),
                () -> original.call(stack, target, actor));
    }
}
