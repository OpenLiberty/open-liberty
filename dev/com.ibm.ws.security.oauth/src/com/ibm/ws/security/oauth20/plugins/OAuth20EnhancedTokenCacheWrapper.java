/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins;

import java.util.Collection;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;

public class OAuth20EnhancedTokenCacheWrapper implements OAuth20EnhancedTokenCache {

    private final OAuth20TokenCache tokenCache;

    public OAuth20EnhancedTokenCacheWrapper(OAuth20TokenCache tokenCache) {
        this.tokenCache = tokenCache;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        if (tokenCache instanceof OAuth20EnhancedTokenCache) {
            ((OAuth20EnhancedTokenCache) tokenCache).initialize();
        }
    }

    @Override
    public void init(OAuthComponentConfiguration config) {
        tokenCache.init(config);
    }

    @Override
    public void add(String lookupKey, OAuth20Token entry, int lifetime) {
        tokenCache.add(lookupKey, entry, lifetime);
    }

    @Override
    public void remove(String lookupKey) {
        tokenCache.remove(lookupKey);
    }

    @Override
    public OAuth20Token get(String lookupKey) {
        return tokenCache.get(lookupKey);
    }

    @Override
    public Collection<OAuth20Token> getAllUserTokens(String username) {
        if (tokenCache instanceof OAuth20EnhancedTokenCache) {
            return ((OAuth20EnhancedTokenCache) tokenCache).getAllUserTokens(username);
        } else {
            throw new RuntimeException("getAllUserTokens() is not supported.");
        }
    }

    @Override
    public Collection<OAuth20Token> getAll() {
        if (tokenCache instanceof OAuth20EnhancedTokenCache) {
            return ((OAuth20EnhancedTokenCache) tokenCache).getAll();
        } else {
            throw new RuntimeException("getAll() is not supported.");
        }
    }

    @Override
    public OAuth20Token getByHash(String hash) {
        OAuth20Token result = null;
        if (tokenCache instanceof OAuth20EnhancedTokenCache) {
            result = ((OAuth20EnhancedTokenCache) tokenCache).getByHash(hash);
            if (result != null) {
                result.setLastAccess();
            }
            return result;
        } else {
            throw new RuntimeException("getByHash() is not supported.");
        }
    }

    @Override
    public void removeByHash(String hash) {
        if (tokenCache instanceof OAuth20EnhancedTokenCache) {
            ((OAuth20EnhancedTokenCache) tokenCache).removeByHash(hash);
        } else {
            throw new RuntimeException("removeByHash() is not supported.");
        }
    }

    @Override
    public int getNumTokens(String username, String client) {
        if (tokenCache instanceof OAuth20EnhancedTokenCache) {
            return ((OAuth20EnhancedTokenCache) tokenCache).getNumTokens(username, client);
        } else {
            throw new RuntimeException("getNumTokens() is not supported.");
        }
    }

    @Override
    public void stopCleanupThread() {
        if (tokenCache instanceof OAuth20EnhancedTokenCache) {
            ((OAuth20EnhancedTokenCache) tokenCache).stopCleanupThread();
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void addByHash(String hash, OAuth20Token entry, int lifetime) {
        if (tokenCache instanceof OAuth20EnhancedTokenCache) {
            ((OAuth20EnhancedTokenCache) tokenCache).addByHash(hash, entry, lifetime);
        } else {
            throw new RuntimeException("addByHash() is not supported.");
        }
    }
    /** {@inheritDoc} */
    @Override
    public Collection<OAuth20Token> getMatchingTokens(String username, String client, String tokenType) {
        if (tokenCache instanceof OAuth20EnhancedTokenCache) {
            return ((OAuth20EnhancedTokenCache) tokenCache).getMatchingTokens(username, client, tokenType);
        } else {
            throw new RuntimeException("getMatchingTokens() is not supported.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Collection<OAuth20Token> getUserAndClientTokens(String username, String client) {
        if (tokenCache instanceof OAuth20EnhancedTokenCache) {
            return ((OAuth20EnhancedTokenCache) tokenCache).getUserAndClientTokens(username, client);
        } else {
            throw new RuntimeException("getUserAndClientTokens() is not supported.");
        }
    }
}
