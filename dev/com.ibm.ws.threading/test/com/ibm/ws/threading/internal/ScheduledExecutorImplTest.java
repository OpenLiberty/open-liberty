/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.threading.internal.SchedulingHelper.ExpeditedFutureTask;
import test.common.SharedOutputManager;

public class ScheduledExecutorImplTest {
    static final DateFormat tsFormatZone = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSSSS (zz)");

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=info:Threading=all");
    @Rule
    public TestRule managerRule = outputMgr;

    private static final Class<?> c = ScheduledExecutorImplTest.class;
    private static final String cName = c.getCanonicalName();
    private static Logger logger = Logger.getLogger(cName);

    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    ScheduledExecutorImpl m_scheduledExecutor;

    {
        m_scheduledExecutor = new ScheduledExecutorImpl();
        ExecutorServiceImpl executorService = new ExecutorServiceImpl();
        Map<String, Object> componentConfig = new HashMap<String, Object>(6);
        componentConfig.put("name", "testExecutor");
        componentConfig.put("rejectedWorkPolicy", "CALLER_RUNS");
        componentConfig.put("stealPolicy", "STRICT");
        componentConfig.put("keepAlive", 60);
        componentConfig.put("coreThreads", -1);
        componentConfig.put("maxThreads", -1);
        executorService.activate(componentConfig);

        m_scheduledExecutor.setExecutor(executorService);
    }

    /**
     * Test ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
     * 
     * Submit a Runnable and make sure it executes after the specified delay.
     * 
     * @throws Exception
     */
    @Test(timeout = 60000)
    public void testScheduleRunnable() throws Exception {
        logger.logp(Level.INFO, cName, "testScheduleRunnable", "Entry.");

        SimpleRunnable target = new SimpleRunnable();
        long startTime = new Date().getTime();

        ScheduledFuture<?> schedFuture = m_scheduledExecutor.schedule(target, 3, TimeUnit.SECONDS);
        schedFuture.get();

        long duration = target.getScheduledTime() - startTime;

        Assert.assertTrue("Scheduled Runnable did not honor delay", (duration >= TimeUnit.MILLISECONDS.convert(3, TimeUnit.SECONDS)));

        logger.logp(Level.INFO, cName, "testScheduleRunnable", "Exit.");
    }

    /**
     * Test <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
     * 
     * Submit a Callable and make sure it executes after the specified delay.
     * 
     * @throws Exception
     */
    @Test(timeout = 60000)
    public void testScheduleCallable() throws Exception {
        logger.logp(Level.INFO, cName, "testScheduleCallable", "Entry.");

        SimpleCallable<?> target = new SimpleCallable<Object>();
        long startTime = new Date().getTime();

        ScheduledFuture<?> schedFuture = m_scheduledExecutor.schedule(target, 3, TimeUnit.SECONDS);
        schedFuture.get();

        long duration = target.getScheduledTime() - startTime;

        Assert.assertTrue("Scheduled Callable did not honor delay", (duration >= TimeUnit.MILLISECONDS.convert(3, TimeUnit.SECONDS)));

        logger.logp(Level.INFO, cName, "testScheduleCallable", "Exit.");
    }

    /**
     * Test ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
     * 
     * Submit a Runnable and make sure it executes after the initial delay. Also, make sure it executes
     * at least a reasonable amount of times given its fixed delay value.
     * 
     * scheduledWithFixedDelay(fixedDelayRunnable, 3 , 2 , TimeUnit.SECONDS): should execute at least three times given
     * an overall time of 20 seconds...With test system performance variance we really can't be precise. Otherwise,
     * we risk failing the test on slow machines.
     * 
     * @throws Exception
     */
    @Test(timeout = 60000)
    public void testScheduleWithFixedDelay() throws Exception {
        logger.logp(Level.INFO, cName, "testScheduleWithFixedDelay", "Entry.");

        long initialDelay = 3;
        long delay = 2;
        TimeUnit unit = TimeUnit.SECONDS;
        long overWaitTime = 20;
        long acceptableExecutions = 3;

        class FixedDelayRunnable implements Runnable {
            long numberOfDispatches;

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                numberOfDispatches++;
            }

            long getNumberOfDispatches() {
                return numberOfDispatches;
            }

        };

        FixedDelayRunnable target = new FixedDelayRunnable();

        long startTime = new Date().getTime();
        ScheduledFuture<?> schedFuture = m_scheduledExecutor.scheduleWithFixedDelay(target, initialDelay, delay, unit);

        for (int i = 0; i < overWaitTime; i++) {
            Thread.sleep(500);

            logger.logp(Level.INFO, cName, "scheduleWithFixedDelay",
                        " loop: " + tsFormatZone.format(new Date()) + ", getDelay():" + schedFuture.getDelay(TimeUnit.MICROSECONDS) + "us");

            if (target.getNumberOfDispatches() >= acceptableExecutions) {
                break;
            }
        }

        boolean cancelResult = schedFuture.cancel(true);

        if (cancelResult == true) {
            Assert.assertTrue("ScheduleWithFixedDelay returned Future\'s isCancelled() should return true if the cancel() result returned true", schedFuture.isCancelled());
        }
        Assert.assertTrue("ScheduleWithFixedDelay returned Future\'s isDone() should always return true after a cancel()", schedFuture.isDone());

        long numOfDispatches = target.getNumberOfDispatches();

        Assert.assertTrue("ScheduleWithFixedDelay Runnable did not did not execute enough (" + numOfDispatches +
                          ") in time given (" + overWaitTime + ")", (numOfDispatches >= acceptableExecutions));

        long finishTime = new Date().getTime();
        long testDuration = finishTime - startTime;
        Assert.assertTrue("ScheduleWithFixedDelay Runnable ran too many times given overall time scheduled. testDuration (" + testDuration +
                          "ms) initialDelay (" + initialDelay + "s) numOfDispatches (" + numOfDispatches + ") delay (" + delay + "s)",
                          (testDuration >= TimeUnit.MILLISECONDS.convert((initialDelay + (numOfDispatches - 1 * delay)), TimeUnit.SECONDS)));

        logger.logp(Level.INFO, cName, "testScheduleWithFixedDelay", "Exit.");
    }

    /**
     * Test ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
     * 
     * Submit a Runnable and make sure it executes after the initial delay. Also, make sure it executes
     * at least a reasonable amount of times given its fixed rate value.
     * 
     * scheduleAtFixedRate(fixedRateRunnable, 3 , 2 , TimeUnit.SECONDS): should execute at least three times given
     * an overall time of 20 seconds...With test system performance variance we really can't be precise. Otherwise,
     * we risk failing the test on slow machines.
     * 
     * @throws Exception
     */
    @Test(timeout = 60000)
    public void testScheduleAtFixedRate() throws Exception {
        logger.logp(Level.INFO, cName, "testScheduleAtFixedRate", "Entry.");

        long initialDelay = 3;
        long period = 2;
        TimeUnit unit = TimeUnit.SECONDS;
        long overWaitTime = 20;
        long acceptableExecutions = 3;

        class FixedRateRunnable implements Runnable {
            long initialScheduledTime;
            long numberOfDispatches;

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                if (initialScheduledTime == 0) {
                    initialScheduledTime = new Date().getTime();
                }
                numberOfDispatches++;

                try {
                    // try to throw off the timing for Fixed Rate...It will try to keep it on the interval.
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }

                // This trace shows the dispatch staying on the period value and not drifting off from the 1/2
                // second sleep() above.
                logger.logp(Level.INFO, cName, "FixedRateRunnable", " run(): " + tsFormatZone.format(new Date()));
            }

            long getNumberOfDispatches() {
                return numberOfDispatches;
            }

        };

        FixedRateRunnable target = new FixedRateRunnable();

        long startTime = new Date().getTime();
        ScheduledFuture<?> schedFuture = m_scheduledExecutor.scheduleAtFixedRate(target, initialDelay, period, unit);

        for (int i = 0; i < overWaitTime; i++) {
            Thread.sleep(500);

            logger.logp(Level.INFO, cName, "testScheduleAtFixedRate",
                        " loop: " + tsFormatZone.format(new Date()) + ", getDelay():" + schedFuture.getDelay(TimeUnit.MICROSECONDS) + "us");

            if (target.getNumberOfDispatches() >= acceptableExecutions) {
                break;
            }
        }

        boolean cancelResult = schedFuture.cancel(true);

        if (cancelResult == true) {
            Assert.assertTrue("ScheduleAtFixedRate returned Future\'s isCancelled() should return true if the cancel() result returned true", schedFuture.isCancelled());
        }
        Assert.assertTrue("ScheduleAtFixedRate returned Future\'s isDone() should always return true after a cancel()", schedFuture.isDone());

        long numOfDispatches = target.getNumberOfDispatches();

        Assert.assertTrue("ScheduleAtFixedRate Runnable did not did not execute enough (" + numOfDispatches +
                          ") in time given (" + overWaitTime + ")", (numOfDispatches >= acceptableExecutions));

        long finishTime = new Date().getTime();
        long testDuration = finishTime - startTime;
        Assert.assertTrue("ScheduleAtFixedRate Runnable ran too many times given overall time scheduled. testDuration (" + testDuration +
                          "ms) initialDelay (" + initialDelay + "s) numOfDispatches (" + numOfDispatches + ") period (" + period + "s)",
                          (testDuration >= TimeUnit.MILLISECONDS.convert((initialDelay + (numOfDispatches - 1 * period)), TimeUnit.SECONDS)));

        logger.logp(Level.INFO, cName, "testScheduleAtFixedRate", "Exit.");
    }

    /**
     * Ensures that scheduled tasks (both runnable and callable) are expedited
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testScheduledTasksAreExpedited() throws Exception {
        logger.logp(Level.INFO, cName, "testScheduledTasksAreExpedited", "Entry.");

        SimpleRunnable runnable = new SimpleRunnable();
        ScheduledFuture<?> schedFutureFromRunnable = m_scheduledExecutor.schedule(runnable, 1, TimeUnit.SECONDS);
        schedFutureFromRunnable.get();
        ExpeditedFutureTask taskFuturefromRunnable = (ExpeditedFutureTask) ((SchedulingHelper) schedFutureFromRunnable).m_defaultFuture;
        Assert.assertTrue("Scheduled runnable should be expedited", taskFuturefromRunnable.isExpedited());

        SimpleCallable<?> callable = new SimpleCallable<Object>();
        ScheduledFuture<?> schedFutureFromCallable = m_scheduledExecutor.schedule(callable, 1, TimeUnit.SECONDS);
        schedFutureFromCallable.get();
        ExpeditedFutureTask taskFuturefromCallable = (ExpeditedFutureTask) ((SchedulingHelper) schedFutureFromCallable).m_defaultFuture;
        Assert.assertTrue("Scheduled callable should be expedited", taskFuturefromCallable.isExpedited());

        logger.logp(Level.INFO, cName, "testScheduledTasksAreExpedited", "Exit.");
    }

    class SimpleRunnable implements Runnable {
        long scheduledTime;

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            // Record scheduled time
            scheduledTime = new Date().getTime();

        }

        long getScheduledTime() {
            return scheduledTime;
        }

    };

    class SimpleCallable<V> implements Callable<V> {
        long scheduledTime;

        long getScheduledTime() {
            return scheduledTime;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.concurrent.Callable#call()
         */
        @Override
        public V call() throws Exception {
            // Record scheduled time
            scheduledTime = new Date().getTime();

            return null;
        }

    };

}
