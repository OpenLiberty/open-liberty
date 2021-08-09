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
 * Trigger that causes a task to run twice, once now, and once at the last representable point in time.
 * The first execution attempt is skipped.
 */
public class SkipFailingTrigger implements Trigger {
    private boolean skipped;

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        return skipped ? new Date(Long.MAX_VALUE) : taskScheduledTime;
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        skipped = true;
        throw new IllegalStateException("Intentionally failing skipRun");
    }
}
