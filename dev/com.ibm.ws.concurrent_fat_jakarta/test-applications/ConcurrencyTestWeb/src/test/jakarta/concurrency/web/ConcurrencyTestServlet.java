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
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
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
                           context = "java:module/concurrent/ZLContextSvc",
                           hungTaskThreshold = 300000,
                           maxAsync = 1)
@ManagedScheduledExecutorDefinition(name = "java:comp/concurrent/executor6",
                                    context = "java:app/concurrent/appContextSvc",
                                    hungTaskThreshold = 360000,
                                    maxAsync = 2)
@ManagedThreadFactoryDefinition(name = "java:app/concurrent/lowPriorityThreads",
                                context = "java:app/concurrent/appContextSvc",
                                priority = 3)
// TODO add resource definitions with context unspecified

// TODO remove the following once we can enable them in application.xml
@ContextServiceDefinition(name="java:app/concurrent/dd/ZPContextService",
                          cleared = ListContext.CONTEXT_NAME,
                          propagated = { ZipCode.CONTEXT_NAME, "Priority" },
                          unchanged = APPLICATION)
@ManagedExecutorDefinition(name = "java:app/concurrent/dd/ZPExecutor",
                           context = "java:app/concurrent/dd/ZPContextService",
                           hungTaskThreshold = 420000,
                           maxAsync = 2)
@ManagedScheduledExecutorDefinition(name = "java:global/concurrent/dd/ScheduledExecutor",
                                    context = "java:comp/DefaultContextService",
                                    hungTaskThreshold = 410000,
                                    maxAsync = 1)
@ManagedThreadFactoryDefinition(name = "java:app/concurrent/dd/ThreadFactory",
                                context = "java:app/concurrent/appContextSvc",
                                priority = 4)
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
     * Verify that a ManagedExecutorService that is injected from a ManagedExecutorDefinition
     * abides by the configured maxAsync and the configured thread context propagation/clearing
     * that makes it possible to access third-party context from async completion stage actions.
     */
    @Test
    public void testManagedExecutorDefinitionAnno() throws Throwable {
        ManagedExecutorService executor5 = InitialContext.doLookup("java:module/concurrent/executor5");

        CompletableFuture<Exchanger<String>> stage0 = executor5.completedFuture(new Exchanger<String>());

        CompletableFuture<Object[]> stage1a, stage1b, stage1c;

        // Async completion stage action will attempt an exchange (which shouldn't be possible
        // due to maxAsync of 1) and record the thread context under which it runs:
        Function<Exchanger<String>, Object[]> fn = exchanger -> {
            Object[] results = new Object[6];
            try {
                results[0] = exchanger.exchange("maxAsync=1 was not enforced", 1, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException x) {
                results[0] = x;
            }
            results[1] = Timestamp.get(); // should be cleared
            results[2] = ZipCode.get(); // should be propagated
            results[3] = ListContext.asString(); // should be propagated
            results[4] = Thread.currentThread().getPriority(); // should be cleared
            try {
                // Application context should not be applied to pooled thread, causing the lookup to fail
                results[5] = InitialContext.doLookup("java:module/concurrent/executor5");
            } catch (NamingException x) {
                results[5] = x;
            }
            return results;
        };

        // Put some fake context onto the thread:
        Timestamp.set();
        ZipCode.set(55902);
        ListContext.newList();
        ListContext.add(33);
        ListContext.add(56);
        ListContext.add(65);
        Thread.currentThread().setPriority(4);
        try {
            // request async completion stages with above context,
            stage1a = stage0.thenApplyAsync(fn);
            stage1b = stage0.thenApplyAsync(fn);
            // alter context slightly and request another async completion stage,
            ZipCode.set(55904);
            stage1c = stage0.thenApplyAsync(fn);
        } finally {
            // Remove fake context
            Timestamp.clear();
            ZipCode.clear();
            ListContext.clear();
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }

        Object[] results = stage1a.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        if (!(results[0] instanceof TimeoutException)) // must time out due to enforcement of maxAsync=1
            if (results[0] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[0]);
            else
                throw new AssertionError(results[0]);
        assertNull(results[1]); // must be cleared
        assertEquals(Integer.valueOf(55902), results[2]); // must be propagated
        assertEquals("[33, 56, 65]", results[3]); // must be propagated
        assertEquals(Integer.valueOf(Thread.NORM_PRIORITY), results[4]); // must be cleared
        if (!(results[5] instanceof NamingException)) // must be unchanged (not present on async thread)
            if (results[5] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[5]);
            else
                throw new AssertionError(results[5]);

        results = stage1b.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        if (!(results[0] instanceof TimeoutException)) // must time out due to enforcement of maxAsync=1
            if (results[0] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[0]);
            else
                throw new AssertionError(results[0]);
        assertNull(results[1]); // must be cleared
        assertEquals(Integer.valueOf(55902), results[2]); // must be propagated
        assertEquals("[33, 56, 65]", results[3]); // must be propagated
        assertEquals(Integer.valueOf(Thread.NORM_PRIORITY), results[4]); // must be cleared
        if (!(results[5] instanceof NamingException)) // must be unchanged (not present on async thread)
            if (results[5] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[5]);
            else
                throw new AssertionError(results[5]);

        results = stage1c.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        if (!(results[0] instanceof TimeoutException)) // must time out due to enforcement of maxAsync=1
            if (results[0] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[0]);
            else
                throw new AssertionError(results[0]);
        assertNull(results[1]); // must be cleared
        assertEquals(Integer.valueOf(55904), results[2]); // must be propagated
        assertEquals("[33, 56, 65]", results[3]); // must be propagated
        assertEquals(Integer.valueOf(Thread.NORM_PRIORITY), results[4]); // must be cleared
        if (!(results[5] instanceof NamingException)) // must be unchanged (not present on async thread)
            if (results[5] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[5]);
            else
                throw new AssertionError(results[5]);
    }

    /**
     * Verify that a ManagedExecutorService that is defined in application.xml
     * abides by the configured maxAsync and the configured thread context propagation/clearing
     * that makes it possible to access third-party context from async completion stage actions.
     */
    @Test
    public void testManagedExecutorDefinitionAppDD() throws Throwable {
        ManagedExecutorService executor = InitialContext.doLookup("java:app/concurrent/dd/ZPExecutor");

        CountDownLatch blocker = new CountDownLatch(1);
        final TransferQueue<CountDownLatch> queue = new LinkedTransferQueue<CountDownLatch>();

        Future<Object[]> future1, future2, future3, future4;

        // This async task polls the transfer queue for a latch to block on.
        // This allows the caller to use up the maxAsync (which is 2) and then attempt additional
        // transfers to test whether additional async requests can run in parallel.
        Callable<Object[]> task = () -> {
            Object[] results = new Object[6];
            results[0] = queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            results[1] = Timestamp.get(); // should be cleared
            results[2] = ZipCode.get(); // should be propagated
            results[3] = ListContext.asString(); // should be cleared
            results[4] = Thread.currentThread().getPriority(); // should be propagated
            try {
                results[5] = InitialContext.doLookup("java:app/concurrent/dd/ZPExecutor");
            } catch (NamingException x) {
                // expected, due to unchanged Application context on executor thread
                results[5] = x;
            }
            return results;
        };

        // Put some fake context onto the thread:
        Timestamp.set();
        ZipCode.set(55901);
        ListContext.newList();
        ListContext.add(25);
        Thread.currentThread().setPriority(6);
        try {
            // submit async task with the above context,
            future1 = executor.submit(task);
            // alter context slightly and submit more tasks,
            ZipCode.set(55902);
            future2 = executor.submit(task);
            future3 = executor.submit(task);
            future4 = executor.submit(task);
        } finally {
            // Remove fake context
            Timestamp.clear();
            ZipCode.clear();
            ListContext.clear();
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }

        // With maxAsync=2, there should be 2 async completion stage actions running to accept transfers:
        assertTrue(queue.tryTransfer(blocker, TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(queue.tryTransfer(blocker, TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Additional transfers should not be possible
        assertFalse(queue.tryTransfer(blocker, 1, TimeUnit.SECONDS));

        // Allow completion stage actions to finish:
        blocker.countDown();

        // The remaining completion stage actions can start now:
        assertTrue(queue.tryTransfer(blocker, TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(queue.tryTransfer(blocker, TIMEOUT_NS, TimeUnit.NANOSECONDS));

        Object[] results = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(Boolean.TRUE, results[0]);
        assertNull(results[1]); // Timestamp context must be cleared
        assertEquals(Integer.valueOf(55901), results[2]); // must be propagated
        assertEquals("[]", results[3]); // List context must be cleared
        assertEquals(Integer.valueOf(6), results[4]); // must be propagated
        if (!(results[5] instanceof NamingException)) // must be unchanged (not present on async thread)
            if (results[5] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[5]);
            else
                throw new AssertionError(results[5]);

        results = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(Boolean.TRUE, results[0]);
        assertNull(results[1]); // Timestamp context must be cleared
        assertEquals(Integer.valueOf(55902), results[2]); // must be propagated
        assertEquals("[]", results[3]); // List context must be cleared
        assertEquals(Integer.valueOf(6), results[4]); // must be propagated
        if (!(results[5] instanceof NamingException)) // must be unchanged (not present on async thread)
            if (results[5] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[5]);
            else
                throw new AssertionError(results[5]);

        results = future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(Boolean.TRUE, results[0]);
        assertNull(results[1]); // Timestamp context must be cleared
        assertEquals(Integer.valueOf(55902), results[2]); // must be propagated
        assertEquals("[]", results[3]); // List context must be cleared
        assertEquals(Integer.valueOf(6), results[4]); // must be propagated
        if (!(results[5] instanceof NamingException)) // must be unchanged (not present on async thread)
            if (results[5] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[5]);

        results = future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(Boolean.TRUE, results[0]);
        assertNull(results[1]); // Timestamp context must be cleared
        assertEquals(Integer.valueOf(55902), results[2]); // must be propagated
        assertEquals("[]", results[3]); // List context must be cleared
        assertEquals(Integer.valueOf(6), results[4]); // must be propagated
        if (!(results[5] instanceof NamingException)) // must be unchanged (not present on async thread)
            if (results[5] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[5]);
    }

    /**
     * Verify that a ManagedScheduledExecutorService that is injected from a ManagedScheduledExecutorDefinition
     * abides by the configured maxAsync and the configured thread context propagation that
     * makes it possible to look up resource references in the application component's namespace.
     */
    @Test
    public void testManagedScheduledExecutorDefinitionAnno() throws Exception {
        assertNotNull(executor6);

        CountDownLatch blocker = new CountDownLatch(1);
        TransferQueue<CountDownLatch> queue = new LinkedTransferQueue<CountDownLatch>();
        CompletableFuture<TransferQueue<CountDownLatch>> stage0 = executor6.completedFuture(queue);

        CompletableFuture<Object> stage1a, stage1b, stage1c, stage1d;

        // This async completion stage action polls the transfer queue for a latch to block on.
        // This allows the caller to use up the maxAsync (which is 2) and then attempt additional
        // transfers to test whether additional async requests can run in parallel.
        Function<TransferQueue<CountDownLatch>, Object> fn = q -> {
            try {
                if (q.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                    return InitialContext.doLookup("java:comp/concurrent/executor6"); // requires Application context
                else
                    return false;
            } catch (InterruptedException | NamingException x) {
                throw new CompletionException(x);
            }
        };

        stage1a = stage0.thenApplyAsync(fn);
        stage1b = stage0.thenApplyAsync(fn);
        stage1c = stage0.thenApplyAsync(fn);
        stage1d = stage0.thenApplyAsync(fn);

        // With maxAsync=2, there should be 2 async completion stage actions running to accept transfers:
        assertTrue(queue.tryTransfer(blocker, TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(queue.tryTransfer(blocker, TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Additional transfers should not be possible
        assertFalse(queue.tryTransfer(blocker, 1, TimeUnit.SECONDS));

        // Allow completion stage actions to finish:
        blocker.countDown();

        // The remaining completion stage actions can start now:
        assertTrue(queue.tryTransfer(blocker, TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(queue.tryTransfer(blocker, TIMEOUT_NS, TimeUnit.NANOSECONDS));

        Object result;
        assertNotNull(result = stage1a.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService); // successful lookup on completion stage thread

        assertNotNull(result = stage1b.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService); // successful lookup on completion stage thread

        assertNotNull(result = stage1c.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService); // successful lookup on completion stage thread

        assertNotNull(result = stage1d.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService); // successful lookup on completion stage thread
    }

    /**
     * Verify that a ManagedScheduledExecutorService that is defined in application.xml
     * abides by the configured maxAsync and the configured thread context propagation that
     * makes it possible to look up resource references in the application component's namespace.
     */
    @Test
    public void testManagedScheduledExecutorDefinitionAppDD() throws Throwable {
        ManagedExecutorService executor = InitialContext.doLookup("java:global/concurrent/dd/ScheduledExecutor");

        final Exchanger<String> exchanger = new Exchanger<String>();

        Future<Object[]> future1, future2, future3;

        // Async task that attempts an exchange (which shouldn't be possible
        // due to maxAsync of 1) and records the thread context under which it runs:
        Callable<Object[]> task = () -> {
            Object[] results = new Object[6];
            try {
                results[0] = exchanger.exchange("maxAsync=1 was not enforced", 1, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException x) {
                results[0] = x;
            }
            results[1] = Timestamp.get();
            results[2] = ZipCode.get();
            results[3] = ListContext.asString();
            results[4] = Thread.currentThread().getPriority();
            results[5] = InitialContext.doLookup("java:app/concurrent/appContextSvc"); // Application context is propagated
            return results;
        };

        // Put some fake context onto the thread:
        Timestamp.set();
        Long timestamp = Timestamp.get();
        ZipCode.set(55901);
        ListContext.newList();
        ListContext.add(20);
        Thread.currentThread().setPriority(7);
        try {
            future1 = executor.submit(task);
            future2 = executor.submit(task);
            future3 = executor.submit(task);
        } finally {
            // Remove fake context
            Timestamp.clear();
            ZipCode.clear();
            ListContext.clear();
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }

        Object[] results = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        if (!(results[0] instanceof TimeoutException)) // must time out due to enforcement of maxAsync=1
            if (results[0] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[0]);
            else
                throw new AssertionError(results[0]);
        assertEquals(timestamp, results[1]); // must be propagated
        assertEquals(Integer.valueOf(55901), results[2]); // must be propagated
        assertEquals("[20]", results[3]); // must be propagated
        assertEquals(Integer.valueOf(7), results[4]); // must be propagated
        if (results[5] instanceof Throwable)
            throw new AssertionError().initCause((Throwable) results[5]);
        else
            assertNotNull(results[5]);


        results = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        if (!(results[0] instanceof TimeoutException)) // must time out due to enforcement of maxAsync=1
            if (results[0] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[0]);
            else
                throw new AssertionError(results[0]);
        assertEquals(timestamp, results[1]); // must be propagated
        assertEquals(Integer.valueOf(55901), results[2]); // must be propagated
        assertEquals("[20]", results[3]); // must be propagated
        assertEquals(Integer.valueOf(7), results[4]); // must be propagated
        if (results[5] instanceof Throwable)
            throw new AssertionError().initCause((Throwable) results[5]);
        else
            assertNotNull(results[5]);


        results = future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        if (!(results[0] instanceof TimeoutException)) // must time out due to enforcement of maxAsync=1
            if (results[0] instanceof Throwable)
                throw new AssertionError().initCause((Throwable) results[0]);
            else
                throw new AssertionError(results[0]);
        assertEquals(timestamp, results[1]); // must be propagated
        assertEquals(Integer.valueOf(55901), results[2]); // must be propagated
        assertEquals("[20]", results[3]); // must be propagated
        assertEquals(Integer.valueOf(7), results[4]); // must be propagated
        if (results[5] instanceof Throwable)
            throw new AssertionError().initCause((Throwable) results[5]);
        else
            assertNotNull(results[5]);
    }

    /**
     * Verify that a ManagedThreadFactory that is injected from a ManagedThreadFactoryDefinition
     * creates threads that run with the configured priority and with the configured thread context
     * that makes it possible to look up resource references in the application component's namespace.
     */
    @Test
    public void testManagedThreadFactoryDefinitionAnno() throws Exception {
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
     * Verify that a ManagedThreadFactory that is defined in application.xml
     * creates threads that run with the configured priority and with the configured thread context
     * that makes it possible to look up resource references in the application component's namespace.
     */
    @Test
    public void testManagedThreadFactoryDefinitionAppDD() throws Throwable {
        ManagedThreadFactory threadFactory = InitialContext.doLookup("java:app/concurrent/dd/ThreadFactory");

        final LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        threadFactory.newThread(() -> {
            results.add(Thread.currentThread().getName());
            results.add(Thread.currentThread().getPriority());
            try {
                results.add(InitialContext.doLookup("java:app/concurrent/dd/ThreadFactory"));
            } catch (Throwable x) {
                results.add(x);
            }
        }).start();

        Object result;
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(result.toString(), !Thread.currentThread().getName().equals(result));

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Integer.valueOf(4), result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError().initCause((Throwable) result);
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
