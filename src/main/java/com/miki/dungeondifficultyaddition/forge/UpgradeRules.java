package com.miki.dungeondifficultyaddition.forge;

import java.util.List;

public final class UpgradeRules {
    private UpgradeRules() {}
    public record Fragment(GemKind kind, int level) {}
    public static boolean matchingFragments(List<Fragment> fragments) {
        if (fragments.size() != 4) return false;
        var first = fragments.getFirst();
        return first.kind() != null && ForgeRules.validLevel(first.level())
                && fragments.stream().allMatch(first::equals);
    }
    public static boolean canUpgrade(GemKind equipment, int current, GemKind gem, int target) {
        return equipment != null && equipment == gem && ForgeRules.validLevel(current)
                && ForgeRules.validLevel(target) && target == current + 1;
    }
}
