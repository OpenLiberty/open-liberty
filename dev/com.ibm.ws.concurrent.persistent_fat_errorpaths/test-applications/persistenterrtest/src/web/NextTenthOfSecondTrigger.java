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
package web;

import java.util.Date;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * A one-shot trigger that computes the next execution time to be at the next tenth of a second.
 */
public class NextTenthOfSecondTrigger implements Trigger {
    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        if (lastExecution == null)
            return new Date((taskScheduledTime.getTime() / 100 + 1) * 100);
        else
            return null;
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }
}
