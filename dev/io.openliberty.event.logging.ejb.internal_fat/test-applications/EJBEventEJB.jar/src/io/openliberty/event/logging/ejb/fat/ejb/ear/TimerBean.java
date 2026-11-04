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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

/**
 * Stateless session bean with a PROGRAMMATIC single-action timer.
 * {@link #triggerTimer()} creates a timer that fires after 200 ms and
 * signals a static {@link CountDownLatch}. Tests create the timer via
 * HTTP, then wait on the latch before asserting the event log.
 *
 * Context info (EAR): EJBEventApp | EJBEventEJB | TimerBean | doTimeout
 */
@Stateless
public class TimerBean implements TimerBeanLocal {

    private static final Logger log = Logger.getLogger(TimerBean.class.getName());

    /** Signals that doTimeout() has been called. Reset by triggerTimer(). */
    public static volatile CountDownLatch latch = new CountDownLatch(1);

    @Resource
    private TimerService timerService;

    @Override
    public void triggerTimer() {
        latch = new CountDownLatch(1);
        timerService.createSingleActionTimer(200, new TimerConfig("EJBEventEar", false));
        log.info("TimerBean.triggerTimer() created single-action timer");
    }

    @Timeout
    public void doTimeout(Timer timer) {
        log.info("TimerBean.doTimeout() fired: " + timer.getInfo());
        latch.countDown();
    }

    /**
     * Blocks until the timer callback has fired, or throws if it does not arrive
     * within {@code timeoutMs} milliseconds.
     */
    public static void awaitTimer(long timeoutMs) throws InterruptedException {
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Timed out waiting for TimerBean.doTimeout()");
        }
    }
}
