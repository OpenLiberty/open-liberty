/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.jose4j.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.jwt.JwtClaims;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;
import com.ibm.ws.security.openidconnect.common.Constants;

import test.common.SharedOutputManager;
import test.common.junit.matchers.RegexMatcher;

@SuppressWarnings("unchecked")
public class OidcTokenImplBaseTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect.*=all=enabled");

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    final static String TO_STRING_ISS = "iss";
    final static String TO_STRING_TYPE = "type";
    final static String TO_STRING_CLIENT_ID = "client_id";
    final static String TO_STRING_SUB = "sub";
    final static String TO_STRING_AUD = "aud";
    final static String TO_STRING_EXP = "exp";
    final static String TO_STRING_IAT = "iat";
    final static String TO_STRING_NONCE = "nonce";
    final static String TO_STRING_AT_HASH = "at_hash";
    final static String TO_STRING_ACR = "acr";
    final static String TO_STRING_AMR = "amr";
    final static String TO_STRING_AZP = "azp";

    final static String CLAIM_CLIENT_ID = "azp2";
    final static String CLAIM_EXTRA_STRING = "xString";
    final static String CLAIM_EXTRA_STRING_LIST = "xSList";
    final static String CLAIM_EXTRA_ARRAY_LIST_EMPTY = "xArrayListEmpty";
    final static String CLAIM_EXTRA_ARRAY_LIST = "xAList";
    final static String CLAIM_EXTRA_JSON_OBJECT = "xJsonObj";

    final static String ACCESS_TOKEN = "SomeAccessTokenValue";
    final static String REFRESH_TOKEN = "SomeRefreshTokenValue";
    final static String CLIENT_ID = "myClientId";
    final static String ISSUER = "myIssuer";
    final static String TYPE = "Some Type";
    final static String SUBJECT = "mySubject";
    final static String AUDIENCE = "myAudience";
    final static String NONCE = "someNonce";
    final static String AT_HASH = "myAtHash";
    final static String CLASS_REFERENCE = "some class reference";
    final static String METHODS_REFERENCE = "some methods reference";
    final static String AUTHORIZED_PARTY = "myAuthorizedParty";

    final static String EXTRA_KEY_1 = "xKey1";
    final static String EXTRA_VAL_1 = "extra value 1";
    final static String EXTRA_VAL_2 = "extra_val2";
    final static String EXTRA_STRING = "Some extra string claim value";
    final static List<String> EXTRA_STRING_LIST = Arrays.asList(EXTRA_VAL_1, EXTRA_VAL_2);
    final static JSONArray EXTRA_ARRAY_LIST_EMPTY = new JSONArray();
    final static JSONObject EXTRA_JSON_OBJECT = new JSONObject();
    final static int EXPIRATION_TIME_IN_SECS = 100;
    final static int ISSUED_AT_TIME_IN_SECS = 50;

    static {
        EXTRA_JSON_OBJECT.put(EXTRA_KEY_1, EXTRA_VAL_1);
    }

    final JwtClaims jwtClaims = mockery.mock(JwtClaims.class);

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
        mockery.checking(new Expectations() {
            {
                allowing(jwtClaims).toJson();
                will(returnValue(EXTRA_JSON_OBJECT.toString()));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
        System.out.println("Exiting test: " + testName.getMethodName());
    }

    /********************************************* constructor *********************************************/

    @Test
    public void constructor_nullArgs() {
        OidcTokenImplBase token = new OidcTokenImplBase(null, null, null, null, null);

        assertNull("Access token should have been null but wasn't. Result: " + token.getAccessToken(), token.getAccessToken());
        assertNull("Refresh token should have been null but wasn't. Result: " + token.getRefreshToken(), token.getRefreshToken());
        assertNull("Client ID token should have been null but wasn't. Result: " + token.getClientId(), token.getClientId());
    }

    @Test
    public void constructor_validArgs() {
        OidcTokenImplBase token = new OidcTokenImplBase(jwtClaims, ACCESS_TOKEN, REFRESH_TOKEN, CLIENT_ID, Constants.TOKEN_TYPE_JWT);

        assertEquals("Access token did not match expected value.", ACCESS_TOKEN, token.getAccessToken());
        assertEquals("Refresh token did not match expected value.", REFRESH_TOKEN, token.getRefreshToken());
        assertEquals("Client ID did not match expected value.", CLIENT_ID, token.getClientId());
    }

    /********************************************* toString *********************************************/

    @Test
    public void toString_nullMembers() {
        OidcTokenImplBase token = new OidcTokenImplBase(null, null, null, null, null);

        assertEquals("Constructor with null args should result in token string that just has token type.", Constants.TOKEN_TYPE_ID_TOKEN, token.toString());
    }

    @Test
    public void toString_fullClaimsMap() {
        OidcTokenImplBase token = createStandardToken();

        String tokenString = token.toString();

        assertStringContainsRegex("^" + Constants.TOKEN_TYPE_ID_TOKEN + ":" + Pattern.quote(EXTRA_JSON_OBJECT.toString()) + "$", tokenString);
    }

    /********************************************* Helper methods *********************************************/

    OidcTokenImplBase createStandardToken() {
        return new OidcTokenImplBase(jwtClaims, ACCESS_TOKEN, REFRESH_TOKEN, CLIENT_ID, Constants.TOKEN_TYPE_ID_TOKEN);
    }

    void assertStringContainsRegex(String regex, String searchString) {
        assertTrue("Expected to find pattern [" + regex + "] but did not. Searched in: [" + searchString + "].", RegexMatcher.match(searchString, regex));
    }
}
