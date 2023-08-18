/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package com.ibm.ws.security.token.ltpa.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.LTPA;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LTPAKeyRotationTests {

    // Initialize needed strings for the tests
    protected static String METHODS = null;
    protected static final String APP_NAME = "ltpaKeyRotationTestServer";
    protected static final String PROGRAMMATIC_API_SERVLET = "ProgrammaticAPIServlet";
    protected static final String authTypeForm = "FORM";
    protected static final String authTypeBasic = "BASIC";
    protected static final String cookieName = "LtpaToken2";

    // Keys to help readability of the test
    protected static final boolean IS_MANAGER_ROLE = true;
    protected static final boolean NOT_MANAGER_ROLE = false;
    protected static final boolean IS_EMPLOYEE_ROLE = true;
    protected static final boolean NOT_EMPLOYEE_ROLE = false;

    // Initialize a liberty server for basic auth and form login
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer");

    private static final Class<?> thisClass = LTPAKeyRotationTests.class;

    // Initialize the user
    private static final String validUser = "user1";
    private static final String validPassword = "user1pwd";
    private static final String managerUser = "user2";
    private static final String managerPassword = "user2pwd";
    private static final String serverShutdownMessages = "CWWKG0083W";

    // Initialize the BasicAuth Clients
    private static final BasicAuthClient baClient1 = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/basicauth1");
    private static final BasicAuthClient baClient2 = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/basicauth2");

    // Initialize the FormLogin Clients
    private static final FormLoginClient flClient1 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");
    private static final FormLoginClient flClient2 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin2");

    // Define the paths to the key files
    private static final String DEFAULT_KEY_PATH = "resources/security/ltpa.keys";
    private static final String VALIDATION_KEY1_PATH = "resources/security/validation1.keys";
    private static final String VALIDATION_KEYS_PATH = "resources/security/";
    private static final String DEFAULT_SERVER_XML = "server.xml";

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        // Function to make it easier to see when each test starts and ends
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nEntering test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }
    };

    @BeforeClass
    public static void setUp() throws Exception {
        //copy a validation keys file(ltpa2.keys) to the server so that at start-up, the file-monitor works.
        copyFileToServerResourcesSecurityDir("alternate/ltpa2.keys");
        server.startServer(true);

        assertNotNull("Featurevalid did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I", 90000));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I", 90000));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I", 90000));
    }

    @After
    public void resetConnection() {
        baClient1.resetClientState();
        baClient2.resetClientState();
        flClient1.resetClientState();
        flClient2.resetClientState();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer(serverShutdownMessages);
        } finally {
            baClient1.releaseClient();
            baClient2.releaseClient();
            flClient1.releaseClient();
            flClient2.releaseClient();
        }
    }

    // /**
    //  * Verify the following:
    //  * <OL>
    //  * <LI>Start the server with a default server config file (server.xml)
    //  * </OL>
    //  * <P>Expected results:
    //  * <OL>
    //  * <LI>The server starts successfully
    //  * <LI>The server successfully generated a default LTPA key file if the LTPA key file does not exist.
    //  * <LI>A LTPA token can be created
    //  * </OL>
    //  */
    // @Test
    // public void genDefaultLTPAKeyFile() throws Exception {
    //     startServerWithConfigFileAndLog(DEFAULT_SERVER_XML, "genDefaultLTPAKeyFile.log");
    //     assertFeatureCompleteWithKeysGeneratedAndTestApp(DEFAULT_KEY_PATH);
    //     assertTokenCanBeCreated();
    //     assertFileWasCreated(DEFAULT_KEY_PATH);
    // }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 5
     * <LI>Start the server with a default ltpa.keys file
     * <LI>Attempt to access a simple servlet configured for basic auth1 with ltpa cookie
     * <LI>Rename the ltpa.keys file to validation1.keys
     * <LI>Retry access to the simple servlet configured for basic auth1 with ltpa cookie
     * <LI>Check for the creation of a new ltpa.keys file
     * <LI>Attempt to access a new simple servlet configured for basic auth2 with ltpa cookie
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5
     * <LI>Server starts successfully, and a default ltpa.keys file is automatically generated
     * <LI>Successful authentication to simple servlet
     * <LI>The ltpa.keys file is renamed to validation1.keys
     * <LI>Continued authentication to simple servlet; server is not restarted and does not need to login again
     * <LI>A new ltpa.keys file is created
     * <LI>Successful authentication to simple servlet with new ltpa2 cookie
     * </OL>
     */
    @Test
    public void testLTPAFileCreation_monitorDirectory_true_monitorInterval_5() throws Exception {
        // get the server configuration
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        boolean configurationUpdateNeeded = false;

        // Set MonitorDirectory to true, and MonitorInterval to 5
        configurationUpdateNeeded = setLTPAMonitorDirectoryElement(ltpa, "true");
        configurationUpdateNeeded = setLTPAMonitorIntervalElement(ltpa, "5") || configurationUpdateNeeded;

        //apply server configuration update if needed
        if (configurationUpdateNeeded)
            updateConfigDynamically(server, serverConfiguration, true);

        // Assert that a default ltpa.keys file is generated
        assertFileWasCreated(DEFAULT_KEY_PATH);

        // Initial login to simple servlet for form login1 and form login2
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from each login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie 1.", cookie1);

        // Rename the ltpa.keys file to validation1.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Assert that a new ltpa.keys file was created
        assertFileWasCreated(DEFAULT_KEY_PATH);

        // Assert that the new cookie is different from the old cookie
        String response3 = flClient2.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);
        String cookie2 = flClient2.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie 2.", cookie2);
        assertFalse("The new cookie is the same as the old cookie. Cookie1 = " + cookie1 + ". Cookie2 = " + cookie2 + ".",
                    cookie1.equals(cookie2));
    }

    // /**
    //  * Verify the following:
    //  * <OL>
    //  * <LI>Set MonitorDirectory to true
    //  * <LI>Start the server with a default ltpa.keys file
    //  * <LI>Attempt to access a simple servlet configured for basic auth1 with ltap1 cookie
    //  * <LI>Update the server.xml file to include both old and new LTPA keys
    //  * <LI>Attempt to access a simple servlet configured for basic auth1 with existing ltap1 cookie
    //  * <LI>Attempt to access a simple servlet configured for basic auth2 with ltap2 cookie
    //  * </OL>
    //  * <P>Expected Results:
    //  * <OL>
    //  * <LI>MonitorDirectory is set to true
    //  * <LI>Server starts successfully
    //  * <LI>Successful authentication to simple servlet
    //  * <LI>Successful update of server.xml file
    //  * <LI>Continued authentication to simple servlet; server is not restarted and does not need to login again
    //  * <LI>Successful authentication to simple servlet; new key is used for verification
    //  * </OL>
    //  */
    // @Test
    // public void testMultipleLTPAKeyFilesSupport_monitorDirectory_true() throws Exception {
    //     // Set MonitorDirectory to true
    //     setLTPAMonitorDirectoryElement(server, "true");

    //     // Start the server with a default ltpa.keys file
    //     startServerWithConfigFileAndLog(DEFAULT_SERVER_XML, "genDefaultLTPAKeyFile.log");
    //     assertFeatureCompleteWithKeysGeneratedAndTestApp(DEFAULT_KEY_PATH);

    //     // Attempt to access a simple servlet configured for basic auth1 with ltap1 cookie

    // }

    // /**
    //  * Verify the following:
    //  * <OL>
    //  * <LI>Set MonitorDirectory to false
    //  * <LI>Start the server with a default ltpa.keys file
    //  * <LI>Attempt to access a simple servlet configured for basic auth1 with ltap1 cookie
    //  * <LI>Update the server.xml file to include both old and new LTPA keys
    //  * <LI>Attempt to access a simple servlet configured for basic auth1 with existing ltap1 cookie
    //  * <LI>Attempt to access a simple servlet configured for basic auth2 with ltap2 cookie
    //  * </OL>
    //  * <P>Expected Results:
    //  * <OL>
    //  * <LI>MonitorDirectory is set to false
    //  * <LI>Server starts successfully
    //  * <LI>Successful authentication to simple servlet
    //  * <LI>Successful update of server.xml file
    //  * <LI>Unsuccessful authentication to simple servlet; server is not restarted and user does need to login again
    //  * <LI>Successful authentication to simple servlet; new key is used for verification
    //  * </OL>
    //  */
    // @Test
    // public void testMultipleLTPAKeyFilesSupport_monitorDirectory_false() throws Exception {
    //     // Set MonitorDirectory to false
    //     setLTPAMonitorDirectoryElement(server, "false");

    //     // Start the server with a default ltpa.keys file
    //     startServerWithConfigFileAndLog(DEFAULT_SERVER_XML, "genDefaultLTPAKeyFile.log");
    //     assertFeatureCompleteWithKeysGeneratedAndTestApp(DEFAULT_KEY_PATH);

    //     // Attempt to access a simple servlet configured for basic auth1 with ltap1 cookie

    // }

    // /**
    //  * Verify the following:
    //  * <OL>
    //  * <LI>Attempt to access a simple servlet configured for basic auth1 with a valid userId (mnagerUser) and password.
    //  * <LI>Get the cookie back from the session
    //  * <LI>Complete a key rotation, and add key 2 to ltpa1.keys
    //  * <LI>Attempt to access a simple servlet configured for basic auth1 with the cookie corresponding to the first key
    //  * <LI>Verify original LTPA key is still in the ltpa1.keys file
    //  * </OL>
    //  * <P>Expected Results:
    //  * <OL>
    //  * <LI>Successful authentication to simple servlet
    //  * <LI>Successful retrieval of cookie
    //  * <LI>Successful key rotation, and addition of key 2 to ltpa1.keys
    //  * <LI>Successful authentication to simple servlet, the user is still authenticated
    //  * <LI>Successful verification of original LTPA key in ltpa1.keys file
    //  * </OL>
    //  */
    // @Mode(TestMode.LITE)
    // @Test
    // public void testSuccessfulAuthenticationWithOriginalKeys() {

    //     String response = baClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
    //     assertNotNull(response);

    //     // Get the cookie back from the session
    //     String cookie = baClient1.getCookieFromLastLogin();
    //     assertNotNull(cookie);

    //     // Complete a key rotation, and add key 2 to ltpa1.keys
    //     //server.rotateLTPAKeys();

    //     /// Now try to access the servlet with the cookie corresponding to the first key
    //     response = baClient1.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie);
    //     assertNotNull(response);

    //     // Verify original LTPA key is still in the ltpa1.keys file

    // }

    // /**
    //  * Verify the following:
    //  * <OL>
    //  * <LI>Attempt to access a simple servlet configured for basic auth1 with a valid userId (mnagerUser) and password.
    //  * <LI>Get the cookie back from the session
    //  * <LI>Complete a key rotation, and add key 2 to ltpa1.keys
    //  * <LI>Attempt login with a new user after key rotation
    //  * <LI>Verify the new user is authenticated and provided a new cookie from LTPA key 2
    //  * </OL>
    //  * <P>Expected Results:
    //  * <OL>
    //  * <LI>Successful authentication to simple servlet
    //  * <LI>Successful retrieval of cookie
    //  * <LI>Successful key rotation, and addition of key 2 to ltpa1.keys
    //  * <LI>Successful authentication to simple servlet
    //  * <LI>Successful retrieval of cookie from LTPA key 2. LTPA cookie from key 1 is only used for verification but not for new authentication
    //  * </OL>
    //  */
    // @SuppressWarnings("restriction")
    // @Mode(TestMode.LITE)
    // @Test
    // public void testNewUserAuthentication() {

    //     String response = baClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
    //     assertNotNull(response);

    //     // Get the cookie back from the session
    //     String cookie1 = baClient1.getCookieFromLastLogin();
    //     assertNotNull(cookie1);

    //     // Complete a key rotation, and add key 2 to ltpa1.keys
    //     //server.rotateLTPAKeys();

    //     // Attempt login with a new user after key rotation
    //     response = baClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, validUser, validPassword);
    //     assertNotNull(response);

    //     // Verify the new user is authenticated and provided a new cookie from LTPA key 2
    //     String cookie2 = baClient1.getCookieFromLastLogin();
    //     assertNotNull(cookie2);
    //     //assertTrue(cookie2.contains("LtpaToken2"));

    //     // Assert that cookie1 and cookie2 are different
    //     assertFalse(cookie1.equals(cookie2));

    //     // Print both values
    //     Log.info(thisClass, "testNewUserAuthentication", "Cookie: " + cookie1);
    //     Log.info(thisClass, "testNewUserAuthentication", "Cookie: " + cookie2);
    // }

    // /**
    //  * Verify the following:
    //  * <OL>
    //  * <LI>Set ltpa expiration to 3 second
    //  * <LI>Intialize a session with a simple servlet configured for basic auth1 with a valid userId (mnagerUser) and password.
    //  * <LI>Get the cookie back from the session
    //  * <LI>Wait for the key to expire
    //  * <LI>Attempt to access a simple servlet configured for basic auth1 with the cookie corresponding to the first key
    //  * </OL>
    //  * <P>Expected Results:
    //  * <OL>
    //  * <LI>Successful update to the ltpa expiration in the server xml configuration
    //  * <LI>Successful authentication to simple servlet
    //  * <LI>Successful retrieval of cookie
    //  * <LI>Successful expiration of key, and removal of key 1 from ltpa1.keys
    //  * <LI>Unsuccessful authentication to simple servlet with cookie, the user is denied access
    //  * </OL>
    //  */
    // @SuppressWarnings("restriction")
    // @Mode(TestMode.LITE)
    // @Test
    // public void testExpiredKeyForcesReauthentication() {
    //     ;

    //     // Set ltpa expiration to 3 second
    //     //server.setLTPAExpiration(3);

    //     // Intialize a session with a simple servlet configured for basic auth1 with a valid userId (mnagerUser) and password.
    //     String response = baClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
    //     assertNotNull(response);

    //     // Get the cookie back from the session
    //     String cookie = baClient1.getCookieFromLastLogin();
    //     assertNotNull(cookie);

    //     // Wait for the key to expire
    //     try {
    //         Thread.sleep(3000);
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }

    //     // Attempt to access a simple servlet configured for basic auth1 with the cookie corresponding to the first key
    //     assertTrue("The expired LTPA Cookie should not be granted access to the servlet",
    //                baClient1.accessProtectedServletWithUnauthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie));
    // }

    // /**
    //  * Verify the following:
    //  * <OL>
    //  * <LI>Set this new feature off
    //  * <LI>Initialize a session with a simple servlet configured for basic auth1 with a valid userId (mnagerUser) and password.
    //  * <LI>Get the cookie back from the session
    //  * <LI>Complete a key rotation, and add key 2 to ltpa1.keys
    //  * <LI>Attempt to access a simple servlet configured for basic auth1 with the cookie corresponding to the first key
    //  * <LI>Verify the user is forced to reauthenticate
    //  * </OL>
    //  * <P>Expected Results:
    //  * <OL>
    //  * <LI>Successful update to the server xml configuration
    //  * <LI>Successful authentication to simple servlet
    //  * <LI>Successful retrieval of cookie
    //  * <LI>Successful key rotation, and replacement of key 2 to ltpa1.keys file
    //  * <LI>Unsuccessful authentication to simple servlet with cookie, the user is forced to reauthenticate
    //  * </OL>
    //  */
    // @SuppressWarnings("restriction")
    // @Mode(TestMode.LITE)
    // @Test
    // public void testAuthenticationFailureAfterKeyReplacement() {

    //     // Initialize a session with a simple servlet configured for basic auth1 with a valid userId (mnagerUser) and password.
    //     String response = baClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
    //     assertNotNull(response);

    //     // Get the cookie back from the session
    //     String cookie = baClient1.getCookieFromLastLogin();
    //     assertNotNull(cookie);

    //     // Complete a key rotation, and add key 2 to ltpa1.keys
    //     //server.rotateLTPAKeys();

    //     // Attempt to access a simple servlet configured for basic auth1 with the cookie corresponding to the first key
    //     assertTrue("Without multipleLTPAKeys feature enabled, afte key rotation, access should not be granted access to the servlet with the old cookie",
    //                baClient1.accessProtectedServletWithUnauthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie));
    // }

    /**
     * Helper method to delete an existing ltpa keys file if it exists
     */
    private void deleteExistingLTPAKeysFiles() throws Exception {
        deleteFileIfExists(DEFAULT_KEY_PATH);
        deleteFileIfExists(VALIDATION_KEYS_PATH);
    }

    /**
     * Helper method to rename the ltpa.keys file if it exists to validation1.keys
     */
    private void renameLTPAKeysFile() throws Exception {
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEYS_PATH);
    }

    // Function to set the monitorDirectory to true or false
    public boolean setLTPAMonitorDirectoryElement(LTPA ltpa, String value) {
        if (!ltpa.monitorDirectory.equals(value)) {
            ltpa.monitorDirectory = value;
            return true; //config update is needed
        }
        return false; //config update is not needed;
    }

    // Function to configure monitorInterval to a specific value
    public boolean setLTPAMonitorIntervalElement(LTPA ltpa, String value) {
        if (!ltpa.monitorInterval.equals(value)) {
            ltpa.monitorInterval = value;
            return true; //config update is needed
        }
        return false; //config update is not needed;
    }

    // Function to update the server configuration dynamically
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config, boolean waitForAppToStart) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());

        server.updateServerConfiguration(config);
        //CWWKG0017I: The server configuration was successfully updated in {0} seconds.
        //CWWKG0018I: The server configuration was not updated. No functional changes were detected.
        String logLine = server.waitForStringInLogUsingMark("CWWKG001[7-8]I", 20000);
        if (waitForAppToStart && !logLine.contains("CWWKG0018I")) {
            server.waitForStringInLogUsingMark("CWWKZ0003I", 20000); //CWWKZ0003I: The application userRegistry updated in 0.020 seconds.
        }
    }

    /**
     * Delete the file if it exists. If we can't delete it, then
     * throw an exception as we need to be able to delete these files.
     *
     * @param filePath
     *
     * @throws Exception
     */
    private static void deleteFileIfExists(String filePath) throws Exception {
        if (fileExists(filePath)) {
            if (!server.getFileFromLibertyServerRoot(filePath).delete()) {
                throw new Exception("Delete action failed for file: " + filePath);
            }

            // Double check to make sure the file is gone
            if (fileExists(filePath))
                throw new Exception("Unable to delete file: " + filePath);
        }

    }

    /**
     * Rename the file if it exists. If we can't rename it, then
     * throw an exception as we need to be able to rename these files.
     *
     * @param filePath
     *
     * @throws Exception
     */
    private static void renameFileIfExists(String filePath, String newFilePath) throws Exception {
        renameFileIfExists(filePath, newFilePath, true);
    }

    private static void renameFileIfExists(String filePath, String newFilePath, boolean checkFileIsGone) throws Exception {
        Log.info(thisClass, "renameFileIfExists", "\nfilepath: " + filePath + "\nnewFilePath: " + newFilePath);
        if (fileExists(filePath, 2)) {
            Log.info(thisClass, "renameFileIfExists", "file exists, renaming...");
            server.renameLibertyServerRootFile(filePath, newFilePath);

            // Double check to make sure the file is gone
            if (checkFileIsGone && fileExists(filePath, 1))
                throw new Exception("Unable to rename file: " + filePath);
        }
    }

    /**
     * Start the server with the given configuration file and log file.
     */
    private void startServerWithConfigFileAndLog(String configFile, String logFileName) throws Exception {
        server.setServerConfigurationFile(configFile);
        server.startServer(logFileName);
    }

    /**
     * Asserts that the feature update is complete, the LTPA keys are generated,
     * and the test application was installed. Requires info trace.
     */
    private void assertFeatureCompleteWithKeysGeneratedAndTestApp(String generatedLTPAKeysPath) {
        assertApplicationStarted();
        assertFeatureUpdateComplete();
        assertKeysGenerated(generatedLTPAKeysPath);
    }

    private void assertApplicationStarted() {
        assertNotNull("Application ltpaTest does not appear to have started.",
                      server.waitForStringInLog("CWWKZ0001I:.*ltpaTest", 20000));
    }

    private void assertFeatureUpdateComplete() {
        assertNotNull("The app start will cause the token bundle to start. The token bundle did not start.",
                      server.waitForStringInLog("CWWKF0008I:.*", 20000));
    }

    private void assertKeysGenerated(String generatedLTPAKeysPath) {
        assertNotNull("We need to wait for the LTPA keys to be generated at " + generatedLTPAKeysPath + ", but we did not recieve the message",
                      server.waitForStringInLog("CWWKS4104A:.*" + generatedLTPAKeysPath, 20000));
    }

    /**
     * Asserts that a token can be created.
     * It will cause the LTPA keys file to get created if it does not exist.
     */
    private void assertTokenCanBeCreated() throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME);
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            assertNotNull(is);

            String output = read(is);
            assertTrue(output, output.trim().startsWith("Test Passed"));
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    /**
     * Assert that file was created
     */
    private void assertFileWasCreated(String filePath) throws Exception {
        assertTrue(fileExists(filePath));
    }

    /**
     * Check to see if the file exists. We will wait a bit to ensure
     * that the system was not slow to flush the file.
     *
     * @param filePath
     *
     * @return
     */
    private static boolean fileExists(String filePath) throws Exception {
        return fileExists(filePath, 5);
    }

    private static boolean fileExists(String filePath, int numberOfTries) throws Exception {
        boolean exists = false;
        boolean exceptionHasBeenPrinted = false;
        int count = 0;
        do {
            //sleep 2 seconds
            Thread.sleep(20000);
            try {
                exists = server.getFileFromLibertyServerRoot(filePath).exists();
            } catch (Exception e) {
                //the file does not exist if there's an exception
                Log.info(thisClass, "fileExists", "The file does not exist yet, waiting 2s...");
                exists = false;
                //We don't want to print the same exception over and over again... so we'll only print it one time.
                if (!exceptionHasBeenPrinted) {
                    e.printStackTrace();
                    exceptionHasBeenPrinted = true;
                }
            }
            count++;
        }
        //wait up to 10 seconds for the key file to appear
        while ((!exists) && count < numberOfTries);

        return exists;
    }

    private static String read(InputStream in) throws IOException {
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }

    /**
     * Copies a file to the "server/resources/security/" directory
     */
    private static void copyFileToServerResourcesSecurityDir(String sourceFile) throws Exception {
        Log.info(thisClass, "copyFileToServerResourcesSecurityDir", "sourceFile: " + sourceFile);
        String serverRoot = server.getServerRoot();
        String securityResources = serverRoot + "/resources/security";
        server.setServerRoot(securityResources);
        server.copyFileToLibertyServerRoot(sourceFile);
        server.setServerRoot(serverRoot);
    }
}