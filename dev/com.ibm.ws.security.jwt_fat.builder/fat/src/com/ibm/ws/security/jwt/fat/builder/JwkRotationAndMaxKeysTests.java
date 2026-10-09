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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;

import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;
import com.ibm.ws.security.jwt.fat.builder.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;
import com.ibm.ws.security.jwt.fat.builder.utils.JwtBuilderMessageConstants;
import com.ibm.ws.security.jwt.fat.builder.validation.BuilderTestValidationUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT tests for jwkRotationTime and jwkMaxKeys beta attributes on jwtBuilder.
 *
 * Signature verification uses the bare jose4j library directly against a live
 * GET of the /jwk endpoint, bypassing Liberty's JWKSet cache entirely.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwkRotationAndMaxKeysTests extends CommonSecurityFat {

    private static final Class<?> thisClass = JwkRotationAndMaxKeysTests.class;

    /** jwtBuilder server: builds JWT tokens and hosts the /jwk endpoint */
    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;

    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final BuilderTestValidationUtils validationUtils = new BuilderTestValidationUtils();

    private static final String BUILDER_ID = "jwkMaxKeys_3_rotationTime_1m";
    private static final String JWK_URL_PART = "jwt/ibm/api/";
    private static final String JWK_ENDPOINT_SUFFIX = "/jwk";
    /**
     * Offset from the previously observed rotation at which we check that no premature rotation
     * has happened yet (55s — just before the next 1m boundary)
     */
    private static final long PRE_ROTATION_CHECK_MS = 55_000L;
    /** Max time to wait for the next rotation, measured from the previous anchor (1m period + margin) */
    private static final long ROTATION_TIMEOUT_MS = 70_000L;
    /** Interval between polls of the JWK endpoint while waiting for a rotation */
    private static final long POLL_INTERVAL_MS = 1_000L;

    @BeforeClass
    public static void setUp() throws Exception {
        transformApps(builderServer);

        serverTracker.addServer(builderServer);
        skipRestoreServerTracker.addServer(builderServer);
        builderServer.addInstalledAppForValidation(JWTBuilderConstants.JWT_BUILDER_SERVLET);
        builderServer.startServerUsingExpandedConfiguration("server_configTests.xml", CommonWaitForAppChecks.getSecurityReadyMsgs());
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);

        // beta attributes (jwkRotationTime, jwkMaxKeys) emit a warning in GA builds; suppress it
        // CWWKS6059W is emitted by unrelated JWE configs already present in server_configTests.xml
        builderServer.addIgnoredErrors(Arrays.asList(
                JwtBuilderMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE,
                JwtBuilderMessageConstants.CWWKS6055W_BETA_SIGNATURE_ALGORITHM_USED,
                "CWWKS6059W"));
    }

    /**
     * Build the URL for the JWK endpoint of the given builder config.
     */
    private String buildJwkUrl(String builderId) throws Exception {
        return SecurityFatHttpUtils.getServerIpUrlBase(builderServer) + JWK_URL_PART + builderId + JWK_ENDPOINT_SUFFIX;
    }

    /**
     * Call the JWK endpoint and return the "keys" JSON array.
     * Also asserts that the response is HTTP 200 and contains a "keys" field.
     */
    private JsonArray getJwkKeys(String jwkUrl) throws Exception {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, "\"keys\"", "JWK endpoint response did not contain a 'keys' field."));

        Page response = actions.invokeUrl(_testName, jwkUrl);
        validationUtils.validateResult(response, expectations);

        String body = WebResponseUtils.getResponseText(response);
        return Json.createReader(new StringReader(body)).readObject().getJsonArray("keys");
    }

    /**
     * Extract the set of "kid" values from a JWK keys array.
     * Each kid uniquely identifies a key pair; comparing kid sets across
     * rotations tells us which keys were added or evicted.
     */
    private Set<String> extractKids(JsonArray keys) {
        Set<String> kids = new HashSet<>();
        for (JsonObject key : keys.getValuesAs(JsonObject.class)) {
            kids.add(key.getString("kid"));
        }
        return kids;
    }

    /**
     * Build a JWT using the given builder config and return the raw token string.
     */
    private String buildJwt(String builderId) throws Exception {
        JSONObject testSettings = new JSONObject();
        testSettings.put(PayloadConstants.SUBJECT, "testuser");

        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims();
        expectationSettings.put("overrideSettings", testSettings);

        Expectations builderExpectations = BuilderHelpers.createGoodBuilderExpectations(
                JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page builderResponse = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(builderResponse, builderExpectations);

        return BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN);
    }

    /**
     * Fetch the current JWKS from the /jwk endpoint and verify the JWT signature
     * directly using the bare jose4j library — no Liberty JWKSet cache involved.
     *
     * @return true if the JWT signature is valid against the current JWKS, false otherwise
     */
    private boolean verifyJwtSignatureAgainstLiveJwks(String jwtToken, String jwkUrl) throws Exception {
        // Fetch the live JWKS — no caching, every call is a fresh HTTP GET
        Page jwkResponse = actions.invokeUrl(_testName, jwkUrl);
        String jwksJson = WebResponseUtils.getResponseText(jwkResponse);

        JsonWebKeySet jwks = new JsonWebKeySet(jwksJson);
        JwksVerificationKeyResolver resolver = new JwksVerificationKeyResolver(jwks.getJsonWebKeys());

        JwtConsumer consumer = new JwtConsumerBuilder()
                .setVerificationKeyResolver(resolver)
                .setSkipDefaultAudienceValidation()
                .setAllowedClockSkewInSeconds(60)
                .build();

        try {
            consumer.processToClaims(jwtToken);
            return true;
        } catch (InvalidJwtException e) {
            // Covers both: key not found in JWKS (UnresolvableKeyException)
            // and: key found but signature verification failed (InvalidJwtSignatureException)
            return false;
        }
    }

    /**
     * Poll the JWK endpoint until its set of kids differs from previousKids.
     * Fails the test if no change is observed before the deadline.
     *
     * @return the keys array returned by the first poll that observed the change
     */
    private JsonArray waitForKidsChange(String jwkUrl, Set<String> previousKids, long deadlineMs, String phase) throws Exception {
        while (System.currentTimeMillis() < deadlineMs) {
            JsonArray keys = getJwkKeys(jwkUrl);
            if (!extractKids(keys).equals(previousKids)) {
                return keys;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        fail(phase + ": JWK keys did not rotate before the deadline");
        return null; // unreachable
    }

    /**
     * Sleep until the given absolute time (System.currentTimeMillis() based). Returns immediately if already passed.
     */
    private void sleepUntil(long timeMs) throws InterruptedException {
        long remaining = timeMs - System.currentTimeMillis();
        if (remaining > 0) {
            Thread.sleep(remaining);
        }
    }

    /**
     * Verifies the full JWK key lifecycle for jwkMaxKeys=3 and jwkRotationTime=1m:
     *
     * Phase 0 (T=0):   JWK endpoint returns 1 key (lazy init).
     *                  Build JWT signed with Key A. Signature is valid against current JWKS.
     * Phase 1 (T=1m):  After 1st rotation: 2 keys. Key A still present.
     *                  Reuse JWT. Signature still valid.
     * Phase 2 (T=2m):  After 2nd rotation: 3 keys (maxKeys reached). Key A still present.
     *                  Reuse JWT. Signature still valid.
     * Phase 3 (T=3m):  After 3rd rotation: still 3 keys. Key A evicted (sliding window).
     *                  Reuse JWT. Signature is now invalid against current JWKS.
     *
     * The rotation timer starts when the JWKProvider is created (server startup), not at the
     * first /jwk request, so the offset between test T=0 and the timer is unknown. Therefore:
     * - The 1st rotation is detected by polling, and the observed time becomes the anchor.
     * - Before each subsequent rotation, the JWKS is checked at anchor+55s to be unchanged
     *   (no premature rotation), then polled until the rotation is observed (new anchor).
     *
     * Signature verification uses bare jose4j against a live GET of the /jwk endpoint,
     * bypassing Liberty's JWKSet cache (10-minute TTL) entirely.
     */
    @Test
    public void testJwkRotationAndMaxKeys() throws Exception {
        Log.info(thisClass, _testName, "Starting JWK rotation and max keys test");

        String jwkUrl = buildJwkUrl(BUILDER_ID);

        // ── Phase 0: initial state ─────────────────────────────────────────
        Log.info(thisClass, _testName, "Phase 0: checking initial JWK state");
        long phase0Time = System.currentTimeMillis();
        JsonArray phase0Keys = getJwkKeys(jwkUrl);
        assertEquals("Phase 0: expected exactly 1 key before any rotation", 1, phase0Keys.size());
        Set<String> phase0Kids = extractKids(phase0Keys);

        // Build the JWT once with Key A; reuse it for all subsequent phases
        String jwtToken = buildJwt(BUILDER_ID);
        assertTrue("Phase 0: JWT signed with Key A must be valid against current JWKS",
                verifyJwtSignatureAgainstLiveJwks(jwtToken, jwkUrl));

        // ── Phase 1: after 1st rotation ────────────────────────────────────
        // The timer offset relative to T=0 is unknown, so poll for the 1st rotation and use it as the anchor
        Log.info(thisClass, _testName, "Phase 1: polling for 1st rotation...");
        JsonArray phase1Keys = waitForKidsChange(jwkUrl, phase0Kids, phase0Time + ROTATION_TIMEOUT_MS, "Phase 1");
        long rotation1Time = System.currentTimeMillis();
        assertEquals("Phase 1: expected 2 keys after 1st rotation", 2, phase1Keys.size());
        Set<String> phase1Kids = extractKids(phase1Keys);

        // Key A must still be present (smooth transition: existing tokens stay valid)
        assertTrue("Phase 1: original key (Key A) must still be present after 1st rotation",
                phase1Kids.containsAll(phase0Kids));
        // A new key must have been added
        assertFalse("Phase 1: a new key must have been added after 1st rotation",
                phase0Kids.containsAll(phase1Kids));

        // JWT signed with Key A must still be valid (Key A is still in the JWKS)
        assertTrue("Phase 1: JWT signed with Key A must still be valid while Key A is in the JWKS",
                verifyJwtSignatureAgainstLiveJwks(jwtToken, jwkUrl));

        // ── Phase 2: after 2nd rotation ────────────────────────────────────
        Log.info(thisClass, _testName, "Phase 2: waiting until " + (PRE_ROTATION_CHECK_MS / 1000) + "s after 1st rotation to check for premature rotation...");
        sleepUntil(rotation1Time + PRE_ROTATION_CHECK_MS);
        JsonArray prePhase2Keys = getJwkKeys(jwkUrl);
        assertEquals("Pre-Phase 2: kids must not change before 2nd rotation", phase1Kids, extractKids(prePhase2Keys));

        Log.info(thisClass, _testName, "Phase 2: polling for 2nd rotation...");
        JsonArray phase2Keys = waitForKidsChange(jwkUrl, phase1Kids, rotation1Time + ROTATION_TIMEOUT_MS, "Phase 2");
        long rotation2Time = System.currentTimeMillis();
        assertEquals("Phase 2: expected 3 keys after 2nd rotation (maxKeys=3 reached)", 3, phase2Keys.size());
        Set<String> phase2Kids = extractKids(phase2Keys);

        // Key A must still be present
        assertTrue("Phase 2: original key (Key A) must still be present after 2nd rotation",
                phase2Kids.containsAll(phase0Kids));
        // Key B must still be present too (all phase-1 keys retained)
        assertTrue("Phase 2: all keys from phase 1 (Key A and Key B) must still be present after 2nd rotation",
                phase2Kids.containsAll(phase1Kids));
        // A new key (Key C) must have been added
        assertFalse("Phase 2: a new key must have been added after 2nd rotation",
                phase1Kids.containsAll(phase2Kids));

        // JWT signed with Key A must still be valid (Key A is still in the JWKS)
        assertTrue("Phase 2: JWT signed with Key A must still be valid while Key A is in the JWKS",
                verifyJwtSignatureAgainstLiveJwks(jwtToken, jwkUrl));

        // ── Phase 3: after 3rd rotation ────────────────────────────────────
        Log.info(thisClass, _testName, "Phase 3: waiting until " + (PRE_ROTATION_CHECK_MS / 1000) + "s after 2nd rotation to check for premature rotation...");
        sleepUntil(rotation2Time + PRE_ROTATION_CHECK_MS);
        JsonArray prePhase3Keys = getJwkKeys(jwkUrl);
        assertEquals("Pre-Phase 3: kids must not change before 3rd rotation", phase2Kids, extractKids(prePhase3Keys));

        Log.info(thisClass, _testName, "Phase 3: polling for 3rd rotation...");
        JsonArray phase3Keys = waitForKidsChange(jwkUrl, phase2Kids, rotation2Time + ROTATION_TIMEOUT_MS, "Phase 3");
        // Count must stay at maxKeys=3 (sliding window eviction)
        assertEquals("Phase 3: key count must stay at maxKeys=3 after 3rd rotation", 3, phase3Keys.size());
        Set<String> phase3Kids = extractKids(phase3Keys);

        // Key A must have been evicted
        assertFalse("Phase 3: original key (Key A) must have been evicted after 3rd rotation",
                phase3Kids.containsAll(phase0Kids));

        // Key B and Key C (phase2 minus Key A) must still be present — eviction is precise
        Set<String> phase2KidsMinusPhase0Kids = new HashSet<>(phase2Kids);
        phase2KidsMinusPhase0Kids.removeAll(phase0Kids);
        assertTrue("Phase 3: keys from phase 2 except Key A (i.e. Key B and Key C) must still be present after 3rd rotation",
                phase3Kids.containsAll(phase2KidsMinusPhase0Kids));

        // A new key (Key D) must have been added
        assertFalse("Phase 3: a new key must have been added after 3rd rotation",
                phase2Kids.containsAll(phase3Kids));

        // JWT signed with evicted Key A must now be rejected by the live JWKS
        assertFalse("Phase 3: JWT signed with evicted Key A must be invalid against current JWKS",
                verifyJwtSignatureAgainstLiveJwks(jwtToken, jwkUrl));
    }
}
