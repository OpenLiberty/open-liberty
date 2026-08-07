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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;

import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;

@ManagedScheduledExecutorDefinition
/**/ (name = "java:comp/concurrent/cdi/async-3-scheduler",
      maxAsync = 3,
      //TODO: qualifiers = Async3Scheduler.class,
      virtual = false)
@SuppressWarnings("serial")
@WebServlet("/*")
public class Concurrency32CDITestServlet extends FATServlet {

    // Poll interval in nanoseconds
    static final long POLL_NS = TimeUnit.MILLISECONDS.toNanos(200);

    // Maximum number of nanoseconds to wait for a task to finish.
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    // Futures to cancel between tests, to reduce the chance of failures in
    // one test method interfering with others
    private final Set<Future<?>> cancelAfterTest = Collections //
                    .newSetFromMap(new ConcurrentHashMap<Future<?>, Boolean>());

    //@Inject
    //@Async3Scheduler
    // TODO switch to the above
    @Resource(lookup = "java:comp/concurrent/cdi/async-3-scheduler")
    ManagedScheduledExecutorService async3Scheduler;

    static final AtomicLong initTimeNS = new AtomicLong(0);

    @Inject
    LoopbackBean loopbackBean;

    @Inject
    ReadLockBean readLockBean;

    @Inject
    SchedulingBean schedulingBean;

    ExecutorService testThreads = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    WriteLockBean writeLockBean;

    /**
     * Cancel futures that are still incomplete when tests methods end,
     * in order to prevent long-running tasks and queued tasks from
     * interfering with subsequent tests.
     * Ideally, there should never be any futures left incomplete after
     * successful execution of a test method, and this should only be
     * needed for when test methods fail and cannot easily clean up.
     */
    @After
    void cancelFuturesAfterTest() {
        int count = 0;
        for (Iterator<Future<?>> it = cancelAfterTest.iterator(); it.hasNext();) {
            Future<?> f = it.next();
            if (!f.isDone() && f.cancel(true))
                count++;
        }
        if (count > 0)
            System.out.println("Canceled " + count + " futures after previous test");
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        initTimeNS.compareAndSet(0, System.nanoTime());
    }

    /**
     * Tests the current thread being interrupted while waiting for a lock
     */
    @Test
    public void testInterruptedDuringBeanMethod() throws Exception {
        try {
            writeLockBean.interruptSelf();
            fail("Bean method should raise InterruptedException.");
        } catch (InterruptedException x) {
            // expected
        }
    }

    /**
     * Tests the current thread being interrupted while waiting for a lock
     */
    @Test
    public void testInterruptedWhileAttemptingLock() throws Exception {
        Thread currentThread = Thread.currentThread();

        // thread2 acquires the WRITE lock and repeatedly interrupts the current thread
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch doneInterrupting = new CountDownLatch(1);
        Future<?> thread2Future = testThreads.submit(() -> writeLockBean //
                        .interruptRepeatedly(currentThread,
                                             running,
                                             doneInterrupting));
        cancelAfterTest.add(thread2Future);
        running.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        // current thread attempts to acquire the WRITE lock while thread2 holds it;
        // the repeated interrupts must cause this to raise InterruptedException
        try {
            writeLockBean.blockingWriteNumber(22);
            fail("Expected InterruptedException while waiting for the WRITE lock.");
        } catch (IllegalStateException x) {
            if (x.getCause() instanceof InterruptedException)
                ; // expected
            else
                throw x;
        }

        // cancel and wait for thread 2 to stop interrupting
        thread2Future.cancel(true);
        for (boolean thread2Done = false; !thread2Done;)
            try {
                assertEquals(true,
                             thread2Done = doneInterrupting //
                                             .await(TIMEOUT_NS,
                                                    TimeUnit.NANOSECONDS));
            } catch (InterruptedException x) {
                thread2Done = false;
            }
    }

    /**
     * A bean method that lacks the Schedule annotation does not run automatically.
     */
    @Test
    public void testMethodWithoutScheduleAnnotationDoesNotRunAutomatically() //
                    throws InterruptedException {
        CountDownLatch methodStarts = schedulingBean.trackerOfNotScheduled();

        // wait up to 15 seconds past initialization to see if it runs
        long elapsedNS = System.nanoTime() - initTimeNS.get();;
        long remainingNS = TimeUnit.SECONDS.toNanos(15) - elapsedNS;
        if (remainingNS > 0)
            assertEquals(false,
                         methodStarts.await(remainingNS, TimeUnit.NANOSECONDS));
        else
            assertEquals(1, // countDown was never invoked
                         methodStarts.getCount());
    }

    /**
     * Confirm that a java:comp lookup can be successfully performed from a
     * scheduled task.
     */
    @Test
    public void testJavaCompAccessibleFromScheduledTask() throws Exception {
        ScheduledFuture<?> future = async3Scheduler //
                        .schedule(() -> InitialContext //
                                        .doLookup("java:comp/env/TestEntry"),
                                  300,
                                  TimeUnit.MILLISECONDS);
        assertEquals("value3",
                     future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Bean methods that require a READ Lock must be able to run at the same time.
     * An ApplicationScoped CDI bean is annotated with a Lock of type READ, which
     * becomes the default for all bean methods unless explicitly overridden by
     * annotating the method.
     */
    @Test
    public void testReadLockAcquiredByMultipleThreads() throws Exception {
        readLockBean.writeValue("testReadLockAcquiredByMultipleThreads");

        // read by single thread
        assertEquals("testReadLockAcquiredByMultipleThreads",
                     readLockBean.readValue());

        // have thread2 acquire READ lock on bean
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch running = new CountDownLatch(1);
        Future<?> thread2Future = testThreads.submit(() -> readLockBean //
                        .delayedReadValue(running, blocker));
        cancelAfterTest.add(thread2Future);
        running.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        // current thread must also be able to acquire READ lock on bean
        assertEquals("testReadLockAcquiredByMultipleThreads",
                     readLockBean.readValue());

        assertEquals("testReadLockAcquiredByMultipleThreads",
                     readLockBean.blockingReadValue());

        // allow thread2 to complete
        blocker.countDown();

        // current thread can acquire WRITE lock now
        readLockBean.blockingWriteValue(null);

        assertEquals(null,
                     readLockBean.readValue());

        thread2Future.cancel(true);
    }

    /**
     * Invoke a bean method annotated Lock(READ) which invokes another
     * method of the same bean instance annotated Lock(READ).
     * The LoopbackBean.power method is annotated Lock(READ) and recursively
     * invokes itself through the CDI proxy, also annotated Lock(READ).
     * Because READ locks are shared and reentrant, this must succeed.
     */
    @Test
    public void testReadLockLoopbackToReadLock() throws Exception {
        loopbackBean.setNumber(3);

        // power(4, bean) returns number^4 = 81; exercises deep READ->READ recursion
        assertEquals(81L, loopbackBean.power(4, loopbackBean));
    }

    /**
     * Invoke a bean method annotated Lock(READ) which invokes another
     * method of the same bean instance annotated Lock(WRITE) and confirm
     * that this raises IllegalStateException as required by the Lock
     * annotation.
     * The LoopbackBean.square method is annotated Lock(READ) and calls
     * setNumber which is annotated Lock(WRITE, accessTimeout=IMMEDIATE).
     * Upgrading from READ to WRITE on the same thread is prohibited, so
     * IllegalStateException must be raised.
     */
    @Test
    public void testReadLockLoopbackToWriteLock() throws Exception {
        loopbackBean.setNumber(5);

        try {
            loopbackBean.square(loopbackBean);
            fail("Expected IllegalStateException when a READ-locked method " +
                 "attempts to invoke a WRITE-locked method via loopback.");
        } catch (IllegalStateException x) {
            // expected: READ -> WRITE loopback is not permitted
        }

        // The value must be unchanged because the write was blocked
        assertEquals(5L, loopbackBean.getNumber());
    }

    /**
     * When the current thread holds a READ lock, another thread must not be able
     * to acquire a WRITE lock.
     */
    @Test
    public void testReadLockPreventsWriteLockFromOtherThread() throws Exception {
        readLockBean.writeValue("testReadLockPreventsWriteLockFromOtherThread");

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch running = new CountDownLatch(1);
        CompletableFuture<?> thread2Future = CompletableFuture.runAsync(() -> {
            // wait for main thread to acquire READ lock
            try {
                assertEquals(true,
                             running.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            } catch (InterruptedException x) {
                throw new AssertionError(x);
            }

            try {
                readLockBean.writeValue("thread2's value");
            } finally {
                // allow main thread to complete
                blocker.countDown();
            }
        });

        // obtain READ lock and block until thread2 allows us to continue
        assertEquals("testReadLockPreventsWriteLockFromOtherThread",
                     readLockBean.delayedReadValue(running, blocker));

        try {
            thread2Future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        } catch (ExecutionException x) {
            if (x.getCause() instanceof TimeoutException) // TODO different exception
                ; // expected
            else
                throw x;
        }

        try {
            thread2Future.join();
        } catch (CompletionException x) {
            if (x.getCause() instanceof TimeoutException) // TODO different exception
                ; // expected
            else
                throw x;
        }

        readLockBean.blockingWriteValue(null);
    }

    /**
     * A bean method that is annotated Asynchronous and Schedule must raise
     * UnsupportedOperationException.
     */
    @Test
    public void testRejectAsynchronousAndScheduleOnSameMethod() {
        try {
            schedulingBean.alsoAsynchronous();
            fail("Expected UnsupportedOperationException for a method " +
                 "annotated both @Asynchronous and @Schedule.");
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
            // TODO NLS message prefix can be asserted once added
                !x.getMessage().contains("alsoAsynchronous") ||
                !x.getMessage().contains("@Asynchronous") ||
                !x.getMessage().contains("@Schedule"))
                throw x;
            // else expected error
        }
    }

    /**
     * A bean method that is scheduled to automatically run every 4 seconds,
     * but completes itself the first time it runs must run exactly once.
     */
    @Test
    public void testScheduledMethodCompletesItselfAfter1Execution() //
                    throws InterruptedException {
        AtomicInteger executionCount = schedulingBean.trackerOfOnceOn4thSecond();
        for (long start = System.nanoTime(); //
                        System.nanoTime() - start < TIMEOUT_NS &&
                                             executionCount.get() == 0; //
                        TimeUnit.NANOSECONDS.sleep(POLL_NS));

        assertEquals(1L,
                     executionCount.get());

        // wait up to 15 seconds past initialization to see if it runs a second time
        long elapsedNS = System.nanoTime() - initTimeNS.get();;
        long remainingNS = TimeUnit.SECONDS.toNanos(15) - elapsedNS;
        if (remainingNS > 0)
            TimeUnit.NANOSECONDS.sleep(remainingNS);

        assertEquals(1L,
                     executionCount.get());
    }

    /**
     * A bean method that is scheduled (with a cron expression) to automatically
     * run every 3 seconds indefinitely continues running multiple times.
     */
    @Test
    public void testScheduledMethodRepeats() //
                    throws InterruptedException {
        Thread execThread;
        LinkedBlockingQueue<Thread> execThreads = //
                        schedulingBean.trackerOfEvery3SecondsCron();

        // verify that it runs 3 times
        assertNotNull(execThread = execThreads.poll(TIMEOUT_NS,
                                                    TimeUnit.NANOSECONDS));

        assertNotNull(execThread = execThreads.poll(TIMEOUT_NS,
                                                    TimeUnit.NANOSECONDS));

        assertNotNull(execThread = execThreads.poll(TIMEOUT_NS,
                                                    TimeUnit.NANOSECONDS));

        // clear out all executions up to current point in time
        for (execThread = execThreads.poll(); //
                        execThread != null; //
                        execThread = execThreads.poll());

        // verify that it is still running
        assertNotNull(execThread = execThreads.poll(TIMEOUT_NS,
                                                    TimeUnit.NANOSECONDS));
        assertEquals(false, execThread == Thread.currentThread());
    }

    /**
     * A bean method that is scheduled to automatically run every 5 seconds,
     * but raises an exception its third execution, runs only 3 times.
     */
    @Test
    public void testScheduledMethodStopsAfterRaisingException() //
                    throws InterruptedException {

        AtomicInteger executionCount = schedulingBean.trackerOfEvery5Seconds3Times();
        for (long start = System.nanoTime(); //
                        System.nanoTime() - start < TIMEOUT_NS &&
                                             executionCount.get() < 3; //
                        TimeUnit.NANOSECONDS.sleep(POLL_NS));

        assertEquals(3L,
                     executionCount.get());

        // wait up to 25 seconds past initialization to see if it runs a 4th time
        long elapsedNS = System.nanoTime() - initTimeNS.get();;
        long remainingNS = TimeUnit.SECONDS.toNanos(25) - elapsedNS;
        if (remainingNS > 0)
            TimeUnit.NANOSECONDS.sleep(remainingNS);

        assertEquals(3L,
                     executionCount.get());
    }

    /**
     * Two methods annotated Lock and Schedule attempt to run every third second
     * four times and increment a shared counter in a way that will lose updates
     * if the executions overlap. Verify the counter records all 8 executions.
     */
    @Test
    public void testScheduledMethodsWithWriteLockDoNotOverlap() //
                    throws InterruptedException {
        AtomicInteger executionCount = schedulingBean //
                        .trackerOfLockEvery3Seconds4Times();

        for (long start = System.nanoTime(); //
                        System.nanoTime() - start < TIMEOUT_NS * 2 &&
                                             executionCount.get() < 8L; //
                        TimeUnit.NANOSECONDS.sleep(POLL_NS));

        assertEquals(8L,
                     executionCount.get());

        // wait up to 20 seconds past initialization to find out if any additional
        // executions occur
        long elapsedNS = System.nanoTime() - initTimeNS.get();;
        long remainingNS = TimeUnit.SECONDS.toNanos(20) - elapsedNS;
        if (remainingNS > 0)
            TimeUnit.NANOSECONDS.sleep(remainingNS);

        assertEquals(8L,
                     executionCount.get());
    }

    /**
     * Invoke a bean method annotated Lock(WRITE) which invokes another
     * method of the same bean instance annotated Lock(READ).
     * The LoopbackBean.increment method is annotated Lock(WRITE) and calls
     * getNumber which is annotated Lock(READ, accessTimeout=UNLIMITED).
     * A thread holding the WRITE lock may also acquire a READ lock, so
     * this must succeed and leave the number incremented by one.
     */
    @Test
    public void testWriteLockLoopbackToReadLock() throws Exception {
        loopbackBean.setNumber(2);

        loopbackBean.increment(loopbackBean);

        assertEquals(3L, loopbackBean.getNumber());
    }

    /**
     * Invoke a bean method annotated Lock(WRITE) which invokes another
     * method of the same bean instance annotated Lock(WRITE).
     * The LoopbackBean.clear method is annotated Lock(WRITE) and calls
     * setNumber which is annotated Lock(WRITE, accessTimeout=IMMEDIATE).
     * Because WRITE locks are reentrant on the same thread, this must
     * succeed and leave the number set to zero.
     */
    @Test
    public void testWriteLockLoopbackToWriteLock() throws Exception {
        loopbackBean.setNumber(99);
        assertEquals(99L, loopbackBean.getNumber());

        loopbackBean.clear(loopbackBean);

        assertEquals(0L, loopbackBean.getNumber());
    }

    /**
     * Bean methods that require a WRITE Lock must not run at the same time on
     * different threads.
     * An ApplicationScoped CDI bean is annotated with a Lock of type WRITE, which
     * becomes the default for all bean methods unless explicitly overridden by
     * annotating the method.
     */
    @Test
    public void testWriteLockNotAcquiredByMultipleThreads() throws Exception {

        // write by single thread
        writeLockBean.writeNumber(41);

        // have thread2 acquire WRITE lock on bean
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch running = new CountDownLatch(1);
        Future<Boolean> thread2Future = testThreads.submit(() -> writeLockBean //
                        .delayedWriteNumber(running, blocker, 51));
        cancelAfterTest.add(thread2Future);
        running.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        // have thread3 attempt WRITE lock and end up blocked
        Future<?> thread3Future = testThreads.submit(() -> writeLockBean //
                        .blockingWriteNumber(61));
        cancelAfterTest.add(thread3Future);

        // current thread must not be able to acquire WRITE lock on bean
        try {
            writeLockBean.writeNumber(71);
            fail("Should not be able to acquire the WRITE lock while the second" +
                 " thread still holds the WRITE lock.");
        } catch (CompletionException x) {
            if (x.getCause() instanceof TimeoutException) // TODO different exception
                ; // expected
            else
                throw x;
        }

        // current thread must not be able to acquire READ lock on bean
        try {
            int num = writeLockBean.readNumber();
            fail("Should not be able to acquire a READ lock while the second" +
                 " thread still holds the WRITE lock. Result: " + num);
        } catch (CompletionException x) {
            if (x.getCause() instanceof TimeoutException) // TODO different exception
                ; // expected
            else
                throw x;
        }

        try {
            thread3Future.get(1, TimeUnit.SECONDS);
            fail("A third thread should not be able to acquire the WRITE lock" +
                 " while the second thread still holds the WRITE lock.");
        } catch (TimeoutException x) {
            // expected
        }

        // interrupt thread3 while it is waiting for the WRITE lock
        assertEquals(true,
                     thread3Future.cancel(true));

        // allow thread2 to complete
        blocker.countDown();

        // current thread can acquire READ lock now
        assertEquals(51,
                     writeLockBean.blockingReadNumber());

        // current thread can acquire WRITE lock now
        writeLockBean.writeNumber(81);

        assertEquals(81,
                     writeLockBean.readNumber());

        assertEquals(true,
                     thread2Future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * The Lock annotation also applies to methods annotated Asynchronous,
     * such that the execution of the asynchronous work (not the submission of it)
     * is subject to the lock.
     */
    @Test
    public void testWriteLockOnAsyncMethod() throws Exception {

        // write by single thread
        writeLockBean.writeNumber(90);

        // invoke an asynchronous method and wait for it to start running
        CountDownLatch blocker1 = new CountDownLatch(1);
        CountDownLatch asyncMethod1Running = new CountDownLatch(1);
        CompletableFuture<Integer> future1 = writeLockBean //
                        .asyncWriteNumber(asyncMethod1Running, blocker1, 91);
        cancelAfterTest.add(future1);

        asyncMethod1Running.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        // invoke a second asynchronous method that is subject to the same
        // WRITE Lock and verify it does not run yet
        CountDownLatch blocker2 = new CountDownLatch(1);
        CountDownLatch asyncMethod2Running = new CountDownLatch(1);
        CompletableFuture<Integer> future2 = writeLockBean //
                        .asyncWriteNumber(asyncMethod2Running, blocker2, 92);
        cancelAfterTest.add(future2);

        assertEquals(false,
                     asyncMethod2Running.await(2, TimeUnit.SECONDS));

        // allow the first asynchronous method to complete
        blocker1.countDown();

        // the second asynchronous method must start now
        assertEquals(true,
                     asyncMethod2Running.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals(Integer.valueOf(91),
                     future1.join());

        // allow the second asynchronous method to complete
        blocker2.countDown();
        assertEquals(Integer.valueOf(92),
                     future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals(92,
                     writeLockBean.blockingReadNumber());
    }
}
