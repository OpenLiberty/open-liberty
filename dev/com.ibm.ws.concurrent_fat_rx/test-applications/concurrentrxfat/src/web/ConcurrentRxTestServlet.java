/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.concurrent.rx.ManagedCompletableFuture;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ConcurrentRxTestServlet")
public class ConcurrentRxTestServlet extends FATServlet {
    // Maximum number of nanoseconds to wait for a task to complete
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource(name = "java:comp/env/executorRef")
    private ManagedExecutorService defaultManagedExecutor;

    @Resource(name = "java:module/noContextExecutorRef", lookup = "concurrent/noContextExecutor")
    private ManagedScheduledExecutorService noContextExecutor;

    // Executor that can be used when tests don't want to tie up threads from the Liberty global thread pool to perform concurrent test logic
    private ExecutorService testThreads;

    @Override
    public void destroy() {
        testThreads.shutdownNow();
    }

    @Override
    public void init(ServletConfig config) {
        testThreads = Executors.newFixedThreadPool(20);
    }

    /**
     * Verify that acceptEither only requires one of the stages to complete, and that it runs with the context of the thread that
     * creates the stage.
     */
    @Test
    public void testAcceptEither() throws Exception {
        CountDownLatch blocker1 = new CountDownLatch(1);
        CountDownLatch blocker2 = new CountDownLatch(1);

        try {
            ManagedCompletableFuture<Boolean> cf1 = ManagedCompletableFuture.supplyAsync(() -> {
                System.out.println("> supplyAsync[1] from testAcceptEither");
                try {
                    boolean result = blocker1.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
                    System.out.println("< supplyAsync[1] " + result);
                    return result;
                } catch (InterruptedException x) {
                    System.out.println("< supplyAsync[1] " + x);
                    throw new CompletionException(x);
                }
            });

            CompletableFuture<Boolean> cf2 = CompletableFuture.supplyAsync(() -> {
                System.out.println("> supplyAsync[2] from testAcceptEither");
                try {
                    boolean result = blocker2.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
                    System.out.println("< supplyAsync[2] " + result);
                    return result;
                } catch (InterruptedException x) {
                    System.out.println("< supplyAsync[2] " + x);
                    throw new CompletionException(x);
                }
            });

            LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
            ManagedCompletableFuture<Void> cf3 = cf1.acceptEither(cf2, (b) -> {
                System.out.println("> lookup from testAcceptEither");
                results.add(b);
                results.add(Thread.currentThread().getName());
                try {
                    ManagedExecutorService result = InitialContext.doLookup("java:module/noContextExecutorRef");
                    results.add(result);
                    System.out.println("< lookup: " + result);
                } catch (NamingException x) {
                    System.out.println("< lookup failed");
                    x.printStackTrace(System.out);
                    throw new CompletionException(x);
                }
            });

            assertFalse(cf3.isDone());
            try {
                Object result = cf3.get(100, TimeUnit.MILLISECONDS);
                fail("Dependent completion stage must not complete first");
            } catch (TimeoutException x) {
            }

            // Allow cf2 to complete
            blocker2.countDown();

            // Dependent stage must be able to complete now
            assertNull(cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(cf3.isDone());
            assertFalse(cf3.isCancelled());
            assertFalse(cf3.isCompletedExceptionally());

            // Verify the parameter that is supplied to acceptEither's consumer
            assertEquals(Boolean.TRUE, results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // acceptEither runs on the unmanaged thread of the stage that completed
            String threadName;
            assertNotNull(threadName = (String) results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(threadName, !threadName.startsWith(("Default Executor-thread-")));

            // thread context is made available to acceptEither's consumer per the managed executor which is the default asynchronous execution facility,
            // enabling java:module lookup to succeed from the unmanaged thread.
            Object lookupResult;
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(noContextExecutor, lookupResult);

            // allow cf1 to complete
            blocker1.countDown();
            assertEquals(Boolean.TRUE, cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            // allow threads to complete in case test fails
            blocker1.countDown();
            blocker2.countDown();
        }
    }

    /**
     * Verify that acceptEitherAsync only requires one of the stages to complete, and that it runs with the context of the thread that
     * creates the stage, as captured and propagated by the default asynchronous execution facility.
     */
    @Test
    public void testAcceptEitherAsync() throws Exception {
        CountDownLatch blocker1 = new CountDownLatch(1);
        CountDownLatch blocker2 = new CountDownLatch(1);

        try {
            CompletableFuture<Boolean> cf1 = ManagedCompletableFuture.supplyAsync(() -> {
                System.out.println("> supplyAsync[1] from testAcceptEitherAsync");
                try {
                    boolean result = blocker1.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
                    System.out.println("< supplyAsync[1] " + result);
                    return result;
                } catch (InterruptedException x) {
                    System.out.println("< supplyAsync[1] " + x);
                    throw new CompletionException(x);
                }
            });

            CompletableFuture<Boolean> cf2 = CompletableFuture.supplyAsync(() -> {
                System.out.println("> supplyAsync[2] from testAcceptEitherAsync");
                try {
                    boolean result = blocker2.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
                    System.out.println("< supplyAsync[2] " + result);
                    return result;
                } catch (InterruptedException x) {
                    System.out.println("< supplyAsync[2] " + x);
                    throw new CompletionException(x);
                }
            });

            LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
            CompletableFuture<Void> cf3 = cf1.acceptEitherAsync(cf2, (b) -> {
                System.out.println("> lookup from testAcceptEitherAsyncOnExecutor");
                results.add(b);
                results.add(Thread.currentThread().getName());
                try {
                    ManagedExecutorService result = InitialContext.doLookup("java:module/noContextExecutorRef");
                    results.add(result);
                    System.out.println("< lookup: " + result);
                } catch (NamingException x) {
                    System.out.println("< lookup failed");
                    x.printStackTrace(System.out);
                    throw new CompletionException(x);
                }
            });

            assertFalse(cf3.isDone());
            try {
                Object result = cf3.get(100, TimeUnit.MILLISECONDS);
                fail("Dependent completion stage must not complete first");
            } catch (TimeoutException x) {
            }

            // Allow cf1 to complete
            blocker1.countDown();

            // Dependent stage must be able to complete now
            assertNull(cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(cf3.isDone());
            assertFalse(cf3.isCancelled());
            assertFalse(cf3.isCompletedExceptionally());

            // Verify the parameter that is supplied to acceptEither's consumer
            assertEquals(Boolean.TRUE, results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // acceptEither runs on the Liberty global thread pool
            String threadName;
            assertNotNull(threadName = (String) results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(threadName, threadName.startsWith(("Default Executor-thread-")));

            // thread context is made available to acceptEither's consumer per the supplied managed executor, enabling java:module lookup to succeed
            Object lookupResult;
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(noContextExecutor, lookupResult);

            // allow cf2 to complete
            blocker2.countDown();
            assertEquals(Boolean.TRUE, cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            // allow threads to complete in case test fails
            blocker2.countDown();
            blocker1.countDown();
        }
    }

    /**
     * Verify that acceptEitherAsync only requires one of the stages to complete, and that it runs with the context of the thread that
     * creates the stage, as captured and propagated by the specified executor.
     */
    @Test
    public void testAcceptEitherAsyncOnExecutor() throws Exception {
        CountDownLatch blocker1 = new CountDownLatch(1);
        CountDownLatch blocker2 = new CountDownLatch(1);

        try {
            CompletableFuture<Boolean> cf1 = ManagedCompletableFuture.supplyAsync(() -> {
                System.out.println("> supplyAsync[1] from testAcceptEitherAsyncOnExecutor");
                try {
                    boolean result = blocker1.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
                    System.out.println("< supplyAsync[1] " + result);
                    return result;
                } catch (InterruptedException x) {
                    System.out.println("< supplyAsync[1] " + x);
                    throw new CompletionException(x);
                }
            }, noContextExecutor);

            CompletableFuture<Boolean> cf2 = CompletableFuture.supplyAsync(() -> {
                System.out.println("> supplyAsync[2] from testAcceptEitherAsyncOnExecutor");
                try {
                    boolean result = blocker2.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
                    System.out.println("< supplyAsync[2] " + result);
                    return result;
                } catch (InterruptedException x) {
                    System.out.println("< supplyAsync[2] " + x);
                    throw new CompletionException(x);
                }
            });

            LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
            CompletableFuture<Void> cf3 = cf1.acceptEitherAsync(cf2, (b) -> {
                System.out.println("> lookup from testAcceptEitherAsyncOnExecutor");
                results.add(b);
                results.add(Thread.currentThread().getName());
                try {
                    ManagedExecutorService result = InitialContext.doLookup("java:module/noContextExecutorRef");
                    results.add(result);
                    System.out.println("< lookup: " + result);
                } catch (NamingException x) {
                    System.out.println("< lookup failed");
                    x.printStackTrace(System.out);
                    throw new CompletionException(x);
                }
            }, defaultManagedExecutor);

            assertFalse(cf3.isDone());
            try {
                Object result = cf3.get(100, TimeUnit.MILLISECONDS);
                fail("Dependent completion stage must not complete first");
            } catch (TimeoutException x) {
            }

            // Allow cf2 to complete
            blocker2.countDown();

            // Dependent stage must be able to complete now
            assertNull(cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(cf3.isDone());
            assertFalse(cf3.isCancelled());
            assertFalse(cf3.isCompletedExceptionally());

            // Verify the parameter that is supplied to acceptEither's consumer
            assertEquals(Boolean.TRUE, results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // acceptEither runs on the Liberty global thread pool
            String threadName;
            assertNotNull(threadName = (String) results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(threadName, threadName.startsWith(("Default Executor-thread-")));

            // thread context is made available to acceptEither's consumer per the supplied managed executor, enabling java:module lookup to succeed
            Object lookupResult;
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(noContextExecutor, lookupResult);

            // allow cf1 to complete
            blocker1.countDown();
            assertEquals(Boolean.TRUE, cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            // allow threads to complete in case test fails
            blocker2.countDown();
            blocker1.countDown();
        }
    }

    /**
     * Verify that CompletableFuture.allOf properly identifies exceptional completion of a managed completable future.
     */
    @Test
    public void testAllOf_ExceptionalResult() throws Exception {
        // Managed completable future with non-null result:
        CompletableFuture<Object> cf1 = ManagedCompletableFuture.supplyAsync(() -> {
            System.out.println("> supply from testAllOf_ExceptionalResult");
            System.out.println("< supply ArrayIndexOutOfBoundsException (intentional failure)");
            throw new ArrayIndexOutOfBoundsException("Intentionally caused failure in order to test exceptional completion");
        }, defaultManagedExecutor);

        assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);

        CompletableFuture<Void> cf2 = CompletableFuture.allOf(cf1);
        try {
            Object result = cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Should have raised ExecutionException. Instead: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArrayIndexOutOfBoundsException))
                throw new Exception("Unexpected cause of ExecutionException. See exception chain.", x);
        }
        assertTrue(cf2.isDone());
        assertTrue(cf2.isCompletedExceptionally());
        assertTrue(cf1.isDone());
        assertTrue(cf1.isCompletedExceptionally());
    }

    /**
     * Verify that CompletableFuture.allOf properly identifies completion of a managed completable future with a non-null result.
     */
    @Test
    public void testAllOf_NonNullResult() throws Exception {
        // Managed completable future with non-null result:
        CompletableFuture<ManagedExecutorService> cf1 = ManagedCompletableFuture.supplyAsync(() -> {
            System.out.println("> supply from testAllOf_NonNullResult");
            try {
                ManagedExecutorService result = InitialContext.doLookup("java:comp/env/executorRef");
                System.out.println("< supply " + result);
                return result;
            } catch (NamingException x) {
                System.out.println("< supply raised exception");
                x.printStackTrace(System.out);
                throw new CompletionException(x);
            }
        }, defaultManagedExecutor);

        assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);

        CompletableFuture<Void> cf2 = CompletableFuture.allOf(cf1);
        assertNull(cf2.join());
        assertTrue(cf2.isDone());
        assertFalse(cf2.isCompletedExceptionally());
        assertTrue(cf1.isDone());
        assertFalse(cf1.isCompletedExceptionally());
        assertEquals(defaultManagedExecutor, cf1.get());
    }

    /**
     * Verify that CompletableFuture.allOf properly identifies completion of a managed completable future with a null result.
     */
    @Test
    public void testAllOf_NullResult() throws Exception {
        // Managed completable future with non-null result:
        CompletableFuture<Object> cf1 = ManagedCompletableFuture.supplyAsync(() -> {
            System.out.println("> supply from testAllOf_NullResult");
            System.out.println("< supply: null");
            return null;
        }, defaultManagedExecutor);

        assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);

        CompletableFuture<Void> cf2 = CompletableFuture.allOf(cf1);
        assertNull(cf2.join());
        assertTrue(cf2.isDone());
        assertFalse(cf2.isCompletedExceptionally());
        assertTrue(cf1.isDone());
        assertFalse(cf1.isCompletedExceptionally());
        assertNull(cf1.get());
    }

    /**
     * Verify that CompletableFuture.anyOf properly identifies exceptional completion of a managed completable future.
     */
    @Test
    public void testAnyOf_ExceptionalResult() throws Exception {
        // Managed completable future with non-null result:
        CompletableFuture<Object> cf1 = ManagedCompletableFuture.supplyAsync(() -> {
            System.out.println("> supply from testAnyOf_ExceptionalResult");
            System.out.println("< supply ArrayIndexOutOfBoundsException (intentional failure)");
            throw new ArrayIndexOutOfBoundsException("Intentionally caused failure in order to test exceptional completion");
        }, defaultManagedExecutor);

        assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);

        CompletableFuture<Object> cf2 = CompletableFuture.anyOf(cf1);
        try {
            Object result = cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Should have raised ExecutionException. Instead: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArrayIndexOutOfBoundsException))
                throw new Exception("Unexpected cause of ExecutionException. See exception chain.", x);
        }
        assertTrue(cf2.isDone());
        assertTrue(cf2.isCompletedExceptionally());
        assertTrue(cf1.isDone());
        assertTrue(cf1.isCompletedExceptionally());
    }

    /**
     * Verify that CompletableFuture.anyOf properly identifies completion of a managed completable future with a non-null result.
     */
    @Test
    public void testAnyOf_NonNullResult() throws Exception {
        // Managed completable future with non-null result:
        CompletableFuture<ManagedExecutorService> cf1 = ManagedCompletableFuture.supplyAsync(() -> {
            System.out.println("> supply from testAnyOf_NonNullResult");
            try {
                ManagedExecutorService result = InitialContext.doLookup("java:comp/env/executorRef");
                System.out.println("< supply " + result);
                return result;
            } catch (NamingException x) {
                System.out.println("< supply raised exception");
                x.printStackTrace(System.out);
                throw new CompletionException(x);
            }
        }, defaultManagedExecutor);

        assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);

        CompletableFuture<Object> cf2 = CompletableFuture.anyOf(cf1);
        assertEquals(defaultManagedExecutor, cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(cf2.isDone());
        assertFalse(cf2.isCompletedExceptionally());
        assertTrue(cf1.isDone());
        assertFalse(cf1.isCompletedExceptionally());
    }

    /**
     * Verify that CompletableFuture.anyOf properly identifies completion of a managed completable future with a null result.
     */
    @Test
    public void testAnyOf_NullResult() throws Exception {
        // Managed completable future with non-null result:
        CompletableFuture<Object> cf1 = ManagedCompletableFuture.supplyAsync(() -> {
            System.out.println("> supply from testAnyOf_NullResult");
            System.out.println("< supply: null");
            return null;
        }, defaultManagedExecutor);

        assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);

        CompletableFuture<Object> cf2 = CompletableFuture.anyOf(cf1);
        assertNull(cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(cf2.isDone());
        assertFalse(cf2.isCompletedExceptionally());
        assertTrue(cf1.isDone());
        assertFalse(cf1.isCompletedExceptionally());
    }

    /**
     * Verify applyToEither and both forms of applyToEitherAsync.
     */
    @Test
    public void testApplyToEither() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();

        Function<Integer, Integer> increment = (count) -> {
            System.out.println("> increment #" + (++count) + " from testApplyToEither");
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            System.out.println("< increment");
            return count;
        };

        CountDownLatch blocker1 = new CountDownLatch(1);
        Supplier<Integer> awaitBlocker1 = () -> {
            System.out.println("> awaitBlocker1 from testApplyToEither");
            try {
                int result = blocker1.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS) ? 100 : 1000;
                System.out.println("< awaitBlocker1: " + result);
                return result;
            } catch (InterruptedException x) {
                System.out.println("< awaitBlocker1: " + x);
                throw new CompletionException(x);
            }
        };

        CountDownLatch blocker2 = new CountDownLatch(1);
        Supplier<Integer> awaitBlocker2 = () -> {
            System.out.println("> awaitBlocker2 from testApplyToEither");
            try {
                int result = blocker2.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS) ? 200 : 2000;
                System.out.println("< awaitBlocker2: " + result);
                return result;
            } catch (InterruptedException x) {
                System.out.println("< awaitBlocker2: " + x);
                throw new CompletionException(x);
            }
        };

        try {
            CompletableFuture<Integer> cf1 = ManagedCompletableFuture.supplyAsync(awaitBlocker1, defaultManagedExecutor);

            CompletableFuture<Integer> cf2 = ManagedCompletableFuture.supplyAsync(awaitBlocker2, noContextExecutor);

            CompletableFuture<Integer> cf3 = cf1.applyToEitherAsync(cf2, increment);

            CompletableFuture<Integer> cf4 = cf3.applyToEitherAsync(cf2, increment, testThreads);

            CompletableFuture<Integer> cf5 = cf4.applyToEither(cf2, increment);

            CompletableFuture<Integer> cf6 = cf2.applyToEitherAsync(cf5, increment);

            String threadName;
            Object lookupResult;

            assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);
            assertTrue(cf3.toString(), cf3 instanceof ManagedCompletableFuture);
            assertTrue(cf4.toString(), cf4 instanceof ManagedCompletableFuture);
            assertTrue(cf5.toString(), cf5 instanceof ManagedCompletableFuture);
            assertTrue(cf6.toString(), cf6 instanceof ManagedCompletableFuture);

            assertFalse(cf1.isDone());
            try {
                Object result = cf1.get(100, TimeUnit.MILLISECONDS);
                fail("Dependent completion stage must not complete first");
            } catch (TimeoutException x) {
            }
            assertFalse(cf1.isDone()); // still blocked
            assertFalse(cf2.isDone()); // still blocked
            assertEquals(Integer.valueOf(-1), cf1.getNow(-1));
            assertEquals(Integer.valueOf(9999), cf2.getNow(9999));

            // allow cf1 to complete
            blocker1.countDown();

            // [cf3] applyToEitherAsync on default execution facility
            assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
            assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
            assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (lookupResult instanceof Throwable)
                throw new Exception((Throwable) lookupResult);
            assertEquals(defaultManagedExecutor, lookupResult);

            // [cf4] applyToEitherAsync on unmanaged executor
            assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
            assertFalse(threadName, threadName.startsWith("Default Executor-thread-")); // must run async on unmanaged thread
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (lookupResult instanceof NamingException)
                ; // pass
            else if (lookupResult instanceof Throwable)
                throw new Exception((Throwable) lookupResult);
            else
                fail("Unexpected result of lookup: " + lookupResult);

            // [cf5] applyToEither on unmanaged thread (or possibly servlet thread) - context should be applied from stage creation time
            assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
            assertTrue(threadName, threadName.equals(currentThreadName) || !threadName.startsWith("Default Executor-thread-")); // could run on current thread if previous stage is complete, otherwise must run on unmanaged thread
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (lookupResult instanceof Throwable)
                throw new Exception((Throwable) lookupResult);
            assertEquals(defaultManagedExecutor, lookupResult);

            // [cf6] applyToEitherAsync (second occurrence) on default execution facility, which does not propagate thread context
            assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
            assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
            assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (lookupResult instanceof NamingException)
                ; // pass
            else if (lookupResult instanceof Throwable)
                throw new Exception((Throwable) lookupResult);
            else
                fail("Unexpected result of lookup: " + lookupResult);

            assertEquals(Integer.valueOf(100), cf1.get());
            assertEquals(Integer.valueOf(101), cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // result after 1 increment
            assertEquals(Integer.valueOf(102), cf4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // result after 2 increments
            assertEquals(Integer.valueOf(103), cf5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // result after 3 increments
            assertEquals(Integer.valueOf(104), cf6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // result after 4 increments

            blocker2.countDown();
            assertEquals(Integer.valueOf(200), cf2.get());
        } finally {
            // allow threads to complete in case test fails
            blocker2.countDown();
            blocker1.countDown();
        }
    }

    /**
     * Verify that dependent stage for exceptionally is invoked in the event of an exception during the prior stage
     * and runs with context captured from the thread that creates the dependent stage.
     * Verify that dependent stage for exceptionally is not invoked when prior stage is successful.
     */
    @Test
    public void testExceptionally() throws Exception {
        AtomicInteger count = new AtomicInteger();
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();

        final Function<Throwable, Executor> lookup = (previousFailure) -> {
            System.out.println("> lookup #" + count.incrementAndGet() + " from testExceptionally");
            if (previousFailure != null)
                results.add(previousFailure);
            results.add(Thread.currentThread().getName());
            try {
                ManagedExecutorService result = InitialContext.doLookup("java:comp/env/executorRef");
                System.out.println("< lookup: " + result);
                return result;
            } catch (NamingException x) {
                System.out.println("< lookup failed");
                x.printStackTrace(System.out);
                throw new CompletionException(x);
            }
        };

        String threadName;
        Object previousFailure;

        // Verify that exceptionally is skipped when no exception is raised by prior stage

        CompletableFuture<?> cf1 = ManagedCompletableFuture
                        .completedFuture((Throwable) null)
                        .thenApplyAsync(lookup) // expect lookup to succeed because managed executor transfers thread context from the servlet
                        .exceptionally(lookup); // should not be invoked due to lack of any failure in prior stage

        assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);

        // thenApplyAsync on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async

        assertEquals(defaultManagedExecutor, cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals(1, count.get()); // lookup function only ran once

        // Verify that exceptionally is invoked when exception is raised by prior stage

        CompletableFuture<?> cf2 = ManagedCompletableFuture
                        .completedFuture((Throwable) null)
                        .thenApplyAsync(lookup, testThreads) // expect lookup to fail without the context of the servlet thread
                        .exceptionally(lookup);

        assertTrue(cf2.toString(), cf2 instanceof ManagedCompletableFuture);

        // thenApplyAsync on unmanaged executor
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertFalse(threadName, threadName.startsWith("Default Executor-thread-")); // must run async on unmanaged thread

        // exceptionally on unmanaged thread or servlet thread
        assertNotNull(previousFailure = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (previousFailure instanceof CompletionException && ((CompletionException) previousFailure).getCause() instanceof NamingException)
            ; // pass
        else if (previousFailure instanceof Throwable)
            throw new Exception((Throwable) previousFailure);
        else
            fail("Unexpected value supplied to function as previous failure: " + previousFailure);

        String previousThreadName = threadName;
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, previousThreadName.equals(threadName) || currentThreadName.equals(threadName)); // must run on same unmanaged thread or on servlet thread

        assertEquals(defaultManagedExecutor, cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(3, count.get()); // two additional executions of the lookup function
    }

    /**
     * Verify the handle method and both forms of handleAsync.
     */
    @Test
    public void testHandle() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();
        AtomicInteger failureCount = new AtomicInteger();

        BiFunction<Integer, Throwable, Integer> increment = (count, failure) -> {
            if (failure != null)
                count = failureCount.incrementAndGet() * 1000;
            System.out.println("> increment #" + (++count) + " from testHandle");
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
                System.out.println("< increment");
                return count;
            } catch (NamingException x) {
                results.add(x);
                System.out.println("< increment: " + x);
                throw new CompletionException(x);
            }
        };

        CompletableFuture<Integer> cf1 = ManagedCompletableFuture
                        .supplyAsync(() -> 0, defaultManagedExecutor)
                        .handleAsync(increment);

        CompletableFuture<Integer> cf2 = cf1.handleAsync(increment, testThreads);

        CompletableFuture<Integer> cf3 = cf2.handle(increment);

        CompletableFuture<Integer> cf4 = cf3.handleAsync(increment);

        LinkedBlockingQueue<CompletableFuture<Integer>> cf5q = new LinkedBlockingQueue<CompletableFuture<Integer>>();

        // Submit from thread that lacks context
        CompletableFuture.runAsync(() -> {
            cf5q.add(cf4.handleAsync(increment));
        });

        String threadName;
        Object lookupResult;

        assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);
        assertTrue(cf2.toString(), cf2 instanceof ManagedCompletableFuture);
        assertTrue(cf3.toString(), cf3 instanceof ManagedCompletableFuture);
        assertTrue(cf4.toString(), cf4 instanceof ManagedCompletableFuture);

        // handleAsync on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);
        assertEquals(Integer.valueOf(1), cf1.get());

        // handleAsync on unmanaged executor
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertFalse(threadName, threadName.startsWith("Default Executor-thread-")); // must run async on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);
        try {
            Integer result = cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Action should fail, not return " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof NamingException))
                throw x;
        }

        // handle on unmanaged thread (context should be applied from stage creation time)
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.equals(currentThreadName) || !threadName.startsWith("Default Executor-thread-")); // could run on current thread if previous stage is complete, otherwise must run on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);
        assertEquals(Integer.valueOf(1001), cf3.get());

        // handleAsync (second occurrence) on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);
        assertEquals(Integer.valueOf(1002), cf4.get());

        // handleAsync requested from unmanaged thread
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        CompletableFuture<Integer> cf5 = cf5q.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        try {
            Integer result = cf5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Action should fail, not return " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof NamingException))
                throw x;
        }

        assertEquals(Integer.valueOf(2001), cf5.handle(increment).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * From threads lacking application context, create 2 managed completable futures.
     * From the application thread, invoke runAfterBoth for these completable futures.
     * Verify that the runnable action runs on the same thread as at least one of the others (or on the servlet thread in case the stage completes quickly).
     * Verify that the runnable action runs after both of the others (it will be the only one that can look up from java:comp).
     */
    @Test
    public void testRunAfterBoth() throws Exception {
        AtomicInteger count = new AtomicInteger();
        LinkedBlockingQueue<Object[]> results = new LinkedBlockingQueue<Object[]>();
        String currentThreadName = Thread.currentThread().getName();

        final Runnable runnable = () -> {
            System.out.println("> run #" + count.incrementAndGet() + " from testRunAfterBoth");
            Object[] result = new Object[2];
            result[0] = Thread.currentThread().getName();
            try {
                result[1] = InitialContext.doLookup("java:comp/env/executorRef");
            } catch (NamingException x) {
                result[1] = x;
            } finally {
                results.add(result);
            }
            System.out.println("< run");
        };

        Callable<CompletableFuture<Void>> submitWithoutContext = () -> {
            return ManagedCompletableFuture.runAsync(runnable, defaultManagedExecutor);
        };

        List<Future<CompletableFuture<Void>>> completableFutures = testThreads.invokeAll(Arrays.asList(submitWithoutContext, submitWithoutContext));
        CompletableFuture<Void> cf1 = completableFutures.get(0).get();
        CompletableFuture<Void> cf2 = completableFutures.get(1).get();

        CompletableFuture<Void> cf3 = cf1.runAfterBoth(cf2, runnable);

        Object[] result;
        String threadName1, threadName2, threadName3;
        Object lookupResult;

        assertTrue(cf3.toString(), cf3 instanceof ManagedCompletableFuture);

        // static runAsync
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(threadName1 = result[0].toString());
        assertNotSame(currentThreadName, threadName1);
        assertTrue(threadName1, threadName1.startsWith("Default Executor-thread-"));
        assertNotNull(lookupResult = result[1]);
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        // static runAsync
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(threadName2 = result[0].toString());
        assertNotSame(currentThreadName, threadName2);
        assertTrue(threadName2, threadName2.startsWith("Default Executor-thread-"));
        assertNotNull(lookupResult = result[1]);
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        // runAfterBoth
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(threadName3 = result[0].toString());
        // runs on same thread as previous stage, or on current thread if both are complete:
        assertTrue(threadName3, threadName3.equals(threadName1) || threadName3.equals(threadName2) || threadName3.equals(currentThreadName));
        assertNotNull(lookupResult = result[1]);
        if (lookupResult instanceof Throwable) // thread context is available, even if it wasn't available to the previous stage
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        assertNull(cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(cf3.isDone());
        assertFalse(cf3.isCancelled());
        assertFalse(cf3.isCompletedExceptionally());
        assertFalse(cf3.complete(null));
        assertFalse(cf3.completeExceptionally(new ArrayIndexOutOfBoundsException("Not a real failure")));
        assertFalse(cf3.isCompletedExceptionally());
    }

    /**
     * From threads lacking application context, create 2 managed completable futures, one of which with an action that blocks for a long time.
     * From the application thread, invoke runAfterEither for these completable futures.
     * Verify that the runnable action runs on the same thread as the one that isn't blocked (or on the servlet thread in case the stage completes quickly).
     * Verify that the runnable action runs after both of the others (it will be the only one that can look up from java:comp).
     */
    @Test
    public void testRunAfterEither() throws Exception {
        AtomicInteger count = new AtomicInteger();
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();

        final Runnable runnable = () -> {
            System.out.println("> run #" + count.incrementAndGet() + " from testRunAfterEither");
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            System.out.println("< run");
        };

        final CountDownLatch blocker = new CountDownLatch(1);

        final Supplier<Boolean> blockedSupplier = () -> {
            System.out.println("> supplier #" + count.incrementAndGet() + " from testRunAfterEither");
            try {
                boolean awaited = blocker.await(5 * TIMEOUT_NS, TimeUnit.NANOSECONDS);
                results.add(awaited);
                System.out.println("< supplier successfully awaited latch? " + awaited);
                return awaited;
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        };

        Callable<CompletableFuture<?>[]> submitWithoutContext = () -> {
            return new CompletableFuture<?>[] {
                                                ManagedCompletableFuture.supplyAsync(blockedSupplier, defaultManagedExecutor),
                                                ManagedCompletableFuture.runAsync(runnable, defaultManagedExecutor)
            };
        };

        try {
            List<Future<CompletableFuture<?>[]>> completableFutures = testThreads.invokeAll(Collections.singleton(submitWithoutContext));
            CompletableFuture<?>[] cf = completableFutures.get(0).get();

            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean> cf1 = (CompletableFuture<Boolean>) cf[0];

            @SuppressWarnings("unchecked")
            CompletableFuture<Void> cf2 = (CompletableFuture<Void>) cf[1];

            CompletableFuture<Void> cf3 = cf1.runAfterEither(cf2, runnable);

            String threadName2, threadName3;
            Object lookupResult;

            assertTrue(cf3.toString(), cf3 instanceof ManagedCompletableFuture);

            // static runAsync
            assertNotNull(threadName2 = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
            assertNotSame(currentThreadName, threadName2);
            assertTrue(threadName2, threadName2.startsWith("Default Executor-thread-"));
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (lookupResult instanceof NamingException)
                ; // pass
            else if (lookupResult instanceof Throwable)
                throw new Exception((Throwable) lookupResult);
            else
                fail("Unexpected result of lookup: " + lookupResult);

            // runAfterEither
            assertNotNull(threadName3 = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
            // runs on same thread as previous stage, or on current thread if both are complete:
            assertTrue(threadName3, threadName3.equals(threadName2) || threadName3.equals(currentThreadName));
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (lookupResult instanceof Throwable) // thread context is available, even if it wasn't available to the previous stage
                throw new Exception((Throwable) lookupResult);
            assertEquals(defaultManagedExecutor, lookupResult);

            assertNull(cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(cf3.isDone());
            assertFalse(cf3.isCancelled());
            assertFalse(cf3.isCompletedExceptionally());

            // Blocked supplier does not run until we release the latch
            assertNull(results.peek());

            blocker.countDown();

            // supplyAsync
            assertTrue(cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(Boolean.TRUE, results.poll());
        } finally {
            // allow threads to complete in case test fails
            blocker.countDown();
        }
    }

    /**
     * Create 2 completion stages to run actions where one is blocked and the other can complete.
     * Use runAfterEitherAsync on these stages and verify for that the action runs after only one of the stages completes.
     */
    @Test
    public void testRunAfterEitherAsync() throws Exception {
        AtomicInteger count = new AtomicInteger();
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();

        final Runnable runnable = () -> {
            System.out.println("> run #" + count.incrementAndGet() + " from testRunAfterEitherAsync");
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            System.out.println("< run");
        };

        final CountDownLatch blocker = new CountDownLatch(1);

        final Runnable blockedRunnable = () -> {
            System.out.println("> run #" + count.incrementAndGet() + " from testRunAfterEitherAsync");
            try {
                boolean awaited = blocker.await(5 * TIMEOUT_NS, TimeUnit.NANOSECONDS);
                results.add(awaited);
                System.out.println("< run successfully awaited latch? " + awaited);
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        };

        try {
            CompletableFuture<?> cf0 = ManagedCompletableFuture.completedFuture("Completed Result");
            CompletableFuture<?> cf1 = cf0.thenRunAsync(blockedRunnable, noContextExecutor);
            CompletableFuture<?> cf2 = cf0.thenRunAsync(runnable, noContextExecutor);
            CompletableFuture<?> cf3 = cf1.runAfterEitherAsync(cf2, runnable);

            String threadName2, threadName3;
            Object lookupResult;

            assertTrue(cf3.toString(), cf3 instanceof ManagedCompletableFuture);

            // runAsync on noContextExecutor (not blocked)
            assertNotNull(threadName2 = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
            assertNotSame(currentThreadName, threadName2);
            assertTrue(threadName2, threadName2.startsWith("Default Executor-thread-"));
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (lookupResult instanceof NamingException)
                ; // pass
            else if (lookupResult instanceof Throwable)
                throw new Exception((Throwable) lookupResult);
            else
                fail("Unexpected result of lookup: " + lookupResult);

            // runAfterEitherAsync
            assertNotNull(threadName3 = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
            assertNotSame(currentThreadName, threadName3);
            assertTrue(threadName3, threadName3.startsWith("Default Executor-thread-"));
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (lookupResult instanceof Throwable) // thread context is available, even if it wasn't available to the previous stage
                throw new Exception((Throwable) lookupResult);
            assertEquals(defaultManagedExecutor, lookupResult);

            assertNull(cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(cf3.isDone());
            assertFalse(cf3.isCancelled());
            assertFalse(cf3.isCompletedExceptionally());

            // Blocked Runnable does not run until we release the latch
            assertNull(results.peek());

            blocker.countDown();

            // runAsync that was previously blocked now completes
            assertNull(cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(cf1.isDone());
            assertFalse(cf1.isCompletedExceptionally());
            assertEquals(Boolean.TRUE, results.poll());
        } finally {
            // allow threads to complete in case test fails
            blocker.countDown();
        }
    }

    /**
     * Create 2 completion stages to run actions where one is blocked and the other can complete.
     * Use runAfterEitherAsync on these stages, specifying an executor with different thread context propagation settings,
     * and verify for that the action runs with the specified thread context after only one of the stages completes.
     */
    @Test
    public void testRunAfterEitherAsyncOnExecutor() throws Exception {
        AtomicInteger count = new AtomicInteger();
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();

        final Runnable runnable = () -> {
            System.out.println("> run #" + count.incrementAndGet() + " from testRunAfterEitherAsyncOnExecutor");
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            System.out.println("< run");
        };

        final CountDownLatch blocker = new CountDownLatch(1);

        final Runnable blockedRunnable = () -> {
            System.out.println("> run #" + count.incrementAndGet() + " from testRunAfterEitherAsyncOnExecutor");
            try {
                boolean awaited = blocker.await(5 * TIMEOUT_NS, TimeUnit.NANOSECONDS);
                results.add(awaited);
                System.out.println("< run successfully awaited latch? " + awaited);
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        };

        try {
            CompletableFuture<?> cf1 = ManagedCompletableFuture.runAsync(blockedRunnable, noContextExecutor);
            CompletableFuture<?> cf2 = ManagedCompletableFuture.runAsync(runnable, noContextExecutor);
            CompletableFuture<?> cf3 = cf1.runAfterEitherAsync(cf2, runnable, defaultManagedExecutor);

            String threadName2, threadName3;
            Object lookupResult;

            assertTrue(cf3.toString(), cf3 instanceof ManagedCompletableFuture);

            // runAsync on noContextExecutor (not blocked)
            assertNotNull(threadName2 = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
            assertNotSame(currentThreadName, threadName2);
            assertTrue(threadName2, threadName2.startsWith("Default Executor-thread-"));
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (lookupResult instanceof NamingException)
                ; // pass
            else if (lookupResult instanceof Throwable)
                throw new Exception((Throwable) lookupResult);
            else
                fail("Unexpected result of lookup: " + lookupResult);

            // runAfterEitherAsync
            assertNotNull(threadName3 = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
            assertNotSame(currentThreadName, threadName3);
            assertTrue(threadName3, threadName3.startsWith("Default Executor-thread-"));
            assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (lookupResult instanceof Throwable) // thread context is available, even if it wasn't available to the previous stage
                throw new Exception((Throwable) lookupResult);
            assertEquals(defaultManagedExecutor, lookupResult);

            assertNull(cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(cf3.isDone());
            assertFalse(cf3.isCancelled());
            assertFalse(cf3.isCompletedExceptionally());

            // Blocked Runnable does not run until we release the latch
            assertNull(results.peek());

            blocker.countDown();

            // runAsync that was previously blocked now completes
            assertNull(cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(cf1.isDone());
            assertFalse(cf1.isCompletedExceptionally());
            assertEquals(Boolean.TRUE, results.poll());
        } finally {
            // allow threads to complete in case test fails
            blocker.countDown();
        }
    }

    /**
     * Verify thenAccept and both forms of thenAcceptAsync by checking that the parameter is correctly supplied,
     * that thread context is captured from the code that adds the dependent stage, not the thread that runs the action for the prior stage,
     * and that async operations run on threads from the Liberty global thread pool.
     */
    @Test
    public void testThenAccept() throws Exception {
        AtomicInteger count = new AtomicInteger();
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();

        final Consumer<String> consumer = (prevThreadName) -> {
            System.out.println("> accept #" + count.incrementAndGet() + " from testThenAccept");
            results.add(prevThreadName);
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            System.out.println("< accept");
        };

        CompletableFuture<Void> cf = ManagedCompletableFuture
                        .supplyAsync(() -> Thread.currentThread().getName())
                        .thenAcceptAsync(consumer, noContextExecutor)
                        .thenApplyAsync(unused -> Thread.currentThread().getName(), testThreads)
                        .thenAccept(consumer)
                        .thenApply(unused -> Thread.currentThread().getName())
                        .thenAcceptAsync(consumer);

        String threadName;
        Object lookupResult;

        assertTrue(cf.toString(), cf instanceof ManagedCompletableFuture);

        // static supplyAsync that creates ManagedCompletableFuture (value stored by dependent stage)
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async

        // thenAcceptAsync on noContextExecutor
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        // thenApplyAsync on unmanaged executor (value stored by dependent stage)
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertFalse(threadName, threadName.startsWith("Default Executor-thread-")); // must run async on unmanaged thread

        // thenAccept on unmanaged thread or servlet thread
        String previousThreadName = threadName;
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, previousThreadName.equals(threadName) || currentThreadName.equals(threadName)); // must run on the thread that ran the previous action or the servlet thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenApply on unmanaged thread or servlet thread
        previousThreadName = threadName;
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, previousThreadName.equals(threadName) || currentThreadName.equals(threadName)); // must run on the thread that ran the previous action or the servlet thread

        // thenAcceptAsync on default asynchronous execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        assertNull(cf.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Verify thenAcceptBoth and both forms of thenAcceptBothAsync.
     */
    @Test
    public void testThenAcceptBoth() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();

        BiConsumer<Integer, Integer> action = (a, b) -> {
            System.out.println("> sum " + a + "+" + b + " from testThenAcceptBoth");
            results.add(a + b);
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            System.out.println("< sum");
        };

        // The test logic requires that all of these completable futures run in order.
        // To guarantee this, ensure that at least one of the completable futures supplied to thenAcceptBoth* is the previous one.

        CompletableFuture<Integer> cf1 = ManagedCompletableFuture.supplyAsync(() -> 1, defaultManagedExecutor);

        CompletableFuture<Integer> cf2 = ManagedCompletableFuture.supplyAsync(() -> 2, noContextExecutor);

        CompletableFuture<Integer> cf5 = cf1
                        .thenAcceptBothAsync(cf2, action)
                        .thenApply((unused) -> 3)
                        .thenAcceptBothAsync(cf1, action, testThreads)
                        .thenApply((unused) -> 4)
                        .thenAcceptBoth(cf1, action)
                        .thenApply((unused) -> 5);

        CompletableFuture<Void> cf7 = cf2.thenAcceptBothAsync(cf5, action);

        String threadName;
        Object lookupResult;

        // thenAcceptBothAsync on default execution facility
        assertEquals(Integer.valueOf(3), results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenAcceptBothAsync on unmanaged executor
        assertEquals(Integer.valueOf(4), results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertFalse(threadName, threadName.startsWith("Default Executor-thread-")); // must run async on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        // thenAcceptBoth on unmanaged thread or servlet thread (context should be applied from stage creation time)
        assertEquals(Integer.valueOf(5), results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.equals(currentThreadName) || !threadName.startsWith("Default Executor-thread-")); // could run on current thread if previous stage is complete, otherwise must run on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenAcceptBothAsync (second occurrence) on default execution facility
        assertEquals(Integer.valueOf(7), results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        assertTrue(cf7.toString(), cf7 instanceof ManagedCompletableFuture);
        assertNull(cf7.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Verify thenApply and both forms of thenApplyAsync.
     */
    @Test
    public void testThenApply() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();

        final Function<Integer, Integer> increment = (count) -> {
            System.out.println("> increment #" + (++count) + " from testThenApply");
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            System.out.println("< increment");
            return count;
        };

        final CompletableFuture<Integer> cf = ManagedCompletableFuture
                        .supplyAsync(() -> 0, defaultManagedExecutor)
                        .thenApplyAsync(increment)
                        .thenApplyAsync(increment, testThreads)
                        .thenApply(increment)
                        .thenApplyAsync(increment);

        // Submit from thread that lacks context
        CompletableFuture.runAsync(() -> cf.thenApplyAsync(increment));

        String threadName;
        Object lookupResult;

        assertTrue(cf.toString(), cf instanceof ManagedCompletableFuture);

        // thenApplyAsync on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenApplyAsync on unmanaged executor
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertFalse(threadName, threadName.startsWith("Default Executor-thread-")); // must run async on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        // thenApply on unmanaged thread (context should be applied from stage creation time)
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.equals(currentThreadName) || !threadName.startsWith("Default Executor-thread-")); // could run on current thread if previous stage is complete, otherwise must run on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenApplyAsync (second occurrence) on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenApplyAsync requested from unmanaged thread
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        // result after 4 increments (the 5th increment is on a subsequent stage, and so would not be reflected in cf's result)
        assertEquals(Integer.valueOf(4), cf.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Verify thenCombine and both forms of thenCombineAsync.
     */
    @Test
    public void testThenCombine() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();

        BiFunction<Integer, Integer, Integer> sum = (a, b) -> {
            System.out.println("> sum " + a + "+" + b + " from testThenCombine");
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            System.out.println("< sum: " + (a + b));
            return a + b;
        };

        // The test logic requires that all of these completable futures run in order.
        // To guarantee this, ensure that at least one of the completable futures supplied to thenCombine* is the previous one.

        CompletableFuture<Integer> cf1 = ManagedCompletableFuture.supplyAsync(() -> 1, defaultManagedExecutor);

        CompletableFuture<Integer> cf2 = ManagedCompletableFuture.supplyAsync(() -> 2, noContextExecutor);

        CompletableFuture<Integer> cf3 = cf1.thenCombineAsync(cf2, sum);

        CompletableFuture<Integer> cf4 = cf1.thenCombineAsync(cf3, sum, testThreads);

        CompletableFuture<Integer> cf5 = cf4.thenCombine(cf1, sum);

        CompletableFuture<Integer> cf6 = cf5.thenCombineAsync(cf1, sum);

        // Submit from thread that lacks context
        CompletableFuture<CompletableFuture<Integer>> cfcf12 = CompletableFuture.supplyAsync(() -> cf6.thenCombineAsync(cf6, sum));

        String threadName;
        Object lookupResult;

        assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);
        assertTrue(cf2.toString(), cf2 instanceof ManagedCompletableFuture);
        assertTrue(cf3.toString(), cf3 instanceof ManagedCompletableFuture);
        assertTrue(cf4.toString(), cf4 instanceof ManagedCompletableFuture);
        assertTrue(cf5.toString(), cf5 instanceof ManagedCompletableFuture);
        assertTrue(cf6.toString(), cf6 instanceof ManagedCompletableFuture);

        // thenCombineAsync on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);
        assertEquals(Integer.valueOf(3), cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // thenCombineAsync on unmanaged executor
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertFalse(threadName, threadName.startsWith("Default Executor-thread-")); // must run async on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);
        assertEquals(Integer.valueOf(4), cf4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // thenCombine on unmanaged thread or servlet thread (context should be applied from stage creation time)
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.equals(currentThreadName) || !threadName.startsWith("Default Executor-thread-")); // could run on current thread if previous stage is complete, otherwise must run on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);
        assertEquals(Integer.valueOf(5), cf5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // thenCombineAsync (second occurrence) on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);
        assertEquals(Integer.valueOf(6), cf6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // thenCombineAsync requested from unmanaged thread
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        CompletableFuture<Integer> cf12 = cfcf12.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(Integer.valueOf(12), cf12.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Verify thenCompose and both forms of thenComposeAsync.
     */
    @Test
    public void testThenCompose() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();

        Function<Integer, CompletionStage<Long>> incrementIntToLong = (count) -> {
            System.out.println("> incrementIntToLong #" + (++count) + " from testThenCompose");
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            System.out.println("< incrementIntToLong");
            return CompletableFuture.completedFuture(count.longValue());
        };

        Function<Long, CompletionStage<Integer>> incrementLongToInt = (count) -> {
            System.out.println("> incrementLongToInt #" + (++count) + " from testThenCompose");
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            System.out.println("< incrementLongToInt");
            return CompletableFuture.completedFuture(count.intValue());
        };

        final CompletableFuture<Integer> cf = ManagedCompletableFuture
                        .supplyAsync(() -> 0, defaultManagedExecutor)
                        .thenComposeAsync(incrementIntToLong)
                        .thenComposeAsync(incrementLongToInt, testThreads)
                        .thenCompose(incrementIntToLong)
                        .thenComposeAsync(incrementLongToInt);

        // Submit from thread that lacks context
        CompletableFuture.runAsync(() -> cf.thenApplyAsync(incrementIntToLong));

        String threadName;
        Object lookupResult;

        assertTrue(cf.toString(), cf instanceof ManagedCompletableFuture);

        // thenComposeAsync on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenComposeAsync on unmanaged executor
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertFalse(threadName, threadName.startsWith("Default Executor-thread-")); // must run async on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        // thenCompose on unmanaged thread (context should be applied from stage creation time)
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.equals(currentThreadName) || !threadName.startsWith("Default Executor-thread-")); // could run on current thread if previous stage is complete, otherwise must run on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenComposeAsync (second occurrence) on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenComposeAsync requested from unmanaged thread
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        // result after 4 increments (the 5th increment is on a subsequent stage, and so would not be reflected in cf's result)
        assertEquals(Integer.valueOf(4), cf.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Verify that the function argument to thenCompose can return a result that is a ManagedCompletableFuture.
     */
    @Test
    public void testThenComposeManagedCompletableFuture() throws Exception {
        ManagedCompletableFuture<String> cf = ManagedCompletableFuture
                        .supplyAsync(() -> 100)
                        .thenCompose(t -> {
                            return ManagedCompletableFuture.supplyAsync(() -> {
                                try {
                                    return t + "," + InitialContext.doLookup("java:comp/env/executorRef");
                                } catch (NamingException x) {
                                    throw new CompletionException(x);
                                }
                            });
                        });
        String result;
        assertNotNull(result = cf.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(result, result.startsWith("100,"));
        assertTrue(result, result.indexOf("ManagedExecutorService") > 0);
    }

    /**
     * Verify thenRun and both forms of thenRunAsync.
     */
    @Test
    public void testThenRun() throws Exception {
        AtomicInteger count = new AtomicInteger();
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        String currentThreadName = Thread.currentThread().getName();

        final Runnable runnable = () -> {
            System.out.println("> run #" + count.incrementAndGet() + " from testThenRun");
            results.add(Thread.currentThread().getName());
            try {
                results.add(InitialContext.doLookup("java:comp/env/executorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            System.out.println("< run");
        };

        final CompletableFuture<Void> cf = ManagedCompletableFuture
                        .runAsync(runnable, defaultManagedExecutor)
                        .thenRunAsync(runnable)
                        .thenRunAsync(runnable, testThreads)
                        .thenRun(runnable)
                        .thenRunAsync(runnable);

        // Submit from thread that lacks context
        CompletableFuture.runAsync(() -> {
            cf.thenRunAsync(runnable);
        });

        String threadName;
        Object lookupResult;

        assertTrue(cf.toString(), cf instanceof ManagedCompletableFuture);

        // static runAsync that creates ManagedCompletableFuture
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenRunAsync on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenRunAsync on unmanaged executor
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertFalse(threadName, threadName.startsWith("Default Executor-thread-")); // must run async on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        // thenRun on unmanaged thread (context should be applied from stage creation time)
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.equals(currentThreadName) || !threadName.startsWith("Default Executor-thread-")); // could run on current thread if previous stage is complete, otherwise must run on unmanaged thread
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenRunAsync (second occurrence) on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenRunAsync requested from unmanaged thread
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-")); // must run on Liberty global thread pool
        assertNotSame(currentThreadName, threadName); // cannot be the servlet thread because operation is async
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);
    }

    /**
     * Supply unmanaged CompletableFuture.runAfterBoth with a ManagedCompletableFuture and see if it can notice
     * when the ManagedCompletableFuture completes.
     */
    @Test
    public void testUnmanagedRunAfterBoth() throws Exception {
        AtomicInteger count = new AtomicInteger();
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();

        final Runnable runnable = () -> {
            System.out.println("> run #" + count.incrementAndGet() + " from testUnmanagedRunAfterBoth");
            results.add(Thread.currentThread().getName());
            System.out.println("< run");
        };

        CompletableFuture<Void> cf1 = CompletableFuture.runAsync(runnable);
        CompletableFuture<Void> cf2 = ManagedCompletableFuture.runAsync(runnable, defaultManagedExecutor);
        CompletableFuture<Void> cf3 = cf1.runAfterBoth(cf2, runnable);

        // static runAsync
        assertNotNull(results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // static runAsync
        assertNotNull(results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // runAfterBoth
        assertNotNull(results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertNull(cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Supply unmanaged CompletableFuture as a parameter to thenCombineAsync and thenAcceptBothAsync of ManagedCompletableFuture.
     */
    @Test
    public void testUnmanagedThenCombineThenAcceptBoth() {
        LinkedList<String> results = new LinkedList<String>();
        BiFunction<String, String, List<String>> fn = (item1, item2) -> {
            results.add(item1);
            results.add(item2);
            return results;
        };
        CompletableFuture<String> cf1 = ManagedCompletableFuture.supplyAsync(() -> "param1", noContextExecutor);
        CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> "param2");
        CompletableFuture<String> cf3 = CompletableFuture.supplyAsync(() -> "param3");
        CompletableFuture<Void> cf = cf1
                        .thenCombineAsync(cf2, (a, b) -> {
                            results.add(Thread.currentThread().getName());
                            results.add(a);
                            results.add(b);
                            return results;
                        })
                        .thenAcceptBothAsync(cf3, (a, b) -> {
                            a.add(Thread.currentThread().getName());
                            a.add(b);
                        });

        assertTrue(cf.toString(), cf instanceof ManagedCompletableFuture);

        cf.join();

        assertTrue(cf.isDone());
        assertFalse(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        String servletThreadName = Thread.currentThread().getName();
        String threadName;

        assertNotNull(threadName = results.poll());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-"));
        assertNotSame(servletThreadName, threadName);

        assertEquals("param1", results.poll());
        assertEquals("param2", results.poll());

        assertNotNull(threadName = results.poll());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-"));
        assertNotSame(servletThreadName, threadName);

        assertEquals("param3", results.poll());
    }

    /**
     * Verify that the whenComplete and whenCompleteAsync methods are invoked after exceptional completion,
     * that thread context is propagated by the managed executor to the BiConsumer, that it runs on a managed executor
     * thread when invoked as Async, and that the parameters indicating the failing result are passed correctly to the BiConsumer.
     */
    @Test
    public void testWhenCompleteAfterFailure() throws Exception {
        AtomicInteger count = new AtomicInteger();
        LinkedBlockingQueue<Object[]> results = new LinkedBlockingQueue<Object[]>();
        String currentThreadName = Thread.currentThread().getName();

        final int PREV_RESULT = 0, PREV_FAILURE = 1, THREAD = 2, LOOKUP_RESULT = 3;

        final BiConsumer<Integer, Throwable> lookup = (result, failure) -> {
            System.out.println("> lookup #" + count.incrementAndGet() + " from testWhenCompleteAfterFailure");
            Object[] r = new Object[4];
            r[PREV_RESULT] = result;
            r[PREV_FAILURE] = failure;
            r[THREAD] = Thread.currentThread().getName();
            try {
                r[LOOKUP_RESULT] = InitialContext.doLookup("java:comp/env/executorRef");
            } catch (NamingException x) {
                r[LOOKUP_RESULT] = x;
            }
            results.add(r);
            System.out.println("< lookup");
        };

        CompletableFuture<Integer> cf0 = ManagedCompletableFuture
                        .completedFuture(0)
                        .thenApplyAsync(t -> 10 / t, testThreads); // intentionally fail with division by 0

        CompletableFuture<Integer> cf1 = cf0.whenCompleteAsync(lookup);
        CompletableFuture<Integer> cf2 = cf0.whenCompleteAsync(lookup, noContextExecutor);
        CompletableFuture<Integer> cf3 = cf0.whenComplete(lookup);

        assertTrue(cf0.toString(), cf0 instanceof ManagedCompletableFuture);
        assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);
        assertTrue(cf2.toString(), cf2 instanceof ManagedCompletableFuture);
        assertTrue(cf3.toString(), cf3 instanceof ManagedCompletableFuture);

        // Order in which the above run is unpredictable. Distinguish by looking at the execution thread and lookup result.

        Object[] cf1result = null, cf2result = null, cf3result = null;

        for (int i = 1; i <= 3; i++) {
            Object[] result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertNotNull("missing result #" + i, result);
            System.out.println(Arrays.asList(result));
            String threadName = (String) result[THREAD];
            if (threadName.startsWith("Default Executor-thread-") && !threadName.equals(currentThreadName))
                if (result[LOOKUP_RESULT] instanceof ManagedExecutorService)
                    cf1result = result;
                else
                    cf2result = result;
            else
                cf3result = result;
        }

        assertNotNull(cf1result);
        assertNotNull(cf2result);
        assertNotNull(cf3result);

        // whenCompleteAsync on default asynchronous execution facility
        assertNull(cf1result[PREV_RESULT]);
        assertTrue(cf1result[PREV_FAILURE].toString(), // CompletableFuture wraps the exception with CompletionException, so expect the same here
                   cf1result[PREV_FAILURE] instanceof CompletionException && ((CompletionException) cf1result[PREV_FAILURE]).getCause() instanceof ArithmeticException);
        assertNotSame(currentThreadName, cf1result[THREAD]); // cannot be the servlet thread because operation is async

        // whenCompleteAsync on noContextExecutor
        assertNull(cf2result[PREV_RESULT]);
        assertTrue(cf2result[PREV_FAILURE].toString(), // CompletableFuture wraps the exception with CompletionException, so expect the same here
                   cf2result[PREV_FAILURE] instanceof CompletionException && ((CompletionException) cf2result[PREV_FAILURE]).getCause() instanceof ArithmeticException);
        assertNotSame(currentThreadName, cf2result[THREAD]); // cannot be the servlet thread because operation is async
        assertTrue(cf2result[LOOKUP_RESULT].toString(), cf2result[LOOKUP_RESULT] instanceof NamingException);

        // whenComplete on unmanaged thread or servlet thread
        assertNull(cf3result[PREV_RESULT]);
        assertTrue(cf3result[PREV_FAILURE].toString(), // CompletableFuture wraps the exception with CompletionException, so expect the same here
                   cf3result[PREV_FAILURE] instanceof CompletionException && ((CompletionException) cf3result[PREV_FAILURE]).getCause() instanceof ArithmeticException);
        assertEquals(defaultManagedExecutor, cf3result[LOOKUP_RESULT]);

        try {
            fail("Completable future 0 should not have successful result: " + cf0.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        try {
            fail("Completable future 1 should not have successful result: " + cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        try {
            fail("Completable future 2 should not have successful result: " + cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        try {
            fail("Completable future 3 should not have successful result: " + cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        assertEquals(3, count.get());
    }

    /**
     * Verify that the whenComplete and whenCompleteAsync methods are invoked after successful completion,
     * that thread context is propagated by the managed executor to the BiConsumer, that it runs on a managed executor
     * thread when invoked as Async, and that the BiConsumer parameters include the successful result and no exception.
     */
    @Test
    public void testWhenCompleteAfterSuccessfulCompletion() throws Exception {
        AtomicInteger count = new AtomicInteger();
        LinkedBlockingQueue<Object[]> results = new LinkedBlockingQueue<Object[]>();
        String currentThreadName = Thread.currentThread().getName();

        final int PREV_RESULT = 0, PREV_FAILURE = 1, THREAD = 2, LOOKUP_RESULT = 3;

        final BiConsumer<String, Throwable> lookup = (result, failure) -> {
            System.out.println("> lookup #" + count.incrementAndGet() + " from testWhenCompleteAfterSuccessfulCompletion");
            Object[] r = new Object[4];
            r[PREV_RESULT] = result;
            r[PREV_FAILURE] = failure;
            r[THREAD] = Thread.currentThread().getName();
            try {
                r[LOOKUP_RESULT] = InitialContext.doLookup("java:comp/env/executorRef");
            } catch (NamingException x) {
                r[LOOKUP_RESULT] = x;
            }
            results.add(r);
            System.out.println("< lookup");
        };

        CompletableFuture<String> cf0 = ManagedCompletableFuture
                        .completedFuture("initial result")
                        .thenApplyAsync(t -> Thread.currentThread().getName(), testThreads);

        CompletableFuture<String> cf1 = cf0.whenCompleteAsync(lookup);
        CompletableFuture<String> cf2 = cf0.whenCompleteAsync(lookup, noContextExecutor);
        CompletableFuture<String> cf3 = cf0.whenComplete(lookup);

        assertTrue(cf0.toString(), cf0 instanceof ManagedCompletableFuture);
        assertTrue(cf1.toString(), cf1 instanceof ManagedCompletableFuture);
        assertTrue(cf2.toString(), cf2 instanceof ManagedCompletableFuture);
        assertTrue(cf3.toString(), cf3 instanceof ManagedCompletableFuture);

        String cf0ThreadName = cf0.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertFalse(cf0ThreadName, cf0ThreadName.startsWith("Default Executor-thread-")); // must run async on unmanaged thread

        // Order in which the above run is unpredictable. Distinguish by looking at the execution thread and lookup result.

        Object[] cf1result = null, cf2result = null, cf3result = null;

        for (int i = 1; i <= 3; i++) {
            Object[] result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertNotNull("missing result #" + i, result);
            System.out.println(Arrays.asList(result));
            String threadName = (String) result[THREAD];
            if (threadName.startsWith("Default Executor-thread-") && !threadName.equals(currentThreadName))
                if (result[LOOKUP_RESULT] instanceof ManagedExecutorService)
                    cf1result = result;
                else
                    cf2result = result;
            else
                cf3result = result;
        }

        assertNotNull(cf1result);
        assertNotNull(cf2result);
        assertNotNull(cf3result);

        // whenCompleteAsync on default asynchronous execution facility
        assertEquals(cf0ThreadName, cf1result[PREV_RESULT]);
        assertNull(cf1result[PREV_FAILURE]);
        assertNotSame(currentThreadName, cf1result[THREAD]); // cannot be the servlet thread because operation is async

        // whenCompleteAsync on noContextExecutor
        assertEquals(cf0ThreadName, cf2result[PREV_RESULT]);
        assertNull(cf2result[PREV_FAILURE]);
        assertNotSame(currentThreadName, cf2result[THREAD]); // cannot be the servlet thread because operation is async
        assertTrue(cf2result[LOOKUP_RESULT].toString(), cf2result[LOOKUP_RESULT] instanceof NamingException);

        // whenComplete on unmanaged thread or servlet thread
        assertEquals(cf0ThreadName, cf3result[PREV_RESULT]);
        assertNull(cf3result[PREV_FAILURE]);
        String cf3Thread = (String) cf3result[THREAD];
        assertTrue(cf3Thread, cf0ThreadName.equals(cf3Thread) || currentThreadName.equals(cf3Thread));
        assertEquals(defaultManagedExecutor, cf3result[LOOKUP_RESULT]);

        assertEquals(cf0ThreadName, cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(cf0ThreadName, cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(cf0ThreadName, cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals(3, count.get());
    }
}
