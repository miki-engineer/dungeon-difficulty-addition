package com.example.dungeondifficultyaddition.mixin.client;

import com.example.dungeondifficultyaddition.FixedLevelPreview;
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
    private EmiIngredient ddJewelryCompat$cachedSource;
    @Unique
    private EmiIngredient ddJewelryCompat$cachedPreview;

    @Inject(method = "getStack", at = @At("RETURN"), cancellable = true, remap = false)
    private void ddJewelryCompat$scaleRecipeOutputPreview(
            CallbackInfoReturnable<EmiIngredient> cir
    ) {
        // Recipe result slots have context; sidebar slots do not.
        if (recipe == null || cir.getReturnValue() == null) {
            return;
        }

        var source = cir.getReturnValue();
        if (source == ddJewelryCompat$cachedSource && ddJewelryCompat$cachedPreview != null) {
            cir.setReturnValue(ddJewelryCompat$cachedPreview);
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

        ddJewelryCompat$cachedSource = source;
        ddJewelryCompat$cachedPreview = replacements.size() == 1
                ? replacements.getFirst()
                : EmiIngredient.of(replacements, source.getAmount()).setChance(source.getChance());
        cir.setReturnValue(ddJewelryCompat$cachedPreview);
    }
}
