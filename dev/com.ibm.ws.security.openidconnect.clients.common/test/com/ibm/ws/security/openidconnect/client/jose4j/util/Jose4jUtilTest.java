/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.client.jose4j.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jose4j.jwt.JwtClaims;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.ws.security.openidconnect.clients.common.Constants;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException;
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

    /************************************** checkJwtFormatAgainstConfigRequirements **************************************/

    @Test
    public void test_checkJwtFormatAgainstConfigRequirements_nullJwt_nullKeyAlias() {
        String jwtString = null;
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getKeyManagementKeyAlias();
                will(returnValue(null));
                one(clientConfig).getId();
                will(returnValue("configId"));
            }
        });
        try {
            util.checkJwtFormatAgainstConfigRequirements(jwtString, clientConfig);
            fail("Should have thrown a JWTTokenValidationFailedException exception but did not.");
        } catch (JWTTokenValidationFailedException e) {
            verifyException(e, "CWWKS1536E");
        }
    }

    @Test
    public void test_checkJwtFormatAgainstConfigRequirements_nullJwt_withKeyAlias() {
        String jwtString = null;
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getKeyManagementKeyAlias();
                will(returnValue("someAlias"));
                one(clientConfig).getId();
                will(returnValue("configId"));
            }
        });
        try {
            util.checkJwtFormatAgainstConfigRequirements(jwtString, clientConfig);
            fail("Should have thrown a JWTTokenValidationFailedException exception but did not.");
        } catch (JWTTokenValidationFailedException e) {
            verifyException(e, "CWWKS1537E");
        }
    }

    @Test
    public void test_checkJwtFormatAgainstConfigRequirements_emptyJwt_nullKeyAlias() {
        String jwtString = "";
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getKeyManagementKeyAlias();
                will(returnValue(null));
                one(clientConfig).getId();
                will(returnValue("configId"));
            }
        });
        try {
            util.checkJwtFormatAgainstConfigRequirements(jwtString, clientConfig);
            fail("Should have thrown a JWTTokenValidationFailedException exception but did not.");
        } catch (JWTTokenValidationFailedException e) {
            verifyException(e, "CWWKS1536E");
        }
    }

    @Test
    public void test_checkJwtFormatAgainstConfigRequirements_emptyJwt_withKeyAlias() {
        String jwtString = "";
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getKeyManagementKeyAlias();
                will(returnValue("someAlias"));
                one(clientConfig).getId();
                will(returnValue("configId"));
            }
        });
        try {
            util.checkJwtFormatAgainstConfigRequirements(jwtString, clientConfig);
            fail("Should have thrown a JWTTokenValidationFailedException exception but did not.");
        } catch (JWTTokenValidationFailedException e) {
            verifyException(e, "CWWKS1537E");
        }
    }

    @Test
    public void test_checkJwtFormatAgainstConfigRequirements_jws_nullKeyAlias() {
        String jwtString = "xxx.yyy.zzz";
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getKeyManagementKeyAlias();
                will(returnValue(null));
            }
        });
        try {
            util.checkJwtFormatAgainstConfigRequirements(jwtString, clientConfig);
        } catch (JWTTokenValidationFailedException e) {
            fail("Should not have thrown a JWTTokenValidationFailedException exception but did: " + e);
        }
    }

    @Test
    public void test_checkJwtFormatAgainstConfigRequirements_jws_withKeyAlias() {
        String jwtString = "xxx.yyy.zzz";
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getKeyManagementKeyAlias();
                will(returnValue("someAlias"));
                one(clientConfig).getId();
                will(returnValue("configId"));
            }
        });
        try {
            util.checkJwtFormatAgainstConfigRequirements(jwtString, clientConfig);
            fail("Should have thrown a JWTTokenValidationFailedException exception but did not.");
        } catch (JWTTokenValidationFailedException e) {
            verifyException(e, "CWWKS1537E");
        }
    }

    @Test
    public void test_checkJwtFormatAgainstConfigRequirements_jwe_nullKeyAlias() {
        String jwtString = "xxx.yyy.zzz.aaa.bbb";
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getKeyManagementKeyAlias();
                will(returnValue(null));
                one(clientConfig).getId();
                will(returnValue("configId"));
            }
        });
        try {
            util.checkJwtFormatAgainstConfigRequirements(jwtString, clientConfig);
            fail("Should have thrown a JWTTokenValidationFailedException exception but did not.");
        } catch (JWTTokenValidationFailedException e) {
            verifyException(e, "CWWKS1536E");
        }
    }

    @Test
    public void test_checkJwtFormatAgainstConfigRequirements_jwe_withKeyAlias() {
        String jwtString = "xxx.yyy.zzz.aaa.bbb";
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getKeyManagementKeyAlias();
                will(returnValue("someAlias"));
            }
        });
        try {
            util.checkJwtFormatAgainstConfigRequirements(jwtString, clientConfig);
        } catch (JWTTokenValidationFailedException e) {
            fail("Should not have thrown a JWTTokenValidationFailedException exception but did: " + e);
        }
    }

    /************************************** getVerifyKey **************************************/

    @Test
    public void testGetVerifyKey_signatureAlgorithmNone() {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getSignatureAlgorithm();
                will(returnValue(SIGNATURE_ALG_NONE));
            }
        });
        try {
            Object keyValue = util.getVerifyKey(clientConfig, "kid", "x5t");
            assertNull("Expected a null key but received " + keyValue, keyValue);
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

    /************************************** getUserName **************************************/

    private static final String CLAIM_NAME_USER_NAME = "user_name";
    private static final String CLAIM_USER_ANOTHER = "user_another";
    private static final String CLAIM_NAME_REALM_NAME = "realm_name";
    private static final String CLAIM_NAME_UNIQUE_SECURITY_NAME = "unique_security_name";
    private static final String CLAIM_NAME_GROUP_IDENTIFIER = "groupIds";

    private static final String USER_1 = "User1";
    private static final String REALM_NAME_1 = "RealmName1";
    private static final String USN_1 = "USN1";
    private static final List<String> GROUP_IDS_1 = Arrays.asList(new String[] { "GID11", "GID12", "GID13" });

    private static final String USER_2 = "User2";
    private static final String REALM_NAME_2 = "RealmName2";
    private static final String USN_2 = "USN2";
    private static final String S_GROUP_ID_2 = "GID20";
    private static final List<String> GROUP_IDS_2 = Arrays.asList(new String[] { "GID21", "GID22", "GID23" });

    private static final String USER_3 = "User3";
    private static final String REALM_NAME_3 = "RealmName3";
    private static final String USN_3 = "USN3";
    private static final List<String> GROUP_IDS_3 = Arrays.asList(new String[] { "GID31", "GID32", "GID33" });

    @Test
    public void testGetUserName_fromIdToken() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getUserIdentifier();
                will(returnValue(CLAIM_NAME_USER_NAME));
            }
        });
        try {
            Object userNameValue = util.getUserName(clientConfig, getTokensOrderToFetchCallerClaimsIdTokenOnly(), createClaimsMapIdTokenOnly());
            assertEquals("Expected userName:" + USER_2 + " but received:" + userNameValue + ".", USER_2, userNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetUserName_fromIdToken", e);
        }
    }
    
    @Test
    public void testGetUserName_fromIdToken_usingTokenOrder() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getUserIdentifier();
                will(returnValue(CLAIM_NAME_USER_NAME));
            }
        });
        try {
            Object userNameValue = util.getUserName(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createEmptyClaimsMapAccessToken());
            assertEquals("Expected userName:" + USER_2 + " but received:" + userNameValue + ".", USER_2, userNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetUserName_fromIdToken_usingTokenOrder", e);
        }
    }
    
    @Test
    public void testGetUserName_fromIdToken_missingClaim() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).isSocial();
                will(returnValue(false));
                allowing(clientConfig).getUserIdentifier();
                will(returnValue(CLAIM_NAME_USER_NAME));
                one(clientConfig).getClientId();
                will(returnValue("clientId"));
            }
        });
        try {
            Object userNameValue = util.getUserName(clientConfig, getTokensOrderToFetchCallerClaimsIdTokenOnly(), createClaimsMapUserInfoOnly());
            assertNull("user claim should have been null but was [" + userNameValue + "].", userNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetUserName_fromIdToken_missingClaim", e);
        }
    }
    
    @Test
    public void testGetUserName_fromAllTokens_missingClaim() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).isSocial();
                will(returnValue(false));
                allowing(clientConfig).getUserIdentifier();
                will(returnValue(CLAIM_USER_ANOTHER));
                one(clientConfig).getClientId();
                will(returnValue("clientId"));
            }
        });
        try {
            Object userNameValue = util.getUserName(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createClaimsMapAllTokens());
            assertNull("user claim should have been null but was [" + userNameValue + "].", userNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetUserName_fromAllTokens_missingClaim", e);
        }
    }
    
    @Test
    public void testGetClaim_fromAllTokens_nullClaimMap() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).isSocial();
                will(returnValue(false));
                allowing(clientConfig).getUserIdentifier();
                will(returnValue(CLAIM_USER_ANOTHER));
                one(clientConfig).getClientId();
                will(returnValue("clientId"));
            }
        });
        try {
            Object userNameValue = util.getUserName(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createNullClaimsMapATAndUI());
            assertNull("user claim should have been null but was [" + userNameValue + "].", userNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetClaim_fromAllTokens_nullClaimMap", e);
        }
    }

    @Test
    public void testGetUserName_fromAccessToken() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getUserIdentifier();
                will(returnValue(CLAIM_NAME_USER_NAME));
            }
        });
        try {
            Object userNameValue = util.getUserName(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createClaimsMapAllTokens());
            assertEquals("Expected userName:" + USER_1 + " but received:" + userNameValue + ".", USER_1, userNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetUserName_fromAccessToken", e);
        }
    }

    @Test
    public void testGetUserName_fromUserInfo() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getUserIdentifier();
                will(returnValue(CLAIM_NAME_USER_NAME));
            }
        });
        try {
            Object userNameValue = util.getUserName(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createClaimsMapUserInfoOnly());
            assertEquals("Expected userName:" + USER_3 + " but received:" + userNameValue + ".", USER_3, userNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetUserName_fromUserInfo", e);
        }
    }

    /************************************** testGetRealmName **************************************/

    @Test
    public void testGetRealmName_fromIdToken() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getRealmName();
                will(returnValue(null));
                one(clientConfig).getRealmIdentifier();
                will(returnValue(CLAIM_NAME_REALM_NAME));
            }
        });
        try {
            Object realmNameValue = util.getRealmName(clientConfig, getTokensOrderToFetchCallerClaimsIdTokenOnly(), createClaimsMapIdTokenOnly());
            assertEquals("Expected realmName:" + REALM_NAME_2 + " but received:" + realmNameValue + ".", REALM_NAME_2, realmNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetRealmName_fromIdToken", e);
        }
    }

    @Test
    public void testGetRealmName_fromAccessToken() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getRealmName();
                will(returnValue(null));
                one(clientConfig).getRealmIdentifier();
                will(returnValue(CLAIM_NAME_REALM_NAME));
            }
        });
        try {
            Object realmNameValue = util.getRealmName(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createClaimsMapAllTokens());
            assertEquals("Expected realmName:" + REALM_NAME_1 + " but received:" + realmNameValue + ".", REALM_NAME_1, realmNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetRealmName_fromAccessToken", e);
        }
    }

    @Test
    public void testGetRealmName_fromUserInfo() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getRealmName();
                will(returnValue(null));
                one(clientConfig).getRealmIdentifier();
                will(returnValue(CLAIM_NAME_REALM_NAME));
            }
        });
        try {
            Object realmNameValue = util.getRealmName(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createClaimsMapUserInfoOnly());
            assertEquals("Expected realmName:" + REALM_NAME_3 + " but received:" + realmNameValue + ".", REALM_NAME_3, realmNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetRealmName_fromUserInfo", e);
        }
    }

    /************************************** getUniqueSecurityName **************************************/

    @Test
    public void testGetUniqueSecurityName_fromIdToken() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getUniqueUserIdentifier();
                will(returnValue(CLAIM_NAME_UNIQUE_SECURITY_NAME));
            }
        });
        try {
            Object uniqueSecurityNameValue = util.getUniqueSecurityName(clientConfig, getTokensOrderToFetchCallerClaimsIdTokenOnly(), createClaimsMapIdTokenOnly(), USER_2);
            assertEquals("Expected uniqueSecurityNameValue:" + USN_2 + " but received:" + uniqueSecurityNameValue + ".", USN_2, uniqueSecurityNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetUniqueSecurityName_fromIdToken", e);
        }
    }

    @Test
    public void testGetUniqueSecurityName_fromAccessToken() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getUniqueUserIdentifier();
                will(returnValue(CLAIM_NAME_UNIQUE_SECURITY_NAME));
            }
        });
        try {
            Object uniqueSecurityNameValue = util.getUniqueSecurityName(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createClaimsMapAllTokens(), USER_1);
            assertEquals("Expected uniqueSecurityNameValue:" + USN_1 + " but received:" + uniqueSecurityNameValue + ".", USN_1, uniqueSecurityNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetUniqueSecurityName_fromAccessToken", e);
        }
    }

    @Test
    public void testGetUniqueSecurityName_fromUserInfo() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getUniqueUserIdentifier();
                will(returnValue(CLAIM_NAME_UNIQUE_SECURITY_NAME));
            }
        });
        try {
            Object uniqueSecurityNameValue = util.getUniqueSecurityName(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createClaimsMapUserInfoOnly(), USER_3);
            assertEquals("Expected uniqueSecurityNameValue:" + USN_3 + " but received:" + uniqueSecurityNameValue + ".", USN_3, uniqueSecurityNameValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetUniqueSecurityName_fromUserInfo", e);
        }
    }

    /************************************** getGroups **************************************/

    @Test
    public void testGetGroupIds_Single_fromIdToken() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getGroupIdentifier();
                will(returnValue(CLAIM_NAME_GROUP_IDENTIFIER));
            }
        });
        try {
            List<String> groupIdsValue = util.getGroupIds(clientConfig, getTokensOrderToFetchCallerClaimsIdTokenOnly(), createClaimsMapSingleGroupIdTokenOnly());
            assertEquals("Expected list of groupIds: {" + S_GROUP_ID_2 + "} matches the received", Arrays.asList(new String[] { S_GROUP_ID_2 }), groupIdsValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetGroupIds_Single_fromIdToken", e);
        }
    }

    @Test
    public void testGetGroups_fromIdToken() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getGroupIdentifier();
                will(returnValue(CLAIM_NAME_GROUP_IDENTIFIER));
                one(clientConfig).getGroupIdentifier();
                will(returnValue(CLAIM_NAME_GROUP_IDENTIFIER));
            }
        });
        try {
            List<String> groupIdsValue = util.getGroupIds(clientConfig, getTokensOrderToFetchCallerClaimsIdTokenOnly(), createClaimsMapIdTokenOnly());
            assertEquals("Expected list of groupIds: " + toString(GROUP_IDS_2) + " matches the received", GROUP_IDS_2, groupIdsValue);
            List<String> groupsValue = util.getGroups(clientConfig, getTokensOrderToFetchCallerClaimsIdTokenOnly(), createClaimsMapIdTokenOnly(), REALM_NAME_2);
            List<String> expectedGroups = convertGroups(GROUP_IDS_2, REALM_NAME_2);
            assertEquals("Expected list of groups: " + toString(expectedGroups) + " matches the received", expectedGroups, groupsValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetGroups_fromIdToken", e);
        }
    }

    @Test
    public void testGetGroups_fromAccessToken() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getGroupIdentifier();
                will(returnValue(CLAIM_NAME_GROUP_IDENTIFIER));
                one(clientConfig).getGroupIdentifier();
                will(returnValue(CLAIM_NAME_GROUP_IDENTIFIER));
            }
        });
        try {
            List<String> groupIdsValue = util.getGroupIds(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createClaimsMapAllTokens());
            assertEquals("Expected list of groupIds: " + toString(GROUP_IDS_1) + " matches the received", GROUP_IDS_1, groupIdsValue);
            List<String> groupsValue = util.getGroups(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createClaimsMapAllTokens(), REALM_NAME_1);
            List<String> expectedGroups = convertGroups(GROUP_IDS_1, REALM_NAME_1);
            assertEquals("Expected list of groups: " + toString(expectedGroups) + " matches the received", expectedGroups, groupsValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetGroups_fromAccessToken", e);
        }
    }

    @Test
    public void testGetGroups_fromUserInfo() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getGroupIdentifier();
                will(returnValue(CLAIM_NAME_GROUP_IDENTIFIER));
                one(clientConfig).getGroupIdentifier();
                will(returnValue(CLAIM_NAME_GROUP_IDENTIFIER));
            }
        });

        try {
            List<String> groupIdsValue = util.getGroupIds(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createClaimsMapUserInfoOnly());
            assertEquals("Expected list of groupIds: " + toString(GROUP_IDS_3) + " matches the received", GROUP_IDS_3, groupIdsValue);
            List<String> groupsValue = util.getGroups(clientConfig, getTokensOrderToFetchCallerClaimsAll(), createClaimsMapUserInfoOnly(), REALM_NAME_3);
            List<String> expectedGroups = convertGroups(GROUP_IDS_3, REALM_NAME_3);
            assertEquals("Expected list of groups: " + toString(expectedGroups) + " matches the received", expectedGroups, groupsValue);
        } catch (Exception e) {
            outputMgr.failWithThrowable("testGetGroups_fromAccessToken", e);
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

    private List<String> getTokensOrderToFetchCallerClaimsAll() {
        return Arrays.asList(new String[] { Constants.TOKEN_TYPE_ACCESS_TOKEN, Constants.TOKEN_TYPE_ID_TOKEN, Constants.TOKEN_TYPE_USER_INFO });
    }

    private List<String> getTokensOrderToFetchCallerClaimsIdTokenOnly() {
        return Arrays.asList(new String[] { Constants.TOKEN_TYPE_ID_TOKEN });
    }

    private JwtClaims createClaims(String userName, String realmName, String usn, Object groupIds) {
        JwtClaims rvalue = new JwtClaims();
        rvalue.setClaim(CLAIM_NAME_USER_NAME, userName);
        rvalue.setClaim(CLAIM_NAME_REALM_NAME, realmName);
        rvalue.setClaim(CLAIM_NAME_UNIQUE_SECURITY_NAME, usn);
        rvalue.setClaim(CLAIM_NAME_GROUP_IDENTIFIER, groupIds);
        return rvalue;
    }

    //    private JwtClaims createDefaultClaims() {
    //        return createClaims(BASIC_USER, BASIC_REALM_NAME, BASIC_USN, BASIC_GROUP_IDS);
    //    }

    private JwtClaims createEmptyClaims() {
        return new JwtClaims();
    }

    private Map<String, JwtClaims> createClaimsMapIdTokenOnly() {
        Map<String, JwtClaims> claimsMap = new HashMap<String, JwtClaims>();
        claimsMap.put(Constants.TOKEN_TYPE_ID_TOKEN, createClaims(USER_2, REALM_NAME_2, USN_2, GROUP_IDS_2));

        return claimsMap;
    }

    private Map<String, JwtClaims> createClaimsMapSingleGroupIdTokenOnly() {
        Map<String, JwtClaims> claimsMap = new HashMap<String, JwtClaims>();
        claimsMap.put(Constants.TOKEN_TYPE_ID_TOKEN, createClaims(USER_2, REALM_NAME_2, USN_2, S_GROUP_ID_2));

        return claimsMap;
    }

    private Map<String, JwtClaims> createClaimsMapAllTokens() {
        Map<String, JwtClaims> claimsMap = new HashMap<String, JwtClaims>();
        claimsMap.put(Constants.TOKEN_TYPE_ACCESS_TOKEN, createClaims(USER_1, REALM_NAME_1, USN_1, GROUP_IDS_1));
        claimsMap.put(Constants.TOKEN_TYPE_ID_TOKEN, createClaims(USER_2, REALM_NAME_2, USN_2, GROUP_IDS_2));
        claimsMap.put(Constants.TOKEN_TYPE_USER_INFO, createClaims(USER_3, REALM_NAME_3, USN_3, GROUP_IDS_3));

        return claimsMap;
    }
    
    private Map<String, JwtClaims> createEmptyClaimsMapAccessToken() {
        Map<String, JwtClaims> claimsMap = new HashMap<String, JwtClaims>();
        claimsMap.put(Constants.TOKEN_TYPE_ACCESS_TOKEN, createEmptyClaims());
        claimsMap.put(Constants.TOKEN_TYPE_ID_TOKEN, createClaims(USER_2, REALM_NAME_2, USN_2, GROUP_IDS_2));
        claimsMap.put(Constants.TOKEN_TYPE_USER_INFO, createClaims(USER_3, REALM_NAME_3, USN_3, GROUP_IDS_3));

        return claimsMap;
    }
    
    private Map<String, JwtClaims> createNullClaimsMapATAndUI() {
        Map<String, JwtClaims> claimsMap = new HashMap<String, JwtClaims>();
        claimsMap.put(Constants.TOKEN_TYPE_ACCESS_TOKEN, null);
        claimsMap.put(Constants.TOKEN_TYPE_ID_TOKEN, createClaims(USER_2, REALM_NAME_2, USN_2, GROUP_IDS_2));
        claimsMap.put(Constants.TOKEN_TYPE_USER_INFO, null);

        return claimsMap;
    }

    private Map<String, JwtClaims> createClaimsMapUserInfoOnly() {
        Map<String, JwtClaims> claimsMap = new HashMap<String, JwtClaims>();
        claimsMap.put(Constants.TOKEN_TYPE_ACCESS_TOKEN, createEmptyClaims());
        claimsMap.put(Constants.TOKEN_TYPE_ID_TOKEN, createEmptyClaims());
        claimsMap.put(Constants.TOKEN_TYPE_USER_INFO, createClaims(USER_3, REALM_NAME_3, USN_3, GROUP_IDS_3));

        return claimsMap;
    }

    private List<String> convertGroups(List<String> groupIds, String realm) {
        List<String> groups = new ArrayList<String>();
        for (String gid : groupIds) {
            String group = new StringBuffer("group:").append(realm).append("/").append(gid).toString();
            groups.add(group);
        }

        return groups;
    }

    private static String toString(List<String> myList) {
        return Arrays.toString(myList.toArray());
    }

}
