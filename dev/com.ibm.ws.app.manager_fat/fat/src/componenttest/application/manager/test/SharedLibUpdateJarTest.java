/*******************************************************************************
 * Copyright (c) 2020,2023 IBM Corporation and others.
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
package componenttest.application.manager.test;

import static componenttest.application.manager.test.SharedLibTestUtils.FROM_ALT;
import static componenttest.application.manager.test.SharedLibTestUtils.assertContainerActions;
import static componenttest.application.manager.test.SharedLibTestUtils.assertRestarted;
import static componenttest.application.manager.test.SharedLibTestUtils.assertSnoop;
import static componenttest.application.manager.test.SharedLibTestUtils.installLib;
import static componenttest.application.manager.test.SharedLibTestUtils.setupServer;
import static componenttest.application.manager.test.SharedLibTestUtils.startServer;
import static componenttest.application.manager.test.SharedLibTestUtils.stopServer;
import static componenttest.application.manager.test.SharedLibTestUtils.verifyContainers;
import static componenttest.application.manager.test.SharedLibTestUtils.verifyServer;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.application.manager.test.SharedLibTestUtils.CacheTransitions;
import componenttest.application.manager.test.SharedLibTestUtils.ContainerAction;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test of shared libraries when there are many applications.
 */
@RunWith(FATRunner.class)
public class SharedLibUpdateJarTest {
    public Class<?> getLogClass() {
        return SharedLibUpdateJarTest.class;
    }

    public static void assertNotNull(Object value) {
        SharedLibTestUtils.assertNotNull(value);
    }

    public static void fail(String message) {
        SharedLibTestUtils.fail(message);
    }

    //

    public static final String SERVER_NAME = "sharedLibJarServer";
    public static final LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);

    public static final String SERVER_2SA_2SB_XML = "server2sa2sb.xml";
    public static final String SERVER_3SA_2SB_XML = "server3sa2sb.xml";
    public static final String SERVER_2SA_3SB_XML = "server2sa3sb.xml";
    public static final String SERVER_3SA_3SB_XML = "server3sa3sb.xml";

    public static final String LIB_NAME_0 = SharedLibTestUtils.LIB_NAME_0;
    public static final String LIB_NAME_1 = SharedLibTestUtils.LIB_NAME_1;
    public static final String LIB_NAME_2 = SharedLibTestUtils.LIB_NAME_2;
    public static final String LIB_NAME_3 = SharedLibTestUtils.LIB_NAME_3;

    public static final String[] APP_NAMES = { "snoop0",  "snoop1",  "snoop2",  "snoop3" };
    public static final int APP_COUNT = APP_NAMES.length;

    // Negative means stopping: -(appNo + 1).
    public static final int[] APP_ACTIVITY_INITIAL = { 0, APP_COUNT };
    public static final int[] APP_ACTIVITY_FINAL = { -1, -(APP_COUNT + 1) };

    // min -> max + 1
    public static final int[] APPS_RUNNING = { 0, APP_COUNT };

    // Usage grids:

    // A: y n n n
    // B: n y n n
    // C: n n y n
    // D: n n n y

    public static final CacheTransitions INITIAL_TRANSITIONS =
        new CacheTransitions( new int[] { 0, 0, 0, 0 }, 0, 0, 0 );

    // app0 uses lib0, lib1
    // app1 uses lib0, lib1
    // app2 uses lib2
    // app3 uses lib3
    public static final CacheTransitions EXPECTED_TRANSITIONS_TO_BASE =
        new CacheTransitions( new int[] { 2, 2, 1, 1 }, 6, 0, 6 );

    // app0 refreshes
    // app1 does NOT refresh
    public static final CacheTransitions EXPECTED_TRANSITIONS_BASE_TO_ALT0 =
        new CacheTransitions( new int[] { 2, 2, 1, 1 }, 2, 2, 6 );

    // app0 does NOT refresh
    // app1 refreshes
    public static final CacheTransitions EXPECTED_TRANSITIONS_ALT0_TO_ALT0_ALT1 =
        new CacheTransitions( new int[] { 2, 2, 1, 1 }, 2, 2, 6 );

    // app0 refreshes
    // app1 does NOT refresh
    public static final CacheTransitions EXPECTED_TRANSITIONS_ALT0_ALT1_TO_ALT1 =
        new CacheTransitions( new int[] { 2, 2, 1, 1 }, 2, 2, 6 );

    // app0 does NOT refresh
    // app1 refreshes
    public static final CacheTransitions EXPECTED_TRANSITIONS_ALT1_TO_BASE =
        new CacheTransitions( new int[] { 2, 2, 1, 1 }, 2, 2, 6 );

    public static final int[] EXPECTED_VALUES_BASE0_BASE1 = { 1, 0, 1, 0, 0, 0 };
    public static final int[] EXPECTED_VALUES_ALT0_BASE1  = { 0, 1, 1, 0, 0, 0 };
    public static final int[] EXPECTED_VALUES_BASE0_ALT1  = { 1, 0, 0, 1, 0, 0 };
    public static final int[] EXPECTED_VALUES_ALT0_ALT1   = { 0, 1, 0, 1, 0, 0 };

    public static final int[] EXPECTED_VALUES_APP2_BASE = { 0, 0, 0, 0, 1, 0 };
    public static final int[] EXPECTED_VALUES_APP3_BASE = { 0, 0, 0, 0, 0, 1 };

    public static final int[][] EXPECTED_VALUES_BASE =
        { EXPECTED_VALUES_BASE0_BASE1,
          EXPECTED_VALUES_BASE0_BASE1,
          EXPECTED_VALUES_APP2_BASE,
          EXPECTED_VALUES_APP3_BASE };

    public static final int[][] EXPECTED_VALUES_BASE_TO_ALT0 =
        { EXPECTED_VALUES_ALT0_BASE1, // Refreshed of lib0
          EXPECTED_VALUES_BASE0_BASE1, // Stale lib0 !!!
          EXPECTED_VALUES_APP2_BASE,
          EXPECTED_VALUES_APP3_BASE };

    public static final int[][] EXPECTED_VALUES_ALT0_TO_ALT0_ALT1 =
        { EXPECTED_VALUES_ALT0_BASE1, // Stale lib1 !!!
          EXPECTED_VALUES_ALT0_ALT1, // Refresh of lib0 and lib1 !!!
          EXPECTED_VALUES_APP2_BASE,
          EXPECTED_VALUES_APP3_BASE };

    public static final int[][] EXPECTED_VALUES_ALT0_ALT1_TO_ALT1 =
        { EXPECTED_VALUES_BASE0_ALT1, // Refresh lib0 and lib1 !!!
          EXPECTED_VALUES_ALT0_ALT1, // Stale lib0 !!!
          EXPECTED_VALUES_APP2_BASE,
          EXPECTED_VALUES_APP3_BASE };

    public static final int[][] EXPECTED_VALUES_ALT1_TO_BASE =
        { EXPECTED_VALUES_BASE0_ALT1, // Stale lib1 !!!
          EXPECTED_VALUES_BASE0_BASE1, // Refresh lib0 and lib1 !!!
          EXPECTED_VALUES_APP2_BASE,
          EXPECTED_VALUES_APP3_BASE };

    @BeforeClass
    public static void setup() throws Exception {
        setupServer(server);
        startServer(SERVER_NAME, server, SERVER_2SA_2SB_XML);
        verifyServer(server, APP_ACTIVITY_INITIAL, APPS_RUNNING, EXPECTED_VALUES_BASE);
    }

    @AfterClass
    public static void teardown() throws Exception {
        stopServer(server);
    }

    // [11/28/23, 16:15:35:542 EST] 00000044 id=00000000 com.ibm.ws.app.manager.AppMessageHelper A
    // CWWKZ0001I: Application snoop0 started in 3.129 seconds.
    // [11/28/23, 16:15:40:051 EST] 0000003e id=00000000 com.ibm.ws.app.manager.AppMessageHelper A
    // CWWKZ0009I: The application snoop3 has stopped successfully.

    public CacheTransitions verifyUpdate(String serverConfig,
                                         boolean fromAlt, String libName,
                                         int appNo,
                                         int[] appsRunning,
                                         int[][] expectedValues,
                                         CacheTransitions expectedTransitions,
                                         CacheTransitions transitions,
                                         List<ContainerAction> containerActions) throws Exception {

        String title = ( fromAlt ? "Updating [ " : "Restoring [ " ) + libName + " ]";
        System.out.println(title);
        installLib(server, fromAlt, libName);

        // Updating an archive is not triggering a refresh!!!
        // Updating the configuration does.
        System.out.println("Using server configuration [ " + serverConfig + " ]");
        server.setServerConfigurationFile(SERVER_NAME + '/' + serverConfig);

        // The apps are already running ... the update causes a different
        // message code than the initial application start.
        assertRestarted(server, appNo);
        assertSnoop(server, appsRunning, expectedValues);

        return assertContainerActions(server, 0, 0,
                                      expectedTransitions, transitions, containerActions);
    }

    @Test
    public void testSharedLibUpdateJar() throws Exception {
        System.out.println("Testing shared library jar updates ...");

        CacheTransitions transitions = INITIAL_TRANSITIONS;
        List<ContainerAction> containerActions = new ArrayList<>();

        transitions = assertContainerActions(server, 0, 0,
                                             EXPECTED_TRANSITIONS_TO_BASE,
                                             transitions, containerActions);

        // Updating the configuration is a hack to for the applications to restart.
        // Simply modifying a JAR does not seem to trigger an update.
        //
        // This creates an odd result: Both app0 and app1 use jar0 and jar1.
        // A refresh of jar0 should cause both apps to restart.
        // This does not happen.  To verify that the capture of the updated
        // jar refreshes the container of that jar, the test performs a
        // configuration update, which causes app restarts.
        //
        // The test is set to update only one of the application configurations at a time.
        // That results in one of the applications lagging in obtaining updated jars.
        //
        // Then: The initial update to jar0 is followed by an update of the app0 configuration.
        // App0 sees the update to jar0; app1 does not see the update to jar0.
        //
        // The following update to jar1 is followed by an update of the app1 configuration.
        // App1 sees the update to jar1 (and sees the prior update to jar0);
        // app0 does not see the update to jar1.

        transitions = verifyUpdate(SERVER_3SA_2SB_XML, // force app0/sharedLibA to refresh
                                   FROM_ALT, LIB_NAME_0,
                                   0, APPS_RUNNING, // app0 should restart; app1 should, but does not
                                   EXPECTED_VALUES_BASE_TO_ALT0,
                                   EXPECTED_TRANSITIONS_BASE_TO_ALT0,
                                   transitions, containerActions);

        transitions = verifyUpdate(SERVER_3SA_3SB_XML, // force app1/sharedLibB to refresh
                                   FROM_ALT, LIB_NAME_1,
                                   1, APPS_RUNNING, // app1 should restart; app0 should, but does not
                                   EXPECTED_VALUES_ALT0_TO_ALT0_ALT1,
                                   EXPECTED_TRANSITIONS_ALT0_TO_ALT0_ALT1,
                                   transitions, containerActions);

        transitions = verifyUpdate(SERVER_2SA_3SB_XML, // force app0/sharedLibA to refresh
                                   !FROM_ALT, LIB_NAME_0,
                                   0, APPS_RUNNING, // app0 should restart; app1 should, but does not
                                   EXPECTED_VALUES_ALT0_ALT1_TO_ALT1,
                                   EXPECTED_TRANSITIONS_ALT0_ALT1_TO_ALT1,
                                   transitions, containerActions);

        transitions = verifyUpdate(SERVER_2SA_2SB_XML, // force app1/sharedLibB to refresh
                                   !FROM_ALT, LIB_NAME_1,
                                   1, APPS_RUNNING, // app1 should restart; app0 should, but does not
                                   EXPECTED_VALUES_ALT1_TO_BASE,
                                   EXPECTED_TRANSITIONS_ALT1_TO_BASE,
                                   transitions, containerActions);

        verifyContainers(containerActions);
    }
}
