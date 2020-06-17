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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import test.utils.TestUtils;

@RunWith(FATRunner.class)
public class DropinsTests extends AbstractAppManagerTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("dropinsTestServer");
    private final Class<?> c = DropinsTests.class;

    @Rule
    public TestName testName = new TestName();

    @Override
    protected Class<?> getLogClass() {
        return c;
    }

    @Override
    protected LibertyServer getServer() {
        return server;
    }

    /**
     * This test tests the application manager dropin monitor's ability to install a war
     * file from the dropins folder and then remove and uninstall the war file.
     * <br/>
     * It also tests that we update the application while the server is running and
     * that the new application version is being hosted.
     */
    @Test
    public void testAutoInstallWar() throws Exception {
        final String method = testName.getMethodName();
        try {
            //make sure we are using the correct server configuration before starting the server
            server.setServerConfigurationFile("/defaultServer/server.xml");
            server.startServer(method + ".log");

            // Pause to make sure the server is ready to install apps
            assertNotNull("The server never reported that it was ready to install web apps.",
                          server.waitForStringInLog("CWWKZ0058I:"));

            //install file
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, DROPINS_FISH_DIR, TEST_WAR_APPLICATION);

            // Pause for application to start properly and server to say it's listening on ports
            final int httpDefaultPort = server.getHttpDefaultPort();
            assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                          server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
            assertNotNull("The application testWarApplication did not appear to have started.",
                          server.waitForStringInLog("CWWKZ0001I.* testWarApplication"));
            assertNotNull("The server did not report that the app would not be extracted",
                          server.waitForStringInLog("CWWKZ0136I.* testWarApplication"));

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
            server.copyFileToLibertyServerRoot(PUBLISH_UPDATED, DROPINS_FISH_DIR, "testWarApplication.war");

            //make sure the application is claiming to have been updated
            assertNotNull("The application testWarApplication did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* testWarApplication"));

            //get the message from the application to make sure it is the new appication contents
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            assertTrue("The response did not contain the \'updated test servlet\'",
                       line.contains(UPDATED_MESSAGE));
            con.disconnect();

            // remove file
            boolean deleted = deleteFile(server.getMachine(),
                                         server.getServerRoot() + "/" + DROPINS_FISH_DIR + "/testWarApplication.war");

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
            pathsToCleanup.add(server.getServerRoot() + "/" + DROPINS_FISH_DIR);
            server.stopServer("CWWKZ0014W");
        }
    }

    /**
     * This test tests the application manager dropin monitor's ability to install an
     * extracted war folder from the dropins folder and then to stop and remove the application.
     */
    @Test
    public void testAutoInstallAndStopExtractedWar() throws Exception {
        final String method = testName.getMethodName();
        try {
            //make sure we are using the correct server configuration before starting the server
            server.setServerConfigurationFile("/defaultServer/server.xml");
            server.startServer(method + ".log");

            //unzip snoop into folder structure
            TestUtils.unzip(new File(PUBLISH_FILES + "/testWarApplication.war"),
                            new File(server.getServerRoot() + "/" + DROPINS_FISH_DIR + "/war/testWarApplication"));

            // Wait for the application to be installed before proceeding
            assertNotNull("The testWarApplication application never came up",
                          server.waitForStringInLog("CWWKZ0001I.* testWarApplication"));
            assertNotNull("The server did not report that the loose app was being used",
                          server.waitForStringInLog("CWWKZ0135I.* testWarApplication"));

            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/testWarApplication/TestServlet");
            Log.info(c, method, "Calling testWarApplication Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Test servlet\' : " + line,
                       line.contains("test servlet is running."));
            con.disconnect();

            //replace original application with new version to test that it updates
            server.setMarkToEndOfLog();
            TestUtils.unzip(new File(PUBLISH_UPDATED + "/testWarApplication.war"),
                            new File(server.getServerRoot() + "/" + DROPINS_FISH_DIR + "/war/testWarApplication"));

            //make sure the application is claiming to have been updated - one of two possible messages here:
            //0003I for an app restart or 0062I for incremental publishing (update without app restart)
            assertNotNull("The application testWarApplication did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* testWarApplication|CWWKZ0062I.* testWarApplication"));

            //get the message from the application to make sure it is the new appication contents
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            br = HttpUtils.getConnectionStream(con);
            line = br.readLine();
            if (!line.contains(UPDATED_MESSAGE)) {
                // It's possible that we're restarting twice because the file monitor picked up the changes in the middle
                // of the unzip. If that happens, wait for another updated message and try again.
                assertNotNull("The application testWarApplication did not appear to have been updated a second time.",
                              server.waitForMultipleStringsInLogUsingMark(2, "CWWKZ0003I.* testWarApplication|CWWKZ0062I.* testWarApplication"));
                con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
                br = HttpUtils.getConnectionStream(con);
                line = br.readLine();
            }
            assertTrue("The response did not contain the \'updated test servlet\' : " + line,
                       line.contains(UPDATED_MESSAGE));

            //add a file, only WEB-INF is monitored so add it in there
            server.setMarkToEndOfLog();
            server.copyFileToLibertyServerRoot("/" + DROPINS_FISH_DIR + "/war/testWarApplication/WEB-INF",
                                               "updatedApplications/blankFile.txt");

            //make sure the application is claiming to have been updated
            assertNotNull("The application testWarApplication did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* testWarApplication"));

            //remove a file
            server.setMarkToEndOfLog();
            server.deleteFileFromLibertyServerRoot("/" + DROPINS_FISH_DIR + "/war/testWarApplication/WEB-INF/blankFile.txt");

            //make sure the application is claiming to have been updated
            assertNotNull("The application testWarApplication did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* testWarApplication"));

            //delete application then check it is deleted
            server.setMarkToEndOfLog();
            boolean deleted = deleteFile(server.getMachine(),
                                         server.getServerRoot() + "/" + DROPINS_FISH_DIR + "/war");

            if (!deleted) {
                // Occasionally on Windows something else will hold onto a file while we are trying to delete it. If
                // that happens, just exit early.
                return;
            }
            assertNotNull("The application testWarApplication did not appear to have been stopped after deletion.",
                          server.waitForStringInLog("CWWKZ0009I.* testWarApplication"));
            try {
                //check application is not installed
                con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, CONN_TIMEOUT);
                br = HttpUtils.getConnectionStream(con);
                line = br.readLine();
                fail("was expecting exception, but failed to hit one! In fact we got text output as: " + line);
                con.disconnect();
            } catch (FileNotFoundException e) {
                //expected.
            } catch (Exception ex) {
                //unexpected - fail
                fail("unexpected exception thrown in test. The exception was: " + ex.toString());
            }
        } finally {
            //if we failed to delete file before, try to delete it now.
            pathsToCleanup.add(server.getServerRoot() + "/" + DROPINS_FISH_DIR);
            pathsToCleanup.add(server.getServerRoot() + "/testWarApplication.war");
            server.stopServer("CWWKZ0014W");
        }
    }

    /**
     * Using dropins we install a loose config application. We then test that it is installed,
     * add an excluded file to make sure that it doesn't update and then add a non-exluded file
     * to make sure that the loose application does update when it is added. As we already test
     * the updating of applications we don't update the application and only need to look for
     * the updated message.
     *
     * @throws Exception
     */
    @Test
    public void testLooseConfigAppUpdating() throws Exception {
        final String method = testName.getMethodName();
        try {

            server.setServerConfigurationFile("/looseApplication/server.xml");
            server.startServer(method + ".log");

            // Pause to make sure the server is ready to install apps
            assertNotNull("The server never reported that it was ready to install web apps.",
                          server.waitForStringInLog("CWWKZ0058I:"));

            //copy extracted application to server, then add the server xml that defines it to dropins

            TestUtils.unzip(new File(PUBLISH_UPDATED + "/testWarApplication.war"),
                            new File(server.getServerRoot() + "/testWarApplication"));
            server.copyFileToLibertyServerRoot(DROPINS_FISH_DIR, "looseApplication/testWarApplication.war.xml");

            // Pause for application to start properly and server to say it's listening on ports
            final int httpDefaultPort = server.getHttpDefaultPort();
            assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                          server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
            assertNotNull("The application testWarApplication did not appear to have started.",
                          server.waitForStringInLog("CWWKZ0001I.* testWarApplication"));
            assertNotNull("The server did not report that the loose app was being used",
                          server.waitForStringInLog("CWWKZ0134I.* testWarApplication"));

            URL url = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/testWarApplication/TestServlet");
            Log.info(c, method, "Calling test Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Test servlet\'",
                       line.contains(UPDATED_MESSAGE));
            con.disconnect();

            //now we have confirmed it is excluding .txt file updates, add a non txt file and make sure it updates
            server.copyFileToLibertyServerRoot("/testWarApplication/WEB-INF", "updatedApplications/blankFile.blk");

            //make sure the application is claiming to have been updated (we only expect 1 updated message to appear, so no need to check for multiple).
            assertNotNull("The application testWarApplication did not appear to have been updated when we added a valid file type.",
                          server.waitForStringInLog("CWWKZ0003I.* testWarApplication"));
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/" + DROPINS_FISH_DIR);
            pathsToCleanup.add(server.getServerRoot() + "/testWarApplication");
            pathsToCleanup.add(server.getServerRoot() + "/testWarApplication.war");
        }
    }

    @Test
    @Mode(TestMode.FULL)
    public void testLooseConfigAppUpdatingFull() throws Exception {
        final String method = testName.getMethodName();
        try {

            server.setServerConfigurationFile("/looseApplication/server.xml");
            server.startServer(method + ".log");

            // Pause to make sure the server is ready to install apps
            assertNotNull("The server never reported that it was ready to install web apps.",
                          server.waitForStringInLog("CWWKZ0058I:"));

            //copy extracted application to server, then add the server xml that defines it to dropins

            TestUtils.unzip(new File(PUBLISH_UPDATED + "/testWarApplication.war"),
                            new File(server.getServerRoot() + "/testWarApplication"));
            server.copyFileToLibertyServerRoot(DROPINS_FISH_DIR, "looseApplication/testWarApplication.war.xml");

            // Pause for application to start properly and server to say it's listening on ports
            final int httpDefaultPort = server.getHttpDefaultPort();
            assertNotNull("The server never reported that it was listening on port " + httpDefaultPort,
                          server.waitForStringInLog("CWWKO0219I.*" + httpDefaultPort));
            assertNotNull("The application testWarApplication did not appear to have started.",
                          server.waitForStringInLog("CWWKZ0001I.* testWarApplication"));
            assertNotNull("The server did not report that the loose app was being used",
                          server.waitForStringInLog("CWWKZ0134I.* testWarApplication"));

            URL url = new URL("http://" + server.getHostname() + ":" + httpDefaultPort + "/testWarApplication/TestServlet");
            Log.info(c, method, "Calling test Application with URL=" + url.toString());
            //check application is installed
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            assertTrue("The response did not contain the \'Test servlet\'",
                       line.contains(UPDATED_MESSAGE));
            con.disconnect();

            //now we have confirmed it is installed and running correctly, add an excluded file type, only web-inf is monitored so copy it to there
            server.copyFileToLibertyServerRoot("/testWarApplication/WEB-INF", "updatedApplications/blankFile.txt");

            //make sure the application is not claiming to have been updated (as we added a file which should not trigger an update)
            assertNull("The application testWarApplication was updated when we added an excluded file type to the loose config.",
                       server.waitForStringInLog("CWWKZ0003I.* testWarApplication"));

            //now we have confirmed it is excluding .txt file updates, add a non txt file and make sure it updates
            server.copyFileToLibertyServerRoot("/testWarApplication/WEB-INF", "updatedApplications/blankFile.blk");

            //make sure the application is claiming to have been updated (we only expect 1 updated message to appear, so no need to check for multiple).
            assertNotNull("The application testWarApplication did not appear to have been updated when we added a valid file type.",
                          server.waitForStringInLog("CWWKZ0003I.* testWarApplication"));
        } finally {
            pathsToCleanup.add(server.getServerRoot() + "/" + DROPINS_FISH_DIR);
            pathsToCleanup.add(server.getServerRoot() + "/testWarApplication");
            pathsToCleanup.add(server.getServerRoot() + "/testWarApplication.war");
        }
    }

    /**
     * This tests that if we have a dropins directory that is empty, and we change the location of the monitored
     * directory in the server xml, that the old dropins directory is removed.
     */
    @Test
    @FFDCIgnore(value = { IOException.class })
    public void testDropinsDeletedWhenEmptyMonitoredDirChanges() throws Exception {
        final String method = testName.getMethodName();
        try {
            //to make sure that the server has to create the scannedDir location try to delete it before
            //starting the server
            server.deleteFileFromLibertyServerRoot("ScannedDir");

            // Set up the server to use the server.xml defining an app monitor
            server.setServerConfigurationFile("/appMonitoringTesting/monitorScannedDirServer.xml");
            server.copyFileToLibertyServerRoot("/bootstrap.properties");

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

            boolean deleted = deleteFile(server.getMachine(),
                                         server.getServerRoot() + "/ScannedDir/snoop.war");

            if (!deleted) {
                // Occasionally on Windows something else will hold onto a file while we are trying to delete it. If
                // that happens, just exit early.
                return;
            }
            //check that the application stops once we delete it.
            assertNotNull("The snoop application was not stopped when removed",
                          server.waitForStringInLog("CWWKZ0009I.* snoop"));

            // Now change the server.xml file to scan a different directory
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/appMonitoringTesting/monitorOtherDirServer.xml");
            assertNotNull("The server config was not reloaded", server.waitForStringInLog("CWWKG0017I"));

            try {
                //wait for trace message to confirm delete was successful (give it 60 seconds timeout).
                assertNotNull("the trace log did not indicate that the server had successfully removed the old dropins directory.",
                              server.waitForStringInLog(".*Server deleted the old dropins directory.*",
                                                        server.getFileFromLibertyServerRoot("logs/trace.log")));
                assertFalse("The now-empty ScannedDir should be removed by the server", server.fileExistsInLibertyServerRoot("/ScannedDir"));
            } catch (FileNotFoundException e) {
                // we expect not to find the file, so this is ok.
            }
            f = server.getFileFromLibertyServerRoot("OtherDir");
            assertTrue("The other dir should have been created by the server", f.exists());

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
            server.copyFileToLibertyServerRoot(PUBLISH_FILES, "OtherDir", SNOOP_WAR);
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

    @Test
    public void testDropinsAppAlsoConfigured() throws Exception {
        final String method = testName.getMethodName();

        //make sure we are using the correct server configuration before starting the server
        server.setServerConfigurationFile("/appsConfigured/dropins.xml");

        //install file
        server.copyFileToLibertyServerRoot(PUBLISH_FILES, DROPINS_FISH_DIR, SNOOP_WAR);

        server.startServer(method + ".log");

        assertNotNull("We should get a warning message for a configured app pointing to dropins",
                      server.waitForStringInLog("CWWKZ0067W.*"));

        // Wait for the application to be installed
        assertNotNull("The snoop application never came up", server.waitForStringInLog("CWWKZ0001I.* snoop"));

    }

}
