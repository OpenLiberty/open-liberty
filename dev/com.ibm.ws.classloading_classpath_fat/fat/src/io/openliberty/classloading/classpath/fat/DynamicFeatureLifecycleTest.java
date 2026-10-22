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
 * FAT test for the dynamic feature lifecycle classloader baseline (Test 1).
 * <p>
 * Tests the three lifecycle states when a WAR directly imports and invokes a
 * class from an OSGi feature bundle API — with no shared library in between:
 * <ol>
 *   <li><b>State 1</b> — Feature present at server start: app calls feature API.</li>
 *   <li><b>State 2</b> — Feature dynamically removed: app is recycled, then new class load fails.</li>
 *   <li><b>State 3</b> — Feature dynamically re-added: app is not restarted again, succeeds cleanly.</li>
 * </ol>
 *
 * <p>The test asserts that the affected application is stopped and restarted exactly once
 * (CWWKZ0009I + CWWKZ0001I/0003I) when the feature is removed, not restarted again when it is
 * re-added, and that a second application that does not use the feature is <em>not</em> restarted.
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

        // Second app deployed with no dependency on the feature API; used to prove restart is targeted.
        ShrinkHelper.exportAppToServer(server, TEST_DYNAMIC_FEATURE_UNRELATED_WAR, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    /**
     * Walks State 1 → State 2 → State 3 in a single ordered sequence.
     * Each state is probed via an HTTP call to a named servlet method. After each
     * feature-change, the test asserts that the affected app was stopped and restarted
     * and that the unrelated app was not touched.
     */
    @Test
    public void testDynamicFeatureLifecycle() throws Exception {
        int unrelatedMessagesBefore = countAppMessages(TEST_DYNAMIC_FEATURE_UNRELATED_APP);

        // ── STATE 1: Feature present ─────────────────────────────────────────
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH, "testAppDirectlyDependsOnFeatureApi_FeaturePresent");
        assertNotNull("State 1 SUCCESS marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_TEST STATE1 - SUCCESS"));

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

        // State 2a: previously-used interface — expects it NOT to load, because the app was recycled
        // and the new loader has no cached TestFeatureApi.
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH,
            "testAppDirectlyDependsOnFeatureApi_FeatureRemoved_CachedInterface");
        assertNotNull("State 2a NOT_LOADABLE marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_TEST STATE2a - NOT_LOADABLE"));

        // State 2b: fresh-interface probe — expects CNFE (TestFeatureApi2 never cached, bundle gone)
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH,
            "testAppDirectlyDependsOnFeatureApi_FeatureRemoved_FreshInterface");
        assertNotNull("State 2b CNFE marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_TEST STATE2b -"));

        // ── STATE 3: Re-add feature dynamically ──────────────────────────────
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("servlet-4.0", "componenttest-1.0", "testFeatureApi-1.0"));
        // Re-adding does not restart the app: the loader recycled in State 2 has cached nothing from
        // the feature, and its gateway resolves the new feature bundle lazily (dynamic import).
        server.waitForConfigUpdateInLogUsingMark(null, true);
        assertEquals("State 3: unrelated app must not be restarted",
                     unrelatedMessagesBefore, countAppMessages(TEST_DYNAMIC_FEATURE_UNRELATED_APP));
        assertEquals("State 3: " + TEST_DYNAMIC_FEATURE_APP + " must not be restarted again on re-add",
                     3, countAppMessages(TEST_DYNAMIC_FEATURE_APP));

        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH, "testAppDirectlyDependsOnFeatureApi_FeatureReAdded");
        assertNotNull("State 3 SUCCESS marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_TEST STATE3 - SUCCESS"));
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
