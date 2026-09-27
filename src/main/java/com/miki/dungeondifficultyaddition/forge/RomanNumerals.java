package com.miki.dungeondifficultyaddition.forge;

/** Parentheses denote thousands for levels above the conventional Roman range. */
public final class RomanNumerals {
    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
    private RomanNumerals() {}
    public static String format(int level) {
        if (!ForgeRules.validLevel(level)) return "";
        if (level > 3999) return "(" + ordinary(level / 1000) + ")" + ordinary(level % 1000);
        return ordinary(level);
    }
    private static String ordinary(int number) {
        var result = new StringBuilder();
        for (int i = 0; i < VALUES.length; i++) {
            while (number >= VALUES[i]) {
                result.append(SYMBOLS[i]);
                number -= VALUES[i];
            }
        }
        return result.toString();
    }
}
