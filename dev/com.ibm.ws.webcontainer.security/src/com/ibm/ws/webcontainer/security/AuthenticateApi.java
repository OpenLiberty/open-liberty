/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security;

import java.io.IOException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.web.PasswordExpiredException;
import com.ibm.websphere.security.web.UserRevokedException;
import com.ibm.ws.kernel.security.thread.ThreadIdentityException;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.collaborator.CollaboratorUtils;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.mp.jwt.proxy.MpJwtHelper;
import com.ibm.ws.security.sso.SSOAuthFilter;
import com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator;
import com.ibm.ws.webcontainer.security.internal.ChallengeReply;
import com.ibm.ws.webcontainer.security.internal.DenyReply;
import com.ibm.ws.webcontainer.security.internal.OAuthChallengeReply;
import com.ibm.ws.webcontainer.security.internal.RedirectReply;
import com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils;
import com.ibm.ws.webcontainer.security.internal.SSOAuthenticator;
import com.ibm.ws.webcontainer.security.internal.TAIChallengeReply;
import com.ibm.ws.webcontainer.security.internal.WebReply;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

public class AuthenticateApi {
    private static final TraceComponent tc = Tr.register(AuthenticateApi.class);

    static final String KEY_SECURITY_SERVICE = "securityService";
    protected AtomicServiceReference<SecurityService> securityServiceRef = null;

    private final SubjectManager subjectManager = new SubjectManager();
    private final SubjectHelper subjectHelper = new SubjectHelper();
    private final SSOCookieHelper ssoCookieHelper;
    private AuthCacheService authCacheService = null;
    private final CollaboratorUtils collabUtils;
    private AuthenticationService authService = null;
    private AtomicServiceReference<SSOAuthFilter> ssoAuthFilterRef;
    private ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRefs = null;
    private ConcurrentServiceReferenceMap<String, UnprotectedResourceService> unprotectedResourceServiceRef = null;
    private UnauthenticatedSubjectService unauthenticatedSubjectService;
    protected static final WebReply DENY_AUTHN_FAILED = new DenyReply("AuthenticationFailed");
    private Subject logoutSubject = null;
    private final String SECURITY_CONTEXT = "SECURITY_CONTEXT";

    public AuthenticateApi(SSOCookieHelper ssoCookieHelper,
                           AtomicServiceReference<SecurityService> securityServiceRef,
                           CollaboratorUtils collabUtils,
                           ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef,
                           ConcurrentServiceReferenceMap<String, UnprotectedResourceService> unprotectedResourceServiceRef,
                           UnauthenticatedSubjectService unauthenticatedSubjectService,
                           AtomicServiceReference<SSOAuthFilter> ssoAuthFilterRef) {
        this.ssoCookieHelper = ssoCookieHelper;
        this.securityServiceRef = securityServiceRef;
        this.collabUtils = collabUtils;
        this.webAuthenticatorRefs = webAuthenticatorRef;
        this.unprotectedResourceServiceRef = unprotectedResourceServiceRef;
        this.unauthenticatedSubjectService = unauthenticatedSubjectService;
        this.ssoAuthFilterRef = ssoAuthFilterRef;

        // securityService may or may not be available at this point. so if it is available, do the the initialization work,
        // otherwise defer getting authService and authCacheService when it is ready.
        if (securityServiceRef != null) {
            SecurityService ss = securityServiceRef.getService();
            if (ss != null) {
                authService = ss.getAuthenticationService();
                if (authService != null) {
                    authCacheService = authService.getAuthCacheService();
                }
            }
        }
    }

    public AuthenticateApi(SSOCookieHelper ssoCookieHelper, AuthenticationService authService) {
        this.securityServiceRef = null;
        this.collabUtils = null;
        this.authCacheService = null;
        this.authService = authService;
        this.ssoCookieHelper = ssoCookieHelper;
    }

    /**
     * Perform login an user by doing the following:
     * 1) Call basicAuthenticate method.
     * 2) Push a client subject onto the thread.
     * 3) Create SSO cookie if SSO is enabled and allow to add a cookie to the response.
     * 4) setCallerSubject and setInvocationSubject accordingly
     *
     * @param req
     * @param resp
     * @param username
     * @param password
     * @param config
     * @param basicAuthAuthenticator
     * @throws ServletException
     */
    public void login(HttpServletRequest req, HttpServletResponse resp, String username, @Sensitive String password, WebAppSecurityConfig config,
                      BasicAuthAuthenticator basicAuthAuthenticator) throws ServletException {
        // Per JASPI spec no login method allowed with JASPIC
        String isJaspiAuthenticated = (String) req.getServletContext().getAttribute("com.ibm.ws.security.jaspi.authenticated");
        if (isJaspiAuthenticated != null && isJaspiAuthenticated.equals(Boolean.toString(Boolean.TRUE))) {
            throw new ServletException("The login method may not be invoked while JASPI authentication is active.");
        }
        boolean logoutOnHttpSessionExp = config.getLogoutOnHttpSessionExpire();
        if (req.getRequestedSessionId() != null && (req.isRequestedSessionIdValid() == false) && logoutOnHttpSessionExp)
            req.getSession(true);

        throwExceptionIfAlreadyAuthenticate(req, resp, config, username);

        AuthenticationResult authResult = basicAuthAuthenticator.basicAuthenticate(null, username, password, req, resp);
        if (authResult == null || authResult.getStatus() != AuthResult.SUCCESS) {
            String realm = authResult.realm;
            if (realm == null) {
                realm = collabUtils.getUserRegistryRealm(securityServiceRef);
            }
            WebReply reply = null;
            reply = createReplyForAuthnFailure(authResult, realm);

            Audit.audit(Audit.EventID.SECURITY_API_AUTHN_01, req, authResult, Integer.valueOf(reply.getStatusCode()));

            if (authResult.passwordExpired == true) {
                throw new PasswordExpiredException(authResult.getReason());
            } else if (authResult.userRevoked == true) {
                throw new UserRevokedException(authResult.getReason());
            } else {
                throw new ServletException(authResult.getReason());
            }

        } else {

            Audit.audit(Audit.EventID.SECURITY_API_AUTHN_01, req, authResult, Integer.valueOf(HttpServletResponse.SC_OK));

        }
        postProgrammaticAuthenticate(req, resp, authResult);
    }

    /**
     * Perform logout an user by doing the following:
     * 1) Remove client authentication cache
     * 2) Invalidate the session
     * 3) Remove cookie if SSO is enabled
     * 4) Clear out the client subject
     *
     * @param req
     * @param res
     * @param webAttrs
     * @throws ServletException
     * @throws IOException
     */
    public void logout(HttpServletRequest req, HttpServletResponse res, WebAppSecurityConfig config) throws ServletException {
        // logout when the unprotectedResourceService(s) do
        logoutUnprotectedResourceServiceRef(req, res);
        createSubjectAndPushItOnThreadAsNeeded(req, res);

        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, subjectManager.getCallerSubject());
        JaspiService jaspiService = getJaspiService();
        if (jaspiService == null) {
            authResult.setAuditCredType(req.getAuthType());
            authResult.setAuditOutcome(AuditEvent.OUTCOME_SUCCESS);
            Audit.audit(Audit.EventID.SECURITY_API_AUTHN_TERMINATE_01, req, authResult, Integer.valueOf(res.getStatus()));
        }

        removeEntryFromAuthCache(req, res, config);
        invalidateSession(req);
        ssoCookieHelper.removeSSOCookieFromResponse(res);
        ssoCookieHelper.createLogoutCookies(req, res);

        // if we have jwt, put on mpjwt's list of logged out jwt's so it cannot be reused.
        // will be null if mpJwt feature not active, or no jwt in principal
        Principal p = MpJwtHelper.getJsonWebTokenPricipal(subjectManager.getCallerSubject());
        if (p != null) {
            MpJwtHelper.addLoggedOutJwtToList(p);
        }

        try {
            if (unauthenticatedSubjectService != null) {
                ThreadIdentityManager.setAppThreadIdentity(unauthenticatedSubjectService.getUnauthenticatedSubject());
            }

        } catch (ThreadIdentityException e) {
            //FFDC will be generated
        }

        //If authenticated with form login, we need to clear the RefrrerURLCookie
        ReferrerURLCookieHandler referrerURLHandler = config.createReferrerURLCookieHandler();
        referrerURLHandler.clearReferrerURLCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
        SRTServletRequestUtils.removePrivateAttribute(req, "AUTH_TYPE");
        postLogout(req, res);
        subjectManager.clearSubjects();

    }

    /**
     * @param req
     * @param res
     */
    void logoutUnprotectedResourceServiceRef(HttpServletRequest req, HttpServletResponse res) {
        boolean bInitUserName = false;
        String userName = null;
        Set<String> serviceIds = unprotectedResourceServiceRef.keySet();
        for (String serviceId : serviceIds) {
            if (!bInitUserName) {
                bInitUserName = true;
                userName = getSessionUserName(req, res);
            }
            UnprotectedResourceService service = unprotectedResourceServiceRef.getService(serviceId);
            boolean bLogout = service.logout(req, res, userName);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "logout return " + bLogout + " on service " + service);
        }
    }

    void postLogout(HttpServletRequest req, HttpServletResponse res) {
        Set<String> serviceIds = unprotectedResourceServiceRef.keySet();
        for (String serviceId : serviceIds) {
            UnprotectedResourceService service = unprotectedResourceServiceRef.getService(serviceId);
            boolean bLogout = service.postLogout(req, res);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "postLogout returns " + bLogout + " on service " + service);
        }
    }

    /**
     * @param req
     * @param res
     * @return
     */
    String getSessionUserName(HttpServletRequest req, HttpServletResponse res) {
        String result = null;
        if (req instanceof SRTServletRequest) {
            SRTServletRequest servletRequest = (SRTServletRequest) req;
            IWebAppDispatcherContext webAppDispatchContext = servletRequest.getWebAppDispatcherContext();
            WebApp webApp = webAppDispatchContext.getWebApp();
            if (webApp != null) {
                IHttpSessionContext httpSessionContext = webApp.getSessionContext();
                if (httpSessionContext != null) {
                    result = httpSessionContext.getSessionUserName(req, res);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "getSessionUserName:" + result);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "no httpSessionContext in WebApp");
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "no WebApp in SRTServletRequest");
            }

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Not a SRTServletRequest" + req);
        }
        return result;
    }

    /**
     * Perform logout an user by doing the following:
     * 1) Invalidate the session
     * 2) Remove cookie if SSO is enabled
     * 3) Clear out the client subject
     *
     * @param req
     * @param res
     */
    public void simpleLogout(HttpServletRequest req, HttpServletResponse res) {
        createSubjectAndPushItOnThreadAsNeeded(req, res);

        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, subjectManager.getCallerSubject());
        authResult.setAuditCredType(req.getAuthType());
        authResult.setAuditOutcome(AuditEvent.OUTCOME_SUCCESS);
        Audit.audit(Audit.EventID.SECURITY_API_AUTHN_TERMINATE_01, req, authResult, Integer.valueOf(res.getStatus()));

        removeEntryFromAuthCacheForUser(req, res);
        invalidateSession(req);
        ssoCookieHelper.removeSSOCookieFromResponse(res);
        ssoCookieHelper.createLogoutCookies(req, res);
        subjectManager.clearSubjects();

    }

    /**
     * Add the ltpa token string to the logged out token distributed map.
     *
     * @param tokenString
     */
    private void addToLoggedOutTokenCache(String tokenString) {
        String tokenValue = "userName";
        LoggedOutTokenCacheImpl.getInstance().addTokenToDistributedMap(tokenString, tokenValue);
    }

    /**
     * Remove entries in the authentication cache
     *
     * @param req
     * @param res
     * @param config
     */
    private void removeEntryFromAuthCache(HttpServletRequest req, HttpServletResponse res, WebAppSecurityConfig config) {
        /*
         * TODO: we need to optimize this method... if the authCacheService.remove() method
         * return true for successfully removed the entry in the authentication cache, then we
         * do not have to call the second method for token. See defect 66015
         */
        removeEntryFromAuthCacheForUser(req, res);
        removeEntryFromAuthCacheForToken(req, res, config);
    }

    /**
     * Remove entries in the authentication cache using the ltpaToken as a key
     *
     * @param req
     * @param res
     * @param config
     */
    private void removeEntryFromAuthCacheForToken(HttpServletRequest req, HttpServletResponse res, WebAppSecurityConfig config) {
        getAuthCacheService();
        if (authCacheService == null) {
            return;
        }

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            String cookieValues[] = null;
            cookieValues = CookieHelper.getCookieValues(cookies, ssoCookieHelper.getSSOCookiename());
            if ((cookieValues == null || cookieValues.length == 0) &&
                !SSOAuthenticator.DEFAULT_SSO_COOKIE_NAME.equalsIgnoreCase(ssoCookieHelper.getSSOCookiename())) {
                cookieValues = CookieHelper.getCookieValues(cookies, SSOAuthenticator.DEFAULT_SSO_COOKIE_NAME);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Cookie size: ", cookieValues == null ? "<null>" : cookieValues.length);
            if (cookieValues != null && cookieValues.length > 0) {
                for (int n = 0; n < cookieValues.length; n++) {
                    String val = cookieValues[n];
                    if (val != null && val.length() > 0) {
                        try {
                            authCacheService.remove(val);
                            //Add token to the logged out cache if enabled
                            if (config.isTrackLoggedOutSSOCookiesEnabled())
                                addToLoggedOutTokenCache(val);
                        } catch (Exception e) {
                            String user = req.getRemoteUser();
                            if (user == null) {
                                Principal p = req.getUserPrincipal();
                                if (p != null)
                                    user = p.getName();
                            }
                            Tr.warning(tc, "AUTHENTICATE_CACHE_REMOVAL_EXCEPTION", user, e.toString());
                        }
                    }
                }
            }
        }
    }

    /**
     *
     */
    private void getAuthCacheService() {
        if (authCacheService == null && securityServiceRef != null) {
            authCacheService = securityServiceRef.getService().getAuthenticationService().getAuthCacheService();
        }
    }

    /**
     * Remove entries in the authentication cache using the user as a key
     *
     * @param req
     * @param res
     */
    private void removeEntryFromAuthCacheForUser(HttpServletRequest req, HttpServletResponse res) {
        getAuthCacheService();
        if (authCacheService == null) {
            return;
        }
        String user = req.getRemoteUser();
        if (user == null) {
            Principal p = req.getUserPrincipal();
            if (p != null)
                user = p.getName();
        }
        if (user != null) {
            if (collabUtils != null) {
                String realm = collabUtils.getUserRegistryRealm(securityServiceRef);
                if (!user.contains(realm + ":")) {
                    user = realm + ":" + user;
                }
            }
            authCacheService.remove(user);
        }
    }

    /**
     * This method throws an exception if the caller subject is already authenticated and
     * WebAlwaysLogin is false. If the caller subject is already authenticated and
     * WebAlwaysLogin is true, then it will logout the user.
     *
     * @throws IOException
     * @throws ServletException
     *
     */
    public void throwExceptionIfAlreadyAuthenticate(HttpServletRequest req, HttpServletResponse resp, WebAppSecurityConfig config, String username) throws ServletException {
        Subject callerSubject = subjectManager.getCallerSubject();
        if (subjectHelper.isUnauthenticated(callerSubject))
            return;
        if (!config.getWebAlwaysLogin()) {
            AuthenticationResult authResult = new AuthenticationResult(AuthResult.FAILURE, username);
            authResult.setAuditCredType(req.getAuthType());
            authResult.setAuditCredValue(username);
            authResult.setAuditOutcome(AuditEvent.OUTCOME_FAILURE);
            Audit.audit(Audit.EventID.SECURITY_API_AUTHN_01, req, authResult, Integer.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
            throw new ServletException("Authentication had been already established");
        }
        logout(req, resp, config);
    }

    /**
     * Invalidates the session associated with the request.
     *
     * @param req
     */
    private void invalidateSession(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "invalidating existing HTTP Session");
            session.invalidate();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Existing HTTP Session does not exist, nothing to invalidate");
        }
    }

    /**
     * This method set the caller and invocation subject and call the addSsoCookiesToResponse
     *
     * @param req
     * @param resp
     * @param authResult
     */
    public void postProgrammaticAuthenticate(HttpServletRequest req, HttpServletResponse resp, AuthenticationResult authResult) {
        postProgrammaticAuthenticate(req, resp, authResult, false, true);
    }

    /**
     * This method set the caller and invocation subject and call the addSsoCookiesToResponse
     *
     * @param req
     * @param resp
     * @param authResult
     */
    public void postProgrammaticAuthenticate(HttpServletRequest req, HttpServletResponse resp, AuthenticationResult authResult, boolean alwaysSetCallerSubject) {
        postProgrammaticAuthenticate(req, resp, authResult, alwaysSetCallerSubject, true);
    }

    /**
     * This method set the caller and invocation subject and call the addSsoCookiesToResponse
     *
     * @param req
     * @param resp
     * @param authResult
     */
    public void postProgrammaticAuthenticate(final HttpServletRequest req, final HttpServletResponse resp, final AuthenticationResult authResult,
                                             final boolean alwaysSetCallerSubject, final boolean addSSOCookie) {
        if (System.getSecurityManager() == null) {
            setSubjectAndCookies(req, resp, authResult, alwaysSetCallerSubject, addSSOCookie);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {

                @Override
                public Object run() {
                    setSubjectAndCookies(req, resp, authResult, alwaysSetCallerSubject, addSSOCookie);
                    return null;
                }
            });
        }
    }

    private void setSubjectAndCookies(HttpServletRequest req, HttpServletResponse resp, final AuthenticationResult authResult, boolean alwaysSetCallerSubject,
                                      boolean addSSOCookie) {
        Subject subject = authResult.getSubject();
        if (alwaysSetCallerSubject || new SubjectHelper().isUnauthenticated(subjectManager.getCallerSubject())) {
            subjectManager.setCallerSubject(subject);
        }
        subjectManager.setInvocationSubject(subject);
        if (addSSOCookie) {
            ssoCookieHelper.addSSOCookiesToResponse(subject, req, resp);
        }
        try {
            Object loginToken = ThreadIdentityManager.setAppThreadIdentity(subject);
            WebSecurityContext webSecurityContext = (WebSecurityContext) SRTServletRequestUtils.getPrivateAttribute(req, SECURITY_CONTEXT);

            if (webSecurityContext != null && webSecurityContext.getSyncToOSThreadToken() == null) {
                webSecurityContext.setSyncToOSThreadToken(loginToken);
            }
        } catch (ThreadIdentityException e) {
            //FFDC will be generated
        }

    }

    /**
     * For formLogout, this is a new request and there is no subject on the thread. A previous
     * request handled on this thread may not be from this same client. We have to authenticate using
     * the token and push the subject on thread so webcontainer can use the subject credential to invalidate
     * the session.
     *
     * @param req
     * @param res
     */
    private void createSubjectAndPushItOnThreadAsNeeded(HttpServletRequest req, HttpServletResponse res) {
        // We got a new instance of FormLogoutExtensionProcess every request.
        logoutSubject = null;
        Subject subject = subjectManager.getCallerSubject();
        if (subject == null || subjectHelper.isUnauthenticated(subject)) {
            if (authService == null && securityServiceRef != null) {
                authService = securityServiceRef.getService().getAuthenticationService();
            }
            SSOAuthenticator ssoAuthenticator = new SSOAuthenticator(authService, null, null, ssoCookieHelper, ssoAuthFilterRef);
            //TODO: We can not call ssoAuthenticator.authenticate because it can not handle multiple tokens.
            //In the next release, authenticate need to handle multiple authentication data. See story
            AuthenticationResult authResult = ssoAuthenticator.handleSSO(req, res);
            if (authResult != null && authResult.getStatus() == AuthResult.SUCCESS) {
                subjectManager.setCallerSubject(authResult.getSubject());
                logoutSubject = authResult.getSubject();
            }
        }
    }

    /**
     * Debug method used to collect all the Http header names and values.
     *
     * @param webAppSecurityCollaboratorImpl TODO
     * @param req HttpServletRequest
     * @return Returns a string that contains each parameter and it value(s)
     *         in the HttpServletRequest object.
     */
    String debugGetAllHttpHdrs(HttpServletRequest req) {
        if (req == null)
            return null;
        StringBuffer sb = new StringBuffer(512);
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            sb.append(headerName).append("=");
            sb.append("[").append(SRTServletRequestUtils.getHeader(req, headerName)).append("]\n");
        }
        return sb.toString();
    }

    /**
     * @param webRequest
     * @return
     */
    private JaspiService getJaspiService() {
        JaspiService jaspiService = null;
        if (webAuthenticatorRefs != null) {
            WebAuthenticator jaspiAuthenticator = webAuthenticatorRefs.getService("com.ibm.ws.security.jaspi");
            jaspiService = (JaspiService) jaspiAuthenticator;
        }
        return jaspiService;
    }

    /**
     * This method is specifically for handling the Servlet 3.0 method
     * HttpServletRequest.logout(). Per the JSR 196 spec, it will check for
     * JASPI authentication and if enabled will attempt to call the JASPI provider's
     * cleanSubject method, and will always call the main logout method.
     *
     * @param res
     * @param resp
     * @param webAppSecConfig
     */
    public void logoutServlet30(HttpServletRequest res,
                                HttpServletResponse resp,
                                WebAppSecurityConfig webAppSecConfig) throws ServletException {
        JaspiService jaspiService = getJaspiService();
        if (jaspiService != null) {
            try {
                jaspiService.logout(res, resp, webAppSecConfig);
            } catch (AuthenticationException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "AuthenticationException invoking JASPI service logout", e);
            }
        }
        logout(res, resp, webAppSecConfig);
    }

    public WebReply createReplyForAuthnFailure(AuthenticationResult authResult, String realm) {
        WebReply reply = null;
        switch (authResult.getStatus()) {
            case FAILURE:
                return DENY_AUTHN_FAILED;

            case SEND_401:
                return new ChallengeReply(realm);

            case TAI_CHALLENGE:
                return new TAIChallengeReply(authResult.getTAIChallengeCode());

            case REDIRECT:
                return new RedirectReply(authResult.getRedirectURL(), authResult.getCookies());

            case UNKNOWN:

            case CONTINUE:
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Authentication failed with status [" + authResult.getStatus() + "] and reason [" + authResult.getReason() + "]");
                }
                return DENY_AUTHN_FAILED;

            case OAUTH_CHALLENGE:
                return new OAuthChallengeReply(authResult.getReason());

            default:
                break;
        }
        return reply;
    }

    public Subject returnSubjectOnLogout() {
        return logoutSubject;
    }
}
