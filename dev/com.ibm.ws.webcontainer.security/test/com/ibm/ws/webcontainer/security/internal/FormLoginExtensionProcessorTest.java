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
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public class FormLoginExtensionProcessorTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
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
    static final String KEY_RELYING_PARTY_SERVICE = "openIdClientService";

    private final AuthenticationService authService = mock.mock(AuthenticationService.class);
    private final BasicAuthAuthenticator basicAuth = mock.mock(BasicAuthAuthenticator.class);
    private final FormLoginConfiguration flcfg = mock.mock(FormLoginConfiguration.class);
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class);
    private final HttpSession session = mock.mock(HttpSession.class);
    private final IServletContext ctx = mock.mock(IServletContext.class);
    private final LoginConfiguration lcfg = mock.mock(LoginConfiguration.class);
    private final SecurityMetadata smd = mock.mock(SecurityMetadata.class);
    private final States testState = mock.states("state-machine-name").startsAs("initial-state");
    private final UserRegistry registry = mock.mock(UserRegistry.class);
    private final WebAppConfigExtended wac = mock.mock(WebAppConfigExtended.class);
    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    private final WebModuleMetaData wmmd = mock.mock(WebModuleMetaData.class);
    final AuthenticationResult authResultContinue = new AuthenticationResult(AuthResult.CONTINUE, "Continue ....");

    private final WebProviderAuthenticatorProxy webProviderAuthenticatorProxy = mock.mock(WebProviderAuthenticatorProxy.class, "webOpAuthProxy");

    protected final org.osgi.service.cm.Configuration config = mock.mock(org.osgi.service.cm.Configuration.class);

    private final StringBuffer reqURL = new StringBuffer("http://localhost:9080/snoop");
    private final String loginErrorPage = "error.jsp";
    private final String errorURL = "http://localhost:9080/error.jsp";

    private Cookie invalidatedReferrerURLCookie;

    /**
     * common set of Expectations shared by all the
     * test methods
     * 
     * @throws Exception
     * 
     */
    @Before
    public void setup() throws Exception {
        mock.checking(new Expectations() {
            {
                // code in FormLoginExtensionProcessor ctor
                // that is shared by all test methods
                allowing(ctx).getWebAppConfig();
                will(returnValue(wac));
                allowing(wac).getMetaData();
                will(returnValue(wmmd));
                allowing(wac).getApplicationName();
                will(returnValue("Unittest_App"));
                allowing(wmmd).getSecurityMetaData();
                will(returnValue(smd));
                allowing(smd).getLoginConfiguration();
                will(returnValue(lcfg));
                allowing(lcfg).getFormLoginConfiguration();
                will(returnValue(flcfg));
                allowing(flcfg).getErrorPage();
                will(returnValue(loginErrorPage));
                when(testState.is("initial-state"));

                allowing(webAppSecConfig).createSSOCookieHelper();
                will(returnValue(new SSOCookieHelperImpl(webAppSecConfig)));
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));

                // formLogin() method
                allowing(webAppSecConfig).getLogoutOnHttpSessionExpire();
                will(returnValue(true));
                allowing(req).getRequestedSessionId();
                will(returnValue("123"));
                allowing(req).isRequestedSessionIdValid();
                will(returnValue(false));
                allowing(req).getSession(true);
                will(returnValue(session));

                // Divert to setUpAFullUrl()
                allowing(req).getRequestURL();
                will(returnValue(reqURL));
                allowing(req).getContextPath();
                will(returnValue(""));

                // For calls to ReferrerURLCookieHandler.clearReferrerURLCookie(), 
                // which calls invalidateReferrerURLCookie()
                invalidatedReferrerURLCookie = new Cookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, "");
                invalidatedReferrerURLCookie.setPath("/");
                invalidatedReferrerURLCookie.setMaxAge(0);
            }
        });
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Test scenario where SSO is disabled.
     */
    @Test
    public void testHandleRequest_ssoDisabled() throws Exception {

        mock.checking(new Expectations() {
            {
                one(webAppSecConfig).isSingleSignonEnabled();
                will(returnValue(false));
                one(webProviderAuthenticatorProxy).authenticate(req, resp, null);
                will(returnValue(authResultContinue));

                // These expectations verify the path that
                // SSO is disabled is driven.
                one(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                allowing(resp).encodeURL(errorURL);
                will(returnValue(errorURL));
                allowing(resp).sendRedirect(errorURL);
                allowing(webProviderAuthenticatorProxy).authenticate(req, resp, null);
                will(returnValue(authResultContinue));
                allowing(req).getParameter("error_page");
                allowing(req).getCookies();
            }
        });

        FormLoginExtensionProcessor processorDouble = new FormLoginExtensionProcessor(webAppSecConfig, authService, registry, ctx, webProviderAuthenticatorProxy, null);
        processorDouble.handleRequest(req, resp);

        assertTrue("Expected message was not logged",
                   outputMgr.checkForStandardErr("CWWKS9106E: SSO Configuration error. FormLogin is configured for web application "
                                                 + "Unittest_App but SSO is not enabled in the security settings.  SSO must be enabled to use FormLogin."));
    }

    /**
     * Test scenario where user can successfully authenticate and
     * user is authorized to access the resource.
     * 
     * @throws Exception
     */
    @Test
    public void testHandleRequest_ValidUser() throws Exception {

        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, "Authentication was successful!");
        final String username = "user1";
        final String password = "user1pwd";
        final Subject authSubject = new Subject();
        final String cookieValue = "http://localhost:9080/snoop";
        final Cookie cookie = new Cookie("WASReqURL", cookieValue);
        final Cookie cookie2 = new Cookie("WASReLoginURL", cookieValue);
        final Cookie[] cookieArray = { cookie, cookie2 };

        mock.checking(new Expectations() {
            {
                // Back in formLogin()
                allowing(req).getParameter("j_username");
                will(returnValue(username));
                allowing(req).getParameter("j_password");
                will(returnValue(password));
                allowing(basicAuth).basicAuthenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, username, password, req, resp);
                will(returnValue(authResult));
                allowing(authService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(any(AuthenticationData.class)), with(equal((Subject) null)));
                will(returnValue(authSubject));
                one(webProviderAuthenticatorProxy).authenticate(req, resp, null);
                will(returnValue(authResultContinue));
                allowing(webAppSecConfig).isSingleSignonEnabled();
                will(returnValue(true));
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));

                // For calls to ReferrerURLCookieHandler, starting with clearReferrerURLCookie()
                allowing(req).getCookies();
                will(returnValue(cookieArray));
                allowing(resp).addCookie(with(matchingCookie(invalidatedReferrerURLCookie)));
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webAppSecConfig).getWASReqURLRedirectDomainNames();

                allowing(resp).encodeURL(cookieValue);
                will(returnValue(cookieValue));
                allowing(resp).sendRedirect(cookieValue);
                // HTTP status should not be set at this point, for successful login
                never(resp).setStatus(with(any(int.class)));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                one(req).isSecure();
                will(returnValue(true));

                allowing(req).getProtocol();
                will(returnValue("HTTP/1.0"));
                allowing(resp).isCommitted();
                will(returnValue(false));
            }
        });

        FormLoginExtensionProcessor processorDouble = new FormLoginExtensionProcessor(webAppSecConfig, authService, registry, ctx, webProviderAuthenticatorProxy, null);
        processorDouble.handleRequest(req, resp);
        assertEquals("AuthenticationResult should be SUCCESS", AuthResult.SUCCESS, authResult.getStatus());
    }

    /*
     * Test Scenario: Hostname in current request does not match WASReqURL hostname and a matching domain is not found
     * in wasReqURLRedirectDomainNames property.
     */
    @Test(expected = RuntimeException.class)
    public void testHandleRequest_InvalidWASReqURL() throws Exception {

        final Subject authSubject = new Subject();
        final String cookieValue = "http://badhost.myhacker.com:9080/snoop";
        final Cookie cookie = new Cookie("WASReqURL", cookieValue);
        final Cookie cookie2 = new Cookie("WASReLoginURL", cookieValue);
        final Cookie[] cookieArray = { cookie, cookie2 };
        final List<String> domainList = Arrays.asList("ok.com", "good.com");

        mock.checking(new Expectations() {
            {
                allowing(webAppSecConfig).isSingleSignonEnabled();
                will(returnValue(true));
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(req).isSecure();
                will(returnValue(true));

                allowing(req).getCookies();
                will(returnValue(cookieArray));
                allowing(resp).addCookie(with(matchingCookie(invalidatedReferrerURLCookie)));
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webAppSecConfig).getWASReqURLRedirectDomainNames();
                will(returnValue(domainList));

                allowing(resp).encodeURL(cookieValue);
                will(returnValue(cookieValue));
                allowing(resp).sendRedirect(cookieValue);
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                allowing(resp).isCommitted();
                will(returnValue(false));
            }
        });

        FormLoginExtensionProcessor processorDouble = new FormLoginExtensionProcessor(webAppSecConfig, authService, registry, ctx, webProviderAuthenticatorProxy, null);
        processorDouble.postFormLoginProcess(req, resp, authSubject);
    }

    /*
     * Test Scenario: Hostname in current request does not match WASReqURL hostname. However a matching domain is found
     * in wasReqURLRedirectDomainNames property.
     */
    @Test
    public void testHandleRequest_WASReqURLHostFoundInDomainlist() throws Exception {

        final Subject authSubject = new Subject();
        final String cookieValue = "http://badhost.myhacker.com:9080/snoop";
        final Cookie cookie = new Cookie("WASReqURL", cookieValue);
        final Cookie cookie2 = new Cookie("WASReLoginURL", cookieValue);
        final Cookie[] cookieArray = { cookie, cookie2 };
        final List<String> domainList = Arrays.asList("ok.com", "myhacker.com");

        mock.checking(new Expectations() {
            {
                allowing(webAppSecConfig).isSingleSignonEnabled();
                will(returnValue(true));
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(req).isSecure();
                will(returnValue(true));

                allowing(req).getCookies();
                will(returnValue(cookieArray));
                allowing(resp).addCookie(with(matchingCookie(invalidatedReferrerURLCookie)));
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webAppSecConfig).getWASReqURLRedirectDomainNames();
                will(returnValue(domainList));

                allowing(resp).encodeURL(cookieValue);
                will(returnValue(cookieValue));
                allowing(resp).sendRedirect(cookieValue);
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                allowing(resp).isCommitted();
                will(returnValue(false));
            }
        });

        FormLoginExtensionProcessor processorDouble = new FormLoginExtensionProcessor(webAppSecConfig, authService, registry, ctx, webProviderAuthenticatorProxy, null);
        processorDouble.postFormLoginProcess(req, resp, authSubject);
    }

    /**
     * Test scenario where user ID and password are null
     * 
     * @throws Exception
     */
    @Test
    public void testHandleRequest_NullUserPassword() throws Exception {

        mock.checking(new Expectations() {
            {
                one(webAppSecConfig).isSingleSignonEnabled();
                will(returnValue(false));
                one(webProviderAuthenticatorProxy).authenticate(req, resp, null);
                will(returnValue(authResultContinue));

                // Back in formLogin()
                allowing(req).getParameter("j_username");
                will(returnValue(null));
                allowing(req).getParameter("j_password");
                will(returnValue(null));

                // Flow never gets to BasicAuthAuthenticator.basicAuthenticate()  
                // because username and password = null.
                // The below verifies whether the 401 status is set for unauthenticated requests
                one(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                allowing(resp).encodeURL(errorURL);
                will(returnValue(errorURL));
                allowing(resp).sendRedirect(errorURL);
                allowing(req).getParameter("error_page");
                allowing(req).getCookies();
            }
        });

        FormLoginExtensionProcessor processorDouble = new FormLoginExtensionProcessor(webAppSecConfig, authService, registry, ctx, webProviderAuthenticatorProxy, null);
        processorDouble.handleRequest(req, resp);
    }

    /**
     * Test scenario where user is not able to authenticate.
     * 
     * @throws Exception
     */
    @Test
    public void testHandleRequest_InvalidUser() throws Exception {

        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.SEND_401, "Authentication failed");
        final String cookieValue = "http://localhost:9080/snoop";
        final String username = "user2";
        final String password = "user2pwd";
        final Cookie cookie = new Cookie("WASReqURL", cookieValue);
        final Cookie[] cookieArray = { cookie };

        mock.checking(new Expectations() {
            {
                one(webAppSecConfig).isSingleSignonEnabled();
                will(returnValue(false));
                one(webProviderAuthenticatorProxy).authenticate(req, resp, null);
                will(returnValue(authResultContinue));

                // Back in formLogin()
                allowing(req).getParameter("j_username");
                will(returnValue(username));
                allowing(req).getParameter("j_password");
                will(returnValue(password));
                allowing(basicAuth).basicAuthenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, username, password, req, resp);
                will(returnValue(authResult));
                allowing(authService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(any(AuthenticationData.class)), with(equal((Subject) null)));
                will(throwException(new AuthenticationException("Authentication failed")));

                // This verifies whether the 401 status is set for unauthenticated requests
                one(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                allowing(resp).encodeURL(errorURL);
                will(returnValue(errorURL));
                allowing(resp).sendRedirect(errorURL);

                // For code in ReferrerURLCookieHandler, starting with clearReferrerURLCookie()
                allowing(req).getCookies();
                will(returnValue(cookieArray));
                allowing(resp).addCookie(with(matchingCookie(invalidatedReferrerURLCookie)));

                // Back in formLogin()
                allowing(resp).encodeURL(cookieValue);
                will(returnValue(cookieValue));
                allowing(resp).sendRedirect(cookieValue);
                allowing(req).getParameter("error_page");
                allowing(req).getCookies();
            }
        });

        FormLoginExtensionProcessor processorDouble = new FormLoginExtensionProcessor(webAppSecConfig, authService, registry, ctx, webProviderAuthenticatorProxy, null);
        processorDouble.handleRequest(req, resp);
        assertEquals("AuthenticationResult should be SEND_401", AuthResult.SEND_401, authResult.getStatus());
    }

    /**
     * Utility method that verifies whether the specified Cookie
     * matches the Cookie that is instantiated in
     * ReferrerURLCookieHandler.invalidateReferrerURLCookie(), based
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
