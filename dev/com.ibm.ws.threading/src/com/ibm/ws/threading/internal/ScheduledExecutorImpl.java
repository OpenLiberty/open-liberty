/*******************************************************************************
 * Copyright (c) 2010,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.threading.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.threading.ScheduledPolicyExecutorTask;

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
        ExecutorService executorService = this.executor;

        // Run scheduled tasks on a policy executor if supplied via ScheduledPolicyExecutorTask
        if (r instanceof ScheduledPolicyExecutorTask)
            executorService = ((ScheduledPolicyExecutorTask) r).getExecutor();
        else if (r instanceof SchedulingRunnableFixedHelper) {
            Runnable rr = ((SchedulingRunnableFixedHelper<?>) r).m_runnable;
            if (rr instanceof ScheduledPolicyExecutorTask)
                executorService = ((ScheduledPolicyExecutorTask) rr).getExecutor();
        }

        // executor will be null after unsetExecutor is called on shutdown. Just return the task in this case.
        if (this.executor == null)
            return task;

        return new SchedulingHelper<V>(r, task, executorService, getQueue());
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> c, RunnableScheduledFuture<V> task) {
        // Run scheduled tasks on a policy executor if supplied via ScheduledPolicyExecutorTask
        ExecutorService executorService = c instanceof ScheduledPolicyExecutorTask ? //
                        ((ScheduledPolicyExecutorTask) c).getExecutor() : //
                        this.executor;

        // executor will be null after unsetExecutor is called on shutdown. Just return the task in this case.
        if (this.executor == null)
            return task;

        return new SchedulingHelper<V>(c, task, executorService, getQueue());
    }

}
