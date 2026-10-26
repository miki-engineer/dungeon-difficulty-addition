package com.miki.dungeondifficultyaddition.scaling;

import net.minecraft.item.ItemStack;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Item scaling runs inside player and inventory ticks: one bad modded item must never break them. */
public final class ScalingErrors {
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private ScalingErrors() {}

    /** Runs scaling for one stack; a failure is logged once per operation and item, then skipped. */
    public static void guard(String operation, ItemStack stack, Runnable scaling) {
        try {
            scaling.run();
        } catch (RuntimeException | LinkageError exception) {
            warnOnce(operation, stack.isEmpty() ? "empty" : ItemTraits.of(stack).id(), exception);
        }
    }

    static void warnOnce(String operation, String subject, Throwable exception) {
        if (WARNED.add(operation + "|" + subject)) {
            LoggerFactory.getLogger("dungeon_difficulty_addition").warn(
                    "Item scaling failed during {} for {}; leaving the item unchanged. Further warnings for it suppressed.",
                    operation, subject, exception);
        }
    }
}
