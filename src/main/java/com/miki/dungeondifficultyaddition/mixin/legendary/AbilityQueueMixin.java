package com.miki.dungeondifficultyaddition.mixin.legendary;

import com.miki.dungeondifficultyaddition.compat.legendary.AbilityDamageScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.miauczel.legendary_monsters.LegendaryMonsters", remap = false)
public abstract class AbilityQueueMixin {
    @ModifyVariable(method = "queueServerWork", at = @At("HEAD"), argsOnly = true, remap = false)
    private static Runnable retainCast(Runnable action) { return AbilityDamageScope.capture(action); }
}
