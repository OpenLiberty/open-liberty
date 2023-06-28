/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.internal;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.structures.SingleTableCache;
import com.ibm.ws.security.openidconnect.client.jose4j.OidcTokenImpl;
import com.ibm.ws.security.openidconnect.clients.common.Constants;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class AccessTokenCacheHelper {

    private static final TraceComponent tc = Tr.register(AccessTokenCacheHelper.class);

    public AccessTokenCacheKey getCacheKey(@Sensitive String accessToken, String configId) {
        return new AccessTokenCacheKey(accessToken, configId);
    }

    public ProviderAuthenticationResult getCachedTokenAuthenticationResult(OidcClientConfig clientConfig, String token) {
        if (!clientConfig.getAccessTokenCacheEnabled()) {
            return null;
        }
        SingleTableCache cache = clientConfig.getCache();
        AccessTokenCacheKey cacheKey = getCacheKey(token, clientConfig.getId());
        AccessTokenCacheValue cacheEntry = (AccessTokenCacheValue) cache.get(cacheKey);
        if (cacheEntry == null) {
            return null;
        }
        ProviderAuthenticationResult result = cacheEntry.getResult();
        if (isTokenInCachedResultExpired(result, clientConfig)) {
            return null;
        }
        String uniqueID = cacheEntry.getUniqueID();
        Hashtable<String, Object> customProperties = result.getCustomProperties();
        if (customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID) == null && uniqueID != null) {
            customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
        }
        Subject newSubject = recreateSubject(result.getSubject());
        return new ProviderAuthenticationResult(result.getStatus(), result.getHttpStatusCode(), result.getUserName(), newSubject, customProperties, result.getRedirectUrl());
    }

    public void cacheTokenAuthenticationResult(OidcClientConfig clientConfig, String token, ProviderAuthenticationResult result) {
        if (clientConfig.getAccessTokenCacheEnabled()) {
            SingleTableCache cache = clientConfig.getCache();
            Hashtable<String, Object> customProperties = result.getCustomProperties();
            String uniqueID = null;
            if (customProperties != null) {
                uniqueID = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID);
            }
            AccessTokenCacheKey cacheKey = getCacheKey(token, clientConfig.getId());
            cache.put(cacheKey, new AccessTokenCacheValue(uniqueID, result), clientConfig.getClockSkew());
        }
    }

    boolean isTokenInCachedResultExpired(ProviderAuthenticationResult cachedResult, OidcClientConfig clientConfig) {
        Hashtable<String, Object> customProperties = cachedResult.getCustomProperties();
        if (customProperties == null || customProperties.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Custom properties were null or empty");
            }
            return true;
        }
        if (!doesPropertyExistInAccessTokenInfo("exp", customProperties)) {
            return false;
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
    boolean doesPropertyExistInAccessTokenInfo(String propertyName, Hashtable<String, Object> customProperties) {
        if (customProperties == null || customProperties.isEmpty()) {
            return false;
        }
        if (!customProperties.containsKey(Constants.ACCESS_TOKEN_INFO)) {
            return false;
        }
        try {
            Map<String, Object> tokenInfoMap = (Map<String, Object>) customProperties.get(Constants.ACCESS_TOKEN_INFO);
            if (tokenInfoMap == null) {
                return false;
            }
            if (!tokenInfoMap.containsKey(propertyName)) {
                return false;
            }
            return tokenInfoMap.get(propertyName) != null;
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to check " + propertyName + " from customer properties: " + e);
            }
            return false;
        }
    }

    @FFDCIgnore(Exception.class)
    @SuppressWarnings("unchecked")
    long getTokenExpirationFromCustomProperties(Hashtable<String, Object> customProperties) {
        try {
            Map<String, Object> tokenInfoMap = (Map<String, Object>) customProperties.get(Constants.ACCESS_TOKEN_INFO);
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
        if (cachedSubject == null) {
            return newSubject;
        }
        Set<Object> newPRCreds = newSubject.getPrivateCredentials();
        newPRCreds.addAll(cachedSubject.getPrivateCredentials(OidcTokenImpl.class));
        newPRCreds.addAll(cachedSubject.getPrivateCredentials(Hashtable.class));
        return newSubject;
    }

}
