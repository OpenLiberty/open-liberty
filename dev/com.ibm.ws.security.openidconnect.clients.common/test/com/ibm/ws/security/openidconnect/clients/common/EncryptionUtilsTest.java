/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.security.Key;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class EncryptionUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect.clients.common.*=all");

    private final static String ACCESS_TOKEN = "EAANQIE2J5nMBAErWBIFfkmu9r6yQeGoIMg39mHRJrZA7L0jbiD7GEpLSZBm96tgqvvlbQI3UIgQXSJaO6sRJGaFEZCwn5kolWgSjs5q71rrNg0GdbHk5yxrtsZAWsZBv3XV1xFmJ4reZBKA6sx5PqQJejg5RtTWKPg4jJoP0zk1AZDZD";
    private final static String RSA = "RSA";
    private final static String AES = "AES";
    private final static String CIPHER_RSA = "RSA/ECB/PKCS1Padding";
    private final static String RSA_X509_FORMAT = "X.509";
    private final static String CLIENT_SECRET = "myClientSecret";
    private final static String LOWER_ALPHANUM_REGEX = "[a-z0-9]";

    private final int RSA_ENC_TOKEN_LENGTH = 512;
    private final int AES_ENC_TOKEN_LENGTH = 384;
    private final BigInteger keyModulus = BigInteger.probablePrime(512, new Random());
    private final BigInteger keyExponent = BigInteger.valueOf(3);

    private final RSAPublicKey rsaPublicKey = mockery.mock(RSAPublicKey.class);
    private final RSAPrivateKey rsaPrivateKey = mockery.mock(RSAPrivateKey.class);
    private final Cipher cipher = mockery.mock(Cipher.class);

    private EncryptionUtils utils = new EncryptionUtils();

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new EncryptionUtils();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();

        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /******************************************* rsaEncrypt *******************************************/

    @Test
    public void rsaEncrypt_nullToken() throws Exception {
        try {
            String result = utils.rsaEncrypt(rsaPublicKey, null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // @Test
    // test relies on legacy cglib which is not supported post JDK 8
    public void rsaEncrypt_emptyToken() throws Exception {
        try {
            publicKeyExpectations();

            String result = utils.rsaEncrypt(rsaPublicKey, "");
            assertTrue("Encrypted empty token is expected to also be an empty string. Result was: [" + result + "].", result.isEmpty());

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // @Test
    // test relies on legacy cglib which is not supported post JDK 8
    public void rsaEncrypt() throws Exception {
        try {
            publicKeyExpectations();

            String result = utils.rsaEncrypt(rsaPublicKey, ACCESS_TOKEN);
            assertEncryptedToken(RSA_ENC_TOKEN_LENGTH, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* rsaDecrypt *******************************************/

    @Test
    public void rsaDecrypt_nullToken() throws Exception {
        try {
            String result = utils.rsaDecrypt(rsaPrivateKey, null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // @Test
    // test relies on legacy cglib which is not supported post JDK 8
    public void rsaDecrypt_emptyToken() throws Exception {
        try {
            // TODO - test runs successfully locally, but fails in person builds
            privateKeyRSAExpectations();

            String result = utils.rsaDecrypt(rsaPrivateKey, "");
            assertTrue("Decrypted empty token is expected to also be an empty string. Result was: [" + result + "].", result.isEmpty());

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // TODO - rsaDecrypt - valid token

    /******************************************* aesEncrypt *******************************************/

    @Test
    public void aesEncrypt_nullToken() throws Exception {
        try {
            String result = utils.aesEncrypt(CLIENT_SECRET, null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void aesEncrypt_emptyToken() throws Exception {
        try {
            String result = utils.aesEncrypt(CLIENT_SECRET, "");
            assertEncryptedToken(32, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void aesEncrypt() throws Exception {
        try {
            String result = utils.aesEncrypt(CLIENT_SECRET, ACCESS_TOKEN);
            assertEncryptedToken(AES_ENC_TOKEN_LENGTH, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* aesDecrypt *******************************************/

    @Test
    public void aesDecrypt_nullToken() throws Exception {
        try {
            String result = utils.aesDecrypt(CLIENT_SECRET, null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void aesDecrypt_emptyToken() throws Exception {
        try {
            String result = utils.aesDecrypt(CLIENT_SECRET, "");
            assertTrue("Decrypted empty token is expected to also be an empty string. Result was: [" + result + "].", result.isEmpty());

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // TODO - aesDecrypt - valid token

    /******************************************* getBytes *******************************************/

    @Test
    public void getBytes_nullInput() throws Exception {
        try {
            byte[] result = utils.getBytes(cipher, null, 10);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBytes_emptyInput() throws Exception {
        try {
            final String input = "";

            byte[] result = utils.getBytes(cipher, input.getBytes(), 10);
            assertEquals("Result was not empty when it should have been. Result: [" + result + "]", 0, result.length);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBytes_negativeLength() throws Exception {
        try {
            final String input = "input";

            byte[] result = utils.getBytes(cipher, input.getBytes(), -10);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBytes_zeroLength() throws Exception {
        try {
            final String input = "input";

            byte[] result = utils.getBytes(cipher, input.getBytes(), 0);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // @Test
    // test relies on legacy cglib which is not supported post JDK 8
    public void getBytes_smallLength() throws Exception {
        try {
            final String input = "input";

            publicKeyExpectations();

            Cipher cipher = Cipher.getInstance(CIPHER_RSA);
            cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);

            // The value will change with every run, but in any case the result should not be null
            byte[] result = utils.getBytes(cipher, input.getBytes(), 1);
            assertNotNull("Result was null when it should have been.", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // @Test
    // test relies on legacy cglib which is not supported post JDK 8
    public void getBytes_largeLength() throws Exception {
        try {
            final String input = "input";

            publicKeyExpectations();

            Cipher cipher = Cipher.getInstance(CIPHER_RSA);
            cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);

            // The value will change with every run, but in any case the result should not be null
            byte[] result = utils.getBytes(cipher, input.getBytes(), 1000000);
            assertNotNull("Result was null when it should have been.", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getSecretKey *******************************************/

    @Test
    public void getSecretKey_nullClientSecret() {
        try {
            Key result = utils.getSecretKey(null);
            verifyNonNullAesKey(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSecretKey_emptyClientSecret() {
        try {
            Key result = utils.getSecretKey("");
            verifyNonNullAesKey(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSecretKey_nonEmptyClientSecret() {
        try {
            Key result = utils.getSecretKey(CLIENT_SECRET);
            verifyNonNullAesKey(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getIvSpec *******************************************/

    @Test
    public void getIvSpec_nullClientSecret() {
        try {
            IvParameterSpec result = utils.getIvSpec(null);
            verifyNonNullIvSpec(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIvSpec_emptyClientSecret() {
        try {
            IvParameterSpec result = utils.getIvSpec("");
            verifyNonNullIvSpec(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIvSpec_nonEmptyClientSecret() {
        try {
            IvParameterSpec result = utils.getIvSpec(CLIENT_SECRET);
            verifyNonNullIvSpec(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getClientSecretHash *******************************************/

    @Test
    public void getClientSecretHash_nullClientSecret() {
        try {
            final String clientSecret = null;
            byte[] result = utils.getClientSecretHash(clientSecret);
            assertNull("Result should have been null but was " + Arrays.toString(result), result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getClientSecretHash_emptyClientSecret() {
        try {
            final String clientSecret = "";
            byte[] result = utils.getClientSecretHash(clientSecret);
            verifyNonNullClientSecretHash(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getClientSecretHash_nonEmptyClientSecret() {
        try {
            final String clientSecret = CLIENT_SECRET;
            byte[] result = utils.getClientSecretHash(clientSecret);
            verifyNonNullClientSecretHash(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getMessageDigest *******************************************/

    @Test
    public void getMessageDigest_nullAlg() {
        try {
            String algorithm = null;
            MessageDigest result = utils.getMessageDigest(algorithm);
            assertNull("Result should have been null for a null algorithm string, but got MessageDigest with algorithm: " + (result != null ? result.getAlgorithm() : ""),
                    result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getMessageDigest_unknownAlg() {
        try {
            String algorithm = "Some unknown algorithm";
            MessageDigest result = utils.getMessageDigest(algorithm);
            assertNull("Result should have been null for an unknown algorithm string, but got MessageDigest with algorithm: " + (result != null ? result.getAlgorithm() : ""),
                    result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getMessageDigest_sha256() {
        try {
            String algorithm = "SHA-256";
            MessageDigest result = utils.getMessageDigest(algorithm);
            assertNotNull("Result should not have been null for the provided algorithm string [" + algorithm + "].", result);
            assertEquals("Actual algorithm did not match the input algorithm.", algorithm, result.getAlgorithm());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* bytesToHexString *******************************************/

    @Test
    public void bytesToHexString_null() throws Exception {
        try {
            String result = utils.bytesToHexString(null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void bytesToHexString_empty() throws Exception {
        try {
            String result = utils.bytesToHexString(new byte[0]);
            assertTrue("Result is expected to be an empty string. Result was: [" + result + "].", result.isEmpty());

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void bytesToHexString_singleByteChars() throws Exception {
        try {
            // Any unicode value that takes more than 127 bits to represent must be split into two separate bytes,
            // hence why '\u007f' (127) is still represented as "7f" while \u0080 (128) requires two bytes: "c2" + "80".
            String singleByteChars = "\u0000\u007f";
            byte[] input = singleByteChars.getBytes();

            String result = utils.bytesToHexString(input);
            assertEquals("Result did not match expected value.", "007f", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // 20170420 bt/av: for unknown reasons, probably some codepage thing, this fails on windows.
    // todo: debug this
    @Test
    public void bytesToHexString_multiByteChars() throws Exception {
        if (!System.getProperty("os.name").toLowerCase().contains("window")) {
            try {
                // Any unicode value that takes more than 127 bits to represent must be split into two separate bytes,
                // hence why '\u007f' (127) is still represented as "7f" while \u0080 (128) requires two bytes: "c2" + "80".
                String charsStart128 = "\u0080\u00bf";
                String charsStart192 = "\u00c0\u00ff";
                String charsStart256 = "\u0100\u013f";
                //            String charsStart2048 = "\u0800\u083f";
                //            String charsStart65472 = "\uffc0\uffff";
                byte[] input = (charsStart128 + charsStart192 + charsStart256).getBytes();

                String result = utils.bytesToHexString(input);
                assertEquals("Result did not match expected value.", "c280c2bf" + "c380c3bf" + "c480c4bf", result);

                verifyNoLogMessage(MSG_BASE);

            } catch (Throwable t) {
                outputMgr.failWithThrowable(testName.getMethodName(), t);
            }
        }
    }

    /******************************************* hexStringToBytes *******************************************/

    @Test
    public void hexStringToBytes_null() throws Exception {
        try {
            byte[] result = utils.hexStringToBytes(null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hexStringToBytes_empty() throws Exception {
        try {
            byte[] result = utils.hexStringToBytes("");
            assertEquals("Result is expected to be an empty array. Result was: [" + result + "].", 0, result.length);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hexStringToBytes_singleChar() throws Exception {
        try {
            // The code currently expects the input string to be an EVEN number of characters. Any characters beyond the last character in an
            // even index will simply be ignored. Thus, an input with a single char will have its only character ignored.
            byte[] result = utils.hexStringToBytes("a");
            assertEquals("Result is expected to be an empty array. Result was: [" + result + "].", 0, result.length);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hexStringToBytes_notHexadecimal() throws Exception {
        try {
            try {
                byte[] result = utils.hexStringToBytes("Some non-hex value.");
                fail("Should have thrown NumberFormatException but did not. Got result: " + result);
            } catch (NumberFormatException e) {
                // expected
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* End-to-end *******************************************/

    @Test
    public void endToEndHexToBytes() throws Exception {
        try {
            String input = "0123456789abcdef";
            String result = utils.bytesToHexString(utils.hexStringToBytes(input));

            assertEquals("Byte to hex conversion result did not match input.", input, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* Helper methods *******************************************/

    private void publicKeyExpectations() {
        mockery.checking(new Expectations() {
            {
                allowing(rsaPublicKey).getAlgorithm();
                will(returnValue(RSA));
                allowing(rsaPublicKey).getModulus();
                will(returnValue(keyModulus));
                allowing(rsaPublicKey).getPublicExponent();
                will(returnValue(keyExponent));
                allowing(rsaPublicKey).getFormat();
                will(returnValue(RSA_X509_FORMAT));
            }
        });
    }

    private void privateKeyRSAExpectations() {
        mockery.checking(new Expectations() {
            {
                will(returnValue(rsaPrivateKey));
                allowing(rsaPrivateKey).getAlgorithm();
                will(returnValue(RSA));
                allowing(rsaPrivateKey).getModulus();
                will(returnValue(keyModulus));
                allowing(rsaPrivateKey).getPrivateExponent();
                will(returnValue(keyExponent));
                allowing(rsaPrivateKey).getFormat();
                will(returnValue(RSA_X509_FORMAT));
            }
        });
    }

    private void assertEncryptedToken(int expectedLength, String encryptedToken) {
        final String encTokenPatternString = LOWER_ALPHANUM_REGEX + "{" + expectedLength + "}";
        final Pattern encTokenPattern = Pattern.compile(encTokenPatternString);
        Matcher m = encTokenPattern.matcher(encryptedToken);
        assertTrue("Encrypted token value did not match expected pattern '" + encTokenPatternString + "'. Result was: [" + encryptedToken + "].", m.matches());
    }

    private void verifyNoLogMessage(String messageRegex) {
        verifyNoLogMessage(outputMgr, messageRegex);
    }

    private void verifyNonNullAesKey(Key key) {
        assertNotNull("Key should not have been null but was.", key);
        assertEquals("Algorithm did not match expected value.", AES, key.getAlgorithm());
        assertEquals("Byte length of encoded secret key for new password string did not match expected value.", 16, key.getEncoded().length);
        assertEquals("Format of the secret key did not match expected value.", "RAW", key.getFormat());
    }

    private void verifyNonNullIvSpec(IvParameterSpec iv) {
        assertNotNull("IvParameterSpec should not have been null but was.", iv);
        assertEquals("Byte length of initialization vector did not match expected value.", 16, iv.getIV().length);
    }

    private void verifyNonNullClientSecretHash(byte[] hash) {
        assertNotNull("Result should not have been null but was.", hash);
        assertEquals("Hash length did not match expected value.", 32, hash.length);
    }
}
