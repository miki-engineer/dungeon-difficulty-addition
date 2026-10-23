package com.miki.dungeondifficultyaddition.scaling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LevelBonusTest {
    @Test void levelOneIsBaseStats() {
        assertEquals(0, LevelBonus.bonusLevels(0));
        assertEquals(0, LevelBonus.bonusLevels(1));
    }

    @Test void higherLevelsShiftDownByOne() {
        assertEquals(1, LevelBonus.bonusLevels(2));
        assertEquals(9, LevelBonus.bonusLevels(10));
    }
}
