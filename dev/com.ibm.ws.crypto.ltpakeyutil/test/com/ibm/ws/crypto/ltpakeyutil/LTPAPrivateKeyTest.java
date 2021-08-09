/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.ltpakeyutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

public class LTPAPrivateKeyTest {
    private static SharedOutputManager outputMgr;
    private static final int PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH = 4;
    private static final int EXPONENT_LENGTH = 3;
    private static final int PRIME_P_LENGTH = 65;
    private static final int PRIME_Q_LENGTH = 65;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testConstructorWithRawKey() {
        final String methodName = "testConstructorWithRawKey";
        try {
            byte[][] rawKey = getRawKey();
            LTPAPrivateKey ltpaPrivateKey = new LTPAPrivateKey(rawKey);

            assertNotNull("There must be an LTPA private key.", ltpaPrivateKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRawKeyIsEncodedWithRightLength() {
        final String methodName = "testRawKeyIsEncodedWithRightLength";
        try {
            byte[][] rawKey = getRawKey();
            LTPAPrivateKey ltpaPrivateKey = new LTPAPrivateKey(rawKey);
            byte[][] completeRawKey = ltpaPrivateKey.getRawKey();
            int expectedLength = computeExpectedLength(completeRawKey);
            byte[] encodedKey = ltpaPrivateKey.getEncoded();

            assertEquals("The raw key must be encoded with right length.", expectedLength, encodedKey.length);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testEncodedKeyIsDecodedWithRightLengths() {
        final String methodName = "testEncodedKeyIsDecodedWithRightLengths";
        try {
            byte[] encodedKey = getEncodedKey();
            LTPAPrivateKey ltpaPrivateKey = new LTPAPrivateKey(encodedKey);
            byte[][] actualRawKey = ltpaPrivateKey.getRawKey();

            assertEquals("The public exponent must have the right length.", EXPONENT_LENGTH, actualRawKey[2].length);
            assertEquals("The prime p must have the right length.", PRIME_P_LENGTH, actualRawKey[3].length);
            assertEquals("The prime q must have the right length.", PRIME_Q_LENGTH, actualRawKey[4].length);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRawKeyIsEncodedWithRightContents() {
        final String methodName = "testRawKeyIsEncodedWithRightContents";
        try {
            byte[][] rawKey = getRawKey();
            LTPAPrivateKey ltpaPrivateKey = new LTPAPrivateKey(rawKey);
            byte[][] completeRawKey = ltpaPrivateKey.getRawKey();
            byte[] expectedEncodedKey = getExpectedEncodedKeyFromRawKey(completeRawKey);
            byte[] actualEncodedKey = ltpaPrivateKey.getEncoded();

            assertEquals("The raw key must be properly encoded with right contents.", new String(expectedEncodedKey), new String(actualEncodedKey));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testEncodedKeyIsDecodedWithRightContents() {
        final String methodName = "testEncodedKeyIsDecodedWithRightContents";
        try {
            byte[] encodedKey = getEncodedKey();
            byte[] expectedPrivateExponent = getExpectedPrivateExponentFromEncodedKey(encodedKey);
            byte[] expectedPublicExponent = getExpectedPublicExponentFromEncodedKey(encodedKey);
            byte[] expectedPrimeP = getExpectedPrimePFromEncodedKey(encodedKey);
            byte[] expectedPrimeQ = getExpectedPrimeQFromEncodedKey(encodedKey);
            LTPAPrivateKey ltpaPrivateKey = new LTPAPrivateKey(encodedKey);
            byte[][] actualRawKey = ltpaPrivateKey.getRawKey();

            assertEquals("The private exponent must have the right contents.", new String(expectedPrivateExponent), new String(actualRawKey[1]));
            assertEquals("The public exponent must have the right contents.", new String(expectedPublicExponent), new String(actualRawKey[2]));
            assertEquals("The prime p must have the right contents.", new String(expectedPrimeP), new String(actualRawKey[3]));
            assertEquals("The prime q must have the right contents.", new String(expectedPrimeQ), new String(actualRawKey[4]));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetAlgorithm() {
        final String methodName = "testGetAlgorithm";
        try {
            String expectedAlgorithm = "RSA/SHA-1";
            byte[][] rawKey = getRawKey();
            LTPAPrivateKey ltpaPrivateKey = new LTPAPrivateKey(rawKey);
            String actualAlgorithm = ltpaPrivateKey.getAlgorithm();

            assertEquals("The actual algorithm must be RSA/SHA-1", expectedAlgorithm, actualAlgorithm);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetFormat() {
        final String methodName = "testGetFormat";
        try {
            String expectedFormat = "LTPAFormat";
            byte[][] rawKey = getRawKey();
            LTPAPrivateKey ltpaPrivateKey = new LTPAPrivateKey(rawKey);
            String actualFormat = ltpaPrivateKey.getFormat();

            assertEquals("The actual algorithm must be RSA/SHA-1", expectedFormat, actualFormat);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private byte[][] getRawKey() {
        byte[][] rsaPublicKey = new byte[2][];
        byte[][] rsaPrivateKey = new byte[8][];
        LTPADigSignature.generateRSAKeys(rsaPublicKey, rsaPrivateKey);
        return rsaPrivateKey;
    }

    private int computeExpectedLength(byte[][] rawKey) {
        int keyLength = PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH;
        for (int i = 1; i <= 4; i++) {
            if (rawKey[i] != null) {
                keyLength += rawKey[i].length;
            }
        }
        return keyLength;
    }

    private byte[] getEncodedKey() {
        byte[][] rawKey = getRawKey();
        LTPAPrivateKey ltpaPrivateKey = new LTPAPrivateKey(rawKey);
        return ltpaPrivateKey.getEncoded();
    }

    private byte[] getExpectedEncodedKeyFromRawKey(byte[][] rawKey) {
        byte[] privateExponent = rawKey[1];
        byte[] publicExponent = rawKey[2];
        byte[] primeP = rawKey[3];
        byte[] primeQ = rawKey[4];
        byte[] privateExponentLengthBytes = LTPAPrivateKey.toByteArray(privateExponent.length);
        int encodedKeyLength = PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponent.length + publicExponent.length + primeP.length + primeQ.length;
        byte[] expectedEncodedKey = new byte[encodedKeyLength];

        System.arraycopy(privateExponentLengthBytes, 0, expectedEncodedKey, 0, PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH);
        System.arraycopy(privateExponent, 0, expectedEncodedKey, PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH, privateExponent.length);
        System.arraycopy(publicExponent, 0, expectedEncodedKey, PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponent.length, publicExponent.length);
        System.arraycopy(primeP, 0, expectedEncodedKey, PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponent.length + publicExponent.length, primeP.length);
        System.arraycopy(primeQ, 0, expectedEncodedKey, PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponent.length + publicExponent.length + primeP.length, primeQ.length);
        return expectedEncodedKey;
    }

    private byte[] getExpectedPrivateExponentFromEncodedKey(byte[] encodedKey) {
        int privateExponentLength = getPrivateExponentLength(encodedKey);
        byte[] expectedPrivateExponent = new byte[privateExponentLength];
        System.arraycopy(encodedKey, PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH, expectedPrivateExponent, 0, privateExponentLength);
        return expectedPrivateExponent;
    }

    private byte[] getExpectedPublicExponentFromEncodedKey(byte[] encodedKey) {
        byte[] expectedPublicExponent = new byte[EXPONENT_LENGTH];
        int privateExponentLength = getPrivateExponentLength(encodedKey);
        int publicExponentOffset = PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponentLength;
        System.arraycopy(encodedKey, publicExponentOffset, expectedPublicExponent, 0, EXPONENT_LENGTH);
        return expectedPublicExponent;
    }

    private byte[] getExpectedPrimePFromEncodedKey(byte[] encodedKey) {
        byte[] expectedPrimeP = new byte[PRIME_P_LENGTH];
        int privateExponentLength = getPrivateExponentLength(encodedKey);
        int primePOffset = PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponentLength + EXPONENT_LENGTH;
        System.arraycopy(encodedKey, primePOffset, expectedPrimeP, 0, PRIME_P_LENGTH);
        return expectedPrimeP;
    }

    private byte[] getExpectedPrimeQFromEncodedKey(byte[] encodedKey) {
        byte[] expectedPrimeQ = new byte[PRIME_Q_LENGTH];
        int privateExponentLength = getPrivateExponentLength(encodedKey);
        int primeQOffset = PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponentLength + EXPONENT_LENGTH + PRIME_P_LENGTH;
        System.arraycopy(encodedKey, primeQOffset, expectedPrimeQ, 0, PRIME_Q_LENGTH);
        return expectedPrimeQ;
    }

    private int getPrivateExponentLength(byte[] encodedKey) {
        byte[] lengthBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            lengthBytes[i] = encodedKey[i];
        }
        return LTPAPrivateKey.toInt(lengthBytes);
    }
}
