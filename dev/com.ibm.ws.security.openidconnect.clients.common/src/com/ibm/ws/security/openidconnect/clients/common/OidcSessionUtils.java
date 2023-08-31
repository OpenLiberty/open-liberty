/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants;

/**
 * Helper functions used by oidc client and social login
 * to logout invalidated oidc sessions and cleanup logged out oidc sessions
 */
public class OidcSessionUtils {

    private static TraceComponent tc = Tr.register(OidcSessionUtils.class);

    public static void logoutIfSessionInvalidated(HttpServletRequest request, ConvergedClientConfig clientConfig, WebAppSecurityConfig webAppSecurityConfig) {
        // don't logout if state exists (hasn't authenticated yet)
        if (requestHasStateCookie(request)) {
            return;
        }

        //        if (sessionInfo == null && requestHasOidcOrSsoCookie(request, clientConfig, webAppSecurityConfig)) {
        //            performLogout(request);
        //            return;
        //        }

        OidcSessionCache oidcSessionCache = clientConfig.getOidcSessionCache();
        if (!oidcSessionCache.isSessionInvalidated(getOidcOrSsoCookieValue(request, clientConfig, webAppSecurityConfig))) {
            return;
        }

        performLogout(request);
    }

    private static boolean requestHasStateCookie(HttpServletRequest request) {
        return CookieHelper.getCookie(request.getCookies(), OidcClientStorageConstants.WAS_OIDC_STATE_KEY) != null;
    }

    private static String getOidcOrSsoCookieValue(HttpServletRequest request, ConvergedClientConfig clientConfig, WebAppSecurityConfig webAppSecurityConfig) {
        if (clientConfig.isDisableLtpaCookie()) {
            return getOidcClientCookieValue(request, clientConfig);
        } else {
            return getSsoCookieValue(request, webAppSecurityConfig);
        }
    }

    private static String getOidcClientCookieValue(HttpServletRequest request, ConvergedClientConfig clientConfig) {
        Cookie oidcClientCookie = CookieHelper.getCookie(request.getCookies(), clientConfig.getOidcClientCookieName());
        if (oidcClientCookie == null) {
            return null;
        }
        return oidcClientCookie.getValue();
    }

    private static String getSsoCookieValue(HttpServletRequest request, WebAppSecurityConfig webAppSecurityConfig) {
        Cookie ssoCookie = CookieHelper.getCookie(request.getCookies(), webAppSecurityConfig.getSSOCookieName());
        if (ssoCookie == null) {
            return null;
        }
        return ssoCookie.getValue();
    }

    private static void performLogout(HttpServletRequest request) {
        try {
            request.logout();
        } catch (ServletException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not logout invalidated session. An exception is caught : " + e);
            }
        }
    }

    public static void insertSession(ConvergedClientConfig clientConfig, WebAppSecurityConfig webAppSecurityConfig, OidcTokenImplBase idToken) {
        OidcSessionCache sessionCache = clientConfig.getOidcSessionCache();
        String configId = clientConfig.getClientId();
        String iss = (String) idToken.getClaim("iss");
        String sub = (String) idToken.getClaim("sub");
        String sid = (String) idToken.getClaim("sid");
        Long exp = (Long) idToken.getClaim("exp");
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, exp.toString(), "");
        sessionCache.insertSession(sessionInfo);
    }

    public static void removeOidcSession(
            HttpServletRequest request,
            HttpServletResponse response,
            ConvergedClientConfig clientConfig) {
        //        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientConfig);
        //        if (sessionInfo == null) {
        //            return;
        //        }
        //
        //        String configId = sessionInfo.getConfigId();
        //        if (!HashUtils.digest(clientConfig.getId()).equals(configId)) {
        //            return;
        //        }
        //
        //        String sub = sessionInfo.getSub();
        //        String sid = sessionInfo.getSid();
        //
        //        OidcSessionCache oidcSessionCache = clientConfig.getOidcSessionCache();
        //        if (!oidcSessionCache.isSessionInvalidated(sessionInfo)) {
        //            if (sid != null && !sid.isEmpty()) {
        //                oidcSessionCache.invalidateSession(sub, sid);
        //            } else {
        //                oidcSessionCache.invalidateSessionBySessionId(sub, sessionInfo.getSessionId());
        //            }
        //        }
        //        oidcSessionCache.removeInvalidatedSession(sessionInfo);
        //
        //        clearWASOidcSessionCookie(request, response);
    }

    private static void clearWASOidcSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieHelper.clearCookie(request, response, ClientConstants.WAS_OIDC_SESSION, request.getCookies());
    }

}
