/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.naming.InitialContext;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

/**
 * Task that cancels other tasks.
 * Each execution attempt return a list of ids of tasks that were successfully canceled.
 */
public class CancelingTask implements Callable<List<Long>>, ManagedTask, Serializable {
    private static final long serialVersionUID = 4128678798228227218L;

    private final Map<String, String> execProps = new TreeMap<String, String>();
    private int numExecutions;

    // Map of execution attempt number to tasks ids to cancel on that execution attempt.
    Map<Integer, Long[]> taskIdsToCancel = new TreeMap<Integer, Long[]>();

    @Override
    public List<Long> call() throws Exception {
        List<Long> canceledIds = new LinkedList<Long>();
        Long[] taskIds = taskIdsToCancel.get(++numExecutions);
        if (taskIds != null) {
            PersistentExecutor persistentExecutor = (PersistentExecutor) new InitialContext().lookup("java:comp/env/web.SchedulerFATServlet/scheduler");
            for (Long taskId : taskIds) {
                TaskStatus<Integer> status = persistentExecutor.getStatus(taskId);
                if (status != null) {
                    if (status.cancel(false))
                        canceledIds.add(taskId);

                    status = persistentExecutor.getStatus(taskId);
                    if (status != null && (!status.isCancelled() || !status.isDone()))
                        throw new Exception("Task status not properly updated or autopurged after cancel. " + status);
                }
            }
        }
        return canceledIds;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return execProps;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }
}
