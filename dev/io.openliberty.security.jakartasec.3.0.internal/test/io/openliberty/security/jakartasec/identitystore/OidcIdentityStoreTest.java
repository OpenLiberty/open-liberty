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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.jakartasec.tokens.OpenIdClaimsImpl;
import io.openliberty.security.oidcclientcore.client.ClaimsMappingConfig;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
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

    // TODO - createSuccessfulCredentialValidationResult

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

    // TODO - getCallerName

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

    // TODO - getCallerGroups

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

    // TODO - getClaimFromAccessToken

    // TODO - getClaimFromIdToken

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

}
