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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for LTPAPQCSignature - Phase 2 ML-DSA signature operations.
 */
public class LTPAPQCSignatureTest {

    private boolean pqcAvailable;

    @Before
    public void setUp() {
        pqcAvailable = PQCRuntimeSupport.isPQCAvailable();
        if (!pqcAvailable) {
            System.out.println("WARNING: PQC runtime not available - some tests will be skipped");
            System.out.println("Requires Java 26+ with ML-KEM/ML-DSA support");
        }
    }

    @Test
    public void testGenerateMLDSAKeyPair_ML_DSA_44() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_44);

        assertNotNull("KeyPair should not be null", keyPair);
        assertNotNull("Private key should not be null", keyPair.getPrivate());
        assertNotNull("Public key should not be null", keyPair.getPublic());

        // Verify key sizes
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        assertTrue("ML-DSA-44 public key should be ~1312 bytes",
                publicKeyBytes.length > 1200 && publicKeyBytes.length < 1400);
        assertTrue("ML-DSA-44 private key should be ~2560 bytes",
                privateKeyBytes.length > 2400 && privateKeyBytes.length < 2700);
    }

    @Test
    public void testGenerateMLDSAKeyPair_ML_DSA_65() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);

        assertNotNull("KeyPair should not be null", keyPair);

        // Verify key sizes
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        assertTrue("ML-DSA-65 public key should be ~1952 bytes",
                publicKeyBytes.length > 1800 && publicKeyBytes.length < 2100);
        assertTrue("ML-DSA-65 private key should be ~4032 bytes",
                privateKeyBytes.length > 3800 && privateKeyBytes.length < 4200);
    }

    @Test
    public void testGenerateMLDSAKeyPair_ML_DSA_87() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_87);

        assertNotNull("KeyPair should not be null", keyPair);

        // Verify key sizes
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        assertTrue("ML-DSA-87 public key should be ~2592 bytes",
                publicKeyBytes.length > 2400 && publicKeyBytes.length < 2800);
        assertTrue("ML-DSA-87 private key should be ~4896 bytes",
                privateKeyBytes.length > 4600 && privateKeyBytes.length < 5100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateMLDSAKeyPair_NullAlgorithm() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        LTPAPQCSignature.generateMLDSAKeyPair(null);
    }

    @Test
    public void testSignAndVerify_ML_DSA_65() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);
        byte[] data = "Test data for ML-DSA signature".getBytes("UTF-8");

        // Sign the data
        byte[] signature = LTPAPQCSignature.sign(
                data, 
                keyPair.getPrivate().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);

        assertNotNull("Signature should not be null", signature);
        assertEquals("Signature size should match ML-DSA-65 specification",
                3309, signature.length);

        // Verify the signature
        boolean isValid = LTPAPQCSignature.verify(
                data, 
                signature, 
                keyPair.getPublic().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);

        assertTrue("Signature should be valid", isValid);
    }

    @Test
    public void testSignAndVerify_AllAlgorithms() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        byte[] data = "Test data for all ML-DSA algorithms".getBytes("UTF-8");

        // Test ML-DSA-44
        testSignVerifyWithAlgorithm(data, MLDSAAlgorithmType.ML_DSA_44);

        // Test ML-DSA-65
        testSignVerifyWithAlgorithm(data, MLDSAAlgorithmType.ML_DSA_65);

        // Test ML-DSA-87
        testSignVerifyWithAlgorithm(data, MLDSAAlgorithmType.ML_DSA_87);
    }

    private void testSignVerifyWithAlgorithm(byte[] data, MLDSAAlgorithmType algorithm) 
            throws Exception {
        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(algorithm);
        
        byte[] signature = LTPAPQCSignature.sign(
                data, 
                keyPair.getPrivate().getEncoded(), 
                algorithm);

        assertEquals("Signature size should match algorithm specification",
                algorithm.getSignatureSize(), signature.length);

        boolean isValid = LTPAPQCSignature.verify(
                data, 
                signature, 
                keyPair.getPublic().getEncoded(), 
                algorithm);

        assertTrue("Signature should be valid for " + algorithm, isValid);
    }

    @Test
    public void testVerify_TamperedData() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);
        byte[] data = "Original data".getBytes("UTF-8");

        byte[] signature = LTPAPQCSignature.sign(
                data, 
                keyPair.getPrivate().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);

        // Tamper with the data
        byte[] tamperedData = "Tampered data".getBytes("UTF-8");

        boolean isValid = LTPAPQCSignature.verify(
                tamperedData, 
                signature, 
                keyPair.getPublic().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);

        assertFalse("Signature should be invalid for tampered data", isValid);
    }

    @Test
    public void testVerify_TamperedSignature() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);
        byte[] data = "Test data".getBytes("UTF-8");

        byte[] signature = LTPAPQCSignature.sign(
                data, 
                keyPair.getPrivate().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);

        // Tamper with the signature
        byte[] tamperedSignature = Arrays.copyOf(signature, signature.length);
        tamperedSignature[0] ^= 0xFF;

        boolean isValid = LTPAPQCSignature.verify(
                data, 
                tamperedSignature, 
                keyPair.getPublic().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);

        assertFalse("Signature should be invalid for tampered signature", isValid);
    }

    @Test
    public void testVerify_WrongPublicKey() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair1 = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);
        KeyPair keyPair2 = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);
        byte[] data = "Test data".getBytes("UTF-8");

        byte[] signature = LTPAPQCSignature.sign(
                data, 
                keyPair1.getPrivate().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);

        // Try to verify with wrong public key
        boolean isValid = LTPAPQCSignature.verify(
                data, 
                signature, 
                keyPair2.getPublic().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);

        assertFalse("Signature should be invalid with wrong public key", isValid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSign_NullData() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);
        LTPAPQCSignature.sign(null, keyPair.getPrivate().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSign_NullPrivateKey() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        byte[] data = "Test data".getBytes("UTF-8");
        LTPAPQCSignature.sign(data, null, MLDSAAlgorithmType.ML_DSA_65);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSign_NullAlgorithm() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);
        byte[] data = "Test data".getBytes("UTF-8");
        LTPAPQCSignature.sign(data, keyPair.getPrivate().getEncoded(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerify_NullData() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);
        byte[] signature = new byte[3309];
        LTPAPQCSignature.verify(null, signature, keyPair.getPublic().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerify_NullSignature() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);
        byte[] data = "Test data".getBytes("UTF-8");
        LTPAPQCSignature.verify(data, null, keyPair.getPublic().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerify_NullPublicKey() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        byte[] data = "Test data".getBytes("UTF-8");
        byte[] signature = new byte[3309];
        LTPAPQCSignature.verify(data, signature, null, MLDSAAlgorithmType.ML_DSA_65);
    }

    @Test
    public void testReconstructPrivateKey() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        PrivateKey reconstructed = LTPAPQCSignature.reconstructPrivateKey(
                privateKeyBytes, MLDSAAlgorithmType.ML_DSA_65);

        assertNotNull("Reconstructed private key should not be null", reconstructed);
        assertArrayEquals("Reconstructed key should match original",
                privateKeyBytes, reconstructed.getEncoded());
    }

    @Test
    public void testReconstructPublicKey() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();

        PublicKey reconstructed = LTPAPQCSignature.reconstructPublicKey(
                publicKeyBytes, MLDSAAlgorithmType.ML_DSA_65);

        assertNotNull("Reconstructed public key should not be null", reconstructed);
        assertArrayEquals("Reconstructed key should match original",
                publicKeyBytes, reconstructed.getEncoded());
    }

    @Test
    public void testSignWithDifferentDataSizes() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);

        // Test with small data
        byte[] smallData = "Small".getBytes("UTF-8");
        byte[] smallSig = LTPAPQCSignature.sign(
                smallData, keyPair.getPrivate().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);

        // Test with large data
        byte[] largeData = new byte[10000];
        Arrays.fill(largeData, (byte) 'A');
        byte[] largeSig = LTPAPQCSignature.sign(
                largeData, keyPair.getPrivate().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);

        // Signature sizes should be the same regardless of data size
        assertEquals("Signature size should be constant",
                smallSig.length, largeSig.length);
        assertEquals("Signature size should match specification",
                3309, smallSig.length);

        // Both signatures should verify
        assertTrue("Small data signature should verify",
                LTPAPQCSignature.verify(smallData, smallSig, 
                        keyPair.getPublic().getEncoded(), MLDSAAlgorithmType.ML_DSA_65));
        assertTrue("Large data signature should verify",
                LTPAPQCSignature.verify(largeData, largeSig, 
                        keyPair.getPublic().getEncoded(), MLDSAAlgorithmType.ML_DSA_65));
    }

    @Test
    public void testMultipleSignaturesWithSameKey() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = LTPAPQCSignature.generateMLDSAKeyPair(MLDSAAlgorithmType.ML_DSA_65);

        // Sign multiple different messages with the same key
        byte[] data1 = "Message 1".getBytes("UTF-8");
        byte[] data2 = "Message 2".getBytes("UTF-8");
        byte[] data3 = "Message 3".getBytes("UTF-8");

        byte[] sig1 = LTPAPQCSignature.sign(data1, keyPair.getPrivate().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);
        byte[] sig2 = LTPAPQCSignature.sign(data2, keyPair.getPrivate().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);
        byte[] sig3 = LTPAPQCSignature.sign(data3, keyPair.getPrivate().getEncoded(), 
                MLDSAAlgorithmType.ML_DSA_65);

        // All signatures should be different
        assertFalse("Signatures should be different", Arrays.equals(sig1, sig2));
        assertFalse("Signatures should be different", Arrays.equals(sig2, sig3));
        assertFalse("Signatures should be different", Arrays.equals(sig1, sig3));

        // All signatures should verify with the same public key
        assertTrue("Signature 1 should verify",
                LTPAPQCSignature.verify(data1, sig1, keyPair.getPublic().getEncoded(), 
                        MLDSAAlgorithmType.ML_DSA_65));
        assertTrue("Signature 2 should verify",
                LTPAPQCSignature.verify(data2, sig2, keyPair.getPublic().getEncoded(), 
                        MLDSAAlgorithmType.ML_DSA_65));
        assertTrue("Signature 3 should verify",
                LTPAPQCSignature.verify(data3, sig3, keyPair.getPublic().getEncoded(), 
                        MLDSAAlgorithmType.ML_DSA_65));
    }
}

// Made with Bob
