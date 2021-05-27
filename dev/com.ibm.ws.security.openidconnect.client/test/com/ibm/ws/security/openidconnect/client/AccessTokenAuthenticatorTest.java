/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client;

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
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.common.structures.SingleTableCache;
import com.ibm.ws.security.openidconnect.client.internal.AccessTokenCacheEntry;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.MockOidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil;
import com.ibm.ws.security.openidconnect.common.Constants;
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
    private static final String ACCESS_TOKEN = "access_token";
    private static final String HTTPS_URL = "https://localhost:8020/root";
    private static final String BEARER = "Bearer " + ACCESS_TOKEN;
    private static final String GOOD_USER = "user";
    private static final String GOOD_ISSUER = "good_issuer";
    private static final String BAD_ISSUER = "bad_issuer";
    private static final String SUPPORTED = "supported";
    private static final String REQUIRED = "required";

    private final AccessTokenAuthenticator tokenAuth = new AccessTokenAuthenticator() {
        @Override
        boolean isRunningBetaMode() {
            return true;
        }
    };
    private final AccessTokenAuthenticator sslTokenAuth = new FakeAccessTokenAuthenticator(sslSupport);
    private final ReferrerURLCookieHandler referrerURLCookieHandler = new ReferrerURLCookieHandler(webAppSecConfig);
    private final Map<String, Object> respMap = new HashMap<String, Object>();

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
                allowing(clientConfig).getHeaderName();
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
        final Long currentDate = Calendar.getInstance().getTimeInMillis() / 1000;
        final InputStream input = new ByteArrayInputStream(getJSONObjectString(true, currentDate, currentDate).getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");

        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(BEARER));
                one(req).setAttribute(OidcClient.PROPAGATION_TOKEN_AUTHENTICATED, Boolean.TRUE);
                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                one(clientConfig).getTokenReuse();
                will(returnValue(false));
                allowing(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(false));
                allowing(clientConfig).getValidationMethod();
                will(returnValue(ClientConstants.VALIDATION_USERINFO));
                one(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(true));
                one(clientConfig).getUserIdentifier();
                will(returnValue(GOOD_USER));
                one(clientConfig).isHttpsRequired();
                will(returnValue(false));

                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(httpResponse).getEntity();
                will(returnValue(entity));

                one(statusLine).getStatusCode();
                will(returnValue(HttpServletResponse.SC_OK));
            }
        });

        ((FakeAccessTokenAuthenticator) sslTokenAuth).fixSubjectCalled = false;
        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));
        assertEquals("Unexpected status, expected:" + AuthResult.SUCCESS + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.SUCCESS, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_OK + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_OK, oidcResult.getHttpStatusCode());

        // verify that subject fixup was called
        assertTrue("fixSubject was not called as expected ", ((FakeAccessTokenAuthenticator) sslTokenAuth).fixSubjectCalled);
    }

    @Test
    public void testAuthenticate_UserinfoValidation_BadUserIdentifier() {
        final String BAD_USER = "bad_user";
        final Long currentDate = Calendar.getInstance().getTimeInMillis() / 1000;
        final InputStream input = new ByteArrayInputStream(getJSONObjectString(true, currentDate, currentDate).getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");

        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(BEARER));

                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                one(clientConfig).getTokenReuse();
                will(returnValue(false));
                allowing(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(false));
                allowing(clientConfig).getValidationMethod();
                will(returnValue(ClientConstants.VALIDATION_USERINFO));
                one(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(true));
                exactly(2).of(clientConfig).getUserIdentifier();
                will(returnValue(BAD_USER));
                one(clientConfig).isHttpsRequired();
                will(returnValue(true));

                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(httpResponse).getEntity();
                will(returnValue(entity));

                one(statusLine).getStatusCode();
                will(returnValue(HttpServletResponse.SC_OK));
            }
        });

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertEquals("Unexpected status, expected:" + AuthResult.SEND_401 + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.SEND_401, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_UNAUTHORIZED + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_UNAUTHORIZED, oidcResult.getHttpStatusCode());
    }

    @Test
    public void testAuthenticate_UserinfoValidation_BadJSONObject() {
        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(BEARER));

                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                one(clientConfig).getTokenReuse();
                will(returnValue(false));
                one(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(false));
                allowing(clientConfig).getValidationMethod();
                will(returnValue(ClientConstants.VALIDATION_USERINFO));
                one(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(false));
                one(clientConfig).isHttpsRequired();
                will(returnValue(false));
                one(clientConfig).getInboundPropagation();
                will(returnValue("supported"));
            }
        });

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertEquals("Unexpected status, expected:" + AuthResult.FAILURE + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.FAILURE, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_UNAUTHORIZED + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_UNAUTHORIZED, oidcResult.getHttpStatusCode());
    }

    @Test
    public void testAuthenticate_IntrospectTokenValidation_disableIssChecking() {
        final Long currentDate = Calendar.getInstance().getTimeInMillis() / 1000;
        final InputStream input = new ByteArrayInputStream(getJSONObjectString(true, currentDate, currentDate, GOOD_ISSUER).getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");

        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(BEARER));

                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                one(clientConfig).getTokenReuse();
                will(returnValue(false));
                allowing(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(false));
                allowing(clientConfig).getValidationMethod();
                will(returnValue(ClientConstants.VALIDATION_INTROSPECT));
                allowing(clientConfig).getInboundPropagation();
                will(returnValue("required"));
                one(clientConfig).getClientSecret();
                will(returnValue("client_secret"));
                one(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(true));

                one(clientConfig).isHttpsRequired();
                will(returnValue(false));
                one(clientConfig).disableIssChecking();
                will(returnValue(true));

                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(httpResponse).getEntity();
                will(returnValue(entity));

                one(statusLine).getStatusCode();
                will(returnValue(HttpServletResponse.SC_OK));
            }
        });

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertEquals("Unexpected status, expected:" + AuthResult.FAILURE + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.FAILURE, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_UNAUTHORIZED + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_UNAUTHORIZED, oidcResult.getHttpStatusCode());
        assertTrue("Expected message was not logged",
                outputMgr.checkForMessages("CWWKS1735E:"));

    }

    @Test
    public void testAuthenticate_IntrospectTokenValidation_disableIssChecking_no_issclaim() {
        final Long currentDate = Calendar.getInstance().getTimeInMillis() / 1000;
        final InputStream input = new ByteArrayInputStream(getJSONObjectString(true, currentDate, currentDate).getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");

        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(BEARER));
                one(req).setAttribute(OidcClient.PROPAGATION_TOKEN_AUTHENTICATED, Boolean.TRUE);

                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                one(clientConfig).getTokenReuse();
                will(returnValue(false));
                allowing(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(false));
                allowing(clientConfig).getValidationMethod();
                will(returnValue(ClientConstants.VALIDATION_INTROSPECT));
                one(clientConfig).getClientSecret();
                will(returnValue("client_secret"));
                one(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(true));

                one(clientConfig).isHttpsRequired();
                will(returnValue(false));
                one(clientConfig).disableIssChecking();
                will(returnValue(true));
                one(clientConfig).getUserIdentifier();
                will(returnValue(GOOD_USER));
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(httpResponse).getEntity();
                will(returnValue(entity));

                one(statusLine).getStatusCode();
                will(returnValue(HttpServletResponse.SC_OK));
            }
        });

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertEquals("Unexpected status, expected:" + AuthResult.SUCCESS + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.SUCCESS, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_OK + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_OK, oidcResult.getHttpStatusCode());
    }

    @Test
    public void testAuthenticate_IntrospectTokenValidation_GoodOidcResult() {
        SingleTableCache cache = getCache();
        final Long currentDate = Calendar.getInstance().getTimeInMillis() / 1000;
        final InputStream input = new ByteArrayInputStream(getJSONObjectString(true, currentDate, currentDate, GOOD_ISSUER).getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");

        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(BEARER));
                one(req).setAttribute(OidcClient.PROPAGATION_TOKEN_AUTHENTICATED, Boolean.TRUE);

                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                allowing(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(true));
                one(clientConfig).getTokenReuse();
                will(returnValue(false));
                allowing(clientConfig).getValidationMethod();
                will(returnValue(ClientConstants.VALIDATION_INTROSPECT));
                one(clientConfig).getClientSecret();
                will(returnValue("client_secret"));
                one(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(true));
                one(clientConfig).getIssuerIdentifier();
                will(returnValue(GOOD_ISSUER));
                one(clientConfig).getUserIdentifier();
                will(returnValue(GOOD_USER));
                one(clientConfig).isHttpsRequired();
                will(returnValue(false));
                one(clientConfig).disableIssChecking();
                will(returnValue(false));

                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(httpResponse).getEntity();
                will(returnValue(entity));

                one(statusLine).getStatusCode();
                will(returnValue(HttpServletResponse.SC_OK));
                exactly(2).of(clientConfig).getCache();
                will(returnValue(cache));
            }
        });

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertEquals("Unexpected status, expected:" + AuthResult.SUCCESS + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.SUCCESS, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_OK + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_OK, oidcResult.getHttpStatusCode());

        // Verify that the result was cached
        AccessTokenCacheEntry cacheEntry = (AccessTokenCacheEntry) cache.get(ACCESS_TOKEN);
        assertNotNull("Cached authentication result should not have been null but was.", cacheEntry);
        assertEquals("Cached result did not match the result originally returned from the authenticate method.", oidcResult, cacheEntry.getResult());
    }

    @Test
    public void testAuthenticate_IntrospectTokenValidation_TokenNotActive() {
        final Long currentDate = Calendar.getInstance().getTimeInMillis() / 1000;
        final InputStream input = new ByteArrayInputStream(getJSONObjectString(false, currentDate, currentDate, GOOD_ISSUER).getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");

        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(BEARER));

                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                one(clientConfig).getTokenReuse();
                will(returnValue(false));
                allowing(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(false));
                allowing(clientConfig).getValidationMethod();
                will(returnValue(ClientConstants.VALIDATION_INTROSPECT));
                one(clientConfig).getClientSecret();
                will(returnValue(null));
                one(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(true));
                one(clientConfig).isHttpsRequired();
                will(returnValue(true));

                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(httpResponse).getEntity();
                will(returnValue(entity));

                one(statusLine).getStatusCode();
                will(returnValue(HttpServletResponse.SC_OK));

                allowing(clientConfig).getInboundPropagation();
                will(returnValue(SUPPORTED));
            }
        });

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertEquals("Unexpected status, expected:" + AuthResult.FAILURE + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.FAILURE, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_UNAUTHORIZED + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_UNAUTHORIZED, oidcResult.getHttpStatusCode());
    }

    @Test
    public void testAuthenticate_IntrospectTokenValidation_BadJSONObject() {
        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(BEARER));

                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                one(clientConfig).getTokenReuse();
                will(returnValue(true));
                one(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(false));
                allowing(clientConfig).getValidationMethod();
                will(returnValue(ClientConstants.VALIDATION_INTROSPECT));
                one(clientConfig).getClientSecret();
                will(returnValue(null));
                one(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(false));
                one(clientConfig).isHttpsRequired();
                will(returnValue(false));
                allowing(clientConfig).getInboundPropagation();
                will(returnValue(REQUIRED));
            }
        });

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertEquals("Unexpected status, expected:" + AuthResult.FAILURE + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.FAILURE, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_UNAUTHORIZED + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_UNAUTHORIZED, oidcResult.getHttpStatusCode());
    }

    @Test
    public void testAuthenticate_InvalidValidationMethod() throws javax.net.ssl.SSLException {
        final String INVALID_VALIDATION = "invalid_validation";
        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(BEARER));

                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                one(clientConfig).getTokenReuse();
                will(returnValue(false));
                one(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(false));
                allowing(clientConfig).getValidationMethod();
                will(returnValue(INVALID_VALIDATION));
                one(clientConfig).getTokenEndpointUrl();
                will(returnValue(HTTPS_URL));
                one(clientConfig).isHttpsRequired();
                will(returnValue(false));

            }
        });

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertEquals("Unexpected status, expected:" + AuthResult.FAILURE + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.FAILURE, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_UNAUTHORIZED + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_UNAUTHORIZED, oidcResult.getHttpStatusCode());
    }

    @Test
    public void testAuthenticate_NullAccessToken() {
        final HashMap map = new HashMap();
        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(null));
                one(req).getMethod(); //
                will(returnValue(ClientConstants.REQ_METHOD_POST));
                one(req).getHeader(ClientConstants.REQ_CONTENT_TYPE_NAME);
                will(returnValue(ClientConstants.REQ_CONTENT_TYPE_APP_FORM_URLENCODED));
                one(req).getParameter(ACCESS_TOKEN);
                will(returnValue(null));

                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                allowing(clientConfig).getInboundPropagation();
                will(returnValue(SUPPORTED));
                //one(clientConfig).getIssuerIdentifier();
                //will(returnValue(null));

                //one(res).setHeader(with(any(String.class)), with(any(String.class)));
            }
        });

        ProviderAuthenticationResult oidcResult = tokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertEquals("Unexpected status, expected:" + AuthResult.FAILURE + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.FAILURE, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_UNAUTHORIZED + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_UNAUTHORIZED, oidcResult.getHttpStatusCode());
    }

    @Test
    public void testAuthenticate_ThrowsSSLException() {
        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(null));
                one(req).getMethod();//
                will(returnValue(ClientConstants.REQ_METHOD_POST));
                one(req).getHeader(ClientConstants.REQ_CONTENT_TYPE_NAME);
                will(returnValue(ClientConstants.REQ_CONTENT_TYPE_APP_FORM_URLENCODED));
                one(req).getParameter(ACCESS_TOKEN);
                will(returnValue(ACCESS_TOKEN));

                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                one(clientConfig).getTokenReuse();
                will(returnValue(false));
                one(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(false));
                one(clientConfig).getValidationMethod();
                will(returnValue(ClientConstants.VALIDATION_INTROSPECT));

                allowing(clientConfig).getInboundPropagation();
                will(returnValue(REQUIRED));
            }
        });

        ProviderAuthenticationResult oidcResult = tokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertEquals("Unexpected status, expected:" + AuthResult.SEND_401 + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.SEND_401, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_UNAUTHORIZED + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_UNAUTHORIZED, oidcResult.getHttpStatusCode());
    }

    @Test
    public void testAuthenticate_resultAlreadyCached() {
        SingleTableCache cache = getCache();
        ProviderAuthenticationResult cachedResult = createProviderAuthenticationResult(System.currentTimeMillis());
        AccessTokenCacheEntry cacheEntry = new AccessTokenCacheEntry("unique id", cachedResult);
        cache.put(ACCESS_TOKEN, cacheEntry);

        mockery.checking(new Expectations() {
            {
                one(req).getHeader(Authorization_Header);
                will(returnValue(BEARER));
                one(req).setAttribute(OidcClient.PROPAGATION_TOKEN_AUTHENTICATED, Boolean.TRUE);
                one(clientConfig).getAccessTokenInLtpaCookie();
                will(returnValue(false));
                one(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(true));
                one(clientConfig).getTokenReuse();
                will(returnValue(true));
                one(clientConfig).getCache();
                will(returnValue(cache));
            }
        });

        ProviderAuthenticationResult oidcResult = sslTokenAuth.authenticate(req, res, clientConfig, new MockOidcClientRequest(referrerURLCookieHandler));

        assertEquals("Unexpected status, expected:" + AuthResult.SUCCESS + " but received:" + oidcResult.getStatus() + ".",
                AuthResult.SUCCESS, oidcResult.getStatus());
        assertEquals("Unexpected status code, expected:" + HttpServletResponse.SC_OK + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_OK, oidcResult.getHttpStatusCode());
    }

    @Test
    public void testValidateJsonResponse_TimeIsExpired() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);

        Long minusDate = cal.getTimeInMillis() / 1000;
        final String JSONSTRING = getJSONObjectString(true, minusDate);
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getInboundPropagation();
                will(returnValue(SUPPORTED));
            }
        });

        Boolean isValid = tokenAuth.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_noExpirationTime() throws IOException {
        final String JSONSTRING = "{\"user\":\"user1\" , \"active\":true}";
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        mockery.checking(new Expectations() {
            {
                one(clientConfig).requireExpClaimForIntrospection();
                will(returnValue(true));
                allowing(clientConfig).getInboundPropagation();
                will(returnValue(REQUIRED));
            }
        });

        Boolean isValid = tokenAuth.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_noExpirationTime_doNotRequireExpClaim() throws IOException {
        final String JSONSTRING = "{\"user\":\"user1\" , \"active\":true}";
        JSONObject jobj = JSONObject.parse(JSONSTRING);
        Calendar cal = Calendar.getInstance();
        Long currentDate = cal.getTimeInMillis() / 1000;
        jobj.put("iat", currentDate);
        jobj.put("iss", GOOD_ISSUER);

        mockery.checking(new Expectations() {
            {
                one(clientConfig).requireExpClaimForIntrospection();
                will(returnValue(false));
                one(clientConfig).disableIssChecking();
                will(returnValue(false));
                one(clientConfig).getIssuerIdentifier();
                will(returnValue(GOOD_ISSUER));
            }
        });

        Boolean isValid = tokenAuth.validateJsonResponse(jobj, clientConfig);
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

        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getInboundPropagation();
                will(returnValue(SUPPORTED));
            }
        });

        Boolean isValid = tokenAuth.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_noIssueAtTime() throws IOException {
        Calendar cal = Calendar.getInstance();
        Long expTime = cal.getTimeInMillis() / 1000;

        final String JSONSTRING = getJSONObjectString(true, expTime);
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        mockery.checking(new Expectations() {
            {
                one(clientConfig).requireIatClaimForIntrospection();
                will(returnValue(true));
                allowing(clientConfig).getInboundPropagation();
                will(returnValue(REQUIRED));
            }
        });

        Boolean isValid = tokenAuth.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_noIssueAtTime_doNotRequireIatClaim() throws IOException {
        Calendar cal = Calendar.getInstance();
        Long expTime = cal.getTimeInMillis() / 1000;

        final String JSONSTRING = getJSONObjectString(true, expTime);
        JSONObject jobj = JSONObject.parse(JSONSTRING);
        jobj.put("iss", GOOD_ISSUER);

        mockery.checking(new Expectations() {
            {
                one(clientConfig).requireIatClaimForIntrospection();
                will(returnValue(false));
                one(clientConfig).disableIssChecking();
                will(returnValue(false));
                one(clientConfig).getIssuerIdentifier();
                will(returnValue(GOOD_ISSUER));
            }
        });

        Boolean isValid = tokenAuth.validateJsonResponse(jobj, clientConfig);
        assertTrue("Response should have been considered valid, but was not.", isValid);
    }

    @Test
    public void testValidateJsonResponse_IncompatibleIssuer() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getIssuerIdentifier();
                will(returnValue(GOOD_ISSUER));
                one(clientConfig).disableIssChecking();
                will(returnValue(false));
                allowing(clientConfig).getInboundPropagation();
                will(returnValue(SUPPORTED));
            }
        });

        Calendar cal = Calendar.getInstance();
        Long currentDate = cal.getTimeInMillis() / 1000;

        final String JSONSTRING = getJSONObjectString(true, currentDate, currentDate, BAD_ISSUER);
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        Boolean isValid = tokenAuth.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_NotBeforeTime_Good() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getIssuerIdentifier();
                will(returnValue(GOOD_ISSUER));
                one(clientConfig).disableIssChecking();
                will(returnValue(false));

            }
        });

        Calendar cal = Calendar.getInstance();
        Long currentDate = cal.getTimeInMillis() / 1000;

        final String JSONSTRING = getJSONObjectString(true, currentDate, currentDate, GOOD_ISSUER, currentDate);
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        Boolean isValid = tokenAuth.validateJsonResponse(jobj, clientConfig);
        assertTrue("Expected to receive a true value but was received: " + isValid, isValid);
    }

    @Test
    public void testValidateJsonResponse_NotBeforeTime_Bad() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getIssuerIdentifier();
                will(returnValue(GOOD_ISSUER));
                one(clientConfig).disableIssChecking();
                will(returnValue(false));
                allowing(clientConfig).getInboundPropagation();
                will(returnValue(REQUIRED));
            }
        });

        Calendar cal = Calendar.getInstance();
        Long currentDate = cal.getTimeInMillis() / 1000;

        cal.add(Calendar.YEAR, 1);
        Long notBeforeTime = cal.getTimeInMillis() / 1000;

        final String JSONSTRING = getJSONObjectString(true, currentDate, currentDate, GOOD_ISSUER, notBeforeTime);
        JSONObject jobj = JSONObject.parse(JSONSTRING);

        Boolean isValid = tokenAuth.validateJsonResponse(jobj, clientConfig);
        assertFalse("Expected to receive a false value but was received: " + isValid, isValid);
    }

    @Test
    public void testGetLong() {
        Long currentDate = Calendar.getInstance().getTimeInMillis() / 1000;
        Integer intCurrentDate = Integer.valueOf(currentDate.toString());

        Long result1 = tokenAuth.getLong(intCurrentDate);
        assertEquals("Expected to receive: " + currentDate + " but received: " + result1 + ".", currentDate, result1);

        String[] dates = { currentDate.toString(), Calendar.getInstance().toString() };
        Long result2 = tokenAuth.getLong(dates);
        assertEquals("Expected to receive: " + currentDate + " but received: " + result2 + ".", currentDate, result2);
    }

    @Test
    public void testHandleResponseMap() throws Exception {
        final InputStream input = new ByteArrayInputStream(new String("").getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);

        final String failMsg = "CWWKS1720E: The resource server failed the authentication request because the access token which is in the request is not active. The validation method is [null], and the validation endpoint url is ["
                + HTTPS_URL + "].";

        mockery.checking(new Expectations() {
            {
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(httpResponse).getEntity();
                will(returnValue(entity));
                one(httpResponse).getFirstHeader("WWW-Authenticate");
                will(returnValue(header));

                one(statusLine).getStatusCode();
                will(returnValue(HttpServletResponse.SC_FORBIDDEN));

                one(header).getValue();
                will(returnValue("error.error_description=invalid_token"));

                one(clientConfig).getValidationMethod();
                will(returnValue(null));

                allowing(clientConfig).getInboundPropagation();
                will(returnValue(SUPPORTED));

                one(clientRequest).getRsFailMsg();
                will(returnValue(null));
                one(clientRequest).setRsFailMsg(null, failMsg);
                one(clientRequest).getRsFailMsg();
                will(returnValue(failMsg));
            }
        });

        respMap.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);
        JSONObject jsonObject = tokenAuth.handleResponseMap(respMap, clientConfig, clientRequest);
        assertNull("Expected to receive a null value but was received: " + jsonObject, jsonObject);
    }

    @Test
    public void testHandleResponseMap_NullHeader() throws Exception {
        final InputStream input = new ByteArrayInputStream(new String("").getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);

        final String failMsg = "CWWKS1721E: The resource server received an error [] while it was attempting to validate the access token. It is either expired or cannot be recognized by the validation end point ["
                + HTTPS_URL + "].";
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(httpResponse).getEntity();
                will(returnValue(entity));
                one(httpResponse).getFirstHeader("WWW-Authenticate");
                will(returnValue(header));

                one(statusLine).getStatusCode();
                will(returnValue(HttpServletResponse.SC_FORBIDDEN));

                one(header).getValue();
                will(returnValue(null));

                allowing(clientConfig).getInboundPropagation();
                will(returnValue(REQUIRED));

                one(clientRequest).getRsFailMsg();
                will(returnValue(null));
                one(clientRequest).setRsFailMsg(null, failMsg);
            }
        });

        respMap.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);
        JSONObject jsonObject = tokenAuth.handleResponseMap(respMap, clientConfig, clientRequest);
        assertNull("Expected to receive a null value but was received: " + jsonObject, jsonObject);
    }

    @Test
    public void test_extractSuccessfulResponse_responseMissingEntity() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(null));
            }
        });
        JSONObject result = tokenAuth.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractSuccessfulResponse_emptyString() throws Exception {
        final InputStream input = new ByteArrayInputStream(("").getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(entity));
                one(clientConfig).getInboundPropagation();
                will(returnValue(REQUIRED));
                one(clientRequest).getRsFailMsg();
                will(returnValue("doesn't matter"));
            }
        });
        JSONObject result = tokenAuth.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractSuccessfulResponse_missingContentType() throws Exception {
        String inputString = "This is not JSON";
        final InputStream input = new ByteArrayInputStream(inputString.getBytes());
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
                one(httpEntity).getContent();
                will(returnValue(input));
                allowing(httpEntity).getContentLength();
                will(returnValue((long) inputString.length()));
                allowing(httpEntity).getContentType();
                will(returnValue(null));
                one(clientConfig).getInboundPropagation();
                will(returnValue(REQUIRED));
                one(clientRequest).getRsFailMsg();
                will(returnValue("doesn't matter"));
            }
        });
        JSONObject result = tokenAuth.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractSuccessfulResponse_notJson() throws Exception {
        final InputStream input = new ByteArrayInputStream(("This is not JSON").getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("text/plain");
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(entity));
            }
        });
        JSONObject result = tokenAuth.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractSuccessfulResponse_emptyJson() throws Exception {
        JSONObject responseJson = new JSONObject();
        final InputStream input = new ByteArrayInputStream(new String(responseJson.toString()).getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(entity));
            }
        });
        JSONObject result = tokenAuth.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty, but was " + result + ".", result.isEmpty());
    }

    @Test
    public void test_extractSuccessfulResponse_validNonEmptyJson() throws Exception {
        JSONObject responseJson = new JSONObject();
        responseJson.put("key1", "value1");
        responseJson.put("key2", "value2");
        final InputStream input = new ByteArrayInputStream(new String(responseJson.toString()).getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(entity));
            }
        });
        JSONObject result = tokenAuth.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Result did not match the expected value.", responseJson, result);
    }

    @Test
    public void test_extractSuccessfulResponse_validNonEmptyJson_contentTypeJwt() throws Exception {
        JSONObject responseJson = new JSONObject();
        responseJson.put("key1", "value1");
        responseJson.put("key2", "value2");
        final InputStream input = new ByteArrayInputStream(new String(responseJson.toString()).getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/jwt");
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(entity));
            }
        });
        JSONObject result = tokenAuth.extractSuccessfulResponse(clientConfig, clientRequest, httpResponse);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractClaimsFromJwtResponse_responseStringEmpty() throws Exception {
        String rawResponse = "";

        JSONObject result = tokenAuth.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractClaimsFromJwtResponse_notJwt() throws Exception {
        String rawResponse = "This is not in JWT format";

        JSONObject result = tokenAuth.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void test_extractClaimsFromJwtResponse_jwsMalformed() throws Exception {
        String rawResponse = "aaa.bbb.ccc";
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getId();
                will(returnValue("configId"));
            }
        });
        try {
            JSONObject result = tokenAuth.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
            fail("Should have thrown an exception, but got " + result + ".");
        } catch (Exception e) {
            verifyException(e, "CWWKS1533E" + ".+" + Pattern.quote("org.jose4j.jwt.consumer.InvalidJwtException"));
        }
    }

    @Test
    public void test_extractClaimsFromJwtResponse_jweMalformed() throws Exception {
        String rawResponse = "";
        for (int i = 1; i <= 4; i++) {
            rawResponse += Base64Coder.base64Encode("part" + i) + ".";
        }
        rawResponse += Base64Coder.base64Encode("part" + 5);

        mockery.checking(new Expectations() {
            {
                one(clientConfig).getJweDecryptionKey();
                will(returnValue(decryptionKey));
                allowing(clientConfig).getId();
                will(returnValue("configId"));
            }
        });
        try {
            JSONObject result = tokenAuth.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
            fail("Should have thrown an exception, but got " + result + ".");
        } catch (Exception e) {
            verifyException(e, "CWWKS1533E" + ".+" + "CWWKS6056E");
        }
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

    private SingleTableCache getCache() {
        return new SingleTableCache(1000 * 60);
    }

    private ProviderAuthenticationResult createProviderAuthenticationResult(long expTime) {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> accessTokenInfo = new HashMap<String, Object>();
        accessTokenInfo.put("exp", expTime);
        customProperties.put(Constants.ACCESS_TOKEN_INFO, accessTokenInfo);
        ProviderAuthenticationResult result = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, GOOD_USER, new Subject(), customProperties, HTTPS_URL);
        return result;
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
            return new OidcClientAuthenticator().fixSubject(oidcResult);
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