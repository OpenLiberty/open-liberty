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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Test class for the securityUtility generateAESKey command.
 * 
 * This test suite validates the functionality of the generateAESKey command, including:
 * - Parameter validation (missing/empty parameters)
 * - File handling (existing files, file creation)
 * - Key generation (random and specified keys)
 * - XML file usage (inclusion in server configuration)
 * - Integration with other security features (encoded passwords, keystore configuration)
 */

@RunWith(FATRunner.class)
public class SecurityUtilityGenerateAesKeyTest {
    private static final Class<?> thisClass = SecurityUtilityGenerateAesKeyTest.class;
    private static LibertyServer testServer = LibertyServerFactory.getLibertyServer("SecurityUtilityGenerateServer");
    private static Machine testMachine;

    // Test environment
    private static Properties testEnvironment;
    private static String libertyInstallRoot;
    private static String testOutputDir;
    private String securityUtilityPath = libertyInstallRoot + "/bin/securityUtility";

    // Return codes
    private static final int SUCCESS_RC = 0;
    private static final int FAILURE_RC = 1;
    
    @Rule
    public TestName testName = new TestName();

    /**
     * Setup test environment before running any tests
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        libertyInstallRoot = testServer.getInstallRoot();
        testMachine = testServer.getMachine();

        // Create test output directory
        testOutputDir = libertyInstallRoot + "/output";
        if (!Files.exists(Paths.get(testOutputDir))) {
            Files.createDirectories(Paths.get(testOutputDir));
        }
    }

    /**
     * Setup before each test
     */
    @Before
    public void setUp() throws Exception {
        // Create test output directory
        testOutputDir = libertyInstallRoot + "/output";
        if (!Files.exists(Paths.get(testOutputDir))) {
            Files.createDirectories(Paths.get(testOutputDir));
        }

        // Delete all XML files in the autoFVTTestFiles directory
        File autoFvtDir = new File(testServer.pathToAutoFVTTestFiles);
        if (autoFvtDir.exists()) {
            File[] files = autoFvtDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        System.err.println("Failed to delete test file: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * Clean up after each test
     */
    @After
    public void tearDown() throws Exception {
        // Stop servers defensively
        if (testServer.isStarted()) {
            testServer.stopServer();
        }
        // Delete all XML files in the output directory
        File outputDirFile = new File(testOutputDir);
        if (outputDirFile.exists()) {
            File[] files = outputDirFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        System.err.println("Failed to delete output file: " + file.getAbsolutePath());
                    }
                }
            }
            if (!outputDirFile.delete()) {
                System.err.println("Failed to delete output directory: " + outputDirFile.getAbsolutePath());
            }
        }
    
        // Delete all XML files in the autoFVTTestFiles directory
        File autoFvtDir = new File(testServer.pathToAutoFVTTestFiles);
        if (autoFvtDir.exists()) {
            File[] files = autoFvtDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        System.err.println("Failed to delete test file: " + file.getAbsolutePath());
                    }
                }
            }
        }


    }

    /**
     * Clean up after all tests complete
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        // Delete all XML files in the alternate autoFVTTestFiles directory
        File alternateAutoFvtDir = new File(testServer.pathToAutoFVTTestFiles + "/alternate");
        if (alternateAutoFvtDir.exists()) {
            File[] files = alternateAutoFvtDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        System.err.println("Failed to delete test file: " + file.getAbsolutePath());
                    }
                }
            }
            alternateAutoFvtDir.delete();   
        }
    }

    /**
     * Test that verifies the generateAESKey command fails properly when the --createConfigFile parameter is empty.
     */
    @Test
    public void testEmptyFileParameter() throws Exception {
        // Test with empty --createConfigFile parameter
        

        testEnvironment = new Properties();

        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { "generateAESKey", "--createConfigFile=" },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stderr:\n" + commandOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        // Verify failure
        assertEquals("generateAESKey with empty --createConfigFile parameter should fail", 
                    FAILURE_RC, commandOutput.getReturnCode());
    }

    /**
     * Test that verifies the generateAESKey command succeeds when the file parameter is missing,
     * producing a base64-encoded key in the output.
     */
    @Test
    public void testMissingFileParameter() throws Exception {
        testEnvironment = new Properties();

        // Test with missing --createConfigFile parameter
        

        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { "generateAESKey" },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stderr:\n" + commandOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        // Verify success
        assertEquals("generateAESKey without --createConfigFile parameter should succeed", 
                    SUCCESS_RC, commandOutput.getReturnCode());

        // Verify stdout contains a valid base64 key
        String generatedKey = commandOutput.getStdout().trim();
        assertTrue("Output should contain a base64 key",
                 generatedKey.matches("^[A-Za-z0-9+/=]+$"));
        
        // Verify key is the correct length for AES-256 (32 bytes)
        byte[] decodedKey = Base64.getDecoder().decode(generatedKey);
        assertEquals("Decoded key should be 32 bytes for AES-256", 32, decodedKey.length);
    }

    /**
     * Test that verifies the generateAESKey command fails properly when the target file already exists.
     */
    @Test
    public void testExistingFile() throws Exception {
        // Create existing test file
        testEnvironment = new Properties();
        
        String existingFileName = "existing.xml";
        String existingFilePath = testOutputDir + "/" + existingFileName;
        File existingFile = new File(existingFilePath);
        
        try (FileWriter writer = new FileWriter(existingFile)) {
            writer.write("<server><variable name=\"test\" value=\"test\"/></server>");
        }
        catch (Exception e) {
            fail("Cannot write the xml file: " + e.getMessage());
        }
        assertTrue("Test file should exist before test", existingFile.exists());

        // Test with existing file
        

        ProgramOutput commandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { "generateAESKey", "--createConfigFile=" + existingFilePath },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "stderr:\n" + commandOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "stdout:\n" + commandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Return code: " + commandOutput.getReturnCode());

        // Verify failure
        assertEquals("generateAESKey with existing file should fail", FAILURE_RC, commandOutput.getReturnCode());
    }

    /**
     * Test that verifies the generateAESKey command successfully creates files with specified keys
     * and that the same key input produces identical encryption keys.
     */
    @Test
    public void testSpecifiedKeyGeneration() throws Exception {
        testEnvironment = new Properties();

        // Test specified key generation
        String specifiedKey = "myTestKey123";
        String specifiedKeyFile1 = "specified_key1.xml";
        String specifiedKeyFile2 = "specified_key2.xml";
        String outputFilePath1 = testOutputDir + "/" + specifiedKeyFile1;
        String outputFilePath2 = testOutputDir + "/" + specifiedKeyFile2;
        File outputFile1 = new File(outputFilePath1);
        File outputFile2 = new File(outputFilePath2);

        

        // Generate first file
        ProgramOutput firstCommandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { "generateAESKey", "--key=" + specifiedKey, "--createConfigFile=" + outputFilePath1 },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "First generation - stderr:\n" + firstCommandOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "First generation - stdout:\n" + firstCommandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "First generation - Return code: " + firstCommandOutput.getReturnCode());

        assertEquals("First generate with specified key should succeed", SUCCESS_RC, firstCommandOutput.getReturnCode());

        // Generate second file with same key
        ProgramOutput secondCommandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { "generateAESKey", "--key=" + specifiedKey, "--createConfigFile=" + outputFilePath2 },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "Second generation - stderr:\n" + secondCommandOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "Second generation - stdout:\n" + secondCommandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Second generation - Return code: " + secondCommandOutput.getReturnCode());

        assertEquals("Second generate with specified key should succeed", SUCCESS_RC, secondCommandOutput.getReturnCode());

        // Verify files and content
        assertTrue("First output file should be created", outputFile1.exists());
        assertTrue("Second output file should be created", outputFile2.exists());

        String fileContent1 = new String(Files.readAllBytes(outputFile1.toPath()));
        String fileContent2 = new String(Files.readAllBytes(outputFile2.toPath()));

        assertEquals("Using the same --key both output files should be identical", fileContent1, fileContent2);
    }

    /**
     * Test that verifies that the generateAESKey command fails to create files if the key parameter is empty
     */
    @Test
    public void testEmptyKeyParameter() throws Exception {
        testEnvironment = new Properties();

        String specifiedKeyFile1 = "specified_key1.xml";
        String outputFilePath1 = testOutputDir + "/" + specifiedKeyFile1;

        // Generate first file
        ProgramOutput firstCommandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { "generateAESKey", "--key=" + "", "--createConfigFile=" + outputFilePath1 },
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "First generation - stderr:\n" + firstCommandOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "First generation - stdout:\n" + firstCommandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "First generation - Return code: " + firstCommandOutput.getReturnCode());

        assertEquals("Generate without specified key should fail", FAILURE_RC, firstCommandOutput.getReturnCode());
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
     * Test that verifies V1 and V2 passwords can coexist in the same server.xml.
     * Uses a V2 (AES) password for LTPA, a V1 password for SSL keystore,
     * and dynamically generates the wlp.aes.encryption.key variable.
     */
	@Test
	public void testV1V2PasswordsCoexistInServerXml() throws Exception {
		testEnvironment = new Properties();

		String aesConfigFile = testOutputDir + "/temp_aes.xml";

		// Generate AES base64 key for wlp.aes.encryption.key
		ProgramOutput aesKeyOutput = testMachine.execute(securityUtilityPath,
				new String[] { "generateAESKey", "--createConfigFile=" + aesConfigFile }, libertyInstallRoot,
				testEnvironment);
		assertEquals("AES key generation should succeed", SUCCESS_RC, aesKeyOutput.getReturnCode());

		Log.info(thisClass, testName.getMethodName(),
				"aesConfigFile contents: " + readStringUsingBufferedReader(new File(aesConfigFile).toPath()));

		Log.info(thisClass, testName.getMethodName(), "Generate command - stderr:\n" + aesKeyOutput.getStderr());
		Log.info(thisClass, testName.getMethodName(), "Generate command - stdout:\n" + aesKeyOutput.getStdout());
		Log.info(thisClass, testName.getMethodName(),
				"Generate command - Return code: " + aesKeyOutput.getReturnCode());

		// Generate V2 LTPA password
		ProgramOutput v2Output = testMachine.execute(securityUtilityPath,
				new String[] { "createLTPAKeys", "--server=SecurityUtilityGenerateServer",
						"--aesConfigFile=" + aesConfigFile, "--passwordEncoding=aes", "--password=passw0rd" },
				libertyInstallRoot, testEnvironment);
		assertEquals("V2 encode should succeed", SUCCESS_RC, v2Output.getReturnCode());

		Log.info(thisClass, testName.getMethodName(), "Generate command - stderr:\n" + v2Output.getStderr());
		Log.info(thisClass, testName.getMethodName(), "Generate command - stdout:\n" + v2Output.getStdout());
		Log.info(thisClass, testName.getMethodName(), "Generate command - Return code: " + v2Output.getReturnCode());

		// Generate V1 SSL keystore password
		ProgramOutput v1Output = testMachine
				.execute(securityUtilityPath,
						new String[] { "createSSLCertificate", "--server=SecurityUtilityGenerateServer",
								"--password=passw0rd2", "--passwordEncoding=aes" },
						libertyInstallRoot, testEnvironment);
		assertEquals("V1 encode should succeed", SUCCESS_RC, v1Output.getReturnCode());

		Log.info(thisClass, testName.getMethodName(),
				"createSSLCertificate command - stderr:\n" + v1Output.getStderr());
		Log.info(thisClass, testName.getMethodName(),
				"createSSLCertificate command - stdout:\n" + v1Output.getStdout());
		Log.info(thisClass, testName.getMethodName(),
				"createSSLCertificate command - Return code: " + v1Output.getReturnCode());

		Pattern ltpaPattern = Pattern.compile("<ltpa\\s+keysPassword=\"[^\"]+\"\\s*/>");
		Matcher ltpaMatcher = ltpaPattern.matcher(v2Output.getStdout());
		assertTrue("Could not find ltpa snippet in stdout", ltpaMatcher.find());
		String ltpaSnippet = ltpaMatcher.group();

		// Extract <keyStore> snippet from stdout
		Pattern ksPattern = Pattern.compile("<keyStore\\s+id=\"[^\"]+\"\\s+password=\"[^\"]+\"\\s*/>");
		Matcher matcher = ksPattern.matcher(v1Output.getStdout());
		assertTrue("Could not find keyStore snippet in stdout", matcher.find());
		String keystoreSnippet = matcher.group();

		String passwordsnippet = getPasswordOverride(keystoreSnippet, ltpaSnippet, aesConfigFile);
		writeStringToServerOverride(passwordsnippet, testServer);

		// setup
		testServer.startServer(testName.getMethodName());

		// Verify startup log contains LTPA initialization
		String logOutput = testServer.waitForStringInLogUsingMark("CWWKS4105I", 5000);
		if (logOutput == null) {
			Log.info(thisClass, testName.getMethodName(), "LTPA ready not found, aesConfigFile contents: "
					+ readStringUsingBufferedReader(new File(aesConfigFile).toPath()));
		}

		assertNotNull("Expected LTPA configuration ready message not found in the log.", logOutput);

		// Verify startup log contains the keystore loaded successfully message
		assertNotNull("Expected Keystore loaded message not found in the log.",
				testServer.waitForStringInLogUsingMark("Successfully loaded default keystore", 5000));

		testServer.stopServer();
	}

    /**
     * Test that verifies the generated XML file is properly included in the Liberty server
     * configuration using the directions from the command success output.
     */
    @Test
    public void testGeneratedXmlFileInclusion() throws Exception {
        testEnvironment = new Properties();

        // Generate encryption XML file
        String includeFileName = "include.xml";
        String xmlFilePath = testOutputDir + "/" + includeFileName;
        File xmlFile = new File(xmlFilePath);

        // Paths
        File baseServerXml = new File(testServer.getInstallRoot()+"/servers/SecurityUtilityGenerateServer/server.xml");
        

        ProgramOutput generateCommandOutput = testMachine.execute(
            securityUtilityPath,
            new String[] { "generateAESKey", "--createConfigFile=" + xmlFilePath},
            libertyInstallRoot,
            testEnvironment);

        Log.info(thisClass, testName.getMethodName(), "Generate command - stderr:\n" + generateCommandOutput.getStderr());
        Log.info(thisClass, testName.getMethodName(), "Generate command - stdout:\n" + generateCommandOutput.getStdout());
        Log.info(thisClass, testName.getMethodName(), "Generate command - Return code: " + generateCommandOutput.getReturnCode());

        // Verify success
        assertEquals("generateAESKey command should succeed", SUCCESS_RC, generateCommandOutput.getReturnCode());
        assertTrue("Output file should be created", xmlFile.exists());

        // Verify output instructions
        String generateOutput = generateCommandOutput.getStdout();
        assertTrue("Output should mention include directive",
                  generateOutput.contains("<include location="));

        Pattern pattern = Pattern.compile("<include location=\"([^\"]+)\" />");
        Matcher match = pattern.matcher(generateOutput);
        String filepath = "";

        if (match.find()) {
            filepath = match.group(1);
        }

        String includesnippet = getincludeOverride(filepath);
        writeStringToServerOverride(includesnippet, testServer);
        
        // Start server
        testServer.startServer(testName.getMethodName(), true);

        // Verify server processed the include
        String configProcessedMsg = testServer.waitForStringInLog("CWWKG0028A", 5000);
        assertNotNull("Generated XML file was not included by the server", configProcessedMsg);

        // Verify startup completed
        String startupCompleteMsg = testServer.waitForStringInLog("CWWKF0011I", 5000);
        assertNotNull("Server did not report startup complete", startupCompleteMsg);

        // Stop server
        testServer.stopServer();
    }

   /**
     * Creates a configuration snippet that includes the specified config file.
     * 
     * @param configFilePath Path to the configuration file to include
     * @return A formatted XML snippet for server configuration
     */
    private String getincludeOverride(String configFilePath) {
        String configSnippet = "<server>\n <include location=\"" + configFilePath + "\"/>\n</server>\n";
        return configSnippet;
    }

    private String getPasswordOverride(String keystoreSnippet, String ltpaSnippet, String aesConfigFile) {
        String passwordSnippet = "<server description=\"V1/V2 coexistence\">\n<featureManager>\n<feature>jsp-2.3</feature>\n<feature>appSecurity-2.0</feature>\n<feature>transportSecurity-1.0</feature>\n</featureManager>\n<httpEndpoint id=\"defaultHttpEndpoint\" httpPort=\"9080\" httpsPort=\"9443\"/>\n<ssl id=\"defaultSSLConfig\" trustDefaultCerts=\"true\" />\n"+ltpaSnippet+"\n"+keystoreSnippet+"\n<include location=\""+ aesConfigFile+"\""+ "/>\n</server>\n";

        return passwordSnippet;

    }

	/**
	 * Writes a configuration snippet to a server override file.
	 * 
	 * @param configSnippet The XML configuration snippet
	 * @param server        The Liberty server to apply the override to
	 * @throws Exception If an error occurs writing the file
	 */
	private void writeStringToServerOverride(String configSnippet, LibertyServer server) throws Exception {
		File overrideFile = new File(server.pathToAutoFVTTestFiles + "overrides.xml");
		overrideFile.delete();
		Files.write(overrideFile.toPath(), configSnippet.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
				StandardOpenOption.WRITE);
		server.addDropinOverrideConfiguration(overrideFile.getName());
	}

	private static String readStringUsingBufferedReader(Path filePath) throws IOException {
 		try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
			return reader.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

}