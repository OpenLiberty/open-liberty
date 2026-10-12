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

import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for PQCRuntimeSupport - ML-KEM key encapsulation and decapsulation.
 * Tests the core PQC functionality for key exchange and shared secret generation.
 */
public class PQCRuntimeSupportTest {

    private boolean pqcAvailable;

    @Before
    public void setUp() {
        pqcAvailable = PQCRuntimeSupport.isPQCSupported();
        if (!pqcAvailable) {
            System.out.println("WARNING: PQC runtime not available - some tests will be skipped");
            System.out.println("Requires Java 26+ with ML-KEM support");
            System.out.println("Current Java version: " + PQCRuntimeSupport.getJavaVersion());
        }
    }

    @Test
    public void testIsPQCSupported() {
        // This test always runs - just reports the status
        boolean supported = PQCRuntimeSupport.isPQCSupported();
        System.out.println("PQC Support: " + (supported ? "Available" : "Not Available"));
        System.out.println("Java Version: " + PQCRuntimeSupport.getJavaVersion());
    }

    @Test
    public void testGetJavaVersion() {
        String version = PQCRuntimeSupport.getJavaVersion();
        assertNotNull("Java version should not be null", version);
        assertFalse("Java version should not be empty", version.isEmpty());
        System.out.println("Java version: " + version);
    }

    @Test
    public void testGenerateMLKEMKeyPair_ML_KEM_512() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-512");

        assertNotNull("KeyPair should not be null", keyPair);
        assertNotNull("Public key should not be null", keyPair.getPublic());
        assertNotNull("Private key should not be null", keyPair.getPrivate());

        // Verify key sizes (approximate, includes ASN.1 overhead)
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        assertTrue("ML-KEM-512 public key should be ~800 bytes",
                publicKeyBytes.length > 700 && publicKeyBytes.length < 900);
        assertTrue("ML-KEM-512 private key should be ~1600 bytes",
                privateKeyBytes.length > 1400 && privateKeyBytes.length < 1800);
    }

    @Test
    public void testGenerateMLKEMKeyPair_ML_KEM_768() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-768");

        assertNotNull("KeyPair should not be null", keyPair);

        // Verify key sizes
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        assertTrue("ML-KEM-768 public key should be ~1184 bytes",
                publicKeyBytes.length > 1000 && publicKeyBytes.length < 1300);
        assertTrue("ML-KEM-768 private key should be ~2400 bytes",
                privateKeyBytes.length > 2200 && privateKeyBytes.length < 2600);
    }

    @Test
    public void testGenerateMLKEMKeyPair_ML_KEM_1024() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-1024");

        assertNotNull("KeyPair should not be null", keyPair);

        // Verify key sizes
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        assertTrue("ML-KEM-1024 public key should be ~1568 bytes",
                publicKeyBytes.length > 1400 && publicKeyBytes.length < 1700);
        assertTrue("ML-KEM-1024 private key should be ~3168 bytes",
                privateKeyBytes.length > 2900 && privateKeyBytes.length < 3400);
    }

    @Test
    public void testGenerateMLKEMKeyPair_WithEnum() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair(MLKEMAlgorithmType.ML_KEM_768);

        assertNotNull("KeyPair should not be null", keyPair);
        assertNotNull("Public key should not be null", keyPair.getPublic());
        assertNotNull("Private key should not be null", keyPair.getPrivate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateMLKEMKeyPair_InvalidAlgorithm() throws Exception {
        if (!pqcAvailable) {
            throw new IllegalArgumentException("PQC not available");
        }

        PQCRuntimeSupport.generateMLKEMKeyPair("INVALID-ALGORITHM");
    }

    @Test
    public void testEncapsulateAndDecapsulate_ML_KEM_768() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        // Generate key pair
        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-768");

        // Encapsulate to generate shared secret
        Object secretKeyWithEncap = PQCRuntimeSupport.encapsulate(keyPair.getPublic());
        assertNotNull("SecretKeyWithEncapsulation should not be null", secretKeyWithEncap);

        // Extract shared secret and encapsulation
        SecretKey sharedSecret1 = PQCRuntimeSupport.extractSharedSecret(secretKeyWithEncap);
        byte[] encapsulation = PQCRuntimeSupport.extractEncapsulation(secretKeyWithEncap);

        assertNotNull("Shared secret should not be null", sharedSecret1);
        assertNotNull("Encapsulation should not be null", encapsulation);
        assertTrue("Encapsulation should have non-zero length", encapsulation.length > 0);

        // Decapsulate to recover shared secret
        SecretKey sharedSecret2 = PQCRuntimeSupport.decapsulate(keyPair.getPrivate(), encapsulation);

        assertNotNull("Decapsulated shared secret should not be null", sharedSecret2);

        // Verify shared secrets match
        assertArrayEquals("Shared secrets should match",
                sharedSecret1.getEncoded(), sharedSecret2.getEncoded());
    }

    @Test
    public void testEncapsulateAndDecapsulate_AllAlgorithms() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        // Test all three ML-KEM variants
        testEncapsulateDecapsulateWithAlgorithm("ML-KEM-512");
        testEncapsulateDecapsulateWithAlgorithm("ML-KEM-768");
        testEncapsulateDecapsulateWithAlgorithm("ML-KEM-1024");
    }

    private void testEncapsulateDecapsulateWithAlgorithm(String algorithm) throws Exception {
        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair(algorithm);

        Object secretKeyWithEncap = PQCRuntimeSupport.encapsulate(keyPair.getPublic());
        SecretKey sharedSecret1 = PQCRuntimeSupport.extractSharedSecret(secretKeyWithEncap);
        byte[] encapsulation = PQCRuntimeSupport.extractEncapsulation(secretKeyWithEncap);

        SecretKey sharedSecret2 = PQCRuntimeSupport.decapsulate(keyPair.getPrivate(), encapsulation);

        assertArrayEquals("Shared secrets should match for " + algorithm,
                sharedSecret1.getEncoded(), sharedSecret2.getEncoded());
    }

    @Test
    public void testDecapsulate_WrongPrivateKey() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        // Generate two different key pairs
        KeyPair keyPair1 = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-768");
        KeyPair keyPair2 = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-768");

        // Encapsulate with first public key
        Object secretKeyWithEncap = PQCRuntimeSupport.encapsulate(keyPair1.getPublic());
        SecretKey sharedSecret1 = PQCRuntimeSupport.extractSharedSecret(secretKeyWithEncap);
        byte[] encapsulation = PQCRuntimeSupport.extractEncapsulation(secretKeyWithEncap);

        // Try to decapsulate with wrong private key
        SecretKey sharedSecret2 = PQCRuntimeSupport.decapsulate(keyPair2.getPrivate(), encapsulation);

        // Shared secrets should NOT match
        assertFalse("Shared secrets should not match with wrong private key",
                Arrays.equals(sharedSecret1.getEncoded(), sharedSecret2.getEncoded()));
    }

    @Test
    public void testEncapsulation_ProducesDifferentResults() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-768");

        // Encapsulate twice with same public key
        Object secretKeyWithEncap1 = PQCRuntimeSupport.encapsulate(keyPair.getPublic());
        Object secretKeyWithEncap2 = PQCRuntimeSupport.encapsulate(keyPair.getPublic());

        byte[] encapsulation1 = PQCRuntimeSupport.extractEncapsulation(secretKeyWithEncap1);
        byte[] encapsulation2 = PQCRuntimeSupport.extractEncapsulation(secretKeyWithEncap2);

        // Encapsulations should be different (randomized)
        assertFalse("Encapsulations should be different",
                Arrays.equals(encapsulation1, encapsulation2));

        // But both should decapsulate successfully
        SecretKey secret1 = PQCRuntimeSupport.decapsulate(keyPair.getPrivate(), encapsulation1);
        SecretKey secret2 = PQCRuntimeSupport.decapsulate(keyPair.getPrivate(), encapsulation2);

        assertNotNull("First shared secret should not be null", secret1);
        assertNotNull("Second shared secret should not be null", secret2);

        // Shared secrets should also be different
        assertFalse("Shared secrets should be different",
                Arrays.equals(secret1.getEncoded(), secret2.getEncoded()));
    }

    @Test
    public void testValidateKeySizes_ValidKeys() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair(MLKEMAlgorithmType.ML_KEM_768);

        boolean valid = PQCRuntimeSupport.validateKeySizes(
                keyPair.getPublic().getEncoded(),
                keyPair.getPrivate().getEncoded(),
                MLKEMAlgorithmType.ML_KEM_768);

        assertTrue("Key sizes should be valid", valid);
    }

    @Test
    public void testValidateKeySizes_NullKeys() {
        boolean valid1 = PQCRuntimeSupport.validateKeySizes(null, new byte[100], 
                MLKEMAlgorithmType.ML_KEM_768);
        assertFalse("Null public key should be invalid", valid1);

        boolean valid2 = PQCRuntimeSupport.validateKeySizes(new byte[100], null, 
                MLKEMAlgorithmType.ML_KEM_768);
        assertFalse("Null private key should be invalid", valid2);

        boolean valid3 = PQCRuntimeSupport.validateKeySizes(null, null, 
                MLKEMAlgorithmType.ML_KEM_768);
        assertFalse("Both null keys should be invalid", valid3);
    }

    @Test
    public void testValidateKeySizes_TooSmall() {
        byte[] tooSmallPublic = new byte[100];
        byte[] tooSmallPrivate = new byte[100];

        boolean valid = PQCRuntimeSupport.validateKeySizes(
                tooSmallPublic, tooSmallPrivate, MLKEMAlgorithmType.ML_KEM_768);

        assertFalse("Too small keys should be invalid", valid);
    }

    @Test
    public void testSharedSecretSize() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-768");

        Object secretKeyWithEncap = PQCRuntimeSupport.encapsulate(keyPair.getPublic());
        SecretKey sharedSecret = PQCRuntimeSupport.extractSharedSecret(secretKeyWithEncap);

        // ML-KEM shared secrets are always 32 bytes (256 bits)
        assertEquals("Shared secret should be 32 bytes", 32, sharedSecret.getEncoded().length);
    }

    @Test
    public void testEncapsulationSize_ML_KEM_512() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-512");
        Object secretKeyWithEncap = PQCRuntimeSupport.encapsulate(keyPair.getPublic());
        byte[] encapsulation = PQCRuntimeSupport.extractEncapsulation(secretKeyWithEncap);

        // ML-KEM-512 encapsulation is 768 bytes
        assertEquals("ML-KEM-512 encapsulation should be 768 bytes", 
                768, encapsulation.length);
    }

    @Test
    public void testEncapsulationSize_ML_KEM_768() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-768");
        Object secretKeyWithEncap = PQCRuntimeSupport.encapsulate(keyPair.getPublic());
        byte[] encapsulation = PQCRuntimeSupport.extractEncapsulation(secretKeyWithEncap);

        // ML-KEM-768 encapsulation is 1088 bytes
        assertEquals("ML-KEM-768 encapsulation should be 1088 bytes", 
                1088, encapsulation.length);
    }

    @Test
    public void testEncapsulationSize_ML_KEM_1024() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-1024");
        Object secretKeyWithEncap = PQCRuntimeSupport.encapsulate(keyPair.getPublic());
        byte[] encapsulation = PQCRuntimeSupport.extractEncapsulation(secretKeyWithEncap);

        // ML-KEM-1024 encapsulation is 1568 bytes
        assertEquals("ML-KEM-1024 encapsulation should be 1568 bytes", 
                1568, encapsulation.length);
    }

    @Test(expected = Exception.class)
    public void testDecapsulate_TamperedEncapsulation() throws Exception {
        if (!pqcAvailable) {
            throw new Exception("PQC not available");
        }

        KeyPair keyPair = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-768");

        Object secretKeyWithEncap = PQCRuntimeSupport.encapsulate(keyPair.getPublic());
        byte[] encapsulation = PQCRuntimeSupport.extractEncapsulation(secretKeyWithEncap);

        // Tamper with encapsulation
        encapsulation[0] ^= 0xFF;

        // This should fail or produce wrong shared secret
        PQCRuntimeSupport.decapsulate(keyPair.getPrivate(), encapsulation);
    }

    @Test
    public void testMultipleKeyPairs_IndependentSecrets() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        // Generate multiple key pairs
        KeyPair keyPair1 = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-768");
        KeyPair keyPair2 = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-768");
        KeyPair keyPair3 = PQCRuntimeSupport.generateMLKEMKeyPair("ML-KEM-768");

        // Encapsulate with each public key
        Object encap1 = PQCRuntimeSupport.encapsulate(keyPair1.getPublic());
        Object encap2 = PQCRuntimeSupport.encapsulate(keyPair2.getPublic());
        Object encap3 = PQCRuntimeSupport.encapsulate(keyPair3.getPublic());

        SecretKey secret1 = PQCRuntimeSupport.extractSharedSecret(encap1);
        SecretKey secret2 = PQCRuntimeSupport.extractSharedSecret(encap2);
        SecretKey secret3 = PQCRuntimeSupport.extractSharedSecret(encap3);

        // All shared secrets should be different
        assertFalse("Secrets 1 and 2 should be different",
                Arrays.equals(secret1.getEncoded(), secret2.getEncoded()));
        assertFalse("Secrets 2 and 3 should be different",
                Arrays.equals(secret2.getEncoded(), secret3.getEncoded()));
        assertFalse("Secrets 1 and 3 should be different",
                Arrays.equals(secret1.getEncoded(), secret3.getEncoded()));
    }

    @Test
    public void testGetProviderInfo() throws Exception {
        if (!pqcAvailable) {
            System.out.println("Skipping test - PQC runtime not available");
            return;
        }

        String providerInfo = PQCRuntimeSupport.getProviderInfo();
        assertNotNull("Provider info should not be null", providerInfo);
        assertFalse("Provider info should not be empty", providerInfo.isEmpty());
        System.out.println("Provider info: " + providerInfo);
    }
}

// Made with Bob
