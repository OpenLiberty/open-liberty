/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.security.token.ltpa.internal.pqc.bc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.token.ltpa.internal.pqc.PQCAlgorithm;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCException;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCKeyPair;

/**
 * Unit tests for BouncyCastlePQCProvider.
 * 
 * <p>This test suite provides comprehensive coverage of the BouncyCastle-based
 * PQC provider implementation, including provider availability, key generation,
 * signature operations, and error handling.</p>
 * 
 * <h3>Test Coverage Areas:</h3>
 * <ul>
 *   <li>Provider availability detection</li>
 *   <li>Provider information methods</li>
 *   <li>Algorithm support queries</li>
 *   <li>Key pair generation (ML-DSA-65, ML-DSA-87)</li>
 *   <li>Signature generation and verification</li>
 *   <li>Key encoding/decoding (X.509, PKCS#8)</li>
 *   <li>FIPS compliance reporting</li>
 *   <li>Error handling (unsupported algorithms, invalid keys)</li>
 * </ul>
 * 
 * <h3>Provider Availability:</h3>
 * <p>BouncyCastle may not be available in all test environments. Tests that
 * require BouncyCastle will be skipped if the provider is not available.</p>
 */
public class BouncyCastlePQCProviderTest {
    
    private static boolean providerAvailable;
    private BouncyCastlePQCProvider provider;
    
    // ========================================================================
    // Test Setup
    // ========================================================================
    
    /**
     * One-time setup to check if BouncyCastle provider is available.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        try {
            BouncyCastlePQCProvider testProvider = new BouncyCastlePQCProvider();
            providerAvailable = testProvider.isAvailable();
            System.out.println("BouncyCastlePQCProvider availability: " + providerAvailable);
        } catch (Exception e) {
            providerAvailable = false;
            System.out.println("BouncyCastlePQCProvider not available: " + e.getMessage());
        }
    }
    
    /**
     * Set up test fixture before each test.
     */
    @Before
    public void setUp() {
        provider = new BouncyCastlePQCProvider();
    }
    
    // ========================================================================
    // Provider Information Tests
    // ========================================================================
    
    /**
     * Test that provider name is correct.
     */
    @Test
    public void test_getProviderName() {
        String name = provider.getProviderName();
        assertNotNull("Provider name should not be null", name);
        assertEquals("Provider name should be 'BCPQC'", "BCPQC", name);
    }
    
    /**
     * Test that provider version is correct.
     */
    @Test
    public void test_getProviderVersion() {
        String version = provider.getProviderVersion();
        assertNotNull("Provider version should not be null", version);
        assertEquals("Provider version should be '1.78'", "1.78", version);
    }
    
    /**
     * Test that isAvailable returns consistent result.
     */
    @Test
    public void test_isAvailable() {
        boolean available = provider.isAvailable();
        assertEquals("isAvailable should match class-level detection", 
                     providerAvailable, available);
    }
    
    /**
     * Test that FIPS compliance is false for BouncyCastle.
     */
    @Test
    public void test_isFIPSCompliant_ReturnsFalse() {
        assertFalse("BouncyCastle PQC is not FIPS 140-3 compliant", 
                    provider.isFIPSCompliant());
    }
    
    // ========================================================================
    // Algorithm Support Tests
    // ========================================================================
    
    /**
     * Test that ML-DSA-65 is supported.
     */
    @Test
    public void test_supportsAlgorithm_ML_DSA_65() {
        assertTrue("Provider should support ML-DSA-65", 
                   provider.supportsAlgorithm(PQCAlgorithm.ML_DSA_65));
    }
    
    /**
     * Test that ML-DSA-87 is supported.
     */
    @Test
    public void test_supportsAlgorithm_ML_DSA_87() {
        assertTrue("Provider should support ML-DSA-87", 
                   provider.supportsAlgorithm(PQCAlgorithm.ML_DSA_87));
    }
    
    /**
     * Test that supportsAlgorithm throws exception for null algorithm.
     */
    @Test
    public void test_supportsAlgorithm_NullAlgorithm_ThrowsException() {
        try {
            provider.supportsAlgorithm(null);
            fail("supportsAlgorithm(null) should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Exception message should indicate null algorithm",
                         "Algorithm cannot be null", e.getMessage());
        }
    }
    
    /**
     * Test that getSupportedAlgorithms returns correct set.
     */
    @Test
    public void test_getSupportedAlgorithms() {
        Set<PQCAlgorithm> algorithms = provider.getSupportedAlgorithms();
        assertNotNull("Supported algorithms set should not be null", algorithms);
        assertEquals("Should support exactly 2 algorithms", 2, algorithms.size());
        assertTrue("Should contain ML-DSA-65", algorithms.contains(PQCAlgorithm.ML_DSA_65));
        assertTrue("Should contain ML-DSA-87", algorithms.contains(PQCAlgorithm.ML_DSA_87));
    }
    
    /**
     * Test that getSupportedAlgorithms returns immutable set.
     */
    @Test
    public void test_getSupportedAlgorithms_ReturnsImmutableSet() {
        Set<PQCAlgorithm> algorithms = provider.getSupportedAlgorithms();
        try {
            algorithms.add(PQCAlgorithm.ML_DSA_65);
            fail("Supported algorithms set should be immutable");
        } catch (UnsupportedOperationException e) {
            // Expected - set is immutable
        }
    }
    
    // ========================================================================
    // Key Generation Tests - ML-DSA-65
    // ========================================================================
    
    /**
     * Test key pair generation for ML-DSA-65.
     */
    @Test
    public void test_generateKeyPair_ML_DSA_65() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        
        assertNotNull("Key pair should not be null", keyPair);
        assertNotNull("Public key should not be null", keyPair.getPublicKey());
        assertNotNull("Private key should not be null", keyPair.getPrivateKey());
        assertEquals("Algorithm should be ML-DSA-65", 
                     PQCAlgorithm.ML_DSA_65, keyPair.getAlgorithm());
    }
    
    /**
     * Test that generated ML-DSA-65 keys have correct sizes.
     */
    @Test
    public void test_generateKeyPair_ML_DSA_65_KeySizes() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        
        byte[] publicKeyBytes = keyPair.getPublicKey().getEncoded();
        byte[] privateKeyBytes = keyPair.getPrivateKey().getEncoded();
        
        assertNotNull("Public key encoding should not be null", publicKeyBytes);
        assertNotNull("Private key encoding should not be null", privateKeyBytes);
        
        // Note: Encoded sizes include ASN.1 overhead, so they're larger than raw key sizes
        assertTrue("Public key should be at least 1952 bytes", 
                   publicKeyBytes.length >= 1952);
        assertTrue("Private key should be at least 4000 bytes", 
                   privateKeyBytes.length >= 4000);
    }
    
    // ========================================================================
    // Key Generation Tests - ML-DSA-87
    // ========================================================================
    
    /**
     * Test key pair generation for ML-DSA-87.
     */
    @Test
    public void test_generateKeyPair_ML_DSA_87() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_87);
        
        assertNotNull("Key pair should not be null", keyPair);
        assertNotNull("Public key should not be null", keyPair.getPublicKey());
        assertNotNull("Private key should not be null", keyPair.getPrivateKey());
        assertEquals("Algorithm should be ML-DSA-87", 
                     PQCAlgorithm.ML_DSA_87, keyPair.getAlgorithm());
    }
    
    /**
     * Test that generated ML-DSA-87 keys have correct sizes.
     */
    @Test
    public void test_generateKeyPair_ML_DSA_87_KeySizes() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_87);
        
        byte[] publicKeyBytes = keyPair.getPublicKey().getEncoded();
        byte[] privateKeyBytes = keyPair.getPrivateKey().getEncoded();
        
        assertNotNull("Public key encoding should not be null", publicKeyBytes);
        assertNotNull("Private key encoding should not be null", privateKeyBytes);
        
        // Note: Encoded sizes include ASN.1 overhead
        assertTrue("Public key should be at least 2592 bytes", 
                   publicKeyBytes.length >= 2592);
        assertTrue("Private key should be at least 4864 bytes", 
                   privateKeyBytes.length >= 4864);
    }
    
    /**
     * Test that generateKeyPair throws exception for null algorithm.
     */
    @Test
    public void test_generateKeyPair_NullAlgorithm_ThrowsException() {
        try {
            provider.generateKeyPair(null);
            fail("generateKeyPair(null) should throw PQCException");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention null algorithm",
                       e.getMessage().contains("Algorithm cannot be null"));
        }
    }
    
    /**
     * Test that generateKeyPair throws exception when provider unavailable.
     */
    @Test
    public void test_generateKeyPair_ProviderUnavailable_ThrowsException() {
        assumeTrue("Test only runs when provider is unavailable", !providerAvailable);
        
        try {
            provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
            fail("generateKeyPair should throw PQCException when provider unavailable");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention unavailable provider",
                       e.getMessage().contains("not available"));
        }
    }
    
    // ========================================================================
    // Signature Generation and Verification Tests
    // ========================================================================
    
    /**
     * Test signature generation and verification for ML-DSA-65.
     */
    @Test
    public void test_signAndVerify_ML_DSA_65() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        // Generate key pair
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        
        // Test data
        byte[] data = "Test data for ML-DSA-65 signature".getBytes("UTF-8");
        
        // Sign data
        byte[] signature = provider.sign(data, keyPair.getPrivateKey(), PQCAlgorithm.ML_DSA_65);
        
        assertNotNull("Signature should not be null", signature);
        assertTrue("Signature should be at least 3293 bytes", signature.length >= 3293);
        
        // Verify signature
        boolean valid = provider.verify(data, signature, keyPair.getPublicKey(), PQCAlgorithm.ML_DSA_65);
        assertTrue("Signature should be valid", valid);
    }
    
    /**
     * Test signature generation and verification for ML-DSA-87.
     */
    @Test
    public void test_signAndVerify_ML_DSA_87() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        // Generate key pair
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_87);
        
        // Test data
        byte[] data = "Test data for ML-DSA-87 signature".getBytes("UTF-8");
        
        // Sign data
        byte[] signature = provider.sign(data, keyPair.getPrivateKey(), PQCAlgorithm.ML_DSA_87);
        
        assertNotNull("Signature should not be null", signature);
        assertTrue("Signature should be at least 4595 bytes", signature.length >= 4595);
        
        // Verify signature
        boolean valid = provider.verify(data, signature, keyPair.getPublicKey(), PQCAlgorithm.ML_DSA_87);
        assertTrue("Signature should be valid", valid);
    }
    
    /**
     * Test that signature verification fails with wrong data.
     */
    @Test
    public void test_verify_WrongData_ReturnsFalse() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        
        byte[] originalData = "Original data".getBytes("UTF-8");
        byte[] modifiedData = "Modified data".getBytes("UTF-8");
        
        byte[] signature = provider.sign(originalData, keyPair.getPrivateKey(), PQCAlgorithm.ML_DSA_65);
        
        boolean valid = provider.verify(modifiedData, signature, keyPair.getPublicKey(), PQCAlgorithm.ML_DSA_65);
        assertFalse("Signature should not be valid for modified data", valid);
    }
    
    /**
     * Test that signature verification fails with wrong key.
     */
    @Test
    public void test_verify_WrongKey_ReturnsFalse() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair1 = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        PQCKeyPair keyPair2 = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        
        byte[] data = "Test data".getBytes("UTF-8");
        
        byte[] signature = provider.sign(data, keyPair1.getPrivateKey(), PQCAlgorithm.ML_DSA_65);
        
        boolean valid = provider.verify(data, signature, keyPair2.getPublicKey(), PQCAlgorithm.ML_DSA_65);
        assertFalse("Signature should not be valid with different key", valid);
    }
    
    /**
     * Test that signature verification fails with corrupted signature.
     */
    @Test
    public void test_verify_CorruptedSignature_ReturnsFalse() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        
        byte[] data = "Test data".getBytes("UTF-8");
        byte[] signature = provider.sign(data, keyPair.getPrivateKey(), PQCAlgorithm.ML_DSA_65);
        
        // Corrupt the signature
        signature[0] ^= 0xFF;
        
        boolean valid = provider.verify(data, signature, keyPair.getPublicKey(), PQCAlgorithm.ML_DSA_65);
        assertFalse("Corrupted signature should not be valid", valid);
    }
    
    // ========================================================================
    // Key Encoding/Decoding Tests
    // ========================================================================
    
    /**
     * Test encoding and decoding public key.
     */
    @Test
    public void test_encodeDecodePublicKey() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        PublicKey originalPublicKey = keyPair.getPublicKey();
        
        // Encode public key
        byte[] encodedKey = provider.encodePublicKey(originalPublicKey);
        assertNotNull("Encoded public key should not be null", encodedKey);
        
        // Decode public key
        PublicKey decodedPublicKey = provider.decodePublicKey(encodedKey, PQCAlgorithm.ML_DSA_65);
        assertNotNull("Decoded public key should not be null", decodedPublicKey);
        
        // Verify decoded key works for verification
        byte[] data = "Test data".getBytes("UTF-8");
        byte[] signature = provider.sign(data, keyPair.getPrivateKey(), PQCAlgorithm.ML_DSA_65);
        
        boolean valid = provider.verify(data, signature, decodedPublicKey, PQCAlgorithm.ML_DSA_65);
        assertTrue("Decoded public key should work for verification", valid);
    }
    
    /**
     * Test encoding and decoding private key.
     */
    @Test
    public void test_encodeDecodePrivateKey() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        PrivateKey originalPrivateKey = keyPair.getPrivateKey();
        
        // Encode private key
        byte[] encodedKey = provider.encodePrivateKey(originalPrivateKey);
        assertNotNull("Encoded private key should not be null", encodedKey);
        
        // Decode private key
        PrivateKey decodedPrivateKey = provider.decodePrivateKey(encodedKey, PQCAlgorithm.ML_DSA_65);
        assertNotNull("Decoded private key should not be null", decodedPrivateKey);
        
        // Verify decoded key works for signing
        byte[] data = "Test data".getBytes("UTF-8");
        byte[] signature = provider.sign(data, decodedPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        boolean valid = provider.verify(data, signature, keyPair.getPublicKey(), PQCAlgorithm.ML_DSA_65);
        assertTrue("Decoded private key should work for signing", valid);
    }
    
    /**
     * Test that encodePublicKey throws exception for null key.
     */
    @Test
    public void test_encodePublicKey_NullKey_ThrowsException() {
        try {
            provider.encodePublicKey(null);
            fail("encodePublicKey(null) should throw PQCException");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention null key",
                       e.getMessage().contains("Public key cannot be null"));
        }
    }
    
    /**
     * Test that encodePrivateKey throws exception for null key.
     */
    @Test
    public void test_encodePrivateKey_NullKey_ThrowsException() {
        try {
            provider.encodePrivateKey(null);
            fail("encodePrivateKey(null) should throw PQCException");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention null key",
                       e.getMessage().contains("Private key cannot be null"));
        }
    }
    
    /**
     * Test that decodePublicKey throws exception for null data.
     */
    @Test
    public void test_decodePublicKey_NullData_ThrowsException() {
        try {
            provider.decodePublicKey(null, PQCAlgorithm.ML_DSA_65);
            fail("decodePublicKey(null, ...) should throw PQCException");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention null data",
                       e.getMessage().contains("Encoded key data cannot be null"));
        }
    }
    
    /**
     * Test that decodePrivateKey throws exception for null data.
     */
    @Test
    public void test_decodePrivateKey_NullData_ThrowsException() {
        try {
            provider.decodePrivateKey(null, PQCAlgorithm.ML_DSA_65);
            fail("decodePrivateKey(null, ...) should throw PQCException");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention null data",
                       e.getMessage().contains("Encoded key data cannot be null"));
        }
    }
    
    /**
     * Test that decodePublicKey throws exception for invalid data.
     */
    @Test
    public void test_decodePublicKey_InvalidData_ThrowsException() {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        byte[] invalidData = "This is not a valid key encoding".getBytes();
        
        try {
            provider.decodePublicKey(invalidData, PQCAlgorithm.ML_DSA_65);
            fail("decodePublicKey with invalid data should throw PQCException");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }
    
    // ========================================================================
    // Error Handling Tests
    // ========================================================================
    
    /**
     * Test that sign throws exception for null data.
     */
    @Test
    public void test_sign_NullData_ThrowsException() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        
        try {
            provider.sign(null, keyPair.getPrivateKey(), PQCAlgorithm.ML_DSA_65);
            fail("sign(null, ...) should throw PQCException");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention null data",
                       e.getMessage().contains("Data cannot be null"));
        }
    }
    
    /**
     * Test that sign throws exception for null key.
     */
    @Test
    public void test_sign_NullKey_ThrowsException() {
        byte[] data = "Test data".getBytes();
        
        try {
            provider.sign(data, null, PQCAlgorithm.ML_DSA_65);
            fail("sign(..., null, ...) should throw PQCException");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention null key",
                       e.getMessage().contains("Private key cannot be null"));
        }
    }
    
    /**
     * Test that verify throws exception for null data.
     */
    @Test
    public void test_verify_NullData_ThrowsException() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        byte[] signature = new byte[3293];
        
        try {
            provider.verify(null, signature, keyPair.getPublicKey(), PQCAlgorithm.ML_DSA_65);
            fail("verify(null, ...) should throw PQCException");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention null data",
                       e.getMessage().contains("Data cannot be null"));
        }
    }
    
    /**
     * Test that verify throws exception for null signature.
     */
    @Test
    public void test_verify_NullSignature_ThrowsException() throws Exception {
        assumeTrue("BouncyCastle provider must be available", providerAvailable);
        
        PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        byte[] data = "Test data".getBytes();
        
        try {
            provider.verify(data, null, keyPair.getPublicKey(), PQCAlgorithm.ML_DSA_65);
            fail("verify(..., null, ...) should throw PQCException");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention null signature",
                       e.getMessage().contains("Signature cannot be null"));
        }
    }
    
    /**
     * Test that verify throws exception for null key.
     */
    @Test
    public void test_verify_NullKey_ThrowsException() {
        byte[] data = "Test data".getBytes();
        byte[] signature = new byte[3293];
        
        try {
            provider.verify(data, signature, null, PQCAlgorithm.ML_DSA_65);
            fail("verify(..., null) should throw PQCException");
        } catch (PQCException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention null key",
                       e.getMessage().contains("Public key cannot be null"));
        }
    }
}

// Made with Bob