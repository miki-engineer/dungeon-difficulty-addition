package com.miki.dungeondifficultyaddition.compat.legendary;

import java.util.UUID;
import java.util.function.Supplier;

/** Cast-local, immutable DD damage terms. Never retains a player or an inventory stack. */
public final class AbilityDamageScope {
    public record Cast(UUID caster, int level, float addition, float multiplyBase) {
        public float apply(float damage) {
            if (damage <= 0 || !Float.isFinite(damage)) return damage;
            // Same operation order as DD's ModifierSummary.apply.
            float result = (damage + addition) * (1 + multiplyBase);
            return Float.isFinite(result) ? Math.max(0, result) : damage;
        }
        public boolean owns(UUID attacker, UUID victim) {
            return caster.equals(attacker) && !caster.equals(victim);
        }
    }
    private static final ThreadLocal<Cast> CURRENT = new ThreadLocal<>();
    private AbilityDamageScope() {}
    public static Cast current() { return CURRENT.get(); }

    public static <T> T call(Cast cast, Supplier<T> action) {
        var previous = CURRENT.get();
        if (cast == null) CURRENT.remove(); else CURRENT.set(cast);
        try { return action.get(); }
        finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    public static void run(Cast cast, Runnable action) {
        call(cast, () -> { action.run(); return null; });
    }

    public static Runnable capture(Runnable action) {
        var cast = current();
        return cast == null ? action : () -> run(cast, action);
    }
}
