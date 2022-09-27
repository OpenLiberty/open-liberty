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
package io.openliberty.security.jakartasec.identitystore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.IdentityToken;
import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.security.enterprise.identitystore.openid.RefreshToken;

public class OpenIdContextImplTest {

    private static final String SUBJECT_IN_ID_TOKEN = "Jackson";
    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final String TOKEN_TYPE_MAC = "MAC";
    private static final Long ONE_HOUR = Long.valueOf(3600);

    private final Mockery mockery = new JUnit4Mockery();

    private AccessToken accessToken;
    private IdentityToken identityToken;
    private RefreshToken refreshToken;
    private OpenIdClaims userinfoClaims;
    private JsonObject providerMetadata;
    private JsonObject jsonObject;
    private OpenIdContext openIdContext;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        accessToken = mockery.mock(AccessToken.class);
        identityToken = mockery.mock(IdentityToken.class);
        refreshToken = mockery.mock(RefreshToken.class);
        userinfoClaims = mockery.mock(OpenIdClaims.class);
        providerMetadata = Json.createObjectBuilder().add(OpenIdConstant.ISSUER, "https://localhost:9443/oidc/endpoint/OP/authorize").build();

        openIdContext = new OpenIdContextImpl(SUBJECT_IN_ID_TOKEN, TOKEN_TYPE_BEARER, accessToken, identityToken, userinfoClaims, providerMetadata);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetSubject() {
        assertEquals("The subject identifier must be set.", SUBJECT_IN_ID_TOKEN, openIdContext.getSubject());
    }

    @Test
    public void testGetTokenType() {
        assertEquals("The token type must be set.", TOKEN_TYPE_BEARER, openIdContext.getTokenType());
    }

    @Test
    public void testGetAccessToken() {
        assertEquals("The access token must be set.", accessToken, openIdContext.getAccessToken());
    }

    @Test
    public void testGetIdentityToken() {
        assertEquals("The identity token must be set.", identityToken, openIdContext.getIdentityToken());
    }

    @Test
    public void testGetRefreshToken_notSet() {
        Optional<RefreshToken> optionalRefreshToken = openIdContext.getRefreshToken();

        assertFalse("The refresh token must not be set.", optionalRefreshToken.isPresent());
    }

    @Test
    public void testGetRefreshToken_set() {
        ((OpenIdContextImpl) openIdContext).setRefreshToken(refreshToken);
        Optional<RefreshToken> optionalRefreshToken = openIdContext.getRefreshToken();

        assertEquals("The refresh token must be set.", refreshToken, optionalRefreshToken.get());
    }

    @Test
    public void testGetExpiresIn_notSet() {
        Optional<Long> optionalExpiresIn = openIdContext.getExpiresIn();

        assertFalse("The 'expires in' must not be set.", optionalExpiresIn.isPresent());
    }

    @Test
    public void testGetExpiresIn_set() {
        ((OpenIdContextImpl) openIdContext).setExpiresIn(ONE_HOUR);
        Optional<Long> optionalExpiresIn = openIdContext.getExpiresIn();

        assertEquals("The 'expires in' must be set.", ONE_HOUR, optionalExpiresIn.get());
    }

//
//    @Test
//    public void testGetClaimsJson() {
//        fail("Not yet implemented");
//    }
//
    @Test
    public void testGetClaims() {
        assertEquals("The userinfo claims must be set.", userinfoClaims, openIdContext.getClaims());
    }

    @Test
    public void testGetProviderMetadata() {
        assertEquals("The provider metadata must be set.", providerMetadata, openIdContext.getProviderMetadata());
    }
//
//    @Test
//    public void testGetStoredValue() {
//        fail("Not yet implemented");
//    }

}
