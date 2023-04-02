/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.tokens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.Before;
import org.junit.Test;

import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.AccessToken.Type;
import jakarta.security.enterprise.identitystore.openid.JwtClaims;
import jakarta.security.enterprise.identitystore.openid.Scope;

public class AccessTokenImplTest {

    private static final String JWT_ACCESS_TOKEN_STRING = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJKYWNrc29uIiwiYXRfaGFzaCI6ImJrR0NWcy1EcndMMGMycEtoN0ZVNGciLCJyZWFsbU5hbWUiOiJCYXNpY1JlZ2lzdHJ5IiwidW5pcXVlU2VjdXJpdHlOYW1lIjoiSmFja3NvbiIsInNpZCI6InFTTzBXeWs0VVNjMWFCYlMyUVlmIiwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTQ0My9vaWRjL2VuZHBvaW50L09QIiwiYXVkIjoib2lkY2NsaWVudCIsImV4cCI6MTY2MTIwNzIwOCwiaWF0IjoxNjYxMjAwMDA4fQ.a4PRKYeG18vsmBOukcjmNve10KnVSBGVgwh2RqXkNbY";
    public static final String SUBJECT_IN_ACCESS_TOKEN = "Jackson";
    private static final String OPAQUE_TOKEN = "12345670";
    private static final String CLAIM_NAME = "claim";
    private static final String SCOPE1 = "scope1";
    private static final String SCOPE2 = "scope2";
    public static final Long ONE_HOUR = Long.valueOf(3600);
    private static final Instant NOW_INSTANT = Instant.now();
    private final Instant AN_HOUR_AGO = NOW_INSTANT.minusSeconds(ONE_HOUR);
    private final Instant THIRTY_MINS_AGO = NOW_INSTANT.minusSeconds(1800);
    public static final Instant A_MINUTE_AGO = Instant.now().minusSeconds(60);
    public static final Long TOKEN_MIN_VALIDITY_10_MILLIS = Long.valueOf(10000);
    private final Long TOKEN_MIN_VALIDITY_45_MINS_IN_MILLIS = Long.valueOf(2700000);

    protected final AccessToken opaqueAccessToken = new AccessTokenImpl(OPAQUE_TOKEN, A_MINUTE_AGO, ONE_HOUR, TOKEN_MIN_VALIDITY_10_MILLIS);
    private Map<String, Object> accessTokenClaimsMap;
    private AccessToken jwtAccessToken;

    @Before
    public void setUp() throws Exception {
        accessTokenClaimsMap = createClaimsMap(JWT_ACCESS_TOKEN_STRING);
        jwtAccessToken = new AccessTokenImpl(JWT_ACCESS_TOKEN_STRING, accessTokenClaimsMap, A_MINUTE_AGO, ONE_HOUR, TOKEN_MIN_VALIDITY_10_MILLIS);
    }

    public static Map<String, Object> createClaimsMap(String tokenString) throws UnsupportedEncodingException, InvalidJwtException {
        String[] parts = tokenString.split(("\\."));
        String claimsAsJsonString = new String(Base64.getDecoder().decode(parts[1]), "UTF-8");
        return org.jose4j.jwt.JwtClaims.parse(claimsAsJsonString).getClaimsMap();
    }

    @Test
    public void testGetToken() {
        assertEquals("The access token must be set.", OPAQUE_TOKEN, opaqueAccessToken.getToken());
    }

    @Test
    public void testGetToken_jwt() {
        assertEquals("The access token must be set.", JWT_ACCESS_TOKEN_STRING, jwtAccessToken.getToken());
    }

    @Test
    public void testIsJWT_opaque() {
        assertFalse("The access token must not be a JWT for an opaque access token.", opaqueAccessToken.isJWT());
    }

    @Test
    public void testIsJWT_jwt() {
        assertTrue("The access token must be a JWT for an jwt access token.", jwtAccessToken.isJWT());
    }

    @Test
    public void testGetJwtClaims_opaque() {
        assertEquals("There must not be JWT claims for an opaque access token.", JwtClaims.NONE, opaqueAccessToken.getJwtClaims());
    }

    @Test
    public void testGetJwtClaims_jwt() {
        JwtClaims jwtClaims = jwtAccessToken.getJwtClaims();

        assertNotNull("The JwtClaims must be set.", jwtClaims);
        assertFalse("The JwtClaims must not be JwtClaims.NONE.", JwtClaims.NONE.equals(jwtClaims));
    }

    @Test
    public void testGetClaims_opaque() {
        Map<String, Object> expectedValue = Collections.emptyMap();

        assertEquals("There must not be claims for an opaque access token.", expectedValue, opaqueAccessToken.getClaims());
    }

    @Test
    public void testGetClaims_jwt() {
        Map<String, Object> claims = jwtAccessToken.getClaims();

        assertNotNull("The claims must be set.", claims);
        assertTrue("All the claims must be returned.", accessTokenClaimsMap.entrySet().containsAll(claims.entrySet()));
    }

    @Test
    public void testGetClaim_opaque() {
        assertNull("There must not be a claim for an opaque access token.", opaqueAccessToken.getClaim(CLAIM_NAME));
    }

    @Test
    public void testGetClaim_jwt() {
        assertEquals("There must be be a claim for an jwt access token.", SUBJECT_IN_ACCESS_TOKEN, jwtAccessToken.getClaim(OpenIdConstant.SUBJECT_IDENTIFIER));
    }

    @Test
    public void testGetExpirationTime() {
        assertEquals("The expiration time must be set.", ONE_HOUR, opaqueAccessToken.getExpirationTime());
    }

    @Test
    public void testIsExpired_opaque_notExpired() {
        assertFalse("The access token must not be expired.", opaqueAccessToken.isExpired());
    }

    @Test
    public void testIsExpired_jwt_notExpired() {
        assertFalse("The access token must not be expired.", jwtAccessToken.isExpired());
    }

    @Test
    public void testIsExpired_jwt_expired() {
        jwtAccessToken = new AccessTokenImpl(JWT_ACCESS_TOKEN_STRING, accessTokenClaimsMap, A_MINUTE_AGO, null, TOKEN_MIN_VALIDITY_10_MILLIS);
        assertTrue("The access token must be expired.", jwtAccessToken.isExpired());
    }

    @Test
    public void testIsExpired_opaque_expired() {
        AccessToken opaqueAccessToken = new AccessTokenImpl(OPAQUE_TOKEN, AN_HOUR_AGO, ONE_HOUR, TOKEN_MIN_VALIDITY_10_MILLIS);
        assertTrue("The access token must be expired.", opaqueAccessToken.isExpired());
    }

    @Test
    public void testIsExpired_opaque_expiresInNextTokenMinValidityMillis() {
        AccessToken opaqueAccessToken = new AccessTokenImpl(OPAQUE_TOKEN, THIRTY_MINS_AGO, ONE_HOUR, TOKEN_MIN_VALIDITY_45_MINS_IN_MILLIS);
        assertTrue("The access token must be expired if it expires in the next 45 mins.", opaqueAccessToken.isExpired());
    }

    @Test
    public void testGetScope_opaque() {
        assertNull("There must not be a scope for an opaque access token.", opaqueAccessToken.getScope());
    }

    @Test
    public void testGetScope_jwt_scopeClaimNotAvailable() {
        assertNull("There must not be a scope for an jwt access token if scope claim is not available.", jwtAccessToken.getScope());
    }

    @Test
    public void testGetScope_jwt_scopeClaimAvailable() throws InvalidJwtException {
        jwtAccessToken = createAccessTokenWithScopeClaim(SCOPE1);

        Scope scope = jwtAccessToken.getScope();

        assertNotNull("There must be a scope for an jwt access token if scope claim is available.", scope);
        assertTrue("The scope must be set.", scope.contains(SCOPE1));
    }

    @Test
    public void testGetScope_jwt_scopeClaimAvailable_multiple() throws InvalidJwtException {
        jwtAccessToken = createAccessTokenWithScopeClaim("scope1 scope2");

        Scope scope = jwtAccessToken.getScope();

        assertNotNull("There must be a scope for an jwt access token if scope claim is available.", scope);
        assertTrue("The scope must be set with scope1.", scope.contains(SCOPE1));
        assertTrue("The scope must be set with scope2.", scope.contains(SCOPE2));
    }

    @Test
    public void testGetScope_jwt_scopeClaimAvailable_immutable() throws InvalidJwtException {
        jwtAccessToken = createAccessTokenWithScopeClaim(SCOPE1);
        Scope scope = jwtAccessToken.getScope();
        scope.add(SCOPE2);
        scope = jwtAccessToken.getScope();

        assertFalse("The current scope must not be modified in the jwt access token.", scope.contains(SCOPE2));
    }

    private AccessToken createAccessTokenWithScopeClaim(String value) throws InvalidJwtException {
        accessTokenClaimsMap = org.jose4j.jwt.JwtClaims.parse("{\"" + OpenIdConstant.SCOPE + "\":\"" + value + "\"}").getClaimsMap();
        return new AccessTokenImpl(JWT_ACCESS_TOKEN_STRING, accessTokenClaimsMap, A_MINUTE_AGO, ONE_HOUR, TOKEN_MIN_VALIDITY_10_MILLIS);
    }

    @Test
    public void testGetType_opaque() {
        assertEquals("The type must be 'Type.MAC' for an opaque access token.", Type.MAC, opaqueAccessToken.getType());
    }

    @Test
    public void testGetType_jwt() {
        assertEquals("The type must be 'Type.BEARER' for an opaque access token.", Type.BEARER, jwtAccessToken.getType());
    }

}
