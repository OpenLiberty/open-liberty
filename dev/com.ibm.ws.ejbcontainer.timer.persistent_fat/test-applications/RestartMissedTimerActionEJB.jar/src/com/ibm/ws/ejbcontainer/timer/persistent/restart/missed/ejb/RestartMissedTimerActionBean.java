/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.persistent.restart.missed.ejb;

import static javax.ejb.ConcurrencyManagementType.BEAN;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;

@Singleton
@Startup
@ConcurrencyManagement(BEAN) // allows timeout and verify methods concurrently
public class RestartMissedTimerActionBean implements RestartMissedTimerAction {
    private static final Logger logger = Logger.getLogger(RestartMissedTimerActionBean.class.getName());

    private static final long INTERVAL = 1000; // 1 second
    private static final int TIMER_CHECK_COUNT = 10;
    private static final long BEAN_START_TIME = System.currentTimeMillis();

    private static volatile CountDownLatch firstTimeoutLatch = new CountDownLatch(1);
    private static volatile CountDownLatch timerLatch = new CountDownLatch(TIMER_CHECK_COUNT);
    private static volatile ArrayList<Date> nextTimeouts = new ArrayList<Date>();
    private static volatile ArrayList<Long> timeRemains = new ArrayList<Long>();

    @Resource
    private TimerService timerService;

    @PostConstruct
    void postConstruct() {
        logger.info("RestartMissedTimerActionBean started : " + BEAN_START_TIME);
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void createIntervalTimer(boolean waitForExpiration) {
        Timer timer = timerService.createIntervalTimer(0, INTERVAL, null);
        logger.info("RestartMissedTimerActionBean.createIntervalTimer : " + timer);
        if (waitForExpiration) {
            try {
                logger.info("RestartMissedTimerActionBean.createIntervalTimer : waiting for timer to run once");
                firstTimeoutLatch.await(2, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new EJBException("Timer failed to run in 2 minutes : e", e);
            }
        }
    }

    @Override
    public void cancelAllTimers() {
        Collection<Timer> timers = timerService.getTimers();
        logger.info("RestartMissedTimerActionBean.cancelAllTimers : " + timers.size());
        for (Timer timer : timers) {
            try {
                timer.cancel();
            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
            }
        }
        timerLatch = new CountDownLatch(TIMER_CHECK_COUNT);
        nextTimeouts = new ArrayList<Date>();
        timeRemains = new ArrayList<Long>();
    }

    @Override
    public void verifyMissedTimerAction(String missedTimerAction) {
        // Wait for the timer to run a few times (TIMER_CHECK_COUNT) after server start
        try {
            logger.info("RestartMissedTimerActionBean.verifyMissedTimerAction : waiting for latch...");
            timerLatch.await(3, TimeUnit.MINUTES);
            logger.info("RestartMissedTimerActionBean.verifyMissedTimerAction : latch obtained");
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new EJBException("Timers failed to run in 3 minutes : e", e);
        }

        assertEquals("Expected number of timer expirations did not occurred", TIMER_CHECK_COUNT, nextTimeouts.size());
        Date firstNextTimeout = nextTimeouts.get(0);
        assertNotNull("Unexpected null for getNextTimeout()", firstNextTimeout);
        long nextExpected = firstNextTimeout.getTime();

        if ("ALL".equals(missedTimerAction)) {
            // next timeout should be in the past, prior to server restart
            // all subsequent will be exactly the repeat interval later (still in the past)
            assertTrue("Next timeout should be prior to server start : " + nextExpected + " < " + BEAN_START_TIME, nextExpected < BEAN_START_TIME);
            nextExpected = nextExpected - INTERVAL;
            for (int index = 0; index < nextTimeouts.size(); index++) {
                Date nextTimeout = nextTimeouts.get(index);
                assertNotNull("Unexpected null for getNextTimeout() on result #" + index, nextTimeout);
                long interval = nextTimeout.getTime() - nextExpected;
                assertEquals("getNextTimeout() interval not expected on result #" + index, INTERVAL, interval);
                nextExpected = nextTimeout.getTime();
            }
        } else if ("ONCE".equals(missedTimerAction)) {
            // next timeout should be after server application start
            // all subsequent will be exactly the repeat interval later (still in the past)
            assertTrue("Next timeout should be after server start : " + nextExpected + " >= " + BEAN_START_TIME,
                       nextExpected >= BEAN_START_TIME);
            nextExpected = nextExpected - INTERVAL;
            for (int index = 0; index < nextTimeouts.size(); index++) {
                Date nextTimeout = nextTimeouts.get(index);
                assertNotNull("Unexpected null for getNextTimeout() on result #" + index, nextTimeout);
                long interval = nextTimeout.getTime() - nextExpected;
                assertEquals("getNextTimeout() interval not expected on result #" + index, INTERVAL, interval);
                nextExpected = nextTimeout.getTime();
            }
        } else {
            fail("Unexpected missedPersistentTimerAction : " + missedTimerAction);
        }
    }

    @Timeout
    public void timeout(Timer timer) {
        Date nextTimeout = null;
        long timeRemaining = 0;
        try {
            nextTimeout = timer.getNextTimeout();
            timeRemaining = timer.getTimeRemaining();
        } catch (NoSuchObjectLocalException ex) {
            // NoSuchObjectLocalException can occur if timer is running when cancelled
            // by a concurrent thread. Possible when using bean managed transactions as
            // persistent executor will not hold lock on timer in DB.
            logger.info("RestartMissedTimerActionBean.timeout allowed ex : " + ex);
        }

        CountDownLatch latch = timerLatch;
        if (latch != null && latch.getCount() > 0) {
            nextTimeouts.add(nextTimeout);
            timeRemains.add(timeRemaining);
            logger.info("RestartMissedTimerActionBean.timeout : " + nextTimeout + ", " + timeRemaining);
            timerLatch.countDown();
            firstTimeoutLatch.countDown();
        } else {
            logger.info("RestartMissedTimerActionBean.timeout : " + nextTimeout + ", " + timeRemaining);
        }
    }

}
