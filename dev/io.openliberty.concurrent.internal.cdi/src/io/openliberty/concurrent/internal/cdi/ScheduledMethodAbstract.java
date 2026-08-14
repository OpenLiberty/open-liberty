/*******************************************************************************
 * Copyright (c) 2023,2026 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.cdi;

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

import jakarta.enterprise.concurrent.ManagedExecutorService;

/**
 * A task that can be scheduled to run a method at the appropriate time.
 *
 * This can be used in two ways:
 * - Asynchronous method with runAt=Schedule (see subclass ScheduledAsyncMethod)
 * - Schedule directly annotating a bean method (see subclass ScheduledMethod)
 */
public abstract class ScheduledMethodAbstract implements //
                Callable<CompletableFuture<Object>>, //
                ScheduledCustomExecutorTask {
    private static final TraceComponent tc = //
                    Tr.register(ScheduledMethodAbstract.class);

    private final ThreadContextDescriptor contextDescriptor;
    public final CompletableFuture<Object> future;
    protected final Method method;
    private long nextExecutionSkipIfLateBySeconds;
    private ZonedDateTime nextExecutionTime;
    private final List<Long> skipIfLateBySeconds;
    private final List<ScheduleCronTrigger> triggers;
    private final Executor virtualThreadExecutor;

    /**
     * Initialization that is common between ScheduledMethod and
     * ScheduledAsyncMethod.
     *
     * @param contextDescriptor   thread context to apply when running the method
     * @param managedExecutor     managed executor or managed scheduled executor
     * @param method              the bean method to run at the scheduled time
     * @param triggers            indicates when the task should run
     * @param skipIfLateBySeconds number of seconds after which to skip a late task
     *                                execution
     */
    @Trivial
    protected ScheduledMethodAbstract(Method method,
                                      ThreadContextDescriptor contextDescriptor,
                                      WSManagedExecutorService managedExecutor,
                                      List<ScheduleCronTrigger> triggers,
                                      List<Long> skipIfLateBySeconds) {
        this.contextDescriptor = contextDescriptor;
        this.future = ((ManagedExecutorService) managedExecutor).newIncompleteFuture();
        this.method = method;
        this.skipIfLateBySeconds = skipIfLateBySeconds;
        this.triggers = triggers;
        this.virtualThreadExecutor = managedExecutor //
                        .getNormalPolicyExecutor() //
                        .getVirtualThreadExecutor();

        ConcurrencyExtensionMetadata.scheduledExecutor //
                        .schedule(this, computeDelayNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * This is invoked when it is time for the scheduled method to run.
     */
    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public CompletableFuture<Object> call() {
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

            cs = invokeMethod();

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
            // TODO application can also raise types of RuntimeException
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
    long computeDelayNanos() {
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

        long delayNanos = ZonedDateTime //
                        .now(nextExecutionTime.getZone()) //
                        .until(nextExecutionTime, ChronoUnit.NANOS);
        delayNanos = delayNanos < 0L ? 0L : delayNanos;

        if (trace && tc.isEntryEnabled())
            // format as: seconds millis nanos to improve readability
            Tr.exit(this, tc, "computeDelayNanos", new StringBuilder() //
                            .append(delayNanos / 1000000000L).append("s ") //
                            .append(delayNanos % 1000000000L / 1000000L).append("ms ") //
                            .append(delayNanos % 1000000L).append("ns") //
                            .toString());
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

    /**
     * The subclasses ScheduledMethod and ScheduledAsyncMethod must implement
     * this to invoke the bean method that is annotated Schedule or
     * Asynchronous(runAt=Schedule).
     *
     * @return completion stage that is returned by the bean method, otherwise null
     * @throws InvocationTargetException if the bean method raises a declared
     *                                       exception
     * @throws Exception                 if another error occurs attempting to
     *                                       invoke the bean method
     */
    protected abstract CompletionStage<?> invokeMethod() //
                    throws InvocationTargetException, Exception;
}
