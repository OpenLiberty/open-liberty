/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.Authentication;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.LTPA;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ValidationKeys;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;

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
                                                             "CWWKS4114W", "CWWKS4115W", "CWWKS1859E" };

    private static String validationKeyPassword = "{xor}Lz4sLCgwLTs=";
    private static String validationKeyFIPSPassword = "{xor}CDo9Hgw=";

    // Initialize the FormLogin Clients
    private static final FormLoginClient flClient1 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");
    private static final FormLoginClient flClient2 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin2");
    private static final String DEFAULT_SERVER_XML = "server.xml";
    private static final String DEFAULT_FIPS_SERVER_XML = "serverFIPS.xml";
    // Define the paths to the key files
    private static final String DEFAULT_KEY_PATH = "resources/security/ltpa.keys";
    private static final String CONFIGURED_VALIDATION_KEY1_PATH = "resources/security/configuredValidation1.keys";
    private static final String VALIDATION_KEYS_PATH = "resources/security/";
    private static final String VALIDATION_KEY1_PATH = "resources/security/validation1.keys";
    private static final String VALIDATION_KEY2_PATH = "resources/security/validation2.keys";
    private static final String BAD_SHARED_VALIDATION_KEY1_PATH = "resources/security/validation3.keys";
    private static final String BAD_SHARED_VALIDATION_KEY2_PATH = "resources/security/validation4.keys";
    private static final String BAD_PRIVATE_VALIDATION_KEY1_PATH = "resources/security/validation5.keys";
    private static final String BAD_PRIVATE_VALIDATION_KEY2_PATH = "resources/security/validation6.keys";
    private static final String BAD_PUBLIC_VALIDATION_KEY1_PATH = "resources/security/validation7.keys";
    private static final String BAD_PUBLIC_VALIDATION_KEY2_PATH = "resources/security/validation8.keys";

    // Define the paths to the alternate key files
    private static String ALT_VALIDATION_KEY1_PATH = "alternate/validation1.keys";
    private static String ALT_VALIDATION_KEY2_PATH = "alternate/validation2.keys";
    private static String ALT_VALIDATION_KEY3_PATH = "alternate/validation3.keys";
    private static String ALT_VALIDATION_KEY4_PATH = "alternate/validation4.keys";
    private static String ALT_VALIDATION_KEY5_PATH = "alternate/validation5.keys";
    private static String ALT_VALIDATION_KEY6_PATH = "alternate/validation6.keys";
    private static String ALT_VALIDATION_KEY7_PATH = "alternate/validation7.keys";
    private static String ALT_VALIDATION_KEY8_PATH = "alternate/validation8.keys";
    private static String ALT_CONFIGVALIDATION_KEY1_PATH = "alternate/configuredValidation1.keys";
    private static String SERVER_XML_PATH = "server.xml";

    // Define the paths to the alternate key files
    private static String ALT_FIPS_VALIDATION_KEY1_PATH = "alternateFIPS/validation1.keys";
    private static String ALT_FIPS_VALIDATION_KEY2_PATH = "alternateFIPS/validation2.keys";
    private static String ALT_FIPS_VALIDATION_KEY3_PATH = "alternateFIPS/validation3.keys";
    private static String ALT_FIPS_VALIDATION_KEY4_PATH = "alternateFIPS/validation4.keys";
    private static String ALT_FIPS_VALIDATION_KEY5_PATH = "alternateFIPS/validation5.keys";
    private static String ALT_FIPS_VALIDATION_KEY6_PATH = "alternateFIPS/validation6.keys";
    private static String ALT_FIPS_VALIDATION_KEY7_PATH = "alternateFIPS/validation7.keys";
    private static String ALT_FIPS_VALIDATION_KEY8_PATH = "alternateFIPS/validation8.keys";
    private static String ALT_FIPS_CONFIGVALIDATION_KEY1_PATH = "alternateFIPS/configuredValidation1.keys";
    private static String FIPS_SERVER_XML_PATH = "serverFIPS.xml";

    // Define the paths to the server.xml files
    private static final String relativeDirectory = server.getServerRoot();
    private static final String wlpDirectory = server.getInstallRoot();
    private static final String baseDirectory = server.getInstallRootParent();

    // Define the remote message log file
    private static RemoteFile messagesLogFile = null;

    // Define fipsEnabled
    private static final boolean fipsEnabled;

    static {
        boolean isFipsEnabled = false;
        try {
            isFipsEnabled = server.isFIPS140_3EnabledAndSupported();
        } catch (Exception e) {
            e.printStackTrace();
        }
        fipsEnabled = isFipsEnabled;

        if (fipsEnabled) {
            ALT_VALIDATION_KEY1_PATH = ALT_FIPS_VALIDATION_KEY1_PATH;
            ALT_VALIDATION_KEY2_PATH = ALT_FIPS_VALIDATION_KEY2_PATH;
            ALT_VALIDATION_KEY3_PATH = ALT_FIPS_VALIDATION_KEY3_PATH;
            ALT_VALIDATION_KEY4_PATH = ALT_FIPS_VALIDATION_KEY4_PATH;
            ALT_VALIDATION_KEY5_PATH = ALT_FIPS_VALIDATION_KEY5_PATH;
            ALT_VALIDATION_KEY6_PATH = ALT_FIPS_VALIDATION_KEY6_PATH;
            ALT_VALIDATION_KEY7_PATH = ALT_FIPS_VALIDATION_KEY7_PATH;
            ALT_VALIDATION_KEY8_PATH = ALT_FIPS_VALIDATION_KEY8_PATH;
            ALT_CONFIGVALIDATION_KEY1_PATH = ALT_FIPS_CONFIGVALIDATION_KEY1_PATH;
            validationKeyPassword = validationKeyFIPSPassword;
        }
    }

    @Rule
    public TestRule passwordChecker = new LeakedPasswordChecker(server);

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
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH);

        server.setupForRestConnectorAccess();
        if (fipsEnabled) {
            File fipsServerXml = new File(server.pathToAutoFVTTestFiles + DEFAULT_FIPS_SERVER_XML);
            File serverXml = new File(server.pathToAutoFVTTestFiles + DEFAULT_SERVER_XML);
            Files.move(fipsServerXml.toPath(), serverXml.toPath(), StandardCopyOption.REPLACE_EXISTING);
            server.copyFileToLibertyServerRoot(DEFAULT_SERVER_XML);
        }

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
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Check for the creation of a new ltpa.keys file.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>Continued authentication to simple servlet; server is not restarted and does not need to login again.
     * <LI>A new ltpa.keys file is created.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created and ltpa cookie2 is different from ltpa cookie1.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileCreationDeletion_monitorValidationKeysDir_true_monitorInterval_10() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

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
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different valid key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key.
     * <LI>Failed authentication to simple servlet.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created and ltpa cookie2 is different from ltpa cookie1.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileReplacement_newValidKey_monitorValidationKeysDir_true_monitorInterval_10() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

        // Copy validation key file (validation2.keys) to the server
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY2_PATH);

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
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different invalid key which has garbage values in the Shared key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Replace the primary key with a different invalid key which has swapped values in the Shared key from another validation.keys file.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different Shared key causing a CWWKS4106E: LTPA configuration error.
     * <LI>Successful authentication to simple servlet since the old cookie is still being used.
     * <LI>The ltpa.keys file is replaced with a different Shared key.
     * <LI>Unsuccessful authentication to simple servlet since the decryption fails with the swapped Shared values.
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "javax.crypto.BadPaddingException", "java.lang.IllegalArgumentException", "java.lang.NullPointerException" })
    public void testLTPAFileReplacement_invalidSharedKey_monitorValidationKeysDir_true_monitorInterval_10() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

        // Set cacheEnabled to false to avoid caching the validation keys
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        Authentication auth = serverConfiguration.getAuthentication();
        Boolean configurationUpdateNeeded = setAuthenticationCacheEnabledElement(auth, "false");
        updateConfigDynamically(server, serverConfiguration);

        // Copy validation keys file (validation1.keys) to the server. This file has a valid Shared key.
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Replace the primary key with the valid key
        renameFileIfExists(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, true);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Copy validation keys file (validation3.keys) to the server. This file has garbage values in the Shared key.
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY3_PATH);

        // Wait for the LTPA configuration modified message after the change
        assertNotNull("Expected LTPA configuration modified message not found in the log.",
                      server.waitForStringInLog("CWWKS4107A", 5000));

        // Replace the primary key with a different invalid key
        renameFileIfExists(BAD_SHARED_VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, true);

        // Check for the following exception message in the log
        assertNotNull("Expected LTPA configuration error message not found in the log.",
                      server.waitForStringInLog("CWWKS4106E", 5000));

        // Attempt to access the simple servlet again with the same ltpa cookie1 and assert it works
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Copy validation keys file (validation4.keys) to the server. This file has swapped values in the Shared key from another validation.keys file.
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY4_PATH);

        // Wait for the LTPA configuration modified message after the change
        assertNotNull("Expected LTPA configuration modified message not found in the log.",
                      server.waitForStringInLog("CWWKS4107A", 5000));

        // Replace the primary key with a different invalid key
        renameFileIfExists(BAD_SHARED_VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, true);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Attempt to access the simple servlet again with the same ltpa cookie1 and assert it fails due to the decryption failure
        assertTrue("An invalid cookie should result in authorization challenge",
                   flClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1));

        // Wait for a security token cannot be validated message in the log
        assertNotNull("Expected security token cannot be validated message not found in the log.",
                      server.waitForStringInLog("CWWKS4001I", 5000));

        // Set cacheEnabled back to true
        configurationUpdateNeeded = setAuthenticationCacheEnabledElement(auth, "true");
        updateConfigDynamically(server, serverConfiguration);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different invalid key which has garbage values in the Private key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Replace the primary key with a different invalid key which has swapped values in the Private key from another validation.keys file.
     * <LI>Try to access the simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different Private key causing a CWWKS4106E: LTPA configuration error.
     * <LI>Successful authentication to simple servlet since the old cookie is still being used.
     * <LI>The ltpa.keys file is replaced with a different Private key.
     * <LI>Successful authorization to simple servlet, but unsuccessful authentication since the decryption fails with the swapped Private Key values.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "javax.crypto.BadPaddingException", "java.lang.IllegalArgumentException", "java.lang.NullPointerException" })
    public void testLTPAFileReplacement_invalidPrivateKey_monitorValidationKeysDir_true_monitorInterval_10() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

        // Set cacheEnabled to false to avoid caching the validation keys
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        Authentication auth = serverConfiguration.getAuthentication();
        Boolean configurationUpdateNeeded = setAuthenticationCacheEnabledElement(auth, "false");
        updateConfigDynamically(server, serverConfiguration);

        // Copy validation keys file (validation1.keys) to the server. This file has a valid Private key.
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Replace the primary key with the valid key
        renameFileIfExists(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, true);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Copy validation keys file (validation5.keys) to the server. This file has garbage values in the Private key.
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY5_PATH);

        // Wait for the LTPA configuration modified message after the change
        assertNotNull("Expected LTPA configuration modified message not found in the log.",
                      server.waitForStringInLog("CWWKS4107A", 5000));

        // Replace the primary key with a different invalid key
        renameFileIfExists(BAD_PRIVATE_VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, true);

        // Check for the following exception message in the log
        assertNotNull("Expected LTPA configuration error message not found in the log.",
                      server.waitForStringInLog("CWWKS4106E", 5000));

        // Attempt to access the simple servlet again with the same ltpa cookie1 and assert it works
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Copy validation keys file (validation6.keys) to the server. This file has swapped values in the Private key from another validation.keys file.
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY6_PATH);

        // Wait for the LTPA configuration modified message after the change
        assertNotNull("Expected LTPA configuration modified message not found in the log.",
                      server.waitForStringInLog("CWWKS4107A", 5000));

        // Replace the primary key with a different invalid key
        renameFileIfExists(BAD_PRIVATE_VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, true);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Attempt initial login to simple servlet for form login2
        assertTrue("Authentication should fail with decryption failure",
                   flClient2.accessProtectedServletWithAuthorizedCredentialsExpectsFailure(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword));

        // Wait for a security token cannot be validated message in the log
        assertNotNull("Expected security token cannot be validated message not found in the log.",
                      server.waitForStringInLog("CWWKS4001I", 5000));

        // Set cacheEnabled back to true
        configurationUpdateNeeded = setAuthenticationCacheEnabledElement(auth, "true");
        updateConfigDynamically(server, serverConfiguration);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different invalid key which has garbage values in the Public key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Replace the primary key with a different invalid key which has swapped values in the Public key from another validation.keys file.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key causing a CWWKS4106E: LTPA configuration error.
     * <LI>Successful authentication to simple servlet since the old cookie is still being used.
     * <LI>The ltpa.keys file is replaced with a different Public key.
     * <LI>Unsuccessful authentication to simple servlet since the decryption fails with the swapped Public Key values.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "javax.crypto.BadPaddingException", "java.lang.IllegalArgumentException", "java.lang.NullPointerException" })
    public void testLTPAFileReplacement_invalidPublicKey_monitorValidationKeysDir_true_monitorInterval_10() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

        // Set cacheEnabled to false to avoid caching the validation keys
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        Authentication auth = serverConfiguration.getAuthentication();
        Boolean configurationUpdateNeeded = setAuthenticationCacheEnabledElement(auth, "false");
        updateConfigDynamically(server, serverConfiguration);

        // Copy validation keys file (validation1.keys) to the server. This file has a valid Public key.
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Replace the primary key with the valid key
        renameFileIfExists(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, true);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Copy validation keys file (validation7.keys) to the server. This file has garbage values in the Public key.
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY7_PATH);

        // Wait for the LTPA configuration modified message after the change
        assertNotNull("Expected LTPA configuration modified message not found in the log.",
                      server.waitForStringInLog("CWWKS4107A", 5000));

        // Replace the primary key with a different invalid key
        renameFileIfExists(BAD_PUBLIC_VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, true);

        // Check for the following exception message in the log
        assertNotNull("Expected LTPA configuration error message not found in the log.",
                      server.waitForStringInLog("CWWKS4106E", 5000));

        // Attempt to access the simple servlet again with the same ltpa cookie1 and assert it works
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Copy validation keys file (validation8.keys) to the server. This file has swapped values in the Public key from another validation.keys file.
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY8_PATH);

        // Wait for the LTPA configuration modified message after the change
        assertNotNull("Expected LTPA configuration modified message not found in the log.",
                      server.waitForStringInLog("CWWKS4107A", 5000));

        // Replace the primary key with a different invalid key
        renameFileIfExists(BAD_PUBLIC_VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, true);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Attempt to access the simple servlet again with the same ltpa cookie1 and assert it fails and the server needs to login again
        assertTrue("An invalid cookie should result in authorization challenge",
                   flClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1));

        // Wait for a security token cannot be validated message in the log
        assertNotNull("Expected security token cannot be validated message not found in the log.",
                      server.waitForStringInLog("CWWKS4001I", 5000));

        // Set cacheEnabled back to true
        configurationUpdateNeeded = setAuthenticationCacheEnabledElement(auth, "true");
        updateConfigDynamically(server, serverConfiguration);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorValidationKeysDir to false, and MonitorInterval to 0.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Check for the creation of a new ltpa.keys file.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to false, and MonitorInterval to 0.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>Continued authentication to simple servlet; server is not restarted and does not need to login again.
     * <LI>A new ltpa.keys file is not created.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileCreationDeletion_monitorValidationKeysDir_false_monitorInterval_0() throws Exception {
        // Configure the server
        configureServer("false", "0", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);
        Thread.sleep(1000);

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
     * <LI>Set MonitorValidationKeysDir to false, and MonitorInterval to 0.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different valid key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to false, and MonitorInterval to 0.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key.
     * <LI>Successful authentication to simple servlet, since the server still uses the old key without file monitoring.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileReplacement_newValidKey_monitorValidationKeysDir_false_monitorInterval_0() throws Exception {
        // Configure the server
        configureServer("false", "0", true);

        // Copy validation key file (validation2.keys) to the server
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY2_PATH);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Replace the primary key with a different valid key
        renameFileIfExists(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, true);
        Thread.sleep(1000);

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
     * <LI>Set MonitorValidationKeysDir to false, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to configuredValidation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Check for the creation of a new ltpa.keys file.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to false, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to configuredValidation1.keys.
     * <LI>Continued authentication to simple servlet; server is not restarted and does not need to login again.
     * <LI>A new ltpa.keys file is created.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created and ltpa cookie2 is different from ltpa cookie1.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileCreationDeletion_monitorValidationKeysDir_false_monitorInterval_10() throws Exception {
        // Configure the server
        configureServer("false", "10", false);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to configuredValidation1.keys
        renameFileIfExists(DEFAULT_KEY_PATH, CONFIGURED_VALIDATION_KEY1_PATH, false);

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
     * <LI>Set MonitorValidationKeysDir to false, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different valid key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to false, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key.
     * <LI>Failed authentication to simple servlet.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created and ltpa cookie2 is different from ltpa cookie1.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileReplacement_newValidKey_monitorValidationKeysDir_false_monitorInterval_10() throws Exception {
        // Configure the server
        configureServer("false", "10", true);

        // Copy validation key file (validation2.keys) to the server
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY2_PATH);

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
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 0.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Check for the creation of a new ltpa.keys file.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 0.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>Continued authentication to simple servlet; server is not restarted and does not need to login again.
     * <LI>A new ltpa.keys file is not created.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileCreationDeletion_monitorValidationKeysDir_true_monitorInterval_0() throws Exception {
        // Configure the server
        configureServer("true", "0", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);
        Thread.sleep(1000);

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
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 0.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different valid key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 0.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key.
     * <LI>Successful authentication to simple servlet, since the server still uses the old key without file monitoring.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileReplacement_newValidKey_monitorValidationKeysDir_true_monitorInterval_0() throws Exception {
        // Configure the server
        configureServer("true", "0", true, false);

        // Copy validation key file (validation2.keys) to the server
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY2_PATH);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Replace the primary key with a different valid key
        renameFileIfExists(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, true);
        Thread.sleep(1000);

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
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to configuredValidation1.keys.
     * <LI>Set fileName to null to make it not configured in the validation keys element.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set fileName value to empty string.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set fileName value to a non-existent file.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set fileName's path to a different path than the default ltpa.keys file.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set fileName to point to a invalid keys file which has garbage values in the Private key.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set fileName to point to a valid keys file.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to configuredValidation1.keys.
     * <LI>The fileName attribute is not configured in the validation keys element.
     * <LI>Exception is thrown in the logs since fileName is a required attribute.
     * <LI>The fileName attribute is set to empty string.
     * <LI>Exception is thrown in the logs since fileName is a required attribute.
     * <LI>The fileName attribute is set to a non-existent file.
     * <LI>Exception is thrown in the logs since the file does not exist
     * <LI>The fileName attribute's path is set to a different path than the default ltpa.keys file.
     * <LI>Exception is thrown in the logs since the file does not exist
     * <LI>The fileName attribute is set to point to a invalid keys file which has garbage values in the Private key.
     * <LI>Exception is thrown in the logs since the file is invalid. CWWKS4106E: LTPA configuration error.
     * <LI>The fileName attribute is set to point to a valid keys file.
     * <LI>Successful authentication to simple servlet.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testValidationKeys_fileNameAttribute() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys.
        renameFileIfExists(DEFAULT_KEY_PATH, CONFIGURED_VALIDATION_KEY1_PATH, false);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Set fileName to null to make it not configured in the validation keys element.
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAvalidationKeyFileNameElement(ltpa, null);
        updateConfigDynamically(server, serverConfiguration);

        // Exceptions are thrown in the logs since fileName is a required attribute
        assertNotNull("Expected fileName is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKG0058E", 5000));

        assertNotNull("Expected fileName is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKS4111E", 5000));

        // Set fileName value to empty string
        configurationUpdateNeeded = setLTPAvalidationKeyFileNameElement(ltpa, "");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since fileName is a required attribute
        assertNotNull("Expected fileName is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKS4111E", 5000));

        // Set fileName value to a non-existent file
        configurationUpdateNeeded = setLTPAvalidationKeyFileNameElement(ltpa, "nonExistentFile.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since the file does not exist
        assertNotNull("Expected key file does not exist exception not found in the log.",
                      server.waitForStringInLog("CWWKS4112E", 5000));

        // Set fileName's path to a different path than the default ltpa.keys file
        configurationUpdateNeeded = setLTPAvalidationKeyFileNameElement(ltpa, ALT_VALIDATION_KEY1_PATH);
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since the file does not exist
        assertNotNull("Expected key file does not exist exception not found in the log.",
                      server.waitForStringInLog("CWWKS4112E", 5000));

        // Copy validation key file (validation5.keys) to the server.
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY5_PATH);

        // Set fileName to point to a malformed/invalid keys file
        configurationUpdateNeeded = setLTPAvalidationKeyFileNameElement(ltpa, "validation5.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since the file is invalid. CWWKS4106E: LTPA configuration error.
        assertNotNull("Expected LTPA configuration error message not found in the log.",
                      server.waitForStringInLog("CWWKS4106E", 5000));

        // Delete the validation5.keys file and wait for the LTPA configuration to be ready after the change
        deleteFileIfExists(BAD_PRIVATE_VALIDATION_KEY1_PATH, true);
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Set fileName back to the default ltpa.keys file
        configurationUpdateNeeded = setLTPAvalidationKeyFileNameElement(ltpa, "configuredValidation1.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Successful authentication to simple servlet
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to configuredValidation1.keys.
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
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to configuredValidation1.keys.
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
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "javax.crypto.BadPaddingException", "java.lang.IllegalArgumentException", "java.lang.NullPointerException" })
    public void testValidationKeys_passwordAttribute() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys.
        renameFileIfExists(DEFAULT_KEY_PATH, CONFIGURED_VALIDATION_KEY1_PATH, false);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Set password value to null
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAvalidationKeyPasswordElement(ltpa, null);
        updateConfigDynamically(server, serverConfiguration);

        // Exceptions are thrown in the logs since the validation key password is a required attribute if ltpa element has password configured
        assertNotNull("Expected password is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKG0058E", 5000));

        assertNotNull("Expected password is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKS4111E", 5000));

        // Set password value to empty string
        configurationUpdateNeeded = setLTPAvalidationKeyPasswordElement(ltpa, "");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since the validation key password is a required attribute if ltpa element has password configured
        assertNotNull("Expected password is missing exception not found in the log.",
                      server.waitForStringInLog("CWWKS4111E", 5000));

        // Set password value to an incorrect password
        configurationUpdateNeeded = setLTPAvalidationKeyPasswordElement(ltpa, "{xor}incorrectPassword=");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs since the validation key password must match the primary key password
        assertNotNull("Expected problems during password decryption encountered exception not found in the log.",
                      server.waitForStringInLog("CWWKS1859E", 5000));

        // Set password value to a correct password
        configurationUpdateNeeded = setLTPAvalidationKeyPasswordElement(ltpa, validationKeyPassword);
        updateConfigDynamically(server, serverConfiguration);

        // Successful authentication to simple servlet
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to configuredValidation1.keys.
     * <LI>Set validUntilDate to null to make it not configured in the validation keys element.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set validUntilDate value to empty string.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set validUntilDate value to an invalid date string.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set validUntilDate value to a date string in the past.
     * <LI>Check for an exception based on this configuration.
     * <LI>Set validUntilDate value to a valid date string in the future.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to configuredValidation1.keys.
     * <LI>The validUntilDate attribute is not configured in the validation keys element.
     * <LI>Continued authentication to simple servlet; the element is not required to be configured.
     * <LI>The validUntilDate attribute is set to empty string.
     * <LI>Exception is thrown in the logs for the empty date string.
     * <LI>The validUntilDate attribute is set to an invalid date string.
     * <LI>Exception is thrown in the logs for the invalid date string.
     * <LI>The validUntilDate attribute is set to a date string in the past.
     * <LI>Exception is thrown in the logs for the expired date string.
     * <LI>The validUntilDate attribute is set to a valid date string in the future.
     * <LI>Successful authentication to simple servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.time.format.DateTimeParseException" })
    public void testValidationKeys_validUntilDateAttribute() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys.
        renameFileIfExists(DEFAULT_KEY_PATH, CONFIGURED_VALIDATION_KEY1_PATH, false);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Set validUntilDate value to null
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAvalidationKeyValidUntilDateElement(ltpa, null);
        updateConfigDynamically(server, serverConfiguration);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Continued authentication to simple servlet; the element is not required to be configured
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Set validUntilDate value to empty string
        configurationUpdateNeeded = setLTPAvalidationKeyValidUntilDateElement(ltpa, "");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs for the empty date string
        assertNotNull("Expected invalid date format exception not found in the log.",
                      server.waitForStringInLog("CWWKS4110E", 5000));

        // Set validUntilDate value to an invalid date string
        configurationUpdateNeeded = setLTPAvalidationKeyValidUntilDateElement(ltpa, "2023-18T18:08:35Z");
        updateConfigDynamically(server, serverConfiguration);

        // Exception is thrown in the logs for the invalid date string
        assertNotNull("Expected invalid date format exception not found in the log.",
                      server.waitForStringInLog("CWWKS4110E", 5000));

        // Set validUntilDate value to a date string in the past
        configurationUpdateNeeded = setLTPAvalidationKeyValidUntilDateElement(ltpa, "2023-01-01T00:00:00Z");
        updateConfigDynamically(server, serverConfiguration);

        // Message is thrown in the logs for a expired date string
        assertNotNull("Expected expired date exception not found in the log.",
                      server.waitForStringInLog("CWWKS4109W", 5000));

        // Set validUntilDate value to a valid date string in the future
        configurationUpdateNeeded = setLTPAvalidationKeyValidUntilDateElement(ltpa, "2099-01-01T00:00:00Z");
        updateConfigDynamically(server, serverConfiguration);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Successful authentication to simple servlet
        String response3 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorValidationKeysDir to false, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Add a new validation keys element with a fileName attribute as "validation2.keys".
     * <LI>Rename the ltpa.keys file to validation2.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 0.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Delete the validation2.keys file.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to false, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>A new validation keys element is added with a fileName attribute as "validation2.keys".
     * <LI>The ltpa.keys file is renamed to validation2.keys.
     * <LI>Successful authentication to simple servlet.
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 0.
     * <LI>Successful authentication to simple servlet.
     * <LI>The validation2.keys file is deleted.
     * <LI>Successful authentication to simple servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @CheckForLeakedPasswords({ validPassword })
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
        validationKey.password = validationKeyPassword;
        validationKey.validUntilDate = "2099-01-01T00:00:00Z";
        validationKeys.add(validationKey);
        updateConfigDynamically(server, serverConfiguration);

        // Rename the ltpa.keys file to validation2.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY2_PATH, false);
        Thread.sleep(1000);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Set MonitorValidationKeysDir to true, and MonitorInterval to 0
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
     * <LI>Set MonitorValidationKeysDir to true, and MonitorInterval to 0.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Add a new validation keys element with a fileName attribute as "validation2.keys" and validUntilDate attribute as current time + 10 second.
     * <LI>Rename the ltpa.keys file to validation2.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Wait for 10 seconds.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorValidationKeysDir is set to true, and MonitorInterval to 0.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>A new validation keys element is added with a fileName attribute as "validation2.keys" and validUntilDate attribute as current time + 10 second.
     * <LI>The ltpa.keys file is renamed to validation2.keys.
     * <LI>Successful authentication to simple servlet.
     * <LI>The validUntilDate attribute is expired.
     * <LI>Failed authentication to simple servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testValidUntilDate_expiringAfterConfigurationAndUsage() throws Exception {
        // Configure the server
        configureServer("true", "0", true);

        // Set cacheEnabled to false to avoid caching the validation keys
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        Authentication auth = serverConfiguration.getAuthentication();
        Boolean configurationUpdateNeeded = setAuthenticationCacheEnabledElement(auth, "false");
        updateConfigDynamically(server, serverConfiguration);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation2.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY2_PATH, false);
        Thread.sleep(1000);

        // Configure the time of expiry as 10 seconds from present time
        String currentTime = Instant.now().toString();
        String expiryTime = Instant.now().plusSeconds(10).toString();

        // Add a new validation keys element with a fileName attribute as "validation2.keys"
        LTPA ltpa = serverConfiguration.getLTPA();
        ConfigElementList<ValidationKeys> validationKeys = ltpa.getValidationKeys();
        ValidationKeys validationKey = new ValidationKeys();
        validationKey.fileName = "validation2.keys";
        validationKey.password = validationKeyPassword;
        validationKey.validUntilDate = expiryTime;
        validationKeys.add(validationKey);
        updateConfigDynamically(server, serverConfiguration);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Wait for 10 seconds
        Thread.sleep(10000);

        // Attempt to access the simple servlet again with the same cookie and assert it fails and the server needs to login again
        assertTrue("An invalid cookie should result in authorization challenge",
                   flClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1));

        // Delete the validation2.keys element and set cacheEnabled back to true
        validationKeys.remove(validationKey);
        configurationUpdateNeeded = setAuthenticationCacheEnabledElement(auth, "true");
        updateConfigDynamically(server, serverConfiguration);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set Expiry to 1m, MonitorDirectory to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Wait for 70 seconds.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Expiry is set to 1m, MonitorDirectory is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>Successful authentication to simple servlet.
     * <LI>Wait for 70 seconds.
     * <LI>Failed authentication to simple servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testExpiredLtpaToken_monitorValidationKeysDir_true_monitorInterval_10() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

        // Set the expiry to 1m
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAexpiryElement(ltpa, "1m");
        updateConfigDynamically(server, serverConfiguration);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Wait for 70 seconds
        Thread.sleep(70000);

        // Attempt to access the simple servlet again with the same cookie and assert it fails and the server needs to login again
        assertTrue("An expired cookie should result in authorization challenge",
                   flClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1));

        // Reset the expiry to 10m
        configurationUpdateNeeded = setLTPAexpiryElement(ltpa, "10m");
        updateConfigDynamically(server, serverConfiguration);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set Expiry to 1m, MonitorDirectory to false, and MonitorInterval to 0.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Wait for 70 seconds.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Expiry is set to 1m, MonitorDirectory is set to false, and MonitorInterval to 0.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>Successful authentication to simple servlet.
     * <LI>Wait for 70 seconds.
     * <LI>Failing authentication to simple servlet.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testExpiredLtpaToken_monitorValidationKeysDir_false_monitorInterval_0() throws Exception {
        // Configure the server
        configureServer("false", "0", true);

        // Set the expiry to 1m
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAexpiryElement(ltpa, "1m");
        updateConfigDynamically(server, serverConfiguration);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Wait for 70 seconds
        Thread.sleep(70000);

        // Attempt to access the simple servlet again with the same cookie and assert it fails and the server needs to login again
        assertTrue("An expired cookie should result in authorization challenge",
                   flClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1));

        // Reset the expiry to 10m
        configurationUpdateNeeded = setLTPAexpiryElement(ltpa, "10m");
        updateConfigDynamically(server, serverConfiguration);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set UpdateTrigger to disabled, MonitorValidationKeysDir to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Check for the creation of a new ltpa.keys file.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>UpdateTrigger is set to disabled, MonitorValidationKeysDir is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>Continued authentication to simple servlet; server is not restarted and does not need to login again.
     * <LI>A new ltpa.keys file is not created.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileCreationDeletion_updateTrigger_disabled() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

        // Set the updateTrigger to disabled
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAupdateTriggerElement(ltpa, "disabled");
        updateConfigDynamically(server, serverConfiguration);

        // Wait for two warning messages message to be logged
        assertNotNull("Expected LTPA configuration warning message not found in the log.",
                      server.waitForStringInLog("CWWKS4114W", 5000));

        assertNotNull("Expected LTPA configuration warning message not found in the log.",
                      server.waitForStringInLog("CWWKS4115W", 5000));

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Rename the ltpa.keys file to validation1.keys
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY1_PATH, false);
        Thread.sleep(1000);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Assert that a new ltpa.keys file was not created
        assertFileWasNotCreated(DEFAULT_KEY_PATH);

        // New login to simple servlet for form login2
        String response3 = flClient2.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);
        String cookie2 = flClient2.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 2 is missing.", cookie2);

        // Reset the updateTrigger to polled
        configurationUpdateNeeded = setLTPAupdateTriggerElement(ltpa, "polled");
        updateConfigDynamically(server, serverConfiguration);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set UpdateTrigger to disabled, MonitorValidationKeysDir to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Replace the primary key with a different valid key.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Attempt to access a new simple servlet configured for form login2 with valid credentials.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>UpdateTrigger is set to disabled, MonitorValidationKeysDir is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is replaced with a different key.
     * <LI>Successful authentication to simple servlet, since the server still uses the old key. This occurs when updateTrigger is disabled meaning there is no file/directory
     * monitoring.
     * <LI>Successful authentication to simple servlet with new ltpa cookie2 created.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPAFileReplacement_newValidKey_updateTrigger_disabled() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

        // Set the updateTrigger to disabled
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAupdateTriggerElement(ltpa, "disabled");
        updateConfigDynamically(server, serverConfiguration);

        // Wait for two warning messages message to be logged
        assertNotNull("Expected LTPA configuration warning message not found in the log.",
                      server.waitForStringInLog("CWWKS4114W", 5000));

        assertNotNull("Expected LTPA configuration warning message not found in the log.",
                      server.waitForStringInLog("CWWKS4115W", 5000));

        // Copy validation key file (validation2.keys) to the server
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY2_PATH);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Replace the primary key with a different valid key
        renameFileIfExists(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, true);
        Thread.sleep(1000);

        // Attempt to access the simple servlet again with the same cookie and assert it works
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // New login to simple servlet for form login2
        String response3 = flClient2.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);
        String cookie2 = flClient2.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 2 is missing.", cookie2);

        // Reset the updateTrigger to polled
        configurationUpdateNeeded = setLTPAupdateTriggerElement(ltpa, "polled");
        updateConfigDynamically(server, serverConfiguration);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set the updateTrigger to mbean, monitorValidationKeysDir to false, and monitorInterval to 0.
     * <LI>Modify primary LTPA keys file (i.e. ltpa.keys).
     * <LI>Use the FileNotification MBean to notify the server of the modified keys file
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful authentication to simple servlet will return a valid ltpa cookie1 back to the client.
     * <LI>Liberty server processes modifications made to LTPA keys file initiated by the FileNotification MBean.
     * <LI>Failing authentication to simple servlet using the cookie, since the LTPA keys have been changed.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testPrimaryLtpaKeysFileModified_updateTrigger_mbean() throws Exception {
        // Configure the server
        configureServer("false", "0", true);

        // Set updateTrigger to mbean
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        ltpa.updateTrigger = "mbean";
        updateConfigDynamically(server, serverConfiguration);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Contents to update ltpa keys with
        Map<String, String> contents;
        if (fipsEnabled) {
            contents = new HashMap<String, String>() {
                {
                    put("com.ibm.websphere.ltpa.3DESKey", "q2yFKBh8u2h1OcI7UQuHCvIpo40WDxYDrfKLvztDrYvoca+oLO1m1HG/BpSX13GR");
                    put("com.ibm.websphere.ltpa.PrivateKey",
                        "IuqjEqWvUrAK90pArV9y2k6XjnJKbSpuUWHUd8qAuVvSBPeoQCluCPWQ3JOq1dkMPexSd/ZUNf03zFQ9XuJjanXPCJiPavamu6xzUsbtXwHA5ReqLWdmeapAB15uOpl5EjKTk3EdDH29sGvs2vzYyzu1uWPVWglIixICb6cs5QnWIIUl+Qs7vmY5S9TRoHQ8Y+npiByNopll5O9K4iPW0dy+0Wm6h/xoh49xGkwyYRHV2HX2/7RRf6y7TofkIdYS9WALb15g+tSR0/IJU/AuXHcp+MYbhU/dRWKx6f5Dconc1pu/QT0vKWIuakTnEJNHSeUzlglKK0pyD9yAjcVxPR3x0qBOKK0ss8N1D0xgID98VOGNlXF9SKjrW7rDqf2P5xP1lRrRsXPklVJN5G4p+DQn2oSnVWV1xp791uOb3Ub5HFK4c4Uv0PPKYIOhB2cAplLh+DV0BJkH6zWxj7Aqt9jXB6zC4bZrmMQ5Q9KlKJUF+LjXYy7uj5avVYt7vjcp7wM3DCr+1XfQKnYbjColRG7N6MzhN3m1aN243mgswpz+5uQHQoyNqpFh1yoqmQdo1Tq8EMgQRpO//e+yhN6alZ6P+fV6TuyK7tZb2wHXJXZo3neULAKeC9i8pwt9IDiu9Ey6bBEhs5NpZakVZqdqgU6SX1wANVe5Cwy8IjXboVNfXmiTnJnFIAT6RdohMvoJ");
                    put("com.ibm.websphere.ltpa.PublicKey",
                        "ANledrwCYpbNbOa1YzBSEP3EmhnznGXKzXlJTU8p0FFMmItr4emBlB78Nw+2vOW0gUb4rSdyqsj6T2QbR3xGhcs8M8AVLHppgnfLOiWlXFicSMemL3LGEkHAF0XdBpwgQo9/RzhJyLT9IzyGkyJWBkVD2ZnbM78JNN3aBeoPKobKsa9779Iz4oU14flCI9E7t6rBpZsCt8ygWP45yq6OgRCct4b31ZXkWL9zxLnSdmNEO99ct0WlcCV6+K5J9WGQBMjPWcNQ2a5w6FJk2iFNua/0hDwWd3hv0tOY81icIcob6ZyHxkJJhfYy6bs78FXr9QXQwafTRY7VrQn3VF//wVMBAAE\\=");
                }
            };
        } else {
            contents = new HashMap<String, String>() {
                {
                    put("com.ibm.websphere.ltpa.3DESKey", "eJh1K9My7p4Uj0Gw/X2XDWxyY1C9E3UEp7ji+BJPSDM\\=");
                    put("com.ibm.websphere.ltpa.PrivateKey",
                        "kmhgRjTUcxvFJoVw8jxWuh3ffuxym/SLYW8TQYKjK/4TJoPx9h2FJvNHkiaxfvACUWN5Lw5A1c500PRD+kcUtY+05IpNbGd0xu7BsjDQoLaEi4jrtBjT0REEYepsj9QQXnQTG9GL3CuNkSmPLxlHWBKZkDlcv4MtOKn2ozeXQjQ5doAJGDm6qk8QxxB7jGHCdQI9L6G4ic34w6DWV9qKZiX/Yp39neL6jR9mH3e9U7EFyefrtOTF7EUscfikBnw0sQUNnwTx2vMv+Q9QI+ykZMJULvzGKf2fW7Qz+OfQIlTatBCYRWtG0BQGi0BkUULApK2qIxQbvLVT7ijEwg2YsWTREcsnbVvHFmSqTF5jf8w\\=");
                    put("com.ibm.websphere.ltpa.PublicKey",
                        "AK/MQIy6PT5GCI1qYDhH6b7yyZPdCcc4cgOKyJOkS/F4IHA51rjW5gVUm0gWkqfCkoU6LsWkBetxiJeZ7ECL4mUOSTfEFx4cPtvCu62DxtgleQt6pbEuvtaDalFL6/6p35y2uyuKhX4YiG9w25lLXTNCMfw3mQn+RpC3pVjYKpB9AQAB");
                }
            };
        }

        // Update LTPA keys file
        modifyFileIfExists(DEFAULT_KEY_PATH, contents);

        // Notify Liberty server of changes made to LTPA key file via mbean
        List<String> modifiedFilePaths = Arrays.asList(new String[] { DEFAULT_KEY_PATH });
        notifyFileChangesWithMbean(null, modifiedFilePaths, null);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Attempt to access the simple servlet again with the same cookie and assert it fails and the server needs to login again
        assertTrue("An expired cookie should result in authorization challenge",
                   flClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set the updateTrigger to mbean, monitorValidationKeysDir to true, and monitorInterval to 0.
     * <LI>Create validation2.keys (by renaming ltpa.keys)
     * <LI>Delete ltpa.keys
     * <LI>Use the FileNotification MBean to notify the server of the created and deleted keys files
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful authentication to simple servlet will return a valid ltpa cookie1 back to the client.
     * <LI>Liberty server processes modifications made to LTPA keys file initiated by the FileNotification MBean.
     * <LI>ltpa.keys are regenerated after the deleted files notification from the mbean
     * <LI>Successful authentication to simple servlet using the cookie, since the original LTPA keys were added in validation2.keys.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testPrimaryLtpaKeysFileDeleted_updateTrigger_mbean_validationKeysFileCreated() throws Exception {
        // Configure the server
        configureServer("true", "0", true);

        // Set updateTrigger to mbean
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        ltpa.updateTrigger = "mbean";
        updateConfigDynamically(server, serverConfiguration);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        moveLogMark();
        renameFileIfExists(DEFAULT_KEY_PATH, VALIDATION_KEY2_PATH, false);
        Thread.sleep(1000);

        // Notify Liberty server of changes made to LTPA key file via mbean
        List<String> createdFilePaths = Arrays.asList(new String[] { VALIDATION_KEY2_PATH });
        List<String> deltedFilePaths = Arrays.asList(new String[] { DEFAULT_KEY_PATH });
        notifyFileChangesWithMbean(createdFilePaths, null, deltedFilePaths);

        // Wait for the ltpa.keys file to be regenerated
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4104A", 5000));

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Re-configure the keysFileName to a different relative path within the server config directory and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName to another relative path within the wlp directory and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName to a different absolute path within the wlp directory and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName to another absolute path within the base directory and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is moved to a different relative path within the server config directory.
     * <LI>Successful authentication to simple servlet.
     * <LI>The ltpa.keys file is moved back to another relative path within the wlp directory.
     * <LI>Successful authentication to simple servlet.
     * <LI>The ltpa.keys file is moved to a different absolute path within the wlp directory.
     * <LI>Successful authentication to simple servlet.
     * <LI>The ltpa.keys file is moved back to another absolute path within the base directory.
     * <LI>Successful authentication to simple servlet.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testDifferentDirectoriesForPrimaryKeys() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

        // Initial login to simple servlet for form login1
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from the login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", cookie1);

        // Move the ltpa.keys file to a different relative path within the server config directory
        moveFileIfExists(relativeDirectory + "/resources/security", relativeDirectory, "ltpa.keys", false);

        // Re-configure the keysFileName to that location with the relative path
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "${server.config.dir}/ltpa.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response2 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Move the ltpa.keys file to another relative path within the wlp directory
        moveFileIfExists(relativeDirectory, wlpDirectory + "/resources", "ltpa.keys", false);

        // Re-configure the keysFileName to that location with the relative path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "${wlp.install.dir}/resources/ltpa.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response3 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Move the ltpa.keys file to a different absolute path within the wlp directory
        moveFileIfExists(wlpDirectory + "/resources", wlpDirectory + "/test", "ltpa.keys", false);

        // Re-configure the keysFileName to that location with the absolute path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, wlpDirectory + "/test/ltpa.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response4 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Move the ltpa.keys file to another absolute path within the base directory
        moveFileIfExists(wlpDirectory + "/test", baseDirectory + "/random", "ltpa.keys", false);

        // Re-configure the keysFileName to that location with the absolute path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, baseDirectory + "/random/ltpa.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response5 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Re-configure the keysFileName to the default value
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "${server.config.dir}/resources/security/ltpa.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Delete the ltpa.keys file from all of the last locations
        deleteFileFromAbsolutePathIfExists(relativeDirectory + "/ltpa.keys", true);
        deleteFileFromAbsolutePathIfExists(wlpDirectory + "/resources/ltpa.keys", true);
        deleteFileFromAbsolutePathIfExists(wlpDirectory + "/test/ltpa.keys", true);
        deleteFileFromAbsolutePathIfExists(baseDirectory + "/random/ltpa.keys", true);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set MonitorDirectory to true, and MonitorInterval to 10.
     * <LI>Attempt to access a simple servlet configured for form login1 with valid credentials.
     * <LI>Rename the ltpa.keys file to validation1.keys.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName and validation fileName to a different relative path within the server config directory and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName and validation fileName to another relative path within the wlp directoryand move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName and validation fileName to a different absolute path within the wlp directory and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <LI>Re-configure the keysFileName and validation fileName to another absolute path within the base directory and move the file there.
     * <LI>Retry access to the simple servlet configured for form login1 with ltpa cookie1.
     * <OL>
     * <P>Expected Results:
     * <OL>
     * <LI>MonitorDirectory is set to true, and MonitorInterval to 10.
     * <LI>Successful authentication to simple servlet with ltpa cookie1 created.
     * <LI>The ltpa.keys file is renamed to validation1.keys.
     * <LI>Successful authentication to simple servlet.
     * <LI>The validation1.keys file is moved to a different relative path within the server config directory.
     * <LI>Successful authentication to simple servlet.
     * <LI>The validation1.keys file is moved back to another relative path within the wlp directory.
     * <LI>Successful authentication to simple servlet.
     * <LI>The validation1.keys file is moved to a different absolute path within the wlp directory.
     * <LI>Successful authentication to simple servlet.
     * <LI>The validation1.keys file is moved back to another absolute path within the base directory.
     * <LI>Successful authentication to simple servlet.
     * </OL>
     */
    @Test
    @CheckForLeakedPasswords({ validPassword })
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testDifferentDirectoriesForValidationKeys() throws Exception {
        // Configure the server
        configureServer("true", "10", true);

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

        // Move the validation1.keys file to a different relative path within the server config directory
        moveFileIfExists(relativeDirectory + "/resources/security", relativeDirectory, "validation1.keys", false);

        // Re-configure the keysFileName and validation fileName to that location with the relative path
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();
        Boolean configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "${server.config.dir}/ltpa.keys")
                                            | setLTPAvalidationKeyFileNameElement(ltpa, "validation1.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response3 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Move the validation2.keys file to another relative path within the wlp directory
        moveFileIfExists(relativeDirectory, wlpDirectory + "/resources", "validation1.keys", false);

        // Re-configure the keysFileName and validation fileName to that location with the relative path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "${wlp.install.dir}/resources/ltpa.keys")
                                    | setLTPAvalidationKeyFileNameElement(ltpa, "validation1.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response4 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Move the validation.keys file to a different absolute path within the wlp directory
        moveFileIfExists(wlpDirectory + "/resources", wlpDirectory + "/test", "validation1.keys", false);

        // Re-configure the keysFileName and validation fileName to that location with the absolute path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, wlpDirectory + "/test/ltpa.keys")
                                    | setLTPAvalidationKeyFileNameElement(ltpa, "validation1.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response5 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Move the validation.keys file to another absolute path within the base directory
        moveFileIfExists(wlpDirectory + "/test", baseDirectory + "/random", "validation1.keys", false);

        // Re-configure the keysFileName and validation fileName to that location with the absolute path
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, baseDirectory + "/random/ltpa.keys")
                                    | setLTPAvalidationKeyFileNameElement(ltpa, "validation1.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Attempt to access the simple servlet again with the same cookie and assert that the server did not need to login again
        String response6 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);

        // Re-configure the keysFileName and validation fileName to the default value
        configurationUpdateNeeded = setLTPAkeysFileNameElement(ltpa, "${server.config.dir}/resources/security/ltpa.keys")
                                    | setLTPAvalidationKeyFileNameElement(ltpa, "configuredValidation1.keys");
        updateConfigDynamically(server, serverConfiguration);

        // Delete the ltpa.keys and validation1.keys file from all of the last locations
        deleteFileFromAbsolutePathIfExists(relativeDirectory + "/ltpa.keys", true);
        deleteFileFromAbsolutePathIfExists(wlpDirectory + "/resources/ltpa.keys", true);
        deleteFileFromAbsolutePathIfExists(wlpDirectory + "/test/ltpa.keys", true);
        deleteFileFromAbsolutePathIfExists(baseDirectory + "/random/ltpa.keys", true);
        deleteFileFromAbsolutePathIfExists(baseDirectory + "/random/validation1.keys", true);
    }

    // Function to do the server configuration for all the tests.
    public void configureServer(String monitorValidationKeysDir, String monitorInterval, Boolean waitForLTPAConfigReadyMessage) throws Exception {
        configureServer(monitorValidationKeysDir, monitorInterval, waitForLTPAConfigReadyMessage, true);
    }

    public void configureServer(String monitorValidationKeysDir, String monitorInterval, Boolean waitForLTPAConfigReadyMessage, boolean setLogMarkToEnd) throws Exception {
        configureServer("polled", monitorValidationKeysDir, monitorInterval, waitForLTPAConfigReadyMessage, true);
    }

    /**
     * Function to do the server configuration for all the tests.
     * Assert that the server has with a default ltpa.keys file.
     *
     * @param updateTrigger
     * @param monitorValidationKeysDir
     * @param monitorInterval
     * @param waitForLTPAConfigReadyMessage
     * @param setLogMarkToEnd
     *
     * @throws Exception
     */
    public void configureServer(String updateTrigger, String monitorValidationKeysDir, String monitorInterval, Boolean waitForLTPAConfigReadyMessage,
                                boolean setLogMarkToEnd) throws Exception {
        moveLogMark();
        // Get the server configuration
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();

        // Check if the configuration needs to be updated
        boolean configurationUpdateNeeded = false;

        configurationUpdateNeeded = setLTPAupdateTriggerElement(ltpa, updateTrigger)
                                    | setLTPAmonitorValidationKeysDirElement(ltpa, monitorValidationKeysDir)
                                    | setLTPAmonitorIntervalElement(ltpa, monitorInterval);

        // Apply server configuration update if needed
        if (configurationUpdateNeeded) {
            updateConfigDynamically(server, serverConfiguration);

            if (updateTrigger.equals("polled") && monitorValidationKeysDir.equals("true") && monitorInterval.equals("0")) {
                // Wait for a warning message message to be logged
                assertNotNull("Expected LTPA configuration warning message not found in the log.",
                              server.waitForStringInLog("CWWKS4113W", 5000));
            }

            if (waitForLTPAConfigReadyMessage) {
                // Wait for the LTPA configuration to be ready after the change
                assertNotNull("Expected LTPA configuration ready message not found in the log.",
                              server.waitForStringInLog("CWWKS4105I", 5000));
            }
        }

        // Assert that a default ltpa.keys file is generated
        assertFileWasCreated(DEFAULT_KEY_PATH);
        server.setKeysAndJVMOptsForFips();
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

    // Function to set the monitorValidationKeysDir to true or false
    public boolean setLTPAmonitorValidationKeysDirElement(LTPA ltpa, String value) {
        if (!ltpa.monitorValidationKeysDir.equals(value)) {
            ltpa.monitorValidationKeysDir = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure monitorInterval to a specific value
    public boolean setLTPAmonitorIntervalElement(LTPA ltpa, String value) {
        if (!ltpa.monitorInterval.equals(value)) {
            ltpa.monitorInterval = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure the expiration time for the LTPA token to a specific value
    public boolean setLTPAexpiryElement(LTPA ltpa, String value) {
        if (!ltpa.expiration.equals(value)) {
            ltpa.expiration = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure the updateTrigger to a specific value
    public boolean setLTPAupdateTriggerElement(LTPA ltpa, String value) {
        if (!ltpa.updateTrigger.equals(value)) {
            ltpa.updateTrigger = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure the fileName for validation keys
    public boolean setLTPAvalidationKeyFileNameElement(LTPA ltpa, String value) {
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
    public boolean setLTPAvalidationKeyPasswordElement(LTPA ltpa, String value) {
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

    // Function to configure the validUntilDate for validation keys
    public boolean setLTPAvalidationKeyValidUntilDateElement(LTPA ltpa, String value) {
        ConfigElementList<ValidationKeys> validationKeys = ltpa.getValidationKeys();
        ValidationKeys validationKey = validationKeys.get(0);

        // Check if null
        if (validationKey.validUntilDate == null) {
            validationKey.validUntilDate = value;
            return true; // Config update is needed
        }

        if (!validationKey.validUntilDate.equals(value)) {
            validationKey.validUntilDate = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure the cacheEnabled element for authentication cache
    public boolean setAuthenticationCacheEnabledElement(Authentication auth, String value) {
        if (!auth.cacheEnabled.equals(value)) {
            auth.cacheEnabled = value;
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
        Thread.sleep(1000);
    }

    /**
     * Modify the file if it exists. If we can't modify it, then
     * throw an exception as we need to be able to modify the file.
     * Modifies the keys contained in the filePath with the keys specified in the
     * contents HashMap
     *
     * @param filename
     * @param newFilePath
     * @param checkFileIsGone
     *
     * @throws Exception
     */
    private static void modifyFileIfExists(String filePath, Map<String, String> contents) throws Exception {
        Log.info(thisClass, "modifyFileIfExists", "\nfilePath: " + filePath + "\ncontents: " + contents);
        server.setMarkToEndOfLog(server.getDefaultLogFile());

        if (fileExists(filePath, 1)) {
            filePath = server.getServerRoot() + "/" + filePath;
            Map<String, String> updatedProperties = new HashMap<>();
            // Read properties from the file and modify accordingly (based on contents)
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = contents.getOrDefault(key, parts[1].trim());
                        updatedProperties.put(key, value);
                    }
                }
            } catch (IOException e) {
                throw e;
            }

            // Update the file with the updated contents
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                for (Map.Entry<String, String> entry : updatedProperties.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    writer.write(key + "=" + value);
                    writer.newLine();
                }

            } catch (IOException e) {
                throw e;
            }

        } else {
            throw new Exception("Unable to modify file because it does not exist: " + filePath);
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
        server.setMarkToEndOfLog(server.getDefaultLogFile());

        if (fileExists(filePath, 1)) {
            if (fileExists(newFilePath, 1)) {
                LibertyFileManager.moveLibertyFile(server.getFileFromLibertyServerRoot(filePath), server.getFileFromLibertyServerRoot(newFilePath));
            } else {
                Log.info(thisClass, "renameFileIfExists", "Calling server.renameLibertyServerRootFile");
                server.renameLibertyServerRootFile(filePath, newFilePath);
            }
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
    private static void moveFileIfExists(String filePath, String newFilePath, String fileName, boolean checkFileIsGone) throws Exception {
        Log.info(thisClass, "moveFileIfExists", "\nfilepath: " + filePath + "\nfileName: " + fileName + "\nnewFilePath: " + newFilePath + "\nfileName: " + fileName);
        if (absoluteFileExists(filePath + "/" + fileName, 1)) {
            Log.info(thisClass, "moveFileIfExists", "file exists, moving...");
            server.renameFileToAbsolutePathInLibertyServerRootFile(filePath, newFilePath, fileName);
            Thread.sleep(3000);

            // Double check to make sure the file is gone
            if (checkFileIsGone && fileExists(filePath + "/" + fileName, 1))
                throw new Exception("Unable to move file: " + filePath + "/" + fileName);
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
     * Delete the file if it exists. If we can't delete it, then
     * throw an exception as we need to be able to delete these files.
     *
     * @param absolutePath
     *
     * @throws Exception
     */
    private static void deleteFileFromAbsolutePathIfExists(String absolutePath, boolean checkFileIsGone) throws Exception {
        Log.info(thisClass, "deleteFileIfExists", "absolutePath: " + absolutePath);
        if (absoluteFileExists(absolutePath, 1)) {
            Log.info(thisClass, "deleteFileFromAbsolutePathInLibertyServer", "file exists, deleting...");
            server.deleteFileFromAbsolutePathInLibertyServer(absolutePath);

            // Double check to make sure the file is gone
            if (checkFileIsGone && absoluteFileExists(absolutePath, 1))
                throw new Exception("Unable to delete file: " + absolutePath);
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
                Thread.sleep(3000);
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
     * Check to see if the file exists given absolute file path.
     * We will retry a few times to ensure that the system was not slow to flush the file.
     *
     * @param absoluteFilePath
     * @param numberOfTries
     *
     * @throws Exception
     */
    private static boolean absoluteFileExists(String absoluteFilePath, int numberOfTries) throws Exception {
        boolean exists = false;
        boolean exceptionHasBeenPrinted = false;
        int count = 0;
        do {
            // Sleep 2 seconds
            if (count != 0) {
                Thread.sleep(3000);
                Log.info(thisClass, "fileExists", "waiting 2s...");
            }
            try {
                exists = server.getFileFromLibertyServerWithAbsoluteFilePath(absoluteFilePath).exists();
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

        // Make sure the mark is at the end of the log, so we don't use earlier messages.
        moveLogMark();

        // We need to put the base config back, otherwise the waits below will timeout on some tests
        configureServer("true", "10", true);

        // Delete all ltpa keys files in the security directory
        deleteFileIfExists(DEFAULT_KEY_PATH, false);
        deleteFileIfExists(VALIDATION_KEY1_PATH, true);
        deleteFileIfExists(VALIDATION_KEY2_PATH, true);
        deleteFileIfExists(CONFIGURED_VALIDATION_KEY1_PATH, true);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server.waitForStringInLog("CWWKS4105I", 5000));

        // Assert that a default ltpa.keys file exists prior to next test case
        assertFileWasCreated(DEFAULT_KEY_PATH);
        Log.info(thisClass, "resetServer", "exiting");
    }

    /**
     * Notify Liberty server of created, modified, and/or deleted files via mbean
     *
     * @param createdFilePaths
     * @param modifiedFilePaths
     * @param deletedFilePaths
     * @throws Exception
     */
    private static void notifyFileChangesWithMbean(List<String> createdFilePaths, List<String> modifiedFilePaths, List<String> deletedFilePaths) throws Exception {

        ObjectName appMBean = new ObjectName("WebSphere:service=com.ibm.ws.kernel.filemonitor.FileNotificationMBean");
        JMXConnector jmxConnector = server.getJMXRestConnector("user1", "user1pwd", "Liberty");
        MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();

        if (mbs.isRegistered(appMBean)) {

            String[] MBEAN_FILE_NOTIFICATION_NOTIFYFILECHANGES_SIGNATURE = new String[] { Collection.class.getName(),
                                                                                          Collection.class.getName(),
                                                                                          Collection.class.getName() };

            // Set MBean method notifyFileChanges parameters (createdFiles, modifiedFiles, deletedFiles)
            Object[] params = new Object[] { createdFilePaths, modifiedFilePaths, deletedFilePaths };

            Log.info(thisClass, "notifyFileChangesWithMbean", "Calling FileNotificationMBean notifyFileChanges");
            Log.info(thisClass, "notifyFileChangesWithMbean", "createdFilePaths: " + (createdFilePaths != null ? createdFilePaths.toString() : "null")
                                                              + "modifiedFilePaths: "
                                                              + (modifiedFilePaths != null ? modifiedFilePaths.toString() : "null")
                                                              + "deletedFilePaths: "
                                                              + (deletedFilePaths != null ? deletedFilePaths.toString() : "null"));
            // Invoke FileNotificationMBean method notifyFileChanges
            mbs.invoke(appMBean, "notifyFileChanges", params,
                       MBEAN_FILE_NOTIFICATION_NOTIFYFILECHANGES_SIGNATURE);
            Log.info(thisClass, "notifyFileChangesWithMbean", "Finished FileNotificationMBean notifyFileChanges");
        } else {
            Log.info(thisClass, "notifyFileChangesWithMbean", "FileNotificationMBean is not registered.");
            throw new Exception("FileNotificationMBean is not registered.");
        }

    }
}