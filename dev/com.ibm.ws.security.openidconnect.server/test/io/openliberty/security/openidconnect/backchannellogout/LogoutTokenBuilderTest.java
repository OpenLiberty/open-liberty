/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
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
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.openidconnect.server.plugins.IDTokenImpl;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.security.test.common.jwt.utils.JwtUnitTestUtils;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import test.common.SharedOutputManager;

public class LogoutTokenBuilderTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("OpenIdConnect*=all");

    private static final String CWWKS1643E_LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN = "CWWKS1643E";
    private static final String CWWKS1645E_ID_TOKEN_ISSUER_NOT_THIS_OP = "CWWKS1645E";
    private static final String CWWKS1646E_ID_TOKEN_MISSING_REQUIRED_CLAIMS = "CWWKS1646E";

    private final String issuerIdentifier = "https://localhost/oidc/endpoint/OP";
    private final String client1Id = "client1";
    private final String client2Id = "client2";
    private final String client3Id = "client3";
    private final String client1Secret = "client1secret";
    private final String client2Secret = "client2secret";
    private final String client3Secret = "client3secret";
    private final String subject = "testuser";
    private final String sid = "somesidvalue";

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final OidcServerConfig oidcServerConfig = mockery.mock(OidcServerConfig.class);
    private final OAuth20Provider oauth20provider = mockery.mock(OAuth20Provider.class);
    private final OidcOAuth20ClientProvider clientProvider = mockery.mock(OidcOAuth20ClientProvider.class);
    private final OidcBaseClient client1 = mockery.mock(OidcBaseClient.class, "client1");
    private final OidcBaseClient client2 = mockery.mock(OidcBaseClient.class, "client2");
    private final OidcBaseClient client3 = mockery.mock(OidcBaseClient.class, "client3");
    private final OAuth20EnhancedTokenCache tokenCache = mockery.mock(OAuth20EnhancedTokenCache.class);
    private final OAuth20Token token = mockery.mock(OAuth20Token.class, "token");
    private final OAuth20Token accessToken1 = mockery.mock(OAuth20Token.class, "accessToken1");
    private final OAuth20Token accessToken2 = mockery.mock(OAuth20Token.class, "accessToken2");
    private final IDTokenImpl idToken = mockery.mock(IDTokenImpl.class, "idToken");
    private final IDTokenImpl idToken1 = mockery.mock(IDTokenImpl.class, "idToken1");
    private final IDTokenImpl idToken2 = mockery.mock(IDTokenImpl.class, "idToken2");
    private final IDTokenImpl idToken3 = mockery.mock(IDTokenImpl.class, "idToken3");

    private LogoutTokenBuilder builder;

    private final JwtUnitTestUtils jwtUtils = new JwtUnitTestUtils();

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
        builder = new MockLogoutTokenBuilder(request, oidcServerConfig);
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getProviderId();
                will(returnValue("OP"));
                allowing(client1).getClientId();
                will(returnValue(client1Id));
                allowing(client2).getClientId();
                will(returnValue(client2Id));
                allowing(client3).getClientId();
                will(returnValue(client3Id));
                allowing(accessToken1).getType();
                will(returnValue(OAuth20Constants.ACCESS_TOKEN));
                allowing(accessToken2).getType();
                will(returnValue(OAuth20Constants.ACCESS_TOKEN));
                allowing(idToken1).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken1).getClientId();
                will(returnValue(client1Id));
                allowing(idToken1).getTokenString();
                will(returnValue(getIdToken1String()));
                allowing(idToken2).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken2).getClientId();
                will(returnValue(client2Id));
                allowing(idToken2).getTokenString();
                will(returnValue(getIdToken2String()));
                allowing(idToken3).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken3).getClientId();
                will(returnValue(client3Id));
                allowing(idToken3).getTokenString();
                will(returnValue(getIdToken3String()));
            }
        });
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
    public void test_buildLogoutTokens_noRegisteredClients() throws Exception {
        setRegisteredClients(new ArrayList<>());

        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        Map<OidcBaseClient, List<String>> result = builder.buildLogoutTokens(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty but wasn't: " + result, result.isEmpty());
    }

    @Test
    public void test_buildLogoutTokens_idTokenOneAudience_oneClientRegistered_doesNotMatch() throws Exception {
        setRegisteredClients(Arrays.asList(client1));

        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client3Id);

        Map<OidcBaseClient, List<String>> result = builder.buildLogoutTokens(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty but wasn't: " + result, result.isEmpty());
    }

    @Test
    public void test_buildLogoutTokens_idTokenOneAudience_oneClientRegistered_matches() throws Exception {
        setRegisteredClients(Arrays.asList(client1));

        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        setTokenCacheExpectations(subject, idToken1);
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        Map<OidcBaseClient, List<String>> result = builder.buildLogoutTokens(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Result did not have the expected number of entries: " + result, 1, result.size());
        assertTrue("Result did not contain an entry for the expected client \"" + client1Id + "\". Result was: " + result, result.containsKey(client1));

        String clientLogoutToken = result.get(client1).get(0);
        verifyLogoutToken(clientLogoutToken, Arrays.asList(client1Id), subject, null);
    }

    @Test
    public void test_buildLogoutTokens_idTokenContainsSid_oneMatchingCachedToken() throws Exception {
        setRegisteredClients(Arrays.asList(client1));

        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setStringClaim("sid", sid);

        // One cached ID token has a matching sid
        JSONObject cachedTokenClaims = getIdTokenClaims(subject, issuerIdentifier, client1Id);
        cachedTokenClaims.put("sid", sid);
        String cachedIdTokenString = jwtUtils.getHS256Jws(cachedTokenClaims, client1Secret);

        setTokenCacheExpectations(subject, idToken1, idToken, idToken2, idToken3);

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
                allowing(idToken).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken).getClientId();
                will(returnValue(client1Id));
                allowing(idToken).getTokenString();
                will(returnValue(cachedIdTokenString));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        Map<OidcBaseClient, List<String>> result = builder.buildLogoutTokens(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Result did not have the expected number of entries: " + result, 1, result.size());
        assertTrue("Result did not contain an entry for the expected client \"" + client1Id + "\". Result was: " + result, result.containsKey(client1));

        String clientLogoutToken = result.get(client1).get(0);
        verifyLogoutToken(clientLogoutToken, Arrays.asList(client1Id), subject, sid);
    }

    @Test
    public void test_buildLogoutTokens_idTokenMultipleAudiences_multipleClientsRegistered_oneCachedTokenHasWrongIssuer() throws Exception {
        setRegisteredClients(Arrays.asList(client1, client2, client3));

        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id, client2Id, client3Id);

        // One cached ID token has a different issuer
        String cachedIdTokenString = jwtUtils.getHS256Jws(getIdTokenClaims(subject, "some other issuer", client2Id), client2Secret);

        setTokenCacheExpectations(subject, idToken1, idToken, idToken3);

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                allowing(idToken).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken).getClientId();
                will(returnValue(client2Id));
                allowing(idToken).getTokenString();
                will(returnValue(cachedIdTokenString));
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
                one(client2).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client2"));
                one(client3).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client3"));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);
        setJwtCreationExpectations(client3, client3Secret);

        Map<OidcBaseClient, List<String>> result = builder.buildLogoutTokens(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Result did not have the expected number of entries: " + result, 2, result.size());
        assertTrue("Result did not contain an entry for the expected client \"" + client1 + "\". Result was: " + result, result.containsKey(client1));
        assertTrue("Result did not contain an entry for the expected client \"" + client3 + "\". Result was: " + result, result.containsKey(client3));

        String client1LogoutToken = result.get(client1).get(0);
        verifyLogoutToken(client1LogoutToken, Arrays.asList(client1Id), subject, null);
        String client3LogoutToken = result.get(client3).get(0);
        verifyLogoutToken(client3LogoutToken, Arrays.asList(client3Id), subject, null);
    }

    @Test
    public void test_buildLogoutTokens_idTokenMultipleAudiences_multipleClientsRegistered() throws Exception {
        setRegisteredClients(Arrays.asList(client1, client2, client3));

        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id, client3Id);

        setTokenCacheExpectations(subject, idToken1, idToken3);
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
                one(client3).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client3"));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);
        setJwtCreationExpectations(client3, client3Secret);

        Map<OidcBaseClient, List<String>> result = builder.buildLogoutTokens(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Result did not have the expected number of entries: " + result, 2, result.size());
        assertTrue("Result did not contain an entry for the expected client \"" + client1 + "\". Result was: " + result, result.containsKey(client1));
        assertTrue("Result did not contain an entry for the expected client \"" + client3 + "\". Result was: " + result, result.containsKey(client3));

        String client1LogoutToken = result.get(client1).get(0);
        verifyLogoutToken(client1LogoutToken, Arrays.asList(client1Id), subject, null);
        String client3LogoutToken = result.get(client3).get(0);
        verifyLogoutToken(client3LogoutToken, Arrays.asList(client3Id), subject, null);
    }

    @Test
    public void test_getClaimsFromIdTokenString_emptyClaims() throws Exception {
        String idTokenString = jwtUtils.getHS256Jws(new JSONObject(), client1Secret);
        try {
            JwtClaims result = builder.getClaimsFromIdTokenString(idTokenString);
            fail("Should have thrown an exception but got: " + result);
        } catch (LogoutTokenBuilderException e) {
            verifyException(e, CWWKS1643E_LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN + ".*" + CWWKS1646E_ID_TOKEN_MISSING_REQUIRED_CLAIMS);
        }
    }

    @Test
    public void test_getClaimsFromIdTokenString_goldenPathClaims() throws Exception {
        JwtClaims input = getClaims(subject, issuerIdentifier, client1Id);
        input.setIssuedAtToNow();
        input.setGeneratedJwtId();
        input.setExpirationTimeMinutesInTheFuture(60);
        input.setNotBeforeMinutesInThePast(10);

        String idTokenString = jwtUtils.getHS256Jws(JSONObject.parse(input.toJson()), client1Secret);

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
    public void test_getClientsToLogOut_noCachedUserTokens() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        setRegisteredClients(Arrays.asList(client1, client2, client3));

        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
            }
        });

        // No tokens cached for user
        setTokenCacheExpectations(subject);

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientsToLogOut(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty but was: " + result, result.isEmpty());
    }

    @Test
    public void test_getClientsToLogOut_noCachedIdTokens() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        setRegisteredClients(Arrays.asList(client1, client2, client3));

        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
            }
        });

        // No ID tokens cached for user
        setTokenCacheExpectations(subject, accessToken1, accessToken2);

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientsToLogOut(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty but was: " + result, result.isEmpty());
    }

    @Test
    public void test_getClientsToLogOut_noRegisteredClients() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        // No registered clients
        setRegisteredClients(new ArrayList<>());

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientsToLogOut(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty but was: " + result, result.isEmpty());
    }

    @Test
    public void test_getClientsToLogOut_idTokenAudDoesNotMatchAnyRegisteredClients() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        // client1 not among registered clients
        setRegisteredClients(Arrays.asList(client2, client3));

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientsToLogOut(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty but was: " + result, result.isEmpty());
    }

    @Test
    public void test_getClientsToLogOut_idTokenAudDoesNotMatchAnyCachedIdTokenClients() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        setRegisteredClients(Arrays.asList(client1, client2, client3));

        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
            }
        });

        // No ID tokens cached for client1
        setTokenCacheExpectations(subject, idToken2, idToken3);

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientsToLogOut(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty but was: " + result, result.isEmpty());
    }

    @Test
    public void test_getClientsToLogOut_goldenPath_oneAud_oneCachedIdToken() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        // One ID token cached for client1
        setTokenCacheExpectations(subject, idToken1, idToken2, idToken3);

        setRegisteredClients(Arrays.asList(client1, client2, client3));

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
            }
        });

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientsToLogOut(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Resulting map of clients to cached ID tokens did not match expected size. Result was: " + result, 1, result.size());
        assertTrue("Resulting map of clients to cached ID tokens did not contain entry for [" + client1 + "]: " + result, result.containsKey(client1));

        // Should find the ID token that matched
        List<IDTokenImpl> cachedIdTokensForClient = result.get(client1);
        assertEquals("List of cached ID tokens for [" + client1 + "] did not match expected size. Result was: " + cachedIdTokensForClient, 1, cachedIdTokensForClient.size());
        assertTrue("List of cached ID tokens for [" + client1 + "] did not contain [" + idToken1 + "]: " + cachedIdTokensForClient, cachedIdTokensForClient.contains(idToken1));
    }

    @Test
    public void test_getClientsToLogOut_goldenPath_oneAud_multipleCachedIdTokens() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        // Two ID tokens cached for client1
        setTokenCacheExpectations(subject, idToken1, idToken, idToken2, idToken3);

        setRegisteredClients(Arrays.asList(client1, client2, client3));

        String cachedIdTokenString = jwtUtils.getHS256Jws(getIdTokenClaims(subject, issuerIdentifier, client1Id), client1Secret);

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                allowing(idToken).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken).getClientId();
                will(returnValue(client1Id));
                allowing(idToken).getTokenString();
                will(returnValue(cachedIdTokenString));
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
            }
        });

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientsToLogOut(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Resulting map of clients to cached ID tokens did not match expected size. Result was: " + result, 1, result.size());
        assertTrue("Resulting map of clients to cached ID tokens did not contain entry for [" + client1 + "]: " + result, result.containsKey(client1));

        // Should find the ID tokens that matched
        List<IDTokenImpl> cachedIdTokensForClient = result.get(client1);
        assertEquals("List of cached ID tokens for [" + client1 + "] did not match expected size. Result was: " + cachedIdTokensForClient, 2, cachedIdTokensForClient.size());
        assertTrue("List of cached ID tokens for [" + client1 + "] did not contain [" + idToken + "]: " + cachedIdTokensForClient, cachedIdTokensForClient.contains(idToken));
        assertTrue("List of cached ID tokens for [" + client1 + "] did not contain [" + idToken1 + "]: " + cachedIdTokensForClient, cachedIdTokensForClient.contains(idToken1));
    }

    @Test
    public void test_getClientsToLogOut_goldenPath_multipleAud_multipleCachedIdTokens() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id, client2Id, client3Id);

        // One ID token cached for each of client1, client2, and client3
        setTokenCacheExpectations(subject, idToken1, idToken2, idToken3);

        setRegisteredClients(Arrays.asList(client1, client2, client3));

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
                one(client2).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client2"));
                one(client3).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client3"));
            }
        });

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientsToLogOut(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Resulting map of clients to cached ID tokens did not match expected size. Result was: " + result, 3, result.size());
        assertTrue("Resulting map of clients to cached ID tokens did not contain entry for [" + client1 + "]: " + result, result.containsKey(client1));
        assertTrue("Resulting map of clients to cached ID tokens did not contain entry for [" + client2 + "]: " + result, result.containsKey(client2));
        assertTrue("Resulting map of clients to cached ID tokens did not contain entry for [" + client3 + "]: " + result, result.containsKey(client3));

        // Should find the ID tokens that matched
        List<IDTokenImpl> cachedIdTokensForClient = result.get(client1);
        assertEquals("List of cached ID tokens for [" + client1 + "] did not match expected size. Result was: " + cachedIdTokensForClient, 1, cachedIdTokensForClient.size());
        assertTrue("List of cached ID tokens for [" + client1 + "] did not contain [" + idToken1 + "]: " + cachedIdTokensForClient, cachedIdTokensForClient.contains(idToken1));

        cachedIdTokensForClient = result.get(client2);
        assertEquals("List of cached ID tokens for [" + client2 + "] did not match expected size. Result was: " + cachedIdTokensForClient, 1, cachedIdTokensForClient.size());
        assertTrue("List of cached ID tokens for [" + client2 + "] did not contain [" + idToken2 + "]: " + cachedIdTokensForClient, cachedIdTokensForClient.contains(idToken2));

        cachedIdTokensForClient = result.get(client3);
        assertEquals("List of cached ID tokens for [" + client3 + "] did not match expected size. Result was: " + cachedIdTokensForClient, 1, cachedIdTokensForClient.size());
        assertTrue("List of cached ID tokens for [" + client3 + "] did not contain [" + idToken3 + "]: " + cachedIdTokensForClient, cachedIdTokensForClient.contains(idToken3));
    }

    @Test
    public void test_getClientsToConsiderLoggingOut_noConfiguredClients() throws Exception {
        setRegisteredClients(new ArrayList<>());

        JwtClaims claims = getClaims(subject, issuerIdentifier, client1Id);

        List<OidcBaseClient> clients = builder.getClientsToConsiderLoggingOut(claims);

        assertNotNull("List of clients should not have been null but was.", clients);
        assertTrue("List of clients should have been empty but wasn't: " + clients, clients.isEmpty());
    }

    @Test
    public void test_getClientsToConsiderLoggingOut_audNotOneOfRegisteredClients() throws Exception {
        setRegisteredClients(Arrays.asList(client1, client2, client3));

        JwtClaims claims = getClaims(subject, issuerIdentifier, "client4");

        List<OidcBaseClient> clients = builder.getClientsToConsiderLoggingOut(claims);

        assertNotNull("List of clients should not have been null but was.", clients);
        assertTrue("List of clients should have been empty but wasn't: " + clients, clients.isEmpty());
    }

    @Test
    public void test_getClientsToConsiderLoggingOut_audMatchesOneRegisteredClient() throws Exception {
        setRegisteredClients(Arrays.asList(client1, client2, client3));

        JwtClaims claims = getClaims(subject, issuerIdentifier, client1Id);

        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
            }
        });

        List<OidcBaseClient> clients = builder.getClientsToConsiderLoggingOut(claims);

        assertNotNull("List of clients should not have been null but was.", clients);
        assertFalse("List of clients should not have been empty but was.", clients.isEmpty());
        assertEquals("Did not receive expected number of clients. Clients were: " + clients, 1, clients.size());
        assertTrue("List of clients did not contain expected client [" + client1 + "]. Clients were: " + clients, clients.contains(client1));
    }

    @Test
    public void test_getClientsToConsiderLoggingOut_audMatchesSubsetOfRegisteredClients_noneHaveLogoutUri() throws Exception {
        setRegisteredClients(Arrays.asList(client1, client2, client3));

        JwtClaims claims = getClaims(subject, issuerIdentifier, client2Id, client3Id);

        mockery.checking(new Expectations() {
            {
                one(client2).getBackchannelLogoutUri();
                will(returnValue(null));
                one(client3).getBackchannelLogoutUri();
                will(returnValue(null));
            }
        });

        List<OidcBaseClient> clients = builder.getClientsToConsiderLoggingOut(claims);

        assertNotNull("List of clients should not have been null but was.", clients);
        assertTrue("List of clients should have been empty but wasn't: " + clients, clients.isEmpty());
    }

    @Test
    public void test_getClientsToConsiderLoggingOut_audMatchesSubsetOfRegisteredClients_someHaveLogoutUri() throws Exception {
        setRegisteredClients(Arrays.asList(client1, client2, client3));

        JwtClaims claims = getClaims(subject, issuerIdentifier, client1Id, client2Id, client3Id);

        mockery.checking(new Expectations() {
            {
                one(client1).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client1"));
                one(client2).getBackchannelLogoutUri();
                will(returnValue(null));
                one(client3).getBackchannelLogoutUri();
                will(returnValue("https://localhost/my/logout/uri/client3"));
            }
        });

        List<OidcBaseClient> clients = builder.getClientsToConsiderLoggingOut(claims);

        assertNotNull("List of clients should not have been null but was.", clients);
        assertFalse("List of clients should not have been empty but was.", clients.isEmpty());
        assertEquals("Did not receive expected number of clients. Clients were: " + clients, 2, clients.size());
        assertTrue("List of clients did not contain expected client [" + client1 + "]. Clients were: " + clients, clients.contains(client1));
        assertTrue("List of clients did not contain expected client [" + client3 + "]. Clients were: " + clients, clients.contains(client3));
    }

    @Test
    public void test_getClientToCachedIdTokensMap_noCachedIdTokens() throws Exception {
        List<OidcBaseClient> clientsUnderConsiderationForLogout = Arrays.asList(client1);

        // No ID tokens are in the cache
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1, accessToken2);

        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientToCachedIdTokensMap(clientsUnderConsiderationForLogout, allCachedUserTokens, idTokenClaims);
        assertNotNull("Resulting map of clients to cached ID tokens should not have been null but was.", result);
        assertTrue("Resulting map of clients to cached ID tokens should have been empty but wasn't: " + result, result.isEmpty());
    }

    @Test
    public void test_getClientToCachedIdTokensMap_multipleCachedIdTokens_noneMatchClientUnderConsideration() throws Exception {
        List<OidcBaseClient> clientsUnderConsiderationForLogout = Arrays.asList(client1);

        // No ID tokens for client1 are cached
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1, idToken3, accessToken2, idToken2);

        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientToCachedIdTokensMap(clientsUnderConsiderationForLogout, allCachedUserTokens, idTokenClaims);
        assertNotNull("Resulting map of clients to cached ID tokens should not have been null but was.", result);
        assertTrue("Resulting map of clients to cached ID tokens should have been empty but wasn't: " + result, result.isEmpty());
    }

    @Test
    public void test_getClientToCachedIdTokensMap_oneCachedIdToken_matchesClientUnderConsideration_differentIss() throws Exception {
        List<OidcBaseClient> clientsUnderConsiderationForLogout = Arrays.asList(client1);

        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(idToken, accessToken1, accessToken2);

        // The ID token hint has been verified to have the correct issuer at this point, so the cached token must be the one with a different issuer value
        String cachedIdTokenString = jwtUtils.getHS256Jws(getIdTokenClaims(subject, "some other issuer", client1Id), client1Secret);

        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                allowing(idToken).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken).getClientId();
                will(returnValue(client1Id));
                allowing(idToken).getTokenString();
                will(returnValue(cachedIdTokenString));
            }
        });

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientToCachedIdTokensMap(clientsUnderConsiderationForLogout, allCachedUserTokens, idTokenClaims);
        assertNotNull("Resulting map of clients to cached ID tokens should not have been null but was.", result);
        assertTrue("Resulting map of clients to cached ID tokens should have been empty but wasn't: " + result, result.isEmpty());
    }

    @Test
    public void test_getClientToCachedIdTokensMap_oneCachedIdToken_matchesClientUnderConsideration_differentSub() throws Exception {
        List<OidcBaseClient> clientsUnderConsiderationForLogout = Arrays.asList(client1);

        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1, accessToken2, idToken1);

        // ID token hint has a different sub claim
        JwtClaims idTokenClaims = getClaims("some other subject", issuerIdentifier, client1Id);

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientToCachedIdTokensMap(clientsUnderConsiderationForLogout, allCachedUserTokens, idTokenClaims);
        assertNotNull("Resulting map of clients to cached ID tokens should not have been null but was.", result);
        assertTrue("Resulting map of clients to cached ID tokens should have been empty but wasn't: " + result, result.isEmpty());
    }

    @Test
    public void test_getClientToCachedIdTokensMap_oneCachedIdToken_matchesClientUnderConsideration_allClaimsMatch() throws Exception {
        List<OidcBaseClient> clientsUnderConsiderationForLogout = Arrays.asList(client1);

        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1, accessToken2, idToken1);

        // All requisite claims in the ID token hint match the cached ID token
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientToCachedIdTokensMap(clientsUnderConsiderationForLogout, allCachedUserTokens, idTokenClaims);
        assertNotNull("Resulting map of clients to cached ID tokens should not have been null but was.", result);
        assertEquals("Resulting map of clients to cached ID tokens did not match expected size. Result was: " + result, 1, result.size());
    }

    @Test
    public void test_getClientToCachedIdTokensMap_multipleCachedIdTokens_oneMatchesClientUnderConsideration_allClaimsMatch() throws Exception {
        List<OidcBaseClient> clientsUnderConsiderationForLogout = Arrays.asList(client1);

        // Multiple ID tokens in the cache, but only one for client1
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1, idToken2, accessToken2, idToken1, idToken3);

        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });

        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientToCachedIdTokensMap(clientsUnderConsiderationForLogout, allCachedUserTokens, idTokenClaims);
        assertNotNull("Resulting map of clients to cached ID tokens should not have been null but was.", result);
        assertEquals("Resulting map of clients to cached ID tokens did not match expected size. Result was: " + result, 1, result.size());
    }

    @Test
    public void test_getClientToCachedIdTokensMap_multipleCachedIdTokens_multipleMatchClientUnderConsideration() throws Exception {
        List<OidcBaseClient> clientsUnderConsiderationForLogout = Arrays.asList(client1);

        // Multiple ID tokens in the cache, more than one are for client1
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1, idToken, accessToken2, idToken1, idToken3);

        // Set a "sid" claim for one of the ID tokens; this should prevent it from matching
        JSONObject cachedTokenClaims = getIdTokenClaims(subject, issuerIdentifier, client1Id);
        cachedTokenClaims.put("sid", sid);
        String cachedIdTokenString = jwtUtils.getHS256Jws(cachedTokenClaims, client1Secret);

        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                allowing(idToken).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken).getClientId();
                will(returnValue(client1Id));
                allowing(idToken).getTokenString();
                will(returnValue(cachedIdTokenString));
            }
        });

        // Should find an entry for client1
        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientToCachedIdTokensMap(clientsUnderConsiderationForLogout, allCachedUserTokens, idTokenClaims);
        assertNotNull("Resulting map of clients to cached ID tokens should not have been null but was.", result);
        assertEquals("Resulting map of clients to cached ID tokens did not match expected size. Result was: " + result, 1, result.size());
        assertTrue("Resulting map of clients to cached ID tokens did not contain entry for [" + client1 + "]: " + result, result.containsKey(client1));

        // Should find the one ID token that matched
        List<IDTokenImpl> cachedIdTokensForClient = result.get(client1);
        assertEquals("List of cached ID tokens for [" + client1 + "] did not match expected size. Result was: " + cachedIdTokensForClient, 1, cachedIdTokensForClient.size());
        assertTrue("List of cached ID tokens for [" + client1 + "] did not contain [" + idToken1 + "]: " + cachedIdTokensForClient, cachedIdTokensForClient.contains(idToken1));
    }

    @Test
    public void test_getClientToCachedIdTokensMap_multipleClientsUnderConsideration_multipleCachedIdTokens_multipleMatchClientsUnderConsideration() throws Exception {
        // Multiple clients are under consideration for logout
        List<OidcBaseClient> clientsUnderConsiderationForLogout = Arrays.asList(client1, client2);

        // A couple cached ID tokens are for client1, another ID token is for client2
        Collection<OAuth20Token> allCachedUserTokens = Arrays.asList(accessToken1, idToken, accessToken2, idToken1, idToken2, idToken3);

        String cachedIdTokenString = jwtUtils.getHS256Jws(getIdTokenClaims(subject, issuerIdentifier, client1Id), client1Secret);

        // ID token hint aud claim lists multiple clients, two of which are under consideration for logout
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id, client2Id, client3Id);

        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                allowing(idToken).getType();
                will(returnValue(OAuth20Constants.ID_TOKEN));
                allowing(idToken).getClientId();
                will(returnValue(client1Id));
                allowing(idToken).getTokenString();
                will(returnValue(cachedIdTokenString));
            }
        });

        // Should find entries for all clients under consideration for logout that were also in the ID token hint's aud claim
        Map<OidcBaseClient, List<IDTokenImpl>> result = builder.getClientToCachedIdTokensMap(clientsUnderConsiderationForLogout, allCachedUserTokens, idTokenClaims);
        assertNotNull("Resulting map of clients to cached ID tokens should not have been null but was.", result);
        assertEquals("Resulting map of clients to cached ID tokens did not match expected size. Result was: " + result, 2, result.size());
        assertTrue("Resulting map of clients to cached ID tokens did not contain entry for [" + client1 + "]: " + result, result.containsKey(client1));
        assertTrue("Resulting map of clients to cached ID tokens did not contain entry for [" + client2 + "]: " + result, result.containsKey(client2));

        List<IDTokenImpl> cachedIdTokensForClient = result.get(client1);
        assertEquals("List of cached ID tokens for [" + client1 + "] did not match expected size. Result was: " + cachedIdTokensForClient, 2, cachedIdTokensForClient.size());
        assertTrue("List of cached ID tokens for [" + client1 + "] did not contain [" + idToken + "]: " + cachedIdTokensForClient, cachedIdTokensForClient.contains(idToken));
        assertTrue("List of cached ID tokens for [" + client1 + "] did not contain [" + idToken1 + "]: " + cachedIdTokensForClient, cachedIdTokensForClient.contains(idToken1));

        cachedIdTokensForClient = result.get(client2);
        assertEquals("List of cached ID tokens for [" + client2 + "] did not match expected size. Result was: " + cachedIdTokensForClient, 1, cachedIdTokensForClient.size());
        assertTrue("List of cached ID tokens for [" + client2 + "] did not contain [" + idToken2 + "]: " + cachedIdTokensForClient, cachedIdTokensForClient.contains(idToken2));
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_differentClientIds() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        mockery.checking(new Expectations() {
            {
                allowing(token).getClientId();
                will(returnValue(client2Id));
            }
        });
        assertFalse("A cached token with a different client ID should not have matched " + client1 + ".", builder.isIdTokenWithMatchingClaims(idTokenClaims, token, client1));
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_cachedTokenMissingIss() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        String cachedTokenString = jwtUtils.getHS256Jws(getIdTokenClaims(subject, null, client1Id), client1Secret);
        mockery.checking(new Expectations() {
            {
                one(idToken).getClientId();
                will(returnValue(client1Id));
                one(idToken).getTokenString();
                will(returnValue(cachedTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        try {
            boolean result = builder.isIdTokenWithMatchingClaims(idTokenClaims, idToken, client1);
            fail("Should have thrown an exception but didn't. Was ID token considered a match? " + result + ".");
        } catch (LogoutTokenBuilderException e) {
            verifyException(e, CWWKS1643E_LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN + ".*" + CWWKS1646E_ID_TOKEN_MISSING_REQUIRED_CLAIMS + ".*" + "iss");
        }
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_differentIssuer() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        JSONObject cachedTokenClaims = getIdTokenClaims(subject, "some other issuer", client1Id);
        String cachedTokenString = jwtUtils.getHS256Jws(cachedTokenClaims, client1Secret);
        mockery.checking(new Expectations() {
            {
                one(idToken).getClientId();
                will(returnValue(client1Id));
                one(idToken).getTokenString();
                will(returnValue(cachedTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        assertFalse("A cached token with a different \"iss\" claim should not have matched. ID token hint claims: " + idTokenClaims + ". Cached token claims: " + cachedTokenClaims
                    + ".", builder.isIdTokenWithMatchingClaims(idTokenClaims, idToken, client1));
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_cachedTokenMissingAud() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        String cachedTokenString = jwtUtils.getHS256Jws(getIdTokenClaims(subject, issuerIdentifier), client1Secret);
        mockery.checking(new Expectations() {
            {
                one(idToken).getClientId();
                will(returnValue(client1Id));
                one(idToken).getTokenString();
                will(returnValue(cachedTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        try {
            boolean result = builder.isIdTokenWithMatchingClaims(idTokenClaims, idToken, client1);
            fail("Should have thrown an exception but didn't. Was ID token considered a match? " + result + ".");
        } catch (LogoutTokenBuilderException e) {
            verifyException(e, CWWKS1643E_LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN + ".*" + CWWKS1646E_ID_TOKEN_MISSING_REQUIRED_CLAIMS + ".*" + "aud");
        }
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_differentAud() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        JSONObject cachedTokenClaims = getIdTokenClaims(subject, issuerIdentifier, client2Id);
        String cachedTokenString = jwtUtils.getHS256Jws(cachedTokenClaims, client1Secret);
        mockery.checking(new Expectations() {
            {
                one(idToken).getClientId();
                will(returnValue(client1Id));
                one(idToken).getTokenString();
                will(returnValue(cachedTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        assertFalse("A cached token with a different \"aud\" claim should not have matched. ID token hint claims: " + idTokenClaims + ". Cached token claims: " + cachedTokenClaims
                    + ".", builder.isIdTokenWithMatchingClaims(idTokenClaims, idToken, client1));
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_cachedTokenMissingSub() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        String cachedTokenString = jwtUtils.getHS256Jws(getIdTokenClaims(null, issuerIdentifier, client1Id), client1Secret);
        mockery.checking(new Expectations() {
            {
                one(idToken).getClientId();
                will(returnValue(client1Id));
                one(idToken).getTokenString();
                will(returnValue(cachedTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        try {
            boolean result = builder.isIdTokenWithMatchingClaims(idTokenClaims, idToken, client1);
            fail("Should have thrown an exception but didn't. Was ID token considered a match? " + result + ".");
        } catch (LogoutTokenBuilderException e) {
            verifyException(e, CWWKS1643E_LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN + ".*" + CWWKS1646E_ID_TOKEN_MISSING_REQUIRED_CLAIMS + ".*" + "sub");
        }
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_differentSub() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        JSONObject cachedTokenClaims = getIdTokenClaims("some other sub", issuerIdentifier, client1Id);
        String cachedTokenString = jwtUtils.getHS256Jws(cachedTokenClaims, client1Secret);
        mockery.checking(new Expectations() {
            {
                one(idToken).getClientId();
                will(returnValue(client1Id));
                one(idToken).getTokenString();
                will(returnValue(cachedTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        assertFalse("A cached token with a different \"sub\" claim should not have matched. ID token hint claims: " + idTokenClaims + ". Cached token claims: " + cachedTokenClaims
                    + ".", builder.isIdTokenWithMatchingClaims(idTokenClaims, idToken, client1));
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_idTokenMissingSid_cachedTokenContainsSid() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        JSONObject cachedTokenClaims = getIdTokenClaims("some other sub", issuerIdentifier, client1Id);
        cachedTokenClaims.put("sid", sid);
        String cachedTokenString = jwtUtils.getHS256Jws(cachedTokenClaims, client1Secret);
        mockery.checking(new Expectations() {
            {
                one(idToken).getClientId();
                will(returnValue(client1Id));
                one(idToken).getTokenString();
                will(returnValue(cachedTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        assertFalse("A cached token with a \"sid\" claim should not have matched an ID token hint with one. ID token hint claims: " + idTokenClaims + ". Cached token claims: "
                    + cachedTokenClaims + ".", builder.isIdTokenWithMatchingClaims(idTokenClaims, idToken, client1));
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_idTokenContainsSid_cachedTokenMissingSid() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setStringClaim("sid", sid);

        JSONObject cachedTokenClaims = getIdTokenClaims("some other sub", issuerIdentifier, client1Id);
        String cachedTokenString = jwtUtils.getHS256Jws(cachedTokenClaims, client1Secret);
        mockery.checking(new Expectations() {
            {
                one(idToken).getClientId();
                will(returnValue(client1Id));
                one(idToken).getTokenString();
                will(returnValue(cachedTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        assertFalse("A cached token with missing a \"sid\" claim should not have matched. ID token hint claims: " + idTokenClaims + ". Cached token claims: " + cachedTokenClaims
                    + ".", builder.isIdTokenWithMatchingClaims(idTokenClaims, idToken, client1));
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_differentSid() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setStringClaim("sid", sid);

        JSONObject cachedTokenClaims = getIdTokenClaims("some other sub", issuerIdentifier, client1Id);
        cachedTokenClaims.put("sid", "some other sid value");
        String cachedTokenString = jwtUtils.getHS256Jws(cachedTokenClaims, client1Secret);
        mockery.checking(new Expectations() {
            {
                one(idToken).getClientId();
                will(returnValue(client1Id));
                one(idToken).getTokenString();
                will(returnValue(cachedTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        assertFalse("A cached token with a different \"sid\" claim should not have matched. ID token hint claims: " + idTokenClaims + ". Cached token claims: " + cachedTokenClaims
                    + ".", builder.isIdTokenWithMatchingClaims(idTokenClaims, idToken, client1));
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_allNecessaryClaimsMatch_excludeSid() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);

        JSONObject cachedTokenClaims = getIdTokenClaims(subject, issuerIdentifier, client1Id);
        String cachedTokenString = jwtUtils.getHS256Jws(cachedTokenClaims, client1Secret);
        mockery.checking(new Expectations() {
            {
                one(idToken).getClientId();
                will(returnValue(client1Id));
                one(idToken).getTokenString();
                will(returnValue(cachedTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        assertTrue("A cached token with matching claims should have matched. ID token hint claims: " + idTokenClaims + ". Cached token claims: " + cachedTokenClaims + ".",
                   builder.isIdTokenWithMatchingClaims(idTokenClaims, idToken, client1));
    }

    @Test
    public void test_isIdTokenWithMatchingClaims_allNecessaryClaimsMatch_includeSid() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setStringClaim("sid", sid);

        JSONObject cachedTokenClaims = getIdTokenClaims(subject, issuerIdentifier, client1Id);
        cachedTokenClaims.put("sid", sid);
        cachedTokenClaims.put(Claims.ISSUED_AT, 1);
        cachedTokenClaims.put(Claims.EXPIRATION, 2);
        cachedTokenClaims.put("nonce", "some nonce value");
        String cachedTokenString = jwtUtils.getHS256Jws(cachedTokenClaims, client1Secret);
        mockery.checking(new Expectations() {
            {
                one(idToken).getClientId();
                will(returnValue(client1Id));
                one(idToken).getTokenString();
                will(returnValue(cachedTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        assertTrue("A cached token with matching claims should have matched. ID token hint claims: " + idTokenClaims + ". Cached token claims: " + cachedTokenClaims + ".",
                   builder.isIdTokenWithMatchingClaims(idTokenClaims, idToken, client1));
    }

    @Test
    public void test_isSameClientId_tokenMissingClientId() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(token).getClientId();
                will(returnValue(null));
            }
        });
        assertFalse("A cached token missing a client ID should not have matched " + client1 + ".", builder.isSameClientId(token, client1));
    }

    @Test
    public void test_isSameClientId_tokenHasDifferentClientId() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(token).getClientId();
                will(returnValue(client2Id));
            }
        });
        assertFalse("A cached token with a different client ID should not have matched " + client1 + ".", builder.isSameClientId(token, client1));
    }

    @Test
    public void test_isSameClientId_tokenHasSameClientId() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(token).getClientId();
                will(returnValue(client1Id));
            }
        });
        assertTrue("A cached token with the same client ID should have matched " + client1 + ".", builder.isSameClientId(token, client1));
    }

    @Test
    public void test_isSameAud_emptyAud() throws Exception {
        JwtClaims cachedIdTokenClaims = getClaims(subject, issuerIdentifier);
        cachedIdTokenClaims.setAudience(new ArrayList<>());
        assertFalse("A token with an empty \"aud\" claim should not have matched " + client1 + ".", builder.isSameAud(client1, cachedIdTokenClaims));
    }

    @Test
    public void test_isSameAud_audSingleString_audDoesNotContainClient() throws Exception {
        JwtClaims cachedIdTokenClaims = getClaims(subject, issuerIdentifier, client2Id);
        assertFalse("A token with an \"aud\" claim that doesn't include the client ID should not have matched " + client1 + ".", builder.isSameAud(client1, cachedIdTokenClaims));
    }

    @Test
    public void test_isSameAud_audSingleString_matchesClient() throws Exception {
        JwtClaims cachedIdTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        assertTrue("A token with an \"aud\" claim that matches the client ID should have matched " + client1 + ".", builder.isSameAud(client1, cachedIdTokenClaims));
    }

    @Test
    public void test_isSameAud_audList_audDoesNotContainClient() throws Exception {
        JwtClaims cachedIdTokenClaims = getClaims(subject, issuerIdentifier, client2Id, client3Id);
        assertFalse("A token with an \"aud\" claim that doesn't include the client ID should not have matched " + client1 + ".", builder.isSameAud(client1, cachedIdTokenClaims));
    }

    @Test
    public void test_isSameAud_audList_audContainsClient() throws Exception {
        JwtClaims cachedIdTokenClaims = getClaims(subject, issuerIdentifier, client2Id, client1Id, client3Id);
        assertTrue("A token with an \"aud\" claim that includes the client ID should have matched " + client1 + ".", builder.isSameAud(client1, cachedIdTokenClaims));
    }

    @Test
    public void test_isSameSub_differentValues() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        JwtClaims cachedIdTokenClaims = getClaims("some other subject", issuerIdentifier, client1Id);
        assertFalse("Two sets of claims with different \"sub\" entries should not be considered to have the same sub.", builder.isSameSub(idTokenClaims, cachedIdTokenClaims));
    }

    @Test
    public void test_isSameSub_sameValue() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        JwtClaims cachedIdTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        assertTrue("Two sets of claims with identical \"sub\" entries should be considered to have the same sub.", builder.isSameSub(idTokenClaims, cachedIdTokenClaims));
    }

    @Test
    public void test_isSameSid_bothClaimsMissingSid() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        JwtClaims cachedIdTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        assertTrue("Two sets of claims both missing a \"sid\" entry should be considered to have the same sid.", builder.isSameSid(idTokenClaims, cachedIdTokenClaims));
    }

    @Test
    public void test_isSameSid_idTokenHasSid_cachedTokenDoesNot() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setStringClaim("sid", sid);
        JwtClaims cachedIdTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        assertFalse("One set of claims with a \"sid\" entry and the other without should not be considered to have the same sid.",
                    builder.isSameSid(idTokenClaims, cachedIdTokenClaims));
    }

    @Test
    public void test_isSameSid_cachedTokenHasSid_idTokenDoesNot() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        JwtClaims cachedIdTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        cachedIdTokenClaims.setStringClaim("sid", sid);
        assertFalse("One set of claims with a \"sid\" entry and the other without should not be considered to have the same sid.",
                    builder.isSameSid(idTokenClaims, cachedIdTokenClaims));
    }

    @Test
    public void test_isSameSid_bothHaveSid_differentValues() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setStringClaim("sid", sid);
        JwtClaims cachedIdTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        cachedIdTokenClaims.setStringClaim("sid", "some other sid");
        assertFalse("Two sets of claims with different \"sid\" entries should not be considered to have the same sid.", builder.isSameSid(idTokenClaims, cachedIdTokenClaims));
    }

    @Test
    public void test_isSameSid_bothHaveSid_sameValue() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setStringClaim("sid", sid);
        JwtClaims cachedIdTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        cachedIdTokenClaims.setStringClaim("sid", sid);
        assertTrue("Two sets of claims with identical \"sid\" entries should be considered to have the same sid.", builder.isSameSid(idTokenClaims, cachedIdTokenClaims));
    }

    @Test
    public void test_addCachedIdTokenToMap_noEntries() throws Exception {
        Map<OidcBaseClient, List<IDTokenImpl>> cachedIdTokensMap = new HashMap<OidcBaseClient, List<IDTokenImpl>>();

        builder.addCachedIdTokenToMap(cachedIdTokensMap, client1, idToken1);

        assertEquals("Updated map did was not the expected size: " + cachedIdTokensMap, 1, cachedIdTokensMap.size());
        assertTrue("Updated map did not contain entry for [" + client1 + "]: " + cachedIdTokensMap, cachedIdTokensMap.containsKey(client1));
        List<IDTokenImpl> cachedIdTokensForClient = cachedIdTokensMap.get(client1);
        assertEquals("List of cached ID tokens for client was not the expected size: " + cachedIdTokensForClient, 1, cachedIdTokensForClient.size());
        assertEquals("Cached ID token did not match expected value.", idToken1, cachedIdTokensForClient.get(0));
    }

    @Test
    public void test_addCachedIdTokenToMap_entriesForOtherClients() throws Exception {
        Map<OidcBaseClient, List<IDTokenImpl>> cachedIdTokensMap = new HashMap<OidcBaseClient, List<IDTokenImpl>>();
        cachedIdTokensMap.put(client2, new ArrayList<>());
        cachedIdTokensMap.put(client3, new ArrayList<>());

        builder.addCachedIdTokenToMap(cachedIdTokensMap, client1, idToken1);

        assertEquals("Updated map did was not the expected size: " + cachedIdTokensMap, 3, cachedIdTokensMap.size());
        assertTrue("Updated map did not contain entry for [" + client1 + "]: " + cachedIdTokensMap, cachedIdTokensMap.containsKey(client1));
        List<IDTokenImpl> cachedIdTokensForClient = cachedIdTokensMap.get(client1);
        assertEquals("List of cached ID tokens for client was not the expected size: " + cachedIdTokensForClient, 1, cachedIdTokensForClient.size());
        assertEquals("Cached ID token did not match expected value.", idToken1, cachedIdTokensForClient.get(0));
    }

    @Test
    public void test_addCachedIdTokenToMap_oneExistingEntryForClient() throws Exception {
        Map<OidcBaseClient, List<IDTokenImpl>> cachedIdTokensMap = new HashMap<OidcBaseClient, List<IDTokenImpl>>();
        List<IDTokenImpl> existingCachedTokens = new ArrayList<>();
        existingCachedTokens.add(idToken1);
        cachedIdTokensMap.put(client1, existingCachedTokens);
        cachedIdTokensMap.put(client2, new ArrayList<>());

        builder.addCachedIdTokenToMap(cachedIdTokensMap, client1, idToken2);

        assertEquals("Updated map did was not the expected size: " + cachedIdTokensMap, 2, cachedIdTokensMap.size());
        assertTrue("Updated map did not contain entry for [" + client1 + "]: " + cachedIdTokensMap, cachedIdTokensMap.containsKey(client1));
        List<IDTokenImpl> cachedIdTokensForClient = cachedIdTokensMap.get(client1);
        assertEquals("List of cached ID tokens for client was not the expected size: " + cachedIdTokensForClient, 2, cachedIdTokensForClient.size());
        assertEquals("Cached ID token #1 did not match expected value. Cached tokens were: " + cachedIdTokensForClient, idToken1, cachedIdTokensForClient.get(0));
        assertEquals("Cached ID token #2 did not match expected value. Cached tokens were: " + cachedIdTokensForClient, idToken2, cachedIdTokensForClient.get(1));
    }

    @Test
    public void test_createLogoutTokenForClientFromCachedIdToken_idTokenDifferentIssuer() throws Exception {
        Map<OidcBaseClient, List<String>> clientsAndLogoutTokens = new HashMap<OidcBaseClient, List<String>>();

        JSONObject idTokenClaims = getIdTokenClaims("some subject", "some other issuer", "some audience");
        final String idTokenString = jwtUtils.getHS256Jws(idTokenClaims, "some secret");
        mockery.checking(new Expectations() {
            {
                one(idToken).getTokenString();
                will(returnValue(idTokenString));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        try {
            builder.createLogoutTokenForClientFromCachedIdToken(clientsAndLogoutTokens, client1, idToken);
            fail("Should have thrown an exception but didn't. Clients and logout tokens map was: " + clientsAndLogoutTokens);
        } catch (LogoutTokenBuilderException e) {
            verifyException(e, CWWKS1643E_LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN + ".*" + CWWKS1645E_ID_TOKEN_ISSUER_NOT_THIS_OP);
        }
    }

    @Test
    public void test_createLogoutTokenForClientFromCachedIdToken_noExistingLogoutTokens() throws Exception {
        Map<OidcBaseClient, List<String>> clientsAndLogoutTokens = new HashMap<OidcBaseClient, List<String>>();

        JSONObject idTokenClaims = getIdTokenClaims(subject, issuerIdentifier, client1Id);
        final String idTokenString = jwtUtils.getHS256Jws(idTokenClaims, "some secret");
        mockery.checking(new Expectations() {
            {
                one(idToken).getTokenString();
                will(returnValue(idTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        builder.createLogoutTokenForClientFromCachedIdToken(clientsAndLogoutTokens, client1, idToken);

        assertEquals("Did not find expected number of entries in the logout tokens map: " + clientsAndLogoutTokens, 1, clientsAndLogoutTokens.size());
        assertTrue("Logout tokens map did not contain entry for client [" + client1 + "]. Map was: " + clientsAndLogoutTokens, clientsAndLogoutTokens.containsKey(client1));
    }

    @Test
    public void test_createLogoutTokenForClientFromCachedIdToken_someExistingLogoutTokensDifferentClient() throws Exception {
        Map<OidcBaseClient, List<String>> clientsAndLogoutTokens = new HashMap<OidcBaseClient, List<String>>();
        clientsAndLogoutTokens.put(client2, Arrays.asList("one", "two"));

        JSONObject idTokenClaims = getIdTokenClaims(subject, issuerIdentifier, client1Id);
        final String idTokenString = jwtUtils.getHS256Jws(idTokenClaims, "some secret");
        mockery.checking(new Expectations() {
            {
                one(idToken).getTokenString();
                will(returnValue(idTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        builder.createLogoutTokenForClientFromCachedIdToken(clientsAndLogoutTokens, client1, idToken);

        assertEquals("Did not find expected number of entries in the logout tokens map: " + clientsAndLogoutTokens, 2, clientsAndLogoutTokens.size());
        assertTrue("Logout tokens map did not contain entry for client [" + client1 + "]. Map was: " + clientsAndLogoutTokens, clientsAndLogoutTokens.containsKey(client1));

        // Verify the new logout token that was created
        List<String> clientLogoutTokens = clientsAndLogoutTokens.get(client1);
        assertEquals("Did not find the expected number of logout tokens for client [" + client1 + "]: " + clientLogoutTokens, 1, clientLogoutTokens.size());
        JwtClaims logoutTokenClaims = builder.getClaimsFromIdTokenString(clientLogoutTokens.get(0));
        verifyLogoutTokenClaims(logoutTokenClaims, Arrays.asList(client1Id), subject, null);
    }

    @Test
    public void test_createLogoutTokenForClientFromCachedIdToken_someExistingLogoutTokens() throws Exception {
        Map<OidcBaseClient, List<String>> clientsAndLogoutTokens = new HashMap<OidcBaseClient, List<String>>();
        // Client already contains entries for a couple logout tokens
        List<String> client1Tokens = new ArrayList<>();
        client1Tokens.add("1-one");
        client1Tokens.add("1-two");
        List<String> client2Tokens = new ArrayList<>();
        client2Tokens.add("2-one");
        client2Tokens.add("2-two");
        clientsAndLogoutTokens.put(client1, client1Tokens);
        clientsAndLogoutTokens.put(client2, client2Tokens);

        JSONObject idTokenClaims = getIdTokenClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.put("sid", sid);
        final String idTokenString = jwtUtils.getHS256Jws(idTokenClaims, "some secret");
        mockery.checking(new Expectations() {
            {
                one(idToken).getTokenString();
                will(returnValue(idTokenString));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        builder.createLogoutTokenForClientFromCachedIdToken(clientsAndLogoutTokens, client1, idToken);

        assertEquals("Did not find expected number of entries in the logout tokens map: " + clientsAndLogoutTokens, 2, clientsAndLogoutTokens.size());
        assertTrue("Logout tokens map did not contain entry for client [" + client1 + "]. Map was: " + clientsAndLogoutTokens, clientsAndLogoutTokens.containsKey(client1));

        // Verify the new logout token that was created
        List<String> clientLogoutTokens = clientsAndLogoutTokens.get(client1);
        assertEquals("Did not find the expected number of logout tokens for client [" + client1 + "]: " + clientLogoutTokens, 3, clientLogoutTokens.size());
        JwtClaims logoutTokenClaims = builder.getClaimsFromIdTokenString(clientLogoutTokens.get(2));
        verifyLogoutTokenClaims(logoutTokenClaims, Arrays.asList(client1Id), subject, sid);
    }

    @Test
    public void test_createLogoutTokenForClient_idTokenContainsSub() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        String result = builder.createLogoutTokenForClient(client1, idTokenClaims);
        verifyLogoutToken(result, Arrays.asList(client1Id), subject, null);
    }

    @Test
    public void test_createLogoutTokenForClient_idTokenContainsSubAndSid() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setStringClaim("sid", sid);
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        String result = builder.createLogoutTokenForClient(client1, idTokenClaims);
        verifyLogoutToken(result, Arrays.asList(client1Id), subject, sid);
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_subGoldenPath() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        JwtClaims result = builder.populateLogoutTokenClaimsFromIdToken(client1, idTokenClaims);

        verifyLogoutTokenClaims(result, Arrays.asList(client1Id), subject, null);
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_sidNotString() throws Exception {
        JwtClaims idTokenClaims = getClaims(subject, issuerIdentifier, client1Id);
        idTokenClaims.setClaim("sid", 123);
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
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
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        JwtClaims result = builder.populateLogoutTokenClaimsFromIdToken(client1, idTokenClaims);

        verifyLogoutTokenClaims(result, Arrays.asList(client1Id), subject, sid);
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_idTokenIssuerDifferent() throws Exception {
        // Ensure that the server config's issuer value is used in the logout token, not the ID token's issuer.
        // These two values should never be different since this server issued the ID token, but we can still check.
        JwtClaims idTokenClaims = getClaims(subject, "some other issuer", client1Id);
        idTokenClaims.setClaim("sid", sid);
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        JwtClaims result = builder.populateLogoutTokenClaimsFromIdToken(client1, idTokenClaims);

        verifyLogoutTokenClaims(result, Arrays.asList(client1Id), subject, sid);
    }

    @Test
    public void test_populateLogoutTokenClaims() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        JwtClaims result = builder.populateLogoutTokenClaims(client1);

        verifyLogoutTokenClaims(result, Arrays.asList(client1Id), null, null);
    }

    @Test
    public void test_getIssuer_configIncludesIssuerIdentifier() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        String result = builder.getIssuer();
        assertEquals("Issuer value did not match expected value.", issuerIdentifier, result);
    }

    @Test
    public void test_getIssuer_configMissingIssuerIdentifier_standardHttpPort() throws Exception {
        final String scheme = "http";
        final String serverName = "myserver";
        final String expectedIssuerPath = "/some/path/to/the/OP";
        final String requestUri = expectedIssuerPath + "/end_session";
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(null));
                one(request).getScheme();
                will(returnValue(scheme));
                one(request).getServerName();
                will(returnValue(serverName));
                one(request).getServerPort();
                will(returnValue(80));
                one(request).getRequestURI();
                will(returnValue(requestUri));
            }
        });
        String expectedIssuer = scheme + "://" + serverName + expectedIssuerPath;
        String result = builder.getIssuer();
        assertEquals("Issuer value did not match expected value.", expectedIssuer, result);
    }

    @Test
    public void test_getIssuer_configMissingIssuerIdentifier_standardHttpsPort() throws Exception {
        final String scheme = "https";
        final String serverName = "myserver";
        final String expectedIssuerPath = "/some/path/to/the/OP";
        final String requestUri = expectedIssuerPath + "/end_session";
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
                one(request).getRequestURI();
                will(returnValue(requestUri));
            }
        });
        String expectedIssuer = scheme + "://" + serverName + expectedIssuerPath;
        String result = builder.getIssuer();
        assertEquals("Issuer value did not match expected value.", expectedIssuer, result);
    }

    @Test
    public void test_getIssuer_configMissingIssuerIdentifier_nonStandardPort() throws Exception {
        final String scheme = "https";
        final String serverName = "myserver";
        final int port = 98765;
        final String expectedIssuerPath = "/some/path/to/the/OP";
        final String requestUri = expectedIssuerPath + "/end_session";
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(null));
                one(request).getScheme();
                will(returnValue(scheme));
                one(request).getServerName();
                will(returnValue(serverName));
                one(request).getServerPort();
                will(returnValue(port));
                one(request).getRequestURI();
                will(returnValue(requestUri));
            }
        });
        String expectedIssuer = scheme + "://" + serverName + ":" + port + expectedIssuerPath;
        String result = builder.getIssuer();
        assertEquals("Issuer value did not match expected value.", expectedIssuer, result);
    }

    void verifyLogoutToken(String logoutTokenString, List<String> expectedAudiences, String expectedSubject, String expectedSid) throws Exception {
        assertNotNull("Logout token string should not have been null but was.", logoutTokenString);

        // Verify token header values
        JwtContext resultContext = Jose4jUtil.parseJwtWithoutValidation(logoutTokenString);
        JsonWebStructure jsonWebStructure = resultContext.getJoseObjects().get(0);
        assertEquals("JWT alg header did not match expected value.", "HS256", jsonWebStructure.getAlgorithmHeaderValue());
        assertEquals("JWT typ header did not match expected value.", "JWT", jsonWebStructure.getHeader("typ"));

        verifyLogoutTokenClaims(resultContext.getJwtClaims(), expectedAudiences, expectedSubject, expectedSid);
    }

    @SuppressWarnings("unchecked")
    private void verifyLogoutTokenClaims(JwtClaims result, List<String> expectedAudiences, String expectedSubject, String expectedSid) throws MalformedClaimException {
        assertNotNull("Result should not have been null but was.", result);

        long now = System.currentTimeMillis() / 1000;
        long timeFrameStart = now - 5;
        long timeFrameEnd = now + 5;

        assertEquals("Issuer did not match expected value. Claims were: " + result, issuerIdentifier, result.getIssuer());
        assertEquals("Audience did not match expected value. Claims were: " + result, expectedAudiences, result.getAudience());
        long issuedAt = result.getIssuedAt().getValue();
        assertTrue("Issued at time (" + issuedAt + ") is not in an expected reasonable time frame (" + timeFrameStart + " to " + timeFrameEnd + "). Claims were: " + result,
                   (timeFrameStart <= issuedAt) && (issuedAt <= timeFrameEnd));
        assertNotNull("JTI claim should not have been null but was. Claims were: " + result, result.getJwtId());
        Map<String, Object> eventsClaim = (Map<String, Object>) result.getClaimValue("events");
        assertNotNull("Events claim should not have been null but was. Claims were: " + result, eventsClaim);
        assertTrue("Events claim did not contain the " + LogoutTokenBuilder.EVENTS_MEMBER_NAME + " member. Claims were: " + result,
                   eventsClaim.containsKey(LogoutTokenBuilder.EVENTS_MEMBER_NAME));
        assertEquals("Events claim entry did not match expected value. Claims were: " + result, new HashMap<>(), eventsClaim.get(LogoutTokenBuilder.EVENTS_MEMBER_NAME));
        assertNull("A nonce claim was found but shouldn't have been. Claims were: " + result, result.getClaimValue("nonce"));

        if (expectedSubject == null) {
            assertNull("A sub claim was found but shouldn't have been: \"" + result + "\".", result.getSubject());
        } else {
            assertEquals("Sub claim did not match expected value. Claims were: " + result, expectedSubject, result.getSubject());
        }
        if (expectedSid == null) {
            assertNull("A sid claim was found but shouldn't have been: \"" + result + "\".", result.getStringClaimValue("sid"));
        } else {
            assertEquals("SID claim did not match expected value. Claims were: " + result, expectedSid, result.getStringClaimValue("sid"));
        }
    }

    private void setRegisteredClients(List<OidcBaseClient> registeredClients) throws OidcServerException {
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(registeredClients));
            }
        });
    }

    private void setTokenCacheExpectations(String user, OAuth20Token... tokens) {
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getTokenCache();
                will(returnValue(tokenCache));
                one(tokenCache).getAllUserTokens(subject);
                will(returnValue(Arrays.asList(tokens)));
            }
        });
    }

    private void setJwtCreationExpectations(OidcBaseClient client, String clientSecret) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(client).getClientSecret();
                will(returnValue(clientSecret));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
                one(oidcServerConfig).getJSONWebKey();
                will(returnValue(null));
                one(oidcServerConfig).getPrivateKey();
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
        return jwtUtils.getHS256Jws(getIdTokenClaims(subject, issuerIdentifier, client1Id), client1Secret);
    }

    private String getIdToken2String() throws Exception {
        return jwtUtils.getHS256Jws(getIdTokenClaims(subject, issuerIdentifier, client2Id), client2Secret);
    }

    private String getIdToken3String() throws Exception {
        return jwtUtils.getHS256Jws(getIdTokenClaims(subject, issuerIdentifier, client3Id), client3Secret);
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

}
