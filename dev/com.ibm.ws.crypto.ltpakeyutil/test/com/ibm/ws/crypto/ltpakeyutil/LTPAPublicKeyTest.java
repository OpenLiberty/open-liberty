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

import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

public class LTPAPublicKeyTest {
    private static SharedOutputManager outputMgr;
    private static final int MODULUS_LENGTH = 129;
    private static final int EXPONENT_LENGTH = 3;

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
            LTPAPublicKey ltpaPublicKey = new LTPAPublicKey(rawKey);

            assertNotNull("There must be an LTPA public key.", ltpaPublicKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testConstructorWithEncodedKey() {
        final String methodName = "testConstructorWithEncodedKey";
        try {
            byte[] encodedKey = getEncodedKey();
            LTPAPublicKey ltpaPublicKey = new LTPAPublicKey(encodedKey);

            assertNotNull("There must be an LTPA public key.", ltpaPublicKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRawKeyIsEncodedWithRightLength() {
        final String methodName = "testRawKeyIsEncodedWithRightLength";
        try {
            int expectedLength = MODULUS_LENGTH + EXPONENT_LENGTH;
            byte[][] rawKey = getRawKey();
            LTPAPublicKey ltpaPublicKey = new LTPAPublicKey(rawKey);
            byte[] encodedKey = ltpaPublicKey.getEncoded();

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
            LTPAPublicKey ltpaPublicKey = new LTPAPublicKey(encodedKey);
            byte[][] actualRawKey = ltpaPublicKey.getRawKey();

            assertEquals("The modulus must have the right length.", MODULUS_LENGTH, actualRawKey[0].length);
            assertEquals("The exponent must have the right length.", EXPONENT_LENGTH, actualRawKey[1].length);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRawKeyIsEncodedWithRightContents() {
        final String methodName = "testRawKeyIsEncodedWithRightContents";
        try {
            byte[][] rawKey = getRawKey();
            byte[] expectedEncodedKey = getExpectedEncodedKeyFromRawKey(rawKey);
            LTPAPublicKey ltpaPublicKey = new LTPAPublicKey(rawKey);
            byte[] actualEncodedKey = ltpaPublicKey.getEncoded();

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
            byte[] expectedModulus = getExpectedModulusFromEncodedKey(encodedKey);
            byte[] expectedExponent = getExpectedExponentFromEncodedKey(encodedKey);
            LTPAPublicKey ltpaPublicKey = new LTPAPublicKey(encodedKey);
            byte[][] actualRawKey = ltpaPublicKey.getRawKey();

            assertEquals("The modulus must have the contents.", new String(expectedModulus), new String(actualRawKey[0]));
            assertEquals("The exponent must have the right contents.", new String(expectedExponent), new String(actualRawKey[1]));
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
            LTPAPublicKey ltpaPublicKey = new LTPAPublicKey(rawKey);
            String actualAlgorithm = ltpaPublicKey.getAlgorithm();

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
            LTPAPublicKey ltpaPublicKey = new LTPAPublicKey(rawKey);
            String actualFormat = ltpaPublicKey.getFormat();

            assertEquals("The actual algorithm must be RSA/SHA-1", expectedFormat, actualFormat);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private byte[][] getRawKey() {
        Random random = new Random();
        byte[][] rawKey = new byte[2][];
        rawKey[0] = new byte[MODULUS_LENGTH];
        rawKey[1] = new byte[EXPONENT_LENGTH];
        random.nextBytes(rawKey[0]);
        random.nextBytes(rawKey[1]);
        return rawKey;
    }

    private byte[] getEncodedKey() {
        Random random = new Random();
        byte[] encodedKey = new byte[MODULUS_LENGTH + EXPONENT_LENGTH];
        random.nextBytes(encodedKey);
        return encodedKey;
    }

    private byte[] getExpectedEncodedKeyFromRawKey(byte[][] rawKey) {
        byte[] modulus = rawKey[0];
        byte[] exponent = rawKey[1];
        int modulusLength = modulus.length;
        int exponentLength = exponent.length;
        byte[] expectedEncodedKey = new byte[modulusLength + exponentLength];
        System.arraycopy(modulus, 0, expectedEncodedKey, 0, modulusLength);
        System.arraycopy(exponent, 0, expectedEncodedKey, modulusLength, exponentLength);
        return expectedEncodedKey;
    }

    private byte[] getExpectedModulusFromEncodedKey(byte[] encodedKey) {
        byte[] expecetdModulus = new byte[MODULUS_LENGTH];
        System.arraycopy(encodedKey, 0, expecetdModulus, 0, MODULUS_LENGTH);
        return expecetdModulus;
    }

    private byte[] getExpectedExponentFromEncodedKey(byte[] encodedKey) {
        byte[] expectedExponent = new byte[EXPONENT_LENGTH];
        System.arraycopy(encodedKey, MODULUS_LENGTH, expectedExponent, 0, EXPONENT_LENGTH);
        return expectedExponent;
    }
}
