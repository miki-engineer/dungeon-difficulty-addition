package com.miki.dungeondifficultyaddition.scaling;

import com.miki.dungeondifficultyaddition.forge.ForgeRules;

/** Only a recorded anvil level can exceed a configured fixed level. */
final class AnvilLevelPolicy {
    private AnvilLevelPolicy() {}
    static int enforcedLevel(int fixed, int earned) {
        return Math.max(fixed, ForgeRules.validLevel(earned) ? earned : 0);
    }
    static boolean canAdvance(int current, int target, int fixed, int earned) {
        return ForgeRules.validLevel(current) && ForgeRules.validLevel(target)
                && target == current + 1
                && (enforcedLevel(fixed, earned) <= 0 || current == enforcedLevel(fixed, earned));
    }
}
