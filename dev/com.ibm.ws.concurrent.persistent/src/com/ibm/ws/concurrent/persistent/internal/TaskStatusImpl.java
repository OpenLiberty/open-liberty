/*******************************************************************************
 * Copyright (c) 2014,2020 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.SkippedException;

import com.ibm.websphere.concurrent.persistent.PersistentStoreException;
import com.ibm.websphere.concurrent.persistent.ResultNotSerializableException;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.persistent.ejb.TimerStatus;
import com.ibm.ws.concurrent.persistent.ejb.TimerTrigger;
import com.ibm.ws.concurrent.persistent.internal.PersistentExecutorImpl.TransactionController;
import com.ibm.ws.concurrent.persistent.serializable.TaskFailure;
import com.ibm.ws.concurrent.persistent.serializable.TaskSkipped;
import com.ibm.wsspi.concurrent.persistent.TaskRecord;
import com.ibm.wsspi.concurrent.persistent.TaskStore;

/**
 * Future for a persistent task.
 *
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class TaskStatusImpl<T> implements TaskStatus<T>, TimerStatus<T> {
    private static final TraceComponent tc = Tr.register(TaskStatusImpl.class);

    /**
     * Identifier of the thread context class loader when the task was originally submitted.
     */
    private final String classLoaderIdentifier;

    /**
     * Unique key for the task.
     */
    private final long id;

    /**
     * Value that represents a combination of miscellaneous boolean flags.
     */
    private final short miscBinaryFlags;

    /**
     * Task name (if any).
     */
    private final String name;

    /**
     * Milliseconds for the next scheduled start time.
     */
    private final long nextExecutionTime;

    /**
     * Persistent scheduled executor that obtained this task status.
     */
    private final PersistentExecutorImpl persistentExecutor;

    /**
     * Snapshot of task result if any.
     */
    private final byte[] resultBytes;

    /**
     * Bits representing a snapshot of task state.
     */
    private final short state;

    /**
     * Snapshot of trigger if any.
     */
    private final byte[] triggerBytes;

    /**
     * Initialize a snapshot of task status.
     *
     * @param task               persistent task.
     * @param persistentExecutor persistent scheduled executor that obtained this task status.
     * @throws IllegalStateException if any of the following are unspecified in the task record:
     *                                   (Id, IdentifierOfClassLoader, MiscBinaryFlags, Name, NextExecutionTime, Result, State)
     */
    public TaskStatusImpl(TaskRecord task, PersistentExecutorImpl persistentExecutor) {
        classLoaderIdentifier = task.getIdentifierOfClassLoader();
        id = task.getId();
        miscBinaryFlags = task.getMiscBinaryFlags();
        name = task.getName();
        nextExecutionTime = task.getNextExecutionTime();
        resultBytes = task.getResult();
        state = task.getState();
        triggerBytes = task.hasTrigger() ? task.getTrigger() : null;
        this.persistentExecutor = persistentExecutor;
    }

    /**
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        TransactionController tranController = persistentExecutor.new TransactionController();
        boolean result = false, removed = false;
        TaskStore taskStore = persistentExecutor.taskStore;
        try {
            tranController.preInvoke();
            result = (miscBinaryFlags & TaskRecord.Flags.AUTO_PURGE_ALWAYS.bit) == 0 ? taskStore.cancel(id) : (removed = taskStore.remove(id, null, false));
        } catch (Throwable x) {
            tranController.setFailure(x);
        } finally {
            PersistentStoreException x = tranController.postInvoke(PersistentStoreException.class); // TODO proposed spec class
            if (x != null)
                throw x;
        }

        if (removed) {
            long[] runningTaskState = InvokerTask.runningTaskState.get();
            if (runningTaskState != null && runningTaskState[0] == id)
                runningTaskState[1] = InvokerTask.REMOVED_BY_SELF;
        }
        return result;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Delayed delayed) {
        // Match what we observe Java executor implementations doing (-1, 0, 1).
        int result;
        if (delayed instanceof TaskStatusImpl) { // avoid checking current time if possible
            long otherNextExecTime = ((TaskStatusImpl<?>) delayed).nextExecutionTime;
            result = this == delayed || nextExecutionTime == otherNextExecTime ? 0 : nextExecutionTime - otherNextExecTime < 0 ? -1 : 1;
        } else {
            long diff = (nextExecutionTime - new Date().getTime()) - delayed.getDelay(TimeUnit.MILLISECONDS);
            // Because getDelay() compares with the current time, which will be slightly different between
            // invocations on this and the other delayed instance, we are limiting precision to a tenth of a second.
            result = diff < -100 ? -1 : diff > 100 ? 1 : 0;
        }

        return result;
    }

    /**
     * Comparison of task ids and status.
     *
     * @return true if the task ids and status match, otherwise false.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof TaskStatusImpl) {
            TaskStatusImpl<?> other = (TaskStatusImpl<?>) obj;
            return id == other.id
                   && state == other.state
                   && nextExecutionTime == other.nextExecutionTime
                   && persistentExecutor == other.persistentExecutor
                   && Arrays.equals(resultBytes, other.resultBytes);
        }
        return false;
    }

    /**
     * @see java.util.concurrent.Future#get()
     */
    @Override
    public T get() throws ExecutionException {
        if ((state & TaskState.ENDED.bit) == 0)
            throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1551.result.unavailable.until.ended", "get"));
        else
            return getResult();
    }

    /**
     * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException {
        if ((state & TaskState.ENDED.bit) == 0)
            throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1551.result.unavailable.until.ended"));
        else
            return getResult();
    }

    /**
     * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
     */
    @Override
    public long getDelay(TimeUnit unit) {
        // We can only implement this for one-shot tasks or any task that has ended,
        // in which case the next execution time can never change.
        if ((miscBinaryFlags & TaskRecord.Flags.ONE_SHOT_TASK.bit) == 0 && (state & TaskState.ENDED.bit) == 0)
            throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1552.delay.unavailable"));
        else
            return unit.convert(nextExecutionTime - new Date().getTime(), TimeUnit.MILLISECONDS);
    }

    /**
     * Returns text formatted with both the task id and name (if any).
     * For example: "1001 (My Task Name)" or just "1001"
     *
     * @return text formatted with both the task id and name (if any).
     */
    private final String getIdAndName() {
        StringBuilder sb = new StringBuilder().append(id);
        if (!" ".equals(name))
            sb.append(" (").append(name).append(')');
        return sb.toString();
    }

    /**
     * @see com.ibm.websphere.concurrent.persistent.TaskStatus#getNextExecutionTime()
     */
    @Override
    @Trivial
    public Date getNextExecutionTime() {
        Date time = (state & TaskState.ENDED.bit) == 0 ? new Date(nextExecutionTime) : null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, Utils.appendDate(new StringBuilder("getNextExecutionTime "),
                                                time == null ? null : time.getTime())
                            .toString());
        return time;
    }

    /**
     * @see com.ibm.websphere.concurrent.persistent.TaskStatus#getResult()
     */
    @Override
    public T getResult() throws ExecutionException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getResult");

        Exception failure = null;
        T instance = null;

        if ((state & TaskState.UNATTEMPTED.bit) != 0)
            ; // null return value for unattempted
        else if ((state & TaskState.CANCELED.bit) != 0)
            failure = new CancellationException();
        else if ((state & TaskState.SKIPPED.bit) != 0 && (state & TaskState.SKIPRUN_FAILED.bit) == 0)
            failure = new SkippedException();

        else if (resultBytes != null) {
            try {
                ClassLoader loader = classLoaderIdentifier == null ? null : persistentExecutor.classloaderIdSvc.getClassLoader(classLoaderIdentifier);
                // TODO what if null?

                Object result = persistentExecutor.deserialize(resultBytes, loader);

                // A special type of result indicates a task failure
                if (result instanceof TaskFailure) {
                    TaskFailure taskFailure = (TaskFailure) result;
                    switch (taskFailure.getReason()) {
                        case TaskFailure.FAILURE_LIMIT_REACHED:
                            failure = new AbortedException(Tr.formatMessage(tc, "CWWKC1555.retry.limit.reached", getIdAndName(),
                                                                            taskFailure.getParameter(0)), taskFailure.getCause());
                            break;
                        case TaskFailure.NONSER_RESULT:
                            failure = new ResultNotSerializableException(taskFailure.getParameter(0));
                            failure.initCause(taskFailure.getCause());
                            break;
                        default:
                            failure = new ExecutionException(Tr.formatMessage(tc, "CWWKC1554.general.task.failure", getIdAndName()), taskFailure.getCause());
                    }
                } else if (result instanceof TaskSkipped) {
                    failure = new SkippedException(((TaskSkipped) result).getCause());
                } else {
                    @SuppressWarnings("unchecked")
                    T newInstance = (T) result;
                    instance = newInstance;
                }
            } catch (ClassNotFoundException x) {
                failure = new RuntimeException(Tr.formatMessage(tc, "CWWKC1553.result.inaccessible", persistentExecutor.name, getIdAndName()), x);
            } catch (IOException x) {
                failure = new RuntimeException(Tr.formatMessage(tc, "CWWKC1553.result.inaccessible", persistentExecutor.name, getIdAndName()), x);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "getResult", failure == null ? instance : failure);
        if (failure == null)
            return instance;
        else if (failure instanceof ExecutionException)
            throw (ExecutionException) failure;
        else
            throw (RuntimeException) failure;
    }

    /**
     * Returns the unique identifier for the task.
     *
     * @return the unique identifier for the task.
     */
    @Override
    public long getTaskId() {
        return id;
    }

    /**
     * @see com.ibm.websphere.concurrent.persistent.TaskStatus#getTaskName()
     */
    @Override
    public String getTaskName() {
        return name;
    }

    /**
     * @see com.ibm.ws.concurrent.persistent.ejb.TimerStatus#getTimer()
     */
    @Override
    public TimerTrigger getTimer() throws ClassNotFoundException, IOException {
        if (triggerBytes == null)
            return null;

        ClassLoader loader = classLoaderIdentifier == null //
                        ? InvokerTask.priv.getSystemClassLoader() //
                        : persistentExecutor.classloaderIdSvc.getClassLoader(classLoaderIdentifier);
        return (TimerTrigger) persistentExecutor.deserialize(triggerBytes, loader);
    }

    /**
     * Returns the task id as an integer.
     *
     * @Return the task id as an integer.
     */
    @Override
    public int hashCode() {
        return (int) (id % Integer.MAX_VALUE);
    }

    /**
     * @see com.ibm.websphere.concurrent.persistent.TaskStatus#hasResult()
     */
    @Override
    public boolean hasResult() {
        return (state & TaskState.UNATTEMPTED.bit) == 0;
    }

    /**
     * @see java.util.concurrent.Future#isCancelled()
     */
    @Override
    public boolean isCancelled() {
        if ((state & TaskState.ENDED.bit) == 0)
            throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1550.status.unavailable.until.ended", "isCancelled"));
        else
            return (state & TaskState.CANCELED.bit) != 0;
    }

    /**
     * @see java.util.concurrent.Future#isDone()
     */
    @Override
    public boolean isDone() {
        if ((state & TaskState.ENDED.bit) == 0)
            throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1550.status.unavailable.until.ended", "isDone"));
        else
            return true;
    }

    /**
     * Returns a string consisting of the identity hash code, task id, task name (if any is known), next time, and state.
     * Example: TaskStatus[301]@a556a556 My Task Name SCHEDULED 2014/06/03-8:48:00.000-CDT
     *
     * @return a string consisting of the identity hash code, task id, and task name (if any is known).
     */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder(100).append("TaskStatus[").append(id).append("]@").append(Integer.toHexString(System.identityHashCode(this)));
        if (name != null && name.length() > 0)
            output.append(' ').append(name);
        Utils.appendState(output.append(' '), state);
        Utils.appendDate(output.append(' '), nextExecutionTime);
        return output.toString();
    }
}
