/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.LastExecution;
import jakarta.enterprise.concurrent.ManageableThread;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import jakarta.enterprise.concurrent.SkippedException;
import jakarta.enterprise.concurrent.ZonedTrigger;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.context.list.ListContext;
import test.context.location.ZipCode;
import test.context.timing.Timestamp;
import test.jakarta.concurrency.ejb.MTFDBean;

@ContextServiceDefinition(name = "java:app/concurrent/appContextSvc",
                          propagated = APPLICATION,
                          cleared = { TRANSACTION, SECURITY },
                          unchanged = ALL_REMAINING)
@ContextServiceDefinition(name = "java:module/concurrent/clearRemainingContextSvc",
                          propagated = { APPLICATION, "Bogus" },
                          cleared = ALL_REMAINING,
                          unchanged = { Timestamp.CONTEXT_NAME, "Priority", TRANSACTION })
@ContextServiceDefinition(name = "java:comp/concurrent/propagateRemainingContextSvc",
                          propagated = ALL_REMAINING,
                          cleared = { "Priority", Timestamp.CONTEXT_NAME },
                          unchanged = { ListContext.CONTEXT_NAME, TRANSACTION, ZipCode.CONTEXT_NAME })
@ContextServiceDefinition(name = "java:module/concurrent/ZLContextSvc",
                          propagated = { ZipCode.CONTEXT_NAME, ListContext.CONTEXT_NAME },
                          cleared = "Priority",
                          unchanged = { APPLICATION, TRANSACTION })
@ContextServiceDefinition(name = "java:comp/concurrent/ThirdPartyContextService",
                          // defaults to all other context types propagated
                          cleared = { TRANSACTION, SECURITY })
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
@ManagedExecutorDefinition(name = "java:global/concurrent/executor7",
                           maxAsync = 3)

// Merged with web.xml
@ContextServiceDefinition(name = "java:app/concurrent/merged/web/LTContextService",
                          cleared = "Priority", // web.xml replaces with {}
                          propagated = { ListContext.CONTEXT_NAME, Timestamp.CONTEXT_NAME },
                          unchanged = ZipCode.CONTEXT_NAME) // web.xml replaces with Remaining

@ContextServiceDefinition(name = "java:comp/concurrent/merged/web/PTContextService",
                          cleared = ListContext.CONTEXT_NAME,
                          propagated = ALL_REMAINING, // web.xml replaces with Priority, Timestamp
                          unchanged = { APPLICATION, ALL_REMAINING })

@ContextServiceDefinition(name = "java:module/concurrent/merged/web/ZContextService",
                          cleared = "Priority", // web.xml replaces with Transaction
                          propagated = { ListContext.CONTEXT_NAME, Timestamp.CONTEXT_NAME }, // web.xml replaces with ZipCode
                          unchanged = ALL_REMAINING)

@ManagedExecutorDefinition(name = "java:module/concurrent/merged/web/ZLExecutor",
                           context = "java:module/concurrent/ZLContextSvc",
                           maxAsync = 6, // web.xml replaces with 3
                           hungTaskThreshold = 360000)

@ManagedExecutorDefinition(name = "java:comp/concurrent/merged/web/ZPExecutor",
                           context = "java:comp/concurrent/dd/web/TZContextService", // web.xml replaces with ZPContextService
                           maxAsync = 2)

@ManagedScheduledExecutorDefinition(name = "java:module/concurrent/merged/web/ZLScheduledExecutor",
                                    context = "java:module/concurrent/ZLContextSvc",
                                    maxAsync = 7, // web.xml replaces with 1
                                    hungTaskThreshold = 170000)

@ManagedScheduledExecutorDefinition(name = "java:app/concurrent/merged/web/LPScheduledExecutor",
                                    context = "java:app/concurrent/dd/ZPContextService", // web.xml replaces with LPContextService
                                    maxAsync = 4)

@ManagedThreadFactoryDefinition(name = "java:comp/concurrent/merged/web/TZThreadFactory",
                                context = "java:module/concurrent/ZLContextSvc", // web.xml replaces with TZContextService
                                priority = 3)

@ManagedThreadFactoryDefinition(name = "java:app/concurrent/merged/web/LTThreadFactory",
                                context = "java:app/concurrent/merged/web/LTContextService",
                                priority = 4) // web.xml replaces with 8

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

    @Resource(lookup = "java:module/concurrent/executor5")
    ManagedExecutorService executor5;

    @Resource(lookup = "java:comp/concurrent/executor6")
    ManagedScheduledExecutorService executor6;

    @Resource(lookup = "java:comp/DefaultManagedThreadFactory")
    ForkJoinWorkerThreadFactory forkJoinThreadFactory;

    @Resource(lookup = "java:app/concurrent/lowPriorityThreads")
    ManagedThreadFactory lowPriorityThreads;

    //Do not use for any other tests
    @EJB
    MTFDBean testEJBAnnoManagedThreadFactoryInitializationBean;

    private ExecutorService unmanagedThreads;

    @Override
    public void destroy() {
        unmanagedThreads.shutdownNow();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        unmanagedThreads = Executors.newFixedThreadPool(5);

        // These EJBs need to be used so that their ContextServiceDefinition annotations,
        // which are relied upon by the web module and the other EJB, can be processed
        try {
            InitialContext.doLookup("java:global/ConcurrencyTestApp/ConcurrencyTestEJB/ContextServiceDefinerBean!test.jakarta.concurrency.ejb.ContextServiceDefinerBean");
            Executor bean = InitialContext.doLookup("java:global/ConcurrencyTestApp/ConcurrencyTestEJB/ExecutorBean!java.util.concurrent.Executor");
        } catch (NamingException x) {
            throw new ServletException(x);
        }
    }

    /**
     * Covers the new ManagedExecutorService API methods: completedStage and failedStage,
     * ensuring propagation of context to dependent stages.
     */
    @Test
    public void testCompletedAndFailedStages() throws Exception {
        CompletionStage<Integer> stage1 = executor5.completedStage(1);
        CompletionStage<Integer> stage2 = executor5.failedStage(new IOException("Not a real error."));
        CompletionStage<Integer> stage3 = stage2.exceptionally(failure -> failure.getClass().getSimpleName().length());
        ListContext.newList();
        try {
            CompletionStage<Void> stage4 = stage3.thenAcceptBothAsync(stage1, (r3, r1) -> ListContext.add(r3 - r1));
            LinkedBlockingQueue<String> results = new LinkedBlockingQueue<String>();
            stage4.thenRunAsync(() -> results.add(ListContext.asString()));
            ListContext.newList();
            String result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals("[10]", result); // "IOException".length() - 1
        } finally {
            ListContext.clear();
        }
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
     * and leave other types of context, such as Application, unchanged.
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

            // Alter some of the context on the current thread
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
     * Verify that the ManagedExecutorService copy(stage) and copy(CompletableFuture) methods
     * create copies that propagate thread context to dependent stages that are created from the
     * copy.
     */
    @Test
    public void testCopy() throws Exception {
        CompletableFuture<Integer> stage1 = new CompletableFuture<Integer>();
        CompletionStage<Integer> stage2 = CompletableFuture.completedStage(6);

        CompletableFuture<Integer> stage1copy = executor5.copy(stage1);
        CompletionStage<Integer> stage2copy = executor6.copy(stage2);

        try {
            ZipCode.set(55901);
            CompletableFuture<Integer> stage3 = stage1copy.thenApplyAsync(i -> ZipCode.get() + i); // 55902

            ZipCode.set(55906);
            CompletionStage<Object> stage4 = stage2copy.thenApplyAsync(i -> {
                try {
                    return InitialContext.doLookup("java:comp/concurrent/executor" + i); // executor6
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });

            ZipCode.set(55904);
            CompletableFuture<Integer> stage5 = stage3.thenCombine(stage4, (z, s) -> {
                return ZipCode.get() - (s instanceof ManagedScheduledExecutorService ? z : 0); // 55904 - 55902 = 2
            });

            ZipCode.clear();
            assertTrue(stage1.complete(1));
            assertEquals(Integer.valueOf(2), stage5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            ZipCode.clear();
        }
    }

    /**
     * Use currentContextExecutor to capture a snapshot of the current thread context and apply
     * it elsewhere at later points in time.
     */
    @Test
    public void testCurrentContextExecutor() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:module/concurrent/ZLContextSvc");

        try {
            Thread.currentThread().setPriority(7);

            ZipCode.set(55901);
            Executor nwRochesterExecutor = contextSvc.currentContextExecutor();

            ZipCode.set(55902);
            Executor swRochesterExecutor = contextSvc.currentContextExecutor();

            ZipCode.set(55906);
            Thread.currentThread().setPriority(6);
            Timestamp.set();
            Long timestamp1 = Timestamp.get();

            nwRochesterExecutor.execute(() -> {
                assertEquals(55901, ZipCode.get()); // propagated
                assertEquals(null, Timestamp.get()); // cleared
                assertEquals(Thread.NORM_PRIORITY, Thread.currentThread().getPriority()); // cleared
                try {
                    assertNotNull(InitialContext.doLookup("java:module/concurrent/ZLContextSvc")); // unchanged
                } catch (NamingException x) {
                    throw new AssertionError(x);
                }
            });

            assertEquals(55906, ZipCode.get());
            assertEquals(timestamp1, Timestamp.get());
            assertEquals(6, Thread.currentThread().getPriority());

            swRochesterExecutor.execute(() -> {
                assertEquals(55902, ZipCode.get()); // propagated
                assertEquals(null, Timestamp.get()); // cleared
                assertEquals(Thread.NORM_PRIORITY, Thread.currentThread().getPriority()); // cleared
                try {
                    assertNotNull(InitialContext.doLookup("java:module/concurrent/ZLContextSvc")); // unchanged
                } catch (NamingException x) {
                    throw new AssertionError(x);
                }
            });

            assertEquals(55906, ZipCode.get());
            assertEquals(timestamp1, Timestamp.get());
            assertEquals(6, Thread.currentThread().getPriority());
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            Timestamp.clear();
            ZipCode.clear();
        }
    }

    /**
     * Use a ContextService from a ContextServiceDefinition that is defined in an EJB.
     */
    @Test
    public void testEJBAnnoContextServiceDefinition() throws Exception {
        Executor bean = InitialContext.doLookup("java:global/ConcurrencyTestApp/ConcurrencyTestEJB/ExecutorBean!java.util.concurrent.Executor");
        assertNotNull(bean);
        bean.execute(() -> {
            try {
                ContextService contextSvc = InitialContext.doLookup("java:module/concurrent/ZLContextSvc");

                // Put some fake context onto the thread:
                Timestamp.set();
                ZipCode.set(55906);
                ListContext.newList();
                ListContext.add(6);
                ListContext.add(12);
                Thread.currentThread().setPriority(4);
                Long ts0 = Timestamp.get();
                Thread.sleep(100); // ensure we progress from the current timestamp

                try {
                    // Contextualize a Callable with the above context:
                    Callable<Object[]> contextualCallable = contextSvc.contextualCallable(() -> {
                        // The Callable records the context
                        Object lookupResult;
                        try {
                            lookupResult = InitialContext.doLookup("java:comp/concurrent/executor8");
                        } catch (NamingException x) {
                            throw new CompletionException(x);
                        }
                        return new Object[] {
                                              lookupResult,
                                              ZipCode.get(),
                                              ListContext.asString(),
                                              Thread.currentThread().getPriority(),
                                              Timestamp.get()
                        };
                    });

                    // Alter some of the context on the current thread
                    ZipCode.set(55902);
                    ListContext.newList();
                    ListContext.add(2);
                    Thread.currentThread().setPriority(3);

                    // Run with the captured context:
                    Object[] results;
                    try {
                        results = contextualCallable.call();
                    } catch (RuntimeException x) {
                        throw x;
                    } catch (Exception x) {
                        throw new CompletionException(x);
                    }

                    // Application context was configured to be propagated
                    assertTrue(results[0].toString(), results[0] instanceof ManagedExecutorService);

                    // Zip code context was configured to be propagated
                    assertEquals(Integer.valueOf(55906), results[1]);

                    // List context was configured to be propagated
                    assertEquals("[6, 12]", results[2]);

                    // Priority context was configured to be left unchanged
                    assertEquals(Integer.valueOf(3), results[3]);

                    // Timestamp context was configured to be cleared
                    assertNull(results[4]);

                    // Verify that context is restored on the current thread:
                    assertEquals(55902, ZipCode.get());
                    assertEquals("[2]", ListContext.asString());
                    assertEquals(3, Thread.currentThread().getPriority());
                    assertEquals(ts0, Timestamp.get());

                    // Run the supplier on another thread
                    Future<Object[]> future = unmanagedThreads.submit(contextualCallable);
                    results = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

                    // Application context was configured to be propagated
                    assertTrue(results[0].toString(), results[0] instanceof ManagedExecutorService);

                    // Zip code context was configured to be propagated
                    assertEquals(Integer.valueOf(55906), results[1]);

                    // List context was configured to be propagated
                    assertEquals("[6, 12]", results[2]);

                    // Priority context was configured to be left unchanged
                    assertEquals(Integer.valueOf(Thread.NORM_PRIORITY), results[3]);

                    // Timestamp context was configured to be cleared
                    assertNull(results[4]);
                } finally {
                    // Remove fake context
                    Timestamp.clear();
                    ZipCode.clear();
                    ListContext.clear();
                    Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                }

            } catch (ExecutionException | InterruptedException | NamingException | TimeoutException x) {
                throw new EJBException(x);
            }
        });
    }

    /**
     * Use a ManagedExecutorService from a ManagedExecutorDefinition that is defined in an EJB.
     */
    @Test
    public void testEJBAnnoManagedExecutorDefinition() throws Exception {
        Exchanger<Long> exchanger = new Exchanger<Long>();
        Supplier<Object> runTestAsEJB = () -> {
            // Enforcement of maxAsync=1
            try {
                long threadId = Thread.currentThread().getId();
                Long otherThreadId = exchanger.exchange(threadId, 1, TimeUnit.SECONDS);
                fail("This thread (" + threadId + ") and other thread (" + otherThreadId +
                     ") are running at the same time in violation of maxAsync=1 of ManagedExecutorDefinition");
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            } catch (TimeoutException x) {
                // expected
            }

            // Third-party ZipCode context must be cleared
            assertEquals(0, ZipCode.get());

            // Application context must be propagated
            try {
                return InitialContext.doLookup("java:comp/concurrent/executor8");
            } catch (NamingException x) {
                throw new CompletionException(x);
            }
        };

        Executor bean = InitialContext.doLookup("java:global/ConcurrencyTestApp/ConcurrencyTestEJB/ExecutorBean!java.util.concurrent.Executor");
        assertNotNull(bean);
        bean.execute(() -> {
            try {
                ZipCode.set(55901);

                ManagedExecutorService executor = InitialContext.doLookup("java:app/env/concurrent/executor8ref");

                CompletableFuture<?> future1 = executor.supplyAsync(runTestAsEJB);
                CompletableFuture<?> future2 = executor.supplyAsync(runTestAsEJB);

                Object result = CompletableFuture.anyOf(future1, future2).join();
                assertNotNull(result);
                assertTrue(result.toString(), result instanceof ManagedExecutorService);

                // Cancel whichever hasn't completed yet,
                future1.cancel(true);
                future2.cancel(true);
            } catch (NamingException x) {
                throw new EJBException(x);
            } finally {
                ZipCode.clear();
            }
        });
    }

    /**
     * Use a ManagedScheduledExecutorService from a ManagedScheduledExecutorDefinition that is defined in an EJB.
     */
    @Test
    public void testEJBAnnoManagedScheduledExecutorDefinition() throws Exception {
        Function<CyclicBarrier, Integer> runTestAsEJB = barrier -> {
            // Enforcement of maxAsync=2
            try {
                int index = barrier.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                fail("3 async tasks were able to run at once, in violation of maxAsync=2. Arrival index: " + index);
            } catch (BrokenBarrierException x) {
                // expected
            } catch (InterruptedException | TimeoutException x) {
                throw new CompletionException(x);
            }

            // Third-party List context must be propagated
            assertEquals("[28, 45, 53]", ListContext.asString());

            // Application context must be propagated
            try {
                ManagedScheduledExecutorService result = InitialContext.doLookup("java:module/concurrent/executor9");
                assertNotNull(result);
            } catch (NamingException x) {
                throw new CompletionException(x);
            }

            // Third-party Timestamp context must be cleared
            assertEquals(null, Timestamp.get());

            return ZipCode.get();
        };

        Executor bean = InitialContext.doLookup("java:global/ConcurrencyTestApp/ConcurrencyTestEJB/ExecutorBean!java.util.concurrent.Executor");
        assertNotNull(bean);
        bean.execute(() -> {
            try {
                // Add some fake context
                ListContext.newList();
                ListContext.add(28);
                ListContext.add(45);
                ListContext.add(53);
                Timestamp.set();

                ManagedScheduledExecutorService executor = InitialContext.doLookup("java:module/concurrent/executor9");

                CompletableFuture<CyclicBarrier> stage1 = executor.newIncompleteFuture();

                ZipCode.set(55901);
                CompletableFuture<?> stage2a = stage1.thenApplyAsync(runTestAsEJB);

                ZipCode.set(55902);
                CompletableFuture<?> stage2b = stage1.thenApplyAsync(runTestAsEJB);

                ZipCode.set(55904);
                CompletableFuture<?> stage2c = stage1.thenApplyAsync(runTestAsEJB);

                ZipCode.set(55906);
                assertTrue(stage1.complete(new CyclicBarrier(3)));

                CyclicBarrier barrier = stage1.join();

                // 2 must run in parallel
                for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && barrier.getNumberWaiting() < 2;)
                    TimeUnit.MILLISECONDS.sleep(200);

                assertEquals(2, barrier.getNumberWaiting());

                // ensure that all 3 do not run in parallel
                try {
                    CompletableFuture.allOf(stage2a, stage2b, stage2c).get(1, TimeUnit.SECONDS);
                    fail("3 async tasks must not run in parallel when maxAsync=2");
                } catch (TimeoutException x) {
                    // expected
                }

                // break the barrier
                barrier.reset();

                // wait for the third async task to start
                for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && barrier.getNumberWaiting() < 1;)
                    TimeUnit.MILLISECONDS.sleep(200);

                barrier.reset();

                assertEquals(Integer.valueOf(55901), stage2a.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertEquals(Integer.valueOf(55902), stage2b.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertEquals(Integer.valueOf(55904), stage2c.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            } catch (ExecutionException | InterruptedException | NamingException | TimeoutException x) {
                throw new EJBException(x);
            } finally {
                ListContext.clear();
                Timestamp.clear();
                ZipCode.clear();
            }
        });
    }

    /**
     * Use a ManagedThreadFactory from a ManagedThreadFactoryDefinition that is defined in an EJB.
     */
    @Test
    public void testEJBAnnoManagedThreadFactoryDefinition() throws Exception {
        Executor bean = InitialContext.doLookup("java:global/ConcurrencyTestApp/ConcurrencyTestEJB/ExecutorBean!java.util.concurrent.Executor");
        assertNotNull(bean);
        bean.execute(() -> {
            try {
                ManagedThreadFactory threadFactory = InitialContext.doLookup("java:module/concurrent/tf");

                try {
                    ZipCode.set(55904);
                    Thread.currentThread().setPriority(4);

                    LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();

                    threadFactory.newThread(() -> {
                        results.add(Thread.currentThread().getPriority());
                        results.add(ZipCode.get());
                        try {
                            results.add(InitialContext.doLookup("java:module/concurrent/tf"));
                        } catch (Throwable x) {
                            results.add(x);
                        }
                    }).start();

                    // Verify that priority from the ManagedThreadFactoryDefinition is used,
                    Object priority = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                    assertEquals(Integer.valueOf(6), priority);

                    // Verify that custom thread context type ZipCode is cleared
                    Object zipCode = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                    assertEquals(Integer.valueOf(0), zipCode);

                    // Verify that Application component context is propagated,
                    Object resultOfLookup = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                    if (resultOfLookup instanceof Throwable)
                        throw new CompletionException((Throwable) resultOfLookup);
                    assertNotNull(resultOfLookup);
                    assertTrue(resultOfLookup.toString(), resultOfLookup instanceof ManagedThreadFactory);
                } finally {
                    Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                    ZipCode.clear();
                }

            } catch (InterruptedException | NamingException x) {
                throw new EJBException(x);
            }
        });
    }

    /**
     * Use a ManagedThreadFactory from a ManagedThreadFactoryDefinition that is defined in an EJB that has
     * not been previously initialized/invoked.
     */
    @Test
    public void testEJBAnnoManagedThreadFactoryInitialization() throws Exception {
        Object resultOfLookup = testEJBAnnoManagedThreadFactoryInitializationBean.lookupThreadFactory();
        assertTrue(resultOfLookup.toString(), resultOfLookup instanceof ManagedThreadFactory);
    }

    /**
     * Use a ContextService that is defined by another ContextServiceDefinition in an EJB.
     */
    @Test
    public void testEJBContextServiceDefinition() throws Exception {
        // java:global name is accessible outside of the EJB module,
        ContextService contextSvc = InitialContext.doLookup("java:global/concurrent/anno/ejb/LPContextService");

        // Put some fake context onto the thread:
        Timestamp.set();
        ZipCode.set(55904);
        ListContext.newList();
        ListContext.add(4);
        ListContext.add(8);
        Thread.currentThread().setPriority(4);
        Long ts0 = Timestamp.get();

        try {
            // Contextualize a Callable with the above context:
            Callable<Object[]> contextualCallable = contextSvc.contextualCallable(() -> {
                // The Callable records the context
                Object lookupResult;
                try {
                    lookupResult = InitialContext.doLookup("java:app/concurrent/anno/ejb/LPScheduledExecutor");
                    throw new AssertionError("Application context was not cleared. Looked up: " + lookupResult);
                } catch (NamingException x) {
                    // expected
                }
                return new Object[] {
                                      ListContext.asString(),
                                      Thread.currentThread().getPriority(),
                                      Timestamp.get(),
                                      ZipCode.get(),
                };
            });

            // Alter some of the context on the current thread
            ZipCode.set(55901);
            ListContext.newList();
            ListContext.add(1);
            Thread.currentThread().setPriority(6);
            TimeUnit.MILLISECONDS.sleep(100);
            Long ts1 = Timestamp.get();

            // Run with the captured context:
            Object[] results;
            try {
                results = contextualCallable.call();
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new CompletionException(x);
            }

            // List context was configured to be propagated
            assertEquals("[4, 8]", results[0]);

            // Priority context was configured to be propagated
            assertEquals(Integer.valueOf(4), results[1]);

            // Remaining context (Timestamp) was configured to be left unchanged
            assertEquals(ts1, results[2]);

            // Zip code context was configured to be left unchanged
            assertEquals(Integer.valueOf(55901), results[3]);

            // Verify that context is restored on the current thread:
            assertEquals("[1]", ListContext.asString());
            assertEquals(6, Thread.currentThread().getPriority());
            assertEquals(ts1, Timestamp.get());
            assertEquals(55901, ZipCode.get());

            // Run the task on another thread
            Future<Object[]> future = unmanagedThreads.submit(contextualCallable);
            results = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            // List context was configured to be propagated
            assertEquals("[4, 8]", results[0]);

            // Priority context was configured to be propagated
            assertEquals(Integer.valueOf(4), results[1]);

            // Remaining context (Timestamp) was configured to be left unchanged
            assertEquals(null, results[2]);

            // Zip code context was configured to be left unchanged
            assertEquals(Integer.valueOf(0), results[3]);
        } finally {
            // Remove fake context
            Timestamp.clear();
            ZipCode.clear();
            ListContext.clear();
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
    }

    /**
     * Use a ManagedExecutorService that is defined by another ManagedExecutorDefinition in an EJB.
     */
    @Test
    public void testEJBManagedExecutorDefinition() throws Exception {
        BiFunction<CountDownLatch, CountDownLatch, Object> task = (twoStarted, threeStarted) -> {
            twoStarted.countDown();
            threeStarted.countDown();
            try {
                assertTrue(threeStarted.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                // requires application context
                return InitialContext.doLookup("java:comp/concurrent/anno/ejb/Executor");
            } catch (InterruptedException | NamingException x) {
                throw new CompletionException(x);
            }
        };

        Executor bean = InitialContext.doLookup("java:global/ConcurrencyTestApp/ConcurrencyTestEJB/ExecutorBean!java.util.concurrent.Executor");
        assertNotNull(bean);
        bean.execute(() -> {
            try {
                ManagedExecutorService executor = InitialContext.doLookup("java:comp/concurrent/anno/ejb/Executor");

                CompletableFuture<CountDownLatch> twoStartedFuture = executor.completedFuture(new CountDownLatch(2));
                CompletableFuture<CountDownLatch> threeStartedFuture = executor.completedFuture(new CountDownLatch(3));

                CompletableFuture<?> stage3 = twoStartedFuture.thenCombineAsync(threeStartedFuture, task);
                CompletableFuture<?> stage4 = twoStartedFuture.thenCombineAsync(threeStartedFuture, task);
                CompletableFuture<?> stage5 = twoStartedFuture.thenCombineAsync(threeStartedFuture, task);

                // 2 tasks must run concurrently per <max-async>2</max-async>
                assertTrue(twoStartedFuture.join().await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

                // 3 tasks must not run concurrently
                assertFalse(threeStartedFuture.join().await(1, TimeUnit.SECONDS));

                // running inline is not considered async
                CompletableFuture<?> stage6 = twoStartedFuture.thenCombine(threeStartedFuture, task);
                assertNotNull(stage6.join());

                assertNotNull(stage5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertNotNull(stage4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertNotNull(stage3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            } catch (ExecutionException | InterruptedException | NamingException | TimeoutException x) {
                throw new EJBException(x);
            }
        });
    }

    /**
     * Use a ManagedScheduledExecutorService that is defined by another ManagedScheduledExecutorDefinition in an EJB.
     */
    @Test
    public void testEJBDDManagedScheduledExecutorDefinition() throws Exception {
        BiFunction<CountDownLatch, CountDownLatch, int[]> task = (threeStarted, fourStarted) -> {
            threeStarted.countDown();
            fourStarted.countDown();

            try {
                assertTrue(fourStarted.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }

            // Application context must be cleared, per java:global/concurrent/anno/ejb/LPContextService,
            // which is configured as the context-service-ref for the managed-scheduled-executor,
            try {
                Object lookedUp = InitialContext.doLookup("java:app/concurrent/anno/ejb/LPScheduledExecutor");
                throw new AssertionError("Application context should be cleared. Instead looked up " + lookedUp);
            } catch (NamingException x) {
                // pass
            }

            return new int[] {
                               Thread.currentThread().getPriority(), // must be propagated
                               ZipCode.get() // must be left unchanged
            };
        };

        Executor bean = InitialContext.doLookup("java:global/ConcurrencyTestApp/ConcurrencyTestEJB/ExecutorBean!java.util.concurrent.Executor");
        assertNotNull(bean);
        bean.execute(() -> {
            try {
                ManagedExecutorService executor = InitialContext.doLookup("java:app/concurrent/anno/ejb/LPScheduledExecutor");

                CompletableFuture<CountDownLatch> threeStartedFuture = executor.completedFuture(new CountDownLatch(3));
                CompletableFuture<CountDownLatch> fourStartedFuture = executor.completedFuture(new CountDownLatch(4));

                Thread.currentThread().setPriority(4);
                ZipCode.set(55904);
                CompletableFuture<int[]> stage3 = threeStartedFuture.thenCombineAsync(fourStartedFuture, task);
                CompletableFuture<int[]> stage4 = threeStartedFuture.thenCombineAsync(fourStartedFuture, task);

                Thread.currentThread().setPriority(6);
                ZipCode.set(55906);
                CompletableFuture<int[]> stage5 = threeStartedFuture.thenCombineAsync(fourStartedFuture, task);
                CompletableFuture<int[]> stage6 = threeStartedFuture.thenCombineAsync(fourStartedFuture, task);

                Thread.currentThread().setPriority(2);
                ZipCode.set(55902);

                // 3 tasks must run concurrently per <max-async>3</max-async>
                assertTrue(threeStartedFuture.join().await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

                // 4 tasks must not run concurrently
                assertFalse(fourStartedFuture.join().await(1, TimeUnit.SECONDS));

                // running inline is not considered async
                CompletableFuture<int[]> stage7 = threeStartedFuture.thenCombine(fourStartedFuture, task);

                int[] results;
                assertNotNull(results = stage7.join());
                assertEquals(2, results[0]); // Priority context is propagated
                assertEquals(55902, results[1]); // ZipCode context is left unchanged

                assertNotNull(results = stage6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertEquals(6, results[0]); // Priority context is propagated
                assertEquals(0, results[1]); // ZipCode context is left unchanged

                assertNotNull(results = stage5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertEquals(6, results[0]); // Priority context is propagated
                assertEquals(0, results[1]); // ZipCode context is left unchanged

                assertNotNull(results = stage4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertEquals(4, results[0]); // Priority context is propagated
                assertEquals(0, results[1]); // ZipCode context is left unchanged

                assertNotNull(results = stage3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertEquals(4, results[0]); // Priority context is propagated
                assertEquals(0, results[1]); // ZipCode context is left unchanged
            } catch (ExecutionException | InterruptedException | NamingException | TimeoutException x) {
                throw new EJBException(x);
            } finally {
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                ZipCode.clear();
            }
        });
    }

    /**
     * Use a ManagedThreadFactory that is defined by another ManagedThreadFactoryDefinition in an EJB.
     */
    @Test
    public void testEJBDDManagedThreadFactoryDefinition() throws Exception {
        Executor bean = InitialContext.doLookup("java:global/ConcurrencyTestApp/ConcurrencyTestEJB/ExecutorBean!java.util.concurrent.Executor");
        assertNotNull(bean);
        bean.execute(() -> {
            try {
                try {
                    Timestamp.set();
                    Thread.currentThread().setPriority(6);

                    ManagedThreadFactory threadFactory = InitialContext.doLookup("java:module/concurrent/anno/ejb/ZLThreadFactory");

                    LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();

                    threadFactory.newThread(() -> {
                        results.add(Thread.currentThread().getPriority());

                        Long timestamp = Timestamp.get();
                        results.add(timestamp == null ? "none" : timestamp);

                        try {
                            results.add(InitialContext.doLookup("java:module/concurrent/anno/ejb/ZLThreadFactory"));
                        } catch (Throwable x) {
                            results.add(x);
                        }
                    }).start();

                    // Verify that priority from the managed-thread-factory is used,
                    Object priority = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                    assertEquals(Integer.valueOf(7), priority);

                    // Verify that custom thread context type TimestampContext is cleared
                    Object timestamp = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                    assertEquals("none", timestamp);

                    // Verify that Application component context is propagated,
                    Object resultOfLookup = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                    if (resultOfLookup instanceof Throwable)
                        throw new CompletionException((Throwable) resultOfLookup);
                    assertNotNull(resultOfLookup);
                    assertTrue(resultOfLookup.toString(), resultOfLookup instanceof ManagedThreadFactory);
                } finally {
                    Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                    ListContext.clear();
                    Timestamp.clear();
                }

            } catch (InterruptedException | NamingException x) {
                throw new EJBException(x);
            }
        });
    }

    /**
     * Use Future.exceptionNow on a managed completable future that is
     *
     * <li>successfully completed
     * <li>exceptionally completed
     * <li>running
     * <li>forcibly completed
     * <li>has its results replaced
     * <li>cancelled
     */
    @Test
    public void testExceptionNow() throws Throwable {
        ManagedExecutorService executor = InitialContext.doLookup("java:module/concurrent/executor5"); // maxAsync = 1

        CompletableFuture<Long> successfulTaskFuture = executor.supplyAsync(() -> 600L);

        CompletableFuture<Long> failingTaskFuture = executor.supplyAsync(() -> {
            throw new CompletionException(new IOException("This is an expected exception for testExceptionNow."));
        });

        // Use up maxConcurrency
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch task3started = new CountDownLatch(1);

        CompletableFuture<Boolean> task3future = executor.supplyAsync(() -> {
            task3started.countDown();
            try {
                return blocker.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        });

        try {
            assertEquals(true, task3started.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // Future.exceptionNow on task that completes successfully:
            // successfulTaskFuture must have completed by now because task3 is using up max concurrency
            Method exceptionNow = successfulTaskFuture.getClass().getMethod("exceptionNow");
            try {
                Throwable exception = (Throwable) exceptionNow.invoke(successfulTaskFuture);
                if (exception == null)
                    throw new AssertionError("exceptionNow returned null for successfully completed task");
                else
                    throw new AssertionError("exceptionNow returned value for successfully completed task").initCause(exception);
            } catch (InvocationTargetException x) {
                if (!(x.getCause() instanceof IllegalStateException))
                    throw x;
            } catch (IllegalStateException x) {
                // pass
            }

            // Future.exceptionNow on task that failed with an exception:
            // failingTaskFuture must have completed by now because task3 is using up max concurrency
            Throwable exception = (Throwable) exceptionNow.invoke(failingTaskFuture);
            if (!IOException.class.equals(exception.getClass()))
                throw exception;

            // Future.exceptionNow on running task:
            try {
                exception = (Throwable) exceptionNow.invoke(task3future);
                if (exception == null)
                    throw new AssertionError("exceptionNow returned null for running task");
                else
                    throw new AssertionError("exceptionNow returned value for running task").initCause(exception);
            } catch (InvocationTargetException x) {
                if (!(x.getCause() instanceof IllegalStateException))
                    throw x;
            } catch (IllegalStateException x) {
                // pass
            }

            // Future.exceptionNow on task that is forcibly completed:
            CompletableFuture<Long> task4future = executor.supplyAsync(() -> 664L);
            task4future.complete(644L);
            try {
                exception = (Throwable) exceptionNow.invoke(task4future);
                if (exception == null)
                    throw new AssertionError("exceptionNow returned null for successfully completed task");
                else
                    throw new AssertionError("exceptionNow returned value for successfully completed task").initCause(exception);
            } catch (InvocationTargetException x) {
                if (!(x.getCause() instanceof IllegalStateException))
                    throw x;
            } catch (IllegalStateException x) {
                // pass
            }

            // Future.exceptionNow after obtruding the result with exceptional completion
            task4future.obtrudeException(new SQLException("Not a real error."));
            exception = (Throwable) exceptionNow.invoke(task4future);
            if (!SQLException.class.equals(exception.getClass()))
                throw exception;

            // Future.exceptionNow after obtruding the exceptional result back to successful, but with a different value:
            task4future.obtrudeValue(640L);
            try {
                exception = (Throwable) exceptionNow.invoke(task4future);
                if (exception == null)
                    throw new AssertionError("exceptionNow returned null for successfully completed task");
                else
                    throw new AssertionError("exceptionNow returned value for successfully completed task").initCause(exception);
            } catch (InvocationTargetException x) {
                if (!(x.getCause() instanceof IllegalStateException))
                    throw x;
            } catch (IllegalStateException x) {
                // pass
            }

            // Future.exceptionNow on cancelled task:
            assertEquals(true, task3future.cancel(true));
            try {
                exception = (Throwable) exceptionNow.invoke(task3future);
                if (exception == null)
                    throw new AssertionError("exceptionNow returned null for cancelled task");
                else
                    throw new AssertionError("exceptionNow returned value for cancelled task").initCause(exception);
            } catch (InvocationTargetException x) {
                if (!(x.getCause() instanceof IllegalStateException))
                    throw x;
            } catch (IllegalStateException x) {
                // pass
            }

            // Future.exceptionNow on minimal completion stage
            CompletionStage<Long> task4stage = task4future.minimalCompletionStage();
            try {
                exception = (Throwable) exceptionNow.invoke(task4stage);
                if (exception == null)
                    throw new AssertionError("Shoud not be able to invoke exceptionNow on a minimal CompletionStage");
                else
                    throw new AssertionError("Shoud not be able to invoke exceptionNow on a minimal CompletionStage").initCause(exception);
            } catch (InvocationTargetException x) {
                if (!(x.getCause() instanceof UnsupportedOperationException))
                    throw x;
            } catch (UnsupportedOperationException x) {
                // pass - not permitted on completion stages, only futures
            }
        } finally {
            blocker.countDown();
        }
    }

    /**
     * Verify that a ManagedExecutorService propagates context to dependent stages that are
     * created from a failedFuture().
     */
    @Test
    public void testFailedFuture() throws Exception {
        Throwable failure = new IllegalStateException("Intentional failure to test error paths");
        CompletableFuture<Integer> failed = executor5.failedFuture(failure);
        CompletableFuture<Integer> handled;
        try {
            ZipCode.set(55904);
            handled = failed.handleAsync((result, x) -> {
                if (result == null && x == failure)
                    return ZipCode.get();
                else
                    throw new CompletionException(x);
            });
        } finally {
            ZipCode.clear();
        }
        assertEquals(Integer.valueOf(55904), handled.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Verify that forced completion of a copied stage neither impacts the original, nor other copies.
     */
    @Test
    public void testForcedCompletionOfCopies() throws Exception {
        CompletableFuture<String> original = new CompletableFuture<String>();
        CompletableFuture<String> copy1 = executor5.copy(original);
        CompletableFuture<String> copy2 = executor5.copy(original);
        CompletableFuture<String> copy3 = executor5.copy(original);
        CompletableFuture<String> copy4 = executor5.copy(original);
        CompletableFuture<String> copy5 = executor5.copy(original);
        CompletableFuture<String> copy6 = executor5.copy(original);
        CompletableFuture<String> copy7 = executor5.copy(original);

        copy7.completeOnTimeout("7 is done", 60, TimeUnit.MILLISECONDS);
        assertTrue(copy6.cancel(true));
        assertTrue(copy5.complete("5 is done"));
        copy4.obtrudeValue("4 is done");
        copy3.completeExceptionally(new IllegalArgumentException("3 is done"));
        copy2.obtrudeException(new ArrayIndexOutOfBoundsException("2 is done"));

        assertFalse(copy1.isDone());
        assertFalse(original.isDone());

        assertEquals("7 is done", copy7.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertTrue(original.complete("original is done"));
        assertEquals("original is done", copy1.join());
        assertTrue(copy2.isCompletedExceptionally());
        assertTrue(copy3.isCompletedExceptionally());
        assertEquals("4 is done", copy4.join());
        assertEquals("5 is done", copy5.join());
        assertTrue(copy6.isCancelled());
    }

    /**
     * Verify that it is possible to obtain the nested ContextService of a ManagedExecutorService
     * that is configured as a ManagedExecutorDefinition, and that when withContextCapture is invoked on this ContextService,
     * the resulting CompletableFuture is backed by the ManagedExecutorService, subject to its concurrency
     * constraints (maxAsync=1) and runs tasks under the context propagation settings of its nested ContextService.
     */
    @Test
    public void testGetContextServiceFromManagedExecutorDefinition() throws Exception {
        Executor bean = InitialContext.doLookup("java:global/ConcurrencyTestApp/ConcurrencyTestEJB/ExecutorBean!java.util.concurrent.Executor");
        assertNotNull(bean);
        bean.execute(() -> {
            CountDownLatch blocker = new CountDownLatch(1);
            CountDownLatch blocking = new CountDownLatch(1);
            Callable<Boolean> blockerTask = () -> {
                blocking.countDown();
                return blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            };

            try {
                ManagedExecutorService executor = InitialContext.doLookup("java:comp/concurrent/executor8");
                ContextService contextSvc = executor.getContextService();

                CompletableFuture<String> stage1 = new CompletableFuture<String>();

                CompletableFuture<String> stage1copy = contextSvc.withContextCapture(stage1);

                // block the managed executor's single maxAsync slot
                Future<Boolean> blockerFuture1 = executor.submit(blockerTask);
                assertTrue(blocking.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

                CompletableFuture<Object> stage2 = stage1copy.thenApplyAsync(jndiName -> {
                    try {
                        return InitialContext.doLookup(jndiName);
                    } catch (NamingException x) {
                        throw new CompletionException(x);
                    }
                });

                stage1.complete("java:comp/concurrent/executor8");

                // copied stage completes,
                assertEquals("java:comp/concurrent/executor8", stage1copy.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

                // but the async stage must be blocked from running on the executor,
                try {
                    Object result = stage2.get(1, TimeUnit.SECONDS);
                    fail("Dependent stage of withContextCapture stage should be blocked from asynchronous execution " +
                         "due to both maxAsync slots of the executor being used up. Instead: " + result);
                } catch (TimeoutException x) {
                    // expected
                }

                blocker.countDown();

                Object result;
                assertNotNull(result = stage2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertTrue(result.toString(), result instanceof ManagedExecutorService);

                assertEquals(true, blockerFuture1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            } catch (ExecutionException | InterruptedException | NamingException | TimeoutException x) {
                throw new EJBException(x);
            } finally {
                blocker.countDown();
            }
        });
    }

    /**
     * Verify that it is possible to obtain the nested ContextService of a ManagedScheduledExecutorService
     * that is configured as a ManagedScheduledExecutorDefinition, and that when withContextCapture is invoked on this ContextService,
     * the resulting CompletableFuture is backed by the ManagedScheduledExecutorService, subject to its concurrency
     * constraints (maxAsync=2) and runs tasks under the context propagation settings of its nested ContextService.
     */
    @Test
    public void testGetContextServiceFromManagedScheduledExecutorDefinition() throws Exception {
        ManagedScheduledExecutorService executor = InitialContext.doLookup("java:comp/concurrent/executor6");
        ContextService contextSvc = executor.getContextService();

        CompletableFuture<String> stage1 = new CompletableFuture<String>();

        CompletableFuture<String> stage1copy = contextSvc.withContextCapture(stage1);

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch blocking = new CountDownLatch(2);
        Callable<Boolean> blockerTask = () -> {
            blocking.countDown();
            return blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        };
        try {
            // block both of the managed executor's maxAsync slots
            Future<Boolean> blockerFuture1 = executor.submit(blockerTask);
            Future<Boolean> blockerFuture2 = executor.submit(blockerTask);
            assertTrue(blocking.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            CompletableFuture<Object> stage2 = stage1copy.thenApplyAsync(jndiName -> {
                try {
                    return InitialContext.doLookup(jndiName);
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });

            stage1.complete("java:comp/concurrent/executor6");

            // copied stage completes,
            assertEquals("java:comp/concurrent/executor6", stage1copy.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // but the async stage must be blocked from running on the executor,
            try {
                Object result = stage2.get(1, TimeUnit.SECONDS);
                fail("Dependent stage of withContextCapture stage should be blocked from asynchronous execution " +
                     "due to both maxAsync slots of the executor being used up. Instead: " + result);
            } catch (TimeoutException x) {
                // expected
            }

            blocker.countDown();

            Object result;
            assertNotNull(result = stage2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(result.toString(), result instanceof ManagedScheduledExecutorService);

            assertEquals(true, blockerFuture1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(true, blockerFuture2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            blocker.countDown();
        }
    }

    /**
     * Verify that it is possible to obtain the nested ContextService of a ManagedExecutorService
     * that is configured in server.xml, and that when withContextCapture is invoked on this ContextService,
     * the resulting CompletableFuture is backed by the ManagedExecutorService, subject to its concurrency
     * constraints, and runs tasks under the context propagation settings of its nested ContextService.
     */
    @Test
    public void testGetContextServiceFromServerXMLWithContextCapture() throws Exception {
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
     * Per the spec JavaDoc, ContextService.getExecutionProperties can be used on a
     * contextual proxy obtained by createContextualProxy, but otherwise raises
     * IllegalArgumentException.
     */
    @Test
    public void testGetExecutionProperties() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:app/concurrent/appContextSvc");
        Function<Integer, Integer> triple = i -> i * 3;
        Map<String, String> execProps = Collections.singletonMap(ManagedTask.IDENTITY_NAME, "testGetExecutionProperties");

        @SuppressWarnings("unchecked")
        Function<Integer, Integer> proxyFn = contextSvc.createContextualProxy(triple, execProps, Function.class);
        assertEquals(execProps, contextSvc.getExecutionProperties(proxyFn));

        Function<Integer, Integer> contextFn = contextSvc.contextualFunction(triple);
        try {
            Map<String, String> unexpected = contextSvc.getExecutionProperties(contextFn);
            fail("getExecutionProperties must raise IllegalArgumentException when the proxy is " +
                 "not created by createContextualProxy. Result: " + unexpected);
        } catch (IllegalArgumentException x) {
            // expected
        }

        Executor contextExecutor = contextSvc.currentContextExecutor();
        try {
            Map<String, String> unexpected = contextSvc.getExecutionProperties(contextExecutor);
            fail("getExecutionProperties must raise IllegalArgumentException when the proxy is " +
                 "not created by createContextualProxy. Result: " + unexpected);
        } catch (IllegalArgumentException x) {
            // expected
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
        // TODO results[1] to results[4] : does third-party context propagate to the default managed executor?
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
        // TODO results[1] to results[4] : does third-party context propagate to the default managed executor?
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
        // TODO results[1] to results[4] : does third-party context propagate to the default managed executor?
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

    /**
     * Schedule a repeating timer with a ZonedTrigger that rejects the deprecated methods of Trigger
     * and implements only the ZonedTrigger methods that were added in Concurrency 3.0 / EE 10.
     */
    @Test
    public void testRepeatingTimerWithZonedTrigger() throws Exception {
        final AtomicLong counter = new AtomicLong();
        final ZoneId USMountain = ZoneId.of("America/Denver");

        ScheduledFuture<Long> future = executor6.schedule(() -> {
            TimeUnit.MILLISECONDS.sleep(200);
            return counter.incrementAndGet();
        }, new ZonedTrigger() {
            @Override
            public Date getNextRunTime(LastExecution lastExecution, Date scheduledAt) {
                throw new AssertionError("Deprecated getNextRunTime should not be used.");
            }

            // Run 1 second after the midpoint of the start and end of the previous execution.
            @Override
            public ZonedDateTime getNextRunTime(LastExecution lastExecution, ZonedDateTime scheduledAt) {
                if (lastExecution == null)
                    return scheduledAt.plusSeconds(1);
                else if (Long.valueOf(3).equals(lastExecution.getResult())) {
                    return null;
                } else {
                    ZonedDateTime start = lastExecution.getRunStart(getZoneId());
                    long lengthNS = start.until(lastExecution.getRunEnd(getZoneId()), ChronoUnit.NANOS);
                    if (lengthNS < 0)
                        throw new AssertionError(lengthNS);
                    ZonedDateTime midpoint = start.plusNanos(lengthNS / 2);
                    return midpoint.plusSeconds(1);
                }
            }

            @Override
            public ZoneId getZoneId() {
                return USMountain;
            }

            @Override
            public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
                throw new AssertionError("Deprecated skipRun should not be used.");
            }

            @Override
            public boolean skipRun(LastExecution lastExecutionInfo, ZonedDateTime scheduledRunTime) {
                // skip if over an hour late
                return scheduledRunTime.isBefore(ZonedDateTime.now(getZoneId()).minusHours(1));
            }
        });

        try {
            long result = 0;
            long startNS = System.nanoTime();
            do
                result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            while (result < 3 && System.nanoTime() - startNS < TIMEOUT_NS);

            assertEquals(3, result);
            assertFalse(future.isCancelled());
            assertTrue(future.isDone());
        } finally {
            if (!future.isDone())
                future.cancel(true);
        }
    }

    /**
     * Use Future.resultNow on a managed completable future that is
     *
     * <li>successfully completed
     * <li>exceptionally completed
     * <li>running
     * <li>forcibly completed
     * <li>has its results replaced
     * <li>cancelled
     */
    @Test
    public void testResultNow() throws Throwable {
        ManagedExecutorService executor = InitialContext.doLookup("java:module/concurrent/executor5"); // maxAsync = 1

        CompletableFuture<Long> successfulTaskFuture = executor.supplyAsync(() -> 500L);

        CompletableFuture<Long> failingTaskFuture = executor.supplyAsync(() -> {
            throw new CompletionException(new IOException("This is an expected exception for the test."));
        });

        Method resultNow = successfulTaskFuture.getClass().getMethod("resultNow");

        // Use up maxConcurrency
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch task3started = new CountDownLatch(1);

        CompletableFuture<Boolean> task3future = executor.supplyAsync(() -> {
            task3started.countDown();
            try {
                return blocker.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        });

        try {
            assertEquals(true, task3started.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // Future.resultNow on task that completes successfully:
            // successfulTaskFuture must have completed by now because task3 is using up max concurrency
            Object result = resultNow.invoke(successfulTaskFuture);
            assertEquals(Long.valueOf(500L), result);

            // Future.resultNow on task that failed with an exception:
            // failingTaskFuture must have completed by now because task3 is using up max concurrency
            try {
                result = resultNow.invoke(failingTaskFuture);
                throw new AssertionError("resultNow returned " + result + " for failed task");
            } catch (InvocationTargetException xx) { // Java reflection should not be wrapping IllegalStateException because it is a type of RuntimeException!
                IllegalStateException x = (IllegalStateException) xx.getCause();
                if (!(x.getCause() instanceof IOException))
                    throw x;
            } catch (IllegalStateException x) {
                if (!(x.getCause() instanceof IOException))
                    throw x;
            }

            // Future.resultNow on running task:
            try {
                result = resultNow.invoke(task3future);
                throw new AssertionError("resultNow returned " + result + " for running task");
            } catch (InvocationTargetException xx) { // Java reflection should not be wrapping IllegalStateException because it is a type of RuntimeException!
                IllegalStateException x = (IllegalStateException) xx.getCause();
                if (x.getCause() != null)
                    throw x;
            } catch (IllegalStateException x) {
                if (x.getCause() != null)
                    throw x;
            }

            // Future.resultNow on task that is forcibly completed:
            CompletableFuture<Long> task4future = executor.supplyAsync(() -> 400L);
            task4future.complete(444L);
            result = resultNow.invoke(task4future);
            assertEquals(Long.valueOf(444L), result);

            // Future.resultNow after obtruding the result with exceptional completion
            task4future.obtrudeException(new SQLException("Not a real error."));
            try {
                result = resultNow.invoke(task4future);
                throw new AssertionError("resultNow returned " + result + " for task with obtruded result");
            } catch (InvocationTargetException xx) { // Java reflection should not be wrapping IllegalStateException because it is a type of RuntimeException!
                IllegalStateException x = (IllegalStateException) xx.getCause();
                if (!(x.getCause() instanceof SQLException))
                    throw x;
            } catch (IllegalStateException x) {
                if (!(x.getCause() instanceof SQLException))
                    throw x;
            }

            // Future.resultNow after obtruding the exceptional result back to successful, but with a different value:
            task4future.obtrudeValue(440L);
            result = resultNow.invoke(task4future);
            assertEquals(Long.valueOf(440L), result);

            // Future.resultNow on cancelled task:
            assertEquals(true, task3future.cancel(true));
            try {
                result = resultNow.invoke(task3future);
                throw new AssertionError("resultNow returned " + result + " for cancelled task");
            } catch (InvocationTargetException xx) { // Java reflection should not be wrapping IllegalStateException because it is a type of RuntimeException!
                IllegalStateException x = (IllegalStateException) xx.getCause();
                if (!(x.getCause() instanceof CancellationException))
                    throw x;
            } catch (IllegalStateException x) {
                if (!(x.getCause() instanceof CancellationException))
                    throw x;
            }

            // Future.resultNow on minimal completion stage
            CompletionStage<Long> task4stage = task4future.minimalCompletionStage();
            try {
                result = resultNow.invoke(task4stage);
                fail("Shoud not be able to invoke resultNow on a minimal CompletionStage. Result " + result);
            } catch (InvocationTargetException x) {
                if (!(x.getCause() instanceof UnsupportedOperationException))
                    throw x;
            } catch (UnsupportedOperationException x) {
                // pass - not permitted on completion stages, only futures
            }
        } finally {
            blocker.countDown();
        }
    }

    /**
     * Verify that the ManagedExecutorService runAsync runs the completion stage action
     * with context captured per the java:comp/DefaultContextService when the
     * ManagedExecutorDefinition does not specify any value for context.
     */
    @Test
    public void testRunAsyncWithDefaultContextPropagation() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:global/concurrent/executor7");

        ZipCode.set(55906);
        try {
            CompletableFuture<Void> future = executor.runAsync(() -> {
                // third-party context is not propagated
                assertEquals(0, ZipCode.get());
                try {
                    // must have access to application component namespace, per Application context
                    assertNotNull(InitialContext.doLookup("java:comp/env/concurrent/executor3Ref"));
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
            });
            // cause any assertion errors from above to be raised,
            assertNull(future.join());
        } finally {
            ZipCode.clear();
        }
    }

    /**
     * An application defined ContextService that either clears or leaves unchanged
     * all third party context types must be able to create serializable contextual proxies.
     */
    @Test
    public void testSerializeProxyFromAppDefinedContextServiceClearRemaining() throws Throwable {
        ContextService contextSvc = InitialContext.doLookup("java:module/concurrent/clearRemainingContextSvc");

        Executor proxy = (Executor) contextSvc.createContextualProxy(new SameThreadExecutor(),
                                                                     Serializable.class, Executor.class);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(proxy);
        oout.flush();
        byte[] bytes = bout.toByteArray();
        oout.close();

        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Executor copy = (Executor) oin.readObject();
        oin.close();

        UserTransaction tran = InitialContext.doLookup("java:comp/UserTransaction");

        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable task = () -> {
            try {
                results.add(InitialContext.doLookup("java:module/concurrent/clearRemainingContextSvc"));
            } catch (Throwable x) {
                results.add(x);
            }
            try {
                results.add(tran.getStatus());
            } catch (Throwable x) {
                results.add(x);
            }
            results.add(ListContext.asString());
            results.add(Thread.currentThread().getPriority());
            results.add(Timestamp.get() == null ? "none" : Timestamp.get());
            results.add(ZipCode.get());
        };

        Long expectedTimestamp = unmanagedThreads.submit(() -> {
            tran.begin();
            try {
                // Put some fake context onto the thread:
                ListContext.newList();
                ListContext.add(25);
                Thread.currentThread().setPriority(2);
                Timestamp.set();
                Long ts0 = Timestamp.get();
                ZipCode.set(55902);

                copy.execute(task);

                return ts0;
            } finally {
                // Remove fake context
                ListContext.clear();
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                Timestamp.clear();
                ZipCode.clear();

                tran.rollback();
            }
        }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        Object result;

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("Task running on deserialized context failed lookup in application component namespace")
                            .initCause((Throwable) result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("Task running on deserialized context failed lookup in application component namespace")
                            .initCause((Throwable) result);

        assertEquals(Integer.valueOf(Status.STATUS_ACTIVE), result); // Transaction context must be unchanged

        assertEquals("[]", results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // ListContext must be cleared
        assertEquals(Integer.valueOf(2), results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // Priority context must be unchanged
        assertEquals(expectedTimestamp, results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // Timestamp context must be unchanged
        assertEquals(Integer.valueOf(0), results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // ZipCode context must be cleared
    }

    /**
     * An application defined ContextService that only propagates built-in types
     * must be able to create serializable contextual proxies.
     */
    @Test
    public void testSerializeProxyFromAppDefinedContextServicePropagateAppOnly() throws Throwable {
        ContextService contextSvc = InitialContext.doLookup("java:app/concurrent/appContextSvc");

        Executor proxy = (Executor) contextSvc.createContextualProxy(new SameThreadExecutor(),
                                                                     Collections.singletonMap(ManagedTask.IDENTITY_NAME, "SerializableTaskA"),
                                                                     Serializable.class, Executor.class);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(proxy);
        oout.flush();
        byte[] bytes = bout.toByteArray();
        oout.close();

        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Executor copy = (Executor) oin.readObject();
        oin.close();

        UserTransaction tran = InitialContext.doLookup("java:comp/UserTransaction");

        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable task = () -> {
            try {
                results.add(InitialContext.doLookup("java:comp/concurrent/ThirdPartyContextService"));
            } catch (Throwable x) {
                results.add(x);
            }
            try {
                results.add(tran.getStatus());
            } catch (Throwable x) {
                results.add(x);
            }
            results.add(ListContext.asString());
            results.add(Thread.currentThread().getPriority());
            results.add(Timestamp.get() == null ? "none" : Timestamp.get());
            results.add(ZipCode.get());
        };

        Long expectedTimestamp = unmanagedThreads.submit(() -> {
            tran.begin();
            try {
                // Put some fake context onto the thread:
                ListContext.newList();
                ListContext.add(46);
                Thread.currentThread().setPriority(4);
                Timestamp.set();
                Long ts0 = Timestamp.get();
                ZipCode.set(55904);

                copy.execute(task);

                return ts0;
            } finally {
                // Remove fake context
                ListContext.clear();
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                Timestamp.clear();
                ZipCode.clear();

                tran.rollback();
            }
        }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        Object result;

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("Task running on deserialized context failed lookup in application component namespace")
                            .initCause((Throwable) result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("Task running on deserialized context failed lookup in application component namespace")
                            .initCause((Throwable) result);
        assertEquals(Integer.valueOf(Status.STATUS_NO_TRANSACTION), result); // Transaction context must be cleared

        assertEquals("[46]", results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // ListContext must be unchanged
        assertEquals(Integer.valueOf(4), results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // Priority context must be unchanged
        assertEquals(expectedTimestamp, results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // Timestamp context must be unchanged
        assertEquals(Integer.valueOf(55904), results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // ZipCode context must be unchanged
    }

    /**
     * An application defined ContextService that either clears or leaves unchanged
     * all third party context types must be able to create serializable contextual proxies.
     */
    @Test
    public void testSerializeProxyFromAppDefinedContextServicePropagateRemaining() throws Throwable {
        ContextService contextSvc = InitialContext.doLookup("java:comp/concurrent/propagateRemainingContextSvc");

        Executor proxy = (Executor) contextSvc.createContextualProxy(new SameThreadExecutor(),
                                                                     Serializable.class, Executor.class);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(proxy);
        oout.flush();
        byte[] bytes = bout.toByteArray();
        oout.close();

        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Executor copy = (Executor) oin.readObject();
        oin.close();

        UserTransaction tran = InitialContext.doLookup("java:comp/UserTransaction");

        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable task = () -> {
            try {
                results.add(InitialContext.doLookup("java:comp/concurrent/propagateRemainingContextSvc"));
            } catch (Throwable x) {
                results.add(x);
            }
            try {
                results.add(tran.getStatus());
            } catch (Throwable x) {
                results.add(x);
            }
            results.add(ListContext.asString());
            results.add(Thread.currentThread().getPriority());
            results.add(Timestamp.get() == null ? "none" : Timestamp.get());
            results.add(ZipCode.get());
        };

        unmanagedThreads.submit(() -> {
            tran.begin();
            try {
                // Put some fake context onto the thread:
                ListContext.newList();
                ListContext.add(31);
                Thread.currentThread().setPriority(3);
                Timestamp.set();
                Long ts0 = Timestamp.get();
                ZipCode.set(55901);

                copy.execute(task);

                return ts0;
            } finally {
                // Remove fake context
                ListContext.clear();
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                Timestamp.clear();
                ZipCode.clear();

                tran.rollback();
            }
        }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        Object result;

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("Task running on deserialized context failed lookup in application component namespace")
                            .initCause((Throwable) result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("Task running on deserialized context failed lookup in application component namespace")
                            .initCause((Throwable) result);

        assertEquals(Integer.valueOf(Status.STATUS_ACTIVE), result); // Transaction context must be unchanged

        assertEquals("[31]", results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // ListContext must be left unchanged
        assertEquals(Integer.valueOf(Thread.NORM_PRIORITY), results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // Priority context must be cleared
        assertEquals("none", results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // Timestamp context must be cleared
        assertEquals(Integer.valueOf(55901), results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // ZipCode context must be left unchanged
    }

    /**
     * The default ContextService, java:comp/DefaultContextService, must be able to
     * create serializable contextual proxies.
     */
    @Test
    public void testSerializeProxyFromDefaultContextService() throws Throwable {
        ContextService contextSvc = InitialContext.doLookup("java:comp/DefaultContextService");

        Ser1Executor proxy = contextSvc.createContextualProxy(new SameThreadExecutor(), Ser1Executor.class);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(proxy);
        oout.flush();
        byte[] bytes = bout.toByteArray();
        oout.close();

        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Ser1Executor copy = (Ser1Executor) oin.readObject();
        oin.close();

        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable javaCompLookup = () -> {
            try {
                results.add(InitialContext.doLookup("java:comp/concurrent/ThirdPartyContextService"));
            } catch (Throwable x) {
                results.add(x);
            }
        };

        unmanagedThreads.submit(() -> copy.execute(javaCompLookup));

        Object result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(result);
        if (result instanceof Throwable)
            throw new AssertionError("Task running on deserialized context failed lookup in application component namespace")
                            .initCause((Throwable) result);
    }

    /**
     * A ContextService from server configuration must be able to create serializable contextual proxies.
     */
    @Test
    public void testSerializeProxyFromServerConfigContextService() throws Throwable {
        ContextService contextSvc = InitialContext.doLookup("concurrent/appContext");

        Ser2Executor proxy = contextSvc.createContextualProxy(new SameThreadExecutor(), Collections.emptyMap(), Ser2Executor.class);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(proxy);
        oout.flush();
        byte[] bytes = bout.toByteArray();
        oout.close();

        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Ser2Executor copy = (Ser2Executor) oin.readObject();
        oin.close();

        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable javaCompLookup = () -> {
            try {
                results.add(InitialContext.doLookup("java:comp/concurrent/ThirdPartyContextService"));
            } catch (Throwable x) {
                results.add(x);
            }
        };

        unmanagedThreads.submit(() -> copy.execute(javaCompLookup));

        Object result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(result);
        if (result instanceof Throwable)
            throw new AssertionError("Task running on deserialized context failed lookup in application component namespace")
                            .initCause((Throwable) result);
    }

    /**
     * Schedule a repeating timer that skips every other execution.
     */
    @Test
    public void testSkipEveryOtherExecution() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        final AtomicInteger skipEveryOther = new AtomicInteger();

        ScheduledFuture<Integer> future = executor6.schedule(counter::incrementAndGet, new ZonedTrigger() {
            final ZoneId zone = ZoneId.of("Australia/Perth");

            @Override
            public ZonedDateTime getNextRunTime(LastExecution lastExecution, ZonedDateTime scheduledAt) {
                int previousResult = lastExecution == null ? 0 : (Integer) lastExecution.getResult();
                return ZonedDateTime.now(zone).plus(previousResult * 100l, ChronoUnit.MILLIS);
            }

            @Override
            public ZoneId getZoneId() {
                return zone;
            }

            @Override
            public boolean skipRun(LastExecution lastExecutionInfo, ZonedDateTime scheduledRunTime) {
                return skipEveryOther.incrementAndGet() % 2 == 0;
            }
        });

        try {
            // await first 4 executions
            long result = 0;
            long startNS = System.nanoTime();
            do {
                try {
                    result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                } catch (SkippedException x) {
                    // expected due to skips
                }
                if (System.nanoTime() - startNS > TIMEOUT_NS * 2)
                    fail("Timed out with most recent result of " + result);
            } while (result < 4);
        } finally {
            future.cancel(false);
        }

        int s = skipEveryOther.get();
        if (s < 7)
            fail("skipRun was not invoked enough times (" + s + ") to have run 4 or more executions.");

        assertEquals(true, future.isCancelled());
        assertEquals(true, future.isDone());
    }

    /**
     * Third party context types that are configured to be propagated must fail to serialize.
     */
    @Test
    public void testThirdPartyContextForPropagationFailsToSerialize() throws Exception {
        Object proxy;

        ContextService contextSvcZL = InitialContext.doLookup("java:module/concurrent/ZLContextSvc");
        try {
            proxy = contextSvcZL.createContextualProxy(new SameThreadExecutor(), Serializable.class);
            fail("Must not be able to create Serializable proxy when propagated third-party context types are present. " + proxy);
        } catch (UnsupportedOperationException x) {
            // expected, but check the message for names of context that we cannot propagate
            String message = x.getMessage();
            if (message == null || !message.contains("CWWKC1204E") || !message.contains("ZipCode") || !message.contains("List"))
                throw x;
        }

        ContextService contextSvcLT = InitialContext.doLookup("java:app/concurrent/merged/web/LTContextService");
        try {
            proxy = contextSvcLT.createContextualProxy(new SameThreadExecutor(), Executor.class, Serializable.class);
            fail("Must not be able to create Serializable proxy when propagated third-party context types are present. " + proxy);
        } catch (UnsupportedOperationException x) {
            // expected, but check the message for names of context that we cannot propagate
            String message = x.getMessage();
            if (message == null || !message.contains("CWWKC1204E") || !message.contains("List") || !message.contains("Timestamp"))
                throw x;
        }

        ContextService contextSvc3P = InitialContext.doLookup("java:comp/concurrent/ThirdPartyContextService");
        try {
            proxy = contextSvc3P.createContextualProxy(new SameThreadExecutor(), Collections.emptyMap(), Ser1Executor.class);
            fail("Must not be able to create Serializable proxy when propagated third-party context types are present. " + proxy);
        } catch (UnsupportedOperationException x) {
            // expected, but check the message for names of context that we cannot propagate
            String message = x.getMessage();
            if (message == null || !message.contains("CWWKC1204E") || !message.contains(ALL_REMAINING))
                throw x;
        }

        ContextService contextSvcTZ = InitialContext.doLookup("java:comp/concurrent/dd/web/TZContextService");
        try {
            proxy = contextSvcTZ.createContextualProxy(new SameThreadExecutor(), Collections.emptyMap(), Executor.class, Ser2Executor.class);
            fail("Must not be able to create Serializable proxy when propagated third-party context types are present. " + proxy);
        } catch (UnsupportedOperationException x) {
            // expected, but check the message for names of context that we cannot propagate
            String message = x.getMessage();
            if (message == null || !message.contains("CWWKC1204E") || !message.contains("Timestamp") || !message.contains("ZipCode"))
                throw x;
        }
    }

    /**
     * Use a ContextService that is defined by a context-service in a web module deployment descriptor.
     */
    @Test
    public void testWebDDContextServiceDefinition() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:comp/concurrent/dd/web/TZContextService");

        // Put some fake context onto the thread:
        Timestamp.set();
        ZipCode.set(55906);
        ListContext.newList();
        ListContext.add(36);
        Thread.currentThread().setPriority(3);
        Long ts0 = Timestamp.get();
        TimeUnit.MILLISECONDS.sleep(100);

        UserTransaction tran = InitialContext.doLookup("java:comp/UserTransaction");
        tran.begin();
        try {

            // Contextualize a Runnable with the above context:
            Runnable task = contextSvc.contextualRunnable(() -> {
                assertEquals(ts0, Timestamp.get()); // propagated
                assertEquals(55906, ZipCode.get()); // propagated
                assertEquals("[68]", ListContext.asString()); // unchanged
                assertEquals(6, Thread.currentThread().getPriority()); // unchanged
                try {
                    assertEquals(Status.STATUS_NO_TRANSACTION, tran.getStatus()); // cleared
                    assertNotNull(InitialContext.doLookup("java:comp/concurrent/dd/web/TZContextService")); // unchanged
                } catch (NamingException | SystemException x) {
                    throw new CompletionException(x);
                }
            });

            // Alter some of the context on the current thread
            ZipCode.set(55904);
            ListContext.newList();
            ListContext.add(68);
            Thread.currentThread().setPriority(6);
            Long ts1 = Timestamp.get();

            // Run with the captured context:
            task.run();

            // Verify that context is restored on the current thread:
            assertEquals(55904, ZipCode.get());
            assertEquals("[68]", ListContext.asString());
            assertEquals(6, Thread.currentThread().getPriority());
            assertEquals(ts1, Timestamp.get());
        } finally {
            // Remove fake context
            Timestamp.clear();
            ZipCode.clear();
            ListContext.clear();
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);

            tran.rollback();
        }
    }

    /**
     * Use a ManagedExecutorService that is defined via managed-executor in a web module deployment descriptor.
     */
    @Test
    public void testWebDDManagedExecutorDefinition() throws Exception {
        BiFunction<CountDownLatch, CountDownLatch, int[]> task = (threeStarted, fourStarted) -> {
            threeStarted.countDown();
            fourStarted.countDown();
            try {
                assertTrue(fourStarted.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

                return new int[] {
                                   ZipCode.get(), // unchanged
                                   Thread.currentThread().getPriority() // propagated
                };
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        };

        ManagedExecutorService executor = InitialContext.doLookup("java:global/concurrent/dd/web/LPExecutor");

        CompletableFuture<CountDownLatch> threeStartedFuture = executor.completedFuture(new CountDownLatch(3));
        CompletableFuture<CountDownLatch> fourStartedFuture = executor.completedFuture(new CountDownLatch(4));

        try {
            Thread.currentThread().setPriority(4);
            ZipCode.set(55904);

            CompletableFuture<int[]> stage3 = threeStartedFuture.thenCombineAsync(fourStartedFuture, task);
            CompletableFuture<int[]> stage4 = threeStartedFuture.thenCombineAsync(fourStartedFuture, task);
            CompletableFuture<int[]> stage5 = threeStartedFuture.thenCombineAsync(fourStartedFuture, task);

            Thread.currentThread().setPriority(6);
            ZipCode.set(55906);

            CompletableFuture<int[]> stage6 = threeStartedFuture.thenCombineAsync(fourStartedFuture, task);

            // 3 tasks must run concurrently per <max-async>3</max-async>
            assertTrue(threeStartedFuture.join().await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // 4 tasks must not run concurrently
            assertFalse(fourStartedFuture.join().await(1, TimeUnit.SECONDS));

            // running inline is not considered async
            CompletableFuture<int[]> stage7 = threeStartedFuture.thenCombine(fourStartedFuture, task);
            int[] results = stage7.join();
            assertEquals(55906, results[0]);
            assertEquals(6, results[1]);

            results = stage6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(0, results[0]);
            assertEquals(6, results[1]);

            results = stage5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(0, results[0]);
            assertEquals(4, results[1]);

            results = stage4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(0, results[0]);
            assertEquals(4, results[1]);

            results = stage3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(0, results[0]);
            assertEquals(4, results[1]);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            ZipCode.clear();
        }
    }

    /**
     * Use a ManagedScheduledExecutorService that is defined via managed-scheduled-executor in a web module deployment descriptor.
     */
    @Test
    public void testWebDDManagedScheduledExecutorDefinition() throws Exception {
        ManagedScheduledExecutorService scheduledExecutor = InitialContext.doLookup("java:comp/concurrent/dd/web/TZScheduledExecutor");
        CompletableFuture<Exchanger<Object>> stage0 = scheduledExecutor.completedFuture(new Exchanger<Object>());

        try {
            ListContext.newList();
            ListContext.add(135);
            ZipCode.set(55901);
            Thread.currentThread().setPriority(3);

            CompletableFuture<int[]> stage1 = stage0.thenApplyAsync(exchanger -> {
                ExecutorService executor;
                try {
                    executor = (ExecutorService) exchanger.exchange("Task 1 started", TIMEOUT_NS, TimeUnit.NANOSECONDS);
                } catch (InterruptedException | TimeoutException x) {
                    throw new CompletionException(x);
                }

                Future<Integer> task2future = executor.submit(() -> 2);
                try {
                    Object result = task2future.get(1, TimeUnit.SECONDS);
                    fail("max-async=1 must prevent 2 tasks from running at the same time. Task 2 result: " + result);
                } catch (ExecutionException | InterruptedException x) {
                    throw new CompletionException(x);
                } catch (TimeoutException x) {
                    // expected
                }

                assertTrue(task2future.cancel(false));

                return new int[] {
                                   "null".equals(ListContext.asString()) ? 0 : ListContext.sum(),
                                   ZipCode.get(),
                                   Thread.currentThread().getPriority()
                };
            });

            ZipCode.set(55902);

            // wait for task1 to start running
            stage0.join().exchange(scheduledExecutor, TIMEOUT_NS, TimeUnit.NANOSECONDS);

            int[] results = stage1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(0, results[0]); // unchanged
            assertEquals(55901, results[1]); // propagated
            assertEquals(Thread.NORM_PRIORITY, results[2]); // unchanged
        } finally {
            ListContext.clear();
            ZipCode.clear();
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
    }

    /**
     * Use a ManagedThreadFactory that is defined by managed-thread-factory in a web module deployment descriptor.
     */
    @Test
    public void testWebDDManagedThreadFactoryDefinition() throws Exception {
        try {
            try {
                Timestamp.set();
                Long ts0 = Timestamp.get();
                TimeUnit.MILLISECONDS.sleep(100);
                ListContext.newList();
                ListContext.add(25);
                Thread.currentThread().setPriority(6);
                ZipCode.set(55906);

                ManagedThreadFactory threadFactory = InitialContext.doLookup("java:comp/concurrent/dd/web/TZThreadFactory");

                LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();

                ZipCode.set(55904);

                threadFactory.newThread(() -> {
                    results.add(ListContext.asString()); // unchanged
                    results.add(Thread.currentThread().getPriority()); // unchanged

                    Long timestamp = Timestamp.get(); // propagated
                    results.add(timestamp == null ? "none" : timestamp);

                    results.add(ZipCode.get()); // propagated
                }).start();

                // Verify that custom thread context type ListContext is left unchanged
                Object listStr = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertEquals("null", listStr);

                // Verify that priority from the managed-thread-factory is used,
                Object priority = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertEquals(Integer.valueOf(10), priority);

                // Verify that custom thread context type Timestamp is propagated
                Object timestamp = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertEquals(ts0, timestamp);

                // Verify that custom thread context type ZipCode is propagated
                Object zipcode = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertEquals(Integer.valueOf(55906), zipcode);
            } finally {
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                ListContext.clear();
                Timestamp.clear();
                ZipCode.clear();
            }
        } catch (InterruptedException | NamingException x) {
            throw new EJBException(x);
        }
    }

    /**
     * Use a ContextService that is defined by the merging of a ContextServiceDefinition
     * and a context-service in a web module deployment descriptor, which replaces the
     * cleared and propagated context types.
     */
    @Test
    public void testWebMergedContextServiceDefinitionClearedAndPropagated() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:module/concurrent/merged/web/ZContextService");

        // Put some fake context onto the thread:
        Timestamp.set();
        ZipCode.set(55901);
        ListContext.newList();
        ListContext.add(109);
        Thread.currentThread().setPriority(9);
        Long ts0 = Timestamp.get();
        TimeUnit.MILLISECONDS.sleep(100);

        UserTransaction tran = InitialContext.doLookup("java:comp/UserTransaction");
        tran.begin();
        try {
            // Contextualize a BiConsumer with the above context:
            BiConsumer<Number[], String[]> task = contextSvc.contextualConsumer((n, s) -> {
                s[0] = ListContext.asString();
                n[0] = Thread.currentThread().getPriority();
                n[1] = Timestamp.get();
                n[2] = ZipCode.get();
                try {
                    n[3] = tran.getStatus();
                } catch (SystemException x) {
                    throw new CompletionException(x);
                }
            });

            // Alter some of the context on the current thread
            ZipCode.set(55902);
            ListContext.newList();
            ListContext.add(209);
            Thread.currentThread().setPriority(2);
            Long ts1 = Timestamp.get();

            // Run with the captured context:
            Number[] numericResults = new Number[4];
            String[] stringResults = new String[1];
            task.accept(numericResults, stringResults);

            assertEquals("[209]", stringResults[0]); // unchanged
            assertEquals(2, numericResults[0]); // unchanged
            assertEquals(ts1, numericResults[1]); // unchanged
            assertEquals(Integer.valueOf(55901), numericResults[2]); // propagated
            assertEquals(Integer.valueOf(Status.STATUS_NO_TRANSACTION), numericResults[3]); // cleared

            // Verify that context is restored on the current thread:
            assertEquals("[209]", ListContext.asString());
            assertEquals(2, Thread.currentThread().getPriority());
            assertEquals(ts1, Timestamp.get());
            assertEquals(55902, ZipCode.get());
            assertEquals(Status.STATUS_ACTIVE, tran.getStatus());
        } finally {
            // Remove fake context
            Timestamp.clear();
            ZipCode.clear();
            ListContext.clear();
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);

            tran.rollback();
        }
    }

    /**
     * Use a ContextService that is defined by the merging of a ContextServiceDefinition
     * and a context-service in a web module deployment descriptor, which replaces the
     * cleared and unchanged context types.
     */
    @Test
    public void testWebMergedContextServiceDefinitionClearedAndUnchanged() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:app/concurrent/merged/web/LTContextService");

        // Put some fake context onto the thread:
        Timestamp.set();
        ZipCode.set(55902);
        ListContext.newList();
        ListContext.add(22);
        Thread.currentThread().setPriority(2);
        Long ts0 = Timestamp.get();
        TimeUnit.MILLISECONDS.sleep(100);

        UserTransaction tran = InitialContext.doLookup("java:comp/UserTransaction");
        tran.begin();
        try {
            // Contextualize a Consumer with the above context:
            Consumer<Map<String, Object>> task = contextSvc.contextualConsumer(results -> {
                results.put(ListContext.CONTEXT_NAME, ListContext.asString());
                results.put("Priority", Thread.currentThread().getPriority());
                results.put(Timestamp.CONTEXT_NAME, Timestamp.get());
                results.put(ZipCode.CONTEXT_NAME, ZipCode.get());
                try {
                    results.put(APPLICATION, InitialContext.doLookup("java:app/concurrent/merged/web/LTContextService"));
                    results.put(TRANSACTION, tran.getStatus());
                } catch (NamingException | SystemException x) {
                    throw new CompletionException(x);
                }
            });

            // Alter some of the context on the current thread
            ZipCode.set(55904);
            ListContext.newList();
            ListContext.add(44);
            Thread.currentThread().setPriority(4);
            Long ts1 = Timestamp.get();

            // Run with the captured context:
            Map<String, Object> results = new TreeMap<String, Object>();
            task.accept(results);

            assertEquals("[22]", results.get(ListContext.CONTEXT_NAME)); // propagated
            assertEquals(Integer.valueOf(4), results.get("Priority")); // unchanged
            assertEquals(ts0, results.get(Timestamp.CONTEXT_NAME)); // propagated
            assertEquals(Integer.valueOf(55904), results.get(ZipCode.CONTEXT_NAME)); // unchanged
            assertNotNull(results.get(APPLICATION)); // unchanged
            assertEquals(Integer.valueOf(Status.STATUS_ACTIVE), results.get(TRANSACTION)); // unchanged

            // Verify that context is restored on the current thread:
            assertEquals(55904, ZipCode.get());
            assertEquals("[44]", ListContext.asString());
            assertEquals(4, Thread.currentThread().getPriority());
            assertEquals(ts1, Timestamp.get());
            assertEquals(Status.STATUS_ACTIVE, tran.getStatus());
        } finally {
            // Remove fake context
            Timestamp.clear();
            ZipCode.clear();
            ListContext.clear();
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);

            tran.rollback();
        }
    }

    /**
     * Use a ContextService that is defined by the merging of a ContextServiceDefinition
     * and a context-service in a web module deployment descriptor, which replaces the
     * propagated context types.
     */
    @Test
    public void testWebMergedContextServiceDefinitionPropagated() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:comp/concurrent/merged/web/PTContextService");

        UserTransaction tran = InitialContext.doLookup("java:comp/UserTransaction");
        tran.begin();
        try {
            // Put some fake context onto the thread:
            Timestamp.set();
            ZipCode.set(55906);
            ListContext.newList();
            ListContext.add(60);
            Thread.currentThread().setPriority(9);
            Long ts0 = Timestamp.get();
            TimeUnit.MILLISECONDS.sleep(100);

            // Contextualize a Function with the above context:
            Function<String, Map<String, Object>> task = contextSvc.contextualFunction(jndiName -> {
                Map<String, Object> results = new TreeMap<String, Object>();
                results.put(ListContext.CONTEXT_NAME, ListContext.asString());
                results.put("Priority", Thread.currentThread().getPriority());
                results.put(Timestamp.CONTEXT_NAME, Timestamp.get());
                results.put(ZipCode.CONTEXT_NAME, ZipCode.get());
                try {
                    results.put(APPLICATION, InitialContext.doLookup(jndiName));

                    TransactionSynchronizationRegistry tsr = InitialContext.doLookup("java:comp/TransactionSynchronizationRegistry");
                    results.put(TRANSACTION, tsr.getTransactionStatus());
                } catch (NamingException x) {
                    throw new CompletionException(x);
                }
                return results;
            });

            // Alter some of the context on the current thread
            ZipCode.set(55905);
            ListContext.newList();
            ListContext.add(50);
            Thread.currentThread().setPriority(8);
            Long ts1 = Timestamp.get();

            // Run with the captured context:
            Map<String, Object> results = task.apply("java:comp/concurrent/merged/web/PTContextService");

            assertEquals("[]", results.get(ListContext.CONTEXT_NAME)); // cleared
            assertEquals(Integer.valueOf(9), results.get("Priority")); // propagated
            assertEquals(ts0, results.get(Timestamp.CONTEXT_NAME)); // propagated
            assertEquals(Integer.valueOf(55905), results.get(ZipCode.CONTEXT_NAME)); // unchanged
            assertNotNull(results.get(APPLICATION)); // unchanged
            assertEquals(Integer.valueOf(Status.STATUS_ACTIVE), results.get(TRANSACTION)); // unchanged

            // Verify that context is restored on the current thread:
            assertEquals(55905, ZipCode.get());
            assertEquals("[50]", ListContext.asString());
            assertEquals(8, Thread.currentThread().getPriority());
            assertEquals(ts1, Timestamp.get());
            // UserTransaction must still be active,
            TransactionSynchronizationRegistry tsr = InitialContext.doLookup("java:comp/TransactionSynchronizationRegistry");
            assertEquals(Status.STATUS_ACTIVE, tsr.getTransactionStatus());
        } finally {
            // Remove fake context
            Timestamp.clear();
            ZipCode.clear();
            ListContext.clear();
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);

            tran.rollback();
        }
    }

    /**
     * Use a ManagedExecutorService that is defined by the merging of a ManagedExecutorDefinition
     * and a managed-executor in a web module deployment descriptor, which replaces the
     * maxAsync that it uses.
     */
    @Test
    public void testWebMergedManagedExecutorDefinitionAsync() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:module/concurrent/merged/web/ZLExecutor");

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch threeRunning = new CountDownLatch(3);
        CountDownLatch fourRunning = new CountDownLatch(4);

        Supplier<Object[]> task = () -> {
            threeRunning.countDown();
            fourRunning.countDown();
            try {
                blocker.await(TIMEOUT_NS * 3, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
            return new Object[] { Thread.currentThread().getPriority(), ZipCode.get() };
        };

        try {
            // Put some fake context onto the thread:
            Thread.currentThread().setPriority(6);
            ZipCode.set(55901);

            CompletableFuture<Object[]> future1 = executor.supplyAsync(task);

            ZipCode.set(55902);

            CompletableFuture<Object[]> future2 = executor.supplyAsync(task);

            CompletableFuture<Object[]> future3 = executor.supplyAsync(task);

            ZipCode.set(55904);

            CompletableFuture<Object[]> future4 = executor.supplyAsync(task);

            ZipCode.set(55906);

            assertTrue(threeRunning.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertFalse(fourRunning.await(1, TimeUnit.SECONDS)); // maxAsync = 3

            blocker.countDown();

            Object[] results;
            results = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Integer.valueOf(Thread.NORM_PRIORITY), results[0]); // must be left unchanged
            assertEquals(Integer.valueOf(55901), results[1]); // must be propagated

            results = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Integer.valueOf(Thread.NORM_PRIORITY), results[0]); // must be left unchanged
            assertEquals(Integer.valueOf(55902), results[1]); // must be propagated

            results = future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Integer.valueOf(Thread.NORM_PRIORITY), results[0]); // must be left unchanged
            assertEquals(Integer.valueOf(55902), results[1]); // must be propagated

            results = future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Integer.valueOf(Thread.NORM_PRIORITY), results[0]); // must be left unchanged
            assertEquals(Integer.valueOf(55904), results[1]); // must be propagated
        } finally {
            // Remove fake context
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            ZipCode.clear();
        }
    }

    /**
     * Use a ManagedExecutorService that is defined by the merging of a ManagedExecutorDefinition
     * and a managed-executor in a web module deployment descriptor, which replaces the
     * context service that it uses.
     */
    @Test
    public void testWebMergedManagedExecutorDefinitionContext() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:comp/concurrent/merged/web/ZPExecutor");
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch twoRunning = new CountDownLatch(2);
        CountDownLatch threeRunning = new CountDownLatch(3);

        Supplier<Object[]> task = () -> {
            twoRunning.countDown();
            threeRunning.countDown();
            try {
                blocker.await(TIMEOUT_NS * 3, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
            return new Object[] { Timestamp.get(), Thread.currentThread().getPriority() };
        };

        try {
            // Put some fake context onto the thread:
            Thread.currentThread().setPriority(8);
            Timestamp.set();

            CompletableFuture<Object[]> future1 = executor.supplyAsync(task);

            Thread.currentThread().setPriority(7);

            CompletableFuture<Object[]> future2 = executor.supplyAsync(task);

            Thread.currentThread().setPriority(6);

            CompletableFuture<Object[]> future3 = executor.supplyAsync(task);

            Thread.currentThread().setPriority(5);

            assertTrue(twoRunning.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertFalse(threeRunning.await(1, TimeUnit.SECONDS)); // maxAsync = 2

            blocker.countDown();

            Object[] results;
            results = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(null, results[0]); // must be cleared by java:app/concurrent/dd/ZPContextService
            assertEquals(Integer.valueOf(8), results[1]); // must be propagated by java:app/concurrent/dd/ZPContextService

            results = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(null, results[0]); // must be cleared by java:app/concurrent/dd/ZPContextService
            assertEquals(Integer.valueOf(7), results[1]); // must be propagated by java:app/concurrent/dd/ZPContextService

            results = future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(null, results[0]); // must be cleared by java:app/concurrent/dd/ZPContextService
            assertEquals(Integer.valueOf(6), results[1]); // must be propagated by java:app/concurrent/dd/ZPContextService
        } finally {
            // Remove fake context
            Timestamp.clear();
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
    }

    /**
     * Use a ManagedScheduledExecutorService that is defined by the merging of a ManagedScheduledExecutorDefinition
     * and a managed-scheduled-executor in a web module deployment descriptor, which replaces the
     * maxAsync that it uses.
     */
    @Test
    public void testWebMergedManagedScheduledExecutorDefinitionAsync() throws Exception {
        ManagedScheduledExecutorService executor = InitialContext.doLookup("java:module/concurrent/merged/web/ZLScheduledExecutor");

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch oneRunning = new CountDownLatch(1);
        CountDownLatch twoRunning = new CountDownLatch(2);

        Consumer<LinkedBlockingQueue<int[]>> task = queue -> {
            oneRunning.countDown();
            twoRunning.countDown();
            try {
                assertTrue(blocker.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS));
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
            queue.add(new int[] { Thread.currentThread().getPriority(), ZipCode.get() });
        };

        LinkedBlockingQueue<int[]> queue = new LinkedBlockingQueue<int[]>();
        CompletionStage<LinkedBlockingQueue<int[]>> stage0 = executor.completedStage(queue);

        try {
            // Put some fake context onto the thread:
            Thread.currentThread().setPriority(4);
            ZipCode.set(55902);

            stage0.thenAcceptAsync(task);
            stage0.thenAcceptAsync(task);

            assertTrue(oneRunning.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertFalse(twoRunning.await(1, TimeUnit.SECONDS)); // max-async = 1

            blocker.countDown();

            int[] results;
            assertNotNull(results = queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(Thread.NORM_PRIORITY, results[0]); // Priority context must be cleared
            assertEquals(55902, results[1]); // ZipCode context must be propagated

            assertNotNull(results = queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(Thread.NORM_PRIORITY, results[0]); // Priority context must be cleared
            assertEquals(55902, results[1]); // ZipCode context must be propagated
        } finally {
            // Remove fake context
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            ZipCode.clear();
        }
    }

    /**
     * Use a ManagedScheduledExecutorService that is defined by the merging of a ManagedScheduledExecutorDefinition
     * and a managed-scheduled-executor in a web module deployment descriptor, which replaces the
     * context service that it uses.
     */
    @Test
    public void testWebMergedManagedScheduledExecutorDefinitionContext() throws Exception {
        ManagedScheduledExecutorService executor = InitialContext.doLookup("java:app/concurrent/merged/web/LPScheduledExecutor");

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch fourRunning = new CountDownLatch(4);
        CountDownLatch fiveRunning = new CountDownLatch(5);

        Supplier<Object[]> task = () -> {
            fourRunning.countDown();
            fiveRunning.countDown();
            try {
                blocker.await(TIMEOUT_NS * 3, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
            return new Object[] { ZipCode.get(), Thread.currentThread().getPriority() };
        };

        try {
            // Put some fake context onto the thread:
            Thread.currentThread().setPriority(1);
            ZipCode.set(55906);

            CompletableFuture<Object[]> future1 = executor.supplyAsync(task);

            Thread.currentThread().setPriority(2);

            CompletableFuture<Object[]> future2 = executor.supplyAsync(task);

            Thread.currentThread().setPriority(3);

            CompletableFuture<Object[]> future3 = executor.supplyAsync(task);

            Thread.currentThread().setPriority(4);

            CompletableFuture<Object[]> future4 = executor.supplyAsync(task);

            Thread.currentThread().setPriority(6);

            CompletableFuture<Object[]> future5 = executor.supplyAsync(task);

            assertTrue(fourRunning.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertFalse(fiveRunning.await(1, TimeUnit.SECONDS)); // maxAsync = 4

            blocker.countDown();

            Object[] results;
            results = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Integer.valueOf(0), results[0]); // ZipCode context must be left unchanged
            assertEquals(Integer.valueOf(1), results[1]); // Priority context must be propagated

            results = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Integer.valueOf(0), results[0]); // ZipCode context must be left unchanged
            assertEquals(Integer.valueOf(2), results[1]); // Priority context must be propagated

            results = future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Integer.valueOf(0), results[0]); // ZipCode context must be left unchanged
            assertEquals(Integer.valueOf(3), results[1]); // Priority context must be propagated

            results = future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Integer.valueOf(0), results[0]); // ZipCode context must be left unchanged
            assertEquals(Integer.valueOf(4), results[1]); // Priority context must be propagated

            results = future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(Integer.valueOf(0), results[0]); // ZipCode context must be left unchanged
            assertEquals(Integer.valueOf(6), results[1]); // Priority context must be propagated
        } finally {
            // Remove fake context
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            ZipCode.clear();
        }
    }

    /**
     * Use a ManagedThreadFactory that is defined by the merging of a ManagedThreadFactoryDefinition
     * and a managed-thread-factory in a web module deployment descriptor, which replaces the
     * context service that it uses.
     */
    @Test
    public void testWebMergedManagedThreadFactoryDefinitionContext() throws Exception {
        try {
            // Put some fake context onto the thread:
            ListContext.newList();
            ListContext.add(333);
            Thread.currentThread().setPriority(4);
            Timestamp.set();
            Long ts0 = Timestamp.get();

            ManagedThreadFactory threadFactory = InitialContext.doLookup("java:comp/concurrent/merged/web/TZThreadFactory");

            TimeUnit.MILLISECONDS.sleep(100);
            Timestamp.set();

            LinkedBlockingQueue<Number> queue = new LinkedBlockingQueue<>();

            threadFactory.newThread(() -> {
                queue.add("null".equals(ListContext.asString()) ? 0 : ListContext.sum());
                queue.add(Thread.currentThread().getPriority());
                queue.add(Timestamp.get());
            }).start();

            assertEquals(Integer.valueOf(0), queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // List context must remain unchanged
            assertEquals(Integer.valueOf(3), queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // configured priority must be used
            assertEquals(ts0, queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // Timestamp context must be propagated
        } finally {
            // Remove fake context
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            ListContext.clear();
            Timestamp.clear();
        }
    }

    /**
     * Use a ManagedThreadFactory that is defined by the merging of a ManagedThreadFactoryDefinition
     * and a managed-thread-factory in a web module deployment descriptor, which replaces the
     * priority.
     */
    @Test
    public void testWebMergedManagedThreadFactoryDefinitionPriority() throws Exception {
        try {
            // Put some fake context onto the thread:
            ListContext.newList();
            ListContext.add(88);
            Thread.currentThread().setPriority(4);
            ZipCode.set(55904);

            ManagedThreadFactory threadFactory = InitialContext.doLookup("java:app/concurrent/merged/web/LTThreadFactory");

            LinkedBlockingQueue<Number> queue = new LinkedBlockingQueue<>();

            threadFactory.newThread(() -> {
                queue.add("null".equals(ListContext.asString()) ? 0 : ListContext.sum());
                queue.add(Thread.currentThread().getPriority());
                queue.add(ZipCode.get());
            }).start();

            assertEquals(Integer.valueOf(88), queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // List context must be propagated
            assertEquals(Integer.valueOf(8), queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // configured priority must be used
            assertEquals(Integer.valueOf(0), queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // ZipCode context must remain unchanged
        } finally {
            // Remove fake context
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            ListContext.clear();
            ZipCode.clear();
        }
    }
}
