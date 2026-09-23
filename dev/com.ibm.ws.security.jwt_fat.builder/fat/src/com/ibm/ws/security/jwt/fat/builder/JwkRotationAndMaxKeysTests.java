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
    /** Sleep duration in ms: 60s matches the 1m rotation timer exactly */
    private static final long ROTATION_SLEEP_MS = 60_000L;

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
     * Verifies the full JWK key lifecycle for jwkMaxKeys=3 and jwkRotationTime=1m:
     *
     * Phase 1 (T=0):   JWK endpoint returns 1 key (lazy init).
     *                  Build JWT signed with Key A. Signature is valid against current JWKS.
     * Phase 2 (T=1m):  After 1st rotation: 2 keys. Key A still present.
     *                  Reuse JWT. Signature still valid.
     * Phase 3 (T=2m):  After 2nd rotation: 3 keys (maxKeys reached). Key A still present.
     *                  Reuse JWT. Signature still valid.
     * Phase 4 (T=3m):  After 3rd rotation: still 3 keys. Key A evicted (sliding window).
     *                  Reuse JWT. Signature is now invalid against current JWKS.
     *
     * Signature verification uses bare jose4j against a live GET of the /jwk endpoint,
     * bypassing Liberty's JWKSet cache (10-minute TTL) entirely.
     */
    @Test
    public void testJwkRotationAndMaxKeys() throws Exception {
        Log.info(thisClass, _testName, "Starting JWK rotation and max keys test");

        String jwkUrl = buildJwkUrl(BUILDER_ID);

        // ── Phase 1: initial state ─────────────────────────────────────────
        Log.info(thisClass, _testName, "Phase 1: checking initial JWK state");
        JsonArray phase1Keys = getJwkKeys(jwkUrl);
        assertEquals("Phase 1: expected exactly 1 key before any rotation", 1, phase1Keys.size());
        Set<String> phase1Kids = extractKids(phase1Keys);

        // Build the JWT once with Key A; reuse it for all subsequent phases
        String jwtToken = buildJwt(BUILDER_ID);
        assertTrue("Phase 1: JWT signed with Key A must be valid against current JWKS",
                verifyJwtSignatureAgainstLiveJwks(jwtToken, jwkUrl));

        // ── Phase 2: after 1st rotation ────────────────────────────────────
        Log.info(thisClass, _testName, "Phase 2: waiting for 1st rotation...");
        Thread.sleep(ROTATION_SLEEP_MS);

        JsonArray phase2Keys = getJwkKeys(jwkUrl);
        assertEquals("Phase 2: expected 2 keys after 1st rotation", 2, phase2Keys.size());
        Set<String> phase2Kids = extractKids(phase2Keys);

        // Key A must still be present (smooth transition: existing tokens stay valid)
        assertTrue("Phase 2: original key (Key A) must still be present after 1st rotation",
                phase2Kids.containsAll(phase1Kids));
        // A new key must have been added
        assertFalse("Phase 2: a new key must have been added after 1st rotation",
                phase1Kids.containsAll(phase2Kids));

        // JWT signed with Key A must still be valid (Key A is still in the JWKS)
        assertTrue("Phase 2: JWT signed with Key A must still be valid while Key A is in the JWKS",
                verifyJwtSignatureAgainstLiveJwks(jwtToken, jwkUrl));

        // ── Phase 3: after 2nd rotation ────────────────────────────────────
        Log.info(thisClass, _testName, "Phase 3: waiting for 2nd rotation...");
        Thread.sleep(ROTATION_SLEEP_MS);

        JsonArray phase3Keys = getJwkKeys(jwkUrl);
        assertEquals("Phase 3: expected 3 keys after 2nd rotation (maxKeys=3 reached)", 3, phase3Keys.size());
        Set<String> phase3Kids = extractKids(phase3Keys);

        // Key A must still be present
        assertTrue("Phase 3: original key (Key A) must still be present after 2nd rotation",
                phase3Kids.containsAll(phase1Kids));

        // JWT signed with Key A must still be valid (Key A is still in the JWKS)
        assertTrue("Phase 3: JWT signed with Key A must still be valid while Key A is in the JWKS",
                verifyJwtSignatureAgainstLiveJwks(jwtToken, jwkUrl));

        // ── Phase 4: after 3rd rotation ────────────────────────────────────
        Log.info(thisClass, _testName, "Phase 4: waiting for 3rd rotation...");
        Thread.sleep(ROTATION_SLEEP_MS);

        JsonArray phase4Keys = getJwkKeys(jwkUrl);
        // Count must stay at maxKeys=3 (sliding window eviction)
        assertEquals("Phase 4: key count must stay at maxKeys=3 after 3rd rotation", 3, phase4Keys.size());
        Set<String> phase4Kids = extractKids(phase4Keys);

        // Key A must have been evicted
        assertFalse("Phase 4: original key (Key A) must have been evicted after 3rd rotation",
                phase4Kids.containsAll(phase1Kids));

        // JWT signed with evicted Key A must now be rejected by the live JWKS
        assertFalse("Phase 4: JWT signed with evicted Key A must be invalid against current JWKS",
                verifyJwtSignatureAgainstLiveJwks(jwtToken, jwkUrl));
    }
}
