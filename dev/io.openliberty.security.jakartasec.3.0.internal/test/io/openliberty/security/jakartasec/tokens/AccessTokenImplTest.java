/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.tokens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.AccessToken.Type;
import jakarta.security.enterprise.identitystore.openid.JwtClaims;

/**
 *
 */
public class AccessTokenImplTest {

    private static final String OPAQUE_TOKEN = "12345670";
    private static final String CLAIM_NAME = "claim";
    private static final Long ONE_HOUR = Long.valueOf(3600);
    private static final Instant NOW_INSTANT = Instant.now();
    private final Instant AN_HOUR_AGO = NOW_INSTANT.minusSeconds(ONE_HOUR);
    private final Instant THIRTY_MINS_AGO = NOW_INSTANT.minusSeconds(1800);
    private final Instant A_MINUTE_AGO = Instant.now().minusSeconds(60);
    private final Long TOKEN_MIN_VALIDITY_10_MILLIS = Long.valueOf(10000);
    private final Long TOKEN_MIN_VALIDITY_45_MINS_IN_MILLIS = Long.valueOf(2700000);

    private final AccessToken opaqueAccessToken = new AccessTokenImpl(OPAQUE_TOKEN, A_MINUTE_AGO, ONE_HOUR, TOKEN_MIN_VALIDITY_10_MILLIS);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetToken() {
        assertEquals("The access token must be set.", OPAQUE_TOKEN, opaqueAccessToken.getToken());
    }

//    @Test
//    public void testIsJWT() {
//        fail("Not yet implemented");
//    }
//
    @Test
    public void testGetJwtClaims_opaque() {
        assertEquals("There must not be JWT claims for an opaque access token.", JwtClaims.NONE, opaqueAccessToken.getJwtClaims());
    }

    @Test
    public void testGetClaims_opaque() {
        Map<String, Object> expectedValue = Collections.emptyMap();

        assertEquals("There must not be claims for an opaque access token.", expectedValue, opaqueAccessToken.getClaims());
    }

    @Test
    public void testGetClaim_opaque() {
        assertEquals("There must not be a claim for an opaque access token.", null, opaqueAccessToken.getClaim(CLAIM_NAME));
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
    public void testIsExpired_opaque_expired() {
        AccessToken opaqueAccessToken = new AccessTokenImpl(OPAQUE_TOKEN, AN_HOUR_AGO, ONE_HOUR, TOKEN_MIN_VALIDITY_10_MILLIS);
        assertTrue("The access token must be expired.", opaqueAccessToken.isExpired());
    }

    @Test
    public void testIsExpired_opaque_expiresInNextTokenMinValidityMillis() {
        AccessToken opaqueAccessToken = new AccessTokenImpl(OPAQUE_TOKEN, THIRTY_MINS_AGO, ONE_HOUR, TOKEN_MIN_VALIDITY_45_MINS_IN_MILLIS);
        assertTrue("The access token must be expired if it expires in the next 45 mins.", opaqueAccessToken.isExpired());
    }

//
//    @Test
//    public void testGetScope() {
//        fail("Not yet implemented");
//    }
//
    @Test
    public void testGetType_opaque() {
        assertEquals("The type must be 'Type.MAC' for an opaque access token.", Type.MAC, opaqueAccessToken.getType());
    }

}
