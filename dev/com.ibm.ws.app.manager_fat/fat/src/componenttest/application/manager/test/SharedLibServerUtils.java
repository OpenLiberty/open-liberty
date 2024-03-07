/*******************************************************************************
 * Copyright (c) 2020,2024 IBM Corporation and others.
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

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test of shared libraries when there are many applications.
 */
public class SharedLibServerUtils {
    public static void assertNotNull(Object value) {
        SharedLibTestUtils.assertNotNull(value);
    }

    public static void fail(String message) {
        SharedLibTestUtils.fail(message);
    }

    //

    public static final int SNOOP_TIMEOUT = 10;

    public static List<String> captureURL(URL url) throws Exception {
        System.out.println("URL [ " + url + " ]");
        System.out.println("============================================================");

        List<String> lines = new ArrayList<>();

        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, SNOOP_TIMEOUT);
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

    public static String getSnoopUrlPrefix(LibertyServer server) {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop";
    }

    //

    public static void setup(String serverName, LibertyServer server, String serverConfig) throws Exception {
        setupServer(server);
        startServer(serverName, server, serverConfig);
    }

    public static void setupServer(LibertyServer server) throws Exception {
        setupLibs();
        installLibs(server);

        setupApp();
        installApp(server);
    }

    public final static String PUBLISH_FILES = "publish/files";
    public final static String PUBLISH_FILES_ALT = "publish/files_alt";

    public static final int LIB_COUNT = 4;
    public static final String SHARED_LIB_DIR = "snoopLib";

    public static volatile boolean setupLibs;

    public static final String LIB_NAME_0 = "test0.jar";
    public static final String LIB_NAME_1 = "test1.jar";
    public static final String LIB_NAME_2 = "test2.jar";
    public static final String LIB_NAME_3 = "test3.jar";

    // alt0 and alt1 reuse LIB_NAME_0 and LIB_NAME_1
    public static final String LIB_NAMES[] = { LIB_NAME_0, LIB_NAME_1, LIB_NAME_2, LIB_NAME_3 };

    public static volatile JavaArchive[] sharedLibs;
    public static volatile JavaArchive sharedLib_alt0;
    public static volatile JavaArchive sharedLib_alt1;

    public static Object sharedLibLock = new Object();

    public static void setupLibs() throws Exception {
        if (setupLibs) {
            return;
        }

        synchronized (sharedLibLock) {
            if (setupLibs) {
                return;
            }

            JavaArchive[] libs = new JavaArchive[LIB_COUNT];

            for (int libNo = 0; libNo < LIB_COUNT; libNo++) {
                String libName = "test" + libNo + ".jar";
                String libPackage = "com.ibm.ws.test" + libNo + "_base.*"; // Reused packages
                JavaArchive lib = ShrinkHelper.buildJavaArchive(libName, libPackage);
                ShrinkHelper.exportArtifact(lib, PUBLISH_FILES, true, true);

                libs[libNo] = lib;
            }

            // Reusing the names forces the archives to be put in a different directory.

            String libName_alt0 = LIB_NAME_0;
            String libPackage_alt0 = "com.ibm.ws.test0_alt.*";
            JavaArchive lib_alt0 = ShrinkHelper.buildJavaArchive(libName_alt0, libPackage_alt0);
            ShrinkHelper.exportArtifact(lib_alt0, PUBLISH_FILES_ALT, true, true);

            String libName_alt1 = LIB_NAME_1;
            String libPackage_alt1 = "com.ibm.ws.test1_alt.*";
            JavaArchive lib_alt1 = ShrinkHelper.buildJavaArchive(libName_alt1, libPackage_alt1);
            ShrinkHelper.exportArtifact(lib_alt1, PUBLISH_FILES_ALT, true, true);

            sharedLibs = libs;

            sharedLib_alt0 = lib_alt0;
            sharedLib_alt1 = lib_alt1;

            setupLibs = true;
        }
    }

    public static void installLibs(LibertyServer server) throws Exception {
        for (int libNo = 0; libNo < LIB_COUNT; libNo++) {
            String libName = "test" + libNo + ".jar";
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, SHARED_LIB_DIR, libName);
        }
    }

    public static final boolean FROM_ALT = true;

    public static void installLib(LibertyServer server, boolean fromAlt, String libName) throws Exception {
        String publishDir = (fromAlt ? PUBLISH_FILES_ALT : PUBLISH_FILES);
        server.copyFileToLibertyServerRoot(publishDir, SHARED_LIB_DIR, libName);
    }

    public static final String WAR_NAME = "sharedLibSnoop.war";
    public static final String WAR_PACKAGE_NAME = "com.ibm.ws.test.sharedlib";

    public static final String APPS_DIR = "apps";

    public static volatile boolean setupSnoop;
    public static volatile WebArchive sharedLibSnoop;
    public static Object sharedLibSnoopAppLock = new Object();

    public static void setupApp() throws Exception {
        if (!setupSnoop) {
            synchronized (sharedLibSnoopAppLock) {
                if (!setupSnoop) {
                    // Recreate snoop, even though other tests use the same WAR.
                    // We don't know the test order: We cannot rely on this
                    // test running after the test that creates snoop.
                    WebArchive snoop = ShrinkHelper.buildDefaultApp(WAR_NAME, WAR_PACKAGE_NAME);

                    ShrinkHelper.addDirectory(snoop, "test-applications/sharedLibSnoop.war/resources");
                    ShrinkHelper.exportArtifact(snoop, PUBLISH_FILES, true, true);

                    // Do not put in the server configuration ... the test will do this

                    sharedLibSnoop = snoop;
                    setupSnoop = true;
                }
            }
        }
    }

    public static void installApp(LibertyServer server) throws Exception {
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, WAR_NAME);
    }

    public static void startServer(String serverName,
                                   LibertyServer server,
                                   String serverConfig) throws Exception {

        server.setServerConfigurationFile(serverName + '/' + serverConfig);
        server.startServer(serverName + ".log");
        assertNotNull(server.waitForStringInLog("TE9900A"));
    }

    public static void verifyServer(LibertyServer server,
                                    int[] activity,
                                    int[] runningRange,
                                    int[][] expectedValues) throws Exception {

        assertActivity(server, activity);
        assertSnoop(server, runningRange, expectedValues);
    }

    public static void stopServer(LibertyServer server) throws Exception {
        if (server.isStarted()) {
            server.stopServer();
            // assertActivity(server, FINAL_ACTIVITY_RANGE);
        }
    }

    //

    // [11/28/23, 17:24:58:244 EST] 00000037 id=00000000 com.ibm.ws.app.manager.AppMessageHelper
    // A CWWKZ0001I: Application snoop0 started in 3.385 seconds.

    // [11/28/23, 17:25:01:416 EST] 00000034 id=00000000 com.ibm.ws.app.manager.AppMessageHelper
    // A CWWKZ0003I: The application snoop0 updated in 0.099 seconds.

    // [11/28/23, 17:25:01:292 EST] 0000003b id=00000000 com.ibm.ws.app.manager.AppMessageHelper
    // A CWWKZ0009I: The application snoop0 has stopped successfully.

    public static final String START_CODE = "CWWKZ0001I:.*";
    public static final String UPDATE_CODE = "CWWKZ0003I:.*";
    public static final String STOP_CODE = "CWWKZ0009I:.*";

    public static enum AppStateChange {
        STARTED(START_CODE), UPDATED(UPDATE_CODE), STOPPED(STOP_CODE);

        public final String code;

        private AppStateChange(String code) {
            this.code = code;
        }
    }

    public static void assertRestarted(LibertyServer server, int appNo) {
        assertStopped(server, appNo);
        assertUpdated(server, appNo);
    }

    public static void assertStarted(LibertyServer server, int appNo) {
        assertActivity(server, new int[] { appNo, appNo + 1 }, AppStateChange.STARTED);
    }

    public static void assertStopped(LibertyServer server, int appNo) {
        assertActivity(server, new int[] { appNo, appNo + 1 }, AppStateChange.STOPPED);
    }

    public static void assertUpdated(LibertyServer server, int appNo) {
        assertActivity(server, new int[] { appNo, appNo + 1 }, AppStateChange.UPDATED);
    }

    public static void assertActivity(LibertyServer server, int[] range) {
        // [ 0,   16 ] ==> // START 0 .. 15

        // [ 0,    1 ] ==> // START 0 ..  0
        // [ 1,   16 ] ==> // START 1 .. 15

        // [ -1, -18 ] ==> // STOP  0 .. 15
        // [ -2, -18 ] ==> // STOP  1 .. 15
        // [ -1,  -2 ] ==> // STOP  0 ..  1

        int min = range[0];
        int max = range[1];
        AppStateChange change;
        if (min < 0) {
            min = -(min + 1);
            max = -(max + 1);
            range = new int[] { min, max };
            change = AppStateChange.STOPPED;
        } else {
            change = AppStateChange.STARTED;
        }

        assertActivity(server, range, change);
    }

    public static void assertActivity(LibertyServer server, int[] range, AppStateChange change) {
        System.out.println("Verifying activity ...");
        System.out.println("Min [ " + range[0] + " ] Max [ " + range[1] + " ]" +
                           " Code [ " + change + " ] ( " + change.code + " )");

        verifyActivity(range, getActivity(server, range, change));
    }

    public static List<String> getActivity(LibertyServer server, int[] range, AppStateChange change) {
        int count = range[1] - range[0];

        List<String> activity = new ArrayList<>(count);

        for (int actionNo = 0; actionNo < count; actionNo++) {
            String nextAction = server.waitForStringInLogUsingLastOffset(change.code);
            if (nextAction == null) {
                fail("Null action [ " + actionNo + " ] of [ " + count + " ]");
            }
            System.out.println("Action [ " + nextAction + " ]");
            activity.add(nextAction);
        }

        return activity;
    }

    public static void verifyActivity(int[] range, List<String> activity) {
        int min = range[0];
        int max = range[1];
        int count = max - min;

        Set<Integer> expected = new HashSet<>(count);

        for (int appNo = min; appNo < max; appNo++) {
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

    public static int getAppNo(String action) {
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

    public static final int NUM_VALUES = 6;

    public static final String PRESENT_VALUE_0_BASE = "<tr><td>com.ibm.ws.test0_base.Test0_Base</td><td>interface com.ibm.ws.test0_base.Test0_Base</td><td>TEST_VALUE</td><td>Test0</td></tr>";
    public static final String PRESENT_VALUE_0_ALT = "<tr><td>com.ibm.ws.test0_alt.Test0_Alt</td><td>interface com.ibm.ws.test0_alt.Test0_Alt</td><td>TEST_VALUE</td><td>Test0_Alt</td></tr>";
    public static final String PRESENT_VALUE_1_BASE = "<tr><td>com.ibm.ws.test1_base.Test1_Base</td><td>interface com.ibm.ws.test1_base.Test1_Base</td><td>TEST_VALUE</td><td>Test1</td></tr>";
    public static final String PRESENT_VALUE_1_ALT = "<tr><td>com.ibm.ws.test1_alt.Test1_Alt</td><td>interface com.ibm.ws.test1_alt.Test1_Alt</td><td>TEST_VALUE</td><td>Test1_Alt</td></tr>";
    public static final String PRESENT_VALUE_2_BASE = "<tr><td>com.ibm.ws.test2_base.Test2_Base</td><td>interface com.ibm.ws.test2_base.Test2_Base</td><td>TEST_VALUE</td><td>Test2</td></tr>";
    public static final String PRESENT_VALUE_3_BASE = "<tr><td>com.ibm.ws.test3_base.Test3_Base</td><td>interface com.ibm.ws.test3_base.Test3_Base</td><td>TEST_VALUE</td><td>Test3</td></tr>";

    public static final String[] PRESENT_VALUES = { PRESENT_VALUE_0_BASE, PRESENT_VALUE_0_ALT,
                                                    PRESENT_VALUE_1_BASE, PRESENT_VALUE_1_ALT,
                                                    PRESENT_VALUE_2_BASE,
                                                    PRESENT_VALUE_3_BASE };

    public static final String ABSENT_VALUE_0_BASE = "<tr><td>com.ibm.ws.test0_base.Test0_Base</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";
    public static final String ABSENT_VALUE_0_ALT = "<tr><td>com.ibm.ws.test0_alt.Test0_Alt</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";
    public static final String ABSENT_VALUE_1_BASE = "<tr><td>com.ibm.ws.test1_base.Test1_Base</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";
    public static final String ABSENT_VALUE_1_ALT = "<tr><td>com.ibm.ws.test1_alt.Test1_Alt</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";
    public static final String ABSENT_VALUE_2_BASE = "<tr><td>com.ibm.ws.test2_base.Test2_Base</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";
    public static final String ABSENT_VALUE_3_BASE = "<tr><td>com.ibm.ws.test3_base.Test3_Base</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";

    public static final String[] ABSENT_VALUES = { ABSENT_VALUE_0_BASE, ABSENT_VALUE_0_ALT,
                                                   ABSENT_VALUE_1_BASE, ABSENT_VALUE_1_ALT,
                                                   ABSENT_VALUE_2_BASE,
                                                   ABSENT_VALUE_3_BASE };

    public static final String SNOOP_RESPONSE = "Shared Library Test Servlet";

    public static void assertSnoop(LibertyServer server,
                                   int[] range, int[][] expectedValues) throws Exception {
        int min = range[0];
        int max = range[1] - 1;

        System.out.println("Verifying snoop [ " + min + " ] to [ " + max + " ]");

        for (int appNo = min; appNo <= max; appNo++) {
            assertSnoop(server, appNo, expectedValues[appNo]);
        }
    }

    public static void assertSnoop(LibertyServer server,
                                   int snoopNo, int[] expectedValues) throws Exception {
        System.out.println("Invoke snoop [ " + snoopNo + " ]");

        URL url = new URL(getSnoopUrlPrefix(server) + snoopNo);
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

        String responseMessage = "Snoop URL [ " + url + " ] " + (foundResponse ? "contains" : "missing") + " [ " + SNOOP_RESPONSE + " ]";
        System.out.println(responseMessage);
        if (!foundResponse) {
            fail(responseMessage);
        }

        for (int valueNo = 0; valueNo < NUM_VALUES; valueNo++) {
            String message = "Probe value [ " + valueNo + " ] is [ " + (foundValues[valueNo] ? "present" : "missing") + " ]";
            System.out.println(message);
            if ((expectedValues[valueNo] > 0) != foundValues[valueNo]) {
                fail(message);
            }
        }
    }
}