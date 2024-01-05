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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.DateTimeException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class SchedAsyncTestServlet extends FATServlet {
    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Inject
    private SchedAsyncAppScopedBean bean;

    private static CompletableFuture<Long> cfEveryFiveSeconds3Times;

    private static CompletableFuture<long[]> cfEveryThreeAndEvenSeconds8Times;

    /**
     * Nanoseconds at which the init method was invoked on this servlet.
     */
    private static long init_ns;

    @Override
    public void destroy() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        init_ns = System.nanoTime();

        cfEveryFiveSeconds3Times = bean.everyFiveSeconds(new AtomicInteger(3));

        cfEveryThreeAndEvenSeconds8Times = bean.everyThreeOrEvenSeconds(8, new AtomicInteger());
    }

    /**
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
