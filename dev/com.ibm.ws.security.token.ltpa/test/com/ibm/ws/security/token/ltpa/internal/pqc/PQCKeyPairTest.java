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
package com.ibm.ws.security.token.ltpa.internal.pqc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for PQCKeyPair class.
 * 
 * <p>This test suite provides comprehensive coverage of the PQCKeyPair immutable
 * data class, including construction, validation, immutability, equals/hashCode
 * contract, and toString() method.</p>
 * 
 * <h3>Test Coverage Areas:</h3>
 * <ul>
 *   <li>Constructor with valid keys</li>
 *   <li>Constructor validation (null checks)</li>
 *   <li>Getter methods</li>
 *   <li>Immutability guarantees</li>
 *   <li>equals() and hashCode() contract</li>
 *   <li>toString() for debugging</li>
 * </ul>
 */
public class PQCKeyPairTest {
    
    // ========================================================================
    // Test Fixtures
    // ========================================================================
    
    private static PublicKey rsaPublicKey;
    private static PrivateKey rsaPrivateKey;
    private static PublicKey rsaPublicKey2;
    private static PrivateKey rsaPrivateKey2;
    
    private PQCKeyPair keyPair;
    
    // ========================================================================
    // Test Setup
    // ========================================================================
    
    /**
     * One-time setup: Generate RSA key pairs for testing.
     * We use RSA keys as mock PQC keys since actual PQC key generation
     * requires provider setup.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Generate first RSA key pair (2048-bit)
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(2048, new SecureRandom());
        KeyPair rsaKeyPair1 = rsaGen.generateKeyPair();
        rsaPublicKey = rsaKeyPair1.getPublic();
        rsaPrivateKey = rsaKeyPair1.getPrivate();
        
        // Generate second RSA key pair for comparison tests
        KeyPair rsaKeyPair2 = rsaGen.generateKeyPair();
        rsaPublicKey2 = rsaKeyPair2.getPublic();
        rsaPrivateKey2 = rsaKeyPair2.getPrivate();
    }
    
    /**
     * Setup before each test.
     */
    @Before
    public void setUp() throws Exception {
        keyPair = null;
    }
    
    /**
     * Cleanup after each test.
     */
    @After
    public void tearDown() throws Exception {
        keyPair = null;
    }
    
    // ========================================================================
    // Constructor Tests - Valid Keys
    // ========================================================================
    
    /**
     * Test constructor with valid keys creates PQCKeyPair successfully.
     */
    @Test
    public void test_constructor_ValidKeys() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        assertNotNull("PQCKeyPair should not be null", keyPair);
        assertEquals("Public key should match", rsaPublicKey, keyPair.getPublicKey());
        assertEquals("Private key should match", rsaPrivateKey, keyPair.getPrivateKey());
        assertEquals("Algorithm should match", PQCAlgorithm.ML_DSA_65, keyPair.getAlgorithm());
    }
    
    /**
     * Test constructor with ML-DSA-87 algorithm.
     */
    @Test
    public void test_constructor_ML_DSA_87() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_87);
        
        assertNotNull("PQCKeyPair should not be null", keyPair);
        assertEquals("Algorithm should be ML-DSA-87", PQCAlgorithm.ML_DSA_87, keyPair.getAlgorithm());
    }
    
    // ========================================================================
    // Constructor Tests - Null Validation
    // ========================================================================
    
    /**
     * Test constructor with null public key throws IllegalArgumentException.
     */
    @Test
    public void test_constructor_NullPublicKey_ThrowsException() {
        try {
            new PQCKeyPair(null, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
            fail("Constructor should throw IllegalArgumentException for null public key");
        } catch (IllegalArgumentException e) {
            assertEquals("Exception message should indicate null public key",
                         "Public key cannot be null", e.getMessage());
        }
    }
    
    /**
     * Test constructor with null private key throws IllegalArgumentException.
     */
    @Test
    public void test_constructor_NullPrivateKey_ThrowsException() {
        try {
            new PQCKeyPair(rsaPublicKey, null, PQCAlgorithm.ML_DSA_65);
            fail("Constructor should throw IllegalArgumentException for null private key");
        } catch (IllegalArgumentException e) {
            assertEquals("Exception message should indicate null private key",
                         "Private key cannot be null", e.getMessage());
        }
    }
    
    /**
     * Test constructor with null algorithm throws IllegalArgumentException.
     */
    @Test
    public void test_constructor_NullAlgorithm_ThrowsException() {
        try {
            new PQCKeyPair(rsaPublicKey, rsaPrivateKey, null);
            fail("Constructor should throw IllegalArgumentException for null algorithm");
        } catch (IllegalArgumentException e) {
            assertEquals("Exception message should indicate null algorithm",
                         "Algorithm cannot be null", e.getMessage());
        }
    }
    
    /**
     * Test constructor with all null parameters throws IllegalArgumentException.
     */
    @Test
    public void test_constructor_AllNull_ThrowsException() {
        try {
            new PQCKeyPair(null, null, null);
            fail("Constructor should throw IllegalArgumentException for all null parameters");
        } catch (IllegalArgumentException e) {
            // Should fail on first null check (public key)
            assertEquals("Exception message should indicate null public key",
                         "Public key cannot be null", e.getMessage());
        }
    }
    
    // ========================================================================
    // Getter Tests
    // ========================================================================
    
    /**
     * Test getPublicKey returns the correct public key.
     */
    @Test
    public void test_getPublicKey() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        PublicKey retrievedKey = keyPair.getPublicKey();
        assertNotNull("Public key should not be null", retrievedKey);
        assertEquals("Public key should match constructor parameter", rsaPublicKey, retrievedKey);
    }
    
    /**
     * Test getPrivateKey returns the correct private key.
     */
    @Test
    public void test_getPrivateKey() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        PrivateKey retrievedKey = keyPair.getPrivateKey();
        assertNotNull("Private key should not be null", retrievedKey);
        assertEquals("Private key should match constructor parameter", rsaPrivateKey, retrievedKey);
    }
    
    /**
     * Test getAlgorithm returns the correct algorithm.
     */
    @Test
    public void test_getAlgorithm() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        PQCAlgorithm algorithm = keyPair.getAlgorithm();
        assertNotNull("Algorithm should not be null", algorithm);
        assertEquals("Algorithm should match constructor parameter", PQCAlgorithm.ML_DSA_65, algorithm);
    }
    
    // ========================================================================
    // Immutability Tests
    // ========================================================================
    
    /**
     * Test that getters return the same instances (no defensive copies).
     * PQCKeyPair stores references, not copies, for performance.
     */
    @Test
    public void test_immutability_GettersReturnSameInstances() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        PublicKey publicKey1 = keyPair.getPublicKey();
        PublicKey publicKey2 = keyPair.getPublicKey();
        assertTrue("getPublicKey should return same instance", publicKey1 == publicKey2);
        
        PrivateKey privateKey1 = keyPair.getPrivateKey();
        PrivateKey privateKey2 = keyPair.getPrivateKey();
        assertTrue("getPrivateKey should return same instance", privateKey1 == privateKey2);
        
        PQCAlgorithm algorithm1 = keyPair.getAlgorithm();
        PQCAlgorithm algorithm2 = keyPair.getAlgorithm();
        assertTrue("getAlgorithm should return same instance", algorithm1 == algorithm2);
    }
    
    // ========================================================================
    // equals() Tests
    // ========================================================================
    
    /**
     * Test equals with same instance returns true.
     */
    @Test
    public void test_equals_SameInstance() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        assertTrue("equals should return true for same instance", keyPair.equals(keyPair));
    }
    
    /**
     * Test equals with identical key pair returns true.
     */
    @Test
    public void test_equals_IdenticalKeyPair() {
        PQCKeyPair keyPair1 = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        PQCKeyPair keyPair2 = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        assertTrue("equals should return true for identical key pairs", keyPair1.equals(keyPair2));
        assertTrue("equals should be symmetric", keyPair2.equals(keyPair1));
    }
    
    /**
     * Test equals with different public key returns false.
     */
    @Test
    public void test_equals_DifferentPublicKey() {
        PQCKeyPair keyPair1 = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        PQCKeyPair keyPair2 = new PQCKeyPair(rsaPublicKey2, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        assertFalse("equals should return false for different public keys", keyPair1.equals(keyPair2));
    }
    
    /**
     * Test equals with different private key returns false.
     */
    @Test
    public void test_equals_DifferentPrivateKey() {
        PQCKeyPair keyPair1 = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        PQCKeyPair keyPair2 = new PQCKeyPair(rsaPublicKey, rsaPrivateKey2, PQCAlgorithm.ML_DSA_65);
        
        assertFalse("equals should return false for different private keys", keyPair1.equals(keyPair2));
    }
    
    /**
     * Test equals with different algorithm returns false.
     */
    @Test
    public void test_equals_DifferentAlgorithm() {
        PQCKeyPair keyPair1 = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        PQCKeyPair keyPair2 = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_87);
        
        assertFalse("equals should return false for different algorithms", keyPair1.equals(keyPair2));
    }
    
    /**
     * Test equals with null returns false.
     */
    @Test
    public void test_equals_Null() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        assertFalse("equals should return false for null", keyPair.equals(null));
    }
    
    /**
     * Test equals with different class returns false.
     */
    @Test
    public void test_equals_DifferentClass() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        assertFalse("equals should return false for different class", keyPair.equals("not a PQCKeyPair"));
    }
    
    // ========================================================================
    // hashCode() Tests
    // ========================================================================
    
    /**
     * Test hashCode is consistent across multiple calls.
     */
    @Test
    public void test_hashCode_Consistent() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        int hashCode1 = keyPair.hashCode();
        int hashCode2 = keyPair.hashCode();
        
        assertEquals("hashCode should be consistent", hashCode1, hashCode2);
    }
    
    /**
     * Test hashCode is equal for equal objects.
     */
    @Test
    public void test_hashCode_EqualObjects() {
        PQCKeyPair keyPair1 = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        PQCKeyPair keyPair2 = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        assertEquals("hashCode should be equal for equal objects", 
                     keyPair1.hashCode(), keyPair2.hashCode());
    }
    
    /**
     * Test hashCode is different for different objects (best effort).
     */
    @Test
    public void test_hashCode_DifferentObjects() {
        PQCKeyPair keyPair1 = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        PQCKeyPair keyPair2 = new PQCKeyPair(rsaPublicKey2, rsaPrivateKey2, PQCAlgorithm.ML_DSA_87);
        
        // Note: Different objects may have same hash code (collision), but it's unlikely
        assertNotEquals("hashCode should likely be different for different objects",
                        keyPair1.hashCode(), keyPair2.hashCode());
    }
    
    // ========================================================================
    // toString() Tests
    // ========================================================================
    
    /**
     * Test toString returns non-null string.
     */
    @Test
    public void test_toString_NotNull() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        String result = keyPair.toString();
        assertNotNull("toString should not return null", result);
    }
    
    /**
     * Test toString contains algorithm name.
     */
    @Test
    public void test_toString_ContainsAlgorithm() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        String result = keyPair.toString();
        assertTrue("toString should contain algorithm name",
                   result.contains("ML-DSA-65"));
    }
    
    /**
     * Test toString contains key information.
     */
    @Test
    public void test_toString_ContainsKeyInfo() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        String result = keyPair.toString();
        assertTrue("toString should contain 'Public Key'", result.contains("Public Key"));
        assertTrue("toString should contain 'Private Key'", result.contains("Private Key"));
    }
    
    /**
     * Test toString does not contain sensitive key material.
     */
    @Test
    public void test_toString_NoSensitiveData() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        String result = keyPair.toString();
        // Verify that encoded key bytes are not in the string
        byte[] publicKeyBytes = rsaPublicKey.getEncoded();
        byte[] privateKeyBytes = rsaPrivateKey.getEncoded();
        
        // Convert first few bytes to string to check they're not present
        String publicKeyHex = String.format("%02x%02x%02x", 
                                           publicKeyBytes[0], publicKeyBytes[1], publicKeyBytes[2]);
        String privateKeyHex = String.format("%02x%02x%02x",
                                            privateKeyBytes[0], privateKeyBytes[1], privateKeyBytes[2]);
        
        assertFalse("toString should not contain public key bytes", result.contains(publicKeyHex));
        assertFalse("toString should not contain private key bytes", result.contains(privateKeyHex));
    }
    
    /**
     * Test toString format matches expected pattern.
     */
    @Test
    public void test_toString_Format() {
        keyPair = new PQCKeyPair(rsaPublicKey, rsaPrivateKey, PQCAlgorithm.ML_DSA_65);
        
        String result = keyPair.toString();
        assertTrue("toString should start with 'PQCKeyPair['", result.startsWith("PQCKeyPair["));
        assertTrue("toString should contain 'algorithm='", result.contains("algorithm="));
        assertTrue("toString should contain 'publicKey='", result.contains("publicKey="));
        assertTrue("toString should contain 'privateKey='", result.contains("privateKey="));
    }
}

// Made with Bob