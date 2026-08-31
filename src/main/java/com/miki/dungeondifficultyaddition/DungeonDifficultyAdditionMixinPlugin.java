package com.miki.dungeondifficultyaddition;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class DungeonDifficultyAdditionMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var mixinName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        return switch (mixinName) {
            case "ItemScalingMixin" -> isLoaded(DungeonDifficultyAddition.JEWELRY_MOD_ID)
                    || isLoaded(DungeonDifficultyAddition.RELICS_MOD_ID);
            case "ItemStackTooltipMixin", "StatusEffectMixin" -> hasRelicSpellSupport();
            case "JewelryCurioItemMixin" -> isLoaded(DungeonDifficultyAddition.JEWELRY_MOD_ID)
                    && isLoaded("curios");
            case "RelicCurioItemMixin" -> isLoaded(DungeonDifficultyAddition.RELICS_MOD_ID)
                    && isLoaded("curios");
            case "JeiRecipeSlotMixin" -> isLoaded("jei");
            case "EmiRecipeSlotMixin" -> isLoaded("emi");
            default -> !mixinName.startsWith("Spell") || hasRelicSpellSupport();
        };
    }

    private static boolean hasRelicSpellSupport() {
        return isLoaded(DungeonDifficultyAddition.RELICS_MOD_ID)
                && isLoaded(DungeonDifficultyAddition.SPELL_ENGINE_MOD_ID);
    }

    private static boolean isLoaded(String modId) {
        return OptionalModSupport.isLoaded(modId);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }
}
