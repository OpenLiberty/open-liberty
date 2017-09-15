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
package com.ibm.ws.webcontainer.security.internal;

import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 *
 */
public class WebSecurityHelperImpl {
    private static final TraceComponent tc = Tr.register(WebSecurityHelperImpl.class);
    private static WebAppSecurityConfig webAppSecConfig = null;

    public static void setWebAppSecurityConfig(WebAppSecurityConfig webAppSecConfig) {
        WebSecurityHelperImpl.webAppSecConfig = webAppSecConfig;
    }

    /**
     * builds an LTPACookie object
     **/
    private static Cookie constructLTPACookieObj(SingleSignonToken ssoToken) {
        byte[] ssoTokenBytes = ssoToken.getBytes();
        String ssoCookieString = Base64Coder.base64EncodeToString(ssoTokenBytes);
        Cookie cookie = new Cookie(webAppSecConfig.getSSOCookieName(), ssoCookieString);
        return cookie;
    }

    /**
     * Gets the LTPA cookie from the given subject
     * 
     * @param subject
     * @return
     * @throws Exception
     */
    static Cookie getLTPACookie(final Subject subject) throws Exception {
        Cookie ltpaCookie = null;
        SingleSignonToken ssoToken = null;
        Set<SingleSignonToken> ssoTokens = subject.getPrivateCredentials(SingleSignonToken.class);
        Iterator<SingleSignonToken> ssoTokensIterator = ssoTokens.iterator();
        if (ssoTokensIterator.hasNext()) {
            ssoToken = ssoTokensIterator.next();
            if (ssoTokensIterator.hasNext()) {
                throw new WSSecurityException("More than one ssotoken found in subject");
            }
        }
        if (ssoToken != null) {
            ltpaCookie = constructLTPACookieObj(ssoToken);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No ssotoken found for this subject");
            }
        }
        return ltpaCookie;
    }

    /**
     * Extracts an LTPA sso cookie from the subject of current thread
     * and builds a ltpa cookie out of it for use on downstream web invocations.
     * The caller must check for null return value only when not null
     * that getName and getValue can be invoked on the returned Cookie object
     * 
     * @return an object of type javax.servlet.http.Cookie. When the returned value is not
     *         null use Cookie methods getName() and getValue() to set the Cookie header
     *         on an http request with header value of Cookie.getName()=Cookie.getValue()
     */
    public static Cookie getSSOCookieFromSSOToken() throws Exception {
        Subject subject = null;
        Cookie ltpaCookie = null;
        if (webAppSecConfig == null) {
            // if we don't have the config, we can't construct the cookie
            return null;
        }
        try {
            subject = WSSubject.getRunAsSubject();
            if (subject == null) {
                subject = WSSubject.getCallerSubject();
            }
            if (subject != null) {
                ltpaCookie = getLTPACookie(subject);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "No subjects on the thread");
                }
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getSSOCookieFromSSOToken caught exception: " + e.getMessage());
            }
            throw e;
        }
        return ltpaCookie;
    }

    /**
     * @return
     */
    public static WebAppSecurityConfig getWebAppSecurityConfig() {
        return webAppSecConfig;
    }

}
