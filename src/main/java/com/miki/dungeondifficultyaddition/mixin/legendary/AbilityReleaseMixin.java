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
        "net.miauczel.legendary_monsters.item.custom.SoulGreatSwordItem",
        "net.miauczel.legendary_monsters.item.custom.TheTesseractItem",
        "net.miauczel.legendary_monsters.item.custom.AtomSplitterItem",
        "net.miauczel.legendary_monsters.item.custom.BucklerOfAnnihilationItem",
        "net.miauczel.legendary_monsters.item.custom.HandCannonItem",
        "net.miauczel.legendary_monsters.item.custom.ChorusCannonItem",
        "net.miauczel.legendary_monsters.item.custom.ResurrectedJavelinItem"
}, remap = false)
public abstract class AbilityReleaseMixin {

    @WrapMethod(method = {"releaseUsing", "onStoppedUsing"}, remap = false)
    private void cast(ItemStack stack, World world, LivingEntity actor, int remaining, Operation<Void> original) {
        AbilityDamageScope.run(LegendaryAbilityDamage.cast(actor, stack),
                () -> original.call(stack, world, actor, remaining));
    }
}
