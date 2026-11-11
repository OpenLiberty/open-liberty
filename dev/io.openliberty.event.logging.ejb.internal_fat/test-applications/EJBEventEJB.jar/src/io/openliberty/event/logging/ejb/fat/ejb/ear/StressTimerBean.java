/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.ear;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

/**
 * Stateless session bean (no-interface view) that owns a number of PROGRAMMATIC
 * non-persistent interval timers, each firing once a second. It is used to keep
 * EJB timer invocations continuously in flight while the test adds and removes the
 * eventLogging feature, to look for timing problems when the request timing
 * collaborator is set and unset on the EJB container.
 *
 * The timers are not persistent, so they do not survive an application restart.
 * {@link #getTimerCount()} lets the test confirm that the application was not restarted.
 *
 * Separate from {@link TimerBean} so that its invocations do not affect the exact
 * event counts expected by the other tests.
 *
 * Context info (EAR): EJBEventApp | EJBEventEJB | StressTimerBean | doStressTimeout
 */
@Stateless
@LocalBean
public class StressTimerBean {

    private static final Logger log = Logger.getLogger(StressTimerBean.class.getName());

    private static final String TIMER_INFO = "EJBEventStress";

    /** Each callback is held for this long so that calls are likely to be in flight at any instant. */
    private static final long CALLBACK_DURATION_MS = 100;

    @Resource
    private TimerService timerService;

    /**
     * Creates {@code count} interval timers with a one second interval and staggered
     * initial delays, so that the callbacks are spread across each second.
     */
    public void startTimers(int count) {
        for (int i = 0; i < count; i++) {
            timerService.createIntervalTimer(100 + (i * 100), 1000, new TimerConfig(TIMER_INFO, false));
        }
        log.info("StressTimerBean.startTimers() created " + count + " interval timers");
    }

    /** Cancels all timers created by {@link #startTimers(int)}. */
    public void stopTimers() {
        int cancelled = 0;
        for (Timer timer : timerService.getTimers()) {
            if (TIMER_INFO.equals(timer.getInfo())) {
                timer.cancel();
                cancelled++;
            }
        }
        log.info("StressTimerBean.stopTimers() cancelled " + cancelled + " interval timers");
    }

    /** Returns the number of active timers created by {@link #startTimers(int)}. */
    public int getTimerCount() {
        int count = 0;
        for (Timer timer : timerService.getTimers()) {
            if (TIMER_INFO.equals(timer.getInfo())) {
                count++;
            }
        }
        return count;
    }

    @Timeout
    public void doStressTimeout(Timer timer) {
        try {
            Thread.sleep(CALLBACK_DURATION_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
