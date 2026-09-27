package com.miki.dungeondifficultyaddition.compat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessoryFamiliesTest {
    @Test void additionalJewelryBelongsToJewelryFamily() {
        assertTrue(AccessoryFamilies.jewelry("jewelry"));
        assertTrue(AccessoryFamilies.jewelry("additional_rpg_jewelry"));
        assertFalse(AccessoryFamilies.relics("additional_rpg_jewelry"));
    }
    @Test void moreRelicsBelongsToRpgRelicsFamily() {
        assertTrue(AccessoryFamilies.relics("relics_rpgs"));
        assertTrue(AccessoryFamilies.relics("more_relics"));
        assertFalse(AccessoryFamilies.relics("relics"));
        assertFalse(AccessoryFamilies.accessory("minecraft"));
    }
    @Test void rawCraftingGemsAreExcludedButWearablesAreNot() {
        assertTrue(AccessoryFamilies.material("additional_rpg_jewelry:aquamarine"));
        assertTrue(AccessoryFamilies.material("additional_rpg_jewelry:malachite"));
        assertFalse(AccessoryFamilies.material("additional_rpg_jewelry:aquamarine_ring"));
        assertFalse(AccessoryFamilies.material("additional_rpg_jewelry:malachite_necklace"));
    }
    @Test void effectDescriptionsUseTheOwningModsConfig() {
        assertEquals("relics", AccessoryFamilies.effectsDirectory("relics_rpgs"));
        assertEquals("more_relics", AccessoryFamilies.effectsDirectory("more_relics"));
        assertNull(AccessoryFamilies.effectsDirectory("unrelated"));
    }
}
