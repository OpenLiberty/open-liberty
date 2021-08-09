/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.custom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.util.JSONUtil;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.oauth20.store.OAuthStore;
import com.ibm.websphere.security.oauth20.store.OAuthStoreException;
import com.ibm.websphere.security.oauth20.store.OAuthToken;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;
import com.ibm.ws.security.oauth20.util.CacheUtil;
import com.ibm.ws.security.oauth20.util.MessageDigestUtil;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.web.EndpointUtils;

/**
 * Token store that uses customer provided OAuthStore to manage tokens.
 */
public class OauthTokenStore implements OAuth20EnhancedTokenCache {

    private static TraceComponent tc = Tr.register(OauthTokenStore.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private final String componentId;
    private final OAuthStore oauthStore;
    private Timer timer;
    private long cleanupIntervalInMilliseconds = 0;
    private String accessTokenEncoding = OAuth20Constants.PLAIN_ENCODING;
    int accessTokenLength;

    /**
     * @param componentId
     * @param oauthStore
     * @param cleanupIntervalInMilliseconds
     */
    public OauthTokenStore(String componentId, OAuthStore oauthStore, long cleanupIntervalInMilliseconds) {
        this.componentId = componentId;
        this.oauthStore = oauthStore;
        this.cleanupIntervalInMilliseconds = cleanupIntervalInMilliseconds;
    }
    
    /**
     * @param componentId
     * @param oauthStore
     * @param cleanupIntervalInMilliseconds
     * @param accessTokenEncoding
     * @param accessTokenLength
     */
    public OauthTokenStore(String componentId, OAuthStore oauthStore, long cleanupIntervalInMilliseconds, String accessTokenEncoding, int accessTokenLength) {
        this.componentId = componentId;
        this.oauthStore = oauthStore;
        this.cleanupIntervalInMilliseconds = cleanupIntervalInMilliseconds;
        this.accessTokenEncoding = accessTokenEncoding;
        this.accessTokenLength = accessTokenLength;
    }

    /** {@inheritDoc} */
    @Override
    public void init(OAuthComponentConfiguration config) {
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        scheduleCleanupTask();
    }

    private void scheduleCleanupTask() {
        if (cleanupIntervalInMilliseconds > 0) {
            CleanupTask cleanupTask = new CleanupTask();
            timer = new Timer(true);
            long period = cleanupIntervalInMilliseconds;
            long delay = period;
            timer.schedule(cleanupTask, delay, period);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void add(@Sensitive String lookupKeyParam, OAuth20Token entry, int lifetime) {
        String lookupKey = lookupKeyParam;
        boolean shouldHash = false;
        
        CacheUtil cacheUtil = new CacheUtil();
        if (cacheUtil.shouldHash(entry, this.accessTokenEncoding)) {
            shouldHash = true;
            lookupKey = cacheUtil.computeHash(lookupKeyParam, this.accessTokenEncoding);
        } else {
            lookupKey = MessageDigestUtil.getDigest(lookupKeyParam);
        }
        // TODO: Determine if a local cache is needed.
        try {
            oauthStore.create(getOauthToken(lookupKey, entry, false, shouldHash));
        } catch (OAuthStoreException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "ERROR_PERFORMING_OAUTH_STORE_CREATE_TOKEN", lookupKey, e.getLocalizedMessage());
            }
        }
    }

    private OAuthToken getOauthToken(String lookupKey, OAuth20Token token, boolean alreadyHashed, boolean shouldHash) {
        String uniqueId = token.getId();
        String componentId = token.getComponentId();
        String type = token.getType();
        String subType = token.getSubType();
        long createdAt = token.getCreatedAt();
        int lifetimeInSeconds = token.getLifetimeSeconds();

        String tokenString = token.getTokenString();
        CacheUtil cacheUtil = new CacheUtil();
        if (!alreadyHashed) {
            if (shouldHash) {
                uniqueId = cacheUtil.computeHash(uniqueId, this.accessTokenEncoding);
                tokenString = cacheUtil.computeHash(tokenString, this.accessTokenEncoding);
            } else {
                tokenString = PasswordUtil.passwordEncode(tokenString);
            }
        }        

        String clientId = token.getClientId();
        String username = token.getUsername();
        long expires = 0;
        if (token.getLifetimeSeconds() > 0) {
            expires = token.getCreatedAt() + (1000L * token.getLifetimeSeconds());
        }

        StringBuffer scopes = new StringBuffer();
        String[] ascopes = token.getScope();
        if (ascopes != null && ascopes.length > 0) {
            for (int i = 0; i < ascopes.length; i++) {
                scopes.append(ascopes[i].trim());
                if (i < (ascopes.length - 1)) {
                    scopes.append(" ");
                }
            }
        }
        String scope = scopes.toString();
        String redirectUri = token.getRedirectUri();
        String stateId = token.getStateId();

        JsonObject extendedFields = JSONUtil.getJsonObject(token.getExtensionProperties());
        if (extendedFields == null) {
            extendedFields = new JsonObject();
        }
        extendedFields.addProperty(OAuth20Constants.GRANT_TYPE, token.getGrantType());

        String refreshId = null, accessId = null;
        if (OAuth20Constants.TOKENTYPE_ACCESS_TOKEN.equals(token.getType())) {
            if ((refreshId = ((OAuth20TokenImpl) token).getRefreshTokenKey()) != null) {
                extendedFields.addProperty(OAuth20Constants.REFRESH_TOKEN_ID, refreshId);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Access Token is added to cache , refresh token id " + refreshId);
                }
            }
        } else if (OIDCConstants.TOKENTYPE_ID_TOKEN.equals(token.getType())) {
            if ((accessId = ((OAuth20TokenImpl) token).getAccessTokenKey()) != null) {
                extendedFields.addProperty(OAuth20Constants.ACCESS_TOKEN_ID, accessId);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "ID Token is added to cache , access token id " + accessId);
                }
            }
        }

        String tokenProperties = extendedFields.toString();

        return new OAuthToken(lookupKey, uniqueId, componentId, type, subType, createdAt, lifetimeInSeconds, expires, tokenString, clientId, username, scope, redirectUri, stateId, tokenProperties);
    }

    /** {@inheritDoc} */
    @Override
    public OAuth20Token get(@Sensitive String lookupKey) {
        String hash = lookupKey;
        if (!PasswordUtil.isHashed(hash)) {
            if (!OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding) || (lookupKey.length() == (this.accessTokenLength + 2))) { // app-password or app-token
                if (OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding)) { // must be app-password or app-token
                    hash = EndpointUtils.computeTokenHash(lookupKey);
                } else {
                    hash = EndpointUtils.computeTokenHash(lookupKey, this.accessTokenEncoding);
                }
            } else {
                hash = MessageDigestUtil.getDigest(lookupKey);
            }
        }

        return getByHash(hash);
    }

    /** {@inheritDoc} */
    @Override
    public OAuth20Token getByHash(String hash) {
        OAuth20Token token = null;
        OAuthToken oauthToken;

        try {
            oauthToken = oauthStore.readToken(componentId, hash);
            if (oauthToken != null) {
                token = createToken(oauthToken);
            }
        } catch (OAuthStoreException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "ERROR_PERFORMING_OAUTH_STORE_READ_TOKEN", hash, e.getLocalizedMessage());
            }
        }
        if (token != null) {
            token.setLastAccess();
        }
        return token;
    }

    private OAuth20Token createToken(OAuthToken oauthToken) {
        OAuth20Token token = null;
        String uniqueId = oauthToken.getUniqueId();
        String componentId = oauthToken.getProviderId();
        String type = oauthToken.getType();
        String subType = oauthToken.getSubType();
        long createdAt = oauthToken.getCreatedAt();
        int lifetime = oauthToken.getLifetimeInSeconds();
        long expires = oauthToken.getExpires();
        String tokenString = oauthToken.getTokenString();// PasswordUtil.passwordDecode(oauthToken.getTokenString());
        String clientId = oauthToken.getClientId();
        String username = oauthToken.getUsername();

        String scope = oauthToken.getScope();
        String[] scopes = null;
        if (scope != null) {
            scopes = scope.split(" ");
        }

        String redirectUri = oauthToken.getRedirectUri();
        String stateId = oauthToken.getStateId();

        JsonObject extendedFields = new JsonParser().parse(oauthToken.getTokenProperties()).getAsJsonObject();

        String grantType = null;
        // TODO: Determine if these will be used with the custom store.
        String refreshId = null;
        String accessId = null;

        if (extendedFields != null) {
            grantType = extendedFields.get(OAuth20Constants.GRANT_TYPE).getAsString();
            if (OAuth20Constants.TOKENTYPE_ACCESS_TOKEN.equals(type)) {
                if (extendedFields.get(OAuth20Constants.REFRESH_TOKEN_ID) != null) {
                    refreshId = extendedFields.get(OAuth20Constants.REFRESH_TOKEN_ID).getAsString();
                }
            } else if (OIDCConstants.TOKENTYPE_ID_TOKEN.equals(type)) {
                if (extendedFields.get(OAuth20Constants.ACCESS_TOKEN_ID) != null) {
                    accessId = extendedFields.get(OAuth20Constants.ACCESS_TOKEN_ID).getAsString();
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Type is " + type + " but " + OAuth20Constants.ACCESS_TOKEN_ID + " from extended fields was null. " + extendedFields);
                    }
                }
            }
            extendedFields.remove(OAuth20Constants.GRANT_TYPE);
            extendedFields.remove(OAuth20Constants.REFRESH_TOKEN_ID);
            extendedFields.remove(OAuth20Constants.ACCESS_TOKEN_ID);
        }
        boolean isAppPasswordOrAppTokenGT = (OAuth20Constants.GRANT_TYPE_APP_PASSWORD.equals(grantType)) || (OAuth20Constants.GRANT_TYPE_APP_TOKEN.equals(grantType));
        boolean isAuthorizationGrantTypeAndCodeSubType = (OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT.equals(type) && OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE.equals(subType));
        if ((isAuthorizationGrantTypeAndCodeSubType) || (!isAppPasswordOrAppTokenGT && (OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding)))) {
            // decode token string
            tokenString = PasswordUtil.passwordDecode(tokenString); 
        }

        Map<String, String[]> extensionProperties = JSONUtil.jsonObjectToStringsMap(extendedFields);

        Date now = new Date();
        if (now.getTime() < expires) {
            // not yet expired
            token = new OAuth20TokenImpl(uniqueId, componentId, type, subType,
                    createdAt, lifetime, tokenString, clientId, username,
                    scopes, redirectUri, stateId, extensionProperties, grantType);
            if (refreshId != null) {
                ((OAuth20TokenImpl) token).setRefreshTokenKey(refreshId);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Got the Access Token from cache, refresh token id = " + refreshId);
                }
            } else if (accessId != null) {
                ((OAuth20TokenImpl) token).setAccessTokenKey(accessId);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Got the ID Token from cache, access token id = " + accessId);
                }
            }
        } else {
            try {
                throw new Exception("The OAuth20Token is expired already");
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Internal error ceating token :" + e.getMessage(), e);
            }
        }
        if (token != null) {
            token.setLastAccess();
        }

        return token;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<OAuth20Token> getAllUserTokens(String username) {
        try {
            return createTokens(oauthStore.readAllTokens(componentId, username));
        } catch (OAuthStoreException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "ERROR_PERFORMING_OAUTH_STORE_READ_ALL_TOKENS", e.getLocalizedMessage());
            }
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<OAuth20Token> getAll() {
        return Collections.emptyList();
    }

    private Collection<OAuth20Token> createTokens(Collection<OAuthToken> oauthTokens) {
        Collection<OAuth20Token> tokens = new ArrayList<OAuth20Token>();

        if (oauthTokens != null) {
            for (OAuthToken oauthToken : oauthTokens) {
                OAuth20Token token = createToken(oauthToken);
                tokens.add(token);
            }
        }

        return tokens;
    }

    /** {@inheritDoc} */
    @Override
    public int getNumTokens(String username, String client) {
        try {
            return oauthStore.countTokens(componentId, username, client);
        } catch (OAuthStoreException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "ERROR_PERFORMING_OAUTH_STORE_COUNT_TOKENS", e.getLocalizedMessage());
            }
        }
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void remove(@Sensitive String lookupKey) {
        String hash = lookupKey;
        if (!PasswordUtil.isHashed(hash)) {
            if (!OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding) || (lookupKey.length() == (this.accessTokenLength + 2))) { // app-password or app-token
                if (OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding)) { // must be app-password or app-token
                    hash = EndpointUtils.computeTokenHash(lookupKey);
                } else {
                    hash = EndpointUtils.computeTokenHash(lookupKey, this.accessTokenEncoding);
                }
            } else {
                hash = MessageDigestUtil.getDigest(lookupKey);
            }
        }

        removeByHash(hash);
    }

    /** {@inheritDoc} */
    @Override
    public void removeByHash(String hash) {
        try {
            oauthStore.deleteToken(componentId, hash);
        } catch (OAuthStoreException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "ERROR_PERFORMING_OAUTH_STORE_DELETE_TOKEN", hash, e.getLocalizedMessage());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stopCleanupThread() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private class CleanupTask extends TimerTask {

        /** {@inheritDoc} */
        @Override
        public void run() {
            try {
                oauthStore.deleteTokens(componentId, new Date().getTime());
            } catch (OAuthStoreException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                    Tr.error(tc, "ERROR_PERFORMING_OAUTH_STORE_DELETE_TOKENS", e.getLocalizedMessage());
                }
            }
        }

    }
    
    /** {@inheritDoc} */
    @Override
    public void addByHash(@Sensitive String hash, OAuth20Token entry, int lifetime) {
        String lookupKey = hash;

        try {
            oauthStore.create(getOauthToken(lookupKey, entry, true, false));
        } catch (OAuthStoreException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "ERROR_PERFORMING_OAUTH_STORE_CREATE_TOKEN", lookupKey, e.getLocalizedMessage());
            }
        }

    }

  /** {@inheritDoc} */
    @Override
    public Collection<OAuth20Token> getMatchingTokens(String username, String client, String tokenType) {
        
        Collection<OAuth20Token> tokens = getUserAndClientTokens(username, client);
        // Collection<OAuth20Token> tokens = createTokens(oauthStore.readAllTokens(componentId, username));
        if (tokens != null && !tokens.isEmpty()) {
            return getTokensMatchingType(tokens, tokenType);

        }

        return Collections.emptyList();
    }
    
    /**
     * @param tokens
     * @param clientId
     * @return
     */
    private static Collection<OAuth20Token> getTokensMatchingClientId(Collection<OAuth20Token> tokens, String clientId) {
        Iterator<OAuth20Token> it = tokens.iterator();
        HashSet<OAuth20Token> matchingTokens = new HashSet<OAuth20Token>();
        while (it.hasNext()) {
            OAuth20Token token = it.next();
            if (clientId.equals(token.getClientId())) {
                matchingTokens.add(token);
            }
        }
        return matchingTokens;
    }

    /**
     * @param tokens
     * @param grantType
     * @return 
     */
    private static Collection<OAuth20Token> getTokensMatchingType(Collection<OAuth20Token> tokens, String stateId) {
        Iterator<OAuth20Token> it = tokens.iterator();
        HashSet<OAuth20Token> matchingTokens = new HashSet<OAuth20Token>();
        while (it.hasNext()) {
            OAuth20Token token = it.next();
            if (token.getStateId().equals(stateId)) {
                matchingTokens.add(token);
            }
        }
        return matchingTokens;

    }

    /** {@inheritDoc} */
    @Override
    public Collection<OAuth20Token> getUserAndClientTokens(String username, String client) {
        try {
            Collection<OAuth20Token> tokens = createTokens(oauthStore.readAllTokens(componentId, username));
            if (tokens != null && !tokens.isEmpty()) {
                return getTokensMatchingClientId(tokens, client);
            }           
        } catch (OAuthStoreException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "ERROR_PERFORMING_OAUTH_STORE_READ_ALL_TOKENS", e.getLocalizedMessage());
            }
        }
        return Collections.emptyList();
    }
}
