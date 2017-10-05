/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * Trigger to be shared by multiple schedule operations, which schedules a number of executions that is encoded
 * into the Identity Name of the task.
 */
public class MultiTrigger implements Trigger {
    private final Map<String, Integer> numExecutions = new ConcurrentHashMap<String, Integer>();

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (lastExecution == null)
            return taskScheduledTime;

        String identityName = lastExecution.getIdentityName();
        if (identityName == null)
            return null;

        // increment the count of executions after running task
        Integer execNum = numExecutions.get(identityName);
        if (execNum == null)
            numExecutions.put(identityName, execNum = 1);
        else
            numExecutions.put(identityName, ++execNum);

        // parse the execution limit from the identity name
        String executionLimit = identityName.substring(identityName.lastIndexOf('-') + 1);
        int execLimit = Integer.parseInt(executionLimit);

        if (execNum >= execLimit)
            return null;
        else
            return new Date();
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        if (lastExecution == null)
            System.out.println("About to run task for first time: " + this);
        else {
            String identityName = lastExecution.getIdentityName();
            Integer prevExecNum = numExecutions.get(identityName);
            System.out.println("About to run task " + this + " which has previously run " + prevExecNum + " times. Most recent execution was " + lastExecution);
        }
        return false;
    }
}
