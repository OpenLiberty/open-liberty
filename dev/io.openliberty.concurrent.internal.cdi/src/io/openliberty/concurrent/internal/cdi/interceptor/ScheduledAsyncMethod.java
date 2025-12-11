/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.cdi.interceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.threading.ScheduledCustomExecutorTask;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

import io.openliberty.concurrent.internal.cdi.ConcurrencyExtensionMetadata;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.interceptor.InvocationContext;

/**
 * A task that can be scheduled to run an asynchronous method at the appropriate time.
 * The first time executed, this task runs the asynchronous method by delegating back to the
 * interceptor (AsyncInterceptor) to run the proceed method on the original invocation,
 * similar to how an asynchronous method would normally run.
 * On all subsequent executions, this task runs the asynchronous method inline by invoking
 * the method on the bean again. In this case, a ThreadLocal is first place on the thread
 * as a signal to the interceptor (AsyncInterceptor) that this is not a new scheduled asynchronous method,
 * but is instead is subsequent execution that needs to proceed immediately inline.
 * In both cases, InvocationContext.proceed is used, but on different InvocationContext instances,
 * ensuring that each time it runs all of the interceptors run and run in the correct order.
 * The ThreadLocal also ensures that all executions use the same CompletableFuture instance
 * to represent completion of the application's original request.
 */
class ScheduledAsyncMethod implements Callable<CompletableFuture<Object>>, ScheduledCustomExecutorTask {
    private static final TraceComponent tc = Tr.register(ScheduledAsyncMethod.class);

    /**
     * Scheduled executions other than the first are achieved by invoking the asynchronous method
     * on the bean from the scheduled executor thread when the schedule indicates that it is time
     * for the task to run. This causes intercept to be invoked, which uses the presence of this
     * thread local to detect this situation and run the method inline.
     * This approach is used to ensure that all of the interceptors are invoked with each execution of the task.
     */
    static final ThreadLocal<CompletableFuture<?>> inlineExecutionFuture = new ThreadLocal<CompletableFuture<?>>();

    private final ThreadContextDescriptor contextDescriptor;
    private final InvocationContext firstInvocation;
    final CompletableFuture<Object> future;
    private final AsyncInterceptor interceptor;
    private boolean isFirstExecution = true;
    private long nextExecutionSkipIfLateBySeconds;
    private ZonedDateTime nextExecutionTime;
    private final List<Long> skipIfLateBySeconds;
    private final List<ScheduleCronTrigger> triggers;
    private final Executor virtualThreadExecutor;

    ScheduledAsyncMethod(InvocationContext firstInvocation, AsyncInterceptor interceptor, WSManagedExecutorService managedExecutor,
                         List<ScheduleCronTrigger> triggers, List<Long> skipIfLateBySeconds) {
        this.contextDescriptor = managedExecutor.captureThreadContext(null);
        this.firstInvocation = firstInvocation;
        this.future = ((ManagedExecutorService) managedExecutor).newIncompleteFuture();
        this.interceptor = interceptor;
        this.triggers = triggers;
        this.skipIfLateBySeconds = skipIfLateBySeconds;
        this.virtualThreadExecutor = managedExecutor.getNormalPolicyExecutor().getVirtualThreadExecutor();

        ConcurrencyExtensionMetadata.scheduledExecutor.schedule(this, computeDelayNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * This is invoked when it is time for the scheduled async method to run.
     */
    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public CompletableFuture<Object> call() {
        final Method method = firstInvocation.getMethod();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc,
                     "call " + method.getName() + '(' +
                               Arrays.toString(method.getGenericParameterTypes()) + ')',
                     method.getDeclaringClass().getName(),
                     method.getGenericReturnType().getTypeName());

        if (future.isDone()) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "call: ignore, already complete");
            return future;
        }

        // Detect late starting tasks:
        long secondsLate = nextExecutionTime //
                        .until(ZonedDateTime.now(nextExecutionTime.getZone()), //
                               ChronoUnit.SECONDS);
        if (secondsLate > nextExecutionSkipIfLateBySeconds) {
            try {
                long delayNanos = computeDelayNanos();
                ConcurrencyExtensionMetadata.scheduledExecutor //
                                .schedule(this, delayNanos, TimeUnit.NANOSECONDS);
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "call: skip because late by " + secondsLate +
                                      " seconds");
            } catch (Throwable x) {
                FFDCFilter.processException(x, getClass().getName(), "128", this);
                future.completeExceptionally(x);
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "call", x);
            }
            return future;
        }

        Throwable failure = null;
        ArrayList<ThreadContext> contextApplied = null;
        CompletionStage<?> cs = null;
        try {
            if (contextDescriptor != null)
                contextApplied = contextDescriptor.taskStarting();

            if (isFirstExecution) {
                try {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "proceed with " + method.getName());
                    cs = interceptor.invoke(firstInvocation, future);
                } finally {
                    isFirstExecution = false;
                }
            } else {
                // For subsequent executions, invoke the bean method again,
                // but use a thread local to signal that it should run inline
                // on the scheduled async method thread:
                inlineExecutionFuture.set(future);
                try {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc,
                                 "invoke " + method.getName() + " on bean instance",
                                 firstInvocation.getParameters());
                    method.setAccessible(true);
                    cs = (CompletionStage<?>) method.invoke(firstInvocation.getTarget(),
                                                            firstInvocation.getParameters());
                } finally {
                    inlineExecutionFuture.remove();
                }
            }
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "completion stage from invocation is " + cs);
        } catch (Throwable x) {
            boolean appException = false;
            failure = x;
            if (failure instanceof InvocationTargetException) {
                appException = true;
                failure = failure.getCause();
            }
            if (failure instanceof CompletionException) {
                appException = true;
                if (failure.getCause() != null)
                    failure = failure.getCause();
            }
            if (!appException)
                FFDCFilter.processException(x, getClass().getName(), "183", this);
        } finally {
            try {
                if (contextApplied != null)
                    contextDescriptor.taskStopping(contextApplied);
            } catch (RuntimeException x) {
                failure = x;
            } finally {
                if (failure != null)
                    future.completeExceptionally(failure);
            }
        }

        if (!future.isDone())
            if (cs == null)
                try { // reschedule next execution
                    ConcurrencyExtensionMetadata.scheduledExecutor //
                                    .schedule(this, computeDelayNanos(), TimeUnit.NANOSECONDS);
                } catch (Exception x) {
                    future.completeExceptionally(x);
                }
            else
                // complete with the same result/exception as the
                // completion stage that is returned by the async method
                cs.whenComplete((result, x) -> {
                    if (x == null)
                        future.complete(result);
                    else
                        future.completeExceptionally(x);
                });

        if (trace && tc.isEntryEnabled())
            if (future.isCompletedExceptionally()) {
                if (failure == null)
                    try {
                        future.getNow(null);
                    } catch (Throwable x) {
                        failure = x;
                    }
                Tr.exit(this, tc, "call", new Object[] { future, failure });
            } else {
                Tr.exit(this, tc, "call", future);
            }
        return future;
    }

    /**
     * Compute the delay until the next execution
     * and assign the nextExecutionTime and nextExecutionSkipIfLateBySeconds.
     *
     * @return nanoseconds until the next execution.
     */
    @Trivial
    private long computeDelayNanos() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "computeDelayNanos");

        ZonedDateTime now = null;
        nextExecutionTime = null;
        for (int i = 0; i < triggers.size(); i++) {
            ScheduleCronTrigger trigger = triggers.get(i);
            now = now == null ? ZonedDateTime.now(trigger.getZoneId()) : now;
            ZonedDateTime time = trigger.next(now);
            if (nextExecutionTime == null || nextExecutionTime.isAfter(time)) {
                nextExecutionTime = time;
                nextExecutionSkipIfLateBySeconds = skipIfLateBySeconds.get(i);
            }
        }

        long delayNanos = ZonedDateTime.now(nextExecutionTime.getZone()).until(nextExecutionTime, ChronoUnit.NANOS);
        delayNanos = delayNanos < 0L ? 0L : delayNanos;

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "computeDelayNanos", // format as: seconds millis nanos to improve readability
                    (delayNanos / 1000000000L) + "s " + (delayNanos % 1000000000L / 1000000L) + "ms " + (delayNanos % 1000000L) + "ns");
        return delayNanos;
    }

    /**
     * Returns the executor that determines the thread upon which to run this task.
     *
     * @return executor that determines the thread to run on.
     */
    @Override
    @Trivial
    public Executor getExecutor() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getExecutor for virtual threads: " + virtualThreadExecutor);
        return virtualThreadExecutor;
    }
}