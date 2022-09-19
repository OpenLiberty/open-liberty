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
package concurrent.mp.fat.v13.ee9.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MPContextProp1_3_TestServlet")
public class MPContextProp1_3_EE9_TestServlet extends FATServlet {
    // Create some exceptions here to keep the test method name out of the stack.
    // Tests can then check for the presence of their method name in the stack to verify
    // that the completable future is including the caller stack when raising exceptions.

    private static final CancellationException CANCELLATION_X_CAUSED_BY_ARRAY_X = //
                    (CancellationException) new CancellationException("Testing cancellation")
                                    .initCause(new ArrayIndexOutOfBoundsException("Provides a reason to cancel"));

    private static final CompletionException COMPLETION_X_CAUSED_BY_NAMING_X = new CompletionException(new NamingException("Testing exceptional completion"));

    /**
     * 2 minutes. Maximum number of nanoseconds to wait for a task or action to complete.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource(name = "java:app/env/defaultExecutorRef")
    private ManagedExecutor defaultManagedExecutor;

    /**
     * Executor that can be used when tests don't want to tie up threads from the Liberty global thread pool to perform test logic
     */
    private ExecutorService testThreads;

    @Override
    public void destroy() {
        AccessController.doPrivileged((PrivilegedAction<List<Runnable>>) () -> testThreads.shutdownNow());
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        testThreads = Executors.newFixedThreadPool(10);
    }

    /**
     * ManagedExecutor.copy was added in 1.2 and should still be working in 1.3.
     * With this method, you should be able to copy from an unmanaged completion stage
     * that lacks context propagation to create a copied stage that uses the
     * managed executor to propagate thread context.
     */
    @Test
    public void testCopy() throws Exception {
        CompletableFuture<StringBuilder> unmanagedStage = CompletableFuture.supplyAsync(() -> new StringBuilder());

        CompletableFuture<StringBuilder> managedStage = defaultManagedExecutor
                        .copy(unmanagedStage)
                        .thenApplyAsync(b -> {
                            try {
                                return b.append((ManagedExecutor) InitialContext.doLookup("java:app/env/defaultExecutorRef"));
                            } catch (NamingException x) {
                                throw new CompletionException(x);
                            }
                        });
        StringBuilder b = managedStage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertTrue(b.toString(), b.toString().startsWith("ManagedExecutor@"));
    }

    /**
     * Verify that custom thread context types are not propagated by the default ManagedExecutorService.
     */
    @Test
    public void testCustomContextIsNotPropagatedByDefault() throws Exception {
        int defaultPriority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(defaultPriority - 1);
        try {
            CompletableFuture<Integer> executorThreadPriority = defaultManagedExecutor.completedFuture(0)
                            .thenApplyAsync(i -> i + Thread.currentThread().getPriority());
            assertEquals(Integer.valueOf(defaultPriority), executorThreadPriority.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            Thread.currentThread().setPriority(defaultPriority);
        }
    }

    /**
     * Verify that custom thread context types are propagated if configured on the builder.
     */
    @Test
    public void testCustomContextIsPropagatedWhenConfigured() throws Exception {
        ManagedExecutor executor = ManagedExecutor.builder()
                        .cleared(ThreadContext.SECURITY, ThreadContext.TRANSACTION)
                        .propagated(ThreadContext.ALL_REMAINING)
                        .build();
        try {
            CompletableFuture<String> initialStage = executor.newIncompleteFuture();
            CompletableFuture<String> executorThreadPriorities;

            int defaultPriority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(defaultPriority - 1);
            try {
                executorThreadPriorities = initialStage.thenApplyAsync(s -> s + Thread.currentThread().getPriority());

                Thread.currentThread().setPriority(defaultPriority - 2);

                executorThreadPriorities = executorThreadPriorities.thenApply(s -> s + ' ' + Thread.currentThread().getPriority());
            } finally {
                Thread.currentThread().setPriority(defaultPriority);
            }
            initialStage.complete("");

            String expected = (defaultPriority - 1) + " " + (defaultPriority - 2);
            assertEquals(expected, executorThreadPriorities.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            executor.shutdown();
        }
    }

    /**
     * When CompletionException is raised from the getNow method, it should show the
     * stack of the getNow attempt along with the chained exception for the actual error.
     */
    @Test
    public void testFailureStackShowsCallerOfGetNow() throws Exception {
        CompletableFuture<String> future = defaultManagedExecutor.failedFuture(CANCELLATION_X_CAUSED_BY_ARRAY_X);

        try {
            String result = future.getNow("result-if-not-done");
            fail("Got result " + result + " for a failed future");
        } catch (CancellationException x) {
            boolean foundInStack = false;
            for (StackTraceElement line : x.getStackTrace())
                foundInStack |= "testFailureStackShowsCallerOfGetNow".equals(line.getMethodName());
            if (!foundInStack || x.getCause() != CANCELLATION_X_CAUSED_BY_ARRAY_X.getCause())
                throw x;
        }

        future = defaultManagedExecutor.failedFuture(COMPLETION_X_CAUSED_BY_NAMING_X);

        try {
            String result = future.getNow("result-if-not-done");
            fail("Got result " + result + " for a failed future");
        } catch (CompletionException x) {
            boolean foundInStack = false;
            for (StackTraceElement line : x.getStackTrace())
                foundInStack |= "testFailureStackShowsCallerOfGetNow".equals(line.getMethodName());
            if (!foundInStack || x.getCause() != COMPLETION_X_CAUSED_BY_NAMING_X.getCause())
                throw x;
        }
    }

    /**
     * When CompletionException is raised from the join method, it should show the
     * stack of the join attempt along with the chained exception for the actual error.
     */
    @Test
    public void testFailureStackShowsCallerOfJoin() throws Exception {
        CompletableFuture<?> future = defaultManagedExecutor.failedFuture(CANCELLATION_X_CAUSED_BY_ARRAY_X);

        try {
            Object result = future.join();
            fail("Got result " + result + " by joining a failed future");
        } catch (CancellationException x) {
            boolean foundInStack = false;
            for (StackTraceElement line : x.getStackTrace())
                foundInStack |= "testFailureStackShowsCallerOfJoin".equals(line.getMethodName());
            if (!foundInStack || x.getCause() != CANCELLATION_X_CAUSED_BY_ARRAY_X.getCause())
                throw x;
        }

        future = defaultManagedExecutor.failedFuture(COMPLETION_X_CAUSED_BY_NAMING_X);

        try {
            Object result = future.join();
            fail("Got result " + result + " by joining a failed future");
        } catch (CompletionException x) {
            boolean foundInStack = false;
            for (StackTraceElement line : x.getStackTrace())
                foundInStack |= "testFailureStackShowsCallerOfJoin".equals(line.getMethodName());
            if (!foundInStack || x.getCause() != COMPLETION_X_CAUSED_BY_NAMING_X.getCause())
                throw x;
        }
    }

    /**
     * When CompletionException is raised from the timed get method, it should show the
     * stack of the get attempt along with the chained exception for the actual error.
     */
    @Test
    public void testFailureStackShowsCallerOfTimedGet() throws Exception {
        CompletableFuture<String> future = defaultManagedExecutor.failedFuture(COMPLETION_X_CAUSED_BY_NAMING_X);

        try {
            String result = future.get(3, TimeUnit.MILLISECONDS);
            fail("Got result " + result + " for a failed future");
        } catch (ExecutionException x) {
            boolean foundInStack = false;
            for (StackTraceElement line : x.getStackTrace())
                foundInStack |= "testFailureStackShowsCallerOfTimedGet".equals(line.getMethodName());
            if (!foundInStack || x.getCause() != COMPLETION_X_CAUSED_BY_NAMING_X.getCause())
                throw x;
        }

        future = defaultManagedExecutor.failedFuture(CANCELLATION_X_CAUSED_BY_ARRAY_X);

        try {
            String result = future.get(4, TimeUnit.MILLISECONDS);
            fail("Got result " + result + " for a failed future");
        } catch (CancellationException x) {
            boolean foundInStack = false;
            for (StackTraceElement line : x.getStackTrace())
                foundInStack |= "testFailureStackShowsCallerOfTimedGet".equals(line.getMethodName());
            if (!foundInStack || x.getCause() != CANCELLATION_X_CAUSED_BY_ARRAY_X.getCause())
                throw x;
        }
    }

    /**
     * When CompletionException is raised from the untimed get method, it should show the
     * stack of the get attempt along with the chained exception for the actual error.
     */
    @Test
    public void testFailureStackShowsCallerOfUntimedGet() throws Exception {
        CompletableFuture<String> future = defaultManagedExecutor.failedFuture(COMPLETION_X_CAUSED_BY_NAMING_X);

        try {
            String result = future.get();
            fail("Got result " + result + " for a failed future");
        } catch (ExecutionException x) {
            boolean foundInStack = false;
            for (StackTraceElement line : x.getStackTrace())
                foundInStack |= "testFailureStackShowsCallerOfUntimedGet".equals(line.getMethodName());
            if (!foundInStack || x.getCause() != COMPLETION_X_CAUSED_BY_NAMING_X.getCause())
                throw x;
        }

        future = defaultManagedExecutor.failedFuture(CANCELLATION_X_CAUSED_BY_ARRAY_X);

        try {
            String result = future.get();
            fail("Got result " + result + " for a failed future");
        } catch (CancellationException x) {
            boolean foundInStack = false;
            for (StackTraceElement line : x.getStackTrace())
                foundInStack |= "testFailureStackShowsCallerOfUntimedGet".equals(line.getMethodName());
            if (!foundInStack || x.getCause() != CANCELLATION_X_CAUSED_BY_ARRAY_X.getCause())
                throw x;
        }
    }

    /**
     * ThreadContext.withContextCapture was enhanced in 1.2 to enable the copied completion stages
     * to have async dependent stages that run on the product's default executor,
     * which for us is the Liberty global thread pool.
     * This enhancement should continue to be in place for 1.3.
     */
    @Test
    public void testWithContextCapture() throws Exception {
        ThreadContext contextualizer = ThreadContext.builder()
                        .propagated(ThreadContext.APPLICATION, "Priority")
                        .cleared(ThreadContext.TRANSACTION)
                        .unchanged(ThreadContext.ALL_REMAINING)
                        .build();

        CompletableFuture<StringBuilder> unmanagedStage = new CompletableFuture<StringBuilder>();
        CompletableFuture<Void> managedStage;

        int originalPriority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(2);
        try {
            managedStage = contextualizer
                            .withContextCapture(unmanagedStage)
                            .thenAcceptAsync(b -> {
                                try {
                                    b.append("priority ")
                                                    .append(Thread.currentThread().getPriority())
                                                    .append(", looked up ")
                                                    .append((ManagedExecutor) InitialContext.doLookup("java:app/env/defaultExecutorRef"));
                                } catch (NamingException x) {
                                    throw new CompletionException(x);
                                }
                            });
        } finally {
            Thread.currentThread().setPriority(originalPriority);
        }

        StringBuilder b = new StringBuilder();
        unmanagedStage.complete(b);

        managedStage.join();

        assertTrue(b.toString(), b.toString().startsWith("priority 2, looked up ManagedExecutor@"));
    }
}