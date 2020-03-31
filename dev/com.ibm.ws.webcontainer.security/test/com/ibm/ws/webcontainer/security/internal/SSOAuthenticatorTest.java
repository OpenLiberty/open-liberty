/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;

public class SSOAuthenticatorTest {

    private final Mockery mock = new JUnit4Mockery();
    private final AuthenticationService authService = mock.mock(AuthenticationService.class);
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class);
    private final WebRequest webRequest = mock.mock(WebRequest.class);
    private final SecurityMetadata smd = mock.mock(SecurityMetadata.class);
    private final LoginConfiguration lcfg = mock.mock(LoginConfiguration.class);
    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    private final SSOCookieHelper ssoCookieHelper = mock.mock(SSOCookieHelper.class);

    private final String cookieValue = "4TUZR3aU8II+cWveDgIB7ffDQZaKxx1VKlUBW7KsLa2AQjiB6RWBoNuoH+OIUbtntMIsS2956ZvdzSshAuPNuk7y30BhN00WclWtMY6AD7je2aecQxsGNrV/ogCAOip9EobBue4N1zU8S7yD1jEajykfN8Eo2rIqnMK/DraTV65gmlE378VS3Wy6IFHmZm9BBlaSNqPLBkyJ1Xh98PACMr8f/bF290AD75nGrrB0oXODaeoA85/hpiHpvxSNCFx+P3QDvRly5Bb16SQRhHmUhX0uegAdURKAaeX3gmu8zXQ=";
    private final Cookie cookie = new Cookie("LTPAToken2", cookieValue);
    private final Cookie[] cookieArray = { cookie };
    private SSOAuthenticator ssoAuth;

    /**
     * Common set of expectations shared by all the test methods
     *
     */
    @Before
    public void setup() {

        mock.checking(new Expectations() {
            {
                allowing(smd).getLoginConfiguration();
                will(returnValue(lcfg));
                allowing(lcfg).getAuthenticationMethod();
                will(returnValue(LoginConfiguration.FORM));

                allowing(webRequest).getHttpServletRequest();
                will(returnValue(req));
                allowing(webRequest).getHttpServletResponse();
                will(returnValue(resp));
                allowing(req).getHeader("Authorization");
                will(returnValue(null));
                allowing(req).getMethod();
                will(returnValue("GET"));
                allowing(webAppSecConfig).getSameSiteCookie();
                will(returnValue("Disabled"));
            }
        });

        ssoAuth = new SSOAuthenticator(authService, smd, webAppSecConfig, ssoCookieHelper);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    @Test
    public void constructorNullSecurityMetadata() {
        assertNotNull(new SSOAuthenticator(authService, null, webAppSecConfig, ssoCookieHelper));
    }

    /**
     * Tests that handleSSO() will return null for
     * AuthenticationResult when no Cookie exists
     *
     * @throws Exception
     */
    @Test
    public void authenticate_NoCookies() throws Exception {

        mock.checking(new Expectations() {
            {
                allowing(req).getCookies();
                will(returnValue(null));
            }
        });

        AuthenticationResult authResult = ssoAuth.authenticate(webRequest);
        assertNull("AuthenticationResult should be null", authResult);
    }

    /**
     * If we have some cookies, and we are set to logout on session expire,
     * and the session has expired, create the logout cookies.
     */
    @Test
    public void authenticate_createLogoutCookiesOnInvalidSession() {

        mock.checking(new Expectations() {
            {
                allowing(req).getCookies();
                will(returnValue(cookieArray));
                one(webAppSecConfig).getLogoutOnHttpSessionExpire();
                will(returnValue(true));
                one(req).getRequestedSessionId();
                will(returnValue("SessionID"));
                one(req).isRequestedSessionIdValid();
                will(returnValue(false));

                one(ssoCookieHelper).createLogoutCookies(req, resp);
            }
        });

        AuthenticationResult authResult = ssoAuth.authenticate(webRequest);
        assertNull("AuthenticationResult should be null", authResult);
    }

    /**
     * Tests that handleSSO() will return null for
     * AuthenticationResult when no Cookie exists
     *
     * @throws Exception
     */
    @Test
    public void authenticate_NoSSOCookies() throws Exception {
        final Cookie cookie = new Cookie("NotAnSSOCookie", "invalidToken");
        final Cookie[] cookieArray = { cookie };

        mock.checking(new Expectations() {
            {
                allowing(req).getCookies();
                will(returnValue(cookieArray));

                allowing(webAppSecConfig).getLogoutOnHttpSessionExpire();
                will(returnValue(false));

                one(ssoCookieHelper).getSSOCookiename();
                will(returnValue("LTPAToken2"));

                one(webAppSecConfig).isUseOnlyCustomCookieName();
                will(returnValue(false));

                one(ssoCookieHelper).createLogoutCookies(req, resp);
            }
        });

        AuthenticationResult authResult = ssoAuth.authenticate(webRequest);
        assertNull("AuthenticationResult should be null", authResult);
    }

    /**
     * Tests that handleSSO() will return null for AuthenticationResult
     * when the HTTP session is invalid.
     *
     * @throws Exception
     */
    @Test
    public void authenticate_EmptyCookieValue() throws Exception {
        final Cookie emptyValueCookie = new Cookie("LTPAToken2", "");
        final Cookie[] cookieArray = { emptyValueCookie };
        mock.checking(new Expectations() {
            {
                allowing(req).getCookies();
                will(returnValue(cookieArray));

                allowing(webAppSecConfig).getLogoutOnHttpSessionExpire();
                will(returnValue(false));

                one(ssoCookieHelper).getSSOCookiename();
                will(returnValue("LTPAToken2"));

                one(webAppSecConfig).isUseOnlyCustomCookieName();
                will(returnValue(false));

                one(ssoCookieHelper).createLogoutCookies(req, resp);
            }
        });

        AuthenticationResult authResult = ssoAuth.authenticate(webRequest);
        mock.assertIsSatisfied();
        assertEquals("AuthenticationResult should be null", null, authResult);
    }

    /**
     * Tests that handleSSO() will return null for AuthenticationResult
     * when the HTTP session is invalid.
     *
     * @throws Exception
     */
    @Test
    public void authenticate_InvalidCookieValue() throws Exception {
        final Cookie invalidValueCookie = new Cookie("LTPAToken2", "invalidCookie");
        final Cookie[] cookieArray = { invalidValueCookie };
        mock.checking(new Expectations() {
            {
                allowing(req).getCookies();
                will(returnValue(cookieArray));

                allowing(webAppSecConfig).getLogoutOnHttpSessionExpire();
                will(returnValue(false));

                allowing(webAppSecConfig).isTrackLoggedOutSSOCookiesEnabled();
                will(returnValue(false));

                one(ssoCookieHelper).getSSOCookiename();
                will(returnValue("LTPAToken2"));

                one(webAppSecConfig).isUseOnlyCustomCookieName();
                will(returnValue(false));

                one(ssoCookieHelper).createLogoutCookies(req, resp);

                one(authService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(any(AuthenticationData.class)), with(equal((Subject) null)));
                will(throwException(new AuthenticationException("Invalid LTPAToken")));

            }
        });

        AuthenticationResult authResult = ssoAuth.authenticate(webRequest);
        assertEquals("AuthenticationResult should be null", null, authResult);
    }

    /**
     * Tests handleSSO() will use the ssoCookieName Cookie
     * to authenticate successfully with AuthenticationService.authenticate()
     *
     * @throws Exception
     */
    @Test
    public void authenticate_WithCookie() throws Exception {
        final Cookie[] cookieArray = { cookie };
        final Subject authSubject = new Subject();

        mock.checking(new Expectations() {
            {
                allowing(req).getCookies();
                will(returnValue(cookieArray));

                allowing(webAppSecConfig).getLogoutOnHttpSessionExpire();
                will(returnValue(false));

                allowing(webAppSecConfig).isTrackLoggedOutSSOCookiesEnabled();
                will(returnValue(false));

                allowing(ssoCookieHelper).getSSOCookiename();
                will(returnValue("LTPAToken2"));

                one(webAppSecConfig).isUseOnlyCustomCookieName();
                will(returnValue(false));

                allowing(ssoCookieHelper).addJwtSsoCookiesToResponse(authSubject, req, resp);

                // Now authenticate, which should be successful and the result
                // immediately returned
                one(authService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(any(AuthenticationData.class)), with(equal((Subject) null)));
                will(returnValue(authSubject));
            }
        });

        AuthenticationResult authResult = ssoAuth.authenticate(webRequest, webAppSecConfig);
        assertEquals("AuthenticationResult should be SUCCESS",
                     AuthResult.SUCCESS, authResult.getStatus());
    }

    /**
     * Tests handleSSO() will use the ssoCookieName Cookie
     * to authenticate successfully with AuthenticationService.authenticate()
     *
     * @throws Exception
     */
    @Test
    public void authenticate_authenticationFails() throws Exception {
        final Cookie[] cookieArray = { cookie };

        mock.checking(new Expectations() {
            {
                allowing(req).getCookies();
                will(returnValue(cookieArray));

                allowing(webAppSecConfig).getLogoutOnHttpSessionExpire();
                will(returnValue(false));

                allowing(webAppSecConfig).isTrackLoggedOutSSOCookiesEnabled();
                will(returnValue(false));

                one(ssoCookieHelper).getSSOCookiename();
                will(returnValue("LTPAToken2"));

                one(webAppSecConfig).isUseOnlyCustomCookieName();
                will(returnValue(false));

                // Now authenticate, which should be successful and the result
                // immediately returned
                one(authService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(any(AuthenticationData.class)), with(equal((Subject) null)));
                will(throwException(new AuthenticationException("Authentication failed")));

                one(ssoCookieHelper).createLogoutCookies(req, resp);
            }
        });

        AuthenticationResult authResult = ssoAuth.authenticate(webRequest, webAppSecConfig);
        assertNull("Should return null when authentication fails", authResult);
    }

}
