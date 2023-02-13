/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.websphere.ssl.SSLConfigChangeListener;
import com.ibm.websphere.ssl.SSLConfigurationNotAvailableException;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.security.common.structures.SingleTableCache;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.MockOidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.openidconnect.token.JWSHeader;
import com.ibm.ws.security.openidconnect.token.JWT;
import com.ibm.ws.security.openidconnect.token.JWTPayload;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.ssl.JSSEProviderFactory;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.wsspi.ssl.SSLSupport;

import test.common.SharedOutputManager;

public class AccessTokenAuthenticatorTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @Rule
    public TestName name = new TestName();

    private final SRTServletRequest req = mockery.mock(SRTServletRequest.class, "req");
    private final HttpServletResponse res = mockery.mock(HttpServletResponse.class, "res");
    private final OidcClientConfig clientConfig = mockery.mock(OidcClientConfig.class, "clientConfig");
    private final OidcClientRequest clientRequest = mockery.mock(OidcClientRequest.class, "clientRequest");
    private final WebAppSecurityConfig webAppSecConfig = mockery.mock(WebAppSecurityConfig.class, "webAppSecConfig");
    private final SSLSupport sslSupport = mockery.mock(SSLSupport.class, "sslSupport");
    private final HttpResponse httpResponse = mockery.mock(HttpResponse.class, "httpResponse");
    private final StatusLine statusLine = mockery.mock(StatusLine.class, "statusLine");
    private final HttpEntity httpEntity = mockery.mock(HttpEntity.class);
    private final Header header = mockery.mock(Header.class, "header");
    private final Key decryptionKey = mockery.mock(Key.class);
    protected final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class, "sslSocketFactory");

    private static final AccessTokenAuthenticatorTest authenticatorTest = new AccessTokenAuthenticatorTest();
    private static final JSSEHelper jssHelper = authenticatorTest.new FakeJSSEHelper();
    private static final String Authorization_Header = "Authorization";
    private static final String JWT_SEGMENTS = "-segments";
    private static final String JWT_SEGMENT_INDEX = "-";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String HTTPS_URL = "https://localhost:8020/root";
    private static final String BEARER = "Bearer " + ACCESS_TOKEN;
    private static final String GOOD_USER = "user";
    private static final String GOOD_ISSUER = "good_issuer";
    private static final String BAD_ISSUER = "bad_issuer";
    private static final String SUPPORTED = "supported";
    private static final String REQUIRED = "required";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_JWT = "application/jwt";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String CONFIG_ID = "configId";

    private final AccessTokenAuthenticator accessTokenAuthenticator = new AccessTokenAuthenticator();
    private final AccessTokenAuthenticator sslTokenAuth = new FakeAccessTokenAuthenticator(sslSupport);
    private final ReferrerURLCookieHandler referrerURLCookieHandler = new ReferrerURLCookieHandler(webAppSecConfig);
    private final Map<String, Object> respMap = new HashMap<String, Object>();
    private final AccessTokenCacheHelper cacheHelper = new AccessTokenCacheHelper();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.trace("*=all");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.trace("*=all=disabled");
        outputMgr.restoreStreams();
    }

    @Before
    public void setUp() throws Exception {
        respMap.clear();

        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getUserInfoEndpointUrl();
                will(returnValue(null));
                allowing(clientConfig).getUseSystemPropertiesForHttpClientConnections();
                will(returnValue(false));
                allowing(clientConfig).getClientId();
                will(returnValue(null));
                allowing(clientConfig).getSSLConfigurationName();
                will(returnValue(null));
                allowing(clientConfig).getValidationEndpointUrl();
                will(returnValue(HTTPS_URL));
                allowing(clientConfig).isMapIdentityToRegistryUser();
                will(returnValue(true));
                allowing(clientConfig).isIncludeCustomCacheKeyInSubject();
                will(returnValue(true));
                allowing(clientConfig).getClockSkewInSeconds();
                will(returnValue((long) (60 * 5)));
                allowing(clientConfig).getTokenEndpointAuthMethod();
                will(returnValue(ClientConstants.METHOD_BASIC));
                allowing(webAppSecConfig).createSSOCookieHelper();
                will(returnValue(new SSOCookieHelperImpl(webAppSecConfig)));
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));
                allowing(sslSupport).getJSSEHelper();
                will(returnValue(jssHelper));
                allowing(sslSupport).getSSLSocketFactory((String) null);
                will(returnValue(sslSocketFactory));

            }
        });

        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
        System.out.println("*** RUNNING: " + name.getMethodName());
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @Test
    public void testConstructor() {
        try {
            AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator();
            assertNotNull("There must be a AccessTokenAuthenticator", authenticator);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    @Test
    public void testExtractErrorDescription_negativeCases() {
        AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator();
        assertNull(authenticator.extractErrorDescription(null));
    }

    @Test
    public void testExtractErrorDescription_positiveCases() {
        AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator();
        // Format isn't clear; return the argument as-is
        assertEquals("", authenticator.extractErrorDescription(""));
        assertEquals(" ", authenticator.extractErrorDescription(" "));
        assertEquals("test", authenticator.extractErrorDescription("test"));
        assertEquals("error_=value", authenticator.extractErrorDescription("error_=value"));
        assertEquals("error_descriptio=test", authenticator.extractErrorDescription("error_descriptio=test"));
        assertEquals(Constants.ERROR_DESCRIPTION, authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION));
        assertEquals(Constants.ERROR_DESCRIPTION + "\"=", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "\"="));
        assertEquals(Constants.ERROR_DESCRIPTION + ":", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + ":"));
        assertEquals(Constants.ERROR_DESCRIPTION + ":{}", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + ":{}"));
        assertEquals(Constants.ERROR_DESCRIPTION + ":\"value\"", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + ":\"value\""));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + "\"=", authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + "\"="));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + "\":", authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + "\":"));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + "\"=\"", authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + "\"=\""));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + "\":\"", authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + "\":\""));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + "\"={}", authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + "\"={}"));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + "\":{}", authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + "\":{}"));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + "\"=value", authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + "\"=value"));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + "\":value", authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + "\":value"));
        for (char c = '0'; c <= '9'; c++) {
            singleCharacterTests(authenticator, c);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            singleCharacterTests(authenticator, c);
        }
        for (char c = 'a'; c <= 'z'; c++) {
            singleCharacterTests(authenticator, c);
        }

        // Found an error description formatted as expected; return the description value
        assertEquals("", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "="));
        assertEquals("", authenticator.extractErrorDescription(" " + Constants.ERROR_DESCRIPTION + "="));
        assertEquals("", authenticator.extractErrorDescription("other " + Constants.ERROR_DESCRIPTION + "="));
        assertEquals("myValue", authenticator.extractErrorDescription("values," + Constants.ERROR_DESCRIPTION + "=myValue"));
        assertEquals("", authenticator.extractErrorDescription("error=test," + Constants.ERROR_DESCRIPTION + "="));
        assertEquals("\"", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "=\""));
        assertEquals(" ", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "= "));
        assertEquals(" white space ", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "= white space "));
        assertEquals("", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "=\"\""));
        assertEquals("test", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "=test"));
        assertEquals("\"test value", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "=\"test value"));
        assertEquals("test\"", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "=test\""));
        assertEquals("test", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "=\"test\""));
        assertEquals("test \"this\" thing", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "=\"test \"this\" thing\""));

        // TODO would like to improve
        assertEquals("test this thing,other=test2", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "=test this thing,other=test2"));
        assertEquals("\"test value\",other=test2", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "=\"test value\",other=test2"));
        assertEquals("test this \"thing\",other=\"test2\"", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + "=test this \"thing\",other=\"test2\""));
        assertEquals(",param=value", authenticator.extractErrorDescription("error=test," + Constants.ERROR_DESCRIPTION + "=,param=value"));
    }

    @Test
    public void testAuthenticate_UserinfoValidation_GoodOidcResult() throws javax.net.ssl.SSLException {
        withoutAccessTokenInLtpaCookie();
        withDefaultBearerAuthorizationHeaderExpectations();
        withAccessTokenCacheExpectations(false, false);
        withUserInfoExpectations(true, false);
        withOkAndEntity(createJsonBasicHttpEntity(true));
        withUserIdentifier(GOOD_USER);
        withPropagationTokenAuthenticatedAttribute();

        ((FakeAccessTokenAuthenticator) sslTokenAuth).fixSubjectCalled = false;
        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertSuccessWithOK(oidcResult);

        // verify that subject fixup was called
        assertTrue("fixSubject was not called as expected ", ((FakeAccessTokenAuthenticator) sslTokenAuth).fixSubjectCalled);
    }

    @Test
    public void testAuthenticate_UserinfoValidation_BadUserIdentifier() {
        final String BAD_USER = "bad_user";
        withoutAccessTokenInLtpaCookie();
        withDefaultBearerAuthorizationHeaderExpectations();
        withAccessTokenCacheExpectations(false, false);
        withUserInfoExpectations(true, true);
        withOkAndEntity(createJsonBasicHttpEntity(true));
        withUserIdentifier(BAD_USER);

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assert401WithUnauthorized(oidcResult);
    }

    @Test
    public void testAuthenticate_UserinfoValidation_BadJSONObject() {
        withoutAccessTokenInLtpaCookie();
        withDefaultBearerAuthorizationHeaderExpectations();
        withAccessTokenCacheExpectations(false, false);
        withUserInfoExpectations(false, false);
        withInboundPropagationSupported();

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertFailureWithUnauthorized(oidcResult);
    }

    @Test
    public void testAuthenticate_IntrospectTokenValidation_disableIssChecking() {
        withoutAccessTokenInLtpaCookie();
        withDefaultBearerAuthorizationHeaderExpectations();
        withAccessTokenCacheExpectations(false, false);
        withIntrospectionExpectations(true, false, "client_secret");
        withOkAndEntity(createJsonBasicHttpEntity(true, GOOD_ISSUER));
        withoutIssuerChecking();
        withInboundPropagationRequired();

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertFailureWithUnauthorized(oidcResult);
        assertTrue("Expected message was not logged", outputMgr.checkForMessages("CWWKS1735E:"));
    }

    @Test
    public void testAuthenticate_IntrospectTokenValidation_disableIssChecking_no_issclaim() {
        withoutAccessTokenInLtpaCookie();
        withDefaultBearerAuthorizationHeaderExpectations();
        withAccessTokenCacheExpectations(false, false);
        withIntrospectionExpectations(true, false, "client_secret");
        withOkAndEntity(createJsonBasicHttpEntity(true));
        withUserIdentifier(GOOD_USER);
        withoutIssuerChecking();
        withPropagationTokenAuthenticatedAttribute();

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertSuccessWithOK(oidcResult);
    }

    @Test
    public void testAuthenticate_IntrospectTokenValidation_GoodOidcResult() {
        withoutAccessTokenInLtpaCookie();
        withDefaultBearerAuthorizationHeaderExpectations();
        SingleTableCache cache = withAccessTokenCacheExpectations(false, true);
        withIntrospectionExpectations(true, false, "client_secret");
        withOkAndEntity(createJsonBasicHttpEntity(true, GOOD_ISSUER));
        withUserIdentifier(GOOD_USER);
        withIssuerChecking(GOOD_ISSUER);
        withPropagationTokenAuthenticatedAttribute();

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertSuccessWithOK(oidcResult);

        // Verify that the result was cached
        AccessTokenCacheKey cacheKey = cacheHelper.getCacheKey(ACCESS_TOKEN, CONFIG_ID);
        AccessTokenCacheValue cacheEntry = (AccessTokenCacheValue) cache.get(cacheKey);
        assertNotNull("Cached authentication result should not have been null but was.", cacheEntry);
        assertEquals("Cached result did not match the result originally returned from the authenticate method.", oidcResult, cacheEntry.getResult());
    }

    @Test
    public void testAuthenticate_IntrospectTokenValidation_TokenNotActive() {
        withoutAccessTokenInLtpaCookie();
        withDefaultBearerAuthorizationHeaderExpectations();
        withAccessTokenCacheExpectations(false, false);
        withIntrospectionExpectations(true, true, null);
        withOkAndEntity(createJsonBasicHttpEntity(false, GOOD_ISSUER));
        withInboundPropagationSupported();

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertFailureWithUnauthorized(oidcResult);
    }

    @Test
    public void testAuthenticate_IntrospectTokenValidation_BadJSONObject() {
        withoutAccessTokenInLtpaCookie();
        withDefaultBearerAuthorizationHeaderExpectations();
        withAccessTokenCacheExpectations(true, false);
        withIntrospectionExpectations(false, false, null);
        withInboundPropagationRequired();

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertFailureWithUnauthorized(oidcResult);
    }

    @Test
    public void testAuthenticate_InvalidValidationMethod() throws javax.net.ssl.SSLException {
        withoutAccessTokenInLtpaCookie();
        withDefaultBearerAuthorizationHeaderExpectations();
        withAccessTokenCacheExpectations(false, false);
        withValidationMethod("invalid_validation");
        withHttpsRequired(false);

        mockery.checking(new Expectations() {
            {
                one(clientConfig).getTokenEndpointUrl();
                will(returnValue(HTTPS_URL));
            }
        });

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertFailureWithUnauthorized(oidcResult);
    }

    @Test
    public void testAuthenticate_NullAccessToken() {
        withoutAccessTokenInLtpaCookie();
        withHeaderName(null);
        withAuthorizationHeader(null);
        withAccessTokenInRequestParameter(null);
        withInboundPropagationSupported();

        ProviderAuthenticationResult oidcResult = accessTokenAuthenticator.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertFailureWithUnauthorized(oidcResult);
    }

    @Test
    public void testAuthenticate_ThrowsSSLException() {
        withoutAccessTokenInLtpaCookie();
        withHeaderName(null);
        withAuthorizationHeader(null);
        withAccessTokenCacheExpectations(false, false);
        withIntrospection();
        withAccessTokenInRequestParameter(ACCESS_TOKEN);
        withInboundPropagationRequired();

        ProviderAuthenticationResult oidcResult = accessTokenAuthenticator.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assert401WithUnauthorized(oidcResult);
    }

    @Test
    public void testAuthenticate_resultAlreadyCached() {
        withoutAccessTokenInLtpaCookie();
        withDefaultBearerAuthorizationHeaderExpectations();
        SingleTableCache cache = withAccessTokenCacheExpectations(true, true);
        ProviderAuthenticationResult cachedResult = createProviderAuthenticationResult(System.currentTimeMillis());
        AccessTokenCacheValue cacheEntry = new AccessTokenCacheValue("unique id", cachedResult);
        AccessTokenCacheKey cacheKey = cacheHelper.getCacheKey(ACCESS_TOKEN, CONFIG_ID);
        cache.put(cacheKey, cacheEntry);
        withPropagationTokenAuthenticatedAttribute();

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertSuccessWithOK(oidcResult);
    }

    @Test
    public void testGetBearerAccessTokenToken_JwtSegments() throws Exception {
        final String expectedAccessToken = "header.payload.signature";
        final String accessTokenSegment1 = "Bearer " + "header.pay";
        final String accessTokenSegment2 = "load.signature";

        withHeaderName(Authorization_Header);
        withAuthorizationHeader(null);
        withHeader(Authorization_Header + JWT_SEGMENTS, "2");
        withHeader(Authorization_Header + JWT_SEGMENT_INDEX + "1", accessTokenSegment1);
        withHeader(Authorization_Header + JWT_SEGMENT_INDEX + "2", accessTokenSegment2);

        String accessTokenFromHeader = AccessTokenAuthenticator.getBearerAccessTokenToken(req, clientConfig);

        assertEquals("The Bearer access token must be built from all the JWT segments.", expectedAccessToken, accessTokenFromHeader);
    }

    @Test
    public void testValidateJsonResponse_TimeIsExpired() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);

        Long minusDate = cal.getTimeInMillis() / 1000;
        final String JSONSTRING = getJSONObjectString(true, minusDate);
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        withInboundPropagationSupported();

        Boolean isValid = accessTokenAuthenticator.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_noExpirationTime() throws IOException {
        final String JSONSTRING = "{\"user\":\"user1\" , \"active\":true}";
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        withExpClaimRequired(true);
        withInboundPropagationRequired();

        Boolean isValid = accessTokenAuthenticator.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_noExpirationTime_doNotRequireExpClaim() throws IOException {
        final String JSONSTRING = "{\"user\":\"user1\" , \"active\":true}";
        JSONObject jobj = JSONObject.parse(JSONSTRING);
        Long currentDate = getCurrentDate();
        jobj.put("iat", currentDate);
        jobj.put("iss", GOOD_ISSUER);
        withIssuerChecking(GOOD_ISSUER);
        withExpClaimRequired(false);

        Boolean isValid = accessTokenAuthenticator.validateJsonResponse(jobj, clientConfig);
        assertTrue("Response should have been considered valid, but was not.", isValid);
    }

    @Test
    public void testValidateJsonResponse_BadIssueAtTime() throws IOException {
        Calendar cal = Calendar.getInstance();
        Long expTime = cal.getTimeInMillis() / 1000;

        cal.add(Calendar.YEAR, 1);
        Long issueatTime = cal.getTimeInMillis() / 1000;

        final String JSONSTRING = getJSONObjectString(true, expTime, issueatTime);
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        withInboundPropagationSupported();

        Boolean isValid = accessTokenAuthenticator.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_noIssueAtTime() throws IOException {
        Long expTime = getCurrentDate();

        final String JSONSTRING = getJSONObjectString(true, expTime);
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        withIatClaimRequired(true);
        withInboundPropagationRequired();

        Boolean isValid = accessTokenAuthenticator.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_noIssueAtTime_doNotRequireIatClaim() throws IOException {
        Long expTime = getCurrentDate();

        final String JSONSTRING = getJSONObjectString(true, expTime);
        JSONObject jobj = JSONObject.parse(JSONSTRING);
        jobj.put("iss", GOOD_ISSUER);
        withIssuerChecking(GOOD_ISSUER);
        withIatClaimRequired(false);

        Boolean isValid = accessTokenAuthenticator.validateJsonResponse(jobj, clientConfig);
        assertTrue("Response should have been considered valid, but was not.", isValid);
    }

    @Test
    public void testValidateJsonResponse_IncompatibleIssuer() throws IOException {
        withIssuerChecking(GOOD_ISSUER);
        withInboundPropagationSupported();

        Long currentDate = getCurrentDate();

        final String JSONSTRING = getJSONObjectString(true, currentDate, currentDate, BAD_ISSUER);
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        Boolean isValid = accessTokenAuthenticator.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_NotBeforeTime_Good() throws IOException {
        withIssuerChecking(GOOD_ISSUER);

        Long currentDate = getCurrentDate();

        final String JSONSTRING = getJSONObjectString(true, currentDate, currentDate, GOOD_ISSUER, currentDate);
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        Boolean isValid = accessTokenAuthenticator.validateJsonResponse(jobj, clientConfig);
        assertTrue("Expected to receive a true value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_NotBeforeTime_Bad() throws IOException {
        withIssuerChecking(GOOD_ISSUER);
        withInboundPropagationRequired();

        Calendar cal = Calendar.getInstance();
        Long currentDate = cal.getTimeInMillis() / 1000;

        cal.add(Calendar.YEAR, 1);
        Long notBeforeTime = cal.getTimeInMillis() / 1000;

        final String JSONSTRING = getJSONObjectString(true, currentDate, currentDate, GOOD_ISSUER, notBeforeTime);
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        Boolean isValid = accessTokenAuthenticator.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testGetLong() {
        Long currentDate = getCurrentDate();
        Integer intCurrentDate = Integer.valueOf(currentDate.toString());

        Long result1 = accessTokenAuthenticator.getLong(intCurrentDate);
        assertEquals("Expected to receive: " + currentDate + " but received: " + result1 + ".", currentDate, result1);

        String[] dates = { currentDate.toString(), Calendar.getInstance().toString() };
        Long result2 = accessTokenAuthenticator.getLong(dates);
        assertEquals("Expected to receive: " + currentDate + " but received: " + result2 + ".", currentDate, result2);
    }

    @Test
    public void testHandleResponseMap() throws Exception {
        final String failMsg = "CWWKS1720E: The resource server failed the authentication request because the access token which is in the request is not active. The validation method is [null], and the validation endpoint url is ["
                + HTTPS_URL + "].";

        withValidationMethod(null);
        withForbiddenAndEntity(createBasicHttpEntity(new String("")));
        withWWWAuthenticateHeader("error.error_description=invalid_token");
        withInboundPropagationSupported();
        withNewRsFailMsg(failMsg);
        withRsFailMsg(failMsg);

        respMap.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);
        JSONObject jsonObject = accessTokenAuthenticator.handleResponseMap(respMap, clientConfig, clientRequest);
        assertNull("Expected to receive a null value but was received: " + jsonObject, jsonObject);
    }

    @Test
    public void testHandleResponseMap_NullHeader() throws Exception {
        final String failMsg = "CWWKS1721E: The resource server received an error [] while it was attempting to validate the access token. It is either expired or cannot be recognized by the validation end point ["
                + HTTPS_URL + "].";

        withForbiddenAndEntity(createBasicHttpEntity(new String("")));
        withWWWAuthenticateHeader(null);
        withInboundPropagationRequired();
        withNewRsFailMsg(failMsg);

        respMap.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);
        JSONObject jsonObject = accessTokenAuthenticator.handleResponseMap(respMap, clientConfig, clientRequest);
        assertNull("Expected to receive a null value but was received: " + jsonObject, jsonObject);
    }

    @Test
    public void test_extractSuccessfulResponse_responseMissingEntity() throws Exception {
        withEntity(null);
        JSONObject result = accessTokenAuthenticator.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractSuccessfulResponse_emptyString() throws Exception {
        withEntity(createBasicHttpEntity(new String(""), APPLICATION_JSON));
        withInboundPropagationRequired();
        withRsFailMsg("doesn't matter");

        JSONObject result = accessTokenAuthenticator.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractSuccessfulResponse_missingContentType() throws Exception {
        String inputString = "This is not JSON";
        final InputStream input = new ByteArrayInputStream(inputString.getBytes());
        withEntity(httpEntity);
        withInboundPropagationRequired();
        withRsFailMsg("doesn't matter");
        mockery.checking(new Expectations() {
            {
                one(httpEntity).getContent();
                will(returnValue(input));
                allowing(httpEntity).getContentLength();
                will(returnValue((long) inputString.length()));
                allowing(httpEntity).getContentType();
                will(returnValue(null));
            }
        });
        JSONObject result = accessTokenAuthenticator.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractSuccessfulResponse_notJson() throws Exception {
        withEntity(createBasicHttpEntity(new String("This is not JSON"), TEXT_PLAIN));
        JSONObject result = accessTokenAuthenticator.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractSuccessfulResponse_emptyJson() throws Exception {
        JSONObject responseJson = new JSONObject();
        withEntity(createBasicHttpEntity(new String(responseJson.toString()), APPLICATION_JSON));
        JSONObject result = accessTokenAuthenticator.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty, but was " + result + ".", result.isEmpty());
    }

    @Test
    public void test_extractSuccessfulResponse_validNonEmptyJson() throws Exception {
        JSONObject responseJson = new JSONObject();
        responseJson.put("key1", "value1");
        responseJson.put("key2", "value2");
        withEntity(createBasicHttpEntity(new String(responseJson.toString()), APPLICATION_JSON));
        JSONObject result = accessTokenAuthenticator.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Result did not match the expected value.", responseJson, result);
    }

    @Test
    public void test_extractSuccessfulResponse_validNonEmptyJson_contentTypeJwt() throws Exception {
        JSONObject responseJson = new JSONObject();
        responseJson.put("key1", "value1");
        responseJson.put("key2", "value2");
        withEntity(createBasicHttpEntity(new String(responseJson.toString()), APPLICATION_JWT));
        withConfigId();

        try {
            JSONObject result = accessTokenAuthenticator.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
            fail("Should have thrown an exception, but got [" + result + "].");
        } catch (Exception e) {
            verifyException(e, "CWWKS1539E");
        }
    }

    @Test
    public void test_extractClaimsFromJwtResponse_responseStringEmpty() throws Exception {
        String rawResponse = "";

        JSONObject result = accessTokenAuthenticator.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractClaimsFromJwtResponse_notJwt() throws Exception {
        String rawResponse = "This is not in JWT format";
        withConfigId();

        try {
            JSONObject result = accessTokenAuthenticator.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
            fail("Should have thrown an exception, but got [" + result + "].");
        } catch (Exception e) {
            verifyException(e, "CWWKS1539E");
        }
    }

    @Test
    public void test_extractClaimsFromJwtResponse_jwsMalformed() throws Exception {
        String rawResponse = "aaa.bbb.ccc";
        withConfigId();

        try {
            JSONObject result = accessTokenAuthenticator.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
            fail("Should have thrown an exception, but got [" + result + "].");
        } catch (Exception e) {
            verifyException(e, "CWWKS1533E" + ".+" + Pattern.quote("org.jose4j.json.internal.json_simple.parser.ParseException"));
        }
    }

    @Test
    public void test_extractClaimsFromJwtResponse_jweMalformed() throws Exception {
        String rawResponse = "";
        for (int i = 1; i <= 4; i++) {
            rawResponse += Base64Coder.base64Encode("part" + i) + ".";
        }
        rawResponse += Base64Coder.base64Encode("part" + 5);

        withConfigId();
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getJweDecryptionKey();
                will(returnValue(decryptionKey));
            }
        });
        try {
            JSONObject result = accessTokenAuthenticator.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
            fail("Should have thrown an exception, but got [" + result + "].");
        } catch (Exception e) {
            verifyException(e, "CWWKS1533E" + ".+" + "CWWKS6056E");
        }
    }

    @Test
    public void testCanUseIssuerAsSelectorForInboundPropagation_inboundPropagationDisabled() throws Exception {
        withInboundPropagationEnabled(false);
        boolean result = accessTokenAuthenticator.canUseIssuerAsSelectorForInboundPropagation(req, clientConfig);
        assertFalse("Cannot use issuer as selector when inbound propagation is not enabled.", result);
    }

    @Test
    public void testCanUseIssuerAsSelectorForInboundPropagation_inboundPropagationEnabled_issuerCheckingDisabled() throws Exception {
        withInboundPropagationEnabled(true);
        withoutIssuerChecking();

        boolean result = accessTokenAuthenticator.canUseIssuerAsSelectorForInboundPropagation(req, clientConfig);

        assertFalse("Cannot use issuer as selector when inbound propagation is enabled with issuer checking is disabled.", result);
    }

    @Test
    public void testCanUseIssuerAsSelectorForInboundPropagation_inboundPropagationEnabled_issuerCheckingEnabled_noAccessToken() throws Exception {
        withInboundPropagationEnabled(true);
        withIssuerChecking(GOOD_ISSUER);
        withoutAccessTokenInLtpaCookie();
        withHeaderName(null);
        withAuthorizationHeader(null);
        withMethod("GET");

        boolean result = accessTokenAuthenticator.canUseIssuerAsSelectorForInboundPropagation(req, clientConfig);

        assertFalse("Cannot use issuer as selector when inbound propagation is enabled and there is no access token.", result);
    }

    @Test
    public void testCanUseIssuerAsSelectorForInboundPropagation_inboundPropagationEnabled_issuerCheckingEnabled_opaqueAccessToken() throws Exception {
        withIssuerAsSelectorExpectations(ACCESS_TOKEN);

        boolean result = accessTokenAuthenticator.canUseIssuerAsSelectorForInboundPropagation(req, clientConfig);

        assertFalse("Cannot use issuer as selector when inbound propagation is enabled and there is an opaque access token.", result);
    }

    @Test
    public void testCanUseIssuerAsSelectorForInboundPropagation_inboundPropagationEnabled_issuerCheckingEnabled_jwtAccessToken_noIssuer() throws Exception {
        withIssuerAsSelectorExpectations(createSerializedJwtWithIssuer(null));

        boolean result = accessTokenAuthenticator.canUseIssuerAsSelectorForInboundPropagation(req, clientConfig);

        assertFalse("Cannot use issuer as selector when inbound propagation is enabled and there is a JWT access token without issuer.", result);
    }

    @Test
    public void testCanUseIssuerAsSelectorForInboundPropagation_inboundPropagationEnabled_issuerCheckingEnabled_jwtAccessToken_emptyIssuer() throws Exception {
        withIssuerAsSelectorExpectations(createSerializedJwtWithIssuer(""));

        boolean result = accessTokenAuthenticator.canUseIssuerAsSelectorForInboundPropagation(req, clientConfig);

        assertFalse("Cannot use issuer as selector when inbound propagation is enabled and there is a JWT access token with an empty issuer.", result);
    }

    @Test
    public void testCanUseIssuerAsSelectorForInboundPropagation_inboundPropagationEnabled_issuerCheckingEnabled_jwtAccessToken_mismatchedIssuer() throws Exception {
        withIssuerAsSelectorExpectations(createSerializedJwtWithIssuer(BAD_ISSUER));

        boolean result = accessTokenAuthenticator.canUseIssuerAsSelectorForInboundPropagation(req, clientConfig);

        assertFalse("Cannot use issuer as selector when inbound propagation is enabled and there is a JWT access token with a mismatched issuer.", result);
    }

    @Test
    public void testCanUseIssuerAsSelectorForInboundPropagation_inboundPropagationEnabled_issuerCheckingEnabled_jwtAccessToken_matchingIssuer() throws Exception {
        withIssuerAsSelectorExpectations(createSerializedJwtWithIssuer(GOOD_ISSUER));

        boolean result = accessTokenAuthenticator.canUseIssuerAsSelectorForInboundPropagation(req, clientConfig);

        assertTrue("Can use issuer as selector when inbound propagation is enabled and there is a JWT access token with a matching issuer.", result);
    }

    /**
     * Error descriptions are not delineated properly or are off by a single character.
     *
     * @param authenticator
     * @param c
     */
    private void singleCharacterTests(AccessTokenAuthenticator authenticator, char c) {
        assertEquals(c + Constants.ERROR_DESCRIPTION + "=value", authenticator.extractErrorDescription(c + Constants.ERROR_DESCRIPTION + "=value"));
        assertEquals(Constants.ERROR_DESCRIPTION + c + "=value", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + c + "=value"));
        assertEquals("other, " + c + Constants.ERROR_DESCRIPTION + "=value", authenticator.extractErrorDescription("other, " + c + Constants.ERROR_DESCRIPTION + "=value"));
        assertEquals(Constants.ERROR_DESCRIPTION + c + "=value, other=test", authenticator.extractErrorDescription(Constants.ERROR_DESCRIPTION + c + "=value, other=test"));
        assertEquals("\"" + c + Constants.ERROR_DESCRIPTION + "\":value", authenticator.extractErrorDescription("\"" + c + Constants.ERROR_DESCRIPTION + "\":value"));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + c + "\":value", authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + c + "\":value"));
        assertEquals("\"" + c + Constants.ERROR_DESCRIPTION + "\":\"value\"", authenticator.extractErrorDescription("\"" + c + Constants.ERROR_DESCRIPTION + "\":\"value\""));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + c + "\":\"value\"", authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + c + "\":\"value\""));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + "\"" + c + ":\"value\"",
                authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + "\"" + c + ":\"value\""));
        assertEquals("\"" + Constants.ERROR_DESCRIPTION + "\":" + c + "\"value\"",
                authenticator.extractErrorDescription("\"" + Constants.ERROR_DESCRIPTION + "\":" + c + "\"value\""));
    }

    private String getJSONObjectString(Boolean active, Long expDate) {
        return "{\"user\":\"user1\" , \"active\":" + active + " , \"exp\":" + expDate + "}";
    }

    private String getJSONObjectString(Boolean active, Long expDate, Long issueAtDate) {
        return "{\"user\":\"user1\" , \"active\":" + active + " , \"exp\":" + expDate + " , \"iat\":" + issueAtDate + "}";
    }

    private String getJSONObjectString(Boolean active, Long expDate, Long issueAtDate, String issuer) {
        return "{\"user\":\"user1\" , \"active\":" + active + " , \"exp\":" + expDate + " , \"iat\":" + issueAtDate + " , \"iss\":\"" + issuer + "\"}";
    }

    private String getJSONObjectString(Boolean active, Long expDate, Long issueAtDate, String issuer, Long notBeforeTime) {
        return "{\"user\":\"user1\" , \"active\":" + active + " , \"exp\":" + expDate + " , \"iat\":" + issueAtDate + " , \"iss\":\"" + issuer + "\" , \"nbf\":" + notBeforeTime
                + "}";
    }

    private String createSerializedJwtWithIssuer(String issuer) throws Exception {
        String serializedJwt = null;

        JWSHeader header = new JWSHeader();
        header.setAlgorithm("HS256");
        header.setKeyId("keyid");

        JWTPayload payload = new JWTPayload();
        if (issuer != null) {
            payload.setIssuer(issuer);
        }

        byte[] keyBytes = "secretsecretsecretsecretsecretsecret".getBytes("UTF-8");
        JWT jwt = new JWT(header, payload, keyBytes);
        System.out.println(jwt.getJWTString());
        serializedJwt = jwt.getSignedJWTString();

        return serializedJwt;
    }

    private Long getCurrentDate() {
        return Calendar.getInstance().getTimeInMillis() / 1000;
    }

    private BasicHttpEntity createJsonBasicHttpEntity(Boolean active, String issuer) {
        Long currentDate = getCurrentDate();
        return createBasicHttpEntity(getJSONObjectString(active, currentDate, currentDate, issuer), APPLICATION_JSON);
    }

    private BasicHttpEntity createJsonBasicHttpEntity(Boolean active) {
        Long currentDate = getCurrentDate();
        return createBasicHttpEntity(getJSONObjectString(active, currentDate, currentDate), APPLICATION_JSON);
    }

    private BasicHttpEntity createBasicHttpEntity(String string, String contentType) {
        BasicHttpEntity entity = createBasicHttpEntity(string);
        entity.setContentType(contentType);
        return entity;
    }

    private BasicHttpEntity createBasicHttpEntity(String string) {
        InputStream input = new ByteArrayInputStream(string.getBytes());
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        return entity;
    }

    private SingleTableCache getCache() {
        return new SingleTableCache(1000 * 60);
    }

    private ProviderAuthenticationResult createProviderAuthenticationResult(long expTime) {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> accessTokenInfo = new HashMap<String, Object>();
        accessTokenInfo.put("exp", expTime);
        customProperties.put(Constants.ACCESS_TOKEN_INFO, accessTokenInfo);
        return new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, GOOD_USER, new Subject(), customProperties, HTTPS_URL);
    }

    private void withoutAccessTokenInLtpaCookie() {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
            }
        });
    }

    private void withDefaultBearerAuthorizationHeaderExpectations() {
        withHeaderName(null);
        withAuthorizationHeader(BEARER);
    }

    private void withBearerAuthorizationHeader(final String value) {
        withHeaderName(null);
        withAuthorizationHeader("Bearer " + value);
    }

    private void withHeaderName(final String headerName) {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getHeaderName();
                will(returnValue(headerName));
            }
        });
    }

    private void withAuthorizationHeader(final String value) {
        withHeader(Authorization_Header, value);
    }

    private void withAccessTokenInRequestParameter(final String accessToken) {
        withHeader(ClientConstants.REQ_CONTENT_TYPE_NAME, ClientConstants.REQ_CONTENT_TYPE_APP_FORM_URLENCODED);
        withMethod(ClientConstants.REQ_METHOD_POST);
        mockery.checking(new Expectations() {
            {
                one(req).getParameter(ACCESS_TOKEN);
                will(returnValue(accessToken));
            }
        });
    }

    private void withMethod(final String value) {
        mockery.checking(new Expectations() {
            {
                one(req).getMethod();
                will(returnValue(value));
            }
        });
    }

    private void withHeader(final String header, final String value) {
        mockery.checking(new Expectations() {
            {
                one(req).getHeader(header);
                will(returnValue(value));
            }
        });
    }

    private SingleTableCache withAccessTokenCacheExpectations(final boolean tokenReuse, final boolean accessTokenCacheEnabled) {
        final SingleTableCache cache;

        mockery.checking(new Expectations() {
            {
                one(clientConfig).getTokenReuse();
                will(returnValue(tokenReuse));
                allowing(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(accessTokenCacheEnabled));
            }
        });

        if (accessTokenCacheEnabled) {
            cache = getCache();
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getCache();
                    will(returnValue(cache));
                    allowing(clientConfig).getClockSkew();
                    will(returnValue(0L));
                }
            });
            withConfigId();
        } else {
            cache = null;
        }
        return cache;
    }

    private void withUserInfoExpectations(final boolean hostNameVerificationEnabled, final boolean httpsRequired) {
        withValidationMethod(ClientConstants.VALIDATION_USERINFO);
        commonValidationExpectations(hostNameVerificationEnabled, httpsRequired);
    }

    private void withIntrospection() {
        withValidationMethod(ClientConstants.VALIDATION_INTROSPECT);
    }

    private void withIntrospectionExpectations(final boolean hostNameVerificationEnabled, final boolean httpsRequired, final String clientSecret) {
        withValidationMethod(ClientConstants.VALIDATION_INTROSPECT);
        commonValidationExpectations(hostNameVerificationEnabled, httpsRequired);
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getClientSecret();
                will(returnValue(clientSecret));
            }
        });
    }

    private void withValidationMethod(final String validationMethod) {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getValidationMethod();
                will(returnValue(validationMethod));
            }
        });
    }

    private void commonValidationExpectations(final boolean hostNameVerificationEnabled, final boolean httpsRequired) {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(hostNameVerificationEnabled));
            }
        });
        withHttpsRequired(httpsRequired);
    }

    private void withHttpsRequired(final boolean httpsRequired) {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).isHttpsRequired();
                will(returnValue(httpsRequired));
            }
        });
    }

    private void withWWWAuthenticateHeader(final String value) {
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getFirstHeader("WWW-Authenticate");
                will(returnValue(header));

                one(header).getValue();
                will(returnValue(value));
            }
        });
    }

    private void withInboundPropagationSupported() {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getInboundPropagation();
                will(returnValue(SUPPORTED));
            }
        });
    }

    private void withInboundPropagationRequired() {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getInboundPropagation();
                will(returnValue(REQUIRED));
            }
        });
    }

    private void withInboundPropagationEnabled(final boolean value) {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).isInboundPropagationEnabled();
                will(returnValue(value));
            }
        });
    }

    private void withNewRsFailMsg(final String msg) {
        mockery.checking(new Expectations() {
            {
                one(clientRequest).getRsFailMsg();
                will(returnValue(null));
                one(clientRequest).setRsFailMsg(null, msg);
            }
        });
    }

    private void withRsFailMsg(final String msg) {
        mockery.checking(new Expectations() {
            {
                one(clientRequest).getRsFailMsg();
                will(returnValue(msg));
            }
        });
    }

    private void withOkAndEntity(final BasicHttpEntity entity) {
        withEntityAndStatusCode(entity, HttpServletResponse.SC_OK);
    }

    private void withForbiddenAndEntity(final BasicHttpEntity entity) {
        withEntityAndStatusCode(entity, HttpServletResponse.SC_FORBIDDEN);
    }

    private void withEntityAndStatusCode(final BasicHttpEntity entity, int statusCode) {
        withEntity(entity);
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(statusLine).getStatusCode();
                will(returnValue(statusCode));
            }
        });
    }

    private void withEntity(final HttpEntity httpEntity) {
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
            }
        });
    }

    private void withUserIdentifier(final String userIdentifier) {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getUserIdentifier();
                will(returnValue(userIdentifier));
                allowing(clientConfig).isSocial();
                will(returnValue(false));
            }
        });
    }

    private void withoutIssuerChecking() {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).disableIssChecking();
                will(returnValue(true));
            }
        });
    }

    private void withIssuerChecking(final String issuerIdentifier) {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(clientConfig).disableIssChecking();
                will(returnValue(false));
            }
        });
    }

    private void withExpClaimRequired(final boolean required) {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).requireExpClaimForIntrospection();
                will(returnValue(required));
            }
        });
    }

    private void withIatClaimRequired(final boolean required) {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).requireIatClaimForIntrospection();
                will(returnValue(required));
            }
        });
    }

    private void withPropagationTokenAuthenticatedAttribute() {
        mockery.checking(new Expectations() {
            {
                one(req).setAttribute(OidcClient.PROPAGATION_TOKEN_AUTHENTICATED, Boolean.TRUE);
            }
        });
    }

    private void withConfigId() {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getId();
                will(returnValue(CONFIG_ID));
            }
        });
    }

    private void withIssuerAsSelectorExpectations(String bearerAuthorizationHeader) {
        withInboundPropagationEnabled(true);
        withIssuerChecking(GOOD_ISSUER);
        withoutAccessTokenInLtpaCookie();
        withBearerAuthorizationHeader(bearerAuthorizationHeader);
    }

    private void assertSuccessWithOK(ProviderAuthenticationResult oidcResult) {
        assertStatus(AuthResult.SUCCESS, oidcResult);
        assertStatusCode(HttpServletResponse.SC_OK, oidcResult);
    }

    private void assertFailureWithUnauthorized(ProviderAuthenticationResult oidcResult) {
        assertStatus(AuthResult.FAILURE, oidcResult);
        assertUnauthorized(oidcResult);
    }

    private void assert401WithUnauthorized(ProviderAuthenticationResult oidcResult) {
        assertStatus(AuthResult.SEND_401, oidcResult);
        assertUnauthorized(oidcResult);
    }

    private void assertStatus(AuthResult expectedStatus, ProviderAuthenticationResult oidcResult) {
        assertEquals("Unexpected status, expected:" + expectedStatus + " but received:" + oidcResult.getStatus() + ".",
                expectedStatus, oidcResult.getStatus());
    }

    private void assertUnauthorized(ProviderAuthenticationResult oidcResult) {
        assertStatusCode(HttpServletResponse.SC_UNAUTHORIZED, oidcResult);
    }

    private void assertStatusCode(int expectedStatusCode, ProviderAuthenticationResult oidcResult) {
        assertEquals("Unexpected status code, expected:" + expectedStatusCode + " but received:" + oidcResult.getHttpStatusCode() + ".",
                expectedStatusCode, oidcResult.getHttpStatusCode());
    }

    final class FakeAccessTokenAuthenticator extends AccessTokenAuthenticator {
        OidcClientUtil fakeOidcClientUtil = new FakeOidcClientUtil();
        public boolean fixSubjectCalled = false;

        FakeAccessTokenAuthenticator(SSLSupport sslSupport) {
            super.sslSupport = sslSupport;
            super.oidcClientUtil = fakeOidcClientUtil;
        }

        @Override
        ProviderAuthenticationResult fixSubject(ProviderAuthenticationResult oidcResult) {
            fixSubjectCalled = true;
            return OidcClientAuthenticator.fixSubject(oidcResult);
        }
    }

    final class FakeJSSEHelper extends JSSEHelper {

        FakeJSSEHelper() {
        }

        @Override
        public SSLContext getSSLContext(String sslAliasName, Map<String, Object> connectionInfo,
                SSLConfigChangeListener listener, boolean tryDefault) throws SSLException, SSLConfigurationNotAvailableException {

            SSLConfig config = new SSLConfig();
            SSLContext sslContext = JSSEProviderFactory.getInstance().getSSLContextInstance(config);
            return sslContext;
        }
    }

    final class FakeOidcClientUtil extends OidcClientUtil {
        Map<String, Object> responseMap = new HashMap<String, Object>();

        @Override
        public Map<String, Object> getUserinfo(String userInfor, String accessToken, SSLSocketFactory sslSocketFactory, boolean isHostnameVerification, boolean useJvmProps) throws Exception {
            responseMap.clear();
            if (isHostnameVerification) {
                responseMap.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);
                return responseMap;
            } else {
                return null;
            }
        }

        @Override
        public Map<String, Object> checkToken(String tokenInfor, String clientId, @Sensitive String clientSecret,
                String accessToken, boolean isHostnameVerification, String authMethod, SSLSocketFactory sslSocketFactory, boolean useJvmProps) throws Exception {
            responseMap.clear();
            if (!isHostnameVerification) {
                return null;
            } else {
                responseMap.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);
                return responseMap;
            }
        }
    }
}