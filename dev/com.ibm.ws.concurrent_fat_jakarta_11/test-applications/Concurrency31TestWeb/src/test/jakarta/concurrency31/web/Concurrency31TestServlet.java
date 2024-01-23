/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.concurrency31.web;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.ALL_REMAINING;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.APPLICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;

import org.junit.Test;

import componenttest.app.FATServlet;

@ContextServiceDefinition(name = "java:module/concurrent/my-context",
                          propagated = APPLICATION,
                          cleared = ALL_REMAINING)
@ManagedExecutorDefinition(name = "java:module/concurrent/virtual-executor",
                           context = "java:module/concurrent/my-context",
                           virtual = true)
@ManagedScheduledExecutorDefinition(name = "java:comp/concurrent/virtual-scheduled-executor",
                                    context = "java:module/concurrent/my-context",
                                    virtual = true)
@ManagedThreadFactoryDefinition(name = "java:module/concurrent/virtual-thread-factory",
                                context = "java:module/concurrent/my-context",
                                virtual = true)
@SuppressWarnings("serial")
@WebServlet("/*")
public class Concurrency31TestServlet extends FATServlet {

    // Maximum number of nanoseconds to wait for a task to finish.
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Override
    public void destroy() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    }

    /**
     * Use a managed-thread-factory from the web.xml deployment descriptor with virtual=false
     * to request platform threads.
     */
    @Test
    public void testPlatformThreadFactoryWebDD() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable action = () -> {
            try {
                results.add(InitialContext.doLookup("java:comp/concurrent/webdd/virtual-thread-factory"));
            } catch (Throwable x) {
                results.add(x);
            }
        };

        ManagedThreadFactory threadFactory = InitialContext.doLookup("java:global/concurrent/webdd/platform-thread-factory");
        Thread thread1 = threadFactory.newThread(action);
        assertEquals(7, thread1.getPriority());
        assertEquals(Boolean.FALSE, Thread.class.getMethod("isVirtual").invoke(thread1));
        thread1.start();

        Object result;
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("An error occurred on the thread.", (Throwable) result);
    }

    /**
     * Use ManagedScheduledExecutorDefinition with virtual=true to schedule a repeating timer
     * to run on virtual threads. Verify that all executions run on different virtual threads and that
     * context is propagated to these threads.
     */
    @Test
    public void testRepeatingTimerOnVirtualThreads() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        final AtomicInteger executionCount = new AtomicInteger();
        final AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

        Runnable repeatedTask = () -> {
            int count = executionCount.incrementAndGet();
            Thread curThread = Thread.currentThread();
            System.out.println("testRepeatingTimerOnVirtualThreads task execution " + count + " on " + curThread);

            if (count == 3)
                futureRef.get().cancel(false);

            results.add(curThread);
            try {
                results.add(InitialContext.doLookup("java:comp/concurrent/virtual-scheduled-executor"));
            } catch (Throwable x) {
                results.add(x);
            }
        };

        ManagedScheduledExecutorService scheduledExecutor = InitialContext.doLookup("java:comp/concurrent/virtual-scheduled-executor");
        futureRef.set(scheduledExecutor.scheduleAtFixedRate(repeatedTask, 200, 100, TimeUnit.MILLISECONDS));

        Object result;
        Set<Thread> uniqueVirtualThreads = new HashSet<Thread>();

        // execution 1
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(result));
        uniqueVirtualThreads.add((Thread) result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("Lookup failed on first execution", (Throwable) result);

        // execution 2
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(result));
        uniqueVirtualThreads.add((Thread) result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("Lookup failed on first execution", (Throwable) result);

        // execution 3
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(result));
        uniqueVirtualThreads.add((Thread) result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("Lookup failed on first execution", (Throwable) result);

        // each execution must use a different virtual thread
        assertEquals(uniqueVirtualThreads.toString(), 3, uniqueVirtualThreads.size());

        // The task self-cancels on the third execution. There must be no more executions after this.
        assertEquals(null, results.poll(150, TimeUnit.MILLISECONDS));
    }

    /**
     * Use ManagedScheduledExecutorDefinition with virtual=true to schedule a one-shot timer
     * to run on a virtual thread.
     */
    @Test
    public void testOneShotTimerOnVirtualThread() throws Exception {
        ManagedScheduledExecutorService scheduledExecutor = InitialContext.doLookup("java:comp/concurrent/virtual-scheduled-executor");
        ScheduledFuture<Thread> future = scheduledExecutor.schedule(Thread::currentThread, 150, TimeUnit.MILLISECONDS);
        Thread thread = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread));
    }

    /**
     * Use ManagedExecutorDefinition with virtual=true to submit a task to run on a virtual thread.
     */
    @Test
    public void testSubmitToVirtualThread() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:module/concurrent/virtual-executor");
        Future<Thread> future = executor.submit(Thread::currentThread);
        Thread thread = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread));
    }

    /**
     * Use ManagedExecutorDefinition with virtual=true to submit multiple tasks to run on virtual threads,
     * with a timeout before which all must complete.
     */
    @Test
    public void testTimedInvokeAllOnVirtualThreads() throws Exception {
        Callable<Object> task = () -> {
            try {
                InitialContext.doLookup("java:module/concurrent/virtual-executor");
            } catch (Throwable x) {
                return x;
            }
            return Thread.currentThread();
        };

        ManagedExecutorService executor = InitialContext.doLookup("java:module/concurrent/virtual-executor");
        List<Future<Object>> futures = executor.invokeAll(List.of(task, task, task),
                                                          TIMEOUT_NS,
                                                          TimeUnit.NANOSECONDS);

        assertEquals(futures.toString(), 3, futures.size());

        Object threadOrLookupFailure;

        assertNotNull(threadOrLookupFailure = futures.get(0).get(1, TimeUnit.MILLISECONDS));
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(threadOrLookupFailure));

        assertNotNull(threadOrLookupFailure = futures.get(1).get(1, TimeUnit.MILLISECONDS));
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(threadOrLookupFailure));

        assertNotNull(threadOrLookupFailure = futures.get(2).get(1, TimeUnit.MILLISECONDS));
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(threadOrLookupFailure));
    }

    /**
     * Use ManagedExecutorDefinition with virtual=true to submit multiple tasks to run on virtual threads,
     * with a timeout before which one must complete.
     */
    @Test
    public void testTimedInvokeAnyOnVirtualThreads() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:module/concurrent/virtual-executor");
        Thread thread = executor.invokeAny(List.of(Thread::currentThread,
                                                   Thread::currentThread),
                                           TIMEOUT_NS,
                                           TimeUnit.NANOSECONDS);

        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread));
    }

    // TODO after the workaround is removed, write: public void testUntimedInvokeAllOnVirtualThreads() throws Exception {

    /**
     * Use ManagedExecutorDefinition with virtual=true to submit multiple tasks to run on virtual threads,
     * waiting until the first one completes.
     */
    @Test
    public void testUntimedInvokeAnyOnVirtualThreads() throws Exception {
        Callable<Object> anyTask = () -> {
            try {
                InitialContext.doLookup("java:module/concurrent/virtual-executor");
            } catch (Throwable x) {
                return x;
            }
            return Thread.currentThread();
        };

        ManagedExecutorService executor = InitialContext.doLookup("java:module/concurrent/virtual-executor");

        Object threadOrLookupFailure = executor.invokeAny(List.of(anyTask, anyTask));

        if (threadOrLookupFailure instanceof Throwable)
            throw new AssertionError("Task failed, see cause.", (Throwable) threadOrLookupFailure);

        Thread thread = (Thread) threadOrLookupFailure;

        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread));
    }

    // TODO after the workaround is removed, write: public void testUntimedInvokeAnyOneOnVirtualThread() throws Exception {

    /**
     * Use ManagedThreadFactoryDefinition with virtual=true to request virtual threads.
     * TODO It can also define custom context to propagate to the managed virtual thread.
     */
    @Test
    public void testVirtualThreadFactoryAnno() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable action = () -> {
            try {
                results.add(InitialContext.doLookup("java:module/concurrent/virtual-thread-factory"));
            } catch (Throwable x) {
                results.add(x);
            }
        };

        ManagedThreadFactory threadFactory = InitialContext.doLookup("java:module/concurrent/virtual-thread-factory");
        Thread thread1 = threadFactory.newThread(action);
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread1));
        thread1.start();

        Thread thread2 = threadFactory.newThread(action);
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread2));
        thread2.start();

        Object result;
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("An error occurred on one of the virtual threads.", (Throwable) result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("An error occurred on the other virtual thread.", (Throwable) result);
    }

    /**
     * Use a managed-thread-factory from the application.xml deployment descriptor with virtual=true
     * to request virtual threads.
     * TODO It can also define custom context to propagate to the managed virtual thread.
     */
    @Test
    public void testVirtualThreadFactoryAppDD() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable action = () -> {
            try {
                results.add(InitialContext.doLookup("java:app/concurrent/appdd/virtual-thread-factory"));
            } catch (Throwable x) {
                results.add(x);
            }
        };

        ManagedThreadFactory threadFactory = InitialContext.doLookup("java:app/concurrent/appdd/virtual-thread-factory");
        Thread thread1 = threadFactory.newThread(action);
        assertEquals(Thread.NORM_PRIORITY, thread1.getPriority());
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread1));
        thread1.start();

        Object result;
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("An error occurred on the virtual thread.", (Throwable) result);
    }

    /**
     * Use a managed-thread-factory from the web.xml deployment descriptor with virtual=true
     * to request virtual threads.
     * TODO It can also define custom context to propagate to the managed virtual thread.
     */
    @Test
    public void testVirtualThreadFactoryWebDD() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable action = () -> {
            try {
                results.add(InitialContext.doLookup("java:comp/concurrent/webdd/virtual-thread-factory"));
            } catch (Throwable x) {
                results.add(x);
            }
        };

        ManagedThreadFactory threadFactory = InitialContext.doLookup("java:comp/concurrent/webdd/virtual-thread-factory");
        Thread thread1 = threadFactory.newThread(action);
        assertEquals(Thread.NORM_PRIORITY, thread1.getPriority());
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread1));
        thread1.start();

        Object result;
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("An error occurred on the virtual thread.", (Throwable) result);
    }
}
