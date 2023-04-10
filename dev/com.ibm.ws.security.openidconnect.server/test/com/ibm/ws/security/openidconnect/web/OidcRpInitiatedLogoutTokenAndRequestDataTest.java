/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.security.test.common.jwt.utils.JwtUnitTestUtils;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import io.openliberty.security.common.jwt.exceptions.SignatureAlgorithmNotInAllowedList;
import test.common.SharedOutputManager;

public class OidcRpInitiatedLogoutTokenAndRequestDataTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String CWWKS1625E_OIDC_SERVER_IDTOKEN_VERIFY_ERR = "CWWKS1625E";
    private static final String CWWKS1646E_ID_TOKEN_ISSUER_NOT_THIS_OP = "CWWKS1646E";
    private static final String CWWKS2520E_SIG_ALG_IN_HEADER_NOT_ALLOWED = "CWWKS2520E";

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final OidcEndpointServices oidcEndpointServices = mockery.mock(OidcEndpointServices.class);
    private final OAuth20Provider oauth20Provider = mockery.mock(OAuth20Provider.class);
    private final OidcServerConfig oidcServerConfig = mockery.mock(OidcServerConfig.class);
    private final OAuth20EnhancedTokenCache tokenCache = mockery.mock(OAuth20EnhancedTokenCache.class);
    private final OidcOAuth20ClientProvider clientProvider = mockery.mock(OidcOAuth20ClientProvider.class);
    private final OidcBaseClient baseClient = mockery.mock(OidcBaseClient.class);

    private final String clientId = "my oauth client id";
    private final String clientSecret = "some super secret key value";
    private final String issuer = "https://localhost/contextPath/servlet/path";

    private OidcRpInitiatedLogoutTokenAndRequestData data;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        data = new OidcRpInitiatedLogoutTokenAndRequestData(request, oidcEndpointServices, oauth20Provider, oidcServerConfig);
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
    public void test_populate_missingPrincipal_missingAllRequestParameters() {
        mockery.checking(new Expectations() {
            {
                one(request).getUserPrincipal();
                will(returnValue(null));
                one(request).getParameter(OIDCConstants.OIDC_LOGOUT_ID_TOKEN_HINT);
                will(returnValue(null));
                one(request).getParameter(OIDCConstants.OIDC_LOGOUT_REDIRECT_URI);
                will(returnValue(null));
                one(request).getParameter(OIDCConstants.OIDC_LOGOUT_CLIENT_ID);
                will(returnValue(null));
            }
        });
        data.populate();

        assertTrue("Request missing all info should be considered valid for logout.", data.isDataValidForLogout());
    }

    @Test
    public void test_parseAndPopulateDataFromIdTokenHint_signingAlgNotSupported() throws Exception {
        OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
        data = new OidcRpInitiatedLogoutTokenAndRequestData(request, oidcEndpointServices, oauth20Provider, oidcServerConfig);

        JSONObject claims = new JSONObject();
        String idTokenHint = JwtUnitTestUtils.getHS256Jws(claims, clientSecret);
        initializeMemberVariables(data, idTokenHint);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("RS256"));
            }
        });
        data.parseAndPopulateDataFromIdTokenHint();
        assertFalse("Request should not be considered valid for logout.", data.isDataValidForLogout());
        verifyLogMessage(outputMgr, CWWKS1625E_OIDC_SERVER_IDTOKEN_VERIFY_ERR + ".*" + CWWKS2520E_SIG_ALG_IN_HEADER_NOT_ALLOWED);
    }

    @Test
    public void test_parseAndPopulateDataFromIdTokenHint_expiredToken_differentIssuer() throws Exception {
        OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
        data = new OidcRpInitiatedLogoutTokenAndRequestData(request, oidcEndpointServices, oauth20Provider, oidcServerConfig);

        JSONObject claims = new JSONObject();
        claims.put("aud", clientId);
        claims.put("exp", System.currentTimeMillis() - (24 * 60 * 60 * 1000));
        claims.put("iss", issuer + "/plus/something/else");
        String idTokenHint = JwtUnitTestUtils.getHS256Jws(claims, clientSecret);
        initializeMemberVariables(data, idTokenHint);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("HS256"));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
                one(oauth20Provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(baseClient));
                one(baseClient).getClientSecret();
                will(returnValue(clientSecret));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuer));
                one(oidcServerConfig).getProviderId();
                will(returnValue("myOidcProviderId"));
            }
        });
        data.parseAndPopulateDataFromIdTokenHint();
        assertFalse("Request should not be considered valid for logout.", data.isDataValidForLogout());
        verifyLogMessage(outputMgr, CWWKS1625E_OIDC_SERVER_IDTOKEN_VERIFY_ERR + ".*" + CWWKS1646E_ID_TOKEN_ISSUER_NOT_THIS_OP);
    }

    @Test
    public void test_parseAndPopulateDataFromIdTokenHint_goldenPath() throws Exception {
        OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
        data = new OidcRpInitiatedLogoutTokenAndRequestData(request, oidcEndpointServices, oauth20Provider, oidcServerConfig);

        JSONObject claims = new JSONObject();
        claims.put("aud", clientId);
        claims.put("exp", System.currentTimeMillis() + (24 * 60 * 60 * 1000));
        claims.put("iss", issuer);
        String idTokenHint = JwtUnitTestUtils.getHS256Jws(claims, clientSecret);
        initializeMemberVariables(data, idTokenHint);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("HS256"));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
                one(oauth20Provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(baseClient));
                one(baseClient).getClientSecret();
                will(returnValue(clientSecret));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuer));
            }
        });
        data.parseAndPopulateDataFromIdTokenHint();
        assertFalse("Request should be considered valid for logout.", data.isDataValidForLogout());
    }

    @Test
    public void test_parseAndValidateIdTokenHint_signingAlgNotSupported() throws Exception {
        OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
        data = new OidcRpInitiatedLogoutTokenAndRequestData(request, oidcEndpointServices, oauth20Provider, oidcServerConfig);

        JSONObject claims = new JSONObject();
        String idTokenHint = JwtUnitTestUtils.getHS256Jws(claims, clientSecret);
        initializeMemberVariables(data, idTokenHint);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("RS256"));
            }
        });
        try {
            data.parseAndValidateIdTokenHint();
            fail("Should have thrown an exception but didn't.");
        } catch (SignatureAlgorithmNotInAllowedList e) {
            // Expected
        }
    }

    @Test
    public void test_parseAndValidateIdTokenHint_missingClaims() throws Exception {
        OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
        data = new OidcRpInitiatedLogoutTokenAndRequestData(request, oidcEndpointServices, oauth20Provider, oidcServerConfig);

        JSONObject claims = new JSONObject();
        String idTokenHint = JwtUnitTestUtils.getHS256Jws(claims, clientSecret);
        initializeMemberVariables(data, idTokenHint);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("HS256"));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
            }
        });
        try {
            data.parseAndValidateIdTokenHint();
            fail("Should have thrown an exception but didn't.");
        } catch (IDTokenValidationFailedException e) {
            // Should fail to find the signing key because the "aud" claim is missing, hence the respective OAuth client can't be found
            verifyException(e, "InvalidKeyException");
        }
    }

    @Test
    public void test_parseAndValidateIdTokenHint_audClientDoesNotExist() throws Exception {
        OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
        data = new OidcRpInitiatedLogoutTokenAndRequestData(request, oidcEndpointServices, oauth20Provider, oidcServerConfig);

        JSONObject claims = new JSONObject();
        claims.put("aud", clientId);
        String idTokenHint = JwtUnitTestUtils.getHS256Jws(claims, clientSecret);
        initializeMemberVariables(data, idTokenHint);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("HS256"));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
                one(oauth20Provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(null));
            }
        });
        try {
            data.parseAndValidateIdTokenHint();
            fail("Should have thrown an exception but didn't.");
        } catch (IDTokenValidationFailedException e) {
            // Should fail to find the signing key because the "aud" claim is missing, hence the respective OAuth client can't be found
            verifyException(e, "InvalidKeyException");
        }
    }

    @Test
    public void test_parseAndValidateIdTokenHint_audClientSecretMismatch() throws Exception {
        OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
        data = new OidcRpInitiatedLogoutTokenAndRequestData(request, oidcEndpointServices, oauth20Provider, oidcServerConfig);

        JSONObject claims = new JSONObject();
        claims.put("aud", clientId);
        String idTokenHint = JwtUnitTestUtils.getHS256Jws(claims, clientSecret);
        initializeMemberVariables(data, idTokenHint);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("HS256"));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
                one(oauth20Provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(baseClient));
                one(baseClient).getClientSecret();
                will(returnValue(clientSecret + "other"));
            }
        });
        try {
            data.parseAndValidateIdTokenHint();
            fail("Should have thrown an exception but didn't.");
        } catch (IDTokenValidationFailedException e) {
            verifyException(e, "invalid signature");
        }
    }

    @Test
    public void test_parseAndValidateIdTokenHint_missingIssuer() throws Exception {
        OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
        data = new OidcRpInitiatedLogoutTokenAndRequestData(request, oidcEndpointServices, oauth20Provider, oidcServerConfig);

        JSONObject claims = new JSONObject();
        claims.put("aud", clientId);
        String idTokenHint = JwtUnitTestUtils.getHS256Jws(claims, clientSecret);
        initializeMemberVariables(data, idTokenHint);
        setRequestPathExpectations();

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("HS256"));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
                one(oauth20Provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(baseClient));
                one(baseClient).getClientSecret();
                will(returnValue(clientSecret));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(null));
                one(oidcServerConfig).getProviderId();
                will(returnValue("myOidcProviderId"));
            }
        });
        try {
            data.parseAndValidateIdTokenHint();
            fail("Should have thrown an exception but didn't.");
        } catch (IDTokenValidationFailedException e) {
            verifyException(e, CWWKS1646E_ID_TOKEN_ISSUER_NOT_THIS_OP);
        }
    }

    @Test
    public void test_parseAndValidateIdTokenHint_issuerMismatch() throws Exception {
        OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
        data = new OidcRpInitiatedLogoutTokenAndRequestData(request, oidcEndpointServices, oauth20Provider, oidcServerConfig);

        JSONObject claims = new JSONObject();
        claims.put("aud", clientId);
        claims.put("iss", issuer + "/with/some/extra");
        String idTokenHint = JwtUnitTestUtils.getHS256Jws(claims, clientSecret);
        initializeMemberVariables(data, idTokenHint);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("HS256"));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
                one(oauth20Provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(baseClient));
                one(baseClient).getClientSecret();
                will(returnValue(clientSecret));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuer));
                one(oidcServerConfig).getProviderId();
                will(returnValue("myOidcProviderId"));
            }
        });
        try {
            data.parseAndValidateIdTokenHint();
            fail("Should have thrown an exception but didn't.");
        } catch (IDTokenValidationFailedException e) {
            verifyException(e, CWWKS1646E_ID_TOKEN_ISSUER_NOT_THIS_OP);
        }
    }

    @Test
    public void test_parseAndValidateIdTokenHint_() throws Exception {
        OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
        data = new OidcRpInitiatedLogoutTokenAndRequestData(request, oidcEndpointServices, oauth20Provider, oidcServerConfig);

        JSONObject claims = new JSONObject();
        claims.put("aud", clientId);
        claims.put("iss", issuer);
        String idTokenHint = JwtUnitTestUtils.getHS256Jws(claims, clientSecret);
        initializeMemberVariables(data, idTokenHint);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                will(returnValue("HS256"));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
                one(oauth20Provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(baseClient));
                one(baseClient).getClientSecret();
                will(returnValue(clientSecret));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuer));
            }
        });
        data.parseAndValidateIdTokenHint();
        assertEquals("Populated subject value should have been null because the ID token hint was missing a sub claim.", null, data.getSubjectFromIdToken());
        assertEquals("Did not find the expected client ID in the populated request data.", clientId, data.getClientId());
    }

    private void initializeMemberVariables(OidcRpInitiatedLogoutTokenAndRequestData data, String idTokenHint) {
        mockery.checking(new Expectations() {
            {
                one(request).getUserPrincipal();
                will(returnValue(null));
                one(request).getParameter(OIDCConstants.OIDC_LOGOUT_ID_TOKEN_HINT);
                will(returnValue(idTokenHint));
                one(request).getParameter(OIDCConstants.OIDC_LOGOUT_REDIRECT_URI);
                will(returnValue(null));
                one(request).getParameter(OIDCConstants.OIDC_LOGOUT_CLIENT_ID);
                will(returnValue(null));
            }
        });
        data.initializeUserPrincipalData();
        data.initializeValuesFromRequestParameters();
    }

    private void setRequestPathExpectations() {
        mockery.checking(new Expectations() {
            {
                one(request).getScheme();
                will(returnValue("https"));
                one(request).getServerName();
                will(returnValue("localhost"));
                one(request).getServerPort();
                will(returnValue(9443));
                one(request).getContextPath();
                will(returnValue("/contextPath"));
                one(request).getServletPath();
                will(returnValue("/servlet/path"));
            }
        });
    }

}
