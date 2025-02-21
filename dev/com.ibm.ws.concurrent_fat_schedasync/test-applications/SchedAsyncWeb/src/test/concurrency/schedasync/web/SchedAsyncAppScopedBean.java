/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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

import static org.junit.Assert.fail;

import java.time.Month;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

import javax.naming.InitialContext;
import javax.naming.NamingException;

@ApplicationScoped
public class SchedAsyncAppScopedBean {
    /**
     * A countdown for executions of autoSchedule.
     */
    private final CountDownLatch autoScheduleCountdown = new CountDownLatch(3);

    /**
     * A future for the future that represents completion of all executions
     * of the autoSchedule method.
     */
    private final CompletableFuture<CompletableFuture<?>> autoScheduleFutureFuture = //
                    new CompletableFuture<>();

    /**
     * A countdown for executions of delayedSchedule.
     */
    private final CountDownLatch delayedScheduleCountdown = new CountDownLatch(3);

    /**
     * A future for the future that represents completion of all executions
     * of the delayedSchedule method.
     */
    private final CompletableFuture<CompletableFuture<?>> delayedScheduleFutureFuture = //
                    new CompletableFuture<>();

    @Inject
    ManagedScheduledExecutorService executor;

    /**
     * Runs at startup to invoke a scheduled asynchronous method after a delay.
     *
     * @param event startup event.
     */
    public void afterDelay(@Observes Startup event) {
        executor.schedule(this::delayedSchedule, 2, TimeUnit.SECONDS);
    }

    /**
     * Starts automatically and runs every 8 seconds until autoScheduleCountdown
     * reaches 0.
     *
     * CDI invokes this method on startup. Do not invoke it elsewhere.
     *
     * @param event startup event.
     */
    @Asynchronous(executor = "java:module/concurrent/max-2-executor",
                  runAt = @Schedule(cron = "0/8 * * * * *"))
    public void autoSchedule(@Observes Startup event) {
        autoScheduleFutureFuture.complete(Asynchronous.Result.getFuture());

        try {
            InitialContext.doLookup("java:module/concurrent/max-2-executor");
        } catch (NamingException x) {
            throw new CompletionException(x);
        }

        autoScheduleCountdown.countDown();

        if (autoScheduleCountdown.getCount() == 0L)
            Asynchronous.Result.complete(null);
    }

    /**
     * Run every 10 seconds on seconds that have a remainder of 3 when divided by 10
     * until delayedScheduleCountdown reaches 0.
     *
     * The afterDelay method schedules a task to invoke this method.
     * Do not invoke it elsewhere.
     */
    @Asynchronous(executor = "java:module/concurrent/max-2-executor",
                  runAt = @Schedule(cron = "3/10 * * * * *"))
    public void delayedSchedule() {
        delayedScheduleFutureFuture.complete(Asynchronous.Result.getFuture());

        try {
            InitialContext.doLookup("java:module/concurrent/max-2-executor");
        } catch (NamingException x) {
            throw new CompletionException(x);
        }

        delayedScheduleCountdown.countDown();

        if (delayedScheduleCountdown.getCount() == 0L)
            Asynchronous.Result.complete(null);
    }

    /**
     * Bean method to determine if all executions of the autoSchedule method
     * completed successfully.
     *
     * @param timeout maximum amount of time to wait
     * @param unit    time unit
     * @throws ExecutionException   if an execution of the autoSchedule method fails
     * @throws InterruptedException if interrupted
     * @throws TimeoutException     if it times out while waiting
     */
    public void awaitAutoScheduleCompletion(long timeout, TimeUnit unit) //
                    throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<?> future = autoScheduleFutureFuture.get(timeout, unit);
        if (future == null)
            fail("The first execution of the autoSchedule method did not occur.");
        future.get(timeout, unit);
    }

    /**
     * Bean method to determine if all executions of the delayedSchedule method
     * completed successfully.
     *
     * @param timeout maximum amount of time to wait
     * @param unit    time unit
     * @throws ExecutionException   if an execution of the delayedSchedule method fails
     * @throws InterruptedException if interrupted
     * @throws TimeoutException     if it times out while waiting
     */
    public void awaitDelayedScheduleCompletion(long timeout, TimeUnit unit) //
                    throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<?> future = delayedScheduleFutureFuture.get(timeout, unit);
        if (future == null)
            fail("The first execution of the delayedSchedule method did not occur.");
        future.get(timeout, unit);
    }

    /**
     * Runs every 5 seconds.
     *
     * @param countdown number of executions to stop after.
     * @return null to continue with more executions.
     *         To stop, returns a completed future with the current time in nanoseconds.
     */
    @Asynchronous(runAt = @Schedule(hours = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
                                              12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 },
                                    minutes = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
                                                12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
                                                24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35,
                                                36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
                                                48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59 },
                                    seconds = { 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55 }))
    CompletableFuture<Long> everyFiveSeconds(AtomicInteger countdown) {
        System.out.println("> everyFiveSeconds " + countdown);

        CompletableFuture<Long> result;
        if (countdown.decrementAndGet() == 0)
            result = Asynchronous.Result.complete(System.nanoTime());
        else
            result = null;

        System.out.println("< everyFiveSeconds " + result);
        return result;
    }

    /**
     * Run every 4 seconds on seconds that have a remainder of 2 when divided by 4.
     *
     * @param countdown executions remaining.
     * @param threads   a queue for recording the threads where executions have occurred.
     */
    @Asynchronous(executor = "java:module/concurrent/max-2-executor",
                  runAt = @Schedule(cron = "2/4 * * * * *"))
    void everyFourSecondsVirtual(AtomicInteger countdown, LinkedBlockingQueue<Thread> threads) {
        System.out.println("> everyFourSecondsVirtual " + countdown);

        threads.add(Thread.currentThread());

        if (countdown.decrementAndGet() == 0)
            Asynchronous.Result.complete(null);

        System.out.println("< everyFourSecondsVirtual executed on " + Thread.currentThread());
    }

    /**
     * Run every 7 seconds on seconds that have a remainder of 4 when divided by 7:
     * 4 11 18 25 32 39 46 53
     * Complete the future exceptionally upon the countdown reaching 0.
     *
     * @param countdown executions remaining
     */
    @Asynchronous(executor = "java:module/concurrent/max-2-executor",
                  runAt = @Schedule(cron = "4/7 * * * * *"))
    CompletableFuture<String> everySevenSecondsUntilExceptionalCompletion(AtomicInteger countdown) {
        System.out.println("> everySevenSecondsUntilExceptionalCompletion " + countdown);

        if (countdown.decrementAndGet() == 0) {
            Exception x = new DataFormatException("Cannot find anything else to do.");
            Asynchronous.Result.getFuture().completeExceptionally(x);
        }

        System.out.println("< everySevenSecondsUntilExceptionalCompletion executed on " +
                           Thread.currentThread());
        return null;
    }

    /**
     * Run every 7 seconds on seconds that have a remainder of 6 when divided by 7:
     * 6 13 27 34 41 48 55
     * Raise an exception upon the countdown reaching 0.
     *
     * @param countdown executions remaining
     */
    @Asynchronous(executor = "java:module/concurrent/max-2-executor",
                  runAt = @Schedule(cron = "6/7 * * * * *"))
    CompletableFuture<String> everySevenSecondsUntilThrowsError(AtomicInteger countdown) {
        System.out.println("> everySevenSecondsUntilThrowsError " + countdown);

        if (countdown.decrementAndGet() == 0) {
            Error e = new Error("Countdown reached 0, so this won't run again.");
            System.out.println("< everySevenSecondsUntilThrowsError intentionally" +
                               " raised error on " + Thread.currentThread() + ": " +
                               e);
            throw e;
        } else {
            System.out.println("< everySevenSecondsUntilThrowsError executed on " +
                               Thread.currentThread());
            return null;
        }
    }

    /**
     * Run every 7 seconds on seconds that are divisible by 7:
     * 0 7 14 28 35 42 49 56
     * Raise an exception upon the countdown reaching 0.
     *
     * @param countdown executions remaining
     */
    @Asynchronous(executor = "java:module/concurrent/max-2-executor",
                  runAt = @Schedule(cron = "0/7 * * * * *"))
    CompletableFuture<String> everySevenSecondsUntilThrowsException(AtomicInteger countdown) {
        System.out.println("> everySevenSecondsUntilThrowsException " + countdown);

        if (countdown.decrementAndGet() == 0) {
            ArrayIndexOutOfBoundsException x;
            x = new ArrayIndexOutOfBoundsException("Countdown has reached 0.");
            System.out.println("< everySevenSecondsUntilThrowsException intentionally" +
                               " raised exception on " + Thread.currentThread() + ": " +
                               x);
            throw x;
        } else {
            System.out.println("< everySevenSecondsUntilThrowsException executed on " +
                               Thread.currentThread());
            return null;
        }
    }

    /**
     * Combines 4 different schedules to run on seconds that have a remainder of 1 when divided by 6:
     * 1 7 13 19 25 31 37 43 49 55.
     * This could be achieved with a single schedule, but the point of this test is to combine many
     * schedules, including some that use cron with others that don't.
     *
     * @param maxExecutions  number of executions to stop after.
     * @param executionCount for tracking the total number of executions.
     * @return null to continue with more executions.
     *         To stop, returns a completed future with the execution count and current time in nanoseconds.
     */
    @Asynchronous(runAt = { @Schedule(cron = "1/12 * * * * *", zone = "America/New_York"),
                            @Schedule(hours = {}, minutes = {}, seconds = { 19, 55 }, zone = "America/Chicago"),
                            @Schedule(cron = "31 * * * * *", zone = "America/Denver"),
                            @Schedule(hours = {}, minutes = {}, seconds = { 7, 43 }, zone = "America/Los_Angeles") })
    CompletionStage<long[]> everySixSeconds(int maxExecutions, AtomicInteger executionCount) {
        int count = executionCount.incrementAndGet();
        System.out.println("> everySixSeconds " + count);

        CompletableFuture<long[]> result = null;
        if (count >= maxExecutions) {
            result = Asynchronous.Result.complete(new long[] { count, System.nanoTime() });
        }

        System.out.println("< everySixSeconds " + result);
        return result;
    }

    /**
     * Runs on seconds that are divisible by 2 or 3.
     *
     * @param maxExecutions  number of executions to stop after.
     * @param executionCount for tracking the total number of executions.
     * @return null to continue with more executions.
     *         To stop, returns a completed future with the execution count and current time in nanoseconds.
     */
    @Asynchronous(runAt = { @Schedule(cron = "*/3 * * * * *"),
                            @Schedule(cron = "*/2 * * * * *") })
    CompletableFuture<long[]> everyThreeOrEvenSeconds(int maxExecutions, AtomicInteger executionCount) {
        int count = executionCount.incrementAndGet();
        System.out.println("> everyThreeOrEvenSeconds " + count);

        CompletableFuture<long[]> result = null;
        if (count >= maxExecutions) {
            result = Asynchronous.Result.getFuture();
            result.obtrudeValue(new long[] { count, System.nanoTime() });
        }

        System.out.println("< everyThreeOrEvenSeconds " + result);
        return result;
    }

    /**
     * Look up the specified JNDI name every 6 seconds, on seconds that have a remainder of 5 when
     * divided by 6.
     *
     * @param jndiName              JNDI name to look up.
     * @param lookupResults         results of the lookup.
     * @param cancellationCountdown countdown after which this method cancels itself.
     */
    @Asynchronous(executor = "java:module/concurrent/max-2-executor",
                  runAt = @Schedule(cron = "5/12 * * * JAN-DEC SUN-SAT"))
    void lookUpAtSixSecondIntervals(String jndiName, LinkedBlockingQueue<Object> lookupResults, AtomicInteger cancellationCountdown) {
        System.out.println("> lookUpAtSixSecondIntervals " + cancellationCountdown);

        Object result;
        try {
            lookupResults.add(result = InitialContext.doLookup(jndiName));
        } catch (Throwable x) {
            lookupResults.add(result = x);
        }

        if (cancellationCountdown.decrementAndGet() == 0)
            Asynchronous.Result.getFuture().cancel(false);

        System.out.println("< lookUpAtSixSecondIntervals " + result);
    }

    /**
     * Attempt to run on a day that doesn't exist: February 30th.
     */
    @Asynchronous(runAt = @Schedule(months = Month.FEBRUARY,
                                    daysOfMonth = 30))
    CompletionStage<String> runOnFebruary30() {
        return Asynchronous.Result.complete("Should not be running runOnFebruary30!");
    }

    /**
     * Specify an empty list of seconds to run at.
     *
     * @throws IllegalArgumentException for the invalid empty list value on the annotation.
     */
    @Asynchronous(runAt = @Schedule(seconds = {}))
    void withEmptySeconds() {
        System.out.println("Should not be running withEmptySeconds.");
    }
}
