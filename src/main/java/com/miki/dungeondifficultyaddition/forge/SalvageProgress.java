package com.miki.dungeondifficultyaddition.forge;

/** Server-side strike state. A held/repeated packet cannot skip the interval. */
public record SalvageProgress(int hits, long nextAllowedTick) {
    public static final int REQUIRED_HITS = 3, INTERVAL_TICKS = 6;
    public SalvageProgress {
        if (hits < 0 || hits > REQUIRED_HITS) throw new IllegalArgumentException("Invalid strike count");
    }
    public SalvageProgress strike(long now) {
        if (complete() || now < nextAllowedTick) return this;
        return new SalvageProgress(hits + 1, now + INTERVAL_TICKS);
    }
    public boolean complete() { return hits == REQUIRED_HITS; }
}
