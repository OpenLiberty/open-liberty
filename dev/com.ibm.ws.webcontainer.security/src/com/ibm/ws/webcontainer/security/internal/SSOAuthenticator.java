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
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
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

/**
 * This class perform authentication for web request using single sign on cookie.
 */
public class SSOAuthenticator implements WebAuthenticator {

    public static final String DEFAULT_SSO_COOKIE_NAME = "LtpaToken2";

    private static final TraceComponent tc = Tr.register(SSOAuthenticator.class);
    private final AuthenticationService authenticationService;
    private final WebAppSecurityConfig webAppSecurityConfig;
    private final SSOCookieHelper ssoCookieHelper;
    private final String challengeType;

    /**
     * @param authenticationServ
     * @param securityMetadata
     */
    public SSOAuthenticator(AuthenticationService authenticationService,
                            SecurityMetadata securityMetadata,
                            WebAppSecurityConfig webAppSecurityConfig,
                            SSOCookieHelper ssoCookieHelper) {
        this.authenticationService = authenticationService;
        this.webAppSecurityConfig = webAppSecurityConfig;
        this.ssoCookieHelper = ssoCookieHelper;

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
        return authResult;
    }

    //TODO Need a new design to improve performance when we have multiple cookie with the same name.
    /**
     * @param req
     * @param res
     * @return authResult
     */
    @FFDCIgnore({ AuthenticationException.class })
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

                    AuthenticationData authenticationData = createAuthenticationData(req, res, ltpa64);
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

        ssoCookieHelper.createLogoutCookies(req, res);
        return authResult;
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
    private AuthenticationData createAuthenticationData(HttpServletRequest req, HttpServletResponse res, String ltpaToken) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.HTTP_SERVLET_REQUEST, req);
        authenticationData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, res);
        authenticationData.set(AuthenticationData.TOKEN64, ltpaToken);
        return authenticationData;
    }

    @Override
    public AuthenticationResult authenticate(HttpServletRequest req, HttpServletResponse res, HashMap props) throws Exception {
        return null;
    }
}
