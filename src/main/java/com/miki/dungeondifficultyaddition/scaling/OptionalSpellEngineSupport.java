package com.miki.dungeondifficultyaddition.scaling;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.SpellDataComponents;
import net.spell_engine.item.ScrollItem;
import net.spell_engine.item.UniversalSpellBookItem;

/** Loaded only when Spell Engine is installed. */
final class OptionalSpellEngineSupport {
    private static final TagKey<Item> SPELL_BOOKS =
            TagKey.of(RegistryKeys.ITEM, Identifier.of("spell_engine", "spell_books"));
    private static final TagKey<Item> CURIOS_SPELL_BOOKS =
            TagKey.of(RegistryKeys.ITEM, Identifier.of("curios", "spell_book"));
    private static final TagKey<Item> CURIOS_SPELL_SCROLLS =
            TagKey.of(RegistryKeys.ITEM, Identifier.of("curios", "spell_scroll"));

    private OptionalSpellEngineSupport() {
    }

    static boolean hasSpellContainer(ItemStack stack) {
        var spellContainer = stack.get(SpellDataComponents.SPELL_CONTAINER);
        return spellContainer != null && spellContainer.isValid();
    }

    static boolean isSpellBookOrScroll(ItemStack stack) {
        return stack.getItem() instanceof ScrollItem
                || stack.getItem() instanceof UniversalSpellBookItem
                || stack.isIn(SPELL_BOOKS)
                || stack.isIn(CURIOS_SPELL_BOOKS)
                || stack.isIn(CURIOS_SPELL_SCROLLS);
    }
}
