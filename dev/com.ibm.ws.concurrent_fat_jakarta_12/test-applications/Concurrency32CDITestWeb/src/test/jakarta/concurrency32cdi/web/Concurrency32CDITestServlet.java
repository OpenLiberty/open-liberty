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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    ReadLockBean readLockBean;

    ExecutorService testThreads = Executors.newVirtualThreadPerTaskExecutor();

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
}
