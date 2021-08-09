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
 * Trigger that fails every attempt of getNextRunTime.
 * The first attempt can be optionally permitted by modifying allowGetNextRunTime before submitting the task.
 */
public class FailingTrigger implements Trigger {
    /**
     * After the first request to getNextRunTime, this value will always be defaulted to false
     * because we do not preserve the state.
     */
    transient boolean allowFirstGetNextRunTime;

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (allowFirstGetNextRunTime)
            return new Date();
        else
            throw new ArithmeticException("Intentionally failing this method to test what happens");

    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}
