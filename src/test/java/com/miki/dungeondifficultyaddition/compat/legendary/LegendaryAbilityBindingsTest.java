package com.miki.dungeondifficultyaddition.compat.legendary;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Optional production-mapping check against the user's installed upstream JAR; no game bootstrap. */
class LegendaryAbilityBindingsTest {
    @Test void packagedHooksMatchInstalledLegendaryMonstersMethods() throws Exception {
        String upstreamPath = System.getenv("DDA_LEGENDARY_TEST_JAR");
        String packagedPath = System.getenv("DDA_PACKAGED_TEST_JAR");
        assumeTrue(upstreamPath != null && packagedPath != null, "Set both JAR paths for the optional compatibility audit");
        int checked = 0;
        try (var upstream = new JarFile(Path.of(upstreamPath).toFile());
             var packaged = new JarFile(Path.of(packagedPath).toFile())) {
            for (var entry : packaged.stream().filter(e -> e.getName().contains("/mixin/legendary/")
                    && e.getName().endsWith(".class")).toList()) {
                var hook = read(packaged, entry.getName());
                var mixin = annotations(hook.visibleAnnotations, hook.invisibleAnnotations).stream()
                        .filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
                @SuppressWarnings("unchecked") var targets = (List<String>) value(mixin, "targets");
                if (targets == null) continue; // Vanilla targets are remapped by Loom.
                for (String target : targets) {
                    var actual = read(upstream, target.replace('.', '/') + ".class");
                    for (var method : hook.methods) {
                        for (var annotation : annotations(method.visibleAnnotations, method.invisibleAnnotations)) {
                            if (!annotation.desc.endsWith("/WrapMethod;")) continue;
                            @SuppressWarnings("unchecked") var selectors = (List<String>) value(annotation, "method");
                            Type[] args = Type.getArgumentTypes(method.desc);
                            String descriptor = Type.getMethodDescriptor(Type.getReturnType(method.desc),
                                    java.util.Arrays.copyOf(args, args.length - 1));
                            assertTrue(actual.methods.stream().anyMatch(m -> m.desc.equals(descriptor)
                                            && (selectors.contains(m.name) || selectors.contains(m.name + m.desc))),
                                    target + " must match " + selectors + descriptor);
                            checked++;
                        }
                    }
                }
            }
            var queue = read(upstream, "net/miauczel/legendary_monsters/LegendaryMonsters.class");
            assertTrue(queue.methods.stream().anyMatch(m -> m.name.equals("queueServerWork")
                    && m.desc.equals("(ILjava/lang/Runnable;)V")));
        }
        assertEquals(16, checked, "Every selected item/event method must be audited");
    }

    private static ClassNode read(JarFile jar, String path) throws Exception {
        var entry = jar.getJarEntry(path);
        assertNotNull(entry, path);
        var node = new ClassNode();
        try (var stream = jar.getInputStream(entry)) {
            new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        }
        return node;
    }
    private static List<AnnotationNode> annotations(List<AnnotationNode> a, List<AnnotationNode> b) {
        var result = new ArrayList<AnnotationNode>();
        if (a != null) result.addAll(a);
        if (b != null) result.addAll(b);
        return result;
    }
    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values != null) for (int i = 0; i < annotation.values.size(); i += 2)
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        return null;
    }
}
