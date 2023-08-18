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
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;

import componenttest.annotation.AllowedFFDC;
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

    // Initialize a liberty server for form login
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer");

    private static final Class<?> thisClass = LTPAKeyRotationTests.class;

    // Initialize the user
    private static final String validUser = "user1";
    private static final String validPassword = "user1pwd";

    private static final String serverShutdownMessages = "CWWKS4106E";

    // Initialize the FormLogin Clients
    private static final FormLoginClient flClient1 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");
    private static final FormLoginClient flClient2 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin2");

    // Define the paths to the key files
    private static final String DEFAULT_KEY_PATH = "resources/security/ltpa.keys";
    private static final String VALIDATION_KEY1_PATH = "resources/security/validation1.keys";
    private static final String VALIDATION_KEY2_PATH = "resources/security/validation2.keys";
    private static final String VALIDATION_KEY3_PATH = "resources/security/validation3.keys";
    private static final String VALIDATION_KEYS_PATH = "resources/security/";

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
        // Copy validation key file(validation2.keys) to the server
        copyFileToServerResourcesSecurityDir("alternate/validation2.keys");

        server.startServer(true);

        assertNotNull("Featurevalid did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I", 100000));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I", 100000));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I", 100000));
    }

    @After
    public void resetConnection() {
        flClient1.resetClientState();
        flClient2.resetClientState();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer(serverShutdownMessages);
        } finally {
            flClient1.releaseClient();
            flClient2.releaseClient();
        }
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 5.
     * <LI>Start the server with a default ltpa.keys file.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Check for the creation of a new ltpa.keys file.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5.
     * <LI>Server starts successfully, and a default ltpa.keys file is automatically generated.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>Continued authentication to simple servlet; server is not restarted and does not need to login again.
     * <LI>A new ltpa.keys file is created.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created and ltpa cookie2 is different from ltpa cookie1.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testLTPAFileCreationDeletion_monitorDirectory_true_monitorInterval_5() throws Exception {
        // Get the server configuration
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();

        // Check if the configuration needs to be updated
        boolean configurationUpdateNeeded = false;

        // Set MonitorDirectory to true, and MonitorInterval to 5
        configurationUpdateNeeded = setLTPAMonitorDirectoryElement(ltpa, "true") | setLTPAMonitorIntervalElement(ltpa, "5");

        // Apply server configuration update if needed
        if (configurationUpdateNeeded)
            updateConfigDynamically(server, serverConfiguration, true);

        // Assert that a default ltpa.keys file is generated
        assertFileWasCreated(DEFAULT_KEY_PATH);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Assert that a new ltpa.keys file was created otherwise add a final message saying it's intermittent
        assertFileWasCreated(DEFAULT_KEY_PATH);

        // New login to simple servlet for form login2
        String response3 = flClient2.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);
        String cookie2 = flClient2.getCookieFromLastLogin();

        // Assert that the new cookie is different from the old cookie
        assertNotNull("Expected SSO Cookie 2 is missing.", cookie2);
        assertFalse("The new cookie is the same as the old cookie. Cookie1 = " + cookie1 + ". Cookie2 = " + cookie2 + ".",
                    cookie1.equals(cookie2));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 5.
     * <LI>Start the server with a default ltpa.keys file.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different valid key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5.
     * <LI>Server starts successfully, and a default ltpa.keys file is automatically generated.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key.
     * <LI>Failed authentication to simple servlet.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created and ltpa cookie2 is different from ltpa cookie1.
     * </OL>
     */
    @Test
    public void testLTPAFileReplacement_newValidKey_monitorDirectory_true_monitorInterval_5() throws Exception {
        // Get the server configuration
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        boolean configurationUpdateNeeded = false;

        // Set MonitorDirectory to true, and MonitorInterval to 5
        configurationUpdateNeeded = setLTPAMonitorDirectoryElement(ltpa, "true");
        configurationUpdateNeeded = setLTPAMonitorIntervalElement(ltpa, "5") || configurationUpdateNeeded;

        // Apply server configuration update if needed
        if (configurationUpdateNeeded)
            updateConfigDynamically(server, serverConfiguration, true);

        // Assert that a default ltpa.keys file is generated
        assertFileWasCreated(DEFAULT_KEY_PATH);

        // Initial login to simple servlet for form login1 and form login2
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from each login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Replace the primary key with a different valid key
        renameFileIfExists(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, true);

        // Attempt to access the simple servlet again with the same cookie and assert it fails and the server needs to login again
        assertTrue("An invalid cookie should result in authorization challenge",
                   flClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1));

        // New login to simple servlet for form login2
        String response2 = flClient2.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);
        String cookie2 = flClient2.getCookieFromLastLogin();

        // Assert that the new cookie is different from the old cookie
        assertNotNull("Expected SSO Cookie 2 is missing.", cookie2);
        assertFalse("The new cookie is the same as the old cookie. Cookie1 = " + cookie1 + ". Cookie2 = " + cookie2 + ".",
                    cookie1.equals(cookie2));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 5.
     * <LI>Start the server with a default ltpa.keys file.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different invalid key which has garbage values in the private key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5.
     * <LI>Server starts successfully, and a default ltpa.keys file is automatically generated.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key causing a CWWKS4106E: LTPA configuration error.
     * <LI>Succes authentication to simple servlet since the old cookie is still being used.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileReplacement_newInvalidKey_monitorDirectory_true_monitorInterval_5() throws Exception {
        // Copy validation keys file(validation2.keys and validation3.keys) to the server so that at start-up, the file-monitor works.
        copyFileToServerResourcesSecurityDir("alternate/validation3.keys");

        // Get the server configuration
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        boolean configurationUpdateNeeded = false;

        // Set MonitorDirectory to true, and MonitorInterval to 5
        configurationUpdateNeeded = setLTPAMonitorDirectoryElement(ltpa, "true");
        configurationUpdateNeeded = setLTPAMonitorIntervalElement(ltpa, "5") || configurationUpdateNeeded;

        // Apply server configuration update if needed
        if (configurationUpdateNeeded)
            updateConfigDynamically(server, serverConfiguration, true);

        // Assert that a default ltpa.keys file is generated
        assertFileWasCreated(DEFAULT_KEY_PATH);

        // Initial login to simple servlet for form login1 and form login2
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from each login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Replace the primary key with a different invalid key
        renameFileIfExists(VALIDATION_KEY3_PATH, DEFAULT_KEY_PATH, true);

        // Check for the following exception message in the log
        assertNotNull("Expected LTPA configuration error message not found in the log.",
                      server.waitForStringInLog("CWWKS4106E", 100000));

        // Attempt to access the simple servlet again with the same ltpa cookie1 and assert it works
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Set trace to end default trace to prevent FFDC Exception from being thrown in the next test case
        server.setTraceMarkToEndOfDefaultTrace();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to false, and MonitorInterval to 0.
     * <LI>Start the server with a default ltpa.keys file.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Check for the creation of a new ltpa.keys file.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to false, and MonitorInterval to 0.
     * <LI>Server starts successfully, and a default ltpa.keys file is automatically generated.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>Continued authentication to simple servlet; server is not restarted and does not need to login again.
     * <LI>A new ltpa.keys file is not created.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileCreationDeletion_monitorDirectory_false_monitorInterval_0() throws Exception {
        // Get the server configuration
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        boolean configurationUpdateNeeded = false;

        // Set MonitorDirectory to false, and MonitorInterval to 0
        configurationUpdateNeeded = setLTPAMonitorDirectoryElement(ltpa, "false");
        configurationUpdateNeeded = setLTPAMonitorIntervalElement(ltpa, "0") || configurationUpdateNeeded;

        // Apply server configuration update if needed
        if (configurationUpdateNeeded)
            updateConfigDynamically(server, serverConfiguration, true);

        // Assert that a default ltpa.keys file is generated
        assertFileWasCreated(DEFAULT_KEY_PATH);

        // Initial login to simple servlet for form login1 and form login2
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from each login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Assert that a new ltpa.keys file was not created
        assertFileWasNotCreated(DEFAULT_KEY_PATH);

        // New login to simple servlet for form login2
        String response3 = flClient2.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);
        String cookie2 = flClient2.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 2 is missing.", cookie2);
    }

    // Function to set the monitorDirectory to true or false
    public boolean setLTPAMonitorDirectoryElement(LTPA ltpa, String value) {
        if (!ltpa.monitorDirectory.equals(value)) {
            ltpa.monitorDirectory = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure monitorInterval to a specific value
    public boolean setLTPAMonitorIntervalElement(LTPA ltpa, String value) {
        if (!ltpa.monitorInterval.equals(value)) {
            ltpa.monitorInterval = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to update the server configuration dynamically
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config, boolean waitForAppToStart) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());

        server.updateServerConfiguration(config);
        // CWWKG0017I: The server configuration was successfully updated in {0} seconds.
        // CWWKG0018I: The server configuration was not updated. No functional changes were detected.
        String logLine = server.waitForStringInLogUsingMark("CWWKG001[7-8]I", 20000);
        if (waitForAppToStart && !logLine.contains("CWWKG0018I")) {
            server.waitForStringInLogUsingMark("CWWKZ0003I", 20000); // CWWKZ0003I: The application userRegistry updated in 0.020 seconds.
        }
    }

    /**
     * Rename the file if it exists. If we can't rename it, then
     * throw an exception as we need to be able to rename these files.
     * If checkFileIsGone is true, then we will double check to make
     * sure the file is gone.
     *
     * @param filePath
     * @param newFilePath
     * @param checkFileIsGone
     *
     * @throws Exception
     */
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
     * Assert that file was created otherwise print a message saying it's an intermittent failure
     */
    private void assertFileWasCreated(String filePath) throws Exception {
        assertTrue("The file was not created as expected. If this is an intermittent failure, then increase the wait time.",
                   fileExists(filePath));
    }

    /**
     * Assert that file was not created
     */
    private void assertFileWasNotCreated(String filePath) throws Exception {
        assertFalse(fileExists(filePath));
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

    /**
     * Check to see if the file exists. We will retry a few times to ensure
     * that the system was not slow to flush the file.
     *
     * @param filePath
     * @param numberOfTries
     *
     * @return
     */
    private static boolean fileExists(String filePath, int numberOfTries) throws Exception {
        boolean exists = false;
        boolean exceptionHasBeenPrinted = false;
        int count = 0;
        do {
            // Sleep 2 seconds
            Thread.sleep(2000);
            try {
                exists = server.getFileFromLibertyServerRoot(filePath).exists();
            } catch (Exception e) {
                // The file does not exist if there's an exception
                Log.info(thisClass, "fileExists", "The file does not exist yet, waiting 2s...");
                exists = false;
                // We don't want to print the same exception over and over again... so we'll only print it one time.
                if (!exceptionHasBeenPrinted) {
                    e.printStackTrace();
                    exceptionHasBeenPrinted = true;
                }
            }
            count++;
        }
        // Wait up to 10 seconds for the key file to appear
        while ((!exists) && count < numberOfTries);

        return exists;
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