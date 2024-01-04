/*******************************************************************************
 * Copyright (c) 2010,2023 IBM Corporation and others.
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

package com.ibm.ws.threading.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.threading.ScheduledCustomExecutorTask;

/*
 * This implementation is essentially a wrapper around an scheduled executor but defers execution to the default
 * ExecutorService
 */
public final class ScheduledExecutorImpl extends ScheduledThreadPoolExecutor {
    private final static String threadGroupName = "Scheduled Executor Thread Group";

    private ExecutorService executor;

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void unsetExecutor(ExecutorService executor) {
        this.executor = null;
    }

    public ScheduledExecutorImpl() {
        super(1, new ThreadFactoryImpl("Scheduled Executor", threadGroupName));
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        SchedulingRunnableFixedHelper<?> schedulingHelper = new SchedulingRunnableFixedHelper<Object>(false, command, this, initialDelay, period, unit);
        ScheduledFuture<?> schedFuture = schedule(schedulingHelper, initialDelay, unit);
        schedulingHelper.initScheduledFuture(schedFuture);
        return schedulingHelper;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        SchedulingRunnableFixedHelper<?> schedulingHelper = new SchedulingRunnableFixedHelper<Object>(true, command, this, initialDelay, delay, unit);
        ScheduledFuture<?> schedFuture = schedule(schedulingHelper, initialDelay, unit);
        schedulingHelper.initScheduledFuture(schedFuture);
        return schedulingHelper;
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable r, RunnableScheduledFuture<V> task) {
        Executor executor = this.executor;

        // Run scheduled tasks on the provided custom executor if supplied via ScheduledCustomExecutorTask
        if (r instanceof ScheduledCustomExecutorTask)
            executor = ((ScheduledCustomExecutorTask) r).getExecutor();
        else if (r instanceof SchedulingRunnableFixedHelper) {
            Runnable rr = ((SchedulingRunnableFixedHelper<?>) r).m_runnable;
            if (rr instanceof ScheduledCustomExecutorTask)
                executor = ((ScheduledCustomExecutorTask) rr).getExecutor();
        }
        if (executor == null)
            executor = this.executor;

        // executor will be null after unsetExecutor is called on shutdown. Just return the task in this case.
        if (this.executor == null)
            return task;

        return new SchedulingHelper<V>(r, task, executor, getQueue());
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> c, RunnableScheduledFuture<V> task) {
        // Run scheduled tasks on a policy executor if supplied via ScheduledCustomExecutorTask
        Executor executor = c instanceof ScheduledCustomExecutorTask ? //
                        ((ScheduledCustomExecutorTask) c).getExecutor() : //
                        this.executor;
        if (executor == null)
            executor = this.executor;

        // executor will be null after unsetExecutor is called on shutdown. Just return the task in this case.
        if (this.executor == null)
            return task;

        return new SchedulingHelper<V>(c, task, executor, getQueue());
    }

}
