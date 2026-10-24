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
        // Level 1 armor plus three level 6 rings in a level 6 zone.
        var gearScore = ReadinessMath.gearScore(List.of(1, 1, 1, 1, 6, 6, 6), 1000);
        assertEquals(22 / 7D, gearScore, 1e-9);
        assertTrue(ReadinessMath.incoming(6, gearScore, .25) > 1.5);
    }

    @Test void everyUpgradeLowersThePenalty() {
        var before = ReadinessMath.incoming(5, ReadinessMath.gearScore(List.of(4, 4, 4, 4), 1000), .25);
        var after = ReadinessMath.incoming(5, ReadinessMath.gearScore(List.of(5, 4, 4, 4), 1000), .25);
        assertEquals(1.25, before, 1e-9);
        assertEquals(1.1875, after, 1e-9);
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
