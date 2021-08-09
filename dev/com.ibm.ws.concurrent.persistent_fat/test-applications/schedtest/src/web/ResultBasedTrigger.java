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

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * Stateless trigger that uses the last execution result to decide whether or not to run again.
 */
public class ResultBasedTrigger implements Trigger {
    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (lastExecution == null)
            return taskScheduledTime;

        // Keep running until we find a result of 2
        Number result = (Number) lastExecution.getResult();
        return result == null || result.longValue() != 2l
                        ? new Date(lastExecution.getRunEnd().getTime() + 100)
                        : null;
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}