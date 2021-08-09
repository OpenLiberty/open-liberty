/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;

import javax.security.auth.Subject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.internal.CertificateLoginAuthenticator;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import test.common.SharedOutputManager;

/**
 *
 */
public class UserAuthenticationTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule outputRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final HttpServletRequest request = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock.mock(HttpServletResponse.class);
    private final OAuth20Provider provider = mock.mock(OAuth20Provider.class);
    private final OAuthResult result = mock.mock(OAuthResult.class);
    @SuppressWarnings("rawtypes")
    private final AtomicServiceReference serviceReference = mock.mock(AtomicServiceReference.class);
    private final ServletContext servletContext = mock.mock(ServletContext.class);
    private final SecurityService securityService = mock.mock(SecurityService.class);
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final AuthenticationService authenticationService = mock.mock(AuthenticationService.class);
    private final WebAppSecurityConfig config = mock.mock(WebAppSecurityConfig.class);
    private X509Certificate cert = null;
    private final RequestDispatcher rd = mock.mock(RequestDispatcher.class);
    private final AttributeList al = mock.mock(AttributeList.class);
    private final HttpSession hs = mock.mock(HttpSession.class);
    private final Principal ppl = mock.mock(Principal.class);
    private final OidcOAuth20ClientProvider ocp = mock.mock(OidcOAuth20ClientProvider.class);
    private final OidcBaseClient obc = mock.mock(OidcBaseClient.class);
    private final String thisAuthMech = JaasLoginConfigConstants.SYSTEM_WEB_INBOUND;

    private final String AUTH_HEADER_NAME = "Authorization";
    private final String AUTH_ENCODING_HEADER_NAME = "Authorization-Encoding";
    private final String username = "SujeetAndJoe";
    private final String password = "OneLastProjectTogetherItWasFun";
    private final String basicHeader = "Basic U3VqZWV0QW5kSm9lOk9uZUxhc3RQcm9qZWN0VG9nZXRoZXJJdFdhc0Z1bg==";
    private final AuthenticationData authenticationData = new WSAuthenticationData();
    private final String userRole = "Role1";
    private final static String ATTR_PROMPT = "prompt";
    private static final String ATTR_OAUTH_RESULT = "oauthResult";
    private static final String notInRoleMsg = "The user is not authenticated, or is not in the role that is required to complete this request";

    @Before
    public void setUp() throws Exception {
        InputStream inStream = new FileInputStream("test" + File.separator + "resources" + File.separator + "gooduser.cer");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        cert = (X509Certificate) cf.generateCertificate(inStream);
        inStream.close();

        authenticationData.set(AuthenticationData.USERNAME, username);
        authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());

        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(config);
    }

    @After
    public void after() {
        mock.assertIsSatisfied();
    }

    /**
     * Tests Authentication with following conditions
     * 1.User principal is null
     * 2.Authentication type is BASIC
     * Expected result: no error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationNormalBasic() {
        final String methodName = "handleAuthenticationNormalBasic";
        final Subject subject = new Subject();
        final AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, username);
        authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());
        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    allowing(request).isSecure();
                    will(returnValue(true));
                    allowing(request).isUserInRole(userRole);
                    will(returnValue(true));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    allowing(config).getSSORequiresSSL();
                    will(returnValue(true));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(authenticationService).authenticate(with(equal(thisAuthMech)), with(matchingAuthenticationData(authenticationData)), with(equal((Subject) null)));
                    will(returnValue(subject));
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            assertEquals(OAuthResult.STATUS_OK, or.getStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests Authentication with following conditions
     * 1.User principal is null
     * 2.Authentication type is CERT
     * Expected result: no error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationNormalCert() {
        final String methodName = "handleAuthenticationNormalCert";
        final Subject subject = new Subject();

        final X509Certificate cc[] = new X509Certificate[] { cert, null };
        final AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.HTTP_SERVLET_REQUEST, request);
        authData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, response);

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    one(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    allowing(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(cc));
                    one(request).getUserPrincipal();
                    will(returnValue(null));
                    one(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    one(provider).isCertAuthentication();
                    will(returnValue(true));
                    one(serviceReference).getService();
                    will(returnValue(securityService));
                    one(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    one(authenticationService).authenticate(with(equal(thisAuthMech)), with(matchingAuthenticationData(authData)), with(equal((Subject) null)));
                    will(returnValue(subject));
                    one(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    one(request).isSecure();
                    will(returnValue(true));
                    one(config).getSSORequiresSSL();
                    will(returnValue(true));
                    one(request).isUserInRole(userRole);
                    will(returnValue(true));
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            assertEquals(OAuthResult.STATUS_OK, or.getStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests Authentication with following conditions
     * 1. User principal is null
     * 2. Authentication type is CERT
     * 3. certAuthentication is set to false
     * Expected result: null object is returned, redirect to the form login page.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationNormalCert_certAuthenticationDisabled() {
        final String methodName = "handleAuthenticationNormalCert_certAuthenticationDisabled";

        final X509Certificate cc[] = new X509Certificate[] { cert, null };

        final String requestUri = "/test";
        final String loginUrl = OAuth20Constants.DEFAULT_AUTHZ_LOGIN_URL;
        final String contextPath = "/oidc10";
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    allowing(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(cc));
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(provider).isAllowCertAuthentication();
                    will(returnValue(false));
                    allowing(config).getSameSiteCookie();
                    will(returnValue("Disabled"));
                }
            });
            sendForLoginExpectations(requestUri, contextPath, loginUrl);

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            assertNull(or);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests Authentication with following conditions
     * 1. User principal is null
     * 2. Authentication type is CERT
     * 3. certAuthentication is set to false
     * 4. allowCertAuthentication is set to true
     * Expected result: no error, should still handle as certificate authentication
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleBasicAuthentication_certAuthenticationDisabled_allowCertAuthentication() {
        final String methodName = "handleBasicAuthentication_certAuthenticationDisabled_allowCertAuthentication";
        final Subject subject = new Subject();

        final X509Certificate cc[] = new X509Certificate[] { cert, null };
        final AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.HTTP_SERVLET_REQUEST, request);
        authData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, response);

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    one(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    allowing(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(cc));
                    one(request).getUserPrincipal();
                    will(returnValue(null));
                    one(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    one(provider).isCertAuthentication();
                    will(returnValue(false));
                    one(serviceReference).getService();
                    will(returnValue(securityService));
                    one(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    one(authenticationService).authenticate(with(equal(thisAuthMech)), with(matchingAuthenticationData(authData)), with(equal((Subject) null)));
                    will(returnValue(subject));
                    one(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    one(request).isSecure();
                    will(returnValue(true));
                    one(config).getSSORequiresSSL();
                    will(returnValue(true));
                    one(request).isUserInRole(userRole);
                    will(returnValue(true));
                    allowing(provider).isAllowCertAuthentication();
                    will(returnValue(true));
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            assertEquals(OAuthResult.STATUS_OK, or.getStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests Authentication with following conditions
     * 1.prompt is set as none
     * Expected result: login_required error
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationPromptNone() {
        final String methodName = "handleAuthenticationPromptNone";
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_NONE));
                    one(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    one(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(null));
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            assertNotNull(or);
            assertEquals(OAuthResult.STATUS_FAILED, or.getStatus());
            assertEquals(OIDCConstants.ERROR_LOGIN_REQUIRED, or.getCause().getError());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.prompt is not set.
     * 2. Auth type is set as None.
     * Expected result: null object is returned, redirect to the form login page.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationAuthTypeNonePromptNone() {
        final String methodName = "handleAuthenticationAuthTypeNonePromptNone";
        final String requestUri = "/test";
        final String loginUrl = OAuth20Constants.DEFAULT_AUTHZ_LOGIN_URL;
        final String contextPath = "/oidc10";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    one(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(null));
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(null));
                    allowing(config).getSameSiteCookie();
                    will(returnValue("Disabled"));
                }
            });
            sendForLoginExpectations(requestUri, contextPath, loginUrl);

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            assertNull(or);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.prompt is set as login and concent
     * 2. Auth type is set as None.
     * Expected result: null object is returned, no redirection.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationAuthTypeNonePromptHasLogin() {
        final String methodName = "handleAuthenticationAuthTypeNonePromptHasLogin";
        final String requestUri = "/test";
        final String loginUrl = OAuth20Constants.DEFAULT_AUTHZ_LOGIN_URL;
        final String contextPath = "/oidc10";
        try {
            mock.checking(new Expectations() {
                {

                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    one(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(null));
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    one(request).getUserPrincipal();
                    will(returnValue(ppl));
                    one(request).logout();
                    allowing(config).getSameSiteCookie();
                    will(returnValue("Disabled"));
                }
            });
            sendForLoginExpectations(requestUri, contextPath, loginUrl);

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            assertNull(or);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.prompt is set as login and concent
     * 2. Auth type is set as Basic.
     * 3. Authenticated user isn't in role.
     * Expected result: 403 error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationAuthTypeBasicPromptHasLoginInvalidUserRole() {
        final String methodName = "handleAuthenticationAuthTypeBasicPromptHasLoginInvalidUserRole";
        final Subject subject = new Subject();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    allowing(request).isUserInRole(userRole);
                    will(returnValue(false));
                    allowing(request).isSecure();
                    will(returnValue(true));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    allowing(config).getSSORequiresSSL();
                    will(returnValue(false));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(config).createReferrerURLCookieHandler();
                    will(returnValue(new ReferrerURLCookieHandler(config)));
                    allowing(authenticationService).authenticate(with(equal(thisAuthMech)), with(matchingAuthenticationData(authenticationData)), with(equal((Subject) null)));
                    will(returnValue(subject));
                    one(response).sendError(HttpServletResponse.SC_FORBIDDEN);
                    never(response).addCookie(with(any(Cookie.class)));
                    never(response).sendRedirect(with(any(String.class)));
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            assertNotNull(or);
            assertEquals(OAuthResult.STATUS_FAILED, or.getStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.prompt is set as login and concent
     * 2. Auth type is set as Basic.
     * 3. Authenticatication fails.
     * Expected result: login_required message.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationAuthTypeBasicPromptHasLoginAuthenticationFailure() {
        final String methodName = "handleAuthenticationAuthTypeBasicPromptHasLoginAuthenticationFailure";
        final String basicHeader_malformed = "Basic dGVzdDEyMw==";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(config).isIncludePathInWASReqURL();
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    allowing(request).getUserPrincipal();
                    will(returnValue(ppl));
                    one(request).logout();
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader_malformed));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(config).createReferrerURLCookieHandler();
                    will(returnValue(new ReferrerURLCookieHandler(config)));
                    one(response).setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            assertNotNull(or);
            assertEquals("Does not get failed AuthResult", OAuthResult.STATUS_FAILED, or.getStatus());
            assertTrue("Does not get OAuth20Exception", or.getCause() instanceof OAuth20Exception);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.User principal is null
     * 2.Authentication type is BASIC
     * 3.OAuthResult is valid
     * The expected result is
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationWithOAuthResultNormal() {
        final String methodName = "handleAuthenticationWithOAuthResultNormal";
        final WSPrincipal principal = new WSPrincipal("test", "test", "auth_method");
        final HashSet<Principal> principals = new HashSet<Principal>();
        principals.add(principal);
        final HashSet<String> credentials = new HashSet<String>();
        credentials.add("credential");
        final Subject subject = new Subject(true, principals, credentials, credentials);
        final AttributeList attrs = new AttributeList();
        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    one(result).getAttributeList();
                    will(returnValue(attrs));
                    allowing(request).isSecure();
                    will(returnValue(true));
                    allowing(request).isUserInRole(userRole);
                    will(returnValue(true));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    allowing(config).getSSORequiresSSL();
                    will(returnValue(true));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(authenticationService).authenticate(with(equal(thisAuthMech)), with(matchingAuthenticationData(authenticationData)), with(equal((Subject) null)));
                    will(returnValue(subject));
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthenticationWithOAuthResult(provider, request, response, prompt, serviceReference, servletContext, userRole, result);
            assertEquals(OAuthResult.STATUS_OK, or.getStatus());
            assertEquals(attrs, or.getAttributeList());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.User principal is null
     * 2.Authentication type is BASIC
     * 3.OAuthResult is null
     * The expected result is
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationWithOAuthResultNormalAuthResultNull() {
        final String methodName = "handleAuthenticationWithOAuthResultNormalAuthResultNull";
        final WSPrincipal principal = new WSPrincipal("test", "test", "auth_method");
        final HashSet<Principal> principals = new HashSet<Principal>();
        principals.add(principal);
        final HashSet<String> credentials = new HashSet<String>();
        credentials.add("credential");
        final Subject subject = new Subject(true, principals, credentials, credentials);
        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    allowing(request).isSecure();
                    will(returnValue(true));
                    allowing(request).isUserInRole(userRole);
                    will(returnValue(true));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    allowing(config).getSSORequiresSSL();
                    will(returnValue(true));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(config).createReferrerURLCookieHandler();
                    will(returnValue(new ReferrerURLCookieHandler(config)));
                    allowing(authenticationService).authenticate(with(equal(thisAuthMech)), with(matchingAuthenticationData(authenticationData)), with(equal((Subject) null)));
                    will(returnValue(subject));
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthenticationWithOAuthResult(provider, request, response, prompt, serviceReference, servletContext, userRole, null);
            assertEquals(OAuthResult.STATUS_OK, or.getStatus());
            assertNotNull(or.getAttributeList());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.User principal is null
     * 2.Authentication type is BASIC
     * Expected result: successful login
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationWithOAuthResultPromptNone() {
        final String methodName = "handleAuthenticationWithOAuthResultPromptNone";
        final WSPrincipal principal = new WSPrincipal("test", "test", "auth_method");
        final HashSet<Principal> principals = new HashSet<Principal>();
        principals.add(principal);
        final HashSet<String> credentials = new HashSet<String>();
        credentials.add("credential");
        final Subject subject = new Subject(true, principals, credentials, credentials);
        final AttributeList attrs = new AttributeList();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_NONE));
                    one(result).getAttributeList();
                    will(returnValue(attrs));
                    allowing(request).isSecure();
                    will(returnValue(true));
                    allowing(request).isUserInRole(userRole);
                    will(returnValue(true));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    allowing(config).getSSORequiresSSL();
                    will(returnValue(true));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(authenticationService).authenticate(with(equal(thisAuthMech)), with(matchingAuthenticationData(authenticationData)), with(equal((Subject) null)));
                    will(returnValue(subject));
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthenticationWithOAuthResult(provider, request, response, prompt, serviceReference, servletContext, userRole, result);
            assertNotNull(or);
            assertEquals(OAuthResult.STATUS_OK, or.getStatus());
            assertEquals(attrs, or.getAttributeList());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.prompt is set as login and concent
     * 2. Auth type is set as Basic.
     * 3. Authenticated user isn't in role.
     * Expected result: 403 error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationWithOAuthResultAuthTypeBasicPromptHasLoginInvalidUserRole() {
        final String methodName = "handleAuthenticationWithOAuthResultAuthTypeBasicPromptHasLoginInvalidUserRole";
        final WSPrincipal principal = new WSPrincipal("test", "test", "auth_method");
        final HashSet<Principal> principals = new HashSet<Principal>();
        principals.add(principal);
        final HashSet<String> credentials = new HashSet<String>();
        credentials.add("credential");
        final Subject subject = new Subject(true, principals, credentials, credentials);
        final AttributeList attrs = new AttributeList();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    one(result).getAttributeList();
                    will(returnValue(attrs));
                    allowing(request).getUserPrincipal();
                    will(returnValue(ppl));
                    one(request).logout();
                    allowing(request).isUserInRole(userRole);
                    will(returnValue(false));
                    allowing(request).isSecure();
                    will(returnValue(true));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    allowing(config).getSSORequiresSSL();
                    will(returnValue(false));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(config).createReferrerURLCookieHandler();
                    will(returnValue(new ReferrerURLCookieHandler(config)));
                    allowing(authenticationService).authenticate(with(equal(thisAuthMech)), with(matchingAuthenticationData(authenticationData)), with(equal((Subject) null)));
                    will(returnValue(subject));
                    one(response).sendError(HttpServletResponse.SC_FORBIDDEN);
                    never(response).addCookie(with(any(Cookie.class)));
                    never(response).sendRedirect(with(any(String.class)));
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthenticationWithOAuthResult(provider, request, response, prompt, serviceReference, servletContext, userRole, result);
            assertNotNull(or);
            assertEquals(OAuthResult.STATUS_FAILED, or.getStatus());
            assertEquals(attrs, or.getAttributeList());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.prompt is set as login and concent
     * 2. Auth type is set as Basic.
     * 3. Authenticated user isn't in role.
     * Expected result: 403 error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationWithOAuthResultAuthTypeBasicPromptHasLoginInvalidUserRoleAuthResultNull() {
        final String methodName = "handleAuthenticationWithOAuthResultAuthTypeBasicPromptHasLoginInvalidUserRoleAuthResultNull";
        final WSPrincipal principal = new WSPrincipal("test", "test", "auth_method");
        final HashSet<Principal> principals = new HashSet<Principal>();
        principals.add(principal);
        final HashSet<String> credentials = new HashSet<String>();
        credentials.add("credential");
        final Subject subject = new Subject(true, principals, credentials, credentials);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    one(request).getUserPrincipal();
                    will(returnValue(null));
                    allowing(request).isUserInRole(userRole);
                    will(returnValue(false));
                    allowing(request).isSecure();
                    will(returnValue(true));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    allowing(config).getSSORequiresSSL();
                    will(returnValue(false));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(config).createReferrerURLCookieHandler();
                    will(returnValue(new ReferrerURLCookieHandler(config)));
                    allowing(authenticationService).authenticate(with(equal(thisAuthMech)), with(matchingAuthenticationData(authenticationData)), with(equal((Subject) null)));
                    will(returnValue(subject));
                    one(response).sendError(HttpServletResponse.SC_FORBIDDEN);
                    never(response).addCookie(with(any(Cookie.class)));
                    never(response).sendRedirect(with(any(String.class)));
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthenticationWithOAuthResult(provider, request, response, prompt, serviceReference, servletContext, userRole, null);
            assertNotNull(or);
            assertEquals(OAuthResult.STATUS_FAILED, or.getStatus());
            assertNotNull(or.getAttributeList());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.prompt is set as login and consent
     * 2. Auth type is set as Basic.
     * 3. Authenticatication fails.
     * Expected result: Access should be denied, 401 returned
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationWithOAuthResultAuthTypeBasicPromptHasLoginAuthenticationFailure() {
        final String methodName = "handleAuthenticationWithOAuthResultAuthTypeBasicPromptHasLoginAuthenticationFailure";
        final String basicHeader_malformed = "Basic dGVzdDEyMw==";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(config).isIncludePathInWASReqURL();
                    allowing(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    allowing(request).getUserPrincipal();
                    will(returnValue(ppl));
                    one(request).logout();
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader_malformed));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(config).createReferrerURLCookieHandler();
                    will(returnValue(new ReferrerURLCookieHandler(config)));
                    one(request).getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME);
                    will(returnValue(null));
                    one(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }
            });

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthenticationWithOAuthResult(provider, request, response, prompt, serviceReference, servletContext, userRole, null);
            assertNotNull(or);
            assertEquals("Does not get failed AuthResult", OAuthResult.STATUS_FAILED, or.getStatus());
            assertTrue("Does not get OAuth20AccessDeniedException", or.getCause() instanceof OAuth20AccessDeniedException);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.prompt is not set.
     * 2. Auth type is set as None.
     * Expected result: null object is returned, redirect to the form login page.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationWithOAuthResultAuthTypeNonePromptNone() {
        final String methodName = "handleAuthenticationWithOAuthResultAuthTypeNonePromptNone";
        final String requestUri = "/test";
        final String loginUrl = OAuth20Constants.DEFAULT_AUTHZ_LOGIN_URL;
        final String contextPath = "/oidc10";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    one(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(null));
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(null));
                    allowing(provider).isAllowSpnegoAuthentication();
                    will(returnValue(false));
                    allowing(config).getSameSiteCookie();
                    will(returnValue("Disabled"));
                }
            });
            sendForLoginExpectations(requestUri, contextPath, loginUrl);

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthenticationWithOAuthResult(provider, request, response, prompt, serviceReference, servletContext, userRole, null);
            assertNull(or);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests Authentication with following conditions
     * 1.prompt is set as login.
     * 2. Auth type is set as None.
     * Expected result: null object is returned, redirect to the form login page.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleAuthenticationWithOAuthResultAuthTypeNonePromptLogin() {
        final String methodName = "handleAuthenticationWithOAuthResultAuthTypeNonePromptLogin";
        final String requestUri = "/test";
        final String loginUrl = OAuth20Constants.DEFAULT_AUTHZ_LOGIN_URL;
        final String contextPath = "/oidc10";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    one(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(null));
                    one(request).getUserPrincipal();
                    will(returnValue(ppl));
                    one(request).logout();
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_LOGIN + " " + Constants.PROMPT_CONSENT));
                    allowing(provider).isAllowSpnegoAuthentication();
                    will(returnValue(false));
                    allowing(config).getSameSiteCookie();
                    will(returnValue("Disabled"));
                }
            });
            sendForLoginExpectations(requestUri, contextPath, loginUrl);

            Prompt prompt = new Prompt(request);
            UserAuthentication userAuthentication = new UserAuthentication();
            OAuthResult or = userAuthentication.handleAuthenticationWithOAuthResult(provider, request, response, prompt, serviceReference, servletContext, userRole, null);
            assertNull(or);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests Authentication with following conditions
     * 1.User principal is null
     * 2.Authentication type is BASIC
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleBasicAuthentication() {
        final String methodName = "handleBasicAuthentication";
        final Subject subject = new Subject();
        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    allowing(request).isSecure();
                    will(returnValue(true));
                    allowing(request).isUserInRole(userRole);
                    will(returnValue(true));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    allowing(config).getSSORequiresSSL();
                    will(returnValue(true));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(config).createReferrerURLCookieHandler();
                    will(returnValue(new ReferrerURLCookieHandler(config)));
                    allowing(authenticationService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(matchingAuthenticationData(authenticationData)),
                            with(equal((Subject) null)));
                    will(returnValue(subject));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            userAuthentication.handleBasicAuthenticationWithRequiredRole(provider, request, response, serviceReference, servletContext, userRole);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.User principal is null
     * 2.Authentication type is BASIC
     * 3.Malformed BASIC header
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleBasicAuthenticationMalformedHeader() {
        final String methodName = "handleBasicAuthenticationMalformedHeader";
        final String basicHeader_malformed = "Basic dGVzdDEyMw==";

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    allowing(request).isSecure();
                    will(returnValue(true));
                    allowing(request).isUserInRole(userRole);
                    will(returnValue(true));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader_malformed));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    allowing(config).getSSORequiresSSL();
                    will(returnValue(true));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(config).createReferrerURLCookieHandler();
                    will(returnValue(new ReferrerURLCookieHandler(config)));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            userAuthentication.handleBasicAuthenticationWithRequiredRole(provider, request, response, serviceReference, servletContext, userRole);
        } catch (OidcServerException oidcExc) {
            assertEquals(notInRoleMsg, oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_ACCESS_DENIED, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    /**
     * Tests Authentication with following conditions
     * 1.User principal is null
     * 2.Authentication type is BASIC
     * 3.User does not belong to required role
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleBasicAuthenticationUserNotInRole() {
        final String methodName = "handleBasicAuthentication";
        final Subject subject = new Subject();

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    allowing(request).isSecure();
                    will(returnValue(true));
                    allowing(request).isUserInRole(userRole);
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(basicHeader));
                    allowing(provider).isCertAuthentication();
                    will(returnValue(false));
                    allowing(request).getHeader(AUTH_ENCODING_HEADER_NAME);
                    will(returnValue("UTF-8"));
                    allowing(serviceReference).getService();
                    will(returnValue(securityService));
                    allowing(securityService).getUserRegistryService();
                    will(returnValue(userRegistryService));
                    allowing(securityService).getAuthenticationService();
                    will(returnValue(authenticationService));
                    allowing(userRegistryService).isUserRegistryConfigured();
                    will(returnValue(true));
                    allowing(userRegistryService).getUserRegistry();
                    will(returnValue(userRegistry));
                    allowing(config).getDisplayAuthenticationRealm();
                    will(returnValue(false));
                    allowing(config).isSingleSignonEnabled();
                    will(returnValue(true));
                    allowing(config).getSSORequiresSSL();
                    will(returnValue(true));
                    allowing(config).createWebAuthenticatorProxy();
                    will(returnValue(new WebAuthenticatorProxy(config, null, serviceReference, null, null)));
                    allowing(config).createSSOCookieHelper();
                    will(returnValue(new SSOCookieHelperImpl(config)));
                    allowing(config).createReferrerURLCookieHandler();
                    will(returnValue(new ReferrerURLCookieHandler(config)));
                    allowing(provider).getClientAdmin();
                    will(returnValue(null));

                    allowing(authenticationService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(matchingAuthenticationData(authenticationData)),
                            with(equal((Subject) null)));
                    will(returnValue(subject));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            userAuthentication.handleBasicAuthenticationWithRequiredRole(provider, request, response, serviceReference, servletContext, userRole);
        } catch (OidcServerException oidcExc) {

            assertEquals(notInRoleMsg, oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_ACCESS_DENIED, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_FORBIDDEN, oidcExc.getHttpStatus());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    @Factory
    private static Matcher<AuthenticationData> matchingAuthenticationData(AuthenticationData authData) {
        return new AuthenticationDataMatcher(authData);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void handleBasicAuthenticationNullPrincipalAuthTypeNone() {
        final String methodName = "handleBasicAuthenticationNullPrincipalAuthTypeNone";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    allowing(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    allowing(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(null));

                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            userAuthentication.handleBasicAuthenticationWithRequiredRole(provider, request, response, serviceReference, servletContext, "Role1");
        } catch (OidcServerException oidcExc) {
            assertEquals(notInRoleMsg, oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_ACCESS_DENIED, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests hasAuthenticationData with following conditions
     * 1. Auth type is set as CERT.
     * Expected result: CERT is used.
     */
    @Test
    public void hasAuthenticationDataAuthTypeCert() {
        final X509Certificate cc[] = { cert };
        mock.checking(new Expectations() {
            {
                one(request).getHeader(AUTH_HEADER_NAME);
                will(returnValue(null));
                one(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(cc));
            }
        });

        UserAuthentication userAuthentication = new UserAuthentication();
        assertEquals(AuthType.CERT, userAuthentication.hasAuthenticationData(request));

    }

    /**
     * Tests renderErrorPage with following conditions
     * 1 the error template matches the predefined pattern
     * 2 a dispatcher is found.
     * Expected result: forward request
     */
    @SuppressWarnings("unchecked")
    @Test
    public void renderErrorPageForwardRequest() {
        final String methodName = "renderErrorPageForwardRequest";
        final String root = "/root";
        final String path = "/path";
        final String template = "{" + root + "}" + path;
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_NONE));
                    one(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    one(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(null));
                    one(provider).getAuthorizationErrorTemplate();
                    will(returnValue(template));
                    one(servletContext).getContext(root);
                    will(returnValue(servletContext));
                    one(servletContext).getRequestDispatcher(path);
                    will(returnValue(rd));

                    // set
                    one(request).setAttribute(ATTR_OAUTH_RESULT, result);
                    one(rd).forward(request, response);
                }
            });

            // set servletContext
            UserAuthentication userAuthentication = new UserAuthentication();
            Prompt prompt = new Prompt(request);
            userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            userAuthentication.renderErrorPage(provider, request, response, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests renderErrorPage with following conditions
     * 1 the error template matches the predefined pattern
     * 2 a dispatcher doesn't find.
     * Expected result: nothing is done, but logged a debug message.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void renderErrorPageForwardRequestNoDipatcher() {
        final String methodName = "renderErrorPageForwardRequestNoDispatcher";
        final String root = "/root";
        final String path = "/path";
        final String template = "{" + root + "}" + path;
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_NONE));
                    one(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    one(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(null));
                    one(provider).getAuthorizationErrorTemplate();
                    will(returnValue(template));
                    one(servletContext).getContext(root);
                    will(returnValue(servletContext));
                    one(servletContext).getRequestDispatcher(path);
                    will(returnValue(null));

                    // set
                    one(request).setAttribute(ATTR_OAUTH_RESULT, result);
                    never(rd).forward(request, response);
                }
            });

            // set servletContext
            UserAuthentication userAuthentication = new UserAuthentication();
            Prompt prompt = new Prompt(request);
            userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            userAuthentication.renderErrorPage(provider, request, response, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests renderErrorPage with following conditions
     * 1 the error template doesn'tmatch the predefined pattern
     * Expected result: invoke exception handler.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void renderErrorPageRedirectRequest() {
        final String methodName = "renderErrorPageRedirectRequest";
        final String root = "/root";
        final String path = "/path";
        final String template = "http://localhost" + root + path;
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(ATTR_PROMPT);
                    will(returnValue(Constants.PROMPT_NONE));
                    one(request).getHeader(AUTH_HEADER_NAME);
                    will(returnValue(null));
                    one(request).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                    will(returnValue(null));
                    one(provider).getAuthorizationErrorTemplate();
                    will(returnValue(template));
                    one(result).getAttributeList();
                    will(returnValue(al));
                    one(al).getAttributeValueByName(OAuth20Constants.RESPONSE_TYPE);
                    will(returnValue("code"));
                    one(al).getAttributeValueByName(OAuth20Constants.REDIRECT_URI);
                    will(returnValue("http://localhost/redirect"));
                    one(request).getCharacterEncoding();
                    will(returnValue(null));
                    one(result).getStatus();
                    will(returnValue(OAuthResult.STATUS_OK));
                }
            });

            // set servletContext
            UserAuthentication userAuthentication = new UserAuthentication();
            Prompt prompt = new Prompt(request);
            userAuthentication.handleAuthentication(provider, request, response, prompt, serviceReference, servletContext, userRole);
            userAuthentication.renderErrorPage(provider, request, response, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    @Test
    public void TestGetRegisteredRedirectUriGood() {
        final String methodName = "TestGetRegisteredRedirectUriGood";
        final String clientId = "client01";
        final String uri = "http://host.myco.com/redirect.html";
        final JsonArray uris = new JsonArray();

        uris.add(new JsonPrimitive(uri));

        try {
            mock.checking(new Expectations() {
                {
                    one(al).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    will(returnValue(clientId));
                    one(provider).getClientProvider();
                    will(returnValue(ocp));
                    one(ocp).get(clientId);
                    will(returnValue(obc));
                    one(obc).getRedirectUris();
                    will(returnValue(uris));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            assertEquals(userAuthentication.getRegisteredRedirectUri(provider, al), uri);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void TestGetRegisteredRedirectUriErrorNoId() {
        final String methodName = "TestGetRegisteredRedirectUriErrorNoId";

        try {
            mock.checking(new Expectations() {
                {
                    one(al).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    will(returnValue(null));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            assertNull(userAuthentication.getRegisteredRedirectUri(provider, al));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void TestGetRegisteredRedirectUriErrorException() {
        final String methodName = "TestGetRegisteredRedirectUriErrorException";
        final String clientId = "client01";

        try {
            mock.checking(new Expectations() {
                {
                    one(al).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    will(returnValue(clientId));
                    one(provider).getClientProvider();
                    will(returnValue(ocp));
                    one(ocp).get(clientId);
                    will(throwException(new OidcServerException("error", OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST)));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            assertNull(userAuthentication.getRegisteredRedirectUri(provider, al));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void TestGetRegisteredRedirectUriErrorNoClient() {
        final String methodName = "TestGetRegisteredRedirectUriErrorNoClient";
        final String clientId = "client01";
        final String uri = "http://host.myco.com/redirect.html";
        final JsonArray uris = new JsonArray();

        uris.add(new JsonPrimitive(uri));

        try {
            mock.checking(new Expectations() {
                {
                    one(al).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    will(returnValue(clientId));
                    one(provider).getClientProvider();
                    will(returnValue(ocp));
                    one(ocp).get(clientId);
                    will(returnValue(null));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            assertNull(userAuthentication.getRegisteredRedirectUri(provider, al));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void TestValidateIdTokenHintGood() {
        final String methodName = "TestValidateIdTokenHintGood";
        final String username = "user1";
        final String clientId = "clientId";
        try {
            mock.checking(new Expectations() {
                {
                    one(al).getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS);
                    will(returnValue(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_SUCCESS));
                    one(al).getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_USERNAME);
                    will(returnValue(username));
                    one(al).getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_CLIENTID);
                    will(returnValue(clientId));
                    allowing(al).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    will(returnValue(clientId));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            assertTrue(userAuthentication.validateIdTokenHint(username, al));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void TestValidateIdTokenHintFailUsername() {
        final String methodName = "TestValidateIdTokenHintFailUsername";
        final String username = "user1";
        final String badUsername = "user2";
        final String clientId = "clientId";
        try {
            mock.checking(new Expectations() {
                {
                    one(al).getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS);
                    will(returnValue(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_SUCCESS));
                    one(al).getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_USERNAME);
                    will(returnValue(username));
                    one(al).getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_CLIENTID);
                    will(returnValue(clientId));
                    allowing(al).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    will(returnValue(clientId));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            userAuthentication.validateIdTokenHint(badUsername, al);
            fail("OAuth20Exception should be thrown.");
        } catch (OAuth20Exception e) {
            // good.
            assertEquals(OIDCConstants.ERROR_LOGIN_REQUIRED, e.getError());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void TestValidateIdTokenHintFailClientId() {
        final String methodName = "TestValidateIdTokenHintFailClientId";
        final String username = "user1";
        final String clientId = "clientId";
        final String badClientId = "bad";
        try {
            mock.checking(new Expectations() {
                {
                    one(al).getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS);
                    will(returnValue(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_SUCCESS));
                    one(al).getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_USERNAME);
                    will(returnValue(username));
                    one(al).getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_CLIENTID);
                    will(returnValue(clientId));
                    allowing(al).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    will(returnValue(badClientId));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            userAuthentication.validateIdTokenHint(username, al);
            fail("OAuth20Exception should be thrown.");
        } catch (OAuth20Exception e) {
            // good.
            assertEquals(OIDCConstants.ERROR_LOGIN_REQUIRED, e.getError());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    @Test
    public void TestValidateIdTokenHintFailStatus() {
        final String methodName = "TestValidateIdTokenHintFailStatus";
        final String username = "user1";
        try {
            mock.checking(new Expectations() {
                {
                    one(al).getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS);
                    will(returnValue(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            userAuthentication.validateIdTokenHint(username, al);
            fail("OAuth20Exception should be thrown.");
        } catch (OAuth20Exception e) {
            // good.
            assertEquals(OAuth20Exception.INVALID_REQUEST, e.getError());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void TestValidateIdTokenHintGoodNoStatus() {
        final String methodName = "TestValidateIdTokenHintGoodNoStatus";
        final String username = "user1";
        try {
            mock.checking(new Expectations() {
                {
                    one(al).getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS);
                    will(returnValue(null));
                }
            });

            UserAuthentication userAuthentication = new UserAuthentication();
            assertTrue(userAuthentication.validateIdTokenHint(username, al));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    private void sendForLoginExpectations(final String requestUri, final String contextPath, final String loginUrl) throws IOException {
        mock.checking(new Expectations() {
            {
                allowing(provider).getCustomLoginURL();
                will(returnValue(OAuth20Constants.DEFAULT_AUTHZ_LOGIN_URL));
                one(request).getScheme();
                will(returnValue("https"));
                one(request).getServerPort();
                will(returnValue(9080));
                one(request).getRequestURI();
                will(returnValue(requestUri));
                one(request).getQueryString();
                will(returnValue(null));
                one(request).getContextPath();
                will(returnValue(contextPath));
                allowing(config).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(config)));
                allowing(config).isIncludePathInWASReqURL();
                allowing(config).getHttpOnlyCookies();
                will(returnValue(false));
                allowing(config).getSSORequiresSSL();
                will(returnValue(false));
                allowing(provider).isHttpsRequired();
                will(returnValue(true));
                allowing(response).addCookie(with(any(Cookie.class)));
                one(request).getSession(true);
                will(returnValue(hs));
                one(hs).setAttribute(Constants.ATTR_AFTERLOGIN, Boolean.TRUE);
                one(response).sendRedirect(contextPath + "/" + loginUrl);
            }
        });
    }

}
