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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class FATTest {

    private static final String APP_NAME = "ltpaTest";
    private static final String DEFAULT_KEY_PATH = "resources/security/ltpa.keys";
    private static final String ALTERNATE_KEY_PATH = "resources/security/alternate/testLtpa.keys";
    private static final String REPLACEMENT_LTPA_KEYS_PATH = "alternate/ltpa.keys";
    private static final String CORRUPTED_LTPA_KEYS_PATH = "corrupted/ltpa.keys";
    private static final String DEFAULT_SERVER_XML = "server.xml";
    private static final String ALTERNATE_SERVER_XML = "alternate/server.xml";
    private static final String ALTERNATE_SERVER_XML_WITH_LTPA_FILE_MONITOR = "alternate/serverWithLTPAFileMonitor.xml";
    private static final String ALTERNATE_SERVER_XML_WITH_LTPA_FILE_MONITOR_AND_WRONG_PASSWORD = "alternate/serverWithLTPAFileMonitorAndWrongPassword.xml";
    private static final String PWD_DEFAULT = "WebAS";
    private static final String PWD_DEFAULT_ENCODED = "\\{xor\\}CDo9Hgw=";
    private static final String PWD_ANY_ENCODED = "\\{xor\\}";
    private static final String PWD_ANOTHER = "anotherPwd";
    private static final String PWD_WRONG = "wrongPassword";
    private static final String serverShutdownMessages = "CWWKS4106E";
    private static final LibertyServer server;
    static {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat");
        try {
            server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/ltpafattestlibertyinternals-1.0.mf");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Rule
    public TestRule passwordChecker = new LeakedPasswordChecker(server);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        server.addInstalledAppForValidation(APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        deleteExistingLTPAKeysFiles();
    }

    @After
    public void tearDown() throws Exception {
        try {
            server.stopServer(serverShutdownMessages);
        } finally {
            deleteExistingLTPAKeysFiles();
        }
    }

    private void deleteExistingLTPAKeysFiles() throws Exception {
        deleteFileIfExists(DEFAULT_KEY_PATH);
        deleteFileIfExists(ALTERNATE_KEY_PATH);
    }

    /**
     * Validate that the LTPA service will generate a default LTPA key file
     * if the LTPA key file does not exist.
     */
    @CheckForLeakedPasswords({ PWD_DEFAULT, PWD_DEFAULT_ENCODED })
    @Test
    public void genDefaultLTPAKeyFile() throws Exception {
        startServerWithConfigFileAndLog(DEFAULT_SERVER_XML, "genDefaultLTPAKeyFile.log");
        assertFeatureCompleteWithKeysGeneratedAndTestApp(DEFAULT_KEY_PATH);
        assertTokenCanBeCreated();
        assertFileWasCreated(DEFAULT_KEY_PATH);
    }

    /**
     * Validate that the LTPA service will generate the LTPA key file
     * if the LTPA key file does not exist (and it is not the default
     * key file name).
     */
    @CheckForLeakedPasswords({ PWD_DEFAULT, PWD_DEFAULT_ENCODED })
    @Test
    public void genAlternateLTPAKeyFile() throws Exception {
        startServerWithConfigFileAndLog(ALTERNATE_SERVER_XML, "genAlternateLTPAKeyFile.log");
        assertFeatureCompleteWithKeysGeneratedAndTestApp(ALTERNATE_KEY_PATH);
        assertTokenCanBeCreated();
        assertFileWasCreated(ALTERNATE_KEY_PATH);
    }

    /**
     * Validate that the LTPA service will generate the LTPA key file
     * if the LTPA key file does not exist (and it is not the default
     * key file name).
     */
    @CheckForLeakedPasswords({ PWD_DEFAULT, PWD_DEFAULT_ENCODED })
    @Test
    public void genAlternateLTPAKeyFileWithoutRestart() throws Exception {
        startServerWithConfigFileAndLog(DEFAULT_SERVER_XML, "genAlternateLTPAKeyFile.log");
        assertFeatureCompleteWithKeysGeneratedAndTestApp(DEFAULT_KEY_PATH);
        assertTokenCanBeCreated();
        assertFileWasCreated(DEFAULT_KEY_PATH);

        // NOW change to the alternate file
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(ALTERNATE_SERVER_XML);
        assertKeysGenerated(ALTERNATE_KEY_PATH);

        // Check to see if the file has been regenerated
        assertFileWasCreated(ALTERNATE_KEY_PATH);
    }

    /**
     * Validate that the LTPA keys are reloaded after modifying the LTPA keys file.
     */
    @CheckForLeakedPasswords({ PWD_DEFAULT, PWD_ANOTHER, PWD_ANY_ENCODED })
    @Test
    public void validateKeysReloadedAfterModification() throws Exception {
        startServerWithConfigFileAndLog(DEFAULT_SERVER_XML, "validateKeysReloadedAfterModification.log");
        assertFeatureCompleteWithLTPAConfigAndTestApp();
        assertTokenCanBeCreated();

        replaceLTPAKeysFile(ALTERNATE_SERVER_XML_WITH_LTPA_FILE_MONITOR, REPLACEMENT_LTPA_KEYS_PATH);
        assertLTPAConfigurationReady();
        assertAppDoesNotRestart();

        // Assert token can be created with new keys
        assertTokenCanBeCreated();
    }

    /**
     * Validate that the LTPA keys are not reloaded after modifying the LTPA keys file
     * if the server.xml has the wrong password.
     * The FFDCs for javax.crypto.BadPaddingException exception are expected since
     * the code will fail to properly decrypt the LTPA keys with the wrong password.
     */
    @CheckForLeakedPasswords({ PWD_WRONG, PWD_ANY_ENCODED })
    @ExpectedFFDC("javax.crypto.BadPaddingException")
    @Test
    public void validateKeysNotReloadedAfterModificationWithWrongPassword() throws Exception {
        startServerWithConfigFileAndLog(DEFAULT_SERVER_XML, "validateKeysNotReloadedAfterModificationWithWrongPassword.log");
        assertFeatureCompleteWithLTPAConfigAndTestApp();
        assertTokenCanBeCreated();

        replaceLTPAKeysFile(ALTERNATE_SERVER_XML_WITH_LTPA_FILE_MONITOR_AND_WRONG_PASSWORD, REPLACEMENT_LTPA_KEYS_PATH);

        assertNotNull("The LTPA configuration must not be reloaded.",
                      server.waitForStringInLog("CWWKS4106E:.*"));

        // Assert token can be created with old keys
        assertTokenCanBeCreated();
    }

    /**
     * Validate that the LTPA keys are not reloaded after modifying the LTPA keys file
     * with corrupted keys.
     * The FFDCs for java.lang.IllegalArgumentException exception are expected since
     * the code will fail to properly decode the corrupted LTPA keys.
     */
    @CheckForLeakedPasswords({ PWD_DEFAULT, PWD_ANOTHER, PWD_ANY_ENCODED })
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    @Test
    public void validateKeysNotReloadedAfterModificationWithCorruptedKeysFile() throws Exception {
        startServerWithConfigFileAndLog(DEFAULT_SERVER_XML, "validateKeysNotReloadedAfterModificationWithCorruptedKeysFile.log");
        assertFeatureCompleteWithLTPAConfigAndTestApp();
        assertTokenCanBeCreated();

        replaceLTPAKeysFile(ALTERNATE_SERVER_XML_WITH_LTPA_FILE_MONITOR, CORRUPTED_LTPA_KEYS_PATH);

        assertNotNull("The LTPA configuration must not be reloaded.",
                      server.waitForStringInLog("CWWKS4106E:.*"));

        // Assert token can be created with old keys
        assertTokenCanBeCreated();
    }

    /**
     * Test API method: com.ibm.wsspi.security.token.WSSecurityPropagationHelper.validateToken
     * Uses: TokenAPIServlet
     * 4 Test parts included below
     * 1. valid user and realm
     * 2. valid user and custom realm as attribute (will be parsed correctly)
     * 3. null token passed into validateToken
     * 4. invalid token passed into validateToken
     */
    @ExpectedFFDC("com.ibm.websphere.security.auth.InvalidTokenException")
    @Test
    public void testWSSecurityPropagationHelper_validateToken() throws Exception {
        startServerWithConfigFileAndLog(DEFAULT_SERVER_XML, "genDefaultLTPAKeyFile.log");
        assertFeatureCompleteWithLTPAConfigAndTestApp();
        assertTokenCanBeCreated();

        //Positive Tests
        testWSSecurityPropagationHelper_validateToken_simple();
        testWSSecurityPropagationhelper_validateToken_customRealm();

        //Negative Tests
        testWSSecurityPropagationhelper_validateToken_null();
        testWSSecurityPropagationhelper_validateToken_invalidToken();

    }

    /**
     * Test API method: com.ibm.wsspi.security.token.WSSecurityPropagationHelper.validateToken
     * Uses: TokenAPIServlet
     * Verify the servlet gets expected user and realm returned from the token
     * test: valid user and realm
     */
    public void testWSSecurityPropagationHelper_validateToken_simple() throws Exception {

        String expectedUniqueId = "user:basicRegistryRealm/kevin";
        String expectedUser = "kevin";
        String expectedRealm = "basicRegistryRealm";
        String testParameters = "uniqueID=" + expectedUniqueId;
        assertValidateTokenMethodResults(expectedUniqueId, expectedUser, expectedRealm, testParameters);
    }

    /**
     * Test API method: com.ibm.wsspi.security.token.WSSecurityPropagationHelper.validateToken
     * Uses: TokenAPIServlet
     * Verify the servlet gets expected user and realm returned from the token
     * test: valid user and custom realm as attribute (will be parsed correctly)
     */
    public void testWSSecurityPropagationhelper_validateToken_customRealm() throws Exception {

        String expectedUniqueId = "user:https://test.com/kevin";
        String expectedUser = "kevin";
        String expectedRealm = "https://test.com";
        String testParameters = "uniqueID=" + expectedUniqueId + "&customRealm=" + expectedRealm;
        assertValidateTokenMethodResults(expectedUniqueId, expectedUser, expectedRealm, testParameters);
    }

    /**
     * Test API method: com.ibm.wsspi.security.token.WSSecurityPropagationHelper.validateToken
     * Uses: TokenAPIServlet
     * Verify the servlet gets expected exception
     * test: null token passed into validateToken
     */
    public void testWSSecurityPropagationhelper_validateToken_null() throws Exception {
        String testParameters = "uniqueID=NullToken";

        String expectedExceptionMessage = "Invalid token, token returned from validation is null.";

        assertValidateTokenMethodException(expectedExceptionMessage, testParameters);
    }

    /**
     * Test API method: com.ibm.wsspi.security.token.WSSecurityPropagationHelper.validateToken
     * Uses: TokenAPIServlet
     * Verify the servlet gets expected Exception
     * ExpectedFFDC("com.ibm.websphere.security.auth.InvalidTokenException")
     * test: invalid token passed into validateToken
     */
    public void testWSSecurityPropagationhelper_validateToken_invalidToken() throws Exception {
        String testParameters = "uniqueID=InvalidToken";

        String expectedExceptionMessage = "CWWKS4001I: The security token cannot be validated.";

        assertValidateTokenMethodException(expectedExceptionMessage, testParameters);
    }

    private String getValidateTokenMethodResults(String testParameters) throws Exception {
        HttpURLConnection con = null;
        try {
            String baseURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME +
                             "/TokenAPIServlet" + "?";
            URL url = new URL(baseURL + testParameters);
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            assertNotNull(is);

            String output = read(is);
            return output;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private void assertValidateTokenMethodResults(String expectedUniqueId, String expectedUser, String expectedRealm, String testParameters) {
        try {
            String results = getValidateTokenMethodResults(testParameters);
            assertTrue("Expected UniqueId: " + expectedUniqueId + " but recieved:\n" + results, results.contains("UniqueId Output: " + expectedUniqueId));
            assertTrue("Expected User: " + expectedUser + " but recieved:\n" + results, results.contains("User from uniqueId: " + expectedUser));
            assertTrue("Expected Realm: " + expectedRealm + " but recieved:\n" + results, results.contains("Realm from uniqueId: " + expectedRealm));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void assertValidateTokenMethodException(String expectedExceptionMessage, String testParameters) {
        try {
            String results = getValidateTokenMethodResults(testParameters);
            assertTrue("Expected Exception Message: " + expectedExceptionMessage + " but recieved:\n" + results, results.contains(expectedExceptionMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

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

    /**
     * Asserts that the feature update is complete, there is an LTPA configuration,
     * and the test application was installed. Requires info trace.
     * The call order of the methods invoked must be preserved since the check for
     * the LTPA configuration readiness depends on the offset in the console log.
     */
    private void assertFeatureCompleteWithLTPAConfigAndTestApp() {
        assertApplicationStarted();
        assertLTPAConfigurationReady();
        assertFeatureUpdateComplete();
    }

    private void assertApplicationStarted() {
        assertNotNull("Application ltpaTest does not appear to have started.",
                      server.waitForStringInLog("CWWKZ0001I:.*ltpaTest"));
    }

    private void assertFeatureUpdateComplete() {
        assertNotNull("The app start will cause the token bundle to start. The token bundle did not start.",
                      server.waitForStringInLog("CWWKF0008I:.*"));
    }

    private void assertKeysGenerated(String generatedLTPAKeysPath) {
        assertNotNull("We need to wait for the LTPA keys to be generated at " + generatedLTPAKeysPath + ", but we did not recieve the message",
                      server.waitForStringInLog("CWWKS4104A:.*" + generatedLTPAKeysPath));
    }

    /**
     * Checks that the LTPA configuration is ready. It looks in the console log
     * starting from the last offset.
     */
    private void assertLTPAConfigurationReady() {
        assertNotNull("The LTPA configuration must be ready.",
                      server.waitForStringInLogUsingMark("CWWKS4105I:.*"));
    }

    /**
     * Checks that the LTPA configuration is ready. It looks in the console log
     * starting from the last offset.
     */
    private void assertAppDoesNotRestart() {
        assertNull("The application should not restart",
                   server.waitForStringInLog("CWWKT0017I:", 5000));
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

    private void assertFileWasCreated(String filePath) {
        Assert.assertTrue(fileExists(filePath));
    }

    /**
     * To replace the LTPA keys file,
     *
     * <pre>
     * 1. Update server.xml to enable LTPA file monitoring
     * 2. Copy replacement LTPA keys file to the location specified in the ltpa element in server.xml
     * </pre>
     */
    private void replaceLTPAKeysFile(String fileNameOfServerXMLWithLTPAKeysFileMonitoring, String sourceLTPAKeys) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(fileNameOfServerXMLWithLTPAKeysFileMonitoring);
        assertNotNull("The server configuration must be updated.",
                      server.waitForStringInLog("CWWKG0017I:.*"));
        String serverRoot = server.getServerRoot();
        String securityResources = serverRoot + "/resources/security";
        server.setServerRoot(securityResources);
        server.copyFileToLibertyServerRoot(sourceLTPAKeys);
        server.setServerRoot(serverRoot);
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
     * Delete the file if it exists. If we can't delete it, then
     * throw an exception as we need to be able to delete these files.
     *
     * @param filePath
     *
     * @throws Exception
     */
    private void deleteFileIfExists(String filePath) throws Exception {
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
     * Check to see if the file exists. We will wait a bit to ensure
     * that the system was not slow to flush the file.
     *
     * @param filePath
     *
     * @return
     */
    private boolean fileExists(String filePath) {
        try {
            RemoteFile remote = server.getFileFromLibertyServerRoot(filePath);
            boolean exists = false;
            int count = 0;
            do {
                //sleep half a second
                Thread.sleep(500);
                exists = remote.exists();
                count++;
            }
            //wait up to 10 seconds for the key file to appear
            while ((!exists) && count < 20);

            return exists;

        } catch (Exception e) {
            // assume the file does not exist and move on
        }

        // if we make it here assume it does not exists
        return false;
    }

}
