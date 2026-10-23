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

import static io.openliberty.classloading.classpath.fat.FATSuite.DYNAMIC_FEATURE_SHARED_LIB_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_SHARED_LIB_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_UNRELATED_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_UNRELATED_WAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_WAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

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
 * FAT test for the dynamic feature lifecycle classloader baseline (Test 2).
 * <p>
 * Tests the three lifecycle states when an application accesses a feature API
 * implementation that lives inside a <em>shared library</em>. The classloader
 * chain under test is:
 * <pre>
 *   WAR AppClassLoader
 *     └─ Shared-library AppClassLoader  (cached in ClassLoadingServiceImpl.aclStore)
 *          └─ GatewayClassLoader
 *               └─ EquinoxClassLoader [test.feature.api]
 * </pre>
 *
 * <ol>
 *   <li><b>State 1</b> — Feature present at server start: app successfully calls through the
 *       shared library to the feature API.</li>
 *   <li><b>State 2</b> — Feature dynamically removed: observes whether the shared-library
 *       {@code AppClassLoader} is evicted from {@code aclStore} or retained (stale loader).</li>
 *   <li><b>State 3</b> — Feature dynamically re-added: observes whether the cast succeeds
 *       (same bundle revision, Failure Mode A) or fails with {@code ClassCastException}
 *       (new bundle revision, Failure Mode B — the bug described in the executive summary).</li>
 * </ol>
 *
 * <p>The test does not assume ideal behaviour; it records <em>actual current</em>
 * Liberty behaviour so findings can drive architectural discussions about classloader
 * invalidation on feature lifecycle changes.
 *
 * <p><b>Why no {@code @TestServlet} / no {@code extends FATServletClient}:</b>
 * The three lifecycle states must run inside a <em>single</em> {@code @Test} method that
 * walks State 1 → 2 → 3 in one continuous, ordered sequence. {@code @TestServlet}
 * synthesises independent JUnit tests with no FAT-class code running between them, so
 * there is no hook to inject {@code changeFeatures()} and wait for {@code CWWKF0008I}.
 */
@RunWith(FATRunner.class)
public class DynamicFeatureSharedLibTest {

    @Server(DYNAMIC_FEATURE_SHARED_LIB_TEST_SERVER)
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

        // Deploy the WAR to the apps directory.
        ShrinkHelper.exportAppToServer(server, TEST_DYNAMIC_FEATURE_WAR, DeployOptions.SERVER_ONLY);

        // Second app deployed with no dependency on the feature API or shared library; used to prove restart is targeted.
        ShrinkHelper.exportAppToServer(server, TEST_DYNAMIC_FEATURE_UNRELATED_WAR, DeployOptions.SERVER_ONLY);

        // Deploy the shared library JAR to the server's sharedLibs directory — this
        // corresponds to the <fileset dir="${server.config.dir}/sharedLibs"> in server.xml.
        ShrinkHelper.exportToServer(server, "sharedLibs", TEST_DYNAMIC_FEATURE_SHARED_LIB_JAR, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    /**
     * Walks State 1 → State 2 → State 3 in a single ordered sequence.
     * <p>
     * Each state is probed via an HTTP call to a named servlet method. After each
     * feature-change, the test asserts that the affected app was stopped and restarted
     * and that a second unrelated app was not touched.
     */
    @Test
    public void testDynamicFeatureSharedLibLifecycle() throws Exception {
        int unrelatedMessagesBefore = countAppMessages(TEST_DYNAMIC_FEATURE_UNRELATED_APP);

        // ── STATE 1: Feature present ─────────────────────────────────────────
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH, "testLibraryDependsOnFeatureApi_FeaturePresent");
        assertNotNull("State 1 SUCCESS marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE1 - SUCCESS"));

        // ── STATE 2: Remove feature dynamically ──────────────────────────────
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("servlet-4.0", "componenttest-1.0"));
        List<String> removalMessages = server.waitForConfigUpdateInLogUsingMark(
            null, true,
            "CWWKZ0009I.*" + TEST_DYNAMIC_FEATURE_APP,
            "CWWKZ000[13]I.*" + TEST_DYNAMIC_FEATURE_APP);
        assertAppStoppedThenStarted("State 2", removalMessages);
        assertEquals("State 2: unrelated app must not be restarted",
                     unrelatedMessagesBefore, countAppMessages(TEST_DYNAMIC_FEATURE_UNRELATED_APP));
        // Exact count guards against double-restart: initial-start(1) + stop(1) + restart(1) = 3.
        assertEquals("State 2: " + TEST_DYNAMIC_FEATURE_APP + " must have restarted exactly once",
                     3, countAppMessages(TEST_DYNAMIC_FEATURE_APP));

        // State 2a: cached-interface probe — expects NOT_LOADABLE because the library
        // classloader was also evicted along with the WAR classloader on feature removal.
        // The recycled app gets a fresh library loader with no cache; resolving TestFeatureApi
        // through it fails with NoClassDefFoundError (feature bundle is absent).
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH,
            "testLibraryDependsOnFeatureApi_FeatureRemoved_CachedInterface");
        assertNotNull("State 2a NOT_LOADABLE marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2a - NOT_LOADABLE"));

        // State 2b: fresh-interface probe — expects NCDFE (TestFeatureApi2 never cached, bundle gone)
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH,
            "testLibraryDependsOnFeatureApi_FeatureRemoved_FreshInterface");
        assertNotNull("State 2b NCDFE/CNFE marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b -"));

        // ── STATE 3: Re-add feature dynamically ──────────────────────────────
        // No application restart is expected: the recycled loader from State 2 has a fresh
        // gateway and fresh library loader, both with DynamicImport-Package=*. They resolve
        // feature API packages lazily from the new bundle. The count stays at 3.
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("servlet-4.0", "componenttest-1.0", "testFeatureApi-1.0"));
        server.waitForConfigUpdateInLogUsingMark(null, true);
        assertEquals("State 3: unrelated app must not be restarted",
                     unrelatedMessagesBefore, countAppMessages(TEST_DYNAMIC_FEATURE_UNRELATED_APP));
        // State 2 cycle(3) — no extra stop/restart on re-add.
        assertEquals("State 3: " + TEST_DYNAMIC_FEATURE_APP + " must not have restarted again",
                     3, countAppMessages(TEST_DYNAMIC_FEATURE_APP));

        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH, "testLibraryDependsOnFeatureApi_FeatureReAdded");
        assertNotNull("State 3 SUCCESS marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - SUCCESS"));
    }

    private static void assertAppStoppedThenStarted(String state, List<String> messages) {
        int stopped = -1;
        int started = -1;
        for (int i = 0; i < messages.size(); i++) {
            String line = messages.get(i);
            if (!line.contains(TEST_DYNAMIC_FEATURE_APP)) continue;
            if (line.contains("CWWKZ0009I") && stopped < 0) stopped = i;
            else if ((line.contains("CWWKZ0001I") || line.contains("CWWKZ0003I")) && started < 0) started = i;
        }
        assertTrue(state + ": " + TEST_DYNAMIC_FEATURE_APP + " was not stopped (CWWKZ0009I)", stopped >= 0);
        assertTrue(state + ": " + TEST_DYNAMIC_FEATURE_APP + " was not started again (CWWKZ0001I/0003I)", started >= 0);
        assertTrue(state + ": " + TEST_DYNAMIC_FEATURE_APP + " started before it stopped", stopped < started);
    }

    private static int countAppMessages(String appName) throws Exception {
        return server.findStringsInLogs("CWWKZ000[139]I.*" + appName).size();
    }

    @AfterClass
    public static void stopServer() throws Exception {
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
