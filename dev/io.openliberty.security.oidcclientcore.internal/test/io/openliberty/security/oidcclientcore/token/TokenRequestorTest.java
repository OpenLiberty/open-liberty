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
package io.openliberty.security.oidcclientcore.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;
import test.common.SharedOutputManager;

public class TokenRequestorTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String tokenEndpoint = "http://some-domain.com/path/token";
    private static final String tokenEndpointSecure = "https://some-domain.com/path/token";
    private static final String clientId = "myClientId";
    private static final String clientSecret = "myClientSecret";
    private static final String redirectUri = "http://redirect-uri.com/some/path";
    private static final String code = "abc123";

    private static final String accessToken = "qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw";
    private static final String tokenType = "bearer";
    private static final Long expiresIn = 3599L;
    private static final String scope = "openid profile";
    private static final String refreshToken = "QGCYpfziPZY2saAagbsf5jxbMucqcF3743euknBxzkUlof7uSv";
    private static final String idToken = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMS9vYXV0aDIvZW5kcG9pbnQvT0F1dGhDb25maWdTYW1wbGUvdG9rZW4iLCJpYXQiOjEzODczODM5NTMsInN1YiI6InRlc3R1c2VyIiwiZXhwIjoxMzg3Mzg3NTUzLCJhdWQiOiJjbGllbnQwMSJ9.ottD3eYa6qrnItRpL_Q9UaKumAyo14LnlvwnyF3Kojk";
    private static final String tokenResponseEntity = "{\"access_token\":\"" + accessToken + "\",\"token_type\":\"" + tokenType + "\",\"expires_in\":" + expiresIn + ",\"scope\":\""
                                                      + scope + "\",\"refresh_token\":\"" + refreshToken + "\",\"id_token\":\"" + idToken + "\"}";

    private static final Map<String, Object> postResponseMap;

    static {
        postResponseMap = new HashMap<String, Object>();
        postResponseMap.put(TokenConstants.ACCESS_TOKEN, accessToken);
        postResponseMap.put(TokenConstants.TOKEN_TYPE, tokenType);
        postResponseMap.put(TokenConstants.EXPIRES_IN, expiresIn);
        postResponseMap.put(TokenConstants.SCOPE, scope);
        postResponseMap.put(TokenConstants.REFRESH_TOKEN, refreshToken);
        postResponseMap.put(TokenConstants.ID_TOKEN, idToken);
    }

    private final OidcClientHttpUtil oidcClientHttpUtil = mockery.mock(OidcClientHttpUtil.class);
    private final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class);

    private List<NameValuePair> params;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(TokenConstants.GRANT_TYPE, TokenConstants.AUTHORIZATION_CODE));
        params.add(new BasicNameValuePair(TokenConstants.REDIRECT_URI, redirectUri));
        params.add(new BasicNameValuePair(TokenConstants.CODE, code));
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_requestTokens() throws Exception {
        TokenRequestor tokenRequestor = new TokenRequestor.Builder(tokenEndpoint, clientId, clientSecret, redirectUri, code).build();
        tokenRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).postToEndpoint(tokenEndpoint, params, clientId, clientSecret, null, null, false, TokenConstants.METHOD_BASIC, false);
                will(returnValue(postResponseMap));
                one(oidcClientHttpUtil).extractEntityFromTokenResponse(postResponseMap);
                will(returnValue(tokenResponseEntity));
            }
        });

        TokenResponse tokenResponse = tokenRequestor.requestTokens();

        assertEquals(accessToken, tokenResponse.getAccessTokenString());
        assertEquals(refreshToken, tokenResponse.getRefreshTokenString());
        assertEquals(idToken, tokenResponse.getIdTokenString());
        assertNotNull("The token response generation time must be set.", tokenResponse.getResponseGenerationTime());
    }

    @Test
    public void test_requestTokens_secure() throws Exception {
        TokenRequestor tokenRequestor = new TokenRequestor.Builder(tokenEndpointSecure, clientId, clientSecret, redirectUri, code).sslSocketFactory(sslSocketFactory).build();
        tokenRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).postToEndpoint(tokenEndpointSecure, params, clientId, clientSecret, null, sslSocketFactory, false,
                                                       TokenConstants.METHOD_BASIC, false);
                will(returnValue(postResponseMap));
                one(oidcClientHttpUtil).extractEntityFromTokenResponse(postResponseMap);
                will(returnValue(tokenResponseEntity));
            }
        });

        TokenResponse tokenResponse = tokenRequestor.requestTokens();

        assertEquals(accessToken, tokenResponse.getAccessTokenString());
        assertEquals(refreshToken, tokenResponse.getRefreshTokenString());
        assertEquals(idToken, tokenResponse.getIdTokenString());
    }

    @Test
    public void test_requestTokens_useHostnameVerification() throws Exception {
        TokenRequestor tokenRequestor = new TokenRequestor.Builder(tokenEndpoint, clientId, clientSecret, redirectUri, code).isHostnameVerification(true).build();
        tokenRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).postToEndpoint(tokenEndpoint, params, clientId, clientSecret, null, null, true, TokenConstants.METHOD_BASIC, false);
                will(returnValue(postResponseMap));
                one(oidcClientHttpUtil).extractEntityFromTokenResponse(postResponseMap);
                will(returnValue(tokenResponseEntity));
            }
        });

        TokenResponse tokenResponse = tokenRequestor.requestTokens();

        assertEquals(accessToken, tokenResponse.getAccessTokenString());
        assertEquals(refreshToken, tokenResponse.getRefreshTokenString());
        assertEquals(idToken, tokenResponse.getIdTokenString());
    }

    @Test
    public void test_requestTokens_authMethodPost() throws Exception {
        TokenRequestor tokenRequestor = new TokenRequestor.Builder(tokenEndpoint, clientId, clientSecret, redirectUri, code).authMethod(TokenConstants.METHOD_POST).build();
        tokenRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        params.add(new BasicNameValuePair(TokenConstants.CLIENT_ID, clientId));
        params.add(new BasicNameValuePair(TokenConstants.CLIENT_SECRET, clientSecret));

        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).postToEndpoint(tokenEndpoint, params, clientId, clientSecret, null, null, false, TokenConstants.METHOD_POST, false);
                will(returnValue(postResponseMap));
                one(oidcClientHttpUtil).extractEntityFromTokenResponse(postResponseMap);
                will(returnValue(tokenResponseEntity));
            }
        });

        TokenResponse tokenResponse = tokenRequestor.requestTokens();

        assertEquals(accessToken, tokenResponse.getAccessTokenString());
        assertEquals(refreshToken, tokenResponse.getRefreshTokenString());
        assertEquals(idToken, tokenResponse.getIdTokenString());
    }

    @Test
    public void test_requestTokens_customParams() throws Exception {
        HashMap<String, String> customParams = new HashMap<>();
        customParams.put("customkey1", "customvalue1");
        customParams.put("customkey2", "customvalue2");

        TokenRequestor tokenRequestor = new TokenRequestor.Builder(tokenEndpoint, clientId, clientSecret, redirectUri, code).customParams(customParams).build();
        tokenRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        params.add(new BasicNameValuePair("customkey1", "customvalue1"));
        params.add(new BasicNameValuePair("customkey2", "customvalue2"));

        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).postToEndpoint(tokenEndpoint, params, clientId, clientSecret, null, null, false, TokenConstants.METHOD_BASIC, false);
                will(returnValue(postResponseMap));
                one(oidcClientHttpUtil).extractEntityFromTokenResponse(postResponseMap);
                will(returnValue(tokenResponseEntity));
            }
        });

        TokenResponse tokenResponse = tokenRequestor.requestTokens();

        assertEquals(accessToken, tokenResponse.getAccessTokenString());
        assertEquals(refreshToken, tokenResponse.getRefreshTokenString());
        assertEquals(idToken, tokenResponse.getIdTokenString());
    }

    @Test
    public void test_requestTokens_useSystemPropertiesForHttpClientConnections() throws Exception {
        TokenRequestor tokenRequestor = new TokenRequestor.Builder(tokenEndpoint, clientId, clientSecret, redirectUri, code).useSystemPropertiesForHttpClientConnections(true).build();

        tokenRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).postToEndpoint(tokenEndpoint, params, clientId, clientSecret, null, null, false, TokenConstants.METHOD_BASIC, true);
                will(returnValue(postResponseMap));
                one(oidcClientHttpUtil).extractEntityFromTokenResponse(postResponseMap);
                will(returnValue(tokenResponseEntity));
            }
        });

        TokenResponse tokenResponse = tokenRequestor.requestTokens();

        assertEquals(accessToken, tokenResponse.getAccessTokenString());
        assertEquals(refreshToken, tokenResponse.getRefreshTokenString());
        assertEquals(idToken, tokenResponse.getIdTokenString());
    }

    @Test
    public void test_requestTokens_multipleBuilderMethodsUsed() throws Exception {
        TokenRequestor tokenRequestor = new TokenRequestor.Builder(tokenEndpointSecure, clientId, clientSecret, redirectUri, code).sslSocketFactory(sslSocketFactory).isHostnameVerification(true).authMethod(TokenConstants.METHOD_POST).useSystemPropertiesForHttpClientConnections(true).build();

        tokenRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        params.add(new BasicNameValuePair(TokenConstants.CLIENT_ID, clientId));
        params.add(new BasicNameValuePair(TokenConstants.CLIENT_SECRET, clientSecret));

        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).postToEndpoint(tokenEndpointSecure, params, clientId, clientSecret, null, sslSocketFactory, true, TokenConstants.METHOD_POST, true);
                will(returnValue(postResponseMap));
                one(oidcClientHttpUtil).extractEntityFromTokenResponse(postResponseMap);
                will(returnValue(tokenResponseEntity));
            }
        });

        TokenResponse tokenResponse = tokenRequestor.requestTokens();

        assertEquals(accessToken, tokenResponse.getAccessTokenString());
        assertEquals(refreshToken, tokenResponse.getRefreshTokenString());
        assertEquals(idToken, tokenResponse.getIdTokenString());
    }

}
