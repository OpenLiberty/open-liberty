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

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * A trigger that causes tasks to run immediately, and can be configured to skip some executions.
 * State is shared across all instances and is not persisted.
 * Tests that use this are required to clear it (whether successful or not)
 * before returning so as not to interfere with other tests.
 */
public class SharedSkippingTrigger implements Trigger {
    private static AtomicInteger executionAttemptCounter = new AtomicInteger();
    final static Set<Integer> skipExecutionAttempts = new TreeSet<Integer>();

    static void clear() {
        executionAttemptCounter.set(0);
        skipExecutionAttempts.clear();
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        return new Date(System.currentTimeMillis() + 500);
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        int attemptNumber = executionAttemptCounter.incrementAndGet();
        if (skipExecutionAttempts.contains(attemptNumber))
            return true;
        else
            return false;
    }
}