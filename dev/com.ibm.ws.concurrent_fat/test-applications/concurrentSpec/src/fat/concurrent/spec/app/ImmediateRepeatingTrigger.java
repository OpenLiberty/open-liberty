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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * A simple trigger that schedules a task to run as quickly as possible for a fixed number of times.
 */
class ImmediateRepeatingTrigger implements Trigger {
    final static int NO_FAILURE = -1;
    volatile boolean failSkips;
    final int maxExecutions;
    final int numExecutionToFailOn;
    volatile int numSkipped;
    final Set<LastExecution> previousExecutions = Collections.newSetFromMap(new ConcurrentHashMap<LastExecution, Boolean>());
    final int[] skipExecutions;

    // maxExecutions includes both executions that actually occur and executions that are skipped
    ImmediateRepeatingTrigger(int maxExecutions, int numExecutionToFailOn, int... skipExecutions) {
        this.maxExecutions = maxExecutions;
        this.numExecutionToFailOn = numExecutionToFailOn;
        this.skipExecutions = skipExecutions;
        Arrays.sort(skipExecutions);
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (lastExecution != null)
            previousExecutions.add(lastExecution);
        if (numExecutionToFailOn == previousExecutions.size() + numSkipped)
            throw new IllegalStateException("Intentionally failing execution #" + numExecutionToFailOn);
        return previousExecutions.size() + numSkipped < maxExecutions ? new Date() : null;
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        if (lastExecution != null)
            previousExecutions.add(lastExecution);
        // For this to work, an assumption is made that skipRun is only invoked once per execution.
        // Our implementation currently does that, but there is no spec requirement for it.
        boolean skip = Arrays.binarySearch(skipExecutions, previousExecutions.size() + numSkipped + 1) >= 0;
        if (skip) {
            numSkipped++;
            if (failSkips)
                throw new NegativeArraySizeException("Intentionally failing skipRun");
        }
        return skip;
    }
}