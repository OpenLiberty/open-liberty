/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.security.Key;
import java.security.PublicKey;
import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.test.common.jwt.utils.JwtUnitTestUtils;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import io.openliberty.security.common.jwt.exceptions.SignatureAlgorithmNotInAllowedList;

/**
 *
 */
public class JwtUtilsTest {

    private final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final OidcServerConfig oidcServerConfig = context.mock(OidcServerConfig.class);
    private final OAuth20Provider oauth20Provider = context.mock(OAuth20Provider.class);
    private final OidcOAuth20ClientProvider oidcoauth20clientprovider = context.mock(OidcOAuth20ClientProvider.class);
    private final JwtContext jwtContext = context.mock(JwtContext.class);
    private final JwtClaims jwtClaims = context.mock(JwtClaims.class);
    private final JsonWebStructure jws = context.mock(JsonWebStructure.class);
    private final JSONWebKey jsonWebKey = context.mock(JSONWebKey.class);
    private final PublicKey publicKey = context.mock(PublicKey.class);

    @Test
    public void test_getJwtVerificationKey_algNotAllowed() throws Exception {
        JSONObject header = new JSONObject();
        header.put("alg", "HS256");
        JSONObject claims = new JSONObject();
        claims.put("aud", "client-id");
        String tokenString = JwtUnitTestUtils.encode(header) + "." + JwtUnitTestUtils.encode(claims) + ".";

        context.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("RS256"));
            }
        });
        try {
            Object key = JwtUtils.getJwtVerificationKey(tokenString, oauth20Provider, oidcServerConfig);
            fail("Should have thrown an exception, but got: " + key);
        } catch (SignatureAlgorithmNotInAllowedList e) {
            // Expected
        }
    }

    @Test
    public void test_getJwtVerificationKey_unsigned() throws Exception {
        JSONObject header = new JSONObject();
        header.put("alg", "none");
        JSONObject claims = new JSONObject();
        claims.put("aud", "client-id");
        String tokenString = JwtUnitTestUtils.encode(header) + "." + JwtUnitTestUtils.encode(claims) + ".";

        context.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("none"));
            }
        });
        Object key = JwtUtils.getJwtVerificationKey(tokenString, oauth20Provider, oidcServerConfig);
        assertNull("Should not have returned a key, but got: " + key, key);
    }

    @Test
    public void test_getJwtVerificationKey_hs256() throws Exception {
        String clientId = "client-id";
        String clientSecret = "client-secret";
        JSONObject claims = new JSONObject();
        claims.put("aud", clientId);
        String tokenString = JwtUnitTestUtils.getHS256Jws(claims, clientSecret);
        final OidcBaseClient oidcbaseclient = new OidcBaseClient(clientId, clientSecret, null, "clientName", "componentId", true);

        context.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("HS256"));
                one(oauth20Provider).getClientProvider();
                will(returnValue(oidcoauth20clientprovider));
                one(oidcoauth20clientprovider).get(clientId);
                will(returnValue(oidcbaseclient));
            }
        });
        Object key = JwtUtils.getJwtVerificationKey(tokenString, oauth20Provider, oidcServerConfig);
        assertEquals("Returned key should have matched the expected client, but didn't.", clientSecret, key);
    }

    // TODO

    @Test
    public void test_getSharedKey_missingAud() throws Exception {
        context.checking(new Expectations() {
            {
                one(jwtContext).getJwtClaims();
                will(returnValue(jwtClaims));
                one(jwtClaims).getAudience();
                will(returnValue(null));
            }
        });
        Object key = JwtUtils.getSharedKey(jwtContext, oauth20Provider);
        assertNull("Should not have returned a key, but got: " + key, key);
    }

    @Test
    public void test_getSharedKey_multipleAud() throws Exception {
        context.checking(new Expectations() {
            {
                one(jwtContext).getJwtClaims();
                will(returnValue(jwtClaims));
                one(jwtClaims).getAudience();
                will(returnValue(Arrays.asList("client1", "client2")));
            }
        });
        Object key = JwtUtils.getSharedKey(jwtContext, oauth20Provider);
        assertNull("Should not have returned a key, but got: " + key, key);
    }

    @Test
    public void test_getSharedKey_noClientMatchingAud() throws Exception {
        String audValue = "the-aud";
        context.checking(new Expectations() {
            {
                one(jwtContext).getJwtClaims();
                will(returnValue(jwtClaims));
                one(jwtClaims).getAudience();
                will(returnValue(Arrays.asList(audValue)));
                one(oauth20Provider).getClientProvider();
                will(returnValue(oidcoauth20clientprovider));
                one(oidcoauth20clientprovider).get(audValue);
                will(returnValue(null));
            }
        });
        Object key = JwtUtils.getSharedKey(jwtContext, oauth20Provider);
        assertNull("Should not have returned a key, but got: " + key, key);
    }

    @Test
    public void test_getSharedKey_clientMatchesAud() throws Exception {
        String clientId = "the-aud";
        String clientSecret = "client-secret";
        final OidcBaseClient oidcbaseclient = new OidcBaseClient(clientId, clientSecret, null, "clientName", "componentId", true);

        context.checking(new Expectations() {
            {
                one(jwtContext).getJwtClaims();
                will(returnValue(jwtClaims));
                one(jwtClaims).getAudience();
                will(returnValue(Arrays.asList(clientId)));
                one(oauth20Provider).getClientProvider();
                will(returnValue(oidcoauth20clientprovider));
                one(oidcoauth20clientprovider).get(clientId);
                will(returnValue(oidcbaseclient));
            }
        });
        Object key = JwtUtils.getSharedKey(jwtContext, oauth20Provider);
        assertEquals("Returned key should have matched the expected client, but didn't.", clientSecret, key);
    }

    @Test
    public void test_getPublicKeyFromJsonWebStructure_serverMissingJwk() {
        context.checking(new Expectations() {
            {
                one(jws).getAlgorithmHeaderValue();
                one(jws).getKeyIdHeaderValue();
                one(jws).getX509CertSha1ThumbprintHeaderValue();
                one(oidcServerConfig).getJSONWebKey();
                will(returnValue(null));
            }
        });
        Key key = JwtUtils.getPublicKeyFromJsonWebStructure(jws, oidcServerConfig);
        assertNull("Should not have returned a key, but got: " + key, key);
    }

    @Test
    public void test_getPublicKeyFromJsonWebStructure_algMismatch() {
        context.checking(new Expectations() {
            {
                one(jws).getAlgorithmHeaderValue();
                will(returnValue("HS256"));
                one(jws).getKeyIdHeaderValue();
                one(jws).getX509CertSha1ThumbprintHeaderValue();
                one(oidcServerConfig).getJSONWebKey();
                will(returnValue(jsonWebKey));
                one(jsonWebKey).getAlgorithm();
                will(returnValue("RS256"));
            }
        });
        Key key = JwtUtils.getPublicKeyFromJsonWebStructure(jws, oidcServerConfig);
        assertNull("Should not have returned a key, but got: " + key, key);
    }

    @Test
    public void test_getPublicKeyFromJsonWebStructure_noHeadersMatch() {
        context.checking(new Expectations() {
            {
                one(jws).getAlgorithmHeaderValue();
                will(returnValue("RS256"));
                one(jws).getKeyIdHeaderValue();
                will(returnValue("bad-kid-value"));
                one(jws).getX509CertSha1ThumbprintHeaderValue();
                will(returnValue("bad-x5t-value"));
                one(oidcServerConfig).getJSONWebKey();
                will(returnValue(jsonWebKey));
                one(jsonWebKey).getAlgorithm();
                will(returnValue("RS256"));
                one(jsonWebKey).getKeyID();
                will(returnValue("expected-kid-value"));
                one(jsonWebKey).getKeyX5t();
                will(returnValue("expected-x5t-value"));
            }
        });
        Key key = JwtUtils.getPublicKeyFromJsonWebStructure(jws, oidcServerConfig);
        assertNull("Should not have returned a key, but got: " + key, key);
    }

    @Test
    public void test_getPublicKeyFromJsonWebStructure_kidMatches() {
        context.checking(new Expectations() {
            {
                one(jws).getAlgorithmHeaderValue();
                will(returnValue("RS256"));
                one(jws).getKeyIdHeaderValue();
                will(returnValue("expected-kid-value"));
                one(jws).getX509CertSha1ThumbprintHeaderValue();
                will(returnValue("bad-x5t-value"));
                one(oidcServerConfig).getJSONWebKey();
                will(returnValue(jsonWebKey));
                one(jsonWebKey).getAlgorithm();
                will(returnValue("RS256"));
                one(jsonWebKey).getKeyID();
                will(returnValue("expected-kid-value"));
                one(jsonWebKey).getPublicKey();
                will(returnValue(publicKey));
            }
        });
        Key key = JwtUtils.getPublicKeyFromJsonWebStructure(jws, oidcServerConfig);
        assertEquals("Returned key did not match the expected public key.", publicKey, key);
    }

    @Test
    public void test_getPublicKeyFromJsonWebStructure_x5tMatches() {
        context.checking(new Expectations() {
            {
                one(jws).getAlgorithmHeaderValue();
                will(returnValue("RS256"));
                one(jws).getKeyIdHeaderValue();
                will(returnValue("bad-kid-value"));
                one(jws).getX509CertSha1ThumbprintHeaderValue();
                will(returnValue("expected-kid-value"));
                one(oidcServerConfig).getJSONWebKey();
                will(returnValue(jsonWebKey));
                one(jsonWebKey).getAlgorithm();
                will(returnValue("RS256"));
                one(jsonWebKey).getKeyID();
                will(returnValue("expected-kid-value"));
                one(jsonWebKey).getKeyX5t();
                will(returnValue("expected-kid-value"));
                one(jsonWebKey).getPublicKey();
                will(returnValue(publicKey));
            }
        });
        Key key = JwtUtils.getPublicKeyFromJsonWebStructure(jws, oidcServerConfig);
        assertEquals("Returned key did not match the expected public key.", publicKey, key);
    }

    @Test
    public void test_getPublicKeyFromJsonWebStructure_kidAndX5tNotSpecified() {
        context.checking(new Expectations() {
            {
                one(jws).getAlgorithmHeaderValue();
                will(returnValue("RS256"));
                one(jws).getKeyIdHeaderValue();
                will(returnValue(null));
                one(jws).getX509CertSha1ThumbprintHeaderValue();
                will(returnValue(null));
                one(oidcServerConfig).getJSONWebKey();
                will(returnValue(jsonWebKey));
                one(jsonWebKey).getAlgorithm();
                will(returnValue("RS256"));
                one(jsonWebKey).getPublicKey();
                will(returnValue(publicKey));
            }
        });
        Key key = JwtUtils.getPublicKeyFromJsonWebStructure(jws, oidcServerConfig);
        assertEquals("Returned key did not match the expected public key.", publicKey, key);
    }

}
