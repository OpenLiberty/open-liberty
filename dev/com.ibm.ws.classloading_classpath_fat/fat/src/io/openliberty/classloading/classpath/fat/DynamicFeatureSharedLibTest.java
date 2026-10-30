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

import static io.openliberty.classloading.classpath.fat.FATSuite.BASE_FEATURES;
import static io.openliberty.classloading.classpath.fat.FATSuite.BASE_PLUS_DYNAMIC_FEATURE;
import static io.openliberty.classloading.classpath.fat.FATSuite.DYNAMIC_FEATURE_APP2_SERVLET_PATH;
import static io.openliberty.classloading.classpath.fat.FATSuite.DYNAMIC_FEATURE_SERVLET_PATH;
import static io.openliberty.classloading.classpath.fat.FATSuite.DYNAMIC_FEATURE_SHARED_LIB_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.DYNAMIC_FEATURE_SHARED_LIB_TWO_APPS_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_APP2;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_APP2_WAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_SHARED_LIB_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_UNRELATED_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_UNRELATED_WAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_WAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.assertAppStoppedThenStarted;
import static io.openliberty.classloading.classpath.fat.FATSuite.countAppMessages;
import static io.openliberty.classloading.classpath.fat.FATSuite.installTestFeatureAndBundle;
import static io.openliberty.classloading.classpath.fat.FATSuite.stopServerAndUninstall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
 * FAT test for dynamic feature lifecycle classloader behaviour (Test 2).
 * <p>
 * Exercises the three lifecycle states when the feature API implementation lives in a
 * shared library rather than directly in the WAR:
 * <pre>
 *   WAR AppClassLoader
 *     └─ Shared-library AppClassLoader  (cached in ClassLoadingServiceImpl.aclStore)
 *          └─ GatewayClassLoader
 *               └─ EquinoxClassLoader [test.feature.api]
 * </pre>
 * A single {@code @Test} method walks State 1 → 2 → 3 in order so that
 * {@code changeFeatures()} can be injected between states.
 */
@RunWith(FATRunner.class)
public class DynamicFeatureSharedLibTest {

    @Server(DYNAMIC_FEATURE_SHARED_LIB_TEST_SERVER)
    public static LibertyServer server;

    @Server(DYNAMIC_FEATURE_SHARED_LIB_TWO_APPS_TEST_SERVER)
    public static LibertyServer twoAppsServer;

    @BeforeClass
    public static void setupTestServer() throws Exception {
        // Feature / bundle installs must happen before startServer().
        installTestFeatureAndBundle(server);

        // Server 1 (single app)
        ShrinkHelper.exportAppToServer(server, TEST_DYNAMIC_FEATURE_WAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, TEST_DYNAMIC_FEATURE_UNRELATED_WAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "sharedLibs", TEST_DYNAMIC_FEATURE_SHARED_LIB_JAR, DeployOptions.SERVER_ONLY);

        // Server 2 (two apps sharing one library)
        ShrinkHelper.exportAppToServer(twoAppsServer, TEST_DYNAMIC_FEATURE_WAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportAppToServer(twoAppsServer, TEST_DYNAMIC_FEATURE_APP2_WAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportAppToServer(twoAppsServer, TEST_DYNAMIC_FEATURE_UNRELATED_WAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(twoAppsServer, "sharedLibs", TEST_DYNAMIC_FEATURE_SHARED_LIB_JAR, DeployOptions.SERVER_ONLY);
    }

    @Test
    public void testDynamicFeatureSharedLibLifecycle() throws Exception {
        server.startServer();
        try {
            int unrelatedMessagesBefore = countAppMessages(server, TEST_DYNAMIC_FEATURE_UNRELATED_APP);

        // ── STATE 1: Feature present ─────────────────────────────────────────
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, DYNAMIC_FEATURE_SERVLET_PATH, "testLibraryDependsOnFeatureApi_FeaturePresent");
        assertNotNull("State 1 SUCCESS marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE1 - SUCCESS"));

        // ── STATE 2: Remove feature dynamically ──────────────────────────────
        server.setMarkToEndOfLog();
        server.changeFeatures(BASE_FEATURES);
        List<String> removalMessages = server.waitForConfigUpdateInLogUsingMark(
            null, true,
            "CWWKZ0009I.*" + TEST_DYNAMIC_FEATURE_APP,
            "CWWKZ000[13]I.*" + TEST_DYNAMIC_FEATURE_APP);
        assertAppStoppedThenStarted("State 2", removalMessages);
        assertEquals("State 2: unrelated app must not be restarted",
                     unrelatedMessagesBefore, countAppMessages(server, TEST_DYNAMIC_FEATURE_UNRELATED_APP));
        assertEquals("State 2: " + TEST_DYNAMIC_FEATURE_APP + " must have restarted exactly once",
                     3, countAppMessages(server, TEST_DYNAMIC_FEATURE_APP));

        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, DYNAMIC_FEATURE_SERVLET_PATH,
            "testLibraryDependsOnFeatureApi_FeatureRemoved_CachedInterface");
        assertNotNull("State 2a NOT_LOADABLE marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2a - NOT_LOADABLE"));

        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, DYNAMIC_FEATURE_SERVLET_PATH,
            "testLibraryDependsOnFeatureApi_FeatureRemoved_FreshInterface");
        assertNotNull("State 2b marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b -"));

        // ── STATE 3: Re-add feature (no app restart expected) ────────────────
        server.setMarkToEndOfLog();
        server.changeFeatures(BASE_PLUS_DYNAMIC_FEATURE);
        server.waitForConfigUpdateInLogUsingMark(null, true);
        assertEquals("State 3: unrelated app must not be restarted",
                     unrelatedMessagesBefore, countAppMessages(server, TEST_DYNAMIC_FEATURE_UNRELATED_APP));
        assertEquals("State 3: " + TEST_DYNAMIC_FEATURE_APP + " must not have restarted again",
                     3, countAppMessages(server, TEST_DYNAMIC_FEATURE_APP));

        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, DYNAMIC_FEATURE_SERVLET_PATH, "testLibraryDependsOnFeatureApi_FeatureReAdded");
        assertNotNull("State 3 SUCCESS marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - SUCCESS"));

        // ── STATE 4: Remove feature a second time ────────────────────────────
        // The GCL created after the re-add must have had its owner app names registered,
        // otherwise this second removal would not recycle the app.
        server.setMarkToEndOfLog();
        server.changeFeatures(BASE_FEATURES);
        List<String> secondRemovalMessages = server.waitForConfigUpdateInLogUsingMark(
            null, true,
            "CWWKZ0009I.*" + TEST_DYNAMIC_FEATURE_APP,
            "CWWKZ000[13]I.*" + TEST_DYNAMIC_FEATURE_APP);
        assertAppStoppedThenStarted("State 4", secondRemovalMessages);
        assertEquals("State 4: unrelated app must not be restarted",
                     unrelatedMessagesBefore, countAppMessages(server, TEST_DYNAMIC_FEATURE_UNRELATED_APP));
        assertEquals("State 4: " + TEST_DYNAMIC_FEATURE_APP + " must have restarted exactly once more",
                     5, countAppMessages(server, TEST_DYNAMIC_FEATURE_APP));

        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, DYNAMIC_FEATURE_SERVLET_PATH,
            "testLibraryDependsOnFeatureApi_SecondRemoval_CachedInterface");
        assertNotNull("State 4a NOT_LOADABLE marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE4a - NOT_LOADABLE"));

        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, DYNAMIC_FEATURE_SERVLET_PATH,
            "testLibraryDependsOnFeatureApi_SecondRemoval_FreshInterface");
        assertNotNull("State 4b marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE4b -"));

        // ── STATE 5: Re-add feature a second time (no app restart expected) ──
        server.setMarkToEndOfLog();
        server.changeFeatures(BASE_PLUS_DYNAMIC_FEATURE);
        server.waitForConfigUpdateInLogUsingMark(null, true);
        assertEquals("State 5: unrelated app must not be restarted",
                     unrelatedMessagesBefore, countAppMessages(server, TEST_DYNAMIC_FEATURE_UNRELATED_APP));
        assertEquals("State 5: " + TEST_DYNAMIC_FEATURE_APP + " must not have restarted again",
                     5, countAppMessages(server, TEST_DYNAMIC_FEATURE_APP));

        // The app and library loaders were recycled in State 4, so the State 3 probe loads
        // its classes through fresh loaders again.
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, DYNAMIC_FEATURE_SERVLET_PATH, "testLibraryDependsOnFeatureApi_FeatureReAdded");
        assertNotNull("State 5 SUCCESS marker not found in log",
                      server.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - SUCCESS"));
        } finally {
            if (server.isStarted()) {
                server.stopServer();
            }
        }
    }

    /**
     * Scenario S2: Two applications share one library.
     * When the feature is removed, both applications must be recycled.
     */
    @Test
    public void testDynamicFeatureSharedLibTwoAppsLifecycle() throws Exception {
        twoAppsServer.startServer();
        try {
            int unrelatedMessagesBefore = countAppMessages(twoAppsServer, TEST_DYNAMIC_FEATURE_UNRELATED_APP);

            // ── STATE 1: Feature present ─────────────────────────────────────────
            twoAppsServer.setMarkToEndOfLog();
            FATServletClient.runTest(twoAppsServer, DYNAMIC_FEATURE_SERVLET_PATH, "testLibraryDependsOnFeatureApi_FeaturePresent");
            assertNotNull("State 1 App 1 SUCCESS marker not found in log",
                          twoAppsServer.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE1 - SUCCESS"));

            twoAppsServer.setMarkToEndOfLog();
            FATServletClient.runTest(twoAppsServer, DYNAMIC_FEATURE_APP2_SERVLET_PATH, "testLibraryDependsOnFeatureApi_FeaturePresent");
            assertNotNull("State 1 App 2 SUCCESS marker not found in log",
                          twoAppsServer.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE1 - SUCCESS"));

            // ── STATE 2: Remove feature dynamically ──────────────────────────────
            twoAppsServer.setMarkToEndOfLog();
            twoAppsServer.changeFeatures(BASE_FEATURES);
            List<String> removalMessages = twoAppsServer.waitForConfigUpdateInLogUsingMark(
                null, true,
                "CWWKZ0009I.*\\b" + TEST_DYNAMIC_FEATURE_APP + "\\b",
                "CWWKZ000[13]I.*\\b" + TEST_DYNAMIC_FEATURE_APP + "\\b",
                "CWWKZ0009I.*\\b" + TEST_DYNAMIC_FEATURE_APP2 + "\\b",
                "CWWKZ000[13]I.*\\b" + TEST_DYNAMIC_FEATURE_APP2 + "\\b");
            assertAppStoppedThenStarted(TEST_DYNAMIC_FEATURE_APP, "State 2 App 1", removalMessages);
            assertAppStoppedThenStarted(TEST_DYNAMIC_FEATURE_APP2, "State 2 App 2", removalMessages);
            assertEquals("State 2: unrelated app must not be restarted",
                         unrelatedMessagesBefore, countAppMessages(twoAppsServer, TEST_DYNAMIC_FEATURE_UNRELATED_APP));
            assertEquals("State 2: " + TEST_DYNAMIC_FEATURE_APP + " must have restarted exactly once",
                         3, countAppMessages(twoAppsServer, TEST_DYNAMIC_FEATURE_APP));
            assertEquals("State 2: " + TEST_DYNAMIC_FEATURE_APP2 + " must have restarted exactly once",
                         3, countAppMessages(twoAppsServer, TEST_DYNAMIC_FEATURE_APP2));

            twoAppsServer.setMarkToEndOfLog();
            FATServletClient.runTest(twoAppsServer, DYNAMIC_FEATURE_SERVLET_PATH,
                "testLibraryDependsOnFeatureApi_FeatureRemoved_CachedInterface");
            assertNotNull("State 2a App 1 NOT_LOADABLE marker not found in log",
                          twoAppsServer.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2a - NOT_LOADABLE"));

            twoAppsServer.setMarkToEndOfLog();
            FATServletClient.runTest(twoAppsServer, DYNAMIC_FEATURE_APP2_SERVLET_PATH,
                "testLibraryDependsOnFeatureApi_FeatureRemoved_CachedInterface");
            assertNotNull("State 2a App 2 NOT_LOADABLE marker not found in log",
                          twoAppsServer.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2a - NOT_LOADABLE"));

            twoAppsServer.setMarkToEndOfLog();
            FATServletClient.runTest(twoAppsServer, DYNAMIC_FEATURE_SERVLET_PATH,
                "testLibraryDependsOnFeatureApi_FeatureRemoved_FreshInterface");
            assertNotNull("State 2b App 1 marker not found in log",
                          twoAppsServer.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b -"));

            twoAppsServer.setMarkToEndOfLog();
            FATServletClient.runTest(twoAppsServer, DYNAMIC_FEATURE_APP2_SERVLET_PATH,
                "testLibraryDependsOnFeatureApi_FeatureRemoved_FreshInterface");
            assertNotNull("State 2b App 2 marker not found in log",
                          twoAppsServer.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE2b -"));

            // ── STATE 3: Re-add feature (no app restart expected) ────────────────
            twoAppsServer.setMarkToEndOfLog();
            twoAppsServer.changeFeatures(BASE_PLUS_DYNAMIC_FEATURE);
            twoAppsServer.waitForConfigUpdateInLogUsingMark(null, true);
            assertEquals("State 3: unrelated app must not be restarted",
                         unrelatedMessagesBefore, countAppMessages(twoAppsServer, TEST_DYNAMIC_FEATURE_UNRELATED_APP));
            assertEquals("State 3: " + TEST_DYNAMIC_FEATURE_APP + " must not have restarted again",
                         3, countAppMessages(twoAppsServer, TEST_DYNAMIC_FEATURE_APP));
            assertEquals("State 3: " + TEST_DYNAMIC_FEATURE_APP2 + " must not have restarted again",
                         3, countAppMessages(twoAppsServer, TEST_DYNAMIC_FEATURE_APP2));

            twoAppsServer.setMarkToEndOfLog();
            FATServletClient.runTest(twoAppsServer, DYNAMIC_FEATURE_SERVLET_PATH, "testLibraryDependsOnFeatureApi_FeatureReAdded");
            assertNotNull("State 3 App 1 SUCCESS marker not found in log",
                          twoAppsServer.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - SUCCESS"));

            twoAppsServer.setMarkToEndOfLog();
            FATServletClient.runTest(twoAppsServer, DYNAMIC_FEATURE_APP2_SERVLET_PATH, "testLibraryDependsOnFeatureApi_FeatureReAdded");
            assertNotNull("State 3 App 2 SUCCESS marker not found in log",
                          twoAppsServer.waitForStringInLogUsingMark("DYNAMIC_FEATURE_SHAREDLIB_TEST STATE3 - SUCCESS"));
        } finally {
            if (twoAppsServer.isStarted()) {
                twoAppsServer.stopServer();
            }
        }
    }

    @AfterClass
    public static void stopServer() throws Exception {
        try {
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        } finally {
            stopServerAndUninstall(twoAppsServer != null ? twoAppsServer : server);
        }
    }
}
