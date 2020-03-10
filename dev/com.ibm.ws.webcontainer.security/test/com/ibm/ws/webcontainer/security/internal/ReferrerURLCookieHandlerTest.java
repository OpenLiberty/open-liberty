/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

@RunWith(JMock.class)
public class ReferrerURLCookieHandlerTest {
    private final Mockery context = new JUnit4Mockery();
    private final HttpServletRequest req = context.mock(HttpServletRequest.class);
    private final HttpServletResponse res = context.mock(HttpServletResponse.class);
    private final WebAppSecurityConfig webAppSecConfig = context.mock(WebAppSecurityConfig.class);
    private Cookie invalidatedReferrerURLCookie;
    private Cookie HttpOnlyTrueSecureTrueCookie;
    private Cookie HttpOnlyFalseSecureTrueCookie;
    private Cookie HttpOnlyTrueSecureFalseCookie;
    private Cookie appPathCookie;
    private Cookie slashPathCookie;
    private ReferrerURLCookieHandler handler;
    String appPath = "/myApp";
    String slashPath = "/";

    @Before
    public void setUp() {
        invalidatedReferrerURLCookie = new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, "");
        invalidatedReferrerURLCookie.setPath("/");
        invalidatedReferrerURLCookie.setMaxAge(0);
        HttpOnlyTrueSecureTrueCookie = new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, "");
        HttpOnlyTrueSecureTrueCookie.setPath("/");
        HttpOnlyTrueSecureTrueCookie.setMaxAge(0);
        HttpOnlyTrueSecureTrueCookie.setSecure(true);
        HttpOnlyTrueSecureTrueCookie.setHttpOnly(true);
        HttpOnlyFalseSecureTrueCookie = new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, "");
        HttpOnlyFalseSecureTrueCookie.setPath("/");
        HttpOnlyFalseSecureTrueCookie.setMaxAge(0);
        HttpOnlyFalseSecureTrueCookie.setSecure(true);
        HttpOnlyFalseSecureTrueCookie.setHttpOnly(false);
        HttpOnlyTrueSecureFalseCookie = new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, "");
        HttpOnlyTrueSecureFalseCookie.setPath("/");
        HttpOnlyTrueSecureFalseCookie.setMaxAge(0);
        HttpOnlyTrueSecureFalseCookie.setSecure(false);
        HttpOnlyTrueSecureFalseCookie.setHttpOnly(true);
        appPathCookie = new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, "");
        appPathCookie.setPath(appPath);
        appPathCookie.setMaxAge(0);
        appPathCookie.setSecure(true);
        appPathCookie.setHttpOnly(true);
        slashPathCookie = new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, "");
        slashPathCookie.setPath(slashPath);
        slashPathCookie.setMaxAge(0);

        handler = new ReferrerURLCookieHandler(webAppSecConfig);
    }

    @Test
    /**
     * getPathNameTest_appContext will make sure ReferrerURLCookieHandler.getPathName
     * will return the appContext when WebAppSecConfig.isIncludePathInWASReqURL()
     * returns true.
     */
    public void getPathNameTest_appContext() {
        context.checking(new Expectations() {
            {
                one(webAppSecConfig).isIncludePathInWASReqURL();
                will(returnValue(true));
                one(req).getContextPath();
                will(returnValue(appPath));
            }
        });
        assertEquals(appPath, handler.getPathName(req));
    }

    @Test
    /**
     * getPathNameTest_slashContext will make sure ReferrerURLCookieHandler.getPathName
     * will return the "/" when WebAppSecConfig.isIncludePathInWASReqURL()
     * returns false.
     */
    public void getPathNameTest_slashContext() {
        context.checking(new Expectations() {
            {
                one(webAppSecConfig).isIncludePathInWASReqURL();
                will(returnValue(false));
            }
        });
        assertEquals(slashPath, handler.getPathName(req));
    }

    @Test
    /**
     * getPathNameTest_slashContext will make sure ReferrerURLCookieHandler.getPathName
     * will return the "/" when WebAppSecConfig.isIncludePathInWASReqURL()
     * returns true.
     */
    public void getPathNameTest_slashContext_includePathTrue() {
        context.checking(new Expectations() {
            {
                one(webAppSecConfig).isIncludePathInWASReqURL();
                will(returnValue(true));
                one(req).getContextPath();
                will(returnValue(null));
            }
        });
        assertEquals(slashPath, handler.getPathName(req));
    }

    /**
     * getReferrerURLFromCookies shall return null if there is no
     * REFERRER_URL cookie.
     */
    @Test
    public void getReferrerURLFromCookies_noCookie() {
        final Cookie[] cookies = new Cookie[0];
        context.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
            }
        });
        assertNull(handler.getReferrerURLFromCookies(req, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME));
    }

    /**
     * getReferrerURLFromCookies shall return the decoded, host restored
     * URL specified by the REFERRER_URL_COOKIENAME.
     */
    @Test
    public void getReferrerURLFromCookies_encodedWithoutHost() {
        String expected = "http://site.com:80/page;/subpage,/more%.html";
        final String encoded = "http://:80/page%3B/subpage%2C/more%25.html";
        final StringBuffer currentURL = new StringBuffer("http://site.com/page");
        final Cookie[] cookies = new Cookie[] {
                                                new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, encoded)
        };
        context.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                one(req).getRequestURL();
                will(returnValue(currentURL));
            }
        });

        assertEquals(expected, handler.getReferrerURLFromCookies(req, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME));
    }

    /**
     * Get the first (and only) Cookie out of the AuthenticationResult
     * and ensure it has the correct properties and URL String.
     *
     * @param authResult
     * @param url
     */
    private void validateCookieInAuthResult(AuthenticationResult authResult, String url) {
        Cookie c = authResult.getCookies().get(0); // There can be only one!
        assertEquals(-1, c.getMaxAge());
        assertEquals("/", c.getPath());
        assertEquals(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, c.getName());
        assertEquals(url, c.getValue());
    }

    /**
     * setReferrerURLCookie shall set the specified URL String in a Cookie,
     * and set the Cookie into the AuthenticationResult.
     * If PRESERVE_FULLY_QUALIFIED_REFERRER_URL is set, the URL String will
     * not be modified.
     */
    @Test
    public void setReferrerURLCookie_PRESERVE_FULLY_QUALIFIED_REFERRER_URL_true() {
        context.checking(new Expectations() {
            {
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webAppSecConfig).getPreserveFullyQualifiedReferrerUrl();
                will(returnValue(true));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getSameSiteCookie();
                will(returnValue("Disabled"));
            }
        });
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, (Subject) null);
        String url = "http://site.com:80/page";
        Cookie cookie = handler.createReferrerURLCookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, url, req);
        authResult.setCookie(cookie);
        validateCookieInAuthResult(authResult, url);
    }

    /**
     * setReferrerURLCookie shall set the specified URL String in a Cookie,
     * and set the Cookie into the AuthenticationResult.
     * If PRESERVE_FULLY_QUALIFIED_REFERRER_URL is not set, or set to false,
     * the URL String will be modified to remove the host name.
     */
    @Test
    public void setReferrerURLCookie_PRESERVE_FULLY_QUALIFIED_REFERRER_URL_false() {
        context.checking(new Expectations() {
            {
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webAppSecConfig).getPreserveFullyQualifiedReferrerUrl();
                will(returnValue(false));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getSameSiteCookie();
                will(returnValue("Disabled"));
            }
        });
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, (Subject) null);
        String url = "http://site.com:80/page";
        String urlWithoutHost = "http://:80/page";

        Cookie cookie = handler.createReferrerURLCookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, url, req);
        authResult.setCookie(cookie);

        validateCookieInAuthResult(authResult, urlWithoutHost);
    }

    /**
     * clearReferrerURLCookie shall take no action if the WASReqURL Cookie
     * is not set for the HttpServletRequest's cookies.
     */
    @Test
    public void clearReferrerURLCookie_notSet() {
        context.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(null));
                never(res).addCookie(with(any(Cookie.class)));
            }
        });
        handler.clearReferrerURLCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);

    }

    /*
     * We are testing formlogin() scenarios. The WASReqURL hostname must match the current request hostname or be included in the
     * wasReqURLRedirectDomainNames property.
     * Test Scenario: Hostnames match
     */
    @Test
    public void isReferrerHostValid_URLHostnameMatch() throws RuntimeException {
        String currentReqURL = "http://goodhost.my.com:9080/j_security_check";
        String savedWASReqURL = "http://goodhost.my.com/protectedPage.jsp";
        List<String> domainList = Arrays.asList("ok.com", "notok.com");
        ReferrerURLCookieHandler.isReferrerHostValid(currentReqURL, savedWASReqURL, domainList);
    }

    /*
     * We are testing formlogin() scenarios. The WASReqURL hostname must match the current request hostname or be included in the
     * wasReqURLRedirectDomainNames property.
     * Test Scenario: Hostnames do not match. Nor does the current request hostname match domains in wasReqURLRedirectDomainNames property.
     */
    @Test(expected = RuntimeException.class)
    public void isReferrerHostValid_NoURLHostnameMatch() throws RuntimeException {
        String currentReqURL = "http://goodhost.my.com/j_security_check";
        String savedWASReqURL = "http://badhost.my.com/protectedPage.jsp";
        List<String> domainList = Arrays.asList("ok.com", "notok.com");
        ReferrerURLCookieHandler.isReferrerHostValid(currentReqURL, savedWASReqURL, domainList);
    }

    /*
     * We are testing formlogin() scenarios. The WASReqURL hostname must match the current request hostname or be included in the
     * wasReqURLRedirectDomainNames property.
     * Test Scenario: Hostnames do not match. The current request hostname does match domains in wasReqURLRedirectDomainNames property.
     */
    @Test
    public void isReferrerHostValid_DomainHostnameMatch() throws RuntimeException {
        String currentReqURL = "http://goodhost.my.com/j_security_check";
        String savedWASReqURL = "http://badhost.badhost.com/protectedPage.jsp";
        List<String> domainList = Arrays.asList("ok.com", "badhost.com", "notok.com");
        ReferrerURLCookieHandler.isReferrerHostValid(currentReqURL, savedWASReqURL, domainList);
    }

    /*
     * We are testing formlogin() scenarios. The WASReqURL hostname must match the current request hostname or be included in the
     * wasReqURLRedirectDomainNames property.
     * Test Scenario: Hostnames do not match. wasReqURLRedirectDomainNames property is an empty List.
     */
    @Test(expected = RuntimeException.class)
    public void isReferrerHostValid_DomainHostnameListEmpty() throws RuntimeException {
        String currentReqURL = "http://goodhost.my.com/j_security_check";
        String savedWASReqURL = "http://badhost.badhost.com/protectedPage.jsp";
        List<String> domainList = Arrays.asList();
        ReferrerURLCookieHandler.isReferrerHostValid(currentReqURL, savedWASReqURL, domainList);
    }

    /*
     * We are testing formlogin() scenarios. The WASReqURL hostname must match the current request hostname or be included in the
     * wasReqURLRedirectDomainNames property.
     * Test Scenario: Hostnames do not match. wasReqURLRedirectDomainNames property is null.
     */
    @Test(expected = RuntimeException.class)
    public void isReferrerHostValid_DomainHostnameListNull() throws RuntimeException {
        String currentReqURL = "http://goodhost.my.com/j_security_check";
        String savedWASReqURL = "http://badhost.badhost.com/protectedPage.jsp";
        List<String> domainList = null;
        ReferrerURLCookieHandler.isReferrerHostValid(currentReqURL, savedWASReqURL, domainList);
    }

    /*
     * We are testing formlogin() scenarios. The WASReqURL hostname must match the current request hostname or be included in the
     * wasReqURLRedirectDomainNames property.
     * Test Scenario: WASReqURL is a malformed URL. We expect a MalformedURLException.
     */
    @Test(expected = RuntimeException.class)
    public void isReferrerHostValid_WASReqURL_Malformed() throws RuntimeException {
        final String currentReqURL = "http://goodhost.my.com/j_security_check";
        final String savedWASReqURL = "%0dhttp%3A%2F%2Fboss0216.pok.ibm.com%0a%202%203%0a%20Location%3A%20http%3A%2F%2Fboss0216.pok.ibm.com%20";
        final List<String> domainList = null;
        ReferrerURLCookieHandler.isReferrerHostValid(currentReqURL, savedWASReqURL, domainList);
    }

    /*
     * We are testing formlogin() scenarios. The WASReqURL hostname must match the current request hostname or be included in the
     * wasReqURLRedirectDomainNames property.
     * Test Scenario: WASReqURL is null.
     */
    @Test(expected = RuntimeException.class)
    public void isReferrerHostValid_WASReqURLNull() throws RuntimeException {
        String currentReqURL = "http://goodhost.my.com/j_security_check";
        String savedWASReqURL = null;
        List<String> domainList = Arrays.asList("ok.com", "badhost.com", "notok.com");
        ReferrerURLCookieHandler.isReferrerHostValid(currentReqURL, savedWASReqURL, domainList);
    }

    /**
     * clearReferrerURLCookie shall take no action if the WASReqURL Cookie
     * is not set for the HttpServletRequest's cookies.
     */
    @Test
    public void clearReferrerURLCookie_setEmpty() {
        final Cookie[] cookies = new Cookie[] {
                                                new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, "")
        };
        context.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                never(res).addCookie(with(any(Cookie.class)));
            }
        });
        handler.clearReferrerURLCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
    }

    /**
     * cokieMatches is used in clearReferrerURLCookie_set to verify
     * the cookie passed to the HttpServletResponse has the right properties.
     *
     * @param cookie
     * @return boolean if the cookie's properties match
     */
    @Factory
    public static Matcher<Cookie> matchingCookie(Cookie cookie) {
        return new CookieMatcher(cookie);
    }

    /**
     * clearReferrerURLCookie shall invalidate the WASReqURL Cookie
     * for the HttpServletResponse if it is set for the
     * HttpServletRequest's cookies.
     */
    @Test
    public void clearReferrerURLCookie_set() {
        final Cookie[] cookies = new Cookie[] {
                                                new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, "http://site.com/page")
        };
        context.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                one(res).addCookie(with(matchingCookie(invalidatedReferrerURLCookie)));
                one(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
            }
        });
        handler.clearReferrerURLCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
    }

    /**
     * shouldRedirectToReferrerURL shall not set the REFERRER_URL cookie
     * in the response and shall return null if ALWAYS_RESTORE_WASREQURL is
     * set to false.
     */
    @Ignore
    @Test
    public void shouldRedirectToReferrerURL_ALWAYS_RESTORE_WASREQURL_false() {
        assertNull(handler.shouldRedirectToReferrerURL(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME));
    }

    /**
     * shouldRedirectToReferrerURL shall not set the REFERRER_URL cookie
     * in the response and shall return null if there is no
     * REFERRER_URL cookie.
     */
    @Ignore
    @Test
    public void shouldRedirectToReferrerURL_noCookie() {
        final Cookie[] cookies = new Cookie[0];
        context.checking(new Expectations() {
            {
                allowing(req).getCookies();
                will(returnValue(cookies));
                never(res).addCookie(with(any(Cookie.class)));
            }
        });

        assertNull(handler.shouldRedirectToReferrerURL(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME));
    }

    /**
     * shouldRedirectToReferrerURL shall set the REFERRER_URL cookie
     * in the response and shall return the host name if the
     * REFERRER_URL cookie's value is an empty String.
     */
    @Ignore
    @Test
    public void shouldRedirectToReferrerURL_emptyReferrerURL() {
        String referrerURL = "";
        final StringBuffer currentURL = new StringBuffer("http://site.com/page");
        final Cookie[] cookies = new Cookie[] {
                                                new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, referrerURL)
        };
        context.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                allowing(req).getRequestURL();
                will(returnValue(currentURL));
                allowing(req).getQueryString();
                will(returnValue(null));
                one(req).getServletPath();
                will(returnValue(null));
                one(req).getPathInfo();
                will(returnValue(null));
                one(res).addCookie(with(matchingCookie(invalidatedReferrerURLCookie)));
            }
        });

        AuthenticationResult result = handler.shouldRedirectToReferrerURL(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
        assertEquals(AuthResult.REDIRECT, result.getStatus());
        assertEquals("http://site.com", result.getRedirectURL());
    }

    /**
     * shouldRedirectToReferrerURL shall set the REFERRER_URL cookie
     * in the response and shall return the host name + relative URL
     * if the REFERRER_URL cookie's value is a relative URL.
     */
    @Ignore
    @Test
    public void shouldRedirectToReferrerURL_relativeReferrerURL() {
        String referrerURL = "/otherpage";
        final StringBuffer currentURL = new StringBuffer("http://site.com/page");
        final Cookie[] cookies = new Cookie[] {
                                                new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, referrerURL)
        };
        context.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                allowing(req).getRequestURL();
                will(returnValue(currentURL));
                allowing(req).getQueryString();
                will(returnValue(null));
                one(req).getServletPath();
                will(returnValue(null));
                one(req).getPathInfo();
                will(returnValue(null));
                one(res).addCookie(with(matchingCookie(invalidatedReferrerURLCookie)));
            }
        });

        AuthenticationResult result = handler.shouldRedirectToReferrerURL(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
        assertEquals(AuthResult.REDIRECT, result.getStatus());
        assertEquals("http://site.com/otherpage", result.getRedirectURL());
    }

    /**
     * shouldRedirectToReferrerURL shall set the REFERRER_URL cookie
     * in the response and shall return the decoded, host injected URL
     * for the value of the REFERRER_URL cookie.
     */
    @Ignore
    @Test
    public void shouldRedirectToReferrerURL_encodedWithoutHost() {
        String expected = "http://site.com:80/page;/subpage,/more%.html";
        final String referrerURL = "http://:80/page%3B/subpage%2C/more%25.html";
        final StringBuffer currentURL = new StringBuffer("http://site.com/page");
        final Cookie[] cookies = new Cookie[] {
                                                new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, referrerURL)
        };
        context.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                allowing(req).getRequestURL();
                will(returnValue(currentURL));
                allowing(req).getQueryString();
                will(returnValue(null));
                one(req).getServletPath();
                will(returnValue(null));
                one(req).getPathInfo();
                will(returnValue(null));
                one(res).addCookie(with(matchingCookie(invalidatedReferrerURLCookie)));
            }
        });

        AuthenticationResult result = handler.shouldRedirectToReferrerURL(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
        assertEquals(AuthResult.REDIRECT, result.getStatus());
        assertEquals(expected, result.getRedirectURL());
    }

    /**
     * shouldRedirectToReferrerURL shall not set any cookies in the
     * HttpServletResponse and shall return null if the current URI
     * is not contained in the referrerURL.
     */
    @Ignore
    @Test
    public void shouldRedirectToReferrerURL_completelyDifferentURI() {
        final String referrerURL = "http://site.com/page";
        final StringBuffer currentURL = new StringBuffer("http://site.com/page");
        final Cookie[] cookies = new Cookie[] {
                                                new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, referrerURL)
        };
        context.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                allowing(req).getRequestURL();
                will(returnValue(currentURL));
                allowing(req).getQueryString();
                will(returnValue(null));
                one(req).getServletPath();
                will(returnValue("/otherpage"));
                one(req).getPathInfo();
                will(returnValue(null));
                never(res).addCookie(with(any(Cookie.class)));
            }
        });

        assertNull(handler.shouldRedirectToReferrerURL(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME));
    }

    /**
     * shouldRedirectToReferrerURL shall not set any cookies in the
     * HttpServletResponse and shall return null if the current request
     * URL is the same as the referrer URL.
     */
    @Ignore
    @Test
    public void shouldRedirectToReferrerURL_sameURL() {
        final String referrerURL = "http:///page";
        final StringBuffer currentURL = new StringBuffer("http://site.com/page");
        final Cookie[] cookies = new Cookie[] {
                                                new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, referrerURL)
        };
        context.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                allowing(req).getRequestURL();
                will(returnValue(currentURL));
                allowing(req).getQueryString();
                will(returnValue(null));
                one(req).getServletPath();
                will(returnValue("/page"));
                one(req).getPathInfo();
                will(returnValue(null));
                never(res).addCookie(with(any(Cookie.class)));
            }
        });

        assertNull(handler.shouldRedirectToReferrerURL(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME));
    }

    /**
     * shouldRedirectToReferrerURL shall set the referrer URL cookie in the
     * HttpServletResponse and shall return if the current request
     * URL is the same as the referrer URL but not the same URI.
     */
    @Ignore
    @Test
    public void shouldRedirectToReferrerURL_sameURLExceptForQueryString() {
        String expected = "http://site.com/page";
        final String referrerURL = "http:///page";
        final StringBuffer currentURL = new StringBuffer("http://site.com/page");
        final Cookie[] cookies = new Cookie[] {
                                                new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, referrerURL)
        };
        context.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                allowing(req).getRequestURL();
                will(returnValue(currentURL));
                allowing(req).getQueryString();
                will(returnValue("id=12345"));
                one(req).getServletPath();
                will(returnValue("/page"));
                one(req).getPathInfo();
                will(returnValue(null));
            }
        });

        AuthenticationResult result = handler.shouldRedirectToReferrerURL(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
        assertEquals(AuthResult.REDIRECT, result.getStatus());
        assertEquals(expected, result.getRedirectURL());
    }

    /**
     * check boolean of createCookie method
     * Input: HttpOnlyCookies:true, SSORequireSSL: true, enableHttpOnly: true
     * Output: Secure flag: true, httpOnly flag: true
     */
    @Test
    public void testCreateCookieEnableHttpOnlyTrueSecureTrue() {
        context.checking(new Expectations() {
            {
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getSameSiteCookie();
                will(returnValue("Disabled"));
            }
        });
        String url = "http://site.com:80/page";

        Cookie cookie = handler.createCookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, url, true, req);
        assertTrue("The cookie must be set to secure.", cookie.getSecure());
        assertTrue("The cookie must be set to http only.", cookie.isHttpOnly());
    }

    /**
     * check boolean of createCookie method
     */
    @Test
    public void testCreateCookieEnableHttpOnlyTrueHttpOnlyTrueTrueSecureTrue() {
        context.checking(new Expectations() {
            {
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getSameSiteCookie();
                will(returnValue("Disabled"));
            }
        });
        String url = "http://site.com:80/page";
        String urlWithoutHost = "http://:80/page";

        Cookie cookie = handler.createCookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, url, true, req);
        assertTrue("The cookie must be set to secure.", cookie.getSecure());
        assertTrue("The cookie must be set to http only.", cookie.isHttpOnly());
    }

    /**
     * check boolean of createCookie method
     * Input: HttpOnlyCookies:false, SSORequireSSL: false, enableHttpOnly: true
     * Output: Secure flag: true, httpOnly flag: false
     */
    @Test
    public void testCreateCookieEnableHttpOnlyTrueHttpOnlyFalseSecureFalse() {
        context.checking(new Expectations() {
            {
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(false));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(false));
                allowing(webAppSecConfig).getSameSiteCookie();
                will(returnValue("Disabled"));
            }
        });
        String url = "http://site.com:80/page";

        Cookie cookie = handler.createCookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, url, true, req);
        assertFalse("The cookie must not be set to secure.", cookie.getSecure());
        assertFalse("The cookie must not be set to http only.", cookie.isHttpOnly());
    }

    /**
     * check boolean of createCookie method
     * Input: HttpOnlyCookies:true, SSORequireSSL: true, enableHttpOnly: false
     * Output: Secure flag: true, httpOnly flag: false
     */
    @Test
    public void testCreateCookieEnableHttpOnlyFalseHttpOnlyTrueSecureTrue() {
        context.checking(new Expectations() {
            {
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getSameSiteCookie();
                will(returnValue("Disabled"));
            }
        });
        String url = "http://site.com:80/page";

        Cookie cookie = handler.createCookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, url, false, req);
        assertTrue("The cookie must be set to secure.", cookie.getSecure());
        assertFalse("The cookie must not be set to http only.", cookie.isHttpOnly());
    }

    /**
     * invalidateCookie shall invalidate the specified Cookie
     * for the HttpServletResponse if it is set for the
     * HttpServletRequest's cookies.
     * Input: HttpOnlyCookies:true, SSORequireSSL: true, enableHttpOnly: true
     */
    @Test
    public void invalidateCookieHttpOnlyTrueSecureTrueEnableHttpOnlyTrue() {
        context.checking(new Expectations() {
            {
                one(res).addCookie(with(matchingCookie(HttpOnlyTrueSecureTrueCookie)));
                one(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
            }
        });
        handler.invalidateCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, true);

    }

    /**
     * invalidateCookie shall invalidate the specified Cookie
     * for the HttpServletResponse if it is set for the
     * HttpServletRequest's cookies.
     * Input: HttpOnlyCookies:true, SSORequireSSL: true, enableHttpOnly: false
     */
    @Test
    public void invalidateCookieHttpOnlyTrueSecureTrueEnableHttpOnlyFalse() {
        context.checking(new Expectations() {
            {
                one(res).addCookie(with(matchingCookie(HttpOnlyFalseSecureTrueCookie)));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
            }
        });
        handler.invalidateCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, false);

    }

    /**
     * invalidateCookie shall invalidate the specified Cookie
     * for the HttpServletResponse if it is set for the
     * HttpServletRequest's cookies.
     * Input: HttpOnlyCookies:false, SSORequireSSL: true, enableHttpOnly: true
     */
    @Test
    public void invalidateCookieHttpOnlyFalseSecureTrueEnableHttpOnlyTrue() {
        context.checking(new Expectations() {
            {
                one(res).addCookie(with(matchingCookie(HttpOnlyFalseSecureTrueCookie)));
                one(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(false));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
            }
        });
        handler.invalidateCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, true);

    }

    /**
     * invalidateCookie shall invalidate the specified Cookie
     * for the HttpServletResponse if it is set for the
     * HttpServletRequest's cookies.
     * Input: HttpOnlyCookies:true, SSORequireSSL: false, enableHttpOnly: true
     */
    @Test
    public void invalidateCookieHttpOnlyTrueSecureFalseEnableHttpOnlyTrue() {
        context.checking(new Expectations() {
            {
                one(res).addCookie(with(matchingCookie(HttpOnlyTrueSecureFalseCookie)));
                one(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(false));
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
            }
        });
        handler.invalidateCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, true);

    }

    /**
     * invalidateReferrerURLCookies shall invalidate the specified Cookie
     * for the HttpServletResponse if it is set for the
     * HttpServletRequest's cookies.
     * Input: HttpOnlyCookies:true, SSORequireSSL: false, enableHttpOnly: true
     */
    @Test
    public void invalidateReferrerURLCookieHttpOnlyTrueSecureTrueEnableHttpOnlyTrue() {
        context.checking(new Expectations() {
            {
                one(res).addCookie(with(matchingCookie(HttpOnlyTrueSecureTrueCookie)));
                one(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
            }
        });

        handler.invalidateReferrerURLCookies(req, res, new String[] { ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME });
    }

}
