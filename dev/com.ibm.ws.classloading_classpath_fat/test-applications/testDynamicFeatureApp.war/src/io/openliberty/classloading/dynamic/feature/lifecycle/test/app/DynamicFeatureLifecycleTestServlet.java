/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.classloading.dynamic.feature.lifecycle.test.app;

import javax.servlet.annotation.WebServlet;

import org.junit.Assert;

import componenttest.app.FATServlet;
import io.openliberty.classloading.feature.api.TestFeatureApi;
import io.openliberty.classloading.feature.api.TestFeatureApi2;
import io.openliberty.classloading.feature.api.TestFeatureApi3;

/**
 * Single consolidated probe servlet for the dynamic-feature-lifecycle
 * classloader tests.
 *
 * <p>The probe methods are dispatched by name via the {@code testMethod=} query
 * parameter (standard {@link FATServlet} dispatch). None are annotated with
 * {@code @Test} — each FAT test class walks its lifecycle states in one ordered
 * sequence via a single {@code @Test} method.
 *
 * <p>The WAR artifact is deployed to servers whose {@code server.xml}
 * configurations wire up different classloader chains:
 * <ul>
 *   <li><b>dynamicFeatureLifecycleTest</b> — WAR directly depends on feature API.
 *       Uses four in-WAR classes, one per lifecycle probe.</li>
 *   <li><b>dynamicFeatureSharedLibTest</b> — WAR → shared library → feature API.
 *       Uses four shared-library classes, one per lifecycle probe.</li>
 *   <li><b>dynamicFeatureLibJarRemovedTest</b> — WAR → removable shared-library JAR
 *       → feature API. Uses four shared-library classes, one per lifecycle probe.</li>
 * </ul>
 *
 * <h3>Why one class per lifecycle state?</h3>
 * {@code ClassLoader.loadClass()} calls {@code findLoadedClass()} first. If the
 * class was already loaded in a prior state it is returned from the cache and
 * no delegation chain walk occurs — hiding the true classloader behaviour under
 * test. Using a distinct, previously-unloaded class per state forces a genuine
 * re-lookup each time.
 *
 * <p>Uses {@code getClass().getClassLoader()} and {@code loader.loadClass()} rather
 * than the TCCL / {@code Class.forName} — avoids the {@code ThreadContextClassLoader}
 * wrapper and static-initialiser side-effects.
 */
@WebServlet("/DynamicFeatureLifecycleTestServlet")
public class DynamicFeatureLifecycleTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    // ── Feature API (defined by the OSGi bundle, loaded via parent delegation) ──
    private static final String FEATURE_API_CLASS =
        "io.openliberty.classloading.feature.api.TestFeatureApi";

    // ── Test 1: in-WAR impl classes ───────────────────────────────────────────
    // State 1: initial load, feature present; implements TestFeatureApi
    private static final String APP_IMPL_CLASS_STATE1 =
        "io.openliberty.classloading.dynamic.feature.test.app.AppFeatureApiImplState1";
    // State 2a success probe: cached TestFeatureApi — expects success even after removal
    private static final String APP_IMPL_CLASS_STATE2A =
        "io.openliberty.classloading.dynamic.feature.test.app.AppFeatureApiImplState2a";
    // State 2b NCDFE probe: fresh TestFeatureApi2 — expects NoClassDefFoundError after removal
    private static final String APP_IMPL_CLASS_STATE2B =
        "io.openliberty.classloading.dynamic.feature.test.app.AppFeatureApiImplState2b";
    // State 3: fresh TestFeatureApi3 — expects success after feature re-add
    private static final String APP_IMPL_CLASS_STATE3 =
        "io.openliberty.classloading.dynamic.feature.test.app.AppFeatureApiImplState3";

    // ── Tests 2 & 3: shared-library impl classes ──────────────────────────────
    // State 1: initial load, feature present; implements TestFeatureApi
    private static final String LIB_IMPL_CLASS_STATE1 =
        "io.openliberty.classloading.shared.feature.lib.LibraryFeatureApiImplState1";
    // State 2a success probe: cached TestFeatureApi — expects success even after removal
    private static final String LIB_IMPL_CLASS_STATE2A =
        "io.openliberty.classloading.shared.feature.lib.LibraryFeatureApiImplState2a";
    // State 2b NCDFE probe: fresh TestFeatureApi2 — expects NoClassDefFoundError/CNFE after removal
    private static final String LIB_IMPL_CLASS_STATE2B =
        "io.openliberty.classloading.shared.feature.lib.LibraryFeatureApiImplState2b";
    // State 3: fresh TestFeatureApi3 — expects success after feature/library re-add
    private static final String LIB_IMPL_CLASS_STATE3 =
        "io.openliberty.classloading.shared.feature.lib.LibraryFeatureApiImplState3";

    // =========================================================================
    // Test 1 — Application directly depends on feature API (no shared library)
    // =========================================================================

    /**
     * Test 1, State 1: verifies that the in-WAR feature API implementation is
     * reachable and that {@code doWork()} succeeds when the feature is present.
     * <p>
     * {@link AppFeatureApiImplState1} (the State 1 class) is newly loaded here — it has
     * not been touched before this probe, so {@code findLoadedClass()} will not
     * short-circuit the lookup.
     */
    public void testAppDirectlyDependsOnFeatureApi_FeaturePresent() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_TEST STATE1 - AppCL: " + cl);

        // Load the State 1 in-WAR impl — first load, guaranteed fresh lookup.
        Class<?> implClass = cl.loadClass(APP_IMPL_CLASS_STATE1);
        println("DYNAMIC_FEATURE_TEST STATE1 - AppFeatureApiImplState1 loaded by: "
            + implClass.getClassLoader());

        Object impl = implClass.getDeclaredConstructor().newInstance();
        TestFeatureApi api = (TestFeatureApi) impl;

        String result = api.doWork();
        Assert.assertNotNull("STATE1: doWork() returned null", result);
        Assert.assertFalse("STATE1: doWork() returned empty string", result.isEmpty());
        println("DYNAMIC_FEATURE_TEST STATE1 - SUCCESS: " + result);
    }

    /**
     * Test 1, State 2a: verifies that {@link AppFeatureApiImplState2a} — which
     * implements the already-cached {@link TestFeatureApi} — still loads successfully
     * after the feature bundle has been dynamically removed.
     * <p>
     * Because {@code TestFeatureApi} was loaded in State 1, it remains in the WAR
     * classloader's internal cache. The JVM resolves the supertype from the cache
     * without walking the delegation chain to the (now-absent) bundle, so the load
     * succeeds. This documents the "app classloader is not recycled on feature removal"
     * invariant.
     */
    public void testAppDirectlyDependsOnFeatureApi_FeatureRemoved_CachedInterface() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_TEST STATE2a - AppCL: " + cl);

        try {
            Class<?> removedClass = cl.loadClass(APP_IMPL_CLASS_STATE2A);
            println("DYNAMIC_FEATURE_TEST STATE2a - CLASS_STILL_VISIBLE (cached interface, expected): "
                + "AppFeatureApiImplState2a loaded by " + removedClass.getClassLoader());
            if (!removedClass.getClassLoader().toString().contains("WebModule")) {
                Assert.fail("STATE2a: unexpected classloader for AppFeatureApiImplState2a: "
                    + removedClass.getClassLoader());
            }
            println("DYNAMIC_FEATURE_TEST STATE2a - SUCCESS");
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            Assert.fail("STATE2a: AppFeatureApiImplState2a (cached interface) should have loaded but got: "
                + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Test 1, State 2b: verifies that loading {@link AppFeatureApiImplState2b} — which
     * implements the never-before-seen {@link TestFeatureApi2} — produces a
     * {@link NoClassDefFoundError} after the feature bundle has been dynamically removed.
     * <p>
     * {@code TestFeatureApi2} was never referenced in State 1, so it is not in any
     * classloader's cache. When the JVM attempts to resolve it while loading
     * {@code AppFeatureApiImplState2b}, it must walk the full delegation chain to the
     * (now-absent) feature bundle. That walk fails with {@code NoClassDefFoundError},
     * providing genuine evidence that the classloader correctly observes the feature
     * as absent — not a cache hit masking the removal.
     */
    public void testAppDirectlyDependsOnFeatureApi_FeatureRemoved_FreshInterface() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_TEST STATE2b - AppCL: " + cl);

        try {
            Class<?> implClass = cl.loadClass(APP_IMPL_CLASS_STATE2B);
            println("DYNAMIC_FEATURE_TEST STATE2b - CLASS_STILL_VISIBLE (unexpected — stale loader): "
                + "AppFeatureApiImplState2b loaded by " + implClass.getClassLoader());
            Assert.fail("STATE2b: AppFeatureApiImplState2b (fresh TestFeatureApi2 interface) should have produced "
                + "NoClassDefFoundError but loaded successfully — baseline has changed. "
                + "Update this assertion if the fix is intentional.");
        } catch (NoClassDefFoundError ncdfe) {
            // Expected: the feature bundle is gone and TestFeatureApi2 was never cached.
            println("DYNAMIC_FEATURE_TEST STATE2b - NCDFE (expected — fresh interface from absent bundle): "
                + ncdfe.getMessage());
            println("DYNAMIC_FEATURE_TEST STATE2b - NCDFE");
        } catch (ClassNotFoundException cnfe) {
            // Also acceptable if Liberty evicts the app classloader on feature removal.
            println("DYNAMIC_FEATURE_TEST STATE2b - CNFE (stale loader evicted — acceptable): "
                + cnfe.getMessage());
            println("DYNAMIC_FEATURE_TEST STATE2b - CNFE");
        }
    }

    /**
     * Test 1, State 3: verifies that all four in-WAR impl classes are loadable after
     * the feature has been re-added.
     * <p>
     * {@link AppFeatureApiImplState3} is the State 3 class (fresh, not cached). All four
     * impl classes are then loaded to confirm the WAR classloader is fully functional
     * after the feature re-add.
     * <p>
     * <b>Expected baseline (pre-fix):</b> All four loads succeed because the same
     * OSGi bundle object was reused on re-add (same {@code EquinoxClassLoader} instance,
     * same bundle id — Failure Mode A). The cast succeeds for the wrong reason: the
     * stale never-invalidated loader is still in place.
     */
    public void testAppDirectlyDependsOnFeatureApi_FeatureReAdded() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_TEST STATE3 - AppCL: " + cl);

        try {
            // Load the State 3 impl first — fresh TestFeatureApi3, not cached from earlier states.
            Class<?> implClass3 = cl.loadClass(APP_IMPL_CLASS_STATE3);
            println("DYNAMIC_FEATURE_TEST STATE3 - AppFeatureApiImplState3 loaded by: "
                + implClass3.getClassLoader());
            TestFeatureApi3 api3 = (TestFeatureApi3) implClass3.getDeclaredConstructor().newInstance();
            String result3 = api3.doWork();
            Assert.assertNotNull("STATE3: AppFeatureApiImplState3.doWork() returned null", result3);
            println("DYNAMIC_FEATURE_TEST STATE3 - AppFeatureApiImplState3 SUCCESS: " + result3);

            // Verify all four impl classes load cleanly after the re-add.
            Class<?> implClass1 = cl.loadClass(APP_IMPL_CLASS_STATE1);
            TestFeatureApi api1 = (TestFeatureApi) implClass1.getDeclaredConstructor().newInstance();
            Assert.assertNotNull("STATE3: AppFeatureApiImplState1.doWork() returned null", api1.doWork());
            println("DYNAMIC_FEATURE_TEST STATE3 - AppFeatureApiImplState1 re-load SUCCESS");

            Class<?> removedClass = cl.loadClass(APP_IMPL_CLASS_STATE2A);
            TestFeatureApi apiRemoved = (TestFeatureApi) removedClass.getDeclaredConstructor().newInstance();
            Assert.assertNotNull("STATE3: AppFeatureApiImplState2a.doWork() returned null", apiRemoved.doWork());
            println("DYNAMIC_FEATURE_TEST STATE3 - AppFeatureApiImplState2a re-load SUCCESS");

            Class<?> implClass2 = cl.loadClass(APP_IMPL_CLASS_STATE2B);
            TestFeatureApi2 api2 = (TestFeatureApi2) implClass2.getDeclaredConstructor().newInstance();
            Assert.assertNotNull("STATE3: AppFeatureApiImplState2b.doWork() returned null", api2.doWork());
            println("DYNAMIC_FEATURE_TEST STATE3 - AppFeatureApiImplState2b re-load SUCCESS");

            println("DYNAMIC_FEATURE_TEST STATE3 - SUCCESS: all impls loaded and invoked");

        } catch (ClassCastException cce) {
            println("DYNAMIC_FEATURE_TEST STATE3 - CLASSCAST (Failure Mode B): " + cce.getMessage());
            Assert.fail("STATE3: unexpected ClassCastException — baseline behaviour has changed. "
                + "Update this assertion if the change is intentional.");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_TEST STATE3 - CNFE: " + cnfe.getMessage());
            Assert.fail("STATE3: unexpected ClassNotFoundException after feature re-add: "
                + cnfe.getMessage());
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_TEST STATE3 - NCDFE: " + ncdfe.getMessage());
            Assert.fail("STATE3: unexpected NoClassDefFoundError: " + ncdfe.getMessage());
        }
    }

    // =========================================================================
    // Test 2 — Application → shared library → feature API
    // =========================================================================

    /**
     * Test 2, State 1: verifies that the shared-library implementation is reachable
     * and that {@code doWork()} succeeds through the library chain when the feature
     * is present.
     * <p>
     * Only the library impl class is loaded via the WAR classloader — the WAR's
     * {@code AppClassLoader} is intentionally <em>not</em> made the initiating
     * classloader for {@code TestFeatureApi} directly. The feature API class is
     * loaded transitively by the shared-library classloader when it resolves
     * {@code LibraryFeatureApiImplState1}'s supertype. This keeps the WAR loader out of
     * the feature API delegation path and isolates the test to the library loader.
     */
    public void testLibraryDependsOnFeatureApi_FeaturePresent() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE1 - WAR ClassLoader: " + cl);

        // Load only the State 1 library impl — do NOT call cl.loadClass(FEATURE_API_CLASS)
        // directly, to avoid making the WAR classloader the initiating CL for TestFeatureApi.
        Class<?> implClass = cl.loadClass(LIB_IMPL_CLASS_STATE1);
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE1 - LibraryFeatureApiImplState1 loaded by: "
            + implClass.getClassLoader());

        Object impl = implClass.getDeclaredConstructor().newInstance();
        TestFeatureApi api = (TestFeatureApi) impl;

        String result = api.doWork();
        Assert.assertNotNull("STATE1: doWork() returned null", result);
        Assert.assertFalse("STATE1: doWork() returned empty string", result.isEmpty());
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE1 - SUCCESS: " + result);
    }

    /**
     * Test 2, State 2a: verifies that {@code LibraryFeatureApiImplState2a} — which
     * implements the already-cached {@link TestFeatureApi} — still loads successfully
     * after the feature bundle has been dynamically removed.
     * <p>
     * Because {@code TestFeatureApi} was loaded by the shared-library classloader in
     * State 1, it remains in that loader's internal cache. The load succeeds without
     * the delegation chain reaching the absent bundle, documenting that the shared-library
     * {@code AppClassLoader} is not recycled on feature removal.
     */
    public void testLibraryDependsOnFeatureApi_FeatureRemoved_CachedInterface() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2a - WAR ClassLoader: " + cl);

        try {
            Class<?> removedClass = cl.loadClass(LIB_IMPL_CLASS_STATE2A);
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2a - CLASS_STILL_VISIBLE (cached interface, expected): "
                + "LibraryFeatureApiImplState2a loaded by " + removedClass.getClassLoader());
            if (!removedClass.getClassLoader().toString().contains("testFeatureSharedLib")) {
                Assert.fail("STATE2a: unexpected classloader for LibraryFeatureApiImplState2a: "
                    + removedClass.getClassLoader());
            }
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2a - SUCCESS");
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            Assert.fail("STATE2a: LibraryFeatureApiImplState2a (cached interface) should have loaded but got: "
                + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Test 2, State 2b: verifies that loading {@code LibraryFeatureApiImplState2b} — which
     * implements the never-before-seen {@link TestFeatureApi2} — produces a
     * {@link NoClassDefFoundError} after the feature bundle has been dynamically removed.
     * <p>
     * {@code TestFeatureApi2} was never referenced in State 1, so a cold delegation
     * walk is forced through the shared-library loader to the absent bundle, producing
     * the expected {@code NoClassDefFoundError}.
     */
    public void testLibraryDependsOnFeatureApi_FeatureRemoved_FreshInterface() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - WAR ClassLoader: " + cl);

        try {
            Class<?> implClass = cl.loadClass(LIB_IMPL_CLASS_STATE2B);
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - CLASS_STILL_VISIBLE (unexpected): "
                + "LibraryFeatureApiImplState2b loaded by " + implClass.getClassLoader());
            Assert.fail("STATE2b: LibraryFeatureApiImplState2b (fresh TestFeatureApi2 interface) should have produced "
                + "NoClassDefFoundError but loaded successfully — baseline has changed. "
                + "Update this assertion if the fix is intentional.");
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - NCDFE (expected — fresh interface from absent bundle): "
                + ncdfe.getMessage());
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - NCDFE");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - CNFE (stale loader evicted — acceptable): "
                + cnfe.getMessage());
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - CNFE");
        }
    }

    /**
     * Test 2, State 3: verifies that all four shared-library impl classes are loadable
     * after the feature has been re-added.
     * <p>
     * {@code LibraryFeatureApiImplState3} is the State 3 class (fresh, not cached). All four
     * library impl classes are then loaded to confirm the full library chain is functional
     * after the feature re-add.
     * <p>
     * <b>Failure Mode B watch:</b> If Liberty produces a new bundle revision on re-add
     * while the stale library {@code AppClassLoader} is still cached in {@code aclStore},
     * the cast will fail with {@code ClassCastException}. This is the architectural bug
     * described in the background document. The assertion is locked in to the currently
     * observed {@code SUCCESS} baseline and will fail loudly if behaviour changes.
     */
    public void testLibraryDependsOnFeatureApi_FeatureReAdded() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - WAR ClassLoader: " + cl);

        try {
            // Load the State 3 library impl first — fresh TestFeatureApi3, not cached.
            Class<?> implClass3 = cl.loadClass(LIB_IMPL_CLASS_STATE3);
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - LibraryFeatureApiImplState3 loaded by: "
                + implClass3.getClassLoader());
            TestFeatureApi3 api3 = (TestFeatureApi3) implClass3.getDeclaredConstructor().newInstance();
            String result3 = api3.doWork();
            Assert.assertNotNull("STATE3: LibraryFeatureApiImplState3.doWork() returned null", result3);
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - LibraryFeatureApiImplState3 SUCCESS: " + result3);

            // Verify all four library impls load cleanly after the re-add.
            Class<?> implClass1 = cl.loadClass(LIB_IMPL_CLASS_STATE1);
            TestFeatureApi api1 = (TestFeatureApi) implClass1.getDeclaredConstructor().newInstance();
            Assert.assertNotNull("STATE3: LibraryFeatureApiImplState1.doWork() returned null", api1.doWork());
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - LibraryFeatureApiImplState1 re-load SUCCESS");

            Class<?> removedClass = cl.loadClass(LIB_IMPL_CLASS_STATE2A);
            TestFeatureApi apiRemoved = (TestFeatureApi) removedClass.getDeclaredConstructor().newInstance();
            Assert.assertNotNull("STATE3: LibraryFeatureApiImplState2a.doWork() returned null", apiRemoved.doWork());
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - LibraryFeatureApiImplState2a re-load SUCCESS");

            Class<?> implClass2 = cl.loadClass(LIB_IMPL_CLASS_STATE2B);
            TestFeatureApi2 api2 = (TestFeatureApi2) implClass2.getDeclaredConstructor().newInstance();
            Assert.assertNotNull("STATE3: LibraryFeatureApiImplState2b.doWork() returned null", api2.doWork());
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - LibraryFeatureApiImplState2b re-load SUCCESS");

            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - SUCCESS: all impls loaded and invoked");

        } catch (ClassCastException cce) {
            // Failure Mode B: new bundle revision produced on re-add; stale library loader
            // still cached in aclStore — type mismatch detected. This is the bug.
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - CLASSCAST (Failure Mode B): "
                + cce.getMessage());
            Assert.fail("STATE3: ClassCastException observed — Failure Mode B confirmed. "
                + "The shared-library AppClassLoader is stale and Liberty produced a new bundle revision. "
                + "Update this assertion to expect CLASSCAST if that is the intended baseline.");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - CNFE: " + cnfe.getMessage());
            Assert.fail("STATE3: unexpected ClassNotFoundException after feature re-add: "
                + cnfe.getMessage());
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - NCDFE: " + ncdfe.getMessage());
            Assert.fail("STATE3: unexpected NoClassDefFoundError: " + ncdfe.getMessage());
        }
    }

    // =========================================================================
    // Test 3 — Library JAR removed from shared library fileset
    // =========================================================================

    /**
     * Test 3, State 1: verifies that the shared-library implementation is reachable
     * and that {@code doWork()} succeeds through the library chain when both the
     * library JAR and the feature are present.
     * <p>
     * Classloader chain:
     * <pre>
     *   WAR AppClassLoader
     *     └─ Shared-library AppClassLoader  (cached in aclStore)
     *          └─ GatewayClassLoader
     *               └─ EquinoxClassLoader [test.feature.api]
     * </pre>
     * {@code LibraryFeatureApiImplState1} is the State 1 class — fresh, not yet loaded,
     * so {@code findLoadedClass()} does not short-circuit the lookup.
     * <p>
     * Only the library impl class is loaded via the WAR classloader — the WAR's
     * {@code AppClassLoader} is intentionally <em>not</em> made the initiating
     * classloader for {@code TestFeatureApi} directly. The feature API class is
     * resolved transitively by the shared-library classloader when it resolves
     * {@code LibraryFeatureApiImplState1}'s supertype.
     */
    public void testLibraryJarRemovedFromSharedLib_LibraryPresent() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE1 - WAR ClassLoader: " + cl);

        // Load only the State 1 library impl — do NOT call cl.loadClass(FEATURE_API_CLASS)
        // directly, to avoid making the WAR classloader the initiating CL for TestFeatureApi.
        Class<?> implClass = cl.loadClass(LIB_IMPL_CLASS_STATE1);
        println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE1 - LibraryFeatureApiImplState1 loaded by: "
            + implClass.getClassLoader());

        Object impl = implClass.getDeclaredConstructor().newInstance();
        TestFeatureApi api = (TestFeatureApi) impl;

        String result = api.doWork();
        Assert.assertNotNull("STATE1: doWork() returned null", result);
        Assert.assertFalse("STATE1: doWork() returned empty string", result.isEmpty());
        println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE1 - SUCCESS: " + result);
    }

    /**
     * Test 3, State 2a: eviction-detection probe using the already-cached
     * {@link TestFeatureApi} interface.
     * <p>
     * If {@code SharedLibraryImpl.delete()} evicted the library's {@code AppClassLoader}
     * from {@code aclStore}, this load will throw {@code ClassNotFoundException} (the
     * expected, correct outcome). If the loader was <em>not</em> evicted despite the
     * {@code server.xml} change, the load succeeds — a warning logged here, with the
     * 2b probe providing the hard assertion.
     */
    public void testLibraryJarRemovedFromSharedLib_LibraryRemoved_CachedInterface() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2a - WAR ClassLoader: " + cl);

        try {
            Class<?> removedClass = cl.loadClass(LIB_IMPL_CLASS_STATE2A);
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2a - CLASS_STILL_VISIBLE "
                + "(library AppClassLoader NOT evicted despite server.xml change): "
                + "LibraryFeatureApiImplState2a loaded by " + removedClass.getClassLoader());
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2a - WARNING: "
                + "cached interface still visible; library loader may not have been evicted.");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2a - CNFE (library loader evicted, expected): "
                + cnfe.getMessage());
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2a - CNFE");
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2a - NCDFE (unexpected): "
                + ncdfe.getMessage());
            Assert.fail("STATE2a: unexpected NoClassDefFoundError for cached-interface class: " + ncdfe.getMessage());
        }
    }

    /**
     * Test 3, State 2b: hard assertion that {@code LibraryFeatureApiImplState2b} — which
     * implements the never-before-seen {@link TestFeatureApi2} — cannot be loaded after
     * the library JAR has been removed from the fileset.
     * <p>
     * {@code SharedLibraryImpl.delete()} should evict the library's
     * {@code AppClassLoader} from {@code aclStore}. A {@code ClassNotFoundException}
     * here confirms that eviction. If the class loads successfully, the eviction did not
     * occur and the test fails.
     */
    public void testLibraryJarRemovedFromSharedLib_LibraryRemoved_FreshInterface() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2b - WAR ClassLoader: " + cl);

        try {
            Class<?> implClass = cl.loadClass(LIB_IMPL_CLASS_STATE2B);
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2b - CLASS_STILL_VISIBLE "
                + "(library AppClassLoader NOT evicted despite server.xml change): "
                + "LibraryFeatureApiImplState2b loaded by " + implClass.getClassLoader());
            Assert.fail("STATE2b: LibraryFeatureApiImplState2b (fresh TestFeatureApi2) was still visible after "
                + "library JAR removal — SharedLibraryImpl.delete() did not evict the AppClassLoader. "
                + "Update this assertion if this is the confirmed baseline.");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2b - CNFE (library AppClassLoader evicted, expected): "
                + cnfe.getMessage());
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2b - CNFE");
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2b - NCDFE (unexpected): "
                + ncdfe.getMessage());
            Assert.fail("STATE2b: unexpected NoClassDefFoundError: " + ncdfe.getMessage());
        }
    }

    /**
     * Test 3, State 3: verifies that all four shared-library probe classes are
     * loadable after the original {@code server.xml} has been restored (library JAR
     * back in the fileset, feature present throughout).
     * <p>
     * {@code LibraryFeatureApiImplState3} is the State 3 class (fresh, not cached). All
     * four probe classes are then loaded to confirm the library chain is fully
     * reconstructed after the config restore.
     * <p>
     * <b>Predicted outcome:</b> {@code SUCCESS} — Liberty creates a new
     * {@code AppClassLoader} for the restored library. The feature bundle was never
     * removed, so the {@code EquinoxClassLoader} is unchanged and there is no stale
     * type-binding risk.
     * <p>
     * If {@code ClassNotFoundException} is observed, Liberty did not recreate the
     * library classloader on config restore — update the assertion and investigate.
     * {@code ClassCastException} would indicate a type-binding mismatch; this is
     * very unlikely since the feature bundle address is stable across all three states,
     * but the assertion traps it explicitly for completeness.
     */
    public void testLibraryJarRemovedFromSharedLib_LibraryRestored() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - WAR ClassLoader: " + cl);

        try {
            // Load the State 3 library impl first — fresh TestFeatureApi3, not cached.
            Class<?> implClass3 = cl.loadClass(LIB_IMPL_CLASS_STATE3);
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - LibraryFeatureApiImplState3 loaded by: "
                + implClass3.getClassLoader());
            TestFeatureApi3 api3 = (TestFeatureApi3) implClass3.getDeclaredConstructor().newInstance();
            String result3 = api3.doWork();
            Assert.assertNotNull("STATE3: LibraryFeatureApiImplState3.doWork() returned null", result3);
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - LibraryFeatureApiImplState3 SUCCESS: " + result3);

            // Verify all four impls load cleanly after library restore.
            Class<?> implClass1 = cl.loadClass(LIB_IMPL_CLASS_STATE1);
            TestFeatureApi api1 = (TestFeatureApi) implClass1.getDeclaredConstructor().newInstance();
            Assert.assertNotNull("STATE3: LibraryFeatureApiImplState1.doWork() returned null", api1.doWork());
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - LibraryFeatureApiImplState1 re-load SUCCESS");

            Class<?> removedClass = cl.loadClass(LIB_IMPL_CLASS_STATE2A);
            TestFeatureApi apiRemoved = (TestFeatureApi) removedClass.getDeclaredConstructor().newInstance();
            Assert.assertNotNull("STATE3: LibraryFeatureApiImplState2a.doWork() returned null", apiRemoved.doWork());
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - LibraryFeatureApiImplState2a re-load SUCCESS");

            Class<?> implClass2 = cl.loadClass(LIB_IMPL_CLASS_STATE2B);
            TestFeatureApi2 api2 = (TestFeatureApi2) implClass2.getDeclaredConstructor().newInstance();
            Assert.assertNotNull("STATE3: LibraryFeatureApiImplState2b.doWork() returned null", api2.doWork());
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - LibraryFeatureApiImplState2b re-load SUCCESS");

            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - SUCCESS: all impls loaded and invoked");

        } catch (ClassCastException cce) {
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - CLASSCAST: " + cce.getMessage());
            Assert.fail("STATE3: unexpected ClassCastException after library restore — "
                + "type-binding mismatch despite feature being present throughout the test. "
                + "Update this assertion if this is the confirmed baseline.");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - CNFE: " + cnfe.getMessage());
            Assert.fail("STATE3: ClassNotFoundException after library restore — Liberty did not "
                + "recreate the shared-library AppClassLoader on server.xml config restore. "
                + "Update this assertion if this is the confirmed baseline.");
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - NCDFE: " + ncdfe.getMessage());
            Assert.fail("STATE3: unexpected NoClassDefFoundError: " + ncdfe.getMessage());
        }
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private static void println(String message) {
        System.out.println(message);
    }
}
