package com.miki.dungeondifficultyaddition.compat.legendary;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

class AbilityDamageScopeTest {
    private static final UUID CASTER = UUID.randomUUID();
    private static AbilityDamageScope.Cast cast(float add, float multiply) {
        return new AbilityDamageScope.Cast(CASTER, 4, add, multiply);
    }

    @Test void matchesDdAdditionThenMultiplyOrder() {
        assertEquals(19.2f, cast(2, .6f).apply(10), .0001f);
        assertEquals(16, cast(0, .6f).apply(10), .0001f);
        assertEquals(10, cast(0, 0).apply(10));
    }

    @Test void preservesUpstreamAbilityDamageAndRejectsInvalidResults() {
        assertEquals(32, cast(0, .6f).apply(10 * 2), .0001f);
        assertEquals(0, cast(5, .6f).apply(0));
        assertEquals(0, cast(-20, 0).apply(10));
        assertEquals(10, cast(Float.MAX_VALUE, Float.MAX_VALUE).apply(10));
    }

    @Test void onlyCasterOwnedDamageToOtherEntitiesQualifies() {
        var cast = cast(0, .6f);
        assertTrue(cast.owns(CASTER, UUID.randomUUID()));
        assertFalse(cast.owns(CASTER, CASTER));
        assertFalse(cast.owns(null, UUID.randomUUID()));
        assertFalse(cast.owns(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test void scopeRestoresAfterNestedHitsAndExceptions() {
        var original = cast(0, .6f);
        AbilityDamageScope.run(original, () -> {
            AbilityDamageScope.run(null, () -> assertNull(AbilityDamageScope.current()));
            assertSame(original, AbilityDamageScope.current());
            assertThrows(IllegalStateException.class, () -> AbilityDamageScope.run(cast(1, 2), () -> {
                throw new IllegalStateException("test");
            }));
            assertSame(original, AbilityDamageScope.current());
            assertNull(CompletableFuture.supplyAsync(AbilityDamageScope::current).join());
        });
        assertNull(AbilityDamageScope.current());
    }

    @Test void delayedWorkKeepsOriginalCastDespiteWeaponSwitchOrAnotherCaster() {
        var original = cast(0, .6f);
        Runnable task = AbilityDamageScope.call(original, () -> AbilityDamageScope.capture(() -> {
            assertSame(original, AbilityDamageScope.current());
            assertEquals(16, AbilityDamageScope.current().apply(10), .0001f);
        }));
        AbilityDamageScope.run(new AbilityDamageScope.Cast(UUID.randomUUID(), 9, 5, 2), task);
        assertNull(AbilityDamageScope.current());
        task.run();
        assertNull(AbilityDamageScope.current());
    }

    @Test void normalAttacksAndUnscopedTasksStayUnchanged() {
        Runnable task = () -> assertNull(AbilityDamageScope.current());
        assertSame(task, AbilityDamageScope.capture(task));
        task.run();
    }
}
