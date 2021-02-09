/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Timer;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;
import ejb.AutoTimer;
import ejb.MyTimer;
import ejb.MyTimerTracker;

@WebServlet("/*")
public class PersistentTimersTestServlet extends FATServlet {
    private static final long serialVersionUID = 8447513765214641067L;

    /**
     * Interval in milliseconds between polling for task results.
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(30);

    @EJB
    private AutoTimer autoTimer;

    @EJB
    private MyTimer ejb;

    @EJB
    private MyTimerTracker tracker;

    @Resource
    private UserTransaction tran;

    /**
     * Schedule 3 persistent timers: one which only runs once, another which cancels itself while running upon the
     * third execution, and another that keeps running until we cancel it.
     */
    @Test
    public void testCreatePersistentTimers() throws Exception {
        String multiTimerName = "testCreatePersistentTimers-multiTimer";
        Timer multiTimer = ejb.scheduleMultipleExecutionTimer(multiTimerName);

        String tripleTimerName = "testCreatePersistentTimers-tripleTimer";
        Timer tripleTimer = ejb.scheduleTripleExecutionTimer(tripleTimerName);

        String oneTimerName = "testCreatePersistentTimers-oneTimer";
        Timer oneTimer;
        tran.begin();
        try {
            oneTimer = ejb.scheduleOneExecutionTimer(oneTimerName);
        } finally {
            tran.commit();
        }

        int count = tracker.getRunCount(oneTimerName);
        for (long start = System.nanoTime(); count < 1 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            count = tracker.getRunCount(oneTimerName);
        if (count != 1)
            throw new Exception("Unexpected number of executions " + count + " for single execution timer " + oneTimer.getHandle());

        count = tracker.getRunCount(tripleTimerName);
        for (long start = System.nanoTime(); count < 3 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            count = tracker.getRunCount(tripleTimerName);
        if (count != 3)
            throw new Exception("Unexpected number of executions " + count + " for triple execution timer " + tripleTimer.getHandle());

        count = tracker.getRunCount(multiTimerName);
        for (long start = System.nanoTime(); count < 3 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            count = tracker.getRunCount(multiTimerName);
        if (count < 3)
            throw new Exception("Unexpected number of executions " + count + " for multiple execution timer " + multiTimer.getHandle());

        Date nextTime = multiTimer.getNextTimeout();
        if (nextTime.after(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30))))
            throw new Exception("Next timeout is way too far in the future: " + nextTime);

        // Roll back cancellation of a timer.
        tran.begin();
        try {
            multiTimer.cancel();
        } finally {
            tran.rollback();
        }

        // Make sure it keeps running.
        int previousCount = count = tracker.getRunCount(multiTimerName);
        for (long start = System.nanoTime(); count == previousCount && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            count = tracker.getRunCount(multiTimerName);
        if (count == previousCount)
            throw new Exception("Expecting to still see executions beyond " + previousCount + " for multiple execution timer " + multiTimer.getHandle());

        multiTimer.cancel();
    }

    /**
     * Verify that an automatic persistent timer is running multiple times
     */
    @Test
    public void testRepeatingAutomaticPersistentTimer() throws Exception {
        int count = autoTimer.getRunCount();
        for (long start = System.nanoTime(); count < 3 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            count = autoTimer.getRunCount();

        if (count < 3)
            throw new Exception("Expecting EJB timer to run at least 3 times. Instead count was: " + count);

        autoTimer.cancel();
    }
}
