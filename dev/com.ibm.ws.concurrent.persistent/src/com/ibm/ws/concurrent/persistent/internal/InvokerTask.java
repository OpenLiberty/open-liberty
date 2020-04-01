/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.internal;

import java.io.IOException;
import java.security.AccessController;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.persistent.ejb.TaskLocker;
import com.ibm.ws.concurrent.persistent.serializable.TaskFailure;
import com.ibm.ws.concurrent.persistent.serializable.TaskSkipped;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.concurrent.persistent.TaskRecord;
import com.ibm.wsspi.concurrent.persistent.TaskStore;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Invokes a persistent task that has been scheduled to run on the local instance in the near future.
 * An instance of this class can also be registered as a Synchronization with a transaction in order to
 * automatically submit/schedule for execution in the near future after the transaction commits.
 */
public class InvokerTask implements Runnable, Synchronization {
    private static final TraceComponent tc = Tr.register(InvokerTask.class);
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    /**
     * Array position 0 tracks the ID of the task that is currently running on the thread.
     * Array position 1 tracks whether the task is known to have removed itself. This is not detected for pattern-based remove/cancel.
     */
    public final static ThreadLocal<long[]> runningTaskState = new ThreadLocal<long[]>();
    final static long REMOVED_BY_SELF = 1;

    private final static int DEFAULT_TIMEOUT_FOR_SUSPENDED_TRAN = 1800; // 30 minutes

    private final short binaryFlags;
    private long expectedExecTime;
    private final PersistentExecutorImpl persistentExecutor;
    final long taskId;
    private final int txTimeout;

    InvokerTask(PersistentExecutorImpl persistentExecutor, long taskId, long expectedExecTime, short binaryFlags, int txTimeout) {
        this.persistentExecutor = persistentExecutor;
        this.taskId = taskId;
        this.expectedExecTime = expectedExecTime;
        this.binaryFlags = binaryFlags;
        this.txTimeout = txTimeout;
    }

    /**
     * Upon successful transaction commit, automatically schedules a task for execution in the near future.
     *
     * @see javax.transaction.Synchronization#afterCompletion(int)
     */
    @Override
    public void afterCompletion(int status) {
        if (status == Status.STATUS_COMMITTED) {
            Boolean previous = persistentExecutor.inMemoryTaskIds.put(taskId, Boolean.TRUE);
            if (previous == null) {
                long delay = expectedExecTime - new Date().getTime();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Schedule " + taskId + " for " + delay + "ms from now");
                persistentExecutor.scheduledExecutor.schedule(this, delay, TimeUnit.MILLISECONDS);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Found task " + taskId + " already scheduled");
            }
        }
    }

    @Override
    @Trivial
    public void beforeCompletion() {
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InvokerTask
               && taskId == ((InvokerTask) other).taskId
               && persistentExecutor == ((InvokerTask) other).persistentExecutor;
    }

    @Override
    public int hashCode() {
        return (int) taskId;
    }

    /**
     * In a new transaction, updates the database with the new failure count (or autopurges the task).
     * The first failure should always be retried immediately.
     * For subsequent failures, check the retryLimit and retryInterval to determine if we should
     * retry, and how long we should wait before doing so.
     *
     * @param failure                 failure of the task itself or of processing related to the task, such as Trigger.getNextRunTime
     * @param loader                  class loader that can load the task and any exceptions that it might raise
     * @param consecutiveFailureCount number of consecutive task failures
     * @param config                  snapshot of persistent executor configuration
     * @param taskName                identity name for the task
     */
    private void processRetryableTaskFailure(Throwable failure, ClassLoader loader, short consecutiveFailureCount, Config config, String taskName) {
        taskName = taskName == null || taskName.length() == 0 || taskName.length() == 1 && taskName.charAt(0) == ' ' ? String.valueOf(taskId) // empty task name
                        : taskId + " (" + taskName + ")";
        TaskStore taskStore = persistentExecutor.taskStore;
        TransactionManager tranMgr = persistentExecutor.tranMgrRef.getServiceWithException();
        boolean retry = false;

        try {
            Throwable failed = null;
            // Auto purge if we reached the failure limit and auto purge is enabled
            if (config.retryLimit >= 0
                && consecutiveFailureCount > config.retryLimit
                && (binaryFlags & TaskRecord.Flags.AUTO_PURGE_ALWAYS.bit) != 0) {

                if (failure == null)
                    Tr.warning(tc, "CWWKC1510.retry.limit.reached.rollback", persistentExecutor.name, taskName, consecutiveFailureCount);
                else
                    Tr.warning(tc, "CWWKC1511.retry.limit.reached.failed", persistentExecutor.name, taskName, consecutiveFailureCount, failure);

                tranMgr.begin();
                try {
                    taskStore.remove(taskId, null, false);
                } catch (Throwable x) {
                    failed = x;
                } finally {
                    if (failed == null)
                        tranMgr.commit();
                    else
                        tranMgr.rollback();
                }
            } else {
                // Update database with new count
                tranMgr.begin();
                try {
                    if (config.retryLimit < 0 || consecutiveFailureCount <= config.retryLimit)
                        consecutiveFailureCount = taskStore.incrementFailureCount(taskId);

                    if (config.retryLimit >= 0 && consecutiveFailureCount > config.retryLimit) {
                        if (failure == null)
                            Tr.warning(tc, "CWWKC1510.retry.limit.reached.rollback", persistentExecutor.name, taskName, consecutiveFailureCount);
                        else
                            Tr.warning(tc, "CWWKC1511.retry.limit.reached.failed", persistentExecutor.name, taskName, consecutiveFailureCount, failure);

                        TaskFailure taskFailure = new TaskFailure(failure, failure == null ? null : loader, persistentExecutor, TaskFailure.FAILURE_LIMIT_REACHED, Short
                                        .toString(consecutiveFailureCount));
                        // Update database with the result and state if we reached the limit
                        TaskRecord updates = new TaskRecord(false);
                        updates.setConsecutiveFailureCount(consecutiveFailureCount);
                        updates.setResult(persistentExecutor.serialize(taskFailure));
                        updates.setState((short) (TaskState.ENDED.bit | TaskState.FAILURE_LIMIT_REACHED.bit));
                        if (config.missedTaskThreshold > 0)
                            updates.setClaimExpiryOrPartition(-1); // immediately allow another server to claim the task
                        TaskRecord expected = new TaskRecord(false);
                        expected.setId(taskId);
                        taskStore.persist(updates, expected);
                    } else {
                        // -1 indicates the task is no longer in the persistent store
                        retry = consecutiveFailureCount != -1;

                        if (retry && config.missedTaskThreshold == -1) {
                            String seconds = consecutiveFailureCount == 1 || config.retryInterval == 0L ? "0" : NumberFormat.getInstance().format(config.retryInterval / 1000.0);
                            if (failure == null)
                                Tr.warning(tc, "CWWKC1500.task.rollback.retry", persistentExecutor.name, taskName, seconds);
                            else
                                Tr.warning(tc, "CWWKC1501.task.failure.retry", persistentExecutor.name, taskName, failure, seconds);
                        } else {
                            if (failure == null)
                                Tr.warning(tc, "CWWKC1502.task.rollback", persistentExecutor.name, taskName);
                            else
                                Tr.warning(tc, "CWWKC1503.task.failure", persistentExecutor.name, taskName, failure);
                        }
                    }
                } catch (Throwable x) {
                    failed = x;
                    retry = true;
                } finally {
                    if (failed == null)
                        tranMgr.commit();
                    else
                        tranMgr.rollback();
                }
            }
        } catch (Throwable x) {
            retry = true;
        }

        if (retry && config.missedTaskThreshold == -1) {
            // Retry the first failure immediately when fail over is disabled
            if (consecutiveFailureCount == 1 && config.missedTaskThreshold < 0 || config.retryInterval == 0L)
                persistentExecutor.scheduledExecutor.submit(this);
            else {
                long delay = config.retryInterval;
                persistentExecutor.scheduledExecutor.schedule(this, delay, TimeUnit.MILLISECONDS);
            }
        } else {
            persistentExecutor.inMemoryTaskIds.remove(taskId);
        }
    }

    /**
     * Executes the task on a thread from the common Liberty thread pool.
     */
    @FFDCIgnore(RollbackException.class)
    @Override
    public void run() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "run[" + taskId + ']', persistentExecutor);

        Config config = persistentExecutor.configRef.get();
        if (persistentExecutor.deactivated || !config.enableTaskExecution) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run[" + taskId + ']', persistentExecutor.deactivated ? "deactivated" : ("enableTaskExecution? " + config.enableTaskExecution));
            return;
        }

        // Work around for when the scheduled executor fires too early
        long execTime = new Date().getTime();
        if (execTime < expectedExecTime) {
            long delay = expectedExecTime - execTime;
            persistentExecutor.scheduledExecutor.schedule(this, delay, TimeUnit.MILLISECONDS);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run[" + taskId + ']', "attempted to run " + delay + " ms too early. Rescheduled.");
            return;
        }

        // If a Configuration update is in progress, then we will defer the execution of this Task until after.
        if (persistentExecutor.deferExecutionForConfigUpdate(this) == true) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run[" + taskId + ']', "attempted to run during a configuration update.");
            return;
        }

        String taskName = null;
        String taskIdForPropTable = null;
        Long partitionId;
        TaskLocker ejbSingletonLockCollaborator = null;
        String ownerForDeferredTask = null;
        ClassLoader loader = null;
        Throwable failure = null;
        Short prevFailureCount = null, nextFailureCount = null;
        Long nextExecTime = null;
        boolean claimNextExecution = false;
        TaskStore taskStore = persistentExecutor.taskStore;
        ApplicationTracker appTracker = persistentExecutor.appTrackerRef.getServiceWithException();
        TransactionManager tranMgr = persistentExecutor.tranMgrRef.getServiceWithException();
        long[] runningTaskRemovalState = new long[] { taskId, 0 };
        runningTaskState.set(runningTaskRemovalState);
        try {
            partitionId = config.missedTaskThreshold < 1 ? persistentExecutor.getPartitionId() : null;

            int timeout = txTimeout == 0 && (binaryFlags & TaskRecord.Flags.SUSPEND_TRAN_OF_EXECUTOR_THREAD.bit) != 0 ? DEFAULT_TIMEOUT_FOR_SUSPENDED_TRAN : txTimeout;
            tranMgr.setTransactionTimeout(timeout);

            TaskRecord ejbSingletonRecord = null;
            if ((binaryFlags & TaskRecord.Flags.EJB_SINGLETON.bit) != 0 && (binaryFlags & TaskRecord.Flags.SUSPEND_TRAN_OF_EXECUTOR_THREAD.bit) == 0) {
                tranMgr.begin();
                ejbSingletonRecord = taskStore.getTrigger(taskId);
                tranMgr.commit();
            }

            tranMgr.begin();
            long tranBeginNS = System.nanoTime();
            TaskRecord taskRecord;

            // Execution property TRANSACTION=SUSPEND indicates the task should not run in the persistent executor transaction.
            // Lock an entry in a different table to prevent concurrent execution, and run with that transaction suspended.
            if ((binaryFlags & TaskRecord.Flags.SUSPEND_TRAN_OF_EXECUTOR_THREAD.bit) != 0) {
                if (!taskStore.createProperty(taskIdForPropTable = "{" + taskId + "}", " ")) {
                    if (config.missedTaskThreshold > 0)
                        throw new RuntimeException("An attempt to run the task might have been made by a different instance. Retry is needed.");
                    // Determine the partition to which the task is assigned
                    taskIdForPropTable = null;
                    tranMgr.rollback(); // PostgreSQL will not permit any further operations after a duplicate key exception
                    tranMgr.begin();
                    Long assignedTo = taskStore.getPartition(taskId);
                    if (assignedTo != null && assignedTo.equals(partitionId)) {
                        throw new RuntimeException("An attempt to run the task might have been made by a different instance. Retry is needed.");
                    } else { // the task is no longer assigned to the partition id for this executor instance
                        if (trace && tc.isEntryEnabled())
                            Tr.exit(this, tc, "run[" + taskId + ']', "task is assigned to partition " + assignedTo + ", not " + partitionId);
                    }
                }
                Transaction suspendedTran = tranMgr.suspend();
                try {
                    // We still need the task information, but get it in a new transaction that we can commit right away.
                    Throwable failed = null;
                    tranMgr.begin();
                    try {
                        taskRecord = taskStore.find(taskId,
                                                    partitionId,
                                                    System.currentTimeMillis(),
                                                    false);
                    } catch (Throwable x) {
                        throw failed = x;
                    } finally {
                        if (failed == null)
                            tranMgr.commit();
                        else
                            tranMgr.rollback();
                    }
                } finally {
                    tranMgr.resume(suspendedTran); // will be suspended again by application of transaction context
                }
            } else {
                if (ejbSingletonRecord != null) {
                    String owner = ejbSingletonRecord.getIdentifierOfOwner();
                    if (!appTracker.isStarted(owner)) {
                        ownerForDeferredTask = owner;
                        if (trace && tc.isEntryEnabled())
                            Tr.exit(this, tc, "run[" + taskId + ']', "unavailable - deferred");
                        return; // Ignore, we are deferring the task because the application or module is unavailable
                    }

                    byte[] bytes = ejbSingletonRecord.getTrigger();
                    ejbSingletonLockCollaborator = (TaskLocker) persistentExecutor.deserialize(bytes, priv.getSystemClassLoader());
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "notify EJB container to lock singleton");
                    ejbSingletonLockCollaborator.lock();
                }

                taskRecord = taskStore.find(taskId,
                                            partitionId,
                                            System.currentTimeMillis(),
                                            true);
            }

            if (taskRecord == null || (taskRecord.getState() & TaskState.ENDED.bit) != 0) {
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "run[" + taskId + ']', "not appropriate to run task at this time");
                return; // Ignore, because the task was canceled or has been assigned to someone else or someone else already ran it
            }

            taskName = taskRecord.getName();
            prevFailureCount = taskRecord.getConsecutiveFailureCount();

            String classLoaderIdentifier = taskRecord.getIdentifierOfClassLoader();
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "classloader identifier", classLoaderIdentifier);
            loader = ejbSingletonRecord == null ? persistentExecutor.classloaderIdSvc.getClassLoader(classLoaderIdentifier) : priv.getSystemClassLoader();
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "classloader", loader);

            String owner = taskRecord.getIdentifierOfOwner();
            if (loader == null || !appTracker.isStarted(owner)) {
                ownerForDeferredTask = owner;
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "run[" + taskId + ']', "unavailable - deferred");
                return; // Ignore, we are deferring the task because the application or module is unavailable
            }

            TaskInfo info = (TaskInfo) persistentExecutor.deserialize(taskRecord.getTaskInformation(), null);

            byte[] triggerBytes = taskRecord.getTrigger();
            Trigger trigger = triggerBytes == null ? null : ejbSingletonRecord != null
                                                            && Arrays.equals(triggerBytes,
                                                                             ejbSingletonRecord.getTrigger()) ? ejbSingletonLockCollaborator : (Trigger) persistentExecutor
                                                                                             .deserialize(triggerBytes,
                                                                                                          loader);
            if (trigger == null) {
                String triggerClassName = info.getClassNameForNonSerializableTrigger();
                if (triggerClassName != null)
                    trigger = (Trigger) loader.loadClass(triggerClassName).newInstance();
            }

            byte[] taskBytes = taskRecord.getTask();
            Object task = taskBytes == null ? null : persistentExecutor.deserialize(taskBytes, loader);
            if (task == null) {
                String taskClassName = info.getClassNameForNonSerializableTask();
                if (taskClassName == null)
                    task = trigger; // optimization to share single instance for task and trigger
                else
                    task = loader.loadClass(taskClassName).newInstance();
            }

            byte[] resultBytes = taskRecord.getResult();

            boolean skipped = false;
            Throwable skippedX = null;
            LastExecution lastExecution = null;
            long startTime = 0, stopTime = 0;
            Object result = null;

            Map<String, String> execProps = persistentExecutor.getExecutionProperties(task);
            ThreadContextDescriptor threadContext = info.deserializeThreadContext(execProps);
            ArrayList<ThreadContext> contextAppliedToThread = threadContext == null ? null : threadContext.taskStarting();
            try {
                if (trigger != null) {
                    Long prevScheduledStart = taskRecord.getPreviousScheduledStartTime();
                    if (prevScheduledStart != null)
                        lastExecution = new LastExecutionImpl(persistentExecutor, taskId, taskName, resultBytes, taskRecord.getPreviousStopTime(), taskRecord
                                        .getPreviousStartTime(), prevScheduledStart, loader);
                    try {
                        skipped = trigger.skipRun(lastExecution, new Date(taskRecord.getNextExecutionTime()));
                    } catch (RuntimeException x) {
                        skipped = true;
                        skippedX = x;
                    }
                }

                if (skipped) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "skipping task", skippedX);
                    Date nextExecDate = trigger.getNextRunTime(lastExecution, new Date(taskRecord.getOriginalSubmitTime()));
                    nextExecTime = nextExecDate == null ? null : nextExecDate.getTime();
                } else {
                    // Fixed result for one-shot runnable
                    if (!info.isSubmittedAsCallable() && info.getInterval() == -1 && trigger == null && resultBytes != null)
                        result = persistentExecutor.deserialize(resultBytes, loader);

                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "task about to start " + task);

                    startTime = new Date().getTime();
                    try {
                        if (info.isSubmittedAsCallable())
                            result = ((Callable<?>) task).call();
                        else
                            ((Runnable) task).run();

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "task result " + result);
                        nextFailureCount = (short) 0;
                    } catch (Throwable x) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "task failed", x);
                        failure = x;
                        nextFailureCount = (short) ((prevFailureCount < Short.MAX_VALUE) ? (prevFailureCount + 1) : Short.MAX_VALUE);
                        // If we will retry, immediately roll back, then can update persistent store with failure count
                        config = persistentExecutor.configRef.get();
                        if (config.retryLimit == -1 || nextFailureCount <= config.retryLimit)
                            throw failure;
                    } finally {
                        stopTime = new Date().getTime();
                    }

                    // Compute next execution time if the task did not fail.
                    if (failure == null) {
                        long interval = info.getInterval();
                        if (interval == -1) {
                            if (trigger == null) {
                                nextExecTime = null; // one-shot task
                            } else {
                                lastExecution = new LastExecutionImpl(persistentExecutor, taskId, taskName, result, stopTime, startTime, taskRecord.getNextExecutionTime());
                                Date nextExecDate = trigger.getNextRunTime(lastExecution, new Date(taskRecord.getOriginalSubmitTime()));
                                nextExecTime = nextExecDate == null ? null : nextExecDate.getTime();
                            }
                        } else if (info.isFixedRate()) {
                            long originalScheduledStartTime = taskRecord.getOriginalSubmitTime() + info.getInitialDelay();
                            long elapsed = stopTime - originalScheduledStartTime;
                            nextExecTime = (elapsed / interval + 1) * interval + originalScheduledStartTime;
                        } else
                            nextExecTime = stopTime + interval; // fixed-delay
                    }
                }
            } finally {
                if (contextAppliedToThread != null)
                    threadContext.taskStopping(contextAppliedToThread);
            }

            short autoPurgeBit = failure == null ? TaskRecord.Flags.AUTO_PURGE_ON_SUCCESS.bit : TaskRecord.Flags.AUTO_PURGE_ALWAYS.bit;

            if (tranMgr.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                // discontinue if already marked to roll back
                long durationMS = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tranBeginNS);
                if (timeout > 0 && durationMS >= timeout * 1000l) {
                    // The transaction was probably marked for roll back due to the the transaction timing out,
                    // although it could have been marked to roll back even prior to that.
                    String elapsedTime = NumberFormat.getInstance().format(durationMS / 1000.0);
                    if (config.missedTaskThreshold == timeout)
                        throw new RollbackException(Tr.formatMessage(tc, "CWWKC1505.mtt.timeout.rollback", elapsedTime, timeout));
                    else
                        throw new RollbackException(Tr.formatMessage(tc, "CWWKC1504.tx.timeout.rollback", elapsedTime, timeout));
                } else {
                    // The transaction timeout detection above is approximate.
                    // When this code block is reached, it usually means that the transaction was marked to roll back
                    // independently of the transaction timing out. But due to the imprecision, this code path might
                    // some times be reached on the transaction timeout path as well.
                    throw new RollbackException(Tr.formatMessage(tc, "CWWKC1506.marked.rollback.only"));
                }
            } else if ((nextExecTime == null || !skipped && nextFailureCount > 0) && (binaryFlags & autoPurgeBit) != 0) {
                // Autopurge the completed task unless it is known that the task removed/canceled itself during execution
                if (runningTaskRemovalState[1] != InvokerTask.REMOVED_BY_SELF) {
                    taskStore.remove(taskId, null, false);
                    // EJB Persistent Timers requires that we allow in-progress execution of a timer that is
                    // canceled/removed from another thread to commit. This means we cannot issue a rollback
                    // when we find the timer/task to have already been removed. If this ever needed to change,
                    // more work would need to be distinguish self-cancellation/removal for the pattern-based remove operations.
                }
            } else {
                // Update state
                TaskRecord updates = new TaskRecord(false);
                if (nextExecTime != null)
                    updates.setNextExecutionTime(nextExecTime);
                short state = nextExecTime == null ? (short) (TaskState.ENDED.bit | TaskState.SUCCESSFUL.bit) : TaskState.SCHEDULED.bit;
                if (skipped) {
                    state |= TaskState.SKIPPED.bit;
                    if (skippedX != null) {
                        state |= TaskState.SKIPRUN_FAILED.bit;
                        byte[] previousResultBytes = persistentExecutor.serialize(lastExecution == null ? null : lastExecution.getResult());
                        updates.setResult(persistentExecutor.serialize(new TaskSkipped(previousResultBytes, skippedX, loader, persistentExecutor)));
                    }
                } else {
                    updates.setConsecutiveFailureCount(nextFailureCount);
                    updates.setPreviousScheduledStartTime(taskRecord.getNextExecutionTime());
                    updates.setPreviousStartTime(startTime);
                    updates.setPreviousStopTime(stopTime);
                    if (failure == null) {
                        // Only update result blob if it changed
                        byte[] updatedResultBytes = result == null ? null : serializeResult(result, loader);
                        if (updatedResultBytes == null || !Arrays.equals(resultBytes, updatedResultBytes))
                            updates.setResult(updatedResultBytes);
                    } else {
                        updates.setResult(persistentExecutor
                                        .serialize(new TaskFailure(failure, loader, persistentExecutor, TaskFailure.FAILURE_LIMIT_REACHED, Short.toString(nextFailureCount))));
                        state = (short) (TaskState.ENDED.bit | TaskState.FAILURE_LIMIT_REACHED.bit);
                    }
                }
                updates.setState(state);
                // Only update blobs if they have changed
                if (taskBytes != null) {
                    byte[] updatedTaskBytes = persistentExecutor.serialize(task);
                    if (!Arrays.equals(taskBytes, updatedTaskBytes))
                        updates.setTask(updatedTaskBytes);
                }
                if (triggerBytes != null) {
                    byte[] updatedTriggerBytes = persistentExecutor.serialize(trigger);
                    if (!Arrays.equals(triggerBytes, updatedTriggerBytes))
                        updates.setTrigger(updatedTriggerBytes);
                }

                // When updating the task entry, determine whether or not to keep a claim on the task.
                config = persistentExecutor.configRef.get();
                if (config.missedTaskThreshold > 0)
                    if (config.enableTaskExecution
                        && nextExecTime != null
                        && (config.pollInterval < 0 || nextExecTime <= System.currentTimeMillis() + config.pollInterval)) {
                        updates.setClaimExpiryOrPartition(nextExecTime + config.missedTaskThreshold * 1000);
                        claimNextExecution = true;
                    } else {
                        updates.setClaimExpiryOrPartition(-1);
                    }

                TaskRecord expected = new TaskRecord(false);
                expected.setId(taskId);
                expected.setVersion(taskRecord.getVersion());
                boolean updatesPersisted = taskStore.persist(updates, expected);

                if (!updatesPersisted) {
                    // Optimistic update unsuccessful. Need to take into account changes made by the task to itself.
                    TaskRecord taskRecordRefresh = taskStore.findById(taskId, null, false);
                    if (taskRecordRefresh != null) {
                        short refreshedState = taskRecordRefresh.getState();
                        if ((refreshedState & TaskState.CANCELED.bit) != 0
                            && (binaryFlags & TaskRecord.Flags.SUSPEND_TRAN_OF_EXECUTOR_THREAD.bit) == 0) {
                            // task canceled itself, combine the canceled state with other updates
                            updates.setState(state = refreshedState);
                            expected.setVersion(taskRecordRefresh.getVersion());
                            taskStore.persist(updates, expected);
                        } else if ((refreshedState & TaskState.SUSPENDED.bit) != 0) {
                            // task suspended itself, merge the suspended state with new state and combine with other updates
                            state |= TaskState.SUSPENDED.bit;
                            state &= ~TaskState.SCHEDULED.bit;
                            expected.setVersion(taskRecordRefresh.getVersion());
                            taskStore.persist(updates, expected);
                        }
                    } else { // the task was removed, either by itself or another thread
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "task entry is gone",
                                     Arrays.toString(runningTaskRemovalState),
                                     "allows removal by other threads? " + ((binaryFlags & TaskRecord.Flags.SUSPEND_TRAN_OF_EXECUTOR_THREAD.bit) != 0));

                        // EJB Persistent Timers requires that we allow in-progress execution of a timer that is
                        // canceled/removed from another thread to commit. This means we cannot issue a rollback
                        // when we find the timer/task to have already been removed. If this ever needed to change,
                        // more work would need to be distinguish self-cancellation/removal for the pattern-based remove operations.
                    }
                }
            }
        } catch (RollbackException x) {
            if (failure == null)
                failure = x;
        } catch (Throwable x) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "marking transaction to roll back in response to error", x);
            try {
                tranMgr.setRollbackOnly();
            } catch (Throwable t) {
            }
            if (failure == null)
                failure = x;
        } finally {
            if (ejbSingletonLockCollaborator != null)
                ejbSingletonLockCollaborator.unlock();

            runningTaskState.remove();

            try {
                tranMgr.setTransactionTimeout(0); // clear the value so we don't impact subsequent transactions on this thread

                if (tranMgr.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                    if (trace && tc.isEventEnabled())
                        Tr.event(this, tc, "rolling back task execution attempt");
                    if (nextFailureCount == null || nextFailureCount == 0)
                        nextFailureCount = (short) (prevFailureCount == null ? 1 : prevFailureCount < Short.MAX_VALUE ? (prevFailureCount + 1) : Short.MAX_VALUE);
                    tranMgr.rollback();
                    if (config == null)
                        config = persistentExecutor.configRef.get();
                    processRetryableTaskFailure(failure, loader, nextFailureCount, config, taskName);
                } else {
                    if (taskIdForPropTable != null)
                        try {
                            taskStore.removeProperty(taskIdForPropTable);
                        } catch (Throwable x) {
                            tranMgr.rollback();
                            throw x;
                        }

                    tranMgr.commit();

                    // If the transaction commits, immediately reschedule tasks that should run within a single poll cycle. If no polling, then all tasks.
                    config = persistentExecutor.configRef.get();
                    boolean scheduleNextExecution = config.missedTaskThreshold > 0 //
                                    ? claimNextExecution //
                                    : config.enableTaskExecution && nextExecTime != null
                                      && (config.pollInterval < 0
                                          || nextExecTime <= System.currentTimeMillis() + config.pollInterval);
                    if (scheduleNextExecution) {
                        expectedExecTime = nextExecTime;
                        long delay = nextExecTime - System.currentTimeMillis();

                        ScheduledExecutorService executor = persistentExecutor.scheduledExecutor;
                        if (executor == null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(this, tc, "deactivated - reschedule skipped");
                        } else
                            executor.schedule(this, delay, TimeUnit.MILLISECONDS);
                    } else if (ownerForDeferredTask != null)
                        appTracker.deferTask(this, ownerForDeferredTask, persistentExecutor);
                    else {
                        persistentExecutor.inMemoryTaskIds.remove(taskId);
                        if (failure != null) {
                            taskName = taskName == null || taskName.length() == 0 || taskName.length() == 1 && taskName.charAt(0) == ' ' ? String.valueOf(taskId) // empty task name
                                            : taskId + " (" + taskName + ")";
                            Tr.warning(tc, "CWWKC1511.retry.limit.reached.failed", persistentExecutor.name, taskName, nextFailureCount, failure);
                        }
                    }
                }
            } catch (Throwable x) {
                if (failure != null)
                    failure = x;

                // Retry the task if an error occurred
                processRetryableTaskFailure(failure, loader, nextFailureCount, config, taskName);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "run[" + taskId + ']', failure);
    }

    /**
     * Utility method that serializes a task result, or the failure that occurred when attempting
     * to serialize the task result.
     *
     * @param result non-null task result
     * @param loader class loader that can deserialize the task and result.
     * @return serialized bytes
     */
    @FFDCIgnore(Throwable.class)
    @Sensitive
    private byte[] serializeResult(Object result, ClassLoader loader) throws IOException {
        try {
            return persistentExecutor.serialize(result);
        } catch (Throwable x) {
            return persistentExecutor.serialize(new TaskFailure(x, loader, persistentExecutor, TaskFailure.NONSER_RESULT, result.getClass().getName()));
        }
    }
}
