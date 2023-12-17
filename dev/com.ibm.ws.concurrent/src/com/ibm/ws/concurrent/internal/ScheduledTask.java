/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.concurrent.internal;

import java.math.BigInteger;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.SkippedException;
import javax.enterprise.concurrent.Trigger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.ContextualAction;
import com.ibm.ws.concurrent.TriggerService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.threading.ScheduledCustomExecutorTask;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * This class represents a scheduled task and its future.
 * The task can be a repeating task or a one-time task, as determined by the trigger.
 * The constructors schedule the first execution of the task.
 * If a repeating task, each execution of the task schedules the next execution,
 * thus guaranteeing that we never have overlapping executions of the same task.
 */
public class ScheduledTask<T> implements Callable<T>, ScheduledCustomExecutorTask {
    private static final TraceComponent tc = Tr.register(ScheduledTask.class);

    /**
     * Result for a single execution of a task.
     */
    @Trivial
    private class Result {
        /**
         * Thread of execution while task is running. This allows us to determine if Future.get is running on the same thread,
         * which in the case of taskSubmitted/taskStarting we should reject with InterruptedException to prevent a hang/timeout.
         */
        private volatile Thread executionThread;

        /**
         * Can be used to wait for this execution of the task to complete.
         */
        private final CountDownLatch latch = new CountDownLatch(1);

        /**
         * Status of the result. Expect the status to change as the task is scheduled and executes.
         */
        private final AtomicReference<Status<T>> statusRef = new AtomicReference<Status<T>>(Status.of(Status.Type.NONE));

        private final boolean compareAndSet(Status<T> expectedStatus, Status<T> newStatus) {
            boolean updated = statusRef.compareAndSet(expectedStatus, newStatus);
            if (updated && TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                StringBuilder sb = new StringBuilder(60).append(expectedStatus.type).append("-->").append(newStatus.type).append(' ');
                if (Boolean.TRUE.equals(newStatus.hasNext))
                    sb.append("[has next] ");
                else if (Boolean.FALSE.equals(newStatus.hasNext))
                    sb.append("[final] ");
                if (newStatus.value != null)
                    sb.append(newStatus.value).append(' ');
                if (newStatus.failure != null)
                    sb.append(newStatus.failure);
                Tr.event(ScheduledTask.this, tc, sb.toString());
            }
            return updated;
        }

        private final Status<T> getStatus() {
            return statusRef.get();
        }
    }

    /**
     * Status for a task.
     *
     * @param <T> type of the value.
     */
    @Trivial
    private static class Status<T> {
        @Trivial
        private enum Type {
            ABORTED, CANCELED, DONE, NONE, SKIPPED, STARTED, SUBMITTED
        };

        private final Throwable failure;
        private final Boolean hasNext; // keep this value null for unknown until the final execution completes or is aborted or canceled
        private final Type type;
        private final T value;

        private Status(Type type, T value, Throwable failure, Boolean hasNext) {
            this.failure = failure;
            this.hasNext = hasNext;
            this.type = type;
            this.value = value;
        }

        private final boolean isFinalExecutionComplete() {
            return Boolean.FALSE.equals(hasNext);
        }

        private static final <T> Status<T> done(T resultValue, boolean isFinalExecution) {
            return new Status<T>(Type.DONE, resultValue, null, isFinalExecution ? false : null);
        }

        private static final <T> Status<T> of(Type type) {
            Boolean hasNext = type == Type.ABORTED || type == Type.CANCELED || type == Type.DONE ? false : null;
            return new Status<T>(type, null, null, hasNext);
        }

        private static final <T> Status<T> of(Type type, Throwable failure) {
            Boolean hasNext = type == Type.ABORTED || type == Type.CANCELED || type == Type.DONE ? false : null;
            return new Status<T>(type, null, failure, hasNext);
        }

        private Status<T> withNextExecution() {
            return new Status<T>(type, value, failure, true);
        }

        private Status<T> withoutNextExecution() {
            return new Status<T>(type, value, failure, false);
        }
    }

    /**
     * Fixed delay between executions of the task. Only available if using fixed delay.
     */
    private final Long fixedDelay;

    /**
     * Fixed period between the start of executions of the task. Only available if using fixed rate.
     */
    private final Long fixedRate;

    /**
     * Future for this scheduled task.
     */
    final FutureImpl future = new FutureImpl();

    /**
     * Initial delay before the first execution of the task. Only available for fixed delay, fixed rate, or one-shot tasks.
     */
    private final Long initialDelay;

    /**
     * True if task is submitted as a Callable. False if the task is submitted as a Runnable.
     */
    private final boolean isCallable;

    /**
     * Information about the most recent execution of the task. Only available if using a trigger.
     */
    private volatile LastExecution lastExecution;

    /**
     * Managed task listener. Null if there isn't one.
     */
    private final ManagedTaskListener listener;

    /**
     * Managed Scheduled executor service to which the task was submitted.
     */
    private final ManagedScheduledExecutorServiceImpl managedExecSvc;

    /**
     * More than 106751 days cannot be supported
     * due to being built on top of java.util.concurrent.ScheduledThreadPoolExecutor.
     */
    private static final Duration MAX_DELAY = Duration.of(Long.MAX_VALUE, ChronoUnit.NANOS);

    /**
     * Next execution time for the task.
     */
    private volatile ZonedDateTime nextExecutionTime;

    /**
     * Possibly empty result of the task. A CountDownLatch in the result can be used to wait for it to be populated.
     */
    private final AtomicReference<Result> resultRef = new AtomicReference<Result>(new Result());

    /**
     * The task.
     */
    private final Object task;

    /**
     * Date at which the task was originally scheduled.
     */
    private final ZonedDateTime taskScheduledTime;

    /**
     * Previously captured thread context with which the task should run.
     */
    private final ThreadContextDescriptor threadContextDescriptor;

    /**
     * Trigger that controls when and how often to execute the task. Null if not using a trigger.
     */
    private final Trigger trigger;

    /**
     * Unit of time for fixed delay, fixed rate, or one-shot tasks.
     * In the case of trigger, unit is always nanoseconds, which corresponds to the precision of java.time.ZonedDateTime.
     */
    private final ChronoUnit unit;

    /**
     * Construct and schedule a task which also serves as a future.
     *
     * @param managedExecSvc managed scheduled executor service to which the task was submitted
     * @param task           task
     * @param isCallable     indicates whether task is submitted as a Callable or Runnable.
     * @param initialDelay   indicates when the task should first run
     * @param fixedDelay     fixed delay between executions of the task. Null if not using fixed delay.
     * @param fixedRate      fixed period between the start of executions of the task. Null if not using fixed rate.
     * @param timeunit       unit of time.
     */
    ScheduledTask(ManagedScheduledExecutorServiceImpl managedExecSvc, Object task, boolean isCallable,
                  long initialDelay, Long fixedDelay, Long fixedRate, TimeUnit timeunit) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        this.fixedDelay = fixedDelay;
        this.fixedRate = fixedRate;
        this.initialDelay = initialDelay;
        this.isCallable = isCallable;
        this.listener = task instanceof ManagedTask ? ((ManagedTask) task).getManagedTaskListener() : null;
        this.managedExecSvc = managedExecSvc;
        this.taskScheduledTime = ZonedDateTime.now();
        this.trigger = null;
        this.unit = toChronoUnit(timeunit);

        if (task instanceof ContextualAction) {
            ContextualAction<?> a = (ContextualAction<?>) task;
            this.task = a.getAction();
            this.threadContextDescriptor = a.getContextDescriptor();
        } else {
            this.task = task;
            Map<String, String> execProps = managedExecSvc.getExecutionProperties(task);
            try {
                this.threadContextDescriptor = managedExecSvc.captureThreadContext(execProps);
            } catch (NullPointerException x) {
                throw x;
            } catch (Throwable x) {
                throw new RejectedExecutionException(x);
            }
        }

        // Cap the maximum delay at what is supported by ScheduledThreadPoolExecutor, upon which Liberty ScheduledExecutorService is built
        Duration delay = Duration.of(initialDelay, unit);
        if (delay.compareTo(MAX_DELAY) > 0)
            delay = MAX_DELAY;

        nextExecutionTime = taskScheduledTime.plus(delay);

        Result result = resultRef.get();

        // notify listener: taskSubmitted
        if (listener != null) {
            ThreadContext tranContextRestorer = managedExecSvc.suspendTransaction();
            try {
                result.executionThread = Thread.currentThread();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(this, tc, "taskSubmitted", managedExecSvc, this.task);
                listener.taskSubmitted(future, managedExecSvc, this.task);
            } finally {
                result.executionThread = null;
                if (tranContextRestorer != null)
                    tranContextRestorer.taskStopping();
            }
        }

        // schedule the task if the listener didn't cancel it
        Status<T> status = result.getStatus();
        if (status.type == Status.Type.NONE && result.compareAndSet(status, Status.of(Status.Type.SUBMITTED))) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "schedule " + delay + " from now");
            ScheduledExecutorService scheduledExecSvc = managedExecSvc.scheduledExecSvc;
            ScheduledFuture<?> scheduledFuture = scheduledExecSvc.schedule(this, delay.toNanos(), TimeUnit.NANOSECONDS);
            future.scheduledFutureRef.set(scheduledFuture);
        }
    }

    /**
     * Construct and schedule a task which also serves as a future.
     *
     * @param managedExecSvc managed scheduled executor service to which the task was submitted
     * @param task           task
     * @param isCallable     indicates whether task is submitted as a Callable or Runnable.
     * @param trigger        indicates when the task should run
     */
    ScheduledTask(ManagedScheduledExecutorServiceImpl managedExecSvc, Object task, boolean isCallable, Trigger trigger) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        TriggerService triggerSvc = managedExecSvc.concurrencySvc.triggerSvc;

        this.fixedDelay = null;
        this.fixedRate = null;
        this.initialDelay = null;
        this.isCallable = isCallable;
        this.listener = task instanceof ManagedTask ? ((ManagedTask) task).getManagedTaskListener() : null;
        this.managedExecSvc = managedExecSvc;
        this.taskScheduledTime = ZonedDateTime.now(triggerSvc.getZoneId(trigger));
        this.trigger = trigger;
        this.unit = ChronoUnit.NANOS;

        if (task instanceof ContextualAction) {
            ContextualAction<?> a = (ContextualAction<?>) task;
            this.task = a.getAction();
            this.threadContextDescriptor = a.getContextDescriptor();
        } else {
            this.task = task;
            Map<String, String> execProps = managedExecSvc.getExecutionProperties(task);
            try {
                this.threadContextDescriptor = managedExecSvc.captureThreadContext(execProps);
            } catch (Throwable x) {
                throw new RejectedExecutionException(x);
            }
        }

        try {
            nextExecutionTime = triggerSvc.getNextRunTime(null, taskScheduledTime, trigger);
        } catch (Throwable x) {
            throw new RejectedExecutionException(x);
        }
        if (nextExecutionTime == null)
            throw new RejectedExecutionException("Trigger.getNextRunTime: null");

        long delay = taskScheduledTime.until(nextExecutionTime, ChronoUnit.NANOS);

        Result result = resultRef.get();

        // notify listener: taskSubmitted
        if (listener != null) {
            ThreadContext tranContextRestorer = managedExecSvc.suspendTransaction();
            try {
                result.executionThread = Thread.currentThread();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(this, tc, "taskSubmitted", managedExecSvc, this.task);
                listener.taskSubmitted(future, managedExecSvc, this.task);
            } finally {
                result.executionThread = null;
                if (tranContextRestorer != null)
                    tranContextRestorer.taskStopping();
            }
        }

        // schedule the task if the listener didn't cancel it
        Status<T> status = result.getStatus();
        if (status.type == Status.Type.NONE && result.compareAndSet(status, Status.of(Status.Type.SUBMITTED))) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "schedule " + Duration.of(delay, ChronoUnit.NANOS) + " from now");
            ScheduledExecutorService scheduledExecSvc = managedExecSvc.scheduledExecSvc;
            ScheduledFuture<?> scheduledFuture = scheduledExecSvc.schedule(this, delay, TimeUnit.NANOSECONDS);
            future.scheduledFutureRef.set(scheduledFuture);
        }
    }

    /**
     * Callable.call is invoked by the executor to run this task some time (hopefully soon)
     * after the scheduled execution time has been reached.
     */
    @FFDCIgnore(Throwable.class)
    @Override
    public T call() throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (future.isCancelled()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "canceled - not running the task");
            return null;
        }

        TriggerService triggerSvc = managedExecSvc.concurrencySvc.triggerSvc;

        Result result = resultRef.get(), resultForThisExecution = result;
        Status<T> skipped = null;
        Status<T> status;
        boolean done = false;
        T taskResult = null;
        ArrayList<ThreadContext> contextAppliedToThread = null;
        resultForThisExecution.executionThread = Thread.currentThread();
        try {
            // EE Concurrency 3.2.6.1: All tasks submitted to an executor must not run if task's component is not started.
            // ThreadContextDescriptor.taskStarting covers this requirement for us.
            contextAppliedToThread = threadContextDescriptor.taskStarting();

            // Determine if task should be skipped
            if (trigger != null)
                try {
                    if (triggerSvc.skipRun(lastExecution, nextExecutionTime, trigger))
                        skipped = Status.of(Status.Type.SKIPPED);
                } catch (Throwable x) {
                    // spec requires skip when skipRun fails
                    Tr.error(tc, "CWWKC1103.skip.run.failed", getName(), managedExecSvc.name, x);
                    skipped = Status.of(Status.Type.SKIPPED, x);
                }

            ZonedDateTime computedNextExecution = null;

            // Run task if it wasn't skipped
            if (skipped == null) {
                // notify listener: taskStarting
                if (listener != null)
                    try {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            Tr.event(this, tc, "taskStarting", managedExecSvc, task);
                        listener.taskStarting(future, managedExecSvc, task);
                    } finally {
                        done = result.getStatus().type == Status.Type.CANCELED;
                    }

                // run the task if the listener didn't cancel it
                status = result.getStatus();
                if (status.type == Status.Type.SUBMITTED && result.compareAndSet(status, Status.of(Status.Type.STARTED))) {
                    try {
                        if (trigger == null)
                            if (isCallable)
                                taskResult = ((Callable<T>) task).call();
                            else
                                ((Runnable) task).run();
                        else {
                            ZonedDateTime startTime = ZonedDateTime.now(taskScheduledTime.getZone());

                            if (isCallable)
                                taskResult = ((Callable<T>) task).call();
                            else
                                ((Runnable) task).run();

                            ZonedDateTime endTime = ZonedDateTime.now(taskScheduledTime.getZone());

                            Map<String, String> execProps = threadContextDescriptor.getExecutionProperties();
                            String identityName;
                            if (managedExecSvc.eeVersion < 9) {
                                identityName = execProps.get("javax.enterprise.concurrent.IDENTITY_NAME");
                                if (identityName == null)
                                    identityName = execProps.get("jakarta.enterprise.concurrent.IDENTITY_NAME");
                            } else {
                                identityName = execProps.get("jakarta.enterprise.concurrent.IDENTITY_NAME");
                                if (identityName == null)
                                    identityName = execProps.get("javax.enterprise.concurrent.IDENTITY_NAME");
                            }

                            lastExecution = new LastExecutionImpl(identityName, nextExecutionTime, startTime, endTime, taskResult);
                        }
                    } catch (Throwable x) {
                        Tr.error(tc, "CWWKC1101.task.failed", getName(), managedExecSvc.name, x);
                        status = result.getStatus();
                        if (status.type == Status.Type.CANCELED) // include the failure in the result so it will be available to taskDone
                            result.compareAndSet(status, Status.of(Status.Type.CANCELED, x));
                        else if (status.type == Status.Type.STARTED)
                            result.compareAndSet(status, Status.of(Status.Type.DONE, x));
                        result.latch.countDown();
                    }

                    status = result.getStatus();
                    if (status.type == Status.Type.STARTED) {
                        // calculate next execution
                        if (trigger == null)
                            result.compareAndSet(status, Status.done(taskResult, fixedDelay == null && fixedRate == null));
                        else {
                            computedNextExecution = triggerSvc.getNextRunTime(lastExecution, taskScheduledTime, trigger);

                            result.compareAndSet(status, Status.done(taskResult, computedNextExecution == null));
                        }

                        result.latch.countDown();
                    }

                    done = true;

                    if (listener != null)
                        try {
                            if (status.type == Status.Type.CANCELED)
                                try {
                                    CancellationException cancelX = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
                                    if (trace && tc.isEventEnabled())
                                        Tr.event(this, tc, "taskCanceled", managedExecSvc, task, cancelX);
                                    listener.taskAborted(future, managedExecSvc, task, cancelX);
                                } catch (Throwable x) {
                                    Tr.error(tc, "CWWKC1102.listener.failed", getName(), managedExecSvc.name, x);
                                }

                            Throwable failure = result.getStatus().failure;
                            if (trace && tc.isEventEnabled())
                                Tr.event(this, tc, "taskDone", managedExecSvc, task, failure);
                            listener.taskDone(future, managedExecSvc, task, failure);
                        } catch (Throwable x) {
                            Tr.error(tc, "CWWKC1102.listener.failed", getName(), managedExecSvc.name, x);
                        }
                }
            } else {
                // Skip this execution (only possible if using a trigger)
                try {
                    status = result.getStatus();
                    if (status.type == Status.Type.SUBMITTED)
                        result.compareAndSet(status, skipped);

                    // calculate next execution
                    computedNextExecution = triggerSvc.getNextRunTime(lastExecution, taskScheduledTime, trigger);

                    // No next execution
                    if (computedNextExecution == null)
                        result.compareAndSet(skipped, skipped.withoutNextExecution());
                } finally {
                    result.latch.countDown();

                    // notify listener: taskAborted
                    if (listener != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            Tr.event(this, tc, "taskAborted(skipped)", managedExecSvc, task);
                        listener.taskAborted(future, managedExecSvc, task, new SkippedException(skipped.failure));
                    }
                }

                // notify listener: taskDone
                if (listener != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(this, tc, "taskDone(skipped)", managedExecSvc, task);
                    listener.taskDone(future, managedExecSvc, task, null);
                }
            }

            // Resubmit this task to run at the next scheduled time
            status = result.getStatus();
            Result nextResult;
            if (!status.isFinalExecutionComplete()
                && (status.type == Status.Type.DONE || status.type == Status.Type.SKIPPED)
                && result.compareAndSet(status, status.withNextExecution())
                && resultRef.compareAndSet(result, nextResult = new Result())) {
                result = nextResult;
                done = false;
                if (trace && tc.isEventEnabled())
                    Tr.event(this, tc, (status.type == Status.Type.DONE ? "DONE" : "SKIPPED") + "-->NONE (reset for next result)");

                // compute the delay and estimate the next execution time
                Duration delay;
                ZonedDateTime now = ZonedDateTime.now(taskScheduledTime.getZone());
                if (fixedDelay != null) {
                    delay = Duration.of(fixedDelay, unit);
                    nextExecutionTime = now.plus(delay);
                } else if (fixedRate != null) {
                    // Optimistic approach: assume no overlap and just add the fixed rate
                    ZonedDateTime newExecutionTime = nextExecutionTime.plus(fixedRate, unit);
                    Duration newDelay = Duration.between(now, newExecutionTime);
                    if (newDelay.compareTo(Duration.ZERO) > 0) {
                        nextExecutionTime = newExecutionTime;
                        delay = newDelay;
                    } else { // overlapped what would have been the next execution
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "overlapped next fixed-rate execution, computing next from",
                                     taskScheduledTime + " scheduleAtFixedRate invoked at",
                                     nextExecutionTime + " current execution was expected at",
                                     newExecutionTime + " target for subsequent execution overlapped by " + newDelay.negated(),
                                     now + " current time",
                                     Duration.between(taskScheduledTime, now) + " elapsed from scheduleAtFixedRate",
                                     initialDelay + " " + unit + " initial delay");

                        // Time elapsed from when the task should have started for the first time
                        Duration elapsed = Duration.between(taskScheduledTime, now).minus(initialDelay, unit);
                        Duration rate = Duration.of(fixedRate, unit);
                        long count = divide(elapsed, rate); // elapsed.dividedBy(rate); is not available in Java 8
                        delay = rate.multipliedBy(count + 1).minus(elapsed);
                        nextExecutionTime = now.plus(delay);

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "next fixed-rate execution computed as",
                                     elapsed + " elapsed from expected start of first execution",
                                     fixedRate + " " + unit + " fixed rate is " + rate,
                                     (count + 1) + " executions would have occurred if no overlaps or slowdowns",
                                     delay + " delay until next execution",
                                     nextExecutionTime + " expected next execution");
                    }
                } else {
                    delay = Duration.between(now, computedNextExecution);
                    nextExecutionTime = computedNextExecution;
                }

                if (delay.isNegative())
                    delay = Duration.ZERO;

                // notify listener: taskSubmitted
                if (listener != null)
                    try {
                        result.executionThread = Thread.currentThread();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            Tr.event(this, tc, "taskSubmitted", managedExecSvc, task);
                        listener.taskSubmitted(future, managedExecSvc, task);
                    } finally {
                        result.executionThread = null;
                    }

                // reschedule the task if the listener didn't cancel it
                status = result.getStatus();
                if (status.type == Status.Type.NONE && result.compareAndSet(status, Status.of(Status.Type.SUBMITTED))) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "reschedule " + delay + " from now");
                    ScheduledFuture<?> scheduledFuture = managedExecSvc.scheduledExecSvc.schedule(this, delay.toNanos(), TimeUnit.NANOSECONDS);
                    future.scheduledFutureRef.set(scheduledFuture);
                }
            }
        } catch (Throwable x) {
            // Some cases where this can happen:
            // component that scheduled the task is no longer available
            // listener.taskAborted (for skipped task) fails
            // trigger.getNextRunTime (for reschedule after skip) fails
            // listener.taskStarting fails
            // trigger.getNextRunTime (for reschedule after success) fails
            // taskAborted or taskDone fails
            // Liberty scheduled executor unavailable, or it fails to schedule

            if (contextAppliedToThread == null && x instanceof IllegalStateException && FrameworkState.isStopping()) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Task not started due to server shutdown", getName(), x);
            } else
                Tr.error(tc, "CWWKC1101.task.failed", getName(), managedExecSvc.name, x);

            status = result.getStatus();
            if (status.hasNext == null) {
                Status.Type abortedOrDone = status.type == Status.Type.STARTED ? Status.Type.DONE : Status.Type.ABORTED;
                result.compareAndSet(status, Status.of(abortedOrDone, x));
            }
            result.latch.countDown();

            if (listener != null && !done)
                try {
                    try {
                        if (skipped == null && status.type != Status.Type.STARTED) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                Tr.event(this, tc, "taskAborted", managedExecSvc);
                            listener.taskAborted(future, managedExecSvc, task, new AbortedException(x));
                        }
                    } finally {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            Tr.event(this, tc, "taskDone", managedExecSvc, task, x);
                        listener.taskDone(future, managedExecSvc, task, x);
                    }
                } catch (Throwable t) {
                    // Log message, but otherwise ignore because we want to the original failure to be raised
                    Tr.error(tc, "CWWKC1102.listener.failed", getName(), managedExecSvc.name, x);
                }

            if (x instanceof Exception)
                throw (Exception) x;
            else
                throw (Error) x;
        } finally {
            resultForThisExecution.executionThread = null;
            if (contextAppliedToThread != null)
                threadContextDescriptor.taskStopping(contextAppliedToThread);
        }

        return taskResult;
    }

    /**
     * Divide durations to work around the lack of Duration.divideBy in Java 8.
     *
     * @param numerator
     * @param denominator
     * @return quotient
     */
    @Trivial
    private static final long divide(Duration numerator, Duration denominator) {
        BigInteger num = BigInteger.valueOf(numerator.getSeconds()) //
                        .multiply(BigInteger.valueOf(1000000000l)) //
                        .add(BigInteger.valueOf(numerator.getNano()));

        BigInteger denom = BigInteger.valueOf(denominator.getSeconds()) //
                        .multiply(BigInteger.valueOf(1000000000l)) //
                        .add(BigInteger.valueOf(denominator.getNano()));

        return num.divide(denom).longValueExact();
    }

    /**
     * Returns a custom executor upon which to run the task.
     * We use this when virtual=true to direct the task to a new new virtual thread.
     * Otherwise, the null value that is returned when virtual=false means to use the Liberty thread pool.
     */
    @Override
    @Trivial
    public Executor getExecutor() {
        return managedExecSvc.policyExecutor.getVirtualThreadExecutor(); // null if virtual=false
    }

    /**
     * Returns the task name.
     *
     * @return the task name.
     */
    @Trivial
    final String getName() {
        Map<String, String> execProps = threadContextDescriptor.getExecutionProperties();
        String taskName = null;
        if (execProps != null)
            if (managedExecSvc.eeVersion < 9) {
                taskName = execProps.get("javax.enterprise.concurrent.IDENTITY_NAME");
                if (taskName == null)
                    taskName = execProps.get("jakarta.enterprise.concurrent.IDENTITY_NAME");
            } else {
                taskName = execProps.get("jakarta.enterprise.concurrent.IDENTITY_NAME");
                if (taskName == null)
                    taskName = execProps.get("javax.enterprise.concurrent.IDENTITY_NAME");
            }
        return taskName == null ? task.toString() : taskName;
    }

    /**
     * Workaround for Java 8 lacking TimeUnit.toChronoUnit()
     *
     * @param timeunit
     * @return ChronoUnit
     */
    @Trivial
    private static final ChronoUnit toChronoUnit(TimeUnit timeunit) {
        switch (timeunit) {
            case DAYS:
                return ChronoUnit.DAYS;
            case HOURS:
                return ChronoUnit.HOURS;
            case MINUTES:
                return ChronoUnit.MINUTES;
            case SECONDS:
                return ChronoUnit.SECONDS;
            case MILLISECONDS:
                return ChronoUnit.MILLIS;
            case MICROSECONDS:
                return ChronoUnit.MICROS;
            case NANOSECONDS:
                return ChronoUnit.NANOS;
            default:
                throw new IllegalArgumentException(timeunit.toString());
        }
    }

    /**
     * Future for this scheduled task.
     *
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    @Trivial
    private class FutureImpl implements ScheduledFuture<T> {
        /**
         * Future for the scheduled runnable that submits the task for execution. This is useful to cancel the next execution attempt
         * when the task is canceled. There is a window where the future might not be available yet,
         * in which case the next execution will be attempted, but should immediately recognize
         * that it has been canceled and not execute.
         */
        private final AtomicReference<ScheduledFuture<?>> scheduledFutureRef = new AtomicReference<ScheduledFuture<?>>();

        /**
         * Reference to the task.
         */
        private final ScheduledTask<T> task = ScheduledTask.this;

        /**
         * @see java.util.concurrent.Future#cancel(boolean)
         */
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(task, tc, "cancel", mayInterruptIfRunning);

            boolean canceled = false;

            Result result = resultRef.get();
            Status<T> status = result.getStatus();
            while (!canceled && !status.isFinalExecutionComplete()) {
                if (!Boolean.TRUE.equals(status.hasNext) && result.compareAndSet(status, Status.of(Status.Type.CANCELED))) {
                    // Cancel the callable that is scheduled to submit the task for execution.
                    Future<?> future = scheduledFutureRef.get();
                    if (future != null)
                        future.cancel(mayInterruptIfRunning);

                    result.latch.countDown();

                    // Notify the task listener if the previous state was NONE or SUBMITTED,
                    // otherwise the task is in progress and should notify the task listener itself.
                    if ((status.type == Status.Type.NONE || status.type == Status.Type.SUBMITTED) && listener != null) {
                        Throwable failure = null;
                        ThreadContext tranContextRestorer = managedExecSvc.suspendTransaction();
                        try {
                            try {
                                Throwable cancelX = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
                                if (trace && tc.isEventEnabled())
                                    Tr.event(this, tc, "taskAborted", managedExecSvc, task.task, cancelX);
                                listener.taskAborted(this, managedExecSvc, task.task, cancelX);
                            } catch (Error x) {
                                failure = x;
                                throw x;
                            } catch (RuntimeException x) {
                                failure = x;
                                throw x;
                            } finally {
                                if (trace && tc.isEventEnabled())
                                    Tr.event(this, tc, "taskDone", managedExecSvc, task.task, failure);
                                listener.taskDone(this, managedExecSvc, task.task, failure);
                            }
                        } finally {
                            if (tranContextRestorer != null)
                                tranContextRestorer.taskStopping();
                        }
                    }

                    canceled = true;
                } else {
                    // Let pending work (such as reschedule) on other threads go first, and then refresh status
                    Thread.yield();
                    result = resultRef.get();
                    status = result.getStatus();
                }
            }

            if (trace && tc.isEntryEnabled())
                Tr.exit(task, tc, "cancel", canceled);
            return canceled;
        }

        /**
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(Delayed delayed) {
            // We can't implement compareTo in a valid way
            // - We don't have control over all Delayed implementations
            // - delayed.getDelay() is expected to change over time, significantly so if periodic
            // so just match what we observe Java executor implementations doing (-1, 0, 1).
            int result;
            if (delayed instanceof ScheduledTask.FutureImpl) { // avoid checking current time if possible
                ZonedDateTime value1 = nextExecutionTime;
                @SuppressWarnings("unchecked")
                ZonedDateTime value2 = ((FutureImpl) delayed).task.nextExecutionTime;
                result = this == delayed || value1 == value2 ? 0 : value1.compareTo(value2);
                result = result < 0 ? -1 : result > 0 ? 1 : 0;
            } else {
                long diff = getDelay(TimeUnit.MILLISECONDS) - delayed.getDelay(TimeUnit.MILLISECONDS);
                // Because getDelay() compares with the current time, which will be slightly different between
                // invocations on this and the other delayed instance, we are limiting precision to a tenth of a second.
                result = diff < -100 ? -1 : diff > 100 ? 1 : 0;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(task, tc, "compareTo", this, delayed, result);
            return result;
        }

        // Java 19+
        public Throwable exceptionNow() {
            Result result = resultRef.get();
            Status<T> status = result.getStatus();

            switch (status.type) {
                case DONE:
                    if (status.failure == null)
                        throw new IllegalStateException("SUCCESS"); // Future.State.SUCCESS in Java 19+
                    else
                        return status.failure;
                case ABORTED:
                    throw new IllegalStateException(new AbortedException(status.failure));
                case CANCELED:
                    throw new IllegalStateException(new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name)));
                case NONE:
                case SUBMITTED:
                case STARTED:
                    throw new IllegalStateException();
                case SKIPPED:
                    throw new IllegalStateException(new SkippedException(status.failure));
                default: // should be unreachable
                    throw new IllegalStateException(status.type.toString());
            }
        }

        /**
         * @see java.util.concurrent.FutureTask#get()
         */
        @Override
        public T get() throws ExecutionException, InterruptedException {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(task, tc, "get");

            Result result = resultRef.get();
            Status<T> status = result.getStatus();

            Exception x = null;

            if (status.type == Status.Type.CANCELED)
                x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
            else if ((status.type == Status.Type.NONE || status.type == Status.Type.SUBMITTED)
                     && result.executionThread != null && Thread.currentThread().equals(result.executionThread))
                // We do not permit Future.get on taskSubmitted/taskStarting because the thread that invokes
                // the listener method does not submit/start the task until the listener method returns.
                x = new InterruptedException(Tr.formatMessage(tc, "CWWKC1120.future.get.rejected"));
            else {
                result.latch.await();

                status = result.getStatus();
                switch (status.type) {
                    case DONE:
                        if (status.failure == null) {
                            if (trace && tc.isEntryEnabled())
                                Tr.exit(task, tc, "get", status.value);
                            return status.value;
                        } else
                            x = new ExecutionException(status.failure);
                        break;
                    case ABORTED:
                        x = new AbortedException(status.failure);
                        break;
                    case CANCELED:
                        x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
                        break;
                    case SKIPPED:
                        x = new SkippedException(status.failure);
                        break;
                    default:
                        x = new IllegalStateException(status.type.name());
                }
            }

            if (trace && tc.isEntryEnabled())
                Tr.exit(task, tc, "get", Utils.toString(x));
            if (x instanceof ExecutionException)
                throw (ExecutionException) x;
            else if (x instanceof InterruptedException)
                throw (InterruptedException) x;
            else
                throw (RuntimeException) x;
        }

        /**
         * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
         */
        @Override
        public T get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(task, tc, "get", timeout, unit);

            Result result = resultRef.get();
            Status<T> status = result.getStatus();

            Exception x = null;

            if (status.type == Status.Type.CANCELED)
                x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
            else if ((status.type == Status.Type.NONE || status.type == Status.Type.SUBMITTED)
                     && result.executionThread != null && Thread.currentThread().equals(result.executionThread))
                // We do not permit Future.get on taskSubmitted/taskStarting because the thread that invokes
                // the listener method does not submit/start the task until the listener method returns.
                x = new InterruptedException(Tr.formatMessage(tc, "CWWKC1120.future.get.rejected"));
            else if (!result.latch.await(timeout, unit))
                x = new TimeoutException();
            else {
                status = result.getStatus();
                switch (status.type) {
                    case DONE:
                        if (status.failure == null) {
                            if (trace && tc.isEntryEnabled())
                                Tr.exit(task, tc, "get", status.value);
                            return status.value;
                        } else
                            x = new ExecutionException(status.failure);
                        break;
                    case ABORTED:
                        x = new AbortedException(status.failure);
                        break;
                    case CANCELED:
                        x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
                        break;
                    case SKIPPED:
                        x = new SkippedException(status.failure);
                        break;
                    default:
                        x = new IllegalStateException(status.type.name());
                }
            }

            if (trace && tc.isEntryEnabled())
                Tr.exit(task, tc, "get", Utils.toString(x));
            if (x instanceof ExecutionException)
                throw (ExecutionException) x;
            else if (x instanceof InterruptedException)
                throw (InterruptedException) x;
            else if (x instanceof TimeoutException)
                throw (TimeoutException) x;
            else
                throw (RuntimeException) x;
        }

        /**
         * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
         */
        @Override
        public long getDelay(TimeUnit unit) {
            long delay = ZonedDateTime.now(taskScheduledTime.getZone()).until(nextExecutionTime, toChronoUnit(unit));

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(task, tc, "getDelay", unit, delay);
            return delay;
        }

        /**
         * @see java.lang.Object#hashCode(int)
         */
        @Override
        public final int hashCode() {
            return task.hashCode(); // Use the hash code of the containing class to make debug easier
        }

        /**
         * @see java.util.concurrent.Future#isCancelled()
         */
        @Override
        public boolean isCancelled() {
            Status<T> status = resultRef.get().getStatus();
            if (status.type == Status.Type.CANCELED)
                return true;
            else if (status.isFinalExecutionComplete())
                return false;

            Future<?> future = scheduledFutureRef.get();
            if (future != null && future.isCancelled()) {
                future.cancel(true);
                return true;
            }
            return false;
        }

        /**
         * @see java.util.concurrent.Future#isDone()
         */
        @Override
        public boolean isDone() {
            Status<T> status = resultRef.get().getStatus();
            return status.type == Status.Type.ABORTED
                   || status.type == Status.Type.DONE
                   || status.type == Status.Type.SKIPPED
                   || isCancelled();
        }

        // Java 19+
        public T resultNow() {
            Result result = resultRef.get();
            Status<T> status = result.getStatus();

            switch (status.type) {
                case DONE:
                    if (status.failure == null)
                        return status.value;
                    else
                        throw new IllegalStateException(status.failure);
                case ABORTED:
                    throw new IllegalStateException(new AbortedException(status.failure));
                case CANCELED:
                    throw new IllegalStateException(new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name)));
                case NONE:
                case SUBMITTED:
                case STARTED:
                    throw new IllegalStateException();
                case SKIPPED:
                    throw new IllegalStateException(new SkippedException(status.failure));
                default: // should be unreachable
                    throw new IllegalStateException(status.type.toString());
            }
        }
    }
}