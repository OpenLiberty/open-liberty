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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticateApi;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.UnprotectedResourceService;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

public class AuthenticateApiTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class);
    private final HttpSession session = mock.mock(HttpSession.class);
    private final SSOCookieHelper ssoCookieHelper = mock.mock(SSOCookieHelper.class);
    private final WebAppSecurityConfig config = mock.mock(WebAppSecurityConfig.class);
    private final AuthenticationService authnService = mock.mock(AuthenticationService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final ServletContext mockServletContext = mock.mock(ServletContext.class);
    @SuppressWarnings("unchecked")
    private final AtomicServiceReference<SecurityService> securityServiceRef = mock.mock(AtomicServiceReference.class, "securityServiceRef");
    private final SecurityService securityService = mock.mock(SecurityService.class, "securityService");
    private final String user = "user1";
    private final String password = "user1pwd";
    private final SubjectManager subjectManager = new SubjectManager();

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    @Factory
    private static Matcher<AuthenticationData> matchingAuthenticationData(AuthenticationData authData) {
        return new AuthenticationDataMatcher(authData);
    }

    /**
     * Test successful logout
     */
    @Test
    public void testLogoutSuccess() throws Exception {

        mock.checking(new Expectations() {
            {
                allowing(config).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(config)));

                one(req).getAuthType();
                one(resp).getStatus();
                allowing(ssoCookieHelper).createLogoutCookies(req, resp);
                one(ssoCookieHelper).removeSSOCookieFromResponse(resp);
                one(req).getSession(false);
                will(returnValue(session));
                one(req).getCookies();
                one(session).invalidate();

                allowing(securityServiceRef).getService();
                will(returnValue(securityService));

                allowing(securityService).getAuthenticationService();
                allowing(ssoCookieHelper).getSSOCookiename();
                will(returnValue("ltapToken2"));
                allowing(req).getCookies();
                allowing(req).getRemoteUser();
                allowing(req).getUserPrincipal();
            }
        });

        AuthenticateApi authApi = new AuthenticateApi(ssoCookieHelper, securityServiceRef, null, null, new ConcurrentServiceReferenceMap<String, UnprotectedResourceService>("unprotectedResourceService"));
        authApi.logout(req, resp, config);
    }

    /**
     * Test that logout occurs if there is no HTTP session.
     *
     * @throws Exception
     */
    @Test
    public void testLogout_NoHTTPSession() throws Exception {

        mock.checking(new Expectations() {
            {
                allowing(config).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(config)));

                one(req).getAuthType();
                one(resp).getStatus();
                allowing(ssoCookieHelper).createLogoutCookies(req, resp);
                one(req).getSession(false);
                will(returnValue(null));
                never(session).invalidate();
                one(ssoCookieHelper).removeSSOCookieFromResponse(resp);
                one(req).getCookies();
                allowing(securityServiceRef).getService();
                will(returnValue(securityService));
                allowing(securityService).getAuthenticationService();
                allowing(ssoCookieHelper).getSSOCookiename();
                will(returnValue("ltapToken2"));
                allowing(req).getCookies();
                allowing(req).getRemoteUser();
                allowing(req).getUserPrincipal();
                allowing(securityService).getUserRegistryService();
            }
        });

        AuthenticateApi authApi = new AuthenticateApi(ssoCookieHelper, securityServiceRef, null, null, new ConcurrentServiceReferenceMap<String, UnprotectedResourceService>("unprotectedResourceService"));
        authApi.logout(req, resp, config);
    }

    @Test
    public void testThrowExceptionIfAlreadyAuthenticateCallLogout() throws Exception {
        final List<String> roles = new ArrayList<String>();
        final List<String> groupIds = new ArrayList<String>();
        WSCredentialImpl credential = new WSCredentialImpl("realm", "securityName", "uniqueSecurityName", "UNAUTHENTICATED", "primaryGroupId", "accessId", roles, groupIds);
        Subject subject = new Subject();
        subject.getPublicCredentials().add(credential);

        subjectManager.setCallerSubject(subject);
        mock.checking(new Expectations() {
            {
                allowing(config).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(config)));

                one(req).getAuthType();
                one(resp).getStatus();
                one(ssoCookieHelper).createLogoutCookies(req, resp);
                allowing(config).getWebAlwaysLogin();
                will(returnValue(true));
                one(req).getSession(false);
                will(returnValue(null));
                allowing(ssoCookieHelper).removeSSOCookieFromResponse(resp);
                one(req).getCookies();
                allowing(securityServiceRef).getService();
                will(returnValue(securityService));
                allowing(securityService).getAuthenticationService();
                will(returnValue(authnService));
                allowing(authnService).getAuthCacheService();
                allowing(req).getRemoteUser();
                allowing(req).getUserPrincipal();
                allowing(ssoCookieHelper).getSSOCookiename();
                allowing(req).getCookies();
                allowing(securityService).getUserRegistryService();
            }
        });

        AuthenticateApi authApi = new AuthenticateApi(ssoCookieHelper, securityServiceRef, null, null, new ConcurrentServiceReferenceMap<String, UnprotectedResourceService>("unprotectedResourceService"));
        authApi.throwExceptionIfAlreadyAuthenticate(req, resp, config, null);
        assertEquals("The caller subject must be null", subjectManager.getCallerSubject(), null);
    }

    @Test
    public void testThrowExceptionIfAlreadyAuthenticateThrowException() throws Exception {
        subjectManager.setCallerSubject(new Subject());
        mock.checking(new Expectations() {
            {
                allowing(config).getWebAlwaysLogin();
                will(returnValue(false));
                allowing(securityServiceRef).getService();
                will(returnValue(securityService));
                allowing(securityService).getAuthenticationService();
                will(returnValue(authnService));
                allowing(authnService).getAuthCacheService();
                allowing(req).getRemoteUser();
                allowing(req).getUserPrincipal();
                allowing(ssoCookieHelper).getSSOCookiename();
                allowing(req).getCookies();
            }
        });

        AuthenticateApi authApi = new AuthenticateApi(ssoCookieHelper, securityServiceRef, null, null, new ConcurrentServiceReferenceMap<String, UnprotectedResourceService>("unprotectedResourceService"));

        try {
            authApi.throwExceptionIfAlreadyAuthenticate(req, resp, config, null);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Authentication had been already established"));
        }
    }

    @Test
    public void testPostProgrammaticAuthenticateWithNullCallerSubject() throws Exception {
        subjectManager.setCallerSubject(null);

        final Subject loginSubject = new Subject();
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, loginSubject);
        mock.checking(new Expectations() {
            {
                allowing(ssoCookieHelper).addSSOCookiesToResponse(loginSubject, req, resp);
                allowing(config).getWebAlwaysLogin();
                will(returnValue(false));
                allowing(securityServiceRef).getService();
                will(returnValue(securityService));
                allowing(securityService).getAuthenticationService();
                will(returnValue(authnService));
                allowing(authnService).getAuthCacheService();
                allowing(req).getRemoteUser();
                allowing(req).getUserPrincipal();
                allowing(ssoCookieHelper).getSSOCookiename();
                allowing(req).getCookies();
            }

        });

        AuthenticateApi authApi = new AuthenticateApi(ssoCookieHelper, securityServiceRef, null, null, new ConcurrentServiceReferenceMap<String, UnprotectedResourceService>("unprotectedResourceService"));
        authApi.postProgrammaticAuthenticate(req, resp, authResult);
        assertEquals("The caller subject must be equals to the loginSubject subject.", loginSubject, subjectManager.getCallerSubject());
        assertEquals("The invocation subject must be equals to the loginSubject subject.", loginSubject, subjectManager.getInvocationSubject());
    }

    @Test
    public void testPostProgrammaticAuthenticateWithCallerSubject() throws Exception {
        final Subject loginSubject = createAuthenticatedSubject();
        subjectManager.setCallerSubject(loginSubject);
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, loginSubject);
        mock.checking(new Expectations() {
            {
                allowing(ssoCookieHelper).addSSOCookiesToResponse(loginSubject, req, resp);
                allowing(config).getWebAlwaysLogin();
                will(returnValue(true));
                allowing(securityServiceRef).getService();
                will(returnValue(securityService));
                allowing(securityService).getAuthenticationService();
                will(returnValue(authnService));
                allowing(authnService).getAuthCacheService();
                allowing(req).getRemoteUser();
                allowing(req).getUserPrincipal();
                allowing(ssoCookieHelper).getSSOCookiename();
                allowing(req).getCookies();
            }
        });

        AuthenticateApi authApi = new AuthenticateApi(ssoCookieHelper, securityServiceRef, null, null, new ConcurrentServiceReferenceMap<String, UnprotectedResourceService>("unprotectedResourceService"));
        authApi.postProgrammaticAuthenticate(req, resp, authResult);
        assertSame("The caller subject must be equals to the loginSubject subject.", loginSubject, subjectManager.getCallerSubject());
        assertEquals("The invocation subject must be equals to the loginSubject subject.", loginSubject, subjectManager.getInvocationSubject());
    }

    /**
     */
    @Test
    public void testLoginCallerSubjectAlreadyAuthenticated() throws Exception {
        final Subject subject = createAuthenticatedSubject();
        subjectManager.setCallerSubject(subject);
        mock.checking(new Expectations() {
            {
                one(req).getAuthType();
                one(config).getLogoutOnHttpSessionExpire();
                will(returnValue(false));
                one(req).getRequestedSessionId();
                will(returnValue("abc"));
                one(req).isRequestedSessionIdValid();
                will(returnValue(false));
                one(config).getWebAlwaysLogin();
                will(returnValue(false));
                allowing(securityServiceRef).getService();
                will(returnValue(securityService));
                allowing(securityService).getAuthenticationService();
                will(returnValue(authnService));
                allowing(authnService).getAuthCacheService();
                allowing(req).getRemoteUser();
                allowing(req).getUserPrincipal();
                allowing(ssoCookieHelper).getSSOCookiename();
                allowing(req).getCookies();
                allowing(req).getServletContext();
                will(returnValue(mockServletContext));
                allowing(mockServletContext).getAttribute(with(any(String.class)));
                will(returnValue(null));

            }
        });

        AuthenticateApi authApi = new AuthenticateApi(ssoCookieHelper, securityServiceRef, null, null, new ConcurrentServiceReferenceMap<String, UnprotectedResourceService>("unprotectedResourceService"));
        BasicAuthAuthenticator basicAuthAuthenticator = new BasicAuthAuthenticator(authnService, userRegistry, ssoCookieHelper, config);
        try {
            authApi.login(req, resp, user, password, config, basicAuthAuthenticator);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Authentication had been already established"));
        }
    }

    /**
     */
    @Test
    public void testLoginFailure() throws Exception {
        final String jaasEntryName = JaasLoginConfigConstants.SYSTEM_WEB_INBOUND;
        subjectManager.clearSubjects();
        final AuthenticationData authenticationData = createAuthenticationData(user, password);
        mock.checking(new Expectations() {
            {
                one(config).getLogoutOnHttpSessionExpire();
                will(returnValue(false));
                one(req).getRequestedSessionId();
                will(returnValue("abc"));
                one(req).isRequestedSessionIdValid();
                will(returnValue(false));
                allowing(authnService).authenticate(with(equal(jaasEntryName)), with(matchingAuthenticationData(authenticationData)), with(equal((Subject) null)));
                will(returnValue(null));
                one(ssoCookieHelper).addSSOCookiesToResponse(null, req, resp);
                allowing(securityServiceRef).getService();
                will(returnValue(securityService));
                allowing(securityService).getAuthenticationService();
                will(returnValue(authnService));
                allowing(authnService).getAuthCacheService();
                allowing(req).getRemoteUser();
                allowing(req).getUserPrincipal();
                allowing(ssoCookieHelper).getSSOCookiename();
                allowing(req).getCookies();
                allowing(req).getServletContext();
                will(returnValue(mockServletContext));
                allowing(mockServletContext).getAttribute(with(any(String.class)));
                will(returnValue(null));
            }
        });

        AuthenticateApi authApi = new AuthenticateApi(ssoCookieHelper, securityServiceRef, null, null, new ConcurrentServiceReferenceMap<String, UnprotectedResourceService>("unprotectedResourceService"));
        BasicAuthAuthenticator basicAuthAuthenticator = new BasicAuthAuthenticator(authnService, userRegistry, ssoCookieHelper, config);
        authApi.login(req, resp, user, password, config, basicAuthAuthenticator);
        assertNull(subjectManager.getCallerSubject());
    }

    /**
     */
    @Test
    public void testLoginSuccess() throws Exception {
        final String jaasEntryName = JaasLoginConfigConstants.SYSTEM_WEB_INBOUND;
        subjectManager.clearSubjects();
        final Subject subject = createAuthenticatedSubject();
        final AuthenticationData authenticationData = createAuthenticationData(user, password);
        mock.checking(new Expectations() {
            {
                one(config).getLogoutOnHttpSessionExpire();
                will(returnValue(false));
                one(req).getRequestedSessionId();
                will(returnValue("abc"));
                one(req).isRequestedSessionIdValid();
                will(returnValue(false));
                allowing(authnService).authenticate(with(equal(jaasEntryName)), with(matchingAuthenticationData(authenticationData)), with(equal((Subject) null)));
                will(returnValue(subject));
                one(ssoCookieHelper).addSSOCookiesToResponse(subject, req, resp);
                allowing(securityServiceRef).getService();
                will(returnValue(securityService));
                allowing(securityService).getAuthenticationService();
                will(returnValue(authnService));
                allowing(authnService).getAuthCacheService();
                allowing(req).getRemoteUser();
                allowing(req).getUserPrincipal();
                allowing(ssoCookieHelper).getSSOCookiename();
                allowing(req).getCookies();
                allowing(req).getServletContext();
                will(returnValue(mockServletContext));
                allowing(mockServletContext).getAttribute(with(any(String.class)));
                will(returnValue(null));
            }
        });

        AuthenticateApi authApi = new AuthenticateApi(ssoCookieHelper, securityServiceRef, null, null, new ConcurrentServiceReferenceMap<String, UnprotectedResourceService>("unprotectedResourceService"));
        BasicAuthAuthenticator basicAuthAuthenticator = new BasicAuthAuthenticator(authnService, userRegistry, ssoCookieHelper, config);
        authApi.login(req, resp, user, password, config, basicAuthAuthenticator);
        assertEquals(subject, subjectManager.getCallerSubject());
    }

    private AuthenticationData createAuthenticationData(String username, String password) {
        AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.USERNAME, username);
        authData.set(AuthenticationData.PASSWORD, password.toCharArray());
        return authData;
    }

    private Subject createAuthenticatedSubject() {
        final List<String> roles = new ArrayList<String>();
        final List<String> groupIds = new ArrayList<String>();
        WSCredentialImpl credential = new WSCredentialImpl("realm", user, password, "UNAUTHENTICATED", "primaryGroupId", "accessId", roles, groupIds);
        final Subject subject = new Subject();
        subject.getPublicCredentials().add(credential);
        return subject;
    }

}
