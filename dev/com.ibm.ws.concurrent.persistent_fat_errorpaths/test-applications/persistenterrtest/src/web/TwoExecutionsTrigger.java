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
 * A trigger causes a task to execute exactly twice.
 */
public class TwoExecutionsTrigger implements Serializable, Trigger {
    private static final long serialVersionUID = -7941982645314578917L;

    private Date firstExecEndTime;

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (lastExecution == null)
            return new Date();
        Object result = lastExecution.getResult();
        if (result == null || result.getClass().getSimpleName().equals("TaskFailure"))
            throw new IllegalArgumentException("Result is missing or incorrect " + result);
        Date endTime = lastExecution.getRunEnd();
        if (firstExecEndTime == null) {
            firstExecEndTime = endTime;
            return new Date();
        } else if (firstExecEndTime.equals(endTime))
            return new Date();
        else
            return null;
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        if (lastExecution != null) {
            Object result = lastExecution.getResult();
            System.out.println("TwoExecutionsTrigger.skipRun lastExecution result: " + result);
            if (result != null && result.getClass().getSimpleName().equals("TaskFailure"))
                throw new IllegalArgumentException("Internal TaskFailure class should never be returned to application. " + result);
        }
        return false;
    }
}