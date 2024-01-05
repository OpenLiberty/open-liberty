/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.cdi.interceptor;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.enterprise.concurrent.CronTrigger;

/**
 * Inherits from the CronTrigger class to expose the next(ZonedDateTime) method
 * so that multiple triggers can compute from the same time.
 */
class ScheduleCronTrigger extends CronTrigger {

    @Trivial
    ScheduleCronTrigger(String cron, ZoneId zoneId) {
        super(cron, zoneId);
    }

    @Trivial
    ScheduleCronTrigger(ZoneId zoneId) {
        super(zoneId);
    }

    @Override
    protected ZonedDateTime next(ZonedDateTime now) {
        return super.next(now);
    }

    @Trivial
    @Override
    public String toString() {
        // ScheduleCronTrigger@...
        return "Schedule" + super.toString();
    }
}