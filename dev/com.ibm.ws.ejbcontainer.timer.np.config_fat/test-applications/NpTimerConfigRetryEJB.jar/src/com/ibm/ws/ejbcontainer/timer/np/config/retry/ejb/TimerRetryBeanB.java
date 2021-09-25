/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.config.retry.ejb;

import static com.ibm.ws.ejbcontainer.timer.np.config.retry.ejb.TimerRetryDriverBean.INITIAL_DURATION;
import static com.ibm.ws.ejbcontainer.timer.np.config.retry.ejb.TimerRetryDriverBean.MAX_WAIT_TIME;

import java.util.ArrayList;
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
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionRolledbackLocalException;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import junit.framework.Assert;

@Stateless(name = "TimerRetryBeanB")
@LocalBean
public class TimerRetryBeanB {
    private static final String CLASS_NAME = TimerRetryBeanB.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static volatile int count = 0;
    public static ArrayList<Long> timestamps = new ArrayList<Long>();
    private static CountDownLatch timerLatch;
    private static boolean cancelTimers = false;
    private static String timerInfo;
    public static boolean timerExists;

    @Resource
    private TimerService ivTS;

    public void doWork(String testName, int retries) {
        svLogger.info("Entering TimerRetryBeanB.doWork() for test **" + testName + "**");

        timerExists = false;
        timerInfo = testName;

        // If test will have no limit to retries, then cancel when done
        if ("testConfiguredForEndlessRetry".equals(testName)) {
            cancelTimers = true;
        } else {
            cancelTimers = false;
        }

        // Latch to watch for initial timeout + retries
        timerLatch = new CountDownLatch(retries + 1);
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo("Timeout for test: **" + testName + "**");
        timerConfig.setPersistent(false);

        ivTS.createSingleActionTimer(INITIAL_DURATION, timerConfig);

        svLogger.info("Leaving TimerRetryBeanB.doWork()...");
    }

    @Timeout
    public void doTimeoutStuff(Timer timer) {
        svLogger.info("Entering TimerRetryBeanB.doTimeoutStuff(), with pre-execution count of **" + count + "**");

        count++;
        timestamps.add(Long.valueOf(System.currentTimeMillis()));

        svLogger.info("Intentionally throwing error from TimerRetryBeanB.doTimeoutStuff()...currenty retry count is **" + count + "**");
        timerLatch.countDown();
        throw new TransactionRolledbackLocalException("Intentional timer exception to force retry, with post-execution count of **" + count + "**");
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void waitForTimersAndCancel(long cancelDelay) {
        svLogger.info("Waiting for timer(s) to complete...");
        try {
            timerLatch.await(MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace(System.out);
        }

        svLogger.info("Wait for timer(s) completed, cancelling (" + cancelDelay + " delay) ...");

        // Some tests would like to add a delay before canceling
        // any timers to make sure retries don't occur, but
        // at least wait for a postInvoke delay.
        if (cancelDelay > 0) {
            FATHelper.sleep(cancelDelay);
        } else {
            FATHelper.sleep(FATHelper.POST_INVOKE_DELAY);
        }

        // This timer may have been cancelled if max retry was reached
        Collection<Timer> timers = ivTS.getTimers();

        svLogger.info("Timers found : " + timers.size());

        // If not just canceling timers; then wait for last retry, and ensure
        // the timer is canceled (i.e. not returned by getTimers
        if (!cancelTimers) {
            for (int attempts = 0; attempts < 10 && timers.size() > 0; ++attempts) {
                FATHelper.sleep(10000);
                timers = ivTS.getTimers();
                svLogger.info("Timers found : " + timers.size());
            }
        }

        if (timers.size() > 0) {
            svLogger.info("cancelling ...");
            for (Timer timer : timers) {
                try {
                    svLogger.info("timer:" + timer.toString());
                    svLogger.info("timer info:" + timer.getInfo().toString());
                    svLogger.info("testName: " + timerInfo);
                    timerExists |= timer.getInfo().toString().contains(timerInfo);
                    timer.cancel();
                } catch (NoSuchObjectLocalException ex) {
                    svLogger.info("Expected NoSuchObjectLocalException occurred :" + timer);
                }
            }
            if (!cancelTimers) {
                Assert.fail("Timer should not exist; expected timers to be automatically cancelled : " + timers.size());
            }
        }
    }
}
