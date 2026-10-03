package com.miki.dungeondifficultyaddition.forge;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class NebulaRulesTest {
    @Test void arsenalDefaultsAreExactAndOptional() {
        var config = new SalvageConfig();
        config.validate();
        assertEquals(44, config.legendary_items.size());
        assertEquals(44, config.legendary_items.stream().distinct().count());
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
