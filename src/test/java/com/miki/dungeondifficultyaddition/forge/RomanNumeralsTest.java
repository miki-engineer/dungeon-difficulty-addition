package com.miki.dungeondifficultyaddition.forge;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RomanNumeralsTest {
    @Test void ordinaryEquipmentLevels() {
        assertEquals("I", RomanNumerals.format(1));
        assertEquals("III", RomanNumerals.format(3));
        assertEquals("IV", RomanNumerals.format(4));
        assertEquals("IX", RomanNumerals.format(9));
        assertEquals("X", RomanNumerals.format(10));
        assertEquals("XIV", RomanNumerals.format(14));
        assertEquals("XXX", RomanNumerals.format(30));
        assertEquals("XL", RomanNumerals.format(40));
        assertEquals("XCIX", RomanNumerals.format(99));
        assertEquals("CDXLIV", RomanNumerals.format(444));
        assertEquals("CMXCIX", RomanNumerals.format(999));
        assertEquals("MMMCMXCIX", RomanNumerals.format(3999));
    }
    @Test void extendedRangeIsBoundedAndUnambiguous() {
        assertEquals("(IV)", RomanNumerals.format(4000));
        assertEquals("(IV)I", RomanNumerals.format(4001));
        assertEquals("(C)", RomanNumerals.format(100000));
        assertEquals("", RomanNumerals.format(0));
        assertEquals("", RomanNumerals.format(-1));
        assertEquals("", RomanNumerals.format(Integer.MAX_VALUE));
    }
}
