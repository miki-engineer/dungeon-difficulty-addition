package com.miki.dungeondifficultyaddition.forge;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class UpgradeRulesTest {
    @Test void ladderRequiresExactlyTheNextLevelForEveryCategory() {
        for (var kind : GemKind.values()) {
            assertTrue(UpgradeRules.canUpgrade(kind, 3, kind, 4));
            assertFalse(UpgradeRules.canUpgrade(kind, 2, kind, 4));
            assertFalse(UpgradeRules.canUpgrade(kind, 4, kind, 4));
            assertFalse(UpgradeRules.canUpgrade(kind, 5, kind, 4));
            assertFalse(UpgradeRules.canUpgrade(kind, 0, kind, 1));
            assertTrue(UpgradeRules.canUpgrade(kind, 99999, kind, 100000));
            assertFalse(UpgradeRules.canUpgrade(kind, 100000, kind, 100001));
        }
    }
    @Test void cannotUpgradeDifferentEquipmentCategory() {
        for (var equipment : GemKind.values()) for (var gem : GemKind.values()) {
            assertEquals(equipment == gem, UpgradeRules.canUpgrade(equipment, 3, gem, 4));
        }
        assertFalse(UpgradeRules.canUpgrade(null, 3, GemKind.WEAPON, 4));
    }
    @Test void requiresExactlyFourMatchingFragments() {
        for (var kind : GemKind.values()) {
            var fragment = new UpgradeRules.Fragment(kind, 4);
            assertTrue(UpgradeRules.matchingFragments(Collections.nCopies(4, fragment)));
            for (int count : new int[]{0, 1, 2, 3, 5, 9})
                assertFalse(UpgradeRules.matchingFragments(Collections.nCopies(count, fragment)));
            assertFalse(UpgradeRules.matchingFragments(List.of(fragment, fragment, fragment, new UpgradeRules.Fragment(kind, 3))));
        }
        var weapon = new UpgradeRules.Fragment(GemKind.WEAPON, 4);
        assertFalse(UpgradeRules.matchingFragments(List.of(weapon, weapon, weapon, new UpgradeRules.Fragment(GemKind.ARMOR, 4))));
        assertFalse(UpgradeRules.matchingFragments(Collections.nCopies(4, new UpgradeRules.Fragment(GemKind.WEAPON, 0))));
    }
}
