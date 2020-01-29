/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.plugins;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.util.BoundedCache;
import com.ibm.ws.security.oauth20.util.MessageDigestUtil;
import com.ibm.ws.security.oauth20.web.EndpointUtils;

/**
 * This class was imported from tWAS to make only those changes necessary to
 * run OAuth on Liberty. The mission was not to refactor, restructure, or
 * generally cleanup the code.
 */
public class BaseCache implements OAuth20EnhancedTokenCache {

    Thread _cleanupThread;
    static Object _lock = new Object();
    Map<String, CacheEntry> _cache;
    static Map<String, List<CacheEntry>> _indexOnUsername;
    final String MYCLASS = BaseCache.class.getName();
    Logger _log = Logger.getLogger(MYCLASS);
    private int tokenStoreSize;
    private String accessTokenEncoding = OAuth20Constants.PLAIN_ENCODING;
    private int accessTokenLength;

    public BaseCache() {
    }

    public BaseCache(int tokenStoreSize) {
        this.tokenStoreSize = tokenStoreSize;
    }

    public BaseCache(int tokenStoreSize, String accessTokenEncoding, int accessTokenLength) {
        this.tokenStoreSize = tokenStoreSize;
        this.accessTokenEncoding = accessTokenEncoding;
        this.accessTokenLength = accessTokenLength;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        getCache(tokenStoreSize);
        getIndexOnUsernameCache();
        startCleanupThread();
    }

    @Override
    public void init(OAuthComponentConfiguration config) {
        getCache(config.getConfigPropertyIntValue(Constants.TOKEN_STORE_SIZE));

        getIndexOnUsernameCache();

        startCleanupThread();
    }

    private synchronized void getCache(int storeSize) {
        if (_cache == null) {
            storeSize = storeSize <= 0 ? 10 : storeSize;
            _cache = Collections.synchronizedMap(new BoundedCache<String, CacheEntry>(storeSize));
        }
    }

    private static synchronized void getIndexOnUsernameCache() {
        if (_indexOnUsername == null) {
            _indexOnUsername = Collections.synchronizedMap(new BoundedCache<String, List<CacheEntry>>(2000));
        }
    }

    @Override
    public OAuth20Token getByHash(String hash) {
        OAuth20Token result = null;
        synchronized (_lock) {
            CacheEntry ce = _cache.get(hash);
            if (ce != null) {
                if (!ce.isExpired()) {
                    result = ce._token;
                } else {
                    // may as well remove expired entry now
                    removeByHash(hash);
                }
            }
        }
        return result;
    }

    @Override
    public void removeByHash(String hash) {
        synchronized (_lock) {
            CacheEntry ce = _cache.remove(hash);
            List<CacheEntry> entries = null;
            if (ce != null) {
                String username = ce._token.getUsername();
                entries = _indexOnUsername.get(username);
                if (entries == null) {
                    _indexOnUsername.remove(username);
                } else {
                    entries.remove(ce);
                    if (entries.size() == 0) {
                        _indexOnUsername.remove(username);
                    }
                }
            }
        }
    }

    @Override
    public void add(String lookupKey, OAuth20Token entry, int lifetime) {
        String hash = lookupKey;
        boolean isAppPasswordOrAppTokenGT = (OAuth20Constants.GRANT_TYPE_APP_PASSWORD.equals(entry.getGrantType())) || (OAuth20Constants.GRANT_TYPE_APP_TOKEN.equals(entry.getGrantType()));
        boolean isAuthorizationGrantTypeAndCodeSubType = (OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT.equals(entry.getType()) && OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE.equals(entry.getSubType()));
        if (!isAuthorizationGrantTypeAndCodeSubType && (!OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding) || isAppPasswordOrAppTokenGT)) {
            if (OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding)) { // must be app-password or app-token
                hash = EndpointUtils.computeTokenHash(lookupKey);
            } else {
                hash = EndpointUtils.computeTokenHash(lookupKey, this.accessTokenEncoding);
            }

        } else {
            hash = MessageDigestUtil.getDigest(lookupKey);
        }
        // TODO : see if hash is valid before adding
        synchronized (_lock) {
            CacheEntry ce = new CacheEntry(entry, lifetime);
            _cache.put(hash, ce);
            List<CacheEntry> entries = null;
            String username = entry.getUsername();
            entries = _indexOnUsername.get(username);
            if (entries == null) {
                entries = new ArrayList<CacheEntry>();
                _indexOnUsername.put(username, entries);
            }
            entries.add(ce);
        }
    }

    void startCleanupThread() {
        synchronized (BaseCache.class) {
            // Yes this is a hack until we have more time to do it right with
            // a thread pool
            if (_cleanupThread != null) {
                ((CleanupThread) _cleanupThread).stopCleanup();
                _cleanupThread = null;
            }
            final BaseCache self = this;
            _cleanupThread = (CleanupThread) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    return new CleanupThread(self);
                }
            });
            _cleanupThread.start();
        }
    }

    @Override
    public void stopCleanupThread() {
        if (_cleanupThread != null) {
            ((CleanupThread) _cleanupThread).stopCleanup();
        }
    }

    @Trivial
    class CleanupThread extends Thread {
        final String MYCLASS = CleanupThread.class.getName();
        final static int CLEANUP_INTERVAL_SECONDS = 120;
        Logger _log = Logger.getLogger(MYCLASS);
        private boolean stopped = false;

        BaseCache _me;

        public CleanupThread(BaseCache me) {
            _me = me;
        }

        @Override
        public void run() {
            String methodName = "run";
            _log.entering(MYCLASS, methodName);
            boolean finestLoggable = _log.isLoggable(Level.FINEST);
            try {
                while (!stopped) {
                    Date now = new Date();
                    long nowTime = now.getTime();

                    if (finestLoggable) {
                        _log.logp(Level.FINEST, MYCLASS, methodName,
                                "About to delete all expired tokens");
                    }

                    synchronized (BaseCache._lock) {
                        /*
                         * Not efficient, but this is only a test class. Two
                         * phases to the remove are used to avoid
                         * ConcurrentModificationException.
                         */
                        Set<String> keys = _me._cache.keySet();
                        Set<String> keysToRemove = new HashSet<String>();
                        synchronized (_me._cache) {
                            // find and record keys to remove
                            for (Iterator<String> i = keys.iterator(); i.hasNext();) {
                                String key = i.next();
                                CacheEntry ce = _me._cache.get(key);
                                if (ce.isExpired()) {
                                    keysToRemove.add(key);
                                }
                            }
                            // now remove them
                            for (Iterator<String> i = keysToRemove.iterator(); i.hasNext();) {
                                String key = i.next();
                                // _me.remove(key);
                                _me.removeByHash(key);
                            }
                        }
                    }

                    sleep(CLEANUP_INTERVAL_SECONDS * 1000);
                }
            } catch (InterruptedException e) {
                if (finestLoggable) {
                    _log.logp(Level.FINEST, MYCLASS, methodName,
                            "Cleanup thread was interrupted");
                }
            } finally {
                _log.exiting(MYCLASS, methodName);
            }
        }

        // Yes this is a hack until we have more time to do it right with
        // a thread pool
        public void stopCleanup() {
            if (_log.isLoggable(Level.FINEST)) {
                _log.logp(Level.FINEST, MYCLASS, "stopCleanup", "stopping cleanup thread");
            }
            stopped = true;
        }
    }

    @Override
    public Collection<OAuth20Token> getAllUserTokens(String username) {
        List<CacheEntry> tokens = _indexOnUsername.get(username);
        List<OAuth20Token> retVal = new ArrayList<OAuth20Token>();
        if (tokens != null) {
            synchronized (_lock) {
                for (CacheEntry ce : tokens) {
                    OAuth20Token t = ce._token;
                    if (t != null && username.equals(t.getUsername()))
                        retVal.add(t);
                }
            }
        }
        return retVal;
    }

    // Used for testing, performance is not important
    @Override
    public Collection<OAuth20Token> getAll() {
        ArrayList<OAuth20Token> result = new ArrayList<OAuth20Token>();
        for (CacheEntry entry : _cache.values()) {
            result.add(entry._token);
        }
        return result;
    }

    @Override
    public OAuth20Token get(String lookupKey) {
        String hash = lookupKey;
        if (!PasswordUtil.isHashed(hash)) {
            if (!OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding) || (lookupKey.length() == (this.accessTokenLength + 2))) {
                if (OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding)) { // must be app-passwords or app-tokens
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

    @Override
    public void remove(String lookupKey) {
        String hash = lookupKey;
        if (!PasswordUtil.isHashed(hash)) {
            if (!OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding) || (lookupKey.length() == (this.accessTokenLength + 2))) {
                if (OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding)) { // must be app-passwords or app-tokens
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

    @Override
    public int getNumTokens(String username, String client) {
        int result = -1;
        Collection<OAuth20Token> alltokens = getAllUserTokens(username);
        if (alltokens != null) {
            result = 0;
            for (OAuth20Token token : alltokens) {
                if (client.equals(token.getClientId())) {
                    result++;
                }
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void addByHash(String hash, OAuth20Token entry, int lifetime) {

        // TODO : see if hash is valid before adding
        synchronized (_lock) {
            CacheEntry ce = new CacheEntry(entry, lifetime);
            _cache.put(hash, ce);
            List<CacheEntry> entries = null;
            String username = entry.getUsername();
            entries = _indexOnUsername.get(username);
            if (entries == null) {
                entries = new ArrayList<CacheEntry>();
                _indexOnUsername.put(username, entries);
            }
            entries.add(ce);
        }

    }

    /** {@inheritDoc} */
    @Override
    public Collection<OAuth20Token> getMatchingTokens(String username, String client, String tokenType) {

        Collection<OAuth20Token> tokens = getUserAndClientTokens(username, client);
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
        Collection<OAuth20Token> tokens = getAllUserTokens(username);
        if (tokens != null && !tokens.isEmpty()) {
            return getTokensMatchingClientId(tokens, client);
        }
        return Collections.emptyList();
    }

}
