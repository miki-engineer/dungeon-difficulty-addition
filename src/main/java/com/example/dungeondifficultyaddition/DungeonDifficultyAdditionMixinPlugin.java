package com.example.dungeondifficultyaddition;

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
        if (mixinClassName.endsWith(".ItemScalingMixin")) {
            return OptionalModSupport.isLoaded("jewelry")
                    || OptionalModSupport.isLoaded("relics_rpgs");
        }
        if (mixinClassName.endsWith(".ItemStackTooltipMixin")) {
            return OptionalModSupport.isLoaded("relics_rpgs")
                    && OptionalModSupport.isLoaded("spell_engine");
        }
        if (mixinClassName.endsWith(".JewelryCurioItemMixin")) {
            return OptionalModSupport.isLoaded("jewelry")
                    && OptionalModSupport.isLoaded("curios");
        }
        if (mixinClassName.endsWith(".RelicCurioItemMixin")) {
            return OptionalModSupport.isLoaded("relics_rpgs")
                    && OptionalModSupport.isLoaded("curios");
        }
        if (mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1).startsWith("Spell")
                || mixinClassName.endsWith(".StatusEffectMixin")) {
            return OptionalModSupport.isLoaded("relics_rpgs")
                    && OptionalModSupport.isLoaded("spell_engine");
        }
        if (mixinClassName.endsWith(".JeiRecipeSlotMixin")) {
            return OptionalModSupport.isLoaded("jei");
        }
        if (mixinClassName.endsWith(".EmiRecipeSlotMixin")) {
            return OptionalModSupport.isLoaded("emi");
        }
        return true;
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
