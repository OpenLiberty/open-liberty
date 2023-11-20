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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * Test of shared libraries when there are many applications.
 */
@RunWith(FATRunner.class)
public class SharedLibTest {
    protected Class<?> getLogClass() {
        return SharedLibTest.class;
    }

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("sharedLibServer");

    protected LibertyServer getServer() {
        return SharedLibTest.server;
    }

    //

    protected final static String PUBLISH_FILES = "publish/files";
    protected static final String APPS_DIR = "apps";

    protected static final int LIB_COUNT = 16;
    protected static final String SHARED_LIB_DIR = "snoopLib";

    // The test count corresponds with the number of applications configured in each
    // server.  Server 01 has app 0; server 04 has apps 0 through 3; server 08 has
    // apps 0 through 7; server 16 has apps 0 through 15.

    public static final String SERVER_01_XML = "server01.xml";
    public static final String SERVER_04_XML = "server04.xml";
    public static final String SERVER_08_XML = "server08.xml";
    public static final String SERVER_16_XML = "server16.xml";

    public static final String[] SERVER_XMLS =
                { SERVER_01_XML, SERVER_04_XML, SERVER_08_XML, SERVER_16_XML };

    // Application activity ranges ... negative means stopping.

    // min -> max + 1: 0..15 -> [ 0, 16 ]
    // -(min + 1) -> -((max + 1) + 1): 0..15 -> [ -1, -17 ]
    public static final int[] APP_ACTIVITY_RANGE_INITIAL =
                { 0, 16 }; // Start 0 .. 15
    public static final int[][] APP_ACTIVITY_RANGES =
                { { -2, -17 }, { 1, 4 }, { 4, 8 }, { 8, 16 } };
                // Stop 1 .. 15, Start 1 .. 3, Start 4 .. 7, Start 8 .. 15
    public static final int[] APP_ACTIVITY_RANGE_FINAL =
                { -1, -17 }; // Stop 0 .. 15

    // min -> max + 1
    public static final int[] APP_RUNNING_RANGE_INITIAL =
                { 0, 16 };
    public static final int[][] APP_RUNNING_RANGES =
                { { 0, 1 }, { 0, 4 }, { 0, 8 }, { 0, 16 } };
    public static final int[] APP_RUNNING_RANGE_FINAL =
                { 0, 16 };

    public static final String[] APP_NAMES =
                { "snoop0",  "snoop1",  "snoop2",  "snoop3",
                  "snoop4",  "snoop5",  "snoop6",  "snoop7",
                  "snoop8",  "snoop9",  "snoop10", "snoop11",
                  "snoop12", "snoop13", "snoop14", "snoop15" };

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

    public static final int NUM_VALUES = 4;

    public static final int[][] valueGrid1 =
                { { 1, 1, 0, 0 } };

    public static final int[][] valueGrid4 =
                { { 1, 1, 0, 0 }, { 1, 1, 0, 0 }, { 1, 1, 0, 0 }, { 1, 1, 0, 0 } };

    public static final int[][] valueGrid8 =
                { { 1, 1, 0, 0 }, { 1, 1, 0, 0 }, { 1, 1, 0, 0 }, { 1, 1, 0, 0 },
                  { 1, 0, 1, 0 }, { 1, 0, 1, 0 }, { 1, 0, 1, 0 }, { 1, 0, 1, 0 } };

    public static final int[][] valueGrid16 =
                { { 1, 1, 0, 0 }, { 1, 1, 0, 0 }, { 1, 1, 0, 0 }, { 1, 1, 0, 0 },
                  { 1, 0, 0, 1 }, { 1, 0, 0, 1 }, { 1, 0, 0, 1 }, { 1, 0, 0, 1 },
                  { 1, 0, 0, 1 }, { 1, 0, 0, 1 }, { 1, 0, 0, 1 }, { 1, 0, 0, 1 },
                  { 1, 0, 0, 1 }, { 1, 0, 0, 1 }, { 1, 0, 0, 1 }, { 1, 0, 0, 1 } };

    public static final int[][][] valueGrids = { valueGrid1, valueGrid4, valueGrid8, valueGrid16 };

    protected static final String SNOOP_URL_PREFIX = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop";
    protected static final String SNOOP_RESPONSE = "Shared Library Test Servlet";

    protected static final String[] PRESENT_VALUES =
                { "<tr><td>com.ibm.ws.test0.Test0</td><td>interface com.ibm.ws.test0.Test0</td><td>TEST_VALUE</td><td>Test0</td></tr>",
                  "<tr><td>com.ibm.ws.test1.Test1</td><td>interface com.ibm.ws.test1.Test1</td><td>TEST_VALUE</td><td>Test1</td></tr>",
                  "<tr><td>com.ibm.ws.test2.Test2</td><td>interface com.ibm.ws.test2.Test2</td><td>TEST_VALUE</td><td>Test2</td></tr>",
                  "<tr><td>com.ibm.ws.test3.Test3</td><td>interface com.ibm.ws.test3.Test3</td><td>TEST_VALUE</td><td>Test3</td></tr>" };

    protected static final String[] ABSENT_VALUES =
                { "<tr><td>com.ibm.ws.test0.Test0</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>",
                  "<tr><td>com.ibm.ws.test1.Test1</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>",
                  "<tr><td>com.ibm.ws.test2.Test2</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>",
                  "<tr><td>com.ibm.ws.test3.Test3</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>" };

    protected static final int CONN_TIMEOUT = 10;

    public static void assertSnoop(int snoopNo, int[] expectedValues) throws Exception {
        System.out.println("Invoke snoop [ " + snoopNo + " ]");

        URL url = new URL(SNOOP_URL_PREFIX + snoopNo);
        List<String> lines = captureURL(url);

        boolean foundResponse = false;
        boolean[] foundValues = new boolean[NUM_VALUES];

        for (String line : lines) {
            if (line.contains(SNOOP_RESPONSE)) {
                foundResponse = true;
            } else {
                for (int valueNo = 0; valueNo < NUM_VALUES; valueNo++) {
                    if (line.equals(PRESENT_VALUES[valueNo])) {
                        foundValues[valueNo] = true;
                    }
                }
            }
        }

        if (!foundResponse) {
            fail("Snoop URL [ " + url + " ] missing [ " + SNOOP_RESPONSE + " ]");
        }
        System.out.println("Snoop URL [ " + url + " ] contains [ " + SNOOP_RESPONSE + " ]");

        for (int valueNo = 0; valueNo < NUM_VALUES; valueNo++) {
            String message = "Probe value [ " + valueNo + " ] is [ " + (foundValues[valueNo] ? "present" : "missing") + " ]";
            if ((expectedValues[valueNo] > 0) != foundValues[valueNo]) {
                fail(message);
            } else {
                System.out.println(message);
            }
        }
    }

    public static List<String> captureURL(URL url) throws Exception {
        System.out.println("URL [ " + url + " ]");
        System.out.println("============================================================");

        List<String> lines = new ArrayList<>();

        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        try {
            try (BufferedReader br = HttpUtils.getConnectionStream(con)) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                    System.out.println(line);
                }
            }
        } finally {
            con.disconnect();
        }

        System.out.println("============================================================");

        return lines;
    }

    public static void assertSnoop(int[] range, int[][] expectedValues) throws Exception {
        int min = range[0];
        int max = range[1] - 1;

        System.out.println("Verifying snoop [ " + min + " ] to [ " + max + " ]");

        for (int appNo = min; appNo <= max; appNo++) {
            assertSnoop(appNo, expectedValues[appNo]);
        }
    }

    @BeforeClass
    public static void setup() throws Exception {
        setupServer();
        startServer();
    }

    public static void setupServer() throws Exception {
        // Recreate snoop, even though other tests use the same WAR.
        // We don't know the test order: We cannot rely on this
        // test running after the test that creates snoop.
        WebArchive sharedLibSnoop = ShrinkHelper.buildDefaultApp("sharedLibSnoop.war", "com.ibm.ws.test.sharedlib");

        ShrinkHelper.addDirectory(sharedLibSnoop, "test-applications/sharedLibSnoop.war/resources");
        ShrinkHelper.exportArtifact(sharedLibSnoop, PUBLISH_FILES, true, true);

        // Setup 16 jars in a shared library using four packages.  Reuse
        // the initial four jars.

        for (int libNo = 0; libNo < LIB_COUNT; libNo++) {
            String libName = "test" + libNo + ".jar";
            String libPackage = "com.ibm.ws.test" + (libNo % 4) + ".*"; // Reused packages
            JavaArchive lib = ShrinkHelper.buildJavaArchive(libName, libPackage);
            ShrinkHelper.exportArtifact(lib, PUBLISH_FILES, true, true);

            server.copyFileToLibertyServerRoot(PUBLISH_FILES, SHARED_LIB_DIR, libName);
        }

        // Re-use snoop.war.
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, "sharedLibSnoop.war");

        // Do not put in the server configuration ... the test will do this
    }

    public static void startServer() throws Exception {
        server.setServerConfigurationFile("/sharedLibServer/" + SERVER_16_XML);
        server.startServer("SharedLibTest.log");
        assertNotNull(server.waitForStringInLog("TE9900A"));
        assertActivity(APP_ACTIVITY_RANGE_INITIAL);
        assertSnoop(APP_RUNNING_RANGE_INITIAL, valueGrid16);
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
            // assertActivity(FINAL_ACTIVITY_RANGE);
        }
    }

    public static final int TEST_ITERATIONS = 32;

    @Test
    public void testSharedLibs() throws Exception {
        System.out.println("Testing shared libraries ...");

        List<ContainerAction> containerActions = new ArrayList<>();

        CaptureData priorData = assertContainerActions(0, 0, EXPECTED_CAPTURES_0_1,
                                                       INITIAL_CAPTURES, containerActions);

        for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
            int configNo = iter % 4;
            String config = SERVER_XMLS[configNo];
            int[] activityRange = APP_ACTIVITY_RANGES[configNo];
            int[] runningRange = APP_RUNNING_RANGES[configNo];

            System.out.println("Changing configuration to [ " + config + " ]");
            server.setServerConfigurationFile("/sharedLibServer/" + config);
            assertActivity(activityRange);
            assertSnoop(runningRange, valueGrids[configNo]);

            priorData = assertContainerActions(iter + 1, configNo, EXPECTED_CAPTURES[configNo],
                                               priorData, containerActions);
        }
    }

    // [11/17/23, 13:17:42:930 EST] 00000035 id=00000000
    // com.ibm.ws.app.manager.module.internal.CaptureCache 3
    // [container].capture:
    // [ C:\\dev\\repos-pub\\ol-baw\\dev\\build.image\\wlp\\usr\\servers\\sharedLibServer\\snoopLib\\test0.jar ]
    // [ 1 ]

    // [11/16/23, 16:59:39:253 EST] 0000004d id=00000000
    // com.ibm.ws.app.manager.module.internal.CaptureCache          3
    // [container].release:
    // [ C:\\dev\\repos-pub\\ol-baw\\dev\\build.image\\wlp\\usr\\servers\\sharedLibServer\\snoopLib\\test0.jar ]
    // [ 15 ]:
    // [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@45ea83a8 ]

    private static final String CONTAINER_ACTIVITY = "\\[container\\]\\.";

    private CaptureData assertContainerActions(int iter, int configNo, CaptureData expectedCapture,
                                               CaptureData priorCapture, List<ContainerAction> actions) throws Exception {
        int oldActionCount = actions.size();

        List<String> allActionLines = server.findStringsInTrace(CONTAINER_ACTIVITY);
        int newActionCount = allActionLines.size();

        System.out.println("Container actions [ " + iter + " ] [ " + oldActionCount + " ]");
        System.out.println("  [ " + CONTAINER_ACTIVITY + " ]");
        System.out.println("================================================================================");

        for ( int actionNo = oldActionCount; actionNo < newActionCount; actionNo++ ) {
            String actionLine = allActionLines.get(actionNo);
            System.out.printf("[%8d][ %s ]\n", actionNo, actionLine);

            ContainerAction action = new ContainerAction(actionLine);
            actions.add(action);
            System.out.println(action);
        }

        System.out.println("--------------------------------------------------------------------------------");

        CaptureData newCapture = new CaptureData(priorCapture, actions, oldActionCount, newActionCount);
        System.out.println("Capture:  " + newCapture);
        System.out.println("Expected: " + expectedCapture);
        System.out.println("================================================================================");

        String error = newCapture.compare(expectedCapture);
        if ( error != null ) {
            fail(error);
        }

        return newCapture;
    }

    private static final String START_CODE = "CWWKZ0001I:.*";
    private static final String STOP_CODE = "CWWKZ0009I:.*";

    private static void assertActivity(int[] range) {
        System.out.println("Verifying activity ...");

        // [ 0,   16 ] ==> // START 0 .. 15

        // [ 0,    1 ] ==> // START 0 ..  0
        // [ 1,   16 ] ==> // START 1 .. 15

        // [ -1, -18 ] ==> // STOP  0 .. 15
        // [ -2, -18 ] ==> // STOP  1 .. 15
        // [ -1,  -2 ] ==> // STOP  0 ..  1

        int min = range[0];
        int max = range[1];
        String code;
        String codeTag;
        if (min < 0) {
            min = -(min + 1);
            max = -(max + 1);
            code = STOP_CODE;
            codeTag = "Stop";
        } else {
            code = START_CODE;
            codeTag = "Start";
        }

        max -= 1;

        System.out.println("Min [ " + min + " ] Max [ " + max + " ] Code [ " + code + " ] ( " + codeTag + " )");

        List<String> activity = getActivity(min, max, code);

        verifyActivity(min, max, activity);
    }

    private static List<String> getActivity(int min, int max, String code) {
        int count = ((max + 1) - min);

        List<String> activity = new ArrayList<>(count);

        for (int actionNo = 0; actionNo < count; actionNo++) {
            String nextAction = server.waitForStringInLogUsingLastOffset(code);
            if (nextAction == null) {
                fail("Null action [ " + actionNo + " ] of [ " + count + " ]");
            }
            System.out.println("Action [ " + nextAction + " ]");
            activity.add(nextAction);
        }

        return activity;
    }

    private static void verifyActivity(int min, int max, List<String> activity) {
        int count = (max + 1) - min;

        Set<Integer> expected = new HashSet<>(count);
        for (int appNo = min; appNo <= max; appNo++) {
            expected.add(Integer.valueOf(appNo));
        }

        for (String action : activity) {
            int appNo = getAppNo(action);

            Integer actualApp = Integer.valueOf(appNo);
            boolean removed = expected.remove(actualApp);
            if (!removed) {
                fail("Unexpected app [ " + actualApp + " ]");
                return;
            }
        }

        if (!expected.isEmpty()) {
            fail("Missing apps [ " + expected + " ]");
            return;
        }
    }

    // 11/14/23, 16:32:35:584 EST] 0000003b
    //  com.ibm.ws.app.manager.AppMessageHelper
    // A CWWKZ0001I: Application snoop11 started in 0.763 seconds.

    protected static int getAppNo(String action) {
        int appLoc = action.indexOf("snoop");
        if (appLoc == -1) {
            fail("No app location [ " + action + " ]");
            return -1;
        }
        appLoc += "snoop".length();

        int actionLength = action.length();

        boolean failed;
        if (appLoc >= actionLength) {
            failed = true;
        } else {
            char c1 = action.charAt(appLoc);
            if (!Character.isDigit(c1)) {
                failed = true;

            } else {
                int appNo = c1 - '0';

                if (appLoc >= actionLength - 1) {
                    failed = true;
                } else {
                    char c2 = action.charAt(appLoc + 1);
                    if (c2 != ' ') {
                        if (!Character.isDigit(c2)) {
                            failed = true;

                        } else {
                            appNo *= 10;
                            appNo += (c2 - '0');

                            if (appLoc >= actionLength - 2) {
                                failed = true;
                            } else {
                                char c3 = action.charAt(appLoc + 2);
                                if (c3 != ' ') {
                                    failed = true;
                                } else {
                                    failed = false;
                                }
                            }
                        }
                    } else {
                        failed = false;
                    }

                    if (!failed) {
                        return appNo;
                    }
                }
            }
        }

        fail("Bad app number [ " + action + " ]");
        return -1;
    }

    //

    public static final String ACTION_TAG = "[container].";
    public static final String CAPTURE_TAG = "capture";
    public static final String RELEASE_TAG = "release";

    private static class ContainerAction {
        public final boolean isCapture;
        public final String archive;
        public final int references;
        public final String supplierClass;

        public final String asString;

        @Override
        public String toString() {
            return asString;
        }

        public ContainerAction(String line) {
            // [container].release:
            // [ C:\\dev\\repos-pub\\ol-baw\\dev\\build.image\\wlp\\usr\\servers\\sharedLibServer\\snoopLib\\test0.jar ]
            // [ 15 ]:
            // [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@45ea83a8 ]

            int lineLen = line.length();

            int actionOffset = line.indexOf(ACTION_TAG);
            if ( actionOffset == -1 ) {
                throw new IllegalArgumentException("No action tag [ " + line + " ]");
            }

            actionOffset += ACTION_TAG.length();
            if ( line.regionMatches(actionOffset, CAPTURE_TAG, 0, CAPTURE_TAG.length()) ) {
                this.isCapture = true;
            } else if ( line.regionMatches(actionOffset, RELEASE_TAG, 0, RELEASE_TAG.length()) ) {
                this.isCapture = false;
            } else {
                throw new IllegalArgumentException("Bad action [ " + line + " ]");
            }

            actionOffset += (this.isCapture ? CAPTURE_TAG : RELEASE_TAG).length();

            int firstBrace = -1;
            int lastBrace = -1;

            int lastSlash = -1;

            while ( (lastBrace == -1) && (actionOffset < lineLen) ) {
                char c = line.charAt(actionOffset);
                if ( firstBrace == -1 ) {
                    if ( c == '[' ) {
                        firstBrace = actionOffset;
                    }
                } else {
                    if ( c == '[' ) {
                        throw new IllegalArgumentException("Misplaced open brace [ " + line + " ]");
                    } else if ( (c == '/') || (c == '\\') ) {
                        lastSlash = actionOffset;
                    } else if ( c == ']' ) {
                        lastBrace = actionOffset;
                    }
                }
                actionOffset++;
            }

            if ( firstBrace == -1 ) {
                throw new IllegalArgumentException("Missing open brace [ " + line + " ]");
            } else if ( lastBrace == -1 ) {
                throw new IllegalArgumentException("Missing close brace [ " + line + " ]");
            } else if ( (lastBrace - firstBrace) < 4 ) { // [ x ] // need at least 4
                throw new IllegalArgumentException("Incomplete archive [ " + line + " ]");
            } else {
                firstBrace++; // remove space
                lastBrace--; // remove space
            }

            int archiveStart = ( (lastSlash == -1) ? firstBrace : lastSlash );
            this.archive = line.substring(archiveStart + 1, lastBrace);

            firstBrace = -1;
            lastBrace = -1;

            while ( (lastBrace == -1) && (actionOffset < lineLen) ) {
                char c = line.charAt(actionOffset);
                if ( firstBrace == -1 ) {
                    if ( c == '[' ) {
                        firstBrace = actionOffset;
                    }
                } else {
                    if ( c == '[' ) {
                        throw new IllegalArgumentException("Misplaced open brace [ " + line + " ]");
                    } else if ( c == ']' ) {
                        lastBrace = actionOffset;
                    }
                }
                actionOffset++;
            }

            if ( (lastBrace - firstBrace) < 4 ) { // [ x ] // need at least 4
                throw new IllegalArgumentException("Incomplete references [ " + line + " ]");
            } else {
                firstBrace++; // remove space
                lastBrace--; // remove space
            }

            String referencesText = line.substring(firstBrace + 1, lastBrace);
            try {
                this.references = Integer.parseInt(referencesText);
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException("Non-numeric references [ " + line + " ]", e);
            }

            firstBrace = -1;
            lastBrace = -1;

            int lastDot = -1;

            while ( (lastBrace == -1) && (actionOffset < lineLen) ) {
                char c = line.charAt(actionOffset);
                if ( firstBrace == -1 ) {
                    if ( c == '[' ) {
                        firstBrace = actionOffset;
                    }
                } else {
                    if ( c == '[' ) {
                        throw new IllegalArgumentException("Misplaced open brace [ " + line + " ]");
                    } else if ( c == '.' ) {
                        lastDot = actionOffset;
                    } else if ( c == ']' ) {
                        lastBrace = actionOffset;
                    }
                }
                actionOffset++;
            }

            if ( firstBrace == -1 ) {
                throw new IllegalArgumentException("Missing open brace [ " + line + " ]");
            } else if ( lastBrace == -1 ) {
                throw new IllegalArgumentException("Missing close brace [ " + line + " ]");
            } else if ( (lastBrace - firstBrace) < 4 ) { // [ x ] // need at least 4
                throw new IllegalArgumentException("Incomplete supplier class [ " + line + " ]");
            } else {
                firstBrace++; // remove space
                lastBrace--; // remove space
            }

            int supplierStart = ( (lastDot == -1) ? firstBrace : lastDot );

            String useClass = line.substring(supplierStart + 1, lastBrace);
            if ( useClass.isEmpty() ) {
                throw new IllegalArgumentException("Empty archive [ " + line + " ]");
            } else {
                this.supplierClass = useClass;
            }

            this.asString = "[" + (isCapture ? "capture" : "release") + "]" +
                            "[" + String.format("%3d", references) + "]" +
                            "[" + archive + "]" +
                            "[" + supplierClass + "]";
        }
    }

    //

    // initial[4]: last: [16, 4, 0, 12] c[32] r[0]:  32
    // update[4-1] last: [1,  1, 0,  0] c[0]  r[30]:  2
    // update[1-2] last: [4,  4, 0,  0] c[6]  r[0]:   8
    // update[2-3] last: [8,  4, 4,  0] c[8]  r[0]:  16
    // update[3-4] last: [16, 4, 0, 12] c[20] r[4]:  32

    public static final CaptureData INITIAL_CAPTURES =
                    new CaptureData( new int[] { 0, 0, 0, 0 }, 0, 0, 0 );

    public static final CaptureData EXPECTED_CAPTURES_0_1 =
                    new CaptureData( new int[] { 16, 4, 0, 12 }, 32, 0, 32 );

    public static final CaptureData EXPECTED_CAPTURES_16_1 =
                    new CaptureData( new int[] { 1, 1, 0, 0 }, 0, 30, 2 );

    public static final CaptureData EXPECTED_CAPTURES_1_4 =
                    new CaptureData( new int[] { 4, 4, 0, 0 }, 6, 0, 8 );

    public static final CaptureData EXPECTED_CAPTURES_4_8 =
                    new CaptureData( new int[] { 8, 4, 4, 0 }, 8 + 4, 0 + 4, 16 );
    // The releases and captures for (4 -> 8) are 4 higher because
    // the configuration changes for apps 2 and 3, from A: { test0, test1 } to B: { test0, test1 }.
    // the comparison evidently doesn't compare the actual shared libraries: the comparison
    // only sees that a different shared library has been set.
    // similarly for the captures (8 -> 16).
    public static final CaptureData EXPECTED_CAPTURES_8_16 =
                    new CaptureData( new int[] { 16, 4, 0, 12 }, 24 + 4, 8 + 4, 32 );
    // The releases and captures for (8 -> 16) are 4 higher; see the comment
    // for the captures (4 -> 8).

    public static String fill_3(int value) {
        return fill(value, 3, ' ');
    }

    public static String fill_4(int value) {
        return fill(value, 4, ' ');
    }

    public static String fill(int value, int width, char fillChar) {
        char[] valueChars = new char[width];

        int remaining = width;

        if ( value == 0 ) {
            valueChars[ --remaining ] = '0';

        } else {
            int initialValue = value;

            while ( (value > 0) && (remaining > 0) ) {
                int nextDigit = value % 10;
                value = value / 10;

                valueChars[ --remaining ] = (char) ('0' + nextDigit);
            }

            if ( value > 0 ) {
                return Integer.toString(initialValue);
            }
        }

        while ( remaining > 0 ) {
            valueChars[ --remaining ] = fillChar;
        }

        return new String(valueChars);
    }

    public static final CaptureData[] EXPECTED_CAPTURES = new CaptureData[]
                    { EXPECTED_CAPTURES_16_1,
                      EXPECTED_CAPTURES_1_4,
                      EXPECTED_CAPTURES_4_8,
                      EXPECTED_CAPTURES_8_16 };

    public static class CaptureData {
        public final int[] referenceCounts;

        public final int captureActions;
        public final int releaseActions;
        public final int captureTotal;

        public final String asString;

        @Override
        public String toString() {
            return asString;
        }

        private static String asString(int[] refs, int capture, int release, int total) {
            return "[" + fill_3(refs[0]) +
                            ", " + fill_3(refs[1]) +
                            ", " + fill_3(refs[2]) +
                            ", " + fill_3(refs[3]) + "]" +
                   " c[" + fill_4(capture) + "]" +
                   " r[" + fill_4(release) + "]: " +
                   fill_4(total);
        }

        public CaptureData(int[] referenceCounts, int captureActions, int releaseActions, int captureTotal) {
            int captured = 0;
            for ( int refNo = 0; refNo < referenceCounts.length; refNo++ ) {
                captured += referenceCounts[refNo];
            }
            if ( captured != captureTotal ) {
                throw new IllegalArgumentException("Inconsistent capture total [ " + captureTotal + " ]");
            }

            this.referenceCounts = referenceCounts;
            this.captureActions = captureActions;
            this.releaseActions = releaseActions;
            this.captureTotal = captureTotal;

            // [16, 4, 0, 12] c[32] r[0]:  32

            this.asString = asString(referenceCounts,
                                     captureActions, releaseActions, captureTotal);
        }

        public CaptureData(int[] referenceCounts, int captureActions, int releaseActions, CaptureData priorData) {
            this(referenceCounts,
                 captureActions, releaseActions,
                 priorData.captureTotal + captureActions + releaseActions);
        }

        private static int archiveNo(String archive) {
            int archiveLen = archive.length();
            if ( archiveLen < 5 ) {
                throw new IllegalArgumentException("Unexpected archive [ " + archive + " ]");
            }
            if ( archive.charAt(archiveLen - 4) != '.' ) {
                throw new IllegalArgumentException("Non-valid archive [ " + archive + " ]");
            }

            char archiveChar = archive.charAt(archiveLen - 5);
            int archiveNo = archiveChar - '0';
            if ( (archiveNo < 0) || (archiveNo > 3) ) {
                throw new IllegalArgumentException("Archive out of range [ " + archive + " ]");
            }
            return archiveNo;
        }

        public CaptureData(CaptureData priorData,
                           List<ContainerAction> actions, int first, int last) {

            int[] useReferenceCounts = new int[4];

            int useCaptureTotal;

            if ( priorData != null ) {
                for ( int archiveNo = 0; archiveNo < 4; archiveNo++ ) {
                    useReferenceCounts[archiveNo] = priorData.referenceCounts[archiveNo];
                }
                useCaptureTotal = priorData.captureTotal;
            } else {
                useCaptureTotal = 0;
            }

            int useCaptureActions = 0;
            int useReleaseActions = 0;

            for ( int actionNo = first; actionNo < last; actionNo++ ) {
                ContainerAction action = actions.get(actionNo);

                if ( action.isCapture ) {
                    useCaptureActions++;
                    useCaptureTotal++;
                } else {
                    useReleaseActions++;
                    useCaptureTotal--;
                }

                useReferenceCounts[ archiveNo(action.archive) ] = action.references;

                // Use the most recent action value.
                //
                // Alternatively, the reference counts could be incrementally adjusted:
                //
                // int refCount =
                //   ( useReferenceCounts[ archiveNo(action.archive) ] += (action.isCapture ? +1 : -1) );
                //
                // assertEquals(refCount, action.references);
                //
                // TODO: Add this test.
            }

            this.referenceCounts = useReferenceCounts;
            this.captureActions = useCaptureActions;
            this.releaseActions = useReleaseActions;
            this.captureTotal = useCaptureTotal;

            this.asString = asString(useReferenceCounts,
                                     useCaptureActions, useReleaseActions, useCaptureTotal);
        }

        public String compare(CaptureData expected) {
            String error;

            for ( int archiveNo = 0; archiveNo < 4; archiveNo++ ) {
                error = compare( referenceCounts[archiveNo], expected.referenceCounts[archiveNo],
                                 "Incorrect references to archive [ " + archiveNo + " ]");
                if ( error != null ) {
                    return error;
                }
            }

            error = compare( captureActions, expected.captureActions, "Incorrect capture actions");
            if ( error != null ) {
                return error;
            }

            error = compare( releaseActions, expected.releaseActions, "Incorrect release actions");
            if ( error != null ) {
                return error;
            }

            error = compare( captureTotal, expected.captureTotal, "Incorrect capture total");
            if ( error != null ) {
                return error;
            }

            return null;
        }

        public String compare(int actual, int expected, String message) {
            if ( actual == expected ) {
                return null;
            }
            return ( message + "; expected [ " + expected + " ] actual [ " + actual + " ]" );
        }
    }
}
