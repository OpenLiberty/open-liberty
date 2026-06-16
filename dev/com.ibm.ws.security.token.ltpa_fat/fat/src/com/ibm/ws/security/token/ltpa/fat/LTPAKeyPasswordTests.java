/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;

/**
 *
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LTPAKeyPasswordTests {

    private static final Class<?> thisClass = LTPAKeyPasswordTests.class;

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.ltpaKeyPasswordTestServer");
    private static final FormLoginClient formLoginClient = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin");
    private static final List<String> commonServerShutdownMessages = Arrays.asList("CWWKO0801E");
    private static final List<String> serverShutdownMessages = new ArrayList<>();

    private static final String USER1 = "user1";
    private static final String USER1PWD = "user1pwd";

    private static final String LTPA_KEYS_LOCATION = "resources/security/ltpa.keys";
    private static final String LTPA_KEYS_BACKUP_LOCATION = "resources/security/ltpa.keys.defaultpassword.backup";
    private static final String SERVER_ENV_LOCATION = "server.env";

    private static final String LTPA_CONFIG_WEBAS = "ltpaKeyPasswordTests/ltpaConfigWithKeysPassword_WebAS.xml";
    private static final String LTPA_CONFIG_MYKEYSPASSWORD = "ltpaKeyPasswordTests/ltpaConfigWithKeysPassword_myKeysPassword.xml"; // pragma: allowlist secret

    private static final String KEYSTORE_PASSWORD_NAME = "keystore_password";
    private static final String KEYSTORE_PASSWORD_ENTRY = KEYSTORE_PASSWORD_NAME + "=myKeystorePassword";

    private static final String LTPA_KEYS_PASSWORD_NAME = "ltpa_keys_password";
    private static final String LTPA_KEYS_PASSWORD_ENTRY = LTPA_KEYS_PASSWORD_NAME + "=myLtpaKeysPassword";

    private static String FIPS140_3_FOLDER = "fips140-3/";
    private static String LTPA_KEYS_WEBAS = "WebAS/ltpa.keys";
    private static String LTPA_KEYS_MYKEYSPASSWORD = "myKeysPassword/ltpa.keys"; // pragma: allowlist secret
    private static String LTPA_KEYS_MYLTPAKEYSPASSWORD = "myLtpaKeysPassword/ltpa.keys"; // pragma: allowlist secret
    private static String LTPA_KEYS_MYKEYSTOREPASSWORD = "myKeystorePassword/ltpa.keys"; // pragma: allowlist secret

    @Rule
    public TestRule passwordChecker = new LeakedPasswordChecker(server);

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nEntering test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nExiting test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }
    };

    @BeforeClass
    public static void beforeClass() throws Exception {
        Log.info(thisClass, "beforeClass()", "entering");

        // With repeats, we run `beforeClass` for each one, however the value of paths as statics is kept, so once we have updated paths
        // to include the fips directory we don't want to update them another time
        if (server.isFIPS140_3EnabledAndSupported() && !LTPA_KEYS_MYKEYSTOREPASSWORD.startsWith(FIPS140_3_FOLDER)) {
            LTPA_KEYS_WEBAS = FIPS140_3_FOLDER + LTPA_KEYS_WEBAS;
            LTPA_KEYS_MYKEYSPASSWORD = FIPS140_3_FOLDER + LTPA_KEYS_MYKEYSPASSWORD;
            LTPA_KEYS_MYLTPAKEYSPASSWORD = FIPS140_3_FOLDER + LTPA_KEYS_MYLTPAKEYSPASSWORD;
            LTPA_KEYS_MYKEYSTOREPASSWORD = FIPS140_3_FOLDER + LTPA_KEYS_MYKEYSTOREPASSWORD;
        }

        // Transform the application for EE9+ that was copied
        // from com.ibm.ws.webcontainer.security_test.servlets.
        if (JakartaEEAction.isEE9OrLaterActive()) {
            JakartaEEAction.transformApp(Paths.get(server.getServerRoot() + "/apps/formlogin.war"));
        }

        // Now that this test is repeated and these clients are static this is necessary
        // to avoid "Manager is shut down.". The @AfterClass method is executed at the end of each repeat
        // which calls the shutdown() method on the ConnectionManager.
        formLoginClient.resetClientState();

        Log.info(thisClass, "beforeClass()", "exiting");
    }

    @Before
    public void before() throws Exception {
        Log.info(thisClass, "before()", "entering");

        serverShutdownMessages.addAll(commonServerShutdownMessages);
        server.addEnvVar(LTPA_KEYS_PASSWORD_NAME, ""); // make sure fattest.simplicity doesn't set it

        Log.info(thisClass, "before()", "exiting");
    }

    @After
    public void after() throws Exception {
        Log.info(thisClass, "after()", "entering");

        try {
            formLoginClient.resetClientState();
            server.stopServer(serverShutdownMessages.toArray(new String[0]));
        } finally {
            serverShutdownMessages.clear();
            server.deleteFileFromLibertyServerRoot(LTPA_KEYS_LOCATION);
            server.deleteFileFromLibertyServerRoot(LTPA_KEYS_BACKUP_LOCATION);
            server.deleteFileFromLibertyServerRoot(SERVER_ENV_LOCATION);
            server.deleteAllDropinConfigurations();
        }

        Log.info(thisClass, "after()", "exiting");
    }

    @AfterClass
    public static void shutdown() throws Exception {
        Log.info(thisClass, "shutdown()", "entering");

        formLoginClient.releaseClient();

        Log.info(thisClass, "shutdown()", "exiting");
    }

    /**
     * Basic test to ensure that setting <ltpa keysPassword="..." /> works. // pragma: allowlist secret
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testLtpaKeyPassword_serverXml_keysPassword() throws Exception {
        copyLtpaKeysIntoServer(LTPA_KEYS_MYKEYSPASSWORD);
        server.addDropinOverrideConfiguration(LTPA_CONFIG_MYKEYSPASSWORD);
        server.startServer(true);

        verifyLtpaConfigurationReadyMessageFound();
        verifySuccessfulFormLogin();
    }

    /**
     * Basic test to ensure that setting <ltpa keysPassword="WebAS" /> still works. // pragma: allowlist secret
     *
     * @throws Exception
     */
    @Test
    public void testLtpaKeyPassword_serverXml_keysPassword_WebAS() throws Exception {
        copyLtpaKeysIntoServer(LTPA_KEYS_WEBAS);
        server.addDropinOverrideConfiguration(LTPA_CONFIG_WEBAS);
        server.startServer(true);

        verifyReEncryptingLtpaKeysMessageNotFound();
        verifyLtpaConfigurationReadyMessageFound();
        verifySuccessfulFormLogin();
    }

    /**
     * Basic test to ensure that setting ltpa_keys_password in the server.env works.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testLtpaKeyPassword_serverEnv_ltpaKeysPassword() throws Exception {
        copyLtpaKeysIntoServer(LTPA_KEYS_MYLTPAKEYSPASSWORD);
        createServerEnvFile(LTPA_KEYS_PASSWORD_ENTRY);
        server.startServer(true);

        verifyLtpaConfigurationReadyMessageFound();
        verifySuccessfulFormLogin();
    }

    /**
     * Basic test to ensure that setting keystore_passwird in the server.env works.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testLtpaKeyPassword_serverEnv_keystorePassword() throws Exception {
        copyLtpaKeysIntoServer(LTPA_KEYS_MYKEYSTOREPASSWORD);
        createServerEnvFile(KEYSTORE_PASSWORD_ENTRY);
        server.startServer(true);

        verifyLtpaConfigurationReadyMessageFound();
        verifySuccessfulFormLogin();
    }

    /**
     * Basic test to ensure that not setting any of <ltpa keysPassword="..." />, ltpa_keys_password, and keystore_password throws an error. // pragma: allowlist secret
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC({ "java.lang.IllegalArgumentException",
                    "com.ibm.websphere.security.auth.TokenCreationFailedException",
                    "javax.security.auth.login.CredentialException", })
    public void testLtpaKeyPassword_noPassword() throws Exception {
        createServerEnvFile();
        server.startServer(true);

        verifyLtpaPasswordNotSetErrorMessageFound();
        verifyUnsuccessfulFormLogin();
    }

    /**
     * Basic test to ensure that setting the wrong password throws an error.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC({ "javax.crypto.BadPaddingException",
                    "javax.security.auth.login.CredentialException",
                    "java.lang.IllegalArgumentException",
                    "com.ibm.websphere.security.auth.TokenCreationFailedException" })
    public void testLtpaKeyPassword_wrongPassword() throws Exception {
        copyLtpaKeysIntoServer(LTPA_KEYS_WEBAS);
        server.addDropinOverrideConfiguration(LTPA_CONFIG_MYKEYSPASSWORD);
        server.startServer(true);

        verifyUnableToReadOrCreateLtpaKeysErrorMessageFound();
        verifyUnsuccessfulFormLogin();
    }

    /**
     * Test to ensure that the <ltpa keysPassword="..." /> server config takes precedence over the ltpa_keys_password env variable. // pragma: allowlist secret
     *
     * @throws Exception
     */
    @Test
    public void testLtpaKeyPassword_serverXml_keysPassword_takesPrecedenceOver_serverEnv_ltpaKeysPassword() throws Exception {
        copyLtpaKeysIntoServer(LTPA_KEYS_MYKEYSPASSWORD);
        server.addDropinOverrideConfiguration(LTPA_CONFIG_MYKEYSPASSWORD);
        createServerEnvFile(LTPA_KEYS_PASSWORD_ENTRY);
        server.startServer(true);

        verifyLtpaConfigurationReadyMessageFound();
        verifySuccessfulFormLogin();
    }

    /**
     * Test to ensure that the <ltpa keysPassword="..." /> server config takes precedence over the keystore_password env variable. // pragma: allowlist secret
     *
     * @throws Exception
     */
    @Test
    public void testLtpaKeyPassword_serverXml_keysPassword_takesPrecedenceOver_serverEnv_keystorePassword() throws Exception {
        copyLtpaKeysIntoServer(LTPA_KEYS_MYKEYSPASSWORD);
        server.addDropinOverrideConfiguration(LTPA_CONFIG_MYKEYSPASSWORD);
        createServerEnvFile(KEYSTORE_PASSWORD_ENTRY);
        server.startServer(true);

        verifyLtpaConfigurationReadyMessageFound();
        verifySuccessfulFormLogin();
    }

    /**
     * Test to ensure that the ltpa_keys_password env variable takes precedence over the keystore_password env variable.
     *
     * @throws Exception
     */
    @Test
    public void testLtpaKeyPassword_serverEnv_ltpaKeysPassword_takesPrecedenceOver_serverEnv_keystorePassword() throws Exception {
        copyLtpaKeysIntoServer(LTPA_KEYS_MYLTPAKEYSPASSWORD);
        createServerEnvFile(LTPA_KEYS_PASSWORD_ENTRY, KEYSTORE_PASSWORD_ENTRY);
        server.startServer(true);

        verifyLtpaConfigurationReadyMessageFound();
        verifySuccessfulFormLogin();
    }

    /**
     * Test to ensure the ltpa.keys are re-encrypted with the keystore_password when it was encrypted using WebAS and the keystore_password is used as the current password.
     *
     * @throws Exception
     */
    @Test
    public void testLtpaKeyPassword_reencryptLtpaKeys() throws Exception {
        // - copy in the WebAS ltpa.keys
        // - set <ltpa keysPassword="WebAS" /> and keystore_password (keysPassword takes precedence) // pragma: allowlist secret
        // - verify a successful form login and save ltpa token
        copyLtpaKeysIntoServer(LTPA_KEYS_WEBAS);
        server.addDropinOverrideConfiguration(LTPA_CONFIG_WEBAS);
        createServerEnvFile(KEYSTORE_PASSWORD_ENTRY);
        server.startServer(true);

        verifyLtpaConfigurationReadyMessageFound();
        String cookie = verifySuccessfulFormLogin();

        // - delete the <ltpa /> configuration
        // - keystore_password is still set
        // - restart server
        // - verify ltpa keys are re-encrypted and ltpa token still valid
        boolean deleteDropinConfigurations = true;
        restartServer(deleteDropinConfigurations);

        verifyReEncryptingLtpaKeysMessageFound();
        verifySuccessfullyReEncryptedLtpaKeysMessageFound();
        verifyLtpaConfigurationReadyMessageFound();
        verifySuccessfulFormLoginWithCookie(cookie);

        // - restart server one more time (config unchanged)
        // - verify re-encrypted ltpa keys can be read and ltpa token still valid
        deleteDropinConfigurations = false;
        restartServer(deleteDropinConfigurations);

        verifyReEncryptingLtpaKeysMessageNotFound();
        verifyLtpaConfigurationReadyMessageFound();
        verifySuccessfulFormLoginWithCookie(cookie);
    }

    /**
     * Test to ensure the ltpa.keys are not re-encrypted when it wasn't encrypted with WebAS and the keystore_password is used as the current password.
     * This should be pretty much the same scenario as an incorrect password.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "javax.crypto.BadPaddingException",
                    "javax.security.auth.login.CredentialException",
                    "java.lang.IllegalArgumentException",
                    "com.ibm.websphere.security.auth.TokenCreationFailedException" })
    public void testLtpaKeyPassword_reencryptLtpaKeys_notWebAS() throws Exception {
        copyLtpaKeysIntoServer(LTPA_KEYS_MYKEYSPASSWORD);
        createServerEnvFile(KEYSTORE_PASSWORD_ENTRY);
        server.startServer(true);

        verifyReEncryptingLtpaKeysMessageNotFound();
        verifyUnableToReadOrCreateLtpaKeysErrorMessageFound();
        verifyUnsuccessfulFormLogin();
    }

    private void copyLtpaKeysIntoServer(String ltpaKeysFile) throws Exception {
        server.copyFileToLibertyServerRoot("resources/security", "ltpaKeyPasswordTests/" + ltpaKeysFile);
    }

    private void createServerEnvFile(String... entries) throws Exception {
        File file = new File(server.getServerRoot() + File.separator + SERVER_ENV_LOCATION);
        try (Writer output = new BufferedWriter(new FileWriter(file))) {
            for (String entry : entries) {
                output.write(entry + System.lineSeparator());
            }
        }
    }

    private void verifyLtpaConfigurationReadyMessageFound() {
        assertNotNull("Expected LTPA configuration ready message not found in the logs.", server.waitForStringInLog("CWWKS4105I"));
    }

    private void verifyUnableToReadOrCreateLtpaKeysErrorMessageFound() {
        assertNotNull("Expected unable to create or read LTPA keys error message not found in the logs.", server.waitForStringInLog("CWWKS4106E"));
        serverShutdownMessages.add("CWWKS4106E");
    }

    private void verifyLtpaTokenServiceNotFoundErrorMessageFound() {
        assertNotNull("Expected LTPA TokenService not found error message not found in the logs.", server.waitForStringInLog("CWWKS4000E"));
        serverShutdownMessages.add("CWWKS4000E");
    }

    private void verifyLtpaPasswordNotSetErrorMessageFound() {
        assertNotNull("Expected LTPA password not set error message not found in the logs.", server.waitForStringInLog("CWWKS4118E"));
        serverShutdownMessages.add("CWWKS4118E");
    }

    private void verifyReEncryptingLtpaKeysMessageFound() {
        assertNotNull("Expected re-encrypting LTPA keys message not found in the logs.", server.waitForStringInLog("CWWKS4119I"));
    }

    private void verifyReEncryptingLtpaKeysMessageNotFound() {
        assertNull("Unexpected re-encrypting LTPA keys message found in the logs.", server.waitForStringInLog("CWWKS4119I"));
    }

    private void verifySuccessfullyReEncryptedLtpaKeysMessageFound() {
        assertNotNull("Expected successfully re-encrypted LTPA keys message not found in the logs.", server.waitForStringInLog("CWWKS4120I"));
    }

    private String verifySuccessfulFormLogin() {
        formLoginClient.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, USER1, USER1PWD);
        String cookie = formLoginClient.getCookieFromLastLogin();
        verifySuccessfulFormLoginWithCookie(cookie);
        return cookie;
    }

    private void verifySuccessfulFormLoginWithCookie(String cookie) {
        formLoginClient.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie);
    }

    private void verifyUnsuccessfulFormLogin() {
        formLoginClient.accessProtectedServletWithoutLtpaServiceReady(FormLoginClient.PROTECTED_SIMPLE, USER1, USER1PWD);
        verifyLtpaTokenServiceNotFoundErrorMessageFound();
    }

    private void restartServer(boolean deleteDropinConfigurations) throws Exception {
        server.stopServer();
        if (deleteDropinConfigurations) {
            server.deleteAllDropinConfigurations();
        }
        server.addEnvVar(LTPA_KEYS_PASSWORD_NAME, ""); // make sure fattest.simplicity doesn't set it
        server.startServer(true);
    }

}
