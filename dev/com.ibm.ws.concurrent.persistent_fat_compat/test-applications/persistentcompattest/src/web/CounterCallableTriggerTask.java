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
 * Callable that increments and returns a counter each time it runs, up to 3 times.
 */
public class CounterCallableTriggerTask extends CounterCallable implements Trigger {
    private static final long serialVersionUID = -5686084710994254965L;

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (lastExecution == null)
            return taskScheduledTime;
        if (counter >= 3)
            return null;
        long delay = (lastExecution.getRunEnd().getTime() - lastExecution.getRunStart().getTime()) / ((Integer) lastExecution.getResult()).longValue();
        System.out.println("Delay for next execution of " + lastExecution.getIdentityName() + " will be " + delay);
        return new Date(System.currentTimeMillis() + delay);
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}
