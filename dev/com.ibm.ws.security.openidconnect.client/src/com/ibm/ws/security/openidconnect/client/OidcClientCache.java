/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.openidconnect.token.IdToken;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OidcUtil;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 *
 */
public class OidcClientCache {
    private static final TraceComponent tc = Tr.register(OidcClientCache.class);
    static final String UTF8 = "UTF-8";

    AuthCacheService authCache = null;
    OidcClientConfig clientCfg = null;
    OidcClientRequest oidcClientRequest = null;
    String preKeyValue = null;
    @Sensitive
    String customCacheKey = null;

    public OidcClientCache(AuthCacheService authCacheService, OidcClientConfig oidcClientConfig, OidcClientRequest oidcClientRequest) {
        this.authCache = authCacheService;
        this.clientCfg = oidcClientConfig;
        this.oidcClientRequest = oidcClientRequest;
    }

    public Subject getBackValidSubject(HttpServletRequest req, OidcClientConfig clientConfig) {
        String preKeyValue = getPreKeyValue(req);
        if (preKeyValue == null || preKeyValue.isEmpty()) { // no cookie yet
            return null;
        }

        Subject subject = getBackCachedSubject(req, preKeyValue);

        if (subject != null) {
            if (!isValid(subject, clientConfig.getReAuthnCushion(), clientConfig.isReAuthnOnAccessTokenExpire())) { // check access token?
                removeSubject(req);
                subject = null;
            }
        }
        return subject;
    }

    public Subject getBackCachedSubject(HttpServletRequest req, String preKeyValue) {
        Subject subject = null;

        this.customCacheKey = oidcClientRequest.getCustomCookieValue(preKeyValue);
        if (this.customCacheKey != null && !this.customCacheKey.isEmpty()) {
            subject = authCache.getSubject(this.customCacheKey);
        }
        return subject;
    }

    public void removeSubject(HttpServletRequest req) {
        if (this.customCacheKey == null) {
            String preKeyValue = getPreKeyValue(req);
            this.customCacheKey = oidcClientRequest.getCustomCookieValue(preKeyValue);
        }
        if (customCacheKey != null && !customCacheKey.isEmpty()) {
            authCache.remove(customCacheKey);
        }
        // remove the invalid key

        OidcUtil.removeCookie(oidcClientRequest);
    }

    boolean isValid(Subject subject, long cushionMilliseconds, boolean checkAccessToken) {
        boolean valid = true;
        // TODO handle different when refresh token exist and valid
        IdToken idToken = getIdToken(subject);
        if (idToken != null) {
            valid = this.isIdTokenValid(idToken, cushionMilliseconds);
        }
        if (valid) {
            if (checkAccessToken)
                valid = isAccessTokenValid(subject, cushionMilliseconds);
        }

        return valid;
    }

    boolean isIdTokenValid(IdToken idToken, long cushionMilliseconds) {
        long expSeconds = idToken.getExpirationTimeSeconds();
        //long atIssue = idToken.getIssuedAtTimeSeconds();
        Date date = new Date();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "date(" + date.getTime() + ") expSec(" + expSeconds + ") cushionMillisec(" + cushionMilliseconds + ")");
        }
        if (expSeconds * 1000 - cushionMilliseconds > date.getTime()) { // We want it expires earlier with cushion
            return true;
        }

        return false;
    }

    boolean isAccessTokenValid(Subject subject, long cushionMilliseconds) {

        String strExpiresIn = (String) getOAuthAttribute(subject, "expires_in");
        if (strExpiresIn == null || strExpiresIn.isEmpty()) {
            // In this case, the access_token was not produced by the RP
            // It must be an access_token came in through RS.
            // But in RS, no custom cookie is produced (no matter the inboundPropagation is required or supported)
            // So, it should never be here. But return false in case something strange happened
            return false;
        }
        long lExpiresIn = 0l;
        try {
            lExpiresIn = Long.parseLong(strExpiresIn) * 1000; //cushionMilliseconds change from seconds to milliseconds
        } catch (NumberFormatException e) {
            // This should not happen
            // if it happens, the ExpiresIn will be 0L
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "hit unexpected exception", e);
            }
        }
        Long storeTimeMilliseconds = (Long) getOAuthAttribute(subject, ClientConstants.CREDENTIAL_STORING_TIME_MILLISECONDS);

        Date date = new Date();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "date(" + date.getTime() + ") storeMilli(" + storeTimeMilliseconds + ") cushion(" + cushionMilliseconds + ")");
        }
        if (storeTimeMilliseconds + lExpiresIn - cushionMilliseconds > date.getTime()) {
            return true;
        }

        return false;
    }

    protected IdToken getIdToken(Subject subject) {
        IdToken idToken = null;

        if (subject != null) {
            Set<IdToken> idTokens = subject.getPublicCredentials(IdToken.class);
            for (IdToken idTokenTmp : idTokens) {
                idToken = idTokenTmp;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "public IdToken:" + idToken);
                }
                break;
            }
            if (idToken == null) {
                Set<IdToken> privateIdTokens = subject.getPrivateCredentials(IdToken.class);
                for (IdToken idTokenTmp : privateIdTokens) {
                    idToken = idTokenTmp;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "private IdToken:" + idToken);
                    }
                    break;
                }
            }
        }
        return idToken;
    }

    protected Object getOAuthAttribute(Subject subject, String attribKey) {
        Set<Object> publicCredentials = subject.getPublicCredentials();
        int iCnt = 0;
        for (Object credentialObj : publicCredentials) {
            iCnt++;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "publicCredential(" + iCnt + ") class:" + credentialObj.getClass().getName());
            }
            if (credentialObj instanceof Map) {
                Object obj = ((Map<?, ?>) credentialObj).get(attribKey);
                if (obj != null)
                    return obj;
            }
        }
        Set<Object> privCredentials = subject.getPrivateCredentials();
        for (Object credentialObj : privCredentials) {
            iCnt++;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "privateCredential(" + iCnt + ") class:" + credentialObj.getClass().getName());
            }
            if (credentialObj instanceof Map) {
                Object obj = ((Map<?, ?>) credentialObj).get(attribKey);
                if (obj != null)
                    return obj;
            }
        }
        return null;
    }

    public String getPreKeyValue(HttpServletRequest req) {
        if (this.preKeyValue == null) {
            try {
                String oidcClientCookieName = oidcClientRequest.getOidcClientCookieName(); //
                byte[] cookieValueBytes = ((IExtendedRequest) req).getCookieValueAsBytes(oidcClientCookieName);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "cookieValueBytes is null:" + (cookieValueBytes == null));
                }
                if (cookieValueBytes != null && cookieValueBytes.length > 0) {
                    this.preKeyValue = new String(cookieValueBytes, UTF8);
                }
            } catch (Exception e) {
                return null;
            }
        }
        return this.preKeyValue;
    }

    /**
     * @return
     */
    @Trivial
    public String getCustomCacheKey() {
        return customCacheKey;
    }
}
