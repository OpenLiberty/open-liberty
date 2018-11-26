/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.jose4j.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.wsspi.ssl.SSLSupport;

import test.common.SharedOutputManager;

public class Jose4jUtilTest extends CommonTestClass {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect.*=all=enabled");

    private static final String ACCESS_TOKEN = "myAccessToken";
    private static final String REFRESH_TOKEN = "myRefreshToken";
    private static final String SIGNATURE_ALG_RS256 = "RS256";
    private static final String SIGNATURE_ALG_NONE = "none";
    private static final String SHARED_KEY = "secretsecretsecretsecretsecretsecret";
    private static final String TEST_URL = "http://harmonic.austin.ibm.com:8010/formlogin/SimpleServlet";

    private final SSLSupport sslSupport = mockery.mock(SSLSupport.class);
    private final JwKRetriever jwKRetriever = mockery.mock(JwKRetriever.class);
    private final ConvergedClientConfig clientConfig = mockery.mock(ConvergedClientConfig.class);
    private final PublicKey publicKey = mockery.mock(PublicKey.class);

    Jose4jUtil util = new MyJose4jUtil(sslSupport);

    private class MyJose4jUtil extends Jose4jUtil {
        public MyJose4jUtil(SSLSupport sslSupport) {
            super(sslSupport);
        }

        @Override
        public JwKRetriever createJwkRetriever(ConvergedClientConfig config) {
            return jwKRetriever;
        }

    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        util = new MyJose4jUtil(sslSupport);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** getIdToken **************************************/

    @Test
    public void test_getIdToken_nullTokens() {
        Map<String, String> tokens = null;

        String result = util.getIdToken(tokens, clientConfig);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getIdToken_emptyTokens_doNotUseAccessToken() {
        mockUseAccessTokenAsIdTokenValue(false);

        Map<String, String> tokens = new HashMap<String, String>();

        String result = util.getIdToken(tokens, clientConfig);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getIdToken_emptyTokens_useAccessToken() {
        mockUseAccessTokenAsIdTokenValue(true);

        Map<String, String> tokens = new HashMap<String, String>();

        String result = util.getIdToken(tokens, clientConfig);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getIdToken_missingIdToken_doNotUseAccessToken() {
        mockUseAccessTokenAsIdTokenValue(false);

        Map<String, String> tokens = getTokensWithoutIdTokenKey();

        String result = util.getIdToken(tokens, clientConfig);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getIdToken_missingIdToken_useAccessToken() {
        mockUseAccessTokenAsIdTokenValue(true);

        Map<String, String> tokens = getTokensWithoutIdTokenKey();

        String result = util.getIdToken(tokens, clientConfig);
        assertEquals("Access token should have been used as the ID token, but the value did not match.", ACCESS_TOKEN, result);
    }

    @Test
    public void test_getIdToken_missingIdToken_useAccessToken_missingAccessToken() {
        mockUseAccessTokenAsIdTokenValue(true);

        Map<String, String> tokens = getTokensWithoutIdTokenKey();
        tokens.remove(Constants.ACCESS_TOKEN);

        String result = util.getIdToken(tokens, clientConfig);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getIdToken_idTokenValueNull_doNotUseAccessToken() {
        mockUseAccessTokenAsIdTokenValue(false);

        final String idToken = null;
        Map<String, String> tokens = getTokensWithIdTokenKey(idToken);

        String result = util.getIdToken(tokens, clientConfig);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getIdToken_idTokenValueNull_useAccessToken() {
        mockUseAccessTokenAsIdTokenValue(true);

        final String idToken = null;
        Map<String, String> tokens = getTokensWithIdTokenKey(idToken);

        String result = util.getIdToken(tokens, clientConfig);
        assertEquals("Access token should have been used as the ID token, but the value did not match.", ACCESS_TOKEN, result);
    }

    @Test
    public void test_getIdToken_idTokenValueNull_useAccessToken_missingAccessToken() {
        mockUseAccessTokenAsIdTokenValue(true);

        final String idToken = null;
        Map<String, String> tokens = getTokensWithIdTokenKey(idToken);
        tokens.remove(Constants.ACCESS_TOKEN);

        String result = util.getIdToken(tokens, clientConfig);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getIdToken_idTokenValueEmpty() {
        final String idToken = "";
        Map<String, String> tokens = getTokensWithIdTokenKey(idToken);

        String result = util.getIdToken(tokens, clientConfig);
        assertEquals("Result did not match the expected value.", idToken, result);
    }

    /************************************** useAccessTokenAsIdToken **************************************/

    @Test
    public void test_useAccessTokenAsIdToken_false() {
        mockUseAccessTokenAsIdTokenValue(false);
        assertFalse("Result did not match the expected value.", util.useAccessTokenAsIdToken(clientConfig));
    }

    @Test
    public void test_useAccessTokenAsIdToken_true() {
        mockUseAccessTokenAsIdTokenValue(true);
        assertTrue("Result did not match the expected value.", util.useAccessTokenAsIdToken(clientConfig));
    }

    /************************************** getVerifyKey **************************************/

    @Test
    public void testGetVerifyKey_signatureAlgorithmNone() {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getSignatureAlgorithm();
                will(returnValue(SIGNATURE_ALG_NONE));
                one(clientConfig).getSharedKey();
                will(returnValue(SHARED_KEY));
            }
        });
        try {
            Object keyValue = util.getVerifyKey(clientConfig, "kid", "x5t");
            assertNotNull("Expected a valid key but received null.", keyValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetVerifyKey", e);
        }
    }

    @Test
    public void testGetVerifyKey_nullJwkEndpointUrl() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getSignatureAlgorithm();
                will(returnValue(SIGNATURE_ALG_RS256));
                one(clientConfig).getJwkEndpointUrl();
                will(returnValue(null));
                one(clientConfig).getJsonWebKey();
                will(returnValue(null));
                one(clientConfig).getPublicKey();
                will(returnValue(publicKey));
            }
        });
        try {
            Object keyValue = util.getVerifyKey(clientConfig, "kid", "x5t");
            assertEquals("Expected key:" + publicKey + " but received:" + keyValue + ".", publicKey, keyValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetVerifyKey", e);
        }
    }

    @Test
    public void testGetVerifyKey_getKeyFromJwkUrl() throws Exception {
        final String KID = "kid";
        final String x5t = "x5t";
        final String use = "sig";
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getSignatureAlgorithm();
                will(returnValue(SIGNATURE_ALG_RS256));
                one(clientConfig).getJwkEndpointUrl();
                will(returnValue(TEST_URL));
                one(jwKRetriever).getPublicKeyFromJwk(KID, x5t, use, false);
                will(returnValue(publicKey));
                one(clientConfig).getUseSystemPropertiesForHttpClientConnections();
                will(returnValue(false));
            }
        });
        try {
            Object keyValue = util.getVerifyKey(clientConfig, KID, x5t);
            assertEquals("Expected key:" + publicKey + " but received:" + keyValue + ".", publicKey, keyValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetVerifyKey", e);
        }
    }

    /************************************** Helper methods **************************************/

    private void mockUseAccessTokenAsIdTokenValue(final boolean useAccessTokenAsIdToken) {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getUseAccessTokenAsIdToken();
                will(returnValue(useAccessTokenAsIdToken));
            }
        });
    }

    private Map<String, String> getTokensWithoutIdTokenKey() {
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("key1", "value1");
        tokens.put("key2", "value2");
        tokens.put(Constants.ACCESS_TOKEN, ACCESS_TOKEN);
        tokens.put(Constants.REFRESH_TOKEN, REFRESH_TOKEN);
        return tokens;
    }

    private Map<String, String> getTokensWithIdTokenKey(String idTokenValue) {
        Map<String, String> tokens = getTokensWithoutIdTokenKey();
        tokens.put(Constants.ID_TOKEN, idTokenValue);
        return tokens;
    }

}
