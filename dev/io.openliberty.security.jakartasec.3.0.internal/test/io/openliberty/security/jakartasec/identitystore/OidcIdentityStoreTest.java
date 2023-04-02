/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.identitystore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jmock.Expectations;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.jakartasec.ClaimsDefinitionWrapper;
import io.openliberty.security.jakartasec.TestClaimsDefinition;
import io.openliberty.security.jakartasec.tokens.OpenIdClaimsImpl;
import io.openliberty.security.oidcclientcore.client.ClaimsMappingConfig;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.token.TokenConstants;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;
import test.common.SharedOutputManager;

public class OidcIdentityStoreTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final OidcClientConfig config = mockery.mock(OidcClientConfig.class);
    private final ClaimsMappingConfig claimsMappingConfig = mockery.mock(ClaimsMappingConfig.class);
    private final AccessToken accessToken = mockery.mock(AccessToken.class);
    private final JwtClaims idTokenClaims = mockery.mock(JwtClaims.class);
    private final OpenIdClaims userInfoClaims = mockery.mock(OpenIdClaims.class);

    private OidcIdentityStore identityStore;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        identityStore = new OidcIdentityStore();
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_createCredentialValidationResult() throws MalformedClaimException {
        String callerName = "longtimeListenerFirstTimeCaller";
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put(TestClaimsDefinition.CALLER_NAME_CLAIM, callerName);

        String groupName = "longtimeListenerFirstTimeGroup";
        overrides.put(TestClaimsDefinition.CALLER_GROUPS_CLAIM, groupName);

        ClaimsDefinition claimsDefinition = TestClaimsDefinition.getInstanceofAnnotation(overrides);
        ClaimsDefinitionWrapper wrapper = new ClaimsDefinitionWrapper(claimsDefinition);

        List<String> testSet = new ArrayList<String>();
        testSet.add(groupName);

        mockery.checking(new Expectations() {
            {
                allowing(accessToken).isJWT();
                will(returnValue(true));
                allowing(accessToken).getClaim("iss");
                will(returnValue("IamTheIssuer"));
                allowing(config).getClaimsMappingConfig();
                will(returnValue(wrapper));
                allowing(accessToken).getClaim(callerName);
                will(returnValue(callerName));
                allowing(accessToken).getClaim(groupName);
                will(returnValue(testSet));
            }
        });

        CredentialValidationResult result = identityStore.createCredentialValidationResult(config, accessToken, idTokenClaims, userInfoClaims);

        assertNotNull("CredentialValidationResult should be returned", result);
        assertEquals("Caller name incorrect", callerName, result.getCallerUniqueId());
        assertNotNull("Groups name should be returned", result.getCallerGroups());
        assertFalse("Groups set should not be empty", result.getCallerGroups().isEmpty());
        assertEquals("Groups set should have a single entry", 1, result.getCallerGroups().size());
        assertEquals("Wrong groups name returned", groupName, result.getCallerGroups().iterator().next());
    }

    @Test
    public void test_createCredentialValidationResult_nullCaller() throws MalformedClaimException {

        Map<String, Object> overrides = new HashMap<String, Object>();

        String groupName = "longtimeListenerFirstTimeGroup";
        overrides.put(TestClaimsDefinition.CALLER_GROUPS_CLAIM, groupName);

        ClaimsDefinition claimsDefinition = TestClaimsDefinition.getInstanceofAnnotation(overrides);
        ClaimsDefinitionWrapper wrapper = new ClaimsDefinitionWrapper(claimsDefinition);

        List<String> testSet = new ArrayList<String>();
        testSet.add(groupName);

        mockery.checking(new Expectations() {
            {
                allowing(accessToken).isJWT();
                will(returnValue(true));
                allowing(accessToken).getClaim("iss");
                will(returnValue("IamTheIssuer"));
                allowing(config).getClaimsMappingConfig();
                will(returnValue(wrapper));
                allowing(accessToken).getClaim("callerName");
                will(returnValue(null));
                allowing(accessToken).getClaim("preferred_username");
                will(returnValue(null));
                allowing(idTokenClaims).getClaimValue("preferred_username", String.class);
                will(returnValue(null));
                allowing(userInfoClaims).getStringClaim("preferred_username");
                will(returnValue(Optional.ofNullable(null)));
            }
        });

        CredentialValidationResult result = identityStore.createCredentialValidationResult(config, accessToken, idTokenClaims, userInfoClaims);

        assertNotNull("CredentialValidationResult should be returned", result);
        assertEquals("Credential should be invalid as the callerName was null", CredentialValidationResult.INVALID_RESULT, result);
    }

    @Test
    public void test_getCallerName_noClaimsMapping() throws MalformedClaimException {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(null));
            }
        });
        String result = identityStore.getCallerName(config, accessToken, idTokenClaims, userInfoClaims);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getCallerName_FromClaimsConfig() throws MalformedClaimException {
        String callerName = "longtimeListenerFirstTimeCaller";
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put(TestClaimsDefinition.CALLER_NAME_CLAIM, callerName);

        ClaimsDefinition claimsDefinition = TestClaimsDefinition.getInstanceofAnnotation(overrides);
        ClaimsDefinitionWrapper wrapper = new ClaimsDefinitionWrapper(claimsDefinition);

        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(wrapper));
                allowing(accessToken).isJWT();
                allowing(idTokenClaims).getClaimValue(callerName, String.class);
                will(returnValue(callerName));
            }
        });
        String result = identityStore.getCallerName(config, accessToken, idTokenClaims, userInfoClaims);
        assertNotNull("Caller name should be returned", result);
        assertEquals("Wrong caller name returned", callerName, result);
    }

    @Test
    public void test_getCallerGroups_noClaimsMapping() throws MalformedClaimException {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(null));
            }
        });
        Set<String> result = identityStore.getCallerGroups(config, accessToken, idTokenClaims, userInfoClaims);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getCallerGroup_FromClaimsConfig() throws MalformedClaimException {
        String groupName = "longtimeListenerFirstTimeGroup";
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put(TestClaimsDefinition.CALLER_GROUPS_CLAIM, groupName);

        ClaimsDefinition claimsDefinition = TestClaimsDefinition.getInstanceofAnnotation(overrides);
        ClaimsDefinitionWrapper wrapper = new ClaimsDefinitionWrapper(claimsDefinition);

        List<String> testSet = new ArrayList<String>();
        testSet.add(groupName);

        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(wrapper));
                allowing(accessToken).isJWT();
                allowing(idTokenClaims).getClaimValue(groupName, List.class);
                will(returnValue(testSet));
            }
        });
        Set<String> result = identityStore.getCallerGroups(config, accessToken, idTokenClaims, userInfoClaims);
        assertNotNull("Groups name should be returned", result);
        assertFalse("Groups set should not be empty", result.isEmpty());
        assertEquals("Groups set should have a single entry", 1, result.size());
        assertEquals("Wrong groups name returned", groupName, result.iterator().next());
    }

    @Test
    public void test_getCallerNameClaim_noClaimsMapping() {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(null));
            }
        });
        String result = identityStore.getCallerNameClaim(config);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getCallerNameClaim_noClaimConfigured() {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(claimsMappingConfig));
                one(claimsMappingConfig).getCallerNameClaim();
                will(returnValue(null));
            }
        });
        String result = identityStore.getCallerNameClaim(config);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getCallerNameClaim() {
        final String claim = "myCallerNameClaim";
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(claimsMappingConfig));
                one(claimsMappingConfig).getCallerNameClaim();
                will(returnValue(claim));
            }
        });
        String result = identityStore.getCallerNameClaim(config);
        assertEquals(claim, result);
    }

    @Test
    public void test_getCallerGroupsClaim_noClaimsMapping() {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(null));
            }
        });
        String result = identityStore.getCallerGroupsClaim(config);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getCallerGroupsClaim_noClaimConfigured() {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(claimsMappingConfig));
                one(claimsMappingConfig).getCallerGroupsClaim();
                will(returnValue(null));
            }
        });
        String result = identityStore.getCallerGroupsClaim(config);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getCallerGroupsClaim() {
        final String claim = "myCallerGroupsClaim";
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(claimsMappingConfig));
                one(claimsMappingConfig).getCallerGroupsClaim();
                will(returnValue(claim));
            }
        });
        String result = identityStore.getCallerGroupsClaim(config);
        assertEquals(claim, result);
    }

    @Test
    public void test_getClaimValueFromTokens_string_claimInAccessToken() throws MalformedClaimException {
        String claim = "myClaim";
        String value = "the expected value";
        mockery.checking(new Expectations() {
            {
                one(accessToken).isJWT();
                will(returnValue(true));
                one(accessToken).getClaim(claim);
                will(returnValue(value));
            }
        });

        String result = identityStore.getClaimValueFromTokens(claim, accessToken, idTokenClaims, userInfoClaims, String.class);
        assertEquals(value, result);
    }

    @Test
    public void test_getClaimValueFromTokens_string_claimInIdToken() throws MalformedClaimException {
        String claim = "myClaim";
        String value = "the expected value";
        mockery.checking(new Expectations() {
            {
                one(accessToken).isJWT();
                will(returnValue(true));
                one(accessToken).getClaim(claim);
                will(returnValue(null));
                one(idTokenClaims).getClaimValue(claim, String.class);
                will(returnValue(value));
            }
        });

        String result = identityStore.getClaimValueFromTokens(claim, accessToken, idTokenClaims, userInfoClaims, String.class);
        assertEquals(value, result);
    }

    @Test
    public void test_getClaimValueFromTokens_string_claimInUserInfo() throws MalformedClaimException {
        String claim = "myClaim";
        String value = "the expected value";
        Map<String, Object> userInfoClaimsMap = new HashMap<String, Object>();
        userInfoClaimsMap.put(claim, value);
        OpenIdClaims userInfoClaims = new OpenIdClaimsImpl(userInfoClaimsMap);
        mockery.checking(new Expectations() {
            {
                one(accessToken).isJWT();
                will(returnValue(true));
                one(accessToken).getClaim(claim);
                will(returnValue(null));
                one(idTokenClaims).getClaimValue(claim, String.class);
                will(returnValue(null));
            }
        });

        String result = identityStore.getClaimValueFromTokens(claim, accessToken, idTokenClaims, userInfoClaims, String.class);
        assertEquals(value, result);
    }

    @Test
    public void test_getClaimValueFromTokens_string_claimMissing() throws MalformedClaimException {
        String claim = "myClaim";
        Map<String, Object> userInfoClaimsMap = new HashMap<String, Object>();
        OpenIdClaims userInfoClaims = new OpenIdClaimsImpl(userInfoClaimsMap);
        mockery.checking(new Expectations() {
            {
                one(accessToken).isJWT();
                will(returnValue(true));
                one(accessToken).getClaim(claim);
                will(returnValue(null));
                one(idTokenClaims).getClaimValue(claim, String.class);
                will(returnValue(null));
            }
        });

        String result = identityStore.getClaimValueFromTokens(claim, accessToken, idTokenClaims, userInfoClaims, String.class);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_getClaimValueFromTokens_list_claimEmptyInAccessToken_claimNonEmptyElsewhere() throws MalformedClaimException {
        String claim = "myClaim";
        Map<String, Object> userInfoClaimsMap = new HashMap<String, Object>();
        List<String> value = List.of("one", "two", "three");
        userInfoClaimsMap.put(claim, value);
        OpenIdClaims userInfoClaims = new OpenIdClaimsImpl(userInfoClaimsMap);
        mockery.checking(new Expectations() {
            {
                one(accessToken).isJWT();
                will(returnValue(true));
                one(accessToken).getClaim(claim);
                will(returnValue(List.of()));
                one(idTokenClaims).getClaimValue(claim, List.class);
                will(returnValue(null));
            }
        });

        List result = identityStore.getClaimValueFromTokens(claim, accessToken, idTokenClaims, userInfoClaims, List.class);
        assertEquals(value, result);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_getClaimValueFromAccessToken() throws MalformedClaimException {
        String claim = "myClaim";
        Map<String, Object> userInfoClaimsMap = new HashMap<String, Object>();
        List<String> value = List.of("one", "two", "three");
        userInfoClaimsMap.put(claim, value);
        OpenIdClaims userInfoClaims = new OpenIdClaimsImpl(userInfoClaimsMap);

        List<String> valueFromAccessToken = List.of("four", "five", "six");

        mockery.checking(new Expectations() {
            {
                one(accessToken).isJWT();
                will(returnValue(true));
                one(accessToken).getClaim(claim);
                will(returnValue(valueFromAccessToken));
            }
        });

        List result = identityStore.getClaimValueFromTokens(claim, accessToken, idTokenClaims, userInfoClaims, List.class);
        assertEquals("Incorrect list returned on getClaimValueFromTokens", valueFromAccessToken, result);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_getClaimValueFromIdToken() throws MalformedClaimException {
        String claim = "myClaim";
        Map<String, Object> userInfoClaimsMap = new HashMap<String, Object>();
        List<String> value = List.of("one", "two", "three");
        userInfoClaimsMap.put(claim, value);
        OpenIdClaims userInfoClaims = new OpenIdClaimsImpl(userInfoClaimsMap);

        List<String> valueFromIdToken = List.of("four", "five", "six");

        mockery.checking(new Expectations() {
            {
                one(accessToken).isJWT();
                will(returnValue(true));
                one(accessToken).getClaim(claim);
                will(returnValue(List.of()));
                one(idTokenClaims).getClaimValue(claim, List.class);
                will(returnValue(valueFromIdToken));
            }
        });

        List result = identityStore.getClaimValueFromTokens(claim, accessToken, idTokenClaims, userInfoClaims, List.class);
        assertEquals("Incorrect list returned on getClaimValueFromTokens", valueFromIdToken, result);
    }

    @Test
    public void test_getClaimFromUserInfo_nullClaims() {
        String claim = "myClaim";
        String result = identityStore.getClaimFromUserInfo(null, claim, String.class);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getClaimFromUserInfo_string_missing() {
        String claim = "myClaim";
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        OpenIdClaims claims = new OpenIdClaimsImpl(claimsMap);

        String result = identityStore.getClaimFromUserInfo(claims, claim, String.class);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getClaimFromUserInfo_string_wrongType() {
        String claim = "myClaim";
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        claimsMap.put(claim, 112358);
        OpenIdClaims claims = new OpenIdClaimsImpl(claimsMap);

        try {
            String result = identityStore.getClaimFromUserInfo(claims, claim, String.class);
            fail("Should have thrown an exception but got [" + result + "].");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    @Test
    public void test_getClaimFromUserInfo_string() {
        String claim = "myClaim";
        String value = "the expected value";
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        claimsMap.put(claim, value);
        OpenIdClaims claims = new OpenIdClaimsImpl(claimsMap);

        String result = identityStore.getClaimFromUserInfo(claims, claim, String.class);
        assertEquals(value, result);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_getClaimFromUserInfo_list_missing() {
        String claim = "myClaim";
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        OpenIdClaims claims = new OpenIdClaimsImpl(claimsMap);

        List result = identityStore.getClaimFromUserInfo(claims, claim, List.class);
        assertEquals(List.of(), result);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_getClaimFromUserInfo_list_rawString() {
        String claim = "myClaim";
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        String value = "some claim value";
        claimsMap.put(claim, value);
        OpenIdClaims claims = new OpenIdClaimsImpl(claimsMap);

        List result = identityStore.getClaimFromUserInfo(claims, claim, List.class);
        assertEquals(List.of(value), result);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_getClaimFromUserInfo_list_wrongType() {
        String claim = "myClaim";
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        boolean value = true;
        claimsMap.put(claim, value);
        OpenIdClaims claims = new OpenIdClaimsImpl(claimsMap);

        try {
            List result = identityStore.getClaimFromUserInfo(claims, claim, List.class);
            fail("Should have thrown an exception but got [" + result + "].");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_getClaimFromUserInfo_list() {
        String claim = "myClaim";
        List<Object> value = Arrays.asList("group1", 1234);
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        claimsMap.put(claim, value);
        OpenIdClaims claims = new OpenIdClaimsImpl(claimsMap);

        List result = identityStore.getClaimFromUserInfo(claims, claim, List.class);
        assertEquals(value, result);
    }

    @Test
    public void test_getClaimFromUserInfo_unsupportedType() {
        String claim = "myClaim";
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        String value = "some claim value";
        claimsMap.put(claim, value);
        OpenIdClaims claims = new OpenIdClaimsImpl(claimsMap);

        Long result = identityStore.getClaimFromUserInfo(claims, claim, Long.class);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_valueExistsAndIsNotEmpty_null() {
        String claim = null;
        assertFalse(identityStore.valueExistsAndIsNotEmpty(claim, String.class));
    }

    @Test
    public void test_valueExistsAndIsNotEmpty_string_empty() {
        String claim = "";
        assertFalse(identityStore.valueExistsAndIsNotEmpty(claim, String.class));
    }

    @Test
    public void test_valueExistsAndIsNotEmpty_string_nonEmpty() {
        String claim = " ";
        assertTrue(identityStore.valueExistsAndIsNotEmpty(claim, String.class));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_valueExistsAndIsNotEmpty_set_empty() {
        Set claim = new HashSet<>();
        assertFalse(identityStore.valueExistsAndIsNotEmpty(claim, Set.class));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void test_valueExistsAndIsNotEmpty_set_nonEmpty() {
        Set claim = new HashSet<>();
        claim.add("test");
        claim.add(123);
        assertTrue(identityStore.valueExistsAndIsNotEmpty(claim, Set.class));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_valueExistsAndIsNotEmpty_list_empty() {
        List claim = new ArrayList<>();
        assertFalse(identityStore.valueExistsAndIsNotEmpty(claim, List.class));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void test_valueExistsAndIsNotEmpty_list_nonEmpty() {
        List claim = new ArrayList<>();
        claim.add("group");
        assertTrue(identityStore.valueExistsAndIsNotEmpty(claim, List.class));
    }

    @Test
    public void test_valueExistsAndIsNotEmpty_otherType() {
        Long claim = 0L;
        // Anything other than String and Set will just return true
        assertTrue(identityStore.valueExistsAndIsNotEmpty(claim, Long.class));
    }

    /**
     * Test with an existing AccessTokenString that we parse it, create a claims map and mark it as JWT.
     */
    @Test
    public void test_createAccessTokenFromTokenResponse_jwt() {
        String JWT_ACCESS_TOKEN_STRING = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJKYWNrc29uIiwiYXRfaGFzaCI6ImJrR0NWcy1EcndMMGMycEtoN0ZVNGciLCJyZWFsbU5hbWUiOiJCYXNpY1JlZ2lzdHJ5IiwidW5pcXVlU2VjdXJpdHlOYW1lIjoiSmFja3NvbiIsInNpZCI6InFTTzBXeWs0VVNjMWFCYlMyUVlmIiwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTQ0My9vaWRjL2VuZHBvaW50L09QIiwiYXVkIjoib2lkY2NsaWVudCIsImV4cCI6MTY2MTIwNzIwOCwiaWF0IjoxNjYxMjAwMDA4fQ.a4PRKYeG18vsmBOukcjmNve10KnVSBGVgwh2RqXkNbY";
        JSONObject tokenResponseJson = new JSONObject();
        tokenResponseJson.put(TokenConstants.ACCESS_TOKEN, JWT_ACCESS_TOKEN_STRING);

        TokenResponse tokenResponse = new TokenResponse(tokenResponseJson);
        // should contain uniqueSecurityName=Jackson when parsed.

        AccessToken result = identityStore.createAccessTokenFromTokenResponse(10000, tokenResponse);
        assertNotNull("Should have been able to parse claimsMap", result.getJwtClaims());
        assertNotNull("Should have been able to parse claimsMap", result.getClaims());
        assertEquals("Should contain uniqueSecurityName=Jackson in claimsMap", "Jackson", result.getClaim("uniqueSecurityName"));
        assertFalse("Claims map should be populated", result.getClaims().isEmpty());
        assertTrue("Should be marked as JWT token", result.isJWT());

    }

    /**
     * Test with a non jwt string, make sure we don't have any errors and mark it as non-jwt
     */
    @Test
    public void test_createAccessTokenFromTokenResponse_notjwt() {
        JSONObject tokenResponseJson = new JSONObject();
        tokenResponseJson.put(TokenConstants.ACCESS_TOKEN, "HxcmkeFrEXVkB4KxpudAW9GDEBDgNtcGjrBAIUkW");

        TokenResponse tokenResponse = new TokenResponse(tokenResponseJson);

        AccessToken result = identityStore.createAccessTokenFromTokenResponse(10000, tokenResponse);
        assertEquals("Should not have claimsMap", jakarta.security.enterprise.identitystore.openid.JwtClaims.NONE, result.getJwtClaims());
        assertTrue("Claims map should be not populated", result.getClaims().isEmpty());
        assertFalse("Should not be marked as JWT token", result.isJWT());
    }

    /**
     * Test with a semi-jwt string where the second part, when parse, does not create a valid claims map.
     */
    @Test
    public void test_createAccessTokenFromTokenResponse_badjwt() {
        String BAD_JWT_ACCESS_TOKEN_STRING = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJKYWNrc29uIiwiYXRfaGFzaCI6ImJdHJ5IiwidW5pcXVlU2VjdXJpdHlOYW1lIjoiSmFja3NvbiIsInNpZCI6InFTTzBXeWs0VVNjMWFCYlMyUVlmIiwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTQ0My9vaWRjL2VuZHBvaW50L09QIiwiYXVkIjoib2lkY2NsaWVudCIsImV4cCI6MTY2MTIwNzIwOCwiaWF0IjoxNjYxMjAwMDA4fQ.a4PRKYeG18vsmBOukcjmNve10KnVSBGVgwh2RqXkNbY";
        JSONObject tokenResponseJson = new JSONObject();
        tokenResponseJson.put(TokenConstants.ACCESS_TOKEN, BAD_JWT_ACCESS_TOKEN_STRING);

        TokenResponse tokenResponse = new TokenResponse(tokenResponseJson);

        AccessToken result = identityStore.createAccessTokenFromTokenResponse(10000, tokenResponse);
        assertEquals("Should not have claimsMap", jakarta.security.enterprise.identitystore.openid.JwtClaims.NONE, result.getJwtClaims());
        assertTrue("Claims map should be not populated", result.getClaims().isEmpty());
        assertTrue("Should be marked as JWT token", result.isJWT());
    }

}
