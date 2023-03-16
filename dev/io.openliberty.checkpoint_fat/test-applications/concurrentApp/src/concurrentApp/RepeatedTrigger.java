/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrentApp;

import java.time.Instant;
import java.util.Date;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

public class RepeatedTrigger implements Trigger {

    private long delta;
    private Date startTime;

    public RepeatedTrigger(int delta, Instant currentTime) {
        this.delta = delta;
        this.startTime = Date.from(currentTime);
    }

    @Override
    public Date getNextRunTime(LastExecution lastExec, Date taskScheduledTime) {
        if (lastExec == null) {
            return this.startTime;
        }
        return new Date(lastExec.getScheduledStart().getTime() + delta);
    }

    @Override
    public boolean skipRun(LastExecution arg0, Date arg1) {
        //never skip
        return false;
    }

}
