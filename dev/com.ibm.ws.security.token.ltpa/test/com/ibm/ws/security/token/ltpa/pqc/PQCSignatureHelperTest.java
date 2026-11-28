/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.pqc;

import static org.junit.Assert.*;

import java.security.KeyPair;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for PQCSignatureHelper.
 */
public class PQCSignatureHelperTest {

    private String provider;
    private KeyPair mldsaKeyPair;
    private byte[] testData;

    @Before
    public void setUp() throws Exception {
        provider = PQCKeyGenerator.getFirstAvailableProvider();
        if (provider != null) {
            mldsaKeyPair = PQCKeyGenerator.generateMLDSAKeyPair(
                    PQCConstants.ALGORITHM_ML_DSA_65, provider);
        }
        testData = "Test data for PQC signature".getBytes("UTF-8");
    }

    @Test
    public void testSignAndVerifyMLDSA() throws Exception {
        if (provider == null) {
            System.out.println("Skipping test - no PQC provider available");
            return;
        }

        // Sign the data
        byte[] signature = PQCSignatureHelper.signMLDSA(
                testData, mldsaKeyPair.getPrivate(), provider);

        assertNotNull("Signature should not be null", signature);
        assertTrue("Signature should have non-zero length", signature.length > 0);

        // Verify the signature
        boolean isValid = PQCSignatureHelper.verifyMLDSA(
                testData, signature, mldsaKeyPair.getPublic(), provider);

        assertTrue("Signature should be valid", isValid);
    }

    @Test
    public void testVerifyMLDSA_WithTamperedData() throws Exception {
        if (provider == null) {
            System.out.println("Skipping test - no PQC provider available");
            return;
        }

        // Sign the data
        byte[] signature = PQCSignatureHelper.signMLDSA(
                testData, mldsaKeyPair.getPrivate(), provider);

        // Tamper with the data
        byte[] tamperedData = Arrays.copyOf(testData, testData.length);
        tamperedData[0] ^= 0xFF;

        // Verify should fail
        boolean isValid = PQCSignatureHelper.verifyMLDSA(
                tamperedData, signature, mldsaKeyPair.getPublic(), provider);

        assertFalse("Signature should be invalid for tampered data", isValid);
    }

    @Test
    public void testVerifyMLDSA_WithTamperedSignature() throws Exception {
        if (provider == null) {
            System.out.println("Skipping test - no PQC provider available");
            return;
        }

        // Sign the data
        byte[] signature = PQCSignatureHelper.signMLDSA(
                testData, mldsaKeyPair.getPrivate(), provider);

        // Tamper with the signature
        byte[] tamperedSignature = Arrays.copyOf(signature, signature.length);
        tamperedSignature[0] ^= 0xFF;

        // Verify should fail
        boolean isValid = PQCSignatureHelper.verifyMLDSA(
                testData, tamperedSignature, mldsaKeyPair.getPublic(), provider);

        assertFalse("Signature should be invalid for tampered signature", isValid);
    }

    @Test
    public void testGetMLDSASignatureSize() {
        assertEquals("ML-DSA-44 signature size should be 2420",
                2420, PQCSignatureHelper.getMLDSASignatureSize(PQCConstants.ALGORITHM_ML_DSA_44));

        assertEquals("ML-DSA-65 signature size should be 3309",
                3309, PQCSignatureHelper.getMLDSASignatureSize(PQCConstants.ALGORITHM_ML_DSA_65));

        assertEquals("ML-DSA-87 signature size should be 4627",
                4627, PQCSignatureHelper.getMLDSASignatureSize(PQCConstants.ALGORITHM_ML_DSA_87));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMLDSASignatureSize_InvalidAlgorithm() {
        PQCSignatureHelper.getMLDSASignatureSize("INVALID-ALGORITHM");
    }

    @Test
    public void testSignMLDSA_WithDifferentDataSizes() throws Exception {
        if (provider == null) {
            System.out.println("Skipping test - no PQC provider available");
            return;
        }

        // Test with small data
        byte[] smallData = "Small".getBytes("UTF-8");
        byte[] smallSig = PQCSignatureHelper.signMLDSA(
                smallData, mldsaKeyPair.getPrivate(), provider);
        assertTrue("Should sign small data",
                PQCSignatureHelper.verifyMLDSA(smallData, smallSig, mldsaKeyPair.getPublic(), provider));

        // Test with large data
        byte[] largeData = new byte[10000];
        Arrays.fill(largeData, (byte) 'A');
        byte[] largeSig = PQCSignatureHelper.signMLDSA(
                largeData, mldsaKeyPair.getPrivate(), provider);
        assertTrue("Should sign large data",
                PQCSignatureHelper.verifyMLDSA(largeData, largeSig, mldsaKeyPair.getPublic(), provider));

        // Signature sizes should be the same regardless of data size
        assertEquals("Signature size should be constant",
                smallSig.length, largeSig.length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSignMLDSA_WithNullData() throws Exception {
        if (provider == null) {
            throw new IllegalArgumentException("No provider available");
        }

        PQCSignatureHelper.signMLDSA(null, mldsaKeyPair.getPrivate(), provider);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSignMLDSA_WithNullPrivateKey() throws Exception {
        if (provider == null) {
            throw new IllegalArgumentException("No provider available");
        }

        PQCSignatureHelper.signMLDSA(testData, null, provider);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyMLDSA_WithNullData() throws Exception {
        if (provider == null) {
            throw new IllegalArgumentException("No provider available");
        }

        byte[] signature = PQCSignatureHelper.signMLDSA(
                testData, mldsaKeyPair.getPrivate(), provider);

        PQCSignatureHelper.verifyMLDSA(null, signature, mldsaKeyPair.getPublic(), provider);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyMLDSA_WithNullSignature() throws Exception {
        if (provider == null) {
            throw new IllegalArgumentException("No provider available");
        }

        PQCSignatureHelper.verifyMLDSA(testData, null, mldsaKeyPair.getPublic(), provider);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyMLDSA_WithNullPublicKey() throws Exception {
        if (provider == null) {
            throw new IllegalArgumentException("No provider available");
        }

        byte[] signature = PQCSignatureHelper.signMLDSA(
                testData, mldsaKeyPair.getPrivate(), provider);

        PQCSignatureHelper.verifyMLDSA(testData, signature, null, provider);
    }
}
