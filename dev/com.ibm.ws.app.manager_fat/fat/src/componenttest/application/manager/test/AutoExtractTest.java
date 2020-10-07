/*******************************************************************************
 * Copyright (c) 2018-2020 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import test.utils.TestUtils;

@RunWith(FATRunner.class)
public class AutoExtractTest extends AbstractAppManagerTest {

    // Our tests delete binaries while they are still defined in server.xml, so we expect this warning.
    private static final String COULD_NOT_FIND_APP_WARNING = "CWWKZ0014W";

    private final Class<?> c = AutoExtractTest.class;

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("autoExtractTestServer");

    @Rule
    public TestName testName = new TestName();

    @Override
    protected LibertyServer getServer() {
        return AutoExtractTest.server;
    }

    /**
     * Verify that a WAR installed into dropins with auto extract set to true will be extracted to apps/expanded
     *
     * Very similar to testAutoInstallWar except that we also check the expanded directory
     *
     * @throws Exception
     */
    @Test
    public void testAutoExtractWarDropins() throws Exception {
        try {
            final String method = testName.getMethodName();

            server.setServerConfigurationFile("/autoExpand/server.xml");
            server.startServer(method + ".log");

            // Pause to make sure the server is ready to install apps
            assertNotNull("The server never reported that it was ready to install web apps.",
                          server.waitForStringInLog("CWWKZ0058I:"));

            server.copyFileToLibertyServerRoot(PUBLISH_FILES, DROPINS_DIR, TEST_WAR_APPLICATION);

            // Pause for application to start properly and server to say it's listening on ports
            final int httpDefaultPort = server.getHttpDefaultPort();
            assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                          server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
            assertNotNull("The application testWarApplication did not appear to have started.",
                          server.waitForStringInLog("CWWKZ0001I.* testWarApplication"));
            assertNotNull("The server did not report that the app was being extracted",
                          server.waitForStringInLog("CWWKZ0133I.* testWarApplication"));

            URL url = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/testWarApplication/TestServlet");
            Log.info(c, method, "Calling test Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Test servlet\'",
                       line.contains("test servlet is running."));
            con.disconnect();

            server.setMarkToEndOfLog();

            // Sleep for one second to ensure that the updated WAR doesn't end up with the same timestamp
            Thread.sleep(1000);

            //replace original application with new version to test that it updates
            server.copyFileToLibertyServerRoot(PUBLISH_UPDATED, DROPINS_DIR, TEST_WAR_APPLICATION);

            //make sure the application is claiming to have been updated
            assertNotNull("The application testWarApplication did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* testWarApplication"));

            //get the message from the application to make sure it is the new appication contents
            Log.info(c, method, "Calling test Application with URL=" + url.toString());
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the \'updated test servlet\': " + line,
                       line.contains("this is an updated test servlet."));
            con.disconnect();

            server.setMarkToEndOfLog();
            // Replace application in apps/expanded with the original version

            TestUtils.unzip(new File(PUBLISH_FILES + "/testWarApplication.war"),
                            new File(server.getServerRoot() + "/" + EXPANDED_DIR + "/testWarApplication.war"));

            // Make sure the app updated
            assertNotNull("The application testWarApplication did not appear to have been stopped.",
                          server.waitForStringInLog("CWWKZ0009I.* testWarApplication"));
            assertNotNull("The application testWarApplication did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* testWarApplication"));

            //get the message from the application to make sure it is the new application contents
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            Log.info(c, method, line);
            assertTrue("The response did not contain the phrase \'test servlet is running.\'",
                       line.contains("test servlet is running."));
            con.disconnect();

            // remove file
            server.setMarkToEndOfLog();

            boolean deleted = deleteFile(server.getMachine(),
                                         server.getServerRoot() + "/" + DROPINS_DIR + "/testWarApplication.war");

            if (!deleted) {
                // Occasionally on Windows something else will hold onto a file while we are trying to delete it. If
                // that happens, just exit early.
                return;
            }
            // Wait for the server to confirm it stopped the app
            assertNotNull("The application testWarApplication did not appear to have been stopped after deletion.",
                          server.waitForStringInLog("CWWKZ0009I.* testWarApplication"));

            try {
                //check application is not installed
                con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, CONN_TIMEOUT);
                con.setInstanceFollowRedirects(false); // to avoid odd end of file issues
                br = HttpUtils.getConnectionStream(con);
                line = br.readLine();
                fail("was expecting exception, but failed to hit one! In fact we got text output as: " + line);
                con.disconnect();
            } catch (FileNotFoundException e) {
                //expected.
            }
        } finally {
            //if we failed to delete file before, try to delete it now.
            pathsToCleanup.add(server.getServerRoot() + "/" + DROPINS_DIR);
            server.removeAllInstalledAppsForValidation();
            server.stopServer(COULD_NOT_FIND_APP_WARNING);
        }

    }

    /**
     * Test that a war file application defined in server.xml will be extracted when autoExtractApps is set to true
     */
    @Test
    public void testAutoExtractWarDefined() throws Exception {
        try {
            final String method = testName.getMethodName();

            server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, TEST_WAR_APPLICATION);

            server.setServerConfigurationFile("/autoExpand/definedWarServer.xml");
            server.startServer(method + ".log");

            // Pause to make sure the server is ready to install apps
            assertNotNull("The server never reported that it was ready to install web apps.",
                          server.waitForStringInLog("CWWKZ0058I:"));

            //install file

            // Pause for application to start properly and server to say it's listening on ports
            final int httpDefaultPort = server.getHttpDefaultPort();
            assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                          server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
            assertNotNull("The application testWarApplication did not appear to have started.",
                          server.waitForStringInLog("CWWKZ0001I.* testWarApplication"));
            assertNotNull("The server did not report that the app was being extracted",
                          server.waitForStringInLog("CWWKZ0133I.* testWarApplication"));

            URL url = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/testWarApplication/TestServlet");
            Log.info(c, method, "Calling test Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Test servlet\'",
                       line.contains("test servlet is running."));
            con.disconnect();

            //replace original application with new version to test that it updates
            server.copyFileToLibertyServerRoot(PUBLISH_UPDATED, APPS_DIR, TEST_WAR_APPLICATION);

            //make sure the application is claiming to have been updated
            assertNotNull("The application testWarApplication did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* testWarApplication"));

            //get the message from the application to make sure it is the new appication contents
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the \'updated test servlet\': " + line,
                       line.contains("this is an updated test servlet."));
            con.disconnect();

            server.setMarkToEndOfLog();
            // Replace application in apps/expanded with the original version

            TestUtils.unzip(new File(PUBLISH_FILES + "/testWarApplication.war"),
                            new File(server.getServerRoot() + "/" + EXPANDED_DIR + "/testWarApplication.war"));

            // Make sure the app updated
            assertNotNull("The application testWarApplication did not appear to have been stopped.",
                          server.waitForStringInLog("CWWKZ0009I.* testWarApplication"));
            assertNotNull("The application testWarApplication did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* testWarApplication"));

            //get the message from the application to make sure it is the new application contents
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            Log.info(c, method, line);
            assertTrue("The response did not contain the phrase \'test servlet is running.\'",
                       line.contains("test servlet is running."));
            con.disconnect();

            // remove file
            boolean deleted = deleteFile(server.getMachine(),
                                         server.getServerRoot() + "/" + APPS_DIR + "/testWarApplication.war");

            if (!deleted) {
                // Occasionally on Windows something else will hold onto a file while we are trying to delete it. If
                // that happens, just exit early.
                return;
            }
            // Wait for the server to confirm it stopped the app
            assertNotNull("The application testWarApplication did not appear to have been stopped after deletion.",
                          server.waitForStringInLog("CWWKZ0009I.* testWarApplication"));

            try {
                //check application is not installed
                con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, CONN_TIMEOUT);
                con.setInstanceFollowRedirects(false); // to avoid odd end of file issues
                br = HttpUtils.getConnectionStream(con);
                line = br.readLine();
                fail("was expecting exception, but failed to hit one! In fact we got text output as: " + line);
                con.disconnect();
            } catch (FileNotFoundException e) {
                //expected.
            }
        } finally {
            //if we failed to delete file before, try to delete it now.
            pathsToCleanup.add(server.getServerRoot() + "/" + APPS_DIR);
            server.removeAllInstalledAppsForValidation();
            // Ignore warning that happens because we deleted the app with it still defined in server.xml
            server.stopServer(COULD_NOT_FIND_APP_WARNING);
        }

    }

    /**
     * Verify that an EAR installed with auto extract set to true will be extracted to apps/expanded
     *
     * @throws Exception
     */
    @Test
    public void testAutoExtractEarDropins() throws Exception {
        try {
            final String method = testName.getMethodName();

            server.setServerConfigurationFile("/autoExpand/server.xml");
            server.startServer(method + ".log");

            // Pause to make sure the server is ready to install apps
            assertNotNull("The server never reported that it was ready to install web apps.",
                          server.waitForStringInLog("CWWKZ0058I:"));

            //install file
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, DROPINS_DIR, APP_J2EE_EAR);

            // Pause for application to start properly and server to say it's listening on ports
            final int httpDefaultPort = server.getHttpDefaultPort();
            assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                          server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
            assertNotNull("The web application app-j2ee did not appear to have started.",
                          server.waitForStringInLog("CWWKZ0001I.* app-j2ee"));
            assertNotNull("The server did not report that the app was being extracted",
                          server.waitForStringInLog("CWWKZ0133I.* app-j2ee"));

            URL url = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/test-web/DummyServlet");
            URL url1 = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/test-web1/DummyServlet");
            Log.info(c, method, "Calling test Application with URL=" + url.toString());

            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the phrase \'For testing this servlet\'",
                       line.contains("For testing this servlet"));
            con.disconnect();

            //And again for the second war
            Log.info(c, method, "Calling test Application with URL=" + url1.toString());
            con = HttpUtils.getHttpConnection(url1, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the phrase \'For testing this servlet\'",
                       line.contains("For testing this servlet"));
            con.disconnect();

            // Add snoop.war to the extracted directory
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, EXPANDED_DIR + "/app-j2ee.ear", SNOOP_WAR);

            // wait for update
            assertNotNull("The web application app-j2ee did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* app-j2ee"));

            // copy application.xml that contains snoop.war and changes test-web1 to test-web2
            server.copyFileToLibertyServerRoot(EXPANDED_DIR + "/app-j2ee.ear/META-INF", "autoExpand/application.xml");
            // wait for update
            assertNotNull("The web application app-j2ee did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* app-j2ee"));

            // Call snoop and test-web2
            URL url2 = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/test-web2/DummyServlet");
            URL snoop = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/snoop");

            Log.info(c, method, "Calling test Application with URL=" + snoop.toString());
            con = HttpUtils.getHttpConnection(snoop, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the phrase \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();

            Log.info(c, method, "Calling test Application with URL=" + url2.toString());
            con = HttpUtils.getHttpConnection(url2, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the phrase \'For testing this servlet\'",
                       line.contains("For testing this servlet"));
            con.disconnect();

            //replace original application with new version to test that it updates
            server.setMarkToEndOfLog();

            server.copyFileToLibertyServerRoot(PUBLISH_UPDATED, DROPINS_DIR, APP_J2EE_EAR);

            //make sure the application is claiming to have been updated
            assertNotNull("The application app-j2ee did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* app-j2ee"));

            //get the message from the application to make sure it is the new appication contents
            URL url3 = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/test-web3/DummyServlet");
            con = HttpUtils.getHttpConnection(url3, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain \'Some output\'",
                       line.contains("Some output"));
            con.disconnect();

            // remove file
            server.setMarkToEndOfLog();
            boolean deleted = deleteFile(server.getMachine(),
                                         server.getServerRoot() + "/" + DROPINS_DIR + "/app-j2ee.ear");

            if (!deleted) {
                // Occasionally on Windows something else will hold onto a file while we are trying to delete it. If
                // that happens, just exit early.
                return;
            }
            // Wait for the server to confirm it stopped the app
            assertNotNull("The application app-j2ee did not appear to have been stopped after deletion.",
                          server.waitForStringInLog("CWWKZ0009I.* app-j2ee"));

            try {
                //check application is not installed
                con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, CONN_TIMEOUT);
                con.setInstanceFollowRedirects(false); // to avoid odd end of file issues
                br = HttpUtils.getConnectionStream(con);
                line = br.readLine();
                fail("was expecting exception, but failed to hit one! In fact we got text output as: " + line);
                con.disconnect();
            } catch (FileNotFoundException e) {
                //expected.
            }

        } finally {
            //if we failed to delete file before, try to delete it now.
            pathsToCleanup.add(server.getServerRoot() + "/" + DROPINS_DIR);
            server.removeAllInstalledAppsForValidation();
            server.stopServer(COULD_NOT_FIND_APP_WARNING);
        }

    }

    @Test
    public void testAutoExtractEarDefined() throws Exception {
        try {
            final String method = testName.getMethodName();

            //install file

            server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, APP_J2EE_EAR);

            server.setServerConfigurationFile("/autoExpand/definedEarServer.xml");
            server.startServer(method + ".log");

            // Pause to make sure the server is ready to install apps
            assertNotNull("The server never reported that it was ready to install web apps.",
                          server.waitForStringInLog("CWWKZ0058I:"));

            // Pause for application to start properly and server to say it's listening on ports
            final int httpDefaultPort = server.getHttpDefaultPort();
            assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                          server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
            assertNotNull("The web application app-j2ee did not appear to have started.",
                          server.waitForStringInLog("CWWKZ0001I.* app-j2ee"));
            assertNotNull("The server did not report that the app was being extracted",
                          server.waitForStringInLog("CWWKZ0133I.* app-j2ee"));

            URL url = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/test-web/DummyServlet");
            URL url1 = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/test-web1/DummyServlet");
            Log.info(c, method, "Calling test Application with URL=" + url.toString());

            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the phrase \'For testing this servlet\'",
                       line.contains("For testing this servlet"));
            con.disconnect();

            //And again for the second war
            Log.info(c, method, "Calling test Application with URL=" + url1.toString());
            con = HttpUtils.getHttpConnection(url1, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the phrase \'For testing this servlet\'",
                       line.contains("For testing this servlet"));
            con.disconnect();

            server.setMarkToEndOfLog();
            // Add snoop.war to the extracted directory
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, EXPANDED_DIR + "/app-j2ee.ear", SNOOP_WAR);

            // wait for update
            assertNotNull("The web application app-j2ee did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* app-j2ee"));

            // Make sure the files are not both copied within the monitor interval
            Thread.sleep(1000);

            server.setMarkToEndOfLog();
            // copy application.xml that contains snoop.war and changes test-web1 to test-web2
            server.copyFileToLibertyServerRoot(EXPANDED_DIR + "/app-j2ee.ear/META-INF", "autoExpand/application.xml");
            // wait for update
            assertNotNull("The web application app-j2ee did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* app-j2ee"));

            // Call snoop and test-web2
            URL url2 = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/test-web2/DummyServlet");
            URL snoop = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/snoop");

            Log.info(c, method, "Calling test Application with URL=" + snoop.toString());
            con = HttpUtils.getHttpConnection(snoop, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the phrase \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();

            Log.info(c, method, "Calling test Application with URL=" + url2.toString());
            con = HttpUtils.getHttpConnection(url2, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the phrase \'For testing this servlet\'",
                       line.contains("For testing this servlet"));
            con.disconnect();

            // Remove the app
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/autoExpand/server.xml");
            // remove file
            boolean deleted = deleteFile(server.getMachine(),
                                         server.getServerRoot() + "/" + APPS_DIR + "/app-j2ee.ear");

            if (!deleted) {
                // Occasionally on Windows something else will hold onto a file while we are trying to delete it. If
                // that happens, just exit early.
                return;
            }
            // Wait for the server to confirm it stopped the app
            assertNotNull("The application app-j2ee did not appear to have been stopped after deletion.",
                          server.waitForStringInLog("CWWKZ0009I.* app-j2ee"));

            try {
                //check application is not installed
                con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, CONN_TIMEOUT);
                con.setInstanceFollowRedirects(false); // to avoid odd end of file issues
                br = HttpUtils.getConnectionStream(con);
                line = br.readLine();
                fail("was expecting exception, but failed to hit one! In fact we got text output as: " + line);
                con.disconnect();
            } catch (FileNotFoundException e) {
                //expected.
            }

        } finally {
            //if we failed to delete file before, try to delete it now.
            pathsToCleanup.add(server.getServerRoot() + "/" + APPS_DIR);
            server.removeAllInstalledAppsForValidation();
            server.stopServer(COULD_NOT_FIND_APP_WARNING);
        }
    }

    /**
     * Test that a war file application defined in server.xml is not re-extracted on a server restart if the app didn't change.
     */
    @Test
    public void testNoAutoExtractOnRestartWar() throws Exception {
        try {
            final String method = testName.getMethodName();

            server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, TEST_WAR_APPLICATION);

            server.setServerConfigurationFile("/autoExpand/definedWarServer.xml");
            server.startServer(method + ".log");

            // Pause to make sure the server is ready to install apps
            assertNotNull("The server never reported that it was ready to install web apps.",
                          server.waitForStringInLog("CWWKZ0058I:"));

            //install file

            // Pause for application to start properly and server to say it's listening on ports
            final int httpDefaultPort = server.getHttpDefaultPort();
            assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                          server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
            assertNotNull("The application testWarApplication did not appear to have started.",
                          server.waitForStringInLog("CWWKZ0001I.* testWarApplication"));
            assertNotNull("The server did not report that the app was being extracted",
                          server.waitForStringInLog("CWWKZ0133I.* testWarApplication"));

            URL url = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/testWarApplication/TestServlet");
            Log.info(c, method, "Calling test Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Test servlet\'",
                       line.contains("test servlet is running."));
            con.disconnect();

            server.copyFileToLibertyServerRoot(server.pathToAutoFVTTestFiles, APPS_DIR + "/expanded/" + TEST_WAR_APPLICATION, "test.file");
            url = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/testWarApplication/test.file");
            Log.info(c, method, "Calling test Application with URL=" + url.toString());
            //check application is installed
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the \'This is a test update\' actual: " + line,
                       line.contains("This is a test update"));
            con.disconnect();

            server.setMarkToEndOfLog();

            server.stopServer();
            server.startServer(method + ".start2.log", false);

            assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                          server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
            assertNotNull("The application testWarApplication did not appear to have started.",
                          server.waitForStringInLog("CWWKZ0001I.* testWarApplication"));

            Log.info(c, method, "Calling test Application with URL=" + url.toString());
            //check application is installed
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the \'This is a test update\' actual: " + line,
                       line.contains("This is a test update"));
            con.disconnect();

        } finally {
            //if we failed to delete file before, try to delete it now.
            pathsToCleanup.add(server.getServerRoot() + "/" + APPS_DIR);
            server.removeAllInstalledAppsForValidation();
            // Ignore warning that happens because we deleted the app with it still defined in server.xml
            server.stopServer(COULD_NOT_FIND_APP_WARNING);
        }
    }

    @Test
    public void testNoAutoExtractOnRestartEar() throws Exception {
        try {
            final String method = testName.getMethodName();

            //install file

            server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, APP_J2EE_EAR);

            server.setServerConfigurationFile("/autoExpand/definedEarServer.xml");
            server.startServer(method + ".log");

            // Pause to make sure the server is ready to install apps
            assertNotNull("The server never reported that it was ready to install web apps.",
                          server.waitForStringInLog("CWWKZ0058I:"));

            // Pause for application to start properly and server to say it's listening on ports
            final int httpDefaultPort = server.getHttpDefaultPort();
            assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                          server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
            assertNotNull("The web application app-j2ee did not appear to have started.",
                          server.waitForStringInLog("CWWKZ0001I.* app-j2ee"));
            assertNotNull("The server did not report that the app was being extracted",
                          server.waitForStringInLog("CWWKZ0133I.* app-j2ee"));

            URL url = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/test-web/DummyServlet");
            Log.info(c, method, "Calling test Application with URL=" + url.toString());

            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the phrase \'For testing this servlet\'",
                       line.contains("For testing this servlet"));
            con.disconnect();

            server.copyFileToLibertyServerRoot(server.pathToAutoFVTTestFiles, APPS_DIR + "/expanded/" + APP_J2EE_EAR + "/test-web.war", "test.file");
            url = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/test-web/test.file");

            Log.info(c, method, "Calling test Application with URL=" + url.toString());
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the phrase \'This is a test update\', actual: " + line,
                       line.contains("This is a test update"));
            con.disconnect();

            server.setMarkToEndOfLog();

            // restart the server, but don't clear the workarea
            server.stopServer();
            server.startServer(method + ".start2.log", false);

            assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                          server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
            assertNotNull("The application testWarApplication did not appear to have started.",
                          server.waitForStringInLog("CWWKZ0001I.* app-j2ee"));

            Log.info(c, method, "Calling test Application with URL=" + url.toString());
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the phrase \'This is a test update\', actual: " + line,
                       line.contains("This is a test update"));
            con.disconnect();
        } finally {
            //if we failed to delete file before, try to delete it now.
            pathsToCleanup.add(server.getServerRoot() + "/" + APPS_DIR);
            server.removeAllInstalledAppsForValidation();
            server.stopServer(COULD_NOT_FIND_APP_WARNING);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.application.manager.test.AbstractAppManagerTest#getLogClass()
     */
    @Override
    protected Class<?> getLogClass() {
        return c;
    }

}
