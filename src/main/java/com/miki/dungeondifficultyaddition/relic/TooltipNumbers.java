package com.miki.dungeondifficultyaddition.relic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cache syntax only, never scaled values: equipment/config changes remain visible immediately. */
public final class TooltipNumbers {
    private static final Map<String, Pattern> PATTERNS = new LinkedHashMap<>(128, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Pattern> entry) { return size() > 256; }
    };
    private TooltipNumbers() {}
    private static synchronized Pattern pattern(String original) {
        return PATTERNS.computeIfAbsent(original, value -> Pattern.compile(
                "(?<![\\d.-])" + Pattern.quote(value) + (value.endsWith("%") ? "(?![\\d.])" : "(?![\\d.%])")));
    }
    public static String replaceFirst(String line, Map<String, String> replacements) {
        for (var replacement : replacements.entrySet()) {
            var matcher = pattern(replacement.getKey()).matcher(line);
            if (matcher.find()) return matcher.replaceFirst(Matcher.quoteReplacement(replacement.getValue()));
        }
        return line;
    }
}
