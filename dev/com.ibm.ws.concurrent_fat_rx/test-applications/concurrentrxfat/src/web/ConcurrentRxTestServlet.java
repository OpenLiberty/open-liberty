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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
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
                throw new RuntimeException(x);
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
                throw new RuntimeException(x);
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
     * From threads lacking application context, create 2 managed completable futures.
     * From the application thread, invoke runAfterBoth for these completable futures.
     * Verify that the runnable action runs on the same thread as at least one of the others.
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
        assertNotSame(currentThreadName, threadName);
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-"));
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenRunAsync on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-"));
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenRunAsync on unmanaged executor
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.equals(currentThreadName) || !threadName.startsWith("Default Executor-thread-")); // could run on current thread if previous stage is complete
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof NamingException)
            ; // pass
        else if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        else
            fail("Unexpected result of lookup: " + lookupResult);

        // thenRun on unmanaged thread (context should be applied from stage creation time)
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.equals(currentThreadName) || !threadName.startsWith("Default Executor-thread-")); // could run on current thread if previous stage is complete
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenRunAsync (second occurrence) on default execution facility
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertTrue(threadName, threadName.startsWith("Default Executor-thread-"));
        assertNotNull(lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (lookupResult instanceof Throwable)
            throw new Exception((Throwable) lookupResult);
        assertEquals(defaultManagedExecutor, lookupResult);

        // thenRunAsync requested from unmanaged thread
        assertNotNull(threadName = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS).toString());
        assertNotSame(currentThreadName, threadName);
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
}
