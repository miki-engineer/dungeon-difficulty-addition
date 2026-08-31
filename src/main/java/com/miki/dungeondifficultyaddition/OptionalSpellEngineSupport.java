package com.miki.dungeondifficultyaddition;

import net.minecraft.item.ItemStack;
import net.spell_engine.api.spell.SpellDataComponents;

/** Loaded only when Spell Engine is installed. */
final class OptionalSpellEngineSupport {
    private OptionalSpellEngineSupport() {
    }

    static boolean hasSpellContainer(ItemStack stack) {
        var spellContainer = stack.get(SpellDataComponents.SPELL_CONTAINER);
        return spellContainer != null && spellContainer.isValid();
    }
}
