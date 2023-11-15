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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.websphere.simplicity.ShrinkHelper;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Test of shared libraries when there are many applications.
 */
@RunWith(FATRunner.class)
public class SharedLibTest {
    protected Class<?> getLogClass() {
        return SharedLibTest.class;
    }

    private static LibertyServer server =
        LibertyServerFactory.getLibertyServer("sharedLibServer");

    protected LibertyServer getServer() {
        return SharedLibTest.server;
    }

    //

    protected final static String PUBLISH_FILES = "publish/files";
    protected static final String APPS_DIR = "apps";

    protected static final int LIB_COUNT = 16;
    protected static final String SHARED_LIB_DIR = "snoopLibs";

    // The test count corresponds with the number of applications configured in each
    // server.  Server 01 has app 0; server 04 has apps 0 through 3; server 08 has
    // apps 0 through 7; server 16 has apps 0 through 15.

    public static final String SERVER_01_XML = "server01.xml";
    public static final String SERVER_04_XML = "server04.xml";
    public static final String SERVER_08_XML = "server08.xml";
    public static final String SERVER_16_XML = "server16.xml";

    public static final String[] SERVER_XMLS =
        new String[] { SERVER_01_XML, SERVER_04_XML, SERVER_08_XML, SERVER_16_XML };

    // Application activity ranges ... negative means stopping.
    //
    // Ranges are always min(abs) to max(abs): [1, 3] for starting apps
    // 1, 2, and 3; [-1, -3] for stopping apps 1, 2, and 3.
    //
    // Activity is of applictions starting or stopping when changing
    // the server XML.
    //
    // For example going from XMLS[2] to XMLS[3] means
    // going from 8 apps to 16 apps, which means starting apps 8 to 15.
    // Going to XMLS[0] means going from XMLS[3] to XMLS[0], which
    // means going from 16 apps down to 1 app, which means stopping
    // apps 1 to 15.

    // Going from nothing to 0 through 15.
    public static final int[] INITIAL_RANGE = new int[] { 0, 15 };

    public static final int[][] APP_RANGES =
        new int[][] { { -2, -16 }, { 1, 3 }, { 4, 7 }, { 8, 15 } };

    // Going from 0 through 15 to nothing.
    public static final int[] FINAL_RANGE = new int[] { -1, -16 };

    public static final String[] APP_NAMES = {
        "snoop0", "snoop1", "snoop2", "snoop3", 
        "snoop4", "snoop5", "snoop6", "snoop7", 
        "snoop8", "snoop9", "snoop10", "snoop11", 
        "snoop12", "snoop13", "snoop14", "snoop15" };

    @BeforeClass
    public static void setup() throws Exception {
        setupServer();
        startServer();
    }

    public static void setupServer() throws Exception {
        // Recreate snoop, even though other tests use the same WAR.
        // We don't know the test order: We cannot rely on this
        // test running after the test that creates snoop.
        WebArchive sharedLibSnoop = ShrinkHelper.buildDefaultApp("sharedLibSnoop.war",
                                                                 "com.ibm.app.monitor",
                                                                 "com.ibm.ws.security.web.saml.sample");
        ShrinkHelper.addDirectory(sharedLibSnoop, "test-applications/snoop.war/resources");
        ShrinkHelper.exportArtifact(sharedLibSnoop, PUBLISH_FILES, true, true);

        // Setup 16 jars in a shared library using four packages.  Reuse
        // the initial four jars.

        for ( int libNo = 0; libNo < LIB_COUNT; libNo++ ) {
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
        assertNotNull( server.waitForStringInLog("TE9900A") );
        assertActivity(INITIAL_RANGE);
    }

    // URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
    // Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
    // //check application is installed
    // HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
    // BufferedReader br = HttpUtils.getConnectionStream(con);
    // String line = br.readLine();
    // assertTrue("The response did not contain the \'Snoop Servlet\'",
    // line.contains("Snoop Servlet"));
    // con.disconnect();

    @AfterClass
    public static void stopServer() throws Exception {
        if ( server.isStarted() ) {
            server.stopServer();
            // assertActivity(FINAL_RANGE);
        }
    }

    public static final int TEST_ITERATIONS = 32;

    @Test
    public void testSharedLibs() throws Exception {
        System.out.println("Testing shared libraries ...");

        for ( int iter = 0; iter < TEST_ITERATIONS; iter++ ) {
            int configNo = iter % 4;
            String config = SERVER_XMLS[configNo];
            int[] range = APP_RANGES[configNo];

            System.out.println("Changing configuration to [ " + config + " ]");
            server.setServerConfigurationFile("/sharedLibServer/" + config);
            assertActivity(range);
        }
    }

    private static final String START_CODE = "CWWKZ0001I:.*";
    private static final String STOP_CODE = "CWWKZ0009I:.*";

    private static void assertActivity(int[] range) {
        System.out.println("Verifying activity ...");

        int min = range[0];
        int max = range[1];
        String code;
        String codeTag;
        if ( min < 0 ) { // [ -2, -16 ] ==> // STOP 1 ... 15
            min = -(min + 1);
            max = -(max + 1);
            code = STOP_CODE;
            codeTag = "Stop";
        } else { // [ 1, 15 ] ==> START 1 ... 15
            code = START_CODE;
            codeTag = "Start";
        }

        System.out.println("Min [ " + min + " ] Max [ " + max + " ] Code [ " + code + " ] ( " + codeTag + " )");

        List<String> activity = getActivity(min, max, code);

        verifyActivity(min, max, activity);
    }

    private static List<String> getActivity(int min, int max, String code) {
        int count = ((max + 1) - min);

        List<String> activity = new ArrayList<>(count);

        for ( int actionNo = 0; actionNo < count; actionNo++ ) {
            String nextAction = server.waitForStringInLogUsingLastOffset(code);
            if ( nextAction == null ) {
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
        for ( int appNo = min; appNo <= max; appNo++ ) {
            expected.add( Integer.valueOf(appNo) );
        }

        for ( String action : activity ) {
            int appNo = getAppNo(action);

            Integer actualApp = Integer.valueOf(appNo);
            boolean removed = expected.remove(actualApp);
            if ( !removed ) {
                fail("Unexpected app [ " + actualApp + " ]");
                return;
            }
        }

        if ( !expected.isEmpty() ) {
            fail("Missing apps [ " + expected + " ]");
            return;
        }
    }

    // 11/14/23, 16:32:35:584 EST] 0000003b
    //  com.ibm.ws.app.manager.AppMessageHelper
    // A CWWKZ0001I: Application snoop11 started in 0.763 seconds.

    protected static int getAppNo(String action) {
        int appLoc = action.indexOf("snoop");
        if ( appLoc == -1 ) {
            fail("No app location [ " + action + " ]");
            return -1;
        }
        appLoc += "snoop".length();

        int actionLength = action.length();

        boolean failed;
        if ( appLoc >= actionLength ) {
            failed = true;
        } else {
            char c1 = action.charAt(appLoc);
            if ( !Character.isDigit(c1) ) {
                failed = true;

            } else {
                int appNo = c1 - '0';

                if ( appLoc >= actionLength - 1 ) {
                    failed = true;
                } else {
                    char c2 = action.charAt(appLoc + 1);
                    if ( c2 != ' ' ) {
                        if ( !Character.isDigit(c2) ) {
                            failed = true;

                        } else {
                            appNo *= 10;
                            appNo += (c2 - '0');

                            if ( appLoc >= actionLength - 2 ) {
                                failed = true;
                            } else {
                                char c3 = action.charAt(appLoc + 2);
                                if ( c3 != ' ' ) {
                                    failed = true;
                                } else {
                                    failed = false;
                                }
                            }
                        }
                    } else {
                        failed = false;
                    }

                    if ( !failed ) {
                        return appNo;
                    }
                }
            }
        }

        fail("Bad app number [ " + action + " ]");
        return -1;
    }
}
