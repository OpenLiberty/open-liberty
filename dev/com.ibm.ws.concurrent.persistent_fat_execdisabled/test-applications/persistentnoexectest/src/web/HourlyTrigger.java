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
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * Trigger that runs a task every hour after the time when first scheduled.
 */
public class HourlyTrigger implements Serializable, Trigger {
    private static final long serialVersionUID = -6342417787871937187L;

    private long executionCount;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HourlyTrigger && ((HourlyTrigger) obj).executionCount == executionCount;
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        // Add 1 hour for each time invoked
        return new Date(taskScheduledTime.getTime() + (++executionCount) * TimeUnit.HOURS.toMillis(1));
    }

    @Override
    public int hashCode() {
        return (int) executionCount;
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}
