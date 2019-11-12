/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.web;

import javax.servlet.http.Cookie;

import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.internal.WebSecurityHelperImpl;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * Provides methods to perform security functions for web applications.
 *
 * @author International Business Machines Corp.
 * @version WAS 8.5
 * @since WAS 8.0
 * @ibm-api
 */
public class WebSecurityHelper {
    static final String[] cache_key = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };

    /**
     * Extracts the SSO token from the subject of current thread
     * and builds an SSO cookie out of it for use on downstream web invocations.
     * The caller must check for null return value.
     * <p>
     * When the returned value is not null use Cookie methods getName() and getValue()
     * to set the Cookie header on an HTTP request with header value of
     * Cookie.getName()=Cookie.getValue()
     *
     * @return an object of type javax.servlet.http.Cookie. May return {@code null}.
     */
    public static Cookie getSSOCookieFromSSOToken() throws Exception {
        return WebSecurityHelperImpl.getSSOCookieFromSSOToken();
    }

    /**
     * Extracts the SSO token from the subject of current thread
     * and builds an SSO cookie out of it without a list of attributes for use on downstream web invocations.
     * The caller must check for null return value.
     * <p>
     * When the returned value is not null use Cookie methods getName() and getValue()
     * to set the Cookie header on an HTTP request with header value of
     * Cookie.getName()=Cookie.getValue()
     *
     * @param attributes A list of attributes. If attributes is null, then default is AttributeNameConstants.WSCREDENTIAL_CACHE_KEY
     * @return an object of type javax.servlet.http.Cookie. May return {@code null}.
     */
    public static Cookie getSSOCookieFromSSOTokenWithOutAttrs(String[] attributes) throws Exception {
        if (attributes == null || attributes.length < 1) {
            attributes = cache_key;
        }

        return WebSecurityHelperImpl.getSSOCookieFromSSOTokenWithOutAttrs(attributes);
    }

    /**
     * Extracts the SSO cookie name for use on downstream web invocations.
     * Return null when the service is not started or activated.
     *
     * @return a String.
     */
    public static String getSSOCookieName() throws Exception {
        WebAppSecurityConfig config = WebSecurityHelperImpl.getWebAppSecurityConfig();
        if (config != null) {
            return config.getSSOCookieName();
        }
        return null;
    }

    /**
     * Extracts the JWT cookie name for use on downstream web invocations.
     * Return null when the service is not started or activated.
     *
     * @return a String.
     */
    public static String getJwtCookieName() {
        return WebSecurityHelperImpl.getJwtCookieName();
    }
}
