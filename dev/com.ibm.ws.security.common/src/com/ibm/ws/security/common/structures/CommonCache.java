/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.structures;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

import io.openliberty.security.common.osgi.SecurityOSGiUtils;

public abstract class CommonCache {

    private static final TraceComponent tc = Tr.register(CommonCache.class);

    private final PrivilegedAction<ScheduledExecutorService> getScheduledExecutorServiceAction = new GetScheduledExecutorServiceAction();

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
    private ScheduledExecutorService evictionSchedule;

    private ScheduledFuture<?> previousScheduledTask = null;

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
        if (previousScheduledTask != null) {
            previousScheduledTask.cancel(true);
        }
        if (System.getSecurityManager() == null) {
            evictionSchedule = getScheduledExecutorService();
        } else {
            evictionSchedule = AccessController.doPrivileged(getScheduledExecutorServiceAction);
        }
        if (evictionSchedule != null) {
            previousScheduledTask = evictionSchedule.scheduleWithFixedDelay(new EvictionTask(), timeoutInMilliSeconds, timeoutInMilliSeconds, TimeUnit.MILLISECONDS);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to obtain a ScheduledExecutorService");
            }
        }
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

    private ScheduledExecutorService getScheduledExecutorService() {
        return SecurityOSGiUtils.getService(getClass(), ScheduledExecutorService.class);
    }

    private class GetScheduledExecutorServiceAction implements PrivilegedAction<ScheduledExecutorService> {
        @Override
        public ScheduledExecutorService run() {
            return getScheduledExecutorService();
        }
    }

}
