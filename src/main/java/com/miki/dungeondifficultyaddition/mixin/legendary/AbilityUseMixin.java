package com.miki.dungeondifficultyaddition.mixin.legendary;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.miki.dungeondifficultyaddition.compat.legendary.AbilityDamageScope;
import com.miki.dungeondifficultyaddition.compat.legendary.LegendaryAbilityDamage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = {
        "net.miauczel.legendary_monsters.item.custom.Axe_Of_LightningItem",
        "net.miauczel.legendary_monsters.item.custom.TheGreatFrostItem",
        "net.miauczel.legendary_monsters.item.custom.FieryJawItem"
}, remap = false)
public abstract class AbilityUseMixin {

    @WrapMethod(method = "use", remap = false)
    private TypedActionResult<ItemStack> cast(World world, PlayerEntity actor, Hand hand,
                                               Operation<TypedActionResult<ItemStack>> original) {
        return AbilityDamageScope.call(LegendaryAbilityDamage.cast(actor, actor.getStackInHand(hand)),
                () -> original.call(world, actor, hand));
    }
}
