/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.concurrency32cdi.web;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Bean with methods that are automatically scheduled to run per their
 * Schedule annotations.
 */
@ApplicationScoped
public class SchedulingBean {
    /**
     * Tracks the executions of the every3SecondsCron method.
     */
    private final LinkedBlockingQueue<Thread> every3SecondsCronThreads = //
                    new LinkedBlockingQueue<>();

    /**
     * Tracks the executions of the every5Seconds3Times method.
     */
    private final AtomicInteger every5Seconds3TimesCount = //
                    new AtomicInteger(0);

    /**
     * Tracks the non-execution of the notScheduled method.
     */
    private final CountDownLatch notScheduledLatch = //
                    new CountDownLatch(1);

    /**
     * Tracks the execution of the onceOn4thSecond method.
     */
    private final AtomicInteger onceOn4thSecondCount = //
                    new AtomicInteger(0);

    // Seconds at which methods aim to run:
    //     02    05    08    11    14    17    20    23    26    29    32    35    38    41    44    47    50    53    56    59
    //       03        08        13        18        23       28         33        38        43        48        53        58
    //
    // 00      04      08      12      16      20      24     28       32      36      40      44      48      52      56

    @Schedule(cron = "2/3 * * * * *",
              zone = "America/Chicago")
    public CompletableFuture<Void> every3SecondsCron() {
        System.out.println("every3SecondsCron");
        every3SecondsCronThreads.add(Thread.currentThread());
        return null; // continue running
    }

    @Schedule(seconds = { 3, 8, 13, 18, 23, 28, 33, 38, 43, 48, 53, 58 },
              minutes = {}, // not restricted
              hours = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
                        12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 },
              zone = "America/Denver")
    public void every5Seconds3Times() {
        int count = every5Seconds3TimesCount.incrementAndGet();
        System.out.println("every5Seconds3Times #" + count);
        if (count == 3)
            throw new RuntimeException("Please stop running this method");
    }

    public void notScheduled() {
        System.out.println("Running a method that is NOT SCHEDULED!");
        notScheduledLatch.countDown();
    }

    @Schedule(cron = "0/4 * * * * SUN-SAT",
              zone = "America/New_York")
    public CompletionStage<Boolean> onceOn4thSecond() {
        int count = onceOn4thSecondCount.incrementAndGet();
        System.out.println("onceOn4thSecond #" + count);
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Returns a queue tracking the executions of the every3SecondsCron method.
     *
     * @return a queue tracking the executions of the every3SecondsCron method
     */
    public LinkedBlockingQueue<Thread> trackerOfEvery3SecondsCron() {
        return every3SecondsCronThreads;
    }

    /**
     * Returns a count tracking the executions of the every5Seconds3Times method.
     *
     * @return a count tracking the executions of the every5Seconds3Times method
     */
    public AtomicInteger trackerOfEvery5Seconds3Times() {
        return every5Seconds3TimesCount;
    }

    /**
     * Returns a latch tracking the non-execution of the notScheduled method.
     *
     * @return a latch tracking the non-execution of the notScheduled method
     */
    public CountDownLatch trackerOfNotScheduled() {
        return notScheduledLatch;
    }

    /**
     * Returns a count tracking the execution of the onceOn4thSecond method.
     *
     * @return a count tracking the execution of the onceOn4thSecond method
     */
    public AtomicInteger trackerOfOnceOn4thSecond() {
        return onceOn4thSecondCount;
    }

}