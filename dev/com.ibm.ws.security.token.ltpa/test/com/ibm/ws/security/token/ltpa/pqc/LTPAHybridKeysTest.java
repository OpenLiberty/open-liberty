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
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.token.ltpa.LTPAHybridKeys;

/**
 * Unit tests for LTPAHybridKeys - Phase 2 hybrid key container.
 */
public class LTPAHybridKeysTest {

    private KeyPair rsaKeyPair;
    private byte[] mldsaPrivateKey;
    private byte[] mldsaPublicKey;
    private byte[] mlkemPrivateKey;
    private byte[] mlkemPublicKey;

    @Before
    public void setUp() throws Exception {
        // Generate RSA-2048 key pair
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(2048);
        rsaKeyPair = rsaGen.generateKeyPair();

        // Create mock ML-DSA keys (Level 3 - ML-DSA-65)
        mldsaPrivateKey = new byte[4032];
        mldsaPublicKey = new byte[1952];
        Arrays.fill(mldsaPrivateKey, (byte) 0xAA);
        Arrays.fill(mldsaPublicKey, (byte) 0xBB);

        // Create mock ML-KEM keys (Level 3 - ML-KEM-768)
        mlkemPrivateKey = new byte[2400];
        mlkemPublicKey = new byte[1184];
        Arrays.fill(mlkemPrivateKey, (byte) 0xCC);
        Arrays.fill(mlkemPublicKey, (byte) 0xDD);
    }

    @Test
    public void testConstructor_ValidKeys() {
        LTPAHybridKeys keys = new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);

        assertNotNull("Hybrid keys should not be null", keys);
        assertEquals("RSA private key should match", 
                rsaKeyPair.getPrivate(), keys.getRsaPrivateKey());
        assertEquals("RSA public key should match", 
                rsaKeyPair.getPublic(), keys.getRsaPublicKey());
        assertArrayEquals("ML-DSA private key should match", 
                mldsaPrivateKey, keys.getMldsaPrivateKeyBytes());
        assertArrayEquals("ML-DSA public key should match", 
                mldsaPublicKey, keys.getMldsaPublicKeyBytes());
        assertArrayEquals("ML-KEM private key should match", 
                mlkemPrivateKey, keys.getMlkemPrivateKeyBytes());
        assertArrayEquals("ML-KEM public key should match", 
                mlkemPublicKey, keys.getMlkemPublicKeyBytes());
        assertEquals("ML-DSA algorithm should match", 
                MLDSAAlgorithmType.ML_DSA_65, keys.getMldsaAlgorithm());
        assertEquals("ML-KEM algorithm should match", 
                MLKEMAlgorithmType.ML_KEM_768, keys.getMlkemAlgorithm());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullRsaPrivateKey() {
        new LTPAHybridKeys(
                null,
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullRsaPublicKey() {
        new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                null,
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullMldsaPrivateKey() {
        new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                null,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullMldsaPublicKey() {
        new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                null,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullMlkemPrivateKey() {
        new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                null,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullMlkemPublicKey() {
        new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                null,
                MLKEMAlgorithmType.ML_KEM_768);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullMldsaAlgorithm() {
        new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                null,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullMlkemAlgorithm() {
        new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_IncompatibleAlgorithms() {
        // ML-DSA-44 (Level 1) with ML-KEM-1024 (Level 5) - incompatible
        new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                new byte[2560], // ML-DSA-44 private key size
                new byte[1312], // ML-DSA-44 public key size
                MLDSAAlgorithmType.ML_DSA_44,
                new byte[3168], // ML-KEM-1024 private key size
                new byte[1568], // ML-KEM-1024 public key size
                MLKEMAlgorithmType.ML_KEM_1024);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_InvalidMldsaPrivateKeySize() {
        // Wrong size for ML-DSA-65 (should be 4032)
        new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                new byte[1000], // Wrong size
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_InvalidMldsaPublicKeySize() {
        // Wrong size for ML-DSA-65 (should be 1952)
        new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                new byte[1000], // Wrong size
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_InvalidMlkemPublicKeySize() {
        // Wrong size for ML-KEM-768 (should be 1184)
        new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                new byte[1000], // Wrong size
                MLKEMAlgorithmType.ML_KEM_768);
    }

    @Test
    public void testGetSecurityLevel() {
        LTPAHybridKeys keys = new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);

        assertEquals("Security level should be 3", 3, keys.getSecurityLevel());
    }

    @Test
    public void testDefensiveCopying() {
        byte[] originalMldsaPrivate = Arrays.copyOf(mldsaPrivateKey, mldsaPrivateKey.length);
        byte[] originalMldsaPublic = Arrays.copyOf(mldsaPublicKey, mldsaPublicKey.length);
        byte[] originalMlkemPrivate = Arrays.copyOf(mlkemPrivateKey, mlkemPrivateKey.length);
        byte[] originalMlkemPublic = Arrays.copyOf(mlkemPublicKey, mlkemPublicKey.length);

        LTPAHybridKeys keys = new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);

        // Modify the original arrays
        Arrays.fill(mldsaPrivateKey, (byte) 0x00);
        Arrays.fill(mldsaPublicKey, (byte) 0x00);
        Arrays.fill(mlkemPrivateKey, (byte) 0x00);
        Arrays.fill(mlkemPublicKey, (byte) 0x00);

        // Keys should still have the original values (defensive copy)
        assertArrayEquals("ML-DSA private key should be unchanged",
                originalMldsaPrivate, keys.getMldsaPrivateKeyBytes());
        assertArrayEquals("ML-DSA public key should be unchanged",
                originalMldsaPublic, keys.getMldsaPublicKeyBytes());
        assertArrayEquals("ML-KEM private key should be unchanged",
                originalMlkemPrivate, keys.getMlkemPrivateKeyBytes());
        assertArrayEquals("ML-KEM public key should be unchanged",
                originalMlkemPublic, keys.getMlkemPublicKeyBytes());
    }

    @Test
    public void testGettersReturnDefensiveCopies() {
        LTPAHybridKeys keys = new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);

        // Get the keys
        byte[] retrievedMldsaPrivate = keys.getMldsaPrivateKeyBytes();
        byte[] retrievedMldsaPublic = keys.getMldsaPublicKeyBytes();
        byte[] retrievedMlkemPrivate = keys.getMlkemPrivateKeyBytes();
        byte[] retrievedMlkemPublic = keys.getMlkemPublicKeyBytes();

        // Modify the retrieved arrays
        Arrays.fill(retrievedMldsaPrivate, (byte) 0x00);
        Arrays.fill(retrievedMldsaPublic, (byte) 0x00);
        Arrays.fill(retrievedMlkemPrivate, (byte) 0x00);
        Arrays.fill(retrievedMlkemPublic, (byte) 0x00);

        // Original keys should be unchanged (defensive copy)
        assertFalse("ML-DSA private key should be unchanged",
                Arrays.equals(retrievedMldsaPrivate, keys.getMldsaPrivateKeyBytes()));
        assertFalse("ML-DSA public key should be unchanged",
                Arrays.equals(retrievedMldsaPublic, keys.getMldsaPublicKeyBytes()));
        assertFalse("ML-KEM private key should be unchanged",
                Arrays.equals(retrievedMlkemPrivate, keys.getMlkemPrivateKeyBytes()));
        assertFalse("ML-KEM public key should be unchanged",
                Arrays.equals(retrievedMlkemPublic, keys.getMlkemPublicKeyBytes()));
    }

    @Test
    public void testClear() {
        LTPAHybridKeys keys = new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);

        // Clear the keys
        keys.clear();

        // All byte arrays should be zeroed
        byte[] clearedMldsaPrivate = keys.getMldsaPrivateKeyBytes();
        byte[] clearedMldsaPublic = keys.getMldsaPublicKeyBytes();
        byte[] clearedMlkemPrivate = keys.getMlkemPrivateKeyBytes();
        byte[] clearedMlkemPublic = keys.getMlkemPublicKeyBytes();

        assertTrue("ML-DSA private key should be zeroed",
                isAllZeros(clearedMldsaPrivate));
        assertTrue("ML-DSA public key should be zeroed",
                isAllZeros(clearedMldsaPublic));
        assertTrue("ML-KEM private key should be zeroed",
                isAllZeros(clearedMlkemPrivate));
        assertTrue("ML-KEM public key should be zeroed",
                isAllZeros(clearedMlkemPublic));
    }

    @Test
    public void testAllSecurityLevels() {
        // Test Level 1 (128-bit)
        testSecurityLevel(
                MLDSAAlgorithmType.ML_DSA_44,
                MLKEMAlgorithmType.ML_KEM_512,
                1);

        // Test Level 3 (192-bit)
        testSecurityLevel(
                MLDSAAlgorithmType.ML_DSA_65,
                MLKEMAlgorithmType.ML_KEM_768,
                3);

        // Test Level 5 (256-bit)
        testSecurityLevel(
                MLDSAAlgorithmType.ML_DSA_87,
                MLKEMAlgorithmType.ML_KEM_1024,
                5);
    }

    private void testSecurityLevel(MLDSAAlgorithmType mldsaType, 
                                   MLKEMAlgorithmType mlkemType, 
                                   int expectedLevel) {
        byte[] mldsaPriv = new byte[mldsaType.getPrivateKeySize()];
        byte[] mldsaPub = new byte[mldsaType.getPublicKeySize()];
        byte[] mlkemPriv = new byte[mlkemType.getPublicKeySize() * 2];
        byte[] mlkemPub = new byte[mlkemType.getPublicKeySize()];

        LTPAHybridKeys keys = new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPriv,
                mldsaPub,
                mldsaType,
                mlkemPriv,
                mlkemPub,
                mlkemType);

        assertEquals("Security level should match", expectedLevel, keys.getSecurityLevel());
        assertEquals("ML-DSA algorithm should match", mldsaType, keys.getMldsaAlgorithm());
        assertEquals("ML-KEM algorithm should match", mlkemType, keys.getMlkemAlgorithm());
    }

    @Test
    public void testToString() {
        LTPAHybridKeys keys = new LTPAHybridKeys(
                rsaKeyPair.getPrivate(),
                rsaKeyPair.getPublic(),
                mldsaPrivateKey,
                mldsaPublicKey,
                MLDSAAlgorithmType.ML_DSA_65,
                mlkemPrivateKey,
                mlkemPublicKey,
                MLKEMAlgorithmType.ML_KEM_768);

        String str = keys.toString();
        
        assertNotNull("toString should not return null", str);
        assertTrue("toString should contain ML-DSA algorithm", 
                str.contains("ML-DSA-65"));
        assertTrue("toString should contain ML-KEM algorithm", 
                str.contains("ML-KEM-768"));
        assertTrue("toString should contain security level", 
                str.contains("Level 3"));
    }

    private boolean isAllZeros(byte[] array) {
        for (byte b : array) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}

// Made with Bob
