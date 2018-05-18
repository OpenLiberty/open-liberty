/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;

public class FormLoginAuthenticatorTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final FormLoginConfiguration flcfg = mock.mock(FormLoginConfiguration.class);
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class);
    private final SRTServletRequest extReq = mock.mock(SRTServletRequest.class);
    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfigImpl.class);
    private final WebAuthenticator webAuth = mock.mock(WebAuthenticator.class);
    private final WebRequest webRequest = mock.mock(WebRequest.class);
    private final WebProviderAuthenticatorProxy webProviderAuthenticatorProxy = mock.mock(WebProviderAuthenticatorProxy.class);

    private static final String POSTPARAM_COOKIE = "WASPostParam";
    private final StringBuffer sbReqURL = new StringBuffer("http://localhost:9080/snoop");
    private final AuthenticationResult authResultCont = new AuthenticationResult(AuthResult.CONTINUE, "No provider authentication");

    // A lot of the code repeats, but the use of common expectations
    // makes reading of this test too difficult because of the different
    // code paths each test goes through. Maybe in the future we can re-factor
    // some of this into common code through an @Before-annotated method.

    /**
     * Tests redirect to the login page
     * 
     * @throws Exception
     */
    @Test
    public void testAuthenticate_LoginRedirect() throws Exception {
        FormLoginAuthenticator authenticatorDouble = new FormLoginAuthenticator(webAuth, webAppSecConfig, webProviderAuthenticatorProxy);
        final String loginURL = "http://localhost:9080/login.jsp";

        mock.checking(new Expectations() {
            {
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));

                // authenticate(WebRequest, WebAttributes)
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(req));
                allowing(webRequest).getHttpServletResponse();
                will(returnValue(resp));

                // handleFormLogin()
                allowing(webAuth).authenticate(webRequest);
                will(returnValue(null)); // null AuthenticationResult means we should be redirected to login page
                allowing(webRequest).isFormLoginRedirectEnabled();
                will(returnValue(true));

                allowing(webProviderAuthenticatorProxy).authenticate(req, resp, null);
                will(returnValue(authResultCont));

                // handleRedirect(), getFormLoginURL(),
                // and savePostParams()
                allowing(webRequest).getFormLoginConfiguration();
                will(returnValue(flcfg));
                allowing(flcfg).getLoginPage();
                will(returnValue("login.jsp"));
                allowing(req).getRequestURL();
                will(returnValue(sbReqURL));
                allowing(req).getContextPath();
                will(returnValue(""));
                allowing(req).getQueryString();
                will(returnValue(null));
                allowing(req).getRequestURI();
                will(returnValue("/snoop"));
                allowing(req).getMethod();
                will(returnValue("POST"));
                allowing(req).isSecure();
                will(returnValue(true));
                allowing(webAppSecConfig).getPreserveFullyQualifiedReferrerUrl();
                will(returnValue(false));
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                allowing(webAppSecConfig).getOverrideHttpAuthMethod();
                will(returnValue(null));
            }
        });

        AuthenticationResult authResult = authenticatorDouble.authenticate(webRequest, webAppSecConfig);
        assertEquals("AuthenticationResult should be REDIRECT", AuthResult.REDIRECT, authResult.getStatus());
        assertEquals("Redirect URL should be " + loginURL, loginURL, authResult.getRedirectURL());
    }

    /**
     * Tests successful authentication case where user is authorized
     * 
     * @throws Exception
     */
    @Test
    public void testAuthenticateSuccess() {
        FormLoginAuthenticator authenticatorDouble = new FormLoginAuthenticator(webAuth, webAppSecConfig, webProviderAuthenticatorProxy);
        final Subject authSubject = new Subject();

        mock.checking(new Expectations() {
            {
                // authenticate(WebRequest, WebAttributes)
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(req));
                allowing(webRequest).getHttpServletResponse();
                will(returnValue(resp));

                // handleFormLogin()
                allowing(webAuth).authenticate(webRequest);
                will(returnValue(new AuthenticationResult(AuthResult.SUCCESS, authSubject)));

                // restorePostParams()
                allowing(req).getRequestURI();
                will(returnValue("/snoop"));
                allowing(req).getMethod();
                will(returnValue("POST"));
            }
        });

        AuthenticationResult authResult = authenticatorDouble.authenticate(webRequest, webAppSecConfig);
        assertEquals("AuthenticationResult should be SUCCESS", AuthResult.SUCCESS, authResult.getStatus());
    }

    /**
     * Tests redirection to the login page does not happen for unprotected resource when persist cred is enabled.
     * 
     * @throws Exception
     */
    @Test
    public void testAuthenticate_LoginDoesNotRedirectForUnprotectedResourceForPersistCred() throws Exception {
        FormLoginAuthenticator authenticatorDouble = new FormLoginAuthenticator(webAuth, webAppSecConfig, webProviderAuthenticatorProxy);
        mock.checking(new Expectations() {
            {
                // authenticate(WebRequest, WebAttributes)
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(req));
                allowing(webRequest).getHttpServletResponse();
                will(returnValue(resp));

                allowing(webProviderAuthenticatorProxy).authenticate(req, resp, null);
                will(returnValue(authResultCont));

                // handleFormLogin()
                allowing(webAuth).authenticate(webRequest);
                will(returnValue(null)); // null AuthenticationResult means we should be redirected to login page

                // handleRedirect(), getFormLoginURL(),
                // and savePostParams()
                allowing(webRequest).getFormLoginConfiguration();
                will(returnValue(flcfg));
                allowing(webRequest).isFormLoginRedirectEnabled();
                will(returnValue(false));
                allowing(flcfg).getLoginPage();
                will(returnValue("login.jsp"));
                allowing(req).getRequestURL();
                will(returnValue(sbReqURL));
                allowing(req).getContextPath();
                will(returnValue(""));
                allowing(req).getQueryString();
                will(returnValue(null));
                allowing(req).getRequestURI();
                will(returnValue("/snoop"));
                allowing(req).getMethod();
                will(returnValue("POST"));
                allowing(req).isSecure();
                will(returnValue(true));
                allowing(webAppSecConfig).getPreserveFullyQualifiedReferrerUrl();
                will(returnValue(false));
                allowing(webAppSecConfig).isUseAuthenticationDataForUnprotectedResourceEnabled();
                will(returnValue(true));
            }
        });

        AuthenticationResult authResult = authenticatorDouble.authenticate(webRequest, webAppSecConfig);
        assertNull("AuthenticationResult should NOT be REDIRECT.", authResult);
    }

    /**
     * Tests that POST parameters are successfully saved to a Cookie
     * when request is redirected to login
     */
    @Test
    public void testAuthenticateRedirect_SavePostParamsToCookie() throws Exception {
        FormLoginAuthenticator authenticatorDouble = new FormLoginAuthenticator(webAuth, webAppSecConfig, webProviderAuthenticatorProxy);
        final Map<String, String> paramMap = new HashMap<String, String>();
//        paramMap.put("param1", "value1");
        final byte[][] data = { { 0x01, 0x02, 0x03 }, { 0x04, 0x05, 0x07 } };

        mock.checking(new Expectations() {
            {
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));

                // authenticate(WebRequest, WebAttributes)
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(extReq));
                // extReq *should* be an instance of IServletRequest, not SRTServletRequest
                // because savePostParams() checks if req instanceof IServletRequest.
                // But it's not possible to mock req as IServletRequest, because IServletRequest
                // extends ServletRequest, not HttpServletRequest. Most of the time, the code
                // assumes that req is HttpServletRequest, and calls its methods. We would get
                // compilation errors if we mocked req as IServletRequest. But we need it to be
                // a child of IServletRequest, in order to force flow down savePostParams()
                // and restorePostParams().
                allowing(webRequest).getHttpServletResponse();
                will(returnValue(resp));

                // handleFormLogin()
                allowing(webAuth).authenticate(webRequest);
                will(returnValue(null)); // null AuthenticationResult means we should be redirected to login page
                allowing(webRequest).isFormLoginRedirectEnabled();
                will(returnValue(true));

                allowing(webProviderAuthenticatorProxy).authenticate(extReq, resp, null);
                will(returnValue(authResultCont));

                // handleRedirect(), getFormLoginURL(),
                // and savePostParams()
                allowing(webRequest).getFormLoginConfiguration();
                will(returnValue(flcfg));
                allowing(flcfg).getLoginPage();
                will(returnValue("login.jsp"));
                allowing(extReq).getRequestURL();
                will(returnValue(sbReqURL));
                allowing(extReq).getContextPath();
                will(returnValue(""));
                allowing(webAppSecConfig).getOverrideHttpAuthMethod();
                will(returnValue(null));

                // This reflects flow in savePostParams
                allowing(extReq).getRequestURI();
                will(returnValue("/snoop"));
                allowing(extReq).getMethod();
                will(returnValue("POST"));
                one(extReq).getAttribute(PostParameterHelper.ATTRIB_HASH_MAP); //
                will(returnValue(paramMap)); //
                one(extReq).setAttribute(PostParameterHelper.ATTRIB_HASH_MAP, null); //
                one(extReq).setInputStreamData((HashMap) paramMap); //
                allowing(extReq).getInputStreamData();
                will(returnValue(paramMap));
                allowing(extReq).sizeInputStreamData(paramMap);
                will(returnValue(100L));
                allowing(extReq).serializeInputStreamData(paramMap);
                will(returnValue(data));
                allowing(extReq).isSecure();
                will(returnValue(true));
                allowing(webAppSecConfig).getPostParamSaveMethod();
                will(returnValue(WebAppSecurityConfig.POST_PARAM_SAVE_TO_COOKIE));

                // This reflects flow in savePostParamsToCookie()
                allowing(webAppSecConfig).getPostParamCookieSize();
                // Random int, just as long as it's greater than size of the Cookie
                // created with my paramMap and the request URL
                will(returnValue(500));

                // Back in handleRedirect()
                allowing(extReq).getQueryString();
                will(returnValue(null));
                allowing(webAppSecConfig).getPreserveFullyQualifiedReferrerUrl();
                will(returnValue(false));
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
            }
        });

        AuthenticationResult authResult = authenticatorDouble.authenticate(webRequest, webAppSecConfig);
        List<Cookie> cookieList = authResult.getCookies();
        Iterator<Cookie> iter = cookieList.iterator();
        Cookie cookie = null;
        boolean wsPostParamFound = false;
        while (iter.hasNext()) {
            // look for PostParameterHelper.POSTPARAM_COOKIE from AuthenticationResult
            cookie = iter.next();
            if (cookie.getName().equals(POSTPARAM_COOKIE)) {
                wsPostParamFound = true;
                break;
            }
        }

        assertEquals("AuthenticationResult should be REDIRECT", AuthResult.REDIRECT, authResult.getStatus());
        assertEquals("WASPostParam cookie should have been found on AuthenticationResult", true, wsPostParamFound);

    }

    /**
     * Tests that POST parameters are successfully saved to a Cookie
     * when request is redirected to login
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAuthenticateRedirect_SavePostParamsToSession() throws Exception {
        final Enumeration<String> paramEnum = mock.mock(Enumeration.class);
        FormLoginAuthenticator authenticatorDouble = new FormLoginAuthenticator(webAuth, webAppSecConfig, webProviderAuthenticatorProxy);
        final HttpSession session = mock.mock(HttpSession.class);
        final Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("param1", "value1");
        final String reqURI = "/snoop";

        mock.checking(new Expectations() {
            {
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));

                // authenticate(WebRequest, WebAttributes)
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(extReq));
                allowing(webRequest).getHttpServletResponse();
                will(returnValue(resp));

                // handleFormLogin()
                allowing(webAuth).authenticate(webRequest);
                will(returnValue(null)); // null AuthenticationResult means we should be redirected to login page
                allowing(webRequest).isFormLoginRedirectEnabled();
                will(returnValue(true));

                allowing(webProviderAuthenticatorProxy).authenticate(extReq, resp, null);
                will(returnValue(authResultCont));

                // handleRedirect(), getFormLoginURL(),
                // and savePostParams()
                allowing(webRequest).getFormLoginConfiguration();
                will(returnValue(flcfg));
                allowing(flcfg).getLoginPage();
                will(returnValue("login.jsp"));
                allowing(extReq).getRequestURL();
                will(returnValue(sbReqURL));
                allowing(extReq).getContextPath();
                will(returnValue(""));
                allowing(webAppSecConfig).getOverrideHttpAuthMethod();
                will(returnValue(null));

                // This reflects flow in savePostParams
                allowing(extReq).getRequestURI();
                will(returnValue(reqURI));
                allowing(extReq).getMethod();
                will(returnValue("POST"));
                allowing(extReq).getAttribute(PostParameterHelper.ATTRIB_HASH_MAP); //
                will(returnValue(paramMap)); //
                allowing(extReq).setAttribute(PostParameterHelper.ATTRIB_HASH_MAP, null);//
                allowing(extReq).setInputStreamData((HashMap) paramMap); //
                allowing(extReq).getInputStreamData();
                will(returnValue(paramMap));
                allowing(extReq).isSecure();
                will(returnValue(true));
                allowing(webAppSecConfig).getPostParamSaveMethod();
                will(returnValue(WebAppSecurityConfig.POST_PARAM_SAVE_TO_SESSION));

                // This reflects flow in savePostParamsToSession()
                // All these calls must happen.
                allowing(extReq).getSession(true);
                will(returnValue(session));
                allowing(extReq).getParameterNames();
                will(returnValue(paramEnum));
                one(session).setAttribute(PostParameterHelper.INITIAL_URL, reqURI);
                one(session).setAttribute(PostParameterHelper.PARAM_NAMES, null);
                one(session).setAttribute(PostParameterHelper.PARAM_VALUES, paramMap);

                // Back in handleRedirect()
                allowing(extReq).getQueryString();
                will(returnValue(null));
                allowing(webAppSecConfig).getPreserveFullyQualifiedReferrerUrl();
                will(returnValue(false));
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
            }
        });

        AuthenticationResult authResult = authenticatorDouble.authenticate(webRequest, webAppSecConfig);
        assertEquals("AuthenticationResult should be REDIRECT", AuthResult.REDIRECT, authResult.getStatus());
        mock.assertIsSatisfied();
    }

    /**
     * Test the scenario where authentication succeeds
     * and the post parameters are restored from the Cookie.
     */
    @Test
    public void testAuthenticate_RestorePostParamsFromCookie() throws Exception {
        FormLoginAuthenticator authenticatorDouble = new FormLoginAuthenticator(webAuth, webAppSecConfig, webProviderAuthenticatorProxy);
        final String reqURL = sbReqURL.toString();
        final Subject authSubject = new Subject();

        final Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("param1", "value1");
        // this mimics the Cookie created in restorePostParamsFromCookie()
        final byte[][] input = { { 0x31, 0x32, 0x33 } };
        final byte[] cookieByteArray = cookieToByteArray(input, reqURL);
        final Cookie wasPostParamCookie = new Cookie("WASPostParam", "");
        wasPostParamCookie.setMaxAge(0);
        wasPostParamCookie.setPath(reqURL);

        mock.checking(new Expectations() {
            {
                // authenticate(WebRequest, WebAttributes)
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(extReq));
                allowing(webRequest).getHttpServletResponse();
                will(returnValue(resp));

                // handleFormLogin()
                allowing(webAuth).authenticate(webRequest);
                will(returnValue(new AuthenticationResult(AuthResult.SUCCESS, authSubject)));

                // now flow goes into restorePostParams()
                allowing(extReq).getRequestURI();
                will(returnValue(reqURL));
                allowing(extReq).getMethod();
                will(returnValue("GET"));
                allowing(webAppSecConfig).getPostParamSaveMethod();
                will(returnValue(WebAppSecurityConfig.POST_PARAM_SAVE_TO_COOKIE));
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));

                // Now flow is in restorePostParamsFromCookie()
                // The call to IExtendedRequest.setInputstreamData()
                // is verification that the the original URL stored in the
                // POSTPARAM_COOKIE matches the request URL, because only
                // in that case would flow go there
                allowing(extReq).getCookieValueAsBytes(POSTPARAM_COOKIE);
                will(returnValue(cookieByteArray));
                allowing(extReq).deserializeInputStreamData(input);
                will(returnValue(paramMap));
                allowing(extReq).setMethod("POST");
                one(extReq).setInputStreamData((HashMap<String, String>) paramMap);

                // This is the verification that the parameters were restored
                // from the "WASPostParam" Cookie, which should be set on the HttpServletResponse.
                one(resp).addCookie(with(matchingCookie(wasPostParamCookie)));

                // Back in handleRedirect()
                allowing(extReq).getQueryString();
                will(returnValue(null));
                allowing(webAppSecConfig).getPreserveFullyQualifiedReferrerUrl();
                will(returnValue(false));
            }
        });

        AuthenticationResult authResult = authenticatorDouble.authenticate(webRequest, webAppSecConfig);
        mock.assertIsSatisfied();
        assertEquals("AuthenticationResult should be SUCCESS", AuthResult.SUCCESS, authResult.getStatus());
    }

    /**
     * Test the scenario where authentication succeeds
     * and the post parameters are restored from the HTTP session.
     */
    @Test
    public void testAuthenticate_RestorePostParamsFromSession() throws Exception {
        FormLoginAuthenticator authenticatorDouble = new FormLoginAuthenticator(webAuth, webAppSecConfig, webProviderAuthenticatorProxy);
        final HttpSession session = mock.mock(HttpSession.class);
        final Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("param1", "value1");
        final Subject authSubject = new Subject();

        mock.checking(new Expectations() {
            {
                // authenticate(WebRequest, WebAttributes)
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(extReq));
                allowing(webRequest).getHttpServletResponse();
                will(returnValue(resp));

                // handleFormLogin()
                allowing(webAuth).authenticate(webRequest);
                will(returnValue(new AuthenticationResult(AuthResult.SUCCESS, authSubject)));

                // now flow goes into restorePostParams()
                allowing(extReq).getRequestURI();
                will(returnValue("/snoop"));
                allowing(extReq).getMethod();
                will(returnValue("GET"));
                allowing(webAppSecConfig).getPostParamSaveMethod();
                will(returnValue(WebAppSecurityConfig.POST_PARAM_SAVE_TO_SESSION));

                // now flow is in restorePostParamsFromSession()
                allowing(extReq).getSession(false);
                will(returnValue(session));
                allowing(session).getAttribute(PostParameterHelper.INITIAL_URL);
                will(returnValue("/snoop"));
                allowing(extReq).setMethod("POST");
                allowing(session).getAttribute(PostParameterHelper.PARAM_VALUES);
                will(returnValue(paramMap));

                // This is the verification that the parameters were restored to the session
                one(extReq).setInputStreamData((HashMap<String, String>) paramMap);
                one(session).setAttribute(PostParameterHelper.INITIAL_URL, null);
                one(session).setAttribute(PostParameterHelper.PARAM_NAMES, null);
                one(session).setAttribute(PostParameterHelper.PARAM_VALUES, null);
            }
        });

        AuthenticationResult authResult = authenticatorDouble.authenticate(webRequest, webAppSecConfig);
        mock.assertIsSatisfied();
        assertEquals("AuthenticationResult should be SUCCESS", AuthResult.SUCCESS, authResult.getStatus());
    }

    /**
     * Utility method that mimics part of what savePostParamsToCookie() does.
     * 
     * @param params
     * @param sbReqURL
     * @return byte array
     */
    private byte[] cookieToByteArray(byte[][] input, String sbReqURL) {
        byte[] output = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append(Base64Coder.base64Encode(sbReqURL));
            for (int i = 0; i < input.length; i++) {
                sb.append(".").append(StringUtil.toString(Base64Coder.base64Encode(input[i])));
            }
            output = sb.toString().getBytes("UTF-8");
        } catch (Exception e) {
            System.out.println("Caught exception in cookieToByteArray(): " + e);
        }
        return output;

    }

    /**
     * Utility method that verifies whether the specified Cookie
     * matches the Cookie that is instantiated in
     * PostParameterHelper.restorePostParamsFromCookie(), based
     * on name, value, age, and path.
     * 
     * @param cookie
     * @return boolean if the cookie's properties match
     */
    @Factory
    private static Matcher<Cookie> matchingCookie(Cookie cookie) {
        return new CookieMatcher(cookie);
    }
}
