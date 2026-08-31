package com.miki.dungeondifficultyaddition.mixin.client;

import com.miki.dungeondifficultyaddition.FixedLevelPreview;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Pseudo
@Mixin(targets = "dev.emi.emi.api.widget.SlotWidget", remap = false)
public abstract class EmiRecipeSlotMixin {
    @Shadow(remap = false)
    private EmiRecipe recipe;

    @Unique
    private EmiIngredient dungeonDifficultyAddition$cachedSource;
    @Unique
    private EmiIngredient dungeonDifficultyAddition$cachedPreview;

    @Inject(method = "getStack", at = @At("RETURN"), cancellable = true, remap = false)
    private void dungeonDifficultyAddition$scaleRecipeOutputPreview(
            CallbackInfoReturnable<EmiIngredient> cir
    ) {
        // Recipe result slots have context; sidebar slots do not.
        if (recipe == null || cir.getReturnValue() == null) {
            return;
        }

        var source = cir.getReturnValue();
        if (source == dungeonDifficultyAddition$cachedSource && dungeonDifficultyAddition$cachedPreview != null) {
            cir.setReturnValue(dungeonDifficultyAddition$cachedPreview);
            return;
        }

        var replacements = new ArrayList<EmiStack>();
        var changed = false;
        for (var emiStack : source.getEmiStacks()) {
            var original = emiStack.getItemStack();
            var preview = FixedLevelPreview.scaledCopy(original);
            if (preview != original) {
                var replacement = EmiStack.of(preview, emiStack.getAmount());
                replacement.setChance(emiStack.getChance());
                replacements.add(replacement);
                changed = true;
            } else {
                replacements.add(emiStack);
            }
        }
        if (!changed) {
            return;
        }

        dungeonDifficultyAddition$cachedSource = source;
        dungeonDifficultyAddition$cachedPreview = replacements.size() == 1
                ? replacements.getFirst()
                : EmiIngredient.of(replacements, source.getAmount()).setChance(source.getChance());
        cir.setReturnValue(dungeonDifficultyAddition$cachedPreview);
    }
}
