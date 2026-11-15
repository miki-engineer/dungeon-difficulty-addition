package com.miki.dungeondifficultyaddition.scaling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LevelBonusTest {
    @Test void levelOneIsBaseStats() {
        assertEquals(0, LevelBonus.bonusLevels(0, true));
        assertEquals(0, LevelBonus.bonusLevels(1, true));
    }

    @Test void higherLevelsShiftDownByOne() {
        assertEquals(1, LevelBonus.bonusLevels(2, true));
        assertEquals(9, LevelBonus.bonusLevels(10, true));
    }

    @Test void switchedOffKeepsDungeonDifficultysOriginalLevels() {
        assertEquals(0, LevelBonus.bonusLevels(0, false));
        assertEquals(1, LevelBonus.bonusLevels(1, false));
        assertEquals(10, LevelBonus.bonusLevels(10, false));
    }
}
