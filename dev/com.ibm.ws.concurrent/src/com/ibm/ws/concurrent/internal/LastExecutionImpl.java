/*******************************************************************************
 * Copyright (c) 2013,2022 IBM Corporation and others.
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
package com.ibm.ws.concurrent.internal;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.enterprise.concurrent.LastExecution;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Information about a single execution of a task.
 */
@Trivial
public class LastExecutionImpl implements LastExecution {
    private final ZonedDateTime endTime;
    private final String identityName;
    private final Object result;
    private final ZonedDateTime scheduledStartTime;
    private final ZonedDateTime startTime;

    /**
     * Construct a record of task execution.
     *
     * @param identityName       identity name for the task, if any.
     * @param scheduledStartTime the time scheduled for the task to start executing.
     * @param startTime          time when the task actually started executing.
     * @param endTime            time when the task completed executing.
     * @param result             result of the last execution of the task as of the time when this LastExecution instance was created.
     */
    LastExecutionImpl(String identityName, ZonedDateTime scheduledStartTime, ZonedDateTime startTime, ZonedDateTime endTime, Object result) {
        this.identityName = identityName;
        this.scheduledStartTime = scheduledStartTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.result = result;
    }

    /**
     * @see jakarta.enterprise.concurrent.LastExecution#getIdentityName()
     */
    @Override
    public final String getIdentityName() {
        return identityName;
    }

    /**
     * @see jakarta.enterprise.concurrent.LastExecution#getResult()
     */
    @Override
    public final Object getResult() {
        return result;
    }

    /**
     * @see jakarta.enterprise.concurrent.LastExecution#getRunEnd()
     */
    @Override
    public final Date getRunEnd() {
        return Date.from(endTime.toInstant());
    }

    /**
     * @see jakarta.enterprise.concurrent.LastExecution#getRunEnd(java.time.ZoneId)
     */
    public ZonedDateTime getRunEnd(ZoneId zone) {
        return endTime.withZoneSameInstant(zone);
    }

    /**
     * @see jakarta.enterprise.concurrent.LastExecution#getRunStart()
     */
    @Override
    public final Date getRunStart() {
        return Date.from(startTime.toInstant());
    }

    /**
     * @see jakarta.enterprise.concurrent.LastExecution#getRunStart(java.time.ZoneId)
     */
    public ZonedDateTime getRunStart(ZoneId zone) {
        return startTime.withZoneSameInstant(zone);
    }

    /**
     * @see jakarta.enterprise.concurrent.LastExecution#getScheduledStart()
     */
    @Override
    public final Date getScheduledStart() {
        return Date.from(scheduledStartTime.toInstant());
    }

    /**
     * @see jakarta.enterprise.concurrent.LastExecution#getScheduledStart(java.time.ZoneId)
     */
    public ZonedDateTime getScheduledStart(ZoneId zone) {
        return scheduledStartTime.withZoneSameInstant(zone);
    }

    /**
     * Returns a textual representation suitable for display in trace.
     *
     * @return a textual representation suitable for display in trace.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append(getClass().getSimpleName()).append(' ').append(identityName).append(' ') //
                        .append("scheduledStart=").append(scheduledStartTime).append(' ') //
                        .append("runStart=").append(startTime).append(' ') //
                        .append("runEnd=").append(endTime).append(' ') //
                        .append(result);
        return sb.toString();
    }
}
