/*******************************************************************************
 * Copyright (c) 2016, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.jwt.utils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.common.structures.BoundedHashMap;

import io.openliberty.security.common.osgi.SecurityOSGiUtils;

/**
 * Cache containing three internal tables in order to implement a least-recently-used removal algorithm.
 * TODO - Duplicate of com.ibm.ws.security.openidconnect.client.JtiNonceCache
 * - Ideally would consolidate these two
 */
public class JtiNonceCache {

    private static final TraceComponent tc = Tr.register(JtiNonceCache.class);

    private final PrivilegedAction<ScheduledExecutorService> getScheduledExecutorServiceAction = new GetScheduledExecutorServiceAction();

    /**
     * Primary hash table containing the most recently used entries.
     */
    private final Map<String, Object> primaryTable;

    /**
     * Maximum number of entries allowed in the cache.
     */
    private int entryLimit = 100000;

    /**
     * Default cache timeout.
     */
    private long timeoutInMilliSeconds = 10 * 60 * 1000;

    /**
     * Scheduled executor to run the eviction task.
     */
    private ScheduledExecutorService evictionSchedule;

    public JtiNonceCache() {
        this(0, 0);
    }

    public JtiNonceCache(int entryLimit, long timeoutInMilliSeconds) {
        if (entryLimit > 0) {
            this.entryLimit = entryLimit;
        }
        primaryTable = Collections.synchronizedMap(new BoundedHashMap(this.entryLimit));

        if (timeoutInMilliSeconds > 0) {
            this.timeoutInMilliSeconds = timeoutInMilliSeconds;
        }

        scheduleEvictionTask(this.timeoutInMilliSeconds);

    }

    public int size() {
        return entryLimit;
    }

    private void scheduleEvictionTask(long timeoutInMilliSeconds) {
        if (System.getSecurityManager() == null) {
            evictionSchedule = getScheduledExecutorService();
        } else {
            evictionSchedule = AccessController.doPrivileged(getScheduledExecutorServiceAction);
        }
        if (evictionSchedule != null) {
            evictionSchedule.scheduleWithFixedDelay(new EvictionTask(), timeoutInMilliSeconds, timeoutInMilliSeconds, TimeUnit.MILLISECONDS);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to obtain a ScheduledExecutorService");
            }
        }
    }

    private class GetScheduledExecutorServiceAction implements PrivilegedAction<ScheduledExecutorService> {

        @Override
        public ScheduledExecutorService run() {
            return getScheduledExecutorService();
        }

    }

    private ScheduledExecutorService getScheduledExecutorService() {
        return SecurityOSGiUtils.getService(getClass(), ScheduledExecutorService.class);
    }

    /**
     * Remove an object from the Cache.
     */
    public void remove(Object key) {
        primaryTable.remove(key);
    }

    /**
     * Find and return the object associated with the specified key.
     */
    public Object get(String key) {
        Object curEntry = primaryTable.get(key);
        return curEntry;
    }

    /**
     * Find and return the object associated with the specified key.
     */
    public boolean contains(JwtToken token) {
        if (token == null) {
            return false;
        }
        Claims claims = token.getClaims();
        if (claims == null) {
            return false;
        }
        String key = claims.getJwtId();
        if (key == null) {
            return false;
        }
        key = getCacheKey(token);
        long currentTimeMilliseconds = (new Date()).getTime();
        synchronized (primaryTable) {
            Long exp = (Long) primaryTable.get(key); // milliseconds
            if (exp != null) {
                if (exp.longValue() > currentTimeMilliseconds) { // not expired yet // milliseconds
                    return true;
                } else {
                    primaryTable.remove(key);
                }
            }
            // cache the jwt
            long tokenExp = claims.getExpiration() * 1000; // milliseconds
            if (tokenExp == 0) { // in case it's not set, let's give it one hour
                tokenExp = currentTimeMilliseconds + 60 * 60 * 1000; // 1 hour
            }
            primaryTable.put(key, Long.valueOf(tokenExp));
        }

        return false;
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public void cache(JwtToken token) {
        if (token == null) {
            return;
        }
        Claims claims = token.getClaims();
        if (claims == null) {
            return;
        }
        String jti = claims.getJwtId();
        if (jti == null) {
            return;
        }
        String key = getCacheKey(token);
        long tokenExp = claims.getExpiration() * 1000; // milliseconds
        if (tokenExp == 0) { // in case it's not set, let's give it one hour
            tokenExp = (new Date()).getTime() + 60 * 60 * 1000; // 1 hour
        }
        primaryTable.put(key, Long.valueOf(tokenExp));
    }

    protected String getCacheKey(JwtToken token) {
        Claims claims = token.getClaims();
        if (claims == null) {
            return null;
        }
        String jti = claims.getJwtId();
        if (jti == null) {
            return null;
        }
        String key = claims.getIssuer() + ":" + jti;
        return key;
    }

    /**
     * Implementation of the eviction strategy.
     */
    protected synchronized void evictStaleEntries() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            int size = primaryTable.size();
            Tr.debug(tc, "The current cache size is " + size);
        }

        Map<String, Object> secondaryTable = new HashMap<String, Object>();
        secondaryTable.putAll(primaryTable); //create 2nd map to avoid concurrent during entry manipulation
        Set<String> keysToRemove = findExpiredTokens(secondaryTable);
        removeExpiredTokens(keysToRemove);
        secondaryTable.clear();

    }

    protected Set<String> findExpiredTokens(Map<String, Object> allTokens) {
        Set<String> keys = allTokens.keySet();
        Set<String> keysToRemove = new HashSet<String>();
        long lCurrentTimeMilliseconds = (new Date()).getTime();
        for (Iterator<String> i = keys.iterator(); i.hasNext();) {
            String key = i.next();
            Long exp = (Long) allTokens.get(key);
            if (exp.longValue() < lCurrentTimeMilliseconds) {
                keysToRemove.add(key);
            }
        }

        return keysToRemove;
    }

    protected void removeExpiredTokens(Set<String> keysToRemove) {
        for (Iterator<String> i = keysToRemove.iterator(); i.hasNext();) {
            String key = i.next();
            primaryTable.remove(key);
        }
    }

    private class EvictionTask implements Runnable {

        /** {@inheritDoc} */
        @Override
        public void run() {
            evictStaleEntries();
        }

    }

}
