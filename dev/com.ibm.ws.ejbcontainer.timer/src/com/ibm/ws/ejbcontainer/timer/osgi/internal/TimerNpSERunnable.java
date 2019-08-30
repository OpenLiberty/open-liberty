/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.osgi.internal;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.ejs.container.TimerNpImpl;
import com.ibm.ejs.container.TimerNpRunnable;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A TimerNpRunnable implementation that supports the use of the
 * Java EE concurrency scheduled executor service.
 */
public class TimerNpSERunnable extends TimerNpRunnable {

    private static final TraceComponent tc = Tr.register(TimerNpSERunnable.class);

    /** The ScheduledExecutor used to create scheduled task instances **/
    private final ScheduledExecutorService ivExecutorService;

    /**
     * The Java EE concurrency future that represents the scheduled task.
     */
    private ScheduledFuture<?> ivTaskFuture;

    /** The contextual proxy runnable */
    private Runnable npTask;

    TimerNpSERunnable(ScheduledExecutorService executorService, TimerNpImpl timerNpImpl, int retryLimit, long retryInterval) {
        super(timerNpImpl, retryLimit, retryInterval);
        ivExecutorService = executorService;
    }

    /**
     * Sets the contextual proxy runnable. If r is null, it will use this class.
     *
     * @param r
     */
    public void setContextualProxyRunnable(Runnable r) {
        if (r == null) {
            this.npTask = this;
        } else {
            this.npTask = r;
        }
    }

    // --------------------------------------------------------------------------
    //
    // Methods from TimerNpRunnable abstract class
    //
    // --------------------------------------------------------------------------

    @Override
    protected void schedule(long expiration) {
        long delay = Math.max(0, expiration - System.currentTimeMillis());
        ivTaskFuture = ivExecutorService.schedule(this.npTask, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void scheduleNext(long nextExpiration) {
        long delay = Math.max(0, nextExpiration - System.currentTimeMillis());
        ivTaskFuture = ivExecutorService.schedule(this.npTask, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void scheduleRetry(long retryInterval) {
        ivTaskFuture = ivExecutorService.schedule(this.npTask, retryInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void cancel() {
        if (ivTaskFuture != null) {
            boolean cancelled = ivTaskFuture.cancel(false);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Timer was cancelled : " + cancelled);
            }
        }
    }
}
