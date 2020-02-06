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
package com.ibm.wsspi.concurrent.persistent;

import java.util.List;
import java.util.Map;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

/**
 * Interface for persistent task stores.
 */
public interface TaskStore {
    /**
     * Update the record for a task in the persistent store to indicate that the task is canceled.
     *
     * @param taskId unique identifier for a persistent task
     * @return true if the state of the task was updated as a result of this method, otherwise false.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    boolean cancel(long taskId) throws Exception;

    /**
     * Cancels all tasks that match the specified name pattern and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to cancel all unattempted tasks that have a name that starts with "PAYROLL_TASK_",
     * <code>
     * taskStore.cancel("PAYROLL\\_TASK\\_%", '\\', TaskRecord.State.UNATTEMPTED, true);
     * </code>
     * When using this method, tasks are canceled (not removed) regardless of the autopurge setting.
     * It is not possible to cancel tasks that have already ended. Any tasks that meet the criteria which are
     * also in an ended state are ignored.
     *
     * @param pattern task name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character)
     * @param escape  escape character that indicates when matching characters like % and _ should be interpreted literally.
     * @param state   a task state. For example, TaskRecord.State.SCHEDULED.
     * @param inState indicates whether to cancel tasks with or without the specified state
     * @param owner   name of owner to match as the task submitter. Null to ignore.
     * @return count of tasks canceled.
     * @throws Exception if an error occurs updating the persistent store.
     */
    int cancel(String pattern, Character escape, TaskState state, boolean inState, String owner) throws Exception;

    /**
     * Attempts to claim the right to run a task. This happens in either of two different ways.
     * <li>
     * <ol>By assigning the task to the specified partition.</ol>
     * <ol>By writing a new claim expiry value to the task entry where the previous was already expired.</ol>
     * </li>
     * Assigns a task to the specified partition.
     * The implementation should aim to return as quickly as possible with a false value if the entry is already locked
     * by another member, rather than waiting to make the update. A locked task entry indicates that failover is not needed
     * - a false positive occurred because the task was taking too long to run. This could be caused by a lengthy timer/task
     * that is otherwise behaving properly, in which case the customer ought to be using a larger value for missedTaskThreshold
     * so as to avoid triggering failover logic/overhead when there is no outage.
     *
     * @param taskId                 id of the task to reassign.
     * @param version                version number of the task entry which must match in order for the task to be transferred.
     * @param claimExpiryOrPartition timestamp when the claim on task execution expires OR partition id to which to assign the task.
     * @return true if the task was assigned. Otherwise false.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    boolean claimIfNotLocked(long taskId, int version, long claimExpiryOrPartition) throws Exception;

    /**
     * Create an entry in the persistent store for a new task.
     * This method assigns a unique identifier to the task and updates the Id attribute of the TaskRecord with the value.
     *
     * @param task a new persistent task entry. All attributes of the task record must be specified except for the id.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    void create(TaskRecord taskRecord) throws Exception;

    /**
     * Create a property entry in the persistent store.
     *
     * @param name  unique name for the property.
     * @param value value of the property.
     * @return true if the property was created. False if a property with the same name already exists.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    boolean createProperty(String name, String value) throws Exception;

    /**
     * Returns all partition entries that match the specified criteria.
     *
     * @param partitionRecord contains the criteria against which to compare.
     * @return all partition entries that match the specified criteria.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    List<PartitionRecord> find(PartitionRecord expected) throws Exception;

    /**
     * Returns the data contained within a task record in the persistent store if the task is in a SCHEDULED state.
     *
     * @param taskId               unique identifier for a task.
     * @param partitionId          unique identifier for a partition. Null to ignore the partition.
     * @param maxNextExecutionTime milliseconds at (or before) which the task must be scheduled in order to run.
     * @param forUpdate            indicates if a write lock should be obtained on the task record.
     * @return the task record if found and possible to lock it, otherwise null.
     *         The resulting record must contain the attributes
     *         (IdentityOfClassLoader, IdentityOfOwner, MiscBinaryFlags, Name,
     *         NextExecutionTime, OriginalSubmitTime, PreviousScheduledStartTime, PreviousStartTime, PreviousStopTime,
     *         Result, ConsecutiveFailureCount, State, Task, TaskInfo, Trigger, Version)
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    TaskRecord find(long taskId, Long partitionId, long maxNextExecTime, boolean forUpdate) throws Exception;

    /**
     * Returns a snapshot of task state for the task with the specified unique identifier.
     *
     * @param taskId         unique identifier for a task
     * @param owner          name of owner to match as the task submitter. Null to ignore.
     * @param includeTrigger indicates whether to include the Trigger in the status.
     * @return snapshot of task state for the task with the specified unique identifier.
     *         The resulting record must contain the attributes:
     *         (Id, IdentifierOfClassLoader, MiscBinaryFlags, Name, NextExecutionTime, Result, State, Trigger [if includeTrigger=true], Version).
     *         If the task is not found, <code>null</code> is returned.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    TaskRecord findById(long taskId, String owner, boolean includeTrigger) throws Exception;

    /**
     * Creates an entry for a partition record if one with the specified combination of executor/host/server/userdir
     * does not already exist in the persistent store.
     * The invoker makes a best effort to avoid invoking this method concurrently with the same
     * Executor/Host/Server/UserDir combination, however it is not possible to guarantee this in all cases.
     * A scenario such as the following is expected to fail:
     * thread1 finds no entry
     * thread2 finds no entry
     * thread1 creates the entry
     * thread2 attempts to create and fails because it already exists
     * The invoker is expected to handle this by rolling back and retrying.
     *
     * @param record partition entry with executor/host/server/userdir to locate, or if not found, add to the persistent store.
     *                   If an entry is found, it is updated to match the Expiry and States if either of those is supplied.
     *                   The record must contain the following attributes (Executor, Host, Server, UserDir) and can optionally contain (Expiry, States).
     * @return unique identifier for the partition record which either already exists or was newly created.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    long findOrCreate(PartitionRecord record) throws Exception;

    /**
     * Creates a dedicated partition record to be used for partitioning out poll attempts across multiple instances.
     * Only use this when missedTaskTheshold is enabled and pollInterval is unspecified, which means Liberty determines it automatically.
     *
     * @return unique identifier for the partition record to be used for coordination of automatically determined polling.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    long findOrCreatePollPartition() throws Exception;

    /**
     * Reads information from the partition entry for polling coordination and obtains a write lock on it.
     *
     * @param partitionId unique identifier of the partition entry for polling coordination.
     * @return size 2 array where the first element is the expiry (type <code>long</code>) and the second is the last-updated timestamp (type <code>long</code>).
     * @throws Exception if an error occurs accessing the persistent store.
     */
    Object[] findPollInfoForUpdate(long partitionId) throws Exception;

    /**
     * Find all task IDs for tasks that match the specified name pattern and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to find taskIDs for the first 100 tasks belonging to app1 in partition 12 that have not completed all executions
     * and have a name that starts with "PAYROLL_TASK_",
     * taskStore.findTaskIds("PAYROLL\\_TASK\\_%", '\\', TaskState.ENDED, false, null, 100, "app1", 12);
     *
     * @param pattern    task name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character). Null to ignore.
     * @param escape     escape character that indicates when matching characters like % and _ should be interpreted literally. Null to ignore.
     * @param state      a task state. For example, TaskState.CANCELED
     * @param inState    indicates whether to include or exclude results with the specified state
     * @param minId      minimum value for task id to be returned in the results. A null value means no minimum.
     * @param maxResults limits the number of results to return to the specified maximum value. A null value means no limit.
     * @param owner      name of owner to match as the task submitter. Null to ignore.
     * @param partition  identifier of the partition in which to search for tasks. Null to ignore.
     * @return in-memory, ordered list of task ID.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    List<Long> findTaskIds(String pattern, Character escape, TaskState state, boolean inState,
                           Long minId, Integer maxResults, String owner, Long partition) throws Exception;

    /**
     * Find all tasks that match the specified name pattern and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to find tasks that have not completed all executions and have a name that starts with "PAYROLL_TASK_",
     * taskStore.findTaskStatus("PAYROLL\\_TASK\\_%", '\\', TaskState.ENDED, false, "app1", executor1, false);
     *
     * @param pattern        task name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character)
     * @param escape         escape character that indicates when matching characters like % and _ should be interpreted literally.
     * @param state          a task state. For example, TaskState.CANCELED
     * @param inState        indicates whether to include or exclude results with the specified state
     * @param minId          minimum value for task id to be returned in the results. A null value means no minimum.
     * @param maxResults     limits the number of results to return to the specified maximum value. A null value means no limit.
     * @param owner          name of owner to match as the task submitter. Null to ignore.
     * @param includeTrigger indicates whether to include the Trigger in the status.
     * @param executor       persistent executor instance.
     * @return list of task status matching the criteria, ordered by task id.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    List<TaskStatus<?>> findTaskStatus(String pattern, Character escape, TaskState state, boolean inState,
                                       Long minId, Integer maxResults, String owner, boolean includeTrigger,
                                       PersistentExecutor executor) throws Exception;

    /**
     * Find all tasks to execute on or before maxNextExecTime (up to a maximum of maxResults).
     * Only tasks which are not currently claimed are returned.
     *
     * @param maxNextExecTime maximum next execution time (in milliseconds)
     * @param maxResults      maximum number of results to return. Null means unlimited.
     * @return List of (Id, MiscBinaryFlags, NextExecutionTime, TransactionTimeout, Version) pairs.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    List<Object[]> findUnclaimedTasks(long maxNextExecTime, Integer maxResults) throws Exception;

    /**
     * Find all tasks to execute on or before maxNextExecTime (up to a maximum of maxResults).
     * Only tasks that are owned by the specified partition are returned.
     *
     * @param partition       partition number
     * @param maxNextExecTime maximum next execution time (in milliseconds)
     * @param maxResults      maximum number of results to return. Null means unlimited.
     * @return List of (Id, MiscBinaryFlags, NextExecutionTime, TransactionTimeout) pairs, ordered by next execution time.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    List<Object[]> findUpcomingTasks(long partition, long maxNextExecTime, Integer maxResults) throws Exception;

    /**
     * Returns a task record with information about the expected next execution time for the task with the specified id.
     *
     * @param taskId unique identifier for the task.
     * @param owner  name of owner (if any) to match as the task submitter.
     * @return task record containing information about the expected next execution time for the task with the specified id.
     *         The resulting record must contain the attributes (MiscBinaryFlags, NextExecutionTime, State).
     *         If the task is not found then <code>null</code> is returned.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    TaskRecord getNextExecutionTime(long taskId, String owner) throws Exception;

    /**
     * Returns the identifier of the partition to which the task is assigned.
     *
     * @param taskId unique identifier for the task.
     * @return the identifier of the partition to which the task is assigned.
     *         If the task is not found then <code>null</code> is returned.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    Long getPartition(long taskId) throws Exception;

    /**
     * Returns name/value pairs for all persisted properties that match the specified name pattern.
     * For example, to find property names that start with "MY_PROP_NAME_",
     * taskStore.getProperties("MY\\_PROP\\_NAME\\_%", '\\');
     *
     * @param pattern name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character)
     * @param escape  escape character that indicates when matching characters like % and _ should be interpreted literally.
     *                    A value of null avoids designating an escape character, in which case the behavior depends on the persistent store.
     * @return in-memory map of name/value pairs matching the criteria.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    Map<String, String> getProperties(String pattern, Character escape) throws Exception;

    /**
     * Returns the value of the persisted property with the specified name. Null if the property does not exist.
     *
     * @param name property name.
     * @return property value.
     */
    String getProperty(String name) throws Exception;

    /**
     * Returns a task record with information about the <code>Trigger</code> for the task with the specified id.
     *
     * @param taskId unique identifier for the task.
     * @return task record containing information about the <code>Trigger</code> for the task with the specified id.
     *         The resulting record must contain the attributes (IdOfOwner, State, Trigger).
     *         If the task is not found then <code>null</code> is returned.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    TaskRecord getTrigger(long taskId) throws Exception;

    /**
     * Increment and return the consecutive failure count for a task.
     * The consecutive failure count is not incremented beyond Short.MAX_VALUE.
     *
     * @param taskId id of the task.
     * @return the new consecutive failure count. -1 if the task is not found in the persistent store.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    short incrementFailureCount(long taskId) throws Exception;

    /**
     * Persist updates to a task record in the persistent store.
     *
     * @param updates  updates to make to the task. Only the specified fields are persisted.
     * @param expected criteria that must be matched for optimistic update to succeed. Must include Id.
     * @return true if persistent task store was updated, otherwise false.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    boolean persist(TaskRecord updates, TaskRecord expected) throws Exception;

    /**
     * Remove the record for a task from the persistent store.
     *
     * @param taskId        unique identifier for a persistent task
     * @param owner         name of owner to match as the task submitter. Null to ignore.
     * @param removeIfEnded indicates whether or not tasks that have ended can be removed.
     * @return true if the record was removed as a result of this method, otherwise false.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    boolean remove(long taskId, String owner, boolean removeIfEnded) throws Exception;

    /**
     * Remove partition entries matching the specified criteria.
     *
     * @param criteria criteria that must be matched for entries to be removed.
     * @return count of entries that were removed.
     * @throws Exception if an error occurs when attempting to update the persistent store.
     */
    int remove(PartitionRecord criteria) throws Exception;

    /**
     * Remove all tasks that match the specified name pattern and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to remove all canceled tasks that have a name that starts with "PAYROLL_TASK_",
     * taskStore.remove("PAYROLL\\_TASK\\_%", '\\', TaskState.CANCELED, true, "app1");
     *
     * @param pattern task name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character)
     * @param escape  escape character that indicates when matching characters like % and _ should be interpreted literally.
     * @param state   a task state. For example, TaskState.UNATTEMPTED.
     * @param inState indicates whether to remove tasks with or without the specified state
     * @param owner   name of owner to match as the task submitter. Null to ignore.
     * @return count of tasks removed.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    int remove(String pattern, Character escape, TaskState state, boolean inState, String owner) throws Exception;

    /**
     * Removes all persisted properties that match the specified name pattern.
     * For example, to remove properties with names that start with "MY_PROP_NAME_",
     * taskStore.removeProperties("MY\\_PROP\\_NAME\\_%", '\\');
     *
     * @param pattern name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character)
     * @param escape  escape character that indicates when matching characters like % and _ should be interpreted literally.
     *                    A value of null avoids designating an escape character, in which case the behavior depends on the persistent store.
     * @return number of properties removed.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    int removeProperties(String pattern, Character escape) throws Exception;

    /**
     * Removes the property with the specified name from the persistent store.
     *
     * @param name name of the entry.
     * @return true if removed. False if it was not found in the persistent store.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    boolean removeProperty(String name) throws Exception;

    /**
     * Assigns the value of the property if it exists in the persistent store.
     *
     * @param name  property name.
     * @param value new value for the property.
     * @return true if the property exists and was updated or already has the value.
     *         False if the property does not exist in the persistent store.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    boolean setProperty(String name, String value) throws Exception;

    /**
     * Transfers tasks that have not yet completed all executions to another partition.
     *
     * @param taskId         task id including and up to which all non-ended tasks in the partition are reassigned.
     *                           If null, all non-ended tasks in the partition are reassigned.
     * @param oldPartitionId partition id from which to take tasks.
     * @param newPartitionId partition id to which to assign tasks.
     * @return number of tasks updated.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    int transfer(Long maxTaskId, long oldPartitionId, long newPartitionId) throws Exception;

    /**
     * Writes a new expiry and last-updated timestamp to the partition entry for polling coordination.
     *
     * @param partitionId unique identifier of the partition entry for polling coordination.
     * @param newExpiry   the new expiry value to use.
     * @return true if the partition entry for polling was updated. Otherwise false.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    boolean updatePollInfo(long partitionId, long newExpiry) throws Exception;
}
