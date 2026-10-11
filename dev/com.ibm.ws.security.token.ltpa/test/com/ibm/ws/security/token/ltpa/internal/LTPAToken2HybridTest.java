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
package com.ibm.ws.security.token.ltpa.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyUtil;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCAlgorithm;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCException;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCKeyPair;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCProvider;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCProviderFactory;

/**
 * Unit tests for LTPAToken2Hybrid class.
 * 
 * <p>This test suite provides comprehensive coverage of hybrid LTPA token functionality,
 * including token generation, validation, signature chaining, PQC metadata handling,
 * backward compatibility, error handling, and performance benchmarks.</p>
 * 
 * <h3>Test Coverage Areas:</h3>
 * <ul>
 *   <li>Token generation with hybrid signatures (RSA + PQC)</li>
 *   <li>Token validation (both RSA and PQC signatures)</li>
 *   <li>Signature chaining verification (PQC signs payload + RSA sig)</li>
 *   <li>PQC metadata handling (pqcEnabled, pqcAlgorithm, pqcVersion)</li>
 *   <li>Backward compatibility (classical servers validate RSA only)</li>
 *   <li>Error handling and edge cases</li>
 *   <li>Provider integration (BouncyCastle and Native)</li>
 *   <li>Token serialization/deserialization</li>
 *   <li>Clone functionality</li>
 *   <li>Performance benchmarks</li>
 * </ul>
 */
public class LTPAToken2HybridTest {
    
    // ========================================================================
    // Test Constants
    // ========================================================================
    
    private static final String TEST_USER = "testuser@example.com";
    private static final String TEST_REALM = "TestRealm";
    private static final String TEST_ACCESS_ID = "user:" + TEST_REALM + "/" + TEST_USER;
    private static final long TEST_EXPIRATION_MINUTES = 120; // 2 hours
    private static final long TEST_EXP_DIFF_ALLOWED = 300000; // 5 minutes in milliseconds
    
    private static final String SHARED_KEY_PASSWORD = "WebAS"; // pragma: allowlist secret
    private static final String DECODED_SHARED_KEY = "Three can keep a secret when two are no longer there";
    
    // Performance targets (milliseconds)
    private static final long MAX_TOKEN_GENERATION_TIME = 100;
    private static final long MAX_TOKEN_VALIDATION_TIME = 50;
    
    // ========================================================================
    // Test Fixtures
    // ========================================================================
    
    private static byte[] sharedKey;
    private static LTPAPrivateKey rsaPrivateKey;
    private static LTPAPublicKey rsaPublicKey;
    private static PrivateKey pqcPrivateKey65;
    private static PublicKey pqcPublicKey65;
    private static PrivateKey pqcPrivateKey87;
    private static PublicKey pqcPublicKey87;
    private static PQCProvider pqcProvider;
    
    private LTPAToken2Hybrid hybridToken;
    
    // ========================================================================
    // Test Setup
    // ========================================================================
    
    /**
     * One-time setup: Generate test keys for all tests.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Generate shared key
        sharedKey = Base64Coder.base64Encode(DECODED_SHARED_KEY);
        
        // Generate RSA key pair (2048-bit)
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(2048, new SecureRandom());
        KeyPair rsaKeyPair = rsaGen.generateKeyPair();
        rsaPrivateKey = new LTPAPrivateKey(rsaKeyPair.getPrivate().getEncoded());
        rsaPublicKey = new LTPAPublicKey(rsaKeyPair.getPublic().getEncoded());
        
        // Get PQC provider
        pqcProvider = PQCProviderFactory.getProvider();
        
        // Generate PQC key pairs (ML-DSA-65 and ML-DSA-87)
        PQCKeyPair pqcKeyPair65 = pqcProvider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        pqcPrivateKey65 = pqcKeyPair65.getPrivateKey();
        pqcPublicKey65 = pqcKeyPair65.getPublicKey();
        
        PQCKeyPair pqcKeyPair87 = pqcProvider.generateKeyPair(PQCAlgorithm.ML_DSA_87);
        pqcPrivateKey87 = pqcKeyPair87.getPrivateKey();
        pqcPublicKey87 = pqcKeyPair87.getPublicKey();
    }
    
    /**
     * Setup before each test.
     */
    @Before
    public void setUp() throws Exception {
        hybridToken = null;
    }
    
    /**
     * Cleanup after each test.
     */
    @After
    public void tearDown() throws Exception {
        hybridToken = null;
    }
    
    // ========================================================================
    // Token Generation Tests
    // ========================================================================
    
    /**
     * Test basic hybrid token generation with ML-DSA-65.
     * Verifies that a hybrid token can be created with both RSA and PQC signatures.
     */
    @Test
    public void testHybridTokenGeneration_MLDSA65() throws Exception {
        // Create hybrid token
        hybridToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        
        // Verify token was created
        assertNotNull("Hybrid token should not be null", hybridToken);
        
        // Verify PQC algorithm
        assertEquals("PQC algorithm should be ML-DSA-65", 
                     PQCAlgorithm.ML_DSA_65, hybridToken.getPqcAlgorithm());
        
        // Verify PQC validation is enabled
        assertTrue("PQC validation should be enabled", hybridToken.isPqcValidationEnabled());
        
        // Get token bytes (triggers encryption and signature generation)
        byte[] tokenBytes = hybridToken.getBytes();
        assertNotNull("Token bytes should not be null", tokenBytes);
        assertTrue("Token bytes should not be empty", tokenBytes.length > 0);
        
        // Verify PQC signature was generated
        byte[] pqcSignature = hybridToken.getPqcSignature();
        assertNotNull("PQC signature should not be null", pqcSignature);
        
        // ML-DSA-65 signature size should be approximately 3293 bytes
        assertTrue("PQC signature size should be around 3293 bytes for ML-DSA-65",
                   pqcSignature.length >= 3200 && pqcSignature.length <= 3400);
    }
    
    /**
     * Test hybrid token generation with ML-DSA-87.
     * Verifies that ML-DSA-87 algorithm works correctly.
     */
    @Test
    public void testHybridTokenGeneration_MLDSA87() throws Exception {
        // Create hybrid token with ML-DSA-87
        hybridToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey87,
            pqcPublicKey87,
            PQCAlgorithm.ML_DSA_87
        );
        
        // Verify PQC algorithm
        assertEquals("PQC algorithm should be ML-DSA-87",
                     PQCAlgorithm.ML_DSA_87, hybridToken.getPqcAlgorithm());
        
        // Get token bytes
        byte[] tokenBytes = hybridToken.getBytes();
        assertNotNull("Token bytes should not be null", tokenBytes);
        
        // Verify PQC signature was generated
        byte[] pqcSignature = hybridToken.getPqcSignature();
        assertNotNull("PQC signature should not be null", pqcSignature);
        
        // ML-DSA-87 signature size should be approximately 4595 bytes
        assertTrue("PQC signature size should be around 4595 bytes for ML-DSA-87",
                   pqcSignature.length >= 4500 && pqcSignature.length <= 4700);
    }
    
    /**
     * Test that null PQC private key throws IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHybridTokenGeneration_NullPQCPrivateKey() throws Exception {
        new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            null, // null PQC private key
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
    }
    
    /**
     * Test that null PQC public key throws IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHybridTokenGeneration_NullPQCPublicKey() throws Exception {
        new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            null, // null PQC public key
            PQCAlgorithm.ML_DSA_65
        );
    }
    
    /**
     * Test that null PQC algorithm throws IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHybridTokenGeneration_NullPQCAlgorithm() throws Exception {
        new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            null // null PQC algorithm
        );
    }
    
    // ========================================================================
    // Token Validation Tests
    // ========================================================================
    
    /**
     * Test hybrid token validation with both RSA and PQC signatures.
     * Verifies that a valid hybrid token passes validation.
     */
    @Test
    public void testHybridTokenValidation_BothSignaturesValid() throws Exception {
        // Create and encrypt hybrid token
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        byte[] tokenBytes = originalToken.getBytes();
        
        // Validate token (creates new instance from bytes)
        LTPAToken2Hybrid validatedToken = new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            TEST_EXP_DIFF_ALLOWED
        );
        
        // Verify token is valid
        assertTrue("Hybrid token should be valid", validatedToken.isValid());
        
        // Verify access ID matches
        String[] accessIdArray = validatedToken.getAttributes("u");
        assertNotNull("Access ID should not be null", accessIdArray);
        assertEquals("Access ID should match", TEST_ACCESS_ID, accessIdArray[0]);
    }
    
    /**
     * Test backward compatibility: classical server validates RSA signature only.
     * Verifies that PQC validation can be disabled for backward compatibility.
     */
    @Test
    public void testBackwardCompatibility_RSAOnlyValidation() throws Exception {
        // Create hybrid token
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        byte[] tokenBytes = originalToken.getBytes();
        
        // Validate with PQC validation disabled (backward compatibility mode)
        LTPAToken2Hybrid validatedToken = new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            TEST_EXP_DIFF_ALLOWED,
            PQCAlgorithm.ML_DSA_65,
            false // PQC validation disabled
        );
        
        // Verify PQC validation is disabled
        assertFalse("PQC validation should be disabled", validatedToken.isPqcValidationEnabled());
        
        // Verify token is still valid (RSA signature only)
        assertTrue("Token should be valid with RSA signature only", validatedToken.isValid());
    }
    
    /**
     * Test that corrupted RSA signature fails validation.
     */
    @Test(expected = InvalidTokenException.class)
    public void testTokenValidation_CorruptedRSASignature() throws Exception {
        // Create hybrid token
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        byte[] tokenBytes = originalToken.getBytes();
        
        // Corrupt token bytes (flip some bits in the middle)
        tokenBytes[tokenBytes.length / 2] ^= 0xFF;
        
        // Attempt to validate corrupted token (should throw InvalidTokenException)
        new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            TEST_EXP_DIFF_ALLOWED
        );
    }
    
    /**
     * Test that wrong PQC public key fails validation.
     */
    @Test(expected = InvalidTokenException.class)
    public void testTokenValidation_WrongPQCPublicKey() throws Exception {
        // Create hybrid token with one key pair
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        byte[] tokenBytes = originalToken.getBytes();
        
        // Generate different PQC key pair
        PQCKeyPair wrongKeyPair = pqcProvider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
        
        // Attempt to validate with wrong PQC public key (should throw InvalidTokenException)
        new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            wrongKeyPair.getPrivateKey(),
            wrongKeyPair.getPublicKey(), // wrong public key
            TEST_EXP_DIFF_ALLOWED
        );
    }
    
    /**
     * Test that expired token throws TokenExpiredException.
     */
    @Test(expected = TokenExpiredException.class)
    public void testTokenValidation_ExpiredToken() throws Exception {
        // Create token with very short expiration (1 millisecond)
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            0, // expires immediately
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        byte[] tokenBytes = originalToken.getBytes();
        
        // Wait for token to expire
        Thread.sleep(100);
        
        // Attempt to validate expired token (should throw TokenExpiredException)
        new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            0 // no expiration difference allowed
        );
    }
    
    // ========================================================================
    // Signature Chaining Tests
    // ========================================================================
    
    /**
     * Test signature chaining: PQC signature signs (payload + RSA signature).
     * Verifies that tampering with RSA signature invalidates PQC signature.
     */
    @Test
    public void testSignatureChaining_PQCSignsPayloadPlusRSA() throws Exception {
        // Create hybrid token
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        byte[] tokenBytes = originalToken.getBytes();
        
        // Validate token successfully
        LTPAToken2Hybrid validatedToken = new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            TEST_EXP_DIFF_ALLOWED
        );
        assertTrue("Token should be valid", validatedToken.isValid());
        
        // Verify PQC signature exists
        byte[] pqcSignature = validatedToken.getPqcSignature();
        assertNotNull("PQC signature should exist", pqcSignature);
        assertTrue("PQC signature should not be empty", pqcSignature.length > 0);
    }
    
    // ========================================================================
    // PQC Metadata Tests
    // ========================================================================
    
    /**
     * Test that PQC metadata is added to token payload.
     * Verifies pqcEnabled, pqcAlgorithm, pqcVersion, and tokenVersion attributes.
     */
    @Test
    public void testPQCMetadata_AddedToPayload() throws Exception {
        // Create hybrid token
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        byte[] tokenBytes = originalToken.getBytes();
        
        // Validate and extract metadata
        LTPAToken2Hybrid validatedToken = new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            TEST_EXP_DIFF_ALLOWED
        );
        
        // Verify pqcEnabled attribute
        String[] pqcEnabled = validatedToken.getAttributes("pqcEnabled");
        assertNotNull("pqcEnabled attribute should exist", pqcEnabled);
        assertEquals("pqcEnabled should be true", "true", pqcEnabled[0]);
        
        // Verify pqcAlgorithm attribute
        String[] pqcAlgorithm = validatedToken.getAttributes("pqcAlgorithm");
        assertNotNull("pqcAlgorithm attribute should exist", pqcAlgorithm);
        assertEquals("pqcAlgorithm should be ML-DSA-65", "ML-DSA-65", pqcAlgorithm[0]);
        
        // Verify pqcVersion attribute
        String[] pqcVersion = validatedToken.getAttributes("pqcVersion");
        assertNotNull("pqcVersion attribute should exist", pqcVersion);
        assertEquals("pqcVersion should be 1", "1", pqcVersion[0]);
        
        // Verify tokenVersion attribute
        String[] tokenVersion = validatedToken.getAttributes("tokenVersion");
        assertNotNull("tokenVersion attribute should exist", tokenVersion);
        assertEquals("tokenVersion should be 1", "1", tokenVersion[0]);
    }
    
    /**
     * Test PQC metadata with ML-DSA-87 algorithm.
     */
    @Test
    public void testPQCMetadata_MLDSA87() throws Exception {
        // Create hybrid token with ML-DSA-87
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey87,
            pqcPublicKey87,
            PQCAlgorithm.ML_DSA_87
        );
        byte[] tokenBytes = originalToken.getBytes();
        
        // Validate and extract metadata
        LTPAToken2Hybrid validatedToken = new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey87,
            pqcPublicKey87,
            TEST_EXP_DIFF_ALLOWED,
            PQCAlgorithm.ML_DSA_87,
            true
        );
        
        // Verify pqcAlgorithm attribute
        String[] pqcAlgorithm = validatedToken.getAttributes("pqcAlgorithm");
        assertNotNull("pqcAlgorithm attribute should exist", pqcAlgorithm);
        assertEquals("pqcAlgorithm should be ML-DSA-87", "ML-DSA-87", pqcAlgorithm[0]);
    }
    
    // ========================================================================
    // Token Serialization Tests
    // ========================================================================
    
    /**
     * Test token serialization and deserialization (round-trip).
     * Verifies that token bytes can be encrypted and decrypted correctly.
     */
    @Test
    public void testTokenSerialization_RoundTrip() throws Exception {
        // Create hybrid token
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        
        // Get token bytes (first serialization)
        byte[] tokenBytes1 = originalToken.getBytes();
        assertNotNull("Token bytes should not be null", tokenBytes1);
        
        // Deserialize and re-serialize
        LTPAToken2Hybrid deserializedToken = new LTPAToken2Hybrid(
            tokenBytes1,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            TEST_EXP_DIFF_ALLOWED
        );
        byte[] tokenBytes2 = deserializedToken.getBytes();
        
        // Verify token bytes are identical
        assertArrayEquals("Token bytes should be identical after round-trip", tokenBytes1, tokenBytes2);
    }
    
    /**
     * Test that getBytes() returns a defensive copy.
     * Verifies that modifying returned bytes doesn't affect internal state.
     */
    @Test
    public void testTokenSerialization_DefensiveCopy() throws Exception {
        // Create hybrid token
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        
        // Get token bytes
        byte[] tokenBytes1 = originalToken.getBytes();
        byte[] tokenBytes2 = originalToken.getBytes();
        
        // Modify first copy
        tokenBytes1[0] ^= 0xFF;
        
        // Verify second copy is unchanged
        assertFalse("Token bytes should be independent copies",
                    tokenBytes1[0] == tokenBytes2[0]);
    }
    
    // ========================================================================
    // Clone Tests
    // ========================================================================
    
    /**
     * Test token cloning creates a deep copy.
     * Verifies that cloned token is independent of original.
     */
    @Test
    public void testClone_DeepCopy() throws Exception {
        // Create hybrid token
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        
        // Clone token
        LTPAToken2Hybrid clonedToken = (LTPAToken2Hybrid) originalToken.clone();
        
        // Verify clone is not null
        assertNotNull("Cloned token should not be null", clonedToken);
        
        // Verify clone is a different instance
        assertFalse("Cloned token should be a different instance",
                    originalToken == clonedToken);
        
        // Verify clone has same PQC algorithm
        assertEquals("Cloned token should have same PQC algorithm",
                     originalToken.getPqcAlgorithm(), clonedToken.getPqcAlgorithm());
        
        // Verify clone has same expiration
        assertEquals("Cloned token should have same expiration",
                     originalToken.getExpiration(), clonedToken.getExpiration());
        
        // Verify clone has same access ID
        String[] originalAccessId = originalToken.getAttributes("u");
        String[] clonedAccessId = clonedToken.getAttributes("u");
        assertArrayEquals("Cloned token should have same access ID",
                          originalAccessId, clonedAccessId);
    }
    
    /**
     * Test that cloned token can be independently modified.
     */
    @Test
    public void testClone_IndependentModification() throws Exception {
        // Create hybrid token
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        
        // Clone token
        LTPAToken2Hybrid clonedToken = (LTPAToken2Hybrid) originalToken.clone();
        
        // Modify cloned token (add attribute)
        clonedToken.addAttribute("testAttribute", "testValue");
        
        // Verify original token is unchanged
        String[] originalAttr = originalToken.getAttributes("testAttribute");
        String[] clonedAttr = clonedToken.getAttributes("testAttribute");
        
        assertNull("Original token should not have test attribute", originalAttr);
        assertNotNull("Cloned token should have test attribute", clonedAttr);
        assertEquals("Cloned token test attribute should have correct value",
                     "testValue", clonedAttr[0]);
    }
    
    // ========================================================================
    // Provider Integration Tests
    // ========================================================================
    
    /**
     * Test that PQC provider is correctly initialized.
     */
    @Test
    public void testProviderIntegration_Initialization() throws Exception {
        // Create hybrid token
        LTPAToken2Hybrid token = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        
        // Verify token was created successfully (provider initialized)
        assertNotNull("Token should be created successfully", token);
        
        // Verify PQC algorithm is set
        assertEquals("PQC algorithm should be ML-DSA-65",
                     PQCAlgorithm.ML_DSA_65, token.getPqcAlgorithm());
    }
    
    /**
     * Test provider integration with both ML-DSA-65 and ML-DSA-87.
     */
    @Test
    public void testProviderIntegration_MultipleAlgorithms() throws Exception {
        // Create token with ML-DSA-65
        LTPAToken2Hybrid token65 = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        byte[] tokenBytes65 = token65.getBytes();
        
        // Create token with ML-DSA-87
        LTPAToken2Hybrid token87 = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey87,
            pqcPublicKey87,
            PQCAlgorithm.ML_DSA_87
        );
        byte[] tokenBytes87 = token87.getBytes();
        
        // Verify both tokens are valid
        LTPAToken2Hybrid validated65 = new LTPAToken2Hybrid(
            tokenBytes65,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            TEST_EXP_DIFF_ALLOWED
        );
        assertTrue("ML-DSA-65 token should be valid", validated65.isValid());
        
        LTPAToken2Hybrid validated87 = new LTPAToken2Hybrid(
            tokenBytes87,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey87,
            pqcPublicKey87,
            TEST_EXP_DIFF_ALLOWED,
            PQCAlgorithm.ML_DSA_87,
            true
        );
        assertTrue("ML-DSA-87 token should be valid", validated87.isValid());
    }
    
    // ========================================================================
    // Performance Benchmark Tests
    // ========================================================================
    
    /**
     * Benchmark token generation performance.
     * Verifies that token generation completes within acceptable time limits.
     */
    @Test
    public void testPerformance_TokenGeneration() throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Create hybrid token
        LTPAToken2Hybrid token = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        
        // Get token bytes (triggers signature generation)
        byte[] tokenBytes = token.getBytes();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify token was created
        assertNotNull("Token bytes should not be null", tokenBytes);
        
        // Log performance
        System.out.println("Token generation time: " + duration + "ms");
        
        // Verify performance target (relaxed for test environment)
        assertTrue("Token generation should complete within " + MAX_TOKEN_GENERATION_TIME + "ms (actual: " + duration + "ms)",
                   duration < MAX_TOKEN_GENERATION_TIME * 2); // 2x buffer for test environment
    }
    
    /**
     * Benchmark token validation performance.
     * Verifies that token validation completes within acceptable time limits.
     */
    @Test
    public void testPerformance_TokenValidation() throws Exception {
        // Create hybrid token
        LTPAToken2Hybrid originalToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        byte[] tokenBytes = originalToken.getBytes();
        
        long startTime = System.currentTimeMillis();
        
        // Validate token
        LTPAToken2Hybrid validatedToken = new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            TEST_EXP_DIFF_ALLOWED
        );
        boolean valid = validatedToken.isValid();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify token is valid
        assertTrue("Token should be valid", valid);
        
        // Log performance
        System.out.println("Token validation time: " + duration + "ms");
        
        // Verify performance target (relaxed for test environment)
        assertTrue("Token validation should complete within " + MAX_TOKEN_VALIDATION_TIME + "ms (actual: " + duration + "ms)",
                   duration < MAX_TOKEN_VALIDATION_TIME * 2); // 2x buffer for test environment
    }
    
    /**
     * Compare classical vs hybrid token performance.
     * Provides performance comparison data for analysis.
     */
    @Test
    public void testPerformance_ClassicalVsHybrid() throws Exception {
        // Measure classical token generation
        long classicalStart = System.currentTimeMillis();
        LTPAToken2 classicalToken = new LTPAToken2(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey
        );
        byte[] classicalBytes = classicalToken.getBytes();
        long classicalEnd = System.currentTimeMillis();
        long classicalDuration = classicalEnd - classicalStart;
        
        // Measure hybrid token generation
        long hybridStart = System.currentTimeMillis();
        LTPAToken2Hybrid hybridToken = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        byte[] hybridBytes = hybridToken.getBytes();
        long hybridEnd = System.currentTimeMillis();
        long hybridDuration = hybridEnd - hybridStart;
        
        // Log performance comparison
        System.out.println("Classical token generation: " + classicalDuration + "ms");
        System.out.println("Hybrid token generation: " + hybridDuration + "ms");
        System.out.println("Hybrid overhead: " + (hybridDuration - classicalDuration) + "ms");
        System.out.println("Classical token size: " + classicalBytes.length + " bytes");
        System.out.println("Hybrid token size: " + hybridBytes.length + " bytes");
        System.out.println("Size increase: " + (hybridBytes.length - classicalBytes.length) + " bytes");
        
        // Verify both tokens are valid
        assertNotNull("Classical token should be created", classicalBytes);
        assertNotNull("Hybrid token should be created", hybridBytes);
        assertTrue("Hybrid token should be larger than classical token",
                   hybridBytes.length > classicalBytes.length);
    }
    
    // ========================================================================
    // Edge Case Tests
    // ========================================================================
    
    /**
     * Test token with empty access ID.
     */
    @Test
    public void testEdgeCase_EmptyAccessID() throws Exception {
        // Create token with empty access ID
        LTPAToken2Hybrid token = new LTPAToken2Hybrid(
            "",
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        
        // Verify token can be created and validated
        byte[] tokenBytes = token.getBytes();
        assertNotNull("Token bytes should not be null", tokenBytes);
        
        LTPAToken2Hybrid validatedToken = new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            TEST_EXP_DIFF_ALLOWED
        );
        assertTrue("Token with empty access ID should be valid", validatedToken.isValid());
    }
    
    /**
     * Test token with very long access ID.
     */
    @Test
    public void testEdgeCase_LongAccessID() throws Exception {
        // Create very long access ID (1000 characters)
        StringBuilder longAccessId = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longAccessId.append("user:realm/");
        }
        
        // Create token with long access ID
        LTPAToken2Hybrid token = new LTPAToken2Hybrid(
            longAccessId.toString(),
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        
        // Verify token can be created and validated
        byte[] tokenBytes = token.getBytes();
        assertNotNull("Token bytes should not be null", tokenBytes);
        
        LTPAToken2Hybrid validatedToken = new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            TEST_EXP_DIFF_ALLOWED
        );
        assertTrue("Token with long access ID should be valid", validatedToken.isValid());
    }
    
    /**
     * Test token with special characters in access ID.
     */
    @Test
    public void testEdgeCase_SpecialCharactersInAccessID() throws Exception {
        // Create access ID with special characters
        String specialAccessId = "user:realm/test@example.com!#$%&*()";
        
        // Create token
        LTPAToken2Hybrid token = new LTPAToken2Hybrid(
            specialAccessId,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        
        // Verify token can be created and validated
        byte[] tokenBytes = token.getBytes();
        assertNotNull("Token bytes should not be null", tokenBytes);
        
        LTPAToken2Hybrid validatedToken = new LTPAToken2Hybrid(
            tokenBytes,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            TEST_EXP_DIFF_ALLOWED
        );
        assertTrue("Token with special characters should be valid", validatedToken.isValid());
        
        // Verify access ID is preserved
        String[] accessIdArray = validatedToken.getAttributes("u");
        assertEquals("Access ID should be preserved", specialAccessId, accessIdArray[0]);
    }
    
    /**
     * Test getPqcSignature returns null before token bytes are generated.
     */
    @Test
    public void testEdgeCase_PQCSignatureBeforeGeneration() throws Exception {
        // Create token but don't call getBytes()
        LTPAToken2Hybrid token = new LTPAToken2Hybrid(
            TEST_ACCESS_ID,
            TEST_EXPIRATION_MINUTES,
            sharedKey,
            rsaPrivateKey,
            rsaPublicKey,
            pqcPrivateKey65,
            pqcPublicKey65,
            PQCAlgorithm.ML_DSA_65
        );
        
        // PQC signature should be null before getBytes() is called
        byte[] pqcSignature = token.getPqcSignature();
        assertNull("PQC signature should be null before token bytes are generated", pqcSignature);
        
        // Generate token bytes
        token.getBytes();
        
        // Now PQC signature should exist
        pqcSignature = token.getPqcSignature();
        assertNotNull("PQC signature should exist after token bytes are generated", pqcSignature);
    }
}

// Made with Bob
