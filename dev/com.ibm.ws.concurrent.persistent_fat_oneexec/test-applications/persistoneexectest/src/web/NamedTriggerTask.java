/*******************************************************************************
 * Copyright (c) 2014, 2015, 2019 IBM Corporation and others.
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
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.Trigger;

/**
 * Named task that runs once.
 */
public class NamedTriggerTask implements ManagedTask, Runnable, Serializable, Trigger {
    private static final long serialVersionUID = -1457091671272181856L;

    private final Map<String, String> execProps;
    private final Date execTime;

    NamedTriggerTask(String name, long delay) {
        execProps = Collections.singletonMap(ManagedTask.IDENTITY_NAME, name);
        execTime = new Date(System.currentTimeMillis() + delay);
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
        return lastExecution == null ? execTime : null;
    }

    @Override
    public void run() {
        System.out.println("Task " + execProps.get(ManagedTask.IDENTITY_NAME) + " is running");
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}
