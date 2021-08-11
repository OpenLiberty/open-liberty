/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.structures;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.annotation.Sensitive;

public abstract class CommonCache {

    /**
     * Maximum number of entries allowed in the cache.
     */
    protected int entryLimit = 50000;

    /**
     * Default cache timeout.
     */
    protected long timeoutInMilliSeconds = 5 * 60 * 1000;

    /**
     * Scheduled executor to run the eviction task.
     */
    private ScheduledThreadPoolExecutor evictionSchedule;

    public int size() {
        return this.entryLimit;
    }

    public long getTimeoutInMilliseconds() {
        return timeoutInMilliSeconds;
    }

    /**
     * Remove an object from the Cache.
     */
    abstract public void remove(@Sensitive Object key);

    /**
     * Find and return the object associated with the specified key.
     */
    abstract public Object get(@Sensitive String key);

    /**
     * Insert the value into the Cache using the specified key.
     */
    abstract public void put(@Sensitive String key, Object value);

    /**
     * Implementation of the eviction strategy.
     */
    public synchronized void rescheduleCleanup(long newTimeoutInMillis) {
        if (newTimeoutInMillis > 0) {
            this.timeoutInMilliSeconds = newTimeoutInMillis;
        }
        if (evictionSchedule != null) {
            evictionSchedule.shutdownNow();
        }
        evictionSchedule = new ScheduledThreadPoolExecutor(1);
        evictionSchedule.scheduleWithFixedDelay(new EvictionTask(), timeoutInMilliSeconds, timeoutInMilliSeconds, TimeUnit.MILLISECONDS);
    }

    /**
     * Implementation of the eviction strategy.
     */
    abstract protected void evictStaleEntries();

    private class EvictionTask implements Runnable {
        @Override
        public void run() {
            evictStaleEntries();
        }

    }
}
