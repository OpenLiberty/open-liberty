/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.test.base;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;

public class BaseCache implements OAuth20TokenCache {

    static Thread _cleanupThread;
    Object _lock;
    Map<String, CacheEntry> _cache;

    public BaseCache() {

    }

    @Override
    public void init(OAuthComponentConfiguration config) {
        _lock = new Object();
        _cache = new HashMap<String, CacheEntry>();
        // start the cleanup thread
        startCleanupThread();
    }

    @Override
    public OAuth20Token get(String lookupKey) {
        OAuth20Token result = null;
        synchronized (_lock) {
            CacheEntry ce = _cache.get(lookupKey);
            if (ce != null) {
                if (!ce.isExpired()) {
                    result = ce._token;
                } else {
                    // may as well remove expired entry now
                    _cache.remove(lookupKey);
                }
            }
        }
        return result;
    }

    @Override
    public void remove(String lookupKey) {
        synchronized (_lock) {
            _cache.remove(lookupKey);
        }
    }

    @Override
    public void add(String lookupKey, OAuth20Token entry, int lifetime) {
        synchronized (_lock) {
            CacheEntry ce = new CacheEntry(entry, lifetime);
            _cache.put(lookupKey, ce);
        }
    }

    void startCleanupThread() {
        synchronized (this.getClass()) {
            if (_cleanupThread == null) {
                _cleanupThread = new CleanupThread(this);
                _cleanupThread.start();
            }
        }
    }

    class CacheEntry {
        long _expiryTime;
        OAuth20Token _token;

        CacheEntry(OAuth20Token token, int lifetime) {
            _token = token;
            _expiryTime = (new Date()).getTime() + (lifetime * 1000L);
        }

        boolean isExpired() {
            Date now = new Date();
            long nowTime = now.getTime();
            return nowTime >= _expiryTime;
        }
    }

    class CleanupThread extends Thread {
        final String MYCLASS = CleanupThread.class.getName();
        final static int CLEANUP_INTERVAL_SECONDS = 120;
        Logger _log = Logger.getLogger(MYCLASS);

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
                while (true) {
                    Date now = new Date();
                    long nowTime = now.getTime();

                    if (finestLoggable) {
                        _log.logp(Level.FINEST, MYCLASS, methodName,
                                  "About to delete all tokens with expiry <= "
                                                  + nowTime);
                    }

                    synchronized (_me._lock) {
                        /*
                         * Not efficient, but this is only a test class. Two
                         * phases to the remove are used to avoid
                         * ConcurrentModificationException.
                         */
                        Set<String> keys = _me._cache.keySet();
                        Set<String> keysToRemove = new HashSet<String>();

                        // find and record keys to remove
                        for (Iterator<String> i = keys.iterator(); i.hasNext();) {
                            String key = i.next();
                            CacheEntry ce = _me._cache.get(key);
                            if (ce.isExpired()) {
                                keysToRemove.add(key);
                            }
                        }

                        // now remove them
                        for (Iterator<String> i = keysToRemove.iterator(); i
                                        .hasNext();) {
                            String key = i.next();
                            _me._cache.remove(key);
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
    }

}
