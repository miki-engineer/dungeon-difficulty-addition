package com.miki.dungeondifficultyaddition.scaling;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DynamicAttributeLookupTest {
    /** Stands in for a client-only class such as PoseStack. */
    public static final class ClientOnlyType {}

    /** A modded item with a client-only type in one of its method signatures. */
    public static final class ItemWithClientMethod {
        public void render(ClientOnlyType context) {}
    }

    /** Refuses ClientOnlyType the way the dedicated server's dist cleaner refuses client classes. */
    private static final class DedicatedServerLoader extends ClassLoader {
        DedicatedServerLoader() { super(DynamicAttributeLookupTest.class.getClassLoader()); }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals(ClientOnlyType.class.getName())) {
                throw new RuntimeException("Attempted to load class " + name + " for invalid dist DEDICATED_SERVER");
            }
            if (!name.equals(ItemWithClientMethod.class.getName())) return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                var loaded = findLoadedClass(name);
                if (loaded != null) return loaded;
                try (var in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    var bytes = in.readAllBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (IOException exception) {
                    throw new ClassNotFoundException(name, exception);
                }
            }
        }
    }

    @Test void clientOnlySignatureDoesNotThrow() throws Exception {
        var item = Class.forName(ItemWithClientMethod.class.getName(), false, new DedicatedServerLoader());
        // The reflection itself fails, exactly as it did on the server...
        assertThrows(RuntimeException.class, () -> item.getMethod("toString"));
        // ...but the lookup now treats the item as having static defaults instead of crashing a tick.
        assertFalse(DungeonDifficultyNativeScaling.hasDynamicDefaultAttributes(item));
    }

    @Test void plainClassHasNoDynamicDefaults() {
        assertFalse(DungeonDifficultyNativeScaling.hasDynamicDefaultAttributes(String.class));
    }
}
