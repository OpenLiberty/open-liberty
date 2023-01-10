/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.security.openidconnect.clients.common.AuthorizationCodeHandler;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.MockOidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import test.common.SharedOutputManager;

public class OidcClientAuthenticatorTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String TEST_ORIGINAL_STATE = "originalState6547";
    private static final String TEST_URL = "http://harmonic.austin.ibm.com:8010/formlogin/SimpleServlet";
    private static final String TEST_JWK_ENDPOINT = "http://acme:8011/oidc/endpoint/OidcConfigSample/jwk";
    private static final String CLIENTID = "clientid";
    private static final String CLIENT01 = "client01";
    private static final String AUTHZ_CODE = "authorizaCodeAAA";

    private final OidcClientConfig clientConfig = mock.mock(OidcClientConfig.class, "clientConfig");
    @SuppressWarnings("unchecked")
    private final AtomicServiceReference<SSLSupport> sslSupportRef = mock.mock(AtomicServiceReference.class, "sslSupportRef");
    private final SSLSupport sslSupport = mock.mock(SSLSupport.class, "sslSupport");
    private final IExtendedRequest req = mock.mock(IExtendedRequest.class, "req");
    private final HttpServletResponse res = mock.mock(HttpServletResponse.class, "res");
    private final ReferrerURLCookieHandler referrerURLCookieHandler = mock.mock(ReferrerURLCookieHandler.class, "referrerURLCookieHandler");
    private final Cookie cookie1 = mock.mock(Cookie.class, "cookie1");
    private final Cookie cookie2 = mock.mock(Cookie.class, "cookie2");
    private final OidcClientUtil oidcClientUtil = mock.mock(OidcClientUtil.class, "oidcClientUtil");
    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    private final MockOidcClientRequest oidcClientRequest = mock.mock(MockOidcClientRequest.class, "oidcClientRequest");
    private final OidcClientConfig oidcClientConfig = mock.mock(OidcClientConfig.class, "oidcClientConfig");
    private final ConvergedClientConfig convClientConfig = mock.mock(ConvergedClientConfig.class, "convClientConfig");

    private final HashMap<String, String> tokens = new HashMap<String, String>(100);
    private OidcClientAuthenticator commonAuthn;

    @Before
    public void setUp() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);

        mock.checking(new Expectations() {
            {
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                allowing(webAppSecConfig).getSSODomainList();
                will(returnValue(null));
                allowing(webAppSecConfig).getSSOUseDomainFromURL();
                will(returnValue(false));
                allowing(webAppSecConfig).createSSOCookieHelper();
                will(returnValue(new SSOCookieHelperImpl(webAppSecConfig)));
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));

                allowing(req).getAttribute("com.ibm.wsspi.security.oidc.client.request");
                will(returnValue(oidcClientRequest));

                allowing(res).addCookie(cookie2);

                allowing(oidcClientRequest).getAndSetCustomCacheKeyValue();
                will(returnValue("ThisIsCustomCacheKeyvalue"));

                allowing(sslSupportRef).getService();
                will(returnValue(sslSupport));

                allowing(referrerURLCookieHandler).createCookie(with(any(String.class)), with(any(String.class)), with(any(HttpServletRequest.class)));
                will(returnValue(cookie2));

            }
        });

        commonAuthn = new OidcClientAuthenticator();
        tokens.clear();
    }

    @After
    public void tearDown() {
        //mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @Test
    public void testConstructor() {
        try {
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", commonAuthn);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void createJwkRetrieverConstructorExpectations(final OidcClientConfig clientConfig) {

        mock.checking(new Expectations() {
            {
                allowing(clientConfig).getSslRef();
                will(returnValue("sslRef"));
                allowing(clientConfig).getJwkEndpointUrl();
                will(returnValue(TEST_JWK_ENDPOINT));
                allowing(clientConfig).getJwkSet();
                will(returnValue(null));
                allowing(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(false));
                allowing(clientConfig).getJwkClientId();
                will(returnValue(null));
                allowing(clientConfig).getJwkClientSecret();
                will(returnValue(null));
            }
        });
    }

    private void createReferrerUrlCookieExpectations(final String cookieName) {
        mock.checking(new Expectations() {
            {
                allowing(req).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL)));
                allowing(cookie2).setMaxAge(0);
                allowing(cookie2).setMaxAge(-1);
                one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res,
                        cookieName);
            }
        });
    }

    @Test
    public void testHandleAuthorizationCodeFailure() {
        try {
            final String clientId = CLIENTID;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getClientId();
                    will(returnValue(clientId));
                }
            });

            // verifyResponseState
            final Cookie[] cookies = new Cookie[] {
                    cookie1
            };
            final String cookieName = OidcStorageUtils.getStateStorageKey(TEST_ORIGINAL_STATE);
            final String originalState = TEST_ORIGINAL_STATE;

            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getClientId();
                    will(returnValue(CLIENT01));
                    allowing(convClientConfig).getClockSkewInSeconds();
                    will(returnValue(300L));
                    allowing(convClientConfig).getAuthenticationTimeLimitInSeconds();
                    will(returnValue(420L));
                    one(convClientConfig).getId();
                    will(returnValue(CLIENT01));
                    one(convClientConfig).getClientSecret();
                    will(returnValue("clientsecret"));
                    one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res, "WASOidcStateKey");
                    one(req).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(cookieName));
                    one(cookie1).getValue();
                    will(returnValue("BadStateKey"));
                    allowing(oidcClientConfig).getSslRef();
                    will(returnValue("sslRef"));
                    allowing(oidcClientConfig).getJwkEndpointUrl();
                    will(returnValue(TEST_JWK_ENDPOINT));
                    allowing(oidcClientConfig).getJwkSet();
                    will(returnValue(null));
                    allowing(oidcClientConfig).isHostNameVerificationEnabled();
                    will(returnValue(false));
                    allowing(oidcClientConfig).getJwkClientId();
                    will(returnValue(null));
                    allowing(oidcClientConfig).getJwkClientSecret();
                    will(returnValue(null));
                }
            });

            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", oica);

            OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
            createReferrerUrlCookieExpectations(cookieName);
            oica.oidcClientUtil = oidcClientUtil;
            AuthorizationCodeHandler ach = new AuthorizationCodeHandler(req, res, convClientConfig, sslSupport);
            ProviderAuthenticationResult result = ach.handleAuthorizationCode(AUTHZ_CODE, originalState);
            assertNotNull("Ought to get a instance of ProviderAuthenticationResult", result);
            assertTrue("Expect to get an " + AuthResult.SEND_401 + " but a " + result.getStatus(),
                    AuthResult.SEND_401.equals(result.getStatus()));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testHandleAuthorizationCodeFailure2() {
        try {
            final String clientId = CLIENTID;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getClientId();
                    will(returnValue(clientId));
                }
            });

            // verifyResponseState
            final Cookie[] cookies = new Cookie[] {
                    cookie1
            };
            final String cookieName = OidcStorageUtils.getStateStorageKey(TEST_ORIGINAL_STATE);
            final String originalState = TEST_ORIGINAL_STATE;
            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getClientId();
                    will(returnValue(CLIENT01));
                    allowing(convClientConfig).getClockSkewInSeconds();
                    will(returnValue(300L));
                    allowing(convClientConfig).getAuthenticationTimeLimitInSeconds();
                    will(returnValue(420L));
                    one(convClientConfig).getId();
                    will(returnValue(CLIENT01));
                    one(convClientConfig).getClientSecret();
                    will(returnValue("clientsecret"));
                    one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res, "WASOidcStateKey");
                    one(req).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(cookieName));
                    one(cookie1).getValue();
                    will(returnValue("BadStateKey"));
                }
            });
            createJwkRetrieverConstructorExpectations(clientConfig);

            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", oica);

            OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
            createReferrerUrlCookieExpectations(cookieName);
            oica.oidcClientUtil = oidcClientUtil;
            AuthorizationCodeHandler ach = new AuthorizationCodeHandler(req, res, convClientConfig, sslSupport);
            ProviderAuthenticationResult result = ach.handleAuthorizationCode(AUTHZ_CODE, originalState);
            assertNotNull("Ought to get a instance of ProviderAuthenticationResult", result);
            assertTrue("Expect to get an " + AuthResult.SEND_401 + " but a " + result.getStatus(),
                    AuthResult.SEND_401.equals(result.getStatus()));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testHandleAuthorizationCodeHttpBad() {
        try {
            final String tokenUrl = "http://op.ibm.com:8010/oidc/endpoint/token";
            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getClientId();
                    will(returnValue(CLIENT01));
                    one(convClientConfig).getClientSecret();
                    will(returnValue("clientsecret"));
                    allowing(convClientConfig).getClockSkewInSeconds();
                    will(returnValue(300L));
                    allowing(convClientConfig).getAuthenticationTimeLimitInSeconds();
                    will(returnValue(420L));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(tokenUrl));
                    one(convClientConfig).isHttpsRequired();
                    will(returnValue(true));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(tokenUrl));
                }
            });
            OidcClientAuthenticator oica = new mockOidcClientAuthenticator();
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", oica);
            oica.oidcClientUtil = oidcClientUtil;
            AuthorizationCodeHandler ach = new AuthorizationCodeHandler(req, res, convClientConfig, sslSupport);
            ProviderAuthenticationResult result = ach.handleAuthorizationCode(AUTHZ_CODE, "orignalState");
            assertNotNull("Ought to get a instance of ProviderAuthenticationResult", result);
            assertTrue("Expect to get an " + AuthResult.SEND_401 + " but a " + result.getStatus(),
                    AuthResult.SEND_401.equals(result.getStatus()));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testHandleAuthorizationCode_RedirectURLNotHttps() {
        mock.checking(new Expectations() {
            {
                exactly(3).of(convClientConfig).getClientId();
                will(returnValue(CLIENTID));
                allowing(convClientConfig).getClockSkewInSeconds();
                will(returnValue(300L));
                allowing(convClientConfig).getAuthenticationTimeLimitInSeconds();
                will(returnValue(420L));
                one(convClientConfig).getId();
                will(returnValue(CLIENT01));
                one(convClientConfig).getClientSecret();
                will(returnValue("clientsecret"));
                one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res, "WASOidcStateKey");
                one(convClientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                one(convClientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(true));
                one(convClientConfig).getRedirectUrlWithJunctionPath(TEST_URL);
                will(returnValue(TEST_URL));
            }
        });

        createJwkRetrieverConstructorExpectations(clientConfig);
        AuthorizationCodeHandler ach = new AuthorizationCodeHandler(req, res, convClientConfig, sslSupport);
        ProviderAuthenticationResult oidcResult = ach.handleAuthorizationCode(AUTHZ_CODE, "orignalState");

        checkForBadStatusExpectations(oidcResult);
    }

    @Test
    public void testHandleAuthorizationCode_CatchSSLException() throws javax.net.ssl.SSLException {
        mock.checking(new Expectations() {
            {
                exactly(3).of(convClientConfig).getClientId();
                will(returnValue(CLIENTID));
                one(convClientConfig).getClientSecret();
                will(returnValue("clientsecret"));
                allowing(convClientConfig).getClockSkewInSeconds();
                will(returnValue(300L));
                allowing(convClientConfig).getAuthenticationTimeLimitInSeconds();
                will(returnValue(420L));
                one(convClientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                one(convClientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                one(convClientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_URL));
                one(convClientConfig).getSSLConfigurationName();
                will(returnValue(with(any(String.class))));
                one(sslSupport).getSSLSocketFactory((String) null);
                will(throwException(new javax.net.ssl.SSLException("bad factory")));
                one(convClientConfig).getRedirectUrlWithJunctionPath(TEST_URL);
                will(returnValue(TEST_URL));
            }
        });

        AuthorizationCodeHandler ach = new AuthorizationCodeHandler(req, res, convClientConfig, sslSupport);
        ProviderAuthenticationResult oidcResult = ach.handleAuthorizationCode(AUTHZ_CODE, "orignalState");

        checkForBadStatusExpectations(oidcResult);
    }

    private void checkForBadStatusExpectations(ProviderAuthenticationResult oidcResult) {
        assertEquals("Expected to receive status:" + AuthResult.SEND_401 + " but received:" + oidcResult.getStatus() + ".", AuthResult.SEND_401, oidcResult.getStatus());

        assertEquals("Expected to receive status code:" + HttpServletResponse.SC_UNAUTHORIZED + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_UNAUTHORIZED, oidcResult.getHttpStatusCode());
    }

    class MockInputStream extends InputStream {
        String strOut = null;
        int iCnt = 0;

        public MockInputStream(String strOut) {
            this.strOut = strOut;
        }

        @Override
        public int read() {
            if (iCnt < strOut.length()) {
                return strOut.charAt(iCnt++);
            } else {
                return -1;
            }
        }
    }

    class mockOidcClientUtil extends OidcClientUtil {
        IOException ioe = null;
        HttpException httpe = null;

        public mockOidcClientUtil(IOException e) {
            super();
            ioe = e;
        }

        public mockOidcClientUtil(HttpException e) {
            super();
            httpe = e;
        }

    }

    class mockOidcClientAuthenticator extends OidcClientAuthenticator {
        int iTest = 0;

        public mockOidcClientAuthenticator() {
            super();
        }

        public mockOidcClientAuthenticator(AtomicServiceReference<SSLSupport> sslSupportRef, int iTest) {
            super(sslSupportRef);
            this.iTest = iTest;
        }

    }

}
