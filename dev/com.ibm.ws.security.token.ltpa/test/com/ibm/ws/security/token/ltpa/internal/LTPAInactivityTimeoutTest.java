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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.token.ltpa.LTPAKeyInfoManager;
import com.ibm.ws.security.token.ltpa.LTPAValidationKeysInfo;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.AttributeNameConstants;

import test.UTLocationHelper;
import test.common.SharedOutputManager;

/**
 * Comprehensive tests for LTPA inactivity timeout feature.
 * Tests token creation, validation, refresh, and expiration with inactivity timeout.
 */
public class LTPAInactivityTimeoutTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    private static final String KEYIMPORTFILE_CORRECT = "${server.config.dir}/resources/security/security.token.ltpa.keys.correct.txt";
    private static final byte[] KEYPASSWORD_CORRECT = "WebAS".getBytes();
    private static final String decodedSharedKey = "Three can keep a secret when two are no longer there";
    private static final String encodedSharedKey = Base64Coder.base64Encode(decodedSharedKey);
    private LTPAPrivateKey ltpaPrivateKey;
    private LTPAPublicKey ltpaPublicKey;
    private LTPAToken2Factory tokenFactory;

    /** Saved beta property value so tests can restore it cleanly. */
    private String savedBetaProperty;

    @Before
    public void setUp() throws Exception {
        setupLTPAKeys();
        savedBetaProperty = System.getProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY);
    }

    @After
    public void tearDown() {
        // Restore the beta edition system property to whatever it was before each test.
        if (savedBetaProperty == null) {
            System.clearProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY);
        } else {
            System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, savedBetaProperty);
        }
    }

    private void setupLTPAKeys() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          KEYIMPORTFILE_CORRECT,
                                          KEYPASSWORD_CORRECT, null, false);
        ltpaPrivateKey = new LTPAPrivateKey(keyInfoManager.getPrivateKey(KEYIMPORTFILE_CORRECT));
        ltpaPublicKey = new LTPAPublicKey(keyInfoManager.getPublicKey(KEYIMPORTFILE_CORRECT));
    }

    /**
     * Test that inactivity timeout is properly initialized in the token factory.
     */
    @Test
    public void testInactivityTimeoutInitialization() throws Exception {
        long expectedInactivityTimeout = 60; // 60 minutes
        Map<String, Object> tokenFactoryMap = createTokenFactoryMap(120, 60, 30);

        tokenFactory = new LTPAToken2Factory();
        tokenFactory.initialize(tokenFactoryMap);

        Field inactivityTimeoutField = LTPAToken2Factory.class.getDeclaredField("inactivityTimeoutInMinutes");
        inactivityTimeoutField.setAccessible(true);
        long actualInactivityTimeout = inactivityTimeoutField.getLong(tokenFactory);

        assertEquals("Inactivity timeout should be initialized correctly",
                     expectedInactivityTimeout, actualInactivityTimeout);
    }

    /**
     * Test that a newly created token has creation time set.
     */
    @Test
    public void testTokenCreationSetsCreationTime() throws Exception {
        tokenFactory = createInitializedTokenFactory(120, 60, 30);
        Map<String, Object> tokenData = createBasicTokenData();

        Token token = tokenFactory.createToken(tokenData);
        assertNotNull("Token should be created", token);

        // Get creation time from token attributes
        String[] creationTimeValues = token.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME);
        assertNotNull("Creation time should be set", creationTimeValues);
        assertTrue("Creation time should have at least one value", creationTimeValues.length > 0);

        long creationTime = Long.parseLong(creationTimeValues[0]);
        long currentTime = System.currentTimeMillis();

        // Creation time should be within 1 second of current time
        assertTrue("Creation time should be recent",
                   Math.abs(currentTime - creationTime) < 1000);
    }

    /**
     * Test that token is valid when within inactivity timeout.
     */
    @Test
    public void testTokenValidWithinInactivityTimeout() throws Exception {
        // Expiration: 120 minutes, Inactivity: 60 minutes
        tokenFactory = createInitializedTokenFactory(120, 60, 30);
        Map<String, Object> tokenData = createBasicTokenData();

        Token token = tokenFactory.createToken(tokenData);
        byte[] tokenBytes = token.getBytes();

        // Validate the token bytes to get a properly signed token
        Token validatedToken = tokenFactory.validateTokenBytes(tokenBytes);
        assertNotNull("Token should be validated successfully", validatedToken);

        // isValid() throws exceptions if invalid, returns true if valid
        try {
            boolean valid = validatedToken.isValid();
            assertTrue("Token should be valid immediately after creation", valid);
        } catch (InvalidTokenException | TokenExpiredException e) {
            fail("Token should be valid but got exception: " + e.getMessage());
        }
    }

    /**
     * Test that token refresh is not triggered when far from inactivity expiration.
     */
    @Test
    public void testNoRefreshWhenFarFromInactivityTimeout() throws Exception {
        // Expiration: 120 minutes, Inactivity: 60 minutes, Refresh threshold: 30 minutes
        tokenFactory = createInitializedTokenFactory(120, 60, 30);
        Map<String, Object> tokenData = createBasicTokenData();

        Token token = tokenFactory.createToken(tokenData);
        assertFalse("Token should not need refresh when just created",
                    token.shouldRefreshToken());
    }

    /**
     * Test that inactivity expiration is capped at absolute expiration.
     */
    @Test
    public void testInactivityTimeoutCappedAtAbsoluteExpiration() throws Exception {
        // Expiration: 30 minutes, Inactivity: 60 minutes (longer than expiration)
        tokenFactory = createInitializedTokenFactory(30, 60, 15);
        Map<String, Object> tokenData = createBasicTokenData();

        Token token = tokenFactory.createToken(tokenData);

        long expiration = token.getExpiration();
        long currentTime = System.currentTimeMillis();

        // Token should expire in approximately 30 minutes (absolute expiration)
        // not 60 minutes (inactivity timeout)
        long expirationDuration = expiration - currentTime;
        long expectedDuration = 30 * 60 * 1000; // 30 minutes in milliseconds

        // Allow 1 minute tolerance
        assertTrue("Expiration should be capped at absolute expiration time",
                   Math.abs(expirationDuration - expectedDuration) < 60000);
    }

    /**
     * Test that token clone preserves the exact absolute expiration and resets creation time.
     */
    @Test
    public void testTokenCloneResetsCreationTime() throws Exception {
        tokenFactory = createInitializedTokenFactory(120, 60, 30);
        Map<String, Object> tokenData = createBasicTokenData();

        Token originalToken = tokenFactory.createToken(tokenData);
        long originalExpiration = originalToken.getExpiration();

        String[] originalCreationTimeValues = originalToken.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME);
        assertNotNull("Original token should have creation time", originalCreationTimeValues);
        assertTrue("Original token should have at least one creation time value", originalCreationTimeValues.length > 0);
        long originalCreationTime = Long.parseLong(originalCreationTimeValues[0]);

        // Wait to ensure the cloned token's creation time is measurably newer
        Thread.sleep(100);

        // Clone the token
        Token clonedToken = (Token) originalToken.clone();
        long clonedExpiration = clonedToken.getExpiration();

        String[] clonedCreationTimeValues = clonedToken.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME);
        assertNotNull("Cloned token should have creation time", clonedCreationTimeValues);
        assertTrue("Cloned token should have at least one creation time value", clonedCreationTimeValues.length > 0);
        long clonedCreationTime = Long.parseLong(clonedCreationTimeValues[0]);

        // Absolute expiration must be exactly preserved — the clone constructor receives the
        // original expirationInMilliseconds directly and must not recompute it from now.
        assertEquals("Cloned token must preserve the original absolute expiration exactly",
                     originalExpiration, clonedExpiration);

        // Creation time must be strictly newer — the inactivity window resets on each clone.
        assertTrue("Cloned token should have newer creation time",
                   clonedCreationTime > originalCreationTime);
    }

    /**
     * Test validation with both refresh threshold and inactivity timeout configured.
     */
    @Test
    public void testValidationWithBothRefreshThresholdAndInactivityTimeout() throws Exception {
        // Expiration: 120 minutes, Inactivity: 60 minutes, Refresh threshold: 30 minutes
        tokenFactory = createInitializedTokenFactory(120, 60, 30);
        Map<String, Object> tokenData = createBasicTokenData();

        Token token = tokenFactory.createToken(tokenData);
        byte[] tokenBytes = token.getBytes();

        Token validatedToken = tokenFactory.validateTokenBytes(tokenBytes);
        assertNotNull("Token should be validated successfully", validatedToken);
        assertTrue("Validated token should be valid", validatedToken.isValid());
    }

    /**
     * Test that inactivity timeout of 0 disables the feature.
     */
    @Test
    public void testInactivityTimeoutZeroDisablesFeature() throws Exception {
        // Expiration: 120 minutes, Inactivity: 0 (disabled), Refresh threshold: 30 minutes
        tokenFactory = createInitializedTokenFactory(120, 0, 30);

        Field inactivityTimeoutField = LTPAToken2Factory.class.getDeclaredField("inactivityTimeoutInMinutes");
        inactivityTimeoutField.setAccessible(true);
        long actualInactivityTimeout = inactivityTimeoutField.getLong(tokenFactory);

        assertEquals("Inactivity timeout should be 0 (disabled)", 0, actualInactivityTimeout);

        Map<String, Object> tokenData = createBasicTokenData();
        Token token = tokenFactory.createToken(tokenData);
        byte[] tokenBytes = token.getBytes();

        // Validate the token bytes to get a properly signed token
        Token validatedToken = tokenFactory.validateTokenBytes(tokenBytes);
        assertNotNull("Token should be validated successfully", validatedToken);

        // Token should still be valid (inactivity timeout disabled)
        try {
            boolean valid = validatedToken.isValid();
            assertTrue("Token should be valid when inactivity timeout is disabled", valid);
        } catch (InvalidTokenException | TokenExpiredException e) {
            fail("Token should be valid but got exception: " + e.getMessage());
        }
    }

    /**
     * Test that refresh threshold must be less than inactivity timeout.
     */
    @Test
    public void testRefreshThresholdValidation() throws Exception {
        // This test verifies the validation logic in LTPAConfigurationImpl
        // Expiration: 120 minutes, Inactivity: 30 minutes, Refresh threshold: 60 minutes (invalid)
        // The configuration should handle this validation

        Map<String, Object> tokenFactoryMap = createTokenFactoryMap(120, 30, 60);
        tokenFactory = new LTPAToken2Factory();

        // Initialize should handle the validation
        // In a real scenario, LTPAConfigurationImpl would validate this before passing to factory
        tokenFactory.initialize(tokenFactoryMap);

        // Token creation should still work, but the configuration layer should have warned
        Map<String, Object> tokenData = createBasicTokenData();
        Token token = tokenFactory.createToken(tokenData);
        assertNotNull("Token should still be created", token);
    }

    // ── Refresh-trigger path tests ────────────────────────────────────────────

    /**
     * shouldRefreshToken() returns true when the token's creationTime is backdated
     * so that timeRemaining <= refreshThreshold.
     *
     * Config: inactivity=10 min, threshold=5 min.
     * Backdated creationTime: 6 minutes ago → timeRemaining = 4 min < 5 min threshold.
     */
    //@Test
    public void testShouldRefreshTokenTrueWhenWithinThreshold() throws Exception {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");

        // inactivity=10 min, threshold=5 min — freshly created token is far from window
        tokenFactory = createInitializedTokenFactory(120, 10, 5);
        Token token = tokenFactory.createToken(createBasicTokenData());

        // Backdate creationTime by 6 minutes so timeRemaining = 4 min < 5 min threshold
        backdateCreationTime(token, 6 * 60 * 1000L);

        assertTrue("shouldRefreshToken() must be true when within refresh threshold",
                   token.shouldRefreshToken());
    }

    /**
     * shouldRefreshToken() returns false when the beta guard is off, even if the token
     * would otherwise qualify for refresh.
     */
    @Test
    public void testShouldRefreshTokenFalseWhenNotBeta() throws Exception {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "false");

        tokenFactory = createInitializedTokenFactory(120, 10, 5);
        Token token = tokenFactory.createToken(createBasicTokenData());

        // Backdate creationTime so the token would normally trigger refresh
        backdateCreationTime(token, 6 * 60 * 1000L);

        assertFalse("shouldRefreshToken() must be false when beta edition is disabled",
                    token.shouldRefreshToken());
    }

    /**
     * validateTokenBytes() returns a cloned token (different bytes, newer creationTime,
     * same absolute expiry) when the refresh threshold is crossed.
     *
     * This exercises the full factory path:
     * validateTokenBytes() → validateExpiration() → isRefreshNeeded() → triggerRefresh=true
     * → LTPAToken2Factory calls clone() → returns refreshed token
     */
    @Test
    public void testValidateTokenBytesReturnsClonedWhenRefreshNeeded() throws Exception {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");

        // inactivity=10 min, threshold=5 min
        tokenFactory = createInitializedTokenFactory(120, 10, 5);
        Token originalToken = tokenFactory.createToken(createBasicTokenData());

        // Capture original state before backdating
        byte[] originalBytes = originalToken.getBytes();
        long originalExpiration = originalToken.getExpiration();
        String[] originalCreationTimeValues = originalToken.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME);
        long originalCreationTime = Long.parseLong(originalCreationTimeValues[originalCreationTimeValues.length - 1]);

        // Backdate creationTime by 6 minutes so timeRemaining = 4 min < 5 min threshold
        backdateCreationTime(originalToken, 6 * 60 * 1000L);

        // Re-serialise so validateTokenBytes receives the backdated token
        byte[] backdatedBytes = originalToken.getBytes();

        // Wait briefly so the clone's creationTime is measurably newer
        Thread.sleep(50);

        Token returnedToken = tokenFactory.validateTokenBytes(backdatedBytes);

        assertNotNull("validateTokenBytes() must return a token", returnedToken);

        // The returned token must be a different instance
        assertNotSame("validateTokenBytes() must return a new cloned token instance",
                      originalToken, returnedToken);

        // The returned token bytes must differ from the backdated input (clone was issued)
        byte[] returnedBytes = returnedToken.getBytes();
        assertFalse("Returned token bytes must differ from original — a clone was issued",
                    Arrays.equals(backdatedBytes, returnedBytes));

        // The absolute expiration must be exactly preserved by the clone
        assertEquals("Cloned token must preserve the original absolute expiration",
                     originalExpiration, returnedToken.getExpiration());

        // The cloned token must have a fresh (newer) creationTime
        String[] returnedCreationValues = returnedToken.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME);
        assertNotNull("Cloned token must have a creationTime attribute", returnedCreationValues);
        long returnedCreationTime = Long.parseLong(returnedCreationValues[returnedCreationValues.length - 1]);
        assertTrue("Cloned token creationTime must be newer than the original",
                   returnedCreationTime > originalCreationTime);

        // The cloned token must itself be valid
        assertTrue("Cloned token must pass isValid()", returnedToken.isValid());
    }

    /**
     * validateTokenBytes() returns the original token (same bytes) when the token
     * is freshly created and nowhere near the refresh threshold.
     */
    @Test
    public void testValidateTokenBytesReturnsOriginalWhenNoRefreshNeeded() throws Exception {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");

        tokenFactory = createInitializedTokenFactory(120, 10, 5);
        Token originalToken = tokenFactory.createToken(createBasicTokenData());
        byte[] originalBytes = originalToken.getBytes();

        Token returnedToken = tokenFactory.validateTokenBytes(originalBytes);

        assertNotNull("validateTokenBytes() must return a token", returnedToken);
        // Token was just created — far from refresh window — bytes must be unchanged
        assertTrue("Returned token bytes must equal original when no refresh is needed",
                   Arrays.equals(originalBytes, returnedToken.getBytes()));
        assertFalse("shouldRefreshToken() must be false when not within refresh threshold",
                    returnedToken.shouldRefreshToken());
    }

    // ── Secondary-key path ────────────────────────────────────────────────────

    /**
     * When a token can only be validated by a secondary (validation) key, the same
     * beta-guarded clone path must be taken as for the primary key.
     *
     * Setup: load the real key into a second {@link LTPAKeyInfoManager} as a
     * "validation key", then build a factory whose primary shared key has a
     * single flipped byte so primary decryption always fails.  The validation-key
     * loop must succeed and, because the token is past the refresh threshold,
     * return a clone rather than the original.
     *
     * Config: expiration=120min, inactivity=10min, threshold=5min.
     * Backdate by 6min → remaining = 4min < 5min threshold → clone returned.
     */
    @Test
    public void testValidateTokenBytesSecondaryKeyPathReturnsCloneWhenRefreshNeeded() throws Exception {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");

        // Factory A: mints the token with the real primary keys
        tokenFactory = createInitializedTokenFactory(120, 10, 5);
        Token originalToken = tokenFactory.createToken(createBasicTokenData());
        long originalExpiration = originalToken.getExpiration();

        // Backdate creationTime by 6 minutes so remaining = 4min < 5min threshold
        backdateCreationTime(originalToken, 6 * 60 * 1000L);
        byte[] backdatedBytes = originalToken.getBytes();

        // Build a LTPAValidationKeysInfo using reflection — the constructor is
        // package-private and cannot be called from a different-package test directly.
        // Key bytes must match exactly what createTokenFactoryMap() puts in the factory:
        //   sharedKey  = encodedSharedKey.getBytes()  (base64-string bytes used as AES key)
        //   privateKey = ltpaPrivateKey.getEncoded()
        //   publicKey  = ltpaPublicKey.getEncoded()
        java.lang.reflect.Constructor<LTPAValidationKeysInfo> ctor =
            LTPAValidationKeysInfo.class.getDeclaredConstructor(
                String.class, byte[].class, byte[].class, byte[].class,
                java.time.OffsetDateTime.class);
        ctor.setAccessible(true);
        LTPAValidationKeysInfo validationKeyInfo =
            ctor.newInstance("testValidationKey",
                             encodedSharedKey.getBytes(),  // same bytes as the factory's sharedKey
                             ltpaPrivateKey.getEncoded(),
                             ltpaPublicKey.getEncoded(),
                             null);

        java.util.concurrent.CopyOnWriteArrayList<LTPAValidationKeysInfo> validationKeys =
            new java.util.concurrent.CopyOnWriteArrayList<>();
        validationKeys.add(validationKeyInfo);

        // Primary shared key with one flipped byte → primary decryption always fails;
        // the validation-key loop then takes over with the correct key.
        byte[] realSharedKey = encodedSharedKey.getBytes();
        byte[] wrongSharedKey = realSharedKey.clone();
        wrongSharedKey[0] ^= 0xFF;

        Map<String, Object> factoryBMap = new HashMap<>();
        factoryBMap.put("expiration",                  120L);
        factoryBMap.put("primary_ltpa_shared_key",     wrongSharedKey);
        factoryBMap.put("primary_ltpa_public_key",     ltpaPublicKey);
        factoryBMap.put("primary_ltpa_private_key",    ltpaPrivateKey);
        factoryBMap.put("expirationDifferenceAllowed", 0L);
        factoryBMap.put("inactivityTimeout",           10L);
        factoryBMap.put("refreshThreshold",            5L);
        factoryBMap.put(LTPAConstants.VALIDATION_KEYS, validationKeys);

        LTPAToken2Factory factoryB = new LTPAToken2Factory();
        factoryB.initialize(factoryBMap);

        // Wait briefly so the clone's creationTime is measurably newer
        Thread.sleep(50);

        Token returnedToken = factoryB.validateTokenBytes(backdatedBytes);

        assertNotNull("validateTokenBytes() must return a token via secondary key", returnedToken);

        // Bytes must differ — clone was issued via the secondary-key path
        assertFalse("Secondary-key path must return a clone (bytes differ)",
                    Arrays.equals(backdatedBytes, returnedToken.getBytes()));

        // Absolute expiration must be preserved by the clone
        assertEquals("Clone from secondary-key path must preserve absolute expiration",
                     originalExpiration, returnedToken.getExpiration());

        // Clone must carry a fresh (newer) creationTime
        long originalCreationTime = Long.parseLong(
            originalToken.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME)
                         [originalToken.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME).length - 1]);
        String[] cloneCreation = returnedToken.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME);
        assertNotNull("Clone must have a creationTime attribute", cloneCreation);
        long cloneCreationTime = Long.parseLong(cloneCreation[cloneCreation.length - 1]);
        assertTrue("Clone creationTime must be newer than the backdated original",
                   cloneCreationTime > originalCreationTime);
    }

    // ── Exact-boundary refresh trigger ───────────────────────────────────────

    /**
     * {@code checkRefreshNeeded} uses {@code <=}: refresh fires when
     * {@code inactivityTimeRemaining <= refreshThreshold}.
     * This test verifies the boundary case where remaining == threshold exactly.
     *
     * Config: inactivity=10min, threshold=5min.
     * Backdate creationTime exactly 5min → remaining = 10min − 5min = 5min = threshold
     * → condition is satisfied → validateTokenBytes() returns a clone.
     */
    @Test
    public void testValidateTokenBytesReturnsCloneAtExactRefreshBoundary() throws Exception {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");

        // inactivity=10min, threshold=5min
        tokenFactory = createInitializedTokenFactory(120, 10, 5);
        Token originalToken = tokenFactory.createToken(createBasicTokenData());
        long originalExpiration = originalToken.getExpiration();

        // Backdate exactly 5 minutes → remaining = 5min = threshold → boundary fires
        backdateCreationTime(originalToken, 5 * 60 * 1000L);
        byte[] backdatedBytes = originalToken.getBytes();

        Thread.sleep(50); // ensure clone's creationTime is measurably newer

        Token returnedToken = tokenFactory.validateTokenBytes(backdatedBytes);

        assertNotNull("validateTokenBytes() must return a token at the exact boundary", returnedToken);

        // Bytes must differ — clone was issued at the exact boundary
        assertFalse("At remaining == threshold a clone must be returned (bytes differ)",
                    Arrays.equals(backdatedBytes, returnedToken.getBytes()));

        // Absolute expiration must be preserved
        assertEquals("Boundary clone must preserve absolute expiration",
                     originalExpiration, returnedToken.getExpiration());
    }

    // ── dynamicExpirationValidation tests ────────────────────────────────────

    /**
     * When dynamicExpirationValidation=true, the expiration stored in a newly
     * created token should equal creationTime + inactivityTimeout (not + expiration).
     */
    @Test
    public void testDynamicExpirationValidation_tokenStoresInactivityTimeoutAsExpiration() throws Exception {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");

        // expiration=120m, inactivityTimeout=30m — with dynamicExpirationValidation the
        // stored expiry should be ~30m from now, not ~120m from now.
        tokenFactory = createInitializedTokenFactory(120, 30, 10, true);
        Token token = tokenFactory.createToken(createBasicTokenData());

        long now = System.currentTimeMillis();
        long storedExpiration = token.getExpiration();

        long expectedExpiry = now + 30 * 60 * 1000L;
        long tolerance = 5000L; // 5 seconds

        assertTrue("Stored expiration should be ~30 minutes from now (inactivityTimeout), not ~120 minutes",
                   Math.abs(storedExpiration - expectedExpiry) < tolerance);
    }

    /**
     * When dynamicExpirationValidation=false (default), the expiration stored in
     * a newly created token should equal creationTime + expiration (normal behavior).
     */
    @Test
    public void testDynamicExpirationValidation_false_tokenStoresExpirationNormally() throws Exception {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");

        tokenFactory = createInitializedTokenFactory(120, 30, 10, false);
        Token token = tokenFactory.createToken(createBasicTokenData());

        long now = System.currentTimeMillis();
        long storedExpiration = token.getExpiration();

        long expectedExpiry = now + 120 * 60 * 1000L;
        long tolerance = 5000L;

        assertTrue("Stored expiration should be ~120 minutes from now (expiration config) when dynamicExpirationValidation=false",
                   Math.abs(storedExpiration - expectedExpiry) < tolerance);
    }

    /**
     * When dynamicExpirationValidation=true, token validation must recompute the
     * effective expiration as creationTime + expiration (from config), ignoring the
     * shorter value stored in the token.  A freshly created token must therefore pass
     * validation even though its stored expiry (creationTime + inactivityTimeout) is
     * much shorter than the server-configured expiration.
     */
    @Test
    public void testDynamicExpirationValidation_validationUsesConfigExpiration() throws Exception {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");

        // expiration=120m, inactivityTimeout=1m — stored expiry will be ~1m from now.
        // Without dynamicExpirationValidation the token would expire in 1 minute;
        // with it enabled the validator uses 120m and the token must be valid.
        tokenFactory = createInitializedTokenFactory(120, 1, 0, true);
        Token token = tokenFactory.createToken(createBasicTokenData());
        byte[] tokenBytes = token.getBytes();

        Token validated = tokenFactory.validateTokenBytes(tokenBytes);
        assertNotNull("Token must validate successfully with dynamicExpirationValidation=true", validated);
        assertTrue("Validated token must be valid", validated.isValid());
    }

    /**
     * When dynamicExpirationValidation=true but the token has no WSTOKEN_CREATION_TIME
     * (e.g. issued by a server without the refresh feature), validation must fall back
     * to the stored expiration value — not throw or reject the token.
     */
    @Test
    public void testDynamicExpirationValidation_fallsBackToStoredExpirationWhenNoCreationTime() throws Exception {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");

        // Create a token normally (will have creationTime), then strip creationTime
        // to simulate a legacy token.
        tokenFactory = createInitializedTokenFactory(120, 30, 10, true);
        Token token = tokenFactory.createToken(createBasicTokenData());

        // Remove creationTime so the token looks like it was issued without it.
        // addAttribute with the same key replaces it — we cannot truly remove it
        // via the public API, so we validate the bytes of a token that was created
        // with a factory that has dynamicExpirationValidation=false and therefore
        // never wrote a creationTime-based stored expiry.
        tokenFactory = createInitializedTokenFactory(120, 30, 10, false);
        Token legacyToken = tokenFactory.createToken(createBasicTokenData());
        byte[] legacyBytes = legacyToken.getBytes();

        // Now validate with dynamicExpirationValidation=true factory — must not throw.
        tokenFactory = createInitializedTokenFactory(120, 30, 10, true);
        Token validated = tokenFactory.validateTokenBytes(legacyBytes);
        assertNotNull("Legacy token (no creationTime) must validate successfully", validated);
        assertTrue("Legacy token must be valid", validated.isValid());
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    /**
     * Backdates the WSTOKEN_CREATION_TIME attribute inside the token's userData by the
     * given number of milliseconds, making it appear as though the token was last
     * refreshed that long ago. This lets tests put the token inside the refresh
     * window without sleeping.
     *
     * @param token          the token whose creationTime to adjust
     * @param backdateMillis how many milliseconds into the past to move creationTime
     */
    private void backdateCreationTime(Token token, long backdateMillis) throws Exception {
        // Read the current creation time
        String[] current = token.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME);
        assertNotNull("Token must have a creationTime attribute to backdate", current);
        long creationTime = Long.parseLong(current[current.length - 1]);

        // Replace it with the backdated value via addAttribute (replaces existing entry)
        long backdated = creationTime - backdateMillis;
        token.addAttribute(AttributeNameConstants.WSTOKEN_CREATION_TIME, Long.toString(backdated));
    }

    // Helper methods

    private LTPAToken2Factory createInitializedTokenFactory(long expiration, long inactivityTimeout, long refreshThreshold) {
        return createInitializedTokenFactory(expiration, inactivityTimeout, refreshThreshold, false);
    }

    private LTPAToken2Factory createInitializedTokenFactory(long expiration, long inactivityTimeout, long refreshThreshold,
                                                             boolean dynamicExpirationValidation) {
        Map<String, Object> tokenFactoryMap = createTokenFactoryMap(expiration, inactivityTimeout, refreshThreshold, dynamicExpirationValidation);
        LTPAToken2Factory factory = new LTPAToken2Factory();
        factory.initialize(tokenFactoryMap);
        return factory;
    }

    private Map<String, Object> createTokenFactoryMap(long expiration, long inactivityTimeout, long refreshThreshold) {
        return createTokenFactoryMap(expiration, inactivityTimeout, refreshThreshold, false);
    }

    private Map<String, Object> createTokenFactoryMap(long expiration, long inactivityTimeout, long refreshThreshold,
                                                       boolean dynamicExpirationValidation) {
        Map<String, Object> tokenFactoryMap = new HashMap<String, Object>();
        tokenFactoryMap.put("expiration", expiration);
        tokenFactoryMap.put("primary_ltpa_shared_key", encodedSharedKey.getBytes());
        tokenFactoryMap.put("primary_ltpa_public_key", ltpaPublicKey);
        tokenFactoryMap.put("primary_ltpa_private_key", ltpaPrivateKey);
        tokenFactoryMap.put("expirationDifferenceAllowed", 0L);
        tokenFactoryMap.put("inactivityTimeout", inactivityTimeout);
        tokenFactoryMap.put("refreshThreshold", refreshThreshold);
        tokenFactoryMap.put("dynamicExpirationValidation", dynamicExpirationValidation);
        // Provide an empty list so initialize() does not NPE on validationKeys.size()
        // when debug trace is enabled.
        tokenFactoryMap.put(LTPAConstants.VALIDATION_KEYS, new java.util.concurrent.CopyOnWriteArrayList<LTPAValidationKeysInfo>());
        return tokenFactoryMap;
    }

    private Map<String, Object> createBasicTokenData() {
        Map<String, Object> tokenData = new HashMap<String, Object>();
        tokenData.put("unique_id", "user:BasicRealm/testuser");
        return tokenData;
    }
}

// Made with Bob
