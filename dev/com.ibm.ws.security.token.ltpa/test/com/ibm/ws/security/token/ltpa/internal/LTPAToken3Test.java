/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.security.token.ltpa.LTPAHybridKeys;
import com.ibm.ws.security.token.ltpa.pqc.LTPAHybridKeyGenerator;
import com.ibm.ws.security.token.ltpa.pqc.MLDSAAlgorithmType;
import com.ibm.ws.security.token.ltpa.pqc.MLKEMAlgorithmType;

/**
 * Unit tests for LTPAToken3 - LTPA v3 tokens with hybrid PQC support.
 * 
 * Tests cover:
 * - Token creation and initialization
 * - Encryption and decryption with ML-KEM
 * - Signing and verification with RSA + ML-DSA
 * - Token expiration handling
 * - Attribute management
 * - Error conditions and edge cases
 */
public class LTPAToken3Test {

    private LTPAHybridKeys testKeys;
    private static final long ONE_HOUR_MS = 60 * 60 * 1000;
    private static final long ONE_MINUTE_MS = 60 * 1000;

    @Before
    public void setUp() throws Exception {
        // Generate test keys with ML-DSA-44 and ML-KEM-512 for faster tests
        testKeys = LTPAHybridKeyGenerator.generateKeys(
            2048,
            MLDSAAlgorithmType.ML_DSA_44,
            MLKEMAlgorithmType.ML_KEM_512
        );
    }

    @After
    public void tearDown() {
        testKeys = null;
    }

    // ========== Token Creation Tests ==========

    @Test
    public void testTokenCreation_WithValidKeys() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        attributes.put("realm", "defaultRealm");

        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);

        assertNotNull("Token should not be null", token);
        assertEquals("User attribute should match", "testuser", token.getAttributes().get("user"));
        assertEquals("Realm attribute should match", "defaultRealm", token.getAttributes().get("realm"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTokenCreation_WithNullKeys() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        new LTPAToken3(attributes, expiration, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTokenCreation_WithNullAttributes() throws Exception {
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        new LTPAToken3(null, expiration, testKeys);
    }

    @Test
    public void testTokenCreation_WithEmptyAttributes() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);

        assertNotNull("Token should not be null", token);
        assertTrue("Attributes should be empty", token.getAttributes().isEmpty());
    }

    @Test
    public void testTokenCreation_WithPastExpiration() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long pastExpiration = System.currentTimeMillis() - ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, pastExpiration, testKeys);

        assertNotNull("Token should be created even with past expiration", token);
    }

    // ========== Encryption and Decryption Tests ==========

    @Test
    public void testEncryptDecrypt_BasicRoundTrip() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        attributes.put("realm", "defaultRealm");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);

        // Encrypt the token
        byte[] encryptedBytes = token.getBytes();
        assertNotNull("Encrypted bytes should not be null", encryptedBytes);
        assertTrue("Encrypted bytes should not be empty", encryptedBytes.length > 0);

        // Decrypt the token
        LTPAToken3 decryptedToken = new LTPAToken3(encryptedBytes, testKeys);

        assertNotNull("Decrypted token should not be null", decryptedToken);
        assertEquals("User should match", "testuser", decryptedToken.getAttributes().get("user"));
        assertEquals("Realm should match", "defaultRealm", decryptedToken.getAttributes().get("realm"));
    }

    @Test
    public void testEncryptDecrypt_WithMultipleAttributes() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        attributes.put("realm", "defaultRealm");
        attributes.put("groups", "admin,users");
        attributes.put("sessionId", "abc123");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);
        byte[] encryptedBytes = token.getBytes();

        LTPAToken3 decryptedToken = new LTPAToken3(encryptedBytes, testKeys);

        assertEquals("User should match", "testuser", decryptedToken.getAttributes().get("user"));
        assertEquals("Realm should match", "defaultRealm", decryptedToken.getAttributes().get("realm"));
        assertEquals("Groups should match", "admin,users", decryptedToken.getAttributes().get("groups"));
        assertEquals("SessionId should match", "abc123", decryptedToken.getAttributes().get("sessionId"));
    }

    @Test
    public void testEncryptDecrypt_WithSpecialCharacters() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "test@user.com");
        attributes.put("displayName", "Test User (Admin)");
        attributes.put("path", "/home/user/documents");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);
        byte[] encryptedBytes = token.getBytes();

        LTPAToken3 decryptedToken = new LTPAToken3(encryptedBytes, testKeys);

        assertEquals("User with @ should match", "test@user.com", decryptedToken.getAttributes().get("user"));
        assertEquals("Display name with special chars should match", "Test User (Admin)", 
                     decryptedToken.getAttributes().get("displayName"));
        assertEquals("Path should match", "/home/user/documents", decryptedToken.getAttributes().get("path"));
    }

    @Test(expected = InvalidTokenException.class)
    public void testDecrypt_WithCorruptedData() throws Exception {
        byte[] corruptedData = new byte[]{1, 2, 3, 4, 5};

        new LTPAToken3(corruptedData, testKeys);
    }

    @Test(expected = InvalidTokenException.class)
    public void testDecrypt_WithWrongKeys() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);
        byte[] encryptedBytes = token.getBytes();

        // Generate different keys
        LTPAHybridKeys wrongKeys = LTPAHybridKeyGenerator.generateKeys(
            2048,
            MLDSAAlgorithmType.ML_DSA_44,
            MLKEMAlgorithmType.ML_KEM_512
        );

        new LTPAToken3(encryptedBytes, wrongKeys);
    }

    // ========== Signing and Verification Tests ==========

    @Test
    public void testSignVerify_ValidToken() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);
        byte[] tokenBytes = token.getBytes();

        // Verification happens during decryption
        LTPAToken3 verifiedToken = new LTPAToken3(tokenBytes, testKeys);

        assertNotNull("Verified token should not be null", verifiedToken);
        assertEquals("User should match after verification", "testuser", 
                     verifiedToken.getAttributes().get("user"));
    }

    @Test(expected = InvalidTokenException.class)
    public void testVerify_WithTamperedData() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);
        byte[] tokenBytes = token.getBytes();

        // Tamper with the token data
        tokenBytes[tokenBytes.length / 2] ^= 0xFF;

        new LTPAToken3(tokenBytes, testKeys);
    }

    @Test
    public void testSignVerify_BothRSAandMLDSA() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);
        byte[] tokenBytes = token.getBytes();

        // Both RSA and ML-DSA signatures should be verified
        LTPAToken3 verifiedToken = new LTPAToken3(tokenBytes, testKeys);

        assertNotNull("Token with both signatures verified should not be null", verifiedToken);
    }

    // ========== Expiration Tests ==========

    @Test(expected = TokenExpiredException.class)
    public void testExpiration_ExpiredToken() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long pastExpiration = System.currentTimeMillis() - ONE_MINUTE_MS;

        LTPAToken3 token = new LTPAToken3(attributes, pastExpiration, testKeys);
        byte[] tokenBytes = token.getBytes();

        // Should throw TokenExpiredException during decryption
        new LTPAToken3(tokenBytes, testKeys);
    }

    @Test
    public void testExpiration_NotYetExpired() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long futureExpiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, futureExpiration, testKeys);
        byte[] tokenBytes = token.getBytes();

        LTPAToken3 decryptedToken = new LTPAToken3(tokenBytes, testKeys);

        assertNotNull("Non-expired token should decrypt successfully", decryptedToken);
    }

    @Test
    public void testExpiration_JustExpired() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long justExpired = System.currentTimeMillis() - 1000; // 1 second ago

        LTPAToken3 token = new LTPAToken3(attributes, justExpired, testKeys);
        byte[] tokenBytes = token.getBytes();

        try {
            new LTPAToken3(tokenBytes, testKeys);
            fail("Should have thrown TokenExpiredException");
        } catch (TokenExpiredException e) {
            // Expected
            assertTrue("Exception message should mention expiration", 
                       e.getMessage().contains("expired") || e.getMessage().contains("Expired"));
        }
    }

    @Test
    public void testSetExpiration() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long initialExpiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, initialExpiration, testKeys);

        long newExpiration = System.currentTimeMillis() + (2 * ONE_HOUR_MS);
        token.setExpiration(newExpiration);

        byte[] tokenBytes = token.getBytes();
        LTPAToken3 decryptedToken = new LTPAToken3(tokenBytes, testKeys);

        assertNotNull("Token with updated expiration should decrypt", decryptedToken);
    }

    // ========== Attribute Management Tests ==========

    @Test
    public void testAddAttribute() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);
        token.addAttribute("newAttr", "newValue");

        byte[] tokenBytes = token.getBytes();
        LTPAToken3 decryptedToken = new LTPAToken3(tokenBytes, testKeys);

        assertEquals("New attribute should be present", "newValue", 
                     decryptedToken.getAttributes().get("newAttr"));
    }

    @Test
    public void testGetAttributes() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        attributes.put("realm", "defaultRealm");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);

        Map<String, Object> retrievedAttrs = token.getAttributes();

        assertNotNull("Attributes should not be null", retrievedAttrs);
        assertEquals("Should have 2 attributes", 2, retrievedAttrs.size());
        assertEquals("User should match", "testuser", retrievedAttrs.get("user"));
        assertEquals("Realm should match", "defaultRealm", retrievedAttrs.get("realm"));
    }

    // ========== Clone Tests ==========

    @Test
    public void testClone() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        attributes.put("realm", "defaultRealm");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 originalToken = new LTPAToken3(attributes, expiration, testKeys);
        LTPAToken3 clonedToken = (LTPAToken3) originalToken.clone();

        assertNotNull("Cloned token should not be null", clonedToken);
        assertNotSame("Cloned token should be a different object", originalToken, clonedToken);
        assertEquals("User should match in clone", "testuser", clonedToken.getAttributes().get("user"));
        assertEquals("Realm should match in clone", "defaultRealm", clonedToken.getAttributes().get("realm"));
    }

    @Test
    public void testClone_ModifyOriginalDoesNotAffectClone() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 originalToken = new LTPAToken3(attributes, expiration, testKeys);
        LTPAToken3 clonedToken = (LTPAToken3) originalToken.clone();

        originalToken.addAttribute("newAttr", "newValue");

        assertNull("Clone should not have new attribute", clonedToken.getAttributes().get("newAttr"));
        assertEquals("Original should have new attribute", "newValue", 
                     originalToken.getAttributes().get("newAttr"));
    }

    // ========== Edge Cases and Error Conditions ==========

    @Test
    public void testLargeAttributes() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeValue.append("LargeData");
        }
        attributes.put("largeAttr", largeValue.toString());
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);
        byte[] tokenBytes = token.getBytes();

        LTPAToken3 decryptedToken = new LTPAToken3(tokenBytes, testKeys);

        assertEquals("Large attribute should match", largeValue.toString(), 
                     decryptedToken.getAttributes().get("largeAttr"));
    }

    @Test
    public void testManyAttributes() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            attributes.put("attr" + i, "value" + i);
        }
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);
        byte[] tokenBytes = token.getBytes();

        LTPAToken3 decryptedToken = new LTPAToken3(tokenBytes, testKeys);

        assertEquals("Should have 100 attributes", 100, decryptedToken.getAttributes().size());
        for (int i = 0; i < 100; i++) {
            assertEquals("Attribute " + i + " should match", "value" + i, 
                         decryptedToken.getAttributes().get("attr" + i));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecrypt_WithEmptyBytes() throws Exception {
        byte[] emptyBytes = new byte[0];

        new LTPAToken3(emptyBytes, testKeys);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecrypt_WithNullBytes() throws Exception {
        new LTPAToken3((byte[]) null, testKeys);
    }

    @Test
    public void testTokenSize() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", "testuser");
        long expiration = System.currentTimeMillis() + ONE_HOUR_MS;

        LTPAToken3 token = new LTPAToken3(attributes, expiration, testKeys);
        byte[] tokenBytes = token.getBytes();

        // Token should be reasonably sized (RSA sig + ML-DSA sig + encrypted data)
        assertTrue("Token should be at least 500 bytes", tokenBytes.length > 500);
        assertTrue("Token should be less than 10KB", tokenBytes.length < 10240);
    }
}

// Made with Bob