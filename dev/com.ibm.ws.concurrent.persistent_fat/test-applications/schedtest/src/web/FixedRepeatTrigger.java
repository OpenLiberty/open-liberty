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
import java.util.Arrays;
import java.util.Date;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * Trigger that causes a task to execute a fixed number of times.
 */
public class FixedRepeatTrigger implements Serializable, Trigger {
    private static final long serialVersionUID = -2177979362720509818L;

    private final int executionAttemptLimit;
    private int executionAttemptNumber;
    private final long fixedDelay;
    private final int[] skips;

    /**
     * Construct a trigger with fixed delay between execution attempts.
     * 
     * @param executionAttemptLimit maximum number of execution attempts, including any skips.
     * @param fixedDelay interval between task execution attempts
     * @param skips list (sorting in ascending order) of indices of task executions attempts to skip, if any.
     *            For example, (1,2,4) means:
     *            skip the first execution attempt,
     *            skip the second execution attempt,
     *            run the task on the third execution attempt
     *            skip the fourth execution attempt,
     *            run the task on the fifth, sixth, and so on... execution attempts
     */
    FixedRepeatTrigger(int executionAttemptLimit, long fixedDelay, int... skips) {
        this.executionAttemptLimit = executionAttemptLimit;
        this.fixedDelay = fixedDelay;
        this.skips = skips;
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (++executionAttemptNumber > executionAttemptLimit)
            return null; // no more executions

        Date d = lastExecution == null ? taskScheduledTime : lastExecution.getRunEnd();
        return new Date(d.getTime() + fixedDelay);
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return Arrays.binarySearch(skips, executionAttemptNumber) >= 0;
    }
}
