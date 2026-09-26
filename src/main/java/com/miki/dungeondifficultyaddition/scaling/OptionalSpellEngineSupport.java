package com.miki.dungeondifficultyaddition.scaling;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.SpellDataComponents;
import net.spell_engine.item.ScrollItem;
import net.spell_engine.item.UniversalSpellBookItem;

/** Loaded only when Spell Engine is installed. */
final class OptionalSpellEngineSupport {
    private OptionalSpellEngineSupport() {
    }

    static boolean hasSpellContainer(ItemStack stack) {
        var spellContainer = stack.get(SpellDataComponents.SPELL_CONTAINER);
        return spellContainer != null && spellContainer.isValid();
    }

    static boolean isSpellBookOrScroll(ItemStack stack) {
        return stack.getItem() instanceof ScrollItem
                || stack.getItem() instanceof UniversalSpellBookItem
                || stack.isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("spell_engine", "spell_books")))
                || stack.isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("curios", "spell_book")))
                || stack.isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("curios", "spell_scroll")));
    }
}
