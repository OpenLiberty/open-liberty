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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

import componenttest.app.FATServlet;

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

    // TODO @Resource(lookup = "java:comp/DefaultManagedThreadFactory")
    ForkJoinWorkerThreadFactory forkJoinThreadFactory;

    @Override
    public void init() throws ServletException {
        // TODO remove this temporary code once the 3.0 ManagedThreadFactory class
        // is available which implements ForkJoinWorkerThreadFactory
        forkJoinThreadFactory = new ForkJoinWorkerThreadFactory() {
            @Override
            public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                try {
                    java.lang.reflect.Method newThread = defaultThreadFactory.getClass()
                                    .getMethod("newThread", ForkJoinPool.class);
                    newThread.setAccessible(true);
                    return (ForkJoinWorkerThread) newThread.invoke(defaultThreadFactory, pool);
                } catch (Exception x) {
                    throw new RuntimeException(x);
                }
            }
        };
    }

    /**
     * Verify that it is possible to obtain the nested ContextService of a ManagedExecutorService
     * that is configured in server.xml, and that when withContextCapture is invoked on this ContextService,
     * the resulting CompletableFuture is backed by the ManagedExecutorService, subject to its concurrency
     * constraints, and runs tasks under the context propagation settings of its nested ContextService.
     */
    @Test
    public void testGetContextService1WithContextCapture() throws Exception {
        // TODO ContextService contextSvc = executor1.getContextService();
        ContextService contextSvc = (ContextService) executor1.getClass().getMethod("getContextService").invoke(executor1);

        CompletableFuture<String> stage1 = new CompletableFuture<String>();

        // TODO CompletableFuture<String> stage1copy = contextSvc.withContextCapture(stage1);
        CompletableFuture<String> stage1copy = (CompletableFuture<String>)
                        contextSvc.getClass().getMethod("withContextCapture", CompletableFuture.class)
                                             .invoke(contextSvc, stage1);

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
     * Verify that it is possible to use nested ContextService without ever having obtained the
     * managed executor that it is nested under, and that is possible to use the withContextCapture
     * methods which create completion stages that are backed by that managed executor.
     * Verify that the completion stages run on the managed executor, subject to its concurrency
     * constraints, and runs tasks under the context propagation settings of its nested ContextService.
     */
    @Test
    public void testNestedContextService2WithContextCapture() throws Exception {
        CompletableFuture<String> stage1 = new CompletableFuture<String>();

        // TODO CompletableFuture<String> stage1copy = contextSvc2.withContextCapture(stage1);
        CompletableFuture<String> stage1copy = (CompletableFuture<String>)
                        contextSvc2.getClass().getMethod("withContextCapture", CompletableFuture.class)
                                              .invoke(contextSvc2, stage1);

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
}
