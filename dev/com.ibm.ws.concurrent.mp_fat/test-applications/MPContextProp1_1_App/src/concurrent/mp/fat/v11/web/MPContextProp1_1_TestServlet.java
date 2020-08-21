/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.mp.fat.v11.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.Test;
import org.test.context.location.CurrentLocation;
import org.test.context.location.TestContextTypes;

import componenttest.annotation.AllowedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MPContextProp1_1_TestServlet")
public class MPContextProp1_1_TestServlet extends HttpServlet {
    private static final String SUCCESS = "SUCCESS";
    private static final String TEST_METHOD = "testMethod";

    /**
     * 2 minutes. Maximum number of nanoseconds to wait for a task to complete.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource(name = "java:module/env/defaultExecutorRef")
    private ManagedExecutor defaultManagedExecutor;

    /**
     * Executor that can be used when tests don't want to tie up threads from the Liberty global thread pool to perform test logic
     */
    private ExecutorService testThreads;

    @Override
    public void destroy() {
        AccessController.doPrivileged((PrivilegedAction<List<Runnable>>) () -> testThreads.shutdownNow());
    }

    /**
     * The following is copied because this test cannot extend the Jakarta version of FATServlet when other tests
     * in the project already extend the Java EE version of FATServlet, given that both have the same name and package.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String method = request.getParameter(TEST_METHOD);

        System.out.println(">>> BEGIN: " + method);
        System.out.println("Request URL: " + request.getRequestURL() + '?' + request.getQueryString());
        PrintWriter writer = response.getWriter();
        if (method != null && method.length() > 0) {
            try {
                // Use reflection to try invoking various test method signatures:
                // 1)  method(HttpServletRequest request, HttpServletResponse response)
                // 2)  method()
                // 3)  use custom method invocation by calling invokeTest(method, request, response)
                try {
                    Method mthd = getClass().getMethod(method, HttpServletRequest.class, HttpServletResponse.class);
                    mthd.invoke(this, request, response);
                } catch (NoSuchMethodException nsme) {
                    Method mthd = getClass().getMethod(method, (Class<?>[]) null);
                    mthd.invoke(this);
                } finally {
                    CurrentLocation.clear(); // clean up custom thread context after test
                }

                writer.println(SUCCESS);
            } catch (Throwable t) {
                if (t instanceof InvocationTargetException) {
                    t = t.getCause();
                }

                System.out.println("ERROR: " + t);
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                System.err.print(sw);

                writer.println("ERROR: Caught exception attempting to call test method " + method + " on servlet " + getClass().getName());
                t.printStackTrace(writer);
            }
        } else {
            System.out.println("ERROR: expected testMethod parameter");
            writer.println("ERROR: expected testMethod parameter");
        }

        writer.flush();
        writer.close();

        System.out.println("<<< END:   " + method);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        testThreads = Executors.newFixedThreadPool(10);
    }

    /**
     * Verify that ManagedExecutor.copy(CompletableFuture) returns a CompletableFuture that
     * automatically completes when the original CompletableFuture completes, but applies
     * to dependent stages the context and concurrency constraints of the ManagedExecutor.
     */
    @Test
    public void testCopyFuture() throws Exception {
        ManagedExecutor executor = ManagedExecutor.builder()
                        .propagated(TestContextTypes.STATE)
                        .cleared(ThreadContext.ALL_REMAINING)
                        .build();

        CurrentLocation.setLocation("Minneapolis", "Minnesota");

        CompletableFuture<String> unmanagedFuture = new CompletableFuture<String>();
        CompletableFuture<String> copy = executor.copy(unmanagedFuture);
        CompletableFuture<String> dependentStage = copy.thenApplyAsync(s -> {
            try {
                fail("Should not be able to look up " + InitialContext.doLookup("java:module/env/defaultExecutorRef"));
            } catch (NamingException x) {
                // expected because application context should be cleared
            }

            String city = CurrentLocation.getCity();
            return s + ("".equals(city) ? CurrentLocation.getState() : (city + ", " + CurrentLocation.getState()));
        });

        CurrentLocation.setLocation("Des Moines", "Iowa");

        assertFalse(copy.isDone());

        unmanagedFuture.complete("Arriving in ");
        assertEquals("Arriving in Minnesota", dependentStage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertTrue(copy.isDone());

        // Complete the copy stage before the original
        unmanagedFuture = new CompletableFuture<String>();
        copy = executor.copy(unmanagedFuture);
        dependentStage = copy.thenApplyAsync(s -> s + CurrentLocation.getState());

        CurrentLocation.setLocation("Madison", "Wisconsin");

        assertFalse(copy.isDone());
        copy.complete("You are now in ");
        assertEquals("You are now in Iowa", dependentStage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertTrue(copy.isDone());
        // Because the copy was completed separately, the original can complete after this point with a different value.
        assertTrue(unmanagedFuture.complete("ABCDE"));
        assertEquals("ABCDE", unmanagedFuture.getNow("F"));
        assertEquals("You are now in ", copy.getNow("G"));

        executor.shutdown();
    }

    /**
     * Verify that ManagedExecutor.copy(CompletionStage) returns a CompletionStage that
     * automatically completes when the original CompletionStage completes, but applies
     * to dependent stages the context and concurrency constraints of the ManagedExecutor.
     */
    @Test
    public void testCopyStage() throws Exception {
        ManagedExecutor executor = ManagedExecutor.builder()
                        .maxAsync(1)
                        .maxQueued(1)
                        .propagated(TestContextTypes.CITY)
                        .build();

        CountDownLatch originalStageBlocker = new CountDownLatch(1);
        CountDownLatch dependentStageBlocker = new CountDownLatch(1);

        CompletionStage<CountDownLatch> unmanagedFuture = CompletableFuture.supplyAsync(() -> {
            try {
                assertTrue(originalStageBlocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                return dependentStageBlocker;
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        }, testThreads);
        CompletionStage<CountDownLatch> copy = executor.copy(unmanagedFuture);

        CurrentLocation.setLocation("Stewartville", "Minnesota");

        CompletionStage<String> dependentStage1 = copy.thenApplyAsync(latch -> {
            try {
                return latch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS) ? CurrentLocation.getCity() : "Dependent stage 1 timed out";
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        });

        CompletionStage<String> dependentStage2 = copy.thenApplyAsync(latch -> {
            try {
                return latch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS) ? CurrentLocation.getCity() : "Dependent stage 2 timed out";
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        });

        CompletionStage<String> dependentStage3 = copy.thenApplyAsync(latch -> {
            try {
                return latch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS) ? CurrentLocation.getCity() : "Dependent stage 3 timed out";
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        });

        // Allow the original stage to complete
        CurrentLocation.setLocation("Byron", "Minnesota");
        originalStageBlocker.countDown();

        // At least one of the dependent stages will be unable to queue for execution due to maxQueued=1, maxAsync=1
        LinkedBlockingQueue<Throwable> abortedStageExceptions = new LinkedBlockingQueue<Throwable>(3);
        LinkedBlockingQueue<String> successfulStageResults = new LinkedBlockingQueue<String>(3);

        BiConsumer<String, Throwable> collectResult = (result, x) -> {
            if (x == null)
                successfulStageResults.add(result);
            else
                abortedStageExceptions.add(x);
        };
        dependentStage1.whenComplete(collectResult);
        dependentStage2.whenComplete(collectResult);
        dependentStage3.whenComplete(collectResult);

        // At least one dependent stage must be rejected
        Throwable x = abortedStageExceptions.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull("1 or 2 dependent stages should be aborted due to the maxAsync/maxQueued limitations of the managed executor", x);
        assertTrue(x.toString(), x instanceof RejectedExecutionException);
        assertTrue(x.toString(), x.getMessage().contains("CWWKE1201E"));

        // At least one dependent stage must complete successfully
        dependentStageBlocker.countDown();
        assertEquals("Stewartville", successfulStageResults.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // The remaining stage can either be rejected or complete successfully based on timing
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(200)) {
            String result = successfulStageResults.poll();
            if (result != null) {
                assertEquals("Stewartville", result);
                break;
            }
            x = abortedStageExceptions.poll();
            if (x != null) {
                assertTrue(x.toString(), x instanceof RejectedExecutionException);
                assertTrue(x.toString(), x.getMessage().contains("CWWKE1201E"));
                break;
            }
        }

        executor.shutdown();
    }

    /**
     * Verify that ManagedExecutor.getThreadContext returns a usable ThreadContext instance,
     * which propagates and clears the same types of thread context as the ManagedExecutor.
     * Also verifies that a completion stage that is created by withContextCapture runs on the
     * ManagedExecutor from which the ThreadContext was obtained.
     */
    @Test
    public void testGetThreadContext() throws Exception {
        ManagedExecutor executor = ManagedExecutor.builder()
                        .maxAsync(1)
                        .maxQueued(1)
                        .propagated(ThreadContext.APPLICATION, TestContextTypes.STATE)
                        .cleared(ThreadContext.ALL_REMAINING)
                        .build();

        ThreadContext threadContext = executor.getThreadContext();

        CurrentLocation.setLocation("Rochester", "Minnesota");

        Callable<String> contextualAction = threadContext.contextualCallable(() -> {
            // application context must propagate for this to work
            assertNotNull(InitialContext.doLookup("java:module/env/defaultExecutorRef"));

            String city = CurrentLocation.getCity();
            if ("".equals(city))
                city = "Unknown location";
            return city + " in " + CurrentLocation.getState();
        });

        CurrentLocation.setLocation("La Crosse", "Wisconsin");

        // Run on JDK's fork join pool, but with previously captured context
        Future<String> future = testThreads.submit(contextualAction);
        assertEquals("Unknown location in Minnesota", future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Run on current thread
        assertEquals("Unknown location in Minnesota", contextualAction.call());

        // Verify context of current thread is restored
        assertEquals("La Crosse in Wisconsin", CurrentLocation.getCity() + " in " + CurrentLocation.getState());
        assertNotNull(InitialContext.doLookup("java:module/env/defaultExecutorRef"));

        // Use up the maxAsync of 1
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch isBlocking = new CountDownLatch(1);
        CompletableFuture<Boolean> blockingFuture = executor.supplyAsync(() -> {
            try {
                isBlocking.countDown();
                return blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        });
        assertTrue(isBlocking.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        CompletableFuture<String> unmanagedFuture = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(200);
                return "Location is ";
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        }, testThreads);

        CompletableFuture<String> copy = threadContext.withContextCapture(unmanagedFuture);

        CurrentLocation.setLocation("Decorah", "Iowa");

        Function<String, String> getLocation = s -> s + CurrentLocation.getCity() + CurrentLocation.getState();
        CompletableFuture<String> dependentStage1 = copy.thenApplyAsync(getLocation);
        CompletableFuture<String> dependentStage2 = copy.thenApplyAsync(getLocation);

        CurrentLocation.setLocation("Rockford", "Illinois");

        // Only one of the stages can be queued. The other must be rejected.
        Throwable abortedTaskException = null;
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && abortedTaskException == null; TimeUnit.MILLISECONDS.sleep(200)) {
            try {
                assertEquals("1 not done yet", dependentStage1.getNow("1 not done yet"));
                assertEquals("2 not done yet", dependentStage2.getNow("2 not done yet"));
            } catch (CancellationException x) {
                abortedTaskException = x;
            } catch (CompletionException x) {
                Throwable cause = x.getCause();
                if (cause instanceof RejectedExecutionException)
                    abortedTaskException = cause;
                else
                    throw x;
            }
        }
        assertNotNull("Neither stage was rejected, despite queue capacity configured to 1: " + dependentStage1 + ", " + dependentStage2, abortedTaskException);

        blocker.countDown();
 
        if (dependentStage1.isCompletedExceptionally())
            assertEquals("Location is Iowa", dependentStage2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        if (dependentStage2.isCompletedExceptionally())
            assertEquals("Location is Iowa", dependentStage1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor.shutdown();
    }

    /**
     * Verify that a completion stage that is created by withContextCapture runs on the DefaultManagedExecutorService
     * when the ThreadContext was not obtained from a ManagedExecutor.
     */
    @Test
    public void testStageWithContextCaptureThatRunsOnDefaultManagedExecutor() throws Exception {
        ThreadContext threadContext = ThreadContext.builder()
                        .propagated(ThreadContext.APPLICATION, TestContextTypes.CITY)
                        .cleared(ThreadContext.ALL_REMAINING)
                        .build();

        CompletionStage<String> unmanagedStage = CompletableFuture.supplyAsync(() -> "City of ", testThreads);
        CompletionStage<String> copy = threadContext.withContextCapture(unmanagedStage);

        CountDownLatch blocker = new CountDownLatch(1);
        LinkedBlockingQueue<ManagedExecutor> lookupResults = new LinkedBlockingQueue<ManagedExecutor>();
        Function<String, String> getCity = s -> {
            try {
                // verify availability of application context
                lookupResults.add(InitialContext.doLookup("java:module/env/defaultExecutorRef"));

                blocker.await(TIMEOUT_NS * 3, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            } catch (NamingException x) {
                throw new CompletionException(x);
            }

            // verify availability of custom context
            s += CurrentLocation.getCity();
            // verify clearing of other custom context
            String state = CurrentLocation.getState();
            if (!"".equals(state))
                s += " in " + state;
            return s;
        };

        CurrentLocation.setLocation("Chatfield", "Minnesota");
        CompletionStage<String> dependentStage1 = copy.thenApplyAsync(getCity);
        CurrentLocation.setLocation("Fountain", "Minnesota");
        CompletionStage<String> dependentStage2 = copy.thenApplyAsync(getCity);
        CurrentLocation.setLocation("Preston", "Minnesota");
        CompletionStage<String> dependentStage3 = copy.thenApplyAsync(getCity);
        CurrentLocation.setLocation("Lanesboro", "Minnesota");
        CompletionStage<String> dependentStage4 = copy.thenApplyAsync(getCity);
        CurrentLocation.setLocation("Harmony", "Minnesota");

        // wait for all 4 dependent stages to be running
        assertNotNull(lookupResults.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(lookupResults.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(lookupResults.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(lookupResults.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // allow all stages to complete
        blocker.countDown();

        assertEquals("City of Chatfield", dependentStage1.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals("City of Fountain", dependentStage2.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals("City of Preston", dependentStage3.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals("City of Lanesboro", dependentStage4.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Context of current thread does not change
        assertEquals("Harmony", CurrentLocation.getCity());
        assertEquals("Minnesota", CurrentLocation.getState());
    }

    /**
     * Verify that a completion stage that is created by withContextCapture runs on the ManagedExecutor from which
     * the ThreadContext was obtained.
     */
    @Test
    public void testStageWithContextCaptureThatRunsOnItsManagedExecutor() throws Exception {
        ManagedExecutor executor = ManagedExecutor.builder()
                        .maxAsync(2)
                        .propagated(ThreadContext.APPLICATION, TestContextTypes.CITY)
                        .cleared(ThreadContext.ALL_REMAINING)
                        .build();

        ThreadContext threadContext = executor.getThreadContext();

        CompletionStage<String> unmanagedStage = CompletableFuture.supplyAsync(() -> "City of ", testThreads);
        CompletionStage<String> copy = threadContext.withContextCapture(unmanagedStage);

        CountDownLatch blocker = new CountDownLatch(1);
        LinkedBlockingQueue<ManagedExecutor> lookupResults = new LinkedBlockingQueue<ManagedExecutor>();
        Function<String, String> getCity = s -> {
            try {
                // verify availability of application context
                lookupResults.add(InitialContext.doLookup("java:module/env/defaultExecutorRef"));

                blocker.await(TIMEOUT_NS * 3, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            } catch (NamingException x) {
                throw new CompletionException(x);
            }

            // verify availability of custom context
            s += CurrentLocation.getCity();
            // verify clearing of other custom context
            String state = CurrentLocation.getState();
            if (!"".equals(state))
                s += " in " + state;
            return s;
        };

        CurrentLocation.setLocation("Oronoco", "Minnesota");
        CompletionStage<String> dependentStage1 = copy.thenApplyAsync(getCity);
        CurrentLocation.setLocation("Pine Island", "Minnesota");
        CompletionStage<String> dependentStage2 = copy.thenApplyAsync(getCity);
        CurrentLocation.setLocation("Zumbrota", "Minnesota");
        CompletionStage<String> dependentStage3 = copy.thenApplyAsync(getCity);
        CurrentLocation.setLocation("Mazeppa", "Minnesota");
        CompletionStage<String> dependentStage4 = copy.thenApplyAsync(getCity);
        CurrentLocation.setLocation("Cannon Falls", "Minnesota");

        // wait for 2 dependent stages to be running
        assertNotNull(lookupResults.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(lookupResults.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // running of additional dependent stages must be blocked due to maxAsync
        assertNull(lookupResults.poll(2, TimeUnit.SECONDS));

        // allow all stages to complete
        blocker.countDown();
        assertNotNull(lookupResults.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNotNull(lookupResults.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals("City of Oronoco", dependentStage1.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals("City of Pine Island", dependentStage2.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals("City of Zumbrota", dependentStage3.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals("City of Mazeppa", dependentStage4.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }
}
