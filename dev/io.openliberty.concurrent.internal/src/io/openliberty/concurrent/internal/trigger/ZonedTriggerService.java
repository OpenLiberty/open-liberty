/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.trigger;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.TriggerService;

import jakarta.enterprise.concurrent.LastExecution;
import jakarta.enterprise.concurrent.Trigger;
import jakarta.enterprise.concurrent.ZonedTrigger;

/**
 * Enables interoperability with ZonedTrigger of Jakarta Concurrency 3.0+.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class ZonedTriggerService implements TriggerService {
    private static final TraceComponent tc = Tr.register(ZonedTriggerService.class);

    @Override
    public ZonedDateTime getNextRunTime(LastExecution lastExecution, ZonedDateTime taskScheduledTime, Trigger trigger) {
        ZonedDateTime nextExecTime;
        if (trigger instanceof ZonedTrigger) {
            nextExecTime = ((ZonedTrigger) trigger).getNextRunTime(lastExecution, taskScheduledTime);
        } else {
            Date nextExecutionDate = trigger.getNextRunTime(lastExecution, Date.from(taskScheduledTime.toInstant()));
            nextExecTime = nextExecutionDate == null //
                            ? null //
                            : nextExecutionDate.toInstant().atZone(taskScheduledTime.getZone());
        }
        return nextExecTime;
    }

    @Override
    @Trivial
    public ZoneId getZoneId(Trigger trigger) {
        ZoneId zone = trigger instanceof ZonedTrigger //
                        ? ((ZonedTrigger) trigger).getZoneId() //
                        : ZoneId.systemDefault();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getZoneId", zone);
        return zone;
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, ZonedDateTime nextExecutionTime, Trigger trigger) {
        boolean skip;
        if (trigger instanceof ZonedTrigger) {
            skip = ((ZonedTrigger) trigger).skipRun(lastExecution, nextExecutionTime);
        } else {
            skip = trigger.skipRun(lastExecution, Date.from(nextExecutionTime.toInstant()));
        }
        return skip;
    }
}
