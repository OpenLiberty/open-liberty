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
import com.ibm.ws.security.common.web.WebSSOUtils;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;

public class CookieBasedStorage implements Storage {

    public static final TraceComponent tc = Tr.register(CookieBasedStorage.class);

    HttpServletRequest request;
    HttpServletResponse response;
    WebSSOUtils webSsoUtils = new WebSSOUtils();
    ReferrerURLCookieHandler referrerURLCookieHandler;

    public CookieBasedStorage(HttpServletRequest request, HttpServletResponse response) {
        this(request, response, null);
    }

    public CookieBasedStorage(HttpServletRequest request, HttpServletResponse response, ReferrerURLCookieHandler referrerURLCookieHandler) {
        this.request = request;
        this.response = response;
        this.referrerURLCookieHandler = (referrerURLCookieHandler != null) ? referrerURLCookieHandler : webSsoUtils.getCookieHandler();
    }

    @Override
    public void store(String name, @Sensitive String value) {
        store(name, value, null);
    }

    @Override
    public void store(String name, @Sensitive String value, StorageProperties properties) {
        Cookie c = referrerURLCookieHandler.createCookie(name, value, request);
        String domainName = webSsoUtils.getSsoDomain(request);
        if (domainName != null && !domainName.isEmpty()) {
            c.setDomain(domainName);
        }
        if (properties != null) {
            setAdditionalCookieProperties(c, (CookieStorageProperties) properties);
        }
        response.addCookie(c);
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
        referrerURLCookieHandler.invalidateCookie(request, response, name, true);
    }

    private void setAdditionalCookieProperties(Cookie cookie, CookieStorageProperties cookieProps) {
        if (cookieProps.isSecureSet()) {
            cookie.setSecure(cookieProps.isSecure());
        }
        if (cookieProps.isHttpOnlySet()) {
            cookie.setHttpOnly(cookieProps.isHttpOnly());
        }
        cookie.setMaxAge(cookieProps.getStorageLifetimeSeconds());
    }

}
