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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import test.utils.TestUtils;

@RunWith(FATRunner.class)
public class FATTest extends AbstractAppManagerTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("appManagerTestServer");
    private final Class<?> c = FATTest.class;

    @Rule
    public TestName testName = new TestName();

    @Override
    protected Class<?> getLogClass() {
        return c;
    }

    /**
     * This test tests the application manager's ability to start an application in
     * the server.config.dir/apps folder by configuring the server xml
     *
     * It also puts an invalid file in the liberty.install.dir/usr/shared/apps to make
     * sure that the server.config.dir location is searched first.
     *
     * It tests that when removing the application before removing the reference to it in the
     * server.xml that we get an error message and the application is unavailable, we then
     * test that it can be re-added and will load correctly.
     *
     * The test also ensures that if we install a zipped war application and define it in the
     * server.xml that if we replace it with an extracted war that we detect it and restart it.
     *
     *
     * It then removes the applications at the end of the test.
     */
    @Test
    public void testStartStopFromXml() throws Exception {
        final String method = testName.getMethodName();
        try {
            //load a new server xml
            server.setServerConfigurationFile("/appsConfigured/server.xml");
            //copy file to correct location
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "apps",
                                               SNOOP_WAR);
            //copy invalid file to the second location we should check - it should not pick this file up!
            server.copyFileToLibertyInstallRoot("usr/shared/apps",
                                                "invalidSnoopWar/snoop.war");
            server.startServer(method + ".log");
            // Wait for the application to be installed before proceeding
            assertNotNull("The snoop application never came up", server.waitForStringInLog("CWWKZ0001I.* snoop"));

            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();

            //application is working, so now remove it from the install location (we should get an error message)
            boolean deleted = deleteFile(server.getMachine(),
                                         server.getInstallRoot() + "/usr/shared/apps/snoop.war");

            //application is working, so now remove it from the install location (we should get an error message)
            deleted &= deleteFile(server.getMachine(), server.getServerRoot() + "/apps/snoop.war");

            if (!deleted) {
                // If we can't delete files the test is worthless. This tends to happen on Windows randomly.
                Log.info(c, method, "WARNING: Exiting from test early because files could not be deleted.");
                return;
            }

            assertNotNull("The snoop application was not stopped when removed",
                          server.waitForStringInLog("CWWKZ0059E.* snoop"));

            assertNotNull("The snoop application was not removed from the web container when stopped",
                          server.waitForStringInLog("CWWKT0017I.*snoop.*"));

            try {
                //check that the application won't load in the web container once stopped
                Log.info(c, method, "Calling Snoop Application with URL=" + url.toString() + ", expecting no response.");
                con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, CONN_TIMEOUT);
                br = HttpUtils.getConnectionStream(con);
                line = br.readLine();
                assertFalse("The response did contain the \'Snoop Servlet\' when it shouldn't",
                            line.contains("Snoop Servlet"));
                con.disconnect();

            } catch (java.io.FileNotFoundException ex) {
                //expected if the web container has unloaded the page already (it could be blank or not there)
            }

            //re-add the application to make sure it resumes working
            server.setMarkToEndOfLog();
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "tmp",
                                               SNOOP_WAR);

            //unzip the application into a temp location so that we can easily modify the application in a minute
            TestUtils.unzip(new File(server.getServerRoot() + "/tmp/snoop.war"),
                            new File(server.getServerRoot() + "/tmp/unzip/snoop.war"));

            // Copy the expanded snoop.war to "apps"
            server.renameLibertyServerRootFile("tmp/unzip/snoop.war", "apps/snoop.war");

            //make sure the started message has been output twice by the server (once earlier, once now).
            assertNotNull("The snoop application never resumed running after being stopped",
                          server.waitForStringInLog("CWWKZ0003I.* snoop"));

            //add a file to the extracted snoop application
            server.setMarkToEndOfLog();
            server.copyFileToLibertyServerRoot("/apps/snoop.war/",
                                               "updatedApplications/blankFile.txt");

            //make sure it loads into the web front end correctly again
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/apps");
            pathsToCleanup.add(server.getServerRoot() + "/tmp");
            pathsToCleanup.add(server.getInstallRoot() + "/usr/shared/apps/snoop.war");
            server.stopServer("CWWKZ0059E", "CWWKZ0014W");
        }
    }

    @Test
    @Mode(TestMode.FULL)
    public void testStartStopFromXmlFull() throws Exception {
        final String method = testName.getMethodName();
        try {
            //load a new server xml
            server.setServerConfigurationFile("/appsConfigured/server.xml");
            //copy file to correct location
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "apps",
                                               SNOOP_WAR);
            //copy invalid file to the second location we should check - it should not pick this file up!
            server.copyFileToLibertyInstallRoot("usr/shared/apps",
                                                "invalidSnoopWar/snoop.war");
            server.startServer(method + ".log");
            // Wait for the application to be installed before proceeding
            assertNotNull("The snoop application never came up", server.waitForStringInLog("CWWKZ0001I.* snoop"));

            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();

            //application is working, so now remove it from the install location (we should get an error message)
            boolean deleted = deleteFile(server.getMachine(),
                                         server.getInstallRoot() + "/usr/shared/apps/snoop.war");

            //application is working, so now remove it from the install location (we should get an error message)
            deleted &= deleteFile(server.getMachine(), server.getServerRoot() + "/apps/snoop.war");

            if (!deleted) {
                // If we can't delete files the test is worthless. This tends to happen on Windows randomly.
                Log.info(c, method, "WARNING: Exiting from test early because files could not be deleted.");
                return;
            }

            assertNotNull("The snoop application was not stopped when removed",
                          server.waitForStringInLog("CWWKZ0059E.* snoop"));

            assertNotNull("The snoop application was not removed from the web container when stopped",
                          server.waitForStringInLog("CWWKT0017I.*snoop.*"));

            try {
                //check that the application won't load in the web container once stopped
                Log.info(c, method, "Calling Snoop Application with URL=" + url.toString() + ", expecting no response.");
                con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, CONN_TIMEOUT);
                br = HttpUtils.getConnectionStream(con);
                line = br.readLine();
                assertFalse("The response did contain the \'Snoop Servlet\' when it shouldn't",
                            line.contains("Snoop Servlet"));
                con.disconnect();

            } catch (java.io.FileNotFoundException ex) {
                //expected if the web container has unloaded the page already (it could be blank or not there)
            }

            //re-add the application to make sure it resumes working
            server.setMarkToEndOfLog();
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "tmp",
                                               SNOOP_WAR);

            //unzip the application into a temp location so that we can easily modify the application in a minute
            TestUtils.unzip(new File(server.getServerRoot() + "/tmp/snoop.war"),
                            new File(server.getServerRoot() + "/tmp/unzip/snoop.war"));

            // Copy the expanded snoop.war to "apps"
            server.renameLibertyServerRootFile("tmp/unzip/snoop.war", "apps/snoop.war");

            //make sure the started message has been output twice by the server (once earlier, once now).
            assertNotNull("The snoop application never resumed running after being stopped",
                          server.waitForStringInLog("CWWKZ0003I.* snoop"));

            //add a file to the extracted snoop application
            server.setMarkToEndOfLog();
            server.copyFileToLibertyServerRoot("/apps/snoop.war/",
                                               "updatedApplications/blankFile.txt");

            // Make sure we don't update the app a second time.
            assertNull("The application snoop should not have been updated.",
                       server.waitForStringInLog("CWWKZ0003I.* snoop"));

            //make sure it loads into the web front end correctly again
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/apps");
            pathsToCleanup.add(server.getServerRoot() + "/tmp");
            pathsToCleanup.add(server.getInstallRoot() + "/usr/shared/apps/snoop.war");
            server.stopServer("CWWKZ0059E", "CWWKZ0014W");
        }
    }

    @Test
    public void testSymbolicLinkInLooseApp() throws Exception {
        File link = new File("/bin/ln");
        if (link.exists() && link.canExecute()) {
            String serverRoot = server.getServerRoot();
            try {
                final String method = testName.getMethodName();
                Log.info(c, method, "Starting test " + method);
                server.setServerConfigurationFile("/looseApplication/server.xml");

                //copy extracted application to server, then add the server xml that defines it to dropins

                TestUtils.unzip(new File(PUBLISH_UPDATED + "/testWarApplication.war"),
                                new File(serverRoot + "/testWarApplication"));
                TestUtils.unzip(new File(PUBLISH_UPDATED + "/testWarApplication.war"),
                                new File(serverRoot + "/symbolicLink"));

                // delete out the classes directory from the app
                server.deleteDirectoryFromLibertyServerRoot("testWarApplication/WEB-INF/classes");

                // make sure that the hard part of the link doesn't match WEB-INF/classes
                // change it to somethingelse/Classes
                assertTrue("Failed to rename the classes folder",
                           LibertyFileManager.renameLibertyFile(server.getMachine(),
                                                                serverRoot + "/symbolicLink/WEB-INF/classes",
                                                                serverRoot + "/symbolicLink/WEB-INF/Classes"));
                assertTrue("Failed to rename the WEB-INF folder",
                           LibertyFileManager.renameLibertyFile(server.getMachine(),
                                                                serverRoot + "/symbolicLink/WEB-INF",
                                                                serverRoot + "/symbolicLink/somethingelse"));
                server.copyFileToLibertyServerRoot(DROPINS_FISH_DIR, "looseApplication/testWarApplication.war.xml");

                String hardPath = serverRoot + "/symbolicLink/somethingelse/Classes";
                String symPath = serverRoot + "/testWarApplication/WEB-INF/classes";
                String[] execParameters = new String[] { "/bin/ln", "-s", hardPath, symPath };
                Process process = Runtime.getRuntime().exec(execParameters);
                assertEquals("Creating symbolic link didn't return 0.", 0, process.waitFor());

                server.startServer(method + ".log");

                // Pause for application to start properly and server to say it's listening on ports
                final int httpDefaultPort = server.getHttpDefaultPort();
                assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                              server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
                assertNotNull("The application testWarApplication did not appear to have started.",
                              server.waitForStringInLog("CWWKZ0001I.* testWarApplication"));
                assertNotNull("The server did not report that the loose app was being used",
                              server.waitForStringInLog("CWWKZ0134I.* testWarApplication"));

                // make sure a call to the servlet works
                URL url = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/testWarApplication/TestServlet");
                Log.info(c, method, "Calling test Application with URL=" + url.toString());
                HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
                BufferedReader br = HttpUtils.getConnectionStream(con);
                String line = br.readLine();
                assertTrue("The response did not contain the \'Test servlet\'",
                           line.contains(UPDATED_MESSAGE));
                con.disconnect();
            } finally {
                // manually do this clean up because the stopServer command will try
                // to collect the server as is, and will break upon looking at the
                // symbolic links created...
                LibertyFileManager.deleteLibertyDirectoryAndContents(server.getMachine(),
                                                                     serverRoot + "/symbolicLink");
                LibertyFileManager.deleteLibertyDirectoryAndContents(server.getMachine(),
                                                                     serverRoot + "/testWarApplication");
                LibertyFileManager.deleteLibertyDirectoryAndContents(server.getMachine(),
                                                                     serverRoot + "/" + DROPINS_FISH_DIR);
                LibertyFileManager.deleteLibertyFile(server.getMachine(),
                                                     serverRoot + "/testWarApplication.war");
            }
        }
    }

    /**
     * This test tests the application manager prevents you installing two applications with
     * the same name.
     *
     * It also puts an invalid file in the liberty.install.dir/usr/shared/apps to make
     * sure that the server.config.dir location is searched first.
     *
     * It then removes the applications at the end of the test.
     */
    @Test
    public void testYouCannotDeployTwoAppsWithTheSameName() throws Exception {
        final String method = testName.getMethodName();
        try {
            //load a new server xml
            server.setServerConfigurationFile("/twoapps/server.xml");
            //copy file to correct location
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "apps",
                                               SNOOP_WAR);
            server.startServer(method + ".log");
            // We should see one instance of snoop coming up
            assertNotNull("The snoop application never came up", server.waitForStringInLog("CWWKZ0001I.* snoop"));
            // And a second failing to come up.
            assertNotNull("We did not detect two snoop applications", server.waitForStringInLog("CWWKZ0013E.* snoop"));

            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = null;
            try {
                con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            } catch (IOException ioe) {
                //consume error and try again (if it failed it is a rare test framework issue)
                con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            }

            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/apps");
            server.stopServer("CWWKZ0013E");
        }
    }

    /**
     * This test tests when the application in the configuration file is not assigned a name or the name is "",
     * if the file location is correct, the application still needs to start successfully
     *
     * @throws Exception
     */
    @Test
    public void testStartApplicationWithoutName() throws Exception {
        final String method = testName.getMethodName();
        try {
            //load a new server xml
            server.setServerConfigurationFile("/appWithoutName/server.xml");
            //copy file to correct location
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "apps", SNOOP_WAR);
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "apps", TEST_WAR_APPLICATION);

            server.startServer(method + ".log");
            // We should see one instance of snoop coming up
            assertNotNull("The snoop application never came up", server.waitForStringInLog("CWWKZ0001I.* snoop"));
            // Access the snoop
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();

            // Access the testWarApplication
            assertNotNull("The application testWarApplication did not appear to have started.", server.waitForStringInLog("CWWKZ0001I.* testWarApplication"));
            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/testWarApplication/TestServlet");
            Log.info(c, method, "Calling test Application with URL=" + url.toString());
            //check application is installed
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("Unepxected servlet response. Expected = 'test servlet is running'. Actual =" + line, line.contains("test servlet is running"));
            con.disconnect();

        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/apps");
        }

    }

    /**
     * This test tests the application manager's ability to start an application in
     * the liberty.install.dir/usr/shared/apps folder by configuring the server xml.
     *
     * It then removes the application at the end of the test.
     */
    @Test
    public void testStartStopFromXmlInLibertyDir() throws Exception {
        final String method = testName.getMethodName();
        try {
            //load a new server xml
            server.setServerConfigurationFile("/appsConfigured/server.xml");
            //copy file to correct location
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(),
                                                   server.getInstallRoot() + "/usr/shared/apps",
                                                   PUBLISH_FILES + "/" + SNOOP_WAR);
            server.startServer(method + ".log");
            // Wait for the application to be installed before proceeding
            assertNotNull("The snoop application never came up", server.waitForStringInLog("CWWKZ0001I.* snoop"));

            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();
        } finally {
            pathsToCleanup.add(server.getInstallRoot() + "/usr/shared/apps/snoop.war");
        }
    }

    /**
     * This tests to see that the application manager handles being given an invalid
     * application (one which does not exist) correctly.
     */
    @Test
    public void testConfigureMissingApplication() throws Exception {
        final String method = testName.getMethodName();

        //load a new server xml
        server.setServerConfigurationFile("/appsConfigured/server.xml");

        server.startServer(method + ".log", true);
        // Wait for the application to be installed before proceeding
        assertNotNull("Snoop application start failure message not found", server.waitForStringInLog("CWWKZ0014W.* snoop"));

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
        Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
        try {
            //check application is not installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            fail("was expecting exception, but got nothing! In fact we got text output as: " + line);
            con.disconnect();
        } catch (FileNotFoundException e) {
            //expected.
        } catch (Exception e) {
            fail("unexpected exception thrown. Expecting FileNotFoundException, got: " + e.getMessage());
        }

        server.stopServer("CWWKZ0014W");
    }

    /**
     * This tests to see that the application manager handles being given an invalid
     * application (one which does not exist) correctly.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testConfigureMissingApplicationFull() throws Exception {
        final String method = testName.getMethodName();

        //load a new server xml
        server.setServerConfigurationFile("/appsConfigured/server.xml");

        server.startServer(method + ".log", true);
        // Wait for the application to be installed before proceeding
        assertNotNull("Snoop application start failure message not found", server.waitForStringInLog("CWWKZ0014W.* snoop"));
        assertNull("Snoop was started but should have failed", server.waitForStringInLog("CWWKZ0001I: .* snoop"));

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
        Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
        try {
            //check application is not installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            fail("was expecting exception, but got nothing! In fact we got text output as: " + line);
            con.disconnect();
        } catch (FileNotFoundException e) {
            //expected.
        } catch (Exception e) {
            fail("unexpected exception thrown. Expecting FileNotFoundException, got: " + e.getMessage());
        }
        server.stopServer("CWWKZ0014W");
    }

    @Test
    public void testConfigureInvalidApplication() throws Exception {
        final String method = testName.getMethodName();

        try {
            //load a new server xml
            server.setServerConfigurationFile("/appsConfigured/server.xml");

            server.copyFileToLibertyServerRoot("apps",
                                               "/bootstrap.properties");
            server.renameLibertyServerRootFile("apps/bootstrap.properties", "apps/snoop.war");
            server.startServer(method + ".log");

            // Because the required location attribute is missing, config will issue an error
            assertNotNull("Invalid archive message not found", server.waitForStringInLog("CWWKM0101E.*"));
            assertNotNull("Invalid resource message not found", server.waitForStringInLog("CWWKZ0021E.*"));
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/apps");
            server.stopServer("CWWKZ0021E", "CWWKM0101E");
        }
    }

    /**
     * This is a test for defect 48922 to make sure that when application monitor is enabled on a directory and the directory changes in the server.xml then the application is
     * removed
     */
    @Test
    @FFDCIgnore(value = { IOException.class })
    public void testAppsUninstalledWhenMonitoredDirChanges() throws Exception {
        final String method = testName.getMethodName();
        try {
            //to make sure that the server has to create the scannedDir location try to delete it before
            //starting the server
            server.deleteFileFromLibertyServerRoot("ScannedDir");

            // Set up the server to use the server.xml defining an app monitor
            server.setServerConfigurationFile("/appMonitoringTesting/monitorScannedDirServer.xml");

            // Start the server and wait for it to start the application monitor
            server.startServer(method + ".log");
            assertNotNull("The server never reported that the application manager had started", server.waitForStringInLog("CWWKZ0058I"));
            RemoteFile f = server.getFileFromLibertyServerRoot("ScannedDir");
            assertTrue("The scanned dir should have been created by the server", f.exists());

            // Copy the snoop into the scannedDir
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "ScannedDir", SNOOP_WAR);

            // Wait for the application to be installed before proceeding
            assertNotNull("The snoop application never came up", server.waitForStringInLog("CWWKZ0001I.* snoop"));

            // Make sure it was installed
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();

            // Change the server.xml file to scan a different directory
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/appMonitoringTesting/monitorOtherDirServer.xml");
            // make sure configuration was reloaded
            assertNotNull("The server config was not reloaded", server.waitForStringInLog("CWWKG0017I"));
            // Wait for application to stop
            assertNotNull("Snoop should have been uninstalled when the monitored directory changed", server.waitForStringInLog("CWWKZ0009I.* snoop"));
            //check the OtherDir location has been created by the server
            assertTrue("The other dir should have been created by the server", server.fileExistsInLibertyServerRoot("/OtherDir"));

            // Re-run test, snoop should of been removed
            try {
                //check application is not installed
                Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
                con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, CONN_TIMEOUT);
                br = HttpUtils.getConnectionStream(con);
                line = br.readLine();
                fail("was expecting exception, but got nothing! In fact we got text output as: " + line);
                con.disconnect();
            } catch (FileNotFoundException e) {
                //expected.
            } catch (Exception e) {
                fail("unexpected exception thrown. Expecting FileNotFoundException, got: " + e.getMessage());
            }

            // Finally make sure that if the snoop is added to the new directory it is picked up
            server.setMarkToEndOfLog();
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "OtherDir", SNOOP_WAR);
            // Wait for app to be found/installed.
            assertNotNull("The snoop application was not found in the OtherDir", server.waitForStringInLog("CWWKZ0001I.* snoop"));

            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/ScannedDir");
            pathsToCleanup.add(server.getServerRoot() + "/OtherDir");
        }
    }

    /**
     * This test tests the application manager's ability to start an application in
     * the server.config.dir/apps folder by configuring the server xml
     *
     * It also puts an invalid file in the liberty.install.dir/usr/shared/apps to make
     * sure that the server.config.dir location is searched first.
     *
     * It then removes the applications at the end of the test.
     */
    @Test
    public void testStartBadLocation() throws Exception {
        final String method = testName.getMethodName();
        try {
            //load a new server xml
            server.setServerConfigurationFile("/appsConfigured/badLocation.xml");
            //copy file to correct location
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "apps",
                                               SNOOP_WAR);

            server.startServer(method + ".log");
            // Because the required location attribute is missing, config will issue an error
            assertNotNull("Invalid configuration message not found", server.waitForStringInLog("CWWKG0058E.*"));
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/apps");
            server.stopServer("CWWKG0058E");
        }
    }

    @Test
    public void testValidFileDownloadFromWWW() throws Exception {
        final String method = testName.getMethodName();

        try {
            //copy file to correct location to have everything ready
            server.copyFileToLibertyServerRoot("apps",
                                               "testDownloadValidFileFromWWW.war");

            //load a new server xml and start server
            server.setServerConfigurationFile("/testDownload/server1.xml");
            server.startServer(method + ".log");

            // check if application started successfully
            assertNotNull("The testDownloadValidFileFromWWW.war application never came up", server.waitForStringInLog("CWWKZ0001I.* testDownloadValidFileFromWWW"));
            assertNotNull("The testDownloadValidFileFromWWW.war application was not available", server.waitForStringInLog("CWWKT0016I.*testDownloadValidFileFromWWW"));

            //load a new server xml in order to download the new application from url
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/testDownload/server2.xml");
            assertNotNull("The server config was not updated", server.waitForStringInLog("CWWKG0017I"));

            // check if second application has been installed successfully
            assertNotNull("The downloadApp.war application never came up", server.waitForStringInLog("CWWKZ0001I.* downloadApp"));
            assertNotNull("The downloadApp.war application was not available", server.waitForStringInLog("CWWKT0016I.*downloadApp"));

            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/downloadApp/downloadApp");
            Log.info(c, method, "Calling downloadApp Application with URL=" + url.toString());
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            //check if second application has started properly
            assertTrue("The response did not contain the \'Hello Simple Servlet!\'",
                       line.contains("Hello Simple Servlet!"));
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/apps");
        }
    }

    @Test
    public void testDataSourceStillUsableByAppDuringServerShutdown() throws Exception {
        final String method = testName.getMethodName();

        try {

            //ensure that derby.jar is available in the server_home/derby directory
//            File derbyDir = new File(server.getServerRoot(), DERBY_DIR);
//            if (!derbyDir.exists())
//                assertTrue("Unable to create derby directory", derbyDir.mkdir());
//            File derbyJar = new File(derbyDir, "derby.jar");
//            if (!derbyJar.exists()) {
//                server.copyFileToLibertyServerRoot(DERBY_DIR, "dataSourceApp/derby.jar");
//            }
//            assertTrue("derby.jar not available for JDBC driver: " + derbyJar.getAbsolutePath(), derbyJar.exists());

            //make sure we are using the correct server configuration before starting the server
            server.setServerConfigurationFile("/dataSourceApp/server.xml");
            server.startServer(method + ".log");
            if (!server.isJava2SecurityEnabled()) {

                // Pause to make sure the server is ready to install apps
                assertNotNull("The server never reported that it was ready to install web apps.",
                              server.waitForStringInLog("CWWKZ0058I:"));

                //install application
                server.copyFileToLibertyServerRoot(PUBLISH_FILES, DROPINS_FISH_DIR, DATA_SOURCE_APP_EAR);

                //check for text from postConstruct method
                assertNotNull("The EJB was not constructed.",
                              server.waitForStringInLog("DataSourceBean init enter"));
                assertNotNull("The EJB did not update the database appropriately on startup.",
                              server.waitForStringInLog("DataSourceBean init inserted 1 rows"));
                assertNotNull("The EJB PostConstruct method did not exit successfully.",
                              server.waitForStringInLog("DataSourceBean init exit"));
                assertNotNull("The server never reported that the app started successfully.",
                              server.waitForStringInLog("CWWKZ0001I.*DataSourceApp"));

                //stop server
                server.stopServer(false);

                //check for text from preDestroy method
                assertTrue("The EJB was not destroyed.",
                           server.findStringsInLogs("DataSourceBean destroy enter").size() > 0);
                assertTrue("The EJB did not update the database appropriately on shutdown.",
                           server.findStringsInLogs("DataSourceBean destroy inserted 1 rows").size() > 0);
                assertTrue("The EJB PreDestroy method did not exit successfully.",
                           server.findStringsInLogs("DataSourceBean destroy exit").size() > 0);
                assertTrue("The server never reported that the app stopped successfully.",
                           server.findStringsInLogs("CWWKZ0009I.*DataSourceApp").size() > 0);
            }

        } finally {
            //if we failed to delete file before, try to delete it now.
            pathsToCleanup.add(server.getServerRoot() + "/" + DROPINS_FISH_DIR);
            pathsToCleanup.add(server.getServerRoot() + "/" + DERBY_DIR);
            pathsToCleanup.add(server.getServerSharedPath() + "/" + "resources/data");
            server.postStopServerArchive();
        }
    }

    /**
     * On windows applications became locked if they contained items that went through
     * DDParser - the application here is the stack product app that came in with the PMR
     * and contains web.xml and ibm.web.ext.xmi.
     *
     * The test simply starts up server with app loaded, then comments out the app, and
     * then attempts to delete the file from disk.
     *
     * @throws Exception
     */
    @Test
    public void testNoFileLockOnWebExt_PI15121() throws Exception {
        final String method = testName.getMethodName();

        try {
            //copy file to correct location to have everything ready
            server.copyFileToLibertyServerRoot("apps",
                                               "deletingWARPI15121/48881.L6Q.000.isf-servlets.war");

            //load a new server xml and start server
            server.setServerConfigurationFile("/deletingWARPI15121/start.xml");
            server.startServer(method + ".log");

            // check if application started successfully
            assertNotNull("The test application was not available", server.waitForStringInLog("CWWKT0016I.*iis/isf"));

            //load a new server xml in order to stop the application
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/deletingWARPI15121/end.xml");
            assertNotNull("The server config was not updated", server.waitForStringInLog("CWWKG0017I"));

            // Wait for the server to confirm it stopped the app
            assertNotNull("The application isf-servlet did not appear to have been stopped.",
                          server.waitForStringInLog("CWWKZ0009I.* isf-servlets"));

            // Check we can delete the application
            server.deleteFileFromLibertyServerRoot("apps/4881.L6Q.000.isf-servlets.war");
            assertFalse("Application was not deleted", server.fileExistsInLibertyServerRoot("apps/4881.L6Q.000.isf-servlets.war"));

        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/apps");
        }
    }

    @Test
    public void testZeroFeaturesToApplicationFeatures() throws Exception {
        final String method = testName.getMethodName();

        try {
            server.installSystemFeature("test.app.notifications");
            server.installSystemBundle("test.app.notifications");
            //copy files to correct location to have everything ready
            server.copyFileToLibertyServerRoot(DROPINS_FISH_DIR, "appNotifications/apps/app.tan");

            //load a new server xml and start server
            server.setServerConfigurationFile("/noFeatures/server.xml");
            server.startServer(method + ".log");

            // configure to remove the first set of apps and configure the new set
            ServerConfiguration configuration = server.getServerConfiguration();
            configuration.getFeatureManager().getFeatures().add("test.app.notifications");

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(configuration);
            // Here we wait only for 10 seconds.
            // This is questionable, if it starts to fail then we need to think of some other way.
            // Before the fix the update would timeout and continue after 30 seconds so we are really
            // trying to ensure it is less than 30 seconds.
            // CWWKF0008I: Feature update completed ...
            if (server.waitForStringInLogUsingMark("CWWKF0008I", 10000) == null) {
                throw new Exception("Server configuration update did complete within the allotted interval");
            }
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/" + DROPINS_FISH_DIR);
            server.stopServer("CWWKZ0005E");
            server.uninstallSystemFeature("test.app.notifications");
            server.uninstallSystemBundle("test.app.notifications");
        }
    }

    @ExpectedFFDC("java.lang.NullPointerException")
    @Test
    public void testInstallCalledNotification() throws Exception {
        final String method = testName.getMethodName();
        final String[] TAN_APPS1 = getAppIDs(1, 4);
        final String[] TAN_APPS2 = getAppIDs(5, 10);

        try {
            server.installSystemFeature("test.app.notifications");
            server.installSystemBundle("test.app.notifications");

            //copy files to correct location to have everything ready
            server.copyFileToLibertyServerRoot("apps", "appNotifications/apps/app.tan");
            server.copyFileToLibertyServerRoot("apps", "appNotifications/apps/app.nca");
            server.copyFileToLibertyServerRoot("apps", "appNotifications/apps/app.nha");

            //load a new server xml and start server
            server.setServerConfigurationFile("/appNotifications/server.xml");
            server.startServer(method + ".log");

            checkApplications(TAN_APPS1);

            // configure to remove the first set of apps and configure the new set
            ServerConfiguration configuration = server.getServerConfiguration();
            configuration = configuration.clone();

            ConfigElementList<Application> applications = configuration.getApplications();
            for (String id : TAN_APPS1) {
                applications.removeById(id);
            }
            for (String id : TAN_APPS2) {
                Application newApp = new Application();
                newApp.setId(id);
                newApp.setName(id);
                newApp.setLocation("app.tan");
                applications.add(newApp);
            }
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(configuration);
            // CWWKG0016I: Starting server configuration update.
            if (server.waitForStringInLogUsingMark("CWWKG0016I") == null) {
                throw new Exception("Server configuration update did not start within the allotted interval");
            }
            checkApplications(TAN_APPS2);
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/apps");
            // Ignore expected NPE and unknown resource warning
            server.stopServer("CWWKZ0002E", "CWWKZ0014W", "CWWKZ0005E");
        }
    }

    /**
     * Test that the amount of time allowed for application shutdown can be controlled
     * by the <applicationManager stopTimeout='<timeout>' /> setting. The test will also
     * ensure that the existing default timeout of 30 seconds is preserved when this configuration
     * option is not set in server.xml.
     *
     * This configuration option was introduced as a consequence of PMR 60708,227,000 where a
     * servlet context listener is taking longer than 30 seconds to shutdown as it is attempting to clean
     * up other connections / resources. This is an issue at server stop when the daemon thread that
     * is running the contextListenerDestroyed() method exits mid-method when the last user thread
     * goes. This is why this test uses a Servlet context listener and is focused on ensuring correct
     * behaviour at server shutdown, rather than just stopping the application.
     */
    @Mode(TestMode.FULL)
    @Test
    public void testStopTimeout() throws Exception {
        final String EXIT_TRACE = "TEST : SlowStopApp exited"; //message if the app successfully stops
        final String method = testName.getMethodName();
        boolean wasServerRunning = false;
        try {
            //if the server is already running, stop it and start a new one as we need to make
            //sure that the config we are testing is read in correctly
            if (server != null) {
                if (server.isStarted()) {
                    wasServerRunning = true;
                    Log.info(c, method, "Stopping current server");
                    server.stopServer();
                }
            }

            //1: test that default behaviour is maintained when stopTimeout is not specified i.e. it's 30 seconds
            Log.info(c, method, "Testing existing default timeout of 30 seconds for app shutdown is preserved.");
            server = LibertyServerFactory.getLibertyServer("slowAppServer");
            // copy app to dropins
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, DROPINS_FISH_DIR, SLOW_APP);
            server.startServer(method + ".log");
            assertNotNull("Test application not started", server.waitForStringInLog("CWWKT0016I:.*/slowapp"));
            HttpUtils.findStringInUrl(server, "/slowapp/TestServlet?timeout=5", "test servlet");
            server.stopServer(false); //do not archive the files when the server stops as we need to check them
            try {
                assertEquals("Expecting to find exit message, but didn't.", 1, server.findStringsInLogs(EXIT_TRACE).size());
            } finally {
                server.postStopServerArchive(); //now archive the server files
            }

            //2: Have the app wait beyond the timeout period, which will cause it to not produce the output message
            Log.info(c, method, "Testing that a slow stopping app is terminated at approx 30 seconds.");
            server.startServer(method + ".log");
            assertNotNull("Test application not started", server.waitForStringInLog("CWWKT0016I:.*/slowapp"));
            HttpUtils.findStringInUrl(server, "/slowapp/TestServlet?timeout=40", "test servlet");
            server.stopServer(false); //do not archive the files when the server stops as we need to check them
            try {
                assertEquals("Not expecting to find exit message, but did.", 0, server.findStringsInLogs(EXIT_TRACE).size());
            } finally {
                server.postStopServerArchive(); //now archive the server files
            }

            //3: Configure the stop timeout to be 60 seconds and retry with an application shutdown wait of 40 seconds
            Log.info(c, method, "Testing 60 second timeout allows an application that takes 40 seconds to stop to complete normally.");
            server.setServerConfigurationFile("/slowApp/stopTimeout.xml");
            server.startServer(method + ".log");
            assertNotNull("Test application not started", server.waitForStringInLog("CWWKT0016I:.*/slowapp"));
            HttpUtils.findStringInUrl(server, "/slowapp/TestServlet?timeout=40", "test servlet");
            server.stopServer(false); //do not archive the files when the server stops as we need to check them
            try {
                assertEquals("Expecting to find exit message, but didn't.", 1, server.findStringsInLogs(EXIT_TRACE).size());
            } finally {
                server.postStopServerArchive(); //now archive the server files
            }
        } finally {
            //restore the original server setting in case more tests are to be run
            server.stopServer();
            server = LibertyServerFactory.getLibertyServer("appManagerTestServer");
            if (wasServerRunning) {
                server.startServer();
            }
        }
    }

    /**
     * This test verifies that the autoStart property is working correctly.
     * We test a server with two apps defined: one with "autoStart=false" and the other with
     * no autoStart property. The former app should not start, while the latter should.
     *
     * @throws Exception
     */
    @Test
    @MinimumJavaLevel(javaLevel = 7)
    public void testApplicationAutoStartProperty() throws Exception {
        final String method = testName.getMethodName();
        try {
            //load a new server xml
            server.setServerConfigurationFile("/autoStartFalse/server.xml");
            //copy file to correct location
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "apps", SNOOP_WAR);
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "apps", TEST_WAR_APPLICATION);

            server.startServer(method + ".log");
            // We should see one instance of snoop coming up
            assertNotNull("The snoop application never came up", server.waitForStringInLog("CWWKZ0001I.* snoop"));
            // Access the snoop
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
            Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Snoop Servlet\'",
                       line.contains("Snoop Servlet"));
            con.disconnect();

            // Access the testWarApplication and make sure it does NOT start
            assertTrue("The application testWarApplication appears to have started, despite setting autoStart to false",
                       server.findStringsInLogs("CWWKZ0001I.* testWarApplication").isEmpty());
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/apps");
        }
    }

    private static String[] getAppIDs(int startId, int numApps) {
        String[] result = new String[numApps];
        for (int i = 0; i < numApps; i++) {
            result[i] = "app" + (startId + i);
        }
        return result;
    }

    private void checkApplications(String[] appIDs) {
        final String INSTALLING_APP = "INSTALLING APP: ";
        final String STARTING_APP = "STARTING APP: ";
        final String CURRENT_INSTALLING_APPS = "CURRENT INSTALLING APPS: ";
        Collection<String> appsToFind = new ArrayList<String>(Arrays.asList(appIDs));
        for (int i = 0; i < appIDs.length; i++) {
            String foundLog = server.waitForStringInLogUsingLastOffset(INSTALLING_APP + ".*");
            assertNotNull("Did not find the expected number of apps, still looking for: " + appsToFind, foundLog);
            String foundApp = foundLog.substring(foundLog.indexOf(INSTALLING_APP) + INSTALLING_APP.length());
            assertTrue("Found unexpected application installing: " + foundApp, appsToFind.remove(foundApp));
        }
        String currentInstallingLog = server.waitForStringInLogUsingLastOffset(CURRENT_INSTALLING_APPS + ".*");
        for (String app : appIDs) {
            assertTrue("Cound not find app: " + app + " in " + currentInstallingLog, currentInstallingLog.contains(app));
        }
        appsToFind.addAll(Arrays.asList(appIDs));
        for (int i = 0; i < appIDs.length; i++) {
            String foundLog = server.waitForStringInLogUsingLastOffset(STARTING_APP + ".*");
            assertNotNull("Did not find the expected number of apps, still looking for: " + appsToFind, foundLog);
            String foundApp = foundLog.substring(foundLog.indexOf(STARTING_APP) + STARTING_APP.length());
            assertTrue("Found unexpected application starting: " + foundApp, appsToFind.remove(foundApp));
        }

    }

    @Override
    protected LibertyServer getServer() {
        return FATTest.server;
    }

}