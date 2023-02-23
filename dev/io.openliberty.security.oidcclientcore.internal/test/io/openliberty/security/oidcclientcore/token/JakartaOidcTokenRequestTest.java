/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

import static org.junit.Assert.fail;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import test.common.SharedOutputManager;

public class JakartaOidcTokenRequestTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String CWWKS2416E_TOKEN_REQUEST_ERROR = "CWWKS2416E";
    private static final String CWWKS2429E_TOKEN_RESPONSE_MISSING_PARAMETER = "CWWKS2429E";

    private final OidcClientConfig oidcClientConfig = mockery.mock(OidcClientConfig.class);
    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);

    JakartaOidcTokenRequest tokenRequest;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        tokenRequest = new JakartaOidcTokenRequest(oidcClientConfig, request);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_verifyTokenResponseContainsRequiredParameters_emptyResponse() {
        JSONObject tokenResponseJson = new JSONObject();

        TokenResponse tokenResponse = new TokenResponse(tokenResponseJson);
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getClientId();
                will(returnValue("myClientId"));
            }
        });
        try {
            tokenRequest.verifyTokenResponseContainsRequiredParameters(tokenResponse);
            fail("Should have thrown an exception but didn't.");
        } catch (TokenRequestException e) {
            verifyException(e, CWWKS2416E_TOKEN_REQUEST_ERROR + ".*" + CWWKS2429E_TOKEN_RESPONSE_MISSING_PARAMETER + ".*" + TokenConstants.ACCESS_TOKEN + ",.*"
                               + TokenConstants.TOKEN_TYPE + ",.*" + TokenConstants.ID_TOKEN);
        }
    }

    @Test
    public void test_verifyTokenResponseContainsRequiredParameters_missingAccessToken() {
        JSONObject tokenResponseJson = new JSONObject();
        tokenResponseJson.put(TokenConstants.TOKEN_TYPE, "my token type");
        tokenResponseJson.put(TokenConstants.ID_TOKEN, "my ID token");

        TokenResponse tokenResponse = new TokenResponse(tokenResponseJson);
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getClientId();
                will(returnValue("myClientId"));
            }
        });
        try {
            tokenRequest.verifyTokenResponseContainsRequiredParameters(tokenResponse);
            fail("Should have thrown an exception but didn't.");
        } catch (TokenRequestException e) {
            verifyException(e, CWWKS2416E_TOKEN_REQUEST_ERROR + ".*" + CWWKS2429E_TOKEN_RESPONSE_MISSING_PARAMETER + ".*" + TokenConstants.ACCESS_TOKEN);
            verifyExceptionDoesNotContain(e, TokenConstants.TOKEN_TYPE);
            verifyExceptionDoesNotContain(e, TokenConstants.ID_TOKEN);
        }
    }

    @Test
    public void test_verifyTokenResponseContainsRequiredParameters_missingTokenType() {
        JSONObject tokenResponseJson = new JSONObject();
        tokenResponseJson.put(TokenConstants.ACCESS_TOKEN, "my access token");
        tokenResponseJson.put(TokenConstants.ID_TOKEN, "my ID token");

        TokenResponse tokenResponse = new TokenResponse(tokenResponseJson);
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getClientId();
                will(returnValue("myClientId"));
            }
        });
        try {
            tokenRequest.verifyTokenResponseContainsRequiredParameters(tokenResponse);
            fail("Should have thrown an exception but didn't.");
        } catch (TokenRequestException e) {
            verifyException(e, CWWKS2416E_TOKEN_REQUEST_ERROR + ".*" + CWWKS2429E_TOKEN_RESPONSE_MISSING_PARAMETER + ".*" + TokenConstants.TOKEN_TYPE);
            verifyExceptionDoesNotContain(e, TokenConstants.ACCESS_TOKEN);
            verifyExceptionDoesNotContain(e, TokenConstants.ID_TOKEN);
        }
    }

    @Test
    public void test_verifyTokenResponseContainsRequiredParameters_missingIdToken() {
        JSONObject tokenResponseJson = new JSONObject();
        tokenResponseJson.put(TokenConstants.ACCESS_TOKEN, "my access token");
        tokenResponseJson.put(TokenConstants.TOKEN_TYPE, "my token type");

        TokenResponse tokenResponse = new TokenResponse(tokenResponseJson);
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getClientId();
                will(returnValue("myClientId"));
            }
        });
        try {
            tokenRequest.verifyTokenResponseContainsRequiredParameters(tokenResponse);
            fail("Should have thrown an exception but didn't.");
        } catch (TokenRequestException e) {
            verifyException(e, CWWKS2416E_TOKEN_REQUEST_ERROR + ".*" + CWWKS2429E_TOKEN_RESPONSE_MISSING_PARAMETER + ".*" + TokenConstants.ID_TOKEN);
            verifyExceptionDoesNotContain(e, TokenConstants.ACCESS_TOKEN);
            verifyExceptionDoesNotContain(e, TokenConstants.TOKEN_TYPE);
        }
    }

    @Test
    public void test_verifyTokenResponseContainsRequiredParameters_allRequiredParametersPresent() {
        JSONObject tokenResponseJson = new JSONObject();
        tokenResponseJson.put(TokenConstants.ACCESS_TOKEN, "my access token");
        tokenResponseJson.put(TokenConstants.TOKEN_TYPE, "my token type");
        tokenResponseJson.put(TokenConstants.ID_TOKEN, "my ID token");

        TokenResponse tokenResponse = new TokenResponse(tokenResponseJson);

        try {
            tokenRequest.verifyTokenResponseContainsRequiredParameters(tokenResponse);
        } catch (TokenRequestException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

}
