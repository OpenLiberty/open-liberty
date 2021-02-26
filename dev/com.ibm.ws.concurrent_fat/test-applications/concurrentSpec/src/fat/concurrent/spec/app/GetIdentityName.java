/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package fat.concurrent.spec.app;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.Trigger;

/**
 * Combination task/trigger that returns the identity name that is supplied to the Trigger
 * by the managed scheduled executor. This enables us to test if the value specified for the IDENTITY_NAME
 * execution property is honored. The trigger schedules itself to run just long enough to record the
 * identity name, which should be 2 executions because the LastExecution is NULL until after the first execution.
 */
public class GetIdentityName implements Callable<String>, ManagedTask, Trigger {
    final Map<String, String> execProps = new TreeMap<String, String>();
    private final Set<String> identityNames = new TreeSet<String>();

    @Override
    public String call() {
        if (identityNames.size() == 1)
            return identityNames.iterator().next();
        else
            return identityNames.toString();
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
        if (lastExecution == null)
            return taskScheduledTime;
        else {
            Date nextRunTime = identityNames.isEmpty() ? new Date() : null;
            identityNames.add(lastExecution.getIdentityName());
            return nextRunTime;
        }
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        if (lastExecution != null)
            identityNames.add(lastExecution.getIdentityName());
        return false;
    }
}
