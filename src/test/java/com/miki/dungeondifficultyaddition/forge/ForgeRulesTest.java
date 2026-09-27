package com.miki.dungeondifficultyaddition.forge;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class ForgeRulesTest {
    @Test void batchCombinesOnlyMatchingLevels() {
        assertEquals(List.of(new ForgeRules.GemStack(2, 2), new ForgeRules.GemStack(5, 2), new ForgeRules.GemStack(8, 1)),
                ForgeRules.salvage(List.of(5, 2, 8, 5, 2), 1));
    }
    @Test void quantitiesSplitAtNormalStackLimit() {
        assertEquals(List.of(new ForgeRules.GemStack(5, 64), new ForgeRules.GemStack(5, 26)),
                ForgeRules.salvage(Collections.nCopies(9, 5), 10));
    }
    @Test void maximumBatchAlwaysFitsNinePreviewSlots() {
        for (int yield = 1; yield <= 64; yield++) {
            for (var levels : List.of(Collections.nCopies(9, 1), List.of(1,2,3,4,5,6,7,8,9), List.of(1,1,1,2,2,3,4,4,4))) {
                var plan = ForgeRules.salvage(levels, yield);
                assertTrue(plan.size() <= 9);
                assertEquals(9 * yield, plan.stream().mapToInt(ForgeRules.GemStack::count).sum());
                assertTrue(plan.stream().allMatch(s -> s.count() > 0 && s.count() <= 64));
            }
        }
    }
    @Test void invalidBatchFailsAsAWholeRatherThanDeletingSomeItems() {
        assertTrue(ForgeRules.salvage(List.of(5, 0, 2), 1).isEmpty());
        assertTrue(ForgeRules.salvage(List.of(5, -1), 1).isEmpty());
        assertTrue(ForgeRules.salvage(List.of(100_001), 1).isEmpty());
        assertTrue(ForgeRules.salvage(Collections.nCopies(10, 5), 1).isEmpty());
        assertTrue(ForgeRules.salvage(List.of(5), 0).isEmpty());
        assertTrue(ForgeRules.salvage(List.of(5), 65).isEmpty());
        assertTrue(ForgeRules.salvage(List.of(), 1).isEmpty());
    }
    @Test void upgradeRequiresEnoughMatchingLevelGems() {
        assertTrue(ForgeRules.canUpgrade(2, 5, 4, 4, false));
        assertTrue(ForgeRules.canUpgrade(2, 5, 12, 4, false));
        assertFalse(ForgeRules.canUpgrade(2, 5, 3, 4, false));
    }
    @Test void cannotDowngradeOrRerollSameLevel() {
        assertFalse(ForgeRules.canUpgrade(5, 5, 64, 4, false));
        assertFalse(ForgeRules.canUpgrade(8, 5, 64, 4, false));
    }
    @Test void fixedLevelItemsCannotConsumeGemsForAnImpossibleUpgrade() {
        assertFalse(ForgeRules.canUpgrade(2, 5, 64, 4, true));
    }
    @Test void supportsUnmarkedEquipmentButNotInvalidGemLevelsOrCosts() {
        assertTrue(ForgeRules.canUpgrade(0, 1, 4, 4, false));
        assertFalse(ForgeRules.canUpgrade(0, 0, 4, 4, false));
        assertFalse(ForgeRules.canUpgrade(0, 100_001, 4, 4, false));
        assertFalse(ForgeRules.canUpgrade(0, 5, 4, 0, false));
        assertFalse(ForgeRules.canUpgrade(0, 5, 4, 1, false));
        assertFalse(ForgeRules.canUpgrade(0, 5, 100, 65, false));
    }
    @Test void resultPlanIsImmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> ForgeRules.salvage(List.of(1), 1).add(new ForgeRules.GemStack(9, 64)));
    }
}
