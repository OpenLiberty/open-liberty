/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.concurrency.web;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.ALL_REMAINING;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.APPLICATION;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.SECURITY;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.TRANSACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.LastExecution;
import jakarta.enterprise.concurrent.ManageableThread;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import jakarta.enterprise.concurrent.ZonedTrigger;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.context.list.ListContext;
import test.context.location.ZipCode;
import test.context.timing.Timestamp;

@ContextServiceDefinition(name = "java:app/concurrent/appContextSvc",
                          propagated = APPLICATION,
                          cleared = { TRANSACTION, SECURITY },
                          unchanged = ALL_REMAINING)
@ContextServiceDefinition(name = "java:module/concurrent/ZLContextSvc",
                          propagated = { ZipCode.CONTEXT_NAME, ListContext.CONTEXT_NAME },
                          cleared = "Priority",
                          unchanged = { APPLICATION, TRANSACTION })
@ManagedExecutorDefinition(name = "java:module/concurrent/executor5",
                           hungTaskThreshold = 300000,
                           maxAsync = 1) // TODO add context once annotation is fixed
@ManagedScheduledExecutorDefinition(name = "java:comp/concurrent/executor6",
                                    hungTaskThreshold = 360000,
                                    maxAsync = 2) // TODO add context once annotation is fixed
@ManagedThreadFactoryDefinition(name = "java:app/concurrent/lowPriorityThreads",
                                context = "java:app/concurrent/appContextSvc",
                                priority = 3)
@SuppressWarnings("serial")
@WebServlet("/*")
public class ConcurrencyTestServlet extends FATServlet {

    // Maximum number of nanoseconds to wait for a task to finish.
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource(lookup = "concurrent/context2")
    ContextService contextSvc2;

    @Resource(name = "java:module/env/concurrent/threadFactoryRef")
    ManagedThreadFactory defaultThreadFactory;

    @Resource(lookup = "concurrent/executor1")
    ManagedExecutorService executor1;

    @Resource(name = "java:comp/env/concurrent/executor3Ref", lookup = "concurrent/executor3")
    ManagedExecutorService executor3;

    @Resource(name = "java:global/env/concurrent/executor4Ref", lookup = "concurrent/executor4")
    ManagedScheduledExecutorService executor4;

    @Resource(lookup = "java:comp/concurrent/executor6")
    ManagedScheduledExecutorService executor6;

    @Resource(lookup = "java:comp/DefaultManagedThreadFactory")
    ForkJoinWorkerThreadFactory forkJoinThreadFactory;

    @Resource(lookup = "java:app/concurrent/lowPriorityThreads")
    ManagedThreadFactory lowPriorityThreads;

    private ExecutorService unmanagedThreads;

    @Override
    public void destroy() {
        unmanagedThreads.shutdownNow();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        unmanagedThreads = Executors.newFixedThreadPool(5);
    }

    /**
     * Look up an application-defined ContextService that is configured to propagate the
     * application component's name space. Verify that the ContextService provides access to
     * the application component's name space by attempting a lookup from a contextualized
     * completion stage action.
     */
    @Test
    public void testContextServiceDefinitionPropagatesApplicationContext() throws Exception {
        ContextService appContextSvc = InitialContext.doLookup("java:app/concurrent/appContextSvc");
        Callable<?> contextualLookup = appContextSvc.contextualCallable(() -> {
            try {
                return InitialContext.doLookup("java:app/concurrent/appContextSvc");
            } catch (NamingException x) {
                throw new CompletionException(x);
            }
        });
        Future<?> future = unmanagedThreads.submit(contextualLookup);
        Object result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(result);
        assertTrue(result.toString(), result instanceof ContextService);
    }

    /**
     * Look up an application-defined ContextService that is configured to
     * propagate some third-party context (ZipCode and List),
     * clear other third-party context (Priority and Timestamp),
     * and lave other types of context, such as Application, unchanged.
     * Verify that the ContextService behaves as configured.
     */
    @Test
    public void testContextServiceDefinitionPropagatesThirdPartyContext() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:module/concurrent/ZLContextSvc");

        // Put some fake context onto the thread:
        Timestamp.set();
        ZipCode.set(55901);
        ListContext.newList();
        ListContext.add(10);
        ListContext.add(28);
        Thread.currentThread().setPriority(7);
        Long ts0 = Timestamp.get();
        Thread.sleep(100); // ensure we progress from the current timestamp

        try {
            // Contextualize a Supplier with the above context:
            Supplier<Object[]> contextualSupplier = contextSvc.contextualSupplier(() -> {
                // The Supplier records the context
                Object lookupResult;
                try {
                    lookupResult = InitialContext.doLookup("java:app/concurrent/appContextSvc");
                } catch (NamingException x) {
                    lookupResult = x;
                }
                ListContext.add(46); // verify this change is included
                return new Object[] {
                                      lookupResult,
                                      ZipCode.get(),
                                      ListContext.asString(),
                                      Thread.currentThread().getPriority(),
                                      Timestamp.get()
                };
            });

            // Alter soem of the context on the current thread
            ZipCode.set(55906);
            ListContext.newList();
            ListContext.add(5);

            // Run with the captured context:
            Object[] results = contextualSupplier.get();

            // Application context was configured to be left unchanged, so the java:app name must be found:
            if (results[0] instanceof Throwable)
                throw new AssertionError(results[0]);
            assertTrue(results[0].toString(), results[0] instanceof ContextService);

            // Zip code context was configured to be propagated
            assertEquals(Integer.valueOf(55901), results[1]);

            // List context was configured to be propagated
            assertEquals("[10, 28, 46]", results[2]);

            // Priority context was configured to be cleared
            assertEquals(Integer.valueOf(5), results[3]);

            // Timestamp context was implicitly configured to be cleared
            assertNull(results[4]);

            // Verify that context is restored on the current thread:
            assertEquals(55906, ZipCode.get());
            assertEquals("[5]", ListContext.asString());
            assertEquals(7, Thread.currentThread().getPriority());
            assertEquals(ts0, Timestamp.get());

            // Run the supplier on another thread
            CompletableFuture<Object[]> future = CompletableFuture.supplyAsync(contextualSupplier);
            results = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            // Application context was configured to be left unchanged, so the java:app name must not be found:
            assertTrue(results[0].toString(), results[0] instanceof NamingException);

            // Zip code context was configured to be propagated
            assertEquals(Integer.valueOf(55901), results[1]);

            // List context was configured to be propagated
            assertEquals("[10, 28, 46, 46]", results[2]);

            // Priority context was configured to be cleared
            assertEquals(Integer.valueOf(5), results[3]);

            // Timestamp context was implicitly configured to be cleared
            assertNull(results[4]);
        } finally {
            // Remove fake context
            Timestamp.clear();
            ZipCode.clear();
            ListContext.clear();
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
    }

    /**
     * Verify that it is possible to obtain the nested ContextService of a ManagedExecutorService
     * that is configured in server.xml, and that when withContextCapture is invoked on this ContextService,
     * the resulting CompletableFuture is backed by the ManagedExecutorService, subject to its concurrency
     * constraints, and runs tasks under the context propagation settings of its nested ContextService.
     */
    @Test
    public void testGetContextService1WithContextCapture() throws Exception {
        ContextService contextSvc = executor1.getContextService();

        CompletableFuture<String> stage1 = new CompletableFuture<String>();

        CompletableFuture<String> stage1copy = contextSvc.withContextCapture(stage1);

        // block the managed executor's only thread
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch blocking = new CountDownLatch(1);
        try {
            CompletableFuture<Object> stage2a = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    blocking.countDown();
                    if (blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                        return InitialContext.doLookup(jndiName);
                    else
                        return "timed out";
                } catch (InterruptedException | NamingException x) {
                    throw new CompletionException(x);
                }
            });
            stage1.complete("java:comp/env/concurrent/executor3Ref");
            assertTrue(blocking.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // fill the managed executor's only queue slot
            CompletableFuture<Object> stage2b = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    return InitialContext.doLookup(jndiName);
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });

            // attempt to exceed the managed executor's maximum queue size
            CompletableFuture<String> stage2c = stage1copy.thenApplyAsync(s -> s);
            try {
                String result = stage2c.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                fail("Should not be able to queue another completion stage " + stage2c + ", with result: " + result);
            } catch (ExecutionException x) {
                if (x.getCause() instanceof RejectedExecutionException)
                    ; // expected
                else
                    throw x;
            }

            blocker.countDown();

            Object result;
            assertNotNull(result = stage2a.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2b.join());
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);
        } finally {
            blocker.countDown();
        }
    }

    /**
     * TODO write more of this test later. For now, just verify that we can look up the resource.
     */
    @Test
    public void testManagedExecutorDefinition() throws Exception {
        assertNotNull(InitialContext.doLookup("java:module/concurrent/executor5"));
    }

    /**
     * TODO write more of this test later. For now, just verify that we can inject the resource.
     */
    @Test
    public void testManagedScheduledExecutorDefinition() throws Exception {
        assertNotNull(executor6);
    }

    /**
     * Verify that a ManagedThreadFactory that is injected from a ManagedThreadFactoryDefinition
     * creates threads that run with the configured priority and with the configured thread context
     * that makes it possible to look up resource references in the application component's namespace.
     */
    @Test
    public void testManagedThreadFactoryDefinition() throws Exception {
        assertNotNull(lowPriorityThreads);

        int priority = Thread.currentThread().getPriority();

        ForkJoinPool pool = new ForkJoinPool(2, lowPriorityThreads, null, false);
        try {
            ForkJoinTask<Long> task = pool.submit(new Factorial(5)
                            .assertAvailable("java:comp/env/concurrent/executor3Ref")
                            .assertPriority(3));

            assertEquals(Long.valueOf(120), task.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            pool.shutdown();
        }

        assertEquals(priority, Thread.currentThread().getPriority());

        Thread managedThread = lowPriorityThreads.newThread(() -> {
        });
        assertEquals(3, managedThread.getPriority());
        assertTrue(managedThread.getClass().getName(), managedThread instanceof ManageableThread);
    }

    /**
     * Verify that it is possible to use nested ContextService without ever having obtained the
     * managed executor that it is nested under, and that is possible to use the withContextCapture
     * methods which create completion stages that are backed by that managed executor.
     * Verify that the completion stages run on the managed executor, subject to its concurrency
     * constraints, and runs tasks under the context propagation settings of its nested ContextService.
     */
    @Test
    public void testNestedContextService2WithContextCapture() throws Exception {
        CompletableFuture<String> stage1 = new CompletableFuture<String>();

        CompletableFuture<String> stage1copy = contextSvc2.withContextCapture(stage1);

        // block the managed executor's 2 threads
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch blocking = new CountDownLatch(2);
        try {
            CompletableFuture<Object> stage2a = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    blocking.countDown();
                    if (blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                        return InitialContext.doLookup(jndiName);
                    else
                        return "timed out";
                } catch (InterruptedException | NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2b = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    blocking.countDown();
                    if (blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                        return InitialContext.doLookup(jndiName);
                    else
                        return "timed out";
                } catch (InterruptedException | NamingException x) {
                    throw new CompletionException(x);
                }
            });
            stage1.complete("java:comp/env/concurrent/executor3Ref");
            assertTrue(blocking.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // fill the managed executor's 2 queue slots
            CompletableFuture<Object> stage2c = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    return InitialContext.doLookup(jndiName);
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2d = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    return InitialContext.doLookup(jndiName);
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });

            // attempt to exceed the managed executor's maximum queue size
            CompletableFuture<String> stage2e = stage1copy.thenApplyAsync(s -> s);
            try {
                String result = stage2e.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                fail("Should not be able to queue another completion stage " + stage2e + ", with result: " + result);
            } catch (ExecutionException x) {
                if (x.getCause() instanceof RejectedExecutionException)
                    ; // expected
                else
                    throw x;
            }

            blocker.countDown();

            Object result;
            assertNotNull(result = stage2a.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2b.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2c.join());
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2d.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);
        } finally {
            blocker.countDown();
        }
    }

    /**
     * Schedule a one-shot timer with a ZonedTrigger that implements only getZoneId and the
     * getNextRunTime method that accepts a ZonedDateTime. Record the LastExecution and ensure
     * that the methods which specify a ZoneId are working and return times that are consistent
     * with what the ZonedTrigger asks for.
     */
    @Test
    public void testOneShotTimerWithZonedTrigger() throws Exception {
        final AtomicReference<LastExecution> lastExecRef = new AtomicReference<LastExecution>();
        final AtomicReference<ZonedDateTime> scheduledAtRef = new AtomicReference<ZonedDateTime>();
        final ZoneId USCentral = ZoneId.of("America/Chicago");
        final ZoneId USMountain = ZoneId.of("America/Denver");
        final ZoneId NewZealand = ZoneId.of("Pacific/Auckland");
        final long TOLERANCE_NS = TimeUnit.MILLISECONDS.toNanos(500);

        ZonedDateTime beforeScheduled = ZonedDateTime.now(USCentral);
        ScheduledFuture<Integer> future = executor4.schedule(() -> 400, new ZonedTrigger() {
            @Override
            public ZonedDateTime getNextRunTime(LastExecution lastExecution, ZonedDateTime scheduledAt) {
                if (lastExecution == null)
                    return scheduledAt.plusSeconds(4);
                lastExecRef.set(lastExecution);
                scheduledAtRef.set(scheduledAt);
                return null;
            }

            @Override
            public ZoneId getZoneId() {
                return USCentral;
            }
        });
        try {
            ZonedDateTime afterScheduled = ZonedDateTime.now(USCentral);

            assertEquals(Integer.valueOf(400), future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());

            // Is the scheduledAt time within the range of when we actually scheduled it?
            ZonedDateTime scheduledAt = scheduledAtRef.get();
            assertEquals(USCentral, scheduledAt.getZone()); // must supply scheduledAt time in same zone
            assertTrue(beforeScheduled + " must be less or equal to " + scheduledAt,
                       beforeScheduled.minusNanos(TOLERANCE_NS).isBefore(scheduledAt));
            assertTrue(afterScheduled + " must be greater or equal to " + scheduledAt,
                       afterScheduled.plusNanos(TOLERANCE_NS).isAfter(scheduledAt));

            // Does the target start time of the last execution match what the trigger asked for?
            LastExecution lastExec = lastExecRef.get();
            ZonedDateTime targetStartAt = lastExec.getScheduledStart(USCentral);
            assertEquals(USCentral, targetStartAt.getZone());
            ZonedDateTime targetStartAtExpected = scheduledAt.plusSeconds(4);
            assertTrue(targetStartAt + " must be equal to " + targetStartAtExpected,
                       targetStartAt.isAfter(targetStartAtExpected.minusNanos(TOLERANCE_NS)) &&
                                                                                     targetStartAt.isBefore(targetStartAtExpected.plusNanos(TOLERANCE_NS)));

            // Is the actual start time after (or equal to) the expected?
            ZonedDateTime startAt = lastExec.getRunStart(USMountain);
            assertEquals(USMountain, startAt.getZone());
            assertTrue(startAt + " must be greater or equal to " + targetStartAt,
                       startAt.isAfter(targetStartAt.minusNanos(TOLERANCE_NS)));

            // Is the actual end time after (or equal to) the actual start time?
            ZonedDateTime endAt = lastExec.getRunEnd(NewZealand);
            assertEquals(NewZealand, endAt.getZone());
            assertTrue(endAt + " must be greater or equal to " + startAt,
                       endAt.isAfter(startAt.minusNanos(TOLERANCE_NS)));
        } finally {
            if (!future.isDone())
                future.cancel(true);
        }
    }

    /**
     * Verify that a parallel stream can run on a ForkJoinPool that uses a ManagedThreadFactory
     * to create its ForkJoinWorkerThreads, and that those threads run with the application
     * component context of the the application that looked up or injected the ManagedThreadFactory.
     * Verify this by attempting a resource reference lookup from the parallel stream operations.
     */
    @Test
    public void testParallelStreamRunsOnManagedThreadFactory() throws Exception {
        String curThreadName = Thread.currentThread().getName();
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();

        ForkJoinPool pool = new ForkJoinPool(3, forkJoinThreadFactory, null, false);
        try {
            pool.submit(() -> {
                Arrays.asList(1, 2, 3).parallelStream().forEach(i -> {
                    try {
                        // Perform a resource reference lookup to demonstrate that the
                        // application component's context is established on the ForkJoinWorkerThread,
                        Object lookedUp = InitialContext.doLookup("java:module/env/concurrent/threadFactoryRef");
                        results.add(Thread.currentThread().getName() + " (" + i + ") " + lookedUp);
                    } catch (NamingException x) {
                        results.add(x);
                    }
                });
            });

            Object result;
            assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (result instanceof Exception) {
                throw new AssertionError("Failure on parallel stream thread", (Exception) result);
            } else {
                String s = (String) result;
                assertTrue("Current: " + curThreadName + " vs " + s, !s.startsWith(curThreadName));
                assertTrue(s, s.contains(" (1) ") || s.contains(" (2) ") || s.contains(" (3) "));
            }

            assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (result instanceof Exception) {
                throw new AssertionError("Failure on parallel stream thread", (Exception) result);
            } else {
                String s = (String) result;
                assertTrue("Current: " + curThreadName + " vs " + s, !s.startsWith(curThreadName));
                assertTrue(s, s.contains(" (1) ") || s.contains(" (2) ") || s.contains(" (3) "));
            }

            assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (result instanceof Exception) {
                throw new AssertionError("Failure on parallel stream thread", (Exception) result);
            } else {
                String s = (String) result;
                assertTrue("Current: " + curThreadName + " vs " + s, !s.startsWith(curThreadName));
                assertTrue(s, s.contains(" (1) ") || s.contains(" (2) ") || s.contains(" (3) "));
            }
        } finally {
            pool.shutdown();
        }
    }

    /**
     * Verify that it is possible to obtain a ContextService that is referenced by
     * multiple managed executors, and that is possible to use the withContextCapture methods
     * which create completion stages that are backed by the respective managed executor.
     * Verify that the completion stages run on the managed executor, subject to its concurrency
     * constraints, and runs tasks under the context propagation settings of the ContextService.
     * Part 1 - This test covers usage of the managedScheduledExecutorService concurrent/executor3.
     */
    @Test
    public void testReferencedContextServiceWithContextCapture3() throws Exception {
        CompletableFuture<String> stage1 = new CompletableFuture<String>();

        ContextService contextSvc3 = executor3.getContextService();

        CompletableFuture<String> stage1copy = contextSvc3.withContextCapture(stage1);

        // block the managed executor's 3 threads
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch blocking = new CountDownLatch(3);
        try {
            CompletableFuture<Object> stage2a = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    blocking.countDown();
                    if (blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                        return InitialContext.doLookup(jndiName);
                    else
                        return "timed out";
                } catch (InterruptedException | NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2b = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    blocking.countDown();
                    if (blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                        return InitialContext.doLookup(jndiName);
                    else
                        return "timed out";
                } catch (InterruptedException | NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2c = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    blocking.countDown();
                    if (blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                        return InitialContext.doLookup(jndiName);
                    else
                        return "timed out";
                } catch (InterruptedException | NamingException x) {
                    throw new CompletionException(x);
                }
            });
            stage1.complete("java:comp/env/concurrent/executor3Ref");
            assertTrue(blocking.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // fill the managed executor's 3 queue slots
            CompletableFuture<Object> stage2d = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    return InitialContext.doLookup(jndiName);
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2e = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    return InitialContext.doLookup(jndiName);
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2f = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    return InitialContext.doLookup(jndiName);
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });

            // attempt to exceed the managed executor's maximum queue size
            CompletableFuture<String> stage2g = stage1copy.thenApplyAsync(s -> s);
            try {
                String result = stage2g.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                fail("Should not be able to queue another completion stage " + stage2g + ", with result: " + result);
            } catch (ExecutionException x) {
                if (x.getCause() instanceof RejectedExecutionException)
                    ; // expected
                else
                    throw x;
            }

            blocker.countDown();

            Object result;
            assertNotNull(result = stage2a.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2b.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2c.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2d.join());
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2e.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2f.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);
        } finally {
            blocker.countDown();
        }
    }

    /**
     * Verify that it is possible to obtain a ContextService that is referenced by
     * multiple managed executors, and that is possible to use the withContextCapture methods
     * which create completion stages that are backed by the respective managed executor.
     * Verify that the completion stages run on the managed executor, subject to its concurrency
     * constraints, and runs tasks under the context propagation settings of the ContextService.
     * Part 2 - This test covers usage of the managedScheduledExecutorService concurrent/executor4.
     */
    @Test
    public void testReferencedContextServiceWithContextCapture4() throws Exception {
        CompletableFuture<String> stage1 = new CompletableFuture<String>();

        ContextService contextSvc4 = executor4.getContextService();

        CompletableFuture<String> stage1copy = contextSvc4.withContextCapture(stage1);

        // block the managed executor's 4 threads
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch blocking = new CountDownLatch(4);
        try {
            CompletableFuture<Object> stage2a = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    blocking.countDown();
                    if (blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                        return InitialContext.doLookup(jndiName);
                    else
                        return "timed out";
                } catch (InterruptedException | NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2b = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    blocking.countDown();
                    if (blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                        return InitialContext.doLookup(jndiName);
                    else
                        return "timed out";
                } catch (InterruptedException | NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2c = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    blocking.countDown();
                    if (blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                        return InitialContext.doLookup(jndiName);
                    else
                        return "timed out";
                } catch (InterruptedException | NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2d = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    blocking.countDown();
                    if (blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                        return InitialContext.doLookup(jndiName);
                    else
                        return "timed out";
                } catch (InterruptedException | NamingException x) {
                    throw new CompletionException(x);
                }
            });

            stage1.complete("java:comp/env/concurrent/executor3Ref");
            assertTrue(blocking.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // fill the managed executor's 4 queue slots
            CompletableFuture<Object> stage2e = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    return InitialContext.doLookup(jndiName);
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2f = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    return InitialContext.doLookup(jndiName);
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2g = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    return InitialContext.doLookup(jndiName);
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });
            CompletableFuture<Object> stage2h = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    return InitialContext.doLookup(jndiName);
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });

            // attempt to exceed the managed executor's maximum queue size
            CompletableFuture<String> stage2i = stage1copy.thenApplyAsync(s -> s);
            try {
                String result = stage2i.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                fail("Should not be able to queue another completion stage " + stage2i + ", with result: " + result);
            } catch (ExecutionException x) {
                if (x.getCause() instanceof RejectedExecutionException)
                    ; // expected
                else
                    throw x;
            }

            blocker.countDown();

            Object result;
            assertNotNull(result = stage2a.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2b.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2c.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2d.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2e.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2f.join());
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2g.get());
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertNotNull(result = stage2h.join());
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);
        } finally {
            blocker.countDown();
        }
    }
}
