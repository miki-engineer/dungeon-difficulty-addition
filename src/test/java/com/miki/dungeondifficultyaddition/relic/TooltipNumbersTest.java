package com.miki.dungeondifficultyaddition.relic;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TooltipNumbersTest {
    @Test void boundariesProtectOtherNumbersAndPenalties() {
        var replacement = Map.of("5", "8");
        assertEquals("Damage 8", TooltipNumbers.replaceAll("Damage 5", replacement));
        assertEquals("Range 8.", TooltipNumbers.replaceAll("Range 5.", replacement));
        for (var original : new String[]{"15", "5.1", "1.5", "-5", "5%", "51"})
            assertEquals(original, TooltipNumbers.replaceAll(original, replacement));
        assertEquals("Chance 12%", TooltipNumbers.replaceAll("Chance 5%", Map.of("5%", "12%")));
    }
    @Test void commaDecimalAndGroupingLocalesAreNotSplit() {
        assertEquals("2,5", TooltipNumbers.replaceAll("2,5", Map.of("5", "8")));
        assertEquals("1,500 and 8", TooltipNumbers.replaceAll("1,500 and 5", Map.of("5", "8", "500", "9")));
        assertEquals("Radius 3,1", TooltipNumbers.replaceAll("Radius 2,5", Map.of("2,5", "3,1")));
    }
    @Test void replacesEveryMatchOnceWithoutChaining() {
        // 12 -> 10 must not then be rewritten again by the 10 -> 12 rule.
        var replacements = Map.of("12", "10", "10", "12");
        assertEquals("10 then 12, again 10", TooltipNumbers.replaceAll("12 then 10, again 12", replacements));
    }
    @Test void longerValuesWinOverTheirPrefix() {
        assertEquals("+30% and 4", TooltipNumbers.replaceAll("+20% and 2", Map.of("20%", "30%", "2", "4", "20", "99")));
    }
    @Test void changingScaledValuesCannotReuseOldOutput() {
        assertEquals("Power 8", TooltipNumbers.replaceAll("Power 5", Map.of("5", "8")));
        assertEquals("Power 9", TooltipNumbers.replaceAll("Power 5", Map.of("5", "9")));
        assertEquals("Power $1", TooltipNumbers.replaceAll("Power 5", Map.of("5", "$1")));
    }
    @Test void boundedCacheEvictionDoesNotChangeOutput() {
        for (int i = 1; i <= 300; i++)
            assertEquals("X", TooltipNumbers.replaceAll(Integer.toString(i), Map.of(Integer.toString(i), "X")));
        assertEquals("Y", TooltipNumbers.replaceAll("1", Map.of("1", "Y")));
    }
}
