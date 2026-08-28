package com.example.dungeondifficultyaddition;

import net.neoforged.fml.loading.LoadingModList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OptionalModSupport {
    private static final Map<String, Boolean> LOADED = new ConcurrentHashMap<>();

    private OptionalModSupport() {
    }

    public static boolean isLoaded(String modId) {
        return LOADED.computeIfAbsent(
                modId,
                id -> LoadingModList.get().getModFileById(id) != null
        );
    }
}
