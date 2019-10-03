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
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * A trigger that causes task to run immediately, and can be configured to skip some executions.
 */
public class ImmediateSkippingTrigger implements Serializable, Trigger {
    private static final long serialVersionUID = 7626632247620885155L;

    private int executionAttemptCounter;
    private final int maxExecutionAttempts;
    final Set<Integer> skipExecutionAttempts = new TreeSet<Integer>();
    final Set<Integer> skipExecutionAttemptsWithFailure = new TreeSet<Integer>();

    ImmediateSkippingTrigger(int maxExecutionAttempts) {
        this.maxExecutionAttempts = maxExecutionAttempts;
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (executionAttemptCounter >= maxExecutionAttempts)
            return null;
        else
            return new Date();
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        ++executionAttemptCounter;
        if (skipExecutionAttempts.contains(executionAttemptCounter))
            return true;
        if (skipExecutionAttemptsWithFailure.contains(executionAttemptCounter))
            throw new ArrayIndexOutOfBoundsException("Intentionally caused failure on execution attempt " + executionAttemptCounter + ".");
        else
            return false;
    }
}