/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Invokes trigger operations on a Concurrency 3.0+ ZonedTrigger or Concurrency 1.0/2.0 Trigger,
 * depending on which feature is enabled and whether the trigger implements ZonedTrigger.
 */
public interface TriggerService {
    /**
     * Invokes getNextRuntime on the Trigger, using the signature with ZonedDateTime if possible.
     *
     * @param previous          information about the previous execution. Null if no previous execution.
     * @param taskScheduledTime time at which the scheduling of the task occurred.
     * @param trigger           Trigger or ZonedTrigger.
     * @return next time to run the task. Null if the task should not run again.
     */
    // TODO switch Object to LastExecution and Trigger once projects can compile against Jakartified artifacts
    ZonedDateTime getNextRunTime(Object lastExecution, ZonedDateTime taskScheduledTime, Object trigger);

    /**
     * Returns the time zone id for the trigger.
     *
     * @param trigger Trigger or ZonedTrigger.
     * @return ZonedId of the ZonedTrigger. Otherwise the default ZoneId.
     */
    // TODO switch Object to Trigger once projects can compile against Jakartified artifacts
    ZoneId getZoneId(Object trigger);

    /**
     * Invokes skipRun on the Trigger, using the signature with ZonedDateTime if possible.
     *
     * @param lastExecution     information about the previous execution. Null if no previous execution.
     * @param nextExecutionTime the target time for the task execution to start at.
     * @param trigger           Trigger or ZonedTrigger.
     * @return true if the execution of the task should be skipped. Otherwise false.
     */
    // TODO switch Object to LastExecution and Trigger once projects can compile against Jakartified artifacts
    boolean skipRun(Object lastExecution, ZonedDateTime nextExecutionTime, Object trigger);
}
