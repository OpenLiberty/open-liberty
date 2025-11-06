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
import org.junit.After;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.fail;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Test class for the securityUtility encode command.
 * Tests encoding options, custom encryption, and parameter validation.
 */

@RunWith(FATRunner.class)
public class SecurityUtilityEncodeTest {
    private static final Class<?> thisClass = SecurityUtilityEncodeTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("PasswordUtilityEncodeTest");
    
    // Custom encryption constants
    private static final String CUSTOM_PASSWORD_ENCRYPTION_BUNDLE_NAME = "com.ibm.ws.crypto.sample.customencryption_1.0";
    private static final String CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME = "customEncryption-1.0";
    private static final String CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_ROOT = "bin/tools/extensions";
    private static final String CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_PATH = CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_ROOT + "/ws-customPasswordEncryption";
    private static final String CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_NAME = "customEncryption.jar";
    private static final String PROPERTY_KEY_INSTALL_DIR = "install.dir";
    private static String installDir = null;
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
    // Environment variables
    private static Machine machine;
    private static Properties env;
    private static String installRoot;
    private static String securityUtilityPath;

    // Return codes
    private static final int SUCCESS_RC = 0;
    private static final int FAILURE_RC = 1;

    @Rule
    public static TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        // Log features directory inventory
        RemoteFile rf = server.getFileFromLibertyInstallRoot("/lib/features");
        Log.info(SecurityUtilityEncodeTest.class, "setUp", "Dumping lib/features directory");
        for (RemoteFile feature : rf.list(false)) {
            Log.info(SecurityUtilityEncodeTest.class, "setUp", feature.toString());
        }

        // Install custom encryption components
        server.installUserBundle(CUSTOM_PASSWORD_ENCRYPTION_BUNDLE_NAME);
        server.installUserFeature(CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME);
        server.installUserFeatureL10N(CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME);
        server.copyFileToLibertyInstallRoot(CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_PATH, CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_NAME);
        installDir = System.setProperty(PROPERTY_KEY_INSTALL_DIR, server.getInstallRoot());

        // Initialize environment variables
        installRoot = server.getInstallRoot();
        securityUtilityPath = installRoot + "/bin/securityUtility";
        machine = server.getMachine();
    }

    /**
     * Clean up after each test
     */
    @After
    public void tearDown() throws Exception {
   
        // Delete all XML files in the autoFVTTestFiles directory
        File autoFvtDir = new File(server.pathToAutoFVTTestFiles);
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

    @AfterClass
    public static void tearDownClass() throws Exception {
        // Clean up custom encryption components
        server.uninstallUserBundle(CUSTOM_PASSWORD_ENCRYPTION_BUNDLE_NAME);
        server.uninstallUserFeature(CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME);
        server.uninstallUserFeatureL10N(CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME);
        server.deleteDirectoryFromLibertyInstallRoot(CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_ROOT);
        
        if (installDir == null) {
            System.clearProperty(PROPERTY_KEY_INSTALL_DIR);
        } else {
            System.setProperty(PROPERTY_KEY_INSTALL_DIR, installDir);
        }
    }

    /**
     * Tests that the help text contains the custom encryption feature name
     * when the custom password encryption is installed.
     */
    @Test
    public void testCustomHelp() throws Exception {
        env = new Properties();
        // Verify help text includes custom encryption
        ProgramOutput po = machine.execute(
            securityUtilityPath,
            new String[] { "help", "encode" },
            installRoot,
            env);

        Log.info(thisClass, name.getMethodName(), "Executed securityUtility help encode command:" + po.getCommand());
        Log.info(thisClass, name.getMethodName(), "Result:\n" + po.getStdout());
        printManifestIfRC32(po);
        assertEquals("securityUtility encode help did not result in expected return code.", SUCCESS_RC, po.getReturnCode());
        assertTrue("The expected usage message was not received.", po.getStdout().contains("Usage:"));
        Assert.assertTrue("Help for encode should contain custom encoding feature name.",
                          po.getStdout().contains(CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME));
    }

    /**
     * Tests that the --listCustom option correctly displays available custom encodings.
     */
    @Test
    public void testListCustom() throws Exception {
        // Verify --listCustom option works
        env = new Properties();
        String expectedOutput = "[{\"name\":\"custom\",\"featurename\":\"usr:" + CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME +
                               "\",\"description\":\"this sample custom encryption code uses AES encryption with the predefined key.\"}]";

        ProgramOutput po = machine.execute(
            securityUtilityPath,
            new String[] { "encode", "--listCustom" },
            installRoot,
            env);
            
        Log.info(thisClass, name.getMethodName(), "Executed securityUtility encode --listCustom command:" + po.getCommand());
        Log.info(thisClass, name.getMethodName(), "Result:\n" + po.getStdout());
        printManifestIfRC32(po);
        Assert.assertTrue("The output contains the contents of listCustom", po.getStdout().contains(expectedOutput));
    }

    /**
     * Tests that passwords are correctly encrypted using both default and custom encryption.
     */
    @Test
    public void testCustomEncode() throws Exception {
        env = new Properties();

        // Test custom encryption
        final String textToEncode = "textToEncode";
        final String encodedText = "{xor}KzonKwswGjE8MDs6";
        final String customEncodedText = "{custom}NkshbYjxhL2z1Yc5dv+wDg==";

        // Test default XOR encoding
        ProgramOutput po = machine.execute(
            securityUtilityPath,
            new String[] { "encode", textToEncode },
            installRoot,
            env);

        Log.info(thisClass, name.getMethodName(), "Executed securityUtility encode command:" + po.getCommand());
        Log.info(thisClass, name.getMethodName(), "Result:\n" + po.getStdout());
        printManifestIfRC32(po);
        Assert.assertTrue("encode arg result", po.getStdout().contains(encodedText));

        // Test custom encoding
        po = machine.execute(
            securityUtilityPath,
            new String[] { "encode", "--encoding=custom", textToEncode },
            installRoot,
            env);

        Log.info(thisClass, name.getMethodName(), "Executed securityUtility encode --encoding=custom command:" + po.getCommand());
        Log.info(thisClass, name.getMethodName(), "Result:\n" + po.getStdout());
        printManifestIfRC32(po);
        Assert.assertTrue("encode arg result", po.getStdout().contains(customEncodedText));
    }

    /**
     * Tests that appropriate errors are reported for invalid arguments and encodings.
     * Uses English locale since error messages might be translated.
     */
    //@Test
    public void testEncodeError() throws Exception {
        env = new Properties();

        // Test error handling with English locale
        final String invalidArgument = "Error: Invalid argument --unknown.";
        final String invalidAlgorithm = "com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException";
        final String invalidAlgorithm2 = "Error: com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException";
        final String testPassword = "aaa";
        final String englishLocale = "-Duser.language=en";

        // Test invalid argument error
        List<String> output = SecurityUtilityScriptUtils.execute(
            Arrays.asList(new SecurityUtilityScriptUtils.EnvVar("JVM_ARGS", englishLocale)),
            Arrays.asList("encode", "--unknown=invalid", testPassword),
            true);
        Assert.assertTrue("The invalid argument message should be reported.",
            SecurityUtilityScriptUtils.findMatchingLine(output, invalidArgument));

        // Test invalid encoding error
        output = SecurityUtilityScriptUtils.execute(
            Arrays.asList(new SecurityUtilityScriptUtils.EnvVar("JVM_ARGS", englishLocale)),
            Arrays.asList("encode", "--encoding=invalid", testPassword),
            true);
        if (!SecurityUtilityScriptUtils.findMatchingLine(output, invalidAlgorithm)) {
            // Check alternative error format for newer JDKs
            if (!SecurityUtilityScriptUtils.findMatchingLine(output, invalidAlgorithm2)) {
                Assert.fail("The UnsupportedCryptoAlgorithmException should be reported.");
            }
        }
    }

    /**
     * Tests that the securityUtility encode command correctly handles
     * the --base64Key option for AES encryption. Also, prepares an aes config file with a valid AES key.
     */
    @Test
    public void testEncodeWithBase64Key() throws Exception {
        env = new Properties();

        // Test AES encryption with base64Key option
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        String textToEncode = "secretPassword";
        
        // Use beta edition flag
        Properties betaEnv = new Properties();
        betaEnv.put("JVM_ARGS", "-Dcom.ibm.ws.beta.edition=true");

        ProgramOutput po = machine.execute(
            securityUtilityPath,
            new String[] {
                "encode",
                "--encoding=aes",
                "--base64Key=" + base64Key,
                textToEncode
            },
            installRoot,
            betaEnv);

        Log.info(thisClass, name.getMethodName(), "Output for encode with --base64Key: " + po.getStdout()); 
        Log.info(thisClass, name.getMethodName(), "Executed encode with --base64Key: " + po.getCommand());
        Log.info(thisClass, name.getMethodName(), "Result:\n" + po.getStdout() + "\nError:\n" + po.getStderr());
        printManifestIfRC32(po);
        assertEquals("securityUtility encode with --base64Key should succeed", SUCCESS_RC, po.getReturnCode());
        assertTrue("Output should contain AES encoded value", po.getStdout().contains("{aes}"));


        // Prepare secureKeyFile with valid AES key
        File validKeyFile = File.createTempFile("validAESKey", ".xml");
        try (FileWriter writer = new FileWriter(validKeyFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<server>\n");
            writer.write("  <variable name=\"wlp.aes.encryption.key\" value=\""+base64Key+"\"/>\n");
            writer.write("</server>\n");
        }

        String securityUtilityPath = installRoot + "/bin/securityUtility";

        ProgramOutput po2 = machine.execute(
            securityUtilityPath,
            new String[] {
                "encode",
                "password",
                "--encoding=aes",
                "--aesConfigFile=" + validKeyFile.getAbsolutePath()
            },
            betaEnv);
        printManifestIfRC32(po2);
        assertEquals("securityUtility encode unexpectedly failed with valid AES key.", SUCCESS_RC, po2.getReturnCode());

    }



/**
 * Tests that the securityUtility encode command correctly handles
 * mutually exclusive options --key and --base64Key.
 */
@Test
public void testMutuallyExclusiveKeyOptions_keyAndBase64Key() throws Exception {
    env = new Properties();

    // Test mutually exclusive options --key and --base64Key
    String testKey = "myKey";
    String testBase64Key = "dGVzdA==";
    String testPassword = "secretPassword";
    
    Properties betaEnv = new Properties();
    betaEnv.put("JVM_ARGS", "-Dcom.ibm.ws.beta.edition=true");
    
    ProgramOutput po = machine.execute(
        securityUtilityPath,
        new String[] {
            "encode",
            "--encoding=aes",
            "--key=" + testKey,
            "--base64Key=" + testBase64Key,
            testPassword
        },
        installRoot,
        betaEnv);

    assertEquals("encode with both --key and --base64Key should fail", FAILURE_RC, po.getReturnCode());
}

/**
 * Tests that the securityUtility encode command correctly handles
 * mutually exclusive options --key and --aesConfigFile.
 */
@Test
public void testMutuallyExclusiveKeyOptions_keyAndSecureKeyFile() throws Exception {
    env = new Properties();
    // Test mutually exclusive options --key and --aesConfigFile
    String testKey = "myKey";
    String testPassword = "secretPassword";
    String xmlContent = "<server><variable name=\"wlp.password.encryption.key\" value=\"someKey\"/></server>";
    
    File xmlFile = File.createTempFile("enc", ".xml");
    try (FileWriter fw = new FileWriter(xmlFile)) {
        fw.write(xmlContent);
    }
     catch (Exception e) {
    fail("Cannot write the xml file: " + e.getMessage());
    }
    Properties betaEnv = new Properties();
    betaEnv.put("JVM_ARGS", "-Dcom.ibm.ws.beta.edition=true");

    ProgramOutput po = machine.execute(
        securityUtilityPath,
        new String[] {
            "encode",
            "--encoding=aes",
            "--key=" + testKey,
            "--aesConfigFile=" + xmlFile.getAbsolutePath(),
            testPassword
        },
        installRoot,
        betaEnv);

    assertEquals("encode with both --key and --aesConfigFile should fail", FAILURE_RC, po.getReturnCode());

   
}
/**
 * Tests that the securityUtility encode command correctly handles
 * mutually exclusive options --base64Key and --aesConfigFile.
 */
@Test
public void testMutuallyExclusiveKeyOptions_base64KeyAndSecureKeyFile() throws Exception {
    env = new Properties();
    // Test mutually exclusive options --base64Key and --aesConfigFile
    String testBase64Key = "dGVzdA==";
    String testPassword = "testPassword";
    String xmlContent = "<server><variable name=\"wlp.password.encryption.key\" value=\"someKey\"/></server>";
    
    File xmlFile = File.createTempFile("enc", ".xml");
    try (FileWriter fw = new FileWriter(xmlFile)) {
        fw.write(xmlContent);
    }
    catch (Exception e) {
    fail("Cannot write the xml file: " + e.getMessage());
    }
    Properties betaEnv = new Properties();
    betaEnv.put("JVM_ARGS", "-Dcom.ibm.ws.beta.edition=true");

    ProgramOutput po = machine.execute(
        securityUtilityPath,
        new String[] {
            "encode",
            "--encoding=aes",
            "--base64Key=" + testBase64Key,
            "--aesConfigFile=" + xmlFile.getAbsolutePath(),
            testPassword
        },
        installRoot,
        betaEnv);

    assertEquals("encode with both --base64Key and --aesConfigFile should fail", FAILURE_RC, po.getReturnCode());

    }

     /**
     * Test that verifies securityUtility encode fails when both
     * wlp.aes.encryption.key and wlp.password.encryption.key are specified
     * in the same secure key file.
     */
    @Test
    public void testSecureKeyFileWithBothKeysFails() throws Exception {

        Properties betaEnv = new Properties();
        betaEnv.put("JVM_ARGS", "-Dcom.ibm.ws.beta.edition=true");

        byte[] keyBytes = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(keyBytes);
        String validBase64Key = Base64.getEncoder().encodeToString(keyBytes);

        // Create a temporary secure key XML file containing BOTH keys
        File badKeyFile = File.createTempFile("badSecureKey", ".xml");
        try (FileWriter writer = new FileWriter(badKeyFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<server>\n");
            writer.write("  <variable name=\"wlp.aes.encryption.key\" value=\"" + validBase64Key + "\"/>\n");
            writer.write("  <variable name=\"wlp.password.encryption.key\" value=\"MyLegacyKey\"/>\n");
            writer.write("</server>\n");
        }

        String securityUtilityPath = installRoot + "/bin/securityUtility";

        // Execute the encode command
        ProgramOutput po = machine.execute(
            securityUtilityPath,
            new String[] {
                "encode",
                "badPassword",
                "--encoding=aes",
                "--aesConfigFile=" + badKeyFile.getAbsolutePath()
            },
            betaEnv);

        // Log command and output
        Log.info(thisClass, name.getMethodName(), "Executed: " + po.getCommand());
        Log.info(thisClass, name.getMethodName(), "Stdout: " + po.getStdout());
        Log.info(thisClass, name.getMethodName(), "Stderr: " + po.getStderr());

        assertEquals("securityUtility encode unexpectedly succeeded with both AES and password keys present.", FAILURE_RC, po.getReturnCode());

    }

     /**
     * Test that verifies securityUtility encode fails when the AES key is invalid.
     */
    @Test
    public void testEncodeFailsWithInvalidAESKey() throws Exception {
    Properties betaEnv = new Properties();
        betaEnv.put("JVM_ARGS", "-Dcom.ibm.ws.beta.edition=true");
        
    // Prepare secureKeyFile with invalid AES key
    File invalidKeyFile = File.createTempFile("invalidAESKey", ".xml");
    try (FileWriter writer = new FileWriter(invalidKeyFile)) {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<server>\n");
        writer.write("  <variable name=\"wlp.aes.encryption.key\" value=\"shortKey==\"/>\n");
        writer.write("</server>\n");
    }

    String securityUtilityPath = installRoot + "/bin/securityUtility";

    ProgramOutput po = machine.execute(
        securityUtilityPath,
        new String[] {
            "encode",
            "badpassword",
            "--encoding=aes",
            "--aesConfigFile=" + invalidKeyFile.getAbsolutePath()
        },
        betaEnv);

    assertEquals("securityUtility encode unexpectedly succeeded with invalid AES key.", FAILURE_RC, po.getReturnCode());


    // Check that error mentions AES key length
    String output = po.getStdout() + po.getStderr();
    assertTrue("Expected AES key length error not found. Output:\n" + output,
               output.contains("CWWKS1861E"));
}

    /**
     * Prints a byte array as hexadecimal values to System.out
     *
     * @param bytes The byte array to print
     */
    public static String printHexValues(byte[] bytes) {
        if (bytes == null) {
            Log.info(thisClass, name.getMethodName(), "Error: Null byte array provided");
            return null;
        }

        if (bytes.length == 0) {
            Log.info(thisClass, name.getMethodName(), "Empty byte array");
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            // Convert each byte to two hex characters
            char firstChar = HEX_CHARS[(b & 0xF0) >>> 4];
            char secondChar = HEX_CHARS[b & 0x0F];

            sb.append(firstChar);
            sb.append(secondChar);

            // Add space between bytes for readability
            if (i < bytes.length - 1) {
                sb.append(' ');
            }

            // Add a line break every 16 bytes for better readability
            if ((i + 1) % 16 == 0 && i < bytes.length - 1) {
                sb.append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    /**
     * Converts a byte array to a hexadecimal string
     *
     * @param bytes The byte array to convert
     * @return A string containing the hexadecimal representation of the byte array, or null if the input is null
     */
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b & 0xF0) >>> 4]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }

        return sb.toString();
    }

    /**
     * @param po
     * @throws IOException
     */
    private void printManifestIfRC32(ProgramOutput po) throws Exception {
        if (po.getReturnCode() == 32) {
            RemoteFile customJar = server.getFileFromLibertyInstallRoot(CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_PATH + File.separator + CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_NAME);

            try (ZipInputStream zis = new ZipInputStream(customJar.openForReading())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        Log.info(thisClass, name.getMethodName(), "Processing file: " + entry.getName());

                        // Create an in-memory output stream to capture the data
                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                baos.write(buffer, 0, len);
                            }
                            byte[] entryContentBytes = baos.toByteArray();
                            Log.info(thisClass, name.getMethodName(), new String(entryContentBytes));
                            Log.info(thisClass, name.getMethodName(), "\nHex:\n" + printHexValues(entryContentBytes));

                        }
                    }

                }
            }
        }
    }
}


