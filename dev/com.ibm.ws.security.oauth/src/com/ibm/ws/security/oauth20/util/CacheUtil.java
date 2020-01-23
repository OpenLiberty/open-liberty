/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;
import com.ibm.ws.security.oauth20.web.EndpointUtils;

/**
 *
 */
public class CacheUtil {
    private OAuth20TokenCache cache;

    public CacheUtil(OAuth20TokenCache tokenCache) {
        cache = tokenCache;
    }

    public CacheUtil() {

    }

    public String computeHash(String lookupKeyParam, String encoding) {
        String lookupKey = lookupKeyParam;
        boolean isPlainEncoding = OAuth20Constants.PLAIN_ENCODING.equals(encoding);

        if (isPlainEncoding) { // must be app-password or app-token
            lookupKey = EndpointUtils.computeTokenHash(lookupKey);
        } else {
            lookupKey = EndpointUtils.computeTokenHash(lookupKey, encoding);
        }

        return lookupKey;
    }

    public boolean shouldHash(OAuth20Token entry, String encoding) {
        boolean isAppPasswordOrAppTokenGT = (OAuth20Constants.GRANT_TYPE_APP_PASSWORD.equals(entry.getGrantType())) || (OAuth20Constants.GRANT_TYPE_APP_TOKEN.equals(entry.getGrantType()));
        boolean isAuthorizationGrantTypeAndCodeSubType = (OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT.equals(entry.getType()) && OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE.equals(entry.getSubType()));
        boolean isPlainEncoding = OAuth20Constants.PLAIN_ENCODING.equals(encoding);

        if (!isAuthorizationGrantTypeAndCodeSubType && (!isPlainEncoding || isAppPasswordOrAppTokenGT)) {
            return true;
        }
        return false;
    }

    public OAuth20Token getRefreshToken(OAuth20Token token) {
        String refreshLookupKey = null;
        OAuth20Token refresh = null;

        refreshLookupKey = getRefreshTokenId(token);
        /*
         * if (token != null) {
         * if (OAuth20Constants.TOKENTYPE_ACCESS_TOKEN.equals(token.getType())) {
         * refreshLookupKey = getLookupKey(token);
         * } else {
         * OAuth20Token access = getAccessToken(token);
         * if (access != null) {
         * refreshLookupKey = getLookupKey(access);
         * }
         * }
         * }
         */
        if (refreshLookupKey != null) {
            refresh = cache.get(refreshLookupKey);
        }
        return refresh;
    }

    public String getRefreshTokenId(OAuth20Token token) {
        String refreshLookupKey = null;
        if (token != null) {
            if (OAuth20Constants.TOKENTYPE_ACCESS_TOKEN.equals(token.getType())) {
                refreshLookupKey = getLookupKey(token);
            } else {
                OAuth20Token access = getAccessToken(token);
                if (access != null) {
                    refreshLookupKey = getLookupKey(access);
                }
            }
        }
        return refreshLookupKey;
    }

    public String getAccessTokenId(OAuth20Token idtoken) {
        String accessTokenId = null;
        if (idtoken != null) {
            if (OIDCConstants.TOKENTYPE_ID_TOKEN.equals(idtoken.getType())) {
                accessTokenId = ((OAuth20TokenImpl) idtoken).getAccessTokenKey();
            }
        }
        return accessTokenId;
    }

    private OAuth20Token getAccessToken(OAuth20Token idtoken) {
        OAuth20Token access = null;
        if (OIDCConstants.TOKENTYPE_ID_TOKEN.equals(idtoken.getType())) {
            access = cache.get(((OAuth20TokenImpl) idtoken).getAccessTokenKey());
        }
        return access;

    }

    private String getLookupKey(OAuth20Token token) {
        return ((OAuth20TokenImpl) token).getRefreshTokenKey();
    }

}
