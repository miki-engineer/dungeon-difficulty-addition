package com.miki.dungeondifficultyaddition.forge;

public enum HammerTier {
    GOLD, DIAMOND, NETHERITE;
    public boolean accepts(int level, int goldLimit, int diamondLimit) {
        if (!ForgeRules.validLevel(level)) return false;
        return this == NETHERITE || level <= (this == GOLD ? goldLimit : diamondLimit);
    }
}
