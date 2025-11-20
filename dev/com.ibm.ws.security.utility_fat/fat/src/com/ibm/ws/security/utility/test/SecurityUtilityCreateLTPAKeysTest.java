/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/

package com.ibm.ws.security.utility.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import java.util.regex.Matcher;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Test class for the securityUtility createLTPAKeys command.
 * 
 * This test suite validates the functionality of the createLTPAKeys command, including:
 * - Creating LTPA key files in various locations (local, server)
 * - Testing command options (password, passwordKey, passwordEncoding)
 * - Validating error handling for invalid inputs
 * - Verifying server configuration and LTPA initialization
 */

@RunWith(FATRunner.class)
public class SecurityUtilityCreateLTPAKeysTest {
    private static final Class<?> thisClass = SecurityUtilityCreateLTPAKeysTest.class;
    
    // Liberty servers used for testing
    private static final String LTPA_TEST_SERVER_NAME = "LTPAKeysTestServer";
    private static LibertyServer ltpaTestServer = LibertyServerFactory.getLibertyServer(LTPA_TEST_SERVER_NAME);
    private static Machine testMachine;
    
    // Test environment properties
    private static Properties testEnvironment;
    private static String libertyInstallRoot;
    
    // Command return codes
    private static final int SUCCESS_RC = 0;
    private static final int FAILURE_RC = 1;
    private static final int SERVER_NOT_FOUND_RC = 2;
    private static final int FILE_EXISTS_RC = 5;
    private static String ltpaPassword = "WebAS";
    
    // File constants
    private static final String DEFAULT_SERVER_XML = "server.xml";
    private static final String DEFAULT_LTPA_KEY_FILE = "ltpa.keys";
    private static final String CUSTOM_LTPA_KEY_FILE = "custom_ltpa.keys";
    private static final String SERVER_LTPA_PATH = "resources/security/ltpa.keys";
    private static String securityUtilityPath;

    @Rule
    public TestName testName = new TestName();

    /**
     * Setup test environment before running any tests.
     * Initializes the test server, environment properties, and creates necessary directories.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        
        libertyInstallRoot = ltpaTestServer.getInstallRoot();
        securityUtilityPath = libertyInstallRoot + "/bin/securityUtility";
        testEnvironment = new Properties();
        testMachine = ltpaTestServer.getMachine();

        // Ensure resources/security directory exists for the test server
        File securityDir = new File(libertyInstallRoot + "/usr/servers/" + LTPA_TEST_SERVER_NAME + "/resources/security");
        if (!securityDir.exists()) {
            securityDir.mkdirs();
        }
    }

    /**
     * Clean up LTPA key files before each test to ensure a clean state.
     * Deletes any existing LTPA key files that might interfere with tests.
     */
    @Before
    public void cleanupBeforeTest() {
        // Delete default, custom, and server LTPA files before each test
        String[] ltpaPaths = {
            libertyInstallRoot + "/" + DEFAULT_LTPA_KEY_FILE,
            libertyInstallRoot + "/" + CUSTOM_LTPA_KEY_FILE,
            libertyInstallRoot + "/usr/servers/" + LTPA_TEST_SERVER_NAME + "/" + SERVER_LTPA_PATH,
            ltpaTestServer.pathToAutoFVTTestFiles + "overrides.xml"
        };
        for (String path : ltpaPaths) {
            deleteFileIfExists(path, "LTPA key file");
        }
    }

    /**
     * Clean up server directory after all tests complete.
     * Removes the entire test server directory.
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        // Delete the server directory
        File serverDir = new File(libertyInstallRoot + "/usr/servers/" + LTPA_TEST_SERVER_NAME);
        if (serverDir.exists()) {
            deleteDirectory(serverDir);
            Log.info(thisClass, "tearDown", "Deleted server directory: " + serverDir.getAbsolutePath());
        }
    }

    /**
     * Clean up any remaining LTPA key files after each test.
     * Removes all test artifacts to ensure a clean state for the next test.
     */
    @After
    public void tearDown() throws Exception {
        // Delete stray LTPA key files in common locations
        String[] filesToCleanup = {
            libertyInstallRoot + "/" + DEFAULT_LTPA_KEY_FILE,
            libertyInstallRoot + "/" + CUSTOM_LTPA_KEY_FILE,
            DEFAULT_LTPA_KEY_FILE,
            CUSTOM_LTPA_KEY_FILE,
            ltpaTestServer.pathToAutoFVTTestFiles + "server_ltpa_passwordEncoding.xml",
            ltpaTestServer.pathToAutoFVTTestFiles + "server_ltpa_passwordKey.xml",
            ltpaTestServer.pathToAutoFVTTestFiles + "server_ltpa.xml",
            ltpaTestServer.pathToAutoFVTTestFiles + "overrides.xml",
            ltpaTestServer.pathToAutoFVTTestFiles + "Secure_file1.xml",
            ltpaTestServer.pathToAutoFVTTestFiles + "temp_aes.xml"
        };
        for (String filePath : filesToCleanup) {
            deleteFileIfExists(filePath, "LTPA key file");
        }
        
        // Clean up any LTPA key files under wlp or build.image/wlp directories
        cleanupLTPAFiles(new File(libertyInstallRoot + "/wlp"), "wlp");
        File workspaceRootDir = new File(System.getProperty("user.dir")).getParentFile();
        cleanupLTPAFiles(new File(workspaceRootDir, "dev/build.image/wlp"), "build.image/wlp");
        cleanupLTPAFiles(new File(System.getProperty("user.dir"), "dev/build.image/wlp"), "alternate build.image/wlp");

        // Stop servers defensively
        if (ltpaTestServer.isStarted()) {
            ltpaTestServer.stopServer();
        }
    }

    //--------------------------------------------------------------------------
    // Helper Methods
    //--------------------------------------------------------------------------

    /**
     * Recursively deletes a directory and its contents.
     * 
     * @param dir Directory to delete
     */
    private static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }

    /**
     * Deletes a file if it exists, with logging.
     * 
     * @param filePath Path to the file to delete
     * @param fileDescription Description of the file for logging
     */
    private static void deleteFileIfExists(String filePath, String fileDescription) {
        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            Log.info(thisClass, "deleteFileIfExists",
                "Deleted " + fileDescription + ": " + file.getAbsolutePath() + " - Success: " + deleted);
        }
    }

    /**
     * Deletes all LTPA key files within a directory.
     * 
     * @param directory Directory to search for LTPA files
     * @param locationDescription Description of the location for logging
     */
    private static void cleanupLTPAFiles(File directory, String locationDescription) {
        if (directory.exists()) {
            File[] ltpaFiles = directory.listFiles((dir, name) ->
                name.equals(DEFAULT_LTPA_KEY_FILE) ||
                name.equals(CUSTOM_LTPA_KEY_FILE) ||
                name.endsWith(".keys"));
            if (ltpaFiles != null) {
                for (File file : ltpaFiles) {
                    boolean deleted = file.delete();
                    Log.info(thisClass, "cleanupLTPAFiles",
                        "Deleted " + locationDescription + " LTPA file: " + file.getName() + " - Success: " + deleted);
                }
            }
        }
    }

    /**
     * Writes the LTPA configuration snippet to a server override file.
     * 
     * @param ltpaSnippet The LTPA configuration XML snippet
     * @param server The Liberty server to apply the override to
     * @throws Exception If an error occurs writing the file
     */
    private void writeStringToServerOverride(String ltpaSnippet, LibertyServer server) throws Exception {
        File overrideFile = new File(ltpaTestServer.pathToAutoFVTTestFiles + "overrides.xml");
        overrideFile.delete();
        Files.write(overrideFile.toPath(), ltpaSnippet.getBytes(StandardCharsets.UTF_8), 
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        server.addDropinOverrideConfiguration(overrideFile.getName());
    }

    /**
     * Helper method to extract key value from XML content
     * 
     * @param xmlContent XML content containing a key value
     * @return The extracted key value
     */
    private String extractKeyValue(String xmlContent) {
        int valueStart = xmlContent.indexOf("value=\"") + 7;
        int valueEnd = xmlContent.indexOf("\"", valueStart);
        return xmlContent.substring(valueStart, valueEnd);
    }

    /**
     * Extracts the LTPA configuration snippet from command output and formats it for server override.
     * 
     * @param output The program output containing the LTPA snippet
     * @return A formatted XML snippet for server configuration
     */
    private String getLtpaOverride(ProgramOutput output, String aesKey) {
        Pattern ltpaPattern = Pattern.compile("<ltpa\\s+keysPassword=\"[^\"]+\"\\s*/>");
        Matcher ltpaMatcher = ltpaPattern.matcher(output.getStdout());
        assertTrue("Could not find ltpa snippet in stdout", ltpaMatcher.find());
        String ltpaSnippet = ltpaMatcher.group();

        if (aesKey.contains("wlp.password.encryption.key")){
            ltpaSnippet = "<server>\n" + "\n"+ aesKey +"\n"+ltpaSnippet + "\n</server>\n";
        }
        else if(aesKey!=""){
            ltpaSnippet = "<server>\n" + ltpaSnippet + "\n<variable name=\"wlp.aes.encryption.key\" value=\"" + aesKey + "\"/>\n</server>\n";}
        else{
            ltpaSnippet = "<server>\n" + ltpaSnippet + "\n</server>\n";}
        return ltpaSnippet;
    }

    //--------------------------------------------------------------------------
    // Test Methods - Basic Functionality
    //--------------------------------------------------------------------------

    /**
     * Test that verifies the createLTPAKeys command creates a local LTPA keys file
     * with default settings and base64key
     */
    @Test
    public void testCreateLocalLTPAKeys() throws Exception {
        
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        
        // Make sure the file doesn't exist before the test
        File ltpaFile = new File(libertyInstallRoot + "/" + DEFAULT_LTPA_KEY_FILE);
        if (ltpaFile.exists()) {
            ltpaFile.delete();
        }

        // Execute createLTPAKeys command for local file
        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] {
                "createLTPAKeys",
                "--password=" + ltpaPassword,
                "--passwordBase64Key=" + base64Key,
                "--passwordEncoding=aes"
            },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());


        // Verify createLTPAKeys command succeeded
        assertEquals("createLTPAKeys command should succeed", SUCCESS_RC, commandOutput.getReturnCode());
        
        // Verify LTPA keys file was created
        assertTrue("LTPA keys file should be created", ltpaFile.exists());
        
    }

    /**
     * Test that verifies the createLTPAKeys command creates a local LTPA keys file
     * with default settings and secure key file.
     */
    @Test
    public void testCreateLocalLTPAKeysFile() throws Exception {
          
        // Generate a random key for testing
        String specifiedKey = "myTestKey123";
        
        // Make sure the file doesn't exist before the test
        File ltpaFile = new File(libertyInstallRoot + "/" + DEFAULT_LTPA_KEY_FILE);
        if (ltpaFile.exists()) {
            ltpaFile.delete();
        }

        // Create a secure key file for testing
        File outputFile1 = new File(ltpaTestServer.pathToAutoFVTTestFiles, "Secure_file1.xml");
        
        // Generate secure file
        ProgramOutput firstCommandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { "generateAESKey", "--key=" + specifiedKey, "--createConfigFile=" + outputFile1.getAbsolutePath() },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + firstCommandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + firstCommandOutput.getReturnCode());

        // Verify generate command succeeded
        assertEquals("generateAESKey command should succeed", SUCCESS_RC, firstCommandOutput.getReturnCode());
        assertTrue("Secure key file should be created by generateAESKey: " + outputFile1.getAbsolutePath(), 
                  outputFile1.exists());

        // Execute createLTPAKeys command for local file
        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] {
                "createLTPAKeys",
                "--password=" + ltpaPassword,
                "--passwordEncoding=aes",
                "--aesConfigFile=" + outputFile1.getAbsolutePath(),
            },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        // Verify createLTPAKeys command succeeded
        assertEquals("createLTPAKeys command should succeed", SUCCESS_RC, commandOutput.getReturnCode());
        
        // Verify LTPA keys file was created
        assertTrue("LTPA keys file should be created", ltpaFile.exists());
        
        // Verify Secure keys file was created
        assertTrue("Secure key file should be created", outputFile1.exists());
    }
    
    /**
     * Test that verifies the createLTPAKeys command creates an LTPA keys file with a custom name.
     */
    @Test
    public void testCreateCustomLTPAKeysFile() throws Exception {
    
        // Make sure the custom LTPA keys file doesn't exist before the test
        File customLtpaFile = new File(libertyInstallRoot + "/" + CUSTOM_LTPA_KEY_FILE);
        if (customLtpaFile.exists()) {
            customLtpaFile.delete();
        }
        assertFalse("Custom LTPA keys file should not exist before test", customLtpaFile.exists());
        
        // Execute createLTPAKeys command with custom file name
        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] {
                "createLTPAKeys",
                "--file=" + CUSTOM_LTPA_KEY_FILE,
                "--password=" + ltpaPassword
            },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        // Verify command succeeded
        assertEquals("createLTPAKeys command should succeed", SUCCESS_RC, commandOutput.getReturnCode());
        
        // Verify custom LTPA keys file was created
        assertTrue("Custom LTPA keys file should be created", customLtpaFile.exists());
    }

    /**
     * Test that verifies createLTPAKeys creates ltpa.keys directly in the server's resources/security
     * and that the server starts and loads those keys using the snippet from stdout. This creates an AES_V2 key using --passwordBase64Key.
     */
    @Test
    public void testCreateServerLTPAKeys() throws Exception {
        
         // Create a secure key file for testing
        File outputFile1 = new File(ltpaTestServer.pathToAutoFVTTestFiles, "temp_aes.xml");

        // Generate AES base64 key for wlp.aes.encryption.key
        ProgramOutput aesKeyOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { "generateAESKey", "--createConfigFile=" + outputFile1.getAbsolutePath(), "--key="+"KeyString2"},
            libertyInstallRoot, testEnvironment);
        
        Log.info(thisClass, testName.getMethodName(), "stderr:\n" + aesKeyOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + aesKeyOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + aesKeyOutput.getReturnCode());
        assertEquals("AES key generation should succeed", SUCCESS_RC, aesKeyOutput.getReturnCode());

        String aesKeyFileContent = new String(Files.readAllBytes(
            Paths.get(outputFile1.getAbsolutePath())), StandardCharsets.UTF_8);
        String aesEncryptionKey = extractKeyValue(aesKeyFileContent);

        
        // Run createLTPAKeys and capture output
        ProgramOutput output = testMachine.execute(
            securityUtilityPath,
            new String[] { "createLTPAKeys", "--server=" + LTPA_TEST_SERVER_NAME,
                "--password=" + ltpaPassword,
                "--passwordBase64Key=" + aesEncryptionKey,
                "--passwordEncoding=aes"},
            libertyInstallRoot, testEnvironment);
        assertEquals("createLTPAKeys should succeed", SUCCESS_RC, output.getReturnCode());

        // Extract LTPA configuration and apply to server
        String ltpaSnippet = getLtpaOverride(output, aesEncryptionKey);
        writeStringToServerOverride(ltpaSnippet, ltpaTestServer);
        
        // Start the server (ltpa.keys is already in resources/security)
        ltpaTestServer.startServer();

        // Verify startup log contains LTPA initialization
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      ltpaTestServer.waitForStringInLogUsingMark("CWWKS4105I", 5000));
        ltpaTestServer.stopServer();

        if (!JavaInfo.forCurrentVM().isCriuSupported()) {
            // skip testing InstantOn if CRIU is not supported on this platform
            return;
        }

        try {
            // clean up previous overrides file before checkpoint
            deleteFileIfExists(ltpaTestServer.pathToAutoFVTTestFiles + "overrides.xml", "overrides");

            // do checkpoint
            ltpaTestServer.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
            ltpaTestServer.startServer("checkpoint-test.log");

            // Configure the AES key again
            writeStringToServerOverride(ltpaSnippet, ltpaTestServer);
            // Restore from checkpoint after configuring the AES key 
            ltpaTestServer.checkpointRestore();
            // Verify startup log contains LTPA initialization
            assertNotNull("Expected LTPA configuration ready message not found in the log.",
                    ltpaTestServer.waitForStringInLogUsingMark("CWWKS4105I", 5000));
            ltpaTestServer.stopServer();
        } finally {
            ltpaTestServer.unsetCheckpoint();
        }
    }

    //--------------------------------------------------------------------------
    // Test Methods - Advanced Options
    //--------------------------------------------------------------------------

    /**
     * Test that verifies the createLTPAKeys command with the --passwordKey option.
     * This tests custom encryption key functionality for LTPA keys.
     */
    @Test
    public void testCreateLTPAKeysWithPasswordKey() throws Exception {

        String customKey = "myCustomEncryptionKey";

        // Run createLTPAKeys with passwordKey and capture output
        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] {
                "createLTPAKeys",
                "--server=" + LTPA_TEST_SERVER_NAME,
                "--password=" + ltpaPassword,
                "--passwordKey=" + customKey,
                "--passwordEncoding=aes"
            },
            libertyInstallRoot,
            testEnvironment);
        assertEquals("createLTPAKeys should succeed", SUCCESS_RC, commandOutput.getReturnCode());

        // Build ltpa.keys path for the server
        String ltpaPath = ltpaTestServer.getInstallRoot() + "/usr/servers/" + LTPA_TEST_SERVER_NAME + "/resources/security/ltpa.keys";
        File ltpaFile = new File(ltpaPath);
        assertTrue("LTPA keys file missing: " + ltpaPath, ltpaFile.exists());

        // Build server.xml using the snippet
        String ltpaSnippet = getLtpaOverride(commandOutput, "<variable name=\"wlp.password.encryption.key\" value=\""+customKey+"\" />");
        writeStringToServerOverride(ltpaSnippet, ltpaTestServer);
        
        // Start the server
        ltpaTestServer.startServer();

        // Verify startup log contains LTPA initialization
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      ltpaTestServer.waitForStringInLogUsingMark("CWWKS4105I", 5000));
        ltpaTestServer.stopServer();

        if (!JavaInfo.forCurrentVM().isCriuSupported()) {
            // skip testing InstantOn if CRIU is not supported on this platform
            return;
        }

        try {
            // clean up previous overrides file before checkpoint
            deleteFileIfExists(ltpaTestServer.pathToAutoFVTTestFiles + "overrides.xml", "overrides");

            // do checkpoint
            ltpaTestServer.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
            ltpaTestServer.startServer("checkpoint-test.log");

            // Configure the AES key again
            writeStringToServerOverride(ltpaSnippet, ltpaTestServer);
            // Restore from checkpoint after configuring the AES key 
            ltpaTestServer.checkpointRestore();
            // Verify startup log contains LTPA initialization
            assertNotNull("Expected LTPA configuration ready message not found in the log.",
                    ltpaTestServer.waitForStringInLogUsingMark("CWWKS4105I", 5000));
            ltpaTestServer.stopServer();
        } finally {
            ltpaTestServer.unsetCheckpoint();
        }
    }

    /**
     * Test that verifies the createLTPAKeys command with password encoding options.
     * This tests the ability to specify different password encoding algorithms.
     */
    @Test
    public void testCreateLTPAKeysWithPasswordEncoding() throws Exception {

        // Run createLTPAKeys with passwordEncoding=aes for the test server
        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] {
                "createLTPAKeys",
                "--password=" + ltpaPassword,
                "--passwordEncoding=aes",
                "--server=" + LTPA_TEST_SERVER_NAME
            },
            libertyInstallRoot,
            testEnvironment);
        assertEquals("createLTPAKeys should succeed", SUCCESS_RC, commandOutput.getReturnCode());

        // Build server.xml using the snippet
        String ltpaSnippet = getLtpaOverride(commandOutput, "");
        writeStringToServerOverride(ltpaSnippet, ltpaTestServer);
        
        // Start the server
        ltpaTestServer.startServer();

        // Verify startup log contains LTPA initialization
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      ltpaTestServer.waitForStringInLogUsingMark("CWWKS4105I", 5000));
        ltpaTestServer.stopServer();
    }

    //--------------------------------------------------------------------------
    // Test Methods - Error Handling
    //--------------------------------------------------------------------------

        /**
     * Test that verifies the createLTPAKeys command fails as the --passwordBase64Key argument or the --aesConfigFile argument must be specified, but not both.
     * with default settings and secure key file.
     */
    @Test
    public void testCreateLocalLTPAKeysMutuallyExclusive() throws Exception {
        
        String specifiedKey = "myTestKey123";
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        
        // Make sure the file doesn't exist before the test
        File ltpaFile = new File(libertyInstallRoot + "/" + DEFAULT_LTPA_KEY_FILE);
        if (ltpaFile.exists()) {
            ltpaFile.delete();
        }

        // Create a secure key file for testing
        File outputFile1 = new File(ltpaTestServer.pathToAutoFVTTestFiles, "Secure_file1.xml");
        
        // Generate secure file
        ProgramOutput firstCommandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { "generateAESKey", "--key=" + specifiedKey, "--createConfigFile=" + outputFile1.getAbsolutePath() },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + firstCommandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + firstCommandOutput.getReturnCode());

        // Verify generateAESKey command succeeded
        assertEquals("generateAESKey command should succeed", SUCCESS_RC, firstCommandOutput.getReturnCode());
        assertTrue("Secure key file should be created by generateAESKey: " + outputFile1.getAbsolutePath(), 
                  outputFile1.exists());

        // Execute createLTPAKeys command for local file
        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] {
                "createLTPAKeys",
                "--password=" + ltpaPassword,
                "--passwordBase64Key=" + base64Key,
                "--aesConfigFile=" + outputFile1.getAbsolutePath(),
            },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        // Verify createLTPAKeys command fail
        assertEquals("createLTPAKeys command should fail", FAILURE_RC, commandOutput.getReturnCode());
        

    }
    
    /**
     * Test that verifies the createLTPAKeys command fails when server doesn't exist.
     */
    @Test
    public void testCreateLTPAKeysNonExistentServer() throws Exception {
        
        String nonExistentServer = "NonExistentServer";
        
        // Execute createLTPAKeys command with non-existent server
        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { 
                "createLTPAKeys", 
                "--server=" + nonExistentServer, 
                "--password=" + ltpaPassword 
            },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        // Verify command failed with SERVER_NOT_FOUND_RC
        assertEquals("createLTPAKeys command should fail for non-existent server", 
                    SERVER_NOT_FOUND_RC, commandOutput.getReturnCode());
    }
    
    /**
     * Test that verifies the createLTPAKeys command fails when LTPA keys file already exists.
     */
    @Test
    public void testCreateLTPAKeysFileExists() throws Exception {
        
        // Create a dummy LTPA keys file first to simulate file exists scenario
        File ltpaFile = new File(libertyInstallRoot + "/" + DEFAULT_LTPA_KEY_FILE);
        ltpaFile.createNewFile();
        assertTrue("LTPA keys file should exist before test", ltpaFile.exists());
        
        // Try to create the LTPA keys file when it already exists
        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] {
                "createLTPAKeys",
                "--password=" + ltpaPassword
            },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        // Verify command failed with FILE_EXISTS_RC
        assertEquals("createLTPAKeys command should fail because file exists",
                    FILE_EXISTS_RC, commandOutput.getReturnCode());
    }
    
    /**
     * Test that verifies the createLTPAKeys command fails with missing required arguments.
     */
    @Test
    public void testCreateLTPAKeysMissingPassword() throws Exception {

        // Execute createLTPAKeys command without password
        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { 
                "createLTPAKeys"
            },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        // Verify command failed
        assertEquals("createLTPAKeys command should fail with missing password", 
                    FAILURE_RC, commandOutput.getReturnCode());
    }
    
    /**
     * Test that verifies the createLTPAKeys command fails with mutually exclusive arguments
     * (--server and --file).
     */
    @Test
    public void testCreateLTPAKeysExclusiveArgs() throws Exception {
        
        // Execute createLTPAKeys command with both --server and --file
        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { 
                "createLTPAKeys", 
                "--server=" + LTPA_TEST_SERVER_NAME,
                "--file=" + CUSTOM_LTPA_KEY_FILE,
                "--password=" + ltpaPassword 
            },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        // Verify command failed
        assertEquals("createLTPAKeys command should fail with exclusive arguments", 
                    FAILURE_RC, commandOutput.getReturnCode());
    }
}
