/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.war;

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
 * PROGRAMMATIC timer bean embedded in the standalone WAR.
 * Context info (standalone WAR): EJBEventWar | TimerBeanInWar | doTimeout
 */
@Stateless
public class TimerBeanInWar {

    private static final Logger log = Logger.getLogger(TimerBeanInWar.class.getName());

    public static volatile CountDownLatch latch = new CountDownLatch(1);

    @Resource
    private TimerService timerService;

    public void triggerTimer() {
        latch = new CountDownLatch(1);
        timerService.createSingleActionTimer(200, new TimerConfig("EJBEventWar", false));
        log.info("TimerBeanInWar.triggerTimer() created single-action timer");
    }

    @Timeout
    public void doTimeout(Timer timer) {
        log.info("TimerBeanInWar.doTimeout() fired: " + timer.getInfo());
        latch.countDown();
    }

    public static void awaitTimer(long timeoutMs) throws InterruptedException {
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Timed out waiting for TimerBeanInWar.doTimeout()");
        }
    }
}
