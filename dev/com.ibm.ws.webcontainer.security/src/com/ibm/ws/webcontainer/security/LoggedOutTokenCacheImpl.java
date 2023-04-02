/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.util.Map;
import java.util.Properties;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.cache.DistributedMap;
import com.ibm.websphere.cache.EntryInfo;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.security.jaas.common.callback.AuthenticationHelper;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.cache.DistributedObjectCacheFactory;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.ltpa.Token;

/**
 * Methods to create, add, and get logged out tokens from the LoggedOutTokenMap DistributedMap
 */
public class LoggedOutTokenCacheImpl implements LoggedOutTokenCache {
    private static final TraceComponent tc = Tr.register(LoggedOutTokenCacheImpl.class);

    private static final AtomicServiceReference<TokenManager> tokenManager = new AtomicServiceReference<TokenManager>("tokenManager");

    private final InMemoryLoggedOutTokenCache inMemoryCookieCache = new InMemoryLoggedOutTokenCache();

    protected void setTokenManager(ServiceReference<TokenManager> ref) {
        tokenManager.setReference(ref);
    }

    protected void unsetTokenManager(ServiceReference<TokenManager> ref) {
        tokenManager.setReference(ref);
    }

    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        tokenManager.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        tokenManager.deactivate(cc);
    }

    static final class Singleton {
        private static final LoggedOutTokenCache instance = new LoggedOutTokenCacheImpl();
    }

    @Trivial
    public static LoggedOutTokenCache getInstance() {
        return Singleton.instance;
    }

    @Override
    public boolean contains(Object key) {
        String keyStr = (String) key;

        LoggedOutCookieCache jCacheCookieCache = LoggedOutCookieCacheHelper.getLoggedOutCookieCacheService();
        if (jCacheCookieCache != null) {
            return jCacheCookieCache.contains(keyStr);
        } else {
            return inMemoryCookieCache.contains(keyStr);
        }
    }

    @Override
    public void put(Object key, Object value) {
        String keyStr = (String) key;

        /*
         * Determine if the token is still valid. If it is not, don't cache it.
         */
        TokenManager tm = LoggedOutTokenCacheImpl.tokenManager.getService();
        int timeOut = -1;
        try {
            byte[] tokenBytes = AuthenticationHelper.copyCredToken(Base64Coder.base64DecodeString(keyStr));
            Token token = tm.recreateTokenFromBytes(tokenBytes);
            if (token != null) {
                long tokenExp = token.getExpiration();
                long calcTimeOut = tokenExp - System.currentTimeMillis();
                timeOut = (int) calcTimeOut / 1000;
                String userName = token.getAttributes("u")[0];
                if (userName != null)
                    value = userName;
            }

        } catch (InvalidTokenException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Token is not valid so do not cache it " + e.getMessage());
            }
            return;
        } catch (TokenExpiredException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Token is expired so do not cache it " + e.getMessage());
            }
            return;
        }

        /*
         * Choose between using the in-memory logged out cookie cache, or the JCache logged out cookie cache.
         */
        LoggedOutCookieCache jCacheCookieCache = LoggedOutCookieCacheHelper.getLoggedOutCookieCacheService();
        if (jCacheCookieCache != null) {
            jCacheCookieCache.put(keyStr, value);
        } else {
            inMemoryCookieCache.put(keyStr, value, timeOut);
        }
    }

    @Override
    public boolean shouldTrackTokens() {
        /*
         * Indicate we should always track tokens if LoggedOutCookieCacheService is available.
         */
        return LoggedOutCookieCacheHelper.getLoggedOutCookieCacheService() != null;
    }

    /**
     * In-memory cache to store logged out tokens.
     */
    private class InMemoryLoggedOutTokenCache {
        private DistributedMap dmns = null;

        /*
         * Look up the key from the DistributedMap if it exists
         */
        public boolean contains(Object key) {
            //check to see if the token is in the distributed map
            if (dmns != null) {
                return dmns.containsKey(key);
            }

            // if dmns does not exist there are no entries in the DistributedMap
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The LoggedOutTokenMap DistributedMap does not exist.");
            }
            return false;
        }

        /*
         * Add the token to the DistributedMap
         *
         * key is the token string
         * value is the subject
         * timeToLive is the about of time left before the token expires, to become the expiring time of the distributed map entry
         */
        public Object put(Object key, Object value, int timeToLive) {

            DistributedMap map = getDMLoggedOutTokenMap();

            if (map != null) {
                Object dist_object = map.put(key, value, 1, timeToLive, EntryInfo.SHARED_PUSH, null);
                return dist_object;
            }
            return null;
        }

        /*
         * Get the LoggedOutTokenMap
         */
        private DistributedMap getDMLoggedOutTokenMap() {

            if (dmns == null) {
                dmns = getDistributedMap("LoggedOutTokenMap");
            }

            return dmns;

        }

        /*
         * Creates the LoggedOutTokeMap if it does not exist.
         */
        private DistributedMap getDistributedMap(String mapName) {
            DistributedMap dm = null;

            dm = DistributedObjectCacheFactory.getMap(mapName, new Properties());

            return dm;
        }
    }
}
