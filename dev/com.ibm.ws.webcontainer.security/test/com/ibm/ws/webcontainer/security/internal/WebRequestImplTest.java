/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenHelperTestHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.WebRequestImpl;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;

import test.common.SharedOutputManager;

public class WebRequestImplTest {

    private static final String CUSTOM_SSO_NAME = "CustomSSOName";
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

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_none() {
        withAuthorizationHeader(null);
        withCertificates(null);
        withCookies(null);

        assertFalse("No authentication data is present so this should be false.", webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_basicOnly() {
        withAuthorizationHeader("Basic SomeAuthzHeader");

        assertTrue("BasicAuth authentication data is present so this should be true.", webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_emptyClientCertChain() {
        withAuthorizationHeader(null);
        final X509Certificate[] emptyCertChain = new X509Certificate[] {};
        withCertificates(emptyCertChain);
        withCookies(null);

        assertFalse("ClientCert authentication is empty so this should be false.", webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_clientCertOnly() {
        withAuthorizationHeader(null);
        withCertificates(certChain);

        assertTrue("ClientCert authentication data is present so this should be true.", webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_customSSOName() {
        withAuthorizationHeader(null);
        withCertificates(null);
        final Cookie[] customCookies = new Cookie[] { new Cookie(CUSTOM_SSO_NAME, "SomeSSOCookieValue") };
        withCookies(customCookies);
        withCookieName(CUSTOM_SSO_NAME);

        assertTrue("SSO authentication data is present so this should be true.", webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_onlyDefaultSSO() {
        withAuthorizationHeader(null);
        withCertificates(null);
        withCookies(cookies);
        withCookieName(CUSTOM_SSO_NAME);
        withCustomCookieNameOnly(false);

        assertTrue("SSO authentication data is present so this should be true.", webRequest.hasAuthenticationData());
    }

    @Test
    public void hasAuthenticationData_onlyCustomCookieNameSSO() {
        withAuthorizationHeader(null);
        withCertificates(null);
        withCookies(cookies);
        withCookieName(CUSTOM_SSO_NAME);
        withCustomCookieNameOnly(true);

        assertFalse("No usable authentication data is present so this should be false.", webRequest.hasAuthenticationData());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebRequestImpl#hasAuthenticationData()}.
     */
    @Test
    public void hasAuthenticationData_ssoOnly() {
        withAuthorizationHeader(null);
        withCertificates(null);
        withCookies(cookies);
        withCookieName(SSOAuthenticator.DEFAULT_SSO_COOKIE_NAME);

        assertTrue("SSO authentication data is present so this should be true.", webRequest.hasAuthenticationData());
    }

    @Test
    public void hasAuthenticationData_JWT() {
        withAuthorizationHeader(null);
        withCertificates(null);
        JwtSSOTokenHelperTestHelper jwtSSOTokenHelperTestHelper = new JwtSSOTokenHelperTestHelper(mock);
        try {
            jwtSSOTokenHelperTestHelper.setJwtSSOTokenProxyWithCookieName("jwtCookieName");
            withCookies(new Cookie[] { new Cookie("jwtCookieName", "") });

            assertTrue("The JWT cookie must be found in the request.", webRequest.hasAuthenticationData());
        } finally {
            jwtSSOTokenHelperTestHelper.tearDown();
        }
    }

    @Test
    public void hasAuthenticationData_Bearer() {
        withCertificates(null);
        withCookies(new Cookie[] {});
        withAuthorizationHeader("Bearer value");

        assertTrue("The Bearer token must be found in the request.", webRequest.hasAuthenticationData());
    }

    private void withAuthorizationHeader(final String header) {
        mock.checking(new Expectations() {
            {
                allowing(req).getHeader("Authorization");
                will(returnValue(header));
            }
        });
    }

    private void withCertificates(final X509Certificate[] certs) {
        mock.checking(new Expectations() {
            {
                one(metadata).getLoginConfiguration();
                will(returnValue(loginConfig));
                one(loginConfig).getAuthenticationMethod();
                will(returnValue("CLIENT_CERT"));
                one(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(certs));
            }
        });
    }

    private void withCookies(final Cookie[] cookies) {
        mock.checking(new Expectations() {
            {
                allowing(req).getCookies();
                will(returnValue(cookies));
            }
        });
    }

    private void withCookieName(final String name) {
        mock.checking(new Expectations() {
            {
                one(config).getSSOCookieName();
                will(returnValue(name));
            }
        });
    }

    private void withCustomCookieNameOnly(final boolean value) {
        mock.checking(new Expectations() {
            {
                one(config).isUseOnlyCustomCookieName();
                will(returnValue(value));
            }
        });
    }

}
