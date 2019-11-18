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
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * Combination trigger/task that relies on state only for the duration of the skipRun/call/getNextRunTime sequence
 * in order to control that the task runs 3 times, with at least 101ms between executions.
 */
public class TriggerTask implements Callable<Integer>, Trigger {
    static LinkedBlockingQueue<Integer> results = new LinkedBlockingQueue<Integer>();
    private int count;

    @Override
    public Integer call() throws Exception {
        int result = ++count;
        results.add(result);
        return result;
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (count < 3) {
            Date delayFrom = lastExecution == null ? taskScheduledTime : lastExecution.getRunEnd();
            return new Date(delayFrom.getTime() + 101l);
        } else
            return null;
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        count = lastExecution == null ? 0 : (Integer) lastExecution.getResult();
        return false;
    }
}
