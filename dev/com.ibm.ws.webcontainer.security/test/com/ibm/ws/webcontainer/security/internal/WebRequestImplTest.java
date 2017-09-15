/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.cert.X509Certificate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.WebRequestImpl;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;

/**
 *
 */
public class WebRequestImplTest {
    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class);
    private final SecurityMetadata metadata = mock.mock(SecurityMetadata.class);
    private final WebAppSecurityConfig config = mock.mock(WebAppSecurityConfig.class);
    private final X509Certificate cert = mock.mock(X509Certificate.class);
    private final X509Certificate[] certChain = new X509Certificate[] { cert };
    private final LoginConfiguration loginConfig = mock.mock(LoginConfiguration.class);
    private final Cookie[] cookies = new Cookie[] { new Cookie(SSOAuthenticator.DEFAULT_SSO_COOKIE_NAME, "SomeSSOCookieValue") };
    private WebRequest webRequest;

    @Before
    public void setUp() {
        webRequest = new WebRequestImpl(req, resp, metadata, config);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_none() {
        mock.checking(new Expectations() {
            {
                one(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue(null));
                one(metadata).getLoginConfiguration();
                will(returnValue(loginConfig));
                one(loginConfig).getAuthenticationMethod();
                will(returnValue("CLIENT_CERT"));
                one(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(null));
                one(req).getCookies();
                will(returnValue(null));
            }
        });
        assertFalse("No authentication data is present so this should be false",
                    webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_basicOnly() {
        mock.checking(new Expectations() {
            {
                one(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue("SomeAuthzHeader"));
                one(metadata).getLoginConfiguration();
                will(returnValue(loginConfig));
                one(loginConfig).getAuthenticationMethod();
                will(returnValue("CLIENT_CERT"));
                one(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(null));
                one(req).getCookies();
                will(returnValue(null));
            }
        });
        assertTrue("BasicAuth authentication data is present so this should be true",
                   webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_emptyClientCertChain() {
        final X509Certificate[] emptyCertChain = new X509Certificate[] {};
        mock.checking(new Expectations() {
            {
                one(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue(null));
                one(metadata).getLoginConfiguration();
                will(returnValue(loginConfig));
                one(loginConfig).getAuthenticationMethod();
                will(returnValue("CLIENT_CERT"));
                one(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(emptyCertChain));
                one(req).getCookies();
                will(returnValue(null));
            }
        });
        assertFalse("ClientCert authentication is empty so this should be false",
                    webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_clientCertOnly() {
        mock.checking(new Expectations() {
            {
                one(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue(null));
                one(metadata).getLoginConfiguration();
                will(returnValue(loginConfig));
                one(loginConfig).getAuthenticationMethod();
                will(returnValue("CLIENT_CERT"));
                one(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(certChain));
                one(req).getCookies();
                will(returnValue(null));
            }
        });
        assertTrue("ClientCert authentication data is present so this should be true",
                   webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_customSSOName() {
        final Cookie[] customCookies = new Cookie[] { new Cookie("CustomSSOName", "SomeSSOCookieValue") };
        mock.checking(new Expectations() {
            {
                one(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue(null));
                one(metadata).getLoginConfiguration();
                will(returnValue(loginConfig));
                one(loginConfig).getAuthenticationMethod();
                will(returnValue("CLIENT_CERT"));
                one(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(null));
                one(req).getCookies();
                will(returnValue(customCookies));
                one(config).getSSOCookieName();
                will(returnValue("CustomSSOName"));
            }
        });
        assertTrue("SSO authentication data is present so this should be true",
                   webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_onlyDefaultSSO() {
        mock.checking(new Expectations() {
            {
                one(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue(null));
                one(metadata).getLoginConfiguration();
                will(returnValue(loginConfig));
                one(loginConfig).getAuthenticationMethod();
                will(returnValue("CLIENT_CERT"));
                one(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(null));
                one(req).getCookies();
                will(returnValue(cookies));
                one(config).getSSOCookieName();
                will(returnValue("CustomSSOName"));
            }
        });
        assertTrue("SSO authentication data is present so this should be true",
                   webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_ssoOnly() {
        mock.checking(new Expectations() {
            {
                one(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue(null));
                one(metadata).getLoginConfiguration();
                will(returnValue(loginConfig));
                one(loginConfig).getAuthenticationMethod();
                will(returnValue("CLIENT_CERT"));
                one(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(null));
                one(req).getCookies();
                will(returnValue(cookies));
                one(config).getSSOCookieName();
                will(returnValue(SSOAuthenticator.DEFAULT_SSO_COOKIE_NAME));
            }
        });
        assertTrue("SSO authentication data is present so this should be true",
                   webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_basicAndSSO() {
        mock.checking(new Expectations() {
            {
                one(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue("SomeAuthzHeader"));
                one(metadata).getLoginConfiguration();
                will(returnValue(loginConfig));
                one(loginConfig).getAuthenticationMethod();
                will(returnValue("CLIENT_CERT"));
                one(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(null));
                one(req).getCookies();
                will(returnValue(cookies));
                one(config).getSSOCookieName();
                will(returnValue(SSOAuthenticator.DEFAULT_SSO_COOKIE_NAME));
            }
        });
        assertTrue("BasicAuth and SSO authentication data is present so this should be true",
                   webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_clientCertAndSSO() {
        mock.checking(new Expectations() {
            {
                one(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue(null));
                one(metadata).getLoginConfiguration();
                will(returnValue(loginConfig));
                one(loginConfig).getAuthenticationMethod();
                will(returnValue("CLIENT_CERT"));
                one(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(certChain));
                one(req).getCookies();
                will(returnValue(cookies));
                one(config).getSSOCookieName();
                will(returnValue(SSOAuthenticator.DEFAULT_SSO_COOKIE_NAME));
            }
        });
        assertTrue("ClientCert and SSO authentication data is present so this should be true",
                   webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_all() {
        mock.checking(new Expectations() {
            {
                one(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue("SomeAuthzHeader"));
                one(metadata).getLoginConfiguration();
                will(returnValue(loginConfig));
                one(loginConfig).getAuthenticationMethod();
                will(returnValue("CLIENT_CERT"));
                one(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(certChain));
                one(req).getCookies();
                will(returnValue(cookies));
                one(config).getSSOCookieName();
                will(returnValue(SSOAuthenticator.DEFAULT_SSO_COOKIE_NAME));
            }
        });
        assertTrue("All authentication data is present so this should be true",
                   webRequest.hasAuthenticationData());
    }
}
