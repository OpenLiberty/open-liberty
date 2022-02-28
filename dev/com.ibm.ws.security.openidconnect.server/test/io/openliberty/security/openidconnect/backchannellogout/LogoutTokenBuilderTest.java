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
package io.openliberty.security.openidconnect.backchannellogout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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

import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import test.common.SharedOutputManager;

public class LogoutTokenBuilderTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("OpenIdConnect*=all");

    private final String issuerIdentifier = "https://localhost/oidc/endpoint/OP";
    private final String client1Id = "client1";
    private final String client2Id = "client2";
    private final String client3Id = "client3";
    private final String client1Secret = "client1secret";
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
    public void test_buildLogoutTokens_noRegisteredClients() throws Exception {
        List<OidcBaseClient> registeredClients = new ArrayList<>();

        JwtClaims idTokenClaims = null;
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(registeredClients));
            }
        });

        Map<String, String> result = builder.buildLogoutTokens(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty but wasn't: " + result, result.isEmpty());
    }

    @Test
    public void test_buildLogoutTokens_nullIdTokenClaims_oneClientRegistered() throws Exception {
        List<OidcBaseClient> registeredClients = new ArrayList<>();
        registeredClients.add(client1);

        JwtClaims idTokenClaims = null;
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(registeredClients));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                allowing(client1).getClientId();
                will(returnValue(client1Id));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        Map<String, String> result = builder.buildLogoutTokens(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Result did not have the expected number of entries: " + result, 1, result.size());
        assertTrue("Result did not contain an entry for the expected client \"" + client1Id + "\". Result was: " + result, result.containsKey(client1Id));

        String clientLogoutToken = result.get(client1Id);
        verifyLogoutToken(clientLogoutToken, Arrays.asList(client1Id), null, null);
    }

    @Test
    public void test_buildLogoutTokens_idTokenOneAudience_oneClientRegistered_doesNotMatch() throws Exception {
        List<OidcBaseClient> registeredClients = new ArrayList<>();
        registeredClients.add(client1);

        JwtClaims idTokenClaims = new JwtClaims();
        idTokenClaims.setAudience(client3Id);
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(registeredClients));
                one(client1).getClientId();
                will(returnValue(client1Id));
            }
        });

        Map<String, String> result = builder.buildLogoutTokens(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty but wasn't: " + result, result.isEmpty());
    }

    @Test
    public void test_buildLogoutTokens_idTokenOneAudience_oneClientRegistered_matches() throws Exception {
        List<OidcBaseClient> registeredClients = new ArrayList<>();
        registeredClients.add(client1);

        JwtClaims idTokenClaims = new JwtClaims();
        idTokenClaims.setAudience(client1Id);
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(registeredClients));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                allowing(client1).getClientId();
                will(returnValue(client1Id));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        Map<String, String> result = builder.buildLogoutTokens(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Result did not have the expected number of entries: " + result, 1, result.size());
        assertTrue("Result did not contain an entry for the expected client \"" + client1Id + "\". Result was: " + result, result.containsKey(client1Id));

        String clientLogoutToken = result.get(client1Id);
        verifyLogoutToken(clientLogoutToken, Arrays.asList(client1Id), null, null);
    }

    @Test
    public void test_buildLogoutTokens_idTokenMultipleAudiences_multipleClientsRegistered() throws Exception {
        List<OidcBaseClient> registeredClients = new ArrayList<>();
        registeredClients.add(client1);
        registeredClients.add(client2);
        registeredClients.add(client3);

        JwtClaims idTokenClaims = new JwtClaims();
        idTokenClaims.setAudience(client1Id, client3Id);
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(registeredClients));
                allowing(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                allowing(client1).getClientId();
                will(returnValue(client1Id));
                one(client2).getClientId();
                will(returnValue(client2Id));
                allowing(client3).getClientId();
                will(returnValue(client3Id));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);
        setJwtCreationExpectations(client3, client3Secret);

        Map<String, String> result = builder.buildLogoutTokens(idTokenClaims);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Result did not have the expected number of entries: " + result, 2, result.size());
        assertTrue("Result did not contain an entry for the expected client \"" + client1Id + "\". Result was: " + result, result.containsKey(client1Id));
        assertTrue("Result did not contain an entry for the expected client \"" + client3Id + "\". Result was: " + result, result.containsKey(client3Id));

        String client1LogoutToken = result.get(client1Id);
        verifyLogoutToken(client1LogoutToken, Arrays.asList(client1Id), null, null);
        String client3LogoutToken = result.get(client3Id);
        verifyLogoutToken(client3LogoutToken, Arrays.asList(client3Id), null, null);
    }

    @Test
    public void test_getClaimsFromIdTokenString_emptyClaims() throws Exception {
        String idTokenString = "eyJhbGciOiJub25lIn0.e30.";
        JwtClaims result = builder.getClaimsFromIdTokenString(idTokenString);
        assertNotNull("Returned claims object should not have been null but was.", result);
        Map<String, Object> claimsMap = result.getClaimsMap();
        assertTrue("Claims should have been empty but were: " + claimsMap, claimsMap.isEmpty());
    }

    @Test
    public void test_getClaimsFromIdTokenString_goldenPathClaims() throws Exception {
        JwtClaims input = new JwtClaims();
        input.setIssuer(issuerIdentifier);
        input.setAudience(client1Id);
        input.setIssuedAtToNow();
        input.setGeneratedJwtId();
        input.setSubject(subject);
        input.setExpirationTimeMinutesInTheFuture(60);
        input.setNotBeforeMinutesInThePast(10);

        String encodedClaims = new String(Base64.getEncoder().encode(input.toJson().getBytes()));
        String idTokenString = "eyJhbGciOiJub25lIn0." + encodedClaims + ".";

        JwtClaims result = builder.getClaimsFromIdTokenString(idTokenString);
        assertNotNull("Returned claims object should not have been null but was.", result);
        assertEquals("Returned claims object did not match the input.", input.getClaimsMap(), result.getClaimsMap());
    }

    //@Test
    public void test_getClientsToLogOut_() throws Exception {
        JwtClaims idTokenClaims = new JwtClaims();
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
            }
        });
        List<OidcBaseClient> result = builder.getClientsToLogOut(idTokenClaims);
    }

    // TODO - continue getClientsToLogOut

    @Test
    public void test_getClientsToConsiderLoggingOut_nullClaims_noConfiguredClients() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(new ArrayList<>()));
            }
        });
        JwtClaims claims = null;
        try {
            List<OidcBaseClient> clients = builder.getClientsToConsiderLoggingOut(claims);
            assertNotNull("List of clients should not have been null but was.", clients);
            assertTrue("List of clients should have been empty but wasn't: " + clients, clients.isEmpty());
        } catch (LogoutTokenBuilderException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_getClientsToConsiderLoggingOut_nullClaims_multipleClients() throws Exception {
        List<OidcBaseClient> registeredClients = new ArrayList<>();
        registeredClients.add(client1);
        registeredClients.add(client2);
        registeredClients.add(client3);
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(registeredClients));
            }
        });
        JwtClaims claims = null;
        try {
            List<OidcBaseClient> clients = builder.getClientsToConsiderLoggingOut(claims);
            assertNotNull("List of clients should not have been null but was.", clients);
            assertFalse("List of clients should not have been empty but was.", clients.isEmpty());
            for (OidcBaseClient expectedClient : registeredClients) {
                assertTrue("List of clients did not contain expected client [" + expectedClient + "]. Clients were: " + clients, clients.contains(expectedClient));
            }
        } catch (LogoutTokenBuilderException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_getClientsToConsiderLoggingOut_claimsMissingAud_multipleRegisteredClients() throws Exception {
        List<OidcBaseClient> registeredClients = new ArrayList<>();
        registeredClients.add(client1);
        registeredClients.add(client2);
        registeredClients.add(client3);
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(registeredClients));
                one(client1).getClientId();
                will(returnValue(client1Id));
                one(client2).getClientId();
                will(returnValue(client2Id));
                one(client3).getClientId();
                will(returnValue(client3Id));
            }
        });
        JwtClaims claims = new JwtClaims();
        claims.setSubject(subject);
        try {
            List<OidcBaseClient> clients = builder.getClientsToConsiderLoggingOut(claims);
            assertNotNull("List of clients should not have been null but was.", clients);
            assertTrue("List of clients should have been empty but wasn't: " + clients, clients.isEmpty());
        } catch (LogoutTokenBuilderException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_getClientsToConsiderLoggingOut_audNotOneOfRegisteredClients() throws Exception {
        List<OidcBaseClient> registeredClients = new ArrayList<>();
        registeredClients.add(client1);
        registeredClients.add(client2);
        registeredClients.add(client3);
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(registeredClients));
                one(client1).getClientId();
                will(returnValue(client1Id));
                one(client2).getClientId();
                will(returnValue(client2Id));
                one(client3).getClientId();
                will(returnValue(client3Id));
            }
        });
        JwtClaims claims = new JwtClaims();
        claims.setSubject(subject);
        claims.setAudience("client4");
        try {
            List<OidcBaseClient> clients = builder.getClientsToConsiderLoggingOut(claims);
            assertNotNull("List of clients should not have been null but was.", clients);
            assertTrue("List of clients should have been empty but wasn't: " + clients, clients.isEmpty());
        } catch (LogoutTokenBuilderException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_getClientsToConsiderLoggingOut_audMatchesOneRegisteredClient() throws Exception {
        List<OidcBaseClient> registeredClients = new ArrayList<>();
        registeredClients.add(client1);
        registeredClients.add(client2);
        registeredClients.add(client3);
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(registeredClients));
                one(client1).getClientId();
                will(returnValue(client1Id));
                one(client2).getClientId();
                will(returnValue(client2Id));
                one(client3).getClientId();
                will(returnValue(client3Id));
            }
        });
        JwtClaims claims = new JwtClaims();
        claims.setAudience(client1Id);
        try {
            List<OidcBaseClient> clients = builder.getClientsToConsiderLoggingOut(claims);
            assertNotNull("List of clients should not have been null but was.", clients);
            assertFalse("List of clients should not have been empty but was.", clients.isEmpty());
            assertEquals("Did not receive expected number of clients. Clients were: " + clients, 1, clients.size());
            assertTrue("List of clients did not contain expected client [" + client1 + "]. Clients were: " + clients, clients.contains(client1));
        } catch (LogoutTokenBuilderException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_getClientsToConsiderLoggingOut_audMatchesMultipleRegisteredClients() throws Exception {
        List<OidcBaseClient> registeredClients = new ArrayList<>();
        registeredClients.add(client1);
        registeredClients.add(client2);
        registeredClients.add(client3);
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(registeredClients));
                one(client1).getClientId();
                will(returnValue(client1Id));
                one(client2).getClientId();
                will(returnValue(client2Id));
                one(client3).getClientId();
                will(returnValue(client3Id));
            }
        });
        JwtClaims claims = new JwtClaims();
        claims.setAudience(client2Id, client3Id);
        try {
            List<OidcBaseClient> clients = builder.getClientsToConsiderLoggingOut(claims);
            assertNotNull("List of clients should not have been null but was.", clients);
            assertFalse("List of clients should not have been empty but was.", clients.isEmpty());
            assertEquals("Did not receive expected number of clients. Clients were: " + clients, 2, clients.size());
            assertTrue("List of clients did not contain expected client [" + client2 + "]. Clients were: " + clients, clients.contains(client2));
            assertTrue("List of clients did not contain expected client [" + client3 + "]. Clients were: " + clients, clients.contains(client3));
        } catch (LogoutTokenBuilderException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_createLogoutTokenForClient_nullIdTokenClaims() throws Exception {
        JwtClaims idTokenClaims = null;
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getClientId();
                will(returnValue(client1Id));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        String result = builder.createLogoutTokenForClient(client1, idTokenClaims);
        verifyLogoutToken(result, Arrays.asList(client1Id), null, null);
    }

    @Test
    public void test_createLogoutTokenForClient_idTokenContainsSub() throws Exception {
        JwtClaims idTokenClaims = new JwtClaims();
        idTokenClaims.setSubject(subject);
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getClientId();
                will(returnValue(client1Id));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        String result = builder.createLogoutTokenForClient(client1, idTokenClaims);
        verifyLogoutToken(result, Arrays.asList(client1Id), subject, null);
    }

    @Test
    public void test_createLogoutTokenForClient_idTokenContainsSid() throws Exception {
        JwtClaims idTokenClaims = new JwtClaims();
        idTokenClaims.setStringClaim("sid", sid);
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getClientId();
                will(returnValue(client1Id));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        String result = builder.createLogoutTokenForClient(client1, idTokenClaims);
        verifyLogoutToken(result, Arrays.asList(client1Id), null, sid);
    }

    @Test
    public void test_createLogoutTokenForClient_idTokenContainsSubAndSid() throws Exception {
        JwtClaims idTokenClaims = new JwtClaims();
        idTokenClaims.setSubject(subject);
        idTokenClaims.setStringClaim("sid", sid);
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getClientId();
                will(returnValue(client1Id));
            }
        });
        setJwtCreationExpectations(client1, client1Secret);

        String result = builder.createLogoutTokenForClient(client1, idTokenClaims);
        verifyLogoutToken(result, Arrays.asList(client1Id), subject, sid);
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_emptyIdTokenClaims() throws Exception {
        JwtClaims idTokenClaims = new JwtClaims();
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getClientId();
                will(returnValue(client1Id));
            }
        });
        // TODO - this should be an error scenario; that has yet to be fully implemented
        JwtClaims result = builder.populateLogoutTokenClaimsFromIdToken(client1, idTokenClaims);
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_subGoldenPath() throws Exception {
        JwtClaims idTokenClaims = new JwtClaims();
        idTokenClaims.setSubject(subject);
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getClientId();
                will(returnValue(client1Id));
            }
        });
        JwtClaims result = builder.populateLogoutTokenClaimsFromIdToken(client1, idTokenClaims);

        verifyLogoutTokenClaims(result, Arrays.asList(client1Id), subject, null);
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_subEmpty_noSid() throws Exception {
        JwtClaims idTokenClaims = new JwtClaims();
        idTokenClaims.setSubject("");
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getClientId();
                will(returnValue(client1Id));
            }
        });
        JwtClaims result = builder.populateLogoutTokenClaimsFromIdToken(client1, idTokenClaims);

        verifyLogoutTokenClaims(result, Arrays.asList(client1Id), "", null);
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_sidNotString() throws Exception {
        JwtClaims idTokenClaims = new JwtClaims();
        idTokenClaims.setClaim("sid", 123);
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getClientId();
                will(returnValue(client1Id));
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
    public void test_populateLogoutTokenClaimsFromIdToken_sidEmpty_noSub() throws Exception {
        JwtClaims idTokenClaims = new JwtClaims();
        idTokenClaims.setClaim("sid", "");
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getClientId();
                will(returnValue(client1Id));
            }
        });
        JwtClaims result = builder.populateLogoutTokenClaimsFromIdToken(client1, idTokenClaims);
        // TODO - this should be an error scenario; that has yet to be fully implemented
    }

    @Test
    public void test_populateLogoutTokenClaimsFromIdToken_subAndSidGoldenPath() throws Exception {
        JwtClaims idTokenClaims = new JwtClaims();
        idTokenClaims.setSubject(subject);
        idTokenClaims.setClaim("sid", sid);
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(client1).getClientId();
                will(returnValue(client1Id));
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
                one(client1).getClientId();
                will(returnValue(client1Id));
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

        // TODO sid claim should at least be there
        verifyLogoutTokenClaims(resultContext.getJwtClaims(), expectedAudiences, expectedSubject, expectedSid);
    }

    @SuppressWarnings("unchecked")
    private void verifyLogoutTokenClaims(JwtClaims result, List<String> expectedAudiences, String expectedSubject, String expectedSid) throws MalformedClaimException {
        assertNotNull("Result should not have been null but was.", result);

        long now = System.currentTimeMillis() / 1000;
        long timeFrameStart = now - 5;
        long timeFrameEnd = now + 5;

        assertEquals("Issuer did not match expected value.", issuerIdentifier, result.getIssuer());
        assertEquals("Audience did not match expected value.", expectedAudiences, result.getAudience());
        long issuedAt = result.getIssuedAt().getValue();
        assertTrue("Issued at time (" + issuedAt + ") is not in an expected reasonable time frame (" + timeFrameStart + " to " + timeFrameEnd + ").",
                   (timeFrameStart <= issuedAt) && (issuedAt <= timeFrameEnd));
        assertNotNull("JTI claim should not have been null but was.", result.getJwtId());
        Map<String, Object> eventsClaim = (Map<String, Object>) result.getClaimValue("events");
        assertNotNull("Events claim should not have been null but was.", eventsClaim);
        assertTrue("Events claim did not contain the " + LogoutTokenBuilder.EVENTS_MEMBER_NAME + " member.", eventsClaim.containsKey(LogoutTokenBuilder.EVENTS_MEMBER_NAME));
        assertEquals("Events claim entry did not match expected value.", new HashMap<>(), eventsClaim.get(LogoutTokenBuilder.EVENTS_MEMBER_NAME));
        assertNull("A nonce claim was found but shouldn't have been: \"" + result + "\".", result.getClaimValue("nonce"));

        if (expectedSubject == null) {
            assertNull("A sub claim was found but shouldn't have been: \"" + result + "\".", result.getSubject());
        } else {
            assertEquals("Sub claim did not match expected value.", expectedSubject, result.getSubject());
        }
        if (expectedSid == null) {
            assertNull("A sid claim was found but shouldn't have been: \"" + result + "\".", result.getStringClaimValue("sid"));
        } else {
            assertEquals("SID claim did not match expected value.", expectedSid, result.getStringClaimValue("sid"));
        }
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
}
