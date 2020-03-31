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

import java.util.Timer;
import java.util.TimerTask;

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
     * Timer to schedule the eviction task.
     */
    protected Timer timer;

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
        timer.cancel();
        scheduleEvictionTask(timeoutInMilliSeconds);
    }

    protected void scheduleEvictionTask(long timeoutInMilliSeconds) {
        EvictionTask evictionTask = new EvictionTask();
        timer = new Timer(true);
        long period = timeoutInMilliSeconds;
        long delay = period;
        timer.schedule(evictionTask, delay, period);
    }

    /**
     * Implementation of the eviction strategy.
     */
    abstract protected void evictStaleEntries();

    private class EvictionTask extends TimerTask {
        /** {@inheritDoc} */
        @Override
        public void run() {
            evictStaleEntries();
        }

    }

}
