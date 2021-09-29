/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.persistent.missed.ejb;

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

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

@Stateless
public class MissedTimerActionBean implements MissedTimerAction {
    private static final Logger logger = Logger.getLogger(MissedTimerActionBean.class.getName());

    private static final long INTERVAL = 1000; // 1 second
    private static final int SKIPS = 10; // number of skipped expirations
    private static final long DELAY = SKIPS * 1000; // 10 seconds

    private static volatile CountDownLatch timerLatch;
    private static volatile Date firstNextTimeout;
    private static volatile Long firstTimeRemaining;
    private static volatile ArrayList<Date> nextTimeouts;
    private static volatile ArrayList<Long> timeRemains;

    @Resource
    private TimerService timerService;

    @Override
    public void createIntervalTimer() {
        timerLatch = new CountDownLatch(SKIPS + 1);
        nextTimeouts = new ArrayList<Date>();
        timeRemains = new ArrayList<Long>();
        Timer timer = timerService.createIntervalTimer(0, INTERVAL, null);
        logger.info("MissedTimerActionBean.createIntervalTimer : " + timer);
    }

    @Override
    public void cancelAllTimers() {
        Collection<Timer> timers = timerService.getTimers();
        logger.info("MissedTimerActionBean.cancelAllTimers : " + timers.size());
        for (Timer timer : timers) {
            try {
                timer.cancel();
            } catch (NoSuchObjectLocalException nso) {

            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
            }
        }
        timerLatch = null;
        firstNextTimeout = null;
        firstTimeRemaining = null;
    }

    @Override
    public void verifyMissedTimerAction(String missedTimerAction) {
        // Wait for the timer to run a few times after a delay
        try {
            logger.info("MissedTimerActionBean.verifyMissedTimerAction : waiting for latch...");
            timerLatch.await(3, TimeUnit.MINUTES);
            logger.info("MissedTimerActionBean.verifyMissedTimerAction : latch obtained");
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new EJBException("Timers failed to run in 3 minutes : e", e);
        }

        if ("ALL".equals(missedTimerAction)) {
            long nextExpected = firstNextTimeout.getTime();
            for (Date nextTimeout : nextTimeouts) {
                assertNotNull("Unexpected null for getNextTimeout()", nextTimeout);
                long interval = nextTimeout.getTime() - nextExpected;
                assertEquals("getNextTimeout() interval not expected", INTERVAL, interval);
                nextExpected = nextTimeout.getTime();
            }
        } else if ("ONCE".equals(missedTimerAction)) {
            long nextExpected = firstNextTimeout.getTime();
            for (int index = 0; index < nextTimeouts.size(); index++) {
                Date nextTimeout = nextTimeouts.get(index);
                assertNotNull("Unexpected null for getNextTimeout() on result #" + index, nextTimeout);
                long interval = nextTimeout.getTime() - nextExpected;
                if (index == 0) {
                    assertTrue("getNextTimeout() not >= DELAY : " + interval + " >= " + DELAY, interval >= DELAY);
                } else {
                    // Interval needs to be > 0 and a multiple of INTERVAL.
                    // Normally would be INTERVAL, but on slow systems ONCE may skip to next future time.
                    if (interval > INTERVAL && interval % INTERVAL == 0) {
                        interval = INTERVAL;
                    }
                    assertEquals("getNextTimeout() interval not expected on result #" + index, INTERVAL, interval);
                }
                nextExpected = nextTimeout.getTime();
            }
        } else {
            fail("Unexpected missedPersistentTimerAction : " + missedTimerAction);
        }
    }

    @Timeout
    public void timeout(Timer timer) {
        if (firstNextTimeout == null) {
            firstNextTimeout = timer.getNextTimeout();
            firstTimeRemaining = timer.getTimeRemaining();
            logger.info("MissedTimerActionBean.timeout : delay for " + DELAY + " : " + firstNextTimeout + ", " + firstTimeRemaining);
            FATHelper.sleep(DELAY);
        } else {
            CountDownLatch latch = timerLatch;
            if (latch != null && latch.getCount() > 0) {
                nextTimeouts.add(timer.getNextTimeout());
                timeRemains.add(timer.getTimeRemaining());
                logger.info("MissedTimerActionBean.timeout : " + timer.getNextTimeout() + ", " + timer.getTimeRemaining());
                if (latch.getCount() == 1) {
                    logger.info("MissedTimerActionBean.timeout : countdown reached, canceling timer : " + timer);
                    timer.cancel();
                }
                timerLatch.countDown();
            }
        }
    }

}
