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

import com.ibm.websphere.security.WebSphereRuntimePermission;
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
    private final static WebSphereRuntimePermission MODIFY_TOKEN = new WebSphereRuntimePermission("modify_token");

    /**
     * Extracts the Single Sign-On (SSO) token from the subject of the current thread
     * and builds an SSO cookie out of it for use on downstream web invocations.
     * The caller must check for null return value.
     * <p>
     * Return null if there is an invalid or expired SSO token, no subject on the current thread, no SSO token in subject or no webAppSecurityConfig object.
     * When the returned value is not null use Cookie methods getName() and getValue()
     * to set the Cookie header on an HTTP request with header value of
     * Cookie.getName()=Cookie.getValue()
     * <p>
     *
     * @return An object of type javax.servlet.http.Cookie. May return {@code null}
     */
    public static Cookie getSSOCookieFromSSOToken() throws Exception {
        return WebSecurityHelperImpl.getSSOCookieFromSSOToken();
    }

    /**
     * Extracts the Single Sign-On (SSO) token from the subject of the current thread
     * and builds an SSO cookie out of it without the attributes specified in the ignoreAttributes parameter for use on downstream web invocations.
     * The caller must check for null return value.
     * The security permission WebSphereRuntimePermission("modify_token") is needed when security manager is enabled.
     * <p>
     * Return null if there is an invalid or expired SSO token, no subject on the current thread, no SSO token in subject or no webAppSecurityConfig object.
     * If the returned value is not null, use Cookie methods getName() and getValue()
     * to set the Cookie header on an HTTP request with header value of
     * Cookie.getName()=Cookie.getValue()
     *
     * @param String ... A list of attributes to be removed.
     *            If null is specified, the custom cache key AttributeNameConstants.WSCREDENTIAL_CACHE_KEY will be removed from the SSO token.
     *
     * @return An object of type javax.servlet.http.Cookie. May return {@code null}
     * @throws Exception If SecurityManager exists and does not permit token modification.
     */
    public static Cookie getSSOCookieFromSSOToken(String... ignoreAttributes) throws Exception {
        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(MODIFY_TOKEN);
        }

        if (ignoreAttributes == null) {
            return WebSecurityHelperImpl.getSSOCookieFromSSOToken(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
        } else {
            return WebSecurityHelperImpl.getSSOCookieFromSSOToken(ignoreAttributes);
        }

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
