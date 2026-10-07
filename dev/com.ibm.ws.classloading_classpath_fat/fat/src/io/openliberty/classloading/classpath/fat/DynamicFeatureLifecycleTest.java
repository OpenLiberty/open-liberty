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
package io.openliberty.classloading.classpath.fat;

import static io.openliberty.classloading.classpath.fat.FATSuite.DYNAMIC_FEATURE_LIFECYCLE_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_WAR;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * FAT test for the dynamic feature lifecycle classloader baseline (Test 1).
 * <p>
 * Tests the three lifecycle states when a WAR directly imports and invokes a
 * class from an OSGi feature bundle API — with no shared library in between:
 * <ol>
 *   <li><b>State 1</b> — Feature present at server start: app calls feature API.</li>
 *   <li><b>State 2</b> — Feature dynamically removed: observes CNFE or stale loader.</li>
 *   <li><b>State 3</b> — Feature dynamically re-added: observes success, ClassCastException,
 *       or persisting CNFE (stale aclStore entry).</li>
 * </ol>
 *
 * <p>The test does not assume ideal behaviour; it records <em>actual current</em>
 * Liberty behaviour so that findings can drive architectural discussions about
 * classloader invalidation on feature lifecycle changes.
 *
 * <p><b>Why no {@code @TestServlet} / no {@code extends FATServletClient}:</b>
 * {@code @TestServlet} synthesises independent JUnit tests with no FAT-class code
 * running between them. There is no hook to inject {@code changeFeatures()} and
 * wait for {@code CWWKF0008I} between those synthesised tests. The three lifecycle
 * states must run inside a <em>single</em> {@code @Test} method that walks
 * State 1 → 2 → 3 in one continuous, ordered sequence.
 */
@RunWith(FATRunner.class)
public class DynamicFeatureLifecycleTest {

    @Server(DYNAMIC_FEATURE_LIFECYCLE_TEST_SERVER)
    public static LibertyServer server;

    private static final String SERVLET_PATH =
        TEST_DYNAMIC_FEATURE_APP + "/DynamicFeatureLifecycleTestServlet";

    @BeforeClass
    public static void setupTestServer() throws Exception {
        // Feature / bundle installs must happen before startServer().
        server.installSystemFeature("testFeatureApi-1.0");
        assertTrue("testFeatureApi-1.0.mf should have been copied to lib/features",
                   server.fileExistsInLibertyInstallRoot("lib/features/testFeatureApi-1.0.mf"));

        server.installSystemBundle("test.feature.api");
        assertTrue("test.feature.api.jar should have been copied to lib",
                   server.fileExistsInLibertyInstallRoot("lib/test.feature.api.jar"));

        ShrinkHelper.exportAppToServer(server, TEST_DYNAMIC_FEATURE_WAR, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    /**
     * Walks State 1 → State 2 → State 3 in a single ordered sequence.
     * <p>
     * Each state is probed via an HTTP call to a named servlet method. After
     * each feature-change, the test waits for {@code CWWKF0008I} (feature update
     * complete) before proceeding to the next probe.
     *
     * <h3>Observed baseline summary</h3>
     * <pre>
     * State 1  EquinoxClassLoader@X [bundle id=N]   — fresh install, cast succeeds ✓
     * State 2  EquinoxClassLoader@X [bundle id=N]   — feature removed, SAME loader still in aclStore
     * State 3  EquinoxClassLoader@X [bundle id=N]   — feature re-added, SAME loader, cast still succeeds
     * </pre>
     *
     * <p><b>Why State 3 succeeds (and why that is still a bug):</b>
     * The {@code AppClassLoader} for the WAR is cached in
     * {@code ClassLoadingServiceImpl.aclStore} and is never evicted when only a
     * {@code <feature>} is removed — no {@code server.xml} change fires the delete
     * notification that would evict it. Because the same OSGi bundle object was
     * never truly unloaded, the re-add in State 3 re-wires that same
     * {@code EquinoxClassLoader} instance rather than producing a new bundle
     * revision. The cast therefore succeeds, but for the wrong reason: it is
     * working off a stale, never-invalidated classloader, not a properly refreshed one.
     *
     * <p><b>Why this test cannot show the {@code ClassCastException}:</b>
     * The {@code ClassCastException} described in the background document only
     * surfaces when the re-add <em>does</em> produce a new bundle revision — which
     * happens when a shared library is in the classloader chain. In that scenario
     * the stale {@code AppClassLoader} in {@code aclStore} holds a reference to the
     * <em>old</em> revision's types while the feature's new bundle revision exports
     * a <em>new</em> version of the same interface. The cast then fails with a
     * subtype mismatch. That failure mode is covered by
     * {@code DynamicFeatureSharedLibTest} (Test 2), not here.
     */
    @Test
    public void testDynamicFeatureLifecycle() throws Exception {
        // ── STATE 1: Feature present ─────────────────────────────────────────
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH, "testAppDirectlyDependsOnFeatureApi_FeaturePresent");
        assertNotNull("State 1 SUCCESS marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_TEST STATE1 - SUCCESS"));

        // ── STATE 2: Remove feature dynamically ──────────────────────────────
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("servlet-4.0", "componenttest-1.0"));
        assertNotNull("Feature update (removal) did not complete — CWWKF0008I not found",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));

        // State 2a: cached-interface probe — expects success (TestFeatureApi already in loader cache)
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH,
            "testAppDirectlyDependsOnFeatureApi_FeatureRemoved_CachedInterface");
        assertNotNull("State 2a SUCCESS marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_TEST STATE2a - SUCCESS"));

        // State 2b: fresh-interface probe — expects NCDFE (TestFeatureApi2 never cached, bundle gone)
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH,
            "testAppDirectlyDependsOnFeatureApi_FeatureRemoved_FreshInterface");
        assertNotNull("State 2b NCDFE/CNFE marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_TEST STATE2b -"));

        // ── STATE 3: Re-add feature dynamically ──────────────────────────────
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("servlet-4.0", "componenttest-1.0", "testFeatureApi-1.0"));
        assertNotNull("Feature update (re-add) did not complete — CWWKF0008I not found",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));

        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH, "testAppDirectlyDependsOnFeatureApi_FeatureReAdded");
        assertNotNull("State 3 marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_TEST STATE3 -"));
    }

    @AfterClass
    public static void stopServer() throws Exception {
        // TODO: Future extension — after server stop/restart, verify the application was
        // reported as "updated" (CWWKZ0003I) rather than simply restarted, confirming that
        // Liberty correctly refreshes the app classloader on feature re-add. See background
        // document Section 7 for details.
        try {
            server.stopServer();
        } finally {
            server.uninstallSystemFeature("testFeatureApi-1.0");
            assertFalse("testFeatureApi-1.0.mf was not cleaned up",
                        server.fileExistsInLibertyInstallRoot("lib/features/testFeatureApi-1.0.mf"));
            server.uninstallSystemBundle("test.feature.api");
            assertFalse("test.feature.api.jar was not cleaned up",
                        server.fileExistsInLibertyInstallRoot("lib/test.feature.api.jar"));
        }
    }
}
