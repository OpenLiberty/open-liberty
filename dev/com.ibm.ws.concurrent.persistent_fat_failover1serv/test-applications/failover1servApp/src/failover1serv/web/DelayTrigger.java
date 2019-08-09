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
package failover1serv.web;

import java.io.Serializable;
import java.util.Date;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * Simple trigger that causes a task to repeat with the specified delay between executions.
 */
public class DelayTrigger implements Serializable, Trigger {
    private static final long serialVersionUID = 1L;
    private final long initialDelayMS;
    private final long intervalMS;

    public DelayTrigger(long initialDelayMS, long intervalMS) {
        this.initialDelayMS = initialDelayMS;
        this.intervalMS = intervalMS;
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        return lastExecution == null
                        ? new Date(taskScheduledTime.getTime() + initialDelayMS)
                        : new Date(lastExecution.getRunStart().getTime() + intervalMS);
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}