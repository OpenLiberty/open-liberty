/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.application.manager.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.jmx.mbeans.ApplicationMBean;
import com.ibm.ws.fat.util.jmx.mbeans.ApplicationMBean.ApplicationState;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests that exercise the 'startAfter' attribute on applications
 */
@RunWith(FATRunner.class)
public class AppOrderTests extends AbstractAppManagerTest {

    private static final long LONG_TIMEOUT = 120000;

    private static final long SHORT_TIMEOUT = 10000;

    private final Class<?> c = AppOrderTests.class;

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("appOrderTestServer");

    @Rule
    public TestName testName = new TestName();

    @Override
    protected Class<?> getLogClass() {
        return c;
    }

    @Override
    protected LibertyServer getServer() {
        return AppOrderTests.server;
    }

    @Test
    public void testSimpleAppOrder() throws Exception {
        final String method = testName.getMethodName();

        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SLOW_APP);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SNOOP_WAR);

        server.setServerConfigurationFile("/appOrder/simple.xml");
        server.startServer(method + ".log");

        // Snoop should easily start within 10 seconds if the ordering isn't used.
        server.verifyStringNotInLogUsingMark("CWWKZ0001I.* snoop", SHORT_TIMEOUT);

        // The slow app should take around 40 seconds to start
        server.waitForStringInLogUsingMark("CWWKZ0001I.* slow", LONG_TIMEOUT);

        // Now that 'slow' is started, snoop should start too
        server.waitForStringInLog("CWWKZ0001I.* snoop");

    }

    @Test
    @Mode(TestMode.FULL)
    public void testAppOrderCycle() throws Exception {
        final String method = testName.getMethodName();

        allowedErrors = "CWWKZ0066E";

        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SLOW_APP);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SNOOP_WAR);

        server.setServerConfigurationFile("/appOrder/cycle.xml");
        server.startServer(method + ".log");

        // Wait for the cycle error
        server.waitForStringInLog("CWWKZ0066E.*");

        server.setMarkToEndOfLog();
        // Only change is to remove the cycle
        server.setServerConfigurationFile("/appOrder/simple.xml");

        // Snoop should easily start within 10 seconds if the ordering isn't used.
        server.verifyStringNotInLogUsingMark("CWWKZ0001I.* snoop", SHORT_TIMEOUT);

        // The slow app should take around 40 seconds to start
        server.waitForStringInLogUsingMark("CWWKZ0001I.* slow", LONG_TIMEOUT);

        // Now that 'slow' is started, snoop should start too
        server.waitForStringInLogUsingMark("CWWKZ0001I.* snoop");
    }

    // Tests that a restart of all applications result in startAfter relationships being enforced
    @Test
    public void testForceRestart() throws Exception {
        final String method = testName.getMethodName();

        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SLOW_APP);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SNOOP_WAR);

        server.setServerConfigurationFile("/appOrder/simple.xml");
        server.startServer(method + ".log");

        // Snoop should easily start within 10 seconds if the ordering isn't used.
        server.verifyStringNotInLogUsingMark("CWWKZ0001I.* snoop", SHORT_TIMEOUT);

        // The slow app should take around 40 seconds to start
        server.waitForStringInLogUsingMark("CWWKZ0001I.* slow", LONG_TIMEOUT);

        // Now that 'slow' is started, snoop should start too
        server.waitForStringInLog("CWWKZ0001I.* snoop");

        // Replace servlet-4.0 with servlet-3.1, causing a restart of all applications
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("/appOrder/forceRestart.xml");

        // Snoop should easily start within 10 seconds if the ordering isn't used.
        server.verifyStringNotInLogUsingMark("CWWKZ0001I.* snoop", SHORT_TIMEOUT);

        // The slow app should take around 40 seconds to start
        server.waitForStringInLogUsingMark("CWWKZ0001I.* slow", LONG_TIMEOUT);

        // Now that 'slow' is started, snoop should start too
        server.waitForStringInLogUsingMark("CWWKZ0001I.* snoop");
    }

    @Test
    public void testInvalidStartAfterError() throws Exception {
        final String method = testName.getMethodName();

        allowedErrors = "CWWKG0033W";

        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SLOW_APP);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SNOOP_WAR);

        server.setServerConfigurationFile("/appOrder/invalidStartAfter.xml");
        server.startServer(method + ".log");

        // Wait for invalid 'startAfter' attribute message
        server.waitForStringInLog("CWWKG0033W");

        // 'slow' should not start because its startAfter attribute is invalid
        server.verifyStringNotInLogUsingMark("CWWKZ0001I.* slow", SHORT_TIMEOUT);
        // Snoop should not start because it depends on 'slow'
        server.verifyStringNotInLogUsingMark("CWWKZ0001I.* snoop", SHORT_TIMEOUT);

    }

    @Test
    @Mode(TestMode.FULL)
    public void testComplexAppOrder() throws Exception {
        final String method = testName.getMethodName();

        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SLOW_APP);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SNOOP_WAR);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, APP_J2EE_EAR);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, TEST_WAR_APPLICATION);

        server.setServerConfigurationFile("/appOrder/complex.xml");
        server.startServer(method + ".log");

        // Snoop and app-j2ee should easily start within 10 seconds if the ordering isn't used.
        server.verifyStringNotInLogUsingMark("CWWKZ0001I.* snoop", SHORT_TIMEOUT);
        server.verifyStringNotInLogUsingMark("CWWKZ0001I.* j2ee", SHORT_TIMEOUT);

        // The slow app should take around 40 seconds to start
        server.waitForStringInLogUsingMark("CWWKZ0001I.* slow", LONG_TIMEOUT);

        // Now that 'slow' is started, snoop should start too
        server.waitForStringInLog("CWWKZ0001I.* snoop");
        // and j2ee
        server.waitForStringInLog("CWWKZ0001I.* j2ee");

    }

    @Test
    @Mode(TestMode.FULL)
    public void testIndividualAppUpdate() throws Exception {
        final String method = testName.getMethodName();

        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SLOW_APP);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SNOOP_WAR);
        server.renameLibertyServerRootFile(APPS_DIR + "/" + SNOOP_WAR, APPS_DIR + "/snooptwo.war");
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SNOOP_WAR);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, APP_J2EE_EAR);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, TEST_WAR_APPLICATION);

        server.setServerConfigurationFile("/appOrder/complex.xml");
        server.startServer(method + ".log");

        // Snoop and app-j2ee should easily start within 10 seconds if the ordering isn't used.
        server.verifyStringNotInLogUsingMark("CWWKZ0001I.* snoop", SHORT_TIMEOUT);
        server.verifyStringNotInLogUsingMark("CWWKZ0001I.* j2ee", SHORT_TIMEOUT);

        // The slow app should take around 40 seconds to start
        server.waitForStringInLogUsingMark("CWWKZ0001I.* slow", LONG_TIMEOUT);

        // Now that 'slow' is started, snoop should start too
        server.waitForStringInLog("CWWKZ0001I.* snoop");
        // and j2ee
        server.waitForStringInLog("CWWKZ0001I.* j2ee");

        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot(PUBLISH_UPDATED, APPS_DIR, APP_J2EE_EAR);
        //make sure app-j2ee claims to have been updated
        assertNotNull("The application j2ee did not appear to have been updated.",
                      server.waitForStringInLog("CWWKZ0003I.* j2ee"));

        // Make sure apps that weren't changed aren't affected (using wildcard regex to catch stop, restart, etc.)
        server.verifyStringNotInLogUsingMark(".* snoop", SHORT_TIMEOUT);
        server.verifyStringNotInLogUsingMark(".* testWar", SHORT_TIMEOUT);
        server.verifyStringNotInLogUsingMark(".* slow", SHORT_TIMEOUT);

        // Update another application
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot(PUBLISH_UPDATED, APPS_DIR, TEST_WAR_APPLICATION);
        //make sure testWar claims to have been updated
        assertNotNull("The application testWar did not appear to have been updated.",
                      server.waitForStringInLog("CWWKZ0003I.* testWar"));

        // Make sure apps that weren't changed aren't affected (using wildcard regex to catch stop, restart, etc.)
        server.verifyStringNotInLogUsingMark(".* snoop", SHORT_TIMEOUT);
        server.verifyStringNotInLogUsingMark(".* j2ee", SHORT_TIMEOUT);
        server.verifyStringNotInLogUsingMark(".* slow", SHORT_TIMEOUT);

        // Add a new application that depends on all others
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("/appOrder/complexUpdate.xml");
        server.waitForStringInLogUsingMark("CWWKZ0001I.* newapp");
        // Make sure apps that weren't changed aren't affected (using wildcard regex to catch stop, restart, etc.)
        server.verifyStringNotInLogUsingMark(".* snoop", SHORT_TIMEOUT);
        server.verifyStringNotInLogUsingMark(".* j2ee", SHORT_TIMEOUT);
        server.verifyStringNotInLogUsingMark(".* slow", SHORT_TIMEOUT);
        server.verifyStringNotInLogUsingMark(".* testWar", SHORT_TIMEOUT);

    }

    @Test
    public void testMBeanStart() throws Exception {
        final String method = testName.getMethodName();

        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SLOW_APP);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SNOOP_WAR);
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, APP_J2EE_EAR);

        server.setServerConfigurationFile("/appOrder/mbean.xml");
        server.startServer(method + ".log");

        ApplicationMBean slow = getApplicationMBean("slow");
        ApplicationMBean snoop = getApplicationMBean("snoop");
        ApplicationMBean j2ee = getApplicationMBean("j2ee");

        assertTrue("The application 'slow' should be stopped", slow.getState() == ApplicationState.STOPPED);
        assertTrue("The application 'snoop' should be stopped", snoop.getState() == ApplicationState.STOPPED);
        assertTrue("The application 'j2ee' should be stopped", j2ee.getState() == ApplicationState.STOPPED);

        j2ee.start();

        // J2EE should start despite being blocked by snoop
        server.waitForStringInLog("CWWKZ0001I.* j2ee");

        slow.start();

        // Slow should start
        server.waitForStringInLog("CWWKZ0001I.* slow", LONG_TIMEOUT);

        // Snoop should start because it was blocked on slow
        server.waitForStringInLog("CWWKZ0001I.* snoop");

    }

    /**
     * An older test (pre-addition of 'startAfter') that just reverses the order of applications in server.xml and makes
     * sure that it doesn't make a difference.
     */
    @Test
    public void testAppOrder() throws Exception {
        final String method = testName.getMethodName();
        try {
            //load a new server xml
            server.setServerConfigurationFile("/appOrder/serverFooBar.xml");
            //copy file to correct location
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot() + "/apps", "foo.war",
                                                   PUBLISH_FILES + "/snoop.war");
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot() + "/apps", "bar.war",
                                                   PUBLISH_FILES + "/snoop.war");
            server.startServer(method + ".log");
            // Wait for the application to be installed before proceeding
            assertNotNull("The foo application never came up", server.waitForStringInLog("CWWKZ0001I.* foo"));
            assertNotNull("The bar application never came up", server.waitForStringInLog("CWWKZ0001I.* bar"));

            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/foo");
            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();

            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/bar");
            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            //check application is installed
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();

            // Change the server.xml file to scan a different directory
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/appOrder/serverBarFoo.xml");
            // make sure configuration was reloaded
            assertNotNull("The server config was not reloaded", server.waitForStringInLog("CWWKG0017I"));
            // Wait for the application to be installed before proceeding
            assertNotNull("The foo application never came up", server.waitForStringInLog("CWWKZ000[13]I.* foo"));
            assertNotNull("The bar application never came up", server.waitForStringInLog("CWWKZ000[13]I.* bar"));

            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/foo");
            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            //check application is installed
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();

            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/bar");
            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            //check application is installed
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();

        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/apps");
        }
    }

}
