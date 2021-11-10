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

@Stateless(name = "TimerRetryBeanC")
@LocalBean
public class TimerRetryBeanC {
    private static final String CLASS_NAME = TimerRetryBeanC.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static volatile int count = 0;
    public static ArrayList<Long> timestamps = new ArrayList<Long>();
    private static CountDownLatch timerLatch;

    @Resource
    private TimerService ivTS;

    public void doWork(String testName) {
        svLogger.info("Entering TimerRetryBeanC.doWork() for test **" + testName + "**");

        timerLatch = new CountDownLatch(1);
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo("Timeout for test: **" + testName + "**");
        timerConfig.setPersistent(false);

        ivTS.createSingleActionTimer(INITIAL_DURATION, timerConfig);

        svLogger.info("Leaving TimerRetryBeanC.doWork()...");
    }

    @Timeout
    public void doTimeoutStuff(Timer timer) {
        svLogger.info("Entering TimerRetryBeanC.doTimeoutStuff(), with pre-execution count of **" + count + "**");

        count++;
        timestamps.add(Long.valueOf(System.currentTimeMillis()));

        if (count < 3) {
            svLogger.info("Intentionally throwing error from TimerRetryBeanC.doTimeoutStuff(), because count **" + count + "** is less than 3.");
            throw new TransactionRolledbackLocalException("Intentional timer exception to force retry, with post-execution count of **" + count + "**");
        }

        svLogger.info("TimerRetryBeanC.doTimeoutStuff() is allowed to pass, because the current count **" + count + "** is not less than 3...");
        timerLatch.countDown();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void waitForTimersAndCancel(long cancelDelay) {
        svLogger.info("Waiting for timer to complete...");
        try {
            timerLatch.await(MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace(System.out);
        }

        // Some tests would like to add a delay before canceling
        // any timers to make sure retries don't occur, but
        // at least wait for a postInvoke delay.
        if (cancelDelay > 0) {
            FATHelper.sleep(cancelDelay);
        } else {
            FATHelper.sleep(FATHelper.POST_INVOKE_DELAY);
        }

        // This attempt to cancel the timer will be racing with the last
        // timeout retry completion; but try it anyway to guarantee that
        // the timer is gone before moving on. Alternative would be to
        // sleep for a 'postInvoke delay'.
        Collection<Timer> timers = ivTS.getTimers();
        for (Timer timer : timers) {
            try {
                timer.cancel();
            } catch (NoSuchObjectLocalException ex) {
                // Last timeout completed; timer already deleted
            }
        }
    }
}
