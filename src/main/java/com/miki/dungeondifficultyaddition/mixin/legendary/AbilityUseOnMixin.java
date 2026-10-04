package com.miki.dungeondifficultyaddition.mixin.legendary;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.miki.dungeondifficultyaddition.compat.legendary.AbilityDamageScope;
import com.miki.dungeondifficultyaddition.compat.legendary.LegendaryAbilityDamage;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = {
        "net.miauczel.legendary_monsters.item.custom.MossyHammerItem",
        "net.miauczel.legendary_monsters.item.custom.DinosaurBoneClubItem",
        "net.miauczel.legendary_monsters.item.custom.TheGreatFrostItem"
}, remap = false)
public abstract class AbilityUseOnMixin {

    // Axe of Lightning's block-use electricity already reads the scaled attack attribute:
    // deliberately excluded to avoid applying the item level twice.
    @WrapMethod(method = {"useOn", "useOnBlock"}, remap = false)
    private ActionResult cast(ItemUsageContext context, Operation<ActionResult> original) {
        return AbilityDamageScope.call(LegendaryAbilityDamage.cast(context.getPlayer(), context.getStack()),
                () -> original.call(context));
    }
}
