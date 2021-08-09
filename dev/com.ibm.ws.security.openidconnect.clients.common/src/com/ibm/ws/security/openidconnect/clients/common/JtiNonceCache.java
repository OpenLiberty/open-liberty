/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;

/**
 * Cache containing three internal tables in order to implement a least-recently-used removal algorithm.
 */
public class JtiNonceCache {

    private static final TraceComponent tc = Tr.register(JtiNonceCache.class);

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
     * Timer to schedule the eviction task.
     */
    private Timer timer;

    public JtiNonceCache() {
        this(0, 0);
    }

    public JtiNonceCache(int entryLimit, long timeoutInMilliSeconds) {
        if (entryLimit > 0) {
            this.entryLimit = entryLimit;
        }
        primaryTable = Collections.synchronizedMap(new com.ibm.ws.security.common.structures.BoundedHashMap(this.entryLimit));

        if (timeoutInMilliSeconds > 0) {
            this.timeoutInMilliSeconds = timeoutInMilliSeconds;
        }

        scheduleEvictionTask(this.timeoutInMilliSeconds);

    }

    public int size() {
        return this.entryLimit;
    }

    private void scheduleEvictionTask(long timeoutInMilliSeconds) {
        EvictionTask evictionTask = new EvictionTask();
        timer = new Timer(true);
        long period = timeoutInMilliSeconds;
        long delay = period;
        timer.schedule(evictionTask, delay, period);
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
     * Find and return the object associated with the specified key. Add it if not already present.
     */
    public boolean contain(OidcTokenImplBase token) { //(IdToken token) {

        String key = token.getJwtId();
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
            long tokenExp = token.getExpirationTimeSeconds() * 1000; // milliseconds
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
    public void cache(OidcTokenImplBase token) { //(IdToken token) {

        String jti = token.getJwtId();
        if (jti == null) {
            return;
        }
        String key = getCacheKey(token);
        long tokenExp = token.getExpirationTimeSeconds() * 1000; // milliseconds
        if (tokenExp == 0) { // in case it's not set, let's give it one hour
            tokenExp = (new Date()).getTime() + 60 * 60 * 1000; // 1 hour
        }
        primaryTable.put(key, Long.valueOf(tokenExp));
    }

    protected String getCacheKey(OidcTokenImplBase token) { //(IdToken token) {
        String key = token.getJwtId();
        if (key == null) {
            return null;
        }
        key = token.getIssuer() + ":" + key;
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

    private class EvictionTask extends TimerTask {

        /** {@inheritDoc} */
        @Override
        public void run() {
            evictStaleEntries();
        }

    }

}
