/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenHelper;
import com.ibm.ws.webcontainer.security.internal.CookieMatcher;
import com.ibm.ws.webcontainer.security.internal.StringUtil;
import com.ibm.wsspi.security.token.SingleSignonToken;

public class SSOCookieHelperImplTest {

    private static final String TEST_URL_STRING = "http://myhost.austin.ibm.com:9080/snoop";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class);
    private final SingleSignonToken ssoToken = mock.mock(SingleSignonToken.class);

    private final byte[] cookieBytes = StringUtil.getBytes("123");
    private final String cookieValue = new String(Base64.encodeBase64(cookieBytes));
    private final String cookieName = "LTPAToken2";
    private final Cookie cookie = new Cookie(cookieName, cookieValue);
    private final WebAppSecurityConfig config = mock.mock(WebAppSecurityConfig.class);
    private final SSOCookieHelperImpl ssoCookieHelper = new SSOCookieHelperImpl(config);
    private final Subject subject = new Subject();
    private final JwtSSOTokenHelper jwtSSOTokenHelper = mock.mock(JwtSSOTokenHelper.class);

    private class SSOCookieHelperImplTestDouble extends SSOCookieHelperImpl {

        public boolean removeSSOCookieFromResponseNOTInvoked = true;

        /**
         * @param config
         */
        public SSOCookieHelperImplTestDouble(WebAppSecurityConfig config) {
            super(config);
        }

        @Override
        public void removeSSOCookieFromResponse(HttpServletResponse resp) {
            removeSSOCookieFromResponseNOTInvoked = false;
        }
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Utility method that verifies whether the specified Cookie
     * matches the Cookie that is instantiated in
     * FormLoginAuthenticator.restorePostParamsFromCookie(), based
     * on name, value, age, and path.
     *
     * @param cookie
     * @return boolean if the cookie's properties match
     */
    @Factory
    private static Matcher<Cookie> matchingCookie(Cookie cookie) {
        return new CookieMatcher(cookie);
    }

    @Test
    public void testAddSSOCookiesToResponse_SingleSignonIsNotEnabled() {
        mock.checking(new Expectations() {
            {
                one(config).isSingleSignonEnabled();
                will(returnValue(false));
            }
        });
        ssoCookieHelper.addSSOCookiesToResponse(subject, req, resp);
        mock.assertIsSatisfied();
    }

    /**
     * Test addSSOCookiesToResponse() with null Subject
     */
    @Test
    public void testAddSSOCookiesToResponse_NullSubject() {
        mock.checking(new Expectations() {
            {
                one(config).isSingleSignonEnabled();
                will(returnValue(true));
                one(req).isSecure();
                will(returnValue(true));
                one(config).getSSORequiresSSL();
                will(returnValue(false));
            }
        });
        ssoCookieHelper.addSSOCookiesToResponse(null, req, resp);
        mock.assertIsSatisfied();
    }

    /**
     * Test addSSOCookiesToResponse() with subject that have no SSOToken
     */
    @Test
    public void testAddSSOCookiesToResponse_SubjectWithNoSSOToken() {
        mock.checking(new Expectations() {
            {
                one(config).isSingleSignonEnabled();
                will(returnValue(true));
                one(req).isSecure();
                will(returnValue(true));
                one(config).getSSORequiresSSL();
                will(returnValue(false));
            }
        });
        ssoCookieHelper.addSSOCookiesToResponse(new Subject(), req, resp);
        mock.assertIsSatisfied();
    }

    /**
     * Test addSSOCookiesToResponse() with subject that have SSOToken
     */
    @Test
    public void testAddSSOCookiesToResponse_SubjectWithSSOToken() {
        SSOCookieHelperImplTestDouble ssoCookieHelper = new SSOCookieHelperImplTestDouble(config);
        mock.checking(new Expectations() {
            {
                one(config).isSingleSignonEnabled();
                will(returnValue(true));
                allowing(ssoToken).getName();
                will(returnValue(cookieName));
                allowing(ssoToken).getBytes();
                will(returnValue(cookieBytes));
                allowing(config).getSSOCookieName();
                will(returnValue(cookieName));
                one(config).getHttpOnlyCookies();
                will(returnValue(true));
                one(resp).addCookie(with(matchingCookie(cookie)));
                one(req).isSecure();
                will(returnValue(true));
                allowing(config).getSSORequiresSSL();
                will(returnValue(false));
                allowing(config).getSSODomainList();
                allowing(config).getSSOUseDomainFromURL();
                allowing(req).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL_STRING)));
                allowing(config).getSameSiteCookie();
                will(returnValue("Disabled"));
            }
        });
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(ssoToken);
        ssoCookieHelper.addSSOCookiesToResponse(subject, req, resp);
        mock.assertIsSatisfied();
        assertTrue("The removeSSOCookieFromResponse method should NOT be invoked.", ssoCookieHelper.removeSSOCookieFromResponseNOTInvoked);
    }

    @Test
    public void testCreateCookie_ssoRequiresSSL_with_httpOnly_true() {
        mock.checking(new Expectations() {
            {
                one(config).getSSOCookieName();
                will(returnValue(cookieName));
                one(config).getSSORequiresSSL();
                will(returnValue(true));
                one(config).getHttpOnlyCookies();
                will(returnValue(true));
                one(config).getSSODomainList();
                one(config).getSSOUseDomainFromURL();
                one(req).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL_STRING)));
                allowing(config).getSameSiteCookie();
                will(returnValue("Disabled"));
            }
        });
        Cookie ssoCookie = ssoCookieHelper.createCookie(req, cookieValue);
        assertEquals("The cookie's value must be set.", cookieValue, ssoCookie.getValue());
        assertEquals("The cookie's max age must be set to -1.", -1, ssoCookie.getMaxAge());
        assertEquals("The cookie's path must be set to forward slash.", "/", ssoCookie.getPath());
        assertTrue("The cookie must be set to secure.", ssoCookie.getSecure());
        assertTrue("The cookie must be set to http only.", ssoCookie.isHttpOnly());
    }

    @Test
    public void testCreateCookie_noSSORequiresSSL_with_httpOnly_false() {
        mock.checking(new Expectations() {
            {
                one(config).getSSOCookieName();
                will(returnValue(cookieName));
                one(config).getSSORequiresSSL();
                will(returnValue(false));
                one(config).getHttpOnlyCookies();
                will(returnValue(false));
                one(config).getSSODomainList();
                one(config).getSSOUseDomainFromURL();
                one(req).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL_STRING)));
                allowing(config).getSameSiteCookie();
                will(returnValue("Disabled"));
            }
        });
        Cookie ssoCookie = ssoCookieHelper.createCookie(req, cookieValue);
        assertFalse("The cookie must not be set to secure.", ssoCookie.getSecure());
        assertFalse("The cookie must not be set to http only.", ssoCookie.isHttpOnly());
    }

    /**
     * Test allowToAddCookieToResponse() with SingleSingonEnabled=false
     */
    @Test
    public void testAllowToAddCookieToResponse_SSODisabled() {
        mock.checking(new Expectations() {
            {
                one(config).isSingleSignonEnabled();
                will(returnValue(false));
            }
        });
        assertFalse(ssoCookieHelper.allowToAddCookieToResponse(req));
    }

    /**
     * Test allowToAddCookieToResponse() with following options
     * - singleSingon=true
     * - request is http
     * - ssoRequiresSSL=true
     */
    @Test
    public void testAllowToAddCookieToResponse_http_SSORequiresSSL_true() {
        mock.checking(new Expectations() {
            {
                one(config).isSingleSignonEnabled();
                will(returnValue(true));
                one(req).isSecure();
                will(returnValue(false));
                one(config).getSSORequiresSSL();
                will(returnValue(true));
            }
        });
        assertFalse(ssoCookieHelper.allowToAddCookieToResponse(req));
    }

    /**
     * Test allowToAddCookieToResponse() with following options
     * - singleSingon=true
     * - request is https
     * - ssoRequiresSSL=false
     */
    @Test
    public void testAllowToAddCookieToResponse_https_SSORequiresSSL_false() {
        mock.checking(new Expectations() {
            {
                one(config).isSingleSignonEnabled();
                will(returnValue(true));
                one(req).isSecure();
                will(returnValue(true));
                one(config).getSSORequiresSSL();
                will(returnValue(false));
            }
        });
        assertTrue(ssoCookieHelper.allowToAddCookieToResponse(req));
    }

    /**
     * Test allowToAddCookieToResponse() with following options
     * - singleSingon=true
     * - request is https
     * - ssoRequiresSSL=true
     */
    @Test
    public void testAllowToAddCookieToResponse_https_SSORequiresSSL_true() {
        mock.checking(new Expectations() {
            {
                one(config).isSingleSignonEnabled();
                will(returnValue(true));
                one(req).isSecure();
                will(returnValue(true));
                one(config).getSSORequiresSSL();
                will(returnValue(true));
            }
        });
        assertTrue(ssoCookieHelper.allowToAddCookieToResponse(req));
    }

    @Test
    public void testGetSSODomainName_IP_Address() {
        mock.checking(new Expectations() {
            {
                one(req).getRequestURL();
                will(returnValue(new StringBuffer("http://1.2.3.4:9080/snoop")));
            }
        });
        assertNull(ssoCookieHelper.getSSODomainName(req, (List<String>) null, false));
    }

    @Test
    public void testGetSSODomainName_localhost() {
        mock.checking(new Expectations() {
            {
                one(req).getRequestURL();
                will(returnValue(new StringBuffer("http://localhost:9080/snoop")));
            }
        });
        assertNull(ssoCookieHelper.getSSODomainName(req, (List<String>) null, false));
    }

    @Test
    public void testGetSSODomainName_host_and_null_domainNameList() {
        mock.checking(new Expectations() {
            {
                one(req).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL_STRING)));
            }
        });
        assertNull(ssoCookieHelper.getSSODomainName(req, (List<String>) null, false));
    }

    @Test
    public void testGetSSODomainName_useURLDomain() {
        mock.checking(new Expectations() {
            {
                one(req).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL_STRING)));
            }
        });
        List<String> ssoDOmainList = new ArrayList<String>();
        ssoDOmainList.add("useURLDomain");
        assertEquals(".austin.ibm.com", ssoCookieHelper.getSSODomainName(req, ssoDOmainList, true));
    }

    @Test
    public void testGetSSODomainName_matchDomain() {
        final StringBuffer url = new StringBuffer(TEST_URL_STRING);
        mock.checking(new Expectations() {
            {
                one(req).getRequestURL();
                will(returnValue(url));
            }
        });
        List<String> ssoDOmainList = new ArrayList<String>();
        ssoDOmainList.add("ibm.com");
        ssoDOmainList.add("austin.ibm.com");
        boolean useURLDomain = true;
        assertEquals("ibm.com", ssoCookieHelper.getSSODomainName(req, ssoDOmainList, useURLDomain));
    }

    @Test
    public void testGetSSODomainName_notMatchDomain_with_useURLDomain_false() {
        final StringBuffer url = new StringBuffer(TEST_URL_STRING);
        mock.checking(new Expectations() {
            {
                one(req).getRequestURL();
                will(returnValue(url));
            }
        });
        List<String> ssoDOmainList = new ArrayList<String>();
        ssoDOmainList.add("pok.ibm.com");
        ssoDOmainList.add("raleigh.ibm.com");
        boolean useURLDomain = false;
        assertNull(ssoCookieHelper.getSSODomainName(req, ssoDOmainList, useURLDomain));
    }

    @Test
    public void testGetSSODomainName_notMatchDomain_with_useURLDomain_true() {
        final StringBuffer url = new StringBuffer(TEST_URL_STRING);
        mock.checking(new Expectations() {
            {
                one(req).getRequestURL();
                will(returnValue(url));
            }
        });
        List<String> ssoDOmainList = new ArrayList<String>();
        ssoDOmainList.add("pok.ibm.com");
        ssoDOmainList.add("raleigh.ibm.com");
        boolean useURLDomain = true;
        assertEquals(".austin.ibm.com", ssoCookieHelper.getSSODomainName(req, ssoDOmainList, useURLDomain));
    }

    @Test
    public void testGetSSODomainName_MatchDomain_with_useURLDomain_true() {
        final StringBuffer url = new StringBuffer(TEST_URL_STRING);
        mock.checking(new Expectations() {
            {
                one(req).getRequestURL();
                will(returnValue(url));
            }
        });
        List<String> ssoDOmainList = new ArrayList<String>();
        ssoDOmainList.add("ibm.com");
        ssoDOmainList.add("raleigh.ibm.com");
        boolean useURLDomain = true;
        assertEquals("ibm.com", ssoCookieHelper.getSSODomainName(req, ssoDOmainList, useURLDomain));
    }

    @Test
    public void createLogoutCookies_noCookies() {
        mock.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(null));
            }
        });
        ssoCookieHelper.createLogoutCookies(req, resp);
    }

    @Test
    public void createLogoutCookies_emptyCookies() {
        mock.checking(new Expectations() {
            {
                one(config).getSSOCookieName();
                will(returnValue("LtpaToken"));
                one(req).getCookies();
                will(returnValue(new Cookie[] {}));
                one(config).isUseOnlyCustomCookieName();
                will(returnValue(false));
            }
        });
        ssoCookieHelper.createLogoutCookies(req, resp);
    }

    @Test
    public void createLogoutCookies_noSSOCookies() {
        mock.checking(new Expectations() {
            {
                one(config).getSSOCookieName();
                will(returnValue("LtpaToken"));
                one(req).getCookies();
                will(returnValue(new Cookie[] { new Cookie("name", "value") }));
                one(config).isUseOnlyCustomCookieName();
                will(returnValue(false));
            }
        });
        ssoCookieHelper.createLogoutCookies(req, resp);
    }

    @Test
    public void createLogoutCookies_withSSOCookie() {
        final Cookie matchCookie = new Cookie("LtpaToken", "");
        matchCookie.setMaxAge(0);
        final StringBuffer sb = new StringBuffer("myhost.austin.ibm.com");
        mock.checking(new Expectations() {
            {
                one(config).getSSOCookieName();
                will(returnValue("LtpaToken"));
                one(req).getCookies();
                will(returnValue(new Cookie[] { new Cookie("LtpaToken", "value") }));
                one(req).isSecure();
                will(returnValue(true));
                one(resp).addCookie(with(matchingCookie(matchCookie)));
                one(config).getSSODomainList();
                one(config).getSSOUseDomainFromURL();
                will(returnValue(false));
                one(req).getRequestURL();
                will(returnValue(sb));
                one(config).getHttpOnlyCookies();
            }
        });
        ssoCookieHelper.createLogoutCookies(req, resp);
    }

    @Test
    public void createLogoutCookies_withMultpleCookies() {
        final Cookie matchCookie = new Cookie("LtpaToken", "");
        matchCookie.setMaxAge(0);
        final StringBuffer sb = new StringBuffer("myhost.austin.ibm.com");
        mock.checking(new Expectations() {
            {
                one(config).getSSOCookieName();
                will(returnValue("LtpaToken"));
                one(req).getCookies();
                will(returnValue(new Cookie[] { new Cookie("name", "value"), new Cookie("LtpaToken", "value") }));
                one(req).isSecure();
                will(returnValue(true));
                one(resp).addCookie(with(matchingCookie(matchCookie)));
                one(config).getSSODomainList();
                one(config).getSSOUseDomainFromURL();
                will(returnValue(false));
                one(req).getRequestURL();
                will(returnValue(sb));
                one(config).getHttpOnlyCookies();
            }
        });
        ssoCookieHelper.createLogoutCookies(req, resp);
    }

    @Test
    public void splitString() {
        String buf = "abcdefghijklmnopqrstuvwxyz"; //26
        String[] result = ssoCookieHelper.splitString(buf, 1);
        assertTrue(result.length == 26);

        result = ssoCookieHelper.splitString(buf, 3);
        assertTrue(result.length == 9);
        assertTrue(result[8].equals("yz"));

        result = ssoCookieHelper.splitString(buf, 25);
        assertTrue(result.length == 2);
        assertTrue(result[0].length() == 25);
        assertTrue(result[1].equals("z"));

        result = ssoCookieHelper.splitString(buf, 26);
        assertTrue(result.length == 1);
        assertTrue(result[0].equals(buf));

    }

    private class SSOCookieHelperImplTestDouble2 extends SSOCookieHelperImpl {
        public boolean removeSSOCookieFromResponseNOTInvoked = true;

        public SSOCookieHelperImplTestDouble2(WebAppSecurityConfig config) {
            super(config);
        }

        boolean secure = true;

        @Override
        public Cookie createCookie(HttpServletRequest req, String cookieName, String cookieValue, boolean secure) {
            return new Cookie(cookieName, cookieValue); //skip hard-to-mock ssodomain stuff.
        }

        @Override
        protected String getJwtCookieName() {
            return "JwtToken"; // alas, static methods cannot be mocked, so override
        }
    }

    @Test
    // add a large cookie and confirm it gets split into two.
    // expectations will fail if addCookie is not called twice.
    public void addCookies() {
        SSOCookieHelperImplTestDouble2 schi = new SSOCookieHelperImplTestDouble2(config);
        StringBuffer sb = new StringBuffer();
        while (sb.length() < 4000) {
            sb.append("x");
        }
        String bigStr = sb.toString();
        mock.checking(new Expectations() {
            {
                allowing(req).isSecure();
                one(resp).addCookie(with(any(Cookie.class)));
                one(resp).addCookie(with(any(Cookie.class)));
//                allowing(config).getSSORequiresSSL();
//                allowing(config).getHttpOnlyCookies();
//                allowing(config).getSSODomainList();
//                allowing(config).getSSOUseDomainFromURL();
//                allowing(req).getRequestURI();
            }
        });

        schi.addJwtCookies(bigStr, req, resp);
        mock.assertIsSatisfied();
    }

    class SSOATestDouble extends SSOCookieHelperImpl {

        SSOATestDouble(WebAppSecurityConfig config) {
            super(config);
        }

        @Override
        protected String getCookieValue(HttpServletRequest req, String cookieName) {
            if (cookieName.endsWith("03"))
                return null;
            return cookieName + "_value";
        }
    }

    @Test
    // test that cookie contatenation in getJwtSsoTokenFromCookies works.
    // Use the testDouble class to feed two cookies into the test.
    public void getJwtSsoTokenFromCookies() {

        SSOATestDouble stb = new SSOATestDouble(config);
        String result = stb.getJwtSsoTokenFromCookies(req, "jwtToken");
        String expected = "jwtToken_valuejwtToken02_value";
        System.out.println("getJwtSsoTokenFromCookies result is: " + result + " expected is: " + expected);
        assertTrue(result.equals(expected));
    }
}
