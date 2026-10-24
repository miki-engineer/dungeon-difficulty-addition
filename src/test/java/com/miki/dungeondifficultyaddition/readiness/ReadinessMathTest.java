package com.miki.dungeondifficultyaddition.readiness;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReadinessMathTest {
    // Four armor slots against a level 4 mob.
    @ParameterizedTest
    @CsvSource({"4,4,4,4,4,1", "4,4,4,3,3.75,1.0625", "3,3,3,3,3,1.25",
            "2,2,2,2,2,1.5", "7,7,7,2,5.75,1", "4,4,0,0,2,1.5"})
    void incomingUsesTheAverageLevel(int a, int b, int c, int d, double expectedScore, double multiplier) {
        var gearScore = ReadinessMath.gearScore(List.of(a, b, c, d), 1000);
        assertEquals(expectedScore, gearScore, 1e-9);
        assertEquals(multiplier, ReadinessMath.incoming(4, gearScore, .25), 1e-9);
    }

    @Test void highLevelAccessoriesCannotCarryLowLevelArmor() {
        // Level 1 armor and weapon (twice) plus three level 7 rings in a level 7 zone.
        var gearScore = ReadinessMath.gearScore(List.of(1, 1, 1, 1, 1, 1), List.of(7, 7, 7), 1000);
        assertEquals(27 / 9D, gearScore, 1e-9);
        assertTrue(ReadinessMath.incoming(7, gearScore, .25) > 1.5);
    }

    // Armor x4 and weapon x2 are required; Curios only count when they raise the score.
    @ParameterizedTest
    @CsvSource({"5,1,1", "5,7,1", "3,7,2", "5,5,5", "1,0,0"})
    void curiosNeverLowerTheScore(int gear, int ringA, int ringB) {
        var required = List.of(gear, gear, gear, gear, gear, gear);
        var withRings = ReadinessMath.gearScore(required, List.of(ringA, ringB), 1000);
        assertTrue(withRings >= ReadinessMath.gearScore(required, 1000));
    }

    @Test void curiosExamples() {
        var level5 = List.of(5, 5, 5, 5, 5, 5);
        assertEquals(5, ReadinessMath.gearScore(level5, List.of(), 1000), 1e-9);
        assertEquals(5, ReadinessMath.gearScore(level5, List.of(1), 1000), 1e-9);
        assertEquals(37 / 7D, ReadinessMath.gearScore(level5, List.of(7), 1000), 1e-9);
        // Only the level 7 ring raises a 3.0 score; the level 2 ring is ignored.
        assertEquals(25 / 7D, ReadinessMath.gearScore(List.of(3, 3, 3, 3, 3, 3), List.of(2, 7), 1000), 1e-9);
    }

    @Test void everyUpgradeLowersThePenalty() {
        var before = ReadinessMath.incoming(5, ReadinessMath.gearScore(List.of(4, 4, 4, 4), 1000), .25);
        var after = ReadinessMath.incoming(5, ReadinessMath.gearScore(List.of(5, 4, 4, 4), 1000), .25);
        assertEquals(1.25, before, 1e-9);
        assertEquals(1.1875, after, 1e-9);
    }

    @Test void doubleWeightWeaponLowersBothPenalties() {
        // Level 5 armor with a level 1 weapon listed twice, in a level 5 zone.
        var gearScore = ReadinessMath.gearScore(List.of(5, 5, 5, 5, 1, 1), 1000);
        assertEquals(22 / 6D, gearScore, 1e-9);
        assertEquals(1 + (5 - 22 / 6D) * .25, ReadinessMath.incoming(5, gearScore, .25), 1e-9);
        assertEquals(1 - (5 - 22 / 6D) * .10, ReadinessMath.outgoing(5, gearScore, .10, .90), 1e-9);
    }

    @Test void emptyAndInvalidLevels() {
        assertEquals(0, ReadinessMath.gearScore(List.of(), 1000));
        assertEquals(2, ReadinessMath.gearScore(List.of(4, 4, -3, 0), 1000), 1e-9);
        assertEquals(1000, ReadinessMath.gearScore(List.of(Integer.MAX_VALUE, 1000, 5000), 1000), 1e-9);
    }

    @ParameterizedTest
    @CsvSource({"4,4,1", "4,7,1", "4,3,0.9", "4,2,0.8", "4,0,0.6", "20,0,0.1"})
    void weaponLevelPenalty(int encounter, int weapon, double expected) {
        assertEquals(expected, ReadinessMath.outgoing(encounter, weapon, .10, .90), 1e-9);
    }

    @Test void fifteenPercentOptionAndNoDamageBonus() {
        assertEquals(.7, ReadinessMath.outgoing(4, 2, .15, .90), 1e-9);
        assertEquals(1, ReadinessMath.outgoing(4, 100, .15, .90));
        assertEquals(1, ReadinessMath.incoming(4, 100, .25));
        assertEquals(1, ReadinessMath.outgoing(4, 0, 0, .90));
    }
}
