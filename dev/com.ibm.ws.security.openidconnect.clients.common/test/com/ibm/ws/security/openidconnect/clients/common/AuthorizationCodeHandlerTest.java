/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.net.ssl.SSLSocketFactory;
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

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import test.common.SharedOutputManager;

public class AuthorizationCodeHandlerTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String TEST_ORIGINAL_STATE = "orignalStateThatIsAtLeastAsLongAsRequired";
    private static final String TEST_URL = "http://harmonic.austin.ibm.com:8010/formlogin/SimpleServlet";
    private static final String CLIENTID = "clientid";
    private static final String CLIENT01 = "client01";
    private static final String AUTHZ_CODE = "authorizaCodeAAA";

    @SuppressWarnings("unchecked")
    private final AtomicServiceReference<SSLSupport> sslSupportRef = mock.mock(AtomicServiceReference.class, "sslSupportRef");
    private final SSLSupport sslSupport = mock.mock(SSLSupport.class, "sslSupport");
    private final IExtendedRequest req = mock.mock(IExtendedRequest.class, "req");
    private final HttpServletResponse res = mock.mock(HttpServletResponse.class, "res");
    private final ReferrerURLCookieHandler referrerURLCookieHandler = mock.mock(ReferrerURLCookieHandler.class, "referrerURLCookieHandler");
    private final Cookie cookie2 = mock.mock(Cookie.class, "cookie2");
    private final OidcClientUtil oidcClientUtil = mock.mock(OidcClientUtil.class, "oidcClientUtil");
    private final OIDCClientAuthenticatorUtil oidcClientAuthUtil = mock.mock(OIDCClientAuthenticatorUtil.class);
    private final Jose4jUtil jose4jUtil = mock.mock(Jose4jUtil.class);
    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    private final MockOidcClientRequest oidcClientRequest = mock.mock(MockOidcClientRequest.class, "oidcClientRequest");
    private final ConvergedClientConfig convClientConfig = mock.mock(ConvergedClientConfig.class, "convClientConfig");

    AuthorizationCodeHandler ach = new SimpleMockAuthorizationCodeHandler(sslSupport);

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
                allowing(convClientConfig).getUseSystemPropertiesForHttpClientConnections();
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

        ach = new SimpleMockAuthorizationCodeHandler(sslSupport);

        //        createConstructorExpectations(convClientConfig);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    //    private void createConstructorExpectations(final ConvergedClientConfig clientConfig) {
    //        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
    //    }

    @Test
    public void testHandleAuthorizationCodeFailure() {
        try {
            final String originalState = TEST_ORIGINAL_STATE;

            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getClientId();
                    will(returnValue(CLIENT01));
                    one(oidcClientAuthUtil).verifyResponseState(req, res, originalState, convClientConfig);
                    will(returnValue(new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED)));
                }
            });

            AuthorizationCodeHandler ach = new SimpleMockAuthorizationCodeHandler(sslSupport);
            ProviderAuthenticationResult result = ach.handleAuthorizationCode(req, res, AUTHZ_CODE, originalState, convClientConfig);
            checkForBadStatusExpectations(result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testHandleAuthorizationCodeHttpBad() {
        try {
            final String originalState = TEST_ORIGINAL_STATE;
            final String tokenUrl = "http://op.ibm.com:8010/oidc/endpoint/token";
            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getClientId();
                    will(returnValue(CLIENT01));
                    one(oidcClientAuthUtil).verifyResponseState(req, res, originalState, convClientConfig);
                    will(returnValue(null));
                    allowing(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(tokenUrl));
                    one(convClientConfig).isHttpsRequired();
                    will(returnValue(true));
                }
            });

            AuthorizationCodeHandler ach = new SimpleMockAuthorizationCodeHandler(sslSupport);
            ProviderAuthenticationResult result = ach.handleAuthorizationCode(req, res,
                    AUTHZ_CODE, //"authorizaCodeAAA",
                    originalState,
                    convClientConfig);
            checkForBadStatusExpectations(result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testHandleAuthorizationCode_RedirectURLNotHttps() {
        final String originalState = TEST_ORIGINAL_STATE;
        mock.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENTID));
                one(oidcClientAuthUtil).verifyResponseState(req, res, originalState, convClientConfig);
                will(returnValue(null));
                one(convClientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                one(oidcClientAuthUtil).setRedirectUrlIfNotDefined(req, convClientConfig);
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(true));
            }
        });

        AuthorizationCodeHandler ach = new SimpleMockAuthorizationCodeHandler(sslSupport);
        ProviderAuthenticationResult oidcResult = ach.handleAuthorizationCode(req, res, AUTHZ_CODE, originalState, convClientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    @Test
    public void testHandleAuthorizationCode_CatchSSLException() throws javax.net.ssl.SSLException {
        final String originalState = TEST_ORIGINAL_STATE;
        final String sslConfigName = "mySslConfig";
        mock.checking(new Expectations() {
            {
                allowing(convClientConfig).getClientId();
                will(returnValue(CLIENTID));
                one(oidcClientAuthUtil).verifyResponseState(req, res, originalState, convClientConfig);
                will(returnValue(null));
                allowing(convClientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                one(oidcClientAuthUtil).setRedirectUrlIfNotDefined(req, convClientConfig);
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                one(convClientConfig).getSSLConfigurationName();
                will(returnValue(sslConfigName));
                one(sslSupport).getSSLSocketFactory(sslConfigName);
                will(throwException(new javax.net.ssl.SSLException("bad factory")));
            }
        });

        AuthorizationCodeHandler ach = new SimpleMockAuthorizationCodeHandler(sslSupport);
        ProviderAuthenticationResult oidcResult = ach.handleAuthorizationCode(req, res, AUTHZ_CODE, originalState, convClientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    private void checkForBadStatusExpectations(ProviderAuthenticationResult oidcResult) {
        assertNotNull("ProviderAuthenticationResult was null but should not have been.", oidcResult);
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

        @Override
        public HashMap<String, String> getTokensFromAuthzCode(String tokenEnpoint,
                String clientId,
                @Sensitive String clientSecret,
                String redirectUri,
                String code,
                String grantType,
                SSLSocketFactory sslSocketFactory,
                boolean b,
                String authMethod,
                String resources,
                HashMap<String, String> customParams,
                boolean useJvmProps) throws HttpException, IOException {
            if (ioe != null) {
                throw ioe;
            }
            if (httpe != null) {
                throw httpe;
            }
            return new HashMap<String, String>();
        }

    }

    class SimpleMockAuthorizationCodeHandler extends AuthorizationCodeHandler {
        public SimpleMockAuthorizationCodeHandler(SSLSupport sslsupt) {
            super(sslsupt);
        }

        @Override
        protected OidcClientUtil getOidcClientUtil() {
            return oidcClientUtil;
        }

        @Override
        protected OIDCClientAuthenticatorUtil getOIDCClientAuthenticatorUtil() {
            return oidcClientAuthUtil;
        }

        @Override
        protected Jose4jUtil getJose4jUtil(SSLSupport sslSupport) {
            return jose4jUtil;
        }
    }

}
