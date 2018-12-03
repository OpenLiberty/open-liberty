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
package com.ibm.ws.ejbcontainer.timer.nodb.pauto.ejb;

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
 * A simple stateless bean with persistent automatically created timers and methods
 * to verify the timers would run as expected if persistent timers supported.
 */
@Stateless
@LocalBean
public class PAutoTimerBean {

    private static final Logger logger = Logger.getLogger(PAutoTimerBean.class.getName());

    private static final CountDownLatch autoTimerLatch = new CountDownLatch(1);

    @Resource
    private TimerService ts;

    @Schedule(hour = "*", minute = "*", second = "*", info = "PAutoTimerBean", persistent = true)
    public void timeout(Timer timer) {
        logger.info("timeout : " + timer);
        autoTimerLatch.countDown();
        timer.cancel();
    }

    // Intentionally non-persistent; should not matter since application should fail to start
    @Schedule(year = "2075", hour = "*", minute = "*", second = "*", info = "Future-PAutoTimerBean", persistent = false)
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
