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

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * Trigger with varying delays between executions.
 */
public class VaryingRepeatTrigger implements Serializable, Trigger {
    private static final long serialVersionUID = -3825491020218369320L;

    private int executionAttemptNumber;
    private final long[] intervals;

    /**
     * Construct a trigger with varying delays between execution attempts.
     * 
     * @param intervals intervals between task execution attempts
     */
    VaryingRepeatTrigger(long... intervals) {
        this.intervals = intervals;
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        int index = executionAttemptNumber++;

        if (index >= intervals.length)
            return null; // no more executions

        Date d = lastExecution == null ? taskScheduledTime : lastExecution.getRunEnd();
        return new Date(d.getTime() + intervals[index]);
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}
