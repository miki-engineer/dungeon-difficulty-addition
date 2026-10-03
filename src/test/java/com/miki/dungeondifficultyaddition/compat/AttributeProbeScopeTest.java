package com.miki.dungeondifficultyaddition.compat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttributeProbeScopeTest {
    @Test void onlyExactProbeIsOverriddenAndNestedReadsRestoreTheOuterProbe() {
        var outer = new Object();
        var inner = new Object();
        assertFalse(AttributeProbeScope.contains(outer));
        assertEquals(42, AttributeProbeScope.read(outer, () -> {
            assertTrue(AttributeProbeScope.contains(outer));
            assertFalse(AttributeProbeScope.contains(inner));
            AttributeProbeScope.read(inner, () -> {
                assertTrue(AttributeProbeScope.contains(inner));
                assertFalse(AttributeProbeScope.contains(outer));
                return 0;
            });
            assertTrue(AttributeProbeScope.contains(outer));
            return 42;
        }));
        assertFalse(AttributeProbeScope.contains(outer));
        assertFalse(AttributeProbeScope.contains(null));
    }

    @Test void exceptionsAndOtherThreadsDoNotLeakTheOverride() {
        var probe = new Object();
        assertThrows(IllegalStateException.class, () -> AttributeProbeScope.read(probe, () -> {
            assertFalse(java.util.concurrent.CompletableFuture.supplyAsync(
                    () -> AttributeProbeScope.contains(probe)).join());
            throw new IllegalStateException("test failure");
        }));
        assertFalse(AttributeProbeScope.contains(probe));
    }
}
