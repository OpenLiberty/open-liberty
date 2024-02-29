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

import static componenttest.application.manager.test.SharedLibTestUtils.assertActivity;
import static componenttest.application.manager.test.SharedLibTestUtils.assertContainerActions;
import static componenttest.application.manager.test.SharedLibTestUtils.assertSnoop;
import static componenttest.application.manager.test.SharedLibTestUtils.setupServer;
import static componenttest.application.manager.test.SharedLibTestUtils.startServer;
import static componenttest.application.manager.test.SharedLibTestUtils.verifyContainers;
import static componenttest.application.manager.test.SharedLibTestUtils.verifyServer;

import java.util.ArrayList;
import java.util.List;

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
public class SharedLibUpdateConfigTest {
    public Class<?> getLogClass() {
        return SharedLibUpdateConfigTest.class;
    }

    public static void assertNotNull(Object value) {
        SharedLibTestUtils.assertNotNull(value);
    }

    public static void fail(String message) {
        SharedLibTestUtils.fail(message);
    }

    //

    public static final String SERVER_NAME = "sharedLibConfigServer";
    public static final LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);

    //

    public static final String SERVER_01_XML = "server01.xml";
    public static final String SERVER_04_XML = "server04.xml";
    public static final String SERVER_08_XML = "server08.xml";
    public static final String SERVER_16_XML = "server16.xml";

    public static final String[] SERVER_XMLS =
        { SERVER_01_XML, SERVER_04_XML, SERVER_08_XML, SERVER_16_XML };

    //

    public static final String[] APP_NAMES =
        { "snoop0",  "snoop1",  "snoop2",  "snoop3",
          "snoop4",  "snoop5",  "snoop6",  "snoop7",
          "snoop8",  "snoop9",  "snoop10", "snoop11",
          "snoop12", "snoop13", "snoop14", "snoop15" };

    public static final int APP_COUNT = APP_NAMES.length;

    // Negative means stopping: -(appNo + 1).

    // min -> max + 1: 0..15 -> [ 0, 16 ]
    // -(min + 1) -> -((max + 1) + 1): 0..15 -> [ -1, -17 ]
    public static final int[] APP_ACTIVITY_INITIAL =
        { 0, APP_COUNT }; // Start 0 .. 15
    public static final int[][] APP_ACTIVITY =
        { { -2, -17 }, { 1, 4 }, { 4, 8 }, { 8, APP_COUNT } };
                // Stop 1 .. 15, Start 1 .. 3, Start 4 .. 7, Start 8 .. 15

    // min -> max + 1
    public static final int[] APPS_RUNNING_INITIAL =
        { 0, APP_COUNT };
    public static final int[][] APPS_RUNNING =
        { { 0, 1 }, { 0, 4 }, { 0, 8 }, { 0, APP_COUNT } };

    // Capture history:
    //
    // initial[4]: last: [16, 4, 0, 12] c[32] r[0]: 32
    //
    // c0(+16) r0(none)
    // c1(+4)  r1(none)
    // c2(+0)  r2(none)
    // c3(+12) r3(none)
    //
    // update[4-1] last: [1, 1, 0, 0] c[0] r[30]: 2
    //
    // r0(-12-3)
    // r1(-3)
    // r2(none)
    // r3(-12)
    //
    // update[1-2] last: [4, 4, 0, 0] c[6] r[0]: 8
    //
    // c0(+3)
    // c1(+3)
    // c2(none)
    // c3(none)
    //
    // update[2-3] last: [8, 4, 4, 0] c[8] r[0]: 16
    //
    // c0(+4)
    // c1(none)
    // c3(+4)
    // c4(none)
    //
    // update[3-4] last: [16, 4, 0, 12] c[20] r[4]: 32
    //
    // c0(+8)
    // c1(none)
    // r2(-4)
    // c3(+12)

    // initial[4]: last: [16, 4, 0, 12] c[32] r[0]:  32
    // update[4-1] last: [1, 1, 0, 0]   c[0]  r[30]:  2
    // update[1-2] last: [4, 4, 0, 0]   c[6]  r[0]:   8
    // update[2-3] last: [8, 4, 4, 0]   c[8]  r[0]:  16
    // update[3-4] last: [16, 4, 0, 12] c[20] r[4]:  32

    // Usage grids:

    // A: y y n n
    // B: y y n n
    // C: y n y n
    // D: y n n y

    //            A
    // test0(1) [snoop0: y/y/n/n]
    // test1(1)
    // test2(0)
    // test3(0)

    //            A A B B
    // test0(4) [snoop0: y/y/n/n]
    // test1(4) [snoop1: y/y/n/n]
    // test2(0) [snoop2: y/y/n/n]
    // test3(0) [snoop3: y/y/n/n]

    //            A A A A           C C C C
    // test0(8) [snoop0: y/y/n/n] [snoop4: y/n/y/n]
    // test1(4) [snoop1: y/y/n/n] [snoop5: y/n/y/n]
    // test2(4) [snoop2: y/y/n/n] [snoop6: y/n/y/n]
    // test3(0) [snoop3: y/y/n/n] [snoop7: y/n/y/n]

    //            A A B B           D D D D           D D D D            D D D D
    // test0(16) [snoop0: y/y/n/n] [snoopy: y/n/n/y] [snoop8:  y/n/n/y] [snoop12: y/n/n/y]
    // test1(4)  [snoop1: y/y/n/n] [snoop5: y/n/n/y] [snoop9:  y/n/n/y] [snoop13: y/n/n/y]
    // test2(0)  [snoop2: y/y/n/n] [snoop6: y/n/n/y] [snoop10: y/n/n/y] [snoop14: y/n/n/y]
    // test3(12) [snoop3: y/y/n/n] [snoop7: y/n/n/y] [snoop11: y/n/n/y] [snoop15: y/n/n/y]

    public static final int NUM_VALUES = SharedLibTestUtils.NUM_VALUES;

    public static final int[] VALUES_0_1 = { 1, 0, 1, 0, 0, 0 }; // 0-Y 0Alt-N 1-Y 1Alt-N 2-N 3-N
    public static final int[] VALUES_0_2 = { 1, 0, 0, 0, 1, 0 }; // 0-Y 0Alt-N 1-N 1Alt-N 2-Y 3-N
    public static final int[] VALUES_0_3 = { 1, 0, 0, 0, 0, 1 }; // 0-Y 0Alt-N 1-N 1Alt-N 2-N 3-Y

    public static final int[][] EXPECTED_VALUES_1 =
        { VALUES_0_1 };

    public static final int[][] EXPECTED_VALUES_4 =
        { VALUES_0_1, VALUES_0_1, VALUES_0_1, VALUES_0_1 };

    public static final int[][] EXPECTED_VALUES_8 =
        { VALUES_0_1, VALUES_0_1, VALUES_0_1, VALUES_0_1,
          VALUES_0_2, VALUES_0_2, VALUES_0_2, VALUES_0_2 };

    public static final int[][] EXPECTED_VALUES_16 =
        { VALUES_0_1, VALUES_0_1, VALUES_0_1, VALUES_0_1,
          VALUES_0_3, VALUES_0_3, VALUES_0_3, VALUES_0_3,
          VALUES_0_3, VALUES_0_3, VALUES_0_3, VALUES_0_3,
          VALUES_0_3, VALUES_0_3, VALUES_0_3, VALUES_0_3 };

    public static final int[][][] ALL_EXPECTED_VALUES =
        { EXPECTED_VALUES_1, EXPECTED_VALUES_4, EXPECTED_VALUES_8, EXPECTED_VALUES_16 };

    @BeforeClass
    public static void setup() throws Exception {
        setupServer(server);
        startServer(SERVER_NAME, server, SERVER_16_XML);
        verifyServer(server, APP_ACTIVITY_INITIAL, APPS_RUNNING_INITIAL, EXPECTED_VALUES_16);
    }

    public static final int TEST_ITERATIONS = 32;

    @Test
    public void testSharedLibsUpdateConfig() throws Exception {
        System.out.println("Testing shared library configuration updates ...");

        List<ContainerAction> containerActions = new ArrayList<>();

        CacheTransitions priorData =
            assertContainerActions(server,
                                                      0, 0, EXPECTED_CAPTURES_0_16,
                                                      INITIAL_CAPTURES, containerActions);

        for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
            int configNo = iter % 4;
            String config = SERVER_XMLS[configNo];
            int[] appActivity = APP_ACTIVITY[configNo];
            int[] appsRunning = APPS_RUNNING[configNo];

            System.out.println("Changing configuration to [ " + config + " ]");
            server.setServerConfigurationFile(SERVER_NAME + '/' + config);
            assertActivity(server, appActivity);
            assertSnoop(server, appsRunning, ALL_EXPECTED_VALUES[configNo]);

            priorData = assertContainerActions(server,
                                               iter + 1, configNo, EXPECTED_CAPTURES[configNo],
                                               priorData, containerActions);
        }

        verifyContainers(containerActions);
    }

    //

    // initial[4]: last: [16, 4, 0, 12] c[32] r[0]:  32
    // update[4-1] last: [1,  1, 0,  0] c[0]  r[30]:  2
    // update[1-2] last: [4,  4, 0,  0] c[6]  r[0]:   8
    // update[2-3] last: [8,  4, 4,  0] c[8]  r[0]:  16
    // update[3-4] last: [16, 4, 0, 12] c[20] r[4]:  32

    public static final CacheTransitions INITIAL_CAPTURES =
        new CacheTransitions( new int[] { 0, 0, 0, 0 }, 0, 0, 0 );

    public static final CacheTransitions EXPECTED_CAPTURES_0_16 =
        new CacheTransitions( new int[] { 16, 4, 0, 12 }, 32, 0, 32 );

    public static final CacheTransitions EXPECTED_CAPTURES_16_1 =
        new CacheTransitions( new int[] { 1, 1, 0, 0 }, 0, 30, 2 );

    public static final CacheTransitions EXPECTED_CAPTURES_1_4 =
        new CacheTransitions( new int[] { 4, 4, 0, 0 }, 6, 0, 8 );

    public static final CacheTransitions EXPECTED_CAPTURES_4_8 =
        new CacheTransitions( new int[] { 8, 4, 4, 0 }, 8 + 4, 0 + 4, 16 );

    // The releases and captures for (4 -> 8) are 4 higher because
    // the configuration changes for apps 2 and 3, from A: { test0, test1 } to B: { test0, test1 }.
    // the comparison evidently doesn't compare the actual shared libraries: the comparison
    // only sees that a different shared library has been set.
    // similarly for the captures (8 -> 16).
    public static final CacheTransitions EXPECTED_CAPTURES_8_16 =
        new CacheTransitions( new int[] { 16, 4, 0, 12 }, 24 + 4, 8 + 4, 32 );
    // The releases and captures for (8 -> 16) are 4 higher; see the comment
    // for the captures (4 -> 8).

    public static final CacheTransitions[] EXPECTED_CAPTURES =
        { EXPECTED_CAPTURES_16_1,
          EXPECTED_CAPTURES_1_4,
          EXPECTED_CAPTURES_4_8,
          EXPECTED_CAPTURES_8_16 };
}
