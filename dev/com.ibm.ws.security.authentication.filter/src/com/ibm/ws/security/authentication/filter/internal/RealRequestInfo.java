/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authentication.filter.internal;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

public class RealRequestInfo implements IRequestInfo {
    private final HttpServletRequest req;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final static String BEARER_AUTHZ = "Bearer ";

    public RealRequestInfo(HttpServletRequest req) {
        this.req = req;
    }

    @Override
    public String getHeader(String name) {
        return req.getHeader(name);
    }

    @Override
    public String getRequestURL() {
        String requestUrl = null;
        String queryString = req.getQueryString();
        if (queryString != null) {
            requestUrl = req.getRequestURL().toString() + "?" + queryString;
        } else {
            requestUrl = req.getRequestURL().toString();
        }
        return requestUrl;
    }

    @Override
    public String getRemoteAddr() {
        return req.getRemoteAddr();
    }

    @Override
    public String getApplicationName() {
        String appName = null;
        ComponentMetaData wcmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (wcmd != null) {
            appName = wcmd.getModuleMetaData().getApplicationMetaData().getName();
        }
        return appName;
    }

    @Override
    public String getCookieName(String cookieName) {
        Cookie[] cookies = req.getCookies();

        if (cookies == null)
            return null;

        for (int i = 0; i < cookies.length; ++i) {
            if (cookieName.equalsIgnoreCase(cookies[i].getName()) && cookies[i].getValue() != null) {
                return cookieName;
            }
        }

        return null;
    }

}
