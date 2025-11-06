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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.SecureRandom;
import java.util.Base64;

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
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Tests for securityUtility createSSLCertificate command.
 * 
 * This test suite validates the functionality of the createSSLCertificate command, including:
 * - Creating SSL certificates for servers and clients
 * - Testing various command options (keyType, passwordEncoding, subject, validity)
 * - Verifying certificate properties (key size, signature algorithm, subject alternative names)
 * - Testing error handling for invalid inputs
 * - Validating server and client configuration with generated certificates
 */

@RunWith(FATRunner.class)
public class SecurityUtilityCreateSSLCertificateTest {

    private static final Class<?> thisClass = SecurityUtilityCreateSSLCertificateTest.class;

    // Liberty servers and clients used for testing
    private static final String SSL_TEST_SERVER_NAME = "SSLCertTestServer";
    private static final String SSL_TEST_CLIENT_NAME = "mySSLCmdClient";
    
    private static LibertyClient testClient = LibertyClientFactory.getLibertyClient(SSL_TEST_CLIENT_NAME);
    private static LibertyServer sslTestServer = LibertyServerFactory.getLibertyServer(SSL_TEST_SERVER_NAME);

    // Test environment properties
    private static Machine testMachine;
    private static Properties testEnvironment;
    private static String libertyInstallRoot;
    private static String autoFVTpath;

    // Command return codes
    private static final int SUCCESS_RC = 0;
    private static final int FAILURE_RC = 1;
    private static final int SERVER_NOT_FOUND_RC = 2;
    private static final int KEYSTORE_EXISTS_RC = 5;

    // Keystore paths
    private static final String KEYSTORE_PATH = "resources/security/key.p12";
    private static final String KEYSTORE_PATH_JKS = "resources/security/key.jks";
    private String securityUtilityPath = libertyInstallRoot + "/bin/securityUtility";

    private static final String testOutputDir = sslTestServer.getInstallRoot() + "/output";

    @Rule
    public TestName testName = new TestName();

    //--------------------------------------------------------------------------
    // Setup and Teardown Methods
    //--------------------------------------------------------------------------

    /**
     * Setup test environment before running any tests.
     * Initializes servers, clients, and creates necessary directories.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        libertyInstallRoot = sslTestServer.getInstallRoot();
        autoFVTpath = sslTestServer.pathToAutoFVTTestFiles;
        testMachine = sslTestServer.getMachine();
        testEnvironment = new Properties();
        testEnvironment.put("JVM_ARGS", "-Dcom.ibm.ws.beta.edition=true");

        // Create security directories for test servers and clients
        new File(libertyInstallRoot + "/usr/clients/" + SSL_TEST_CLIENT_NAME + "/resources/security").mkdirs();
        new File(libertyInstallRoot + "/usr/servers/" + SSL_TEST_SERVER_NAME + "/resources/security").mkdirs();
    }

    /**
     * Clean up keystores before each test to ensure a clean state.
     * Deletes any existing keystores that might interfere with tests.
     */
    @Before
    public void cleanupBeforeTest() throws Exception{
        // Clean up server and client keystores before each test
        deleteFileIfExists(libertyInstallRoot + "/usr/clients/" + SSL_TEST_CLIENT_NAME + "/" + KEYSTORE_PATH);
        deleteFileIfExists(libertyInstallRoot + "/usr/clients/" + SSL_TEST_CLIENT_NAME + "/" + KEYSTORE_PATH_JKS);

        deleteFileIfExists(libertyInstallRoot + "/usr/servers/" + SSL_TEST_SERVER_NAME + "/" + KEYSTORE_PATH);
        deleteFileIfExists(libertyInstallRoot + "/usr/servers/" + SSL_TEST_SERVER_NAME + "/" + KEYSTORE_PATH_JKS);

        deleteFileIfExists(sslTestServer.pathToAutoFVTTestFiles + "overrides.xml");

        if (!Files.exists(Paths.get(testOutputDir))) {
            Files.createDirectories(Paths.get(testOutputDir));
        }
    }

    /**
     * Clean up configuration files after each test.
     * Removes test artifacts and stops any running servers.
     */
    @After
    public void cleanupAfterTest() throws Exception {
        deleteFileIfExists(sslTestServer.pathToAutoFVTTestFiles + "sslCertTestServer.xml");
        deleteFileIfExists(sslTestServer.pathToAutoFVTTestFiles + "serverks.xml");
        deleteFileIfExists(sslTestServer.pathToAutoFVTTestFiles + "overrides.xml");
        deleteFileIfExists(sslTestServer.pathToAutoFVTTestFiles + "overrides.xml");
        deleteFileIfExists(sslTestServer.pathToAutoFVTTestFiles + "temp_aes.xml");
        
        // Stop servers defensively
        if (sslTestServer.isStarted()) {
            sslTestServer.stopServer();
        }
    }

    /**
     * Clean up all test artifacts after all tests complete.
     * Removes keystores, configuration files, and stops any running servers.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        Log.info(thisClass, "teardownClass", "Starting cleanup");
        
        // Defensive server stops
        try {
            if (sslTestServer.isStarted()) {
                sslTestServer.stopServer();
            }
        } catch (Exception e) {
            Log.info(thisClass, "teardownClass", "Failed to stop sslTestServer", e);
        }
        Log.info(thisClass, "teardownClass", "Stopping sslTestServer...");

        // Clean up keystore and config files in common locations
        String[] filesToDelete = { 
            libertyInstallRoot + "/key.p12", 
            libertyInstallRoot + "/key.jks",
            "key.p12", 
            "key.jks" 
        };
        for (String path : filesToDelete) {
            deleteFileIfExists(path);
        }

        // Clean up wlp and build.image directories
        cleanupKeystoreFiles(new File(libertyInstallRoot + "/wlp"), "wlp");
        cleanupConfigFiles(new File(libertyInstallRoot + "/wlp"), "wlp");
        deleteFileIfExists(sslTestServer.pathToAutoFVTTestFiles + "earDD.ear");
    }

    //--------------------------------------------------------------------------
    // File and Directory Helper Methods
    //--------------------------------------------------------------------------

    /**
     * Creates a directory if it doesn't exist.
     * 
     * @param path Path to the directory to create
     */
    private static void createDirectoryIfMissing(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Deletes a file if it exists, with logging.
     * 
     * @param filePath Path to the file to delete
     */
    private static void deleteFileIfExists(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            Log.info(thisClass, "deleteFileIfExists", "Deleted " + file.getAbsolutePath() + " - Success: " + deleted);
        }
    }

    /**
     * Deletes a file if it exists, with logging.
     * 
     * @param filePath Path to the file to delete
     * @param description Description of the file for logging
     */
    private static void deleteFileIfExists(String filePath, String description) {
        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            Log.info(thisClass, "deleteFileIfExists", 
                    "Deleted " + description + ": " + file.getAbsolutePath() + " - Success: " + deleted);
        }
    }

    /**
     * Cleans up keystore files in a directory.
     * 
     * @param directory Directory to search for keystore files
     * @param locationDesc Description of the location for logging
     */
    private static void cleanupKeystoreFiles(File directory, String locationDesc) {
        if (!directory.exists()) return;
        File[] keystores = directory.listFiles((dir, name) -> name.equals("key.p12") || name.equals("key.jks"));
        if (keystores != null) {
            for (File f : keystores) {
                boolean deleted = f.delete();
                Log.info(thisClass, "cleanupKeystoreFiles", 
                        "Deleted " + locationDesc + " keystore file: " + f.getName() + " - Success: " + deleted);
            }
        }
    }

    /**
     * Cleans up XML configuration files in a directory.
     * 
     * @param directory Directory to search for config files
     * @param locationDesc Description of the location for logging
     */
    private static void cleanupConfigFiles(File directory, String locationDesc) {
        if (!directory.exists()) return;
        File[] configs = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        if (configs != null) {
            for (File f : configs) {
                boolean deleted = f.delete();
                Log.info(thisClass, "cleanupConfigFiles", 
                        "Deleted " + locationDesc + " config file: " + f.getName() + " - Success: " + deleted);
            }
        }
    }

    /**
     * Recursively deletes a directory and its contents.
     * 
     * @param directory Directory to delete
     */
    private static void deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            Log.info(thisClass, "deleteDirectory", "Directory does not exist: " + directory);
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    boolean deleted = f.delete();
                    Log.info(thisClass, "deleteDirectory",
                            "Deleted file: " + f.getAbsolutePath() + " - Success: " + deleted);
                }
            }
        }

        boolean dirDeleted = directory.delete();
        Log.info(thisClass, "deleteDirectory",
                "Deleted directory: " + directory.getAbsolutePath() + " - Success: " + dirDeleted);
    }

    /**
     * Helper method to delete a file.
     * 
     * @param path Path to the file to delete
     */
    private static void deleteFile(String path) {
        File f = new File(path);
        if (f.exists()) f.delete();
    }

    //--------------------------------------------------------------------------
    // Command and Configuration Helper Methods
    //--------------------------------------------------------------------------

    /**
     * Executes the createSSLCertificate command with the given arguments.
     *
     * @param args Command arguments
     * @return Program output from command execution
     */
    private ProgramOutput runCreateSSLCert(String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "createSSLCertificate";
        System.arraycopy(args, 0, cmd, 1, args.length);

        ProgramOutput commandOutput = testMachine.execute(
                libertyInstallRoot + "/bin/securityUtility",
                cmd,
                libertyInstallRoot,
                testEnvironment);

        Log.info(thisClass, testName.getMethodName(),
                "stderr:\n" + commandOutput.getStderr() +
                        "\nstdout:\n" + commandOutput.getStdout() +
                        "\nRC: " + commandOutput.getReturnCode());
        return commandOutput;
    }

    /**
     * Extracts the keyStore configuration snippet from command output and formats it for server override.
     * 
     * @param output The program output containing the keyStore snippet
     * @return A formatted XML snippet for server configuration
     */
    private String formatXmlInclude(ProgramOutput output, String aesKey) {
        // Extract <keyStore> snippet from stdout
        Pattern ksPattern = Pattern.compile(
            "<keyStore\\b[^>]*\\bid=\"defaultKeyStore\"[^>]*\\bpassword=\"\\{[a-zA-Z0-9]+\\}[^\"\\s>]+\"[^>]*/>",
            Pattern.DOTALL
        );
        Matcher matcher = ksPattern.matcher(output.getStdout());
        assertTrue("Could not find keyStore snippet in stdout", matcher.find());
        String keystoreSnippet = matcher.group();
        if (aesKey.contains("wlp.password.encryption.key")){
            keystoreSnippet = "<server>\n" + "\n"+ aesKey +"\n"+keystoreSnippet + "\n</server>\n";
        }
        else if(aesKey!=""){
            keystoreSnippet = "<server>\n" + keystoreSnippet + "\n<variable name=\"wlp.aes.encryption.key\" value=\"" + aesKey + "\"/>\n</server>\n";}
        else{
            keystoreSnippet = "<server>\n" + keystoreSnippet + "\n</server>\n";}
        
        return keystoreSnippet;
    }

    
    /**
     * Creates a configuration snippet that includes the specified config file.
     * 
     * @param configFilePath Path to the configuration file to include
     * @return A formatted XML snippet for server configuration
     */
    private String formatXmlConfigInclude(String configFilePath) {
        String configSnippet = "<server>\n <include location=\"" + configFilePath + "\"/>\n</server>\n";
        return configSnippet;
    }

    /**
     * Writes a configuration snippet to a server override file.
     * 
     * @param configSnippet The XML configuration snippet
     * @param server The Liberty server to apply the override to
     * @throws Exception If an error occurs writing the file
     */
    private void writeStringToServerOverride(String configSnippet, LibertyServer server) throws Exception {
        File overrideFile = new File(server.pathToAutoFVTTestFiles + "overrides.xml");
        overrideFile.delete();
        Files.write(overrideFile.toPath(), configSnippet.getBytes(StandardCharsets.UTF_8), 
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        server.addDropinOverrideConfiguration(overrideFile.getName());
    }

    /**
     * Writes a configuration snippet to a client override file.
     * 
     * @param configSnippet The XML configuration snippet
     * @param client The Liberty client to apply the override to
     * @throws Exception If an error occurs writing the file
     */
    private void writeStringToClientOverride(String configSnippet, LibertyClient client) throws Exception {
        File overrideFile = new File(client.pathToAutoFVTTestFiles + "overrides.xml");
        overrideFile.delete();
        Files.write(overrideFile.toPath(), configSnippet.getBytes(StandardCharsets.UTF_8), 
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        client.addDropinOverrideConfiguration(overrideFile.getName());
    }

    /**
     * Configures and starts a server with the keystore configuration from command output.
     * 
     * @param commandOutput Output from createSSLCertificate command
     * @param server Server to configure and start
     * @throws Exception If an error occurs during server configuration or startup
     */
    private void runserver(ProgramOutput commandOutput, LibertyServer server, String aesKey) throws Exception {
        String ksSnippet = formatXmlInclude(commandOutput, aesKey);
        writeStringToServerOverride(ksSnippet, server);

        // Deploy and start the server 
        server.startServer();
        
        // Verify keystore loaded successfully
        String keystoreLoadedMessage = server.waitForStringInLogUsingMark("Successfully loaded default keystore", 5000);
        assertTrue("Server did not log Successfully loaded default keystore", keystoreLoadedMessage != null);
        
        server.stopServer();
    }

    /**
     * Configures and checkpoints and restores a server with the keystore configuration from command output.
     * 
     * @param commandOutput Output from createSSLCertificate command
     * @param server Server to configure and start
     * @throws Exception If an error occurs during server configuration or startup
     */
    private void runCheckpointServer(ProgramOutput commandOutput, LibertyServer server, String aesKey) throws Exception {
        try {
            if (!JavaInfo.forCurrentVM().isCriuSupported()) {
                // skip testing InstantOn if CRIU is not supported on this platform
                return;
            }
            // clean up previous overrides file before checkpoint
            deleteFileIfExists(sslTestServer.pathToAutoFVTTestFiles + "overrides.xml");

            // do checkpoint
            server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
            server.startServer("checkpoint-test.log");

            // configure AES key after checkpoint
            String ksSnippet = formatXmlInclude(commandOutput, aesKey);
            writeStringToServerOverride(ksSnippet, server);

            // Restore from checkpoint after configuring the AES key 
            server.checkpointRestore();

            // Verify keystore loaded successfully
            String keystoreLoadedMessage = server.waitForStringInLogUsingMark("Successfully loaded default keystore", 5000);
            assertTrue("Server did not log Successfully loaded default keystore", keystoreLoadedMessage != null);

            server.stopServer();
        } finally {
            server.unsetCheckpoint();
        }
    }

    /**
     * Configures and starts a client with the keystore configuration from command output.
     *
     * @param commandOutput Output from createSSLCertificate command
     * @param client Client to configure and start
     * @throws Exception If an error occurs during client configuration or startup
     */
    private void runclient(ProgramOutput commandOutput, LibertyClient client) throws Exception {

        String ksSnippet = formatXmlInclude(commandOutput, "");
        writeStringToClientOverride(ksSnippet, client);

        try {
            // Start client and validate as usual
            ProgramOutput clientOutput = client.startClient();
            String clientStdout = clientOutput.getStdout();
            Log.info(thisClass, testName.getMethodName(), "stdout:\n" + clientStdout);

            // Check for common error messages
            String keystorePasswordError = "CWPKI0033E"; // Keystore password is incorrect
            String xmlParserError = "CWWKG0014E"; // XML parser error

            assertTrue("The client has issues with the keystore",
                !(clientStdout.contains(keystorePasswordError)) &&
                !(clientStdout.contains(xmlParserError)));

        } catch (Exception e) {
            // Suppress only CWWKS9702W warning failures (Open Liberty CSIv2 client policy)
            if (e.getMessage() != null && e.getMessage().contains("CWWKS9702W")) {
                Log.info(thisClass, testName.getMethodName(),
                    "Ignoring known harmless Open Liberty warning: CWWKS9702W");
            } else {
                throw e; // rethrow other exceptions
            }
        }
    }
    //--------------------------------------------------------------------------
    // Positive Test Cases
    //--------------------------------------------------------------------------

    /**
     * Tests creating an SSL certificate for a server and verifies it works.
     */
    @Test
    public void testCreateCertificateForServer() throws Exception {
        ProgramOutput commandOutput = runCreateSSLCert(
            "--server=" + SSL_TEST_SERVER_NAME, 
            "--password=Liberty"
        );
        int returnCode = commandOutput.getReturnCode();
        assertTrue(returnCode == SUCCESS_RC);

        File keystoreFile = new File(libertyInstallRoot + "/usr/servers/" + SSL_TEST_SERVER_NAME + "/" + KEYSTORE_PATH);
        assertTrue("Server keystore should exist", keystoreFile.exists());

        runserver(commandOutput, sslTestServer, "");
    }
    
    /**
     * Tests creating an SSL certificate for a client and verifies it works.
     */
    @Test
    public void testCreateCertificateForClient() throws Exception {
        ProgramOutput commandOutput = runCreateSSLCert(
            "--client=" + SSL_TEST_CLIENT_NAME, 
            "--password=Liberty"
        );
        int returnCode = commandOutput.getReturnCode();
        assertTrue(returnCode == SUCCESS_RC);

        File keystoreFile = new File(libertyInstallRoot + "/usr/clients/" + SSL_TEST_CLIENT_NAME + "/" + KEYSTORE_PATH);
        assertTrue("Client keystore should exist", keystoreFile.exists());

        runclient(commandOutput, testClient);
    }

    /**
     * Tests creating an SSL certificate with JKS keystore type.
     */
    @Test
    public void testCreateWithKeyTypeJKS() throws Exception {
        File jksFile = new File(libertyInstallRoot + "/usr/servers/" + SSL_TEST_SERVER_NAME + "/" + KEYSTORE_PATH_JKS);
        if (jksFile.exists()) {
            assertTrue("Failed to delete existing key.jks", jksFile.delete());
        }
        
        ProgramOutput commandOutput = runCreateSSLCert(
            "--server=" + SSL_TEST_SERVER_NAME,
            "--password=Liberty",
            "--keyType=JKS"
        );
        assertEquals(SUCCESS_RC, commandOutput.getReturnCode());
        
        runserver(commandOutput, sslTestServer, "");
    }

    /**
     * Tests creating an SSL certificate with AES password encoding and custom key.
     */
    @Test
    public void testCreateWithPasswordEncodingAESV1() throws Exception {
        ProgramOutput commandOutput = runCreateSSLCert(
            "--server=" + SSL_TEST_SERVER_NAME,
            "--password=Liberty",
            "--passwordEncoding=aes",
            "--passwordKey=MySecretKey123"
        );
        assertEquals(SUCCESS_RC, commandOutput.getReturnCode());
        Log.info(thisClass, testName.getMethodName(), "stderr:\n" + commandOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        String aesEncryptionKey = "<variable name=\"wlp.password.encryption.key\" value=\"MySecretKey123\" />";

        runserver(commandOutput, sslTestServer, aesEncryptionKey);

        runCheckpointServer(commandOutput, sslTestServer, aesEncryptionKey);
    }

    /**
     * Tests creating an SSL certificate with AES V2 password encoding and a custom key.
     */
    @Test
    public void testCreateWithPasswordEncodingAESV2() throws Exception {

        // Create a secure key file for testing
        File outputFile1 = new File(sslTestServer.pathToAutoFVTTestFiles, "temp_aes.xml");

        // Generate AES base64 key for wlp.aes.encryption.key
        ProgramOutput aesKeyOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { "generateAESKey", "--createConfigFile=" + outputFile1.getAbsolutePath(), "--key="+"keystring1234"},
            libertyInstallRoot, testEnvironment);
        
        Log.info(thisClass, testName.getMethodName(), "stderr:\n" + aesKeyOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + aesKeyOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + aesKeyOutput.getReturnCode());
        assertEquals("AES key generation should succeed", SUCCESS_RC, aesKeyOutput.getReturnCode());

        String aesKeyFileContent = new String(Files.readAllBytes(
            Paths.get(outputFile1.getAbsolutePath())), StandardCharsets.UTF_8);
        String aesEncryptionKey = extractKeyValue(aesKeyFileContent);


        ProgramOutput commandOutput = runCreateSSLCert(
            "--server=" + SSL_TEST_SERVER_NAME,
            "--password=Liberty",
            "--passwordEncoding=aes",
            "--passwordBase64Key="+aesEncryptionKey
        );

        assertEquals(SUCCESS_RC, commandOutput.getReturnCode());
        Log.info(thisClass, testName.getMethodName(), "stderr:\n" + commandOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());
        
        

        runserver(commandOutput, sslTestServer, aesEncryptionKey);

        runCheckpointServer(commandOutput, sslTestServer, aesEncryptionKey);
    }

    /**
     * Tests creating an SSL certificate with AES V2 using an aesConfigFile and a custom key.
     */
    @Test
    public void testCreateWithPasswordEncodingAESV2ConfigFile() throws Exception {

        String securityUtilityPath = libertyInstallRoot + "/bin/securityUtility";

        // A random key for testing
        String specifiedKey = "myTestKey123";
    
        // Create a secure key file for testing
        File outputFile1 = new File(sslTestServer.pathToAutoFVTTestFiles, "Secure_file1.xml");
        
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

        ProgramOutput commandOutput = runCreateSSLCert(
            "--server=" + SSL_TEST_SERVER_NAME,
            "--password=Liberty",
            "--passwordEncoding=aes",
            "--aesConfigFile="+outputFile1.getAbsolutePath()
        );
        assertEquals(SUCCESS_RC, commandOutput.getReturnCode());
    }

    
    /**
     * Tests creating an SSL certificate with custom subject and validity period.
     */
    @Test
    public void testCreateWithSubjectAndValidity() throws Exception {
        ProgramOutput commandOutput = runCreateSSLCert(
            "--client=" + SSL_TEST_CLIENT_NAME,
            "--password=Liberty",
            "--subject=CN=CustomSubject,OU=QA,O=IBM,C=US",
            "--validity=730"
        );
        assertEquals(SUCCESS_RC, commandOutput.getReturnCode());
    }

    /**
     * Tests creating an SSL certificate with a separate config file.
     */
    @Test
    public void testCreateConfigFileOption() throws Exception {
        File configFile = new File(libertyInstallRoot + "/usr/servers/" + SSL_TEST_SERVER_NAME + "/resources/security/serverks.xml");
        if (configFile.exists()) configFile.delete();

        ProgramOutput commandOutput = runCreateSSLCert(
            "--server=" + SSL_TEST_SERVER_NAME,
            "--password=Liberty",
            "--passwordEncoding=aes",
            "--createConfigFile=" + configFile.getAbsolutePath()
        );
        assertEquals(SUCCESS_RC, commandOutput.getReturnCode());
        assertTrue("Config file should be created", configFile.exists());

        String configSnippet = formatXmlConfigInclude(configFile.getAbsolutePath());
        writeStringToServerOverride(configSnippet, sslTestServer);

        sslTestServer.startServer();
        
        // Verify keystore loaded successfully
        String keystoreLoadedMessage = sslTestServer.waitForStringInLogUsingMark("Successfully loaded default keystore", 5000);
        assertTrue("Server did not log Successfully loaded default keystore", keystoreLoadedMessage != null);
        
        sslTestServer.stopServer();
    }

    /**
     * Tests creating an SSL certificate with extended information (SAN, key size, signature algorithm).
     */
    @Test
    public void testCreateSSLCertificateWithExtInfo() throws Exception {
        // Create the certificate with extended information
        String subjectAltNames = "san=dns:localhost,ip:127.0.0.1";
        ProgramOutput commandOutput = runCreateSSLCert(
            "--server=" + SSL_TEST_SERVER_NAME,
            "--password=Liberty",
            "--extInfo=" + subjectAltNames,
            "--keySize=4096",
            "--sigAlg=SHA384withRSA"
        );

        assertEquals("Command should succeed", SUCCESS_RC, commandOutput.getReturnCode());

        // Verify keystore exists
        File keystoreFile = new File(libertyInstallRoot + "/usr/servers/" + SSL_TEST_SERVER_NAME + "/" + KEYSTORE_PATH);
        assertTrue("Keystore should exist", keystoreFile.exists());

        // Parse the certificate to validate properties
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(fis, "Liberty".toCharArray());

            String alias = keystore.aliases().nextElement();
            X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);

            // Verify Subject Alternative Names
            Collection<List<?>> subjectAltNamesList = cert.getSubjectAlternativeNames();
            boolean hasDnsName = subjectAltNamesList.stream().anyMatch(l -> l.get(1).equals("localhost"));
            boolean hasIpAddress = subjectAltNamesList.stream().anyMatch(l -> l.get(1).equals("127.0.0.1"));
            assertTrue("SAN should include localhost DNS", hasDnsName);
            assertTrue("SAN should include 127.0.0.1 IP", hasIpAddress);

            // Verify key size and signature algorithm
            assertEquals("Signature algorithm mismatch", "SHA384withRSA", cert.getSigAlgName());
            assertEquals("Key size mismatch", 4096, ((RSAPublicKey) cert.getPublicKey()).getModulus().bitLength());
        }
        catch (Exception e) {
            fail("Cannot read keystore file: " + e.getMessage());
        }
    }

    /**
     * Tests that the command supports a very large key size.
     */
    @Test
    public void testCreateSSLCertificateWithLargeKeySize() throws Exception {
        ProgramOutput commandOutput = testMachine.execute(
            libertyInstallRoot + "/bin/securityUtility",
            new String[] { 
                "createSSLCertificate", 
                "--server=" + SSL_TEST_SERVER_NAME,
                "--password=Liberty", 
                "--keySize=8192" 
            },
            libertyInstallRoot, 
            testEnvironment
        );

        int returnCode = commandOutput.getReturnCode();
        assertTrue("Large key size should succeed or indicate existing keystore",
                returnCode == SUCCESS_RC);
    }

    //--------------------------------------------------------------------------
    // Negative Test Cases
    //--------------------------------------------------------------------------

    /**
     * Tests that the command fails with invalid arguments.
     */
    @Test
    public void testInvalidArguments() throws Exception {
        ProgramOutput commandOutput = runCreateSSLCert("--badArg");
        assertEquals(FAILURE_RC, commandOutput.getReturnCode());
    }

    /**
     * Tests that the command fails when server doesn't exist.
     */
    @Test
    public void testMissingServer() throws Exception {
        ProgramOutput commandOutput = runCreateSSLCert("--server=NoSuchServer", "--password=Liberty");
        assertEquals(SERVER_NOT_FOUND_RC, commandOutput.getReturnCode());
    }

    /**
     * Tests that the command fails when keystore already exists.
     */
    @Test
    public void testDuplicateKeystore() throws Exception {
        runCreateSSLCert("--server=" + SSL_TEST_SERVER_NAME, "--password=Liberty");
        ProgramOutput commandOutput = runCreateSSLCert("--server=" + SSL_TEST_SERVER_NAME, "--password=Liberty");
        assertEquals(KEYSTORE_EXISTS_RC, commandOutput.getReturnCode());
    }

    /**
     * Tests that the command fails with mutually exclusive arguments --server and --client.
     */
    @Test
    public void testCreateSSLCertificateWithExclusiveArgs() throws Exception {
        String securityUtilityPath = libertyInstallRoot + "/bin/securityUtility";
        String certificatePassword = "Liberty";
        
        // Create beta environment with required flag
        Properties betaEnvironment = new Properties();
        betaEnvironment.put("JVM_ARGS", "-Dcom.ibm.ws.beta.edition=true");
        
        // Execute createSSLCertificate command with both --server and --client
        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] {
                "createSSLCertificate",
                "--server=" + SSL_TEST_SERVER_NAME,
                "--client=" + SSL_TEST_CLIENT_NAME,
                "--password=" + certificatePassword
            },
            libertyInstallRoot,
            betaEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stderr:\n" + commandOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        // Verify command failed with FAILURE_RC
        assertEquals("createSSLCertificate command should fail with exclusive arguments",
                   FAILURE_RC, commandOutput.getReturnCode());
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
}
