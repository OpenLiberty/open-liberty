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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionRolledbackLocalException;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

@Stateless(name = "TimerRetryBeanF")
@LocalBean
public class TimerRetryBeanF {
    private static final String CLASS_NAME = TimerRetryBeanF.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static volatile int count = 0;
    public static ArrayList<Long> timestamps = new ArrayList<Long>();
    private static CountDownLatch timerLatch;
    public static String timerInfo;
    public static boolean timerExists;

    @Resource
    private TimerService ivTS;

    public void doWork(String testName, int numOfTimeouts) {
        svLogger.info("Entering TimerRetryBeanf.doWork() for test **" + testName + "**");

        timerExists = false;
        timerInfo = testName;
        timerLatch = new CountDownLatch(numOfTimeouts);
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo("Timeout for test: **" + timerInfo + "**");
        timerConfig.setPersistent(false);

        ScheduleExpression se = new ScheduleExpression();
        se.second("*/5").minute("*").hour("*");

        ivTS.createCalendarTimer(se, timerConfig);

        svLogger.info("Leaving TimerRetryBeanf.doWork()...");
    }

    @Timeout
    public void doTimeoutStuff(Timer timer) {
        svLogger.info("Entering TimerRetryBeanF.doTimeoutStuff(), with pre-execution count of **" + count + "**");

        count++;
        timestamps.add(Long.valueOf(System.currentTimeMillis()));

        svLogger.info("Intentionally throwing error from TimerRetryBeanF.doTimeoutStuff()...currenty retry count is **" + count + "**");
        timerLatch.countDown();
        throw new TransactionRolledbackLocalException("Intentional timer exception to force retry, with post-execution count of **" + count + "**");
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void waitForTimersAndCancel(long cancelDelay) {
        svLogger.info("Waiting for timer(s) to complete...");
        try {
            timerLatch.await(4, TimeUnit.MINUTES);
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

        // This timer is interval; so should always exist.
        Collection<Timer> timers = ivTS.getTimers();
        for (Timer timer : timers) {
            svLogger.info("timer:" + timer.toString());
            svLogger.info("timer info:" + timer.getInfo().toString());
            svLogger.info("timerInfo: " + timerInfo);
            timerExists |= ((String) timer.getInfo()).contains(timerInfo);
            timer.cancel();
        }
    }
}
