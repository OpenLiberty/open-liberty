/*******************************************************************************
 * Copyright (c) 2015,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.ejb;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskState;

/**
 * Interface with methods that we are making available only to EJB Persistent Timers.
 */
public interface TimersPersistentExecutor extends PersistentExecutor {
    /**
     * Returns name/value pairs for all persisted properties that match the specified name pattern.
     * For example, to find property names that start with "MY_PROP_NAME_",
     * executor.getProperties("MY\\_PROP\\_NAME\\_%", '\\');
     * 
     * @param pattern name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character).
     * @param escape escape character that indicates when matching characters like % and _ should be interpreted literally.
     *            A value of null avoids designating an escape character, in which case the behavior depends on the persistent store.
     * @return in-memory map of name/value pairs matching the criteria.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    Map<String, String> findProperties(String pattern, Character escape) throws Exception;

    /**
     * Find status for all tasks that match the specified application and name pattern and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to find all tasks in app1 with a name that starts with mod_1# that have not been canceled,
     * executor.findTimerStatus("app1", "mod\\_1#%", '\\', TaskRecord.State.CANCELED, false, null, null, app1);
     * 
     * @param appName name of the application that owns the task. If null, the application name is not used to narrow the search.
     * @param pattern task name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character)
     * @param escape escape character that indicates when matching characters like % and _ should be interpreted literally. A value of null avoids designating an escape character,
     *            in which case the behavior depends on the persistent store.
     * @param state a task state. For example, TaskRecord.State.SCHEDULED
     * @param inState indicates whether to include or exclude results with the specified state
     * @param minId minimum value for task id to be returned in the results. A null value means no minimum.
     * @param maxResults limits the number of results to return to the specified maximum value. A null value means no limit.
     * @return list of task status matching the criteria, ordered by task id.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    List<TimerStatus<?>> findTimerStatus(String appName, String pattern, Character escape, TaskState state, boolean inState, Long minId, Integer maxResults) throws Exception;

    /**
     * Returns the expected next execution of the task with the specified id.
     * 
     * @param taskId unique identifier for the task.
     * @return the expected next execution of the task with the specified id.
     *         If the task is not found or has ended then <code>null</code> is returned.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    Date getNextExecutionTime(long taskId) throws Exception;

    /**
     * Returns the serializable timer task/trigger for the EJB timer task.
     * 
     * @param taskId unique identifier for the task.
     * @return the task/trigger (if any) for the EJB timer task with the specified id.
     *         If the task is not found or has ended then <code>null</code> is returned.
     * @throws ClassNotFoundException if the class of a serialized object cannot be found.
     * @throws IOException if an error occurs during deserialization of the <code>Trigger</code>.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    TimerTrigger getTimer(long taskId) throws ClassNotFoundException, IOException, Exception;

    /**
     * Returns status for the persistent task with the specified id.
     * 
     * @param taskId unique identifier for the task.
     * @return status for the persistent task with the specified id.
     *         If the task is not found, <code>null</code> is returned.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    <T> TimerStatus<T> getTimerStatus(long taskId) throws Exception;

    /**
     * Indicates whether or not fail over is enabled.
     *
     * @return true if fail over is enabled, otherwise false.
     */
    boolean isFailOverEnabled();

    /**
     * Removes all persisted properties that match the specified name pattern.
     * For example, to remove properties with names that start with "MY_PROP_NAME_",
     * executor.removeProperties("MY\\_PROP\\_NAME\\_%", '\\');
     * 
     * @param pattern name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character)
     * @param escape escape character that indicates when matching characters like % and _ should be interpreted literally.
     *            A value of null avoids designating an escape character, in which case the behavior depends on the persistent store.
     * @return number of properties removed.
     * @throws Exception if an error occurs accessing the persistent store.
     */
    int removeProperties(String pattern, Character escape) throws Exception;

    /**
     * Removes the information for the specified task from the persistent store.
     * This implicitly cancels further executions of the task if not already completed.
     * 
     * @param taskId unique identifier for a task.
     * @return <code>true</code> if the information for the task was removed.
     *         <code>false</code> if the task is not found in the persistent store.
     * @throws Exception if an error occurs updating the persistent store.
     */
    boolean removeTimer(long taskId) throws Exception;

    /**
     * Remove all tasks that match the specified application and name pattern and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to remove all scheduled tasks for app1 that have a name that starts with "mod_1#",
     * executor.removeTimers("app1", "mod\\_1#%", '\\', TaskRecord.State.SCHEDULED, true);
     * 
     * @param appName name of the application that owns the task. If null, the application name is not used to narrow the search.
     * @param pattern task name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character)
     * @param escape escape character that indicates when matching characters like % and _ should be interpreted literally.
     * @param state a task state. For example, TaskRecord.State.UNATTEMPTED.
     * @param inState indicates whether to remove tasks with or without the specified state
     * @return count of tasks removed.
     * @throws Exception if an error occurs updating the persistent store.
     */
    int removeTimers(String appName, String pattern, Character escape, TaskState state, boolean inState) throws Exception;

    /**
     * Schedules a task for execution according to the Trigger that it implements.
     * 
     * @param timerTrigger Serializable Callable or Runnable that is also a ManagedTask and Trigger.
     * @return status for the task.
     * @throws Exception if an error occurs updating the persistent store.
     */
    <T> TimerStatus<T> schedule(TimerTrigger timerTrigger) throws Exception;
}
