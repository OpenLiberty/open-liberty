/*******************************************************************************
 * Copyright (c) 2009, 2025 IBM Corporation and others.
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
package com.ibm.websphere.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.crypto.util.AESKeyManager;
import com.ibm.ws.crypto.util.AESKeyManager.KeyVersion;
import com.ibm.ws.crypto.util.AesConfigFileParser;
import com.ibm.wsspi.security.crypto.PasswordEncryptException;

import test.common.SharedOutputManager;

/**
 * Tests for the password utility class.
 */
public class PasswordUtilTest {
    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    //@BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    //@AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    //@After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    /**
     * Test the utility methods.
     */
    @Test
    public void testUtil() {
        try {
            assertNull(PasswordUtil.getCryptoAlgorithm(null));
            assertNull(PasswordUtil.getCryptoAlgorithm(""));
            assertNull(PasswordUtil.getCryptoAlgorithm("{xor"));
            assertEquals("xor", PasswordUtil.getCryptoAlgorithm("{xor}CDo9Hgw="));

            assertNull(PasswordUtil.getCryptoAlgorithmTag(null));
            assertNull(PasswordUtil.getCryptoAlgorithmTag(""));
            assertNull(PasswordUtil.getCryptoAlgorithmTag("{xor"));
            assertEquals("{xor}", PasswordUtil.getCryptoAlgorithmTag("{xor}CDo9Hgw="));
            assertEquals("{xor}", PasswordUtil.getCryptoAlgorithmTag("{xor}"));

            assertTrue(PasswordUtil.isEncrypted("{xor}CDo9Hgw="));
            assertFalse(PasswordUtil.isEncrypted("CDo9Hgw="));

            assertFalse(PasswordUtil.isValidCryptoAlgorithm(null));
            assertTrue(PasswordUtil.isValidCryptoAlgorithm(""));
            assertTrue(PasswordUtil.isValidCryptoAlgorithm("xor"));

            assertFalse(PasswordUtil.isValidCryptoAlgorithmTag(null));
            assertFalse(PasswordUtil.isValidCryptoAlgorithmTag(""));
            assertFalse(PasswordUtil.isValidCryptoAlgorithmTag("xor"));
            assertTrue(PasswordUtil.isValidCryptoAlgorithmTag("{xor}"));

            assertEquals("WebAS", PasswordUtil.decode("{}WebAS"));
            try {
                PasswordUtil.decode("WebAS");
                fail();
            } catch (InvalidPasswordDecodingException e) {
                // expected exception
            }
            assertEquals("WebAS", PasswordUtil.decode("{xor}CDo9Hgw="));

            assertEquals("{xor}Lz4sLCgwLTsINis3exYxFis=", PasswordUtil.encode("passwordWith$InIt"));
            assertEquals("passwordWith$InIt", PasswordUtil.decode("{xor}Lz4sLCgwLTsINis3exYxFis="));

            try {
                PasswordUtil.encode(null);
                fail();
            } catch (InvalidPasswordEncodingException e) {
                // expected exception
            }
            try {
                PasswordUtil.encode("{xor}CDo9Hgw=");
                fail("We should not encode already encoded passwords");
            } catch (InvalidPasswordEncodingException e) {
                // expected exception
            }

            assertNull(PasswordUtil.removeCryptoAlgorithmTag(null));
            assertNull(PasswordUtil.removeCryptoAlgorithmTag(""));
            assertEquals("test", PasswordUtil.removeCryptoAlgorithmTag("{xor}test"));
            assertEquals("", PasswordUtil.removeCryptoAlgorithmTag("{xor}"));
            assertNull(PasswordUtil.passwordEncode("{test}teststring{/test}", "aes"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable("testUtil", t);
        }
    }

    @Test
    public void testAESEncoding() throws Exception {

        String encoding = PasswordUtil.encode("WebAS", "aes");
        assertTrue("The encoded password should start with {aes} " + encoding, encoding.startsWith("{aes}"));
        String encoding2 = PasswordUtil.encode("WebAS", "aes");
        assertFalse("Encoding the same password twice should result in different encodings: " + encoding + " and " + encoding2, encoding.equals(encoding2));

        assertEquals("The password was not decoded correctly", "WebAS", PasswordUtil.decode(encoding));
        assertEquals("The password was not decoded correctly", "WebAS", PasswordUtil.decode(encoding2));
        assertEquals("The password was not decoded correctly", "WebAS", PasswordUtil.decode("{aes}AGTpzRDW//VE3Jshg1fd89rxw/JMjHfFM9UdYdVNIUt2"));

        assertEquals("Did not decode password encoded with AES_V0 (AES-128) encoded password", "alternatepwd",
                     PasswordUtil.decode("{aes}AEmVKa+jOeA7pos+sSfpHNmH1MVfwg8ZoV29iDi6I0ZGcov6hSZsAxMhFr91jTSBYQ=="));
        assertEquals("Did not decode password encoded with AES_V1 (AES-256) encoded password", "alternatepwd",
                     PasswordUtil.decode("{aes}ARABGAM7S4HrIRtZWJ229TnxuKZrrPN3dsKrrQzCQE/3U5F4zp3UrDQ+Czmnvz1kaQyN7JktDzieJxelwu077ZYET2V+7/1Gi37iztr7lY0i+j4dlHOFIi5PESnZ7V8XOmdSbH9DSgkuJaXNoEqb"));

    }

    @Test
    public void testHashEncoding() throws Exception {
        try {
            PasswordUtil.decode("{hash}ATAAAAAEc2FsdEAAAAAgCvy54cIa4spWU/H9c8WIjTIK/peZ2xH6E7UWMWuz/Qc=");
            fail();
        } catch (InvalidPasswordDecodingException e) {
            // expected exception
        }
        String encoding = PasswordUtil.encode("WebAS", "hash");
        assertTrue("The encoded password should start with {hash} " + encoding, encoding.startsWith("{hash}"));
        String encoding2 = PasswordUtil.encode("WebAS", "hash");
        assertFalse("Encoding the same password twice should result in different encodings: " + encoding + " and " + encoding2, encoding.equals(encoding2));
    }

    @Test
    public void testNoTrim() throws Exception {
        String WITH_SPACE = "  WebAS   ";
        String TRIMMED = "{xor}CDo9Hgw=";
        String NO_TRIM = "{xor}f38IOj0eDH9/fw==";
        assertEquals(PasswordUtil.encode(WITH_SPACE, "xor"), TRIMMED);

        HashMap<String, String> props = new HashMap<String, String>();
        assertEquals(PasswordUtil.encode(WITH_SPACE, "xor", props), TRIMMED);

        props.put(PasswordUtil.PROPERTY_NO_TRIM, "true");
        assertEquals(PasswordUtil.encode(WITH_SPACE, "xor", props), NO_TRIM);
        try {
            assertEquals(WITH_SPACE, PasswordUtil.decode(NO_TRIM));
        } catch (InvalidPasswordDecodingException e) {
            fail();
        }
    }

    /**
     * Test bringing your own AES key for encoding and decoding.
     */
    @Test
    public void testBYOAesKey() throws Exception {
        byte[] keyBytes = generateRandomAes256Key();
        String keyString = Base64.getEncoder().encodeToString(keyBytes);
        String decoded_string = "pass1233";
        Map<String, String> props = new HashMap<>();
        props.put(PasswordUtil.PROPERTY_AES_KEY, keyString);

        try (MockedStatic<AESKeyManager> mock = Mockito.mockStatic(AESKeyManager.class, Mockito.CALLS_REAL_METHODS)) {

            mock.when(() -> AESKeyManager.getKeyCharsUsingResolver(KeyVersion.AES_V2, null)).thenReturn(keyString.toCharArray());

            String encodedPassword = PasswordUtil.encode(decoded_string, "aes", props);
            assertEquals("AES_V2 byte marker not set", getAesVersionFromEncodedPassword(encodedPassword), 2);
            assertEquals("Decoded value does not match original value", decoded_string, PasswordUtil.decode(encodedPassword));

            mock.verify(() -> AESKeyManager.getKeyCharsUsingResolver(KeyVersion.AES_V2, null), times(1));

        }
    }

    /**
     * Test wlp.aes.encryption.key not set with v2 password.
     */
    @Test
    public void testBase64KeyPropertyNotSet() throws Exception {

        try (MockedStatic<AESKeyManager> mock = Mockito.mockStatic(AESKeyManager.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(() -> AESKeyManager.getKeyCharsUsingResolver(KeyVersion.AES_V2, null)).thenReturn(AESKeyManager.PROPERTY_WLP_BASE64_AES_ENCRYPTION_KEY.toCharArray());
            String v2Password = "{aes}AhBGFEeJlillfrTtgxuEL8rV+U0wtouKfmrDj0oYeoFaD4HWATqpJdEmXVacQtxJpWgMTrzBGOzwnsUZLKqmgRmrs9MPpMF4fY5vPrK1N/jOyCxyl3OzqQMwxXopecmoQIxL+lsUmw==";
            try {
                PasswordUtil.decode(v2Password);
                fail("Exception should be thrown when decoding with out the wlp.aes.encryption.key set");
            } catch (InvalidPasswordDecodingException e) {
                //intentionally empty, test should pass if exception is caught
            }
            mock.verify(() -> AESKeyManager.getKeyCharsUsingResolver(KeyVersion.AES_V2, null), times(1));
        }
    }

    /**
     * @param encodedPassword
     * @return
     */
    private byte getAesVersionFromEncodedPassword(String encodedPassword) {
        return Base64.getDecoder().decode(encodedPassword.substring(5))[0];
    }

    /**
     * Helper method to generate a random AES-256 key.
     *
     * @return A byte array containing a random 256-bit AES key
     */
    private byte[] generateRandomAes256Key() {
        byte[] keyBytes;
        SecureRandom secureRandom = new SecureRandom();

        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(CryptoUtils.ENCRYPT_ALGORITHM_AES);
            keyGenerator.init(CryptoUtils.AES_256_KEY_LENGTH_BITS, secureRandom);
            SecretKey secretKey = keyGenerator.generateKey();
            keyBytes = secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to SecureRandom if KeyGenerator is not available
            keyBytes = new byte[CryptoUtils.AES_256_KEY_LENGTH_BYTES];
            secureRandom.nextBytes(keyBytes);
        }

        return keyBytes;
    }

    /**
     * Test that providing an invalid Base64 string as an AES key fails appropriately.
     */
    @Test
    public void testInvalidBase64KeyFails() {

        Map<String, String> props = new HashMap<>();
        // Invalid base64 string: contains characters not valid in Base64 encoding
        String invalidKey = "Not@Valid*Base64==";
        props.put(PasswordUtil.PROPERTY_AES_KEY, invalidKey);

        String testPassword = "badKeyTest";

        try {
            PasswordUtil.encode(testPassword, "aes", props);
            fail("Encoding with an invalid Base64 key should have failed");
        } catch (Exception e) {
            // Verify we get the expected exception type
            assertTrue("Exception should be InvalidPasswordEncodingException",
                       e instanceof InvalidPasswordEncodingException);
        }
    }

    /**
     * Test that attempting to parse an AES encryption file with a non-existent path
     * results in an appropriate exception.
     */
    @Test
    public void testParseAesEncryptionaesConfigFileWithInvalidPath() {

        // Use a path that definitely doesn't exist
        String invalidPath = "/non/existent/path/to/aeskey.xml";

        try {
            AesConfigFileParser.parseAesEncryptionFile(invalidPath);
            fail("Expected IOException for non-existent XML file path");
        } catch (Exception e) {
            // We expect some kind of IOException or parsing failure
            assertTrue(
                       "Unexpected exception type: " + e,
                       e instanceof PasswordEncryptException);
        }
    }

    /**
     * Test that attempting to parse an AES encryption XML file which is malformed
     * results in an appropriate exception.
     */
    @Test
    public void testParseAesEncryptionaesConfigFileWithMalformedXml() throws IOException {

        // Create a temporary file with invalid XML content
        File badXml = File.createTempFile("bad-xml", ".xml");
        try (FileWriter writer = new FileWriter(badXml)) {
            // Write intentionally malformed XML content
            writer.write("<variable name=\"wlp.aes.encryption.key\" value=\"someValue\" >"); // Missing closing tag
        }

        try {
            AesConfigFileParser.parseAesEncryptionFile(badXml.getAbsolutePath());
            fail("Expected UnsupportedConfigurationException for malformed XML content");
        } catch (Exception e) {
            assertTrue(
                       "Unexpected exception type: " + e,
                       e instanceof UnsupportedCryptoAlgorithmException);
        } finally {
            // Clean up the temporary file
            badXml.delete();
        }
    }

    /**
     * Test that attempting to parse an AES encryption XML file which has both
     * wlp.aes.encryption.key and wlp.password.encryption.key specified
     */
    @Test
    public void testParseAesEncryptionaesConfigFileWithBothPropertiesSpecified() throws IOException {

        // Create a temporary file with invalid XML content
        File badXml = File.createTempFile("bad-xml", ".xml");
        try (FileWriter writer = new FileWriter(badXml)) {
            // Write intentionally malformed XML content
            writer.write("<variable name=\"wlp.aes.encryption.key\" value=\"someValue\" />");
            writer.write("<variable name=\"wlp.password.encryption.key\" value=\"someValue\" />");

        }

        try {
            AesConfigFileParser.parseAesEncryptionFile(badXml.getAbsolutePath());
            fail("Expected UnsupportedConfigurationException for malformed XML content");
        } catch (Exception e) {
            assertTrue(
                       "Unexpected exception type: " + e,
                       e instanceof UnsupportedCryptoAlgorithmException);
        } finally {
            // Clean up the temporary file
            badXml.delete();
        }
    }

    /**
     * Test that attempting to parse an AES encryption XML file which has both
     * wlp.aes.encryption.key and wlp.password.encryption.key specified
     */
    @Test
    public void testParseAesEncryptionaesConfigFileWithNoPropertiesSpecified() throws IOException {

        // Create a temporary file with invalid XML content
        File badXml = File.createTempFile("bad-xml", ".xml");
        try (FileWriter writer = new FileWriter(badXml)) {
            // Write intentionally malformed XML content
            writer.write("<variable name=\"name1\" value=\"someValue\" />");
        }

        try {
            AesConfigFileParser.parseAesEncryptionFile(badXml.getAbsolutePath());
            fail("Expected UnsupportedCryptoAlgorithmException for malformed XML content");
        } catch (Exception e) {
            assertTrue(
                       "Unexpected exception type: " + e,
                       e instanceof UnsupportedCryptoAlgorithmException);
        } finally {
            // Clean up the temporary file
            badXml.delete();
        }
    }
}