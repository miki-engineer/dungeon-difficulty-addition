package com.miki.dungeondifficultyaddition.relic;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.LinkedHashMap;
import static org.junit.jupiter.api.Assertions.*;

class TooltipNumbersTest {
    @Test void boundariesProtectOtherNumbersAndPenalties() {
        var replacement = Map.of("5", "8");
        assertEquals("Damage 8", TooltipNumbers.replaceFirst("Damage 5", replacement));
        for (var original : new String[]{"15", "5.1", "1.5", "-5", "5%"})
            assertEquals(original, TooltipNumbers.replaceFirst(original, replacement));
        assertEquals("Chance 12%", TooltipNumbers.replaceFirst("Chance 5%", Map.of("5%", "12%")));
    }
    @Test void replacesOnlyFirstMatchingValueAndRespectsRuleOrder() {
        var replacements = new LinkedHashMap<String, String>();
        replacements.put("5", "8"); replacements.put("3", "6");
        assertEquals("3 and 8 and 5", TooltipNumbers.replaceFirst("3 and 5 and 5", replacements));
    }
    @Test void changingScaledValuesCannotReuseOldOutput() {
        assertEquals("Power 8", TooltipNumbers.replaceFirst("Power 5", Map.of("5", "8")));
        assertEquals("Power 9", TooltipNumbers.replaceFirst("Power 5", Map.of("5", "9")));
        assertEquals("Power $1", TooltipNumbers.replaceFirst("Power 5", Map.of("5", "$1")));
    }
    @Test void boundedCacheEvictionDoesNotChangeOutput() {
        for (int i = 1; i <= 300; i++)
            assertEquals("X", TooltipNumbers.replaceFirst(Integer.toString(i), Map.of(Integer.toString(i), "X")));
        assertEquals("Y", TooltipNumbers.replaceFirst("1", Map.of("1", "Y")));
    }
}
