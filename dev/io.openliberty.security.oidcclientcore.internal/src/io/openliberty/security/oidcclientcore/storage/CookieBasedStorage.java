/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.storage;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import io.openliberty.security.oidcclientcore.JakartaOIDCConstants;

/**
 *
 */
public class CookieBasedStorage implements Storage {

    public static final TraceComponent tc = Tr.register(CookieBasedStorage.class);

    HttpServletRequest request;
    HttpServletResponse response;
    ReferrerURLCookieHandler referrerURLCookieHandler;

    public CookieBasedStorage(ReferrerURLCookieHandler referrerURLCookieHandler, HttpServletRequest request, HttpServletResponse response) {
        this.referrerURLCookieHandler = referrerURLCookieHandler;
        this.request = request;
        this.response = response;
    }

    @Override
    public void store(String name, @Sensitive String value) {
        String cookieName = JakartaOIDCConstants.COOKIE_NAME_OIDC_CLIENT_CORE_PREFIX + name;
        addCookie(cookieName, value);
    }

    @Override
    public String get(String name) {
        Cookie[] cookies = request.getCookies();
        for (Cookie c : cookies) {
            if (c.getName().equals(name)) {
                return c.getValue();
            }
        }
        // error message needed here
        return null;
    }

    public void remove(String name) {
        if (name == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "CookieBasedStorage.remove param is null, return");
            }
            return;
        }
        Cookie c = createCookie(name, "", -1);
        String domainName = getSsoDomain(request);
        if (domainName != null && !domainName.isEmpty()) {
            c.setDomain(domainName);
        }
        c.setMaxAge(0);
        response.addCookie(c);
    }

    public void addCookie(String cookieName, @Sensitive String cookieValue) {
        Cookie c = createCookie(cookieName, cookieValue, -1);
        boolean isHttpsRequest = request.getScheme().toLowerCase().contains("https");
        if (isHttpsRequest) {
            c.setSecure(true);
        }
        response.addCookie(c);
    }

    public Cookie createCookie(String cookieName, @Sensitive String cookieValue, int maxAge) {
        Cookie cookie = referrerURLCookieHandler.createCookie(cookieName, cookieValue, request);
        String domainName = getSsoDomain(request);
        if (domainName != null && !domainName.isEmpty()) {
            cookie.setDomain(domainName);
        }
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    public static String getSsoDomain(HttpServletRequest req) {
        SSOCookieHelper ssoCookieHelper = getWebAppSecurityConfig().createSSOCookieHelper();
        String domainName = ssoCookieHelper.getSSODomainName(req,
                                                             getWebAppSecurityConfig().getSSODomainList(),
                                                             getWebAppSecurityConfig().getSSOUseDomainFromURL());
        return domainName;
    }

    public static WebAppSecurityConfig getWebAppSecurityConfig() {
        return WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
    }

}
