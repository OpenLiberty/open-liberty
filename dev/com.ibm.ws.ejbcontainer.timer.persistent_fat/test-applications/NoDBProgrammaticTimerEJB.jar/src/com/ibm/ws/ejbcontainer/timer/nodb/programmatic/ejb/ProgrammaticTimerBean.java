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
package com.ibm.ws.ejbcontainer.timer.nodb.programmatic.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

/**
 * A simple stateless bean with programmatically created timers and methods
 * to verify the timers run as expected.
 */
@Stateless
@LocalBean
public class ProgrammaticTimerBean {

    private static final Logger logger = Logger.getLogger(ProgrammaticTimerBean.class.getName());

    private static final CountDownLatch timerLatch = new CountDownLatch(1);
    private static long POST_INVOKE_FUDGE_FACTOR = 400; //ms

    @Resource
    private TimerService ts;

    @Timeout
    public void timeout(Timer timer) {
        logger.info("timeout : " + timer);
        timerLatch.countDown();
    }

    public void createNonPersistentTimers() {
        logger.info("> createNonPersistentTimers");

        // Create a non-persistent timer that will expire in 1 second
        logger.info("creating non-persistent timer to expire in 1 second");
        TimerHelper.createTimer(ts, 1 * 1000l, null, "ProgrammaticTimerBean", false, null);

        // Create a non-persistent timer that will not expire
        logger.info("creating non-persistent timer to expire in 1 hour");
        TimerHelper.createTimer(ts, 60 * 60 * 1000l, null, "Future-ProgrammaticTimerBean", false, null);

        // Attempt to create a persistent timer
        logger.info("creating persistent timer; should fail");
        try {
            TimerHelper.createTimer(ts, 1 * 1000l, null, "Persistent-ProgrammaticTimerBean", true, null);
            fail("Expected IllegalStateException did not occur");
        } catch (IllegalStateException ex) {
            logger.info("caught expected exception : " + ex);
        }

        logger.info("< createNonPersistentTimers");
    }

    public void createPersistentTimer() {
        logger.info("> createPersistentTimer");

        // Attempt to create a persistent timer
        logger.info("creating persistent timer; should fail");
        try {
            TimerHelper.createTimer(ts, 1 * 1000l, null, "Persistent-ProgrammaticTimerBean", true, null);
            fail("Expected IllegalStateException did not occur");
        } catch (IllegalStateException ex) {
            logger.info("caught expected exception : " + ex);
        }

        logger.info("< createPersistentTimer");
    }

    public boolean waitForTimer() {
        logger.info("> waitForTimer");
        try {
            timerLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        FATHelper.sleep(POST_INVOKE_FUDGE_FACTOR);
        boolean timedout = timerLatch.getCount() == 0;
        logger.info("< waitForTimer : " + timedout);
        return timedout;
    }

    public void verifyTimers(int size) {
        Collection<Timer> timers = ts.getTimers();
        assertEquals("getTimers wrong number of timers", size, timers.size());

        timers = ts.getAllTimers();
        assertEquals("getAllTimers wrong number of timers", size, timers.size());
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
