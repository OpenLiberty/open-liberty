/*******************************************************************************
 * Copyright (c) 1997, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.clients.common;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;

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

    private void scheduleEvictionTask(long timeoutInMilliSeconds) {
        EvictionTask evictionTask = new EvictionTask();
        timer = new Timer(true);
        long period = timeoutInMilliSeconds;
        long delay = period;
        timer.schedule(evictionTask, delay, period);
    }

    protected Object get(String key) {
        Object curEntry = primaryTable.get(key);
        return curEntry;
    }

    /**
     * Determine if the token's jti is in the cache, remove entry if expired, and add it if not already present.
     */
    public boolean contain(OidcTokenImplBase token) {
        String key = token.getJwtId();
        if (key == null) {
            return false;
        }
        key = getCacheKey(token);

        synchronized (primaryTable) {
            long currentTimeInMilliseconds = (new Date()).getTime();
            Long expInMilliseconds = (Long) primaryTable.get(key);
            if (expInMilliseconds != null) {
                if (expInMilliseconds.longValue() > currentTimeInMilliseconds) {
                    return true;
                } else {
                    primaryTable.remove(key);
                }
            }

            long tokenExpInMilliseconds = token.getExpirationTimeSeconds() * 1000;
            if (tokenExpInMilliseconds == 0) {
                long oneHour = 60 * 60 * 1000;
                tokenExpInMilliseconds = currentTimeInMilliseconds + oneHour;
            }
            primaryTable.put(key, Long.valueOf(tokenExpInMilliseconds));
        }

        return false;
    }

    protected String getCacheKey(OidcTokenImplBase token) {
        String key = token.getJwtId();
        if (key == null) {
            return null;
        }
        key = token.getIssuer() + ":" + key;
        return key;
    }

    protected synchronized void evictStaleEntries() {
        synchronized (primaryTable) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                int size = primaryTable.size();
                Tr.debug(tc, "The current cache size is " + size);
            }

            removeExpiredTokens(primaryTable);
        }
    }

    private void removeExpiredTokens(Map<String, Object> allTokens) {
        Set<String> keys = allTokens.keySet();
        long currentTimeInMilliseconds = (new Date()).getTime();
        for (Iterator<String> i = keys.iterator(); i.hasNext();) {
            String key = i.next();
            Long tokenExpInMilliseconds = (Long) allTokens.get(key);
            if (tokenExpInMilliseconds.longValue() < currentTimeInMilliseconds) {
                primaryTable.remove(key);
            }
        }
    }

    private class EvictionTask extends TimerTask {

        @Override
        public void run() {
            evictStaleEntries();
        }

    }

}
