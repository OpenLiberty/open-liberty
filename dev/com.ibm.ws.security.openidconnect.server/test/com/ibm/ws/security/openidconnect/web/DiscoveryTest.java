/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.web;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.openidconnect.server.internal.HttpUtils;
import com.ibm.ws.security.openidconnect.server.plugins.OIDCWASDiscoveryModel;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import test.common.SharedOutputManager;

/**
 *
 */
public class DiscoveryTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;
    private final static Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final static String providerId = "TestProvider";

    private final static String[] supportedRespTypes = { "code", "id_token token", "token" };
    private final static String[] supportedScopes = { "openid", "profile", "email", "address", "phone", "general" };
    private final static String[] supportedClaims = { "sub", "name", "preferred_username", "profile", "picture", "email", "locale", "groupIds" };
    private final static String[] supportedRespModes = { "query", "fragment", "form_post" };
    private final static String[] supportedGrants = { "authorization_code", "implicit", "refresh_token", "client_credentials", "password",
                                                      "urn:ietf:params:oauth:grant-type:jwt-bearer" };
    private final static String[] supportedTokenEPMethods = { "client_secret_post", "client_secret_basic" };
    private final static String[] supportedDisplayVals = { "page" };
    private final static String[] supportedClaimTypes = { "normal" };
    private final static String[] supportedSubjectTypes = { "public" };
    private final static String[] supportedPkceCodeChallengeMethods = { "plain", "S256" };
    private final static String SIGNING_ALG_VALUE = "HS256";
    private final static String ISSUER_URI = "https://localhost:8020/oidc/endpoint/TestProvider";
    private final static String AUTHORIZE_URI = ISSUER_URI + "/authorize";
    private final static String TOKEN_URI = ISSUER_URI + "/token";
    private final static String JWKS_URI = ISSUER_URI + "/jwk";
    private final static String USER_INFO_URI = ISSUER_URI + "/userinfo";
    private final static String REGISTRATION_URI = ISSUER_URI + "/registration";
    private final static String SESSION_IFRAME_URI = ISSUER_URI + "/check_session_iframe";
    private final static String END_SESSION_URI = ISSUER_URI + "/end_session";
    private final static String INTROSPECTION_URI = ISSUER_URI + "/introspect";
    private final static String COVERAGE_MAP_URI = ISSUER_URI + "/coverage_map";
    private final static String PROXY_URI = ISSUER_URI + "/proxy";
    private final static String BACKING_IDP_PREFIX_URI = ISSUER_URI + "/idp";
    private final static boolean CLAIMS_PARAM = false;
    private final static boolean REQUEST_PARAM = false;
    private final static boolean REQUEST_URI = false;
    private final static boolean REQUIRE_REQUEST_URI_REGISTRATION = false;
    private final static String REVOCATION_URI = ISSUER_URI + "/revoke";
    private final static String APP_PASSWORDS_URI = ISSUER_URI + "/app-passwords";
    private final static String APP_TOKENS_URI = ISSUER_URI + "/app-tokens";
    private final static String CLIENT_MGMT_URI = ISSUER_URI + "/clientManagement";
    private final static String PERSONAL_TOKEN_MGMT_URI = ISSUER_URI + "/personalTokenManagement";
    private final static String USERS_TOKEN_MGMT_URI = ISSUER_URI + "/usersTokenManagement";
    private static String expectedDiscoveryModelJsonString;
    private static HttpServletRequest request;
    private static OidcServerConfig provider;

    @BeforeClass
    public static void traceSetUp() {
        outputMgr.trace("*=all");
        setupMocksAndExpectedModels();
    }

    @AfterClass
    public static void traceTearDown() {
        outputMgr.trace("*=all=disabled");
    }

    private static void setupMocksAndExpectedModels() {
        String methodName = "setupMocksAndExpectedModels";

        try {
            request = createMockRequest();
            provider = setUpProvider();
            expectedDiscoveryModelJsonString = getExpectedDiscoveryModelJsonString();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @Test
    public void testProcessRequest() {
        final String methodName = "testProcessRequest";
        try {

            final HttpServletResponse response = context.mock(HttpServletResponse.class);
            final StringWriter outputWriter = new StringWriter();
            final String cacheCtrHdr = "public, max-age=3600";
            context.checking(new Expectations() {
                {
                    one(response).getWriter();
                    will(returnValue(new PrintWriter(outputWriter)));
                    one(provider).getIssuerIdentifier();
                    will(returnValue(null));
                    one(response).setStatus(with(HttpServletResponse.SC_OK));
                    one(response).setContentType(with(HttpUtils.CT_APPLICATION_JSON));
                    one(response).setHeader(HttpUtils.CACHE_CONTROL, cacheCtrHdr);
                    one(response).flushBuffer();
                }
            });
            Discovery discovery = new Discovery();
            discovery.processRequest(provider, request, response);

            String responseVal = expectedDiscoveryModelJsonString;
            String servletOutput = outputWriter.toString();
            assertEquals("Response should have been written into the servlet", responseVal, servletOutput);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    @Test
    public void testDiscoveryModelAccesors() {
        final String methodName = "testDiscoveryModelAccesors";
        try {
            OIDCWASDiscoveryModel expectedDiscoveryModel = getExpectedDiscoveryModel();

            //Validate OIDC-WAS-Jazz property accessors (which aren't covered in normal flow due to json serialization to bypass getting)
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getIssuer(), ISSUER_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getAuthorizationEndpoint(), AUTHORIZE_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getTokenEndpoint(), TOKEN_URI);
            assertArrayEquals(expectedDiscoveryModel.getResponseTypesSupported(), supportedRespTypes);
            assertArrayEquals(expectedDiscoveryModel.getSubjectTypesSupported(), supportedSubjectTypes);
            assertArrayEquals(expectedDiscoveryModel.getIdTokenSigningAlgValuesSupported(), new String[] { SIGNING_ALG_VALUE });
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getUserinfoEndpoint(), USER_INFO_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getRegistrationEndpoint(), REGISTRATION_URI);
            assertArrayEquals(expectedDiscoveryModel.getScopesSupported(), supportedScopes);
            assertArrayEquals(expectedDiscoveryModel.getClaimsSupported(), supportedClaims);
            assertArrayEquals(expectedDiscoveryModel.getResponseModesSupported(), supportedRespModes);
            assertArrayEquals(expectedDiscoveryModel.getGrantTypesSupported(), supportedGrants);
            assertArrayEquals(expectedDiscoveryModel.getTokenEndpointAuthMethodsSupported(), supportedTokenEPMethods);
            assertArrayEquals(expectedDiscoveryModel.getDisplayValuesSupported(), supportedDisplayVals);
            assertArrayEquals(expectedDiscoveryModel.getClaimTypesSupported(), supportedClaimTypes);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.isClaimsParameterSupported(), CLAIMS_PARAM);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.isRequestParameterSupported(), REQUEST_PARAM);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.isRequestUriParameterSupported(), REQUEST_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.isRequireRequestUriRegistration(), REQUIRE_REQUEST_URI_REGISTRATION);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getCheckSessionIframe(), SESSION_IFRAME_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getEndSessionEndpoint(), END_SESSION_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getIntrospectionEndpoint(), INTROSPECTION_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getCoverageMapEndpoint(), COVERAGE_MAP_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getBackingIdpUriPrefix(), BACKING_IDP_PREFIX_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getProxyEndpoint(), PROXY_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getRevocationEndpoint(), REVOCATION_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getAppPasswordsEndpoint(), APP_PASSWORDS_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getAppTokensEndpoint(), APP_TOKENS_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getClientMgmtEndpoint(), CLIENT_MGMT_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getPersonalTokenMgmtEndpoint(), PERSONAL_TOKEN_MGMT_URI);
            assertEquals("Discovery model property should have matched.", expectedDiscoveryModel.getUsersTokenMgmtEndpoint(), USERS_TOKEN_MGMT_URI);
            assertArrayEquals(expectedDiscoveryModel.getPkceCodeChallengeMethodsSupported(), supportedPkceCodeChallengeMethods);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * @return a mock HttpServletRequest
     * @throws Exception
     */
    private static HttpServletRequest createMockRequest() throws Exception {
        final HttpServletRequest request = context.mock(HttpServletRequest.class);
        context.checking(new Expectations() {
            {
                allowing(request).getScheme();
                will(returnValue("https"));
                allowing(request).getServerName();
                will(returnValue("localhost"));
                allowing(request).getServerPort();
                will(returnValue(8020));
                allowing(request).getContextPath();
                will(returnValue("/oidc"));
                allowing(request).getServletPath();
                will(returnValue("/endpoint"));

            }
        });
        return request;
    }

    /**
     * @return a mock provider
     * @throws Exception
     */
    private static OidcServerConfig setUpProvider() throws Exception {
        //Setup the provider
        final OidcServerConfig provider = context.mock(OidcServerConfig.class);
        context.checking(new Expectations() {
            {
                allowing(provider).getProviderId();
                will(returnValue(providerId));
                one(provider).getResponseTypesSupported();
                will(returnValue(supportedRespTypes));
                one(provider).getSubjectTypesSupported();
                will(returnValue(supportedSubjectTypes));
                one(provider).getIdTokenSigningAlgValuesSupported();
                will(returnValue(SIGNING_ALG_VALUE));
                one(provider).getScopesSupported();
                will(returnValue(supportedScopes));
                one(provider).getClaimsSupported();
                will(returnValue(supportedClaims));
                one(provider).getResponseModesSupported();
                will(returnValue(supportedRespModes));
                one(provider).getGrantTypesSupported();
                will(returnValue(supportedGrants));
                one(provider).getTokenEndpointAuthMethodsSupported();
                will(returnValue(supportedTokenEPMethods));
                one(provider).getDisplayValuesSupported();
                will(returnValue(supportedDisplayVals));
                one(provider).getClaimTypesSupported();
                will(returnValue(supportedClaimTypes));
                one(provider).isClaimsParameterSupported();
                will(returnValue(CLAIMS_PARAM));
                one(provider).isRequestParameterSupported();
                will(returnValue(REQUEST_PARAM));
                one(provider).isRequestUriParameterSupported();
                will(returnValue(REQUEST_PARAM));
                one(provider).isRequireRequestUriRegistration();
                will(returnValue(REQUIRE_REQUEST_URI_REGISTRATION));
                one(provider).getBackingIdpUriPrefix();
                will(returnValue(BACKING_IDP_PREFIX_URI));
                one(provider).getAuthProxyEndpointUrl();
                will(returnValue(PROXY_URI));
            }
        });

        return provider;
    }

    private static String getExpectedDiscoveryModelJsonString() {

        return JsonTokenUtil.toJsonFromObj(getExpectedDiscoveryModel());

    }

    private static OIDCWASDiscoveryModel getExpectedDiscoveryModel() {
        OIDCWASDiscoveryModel model = new OIDCWASDiscoveryModel();
        model.setIssuer(ISSUER_URI);
        model.setAuthorizationEndpoint(AUTHORIZE_URI);
        model.setTokenEndpoint(TOKEN_URI);
        model.setJwks_uri(JWKS_URI);
        model.setResponseTypesSupported(supportedRespTypes);
        model.setSubjectTypesSupported(supportedSubjectTypes);
        model.setIdTokenSigningAlgValuesSupported(new String[] { SIGNING_ALG_VALUE });
        model.setUserinfoEndpoint(USER_INFO_URI);
        model.setRegistrationEndpoint(REGISTRATION_URI);
        model.setScopesSupported(supportedScopes);
        model.setClaimsSupported(supportedClaims);
        model.setResponseModesSupported(supportedRespModes);
        model.setGrantTypesSupported(supportedGrants);
        model.setTokenEndpointAuthMethodsSupported(supportedTokenEPMethods);
        model.setDisplayValuesSupported(supportedDisplayVals);
        model.setClaimTypesSupported(supportedClaimTypes);
        model.setClaimsParameterSupported(CLAIMS_PARAM);
        model.setRequestParameterSupported(REQUEST_PARAM);
        model.setRequestUriParameterSupported(REQUEST_URI);
        model.setRequireRequestUriRegistration(REQUIRE_REQUEST_URI_REGISTRATION);
        model.setCheckSessionIframe(SESSION_IFRAME_URI);
        model.setEndSessionEndpoint(END_SESSION_URI);
        model.setIntrospectionEndpoint(INTROSPECTION_URI);
        model.setCoverageMapEndpoint(COVERAGE_MAP_URI);
        model.setBackingIdpUriPrefix(BACKING_IDP_PREFIX_URI);
        model.setProxyEndpoint(PROXY_URI);
        model.setRevocationEndpoint(REVOCATION_URI);
        model.setAppPasswordsEndpoint(APP_PASSWORDS_URI);
        model.setAppTokensEndpoint(APP_TOKENS_URI);
        model.setClientMgmtEndpoint(CLIENT_MGMT_URI);
        model.setPersonalTokenMgmtEndpoint(PERSONAL_TOKEN_MGMT_URI);
        model.setUsersTokenMgmtEndpoint(USERS_TOKEN_MGMT_URI);
        model.setPkceCodeChallengeMethodsSupported(supportedPkceCodeChallengeMethods);

        return model;
    }
}
