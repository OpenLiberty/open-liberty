/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.Trigger;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;

/**
 * Combination task/trigger that runs with a fixed delay from each start time
 * and increments a counter each time it runs.
 */
public class FixedDelayTriggerTask implements Callable<Integer>, ManagedTask, Serializable, Trigger {
    private static final long serialVersionUID = 3557474666429186043L;

    // In-memory counter of attempts. This value does not roll back, and applies across multiple instances,
    // resetting itself whenever a new instance is created.
    static AtomicInteger inMemoryAttemptCounter = new AtomicInteger();

    // Persisted counter of attmepts. This value can be rolled back upon failure.
    private int counter;

    private final Map<String, String> execProps = new TreeMap<String, String>();
    private final long initialDelay;
    private final long interval;

    public FixedDelayTriggerTask(long initialDelay, long interval) {
        this.initialDelay = initialDelay;
        this.interval = interval;
        inMemoryAttemptCounter.set(0);
    }

    @Override
    public Integer call() throws Exception {
        inMemoryAttemptCounter.incrementAndGet();
        ++counter;
        System.out.println("Task " + TaskIdAccessor.get() + " execution attempt #" + counter);

        // Resource reference lookup requires jeeMetadataContext
        try {
            DataSource ds = (DataSource) new InitialContext().lookup("java:module/env/jdbc/persistcfgdbRef");
            ds.getLoginTimeout();
        } catch (Exception x) {
            x.printStackTrace();
            throw x;
        }

        return counter;
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
        return lastExecution == null
                        ? new Date(taskScheduledTime.getTime() + initialDelay)
                        : new Date(lastExecution.getRunStart().getTime() + interval);
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}
