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
package test.feature.sim.ejb.timer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedTaskListener;

import com.ibm.ws.concurrent.persistent.ejb.TimerTrigger;

/**
 * Simulate an EJB timer task/trigger which supplies the application name and classloader.
 * The result of this task is the number of executions remaining.
 */
public class TimerTriggerTask implements Callable<Integer>, TimerTrigger {
    private static final long serialVersionUID = -8553022836317438689L;

    private transient String appName;
    private transient ClassLoader classloader;
    private final Map<String, String> execProps;
    private final Queue<Date> nextExecTimes = new LinkedList<Date>();

    public TimerTriggerTask(String taskName, Date... nextExecTimes) {
        appName = "schedtest";
        classloader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
        execProps = Collections.singletonMap(IDENTITY_NAME, taskName);
        for (Date date : nextExecTimes)
            this.nextExecTimes.add(date);
    }

    @Override
    public Integer call() throws Exception {
        return nextExecTimes.size();
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classloader;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return execProps;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        Date nextExecTime = nextExecTimes.poll();
        return nextExecTime == null ? null : nextExecTime;
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}
