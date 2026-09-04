/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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

package io.openliberty.classloading.base.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Base class for tests for updating app class binaries.
 * Note that subclasses are responsible for initializing/configuring/starting the server before using.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public abstract class AbstractUpdatingAppClassesFatTest {
    private final static String CLASS_NAME = AbstractUpdatingAppClassesFatTest.class.getName();
    private final static Logger LOG = Logger.getLogger(CLASS_NAME);

    protected static LibertyServer looseConfigServer = null;
    protected static LibertyServer expandedAppServer = null;

    private static final long SLEEP_TIME_BETWEEN_FILE_UPDATES = 1200;

    @Rule
    public TestName testName = new TestName();

    @AfterClass
    public static void afterClass() throws Exception {
        try {
            if (looseConfigServer != null) {
                looseConfigServer.stopServer();
            }
        }finally {
            //ensure both servers are stopped if possible
            if (expandedAppServer != null) {
                expandedAppServer.stopServer();
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        setUp(getServer());
    }

    @After
    public void tearDown() throws Exception {
        tearDown(getServer());
    }

    private LibertyServer getServer() {
        if (testName.getMethodName().endsWith("loose")) {
            return looseConfigServer;
        }
        return expandedAppServer;
    }

    @Ignore // disabling incremental publishing until annotation updates work correctly
    @Test
    public void testMinorUpdateServlet_loose() throws Throwable {
        testMinorUpdateServlet(looseConfigServer, "loose/updateableAppWeb/WEB-INF/classes/com/ibm/test/updateable/web");
    }

    @Ignore // disabling incremental publishing until annotation updates work correctly
    @Test
    public void testMinorUpdateServlet_expandedApp() throws Throwable {
        testMinorUpdateServlet(expandedAppServer, "apps/updateableApp.ear/updateableAppWeb.war/WEB-INF/classes/com/ibm/test/updateable/web");
    }

    private void testMinorUpdateServlet(LibertyServer server, final String fileDest) throws Throwable {
        final String CLASS_NAME = this.getClass().getName();
        final String METHOD_NAME = "testMinorUpdateServlet";
        try {
            LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "starting test");
            URL url = createURL(server, "/updateableAppWeb/MyServlet", "test=true");
            String output = HttpUtils.getHttpResponseAsString(url);
            assertTrue("Expected output from pre-updated servlet was not found", output.contains("Hello from MyServlet"));

            // Sleep for 1200 milliseconds to make sure that the updated file gets a new time stamp that is at least 1 second later.
            // This is necessary in order for the shared class cache to detect that the class has changed.  If the timestamp
            // is the same (with second precision), the shared class cache will think that that old class file is still there.
            Thread.sleep(SLEEP_TIME_BETWEEN_FILE_UPDATES);
            server.setMarkToEndOfLog();
            updateFile(server, fileDest, "updateableAppWeb1.1/MyServlet.class");
            assertNotNull("Expected message indicating app has been updated without restart did not occur", server.waitForStringInLogUsingMark("CWWKZ0062I"));

            output = HttpUtils.getHttpResponseAsString(url);
            assertTrue("Expected output from updated servlet was not found", output.contains("Hello from an updated version of MyServlet"));
            LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "done - success");
        } finally {
            // Sleep for 1200 milliseconds to make sure that the updated file gets a new time stamp that is at least 1 second later.
            // This is necessary in order for the shared class cache to detect that the class has changed.  If the timestamp
            // is the same (with second precision), the shared class cache will think that that old class file is still there.
            Thread.sleep(SLEEP_TIME_BETWEEN_FILE_UPDATES);
            server.setMarkToEndOfLog();
            updateFile(server, fileDest, "updateableAppWeb1/MyServlet.class");
            server.waitForStringInLogUsingMark("CWWKZ0062I");
        }
    }

    @Ignore // disabling incremental publishing until annotation updates work correctly
    @Test
    public void testMinorUpdateEJB_loose() throws Throwable {
        testMinorUpdateEJB(looseConfigServer, "loose/updateableAppEJB/com/ibm/test/updateable/ejb");
    }

    @Ignore // disabling incremental publishing until annotation updates work correctly
    @Test
    public void testMinorUpdateEJB_expandedApp() throws Throwable {
        testMinorUpdateEJB(expandedAppServer, "apps/updateableApp.ear/updateableAppEJB.jar/com/ibm/test/updateable/ejb");
    }

    private void testMinorUpdateEJB(LibertyServer server, String fileDest) throws Throwable {
        final String CLASS_NAME = this.getClass().getName();
        final String METHOD_NAME = "testMinorUpdateEJB";
        try {
            LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "starting test");
            URL url = createURL(server, "/updateableAppWeb/MyServlet", "test=true");
            String output = HttpUtils.getHttpResponseAsString(url);
            assertTrue("Expected output from pre-updated EJB was not found", output.contains("Hello from MySingletonBean"));

            // Sleep for 1200 milliseconds to make sure that the updated file gets a new time stamp that is at least 1 second later.
            // This is necessary in order for the shared class cache to detect that the class has changed.  If the timestamp
            // is the same (with second precision), the shared class cache will think that that old class file is still there.
            Thread.sleep(SLEEP_TIME_BETWEEN_FILE_UPDATES);
            server.setMarkToEndOfLog();
            updateFile(server, fileDest, "updateableAppEJB1.1/MySingletonBean.class");
            assertNotNull("Expected message indicating app has been updated without restart did not occur", server.waitForStringInLogUsingMark("CWWKZ0062I"));

            output = HttpUtils.getHttpResponseAsString(url);
            assertTrue("Expected output from updated EJB was not found", output.contains("Hello from an updated version of MySingletonBean"));
            LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "done - success");
        } finally {
            // Sleep for 1200 milliseconds to make sure that the updated file gets a new time stamp that is at least 1 second later.
            // This is necessary in order for the shared class cache to detect that the class has changed.  If the timestamp
            // is the same (with second precision), the shared class cache will think that that old class file is still there.
            Thread.sleep(SLEEP_TIME_BETWEEN_FILE_UPDATES);
            server.setMarkToEndOfLog();
            updateFile(server, fileDest, "updateableAppEJB1/MySingletonBean.class");
            server.waitForStringInLogUsingMark("CWWKZ0062I");
        }
    }

    @Test
    public void testMajorUpdateServlet_loose() throws Throwable {
        testMajorUpdateServlet(looseConfigServer, "loose/updateableAppWeb/WEB-INF/classes/com/ibm/test/updateable/web");
    }

    @Test
    public void testMajorUpdateServlet_expandedApp() throws Throwable {
        testMajorUpdateServlet(expandedAppServer, "apps/updateableApp.ear/updateableAppWeb.war/WEB-INF/classes/com/ibm/test/updateable/web");
    }

    private void testMajorUpdateServlet(LibertyServer server, String fileDest) throws Throwable {
        final String CLASS_NAME = this.getClass().getName();
        final String METHOD_NAME = "testMajorUpdateServlet";
        if (!server.isJava2SecurityEnabled()) {
            try {
                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "starting test");
                URL url = createURL(server, "/updateableAppWeb/MyServlet", "test=true");
                String output = HttpUtils.getHttpResponseAsString(url);
                assertTrue("Expected output from pre-updated servlet was not found", output.contains("Hello from MyServlet"));

                // Sleep for 1200 milliseconds to make sure that the updated file gets a new time stamp that is at least 1 second later.
                // This is necessary in order for the shared class cache to detect that the class has changed.  If the timestamp
                // is the same (with second precision), the shared class cache will think that that old class file is still there.
                Thread.sleep(SLEEP_TIME_BETWEEN_FILE_UPDATES);
                server.setMarkToEndOfLog();
                updateFile(server, fileDest, "updateableAppWeb2/MyServlet.class");
                assertNotNull("Expected message indicating app has been stopped did not occur", server.waitForStringInLogUsingMark("CWWKZ0009I"));
                assertNotNull("Expected message indicating app has been updated/restarted did not occur", server.waitForStringInLogUsingMark("CWWKZ0003I"));

                output = HttpUtils.getHttpResponseAsString(url);
                assertTrue("Expected output from updated servlet was not found. Instead found: " + output, output.contains("Hello from new method in MyServlet"));
                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "done - success");
            } finally {
                // Sleep for 1200 milliseconds to make sure that the updated file gets a new time stamp that is at least 1 second later.
                // This is necessary in order for the shared class cache to detect that the class has changed.  If the timestamp
                // is the same (with second precision), the shared class cache will think that that old class file is still there.
                Thread.sleep(SLEEP_TIME_BETWEEN_FILE_UPDATES);
                server.setMarkToEndOfLog();
                updateFile(server, fileDest, "updateableAppWeb1/MyServlet.class");
                server.waitForStringInLogUsingMark("CWWKZ0003I");
            }

        }
    }

    @Test
    public void testMajorUpdateEJB_loose() throws Throwable {
        testMajorUpdateEJB(looseConfigServer, "loose/updateableAppEJB/com/ibm/test/updateable/ejb");
    }

    @Test
    public void testMajorUpdateEJB_expandedApp() throws Throwable {
        testMajorUpdateEJB(expandedAppServer, "apps/updateableApp.ear/updateableAppEJB.jar/com/ibm/test/updateable/ejb");
    }

    private void testMajorUpdateEJB(LibertyServer server, String fileDest) throws Throwable {
        final String CLASS_NAME = this.getClass().getName();
        final String METHOD_NAME = "testMajorUpdateEJB";
        if (!server.isJava2SecurityEnabled()) {
            try {
                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "starting test");
                URL url = createURL(server, "/updateableAppWeb/MyServlet", "test=true");
                String output = HttpUtils.getHttpResponseAsString(url);
                assertTrue("Expected output from pre-updated EJB was not found", output.contains("Hello from MySingletonBean"));

                // Sleep for 1200 milliseconds to make sure that the updated file gets a new time stamp that is at least 1 second later.
                // This is necessary in order for the shared class cache to detect that the class has changed.  If the timestamp
                // is the same (with second precision), the shared class cache will think that that old class file is still there.
                Thread.sleep(SLEEP_TIME_BETWEEN_FILE_UPDATES);
                server.setMarkToEndOfLog();
                updateFile(server, fileDest, "updateableAppEJB2/MySingletonBean.class");
                assertNotNull("Expected message indicating app has been stopped did not occur", server.waitForStringInLogUsingMark("CWWKZ0009I"));
                assertNotNull("Expected message indicating app has been updated/restarted did not occur", server.waitForStringInLogUsingMark("CWWKZ0003I"));

                output = HttpUtils.getHttpResponseAsString(url);
                assertTrue("Expected output from updated EJB was not found. Instead found: " + output, output.contains("Hello from a new method in MySingletonBean"));
                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "done - success");
            } finally {
                // Sleep for 1200 milliseconds to make sure that the updated file gets a new time stamp that is at least 1 second later.
                // This is necessary in order for the shared class cache to detect that the class has changed.  If the timestamp
                // is the same (with second precision), the shared class cache will think that that old class file is still there.
                Thread.sleep(SLEEP_TIME_BETWEEN_FILE_UPDATES);
                server.setMarkToEndOfLog();
                updateFile(server, fileDest, "updateableAppEJB1/MySingletonBean.class");
                server.waitForStringInLogUsingMark("CWWKZ0003I");
            }

        }
   }

    @Test
    public void testUpdatePostConstructAnnotationInEJB_loose() throws Throwable {
        testUpdatePostConstructAnnotationInEJB(looseConfigServer, "loose/updateableAppEJB/com/ibm/test/updateable/ejb");
    }

    @Test
    public void testUpdatePostConstructAnnotationInEJB_expandedApp() throws Throwable {
        testUpdatePostConstructAnnotationInEJB(expandedAppServer, "apps/updateableApp.ear/updateableAppEJB.jar/com/ibm/test/updateable/ejb");
    }

    private void testUpdatePostConstructAnnotationInEJB(LibertyServer server, String fileDest) throws Throwable {
        final String CLASS_NAME = this.getClass().getName();
        final String METHOD_NAME = "testUpdatePostConstructAnnotationInEJB";
        if (!server.isJava2SecurityEnabled()) {
            try {
                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "starting test");
                URL url = createURL(server, "/updateableAppWeb/MyServlet", "test=true");
                String output = HttpUtils.getHttpResponseAsString(url);
                assertTrue("Expected output from pre-updated EJB was not found", output.contains("Hello from MySingletonBean - initRun? true"));

                // Sleep for 1200 milliseconds to make sure that the updated file gets a new time stamp that is at least 1 second later.
                // This is necessary in order for the shared class cache to detect that the class has changed.  If the timestamp
                // is the same (with second precision), the shared class cache will think that that old class file is still there.
                Thread.sleep(SLEEP_TIME_BETWEEN_FILE_UPDATES);
                server.setMarkToEndOfLog();
                updateFile(server, fileDest, "updateableAppEJB-ann-chngd/MySingletonBean.class");
                assertNotNull("Expected message indicating app has been stopped did not occur", server.waitForStringInLogUsingMark("CWWKZ0009I"));
                assertNotNull("Expected message indicating app has been updated/restarted did not occur", server.waitForStringInLogUsingMark("CWWKZ0003I"));

                output = HttpUtils.getHttpResponseAsString(url);
                assertTrue("Expected output from updated EJB was not found", output.contains("Hello from MySingletonBean - initRun? false"));
                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "done - success");
            } finally {
                // Sleep for 1200 milliseconds to make sure that the updated file gets a new time stamp that is at least 1 second later.
                // This is necessary in order for the shared class cache to detect that the class has changed.  If the timestamp
                // is the same (with second precision), the shared class cache will think that that old class file is still there.
                Thread.sleep(SLEEP_TIME_BETWEEN_FILE_UPDATES);
                server.setMarkToEndOfLog();
                updateFile(server, fileDest, "updateableAppEJB1/MySingletonBean.class");
                server.waitForStringInLogUsingMark("CWWKZ0003I");
            }
        }

    }
//    @Test
//    public void testMinorUpdateLib() throws Throwable {
//        LOG.logp(Level.INFO, CLASS_NAME, "testMinorUpdateLib", "starting test");
//        URL url = createURL(server, "/updateableAppWeb/MyServlet", "test=true");
//        String output = test(url);
//        assertTrue("Expected output from pre-updated library was not found", output.contains("Hello from someUtilMethod()"));
//
//        server.setMarkToEndOfLog();
//        //server.copyFileToLibertyServerRoot("loose/updateableAppLib/com/ibm/test/updateable/util", "updateableAppLib1.1/Util.class");
//        updateFile(server, "loose/updateableAppLib/com/ibm/test/updateable/util", "updateableAppLib1.1/Util.class");
//        assertNotNull("Expected message indicating app has been updated without restart did not occur", server.waitForStringInLogUsingMark("CWWKZ0062I"));
//
//        output = test(url);
//        assertTrue("Expected output from updated library was not found", output.contains("Hello from an updated version of someUtilMethod()"));
//        LOG.logp(Level.INFO, CLASS_NAME, "testMinorUpdateLib", "done - success");
//    }

    @Test
    public void testJspChangeDoesNotForceRestart_loose() throws Exception {
        testJspChangeDoesNotForceRestart(looseConfigServer, "loose/updateableAppWeb");
    }

    @Test
    public void testJspChangeDoesNotForceRestart_expandedApp() throws Exception {
        testJspChangeDoesNotForceRestart(expandedAppServer, "apps/updateableApp.ear/updateableAppWeb.war");
    }

    private void testJspChangeDoesNotForceRestart(LibertyServer server, String fileDest) throws Exception {
        final String CLASS_NAME = this.getClass().getName();
        final String METHOD_NAME = "testJspChangeDoesNotForceRestart";
        final RemoteFile traceLog = server.getMostRecentTraceFile();
        if (!server.isJava2SecurityEnabled()) {
            try {
                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "starting test");
                URL url = createURL(server, "/updateableAppWeb/updateable.jsp", "greeting=Hello");
                String output = HttpUtils.getHttpResponseAsString(url);
                assertTrue("Expected output from pre-updated JSP was not found. Instead found: " + output, output.contains("Hello Joe"));

                server.setMarkToEndOfLog(traceLog, server.getDefaultLogFile());
                updateFile(server, fileDest, "updateableAppWeb2/updateable.jsp");
                Thread.sleep(5000);
                //assertNotNull("Expected message indicating app has been updated without restart did not occur", server.waitForStringInLogUsingMark("CWWKZ0062I", traceLog));

                // check for the updated output
                output = HttpUtils.getHttpResponseAsString(url);
                assertTrue("Expected output from updated JSP was not found", output.contains("Hello Steve"));

                // check that the app did not restart in between
                assertEquals("Unexpected message indicating app has been stopped found", 0, server.findStringsInLogsAndTraceUsingMark("CWWKZ0009I").size());
                assertEquals("Unexpected message indicating app has been updated/restarted found", 0, server.findStringsInLogsAndTraceUsingMark("CWWKZ0003I").size());

                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "done - success");
            } finally {
                server.setMarkToEndOfLog(traceLog, server.getDefaultLogFile());
                updateFile(server, fileDest, "updateableAppWeb1/updateable.jsp");
                //server.waitForStringInLogUsingMark("CWWKZ0062I", traceLog);
            }

        }
    }

    /**
     * The user can explicitly set file extensions for files that, if changed, do not force the app to
     * restart.  See <code>com.ibm.ws.app.manager.internal.monitor.ApplicationMonitor$ApplicationListener</code>
     * for more details.
     */
    @Test
    public void testChangeToFileMarkedAsMinorDoesNotForceRestart_loose() throws Exception {
        testChangeToFileMarkedAsMinorDoesNotForceRestart(looseConfigServer, "loose/updateableAppWeb/WEB-INF");
    }

    @Test
    public void testChangeToFileMarkedAsMinorDoesNotForceRestart_expandedApp() throws Exception {
        testChangeToFileMarkedAsMinorDoesNotForceRestart(expandedAppServer, "apps/updateableApp.ear/updateableAppWeb.war/WEB-INF");
    }

    private void testChangeToFileMarkedAsMinorDoesNotForceRestart(LibertyServer server, String fileDest) throws Exception {
        final String CLASS_NAME = this.getClass().getName();
        final String METHOD_NAME = "testChangeToFileMarkedAsMinorDoesNotForceRestart";
        final RemoteFile traceLog = server.getMostRecentTraceFile();
        if (!server.isJava2SecurityEnabled()) {
            try {
                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "starting test");
                RemoteFile minorFile = server.getFileFromLibertyServerRoot(fileDest + "/minor.xyz");

                String output = "";
                InputStream is = null;
                try {
                    is = minorFile.openForReading();
                    output = HttpUtils.read( is );
                } finally {
                    if ( is != null)
                        is.close();
                }

                assertTrue("Expected content from pre-updated file was not found", output.contains("Original"));

                server.setMarkToEndOfLog(traceLog, server.getDefaultLogFile());
                updateFile(server, fileDest, "updateableAppWeb2/minor.xyz");
                Thread.sleep(5000);
                //assertNotNull("Expected message indicating app has been updated without restart did not occur", server.waitForStringInLogUsingMark("CWWKZ0062I", traceLog));

                // check for the updated output
                minorFile = server.getFileFromLibertyServerRoot(fileDest + "/minor.xyz");
                is = null;
                output = "";
                try {
                    is = minorFile.openForReading();
                    output = HttpUtils.read( is );
                } finally {
                    if ( is != null)
                        is.close();
                }

                assertTrue("Expected content from updated minor.xyz file was not found", output.contains("Updated"));

                // check that the app did not restart in between
                assertEquals("Unexpected message indicating app has been stopped found", 0, server.findStringsInLogsAndTraceUsingMark("CWWKZ0009I").size());
                assertEquals("Unexpected message indicating app has been updated/restarted found", 0, server.findStringsInLogsAndTraceUsingMark("CWWKZ0003I").size());

                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "done - success");
            } finally {
                server.setMarkToEndOfLog(traceLog, server.getDefaultLogFile());
                updateFile(server, fileDest, "updateableAppWeb1/minor.xyz");
            }
        }
    }


    /**
     * If a user changes a file in the WEB-INF directory that is not marked as a minor file
     * using the minor file extension system property then the app should restart.
     */
    @Test
    public void testChangeToFileNotMarkedAsMinorDoesForceRestart_loose() throws Exception {
        testChangeToFileNotMarkedAsMinorDoesForceRestart(looseConfigServer, "loose/updateableAppWeb/WEB-INF");
    }

    @Test
    public void testChangeToFileNotMarkedAsMinorDoesForceRestart_expandedApp() throws Exception {
        testChangeToFileNotMarkedAsMinorDoesForceRestart(expandedAppServer, "apps/updateableApp.ear/updateableAppWeb.war/WEB-INF");
    }

    private void testChangeToFileNotMarkedAsMinorDoesForceRestart(LibertyServer server, String fileDest) throws Exception {
        final String CLASS_NAME = this.getClass().getName();
        final String METHOD_NAME = "testChangeToFileNotMarkedAsMinorDoesForceRestart";
        final RemoteFile traceLog = server.getMostRecentTraceFile();
        if (!server.isJava2SecurityEnabled()) {
            try {
                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "starting test");
                RemoteFile majorFile = server.getFileFromLibertyServerRoot(fileDest + "/major.txt");
                String output = "";
                InputStream is = null;
                try {
                    is = majorFile.openForReading();
                    output = HttpUtils.read( is );
                } finally {
                    if ( is != null)
                        is.close();
                }


                assertTrue("Expected content from pre-updated file was not found", output.contains("Original"));

                server.setMarkToEndOfLog(traceLog, server.getDefaultLogFile());
                updateFile(server, fileDest, "updateableAppWeb2/major.txt");
                assertNotNull("Expected message indicating app has been stopped did not occur", server.waitForStringInLogUsingMark("CWWKZ0009I"));
                assertNotNull("Expected message indicating app has been updated/restarted did not occur", server.waitForStringInLogUsingMark("CWWKZ0003I"));

                // check for the updated output
                majorFile = server.getFileFromLibertyServerRoot(fileDest + "/major.txt");
                is = null;
                output = "";
                try {
                    is = majorFile.openForReading();
                    output = HttpUtils.read( is );
                } finally {
                    if ( is != null)
                        is.close();
                }

                assertTrue("Expected content from updated major.txt file was not found", output.contains("Updated"));

                LOG.logp(Level.INFO, CLASS_NAME, METHOD_NAME, "done - success");
            } finally {
                server.setMarkToEndOfLog(traceLog, server.getDefaultLogFile());
                updateFile(server, fileDest, "updateableAppWeb1/major.txt");
                server.waitForStringInLogUsingMark("CWWKZ0003I");
            }
        }
    }

    /**
     * Static helper method to create a URL.
     */
    private URL createURL(LibertyServer server, String uri, String queryString) throws MalformedURLException {
        int port = testName.getMethodName().endsWith("loose") ? server.getHttpDefaultPort() : server.getHttpSecondaryPort();
        return new URL("http://" + server.getHostname() + ":" + port + uri + "?" + queryString);
    }

    protected abstract void updateFile(LibertyServer server, String dest, String src) throws Exception;

    protected void setUp(LibertyServer server) throws Exception {

    }

    protected void tearDown(LibertyServer server) throws Exception {

    }
}
