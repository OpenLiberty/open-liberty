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
 * Probe servlet shared by all three dynamic-feature-lifecycle FAT tests.
 * <p>
 * Each probe method loads a distinct, previously-unloaded impl class to force
 * a genuine {@code ClassLoader.loadClass()} delegation walk rather than a
 * {@code findLoadedClass()} cache hit.
 */
@WebServlet("/DynamicFeatureLifecycleTestServlet")
public class DynamicFeatureLifecycleTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    // ── Test 1: in-WAR impl classes ───────────────────────────────────────────
    private static final String APP_IMPL_CLASS_STATE1 =
        "io.openliberty.classloading.dynamic.feature.test.app.AppFeatureApiImplState1";
    // State 2a NOT_LOADABLE probe: implements TestFeatureApi — expects NoClassDefFoundError after removal
    // (app is recycled; fresh classloader has no cache and cannot resolve the absent feature bundle)
    private static final String APP_IMPL_CLASS_STATE2A =
        "io.openliberty.classloading.dynamic.feature.test.app.AppFeatureApiImplState2a";
    private static final String APP_IMPL_CLASS_STATE2B =
        "io.openliberty.classloading.dynamic.feature.test.app.AppFeatureApiImplState2b";
    private static final String APP_IMPL_CLASS_STATE3 =
        "io.openliberty.classloading.dynamic.feature.test.app.AppFeatureApiImplState3";

    // ── Tests 2 & 3: shared-library impl classes ──────────────────────────────
    private static final String LIB_IMPL_CLASS_STATE1 =
        "io.openliberty.classloading.shared.feature.lib.LibraryFeatureApiImplState1";
    // State 2a NOT_LOADABLE probe: implements TestFeatureApi — expects NoClassDefFoundError after removal
    // (app and library classloaders are recycled; fresh loaders have no cache)
    private static final String LIB_IMPL_CLASS_STATE2A =
        "io.openliberty.classloading.shared.feature.lib.LibraryFeatureApiImplState2a";
    private static final String LIB_IMPL_CLASS_STATE2B =
        "io.openliberty.classloading.shared.feature.lib.LibraryFeatureApiImplState2b";
    private static final String LIB_IMPL_CLASS_STATE3 =
        "io.openliberty.classloading.shared.feature.lib.LibraryFeatureApiImplState3";

    // =========================================================================
    // Test 1 — Application directly depends on feature API (no shared library)
    // =========================================================================

    public void testAppDirectlyDependsOnFeatureApi_FeaturePresent() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_TEST STATE1 - AppCL: " + cl);

        Class<?> implClass = cl.loadClass(APP_IMPL_CLASS_STATE1);
        println("DYNAMIC_FEATURE_TEST STATE1 - AppFeatureApiImplState1 loaded by: " + implClass.getClassLoader());

        Object impl = implClass.getDeclaredConstructor().newInstance();
        TestFeatureApi api = (TestFeatureApi) impl;

        String result = api.doWork();
        Assert.assertNotNull("STATE1: doWork() returned null", result);
        Assert.assertFalse("STATE1: doWork() returned empty string", result.isEmpty());
        println("DYNAMIC_FEATURE_TEST STATE1 - SUCCESS: " + result);
    }

    /**
     * State 2a: expects NOT_LOADABLE — app was recycled, fresh classloader cannot resolve the absent bundle.
     * A successful load here means the stale pre-removal classloader is still in use.
     */
    public void testAppDirectlyDependsOnFeatureApi_FeatureRemoved_CachedInterface() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_TEST STATE2a - AppCL: " + cl);

        try {
            Class<?> removedClass = cl.loadClass(APP_IMPL_CLASS_STATE2A);
            println("DYNAMIC_FEATURE_TEST STATE2a - CLASS_STILL_VISIBLE (unexpected — stale loader): "
                + "AppFeatureApiImplState2a loaded by " + removedClass.getClassLoader());
            Assert.fail("STATE2a: AppFeatureApiImplState2a should not load after the feature was removed "
                + "and the application recycled, but it loaded successfully via a stale classloader.");
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            println("DYNAMIC_FEATURE_TEST STATE2a - " + e.getClass().getSimpleName()
                + " (expected — fresh loader, feature absent): " + e.getMessage());
            println("DYNAMIC_FEATURE_TEST STATE2a - NOT_LOADABLE");
        }
    }

    public void testAppDirectlyDependsOnFeatureApi_FeatureRemoved_FreshInterface() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_TEST STATE2b - AppCL: " + cl);

        try {
            Class<?> implClass = cl.loadClass(APP_IMPL_CLASS_STATE2B);
            println("DYNAMIC_FEATURE_TEST STATE2b - CLASS_STILL_VISIBLE (unexpected — stale loader): "
                + "AppFeatureApiImplState2b loaded by " + implClass.getClassLoader());
            Assert.fail("STATE2b: AppFeatureApiImplState2b should have produced NoClassDefFoundError but loaded successfully.");
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_TEST STATE2b - NCDFE (expected): " + ncdfe.getMessage());
            println("DYNAMIC_FEATURE_TEST STATE2b - NCDFE");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_TEST STATE2b - CNFE (acceptable): " + cnfe.getMessage());
            println("DYNAMIC_FEATURE_TEST STATE2b - CNFE");
        }
    }

    public void testAppDirectlyDependsOnFeatureApi_FeatureReAdded() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_TEST STATE3 - AppCL: " + cl);

        try {
            Class<?> implClass3 = cl.loadClass(APP_IMPL_CLASS_STATE3);
            println("DYNAMIC_FEATURE_TEST STATE3 - AppFeatureApiImplState3 loaded by: " + implClass3.getClassLoader());
            TestFeatureApi3 api3 = (TestFeatureApi3) implClass3.getDeclaredConstructor().newInstance();
            String result3 = api3.doWork();
            Assert.assertNotNull("STATE3: AppFeatureApiImplState3.doWork() returned null", result3);
            println("DYNAMIC_FEATURE_TEST STATE3 - AppFeatureApiImplState3 SUCCESS: " + result3);

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
            println("DYNAMIC_FEATURE_TEST STATE3 - CLASSCAST: " + cce.getMessage());
            Assert.fail("STATE3: unexpected ClassCastException — stale classloader still in use.");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_TEST STATE3 - CNFE: " + cnfe.getMessage());
            Assert.fail("STATE3: unexpected ClassNotFoundException after feature re-add: " + cnfe.getMessage());
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_TEST STATE3 - NCDFE: " + ncdfe.getMessage());
            Assert.fail("STATE3: unexpected NoClassDefFoundError: " + ncdfe.getMessage());
        }
    }

    // =========================================================================
    // Test 2 — Application → shared library → feature API
    // =========================================================================

    /**
     * Do NOT call cl.loadClass(TestFeatureApi) directly here — that would make the WAR loader
     * the initiating classloader for the feature API class and contaminate the shared-library probe.
     */
    public void testLibraryDependsOnFeatureApi_FeaturePresent() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE1 - WAR ClassLoader: " + cl);

        Class<?> implClass = cl.loadClass(LIB_IMPL_CLASS_STATE1);
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE1 - LibraryFeatureApiImplState1 loaded by: " + implClass.getClassLoader());

        Object impl = implClass.getDeclaredConstructor().newInstance();
        TestFeatureApi api = (TestFeatureApi) impl;

        String result = api.doWork();
        Assert.assertNotNull("STATE1: doWork() returned null", result);
        Assert.assertFalse("STATE1: doWork() returned empty string", result.isEmpty());
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE1 - SUCCESS: " + result);
    }

    /**
     * State 2a: expects NOT_LOADABLE — both WAR and library classloaders were recycled,
     * fresh loaders cannot resolve the absent feature bundle.
     */
    public void testLibraryDependsOnFeatureApi_FeatureRemoved_CachedInterface() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2a - WAR ClassLoader: " + cl);

        try {
            Class<?> removedClass = cl.loadClass(LIB_IMPL_CLASS_STATE2A);
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2a - CLASS_STILL_VISIBLE (unexpected — stale loader): "
                + "LibraryFeatureApiImplState2a loaded by " + removedClass.getClassLoader());
            Assert.fail("STATE2a: LibraryFeatureApiImplState2a should not load after the feature was removed "
                + "and the application and library classloaders recycled, but it loaded via a stale loader.");
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2a - " + e.getClass().getSimpleName()
                + " (expected — fresh loaders, feature absent): " + e.getMessage());
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2a - NOT_LOADABLE");
        }
    }

    public void testLibraryDependsOnFeatureApi_FeatureRemoved_FreshInterface() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - WAR ClassLoader: " + cl);

        try {
            Class<?> implClass = cl.loadClass(LIB_IMPL_CLASS_STATE2B);
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - CLASS_STILL_VISIBLE (unexpected): "
                + "LibraryFeatureApiImplState2b loaded by " + implClass.getClassLoader());
            Assert.fail("STATE2b: LibraryFeatureApiImplState2b should have produced NoClassDefFoundError but loaded successfully.");
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - NCDFE (expected): " + ncdfe.getMessage());
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - NCDFE");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - CNFE (acceptable): " + cnfe.getMessage());
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b - CNFE");
        }
    }

    public void testLibraryDependsOnFeatureApi_FeatureReAdded() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - WAR ClassLoader: " + cl);

        try {
            Class<?> implClass3 = cl.loadClass(LIB_IMPL_CLASS_STATE3);
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - LibraryFeatureApiImplState3 loaded by: " + implClass3.getClassLoader());
            TestFeatureApi3 api3 = (TestFeatureApi3) implClass3.getDeclaredConstructor().newInstance();
            String result3 = api3.doWork();
            Assert.assertNotNull("STATE3: LibraryFeatureApiImplState3.doWork() returned null", result3);
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - LibraryFeatureApiImplState3 SUCCESS: " + result3);

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
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - CLASSCAST: " + cce.getMessage());
            Assert.fail("STATE3: ClassCastException — shared-library AppClassLoader is stale.");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - CNFE: " + cnfe.getMessage());
            Assert.fail("STATE3: unexpected ClassNotFoundException after feature re-add: " + cnfe.getMessage());
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - NCDFE: " + ncdfe.getMessage());
            Assert.fail("STATE3: unexpected NoClassDefFoundError: " + ncdfe.getMessage());
        }
    }

    // =========================================================================
    // Test 3 — Library JAR removed from shared library fileset
    // =========================================================================

    public void testLibraryJarRemovedFromSharedLib_LibraryPresent() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE1 - WAR ClassLoader: " + cl);

        Class<?> implClass = cl.loadClass(LIB_IMPL_CLASS_STATE1);
        println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE1 - LibraryFeatureApiImplState1 loaded by: " + implClass.getClassLoader());

        Object impl = implClass.getDeclaredConstructor().newInstance();
        TestFeatureApi api = (TestFeatureApi) impl;

        String result = api.doWork();
        Assert.assertNotNull("STATE1: doWork() returned null", result);
        Assert.assertFalse("STATE1: doWork() returned empty string", result.isEmpty());
        println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE1 - SUCCESS: " + result);
    }

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
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2a - CNFE (library loader evicted, expected): " + cnfe.getMessage());
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2a - CNFE");
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2a - NCDFE (unexpected): " + ncdfe.getMessage());
            Assert.fail("STATE2a: unexpected NoClassDefFoundError for cached-interface class: " + ncdfe.getMessage());
        }
    }

    public void testLibraryJarRemovedFromSharedLib_LibraryRemoved_FreshInterface() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2b - WAR ClassLoader: " + cl);

        try {
            Class<?> implClass = cl.loadClass(LIB_IMPL_CLASS_STATE2B);
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2b - CLASS_STILL_VISIBLE "
                + "(library AppClassLoader NOT evicted despite server.xml change): "
                + "LibraryFeatureApiImplState2b loaded by " + implClass.getClassLoader());
            Assert.fail("STATE2b: LibraryFeatureApiImplState2b was still visible after library JAR removal — "
                + "SharedLibraryImpl.delete() did not evict the AppClassLoader.");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2b - CNFE (library AppClassLoader evicted, expected): " + cnfe.getMessage());
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2b - CNFE");
        } catch (NoClassDefFoundError ncdfe) {
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2b - NCDFE (unexpected): " + ncdfe.getMessage());
            Assert.fail("STATE2b: unexpected NoClassDefFoundError: " + ncdfe.getMessage());
        }
    }

    public void testLibraryJarRemovedFromSharedLib_LibraryRestored() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - WAR ClassLoader: " + cl);

        try {
            Class<?> implClass3 = cl.loadClass(LIB_IMPL_CLASS_STATE3);
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - LibraryFeatureApiImplState3 loaded by: " + implClass3.getClassLoader());
            TestFeatureApi3 api3 = (TestFeatureApi3) implClass3.getDeclaredConstructor().newInstance();
            String result3 = api3.doWork();
            Assert.assertNotNull("STATE3: LibraryFeatureApiImplState3.doWork() returned null", result3);
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - LibraryFeatureApiImplState3 SUCCESS: " + result3);

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
            Assert.fail("STATE3: unexpected ClassCastException after library restore.");
        } catch (ClassNotFoundException cnfe) {
            println("DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - CNFE: " + cnfe.getMessage());
            Assert.fail("STATE3: ClassNotFoundException after library restore — AppClassLoader was not recreated.");
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
