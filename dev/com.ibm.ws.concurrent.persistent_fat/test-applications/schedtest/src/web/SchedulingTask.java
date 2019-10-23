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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.naming.InitialContext;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

/**
 * Task that schedules another task when it runs. The return value of call is the task id of the task that it schedules.
 */
public class SchedulingTask implements Callable<Long>, ManagedTask, Serializable {
    private static final long serialVersionUID = -243962895117926154L;

    private final Map<String, String> execProps = new TreeMap<String, String>();
    private int numExecutions;

    @Override
    public Long call() throws Exception {
        ++numExecutions;

        DBIncrementTask task = new DBIncrementTask(toString() + "-DBIncrementTask-" + numExecutions);
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());

        PersistentExecutor persistentExecutor = (PersistentExecutor) new InitialContext().lookup("java:comp/env/web.SchedulerFATServlet/scheduler");
        TaskStatus<Integer> status = persistentExecutor.schedule((Callable<Integer>) task, 25, TimeUnit.NANOSECONDS);
        return status.getTaskId();
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
