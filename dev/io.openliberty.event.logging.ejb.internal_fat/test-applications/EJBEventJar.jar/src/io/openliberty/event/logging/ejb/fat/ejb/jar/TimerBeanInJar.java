/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.jar;

import java.util.logging.Logger;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;

/**
 * Singleton with an AUTOMATIC @Schedule timer that fires every second and
 * cancels itself after the first invocation.
 *
 * The test class verifies a websphere.ejb.timer END entry exists in messages.log
 * by searching from the beginning of the log — no latch, no servlet call, and
 * no mark needed. By the time the server has finished starting the timer has
 * already fired at least once.
 *
 * Context info (standalone JAR): EJBEventJar | TimerBeanInJar | doTimeout
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class TimerBeanInJar {

    private static final Logger log = Logger.getLogger(TimerBeanInJar.class.getName());

    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false, info = "EJBEventJarSchedule")
    public void doTimeout(Timer timer) {
        log.info("TimerBeanInJar.doTimeout() fired: " + timer.getInfo());
        timer.cancel();
    }
}
