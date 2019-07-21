/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.extended;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

/**
 * Contains all WASReqURL Cookie related functions required by WAS.security.
 * Unless a method is explicitly needed by a class outside of this package,
 * methods should be left as default scope.
 */
public class ReferrerURLCookieHandlerExtended extends ReferrerURLCookieHandler {
    private static final TraceComponent tc = Tr.register(ReferrerURLCookieHandlerExtended.class);
    public static final String REFERRER_URL_COOKIENAME = "WASReqURL";
    public static final String CUSTOM_RELOGIN_URL_COOKIENAME = "WASReLoginURL";

    public ReferrerURLCookieHandlerExtended(WebAppSecurityConfig webAppSecConfig) {
        super(webAppSecConfig);
    }

    /**
     * @param cookieName
     * @param value
     * @return
     */
    public Cookie createCookie(String cookieName, @Sensitive String value, boolean enableHttpOnly, HttpServletRequest req) {
        Cookie c = new Cookie(cookieName, value);
        if (cookieName.equals("WASReqURL") || cookieName.startsWith("WASOidcStateKey")) {
            c.setPath(getPathName(req));
        }
        else {
            c.setPath("/");
        }
        c.setMaxAge(-1);
        if (enableHttpOnly && webAppSecConfig.getHttpOnlyCookies()) {
            c.setHttpOnly(true);
        }
        if (webAppSecConfig.getSSORequiresSSL()) {
            c.setSecure(true);
        }
        return c;
    }

}
