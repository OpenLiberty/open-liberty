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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.CronTrigger;
import jakarta.enterprise.concurrent.LastExecution;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import jakarta.enterprise.concurrent.Trigger;
import jakarta.enterprise.concurrent.ZonedTrigger;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.context.timezone.TimeZone;

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

    /**
     * A single instance to share across tests that are okay with using context that
     * was captured during servlet init.
     */
    private ManagedThreadFactory virtualThreadFactory;

    @Override
    public void destroy() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            virtualThreadFactory = (ManagedThreadFactory) InitialContext //
                            .doLookup("java:module/concurrent/virtual-thread-factory");
        } catch (NamingException x) {
            throw new ServletException(x);
        }
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
                results.add(InitialContext.doLookup("java:comp/env/TestEntry"));
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
        assertEquals("TestValue1", result);
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
                results.add(InitialContext.doLookup("java:comp/env/TestEntry"));
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
        assertEquals("TestValue1", result);

        // execution 2
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(result));
        uniqueVirtualThreads.add((Thread) result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("Lookup failed on first execution", (Throwable) result);
        assertEquals("TestValue1", result);

        // execution 3
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(result));
        uniqueVirtualThreads.add((Thread) result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("Lookup failed on first execution", (Throwable) result);
        assertEquals("TestValue1", result);

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
                InitialContext.doLookup("java:comp/env/TestEntry");
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

    /**
     * Use ManagedExecutorDefinition with virtual=true to submit multiple task
     * to run on a virtual thread via the untimed invokeAll method.
     */
    @Test
    public void testUntimedInvokeAllOnVirtualThreads() throws Exception {
        Callable<Thread> task = () -> {
            InitialContext.doLookup("java:comp/env/TestEntry");
            return Thread.currentThread();
        };

        ManagedExecutorService executor = InitialContext //
                        .doLookup("java:module/concurrent/virtual-executor");

        List<Future<Thread>> futures = executor.invokeAll(List.of(task, task, task));

        assertEquals(futures.toString(), 3, futures.size());

        Set<Thread> uniqueThreads = new HashSet<Thread>();
        uniqueThreads.add(Thread.currentThread());

        Thread thread;
        assertNotNull(thread = futures.get(0).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Boolean.TRUE,
                     Thread.class.getMethod("isVirtual").invoke(thread));
        uniqueThreads.add(thread);

        assertNotNull(thread = futures.get(1).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Boolean.TRUE,
                     Thread.class.getMethod("isVirtual").invoke(thread));
        uniqueThreads.add(thread);

        assertNotNull(thread = futures.get(2).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Boolean.TRUE,
                     Thread.class.getMethod("isVirtual").invoke(thread));
        uniqueThreads.add(thread);

        assertEquals(uniqueThreads.toString(), 4, uniqueThreads.size());
        uniqueThreads.clear();

        // run from a virtual thread:
        CompletableFuture<List<Future<Thread>>> ff = new CompletableFuture<>();
        virtualThreadFactory.newThread(() -> {
            try {
                uniqueThreads.add(Thread.currentThread());
                ff.complete(executor.invokeAll(List.of(task, task, task)));
            } catch (Throwable x) {
                ff.completeExceptionally(x);
            }
        }).start();

        futures = ff.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(futures.toString(), 3, futures.size());

        assertNotNull(thread = futures.get(0).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Boolean.TRUE,
                     Thread.class.getMethod("isVirtual").invoke(thread));
        uniqueThreads.add(thread);

        assertNotNull(thread = futures.get(1).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Boolean.TRUE,
                     Thread.class.getMethod("isVirtual").invoke(thread));
        uniqueThreads.add(thread);

        assertNotNull(thread = futures.get(2).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Boolean.TRUE,
                     Thread.class.getMethod("isVirtual").invoke(thread));
        uniqueThreads.add(thread);

        assertEquals(uniqueThreads.toString(), 4, uniqueThreads.size());
    }

    /**
     * Use ManagedExecutorDefinition with virtual=true to submit a single task
     * to run on a virtual thread via the untimed invokeAny method.
     */
    @Test
    public void testUntimedInvokeAnyOneOnVirtualThread() throws Exception {
        Callable<Thread> anyTask = () -> {
            InitialContext.doLookup("java:comp/env/TestEntry");
            return Thread.currentThread();
        };

        ManagedExecutorService executor = InitialContext //
                        .doLookup("java:module/concurrent/virtual-executor");

        Thread thread = executor.invokeAny(List.of(anyTask));

        assertNotSame(Thread.currentThread(), thread); // does not run inline
        assertEquals(Boolean.TRUE,
                     Thread.class.getMethod("isVirtual").invoke(thread));

        // run from a virtual thread:
        CompletableFuture<Thread> futureThread = new CompletableFuture<>();
        Thread newThread = virtualThreadFactory.newThread(() -> {
            try {
                futureThread.complete(executor.invokeAny(List.of(anyTask)));
            } catch (Throwable x) {
                futureThread.completeExceptionally(x);
            }
        });
        newThread.start();

        thread = futureThread.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotSame(newThread, thread); // does not run inline
        assertEquals(Boolean.TRUE,
                     Thread.class.getMethod("isVirtual").invoke(thread));
    }

    /**
     * Use ManagedExecutorDefinition with virtual=true to submit multiple tasks to run on virtual threads,
     * waiting until the first one completes.
     */
    @Test
    public void testUntimedInvokeAnyOnVirtualThreads() throws Exception {
        Callable<Object> anyTask = () -> {
            try {
                InitialContext.doLookup("java:comp/env/TestEntry");
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

    /**
     * Use a managed-executor from the application.xml deployment descriptor with
     * virtual=true to request that tasks run on virtual threads.
     */
    @Test
    public void testVirtualManagedExecutorAppDD() throws Exception {
        LinkedBlockingQueue<Thread> threads = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<ZoneId> zoneIds = new LinkedBlockingQueue<>();
        Callable<Object> action = () -> {
            threads.add(Thread.currentThread());

            ZoneId zoneId = TimeZone.get();
            if (zoneId == null)
                zoneIds.add(ZoneId.of("UTC"));
            else
                zoneIds.add(zoneId);

            return InitialContext.doLookup("java:comp/env/TestEntry");
        };

        ManagedExecutorService executor = InitialContext //
                        .doLookup("java:global/concurrent/appdd/managed-executor");

        final ZoneId CENTRAL = ZoneId.of("America/Chicago");
        TimeZone.set(CENTRAL);
        try {
            Future<Object> future1 = executor.submit(action);
            Future<Object> future2 = executor.submit(action);
            Future<Object> future3 = executor.submit(action);
            Future<Object> future4 = executor.submit(action);
            Future<Object> future5 = executor.submit(action);

            TimeZone.set(ZoneId.of("America/New_York"));

            Set<Thread> uniqueVirtualThreads = new HashSet<Thread>();

            Thread thread;
            thread = threads.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Boolean.TRUE,
                         Thread.class.getMethod("isVirtual").invoke(thread));
            uniqueVirtualThreads.add(thread);

            thread = threads.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Boolean.TRUE,
                         Thread.class.getMethod("isVirtual").invoke(thread));
            uniqueVirtualThreads.add(thread);

            thread = threads.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Boolean.TRUE,
                         Thread.class.getMethod("isVirtual").invoke(thread));
            uniqueVirtualThreads.add(thread);

            thread = threads.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Boolean.TRUE,
                         Thread.class.getMethod("isVirtual").invoke(thread));
            uniqueVirtualThreads.add(thread);

            thread = threads.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Boolean.TRUE,
                         Thread.class.getMethod("isVirtual").invoke(thread));
            uniqueVirtualThreads.add(thread);

            // each execution must use a different virtual thread
            assertEquals(uniqueVirtualThreads.toString(),
                         5,
                         uniqueVirtualThreads.size());

            assertEquals(CENTRAL, zoneIds.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(CENTRAL, zoneIds.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(CENTRAL, zoneIds.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(CENTRAL, zoneIds.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(CENTRAL, zoneIds.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            try {
                Object found = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                fail("Should not be able to look up java:comp name because" +
                     " application context should be cleared. Found: " + found);
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof NamingException))
                    throw x;
            }

            try {
                Object found = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                fail("Should not be able to look up java:comp name because" +
                     " application context should be cleared. Found: " + found);
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof NamingException))
                    throw x;
            }

            try {
                Object found = future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                fail("Should not be able to look up java:comp name because" +
                     " application context should be cleared. Found: " + found);
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof NamingException))
                    throw x;
            }

            try {
                Object found = future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                fail("Should not be able to look up java:comp name because" +
                     " application context should be cleared. Found: " + found);
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof NamingException))
                    throw x;
            }

            try {
                Object found = future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                fail("Should not be able to look up java:comp name because" +
                     " application context should be cleared. Found: " + found);
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof NamingException))
                    throw x;
            }
        } finally {
            TimeZone.remove();
        }
    }

    /**
     * Use a managed-scheduled-executor from the web.xml deployment descriptor with
     * virtual=true to request that tasks run on virtual threads.
     */
    @Test
    public void testVirtualManagedScheduledExecutorWebDD() throws Exception {
        Callable<Object[]> action = () -> {
            return new Object[] {
                                  Thread.currentThread(),
                                  TimeZone.get(),
                                  InitialContext.doLookup("java:comp/env/TestEntry")
            };
        };

        ManagedScheduledExecutorService executor = InitialContext //
                        .doLookup("java:comp/concurrent/webdd/managed-scheduled-executor");

        TimeZone.set(ZoneId.of("America/Chicago"));
        try {
            Trigger trigger1 = new CronTrigger( //
                            "0-59 0-59 0-23 * JAN-DEC SUN-SAT", //
                            ZoneId.of("America/Chicago")) {
                @Override
                public ZonedDateTime getNextRunTime(LastExecution lastExec,
                                                    ZonedDateTime scheduledAt) {
                    if (lastExec == null)
                        return super.getNextRunTime(lastExec, scheduledAt);
                    else
                        return null; // only run once
                }
            };
            ScheduledFuture<Object[]> future1 = executor //
                            .schedule(action, trigger1);

            Trigger trigger2 = new ZonedTrigger() {
                @Override
                public ZonedDateTime getNextRunTime(LastExecution lastExec,
                                                    ZonedDateTime scheduledAt) {
                    if (lastExec == null)
                        return ZonedDateTime.now().plus(20, ChronoUnit.MILLIS);
                    else
                        return null; // only run once
                }
            };
            TimeZone.set(ZoneId.of("America/Denver"));
            ScheduledFuture<Object[]> future2 = executor //
                            .schedule(action, trigger2);

            TimeZone.set(ZoneId.of("America/Juneau"));
            ScheduledFuture<Object[]> future3 = executor //
                            .schedule(action, 30, TimeUnit.MILLISECONDS);

            TimeZone.set(ZoneId.of("Pacific/Honolulu"));
            ScheduledFuture<Object[]> future4 = executor //
                            .schedule(action, 40, TimeUnit.MILLISECONDS);

            TimeZone.set(ZoneId.of("America/Los_Angeles"));

            Set<Thread> uniqueVirtualThreads = new HashSet<Thread>();

            Object[] results;
            Thread thread;

            results = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            assertEquals(ZoneId.of("America/Chicago"), results[1]);
            assertEquals("TestValue1", results[2]);
            thread = (Thread) results[0];
            assertEquals(Boolean.TRUE,
                         Thread.class.getMethod("isVirtual").invoke(thread));
            uniqueVirtualThreads.add(thread);

            results = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            assertEquals(ZoneId.of("America/Denver"), results[1]);
            assertEquals("TestValue1", results[2]);
            thread = (Thread) results[0];
            assertEquals(Boolean.TRUE,
                         Thread.class.getMethod("isVirtual").invoke(thread));
            uniqueVirtualThreads.add(thread);

            results = future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            assertEquals(ZoneId.of("America/Juneau"), results[1]);
            assertEquals("TestValue1", results[2]);
            thread = (Thread) results[0];
            assertEquals(Boolean.TRUE,
                         Thread.class.getMethod("isVirtual").invoke(thread));
            uniqueVirtualThreads.add(thread);

            results = future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            assertEquals(ZoneId.of("Pacific/Honolulu"), results[1]);
            assertEquals("TestValue1", results[2]);
            thread = (Thread) results[0];
            assertEquals(Boolean.TRUE,
                         Thread.class.getMethod("isVirtual").invoke(thread));
            uniqueVirtualThreads.add(thread);

            // each execution must use a different virtual thread
            assertEquals(uniqueVirtualThreads.toString(),
                         4,
                         uniqueVirtualThreads.size());

            assertEquals(ZoneId.of("America/Los_Angeles"), TimeZone.get());
        } finally {
            TimeZone.remove();
        }
    }

    /**
     * Use ManagedThreadFactoryDefinition with virtual=true to request virtual
     * threads. Also covers propagating a cleared custom thread context to a
     * virtual thread.
     */
    @Test
    public void testVirtualThreadFactoryAnno() throws Exception {
        LinkedBlockingQueue<ZoneId> timeZones = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable action = () -> {
            try {
                ZoneId zoneId = TimeZone.get();
                if (zoneId == null)
                    timeZones.add(ZoneId.of("UTC"));
                else
                    timeZones.add(zoneId);

                results.add(InitialContext.doLookup("java:comp/env/TestEntry"));
            } catch (Throwable x) {
                results.add(x);
            }
        };

        TimeZone.set(ZoneId.of("America/New_York"));

        ManagedThreadFactory threadFactory = InitialContext //
                        .doLookup("java:module/concurrent/virtual-thread-factory");
        Thread thread1 = threadFactory.newThread(action);
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread1));
        thread1.start();

        Thread thread2 = threadFactory.newThread(action);
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread2));
        thread2.start();

        assertEquals(ZoneId.of("UTC"),
                     timeZones.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals(ZoneId.of("UTC"),
                     timeZones.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        Object result;
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("An error occurred on one of the virtual threads.", (Throwable) result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("An error occurred on the other virtual thread.", (Throwable) result);
        assertEquals("TestValue1", result);

        assertEquals(ZoneId.of("America/New_York"), TimeZone.get());
        TimeZone.remove();
    }

    /**
     * Use a managed-thread-factory from the application.xml deployment descriptor with virtual=true
     * to request virtual threads.
     */
    @Test
    public void testVirtualThreadFactoryAppDD() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable action = () -> {
            try {
                results.add(InitialContext.doLookup("java:comp/env/TestEntry"));
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
        assertEquals("TestValue1", result);
    }

    /**
     * Use a managed-thread-factory from the web.xml deployment descriptor with
     * virtual=true to request virtual threads. Verify that application context
     * and a custom thread context are propagated to the managed thread, being
     * captured from the main thread upon lookup of the ManagedThreadFactory.
     */
    @Test
    public void testVirtualThreadFactoryWebDD() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable action = () -> {
            try {
                ZoneId zoneId = TimeZone.get();
                if (zoneId == null)
                    results.add(ZoneId.of("UTC"));
                else
                    results.add(zoneId);

                results.add(InitialContext.doLookup("java:comp/env/TestEntry"));
            } catch (Throwable x) {
                results.add(x);
            }
        };

        TimeZone.set(ZoneId.of("America/Denver"));

        ManagedThreadFactory threadFactory1 = InitialContext //
                        .doLookup("java:comp/concurrent/webdd/virtual-thread-factory");

        TimeZone.set(ZoneId.of("America/Los_Angeles"));

        Thread thread1 = threadFactory1.newThread(action);
        assertEquals(Thread.NORM_PRIORITY, thread1.getPriority());
        assertEquals(Boolean.TRUE, //
                     Thread.class.getMethod("isVirtual").invoke(thread1));

        ManagedThreadFactory threadFactory2 = InitialContext //
                        .doLookup("java:comp/concurrent/webdd/virtual-thread-factory");

        TimeZone.set(ZoneId.of("America/Juneau"));

        thread1.start();

        Object result;
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(ZoneId.of("America/Denver"), result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("An error occurred on the virtual thread.", //
                            (Throwable) result);
        assertEquals("TestValue1", result);

        Thread thread2 = threadFactory2.newThread(action);
        assertEquals(Thread.NORM_PRIORITY, thread2.getPriority());
        assertEquals(Boolean.TRUE, //
                     Thread.class.getMethod("isVirtual").invoke(thread2));

        thread2.start();

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(ZoneId.of("America/Los_Angeles"), result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("An error occurred on the virtual thread.", //
                            (Throwable) result);
        assertEquals("TestValue1", result);

        assertEquals(ZoneId.of("America/Juneau"), TimeZone.get());

        TimeZone.remove();
    }

    /**
     * Starts two virtual threads that will remain blocked until they are
     * interrupted, which should occur when the application stops.
     * When interrupted, the threads print a message to System.out that
     * the test can check for later.
     */
    @Test
    public void testVirtualThreadsInterruptedWhenAppStopped() {
        CountDownLatch blocker = new CountDownLatch(1);

        virtualThreadFactory.newThread(() -> {
            try {
                blocker.await();
            } catch (InterruptedException x) {
                System.out.println("TestVirtualThreadsInterruptedWhenAppStopped1");
            }
        }).start();

        virtualThreadFactory.newThread(() -> {
            try {
                blocker.await();
            } catch (InterruptedException x) {
                System.out.println("TestVirtualThreadsInterruptedWhenAppStopped2");
            }
        }).start();
    }
}
