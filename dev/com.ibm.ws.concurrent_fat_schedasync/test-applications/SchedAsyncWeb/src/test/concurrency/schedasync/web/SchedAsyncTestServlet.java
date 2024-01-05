/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrency.schedasync.web;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.ALL_REMAINING;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.APPLICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.DateTimeException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@ContextServiceDefinition(name = "java:app/concurrent/app-context",
                          propagated = APPLICATION,
                          unchanged = ALL_REMAINING)
@ManagedExecutorDefinition(name = "java:module/concurrent/max-2-executor",
                           context = "java:app/concurrent/app-context",
                           maxAsync = 2)
@SuppressWarnings("serial")
@WebServlet("/*")
public class SchedAsyncTestServlet extends FATServlet {
    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    private static LinkedBlockingQueue<long[]> afterSixSeconds3Times = new LinkedBlockingQueue<>();
    private static final AtomicInteger afterSixSeconds3TimesCount = new AtomicInteger();

    private static LinkedBlockingQueue<Object> lookUpAtSixSecondIntervals2Times = new LinkedBlockingQueue<>();

    @Inject
    private SchedAsyncAppScopedBean bean;

    private static CompletableFuture<Long> cfEveryFiveSeconds3Times;
    private static AtomicInteger cfEveryFiveSeconds3TimesCountdown;

    private static CompletableFuture<long[]> cfEveryThreeAndEvenSeconds8Times;
    private static final AtomicInteger cfEveryThreeAndEvenSeconds8TimesCount = new AtomicInteger();

    /**
     * Nanoseconds at which the init method was invoked on this servlet.
     */
    private static long init_ns;

    @Override
    public void destroy() {
    }

    /**
     * To make the tests run faster, schedule the asynchronous methods upon init
     * and have individual tests make assertions about them.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        init_ns = System.nanoTime();

        cfEveryFiveSeconds3Times = bean.everyFiveSeconds(cfEveryFiveSeconds3TimesCountdown = new AtomicInteger(3));

        bean.lookUpAtSixSecondIntervals("java:module/concurrent/max-2-executor",
                                        lookUpAtSixSecondIntervals2Times,
                                        new AtomicInteger(2));

        bean.everySixSeconds(3, afterSixSeconds3TimesCount).thenAccept(l -> afterSixSeconds3Times.add(l));

        cfEveryThreeAndEvenSeconds8Times = bean.everyThreeOrEvenSeconds(8, cfEveryThreeAndEvenSeconds8TimesCount);

        // Seconds at which the above will aim to run:
        //
        // 00        05        10        15        20        25        30        35        40        45        50        55
        //           05          11          17          23          29          35          41          47          53          59
        //   01          07          13          19          25          31          37          43          49          55
        // 00  02..04  06  08..10  12  14..16  18  20..22  24  26..28  30  32..34  36  38..40  42  44..46  48  50..52  54  56..58
    }

    /**
     * Attempt to schedule an asynchronous method specifying the seconds empty.
     */
    @Test
    public void testEmptySeconds() throws Exception {
        try {
            bean.withEmptySeconds();
            fail("Should not be able to schedule an asynchronous method with empty list of seconds.");
        } catch (IllegalArgumentException x) {
            // expected
        }
    }

    /**
     * An asynchronous method that is scheduled to run every 5 seconds for 3 executions
     * and then complete must run exactly 3 times.
     */
    @Test
    public void testEveryFiveSeconds3Times() throws Exception {
        Long timeOfFinalExecution = cfEveryFiveSeconds3Times.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        long elapsed = timeOfFinalExecution - init_ns;
        if (elapsed < TimeUnit.SECONDS.toNanos(10L))
            fail("A task that runs every 5 seconds must not complete 3 executions in under 10 seconds. Elapsed nanoseconds: " + elapsed);
    }

    /**
     * An asynchronous method that combines multiple schedules, intermixing cron and basic schedules
     * to run every 6 seconds for 3 executions and then complete must run exactly 3 times.
     */
    @Test
    public void testEverySixSeconds3Times() throws Exception {
        long[] result = afterSixSeconds3Times.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertNotNull(result);

        long numExecutions = result[0];
        long timeOfFinalExecution = result[1];

        assertEquals(3L, numExecutions);

        long elapsed = timeOfFinalExecution - init_ns;
        if (elapsed < TimeUnit.SECONDS.toNanos(12L))
            fail("A task that runs at 6 second intervals must not complete 3 executions in under 12 seconds." +
                 " Elapsed nanoseconds: " + elapsed);
    }

    /**
     * An asynchronous method is scheduled to run on multiple schedules that combine every even second
     * with every second that is divisible by 3, such that the task runs on seconds
     * 00 xx 02 03 04 xx 06 xx 08 09 10 xx 12 xx 14 15 16 xx 18 xx 20 21 22 xx 24 xx 26 27 28 xx
     * 30 xx 32 33 34 xx 36 xx 38 39 40 xx 42 xx 44 45 46 xx 48 xx 50 51 52 xx 54 xx 56 57 58 xx
     * for a total of 8 executions and then stops running.
     * Verify that the 8 executions complete no sooner than 10 seconds from when scheduled.
     */
    @Test
    public void testEveryThreeAndEvenNumberedSeconds8Times() throws Exception {
        long[] result = cfEveryThreeAndEvenSeconds8Times.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        long numExecutions = result[0];
        long timeOfFinalExecution = result[1];

        assertEquals(8L, numExecutions);

        long elapsed = timeOfFinalExecution - init_ns;
        if (elapsed < TimeUnit.SECONDS.toNanos(10L))
            fail("A task that runs on seconds divisble by 2 or 3 must not complete 8 executions in under 10 seconds." +
                 " Elapsed nanoseconds: " + elapsed);
    }

    /**
     * An asynchronous method that is scheduled to perform a lookup in the application component namespace
     * every six seconds must successfully peform the lookup on multiple executions without raising errors.
     */
    @Test
    public void testLookUpEverySixSeconds2Times() throws Exception {
        Object result1 = lookUpAtSixSecondIntervals2Times.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(result1);
        if (result1 instanceof Exception)
            throw new AssertionError(result1);
        else if (result1 instanceof Error)
            throw new AssertionError(result1);

        Object result2 = lookUpAtSixSecondIntervals2Times.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(result2);
        if (result2 instanceof Exception)
            throw new AssertionError(result2);
        else if (result2 instanceof Error)
            throw new AssertionError(result2);
    }

    /**
     * Attempt to schedule an asynchronous method for a time that does not exist (February 30).
     */
    @Test
    public void testScheduleForNonExistingTime() {
        try {
            CompletionStage<String> cs = bean.runOnFebruary30();
            fail("Should not be able to schedule an asynchronous method for a non-existing time: " + cs);
        } catch (DateTimeException x) {
            // expected
        }
    }
}
