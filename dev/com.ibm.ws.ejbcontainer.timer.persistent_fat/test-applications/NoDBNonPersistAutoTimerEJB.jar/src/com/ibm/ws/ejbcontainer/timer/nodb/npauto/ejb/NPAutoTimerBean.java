/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.nodb.npauto.ejb;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;

/**
 * A simple stateless bean with non-persistent automatically created timers and methods
 * to verify the timers run as expected.
 */
@Stateless
@LocalBean
public class NPAutoTimerBean {

    private static final Logger logger = Logger.getLogger(NPAutoTimerBean.class.getName());

    private static final CountDownLatch autoTimerLatch = new CountDownLatch(1);

    @Resource
    private TimerService ts;

    @Schedule(hour = "*", minute = "*", second = "*", info = "NPAutoTimerBean", persistent = false)
    public void timeout(Timer timer) {
        logger.info("timeout : " + timer);
        autoTimerLatch.countDown();
        timer.cancel();
    }

    @Schedule(year = "2075", hour = "*", minute = "*", second = "*", info = "Future-NPAutoTimerBean", persistent = false)
    public void futureTimeout(Timer timer) {
        logger.info("futureTimeout : " + timer);
        timer.cancel();
    }

    public boolean waitForAutomaticTimer() {
        logger.info("> waitForAutomaticTimer");
        try {
            autoTimerLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean timedout = autoTimerLatch.getCount() == 0;
        logger.info("< waitForAutomaticTimer : " + timedout);
        return timedout;
    }

    public void verifyTimers() {
        Collection<Timer> timers = ts.getTimers();
        assertEquals("getTimers wrong number of timers", 1, timers.size());

        timers = ts.getAllTimers();
        assertEquals("getAllTimers wrong number of timers", 1, timers.size());
    }

    public void clearAllTimers() {
        for (Timer timer : ts.getTimers()) {
            try {
                logger.info("clearAllTimers : " + timer);
                timer.cancel();
            } catch (NoSuchObjectLocalException ex) {
                logger.info("clearAllTimers : " + ex);
            }
        }
    }
}
