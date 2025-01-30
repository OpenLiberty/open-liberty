/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.transactionobserved.web;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@Startup
@Singleton
public class TimerBean {

    @Inject
    private Event<EventDto> event;

    private static EventDto dto = new EventDto();

    @Schedule(hour = "*", minute = "*", second = "*/5", persistent = false)
    public void timer() {
        event.fire(dto);
    }
}
