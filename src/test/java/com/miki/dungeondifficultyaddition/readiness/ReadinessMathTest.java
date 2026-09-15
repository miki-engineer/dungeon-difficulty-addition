package com.miki.dungeondifficultyaddition.readiness;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReadinessMathTest {
    @ParameterizedTest
    @CsvSource({"4,4,4,0,4,1", "4,4,0,4,4,1", "3,3,3,2,3,1.25",
            "2,2,2,2,2,1.5", "7,7,7,2,7,1", "4,4,0,0,0,2"})
    void agreedIncomingExamples(int a, int b, int c, int d, int expectedReadiness, double multiplier) {
        var readiness = ReadinessMath.readiness(List.of(a, b, c, d), 3, 1000);
        assertEquals(expectedReadiness, readiness);
        assertEquals(multiplier, ReadinessMath.incoming(4, readiness, .25), 1e-9);
    }

    @Test void threeAccessoriesCanSatisfyTheRule() {
        assertEquals(4, ReadinessMath.readiness(List.of(4, 4, 4), 3, 1000));
    }

    @Test void tooFewSlotsAndInvalidLevels() {
        assertEquals(0, ReadinessMath.readiness(List.of(4, 4), 3, 1000));
        assertEquals(0, ReadinessMath.readiness(List.of(4, 4, -3), 3, 1000));
        assertEquals(1000, ReadinessMath.readiness(List.of(Integer.MAX_VALUE, 1000, 5000), 3, 1000));
        assertEquals(0, ReadinessMath.readiness(List.of(), 3, 1000));
    }

    @Test void requiredItemCountIsConfigurable() {
        assertEquals(5, ReadinessMath.readiness(List.of(7, 5, 2), 2, 1000));
        assertEquals(7, ReadinessMath.readiness(List.of(7, 5, 2), 1, 1000));
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
