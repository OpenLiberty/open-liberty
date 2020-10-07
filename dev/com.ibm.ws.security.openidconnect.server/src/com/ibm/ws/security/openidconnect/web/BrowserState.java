/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.web;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.server.plugins.OIDCBrowserStateUtil;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;

public class BrowserState
{
    private static TraceComponent tc = Tr.register(BrowserState.class);

    /**
     * Generate session_state attribute and set the value into AttributeList.
     * 
     * @param request is the incoming HttpServletRequest
     * @param options is the AttributeList object which contains optional parameters which will be consumed by OAuth/OIDC handlers.
     * 
     * @throws IOException
     */
    protected void generateState(HttpServletRequest request, AttributeList options)
    {
        String current = OIDCBrowserStateUtil.generateOIDCBrowserState(false);
        String salt = java.lang.Long.toString(System.nanoTime(), 16);
        String clientId = request.getParameter(OAuth20Constants.CLIENT_ID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "clientId : " + clientId + " current browser session : " + current + " salt : " + salt);
        }
        String output = OidcSessionManagementUtil.calculateSessionState(clientId, current, salt);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "session_state : " + output);
        }
        options.setAttribute(OIDCConstants.OIDC_SESSION_STATE, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[] { output });
    }

    /**
     * Set browser state cookie and set a parameters for generating session_state attribute enabled.
     * 
     * @param request is the incoming HttpServletRequest
     * @param response is the outgoing HttpServletResponse
     * 
     * @throws IOException
     */
    protected void processSession(HttpServletRequest request, HttpServletResponse response)
    {
        // compute current state
        String current = OIDCBrowserStateUtil.generateOIDCBrowserState(false);

        // get original state
        String original = getOriginalBrowserState(request);

        processBrowserStateCookie(original, current, response, request);
    }

    /**
     * get current browser state cookie from request.
     * 
     * @param request is the incoming HttpServletRequest
     */
    protected String getOriginalBrowserState(HttpServletRequest request)
    {
        // get current cookie
        Cookie[] cookies = request.getCookies();
        String output = CookieHelper.getCookieValue(cookies, OIDCConstants.OIDC_BROWSER_STATE_COOKIE);
        return output;
    }

    /**
     * Add, or replace, a browser state cookie if original and current browser state values do not match.
     * 
     * @param original the original browser state value
     * @param current the current computed browser state value
     * @param response is the outgoing HttpServletResponse
     * 
     * @throws IOException
     */

    protected void processBrowserStateCookie(String original, String current, HttpServletResponse response, HttpServletRequest request)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, ("original browser state : " + original + " current browser state : " + current));
        }

        if (current != null && !current.equals(original)) {
            // setcookie
            ReferrerURLCookieHandler handler = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig().createReferrerURLCookieHandler();
            Cookie c = handler.createCookie(OIDCConstants.OIDC_BROWSER_STATE_COOKIE, current, false, request);
            response.addCookie(c);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "A browser session state cookie is set.");
            }
        }
    }
}
