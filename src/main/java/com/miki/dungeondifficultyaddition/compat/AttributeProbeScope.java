package com.miki.dungeondifficultyaddition.compat;

import java.util.function.Supplier;

/** Identity-scoped override; never changes real inventory stacks or other threads. */
public final class AttributeProbeScope {
    private static final ThreadLocal<Object> CURRENT = new ThreadLocal<>();

    private AttributeProbeScope() {}

    public static boolean contains(Object stack) {
        return stack != null && CURRENT.get() == stack;
    }

    static <T> T read(Object stack, Supplier<T> reader) {
        Object previous = CURRENT.get();
        CURRENT.set(stack);
        try {
            return reader.get();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
