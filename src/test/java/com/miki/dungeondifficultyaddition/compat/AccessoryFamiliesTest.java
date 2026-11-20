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
    @Test void extraRelicModsJoinTheRelicsFamily() {
        try {
            assertTrue(AccessoryFamilies.relics("realmboundcontent"));
            assertTrue(AccessoryFamilies.extraRelics("realmboundcontent"));
            assertTrue(AccessoryFamilies.accessory("realmboundcontent"));
            // The built-in families are never "extra": their curio classes are hooked directly.
            AccessoryFamilies.setExtraRelics(java.util.List.of("relics_rpgs", "jewelry", "some_mod"));
            assertFalse(AccessoryFamilies.extraRelics("relics_rpgs"));
            assertFalse(AccessoryFamilies.extraRelics("jewelry"));
            assertTrue(AccessoryFamilies.relics("some_mod"));
            assertFalse(AccessoryFamilies.relics("realmboundcontent"));
            AccessoryFamilies.setExtraRelics(java.util.List.of());
            assertFalse(AccessoryFamilies.relics("some_mod"));
            assertTrue(AccessoryFamilies.relics("relics_rpgs"));
        } finally {
            AccessoryFamilies.setExtraRelics(java.util.List.of("realmboundcontent"));
        }
    }
    @Test void rawCraftingGemsAreExcludedButWearablesAreNot() {
        assertTrue(AccessoryFamilies.material("additional_rpg_jewelry:aquamarine"));
        assertTrue(AccessoryFamilies.material("additional_rpg_jewelry:malachite"));
        assertFalse(AccessoryFamilies.material("additional_rpg_jewelry:aquamarine_ring"));
        assertFalse(AccessoryFamilies.material("additional_rpg_jewelry:malachite_necklace"));
    }
}
