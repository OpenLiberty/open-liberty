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

import java.time.Instant;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.LTPA;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ValidationKeys;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
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

    private static final String[] serverShutdownMessages = { "CWWKG0058E", "CWWKG0083W", "CWWKS4106E", "CWWKS4109W", "CWWKS4110E", "CWWKS4111E", "CWWKS4112E", "CWWKS4113W",
                                                             "CWWKS1859E" };

    private static final String validationKeyPassword = "{xor}Lz4sLCgwLTs=";

    // Initialize the FormLogin Clients
    private static final FormLoginClient flClient1 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");
    private static final FormLoginClient flClient2 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin2");

    // Define the paths to the key files
    private static final String DEFAULT_KEY_PATH = "resources/security/ltpa.keys";
    private static final String VALIDATION_KEY1_PATH = "resources/security/validation1.keys";
    private static final String VALIDATION_KEY2_PATH = "resources/security/validation2.keys";
    private static final String VALIDATION_KEY3_PATH = "resources/security/validation3.keys";
    private static final String VALIDATION_KEYS_PATH = "resources/security/";

    private static RemoteFile messagesLogFile = null;

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        // Function to make it easier to see when each test starts and ends
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nEntering test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nExiting test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }

    };

    @BeforeClass
    public static void setUp() throws Exception {
        // Copy validation key file (validation1.keys) to the server
        copyFileToServerResourcesSecurityDir("alternate/validation1.keys");

        server.startServer(true);

        assertNotNull("Featurevalid did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));
        // Wait for the LTPA configuration to be ready
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I"));

        messagesLogFile = server.getDefaultLogFile();
    }

    @Before
    public void moveLogMark() throws Exception {
        server.setMarkToEndOfLog(messagesLogFile);
    }

    @After
    public void after() throws Exception {
        resetConnection();
        resetServer();
    }

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
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Check for the creation of a new ltpa.keys file.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>Continued authentication to simple servlet; server is not restarted and does not need to login again.
     * <LI>A new ltpa.keys file is created.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created and ltpa cookie2 is different from ltpa cookie1.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileCreationDeletion_monitorDirectory_true_monitorInterval_5() throws Exception {
        // Configure the server
        configureServer("true", "5", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Assert that a new ltpa.keys file was created
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
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different valid key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key.
     * <LI>Failed authentication to simple servlet.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created and ltpa cookie2 is different from ltpa cookie1.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileReplacement_newValidKey_monitorDirectory_true_monitorInterval_5() throws Exception {
        // Configure the server
        configureServer("true", "5", true);

        // Copy validation key file (validation2.keys) to the server
        copyFileToServerResourcesSecurityDir("alternate/validation2.keys");

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Replace the primary key with a different valid key
        renameFileIfExists(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, false);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

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
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different invalid key which has garbage values in the private key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key causing a CWWKS4106E: LTPA configuration error.
     * <LI>Successful authentication to simple servlet since the old cookie is still being used.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileReplacement_newInvalidKey_monitorDirectory_true_monitorInterval_5() throws Exception {
        // Configure the server
        configureServer("true", "5", true);

        // Copy validation keys file(validation3.keys) to the server. This file has garbage values in the private key.
        copyFileToServerResourcesSecurityDir("alternate/validation3.keys");

        // Check for the following exception message in the log
        assertNotNull("Expected LTPA configuration error message not found in the log.",
                      server.waitForStringInLog("CWWKS4106E", 5000));

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Replace the primary key with a different invalid key
        renameFileIfExists(VALIDATION_KEY3_PATH, DEFAULT_KEY_PATH, true);

        // Check for the following exception message in the log
        assertNotNull("Expected LTPA configuration error message not found in the log.",
                      server.waitForStringInLog("CWWKS4106E", 5000));

        // Attempt to access the simple servlet again with the same ltpa cookie1 and assert it works
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to false, and MonitorInterval to 0.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Check for the creation of a new ltpa.keys file.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to false, and MonitorInterval to 0.
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
        // Configure the server
        configureServer("false", "0", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
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

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to false, and MonitorInterval to 0.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different valid key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to false, and MonitorInterval to 0.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key.
     * <LI>Successful authentication to simple servlet, since the server still uses the old key without file monitoring.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileReplacement_newValidKey_monitorDirectory_false_monitorInterval_0() throws Exception {
        // Configure the server
        configureServer("false", "0", true);

        // Copy validation key file (validation2.keys) to the server
        copyFileToServerResourcesSecurityDir("alternate/validation2.keys");

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Replace the primary key with a different valid key
        renameFileIfExists(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, true);

        // Attempt to access the simple servlet again with the same cookie and assert it works
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // New login to simple servlet for form login2
        String response3 = flClient2.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);
        String cookie2 = flClient2.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 2 is missing.", cookie2);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to false, and MonitorInterval to 5.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Check for the creation of a new ltpa.keys file.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to false, and MonitorInterval to 5.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>Continued authentication to simple servlet; server is not restarted and does not need to login again.
     * <LI>A new ltpa.keys file is created.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created and ltpa cookie2 is different from ltpa cookie1.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileCreationDeletion_monitorDirectory_false_monitorInterval_5() throws Exception {
        // Configure the server
        configureServer("false", "5", false);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Assert that a new ltpa.keys file was created
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
     * <LI>Set MonitorDirectory to false, and MonitorInterval to 5.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different valid key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to false, and MonitorInterval to 5.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key.
     * <LI>Failed authentication to simple servlet.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created and ltpa cookie2 is different from ltpa cookie1.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileReplacement_newValidKey_monitorDirectory_false_monitorInterval_5() throws Exception {
        // Configure the server
        configureServer("false", "5", true);

        // Copy validation key file (validation2.keys) to the server
        copyFileToServerResourcesSecurityDir("alternate/validation2.keys");

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Replace the primary key with a different valid key
        renameFileIfExists(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, true);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

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
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 0.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Check for the creation of a new ltpa.keys file.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 0.
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
    public void testLTPAFileCreationDeletion_monitorDirectory_true_monitorInterval_0() throws Exception {
        // Configure the server
        configureServer("true", "0", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
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

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 0.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different valid key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 0.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key.
     * <LI>Successful authentication to simple servlet, since the server still uses the old key without file monitoring.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileReplacement_newValidKey_monitorDirectory_true_monitorInterval_0() throws Exception {
        // Configure the server
        configureServer("true", "0", true, false);

        // Wait for configuration error message to be logged
        assertNotNull("Expected LTPA configuration error message not found in the log.",
                      server.waitForStringInLog("CWWKS4113W", 5000));

        // Copy validation key file (validation2.keys) to the server
        copyFileToServerResourcesSecurityDir("alternate/validation2.keys");

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Replace the primary key with a different valid key
        renameFileIfExists(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, true);

        // Attempt to access the simple servlet again with the same cookie and assert it works
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // New login to simple servlet for form login2
        String response3 = flClient2.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);
        String cookie2 = flClient2.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 2 is missing.", cookie2);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 5.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Set fileName to null to make it not configured in the validation keys element.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set fileName value to empty string.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set fileName value to a non-existent file.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set fileName's path to a different path than the default ltpa.keys file.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set fileName to point to a malformed/invalid keys file.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set fileName to point to a valid keys file.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>The fileName attribute is not configured in the validation keys element.
     * <LI>Exception is thrown in the logs since fileName is a required attribute.
     * <LI>The fileName attribute is set to empty string.
     * <LI>Exception is thrown in the logs since fileName is a required attribute.
     * <LI>The fileName attribute is set to a non-existent file.
     * <LI>Exception is thrown in the logs since the file does not exist
     * <LI>The fileName attribute's path is set to a different path than the default ltpa.keys file.
     * <LI>Exception is thrown in the logs since the file does not exist
     * <LI>The fileName attribute is set to point to a malformed/invalid keys file.
     * <LI>Exception is thrown in the logs since the file is invalid. CWWKS4106E: LTPA configuration error.
     * <LI>The fileName attribute is set to point to a valid keys file.
     * <LI>Successful authentication to simple servlet.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testValidationKeys_fileNameAttribute() throws Exception {
        // Configure the server
        configureServer("true", "5", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys.
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Set fileName to null to make it not configured in the validation keys element.
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAValidationKeyFileNameElement(ltpa, null);
        updateConfigDynamically(server, serverConfiguration);

        // Exceptions are thrown in the logs since fileName is a required attribute
        assertNotNull("Expected fileName is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKG0058E", 5000));

        assertNotNull("Expected fileName is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKS4111E", 5000));

        // Set fileName value to empty string
        configurationUpdateNeeded = setLTPAValidationKeyFileNameElement(ltpa, "");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since fileName is a required attribute
        assertNotNull("Expected fileName is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKS4111E", 5000));

        // Set fileName value to a non-existent file
        configurationUpdateNeeded = setLTPAValidationKeyFileNameElement(ltpa, "nonExistentFile.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since the file does not exist
        assertNotNull("Expected key file does not exist exception not found in the log.",
                      server.waitForStringInLog("CWWKS4112E", 5000));

        // Set fileName's path to a different path than the default ltpa.keys file
        configurationUpdateNeeded = setLTPAValidationKeyFileNameElement(ltpa, "alternate/validation1.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since the file does not exist
        assertNotNull("Expected key file does not exist exception not found in the log.",
                      server.waitForStringInLog("CWWKS4112E", 5000));

        // Copy validation key file (validation3.keys) to the server.
        copyFileToServerResourcesSecurityDir("alternate/validation3.keys");

        // Set fileName to point to a malformed/invalid keys file
        configurationUpdateNeeded = setLTPAValidationKeyFileNameElement(ltpa, "validation3.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since the file is invalid. CWWKS4106E: LTPA configuration error.
        assertNotNull("Expected invalid keys file exception not found in the log.",
                      server.waitForStringInLog("CWWKS4106E", 5000));

        // Delete the validation3.keys file
        deleteFileIfExists(VALIDATION_KEY3_PATH, true);

        // Set fileName back to the default ltpa.keys file
        configurationUpdateNeeded = setLTPAValidationKeyFileNameElement(ltpa, "validation1.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Successful authentication to simple servlet
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 5.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Set password to null to make it not configured in the validation keys element.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set password value to empty string.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set password value to an incorrect password.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set password value to a correct password.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>The password attribute is not configured in the validation keys element.
     * <LI>Exception is thrown in the logs since the validation key password must match the primary key password.
     * <LI>The password attribute is set to empty string.
     * <LI>Exception is thrown in the logs since the validation key password must match the primary key password.
     * <LI>The password attribute is set to an incorrect password.
     * <LI>Exception is thrown in the logs since the validation key password must match the primary key password.
     * <LI>The password attribute is set to a correct password.
     * <LI>Continued authentication to simple servlet.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "javax.crypto.BadPaddingException", "java.lang.IllegalArgumentException", "java.lang.NullPointerException" })
    public void testValidationKeys_passwordAttribute() throws Exception {
        // Configure the server
        configureServer("true", "5", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys.
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Set password value to null
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAValidationKeyPasswordElement(ltpa, null);
        updateConfigDynamically(server, serverConfiguration);

        // Exceptions are thrown in the logs since the validation key password is a required attribute if ltpa element has password configured
        assertNotNull("Expected password is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKG0058E", 5000));

        assertNotNull("Expected password is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKS4111E", 5000));

        // Set password value to empty string
        configurationUpdateNeeded = setLTPAValidationKeyPasswordElement(ltpa, "");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since the validation key password is a required attribute if ltpa element has password configured
        assertNotNull("Expected password is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKS4111E", 5000));

        // Set password value to an incorrect password
        configurationUpdateNeeded = setLTPAValidationKeyPasswordElement(ltpa, "{xor}incorrectPassword=");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since the validation key password must match the primary key password
        assertNotNull("Expected problems during password decryption encountered exception not found in the log.",
                      server.waitForStringInLog("CWWKS1859E", 5000));

        // Set password value to a correct password
        configurationUpdateNeeded = setLTPAValidationKeyPasswordElement(ltpa, validationKeyPassword);
        updateConfigDynamically(server, serverConfiguration);

        // Successful authentication to simple servlet
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 5.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Set notUseAfterDate to null to make it not configured in the validation keys element.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set notUseAfterDate value to empty string.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set notUseAfterDate value to an invalid date string.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set notUseAfterDate value to a date string in the past.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set notUseAfterDate value to a valid date string in the future.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>The notUseAfterDate attribute is not configured in the validation keys element.
     * <LI>Continued authentication to simple servlet; the element is not required to be configured.
     * <LI>The notUseAfterDate attribute is set to empty string.
     * <LI>Exception is thrown in the logs for the empty date string.
     * <LI>The notUseAfterDate attribute is set to an invalid date string.
     * <LI>Exception is thrown in the logs for the invalid date string.
     * <LI>The notUseAfterDate attribute is set to a date string in the past.
     * <LI>Exception is thrown in the logs for the expired date string.
     * <LI>The notUseAfterDate attribute is set to a valid date string in the future.
     * <LI>Successful authentication to simple servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC({ "java.time.format.DateTimeParseException" })
    public void testValidationKeys_notUseAfterDateAttribute() throws Exception {
        // Configure the server
        configureServer("true", "5", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys.
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Set notUseAfterDate value to null
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAValidationKeyNotUseAfterDateElement(ltpa, null);
        updateConfigDynamically(server, serverConfiguration);

        // Continued authentication to simple servlet; the element is not required to be configured
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Set notUseAfterDate value to empty string
        configurationUpdateNeeded = setLTPAValidationKeyNotUseAfterDateElement(ltpa, "");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs for the empty date string
        assertNotNull("Expected invalid date format exception not found in the log.",
                      server.waitForStringInLog("CWWKS4110E", 5000));

        // Set notUseAfterDate value to an invalid date string
        configurationUpdateNeeded = setLTPAValidationKeyNotUseAfterDateElement(ltpa, "2023-18T18:08:35Z");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs for the invalid date string
        assertNotNull("Expected invalid date format exception not found in the log.",
                      server.waitForStringInLog("CWWKS4110E", 5000));

        // Set notUseAfterDate value to a date string in the past
        configurationUpdateNeeded = setLTPAValidationKeyNotUseAfterDateElement(ltpa, "2023-01-01T00:00:00Z");
        updateConfigDynamically(server, serverConfiguration);

        // Message is thrown in the logs for a expired date string
        assertNotNull("Expected expired date exception not found in the log.",
                      server.waitForStringInLog("CWWKS4109W", 5000));

        // Set notUseAfterDate value to a valid date string in the future
        configurationUpdateNeeded = setLTPAValidationKeyNotUseAfterDateElement(ltpa, "2099-01-01T00:00:00Z");
        updateConfigDynamically(server, serverConfiguration);

        // Successful authentication to simple servlet
        String response3 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to false, and MonitorInterval to 5.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Add a new validation keys element with a fileName attribute as "validation2.keys".
     * <LI>Rename the ltpa.keys file to validation2.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 0.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Delete the validation2.keys file.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to false, and MonitorInterval to 5.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>A new validation keys element is added with a fileName attribute as "validation2.keys".
     * <LI>The ltpa.keys file is renamed to validation2.keys.
     * <LI>Successful authentication to simple servlet.
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 0.
     * <LI>Successful authentication to simple servlet.
     * <LI>The validation2.keys file is deleted.
     * <LI>Successful authentication to simple servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testConfiguredValidationKeys() throws Exception {
        // Configure the server
        configureServer("false", "0", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Add a new validation keys element with a fileName attribute as "validation2.keys"
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        ConfigElementList<ValidationKeys> validationKeys = ltpa.getValidationKeys();
        ValidationKeys validationKey = new ValidationKeys();
        validationKey.fileName = "validation2.keys";
        validationKey.password = "{xor}Lz4sLCgwLTs=";
        validationKey.notUseAfterDate = "2099-01-01T00:00:00Z";
        validationKeys.add(validationKey);
        updateConfigDynamically(server, serverConfiguration);

        // Rename the ltpa.keys file to validation2.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY2_PATH, false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Set MonitorDirectory to true, and MonitorInterval to 0
        configureServer("true", "0", true);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response3 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Delete the validation2.keys file
        deleteFileIfExists(VALIDATION_KEY2_PATH, true);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response4 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Delete the validation2.keys element
        validationKeys.remove(validationKey);
        updateConfigDynamically(server, serverConfiguration);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 0.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Add a new validation keys element with a fileName attribute as "validation2.keys" and notUseAfterDate attribute as current time + 10 second.
     * <LI>Rename the ltpa.keys file to validation2.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Wait for 10 seconds.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 0.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>A new validation keys element is added with a fileName attribute as "validation2.keys" and notUseAfterDate attribute as current time + 10 second.
     * <LI>The ltpa.keys file is renamed to validation2.keys.
     * <LI>Successful authentication to simple servlet.
     * <LI>The notUseAfterDate attribute is expired.
     * <LI>Failed authentication to simple servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testNotUseAfterDate_expiringAfterConfigurationAndUsage() throws Exception {
        // Configure the server
        configureServer("true", "0", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation2.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY2_PATH, false);

        // Configure the time of expiry as 10 seconds from present time
        String currentTime = Instant.now().toString();
        String expiryTime = Instant.now().plusSeconds(10).toString();

        // Add a new validation keys element with a fileName attribute as "validation2.keys"
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        ConfigElementList<ValidationKeys> validationKeys = ltpa.getValidationKeys();
        ValidationKeys validationKey = new ValidationKeys();
        validationKey.fileName = "validation2.keys";
        validationKey.password = "{xor}Lz4sLCgwLTs=";
        validationKey.notUseAfterDate = expiryTime;
        validationKeys.add(validationKey);
        updateConfigDynamically(server, serverConfiguration);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Wait for 10 seconds
        Thread.sleep(10000);

        // Attempt to access the simple servlet again with the same cookie and assert it fails and the server needs to login again
        assertTrue("An invalid cookie should result in authorization challenge",
                   flClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1));

        // Delete the validation2.keys element
        validationKeys.remove(validationKey);
        updateConfigDynamically(server, serverConfiguration);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 5.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Re-configure the keysFileName to a different relative path and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName to another relative path and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName to a different absolute path and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName to another relative path and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is moved to a different relative path.
     * <LI>Successful authentication to simple servlet.
     * <LI>The ltpa.keys file is moved back to another relative path.
     * <LI>Successful authentication to simple servlet.
     * <LI>The ltpa.keys file is moved to a different absolute path.
     * <LI>Successful authentication to simple servlet.
     * <LI>The ltpa.keys file is moved back to another absolute path.
     * <LI>Successful authentication to simple servlet.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testDifferentDirectoriesForPrimaryKeys() throws Exception {
        // Configure the server
        configureServer("true", "5", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Re-configure the keysFileName to a different relative path
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "${server.config.dir}/ltpa.keys");       
        updateConfigDynamically(server, serverConfiguration);

        // Move the ltpa.keys file to that location
        moveFileIfExists(DEFAULT_KEY_PATH, "${server.config.dir}/ltpa.keys", false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Re-configure the keysFileName to another relative path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "${server.config.dir}/resources/ltpa.keys");       
        updateConfigDynamically(server, serverConfiguration);

        // Move the ltpa.keys file to that location
        moveFileIfExists("${server.config.dir}/ltpa.keys", "${server.config.dir}/resources/ltpa.keys", false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response3 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Re-configure the keysFileName to a different absolute path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "/com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer/test/ltpa.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Move the ltpa.keys file to that location
        moveFileIfExists("${server.config.dir}/resources/ltpa.keys", "/com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer/test/ltpa.keys", false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response4 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Re-configure the keysFileName to another absolute path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "/com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer/random/ltpa.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Move the ltpa.keys file to that location
        moveFileIfExists("/com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer/random/ltpa.keys", "/com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer/random/ltpa.keys", false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again   
        String response5 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 5.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName and validation fileName to a different relative path and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName and validation fileName to another relative path and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName and validation fileName to a different absolute path and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName and validation fileName to another relative path and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 5.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>Successful authentication to simple servlet.
     * <LI>The validation1.keys file is moved to a different relative path.
     * <LI>Successful authentication to simple servlet.
     * <LI>The validation1.keys file is moved back to another relative path.
     * <LI>Successful authentication to simple servlet.
     * <LI>The validation1.keys file is moved to a different absolute path.
     * <LI>Successful authentication to simple servlet.
     * <LI>The validation1.keys file is moved back to another absolute path.
     * <LI>Successful authentication to simple servlet.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testDifferentDirectoriesForValidationKeys() throws Exception {
        // Configure the server
        configureServer("true", "5", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Move the ltpa.keys file to a different relative path
        moveFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Re-configure the keysFileName and validation fileName to a different relative path
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "${server.config.dir}/ltpa.keys") | setLTPAValidationKeyFileNameElement(ltpa, "test1/validation1.keys");       
        updateConfigDynamically(server, serverConfiguration);

        // Move the validation1.keys file to that location
        moveFileIfExists(VALIDATION_KEY1_PATH, "${server.config.dir}/test1/validation1.keys", false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response3 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Re-configure the keysFileName and validation fileName to another relative path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "${server.config.dir}/resources/ltpa.keys") | setLTPAValidationKeyFileNameElement(ltpa, "test2/validation1.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Move the validation2.keys file to that location
        moveFileIfExists("${server.config.dir}/test1/validation1.keys", "${server.config.dir}/resources/test2/validation1.keys", false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response4 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Re-configure the keysFileName and validation fileName to a different absolute path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "/com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer/test/ltpa.keys") | setLTPAValidationKeyFileNameElement(ltpa, "/test3/validation1.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Move the validation.keys file to that location
        moveFileIfExists("${server.config.dir}/resources/test2/validation1.keys", "/com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer/test/test3/validation1.keys", false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response5 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Re-configure the keysFileName and validation fileName to another absolute path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "/com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer/random/ltpa.keys") | setLTPAValidationKeyFileNameElement(ltpa, "/test4/validation1.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Move the validation.keys file to that location
        moveFileIfExists("/com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer/test/test3/validation1.keys", "/com.ibm.ws.security.token.ltpa.fat.ltpaKeyRotationTestServer/random/test4/validation1.keys", false);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response6 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
    }

    public void configureServer(String monitorDirectory, String monitorInterval, Boolean waitForLTPAConfigReadyMessage) throws Exception {
        configureServer(monitorDirectory, monitorInterval, waitForLTPAConfigReadyMessage, true);
    }

    /**
     * Function to do the server configuration for all the tests.
     * Assert that the server has with a default ltpa.keys file.
     *
     * @param monitorDirectory
     * @param monitorInterval
     * @param waitForLTPAConfigReadyMessage
     *
     * @throws Exception
     */
    public void configureServer(String monitorDirectory, String monitorInterval, Boolean waitForLTPAConfigReadyMessage, boolean setLogMarkToEnd) throws Exception {
        // Get the server configuration
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();

        // Check if the configuration needs to be updated
        boolean configurationUpdateNeeded = false;

        // Set MonitorDirectory to true, and MonitorInterval to 0
        configurationUpdateNeeded = setLTPAMonitorDirectoryElement(ltpa, monitorDirectory) | setLTPAMonitorIntervalElement(ltpa, monitorInterval);

        // Apply server configuration update if needed
        if (configurationUpdateNeeded) {
            updateConfigDynamically(server, serverConfiguration);

            if (waitForLTPAConfigReadyMessage) {
                // Wait for the LTPA configuration to be ready after the change
                assertNotNull("Expected LTPA configuration ready message not found in the log.",
                              server.waitForStringInLog("CWWKS4105I", 5000));
            }
        }

        // Assert that a default ltpa.keys file is generated
        assertFileWasCreated(DEFAULT_KEY_PATH);

        if (setLogMarkToEnd)
            server.setMarkToEndOfLog(messagesLogFile);
    }

    // Function to configure the keysFileName to a specific value
    public boolean setLTPAkeysFileNameElement(LTPA ltpa, String value) {
        if (!ltpa.keysFileName.equals(value)) {
            ltpa.keysFileName = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
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

    // Function to configure the fileName for validation keys
    public boolean setLTPAValidationKeyFileNameElement(LTPA ltpa, String value) {
        ConfigElementList<ValidationKeys> validationKeys = ltpa.getValidationKeys();
        ValidationKeys validationKey = validationKeys.get(0);

        // Check if null
        if (validationKey.fileName == null) {
            validationKey.fileName = value;
            return true; // Config update is needed
        }

        if (!validationKey.fileName.equals(value)) {
            validationKey.fileName = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure the password for validation keys
    public boolean setLTPAValidationKeyPasswordElement(LTPA ltpa, String value) {
        ConfigElementList<ValidationKeys> validationKeys = ltpa.getValidationKeys();
        ValidationKeys validationKey = validationKeys.get(0);

        // Check if null
        if (validationKey.password == null) {
            validationKey.password = value;
            return true; // Config update is needed
        }

        if (!validationKey.password.equals(value)) {
            validationKey.password = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure the notUseAfterDate for validation keys
    public boolean setLTPAValidationKeyNotUseAfterDateElement(LTPA ltpa, String value) {
        ConfigElementList<ValidationKeys> validationKeys = ltpa.getValidationKeys();
        ValidationKeys validationKey = validationKeys.get(0);

        // Check if null
        if (validationKey.notUseAfterDate == null) {
            validationKey.notUseAfterDate = value;
            return true; // Config update is needed
        }

        if (!validationKey.notUseAfterDate.equals(value)) {
            validationKey.notUseAfterDate = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to update the server configuration dynamically
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());

        server.updateServerConfiguration(config);
        //CWWKG0017I: The server configuration was successfully updated in {0} seconds.
        //CWWKG0018I: The server configuration was not updated. No functional changes were detected.
        String logLine = server.waitForStringInLogUsingMark("CWWKG001[7-8]I");

        // Wait for feature update to be completed or LTPA configuration to get ready
        Thread.sleep(200);
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
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        if (fileExists(newFilePath, 1)) {
            LibertyFileManager.moveLibertyFile(server.getFileFromLibertyServerRoot(filePath), server.getFileFromLibertyServerRoot(newFilePath));
        } else {
            server.renameLibertyServerRootFile(filePath, newFilePath);
        }

        // Double check to make sure the file is gone
        if (checkFileIsGone && fileExists(filePath, 1))
            throw new Exception("Unable to rename file: " + filePath);
    }
    
    /**
     * Move the file if it exists. If we can't move it, then
     * throw an exception as we need to be able to move these files.
     * If checkFileIsGone is true, then we will double check to make
     * sure the file is gone.
     * 
     * @param filePath
     * @param newFilePath
     * @param checkFileIsGone
     * 
     * @throws Exception
     */
    private static void moveFileIfExists(String filePath, String newFilePath, boolean checkFileIsGone) throws Exception {
        Log.info(thisClass, "moveFileIfExists", "\nfilepath: " + filePath + "\nnewFilePath: " + newFilePath);
        if (fileExists(filePath, 2)) {
            Log.info(thisClass, "moveFileIfExists", "file exists, moving...");
            server.copyFileToLibertyServerRoot(filePath, newFilePath);

            // Double check to make sure the file is gone
            if (checkFileIsGone && fileExists(filePath, 1))
                throw new Exception("Unable to move file: " + filePath);
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
    private static void deleteFileIfExists(String filePath, boolean checkFileIsGone) throws Exception {
        Log.info(thisClass, "deleteFileIfExists", "filepath: " + filePath);
        if (fileExists(filePath, 1)) {
            Log.info(thisClass, "deleteFileIfExists", "file exists, deleting...");
            server.deleteFileFromLibertyServerRoot(filePath);

            // Double check to make sure the file is gone
            if (checkFileIsGone && fileExists(filePath, 1))
                throw new Exception("Unable to delete file: " + filePath);
        }
    }

    /**
     * Assert that file was created otherwise print a message saying it's an intermittent failure.
     *
     * @param filePath
     *
     * @throws Exception
     */
    private void assertFileWasCreated(String filePath) throws Exception {
        assertTrue("The file was not created as expected. If this is an intermittent failure, then increase the wait time.",
                   fileExists(filePath));
    }

    /**
     * Assert that file was not created
     *
     * @param filePath
     *
     * @throws Exception
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
     * @throws Exception
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
     * @throws Exception
     */
    private static boolean fileExists(String filePath, int numberOfTries) throws Exception {
        boolean exists = false;
        boolean exceptionHasBeenPrinted = false;
        int count = 0;
        do {
            // Sleep 2 seconds
            if (count != 0) {
                Thread.sleep(2000);
                Log.info(thisClass, "fileExists", "waiting 2s...");
            }
            try {
                exists = server.getFileFromLibertyServerRoot(filePath).exists();
            } catch (Exception e) {
                // The file does not exist if there's an exception
                Log.info(thisClass, "fileExists", "The file does not exist");
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
     *
     * @param sourceFile
     *
     * @throws Exception
     */
    private static void copyFileToServerResourcesSecurityDir(String sourceFile) throws Exception {
        Log.info(thisClass, "copyFileToServerResourcesSecurityDir", "sourceFile: " + sourceFile);
        String serverRoot = server.getServerRoot();
        String securityResources = serverRoot + "/resources/security";
        server.setServerRoot(securityResources);
        server.copyFileToLibertyServerRoot(sourceFile);
        server.setServerRoot(serverRoot);
    }

    /**
     * Reset the server to the default configuration
     *
     * @throws Exception
     */
    private void resetServer() throws Exception {
        Log.info(thisClass, "resetServer", "entering");
        // We need to put the base config back, otherwise the waits below will timeout on some tests
        configureServer("true", "5", true);

        // Delete all ltpa keys files in the security directory
        deleteFileIfExists(DEFAULT_KEY_PATH, false);
        deleteFileIfExists(VALIDATION_KEY1_PATH, true);
        deleteFileIfExists(VALIDATION_KEY2_PATH, true);
        deleteFileIfExists(VALIDATION_KEY3_PATH, true);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                        server.waitForStringInLog("CWWKS4105I", 5000));

        // Assert that a default ltpa.keys file exists prior to next test case
        assertFileWasCreated(DEFAULT_KEY_PATH);
    }
}