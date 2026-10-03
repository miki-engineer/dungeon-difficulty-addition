package com.miki.dungeondifficultyaddition.scaling;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnvilLevelPolicyTest {
    @Test void onlyEarnedLevelsOverrideFixedLevels() {
        assertEquals(3, AnvilLevelPolicy.enforcedLevel(3, 0));
        assertEquals(4, AnvilLevelPolicy.enforcedLevel(3, 4));
        assertTrue(AnvilLevelPolicy.canAdvance(3, 4, 3, 0));
        assertTrue(AnvilLevelPolicy.canAdvance(4, 5, 3, 4));
        assertFalse(AnvilLevelPolicy.canAdvance(7, 8, 3, 0));
        assertFalse(AnvilLevelPolicy.canAdvance(7, 8, 3, 4));
        assertFalse(AnvilLevelPolicy.canAdvance(3, 5, 3, 0));
    }
    @Test void configChangesRespectEarnedProgressAndNewBaseline() {
        assertEquals(4, AnvilLevelPolicy.enforcedLevel(0, 4));
        assertEquals(4, AnvilLevelPolicy.enforcedLevel(2, 4));
        assertEquals(6, AnvilLevelPolicy.enforcedLevel(6, 4));
        assertTrue(AnvilLevelPolicy.canAdvance(6, 7, 6, 4));
    }
    @Test void regularItemsAndLevelBoundsStillWork() {
        assertTrue(AnvilLevelPolicy.canAdvance(2, 3, 0, 0));
        assertFalse(AnvilLevelPolicy.canAdvance(0, 1, 0, 0));
        assertFalse(AnvilLevelPolicy.canAdvance(100000, 100001, 0, 100000));
        assertEquals(3, AnvilLevelPolicy.enforcedLevel(3, 100001));
    }
}
