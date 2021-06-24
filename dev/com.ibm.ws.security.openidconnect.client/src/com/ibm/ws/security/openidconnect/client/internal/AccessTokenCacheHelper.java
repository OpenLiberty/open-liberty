/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.internal;

import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.structures.SingleTableCache;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

public class AccessTokenCacheHelper {

    private static final TraceComponent tc = Tr.register(AccessTokenCacheHelper.class);

    public ProviderAuthenticationResult getCachedTokenAuthenticationResult(OidcClientConfig clientConfig, String token) {
        if (!clientConfig.getTokenReuse()) {
            return null;
        }
        SingleTableCache cache = clientConfig.getCache();
        ProviderAuthenticationResult result = (ProviderAuthenticationResult) cache.get(token);
        if (result != null) {
            if (isTokenInCachedResultExpired(result, clientConfig)) {
                return null;
            }
            Subject newSubject = recreateSubject(result.getSubject());
            return new ProviderAuthenticationResult(result.getStatus(), result.getHttpStatusCode(), result.getUserName(), newSubject, result.getCustomProperties(), result.getRedirectUrl());
        }
        return null;
    }

    public void cacheTokenAuthenticationResult(OidcClientConfig clientConfig, String token, ProviderAuthenticationResult result) {
        SingleTableCache cache = clientConfig.getCache();
        cache.put(token, result);
    }

    boolean isTokenInCachedResultExpired(ProviderAuthenticationResult cachedResult, OidcClientConfig clientConfig) {
        Hashtable<String, Object> customProperties = cachedResult.getCustomProperties();
        if (customProperties == null || customProperties.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Custom properties were null or empty");
            }
            return true;
        }
        long tokenExp = getTokenExpirationFromCustomProperties(customProperties);
        long clockSkew = clientConfig.getClockSkewInSeconds();
        long now = System.currentTimeMillis() / 1000;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Current system time: " + now + ", token expiration time: " + tokenExp + ", clockSkew: " + clockSkew);
        }
        if (now > (tokenExp + clockSkew)) {
            return true;
        }
        return false;
    }

    @FFDCIgnore(Exception.class)
    @SuppressWarnings("unchecked")
    long getTokenExpirationFromCustomProperties(Hashtable<String, Object> customProperties) {
        if (customProperties == null || customProperties.isEmpty()) {
            return 0;
        }
        if (!customProperties.containsKey(Constants.ACCESS_TOKEN_INFO)) {
            return 0;
        }
        try {
            Map<String, Object> tokenInfoMap = (Map<String, Object>) customProperties.get(Constants.ACCESS_TOKEN_INFO);
            if (tokenInfoMap == null) {
                return 0;
            }
            if (!tokenInfoMap.containsKey("exp")) {
                return 0;
            }
            return (long) tokenInfoMap.get("exp");
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to obtain expiration time from customer properties: " + e);
            }
            return 0;
        }
    }

    Subject recreateSubject(Subject cachedSubject) {
        Subject newSubject = new Subject();
        if (cachedSubject != null) {
            newSubject.getPrincipals().addAll(cachedSubject.getPrincipals());
            newSubject.getPublicCredentials().addAll(cachedSubject.getPublicCredentials());
            newSubject.getPrivateCredentials().addAll(cachedSubject.getPrivateCredentials());
        }
        return newSubject;
    }

}
