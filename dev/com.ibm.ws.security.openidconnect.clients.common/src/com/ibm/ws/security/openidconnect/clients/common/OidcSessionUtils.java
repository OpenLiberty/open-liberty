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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants;

/**
 * Helper functions used by oidc client and social login
 * to logout invalidated oidc sessions and cleanup logged out oidc sessions
 */
public class OidcSessionUtils {

    private static TraceComponent tc = Tr.register(OidcSessionUtils.class);

    public static void logoutIfSessionInvalidated(HttpServletRequest request, OidcSessionInfo sessionInfo, ConvergedClientConfig clientConfig, WebAppSecurityConfig webAppSecurityConfig) {
        // don't logout if state exists (hasn't authenticated yet)
        if (requestHasStateCookie(request)) {
            return;
        }

        if (sessionInfo == null && requestHasOidcOrSsoCookie(request, clientConfig, webAppSecurityConfig)) {
            performLogout(request);
            return;
        }

        OidcSessionCache oidcSessionCache = clientConfig.getOidcSessionCache();
        if (!oidcSessionCache.isSessionInvalidated(sessionInfo)) {
            return;
        }

        performLogout(request);
    }

    private static boolean requestHasStateCookie(HttpServletRequest request) {
        return CookieHelper.getCookie(request.getCookies(), OidcClientStorageConstants.WAS_OIDC_STATE_KEY) != null;
    }

    private static boolean requestHasOidcOrSsoCookie(HttpServletRequest request, ConvergedClientConfig clientConfig, WebAppSecurityConfig webAppSecurityConfig) {
        if (clientConfig.isDisableLtpaCookie()) {
            return requestHasOidcClientCookie(request, clientConfig);
        } else {
            return requestHasSsoCookie(request, webAppSecurityConfig);
        }
    }

    private static boolean requestHasOidcClientCookie(HttpServletRequest request, ConvergedClientConfig clientConfig) {
        return CookieHelper.getCookie(request.getCookies(), clientConfig.getOidcClientCookieName()) != null;
    }

    private static boolean requestHasSsoCookie(HttpServletRequest request, WebAppSecurityConfig webAppSecurityConfig) {
        return CookieHelper.getCookie(request.getCookies(), webAppSecurityConfig.getSSOCookieName()) != null;
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

    public static void removeOidcSession(
            HttpServletRequest request,
            HttpServletResponse response,
            ConvergedClientConfig clientConfig) {
        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientConfig);
        if (sessionInfo == null) {
            return;
        }

        String configId = sessionInfo.getConfigId();
        if (!HashUtils.digest(clientConfig.getId()).equals(configId)) {
            return;
        }

        String sub = sessionInfo.getSub();
        String sid = sessionInfo.getSid();

        OidcSessionCache oidcSessionCache = clientConfig.getOidcSessionCache();
        if (!oidcSessionCache.isSessionInvalidated(sessionInfo)) {
            if (sid != null && !sid.isEmpty()) {
                oidcSessionCache.invalidateSession(sub, sid);
            } else {
                oidcSessionCache.invalidateSessionBySessionId(sub, sessionInfo.getSessionId());
            }
        }
        oidcSessionCache.removeInvalidatedSession(sessionInfo);

        clearWASOidcSessionCookie(request, response);
    }

    private static void clearWASOidcSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieHelper.clearCookie(request, response, ClientConstants.WAS_OIDC_SESSION, request.getCookies());
    }

}
