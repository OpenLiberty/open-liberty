/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
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
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * Trigger that alternates between 1 and 2 second delays from the start of the most recent execution.
 */
public class AlternatingTrigger implements Trigger {
    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        long delaySeconds = lastExecution == null || ((Integer) lastExecution.getResult()) % 2 == 0 ? 1 : 2;
        Date delayFrom = lastExecution == null ? taskScheduledTime : lastExecution.getRunStart();
        return new Date(delayFrom.getTime() + TimeUnit.SECONDS.toMillis(delaySeconds));
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}
