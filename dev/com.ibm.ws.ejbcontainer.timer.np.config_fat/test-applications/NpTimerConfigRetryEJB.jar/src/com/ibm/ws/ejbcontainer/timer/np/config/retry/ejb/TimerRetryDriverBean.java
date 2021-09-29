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

import static javax.ejb.ConcurrencyManagementType.BEAN;

import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Singleton session bean that acts as a test driver in the server process
 * for the TimerRetryTest client container test.
 **/
@Singleton
@Local(TimerRetryDriver.class)
@ConcurrencyManagement(BEAN)
public class TimerRetryDriverBean implements TimerRetryDriver {
    private static final String CLASS_NAME = TimerRetryDriverBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static String COUNT_KEY = "count";
    public static String SCHEDULED_START_TIME_KEY = "scheduledStartTime";
    public static String TIMESTAMP_KEY = "timestamps";
    public static String NEXTTIMEOUT_KEY = "nextTime";
    public static String TIMER_EXISTS = "timerExists";

    public static String BEAN_A = "TimerRetryBeanA";
    public static String BEAN_B = "TimerRetryBeanB";
    public static String BEAN_C = "TimerRetryBeanC";
    public static String BEAN_D = "TimerRetryBeanD";
    public static String BEAN_E = "TimerRetryBeanE";
    public static String BEAN_F = "TimerRetryBeanF";

    public static final long INITIAL_DURATION = 0; // immediate
    public static final long TIMER_INTERVAL = 4500;
    public static final long MAX_WAIT_TIME = 7 * 60 * 1000; // must be > 5 minutes for default retry

    @Resource
    SessionContext ivContext;

    @EJB(name = "ejb/TimerRetryBeanA", beanName = "TimerRetryBeanA")
    TimerRetryBeanA ivTimerRetryBeanA;

    @EJB(name = "ejb/TimerRetryBeanB", beanName = "TimerRetryBeanB")
    TimerRetryBeanB ivTimerRetryBeanB;

    @EJB(name = "ejb/TimerRetryBeanC", beanName = "TimerRetryBeanC")
    TimerRetryBeanC ivTimerRetryBeanC;

    @EJB(name = "ejb/TimerRetryBeanD", beanName = "TimerRetryBeanD")
    TimerRetryBeanD ivTimerRetryBeanD;

    @EJB(name = "ejb/TimerRetryBeanE", beanName = "TimerRetryBeanE")
    TimerRetryBeanE ivTimerRetryBeanE;

    @EJB(name = "ejb/TimerRetryBeanF", beanName = "TimerRetryBeanF")
    TimerRetryBeanF ivTimerRetryBeanF;

    String currentBean = null;

    @Override
    public void forceOneFailure(String testName) {
        svLogger.info("Entering TimerRetryDriverBean.forceOneFailure()...");

        currentBean = BEAN_A;
        TimerRetryBeanA.count = 0;
        TimerRetryBeanA.timestamps.clear();
        ivTimerRetryBeanA.doWork(testName);

        svLogger.info("Leaving TimerRetryDriverBean.forceOneFailure()...");
    }

    @Override
    public void forceEverythingToFail(String testName, int retries) {
        svLogger.info("Entering TimerRetryDriverBean.forceEverythingToFail()...");

        currentBean = BEAN_B;
        TimerRetryBeanB.count = 0;
        TimerRetryBeanB.timestamps.clear();
        ivTimerRetryBeanB.doWork(testName, retries);

        svLogger.info("Leaving TimerRetryDriverBean.forceEverythingToFail()...");
    }

    @Override
    public void forceTwoFailures(String testName) {
        svLogger.info("Entering TimerRetryDriverBean.forceTwoFailures()...");

        currentBean = BEAN_C;
        TimerRetryBeanC.count = 0;
        TimerRetryBeanC.timestamps.clear();
        ivTimerRetryBeanC.doWork(testName);

        svLogger.info("Leaving TimerRetryDriverBean.forceTwoFailures()...");
    }

    @Override
    public void forceRetrysAndRegularSchedulesToOverlap(String testName, int retries) {
        svLogger.info("Entering TimerRetryDriverBean.forceRetrysAndRegularSchedulesToOverlap()...");

        currentBean = BEAN_D;
        TimerRetryBeanD.count = 0;
        TimerRetryBeanD.scheduledStartTime = 0;
        TimerRetryBeanD.timestamps.clear();
        TimerRetryBeanD.nextTimes.clear();
        ivTimerRetryBeanD.doWork(testName, retries);

        svLogger.info("Leaving TimerRetryDriverBean.forceTwoFailures()...");
    }

    @Override
    public void forceEverythingToFailIntervalTimer(String testName, int retries) {
        svLogger.info("Entering TimerRetryDriverBean.forceEverythingToFailIntervalTimer()...");

        currentBean = BEAN_E;
        TimerRetryBeanE.count = 0;
        TimerRetryBeanE.timestamps.clear();
        ivTimerRetryBeanE.doWork(testName, retries);

        svLogger.info("Leaving TimerRetryDriverBean.forceEverythingToFailIntervalTimer()...");
    }

    @Override
    public void forceEverythingToFailCalendarTimer(String testName, int retries) {
        svLogger.info("Entering TimerRetryDriverBean.forceEverythingToFailCalendarTimer()...");

        currentBean = BEAN_F;
        TimerRetryBeanF.count = 0;
        TimerRetryBeanF.timestamps.clear();
        ivTimerRetryBeanF.doWork(testName, retries);

        svLogger.info("Leaving TimerRetryDriverBean.forceEverythingToFailCalendarTimer()...");
    }

    @Override
    public Properties getResults() {
        Properties props = new Properties();
        if (currentBean == BEAN_A) {
            svLogger.info("Getting results for BeanA...");
            props.put(COUNT_KEY, Integer.valueOf(TimerRetryBeanA.count));
            props.put(TIMESTAMP_KEY, TimerRetryBeanA.timestamps);
        } else if (currentBean == BEAN_B) {
            svLogger.info("Getting results for BeanB...");
            props.put(COUNT_KEY, Integer.valueOf(TimerRetryBeanB.count));
            props.put(TIMESTAMP_KEY, TimerRetryBeanB.timestamps);
            props.put(TIMER_EXISTS, Boolean.valueOf(TimerRetryBeanB.timerExists));
        } else if (currentBean == BEAN_C) {
            svLogger.info("Getting results for BeanC...");
            props.put(COUNT_KEY, Integer.valueOf(TimerRetryBeanC.count));
            props.put(TIMESTAMP_KEY, TimerRetryBeanC.timestamps);
        } else if (currentBean == BEAN_D) {
            svLogger.info("Getting results for BeanD...");
            props.put(COUNT_KEY, Integer.valueOf(TimerRetryBeanD.count));
            props.put(SCHEDULED_START_TIME_KEY, Long.valueOf(TimerRetryBeanD.scheduledStartTime));
            props.put(TIMESTAMP_KEY, TimerRetryBeanD.timestamps);
            props.put(NEXTTIMEOUT_KEY, TimerRetryBeanD.nextTimes);
        } else if (currentBean == BEAN_E) {
            svLogger.info("Getting results for BeanE...");
            props.put(COUNT_KEY, Integer.valueOf(TimerRetryBeanE.count));
            props.put(TIMESTAMP_KEY, TimerRetryBeanE.timestamps);
            props.put(TIMER_EXISTS, Boolean.valueOf(TimerRetryBeanE.timerExists));
        } else if (currentBean == BEAN_F) {
            svLogger.info("Getting results for BeanF...");
            props.put(COUNT_KEY, Integer.valueOf(TimerRetryBeanF.count));
            props.put(TIMESTAMP_KEY, TimerRetryBeanF.timestamps);
            props.put(TIMER_EXISTS, Boolean.valueOf(TimerRetryBeanF.timerExists));
        } else {
            svLogger.info("currentBean not set.... no results");
        }
        return props;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void waitForTimersAndCancel(long cancelDelay) {
        if (currentBean == BEAN_A) {
            ivTimerRetryBeanA.waitForTimersAndCancel(cancelDelay);
        } else if (currentBean == BEAN_B) {
            ivTimerRetryBeanB.waitForTimersAndCancel(cancelDelay);
        } else if (currentBean == BEAN_C) {
            ivTimerRetryBeanC.waitForTimersAndCancel(cancelDelay);
        } else if (currentBean == BEAN_D) {
            ivTimerRetryBeanD.waitForTimersAndCancel(cancelDelay);
        } else if (currentBean == BEAN_E) {
            ivTimerRetryBeanE.waitForTimersAndCancel(cancelDelay);
        } else if (currentBean == BEAN_F) {
            ivTimerRetryBeanF.waitForTimersAndCancel(cancelDelay);
        }
    }
}
