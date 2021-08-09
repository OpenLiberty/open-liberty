/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.concurrent.persistent;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.Trigger;

/**
 * Persistent scheduled executor.
 */
public interface PersistentExecutor extends ManagedScheduledExecutorService {
    /**
     * Execution property that specifies a transaction timeout value. The value must be the String form of a
     * non-negative integer, in the range of 0 through Integer.MAX_VALUE.
     * A value of 0 is equivalent to unspecified and indicates that a default transaction timeout should be used,
     * depending on the value of the ManagedTask.TRANSACTION execution property.
     * If ManagedTask.TRANSACTION is set to SUSPEND, a default of 30 minutes is used.
     * If ManagedTask.TRANSACTION is unspecified or set to USE_TRANSACTION_OF_EXECUTION_THREAD, the default from the
     * transaction manager is used.
     */
    static final String TRANSACTION_TIMEOUT = "com.ibm.ws.concurrent.TRANSACTION_TIMEOUT";

    /**
     * Cancels all tasks that match the specified name pattern and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to cancel all unattempted tasks that have a name that starts with "PAYROLL_TASK_",
     * <code>
     * executor.cancel("PAYROLL\\_TASK\\_%", '\\', TaskRecord.State.UNATTEMPTED, true);
     * </code>
     * When using this method, tasks are canceled (not removed) regardless of the autopurge setting.
     * It is not possible to cancel tasks that have already ended. Any tasks that meet the criteria which are
     * also in an ended state are ignored.
     * 
     * @param pattern task name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character).
     *            If null, a task name pattern is not used to narrow the search.
     * @param escape escape character that indicates when matching characters like % and _ should be interpreted literally.
     * @param state a task state. For example, TaskRecord.State.SCHEDULED.
     * @param inState indicates whether to cancel tasks with or without the specified state
     * @return count of tasks canceled.
     * @throws PersistentStoreException if an error occurs updating the persistent store.
     */
    int cancel(String pattern, Character escape, TaskState state, boolean inState);

    /**
     * Create a property name/value to persist in the scheduler task store.
     * One possible use for a persistent property is to indicate if a task or group of tasks have already been scheduled.
     * 
     * @param name property name, up to 254 characters. The property name must begin with an alphanumeric character [A-Z,a-z,0-9]. All other names are reserved for internal use.
     * @param value property value, up to 254 characters.
     * @return true if the property was created. False if it already exists, in which case, the current transaction might be marked to roll back.
     * @throws IllegalArgumentException if the name or value are null or empty.
     */
    // TODO is there any way to stop JPA from marking the transaction to roll back? This behavior is awkward here
    boolean createProperty(String name, String value);

    /**
     * Find task IDs of all tasks accessible to the caller that match the specified name pattern and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to find task ids for the first 100 tasks that have not completed all executions
     * and have a name that starts with "PAYROLL_TASK_",
     * executor.findTaskIds("PAYROLL\\_TASK\\_%", '\\', TaskRecord.State.ENDED, null, 100, false);
     * In-memory results are always returned. Results are ordered in ascending order by task id.
     * 
     * @param pattern task name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character).
     *            If null, a task name pattern is not used to narrow the search.
     * @param escape escape character that indicates when matching characters like % and _ should be interpreted literally. A value of null avoids designating an escape character,
     *            in which case the behavior depends on the persistent store.
     * @param state a task state. For example, TaskRecord.State.CANCELED
     * @param inState indicates whether to include or exclude results with the specified state
     * @param minId minimum value for task id to be returned in the results. A null value means no minimum.
     * @param maxResults limits the number of results to return to the specified maximum value. A null value means no limit.
     * @return list of task ids matching the criteria.
     * @throws PersistentStoreException if an error occurs accessing the persistent store.
     */
    List<Long> findTaskIds(String pattern, Character escape, TaskState state, boolean inState, Long minId, Integer maxResults);

    /**
     * Find all tasks accessible to the caller that match the specified name pattern and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to find tasks that have not completed all executions and have a name that starts with "PAYROLL_TASK_",
     * executor.findTaskStatus("PAYROLL\\_TASK\\_%", '\\', TaskRecord.State.ENDED, false);
     * 
     * @param pattern task name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character).
     *            If null, a task name pattern is not used to narrow the search.
     * @param escape escape character that indicates when matching characters like % and _ should be interpreted literally. A value of null avoids designating an escape character,
     *            in which case the behavior depends on the persistent store.
     * @param state a task state. For example, TaskRecord.State.CANCELED
     * @param inState indicates whether to include or exclude results with the specified state
     * @param minId minimum value for task id to be returned in the results. A null value means no minimum.
     * @param maxResults limits the number of results to return to the specified maximum value. A null value means no limit.
     * @return list of task status matching the criteria, ordered by task id.
     * @throws PersistentStoreException if an error occurs accessing the persistent store.
     */
    List<TaskStatus<?>> findTaskStatus(String pattern, Character escape, TaskState state, boolean inState, Long minId, Integer maxResults);

    /**
     * Returns the value of the persisted property with the specified name. Null if the property does not exist.
     * 
     * @param name property name. The property name must begin with an alphanumeric character [A-Z,a-z,0-9]. All other names are reserved for internal use.
     * @return property value.
     */
    String getProperty(String name);

    /**
     * Returns status for the persistent task with the specified id.
     * 
     * @param taskId unique identifier for the task.
     * @return status for the persistent task with the specified id.
     *         If the task is not found, <code>null</code> is returned.
     * @throws PersistentStoreException if an error occurs accessing the persistent store.
     */
    <T> TaskStatus<T> getStatus(long taskId);

    /**
     * Removes the information for the specified task from the persistent store.
     * This implicitly cancels the task if not already completed.
     * 
     * @param taskId unique identifier for a task.
     * @return <code>true</code> if the information for the task was removed.
     *         <code>false</code> if the task is not found in the persistent store or is not accessible to the caller.
     * @throws PersistentStoreException if an error occurs that is related to the persistent store.
     */
    boolean remove(long taskId);

    /**
     * Remove all tasks that match the specified name pattern and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to remove all canceled tasks that have a name that starts with "PAYROLL_TASK_",
     * executor.remove("PAYROLL\\_TASK\\_%", '\\', TaskRecord.State.CANCELED, true);
     * 
     * @param pattern task name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character).
     *            If null, a task name pattern is not used to narrow the search.
     * @param escape escape character that indicates when matching characters like % and _ should be interpreted literally.
     * @param state a task state. For example, TaskRecord.State.UNATTEMPTED.
     * @param inState indicates whether to remove tasks with or without the specified state
     * @return count of tasks removed.
     * @throws PersistentStoreException if an error occurs updating the persistent store.
     */
    int remove(String pattern, Character escape, TaskState state, boolean inState);

    /**
     * Removes the persisted property with the specified name.
     * 
     * @param name property name. The property name must begin with an alphanumeric character [A-Z,a-z,0-9]. All other names are reserved for internal use.
     * @return true if the property was removed. False if it was not found in the persistent store.
     */
    boolean removeProperty(String name);

    /**
     * Creates and executes a ScheduledFuture that becomes enabled after the given delay.
     * 
     * @param callable the function to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @return task status representing the initial scheduling of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if callable is null
     * @see java.util.concurrent.ScheduledExecutorService#schedule(java.util.concurrent.Callable, long, java.util.concurrent.TimeUnit)
     */
    @Override
    <V> TaskStatus<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    /**
     * Creates and executes a task based on a Trigger. The Trigger determines when the task should run and how often.
     * 
     * @param callable the function to execute.
     * @param trigger the trigger that determines when the task should fire.
     * @return task status representing the initial scheduling of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution.
     * @throws NullPointerException if callable is null.
     * @see javax.enterprise.concurrent.ManagedScheduledExecutorService#schedule(java.util.concurrent.Callable, javax.enterprise.concurrent.Trigger)
     */
    @Override
    <V> TaskStatus<V> schedule(Callable<V> callable, Trigger trigger);

    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay.
     * 
     * @param command the task to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @return task status representing the initial scheduling of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is null
     * @see java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
     */
    @Override
    TaskStatus<?> schedule(Runnable command, long delay, TimeUnit unit);

    /**
     * Creates and executes a task based on a Trigger. The Trigger determines when the task should run and how often.
     * 
     * @param command the task to execute.
     * @param trigger the trigger that determines when the task should fire.
     * @return task status representing the initial scheduling of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution.
     * @throws NullPointerException if command is null.
     * @see javax.enterprise.concurrent.ManagedScheduledExecutorService#schedule(java.lang.Runnable, javax.enterprise.concurrent.Trigger)
     */
    @Override
    TaskStatus<?> schedule(Runnable command, Trigger trigger);

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay,
     * and subsequently with the given period; that is executions will commence after
     * <code>initialDelay</code> then <code>initialDelay+period</code>, then <code>initialDelay + 2 * period</code>, and so on.
     * If any execution of the task encounters an exception, subsequent executions are suppressed.
     * Otherwise, the task will only terminate via cancellation or termination of the executor.
     * If any execution of this task takes longer than its period, then subsequent executions may start late,
     * but will not concurrently execute.
     * 
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit the time unit of the initialDelay and period parameters
     * @return task status representing the initial scheduling of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if period less than or equal to zero
     * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
     */
    @Override
    TaskStatus<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay,
     * and subsequently with the given delay between the termination of one execution and the commencement of the next.
     * If any execution of the task encounters an exception, subsequent executions are suppressed.
     * Otherwise, the task will only terminate via cancellation or termination of the executor.
     * 
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay the delay between the termination of one execution and the commencement of the next
     * @param unit the time unit of the initialDelay and delay parameters
     * @return task status representing the initial scheduling of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if delay less than or equal to zero
     * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
     */
    @Override
    TaskStatus<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);

    /**
     * Assigns the value of the property with the specified name, if it exists in the persistent store.
     * 
     * @param name property name.
     * @param value new value for the property.
     * @return true if the property exists and the value was updated or it already has the specified value.
     *         False if the property does not exist.
     * @throws PersistentStoreException if an error occurs updating the persistent store.
     * @throws IllegalArgumentException if the value is null or empty.
     */
    boolean setProperty(String name, String value);

    /**
     * Signals the persistent executor to start polling for tasks in the persistent store.
     * This method should only be used when initialPollInterval is configured to -1,
     * in which case polling does not happen until this signal is received.
     * 
     * @throws IllegalStateException if initialPollInterval is configured to 0 (immediate) or a positive value.
     */
    void startPolling();

    /**
     * Submits a value-returning task for execution and returns a <code>PersistentTaskStatus</code>
     * representing the initial scheduling of the task.
     * 
     * @param task the task to submit
     * @return task status representing the initial scheduling of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the task is null
     * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
     */
    @Override
    <T> TaskStatus<T> submit(Callable<T> task);

    /**
     * Submits a <code>Runnable</code> task for execution and returns a <code>PersistentTaskStatus</code>
     * representing the initial scheduling of the task. When a snapshot of the task status is obtained
     * after the task completes, the <code>get</code> method will return <code>null</code>.
     * 
     * @param task the task to submit
     * @return task status representing the initial scheduling of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the task is null
     * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
     */
    @Override
    TaskStatus<?> submit(Runnable task);

    /**
     * Submits a <code>Runnable</code> task for execution and returns a <code>PersistentTaskStatus</code>
     * representing the initial scheduling of the task. When a snapshot of the task status is obtained
     * after the task completes, the <code>get</code> method will return the given result.
     * 
     * @param task the task to submit
     * @param result the result to return
     * @return task status representing the initial scheduling of the task
     * @throws IllegalArugmentException if the result is not serializable
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the task is null
     * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable, java.lang.Object)
     */
    @Override
    <T> TaskStatus<T> submit(Runnable task, T result);
}
