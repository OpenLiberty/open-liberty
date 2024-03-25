/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.HashUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.server.plugins.IDTokenImpl;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.security.test.common.jwt.utils.JwtUnitTestUtils;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import test.common.SharedOutputManager;

public class LogoutTokenBuilderTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("OpenIdConnect*=all");

    private static final String CWWKS1643E_LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN = "CWWKS1643E";
    private static final String CWWKS1646E_ID_TOKEN_ISSUER_NOT_THIS_OP = "CWWKS1646E";
    private static final String CWWKS1647E_ID_TOKEN_MISSING_REQUIRED_CLAIMS = "CWWKS1647E";
    private static final String CWWKS1953E_ERROR_BUILDING_LOGOUT_TOKEN_BASED_ON_ID_TOKEN_CLAIMS = "CWWKS1953E";

    private final String providerId = "OP";
    private final String issuerIdentifier = "https://localhost/oidc/endpoint/OP";
    private final String client1Id = "client1";
    private final String client2Id = "client2";
    private final String client3Id = "client3";
    private final String client1Secret = "client1secret";
    private final String client2Secret = "client2secret";
    private final String client3Secret = "client3secret";
    private final String subject = "testuser";
    private final String sid = "somesidvalue";
    private final String idToken1Id = "idToken1";
    private final String idToken2Id = "idToken2";
    private final String idToken3Id = "idToken3";
    private final String customIdTokenId = "customIdToken";

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final OidcServerConfig oidcServerConfig = mockery.mock(OidcServerConfig.class);
    private final OAuth20Provider oauth20provider = mockery.mock(OAuth20Provider.class);
    private final OidcOAuth20ClientProvider clientProvider = mockery.mock(OidcOAuth20ClientProvider.class);
    private final OidcBaseClient client1 = mockery.mock(OidcBaseClient.class, "client1");
    private final OidcBaseClient client2 = mockery.mock(OidcBaseClient.class, "client2");
    private final OidcBaseClient client3 = mockery.mock(OidcBaseClient.class, "client3");
    private final OAuth20EnhancedTokenCache tokenCache = mockery.mock(OAuth20EnhancedTokenCache.class);
    private final OAuth20Token accessToken1 = mockery.mock(OAuth20Token.class, "accessToken1");
    private final OAuth20Token accessToken2 = mockery.mock(OAuth20Token.class, "accessToken2");
    private final IDTokenImpl idToken = mockery.mock(IDTokenImpl.class, "idToken");
    private final IDTokenImpl idToken1 = mockery.mock(IDTokenImpl.class, "idToken1");
    private final IDTokenImpl idToken2 = mockery.mock(IDTokenImpl.class, "idToken2");
    private final IDTokenImpl idToken3 = mockery.mock(IDTokenImpl.class, "idToken3");

    private LogoutTokenBuilder builder;

    private class MockLogoutTokenBuilder extends LogoutTokenBuilder {
        public MockLogoutTokenBuilder(HttpServletRequest request, OidcServerConfig oidcServerConfig) {
            super(request, oidcServerConfig);
        }

        @Override
        OAuth20Provider getOAuth20Provider(OidcServerConfig oidcServerConfig) {
            return oauth20provider;
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getTokenCache();
                will(returnValue(tokenCache));
                allowing(oidcServerConfig).getProviderId();
                will(returnValue(providerId));
                allowing(client1).getClientId();
                will(returnValue(client1Id));
                allowing(client2).getClientId();
                will(returnValue(client2Id));
                allowing(client3).getClientId();
                will(returnValue(client3Id));
                allowing(accessToken1).getType();
                will(returnValue(OAuth20Constants.ACCESS_TOKEN));
                allowing(accessToken1).getGrantType();
                will(returnValue(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
                allowing(accessToken1).getTokenString();
                will(returnValue("accessToken1String"));
                allowing(accessToken2).getType();
                will(returnValue(OAuth20Constants.ACCESS_TOKEN));
                allowing(accessToken2).getGrantType();
                will(returnValue(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
                allowing(accessToken2).getTokenString();
                will(returnValue("accessToken2String"));
                allowing(idToken1).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken1).getClientId();
                will(returnValue(client1Id));
                allowing(idToken1).getTokenString();
                will(returnValue(getIdToken1String()));
                allowing(idToken1).getId();
                will(returnValue(idToken1Id));
                allowing(idToken2).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken2).getClientId();
                will(returnValue(client2Id));
                allowing(idToken2).getTokenString();
                will(returnValue(getIdToken2String()));
                allowing(idToken2).getId();
                will(returnValue(idToken2Id));
                allowing(idToken3).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken3).getClientId();
                will(returnValue(client3Id));
                allowing(idToken3).getTokenString();
                will(returnValue(getIdToken3String()));
                allowing(idToken3).getId();
                will(returnValue(idToken3Id));
            }
        });
        builder = new MockLogoutTokenBuilder(request, oidcServerConfig);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_buildLogoutTokensForUser_noCachedTokens() throws Exception {
        setTokenCacheExpectations(subject);

        Map<OidcBaseClient, Set<String>> result = builder.buildLogoutTokensForUser(subject);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty but wasn't: " + result, result.isEmpty());
    }

    @Test
    public void test_buildLogoutTokensForUser_oneCachedToken_accessTokenType() throws Exception {
        setTokenCacheExpectations(subject, accessToken1);
        setClientLookupExpectations();

        Map<OidcBaseClient, Set<String>> result = builder.buildLogoutTokensForUser(subject);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty but wasn't: " + result, result.isEmpty());
    }

    @Test
    public void test_buildLogoutTokensForUser_oneCachedToken_idToken() throws Exception {
        setTokenCacheExpectations(subject, idToken1);
        setClientLookupExpectations(client1);

        final String idTokenAccessTokenKey = "id token access token key";
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
                one(tokenCache).remove(idToken1Id);
                one(idToken1).getAccessTokenKey();
                will(returnValue(idTokenAccessTokenKey));
                one(tokenCache).get(idTokenAccessTokenKey);
                will(returnValue(null));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        Map<OidcBaseClient, Set<String>> clientsToLogoutTokens = builder.buildLogoutTokensForUser(subject);
        verifyLogoutTokensMapContainsExpectedClients(clientsToLogoutTokens, client1);

        Set<String> client1LogoutTokens = clientsToLogoutTokens.get(client1);
        verifyLogoutTokensForClient(client1Id, client1LogoutTokens, 1, null);
    }

    @Test
    public void test_buildLogoutTokensForUser_multipleCachedIdTokens() throws Exception {
        setTokenCacheExpectations(subject, idToken1, idToken, idToken2, idToken3);
        setClientLookupExpectations(client1, client2, client3);

        // One cached ID token has a sid claim
        JSONObject cachedTokenClaims = getIdTokenClaims(subject, issuerIdentifier, client1Id);
        cachedTokenClaims.put("sid", sid);
        String cachedIdTokenString = JwtUnitTestUtils.getHS256Jws(cachedTokenClaims, client1Secret);

        setCustomIdTokenExpectations(idToken, client1Id, cachedIdTokenString);
        final String idTokenAccessTokenKey = "id token access token key";
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                allowing(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
                one(client2).getBackchannelLogoutUri();
                will(returnValue(null));
                one(client3).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client3"));
                one(tokenCache).remove(idToken1Id);
                one(idToken1).getAccessTokenKey();
                will(returnValue(idTokenAccessTokenKey));
                one(tokenCache).get(idTokenAccessTokenKey);
                will(returnValue(null));
                one(tokenCache).remove(customIdTokenId);
                one(idToken).getAccessTokenKey();
                will(returnValue(idTokenAccessTokenKey));
                one(tokenCache).get(idTokenAccessTokenKey);
                will(returnValue(null));
                one(tokenCache).remove(idToken3Id);
                one(idToken3).getAccessTokenKey();
                will(returnValue(idTokenAccessTokenKey));
                one(tokenCache).get(idTokenAccessTokenKey);
                will(returnValue(null));
            }
        });
        // Should create three logout tokens
        setJwtCreationExpectations(client1, client1Secret);
        setJwtCreationExpectations(client1, client1Secret);
        setJwtCreationExpectations(client3, client3Secret);

        Map<OidcBaseClient, Set<String>> clientsToLogoutTokens = builder.buildLogoutTokensForUser(subject);
        verifyLogoutTokensMapContainsExpectedClients(clientsToLogoutTokens, client1, client3);

        Set<String> client1LogoutTokens = clientsToLogoutTokens.get(client1);
        verifyLogoutTokensForClient(client1Id, client1LogoutTokens, 2, sid);

        Set<String> client3LogoutTokens = clientsToLogoutTokens.get(client3);
        verifyLogoutTokensForClient(client3Id, client3LogoutTokens, 1, null);
    }

    @Test
    public void test_getClaimsFromIdTokenString_emptyClaims() throws Exception {
        String idTokenString = JwtUnitTestUtils.getHS256Jws(new JSONObject(), client1Secret);
        try {
            JwtClaims result = builder.getClaimsFromIdTokenString(idTokenString);
            fail("Should have thrown an exception but got: " + result);
        } catch (LogoutTokenBuilderException e) {
            verifyException(e, CWWKS1643E_LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN + ".*" + CWWKS1647E_ID_TOKEN_MISSING_REQUIRED_CLAIMS);
        }
    }

    @Test
    public void test_getClaimsFromIdTokenString_goldenPathClaims() throws Exception {
        JwtClaims input = getClaims(subject, issuerIdentifier, client1Id);
        input.setIssuedAtToNow();
        input.setGeneratedJwtId();
        input.setExpirationTimeMinutesInTheFuture(60);
        input.setNotBeforeMinutesInThePast(10);

        String idTokenString = JwtUnitTestUtils.getHS256Jws(JSONObject.parse(input.toJson()), client1Secret);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        JwtClaims result = builder.getClaimsFromIdTokenString(idTokenString);
        assertNotNull("Returned claims object should not have been null but was.", result);
        assertEquals("Returned claims object did not match the input.", input.getClaimsMap(), result.getClaimsMap());
    }

    @Test
    public void test_verifyIdTokenContainsRequiredClaims_emptyClaims() {
        JwtClaims claims = new JwtClaims();

        try {
            builder.verifyIdTokenContainsRequiredClaims(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS1647E_ID_TOKEN_MISSING_REQUIRED_CLAIMS + ".*" + "iss, sub, aud");
        }
    }

    @Test
    public void test_verifyIdTokenContainsRequiredClaims_nonEmptyClaims_missingAllRequired() {
        JwtClaims claims = new JwtClaims();
        claims.setStringClaim("claim1", "value");
        claims.setStringClaim("claim2", "value");
        claims.setClaim("int-claim", 123);

        try {
            builder.verifyIdTokenContainsRequiredClaims(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS1647E_ID_TOKEN_MISSING_REQUIRED_CLAIMS + ".*" + "iss, sub, aud");
        }
    }

    @Test
    public void test_verifyIdTokenContainsRequiredClaims_missingIss() {
        JwtClaims claims = new JwtClaims();
        claims.setStringClaim("sub", subject);
        claims.setStringClaim("aud", client1Id);

        try {
            builder.verifyIdTokenContainsRequiredClaims(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS1647E_ID_TOKEN_MISSING_REQUIRED_CLAIMS + ".*" + "iss");
        }
    }

    @Test
    public void test_verifyIdTokenContainsRequiredClaims_missingSub() {
        JwtClaims claims = new JwtClaims();
        claims.setStringClaim("iss", issuerIdentifier);
        claims.setStringClaim("aud", client1Id);

        try {
            builder.verifyIdTokenContainsRequiredClaims(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS1647E_ID_TOKEN_MISSING_REQUIRED_CLAIMS + ".*" + "sub");
        }
    }

    @Test
    public void test_verifyIdTokenContainsRequiredClaims_missingAud() {
        JwtClaims claims = new JwtClaims();
        claims.setStringClaim("iss", issuerIdentifier);
        claims.setStringClaim("sub", subject);

        try {
            builder.verifyIdTokenContainsRequiredClaims(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS1647E_ID_TOKEN_MISSING_REQUIRED_CLAIMS + ".*" + "aud");
        }
    }

    @Test
    public void test_verifyIdTokenContainsRequiredClaims_allClaimsPresent() {
        JwtClaims claims = new JwtClaims();
        claims.setStringClaim("iss", issuerIdentifier);
        claims.setStringClaim("sub", subject);
        claims.setStringClaim("aud", client1Id);
        claims.setStringClaim("claim2", "value");
        claims.setClaim("int-claim", 123);

        try {
            builder.verifyIdTokenContainsRequiredClaims(claims);
        } catch (Exception e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyIssuer_missingIss() {
        JwtClaims claims = getClaims(subject, null, client1Id);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        try {
            builder.verifyIssuer(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (MalformedClaimException e) {
            fail("Did not through the expected exception. Got: " + e);
        } catch (IdTokenDifferentIssuerException e) {
            verifyException(e, CWWKS1646E_ID_TOKEN_ISSUER_NOT_THIS_OP);
        }
    }

    @Test
    public void test_verifyIssuer_emptyIss() {
        JwtClaims claims = getClaims(subject, "", client1Id);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        try {
            builder.verifyIssuer(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (MalformedClaimException e) {
            fail("Did not through the expected exception. Got: " + e);
        } catch (IdTokenDifferentIssuerException e) {
            verifyException(e, CWWKS1646E_ID_TOKEN_ISSUER_NOT_THIS_OP);
        }
    }

    @Test
    public void test_verifyIssuer_issSuperstring() {
        JwtClaims claims = getClaims(subject, issuerIdentifier + "2", client1Id);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        try {
            builder.verifyIssuer(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (MalformedClaimException e) {
            fail("Did not through the expected exception. Got: " + e);
        } catch (IdTokenDifferentIssuerException e) {
            verifyException(e, CWWKS1646E_ID_TOKEN_ISSUER_NOT_THIS_OP);
        }
    }

    @Test
    public void test_verifyIssuer_matchingIss_issuerIdentifierConfigured() {
        JwtClaims claims = getClaims(subject, issuerIdentifier, client1Id);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        try {
            builder.verifyIssuer(claims);
        } catch (Exception e) {
            fail("Should not have thrown an exception but got: " + e);
        }
    }

    @Test
    public void test_verifyIssuer_matchingIss_noIssuerIdentifierConfigured_issOidcEndpoint() {
        final String scheme = "https";
        final String serverName = "localhost";
        final String expectedIssuerPath = "/oidc/endpoint/" + providerId;
        final String iss = scheme + "://" + serverName + expectedIssuerPath;
        JwtClaims claims = getClaims(subject, iss, client1Id);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(null));
                one(request).getScheme();
                will(returnValue(scheme));
                one(request).getServerName();
                will(returnValue(serverName));
                one(request).getServerPort();
                will(returnValue(443));
            }
        });
        try {
            builder.verifyIssuer(claims);
        } catch (Exception e) {
            fail("Should not have thrown an exception but got: " + e);
        }
    }

    @Test
    public void test_verifyIssuer_matchingIss_noIssuerIdentifierConfigured_issOidcProviders() {
        final String scheme = "https";
        final String serverName = "localhost";
        final String expectedIssuerPath = "/oidc/providers/" + providerId;
        final String iss = scheme + "://" + serverName + expectedIssuerPath;
        JwtClaims claims = getClaims(subject, iss, client1Id);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(null));
                one(request).getScheme();
                will(returnValue(scheme));
                one(request).getServerName();
                will(returnValue(serverName));
                one(request).getServerPort();
                will(returnValue(443));
            }
        });
        try {
            builder.verifyIssuer(claims);
        } catch (Exception e) {
            fail("Should not have thrown an exception but got: " + e);
        }
    }

    @Test
    public void test_verifyIssuer_notMatchingIss_noIssuerIdentifierConfigured() {
        final String scheme = "https";
        final String serverName = "localhost";
        final String issuer = scheme + "://" + serverName + "/oidc/providers/OP2";
        JwtClaims claims = getClaims(subject, issuer, client1Id);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(null));
                one(request).getScheme();
                will(returnValue(scheme));
                one(request).getServerName();
                will(returnValue(serverName));
                one(request).getServerPort();
                will(returnValue(443));
            }
        });
        try {
            builder.verifyIssuer(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (MalformedClaimException e) {
            fail("Did not through the expected exception. Got: " + e);
        } catch (IdTokenDifferentIssuerException e) {
            verifyException(e, CWWKS1646E_ID_TOKEN_ISSUER_NOT_THIS_OP);
        }
    }

    // TODO - removeUserIdTokensFromCache

    @Test
    public void test_removeUserAccessTokensFromCache_noCachedTokens() throws Exception {
        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = new HashMap<OidcBaseClient, List<OAuth20Token>>();
        clientsToCachedIdTokens.put(client1, Arrays.asList(idToken1));
        clientsToCachedIdTokens.put(client2, Arrays.asList(idToken2));

        builder.removeUserAccessTokensFromCache(new ArrayList<>(), clientsToCachedIdTokens);
    }

    @Test
    public void test_removeUserAccessTokensFromCache_noCachedAccessTokens() throws Exception {
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(idToken1, idToken2);
        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = new HashMap<OidcBaseClient, List<OAuth20Token>>();
        clientsToCachedIdTokens.put(client1, Arrays.asList(idToken1));
        clientsToCachedIdTokens.put(client2, Arrays.asList(idToken2));

        builder.removeUserAccessTokensFromCache(allCachedUserTokens, clientsToCachedIdTokens);
    }

    @Test
    public void test_removeUserAccessTokensFromCache_noAccessTokensAssociatedWithClientsLoggingOut() throws Exception {
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1, accessToken2);
        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = new HashMap<OidcBaseClient, List<OAuth20Token>>();
        clientsToCachedIdTokens.put(client1, Arrays.asList(idToken1));
        clientsToCachedIdTokens.put(client2, Arrays.asList(idToken2));

        mockery.checking(new Expectations() {
            {
                one(accessToken1).getClientId();
                will(returnValue(client3Id));
                one(accessToken2).getClientId();
                will(returnValue(client3Id));
            }
        });
        builder.removeUserAccessTokensFromCache(allCachedUserTokens, clientsToCachedIdTokens);
    }

    @Test
    public void test_removeUserAccessTokensFromCache_singleAccessToken_appPasswordGrantType() throws Exception {
        OAuth20TokenImpl accessToken1 = mockery.mock(OAuth20TokenImpl.class, "accesstoken1-impl");
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1);

        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = new HashMap<OidcBaseClient, List<OAuth20Token>>();
        clientsToCachedIdTokens.put(client1, Arrays.asList(idToken1));

        mockery.checking(new Expectations() {
            {
                one(accessToken1).getType();
                will(returnValue(OAuth20Constants.ACCESS_TOKEN));
                one(accessToken1).getGrantType();
                will(returnValue(OAuth20Constants.GRANT_TYPE_APP_PASSWORD));
            }
        });
        builder.removeUserAccessTokensFromCache(allCachedUserTokens, clientsToCachedIdTokens);
    }

    @Test
    public void test_removeUserAccessTokensFromCache_singleAccessToken_appTokenGrantType() throws Exception {
        OAuth20TokenImpl accessToken1 = mockery.mock(OAuth20TokenImpl.class, "accesstoken1-impl");
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1);

        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = new HashMap<OidcBaseClient, List<OAuth20Token>>();
        clientsToCachedIdTokens.put(client1, Arrays.asList(idToken1));

        mockery.checking(new Expectations() {
            {
                one(accessToken1).getType();
                will(returnValue(OAuth20Constants.ACCESS_TOKEN));
                one(accessToken1).getGrantType();
                will(returnValue(OAuth20Constants.GRANT_TYPE_APP_TOKEN));
            }
        });
        builder.removeUserAccessTokensFromCache(allCachedUserTokens, clientsToCachedIdTokens);
    }

    @Test
    public void test_removeUserAccessTokensFromCache_singleAccessToken_implicitGrantType() throws Exception {
        OAuth20TokenImpl accessToken1 = mockery.mock(OAuth20TokenImpl.class, "accesstoken1-impl");
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1);

        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = new HashMap<OidcBaseClient, List<OAuth20Token>>();
        clientsToCachedIdTokens.put(client1, Arrays.asList(idToken1));

        String accessTokenString = "someaccesstokenstring1";
        String refreshTokenString = "myrefreshtoken1";
        OAuth20Token refreshToken = getRefreshTokenExpectations(accessToken1, OIDCConstants.TOKENTYPE_ACCESS_TOKEN);

        mockery.checking(new Expectations() {
            {
                one(accessToken1).getGrantType();
                will(returnValue(OAuth20Constants.GRANT_TYPE_IMPLICIT));
                one(accessToken1).getClientId();
                will(returnValue(client1Id));
                one(accessToken1).getTokenString();
                will(returnValue(accessTokenString));
                one(tokenCache).remove(accessTokenString);
                one(refreshToken).getScope();
                will(returnValue(new String[] { "openid" }));
                one(refreshToken).getTokenString();
                will(returnValue(refreshTokenString));
                one(tokenCache).remove(refreshTokenString);
            }
        });
        builder.removeUserAccessTokensFromCache(allCachedUserTokens, clientsToCachedIdTokens);
    }

    @Test
    public void test_removeUserAccessTokensFromCache_multipleAccessTokens_oneValid() throws Exception {
        OAuth20TokenImpl accessToken1 = mockery.mock(OAuth20TokenImpl.class, "accesstoken1-impl");
        OAuth20TokenImpl accessToken2 = mockery.mock(OAuth20TokenImpl.class, "accesstoken2-impl");
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1, accessToken2);

        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = new HashMap<OidcBaseClient, List<OAuth20Token>>();
        clientsToCachedIdTokens.put(client1, Arrays.asList(idToken1));
        clientsToCachedIdTokens.put(client2, Arrays.asList(idToken2));

        String accessTokenString = "someaccesstokenstring1";
        String refreshTokenString = "myrefreshtoken1";
        OAuth20Token refreshToken = getRefreshTokenExpectations(accessToken1, OIDCConstants.TOKENTYPE_ACCESS_TOKEN);

        mockery.checking(new Expectations() {
            {
                one(accessToken1).getGrantType();
                will(returnValue(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
                one(accessToken1).getClientId();
                will(returnValue(client1Id));
                one(accessToken1).getTokenString();
                will(returnValue(accessTokenString));
                one(tokenCache).remove(accessTokenString);
                one(refreshToken).getScope();
                will(returnValue(new String[] { "openid" }));
                one(refreshToken).getTokenString();
                will(returnValue(refreshTokenString));
                one(tokenCache).remove(refreshTokenString);
                // Second access token is associated with some other client not being logged out
                one(accessToken2).getType();
                will(returnValue(OAuth20Constants.ACCESS_TOKEN));
                one(accessToken2).getGrantType();
                will(returnValue(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
                one(accessToken2).getClientId();
                will(returnValue(client3Id));
            }
        });
        builder.removeUserAccessTokensFromCache(allCachedUserTokens, clientsToCachedIdTokens);
    }

    @Test
    public void test_removeAccessTokenAndAssociatedRefreshTokenFromCache_opaqueToken() throws Exception {
        OAuth20TokenImpl accessToken = mockery.mock(OAuth20TokenImpl.class);

        String accessTokenString = "someaccesstokenstring";
        String refreshTokenString = "myrefreshtoken";
        OAuth20Token refreshToken = getRefreshTokenExpectations(accessToken, OIDCConstants.TOKENTYPE_ACCESS_TOKEN);
        mockery.checking(new Expectations() {
            {
                one(accessToken).getTokenString();
                will(returnValue(accessTokenString));
                one(tokenCache).remove(accessTokenString);
                one(refreshToken).getScope();
                will(returnValue(new String[] { "openid" }));
                one(refreshToken).getTokenString();
                will(returnValue(refreshTokenString));
                one(tokenCache).remove(refreshTokenString);
            }
        });
        builder.removeAccessTokenAndAssociatedRefreshTokenFromCache(accessToken);
    }

    @Test
    public void test_removeAccessTokenAndAssociatedRefreshTokenFromCache_jwtToken() throws Exception {
        OAuth20TokenImpl accessToken = mockery.mock(OAuth20TokenImpl.class);

        String accessTokenString = "xxx.yyy.zzz";
        String accessTokenLookupString = HashUtils.digest(accessTokenString);
        String refreshTokenString = "myrefreshtoken";
        OAuth20Token refreshToken = getRefreshTokenExpectations(accessToken, OIDCConstants.TOKENTYPE_ACCESS_TOKEN);
        mockery.checking(new Expectations() {
            {
                one(accessToken).getTokenString();
                will(returnValue(accessTokenString));
                one(tokenCache).remove(accessTokenLookupString);
                one(refreshToken).getScope();
                will(returnValue(new String[] { "openid" }));
                one(refreshToken).getTokenString();
                will(returnValue(refreshTokenString));
                one(tokenCache).remove(refreshTokenString);

            }
        });
        builder.removeAccessTokenAndAssociatedRefreshTokenFromCache(accessToken);
    }

    @Test
    public void test_removeRefreshTokenAssociatedWithOAuthTokenFromCache_accessToken_noMatchingRefreshToken() throws Exception {
        OAuth20TokenImpl accessToken = mockery.mock(OAuth20TokenImpl.class);
        mockery.checking(new Expectations() {
            {
                one(accessToken).getType();
                will(returnValue(OAuth20Constants.ACCESS_TOKEN));
                one(accessToken).getRefreshTokenKey();
                will(returnValue(null));
            }
        });
        builder.removeRefreshTokenAssociatedWithOAuthTokenFromCache(accessToken);
    }

    @Test
    public void test_removeRefreshTokenAssociatedWithOAuthTokenFromCache_accessToken_withMatchingRefreshToken() throws Exception {
        String refreshTokenString = "refreshtokenstring";
        OAuth20TokenImpl accessToken = mockery.mock(OAuth20TokenImpl.class);
        OAuth20Token refreshToken = getRefreshTokenExpectations(accessToken, OIDCConstants.TOKENTYPE_ACCESS_TOKEN);
        mockery.checking(new Expectations() {
            {
                one(refreshToken).getScope();
                will(returnValue(new String[] { "openid" }));
                one(refreshToken).getTokenString();
                will(returnValue(refreshTokenString));
                one(tokenCache).remove(refreshTokenString);
            }
        });
        builder.removeRefreshTokenAssociatedWithOAuthTokenFromCache(accessToken);
    }

    @Test
    public void test_removeRefreshTokenAssociatedWithOAuthTokenFromCache_accessToken_withMatchingRefreshToken_refreshTokenHasOfflineAccess() throws Exception {
        OAuth20TokenImpl accessToken = mockery.mock(OAuth20TokenImpl.class);
        OAuth20Token refreshToken = getRefreshTokenExpectations(accessToken, OIDCConstants.TOKENTYPE_ACCESS_TOKEN);
        mockery.checking(new Expectations() {
            {
                one(refreshToken).getScope();
                will(returnValue(new String[] { "openid", "offline_access" }));
            }
        });
        builder.removeRefreshTokenAssociatedWithOAuthTokenFromCache(accessToken);
    }

    @Test
    public void test_removeRefreshTokenAssociatedWithOAuthTokenFromCache_idToken_noAssociatedAccessToken() throws Exception {
        String accessTokenKey = "accesstokenkey";
        OAuth20TokenImpl idToken = mockery.mock(OAuth20TokenImpl.class);
        mockery.checking(new Expectations() {
            {
                allowing(idToken).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                one(idToken).getAccessTokenKey();
                will(returnValue(accessTokenKey));
                one(tokenCache).get(accessTokenKey);
                will(returnValue(null));
            }
        });
        builder.removeRefreshTokenAssociatedWithOAuthTokenFromCache(idToken);
    }

    @Test
    public void test_removeRefreshTokenAssociatedWithOAuthTokenFromCache_idToken_noMatchingRefreshToken() throws Exception {
        String accessTokenKey = "accesstokenkey";
        OAuth20TokenImpl idToken = mockery.mock(OAuth20TokenImpl.class);
        mockery.checking(new Expectations() {
            {
                allowing(idToken).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                one(idToken).getAccessTokenKey();
                will(returnValue(accessTokenKey));
                one(tokenCache).get(accessTokenKey);
                will(returnValue(null));
            }
        });
        builder.removeRefreshTokenAssociatedWithOAuthTokenFromCache(idToken);
    }

    @Test
    public void test_removeRefreshTokenAssociatedWithOAuthTokenFromCache_idToken_withMatchingRefreshToken() throws Exception {
        OAuth20TokenImpl idToken = mockery.mock(OAuth20TokenImpl.class, "idToken-impl");
        String refreshTokenString = "refreshtokenstring";
        OAuth20Token refreshToken = getRefreshTokenExpectations(idToken, OIDCConstants.TOKENTYPE_ID_TOKEN);
        mockery.checking(new Expectations() {
            {
                one(refreshToken).getScope();
                will(returnValue(new String[] { "openid" }));
                one(refreshToken).getTokenString();
                will(returnValue(refreshTokenString));
                one(tokenCache).remove(refreshTokenString);
            }
        });
        builder.removeRefreshTokenAssociatedWithOAuthTokenFromCache(idToken);
    }

    @Test
    public void test_removeRefreshTokenAssociatedWithOAuthTokenFromCache_idToken_withMatchingRefreshToken_refreshTokenHasOfflineAccess() throws Exception {
        OAuth20TokenImpl idToken = mockery.mock(OAuth20TokenImpl.class, "idToken-impl");
        OAuth20Token refreshToken = getRefreshTokenExpectations(idToken, OIDCConstants.TOKENTYPE_ID_TOKEN);
        mockery.checking(new Expectations() {
            {
                one(refreshToken).getScope();
                will(returnValue(new String[] { "openid", "offline_access" }));
            }
        });
        builder.removeRefreshTokenAssociatedWithOAuthTokenFromCache(idToken);
    }

    @Test
    public void test_refreshTokenHasOfflineAccessScope_nullScopes() {
        OAuth20Token refreshToken = mockery.mock(OAuth20Token.class, "refreshToken");
        mockery.checking(new Expectations() {
            {
                one(refreshToken).getScope();
                will(returnValue(null));
            }
        });
        boolean result = builder.refreshTokenHasOfflineAccessScope(refreshToken);
        assertFalse("Refresh token was not issued with the offline_access scope, but the method returned true.", result);
    }

    @Test
    public void test_refreshTokenHasOfflineAccessScope_noScopes() {
        OAuth20Token refreshToken = mockery.mock(OAuth20Token.class, "refreshToken");
        mockery.checking(new Expectations() {
            {
                one(refreshToken).getScope();
                will(returnValue(new String[] {}));
            }
        });
        boolean result = builder.refreshTokenHasOfflineAccessScope(refreshToken);
        assertFalse("Refresh token was not issued with the offline_access scope, but the method returned true.", result);
    }

    @Test
    public void test_refreshTokenHasOfflineAccessScope_doesNotHaveOfflineAccessScope() {
        OAuth20Token refreshToken = mockery.mock(OAuth20Token.class, "refreshToken");
        mockery.checking(new Expectations() {
            {
                one(refreshToken).getScope();
                will(returnValue(new String[] { "openid" }));
            }
        });
        boolean result = builder.refreshTokenHasOfflineAccessScope(refreshToken);
        assertFalse("Refresh token was not issued with the offline_access scope, but the method returned true.", result);
    }

    @Test
    public void test_refreshTokenHasOfflineAccessScope_hasOfflineAccessScope() {
        OAuth20Token refreshToken = mockery.mock(OAuth20Token.class, "refreshToken");
        mockery.checking(new Expectations() {
            {
                one(refreshToken).getScope();
                will(returnValue(new String[] { "openid", "offline_access" }));
            }
        });
        boolean result = builder.refreshTokenHasOfflineAccessScope(refreshToken);
        assertTrue("Refresh token was issued with the offline_access scope, but the method returned false.", result);
    }

    @Test
    public void test_getClientsToLogOut_noCachedUserTokens() throws Exception {
        // No tokens cached for user
        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = builder.getClientsToLogOut(new ArrayList<>());
        verifyCachedIdTokensMapContainsExpectedClients(clientsToCachedIdTokens);
    }

    @Test
    public void test_getClientsToLogOut_noCachedIdTokens() throws Exception {
        // No ID tokens cached for user
        List<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1, accessToken2);
        setClientLookupExpectations();

        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = builder.getClientsToLogOut(allCachedUserTokens);
        verifyCachedIdTokensMapContainsExpectedClients(clientsToCachedIdTokens);
    }

    @Test
    public void test_getClientsToLogOut_oneCachedIdToken_associatedClientMissingLogoutUri() throws Exception {
        List<OAuth20Token> allCachedUserTokens = Arrays.asList(idToken1);
        setClientLookupExpectations(client1);
        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue(null));
            }
        });

        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = builder.getClientsToLogOut(allCachedUserTokens);
        verifyCachedIdTokensMapContainsExpectedClients(clientsToCachedIdTokens);
    }

    @Test
    public void test_getClientsToLogOut_oneCachedIdToken_associatedClientHasLogoutUri() throws Exception {
        List<OAuth20Token> allCachedUserTokens = Arrays.asList(idToken1);
        setClientLookupExpectations(client1);
        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
            }
        });

        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = builder.getClientsToLogOut(allCachedUserTokens);
        verifyCachedIdTokensMapContainsExpectedClients(clientsToCachedIdTokens, client1);

        // Should find the ID token that matched
        List<OAuth20Token> cachedIdTokensForClient = clientsToCachedIdTokens.get(client1);
        verifyCachedIdTokensForClient(cachedIdTokensForClient, idToken1);
    }

    @Test
    public void test_getClientsToLogOut_multipleCachedIdTokens_subsetHasLogoutUri() throws Exception {
        List<OAuth20Token> allCachedUserTokens = Arrays.asList(idToken1, accessToken1, idToken2, accessToken2, idToken3);
        setClientLookupExpectations(client1, client2, client3);
        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
                // client2 has no logout URI configured, so there shouldn't be any logout tokens created for that client
                one(client2).getBackchannelLogoutUri();
                will(returnValue(null));
                one(client3).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client3"));
            }
        });

        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = builder.getClientsToLogOut(allCachedUserTokens);
        verifyCachedIdTokensMapContainsExpectedClients(clientsToCachedIdTokens, client1, client3);

        // Should find the ID tokens that matched
        List<OAuth20Token> cachedIdTokensForClient1 = clientsToCachedIdTokens.get(client1);
        verifyCachedIdTokensForClient(cachedIdTokensForClient1, idToken1);

        List<OAuth20Token> cachedIdTokensForClient3 = clientsToCachedIdTokens.get(client3);
        verifyCachedIdTokensForClient(cachedIdTokensForClient3, idToken3);
    }

    @Test
    public void test_getClientToCachedIdTokensMap_noCachedIdTokens() throws Exception {
        // No ID tokens are in the cache
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1, accessToken2);

        setClientLookupExpectations();

        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = builder.getClientToCachedIdTokensMap(allCachedUserTokens);
        verifyCachedIdTokensMapContainsExpectedClients(clientsToCachedIdTokens);
    }

    @Test
    public void test_getClientToCachedIdTokensMap_oneCachedIdToken_noLogoutUri() throws Exception {
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(idToken1);

        setClientLookupExpectations(client1);
        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue(null));
            }
        });

        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = builder.getClientToCachedIdTokensMap(allCachedUserTokens);
        verifyCachedIdTokensMapContainsExpectedClients(clientsToCachedIdTokens);
    }

    @Test
    public void test_getClientToCachedIdTokensMap_multipleCachedIdTokens() throws Exception {
        IDTokenImpl tmpIdToken1 = mockery.mock(IDTokenImpl.class, "tmpIdToken1");
        IDTokenImpl tmpIdToken2 = mockery.mock(IDTokenImpl.class, "tmpIdToken2");

        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(idToken1, tmpIdToken1, tmpIdToken2);

        setClientLookupExpectations(client1);
        mockery.checking(new Expectations() {
            {
                allowing(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
                one(tmpIdToken1).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                one(tmpIdToken1).getClientId();
                will(returnValue(client1Id));
                one(tmpIdToken2).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                one(tmpIdToken2).getClientId();
                will(returnValue(client1Id));
            }
        });

        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = builder.getClientToCachedIdTokensMap(allCachedUserTokens);
        verifyCachedIdTokensMapContainsExpectedClients(clientsToCachedIdTokens, client1);

        // Should find the ID tokens that matched
        List<OAuth20Token> cachedIdTokensForClient1 = clientsToCachedIdTokens.get(client1);
        verifyCachedIdTokensForClient(cachedIdTokensForClient1, idToken1, tmpIdToken1, tmpIdToken2);
    }

    @Test
    public void test_isValidClientForBackchannelLogout_noLogoutUri() {
        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue(null));
            }
        });
        assertFalse("Client without a back-channel logout URI should not be considered valid for BCL.", builder.isValidClientForBackchannelLogout(client1));
    }

    @Test
    public void test_isValidClientForBackchannelLogout_logoutUriNotHttp() {
        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue("scp://localhost"));
            }
        });
        assertFalse("Client with non-HTTP back-channel logout URI should not be considered valid for BCL.", builder.isValidClientForBackchannelLogout(client1));
    }

    @Test
    public void test_isValidClientForBackchannelLogout_httpPublicClient() {
        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue("http://localhost"));
                one(client1).isPublicClient();
                will(returnValue(true));
            }
        });
        assertFalse("Public client with HTTP back-channel logout URI should not be considered valid for BCL.", builder.isValidClientForBackchannelLogout(client1));
    }

    @Test
    public void test_isValidClientForBackchannelLogout_httpConfidentialClient() {
        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue("http://localhost"));
                one(client1).isPublicClient();
                will(returnValue(false));
            }
        });
        assertTrue("Confidential client with HTTP back-channel logout URI should be considered valid for BCL.", builder.isValidClientForBackchannelLogout(client1));
    }

    @Test
    public void test_isValidClientForBackchannelLogout_httpsUri() {
        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost"));
            }
        });
        assertTrue("HTTPS back-channel logout URI should be considered valid for BCL.", builder.isValidClientForBackchannelLogout(client1));
    }

    @Test
    public void test_addCachedIdTokenToMap_noEntries() throws Exception {
        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = new HashMap<OidcBaseClient, List<OAuth20Token>>();

        builder.addCachedIdTokenToMap(clientsToCachedIdTokens, client1, idToken1);

        verifyCachedIdTokensMapContainsExpectedClients(clientsToCachedIdTokens, client1);

        List<OAuth20Token> cachedIdTokensForClient = clientsToCachedIdTokens.get(client1);
        verifyCachedIdTokensForClient(cachedIdTokensForClient, idToken1);
    }

    @Test
    public void test_addCachedIdTokenToMap_entriesForOtherClients() throws Exception {
        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = new HashMap<OidcBaseClient, List<OAuth20Token>>();
        clientsToCachedIdTokens.put(client2, new ArrayList<>());
        clientsToCachedIdTokens.put(client3, new ArrayList<>());

        builder.addCachedIdTokenToMap(clientsToCachedIdTokens, client1, idToken1);

        verifyCachedIdTokensMapContainsExpectedClients(clientsToCachedIdTokens, client1, client2, client3);

        List<OAuth20Token> cachedIdTokensForClient1 = clientsToCachedIdTokens.get(client1);
        verifyCachedIdTokensForClient(cachedIdTokensForClient1, idToken1);
    }

    @Test
    public void test_addCachedIdTokenToMap_oneExistingEntryForClient() throws Exception {
        Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens = new HashMap<OidcBaseClient, List<OAuth20Token>>();
        List<OAuth20Token> existingCachedTokens = new ArrayList<>();
        existingCachedTokens.add(idToken1);
        clientsToCachedIdTokens.put(client1, existingCachedTokens);
        clientsToCachedIdTokens.put(client2, new ArrayList<>());

        builder.addCachedIdTokenToMap(clientsToCachedIdTokens, client1, idToken2);

        verifyCachedIdTokensMapContainsExpectedClients(clientsToCachedIdTokens, client1, client2);

        List<OAuth20Token> cachedIdTokensForClient1 = clientsToCachedIdTokens.get(client1);
        verifyCachedIdTokensForClient(cachedIdTokensForClient1, idToken1, idToken2);
    }

    @Test
    public void test_buildLogoutTokensForClients_noClientsToLogOut() throws Exception {
        Map<OidcBaseClient, List<OAuth20Token>> clientsToLogOut = new HashMap<>();

        Map<OidcBaseClient, Set<String>> clientsToLogoutTokens = builder.buildLogoutTokensForClients(clientsToLogOut);

        verifyLogoutTokensMapContainsExpectedClients(clientsToLogoutTokens);
    }

    @Test
    public void test_buildLogoutTokensForClients_oneClient_noIdTokens() throws Exception {
        Map<OidcBaseClient, List<OAuth20Token>> clientsToLogOut = new HashMap<>();
        clientsToLogOut.put(client1, new ArrayList<>());

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        Map<OidcBaseClient, Set<String>> clientsToLogoutTokens = builder.buildLogoutTokensForClients(clientsToLogOut);

        verifyLogoutTokensMapContainsExpectedClients(clientsToLogoutTokens);
    }

    @Test
    public void test_buildLogoutTokensForClients_oneClient_oneIdToken() throws Exception {
        Map<OidcBaseClient, List<OAuth20Token>> clientsToLogOut = new HashMap<>();
        clientsToLogOut.put(client1, Arrays.asList(idToken1));

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        Map<OidcBaseClient, Set<String>> clientsToLogoutTokens = builder.buildLogoutTokensForClients(clientsToLogOut);

        verifyLogoutTokensMapContainsExpectedClients(clientsToLogoutTokens, client1);

        Set<String> logoutTokensForClient = clientsToLogoutTokens.get(client1);
        verifyLogoutTokensForClient(client1Id, logoutTokensForClient, 1, null);
    }

    @Test
    public void test_buildLogoutTokensForClients_multipleClients_multipleIdTokens() throws Exception {
        IDTokenImpl tmpIdToken1 = mockery.mock(IDTokenImpl.class, "tmpIdToken1");
        IDTokenImpl tmpIdToken3 = mockery.mock(IDTokenImpl.class, "tmpIdToken3");

        Map<OidcBaseClient, List<OAuth20Token>> clientsToLogOut = new HashMap<>();
        clientsToLogOut.put(client1, Arrays.asList(idToken1, tmpIdToken1));
        clientsToLogOut.put(client2, Arrays.asList(idToken2));
        clientsToLogOut.put(client3, Arrays.asList(idToken3, tmpIdToken3));

        JSONObject tmpIdToken1Claims = getIdTokenClaims(subject, issuerIdentifier, client1Id);
        String tmpIdToken1Sid = client1Id + "-sid for tmpIdToken1";
        tmpIdToken1Claims.put("sid", tmpIdToken1Sid);
        final String tmpIdToken1String = JwtUnitTestUtils.getHS256Jws(tmpIdToken1Claims, "some secret");

        JSONObject tmpIdToken3Claims = getIdTokenClaims(subject, issuerIdentifier, client3Id);
        String tmpIdToken3Sid = client3Id + "-sid for tmpIdToken3";
        tmpIdToken3Claims.put("sid", tmpIdToken3Sid);
        final String tmpIdToken3String = JwtUnitTestUtils.getHS256Jws(tmpIdToken3Claims, "some secret");

        setCustomIdTokenExpectations(tmpIdToken1, client1Id, tmpIdToken1String);
        setCustomIdTokenExpectations(tmpIdToken3, client3Id, tmpIdToken3String);
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);
        setJwtCreationExpectations(client1, client1Secret);
        setJwtCreationExpectations(client2, client2Secret);
        setJwtCreationExpectations(client3, client3Secret);
        setJwtCreationExpectations(client3, client3Secret);

        Map<OidcBaseClient, Set<String>> clientsToLogoutTokens = builder.buildLogoutTokensForClients(clientsToLogOut);

        verifyLogoutTokensMapContainsExpectedClients(clientsToLogoutTokens, client1, client2, client3);

        Set<String> logoutTokensForClient1 = clientsToLogoutTokens.get(client1);
        verifyLogoutTokensForClient(client1Id, logoutTokensForClient1, 2, tmpIdToken1Sid);

        Set<String> logoutTokensForClient2 = clientsToLogoutTokens.get(client2);
        verifyLogoutTokensForClient(client2Id, logoutTokensForClient2, 1, null);

        Set<String> logoutTokensForClient3 = clientsToLogoutTokens.get(client3);
        verifyLogoutTokensForClient(client3Id, logoutTokensForClient3, 2, tmpIdToken3Sid);
    }

    @Test
    public void test_buildLogoutTokensForClient_oneCachedIdToken() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        List<OAuth20Token> cachedIdTokens = Arrays.asList(idToken1);

        Set<String> logoutTokens = builder.buildLogoutTokensForClient(client1, cachedIdTokens);

        verifyLogoutTokensForClient(client1Id, logoutTokens, 1, null);
    }

    @Test
    public void test_buildLogoutTokensForClient_multipleCachedIdTokens() throws Exception {
        IDTokenImpl tmpIdToken1 = mockery.mock(IDTokenImpl.class, "tmpIdToken1");
        IDTokenImpl tmpIdToken2 = mockery.mock(IDTokenImpl.class, "tmpIdToken2");

        // Give one cached ID token a jti and sid claim so we can ensure the associated logout token gets created with a matching sid claim
        JSONObject idTokenClaims1 = getIdTokenClaims(subject, issuerIdentifier, client1Id);
        String idTokenSid = "sid1";
        idTokenClaims1.put("sid", idTokenSid);
        final String idTokenString1 = JwtUnitTestUtils.getHS256Jws(idTokenClaims1, "some secret");

        // One ID token with a different issuer value; should not have an associated logout token created
        JSONObject idTokenClaims2 = getIdTokenClaims(subject, "some other issuer", client1Id);
        final String idTokenString2 = JwtUnitTestUtils.getHS256Jws(idTokenClaims2, "some secret 2");

        setCustomIdTokenExpectations(tmpIdToken1, client1Id, idTokenString1);
        setCustomIdTokenExpectations(tmpIdToken2, client1Id, idTokenString2);
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        // Should create two logout tokens
        setJwtCreationExpectations(client1, client1Secret);
        setJwtCreationExpectations(client1, client1Secret);

        List<OAuth20Token> cachedIdTokens = Arrays.asList(idToken1, tmpIdToken1, tmpIdToken2);

        Set<String> logoutTokens = builder.buildLogoutTokensForClient(client1, cachedIdTokens);

        verifyLogoutTokensForClient(client1Id, logoutTokens, 2, idTokenSid);
    }

    @Test
    public void test_createLogoutTokenForClientFromCachedIdToken_idTokenDifferentIssuer() throws Exception {
        JSONObject idTokenClaims = getIdTokenClaims(subject, "some other issuer", "some audience");
        final String idTokenString = JwtUnitTestUtils.getHS256Jws(idTokenClaims, "some secret");
        mockery.checking(new Expectations() {
            {
                one(idToken).getTokenString();
                will(returnValue(idTokenString));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        try {
            String logoutToken = builder.createLogoutTokenForClientFromCachedIdToken(client1, idToken);
            fail("Should have thrown an exception but didn't. Got: " + logoutToken);
        } catch (Exception e) {
            verifyException(e, CWWKS1643E_LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN + ".*" + CWWKS1646E_ID_TOKEN_ISSUER_NOT_THIS_OP);
        }
    }

    @Test
    public void test_createLogoutTokenForClientFromCachedIdToken_goldenPath() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        String logoutToken = builder.createLogoutTokenForClientFromCachedIdToken(client1, idToken1);
        assertNotNull("Should have created a logout token for a valid ID token but didn't.", logoutToken);
        verifyLogoutToken(logoutToken, Arrays.asList(client1Id), subject, null);
    }

    @Test
    public void test_createLogoutTokenForClient_idTokenMissingSub() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.unsetClaim("sub");
        try {
            String logoutToken = builder.createLogoutTokenForClient(client1, idTokenClaims);
            fail("Should have thrown an exception but didn't. Got: " + logoutToken);
        } catch (LogoutTokenBuilderException e) {
            verifyException(e, CWWKS1953E_ERROR_BUILDING_LOGOUT_TOKEN_BASED_ON_ID_TOKEN_CLAIMS + ".+" + CWWKS1647E_ID_TOKEN_MISSING_REQUIRED_CLAIMS + ".+" + "sub");
        }
    }

    @Test
    public void test_createLogoutTokenForClient_idTokenContainsSub() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        setJwtCreationExpectations(client1, client1Secret);

        String logoutToken = builder.createLogoutTokenForClient(client1, idTokenClaims);
        verifyLogoutToken(logoutToken, Arrays.asList(client1Id), subject, null);
    }

    @Test
    public void test_createLogoutTokenForClient_idTokenContainsSubAndSid() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setStringClaim("sid", sid);
        setJwtCreationExpectations(client1, client1Secret);

        String logoutToken = builder.createLogoutTokenForClient(client1, idTokenClaims);
        verifyLogoutToken(logoutToken, Arrays.asList(client1Id), subject, sid);
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_subGoldenPath() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        JwtClaims result = builder.populateLogoutTokenClaimsFromIdToken(client1, idTokenClaims);

        verifyLogoutTokenClaims(result, Arrays.asList(client1Id), subject, null);
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_sidNotString() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setClaim("sid", 123);
        try {
            JwtClaims result = builder.populateLogoutTokenClaimsFromIdToken(client1, idTokenClaims);
            fail("Should have thrown an exception but didn't. Got: " + result);
        } catch (MalformedClaimException e) {
            verifyException(e, "sid");
        }
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_subAndSidGoldenPath() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setClaim("sid", sid);
        JwtClaims result = builder.populateLogoutTokenClaimsFromIdToken(client1, idTokenClaims);

        verifyLogoutTokenClaims(result, Arrays.asList(client1Id), subject, sid);
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_idTokenMissingSub() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.unsetClaim("sub");
        try {
            JwtClaims result = builder.populateLogoutTokenClaimsFromIdToken(client1, idTokenClaims);
            fail("Should have thrown an exception but didn't. Got: " + result);
        } catch (LogoutTokenBuilderException e) {
            verifyException(e, CWWKS1647E_ID_TOKEN_MISSING_REQUIRED_CLAIMS + ".+" + "sub");
        }
    }

    @Test
    public void test_getIssuerFromRequest_standardHttpPort() throws Exception {
        final String scheme = "http";
        final String serverName = "myserver";
        final String expectedIssuerPath = "/oidc/providers/" + providerId;
        mockery.checking(new Expectations() {
            {
                one(request).getScheme();
                will(returnValue(scheme));
                one(request).getServerName();
                will(returnValue(serverName));
                one(request).getServerPort();
                will(returnValue(80));
            }
        });
        String expectedIssuer = scheme + "://" + serverName + expectedIssuerPath;
        String result = builder.getIssuerFromRequest();
        assertEquals("Issuer value did not match expected value.", expectedIssuer, result);
    }

    @Test
    public void test_getIssuerFromRequest_standardHttpsPort() throws Exception {
        final String scheme = "https";
        final String serverName = "myserver";
        final String expectedIssuerPath = "/oidc/providers/" + providerId;
        mockery.checking(new Expectations() {
            {
                one(request).getScheme();
                will(returnValue(scheme));
                one(request).getServerName();
                will(returnValue(serverName));
                one(request).getServerPort();
                will(returnValue(443));
            }
        });
        String expectedIssuer = scheme + "://" + serverName + expectedIssuerPath;
        String result = builder.getIssuerFromRequest();
        assertEquals("Issuer value did not match expected value.", expectedIssuer, result);
    }

    @Test
    public void test_getIssuerFromRequest_nonStandardPort() throws Exception {
        final String scheme = "https";
        final String serverName = "myserver";
        final int port = 98765;
        final String expectedIssuerPath = "/oidc/providers/" + providerId;
        mockery.checking(new Expectations() {
            {
                one(request).getScheme();
                will(returnValue(scheme));
                one(request).getServerName();
                will(returnValue(serverName));
                one(request).getServerPort();
                will(returnValue(port));
            }
        });
        String expectedIssuer = scheme + "://" + serverName + ":" + port + expectedIssuerPath;
        String result = builder.getIssuerFromRequest();
        assertEquals("Issuer value did not match expected value.", expectedIssuer, result);
    }

    private void verifyCachedIdTokensMapContainsExpectedClients(Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens, OidcBaseClient... expectedClientEntries) {
        assertNotNull("Map of clients to cached ID tokens should not have been null but was.", clientsToCachedIdTokens);
        if (expectedClientEntries == null || expectedClientEntries.length == 0) {
            assertTrue("Map of clients to cached ID tokens should have been empty but wasn't. Map was: " + clientsToCachedIdTokens, clientsToCachedIdTokens.isEmpty());
            return;
        }
        assertEquals("Map of clients to cached ID tokens was not the expected size: " + clientsToCachedIdTokens, expectedClientEntries.length, clientsToCachedIdTokens.size());
        for (OidcBaseClient expectedClient : expectedClientEntries) {
            assertTrue("Map of clients did not contain entry for [" + expectedClient + "]: " + clientsToCachedIdTokens, clientsToCachedIdTokens.containsKey(expectedClient));
        }
    }

    private void verifyCachedIdTokensForClient(List<OAuth20Token> cachedIdTokensForClient, OAuth20Token... expectedIdTokens) throws Exception {
        if (expectedIdTokens == null || expectedIdTokens.length == 0) {
            assertTrue("List of cached ID tokens should have been empty but wasn't. Map was: " + cachedIdTokensForClient, cachedIdTokensForClient.isEmpty());
            return;
        }
        assertEquals("List of cached ID tokens was not the expected size: " + cachedIdTokensForClient, expectedIdTokens.length, cachedIdTokensForClient.size());
        for (OAuth20Token expectedIdToken : expectedIdTokens) {
            assertTrue("List of cached ID tokens did not contain entry for [" + expectedIdToken + "]: " + cachedIdTokensForClient,
                       cachedIdTokensForClient.contains(expectedIdToken));
        }
    }

    private void verifyLogoutTokensMapContainsExpectedClients(Map<OidcBaseClient, Set<String>> logoutTokens, OidcBaseClient... expectedClientEntries) {
        assertNotNull("Map of clients to logout tokens should not have been null but was.", logoutTokens);
        if (expectedClientEntries == null || expectedClientEntries.length == 0) {
            assertTrue("Map of clients to logout tokens should have been empty but wasn't. Map was: " + logoutTokens, logoutTokens.isEmpty());
            return;
        }
        assertEquals("Map of clients to logout tokens was not the expected size: " + logoutTokens, expectedClientEntries.length, logoutTokens.size());
        for (OidcBaseClient expectedClient : expectedClientEntries) {
            assertTrue("Map of clients did not contain entry for [" + expectedClient + "]: " + logoutTokens, logoutTokens.containsKey(expectedClient));
        }
    }

    private void verifyLogoutTokensForClient(String clientId, Set<String> logoutTokensForClient, int expectedNumberOfEntries, String expectedSid) throws Exception {
        assertEquals("Set of logout tokens for " + clientId + " was not the expected size: " + logoutTokensForClient, expectedNumberOfEntries, logoutTokensForClient.size());

        for (String logoutToken : logoutTokensForClient) {
            JwtClaims logoutTokenClaims = builder.getClaimsFromIdTokenString(logoutToken);

            if (logoutTokenClaims.hasClaim("sid")) {
                // One of the cached ID tokens must have had a sid claim in it; the corresponding logout token should have the expected sid claim value
                verifyLogoutToken(logoutToken, Arrays.asList(clientId), subject, expectedSid);
            } else {
                // The cached ID tokens that don't have a sid claim should not have a logout token with a sid claim in it
                verifyLogoutToken(logoutToken, Arrays.asList(clientId), subject, null);
            }
        }
    }

    void verifyLogoutToken(String logoutTokenString, List<String> expectedAudiences, String expectedSubject, String expectedSid) throws Exception {
        assertNotNull("Logout token string should not have been null but was.", logoutTokenString);

        // Verify token header values
        JwtContext resultContext = JwtParsingUtils.parseJwtWithoutValidation(logoutTokenString);
        JsonWebStructure jsonWebStructure = resultContext.getJoseObjects().get(0);
        assertEquals("JWT alg header did not match expected value.", "HS256", jsonWebStructure.getAlgorithmHeaderValue());
        assertEquals("JWT typ header did not match expected value.", "logout+jwt", jsonWebStructure.getHeader("typ"));

        verifyLogoutTokenClaims(resultContext.getJwtClaims(), expectedAudiences, expectedSubject, expectedSid);
    }

    @SuppressWarnings("unchecked")
    private void verifyLogoutTokenClaims(JwtClaims result, List<String> expectedAudiences, String expectedSubject, String expectedSid) throws MalformedClaimException {
        assertNotNull("Result should not have been null but was.", result);

        long now = System.currentTimeMillis() / 1000;
        long timeFrameStart = now - 5;
        long timeFrameEnd = now + 5;

        long logoutTokenLifetimeSeconds = 120;
        long expTimeFrameStart = timeFrameStart + logoutTokenLifetimeSeconds;
        long expTimeFrameEnd = timeFrameEnd + logoutTokenLifetimeSeconds;

        // iss
        assertNotNull("Token must have an iss claim, but did not. Claims were: " + result, result.getIssuer());
        assertEquals("Issuer did not match expected value. Claims were: " + result, issuerIdentifier, result.getIssuer());
        // aud
        assertNotNull("Token must have an aud claim, but did not. Claims were: " + result, result.getAudience());
        assertEquals("Audience did not match expected value. Claims were: " + result, expectedAudiences, result.getAudience());
        // iat
        assertNotNull("Token must have an iat claim, but did not. Claims were: " + result, result.getIssuedAt());
        long issuedAt = result.getIssuedAt().getValue();
        assertTrue("Issued at time (" + issuedAt + ") is not in an expected reasonable time frame (" + timeFrameStart + " to " + timeFrameEnd + "). Claims were: " + result,
                   (timeFrameStart <= issuedAt) && (issuedAt <= timeFrameEnd));
        // exp
        assertNotNull("Token must have an exp claim, but did not. Claims were: " + result, result.getExpirationTime());
        long exp = result.getExpirationTime().getValue();
        assertTrue("Expiration time (" + exp + ") is not in an expected reasonable time frame (" + (expTimeFrameStart) + " to " + (expTimeFrameEnd) + "). Claims were: " + result,
                   ((expTimeFrameStart) <= exp) && (exp <= (expTimeFrameEnd)));
        // jti
        assertNotNull("JTI claim should not have been null but was. Claims were: " + result, result.getJwtId());
        // events
        Map<String, Object> eventsClaim = (Map<String, Object>) result.getClaimValue("events");
        assertNotNull("Events claim should not have been null but was. Claims were: " + result, eventsClaim);
        assertTrue("Events claim did not contain the " + LogoutTokenBuilder.EVENTS_MEMBER_NAME + " member. Claims were: " + result,
                   eventsClaim.containsKey(LogoutTokenBuilder.EVENTS_MEMBER_NAME));
        assertEquals("Events claim entry did not match expected value. Claims were: " + result, new HashMap<>(), eventsClaim.get(LogoutTokenBuilder.EVENTS_MEMBER_NAME));
        // nonce
        assertNull("A nonce claim was found but shouldn't have been. Claims were: " + result, result.getClaimValue("nonce"));

        // Token must have sub and/or sid claim
        String resultSub = result.getSubject();
        String resultSid = result.getStringClaimValue("sid");
        assertFalse("Token must have a sub and/or sid claim, but it is missing both. Claims were: " + result, resultSub == null && resultSid == null);

        // sub
        if (expectedSubject == null) {
            assertNull("A sub claim was found but shouldn't have been: \"" + result + "\".", resultSub);
        } else {
            assertEquals("Sub claim did not match expected value. Claims were: " + result, expectedSubject, resultSub);
        }
        // sid
        if (expectedSid == null) {
            assertNull("A sid claim was found but shouldn't have been: \"" + result + "\".", resultSid);
        } else {
            assertEquals("SID claim did not match expected value. Claims were: " + result, expectedSid, resultSid);
        }
    }

    private void setClientLookupExpectations(OidcBaseClient... clientsToFetch) throws OidcServerException {
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
            }
        });
        if (clientsToFetch != null) {
            for (OidcBaseClient client : clientsToFetch) {
                mockery.checking(new Expectations() {
                    {
                        one(clientProvider).get(client.getClientId());
                        will(returnValue(client));
                    }
                });
            }
        }
    }

    private void setTokenCacheExpectations(String user, OAuth20Token... tokens) {
        mockery.checking(new Expectations() {
            {
                one(tokenCache).getAllUserTokens(subject);
                will(returnValue(Arrays.asList(tokens)));
            }
        });
    }

    private void setCustomIdTokenExpectations(IDTokenImpl customIdToken, String idTokenClientId, String idTokenString) {
        mockery.checking(new Expectations() {
            {
                allowing(customIdToken).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(customIdToken).getClientId();
                will(returnValue(idTokenClientId));
                allowing(customIdToken).getTokenString();
                will(returnValue(idTokenString));
                allowing(customIdToken).getId();
                will(returnValue(customIdTokenId));
            }
        });
    }

    private void setJwtCreationExpectations(OidcBaseClient client, String clientSecret) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(client).getClientSecret();
                will(returnValue(clientSecret));
                allowing(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
                one(oidcServerConfig).getJSONWebKey();
                will(returnValue(null));
                one(oidcServerConfig).getKeyAliasName();
                will(returnValue(null));
                one(oidcServerConfig).getKeyStoreRef();
                will(returnValue(null));
                one(oidcServerConfig).isJwkEnabled();
                will(returnValue(false));
            }
        });
    }

    private String getIdToken1String() throws Exception {
        return JwtUnitTestUtils.getHS256Jws(getIdTokenClaims(subject, issuerIdentifier, client1Id), client1Secret);
    }

    private String getIdToken2String() throws Exception {
        return JwtUnitTestUtils.getHS256Jws(getIdTokenClaims(subject, issuerIdentifier, client2Id), client2Secret);
    }

    private String getIdToken3String() throws Exception {
        return JwtUnitTestUtils.getHS256Jws(getIdTokenClaims(subject, issuerIdentifier, client3Id), client3Secret);
    }

    private JwtClaims getClaims(String subject, String issuer, String... audiences) {
        JwtClaims claims = new JwtClaims();
        claims.setSubject(subject);
        claims.setIssuer(issuer);
        claims.setAudience(audiences);
        return claims;
    }

    private JSONObject getIdTokenClaims(String subject, String issuer, String... audiences) {
        JSONArray audiencesArray = null;
        if (audiences != null) {
            audiencesArray = new JSONArray();
            for (String aud : audiences) {
                audiencesArray.add(aud);
            }
        }
        return getIdTokenClaims(subject, issuer, audiencesArray);
    }

    private JSONObject getIdTokenClaims(String subject, String issuer, JSONArray audiences) {
        JSONObject claims = new JSONObject();
        if (subject != null) {
            claims.put(Claims.SUBJECT, subject);
        }
        if (issuer != null) {
            claims.put(Claims.ISSUER, issuer);
        }
        if (audiences != null) {
            claims.put(Claims.AUDIENCE, audiences);
        }
        return claims;
    }

    private OAuth20Token getRefreshTokenExpectations(OAuth20TokenImpl accessOrIdToken, String accessOrIdTokenType) {
        OAuth20TokenImpl accessToken;
        OAuth20Token refreshToken = mockery.mock(OAuth20Token.class, "refreshToken-getRefreshTokenExpectations");

        String refreshTokenKey = "refreshtokenkey";

        if (OIDCConstants.TOKENTYPE_ID_TOKEN.equals(accessOrIdTokenType)) {
            accessToken = getAccessTokenFromIdTokenExpectations(accessOrIdToken);
        } else {
            accessToken = accessOrIdToken;
        }
        mockery.checking(new Expectations() {
            {
                allowing(accessToken).getType();
                will(returnValue(OIDCConstants.TOKENTYPE_ACCESS_TOKEN));
                one(accessToken).getRefreshTokenKey();
                will(returnValue(refreshTokenKey));
                one(tokenCache).get(refreshTokenKey);
                will(returnValue(refreshToken));
            }
        });
        return refreshToken;
    }

    private OAuth20TokenImpl getAccessTokenFromIdTokenExpectations(OAuth20TokenImpl idToken) {
        OAuth20TokenImpl accessToken = mockery.mock(OAuth20TokenImpl.class, "accessToken-impl");

        String accessTokenKey = "accesstokenkey";

        mockery.checking(new Expectations() {
            {
                allowing(idToken).getType();
                will(returnValue(OIDCConstants.TOKENTYPE_ID_TOKEN));
                one(idToken).getAccessTokenKey();
                will(returnValue(accessTokenKey));
                one(tokenCache).get(accessTokenKey);
                will(returnValue(accessToken));
            }
        });
        return accessToken;
    }

}
