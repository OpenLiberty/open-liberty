/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 * Trigger that computes the next execution time as 1 second after it last started.
 */
public class OneSecondTrigger implements Trigger {
    static Trigger INSTANCE = new OneSecondTrigger();

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        return lastExecution == null ? taskScheduledTime : new Date(lastExecution.getRunStart().getTime() + TimeUnit.SECONDS.toMillis(1));
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}
