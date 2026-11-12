package com.miki.dungeondifficultyaddition.relic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cache syntax only, never scaled values: equipment/config changes remain visible immediately. */
public final class TooltipNumbers {
    // Not part of a longer number: no digit or minus sign before it, and no digit after it,
    // also across a decimal or grouping separator in either locale ("2.5", "2,5", "1,500").
    private static final String BEFORE = "(?<!\\d[.,]|[\\d-])";
    private static final String AFTER = "(?![\\d]|[.,]\\d)";
    private static final Map<List<String>, Pattern> PATTERNS = new LinkedHashMap<>(128, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<List<String>, Pattern> entry) { return size() > 256; }
    };
    private TooltipNumbers() {}

    private static synchronized Pattern pattern(List<String> keys) {
        return PATTERNS.computeIfAbsent(keys, sorted -> {
            var alternatives = new ArrayList<String>();
            for (var key : sorted) {
                // A plain number must not match the number of a percentage.
                alternatives.add(Pattern.quote(key) + (key.endsWith("%") ? "" : "(?!%)"));
            }
            return Pattern.compile(BEFORE + "(?:" + String.join("|", alternatives) + ")" + AFTER);
        });
    }

    /** Replaces every listed number in one pass, so a replaced value is never matched again. */
    public static String replaceAll(String line, Map<String, String> replacements) {
        if (line.isEmpty() || replacements.isEmpty()) return line;
        // Longest first, so "12.5%" wins over "12.5" and "2.5" over "2".
        var keys = new ArrayList<>(replacements.keySet());
        keys.sort(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()));
        var matcher = pattern(List.copyOf(keys)).matcher(line);
        if (!matcher.find()) return line;
        var result = new StringBuilder(line.length() + 8);
        do {
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacements.get(matcher.group())));
        } while (matcher.find());
        matcher.appendTail(result);
        return result.toString();
    }
}
