/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class SpCookieRetriver {
    public static final TraceComponent tc = Tr.register(SpCookieRetriver.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);
    IExtendedRequest req = null;
    SsoRequest samlRequest = null;
    AuthCacheService authCacheService = null;
    String providerId = null;
    @Sensitive
    String customCacheKey = null;
    boolean cacheKeyInitialized = false;

    /**
     * @param req
     * @param samlRequest
     */
    public SpCookieRetriver(AuthCacheService authCacheService,
                            HttpServletRequest req,
                            SsoRequest samlRequest) {
        this.req = (IExtendedRequest) req;
        this.samlRequest = samlRequest;
        this.authCacheService = authCacheService;
        providerId = samlRequest.getProviderName();
    }

    public Subject getSubjectFromSpCookie() {
        Subject result = null;
        if (authCacheService == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ERROR: No activated authCacheService. Of course no cached subject");
            }
            return null;
        }
        String cacheKey = getCustomCacheKey();
        if (cacheKey != null) {
            result = authCacheService.getSubject(customCacheKey);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Get Subject:" + result);
        }
        return result;
    }

    public void removeSubject() {
        if (authCacheService != null) {
            String cacheKey = getCustomCacheKey();
            if (cacheKey != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "remove Subject");
                }
                authCacheService.remove(cacheKey);
                return;
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Does not remove Subject. It's probably OK.");
        }
    }

    @Sensitive
    @Trivial
    public String getCustomCacheKey() {
        if (!cacheKeyInitialized) {
            // get SP Cookie from request
            String spCookieName = samlRequest.getSpCookieName();
            byte[] cookieValueBytes = req.getCookieValueAsBytes(spCookieName);
            if (cookieValueBytes != null) {
                String preKey = RequestUtil.convertBytesToString(cookieValueBytes);
                customCacheKey = AssertionToSubject.getAfterDigestValue(providerId, preKey);
            }
            cacheKeyInitialized = true;
        }
        return customCacheKey;
    }

}
