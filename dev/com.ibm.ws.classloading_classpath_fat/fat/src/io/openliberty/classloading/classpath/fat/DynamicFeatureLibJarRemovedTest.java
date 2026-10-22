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

import static io.openliberty.classloading.classpath.fat.FATSuite.DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_SHARED_LIB_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_UNRELATED_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_UNRELATED_WAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_DYNAMIC_FEATURE_WAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
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
 * FAT test for the dynamic feature lifecycle classloader baseline (Test 3).
 *
 * <p>Tests the three lifecycle states when the <em>library JAR</em> is removed from
 * a shared-library fileset by a {@code server.xml} change, while the feature
 * ({@code testFeatureApi-1.0}) remains installed throughout. This exercises the
 * {@code SharedLibraryImpl.delete()} eviction path, which is the complement to
 * Test 2.
 *
 * <p>The classloader chain under test is identical to Test 2:
 * <pre>
 *   WAR AppClassLoader
 *     └─ Shared-library AppClassLoader  (cached in ClassLoadingServiceImpl.aclStore)
 *          └─ GatewayClassLoader
 *               └─ EquinoxClassLoader [test.feature.api]
 * </pre>
 *
 * <ol>
 *   <li><b>State 1</b> — Library JAR and feature both present: app successfully calls
 *       through the shared library to the feature API.</li>
 *   <li><b>State 2</b> — Library JAR removed from fileset (server.xml swapped to
 *       {@code server_no_lib.xml}): {@code SharedLibraryImpl.delete()} fires, which
 *       should evict the library's {@code AppClassLoader} from {@code aclStore}.
 *       Expected outcome: {@code ClassNotFoundException}.</li>
 *   <li><b>State 3</b> — Original {@code server.xml} restored (library JAR back in
 *       fileset, feature still present): Liberty should recreate the library's
 *       {@code AppClassLoader}. Expected outcome: {@code SUCCESS}.</li>
 * </ol>
 *
 * <p><b>Contrast with Test 2:</b> That test removes the feature without
 * touching the {@code <library>} element, which bypasses {@code SharedLibraryImpl
 * .delete()} and leaves the stale {@code AppClassLoader} in {@code aclStore}
 * ({@code CLASS_STILL_VISIBLE}). Test 3 modifies the library config, triggering the
 * eviction mechanism directly. State 2 is therefore expected to produce
 * {@code ClassNotFoundException} rather than {@code CLASS_STILL_VISIBLE}.
 *
 * <p><b>Why no {@code @TestServlet} / no {@code extends FATServletClient}:</b>
 * The three lifecycle states must run inside a <em>single</em> {@code @Test} method
 * that walks State 1 → 2 → 3 in one continuous, ordered sequence. {@code @TestServlet}
 * synthesises independent JUnit tests with no FAT-class code running between them, so
 * there is no hook to inject the {@code server.xml} swap and wait for the config-update
 * message.
 */
@RunWith(FATRunner.class)
public class DynamicFeatureLibJarRemovedTest {

    @Server(DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST_SERVER)
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
        ShrinkHelper.exportAppToServer(server, TEST_DYNAMIC_FEATURE_WAR,
                                       DeployOptions.SERVER_ONLY);

        // Second app that does NOT reference the shared library or feature API. It must be left alone
        // by the library change, which proves the restart is targeted and not global.
        ShrinkHelper.exportAppToServer(server, TEST_DYNAMIC_FEATURE_UNRELATED_WAR,
                                       DeployOptions.SERVER_ONLY);

        // Deploy the shared library JAR to the server's sharedLibs directory — this
        // corresponds to the <fileset dir="${server.config.dir}/sharedLibs"> in server.xml.
        ShrinkHelper.exportToServer(server, "sharedLibs", TEST_DYNAMIC_FEATURE_SHARED_LIB_JAR,
                                    DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    /**
     * Walks State 1 → State 2 → State 3 in a single ordered sequence.
     *
     * <p>State 2 is driven by swapping the active {@code server.xml} to
     * {@code server_no_lib.xml}, which empties the shared-library fileset and triggers
     * {@code SharedLibraryImpl.delete()}. State 3 is driven by restoring the original
     * {@code server.xml}.
     *
     * <p>The test waits for {@code CWWKG0017I} or {@code CWWKG0018I}
     * (configuration update complete) between states — <em>not</em> {@code CWWKF0008I}
     * (feature update complete), since the feature list never changes in this test.
     *
     * <h3>Expected baseline summary</h3>
     * <pre>
     * State 1  Shared-library AppClassLoader → GatewayClassLoader → EquinoxClassLoader@X [bundle id=N]
     *          — doWork() succeeds, cast succeeds ✓
     *
     * State 2  SharedLibraryImpl.delete() fires on server.xml change
     *          — library AppClassLoader evicted from aclStore
     *          — ClassNotFoundException expected (correct eviction path)
     *
     * State 3  Original server.xml restored; Liberty recreates library AppClassLoader
     *          — new AppClassLoader instance created for restored library
     *          — feature bundle address unchanged (never removed)
     *          — SUCCESS expected
     * </pre>
     */
    @Test
    public void testDynamicFeatureLibJarRemovedLifecycle() throws Exception {
        // ── STATE 1: Library JAR present, feature present ────────────────────
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH,
            "testLibraryJarRemovedFromSharedLib_LibraryPresent");
        assertNotNull("State 1 SUCCESS marker not found in log",
            server.waitForStringInLogUsingMark(
                "DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE1 - SUCCESS"));

        // ── STATE 2: Swap to server_no_lib.xml (library JAR removed from fileset) ──
        // setServerConfigurationFromFilePath() resolves relative to the server root,
        // so "server_no_lib.xml" finds the file already in the server config directory.
        // setServerConfigurationFile() must NOT be used here — it looks in
        // lib/LibertyFATTestFiles/ which is a different directory.
        //
        // waitForConfigUpdateInLogUsingMark() waits for both:
        //   (a) CWWKG001[7-8]I  — config update complete
        //   (b) the named app to report started (CWWKZ0001I / CWWKZ0003I)
        // This is required because the config swap triggers an app restart cycle; the
        // servlet is briefly unavailable between CWWKG0017I and the app-ready message.
        // Using only CWWKG0017I caused a 404 "Context Root Not Found" response.
        //
        // The stop and start messages for the app are passed as extra regexps so the
        // wait itself requires them. getInstalledAppNames() scans the whole log, so the
        // initial CWWKZ0001I would satisfy the app-name wait without any restart.
        int unrelatedMessagesBefore = countAppMessages(TEST_DYNAMIC_FEATURE_UNRELATED_APP);
        server.setMarkToEndOfLog();
        server.setServerConfigurationFromFilePath("server_no_lib.xml");
        List<String> libRemovedMessages = server.waitForConfigUpdateInLogUsingMark(
            Collections.singleton(TEST_DYNAMIC_FEATURE_APP),
            "CWWKZ0009I.*" + TEST_DYNAMIC_FEATURE_APP,
            "CWWKZ000[13]I.*" + TEST_DYNAMIC_FEATURE_APP);
        assertAppStoppedThenStarted("State 2", libRemovedMessages);
        assertEquals("State 2: app that does not reference the library must not be restarted",
                     unrelatedMessagesBefore, countAppMessages(TEST_DYNAMIC_FEATURE_UNRELATED_APP));
        // Exact count guards against double-restart: initial-start(1) + stop(1) + restart(1) = 3.
        assertEquals("State 2: " + TEST_DYNAMIC_FEATURE_APP + " must have restarted exactly once",
                     3, countAppMessages(TEST_DYNAMIC_FEATURE_APP));

        // State 2a: cached-interface eviction-detection probe (informational — does not fail on class-visible)
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH,
            "testLibraryJarRemovedFromSharedLib_LibraryRemoved_CachedInterface");
        assertNotNull("State 2a marker not found in log",
            server.waitForStringInLogUsingMark(
                "DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2a -"));

        // State 2b: fresh-interface hard assertion — expects CNFE confirming loader eviction
        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH,
            "testLibraryJarRemovedFromSharedLib_LibraryRemoved_FreshInterface");
        assertNotNull("State 2b CNFE marker not found in log",
            server.waitForStringInLogUsingMark(
                "DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE2b -"));

        // ── STATE 3: Restore original server.xml (library JAR back in fileset) ──
        // server_with_lib.xml is an identical copy of the original server.xml kept
        // under a stable name. We cannot restore from "server.xml" because
        // setServerConfigurationFromFilePath() overwrites server.xml in-place —
        // using "server.xml" as the source for State 3 would re-apply the
        // server_no_lib.xml content that was written there in State 2.
        server.setMarkToEndOfLog();
        server.setServerConfigurationFromFilePath("server_with_lib.xml");
        List<String> libRestoredMessages = server.waitForConfigUpdateInLogUsingMark(
            Collections.singleton(TEST_DYNAMIC_FEATURE_APP),
            "CWWKZ0009I.*" + TEST_DYNAMIC_FEATURE_APP,
            "CWWKZ000[13]I.*" + TEST_DYNAMIC_FEATURE_APP);
        assertAppStoppedThenStarted("State 3", libRestoredMessages);
        assertEquals("State 3: app that does not reference the library must not be restarted",
                     unrelatedMessagesBefore, countAppMessages(TEST_DYNAMIC_FEATURE_UNRELATED_APP));
        // State 2 cycle(3) + stop(1) + restart(1) = 5.
        assertEquals("State 3: " + TEST_DYNAMIC_FEATURE_APP + " must have restarted exactly once",
                     5, countAppMessages(TEST_DYNAMIC_FEATURE_APP));

        server.setMarkToEndOfLog();
        FATServletClient.runTest(server, SERVLET_PATH,
            "testLibraryJarRemovedFromSharedLib_LibraryRestored");
        assertNotNull("State 3 SUCCESS marker not found in log",
            server.waitForStringInLogUsingMark(
                "DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST STATE3 - SUCCESS"));
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
            // CWWKG0014W — config element referencing a non-existent file may be emitted
            // when server_no_lib.xml is active (unmatched glob pattern in fileset).
            server.stopServer("CWWKG0014W");
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
