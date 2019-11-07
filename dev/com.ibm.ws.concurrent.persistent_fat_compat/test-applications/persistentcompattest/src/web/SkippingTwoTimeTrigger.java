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

/**
 * Trigger that causes a task to run twice, once now, and once at the last representable point in time.
 * The first execution attempt is skipped.
 */
public class SkippingTwoTimeTrigger extends TwoTimeTrigger {
    private boolean skipped;

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (skipped)
            return new Date(Long.MAX_VALUE);
        else
            return super.getNextRunTime(lastExecution, taskScheduledTime);
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return skipped = lastExecution == null && scheduledRunTime.getTime() != Long.MAX_VALUE;
    }
}
