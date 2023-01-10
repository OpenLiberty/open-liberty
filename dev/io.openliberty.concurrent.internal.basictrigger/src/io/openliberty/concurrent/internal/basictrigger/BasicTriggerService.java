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
package io.openliberty.concurrent.internal.basictrigger;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.TriggerService;

/**
 * Enables interoperability with the Trigger interface of Java EE Concurrency 1.0 and Jakarta Concurrency 2.0.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class BasicTriggerService implements TriggerService {
    private static final TraceComponent tc = Tr.register(BasicTriggerService.class);

    @Override
    public ZonedDateTime getNextRunTime(LastExecution lastExecution, ZonedDateTime taskScheduledTime, Trigger trigger) {
        Date nextExecutionDate = trigger.getNextRunTime(lastExecution, Date.from(taskScheduledTime.toInstant()));
        return nextExecutionDate == null //
                        ? null //
                        : nextExecutionDate.toInstant().atZone(taskScheduledTime.getZone());
    }

    @Override
    @Trivial
    public ZoneId getZoneId(Trigger trigger) {
        return ZoneId.systemDefault();
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, ZonedDateTime nextExecutionTime, Trigger trigger) {
        return trigger.skipRun(lastExecution, Date.from(nextExecutionTime.toInstant()));
    }
}
