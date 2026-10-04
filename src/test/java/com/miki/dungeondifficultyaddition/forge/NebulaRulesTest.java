package com.miki.dungeondifficultyaddition.forge;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class NebulaRulesTest {
    @Test void uniqueEquipmentDefaultsAreExactAndOptional() {
        var config = new SalvageConfig();
        config.validate();
        assertEquals(64, config.legendary_items.size());
        assertEquals(64, config.legendary_items.stream().distinct().count());
        assertTrue(config.isLegendary("arsenal:unique_claymore_1"));
        assertTrue(config.isLegendary("arsenal:unique_shield_sw"));
        assertTrue(config.isLegendary("arsenal:unique_staff_damage_6"));
        assertFalse(config.isLegendary("minecraft:diamond_sword"));
        assertFalse(config.isLegendary("arsenal:crafting_material"));
        config.legendary_items = List.of("example:set_helmet");
        assertTrue(config.isLegendary("example:set_helmet"));
        assertFalse(config.isLegendary("arsenal:unique_claymore_1"));
        config.legendary_items = List.of();
        config.validate();
        assertFalse(config.isLegendary("example:set_helmet"));
    }
    @Test void additionalRpgUniquesAndWitcherRelicSwordsUseNebula() {
        var config = new SalvageConfig();
        for (var id : List.of(
                "bards_rpg:unique_harp_crossbow_0", "bards_rpg:unique_harp_crossbow_1",
                "bards_rpg:unique_lute_0", "bards_rpg:unique_lute_1",
                "bards_rpg:unique_lyre_0", "bards_rpg:unique_lyre_1",
                "bards_rpg:unique_rapier_0", "bards_rpg:unique_rapier_1",
                "berserker_rpg:unique_berserker_axe_1", "berserker_rpg:unique_berserker_axe_2",
                "berserker_rpg:unique_sword_1", "elemental_wizards_rpg:unique_staff_1",
                "forcemaster_rpg:unique_knuckle_0", "forcemaster_rpg:unique_knuckle_1",
                "witcher_rpg:winters_blade_sword", "witcher_rpg:ultimatum_sword",
                "witcher_rpg:azure_wrath_sword", "witcher_rpg:reach_of_the_damned_sword",
                "witcher_rpg:aerondight_sword", "witcher_rpg:iris_sword")) {
            assertTrue(config.isLegendary(id), id);
        }
    }
    @Test void ordinaryWitcherGearAndUniqueJewelryRemainExcluded() {
        var config = new SalvageConfig();
        for (var id : List.of("witcher_rpg:iron_witcher_sword", "witcher_rpg:netherite_witcher_sword",
                "witcher_rpg:grandmaster_wolven_chest", "witcher_rpg:wolf_school_medallion",
                "witcher_rpg:master_spell_book", "witcher_rpg:silver_ingot", "witcher_rpg:igni_glyph",
                "jewelry:unique_attack_ring", "additional_rpg_jewelry:unique_witcher_ring")) {
            assertFalse(config.isLegendary(id), id);
        }
    }
    @Test void invalidItemListsFailClearly() {
        for (var id : List.of("missing_namespace", "Example:item", "arsenal:*", "")) {
            var config = new SalvageConfig();
            config.legendary_items = List.of(id);
            assertThrows(IllegalArgumentException.class, config::validate);
        }
        var config = new SalvageConfig();
        config.legendary_items = null;
        assertThrows(IllegalArgumentException.class, config::validate);
    }
    @Test void ordinaryMaterialsCannotUpgradeLegendaryEquipment() {
        for (var kind : List.of(GemKind.WEAPON, GemKind.ARMOR, GemKind.ACCESSORY)) {
            assertFalse(UpgradeRules.canUpgrade(GemKind.NEBULA, 3, kind, 4));
            assertFalse(UpgradeRules.canUpgrade(kind, 3, GemKind.NEBULA, 4));
            var shard = new UpgradeRules.Fragment(GemKind.NEBULA, 4);
            assertFalse(UpgradeRules.matchingFragments(List.of(shard, shard, shard,
                    new UpgradeRules.Fragment(kind, 4))));
        }
        assertTrue(UpgradeRules.canUpgrade(GemKind.NEBULA, 3, GemKind.NEBULA, 4));
        assertFalse(UpgradeRules.canUpgrade(GemKind.NEBULA, 2, GemKind.NEBULA, 4));
        assertTrue(UpgradeRules.matchingFragments(Collections.nCopies(4,
                new UpgradeRules.Fragment(GemKind.NEBULA, 4))));
    }
}
