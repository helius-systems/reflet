package systems.helius.reflet;

import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LookupManager}, covering successful acquisition, caching, denial of inaccessible
 * targets, fallback selection, and the security gate that prevents leaking cached lookups to callers
 * that are not themselves entitled.
 */
class LookupManagerTest {

    /** Full-power lookup of this (test) class; same module and nest as {@link Secret}. */
    private static final Lookup LOCAL = MethodHandles.lookup();

    /**
     * Verifies that an entitled caller receives a privileged lookup capable of reading private fields.
     *
     * @throws Throwable if the var handle cannot be resolved or read.
     */
    @Test
    void GivenAccessibleClass_WhenGetPrivilegedLookup_ThenReturnsWorkingLookup() throws Throwable {
        LookupManager manager = new LookupManager();

        Lookup lookup = manager.getPrivilegedLookup(Secret.class, LOCAL).value().orElseThrow();
        VarHandle handle = lookup.findVarHandle(Secret.class, "value", String.class);

        assertEquals("hidden", handle.get(new Secret()));
    }

    /**
     * Verifies that repeated requests for the same target reuse the cached privileged lookup.
     */
    @Test
    void GivenRepeatedRequests_WhenGetPrivilegedLookup_ThenReusesCachedLookup() {
        LookupManager manager = new LookupManager();

        Lookup first = manager.getPrivilegedLookup(Secret.class, LOCAL).value().orElseThrow();
        Lookup second = manager.getPrivilegedLookup(Secret.class, LOCAL).value().orElseThrow();

        assertSame(first, second, "the second request should reuse the cached lookup");
    }

    /**
     * Verifies that a JDK class, whose package is never opened to the class path, is denied without
     * leaking the costly {@link IllegalAccessException} as the public failure mode.
     */
    @Test
    void GivenJdkClass_WhenGetPrivilegedLookup_ThenThrowsAcquisitionException() {
        LookupManager manager = new LookupManager();

        assertTrue(manager.getPrivilegedLookup(Integer.class, LOCAL).isErr());
    }

    /**
     * Verifies that an entitled fallback is used when the primary caller cannot grant access.
     */
    @Test
    void GivenDenyingCallerButEntitledFallback_WhenGetPrivilegedLookup_ThenUsesFallback() {
        LookupManager manager = new LookupManager();

        Lookup lookup = manager.getPrivilegedLookup(Secret.class, MethodHandles.publicLookup(), LOCAL).value().orElseThrow();

        assertNotNull(lookup);
        assertEquals(Secret.class, lookup.lookupClass());
    }

    /**
     * Verifies the security gate: once a lookup is cached by an entitled caller, an unentitled caller
     * must still be denied rather than handed the cached privileged lookup.
     */
    @Test
    void GivenCachedLookup_WhenUnentitledCallerRequests_ThenStillDenied() {
        LookupManager manager = new LookupManager();
        manager.getPrivilegedLookup(Secret.class, LOCAL); // prime the cache with an entitled caller

        assertTrue(manager.getPrivilegedLookup(Secret.class, MethodHandles.publicLookup()).isErr());
    }

    /**
     * Verifies that a {@code null} primary caller is rejected and that an entitled fallback is still
     * able to acquire a privileged lookup.
     */
    @Test
    void GivenNullPrimaryCallerAndEntitledFallback_WhenGetPrivilegedLookup_ThenFallbackSucceeds() {
        LookupManager manager = new LookupManager();

        Lookup lookup = manager.getPrivilegedLookup(Secret.class, null, LOCAL).value().orElseThrow();

        assertEquals(Secret.class, lookup.lookupClass());
    }

    /**
     * Verifies that passing {@code null} as the varargs array bypasses fallback iteration and produces
     * the default denial detail.
     */
    @Test
    void GivenNoUsableCaller_WhenGetPrivilegedLookup_ThenUsesDefaultDenialDetail() {
        LookupManager manager = new LookupManager();

        String message = manager.getPrivilegedLookup(Secret.class, null, (Lookup[]) null)
                .error()
                .map(Supplier::get)
                .orElseThrow();

        assertTrue(message.contains("no usable caller lookup was provided"));
    }

    /**
     * Verifies that primitive and array targets generate specific denial reasons.
     */
    @Test
    void GivenPrimitiveAndArrayTargets_WhenGetPrivilegedLookup_ThenDenialMentionsTargetKind() {
        LookupManager manager = new LookupManager();

        String primitiveMessage = manager.getPrivilegedLookup(int.class, LOCAL)
                .error()
                .map(Supplier::get)
                .orElseThrow();
        String arrayMessage = manager.getPrivilegedLookup(String[].class, LOCAL)
                .error()
                .map(Supplier::get)
                .orElseThrow();

        assertTrue(primitiveMessage.contains("primitive type int"));
        assertTrue(arrayMessage.contains("array type java.lang.String[]"));
    }

    /**
     * Verifies that a fallback list containing {@code null} still contributes only concrete denial
     * reasons and omits the default empty-detail text.
     */
    @Test
    void GivenNullFallbackEntry_WhenGetPrivilegedLookup_ThenSkipsNullFallbackReason() {
        LookupManager manager = new LookupManager();

        String message = manager.getPrivilegedLookup(Secret.class, MethodHandles.publicLookup(), (Lookup) null)
                .error()
                .map(Supplier::get)
                .orElseThrow();

        assertTrue(message.contains("lacks the PRIVATE and MODULE lookup modes"));
        assertFalse(message.contains("no usable caller lookup was provided"));
    }

    /**
     * Verifies the race-handling path where another thread cached a lookup between cache read and
     * {@code putIfAbsent}, causing {@code tryAcquire} to return the previously cached value.
     *
     * @throws Exception if reflective access to private members fails.
     */
    @SuppressWarnings("unchecked")
    @Test
    void GivenConcurrentCacheWinner_WhenTryAcquireStoresLookup_ThenReturnsPreviousCachedLookup() throws Exception {
        LookupManager manager = new LookupManager();
        Lookup seeded = manager.getPrivilegedLookup(Secret.class, LOCAL).value().orElseThrow();

        Method tryAcquire = LookupManager.class.getDeclaredMethod("tryAcquire", Class.class, Lookup.class, Lookup.class);
        tryAcquire.setAccessible(true);
        Lookup resolved = (Lookup) tryAcquire.invoke(manager, Secret.class, LOCAL, null);

        Field cacheField = LookupManager.class.getDeclaredField("privilegedLookups");
        cacheField.setAccessible(true);
        Map<Class<?>, Lookup> cache = (Map<Class<?>, Lookup>) cacheField.get(manager);

        assertSame(seeded, resolved);
        assertSame(seeded, cache.get(Secret.class));
    }

    /**
     * Verifies that a named-module lookup can access an unnamed-module target once preconditions are met.
     *
     * @throws Exception if module compilation/loading fails.
     */
    @Test
    void GivenNamedModuleLookupAndUnnamedTarget_WhenCanAccess_ThenReturnsTrue() throws Exception {
        Lookup namedModuleLookup = compileAndLoadNamedModuleLookup();

        assertTrue(LookupManager.canAccess(Secret.class, namedModuleLookup));
    }

    /**
     * Verifies that access is denied when the caller's named module does not read the target's named
     * module.
     *
     * @throws Exception if module compilation/loading fails.
     */
    @Test
    void GivenNamedModulesWithoutReadEdge_WhenCanAccess_ThenReturnsFalse() throws Exception {
        ModuleReadabilityScenario scenario = compileAndLoadUnreadableNamedModules();

        assertFalse(LookupManager.canAccess(scenario.targetClass(), scenario.lookup()));
    }

    /**
     * Verifies the fallback denial text path in {@code describeDenial} when no explicit precondition
     * reason applies.
     *
     * @throws Exception if reflective access to private members fails.
     */
    @Test
    void GivenEntitledLookup_WhenDescribeDenialInvokedReflectively_ThenUsesPrivateLookupInFallbackText() throws Exception {
        Method describeDenial = LookupManager.class.getDeclaredMethod("describeDenial", Class.class, Lookup.class);
        describeDenial.setAccessible(true);

        String reason = (String) describeDenial.invoke(null, Secret.class, LOCAL);

        assertTrue(reason.contains("privateLookupIn denied access for"));
    }

    /**
     * Verifies that {@link LookupManager#canAccess(Class, Lookup)} mirrors the preconditions of
     * {@link MethodHandles#privateLookupIn(Class, Lookup)} for the representative cases.
     */
    @Test
    void GivenVariousTargets_WhenCanAccess_ThenMirrorsPrivateLookupInRules() {
        assertTrue(LookupManager.canAccess(Secret.class, LOCAL), "same-module nestmate is accessible");
        assertFalse(LookupManager.canAccess(Integer.class, LOCAL), "java.base does not open to the class path");
        assertFalse(LookupManager.canAccess(int.class, LOCAL), "primitives have no private lookup");
        assertFalse(LookupManager.canAccess(String[].class, LOCAL), "array types have no private lookup");
        assertFalse(LookupManager.canAccess(Secret.class, MethodHandles.publicLookup()),
                "a public lookup lacks PRIVATE/MODULE modes");
    }

    /**
     * Compiles a temporary named module and returns a full-power lookup originating from that module.
     *
     * @return a named-module lookup.
     * @throws Exception if compilation or class loading fails.
     */
    private static Lookup compileAndLoadNamedModuleLookup() throws Exception {
        Path root = Files.createTempDirectory("lookup-manager-test");
        try {
            Path src = root.resolve("src");
            Path moduleDir = src.resolve("named.lookup.module");
            Path packageDir = moduleDir.resolve(Path.of("named", "lookup"));
            Files.createDirectories(packageDir);

            Files.writeString(moduleDir.resolve("module-info.java"),
                    """
                            module named.lookup.module {
                                opens named.lookup;
                            }
                            """,
                    StandardCharsets.UTF_8);
            Files.writeString(packageDir.resolve("LookupFactory.java"),
                    """
                            package named.lookup;

                            import java.lang.invoke.MethodHandles;
                            import java.lang.invoke.MethodHandles.Lookup;

                            public final class LookupFactory {
                                private LookupFactory() {}

                                public static Lookup lookup() {
                                    return MethodHandles.lookup();
                                }
                            }
                            """,
                    StandardCharsets.UTF_8);

            Path out = root.resolve("out");
            Files.createDirectories(out);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            assertNotNull(compiler, "JDK compiler is required to run this test");
            int exitCode = compiler.run(
                    null,
                    null,
                    null,
                    "-d",
                    out.toString(),
                    moduleDir.resolve("module-info.java").toString(),
                    packageDir.resolve("LookupFactory.java").toString()
            );
            assertEquals(0, exitCode);

            ModuleFinder finder = ModuleFinder.of(out);
            Configuration configuration = ModuleLayer.boot()
                    .configuration()
                    .resolve(finder, ModuleFinder.of(), Set.of("named.lookup.module"));
            URLClassLoader loader = new URLClassLoader(new URL[]{out.toUri().toURL()}, ClassLoader.getSystemClassLoader());
            ModuleLayer layer = ModuleLayer.boot().defineModulesWithOneLoader(configuration, loader);

            ClassLoader moduleLoader = layer.findLoader("named.lookup.module");
            Class<?> factoryClass = moduleLoader.loadClass("named.lookup.LookupFactory");
            Method lookupFactory = factoryClass.getDeclaredMethod("lookup");
            return (Lookup) lookupFactory.invoke(null);
        } finally {
            deleteDirectoryRecursively(root);
        }
    }

    /**
     * Compiles two named modules where the caller module does not read the target module.
     *
     * @return a scenario containing the caller lookup and target class.
     * @throws Exception if compilation or class loading fails.
     */
    private static ModuleReadabilityScenario compileAndLoadUnreadableNamedModules() throws Exception {
        Path root = Files.createTempDirectory("lookup-manager-modules-test");
        try {
            Path src = root.resolve("src");

            Path callerModuleDir = src.resolve("caller.lookup.module");
            Path callerPackageDir = callerModuleDir.resolve(Path.of("caller", "lookup"));
            Files.createDirectories(callerPackageDir);
            Files.writeString(callerModuleDir.resolve("module-info.java"),
                    """
                            module caller.lookup.module {
                                opens caller.lookup;
                            }
                            """,
                    StandardCharsets.UTF_8);
            Files.writeString(callerPackageDir.resolve("LookupFactory.java"),
                    """
                            package caller.lookup;

                            import java.lang.invoke.MethodHandles;
                            import java.lang.invoke.MethodHandles.Lookup;

                            public final class LookupFactory {
                                private LookupFactory() {}

                                public static Lookup lookup() {
                                    return MethodHandles.lookup();
                                }
                            }
                            """,
                    StandardCharsets.UTF_8);

            Path targetModuleDir = src.resolve("target.lookup.module");
            Path targetPackageDir = targetModuleDir.resolve(Path.of("target", "lookup"));
            Files.createDirectories(targetPackageDir);
            Files.writeString(targetModuleDir.resolve("module-info.java"),
                    """
                            module target.lookup.module {
                            }
                            """,
                    StandardCharsets.UTF_8);
            Files.writeString(targetPackageDir.resolve("TargetType.java"),
                    """
                            package target.lookup;

                            public final class TargetType {
                                private final String value = "hidden";

                                public String value() {
                                    return value;
                                }
                            }
                            """,
                    StandardCharsets.UTF_8);

            Path out = root.resolve("out");
            Files.createDirectories(out);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            assertNotNull(compiler, "JDK compiler is required to run this test");
            int exitCode = compiler.run(
                    null,
                    null,
                    null,
                    "--module-source-path",
                    src.toString(),
                    "-d",
                    out.toString(),
                    callerModuleDir.resolve("module-info.java").toString(),
                    callerPackageDir.resolve("LookupFactory.java").toString(),
                    targetModuleDir.resolve("module-info.java").toString(),
                    targetPackageDir.resolve("TargetType.java").toString()
            );
            assertEquals(0, exitCode);

            ModuleFinder finder = ModuleFinder.of(out);
            Configuration configuration = ModuleLayer.boot()
                    .configuration()
                    .resolve(finder, ModuleFinder.of(), Set.of("caller.lookup.module", "target.lookup.module"));
            URLClassLoader loader = new URLClassLoader(new URL[]{out.toUri().toURL()}, ClassLoader.getSystemClassLoader());
            ModuleLayer layer = ModuleLayer.boot().defineModulesWithOneLoader(configuration, loader);

            ClassLoader callerLoader = layer.findLoader("caller.lookup.module");
            Class<?> factoryClass = callerLoader.loadClass("caller.lookup.LookupFactory");
            Method lookupFactory = factoryClass.getDeclaredMethod("lookup");
            Lookup lookup = (Lookup) lookupFactory.invoke(null);

            ClassLoader targetLoader = layer.findLoader("target.lookup.module");
            Class<?> targetClass = targetLoader.loadClass("target.lookup.TargetType");

            return new ModuleReadabilityScenario(lookup, targetClass);
        } finally {
            deleteDirectoryRecursively(root);
        }
    }

    /**
     * Deletes a directory tree recursively.
     *
     * @param root root directory to delete.
     * @throws Exception if deletion fails.
     */
    private static void deleteDirectoryRecursively(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        Files.walk(root)
                .sorted((left, right) -> right.getNameCount() - left.getNameCount())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Simple holder for cross-module readability test inputs.
     *
     * @param lookup caller lookup from a named module.
     * @param targetClass target type in a different named module.
     */
    private record ModuleReadabilityScenario(Lookup lookup, Class<?> targetClass) {
    }

    /**
     * Sample type with a private field used to validate that the acquired lookup grants private access.
     */
    private static class Secret {
        @SuppressWarnings("unused")
        private final String value = "hidden";
    }
}
