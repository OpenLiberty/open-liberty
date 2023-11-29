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

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test of shared libraries when there are many applications.
 */
public class SharedLibTestUtils {
    public Class<?> getLogClass() {
        return SharedLibTestUtils.class;
    }

    public static void assertNotNull(Object value) {
        Assert.assertNotNull(value);
    }

    public static void fail(String message) {
        Assert.fail(message);
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
        if ( setupLibs ) {
            return;
        }

        synchronized( sharedLibLock ) {
            if ( setupLibs) {
                return;
            }

            JavaArchive[] libs = new JavaArchive[LIB_COUNT];

            for ( int libNo = 0; libNo < LIB_COUNT; libNo++ ) {
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
        for ( int libNo = 0; libNo < LIB_COUNT; libNo++ ) {
            String libName = "test" + libNo + ".jar";
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, SHARED_LIB_DIR, libName);
        }
    }

    public static final boolean FROM_ALT = true;

    public static void installLib(LibertyServer server, boolean fromAlt, String libName) throws Exception {
        String publishDir = ( fromAlt ? PUBLISH_FILES_ALT : PUBLISH_FILES );
        server.copyFileToLibertyServerRoot(publishDir, SHARED_LIB_DIR, libName);
    }

    public static final String WAR_NAME = "sharedLibSnoop.war";
    public static final String WAR_PACKAGE_NAME = "com.ibm.ws.test.sharedlib";

    public static final String APPS_DIR = "apps";

    public static volatile boolean setupSnoop;
    public static volatile WebArchive sharedLibSnoop;
    public static Object sharedLibSnoopAppLock = new Object();

    public static void setupApp() throws Exception {
        if ( !setupSnoop ) {
            synchronized(sharedLibSnoopAppLock) {
                if ( !setupSnoop ) {
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

    public static final String CONTAINER_ACTIVITY = "\\[container\\]\\.";

    public static final CacheTransitions INITIAL_CAPTURES =
        new CacheTransitions( new int[] { 0, 0, 0, 0 }, 0, 0, 0 );

    public static CacheTransitions assertContainerActions(LibertyServer server,
                                                        int iter, int configNo, CacheTransitions expectedCapture,
                                                        CacheTransitions priorCapture, List<ContainerAction> actions) throws Exception {
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

        CacheTransitions newCapture = new CacheTransitions(priorCapture, actions, oldActionCount, newActionCount);
        System.out.println("Capture:  " + newCapture);
        System.out.println("Expected: " + expectedCapture);
        System.out.println("================================================================================");

        String error = newCapture.compare(expectedCapture);
        if ( error != null ) {
            fail(error);
        }

        return newCapture;
    }


    // [11/28/23, 17:24:58:244 EST] 00000037 id=00000000 com.ibm.ws.app.manager.AppMessageHelper
    // A CWWKZ0001I: Application snoop0 started in 3.385 seconds.

    // [11/28/23, 17:25:01:416 EST] 00000034 id=00000000 com.ibm.ws.app.manager.AppMessageHelper
    // A CWWKZ0003I: The application snoop0 updated in 0.099 seconds.

    // [11/28/23, 17:25:01:292 EST] 0000003b id=00000000 com.ibm.ws.app.manager.AppMessageHelper
    // A CWWKZ0009I: The application snoop0 has stopped successfully.

    public static final String START_CODE  = "CWWKZ0001I:.*";
    public static final String UPDATE_CODE = "CWWKZ0003I:.*";
    public static final String STOP_CODE   = "CWWKZ0009I:.*";

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

        verifyActivity( range, getActivity(server, range, change) );
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

    public static final String ACTION_TAG = "[container].";
    public static final String CAPTURE_TAG = "capture";
    public static final String RELEASE_TAG = "release";

    public static class ContainerAction {
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

    public static class CacheTransitions {
        public final int[] referenceCounts;

        public final int actionsCapture;
        public final int actionsRelease;
        public final int actionsTotal;

        public final String asString;

        @Override
        public String toString() {
            return asString;
        }

        public static String asString(int[] refs, int capture, int release, int total) {
            return "[" + fill_3(refs[0]) +
                            ", " + fill_3(refs[1]) +
                            ", " + fill_3(refs[2]) +
                            ", " + fill_3(refs[3]) + "]" +
                   " c[" + fill_4(capture) + "]" +
                   " r[" + fill_4(release) + "]: " +
                   fill_4(total);
        }

        public CacheTransitions(int[] referenceCounts,
                                int actionsCapture, int actionsRelease, int actionsTotal) {

            int captured = 0;
            for ( int refNo = 0; refNo < referenceCounts.length; refNo++ ) {
                captured += referenceCounts[refNo];
            }
            if ( captured != actionsTotal ) {
                throw new IllegalArgumentException("Inconsistent capture total [ " + actionsTotal + " ]");
            }

            this.referenceCounts = referenceCounts;
            this.actionsCapture = actionsCapture;
            this.actionsRelease = actionsRelease;
            this.actionsTotal = actionsTotal;

            // [16, 4, 0, 12] c[32] r[0]:  32

            this.asString = asString(referenceCounts,
                                     actionsCapture, actionsRelease, actionsTotal);
        }

        public CacheTransitions(int[] referenceCounts,
                                int actionsCapture, int actionsRelease, CacheTransitions priorTransitions) {
            this(referenceCounts,
                 actionsCapture, actionsRelease,
                 priorTransitions.actionsTotal + actionsCapture + actionsRelease);
        }

        public static int archiveNo(String archiveName) {
            int archiveLen = archiveName.length();
            if ( archiveLen < 5 ) {
                throw new IllegalArgumentException("Unexpected archive [ " + archiveName + " ]");
            }
            if ( archiveName.charAt(archiveLen - 4) != '.' ) {
                throw new IllegalArgumentException("Non-valid archive [ " + archiveName + " ]");
            }

            char archiveChar = archiveName.charAt(archiveLen - 5);
            int archiveNo = archiveChar - '0';
            if ( (archiveNo < 0) || (archiveNo > 3) ) {
                throw new IllegalArgumentException("Archive out of range [ " + archiveName + " ]");
            }
            return archiveNo;
        }

        public CacheTransitions(CacheTransitions priorTransitions,
                                List<ContainerAction> allActions,
                                int firstTransitionNo, int lastTransitionNo) {

            int[] useReferenceCounts = new int[LIB_COUNT];

            int useCaptureTotal;

            if ( priorTransitions != null ) {
                for ( int archiveNo = 0; archiveNo < LIB_COUNT; archiveNo++ ) {
                    useReferenceCounts[archiveNo] = priorTransitions.referenceCounts[archiveNo];
                }
                useCaptureTotal = priorTransitions.actionsTotal;
            } else {
                useCaptureTotal = 0;
            }

            int useCaptureActions = 0;
            int useReleaseActions = 0;

            for ( int actionNo = firstTransitionNo; actionNo < lastTransitionNo; actionNo++ ) {
                ContainerAction action = allActions.get(actionNo);

                int adj;
                if ( action.isCapture ) {
                    useCaptureActions++;
                    adj = +1;
                } else {
                    useReleaseActions++;
                    adj = -1;
                }

                // The reference count running total can deviate from the value
                // which appears in the action.
                //
                // For example, here, the third and fourth release actions were
                // logged out of order:
                //
                // [8:50:35:620] 00000036 [container].release: [ test0.jar ] [ 4 ]
                // [8:50:35:628] 0000002d [container].release: [ test0.jar ] [ 3 ]
                // [8:50:35:658] 00000038 [container].release: [ test0.jar ] [ 1 ]
                // [8:50:35:659] 0000003b [container].release: [ test0.jar ] [ 2 ]
                //   : [ CaptureCache$CaptureSupplier@18b9f40d ]
                //
                // That out-of-order write occurs because the cache trace write is
                // performed outside of the cache lock.
                //
                // The write could be performed inside of the cache lock, at a cost
                // to performance.

                useReferenceCounts[ archiveNo(action.archive) ] += adj;
                useCaptureTotal += adj;
            }

            this.referenceCounts = useReferenceCounts;
            this.actionsCapture = useCaptureActions;
            this.actionsRelease = useReleaseActions;
            this.actionsTotal = useCaptureTotal;

            this.asString = asString(useReferenceCounts,
                                     useCaptureActions, useReleaseActions, useCaptureTotal);
        }

        public String compare(CacheTransitions expected) {
            String error;

            for ( int archiveNo = 0; archiveNo < LIB_COUNT; archiveNo++ ) {
                error = compare( referenceCounts[archiveNo], expected.referenceCounts[archiveNo],
                                 "Incorrect references to archive [ " + archiveNo + " ]");
                if ( error != null ) {
                    return error;
                }
            }

            error = compare( actionsCapture, expected.actionsCapture, "Incorrect capture actions");
            if ( error != null ) {
                return error;
            }

            error = compare( actionsRelease, expected.actionsRelease, "Incorrect release actions");
            if ( error != null ) {
                return error;
            }

            error = compare( actionsTotal, expected.actionsTotal, "Incorrect capture total");
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

    public static void verifyContainers(List<ContainerAction> containerActions) {
        Map<String, String> allSuppliers = new HashMap<>();
        Map<String, int[]> allTransitions = new HashMap<>();

        for ( ContainerAction action : containerActions ) {
            boolean isCapture = action.isCapture;
            String archive = action.archive;
            int references = action.references;
            String supplier = action.supplierClass;

            String priorSupplier = allSuppliers.get(archive);

            // Transitions shows the amount of activity there is between
            // the each initial capture of an archive and each final release
            // of that archive.  For now, the value is only being displayed.
            // No tests are done on the value.

            int[] transitions = allTransitions.computeIfAbsent(archive, (String useArchive) -> new int[1]);

            String failure;
            String success;
            int adjustment;

            if ( isCapture && (references == 1) ) { // Should add a supplier.
                if ( priorSupplier != null ) {
                    failure = "found prior supplier [ " + priorSupplier + " ]";
                    success = null;
                } else {
                    failure = null;
                    success = "adds supplier [ " + supplier + " ]";
                    allSuppliers.put(archive, supplier);
                    transitions[0]++;
                }

            } else if ( !isCapture && (references == 0) ) { // Should remove a supplier.
                if ( priorSupplier == null ) {
                    failure = "found no prior supplier";
                    success = null;
                } else if ( !priorSupplier.equals(supplier) ) {
                    failure = "changed supplier from [ " + priorSupplier + " ] to [ " + supplier + " ]";
                    success = null;
                } else {
                    failure = null;
                    success = "removed supplier [ " + priorSupplier + " ]";
                    allSuppliers.remove(archive);
                    transitions[0]++;
                }

            } else { // Should leave the supplier unchanged.
                if ( priorSupplier == null ) {
                    failure = "found no prior supplier";
                    success = null;
                } else if ( !priorSupplier.equals(supplier) ) {
                    failure = "changed supplier from [ " + priorSupplier + " ] to [ " + supplier + " ]";
                    success = null;
                } else {
                    failure = null; // The correct supplier is associated with the archive.
                    success = null; // "leaves supplier as [ " + activeSupplier + " ]";
                    transitions[0]++;
                }
            }

            if ( (failure != null) || (success != null) ) {
                String actionTag = ( isCapture ? "Capture" : "Release" );
                String prefix = "Action [ " + actionTag + " ] references [ " + references + " ] archive [ " + archive + " ]: ";
                if ( failure != null ) {
                    fail(prefix + failure);
                } else {
                    System.out.println(prefix + success + ": transitions [ " + transitions[0] + " ]");
                    if ( !isCapture && (references == 0) ) {
                        transitions[0] = 0;
                    }
                }
            }
        }
    }

    //

    public static final int NUM_VALUES = 6;

    public static final String PRESENT_VALUE_0_BASE =
        "<tr><td>com.ibm.ws.test0_base.Test0_Base</td><td>interface com.ibm.ws.test0_base.Test0_Base</td><td>TEST_VALUE</td><td>Test0</td></tr>";
    public static final String PRESENT_VALUE_0_ALT =
        "<tr><td>com.ibm.ws.test0_alt.Test0_Alt</td><td>interface com.ibm.ws.test0_alt.Test0_Alt</td><td>TEST_VALUE</td><td>Test0_Alt</td></tr>";
    public static final String PRESENT_VALUE_1_BASE =
        "<tr><td>com.ibm.ws.test1_base.Test1_Base</td><td>interface com.ibm.ws.test1_base.Test1_Base</td><td>TEST_VALUE</td><td>Test1</td></tr>";
    public static final String PRESENT_VALUE_1_ALT =
        "<tr><td>com.ibm.ws.test1_alt.Test1_Alt</td><td>interface com.ibm.ws.test1_alt.Test1_Alt</td><td>TEST_VALUE</td><td>Test1_Alt</td></tr>";
    public static final String PRESENT_VALUE_2_BASE =
        "<tr><td>com.ibm.ws.test2_base.Test2_Base</td><td>interface com.ibm.ws.test2_base.Test2_Base</td><td>TEST_VALUE</td><td>Test2</td></tr>";
    public static final String PRESENT_VALUE_3_BASE =
        "<tr><td>com.ibm.ws.test3_base.Test3_Base</td><td>interface com.ibm.ws.test3_base.Test3_Base</td><td>TEST_VALUE</td><td>Test3</td></tr>";

    public static final String[] PRESENT_VALUES =
        { PRESENT_VALUE_0_BASE, PRESENT_VALUE_0_ALT,
          PRESENT_VALUE_1_BASE, PRESENT_VALUE_1_ALT,
          PRESENT_VALUE_2_BASE,
          PRESENT_VALUE_3_BASE };

    public static final String ABSENT_VALUE_0_BASE =
        "<tr><td>com.ibm.ws.test0_base.Test0_Base</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";
    public static final String ABSENT_VALUE_0_ALT =
        "<tr><td>com.ibm.ws.test0_alt.Test0_Alt</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";
    public static final String ABSENT_VALUE_1_BASE =
        "<tr><td>com.ibm.ws.test1_base.Test1_Base</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";
    public static final String ABSENT_VALUE_1_ALT =
        "<tr><td>com.ibm.ws.test1_alt.Test1_Alt</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";
    public static final String ABSENT_VALUE_2_BASE =
        "<tr><td>com.ibm.ws.test2_base.Test2_Base</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";
    public static final String ABSENT_VALUE_3_BASE =
        "<tr><td>com.ibm.ws.test3_base.Test3_Base</td><td>null</td><td>TEST_VALUE</td><td>null</td></tr>";

    public static final String[] ABSENT_VALUES =
        { ABSENT_VALUE_0_BASE, ABSENT_VALUE_0_ALT,
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

        URL url = new URL( getSnoopUrlPrefix(server) + snoopNo);
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
