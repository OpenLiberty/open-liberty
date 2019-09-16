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

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenHelper;
import com.ibm.ws.security.util.ByteArray;
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.internal.StringUtil;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * Single sign-on cookie helper class.
 */
public class SSOCookieHelperImplExtended extends SSOCookieHelperImpl {
    private static final TraceComponent tc = Tr.register(SSOCookieHelperImplExtended.class);

    private static final String OIDC_BROWSER_STATE_COOKIE = "oidc_bsc";

    private final AtomicServiceReference<OidcServer> oidcServerRef;

    /**
     * Only have a custom cookie name for JASPI session as of now.
     */
    public SSOCookieHelperImplExtended(WebAppSecurityConfig config, String ssoCookieName) {
        this(config, ssoCookieName, (AtomicServiceReference<OidcServer>) null);
    }

    public SSOCookieHelperImplExtended(WebAppSecurityConfig config) {
        this(config, null, (AtomicServiceReference<OidcServer>) null);
    }

    public SSOCookieHelperImplExtended(WebAppSecurityConfig config, AtomicServiceReference<OidcServer> oidcServerRef) {
        this(config, null, oidcServerRef);
    }

    private SSOCookieHelperImplExtended(WebAppSecurityConfig config, String ssoCookieName, AtomicServiceReference<OidcServer> oidcServerRef) {
        super(config, ssoCookieName);
        this.oidcServerRef = oidcServerRef;
    }

    /** {@inheritDoc} */
    /**
     * Set-Cookie: <name>=<value>[; <name>=<value>]...
     * [; expires=<date>][; domain=<domain_name>]
     * [; path=<some_path>][; secure][; httponly]
     **/
    @Override
    public void addSSOCookiesToResponse(Subject subject, HttpServletRequest req, HttpServletResponse resp) {
        if (!allowToAddCookieToResponse(req))
            return;
        addJwtSsoCookiesToResponse(subject, req, resp);

        if (!JwtSSOTokenHelper.shouldAlsoIncludeLtpaCookie()) {
            return;
        }

        SingleSignonToken ssoToken = getDefaultSSOTokenFromSubject(subject);
        if (ssoToken == null) {
            return;
        }

        byte[] ssoTokenBytes = ssoToken.getBytes();
        if (ssoTokenBytes == null) {
            return;
        }

        ByteArray cookieBytes = new ByteArray(ssoTokenBytes);
        String cookieByteString = cookieByteStringCache.get(cookieBytes);
        if (cookieByteString == null) {
            cookieByteString = StringUtil.toString(Base64Coder.base64Encode(ssoTokenBytes));
            updateCookieCache(cookieBytes, cookieByteString);
        }

        Cookie ssoCookie = createCookie(req, cookieByteString);
        resp.addCookie(ssoCookie);

        if (oidcServerRef != null && oidcServerRef.getService() != null) {
            // oidc server exists, remove browser state cookie.
            if (isBrowserStateEnabled(req)) {
                removeBrowserStateCookie(req, resp);
            }
        }

    }

    /** {@inheritDoc} */
    /*
     * 1) If we have the custom cookie name, then delete just the custom cookie name
     * 2) If we have the custom cookie name but no cookie found, then will delete the default cookie name LTPAToken2
     */
    @Override
    public void createLogoutCookies(HttpServletRequest req, HttpServletResponse res) {
        Cookie[] cookies = req.getCookies();
        java.util.ArrayList<Cookie> logoutCookieList = new java.util.ArrayList<Cookie>();
        if (cookies != null) {
            String ssoCookieName = resolveCookieName(cookies);
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equalsIgnoreCase(ssoCookieName)) {
                    cookies[i].setValue(null);
                    addLogoutCookieToList(req, ssoCookieName, logoutCookieList);
                } else if (cookies[i].getName().equalsIgnoreCase(OIDC_BROWSER_STATE_COOKIE)) {
                    // remove oidc browser state cookie, if it exists.
                    if (oidcServerRef != null && oidcServerRef.getService() != null) {
                        removeBrowserStateCookie(req, res);
                    }
                }
            }

            logoutJwtCookies(req, cookies, logoutCookieList);

            //TODO: deal with jwtsso's customizable cookie path.
            for (Cookie cookie : logoutCookieList) {
                res.addCookie(cookie);
            }
        }
    }

    /**
     * check whether BrowserState cookie exists in a http request.
     *
     * @param req
     * @return true if the cookie exists, false otherwise
     */
    protected boolean isBrowserStateEnabled(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equalsIgnoreCase(OIDC_BROWSER_STATE_COOKIE)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void removeBrowserStateCookie(HttpServletRequest req, HttpServletResponse res) {
        Cookie c = new Cookie(OIDC_BROWSER_STATE_COOKIE, "");
        c.setMaxAge(0);
        c.setPath("/");
        c.setSecure(req.isSecure());
        res.addCookie(c);
    }
}
