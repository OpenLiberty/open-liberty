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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    @Inject
    LoopbackBean loopbackBean;

    @Inject
    ReadLockBean readLockBean;

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
}
