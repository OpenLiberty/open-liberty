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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Map;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.Before;
import org.junit.Test;

import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.identitystore.openid.IdentityToken;

public class IdentityTokenImplTest {

    private static final String JWT_ID_TOKEN_STRING = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJKYWNrc29uIiwiYXRfaGFzaCI6ImJrR0NWcy1EcndMMGMycEtoN0ZVNGciLCJyZWFsbU5hbWUiOiJCYXNpY1JlZ2lzdHJ5IiwidW5pcXVlU2VjdXJpdHlOYW1lIjoiSmFja3NvbiIsInNpZCI6InFTTzBXeWs0VVNjMWFCYlMyUVlmIiwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTQ0My9vaWRjL2VuZHBvaW50L09QIiwiYXVkIjoib2lkY2NsaWVudCIsImV4cCI6MTY2MTIwNzIwOCwiaWF0IjoxNjYxMjAwMDA4fQ.a4PRKYeG18vsmBOukcjmNve10KnVSBGVgwh2RqXkNbY";
    protected static final String SUBJECT_IN_ID_TOKEN = "Jackson";
    public static final Long TOKEN_MIN_VALIDITY_10_MILLIS = Long.valueOf(10000);

    private Map<String, Object> idTokenClaimsMap;
    private IdentityToken identityToken;

    @Before
    public void setUp() throws Exception {
        idTokenClaimsMap = createClaimsMap(JWT_ID_TOKEN_STRING);
        identityToken = new IdentityTokenImpl(JWT_ID_TOKEN_STRING, idTokenClaimsMap, TOKEN_MIN_VALIDITY_10_MILLIS);
    }

    public static Map<String, Object> createClaimsMap(String tokenString) throws UnsupportedEncodingException, InvalidJwtException {
        String[] parts = tokenString.split(("\\."));
        String claimsAsJsonString = new String(Base64.getDecoder().decode(parts[1]), "UTF-8");
        return org.jose4j.jwt.JwtClaims.parse(claimsAsJsonString).getClaimsMap();
    }

    @Test
    public void testGetToken() {
        assertEquals("The identity token must be set.", JWT_ID_TOKEN_STRING, identityToken.getToken());
    }

    @Test
    public void testGetJwtClaims() {
        assertNotNull("The JwtClaims must be set.", identityToken.getJwtClaims());
    }

    @Test
    public void testIsExpired_expired() {
        assertTrue("The identity token must be expired.", identityToken.isExpired());
    }

    @Test
    public void testGetClaims() {
        Map<String, Object> claims = identityToken.getClaims();
        assertNotNull("The claims must be set.", claims);
        assertTrue("All the claims must be returned.", idTokenClaimsMap.entrySet().containsAll(claims.entrySet()));
    }

    @Test
    public void testGetClaims_immutable() {
        Map<String, Object> claims = identityToken.getClaims();
        assertEquals("The current 'sub' claim must be set.", SUBJECT_IN_ID_TOKEN, identityToken.getClaims().get(OpenIdConstant.SUBJECT_IDENTIFIER));
        claims.put(OpenIdConstant.SUBJECT_IDENTIFIER, "anotherSubject");
        assertEquals("The current 'sub' claim must not be modified.", SUBJECT_IN_ID_TOKEN, identityToken.getClaims().get(OpenIdConstant.SUBJECT_IDENTIFIER));
    }

}
