/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import com.ibm.ws.webcontainer.security.CookieHelper;

/**
 * Helper functions used by oidc client and social login
 * to logout invalidated oidc sessions and cleanup logged out oidc sessions
 */
public class OidcSessionUtils {

    private static TraceComponent tc = Tr.register(OidcSessionUtils.class);

    public static void logoutIfSessionInvalidated(HttpServletRequest request, OidcSessionInfo sessionInfo, ConvergedClientConfig clientConfig) {
        // don't logout if state exists (hasn't authenticated yet)
        if (requestHasStateCookie(request)) {
            return;
        }

        OidcSessionCache oidcSessionCache = clientConfig.getOidcSessionCache();
        if (!oidcSessionCache.isSessionInvalidated(sessionInfo)) {
            return;
        }

        try {
            request.logout();
        } catch (ServletException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not logout invalidated session. An exception is caught : " + e);
            }
        }
    }

    private static boolean requestHasStateCookie(HttpServletRequest request) {
        return CookieHelper.getCookie(request.getCookies(), ClientConstants.WAS_OIDC_STATE_KEY) != null;
    }

    public static void removeOidcSession(
            HttpServletRequest request,
            HttpServletResponse response,
            ConvergedClientConfig clientConfig) {
        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request);
        if (sessionInfo == null) {
            return;
        }

        String configId = sessionInfo.getConfigId();
        if (!clientConfig.getId().equals(configId)) {
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
