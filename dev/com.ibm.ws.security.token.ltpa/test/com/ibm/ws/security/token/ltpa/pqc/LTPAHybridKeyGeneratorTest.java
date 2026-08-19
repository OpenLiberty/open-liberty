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

import java.security.PrivateKey;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.token.ltpa.LTPAHybridKeyGenerator;
import com.ibm.ws.security.token.ltpa.LTPAHybridKeys;

/**
 * Unit tests for LTPAHybridKeyGenerator - Phase 2 hybrid key generation.
 * Tests RSA-2048 + ML-DSA + ML-KEM key generation with all security levels.
 */
public class LTPAHybridKeyGeneratorTest {

    private boolean pqcAvailable;

    @Before
    public void setUp() {
        // Check if PQC runtime support is available
        pqcAvailable = PQCRuntimeSupport.isPQCAvailable();
        if (!pqcAvailable) {
            System.out.println("WARNING: PQC runtime not available - some tests will be skipped");
            System.out.println("Requires Java 26+ with ML-KEM/ML-DSA support");
        }
    }

    @Test
    public void testGenerateKeys_DefaultSecurityLevel() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        LTPAHybridKeys keys = LTPAHybridKeyGenerator.generateKeys();

        assertNotNull("Hybrid keys should not be null", keys);
        assertNotNull("RSA private key should not be null", keys.getRsaPrivateKey());
        assertNotNull("RSA public key should not be null", keys.getRsaPublicKey());
        assertNotNull("ML-DSA private key should not be null", keys.getMldsaPrivateKeyBytes());
        assertNotNull("ML-DSA public key should not be null", keys.getMldsaPublicKeyBytes());
        assertNotNull("ML-KEM public key should not be null", keys.getMlkemPublicKeyBytes());
        assertNotNull("ML-KEM private key should not be null", keys.getMlkemPrivateKeyBytes());

        // Verify RSA key is 2048-bit
        PrivateKey rsaKey = keys.getRsaPrivateKey();
        assertEquals("RSA algorithm should be RSA", "RSA", rsaKey.getAlgorithm());

        // Verify default security level (Level 3 - 192-bit)
        assertEquals("Default ML-DSA should be ML-DSA-65", 
                MLDSAAlgorithmType.ML_DSA_65, keys.getMldsaAlgorithm());
        assertEquals("Default ML-KEM should be ML-KEM-768", 
                MLKEMAlgorithmType.ML_KEM_768, keys.getMlkemAlgorithm());
    }

    @Test
    public void testGenerateKeys_Level1Security() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        LTPAHybridKeys keys = LTPAHybridKeyGenerator.generateKeys(1);

        assertNotNull("Hybrid keys should not be null", keys);
        assertEquals("ML-DSA should be ML-DSA-44 for Level 1", 
                MLDSAAlgorithmType.ML_DSA_44, keys.getMldsaAlgorithm());
        assertEquals("ML-KEM should be ML-KEM-512 for Level 1", 
                MLKEMAlgorithmType.ML_KEM_512, keys.getMlkemAlgorithm());

        // Verify key sizes for Level 1
        assertTrue("ML-DSA-44 public key should be ~1312 bytes",
                keys.getMldsaPublicKeyBytes().length > 1200 &&
                keys.getMldsaPublicKeyBytes().length < 1400);
        assertTrue("ML-KEM-512 public key should be ~800 bytes",
                keys.getMlkemPublicKeyBytes().length > 700 &&
                keys.getMlkemPublicKeyBytes().length < 900);
    }

    @Test
    public void testGenerateKeys_Level3Security() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        LTPAHybridKeys keys = LTPAHybridKeyGenerator.generateKeys(3);

        assertNotNull("Hybrid keys should not be null", keys);
        assertEquals("ML-DSA should be ML-DSA-65 for Level 3", 
                MLDSAAlgorithmType.ML_DSA_65, keys.getMldsaAlgorithm());
        assertEquals("ML-KEM should be ML-KEM-768 for Level 3", 
                MLKEMAlgorithmType.ML_KEM_768, keys.getMlkemAlgorithm());

        // Verify key sizes for Level 3
        assertTrue("ML-DSA-65 public key should be ~1952 bytes",
                keys.getMldsaPublicKeyBytes().length > 1800 &&
                keys.getMldsaPublicKeyBytes().length < 2100);
        assertTrue("ML-KEM-768 public key should be ~1184 bytes",
                keys.getMlkemPublicKeyBytes().length > 1100 &&
                keys.getMlkemPublicKeyBytes().length < 1300);
    }

    @Test
    public void testGenerateKeys_Level5Security() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        LTPAHybridKeys keys = LTPAHybridKeyGenerator.generateKeys(5);

        assertNotNull("Hybrid keys should not be null", keys);
        assertEquals("ML-DSA should be ML-DSA-87 for Level 5", 
                MLDSAAlgorithmType.ML_DSA_87, keys.getMldsaAlgorithm());
        assertEquals("ML-KEM should be ML-KEM-1024 for Level 5", 
                MLKEMAlgorithmType.ML_KEM_1024, keys.getMlkemAlgorithm());

        // Verify key sizes for Level 5
        assertTrue("ML-DSA-87 public key should be ~2592 bytes",
                keys.getMldsaPublicKeyBytes().length > 2400 &&
                keys.getMldsaPublicKeyBytes().length < 2800);
        assertTrue("ML-KEM-1024 public key should be ~1568 bytes",
                keys.getMlkemPublicKeyBytes().length > 1400 &&
                keys.getMlkemPublicKeyBytes().length < 1700);
    }

    @Test
    public void testGenerateKeys_WithSpecificAlgorithms() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        LTPAHybridKeys keys = LTPAHybridKeyGenerator.generateKeys(
                MLDSAAlgorithmType.ML_DSA_87, 
                MLKEMAlgorithmType.ML_KEM_1024);

        assertNotNull("Hybrid keys should not be null", keys);
        assertEquals("ML-DSA should be ML-DSA-87", 
                MLDSAAlgorithmType.ML_DSA_87, keys.getMldsaAlgorithm());
        assertEquals("ML-KEM should be ML-KEM-1024", 
                MLKEMAlgorithmType.ML_KEM_1024, keys.getMlkemAlgorithm());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateKeys_InvalidSecurityLevel() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        LTPAHybridKeyGenerator.generateKeys(99); // Invalid level
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateKeys_IncompatibleAlgorithms() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        // ML-DSA-44 (Level 1) with ML-KEM-1024 (Level 5) - incompatible
        LTPAHybridKeyGenerator.generateKeys(
                MLDSAAlgorithmType.ML_DSA_44, 
                MLKEMAlgorithmType.ML_KEM_1024);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateKeys_NullMLDSAAlgorithm() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        LTPAHybridKeyGenerator.generateKeys(null, MLKEMAlgorithmType.ML_KEM_768);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateKeys_NullMLKEMAlgorithm() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        LTPAHybridKeyGenerator.generateKeys(MLDSAAlgorithmType.ML_DSA_65, null);
    }

    @Test
    public void testGenerateKeys_MultipleInvocations() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        // Generate multiple key sets and verify they're different
        LTPAHybridKeys keys1 = LTPAHybridKeyGenerator.generateKeys();
        LTPAHybridKeys keys2 = LTPAHybridKeyGenerator.generateKeys();

        assertNotNull("First key set should not be null", keys1);
        assertNotNull("Second key set should not be null", keys2);

        // Verify RSA keys are different
        assertFalse("RSA private keys should be different",
                keys1.getRsaPrivateKey().equals(keys2.getRsaPrivateKey()));

        // Verify ML-DSA keys are different
        assertFalse("ML-DSA public keys should be different",
                java.util.Arrays.equals(keys1.getMldsaPublicKeyBytes(), 
                                       keys2.getMldsaPublicKeyBytes()));

        // Verify ML-KEM keys are different
        assertFalse("ML-KEM public keys should be different",
                java.util.Arrays.equals(keys1.getMlkemPublicKeyBytes(), 
                                       keys2.getMlkemPublicKeyBytes()));
    }

    @Test
    public void testGenerateKeys_AllSecurityLevels() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        // Generate keys for all three security levels
        LTPAHybridKeys level1 = LTPAHybridKeyGenerator.generateKeys(1);
        LTPAHybridKeys level3 = LTPAHybridKeyGenerator.generateKeys(3);
        LTPAHybridKeys level5 = LTPAHybridKeyGenerator.generateKeys(5);

        // Verify all key sets are valid
        assertNotNull("Level 1 keys should not be null", level1);
        assertNotNull("Level 3 keys should not be null", level3);
        assertNotNull("Level 5 keys should not be null", level5);

        // Verify key sizes increase with security level
        int size1 = level1.getMldsaPublicKeyBytes().length;
        int size3 = level3.getMldsaPublicKeyBytes().length;
        int size5 = level5.getMldsaPublicKeyBytes().length;

        assertTrue("Level 3 ML-DSA keys should be larger than Level 1", size3 > size1);
        assertTrue("Level 5 ML-DSA keys should be larger than Level 3", size5 > size3);

        // Verify ML-KEM key sizes also increase
        int kemSize1 = level1.getMlkemPublicKeyBytes().length;
        int kemSize3 = level3.getMlkemPublicKeyBytes().length;
        int kemSize5 = level5.getMlkemPublicKeyBytes().length;

        assertTrue("Level 3 ML-KEM keys should be larger than Level 1", kemSize3 > kemSize1);
        assertTrue("Level 5 ML-KEM keys should be larger than Level 3", kemSize5 > kemSize3);
    }

    @Test
    public void testGenerateKeys_VerifyRSAKeySize() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        LTPAHybridKeys keys = LTPAHybridKeyGenerator.generateKeys();

        // RSA-2048 private key in PKCS#8 format should be ~1200 bytes
        byte[] rsaEncoded = keys.getRsaPrivateKey().getEncoded();
        assertTrue("RSA-2048 private key should be ~1200 bytes",
                rsaEncoded.length > 1100 && rsaEncoded.length < 1300);

        // RSA-2048 public key in X.509 format should be ~294 bytes
        byte[] rsaPubEncoded = keys.getRsaPublicKey().getEncoded();
        assertTrue("RSA-2048 public key should be ~294 bytes",
                rsaPubEncoded.length > 250 && rsaPubEncoded.length < 350);
    }

    @Test
    public void testGenerateKeys_VerifyAlgorithmConsistency() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        // Test all valid combinations
        testAlgorithmCombination(MLDSAAlgorithmType.ML_DSA_44, MLKEMAlgorithmType.ML_KEM_512);
        testAlgorithmCombination(MLDSAAlgorithmType.ML_DSA_65, MLKEMAlgorithmType.ML_KEM_768);
        testAlgorithmCombination(MLDSAAlgorithmType.ML_DSA_87, MLKEMAlgorithmType.ML_KEM_1024);
    }

    private void testAlgorithmCombination(MLDSAAlgorithmType mldsa, MLKEMAlgorithmType mlkem) 
            throws Exception {
        LTPAHybridKeys keys = LTPAHybridKeyGenerator.generateKeys(mldsa, mlkem);
        
        assertEquals("ML-DSA algorithm should match", mldsa, keys.getMldsaAlgorithm());
        assertEquals("ML-KEM algorithm should match", mlkem, keys.getMlkemAlgorithm());
        assertEquals("Security levels should match", 
                mldsa.getSecurityLevel(), mlkem.getSecurityLevel());
    }
}

// Made with Bob
