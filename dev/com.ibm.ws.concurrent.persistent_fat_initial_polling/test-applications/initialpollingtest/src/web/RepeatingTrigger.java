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

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * Simple trigger that causes a task to repeat at an interval.
 */
public class RepeatingTrigger implements Serializable, Trigger {
    private static final long serialVersionUID = 3512152814552775239L;

    private final long initialDelay;
    private final long interval;

    public RepeatingTrigger(long initialDelay, long interval) {
        this.initialDelay = initialDelay;
        this.interval = interval;
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
