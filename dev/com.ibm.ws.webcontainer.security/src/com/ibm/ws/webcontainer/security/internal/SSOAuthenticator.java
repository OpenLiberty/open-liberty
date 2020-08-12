/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.util.HashMap;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
//import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenHelper;
import com.ibm.ws.security.sso.SSOService;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticateApi;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.ws.webcontainer.security.LoggedOutTokenCacheImpl;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This class perform authentication for web request using single sign on cookie.
 */
//@Component(service = WebAuthenticator.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class SSOAuthenticator implements WebAuthenticator {
    public static final String DEFAULT_SSO_COOKIE_NAME = "LtpaToken2";
    private static final String Authorization_Header = "Authorization";
    public final static String REQ_METHOD_POST = "POST";
    public final static String REQ_CONTENT_TYPE_NAME = "Content-Type";
    public final static String REQ_CONTENT_TYPE_APP_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String LTPA_OID = "oid:1.3.18.0.2.30.2";
    private static final String JWT_OID = "oid:1.3.18.0.2.30.3"; // ?????
//    public final static String KEY_FILTER = "authenticationFilter";

    private static final TraceComponent tc = Tr.register(SSOAuthenticator.class);
    private final AuthenticationService authenticationService;
    private final WebAppSecurityConfig webAppSecurityConfig;
    private final SSOCookieHelper ssoCookieHelper;
    private final String challengeType;
    AtomicServiceReference<SSOService> ssoServiceRef;

//    protected final AtomicServiceReference<AuthenticationFilter> authFilterServiceRef = new AtomicServiceReference<AuthenticationFilter>(KEY_FILTER);

    /**
     * @param authenticationServ
     * @param securityMetadata
     */
    public SSOAuthenticator(AuthenticationService authenticationService,
                            SecurityMetadata securityMetadata,
                            WebAppSecurityConfig webAppSecurityConfig,
                            SSOCookieHelper ssoCookieHelper,
                            AtomicServiceReference<SSOService> ssoServiceRef) {
        this.authenticationService = authenticationService;
        this.webAppSecurityConfig = webAppSecurityConfig;
        this.ssoCookieHelper = ssoCookieHelper;
        this.ssoServiceRef = ssoServiceRef;

        LoginConfiguration loginConfig = securityMetadata == null ? null : securityMetadata.getLoginConfiguration();
        challengeType = loginConfig == null ? null : loginConfig.getAuthenticationMethod();
    }

    /** {@inheritDoc} */
    @Override
    public AuthenticationResult authenticate(WebRequest webRequest) {
        return authenticate(webRequest, webAppSecurityConfig);
    }

    /**
     * @param webRequest
     * @return AuthenticationResult
     */
    public AuthenticationResult authenticate(WebRequest webRequest, WebAppSecurityConfig webAppSecConfig) {
        HttpServletRequest req = webRequest.getHttpServletRequest();
        HttpServletResponse res = webRequest.getHttpServletResponse();
        AuthenticationResult authResult = handleSSO(req, res);
        if (authResult != null && authResult.getStatus() == AuthResult.SUCCESS) {
            ssoCookieHelper.addJwtSsoCookiesToResponse(authResult.getSubject(), req, res);
        }
        return authResult;
    }

    /**
     * @param req
     * @param res
     * @return authResult
     */
    //@FFDCIgnore({ AuthenticationException.class })
    public AuthenticationResult handleSSO(HttpServletRequest req, HttpServletResponse res) {
        AuthenticationResult authResult = null;
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return authResult;
        }

        boolean comp = webAppSecurityConfig != null && webAppSecurityConfig.getLogoutOnHttpSessionExpire();
        if (comp && req.getRequestedSessionId() != null && !req.isRequestedSessionIdValid() &&
            challengeType != null && challengeType.equals(LoginConfiguration.FORM)) {
            ssoCookieHelper.createLogoutCookies(req, res);
            return authResult;
        }

        authResult = handleJwtSSO(req, res);
        // If there is a jwtSSOToken in a request, use LTPA will not be allowed.
        // If there is NO jwtSSOToken in a request and shouldUseLtpaIfJwtAbsent is true, use LTPA will be allowed
        if (authResult != null || !JwtSSOTokenHelper.shouldUseLtpaIfJwtAbsent()) {
            return authResult;
        }

        if (!isAuthFilterAccept(req)) {
            return authResult;
        }

        authResult = handleLtpaSSO(req, res, cookies);
        if (authResult != null && authResult.getStatus() == AuthResult.SUCCESS) {
            return authResult;
        }

        ssoCookieHelper.createLogoutCookies(req, res);
        return authResult;
    }

    /**
     * @param req
     * @param res
     * @param authResult
     * @param cookies
     * @return
     */
    @FFDCIgnore({ AuthenticationException.class })
    private AuthenticationResult handleLtpaSSO(HttpServletRequest req, HttpServletResponse res, Cookie[] cookies) {
        AuthenticationResult authResult = null;
        String cookieName = ssoCookieHelper.getSSOCookiename();
        String[] hdrVals = CookieHelper.getCookieValues(cookies, cookieName);
        boolean useOnlyCustomCookieName = webAppSecurityConfig != null && webAppSecurityConfig.isUseOnlyCustomCookieName();
        if (hdrVals == null && !DEFAULT_SSO_COOKIE_NAME.equalsIgnoreCase(cookieName) && !useOnlyCustomCookieName) {
            hdrVals = CookieHelper.getCookieValues(cookies, DEFAULT_SSO_COOKIE_NAME);
        }
        if (hdrVals != null) {
            for (int n = 0; n < hdrVals.length; n++) {
                String hdrVal = hdrVals[n];
                if (hdrVal != null && hdrVal.length() > 0) {
                    String ltpa64 = hdrVal;

                    boolean checkLoggedOutToken = webAppSecurityConfig != null && webAppSecurityConfig.isTrackLoggedOutSSOCookiesEnabled();
                    if (checkLoggedOutToken && isTokenLoggedOut(ltpa64)) {
                        cleanupLoggedOutToken(req, res);
                        return authResult;
                    }

                    AuthenticationData authenticationData = createAuthenticationData(req, res, ltpa64, LTPA_OID);
                    try {
                        Subject authenticatedSubject = authenticationService.authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, null);
                        authResult = new AuthenticationResult(AuthResult.SUCCESS, authenticatedSubject, ssoCookieHelper.getSSOCookiename(), null, AuditEvent.OUTCOME_SUCCESS);
                        return authResult;
                    } catch (AuthenticationException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "handleSSO Exception: ", new Object[] { e });
                        }
                        //TODO - Remove authentication cache.
                    }
                }
            }
        }
        return authResult;
    }

    /**
     * If there is no jwtSSOToken, we will return null. Otherwise, we will AuthenticationResult.
     *
     * @param cookies
     * @return
     */
    private AuthenticationResult handleJwtSSO(HttpServletRequest req, HttpServletResponse res) {
        String jwtCookieName = JwtSSOTokenHelper.getJwtCookieName();
        if (jwtCookieName == null) { // jwtsso feature not active
            return null;
        }

        String encodedjwtssotoken = ssoCookieHelper.getJwtSsoTokenFromCookies(req, jwtCookieName);

        if (encodedjwtssotoken == null) { //jwt sso cookie is missing, look at the auth header
            encodedjwtssotoken = getJwtBearerToken(req);
        }
        if (encodedjwtssotoken == null) {
            return null;
        } else {
            if (LoggedOutJwtSsoCookieCache.contains(encodedjwtssotoken)) {
                String LoggedOutMsg = "JWT_ALREADY_LOGGED_OUT";
                if (req.getAttribute(LoggedOutMsg) == null) {
                    Tr.audit(tc, LoggedOutMsg, new Object[] {});
                    req.setAttribute(LoggedOutMsg, "true");
                }
                ssoCookieHelper.removeSSOCookieFromResponse(res);
                return new AuthenticationResult(AuthResult.FAILURE, Tr.formatMessage(tc, LoggedOutMsg));
            }
            return authenticateWithJwt(req, res, encodedjwtssotoken);
        }
    }

    /**
     * Check to see if the token has been logged out.
     *
     * @param ltpaToken
     */
    private boolean isTokenLoggedOut(String ltpaToken) {
        boolean loggedOut = false;
        Object entry = LoggedOutTokenCacheImpl.getInstance().getDistributedObjectLoggedOutToken(ltpaToken);
        if (entry != null)
            loggedOut = true;
        return loggedOut;
    }

    /*
     * simple logout needed to clean up session and sso cookie
     */
    private void cleanupLoggedOutToken(HttpServletRequest req, HttpServletResponse res) {
        AuthenticateApi aa = new AuthenticateApi(ssoCookieHelper, authenticationService);
        aa.simpleLogout(req, res);
    }

    /**
     * Create an authentication data for ltpaToken
     *
     * @param ssoToken
     * @return authenticationData
     */
    private AuthenticationData createAuthenticationData(HttpServletRequest req, HttpServletResponse res, String token, String oid) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.HTTP_SERVLET_REQUEST, req);
        authenticationData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, res);
        if (oid.equals(LTPA_OID)) {
            authenticationData.set(AuthenticationData.TOKEN64, token);
        } else {
            authenticationData.set(AuthenticationData.JWT_TOKEN, token);
        }

        authenticationData.set(AuthenticationData.AUTHENTICATION_MECH_OID, oid);
        return authenticationData;
    }

    @Override
    public AuthenticationResult authenticate(HttpServletRequest req, HttpServletResponse res, HashMap props) throws Exception {
        return null;
    }

    @FFDCIgnore({ AuthenticationException.class })
    private AuthenticationResult authenticateWithJwt(HttpServletRequest req, HttpServletResponse res, String jwtToken) {
        AuthenticationResult authResult = null;

        try {
            AuthenticationData authenticationData = createAuthenticationData(req, res, jwtToken, JWT_OID);
            Subject new_subject = authenticationService.authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND,
                                                                     authenticationData, null);
            authResult = new AuthenticationResult(AuthResult.SUCCESS, new_subject, "jwtToken", null, AuditEvent.OUTCOME_SUCCESS);
            ssoCookieHelper.addJwtSsoCookiesToResponse(new_subject, req, res);
        } catch (AuthenticationException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "authenticateWithJwt exception: ", new Object[] { e });
            }
            authResult = new AuthenticationResult(AuthResult.FAILURE, e.getMessage());
        }
        return authResult;

    }

    /**
     * @param req
     * @return
     */
    private String getJwtBearerToken(HttpServletRequest req) {
        String token = getBearerTokenFromHeader(req);

        return token;
    }

    /**
     * @param req
     * @return
     */
    private String getBearerTokenFromParameter(HttpServletRequest req) {

        String param = null;
        String reqMethod = req.getMethod();
        if (REQ_METHOD_POST.equalsIgnoreCase(reqMethod)) {
            String contentType = req.getHeader(REQ_CONTENT_TYPE_NAME);

            if (REQ_CONTENT_TYPE_APP_FORM_URLENCODED.equals(contentType)) {
                param = req.getParameter(ACCESS_TOKEN);
            }
        }
        return param;
    }

    /**
     * @param req
     * @return
     */
    private String getBearerTokenFromHeader(HttpServletRequest req) {

        String hdrValue = req.getHeader(Authorization_Header);
        String bearerAuthzMethod = "Bearer ";
        if (hdrValue != null && hdrValue.startsWith(bearerAuthzMethod)) {
            return hdrValue.substring(bearerAuthzMethod.length());
        }
        return null;
    }

    /*
     */
    protected boolean isAuthFilterAccept(HttpServletRequest req) {
//        AuthenticationFilter authFilter = null;
//        AuthenticationFilter authFilter = authFilterServiceRef.
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "authFilter:" + authFilter);
//        }
//        if (authFilter != null) {
//            return authFilter.isAccepted(req);
//        }
//        if (tc.isDebugEnabled()) {
//            Tr.debug(tc, "Authentication filter service is not avaliale, all HTTP LTPA SSO requests will be processed");
//        }
        return true;
    }

//    @Reference(name = KEY_FILTER,
//               service = AuthenticationFilter.class,
//               cardinality = ReferenceCardinality.OPTIONAL,
//               policy = ReferencePolicy.DYNAMIC,
//               policyOption = ReferencePolicyOption.GREEDY)
//    protected void setAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "authFilter id: " + ref.getProperty(AuthFilterConfig.KEY_ID) + " authFilterRef: " + ref);
//        }
//        authFilterServiceRef.setReference(ref);
//    }
//
//    protected void updatedAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
//        authFilterServiceRef.setReference(ref);
//    }
//
//    protected void unsetAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "authFilter id: " + ref.getProperty(AuthFilterConfig.KEY_ID) + " authFilterRef: " + ref);
//        }
//        authFilterServiceRef.unsetReference(ref);
//    }

//    @Activate
//    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
//        authFilterServiceRef.activate(cc);
//    }

//    private volatile LTPAConfiguration ltpaConfig;
//
//    protected void setLtpaConfig(LTPAConfiguration ltpaConfig) {
//        this.ltpaConfig = ltpaConfig;
//    }
//
//    protected void unsetLtpaConfig(LTPAConfiguration ltpaConfig) {
//        if (this.ltpaConfig == ltpaConfig) {
//            ltpaConfig = null;
//        }
//    }

//    @Modified
//    protected synchronized void modified(Map<String, Object> props) {}
//
//    @Deactivate
//    protected synchronized void deactivate(ComponentContext cc) {
//        authFilterServiceRef.deactivate(cc);
//    }
}
