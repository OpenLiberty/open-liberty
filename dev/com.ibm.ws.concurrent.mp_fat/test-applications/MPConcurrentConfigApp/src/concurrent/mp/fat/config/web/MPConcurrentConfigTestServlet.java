/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.mp.fat.config.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.Test;
import org.test.context.location.CurrentLocation;
import org.test.context.location.TestContextTypes;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MPConcurrentConfigTestServlet")
public class MPConcurrentConfigTestServlet extends FATServlet {
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Inject
    ConcurrencyConfigBean bean;

    @Inject
    @Named("securityAndAppContextExecutor")
    ManagedExecutor securityContextExecutor;

    @Inject
    @Named("maxQueued3Executor")
    ManagedExecutor maxQueued3Executor;

    @Inject
    @Named("incompleteStageForSecurityContextTests")
    CompletionStage<LinkedBlockingQueue<String>> incompleteStageForSecurityContextTests;

    /**
     * Demonstrates that MicroProfile Config can be used by the application to override config properties.
     */
    @Test
    public void testApplicationProducedManagedExecutorUsingMicroProfileConfig() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch blocker = new CountDownLatch(1);
        CompletableFuture<Boolean> cf0 = securityContextExecutor.supplyAsync(() -> {
            try {
                started.countDown();
                return blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
        });
        assertTrue(started.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        CompletableFuture<Integer> cf1 = securityContextExecutor.supplyAsync(() -> 1);
        CompletableFuture<Integer> cf2 = securityContextExecutor.supplyAsync(() -> 2);

        try {
            Future<?> future = securityContextExecutor.submit(() -> System.out.println("This should never run!"));
            fail("Should not be able to queue third task when MicroProfile Config overrides maxQueued to be 2. " + future);
        } catch (RejectedExecutionException x) {
            // Pass - intentionally exceeded queue capacity in order to test maxQueued constraint
        }

        blocker.countDown();

        assertEquals(cf0.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), Boolean.TRUE);
        assertEquals(cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), Integer.valueOf(1));
        assertEquals(cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), Integer.valueOf(2));
    }

    /**
     * Verify that previously chained CompletionStage actions are able to access the UserPrincipal
     * using the security context that was captured when the CompletionStage action was created.
     */
    public void testCompletionStageAccessesUserPrincipal(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Principal principal = req.getUserPrincipal();
        assertEquals(principal == null ? null : principal.getName(), "user1");

        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
        incompleteStageForSecurityContextTests.toCompletableFuture().complete(queue);

        Set<String> users = new TreeSet<String>();
        String userA = queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(userA);
        users.add(userA);

        String userB = queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(userB);
        users.add(userB);

        String userC = queue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(userC);
        users.add(userC);

        assertEquals(new TreeSet<String>(Arrays.asList("user2", "user3", "unauthenticated")), users);

        // verify that security context is restored on current thread,
        principal = req.getUserPrincipal();
        assertEquals(principal == null ? null : principal.getName(), "user1");
    }

    /**
     * Chain a completion stage action that obtains the UserPrincipal from the servlet request.
     */
    public void testCreateCompletionStageThatRequiresUserPrincipal(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        incompleteStageForSecurityContextTests.thenAccept(q -> {
            Principal principal = req.getUserPrincipal();
            String user = principal == null ? "unauthenticated" : principal.getName();
            System.out.println("Completion stage found the following user: " + user);
            q.add(user);
        });
    }

    /**
     * Use MicroProfile Config to specify default context propagation, but explicitly override
     * this when using the builder. The configuration explicitly provided to the builder must
     * take precedence.
     */
    @Test
    public void testMPConfigDefaultsDoNotOverrideExplicitlySpecifiedValues() throws Exception {
        ManagedExecutor executor = ManagedExecutor.builder()
                        .propagated(TestContextTypes.STATE)
                        .build();

        CurrentLocation.setLocation("Rochester", "Minnesota");
        try {
            // Run on another thread to confirm propagated context
            CompletableFuture<Void> cf1 = executor.runAsync(() -> {
                assertEquals("Context type not propagated.", "Minnesota", CurrentLocation.getState());
            });
            cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            // Run on same thread to confirm cleared context
            CompletableFuture<Void> cf2 = cf1.thenRun(() -> {
                assertEquals("Context type not cleared.", "", CurrentLocation.getCity());
                CurrentLocation.setLocation("Madison", "Wisconsin");
            });
            cf2.join();

            assertEquals("Context type not restored.", "Rochester", CurrentLocation.getCity());
            assertEquals("Context type not restored.", "Minnesota", CurrentLocation.getState());
        } finally {
            CurrentLocation.clear();
            executor.shutdownNow();
        }
    }

    /**
     * Use MicroProfile Config to default the maxQueued attribute of a ManagedExecutor to 3.
     */
    @Test
    public void testMPConfigDefaultsMaxQueued() throws Exception {
        assertNotNull(maxQueued3Executor);

        // schedule task to block and wait for it to start
        Exchanger<String> status = new Exchanger<String>();
        Future<String> future0 = maxQueued3Executor.submit(() -> status.exchange(status.exchange("READY")));
        try {
            status.exchange("WAITING", TIMEOUT_NS, TimeUnit.NANOSECONDS);

            CompletableFuture<Integer> cf1 = maxQueued3Executor.supplyAsync(() -> 111);
            CompletableFuture<Integer> cf2 = maxQueued3Executor.supplyAsync(() -> 222);
            CompletableFuture<Integer> cf3 = maxQueued3Executor.supplyAsync(() -> 333);

            try {
                Future<?> future = maxQueued3Executor.submit(() -> System.out.println("This shouldn't be running!"));
                fail("Should not be able to run more than 1 task or have more than 3 queued. " + future);
            } catch (RejectedExecutionException x) {
                // Pass - intentionally exceeded queue capacity in order to test maxQueued constraint
            }

            // unblock the running task
            status.exchange("CONTINUE");

            assertEquals("CONTINUE", future0.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(Integer.valueOf(111), cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(Integer.valueOf(222), cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(Integer.valueOf(333), cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            if (future0.isDone())
                future0.cancel(true);
        }
    }

    /**
     * Use MicroProfile Config to specify the default propagated and cleared context types for
     * ManagedExecutor instances. This is verified by confirming that maxAsync=2, maxQueued=3
     * from microprofile-config.properties is enforced.
     */
    @Test
    public void testMPConfigSpecifiesDefaultAsyncAndQueuedMaximumsForManagedExecutor() throws Exception {
        // Defaults:
        // mp.context.ManagedExecutor.maxAsync=2
        // mp.context.ManagedExecutor.maxQueued=3
        ManagedExecutor executor = ManagedExecutor.builder().build();

        // schedule 2 tasks to block and wait for them to start
        Exchanger<String> status1 = new Exchanger<String>();
        Exchanger<String> status2 = new Exchanger<String>();
        Future<String> future1 = executor.submit(() -> status1.exchange(status1.exchange("READY_1")));
        Future<String> future2 = executor.submit(() -> status2.exchange(status2.exchange("READY_2")));

        try {
            status1.exchange("WAITING_1", TIMEOUT_NS, TimeUnit.NANOSECONDS);
            status2.exchange("WAITING_2", TIMEOUT_NS, TimeUnit.NANOSECONDS);

            CompletableFuture<Integer> cf3 = executor.supplyAsync(() -> 30);
            CompletableFuture<Integer> cf4 = executor.supplyAsync(() -> 40);
            CompletableFuture<Integer> cf5 = executor.supplyAsync(() -> 50);

            try {
                Future<?> future = executor.submit(() -> System.out.println("This shouldn't be running!"));
                fail("Should not be able to run more than 2 tasks or have more than 3 queued. " + future);
            } catch (RejectedExecutionException x) {
                // Pass - intentionally exceeded queue capacity in order to test maxQueued constraint
            }

            // unblock one of the the running tasks
            status1.exchange("CONTINUE_1");

            assertEquals("CONTINUE_1", future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(Integer.valueOf(30), cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(Integer.valueOf(40), cf4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(Integer.valueOf(50), cf5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // unblock the other running task
            status2.exchange("CONTINUE_2");

            assertEquals("CONTINUE_2", future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            if (future1.isDone())
                future1.cancel(true);
            if (future2.isDone())
                future2.cancel(true);
        }
    }

    /**
     * Use MicroProfile Config to specify the default propagated and cleared context types for ManagedExecutor instances.
     */
    @Test
    public void testMPConfigSpecifiesDefaultContextPropagationForManagedExecutor() throws Exception {
        // Defaults:
        // mp.context.ManagedExecutor.cleared=Transaction
        // mp.context.ManagedExecutor.propagated=City,Application
        ManagedExecutor executor = ManagedExecutor.builder()
                        .maxAsync(10)
                        .build();

        CurrentLocation.setLocation("Mantorville", "Minnesota");
        try {
            // Run on same thread to confirm cleared context
            CompletableFuture<Void> cf1 = executor.completedFuture("unused").thenAccept(s -> {
                assertEquals("Context type not cleared.", "", CurrentLocation.getState());
                CurrentLocation.setLocation("Tomah", "Wisconsin");
            });
            cf1.join();

            // Run on another thread to confirm propagated context
            CompletableFuture<Void> cf2 = cf1.thenRunAsync(() -> assertEquals("Context type not propagated.", "Mantorville", CurrentLocation.getCity()));
            cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            assertEquals("Context type not restored.", "Mantorville", CurrentLocation.getCity());
            assertEquals("Context type not restored.", "Minnesota", CurrentLocation.getState());
        } finally {
            CurrentLocation.clear();
            executor.shutdownNow();
        }
    }

    /**
     * Use MicroProfile Config to specify the default propagated and cleared context types for ThreadContext instances.
     */
    @Test
    public void testMPConfigSpecifiesDefaultContextPropagationForThreadContext() throws Exception {
        // Defaults:
        // mp.context.ThreadContext.cleared=
        // mp.context.ThreadContext.propagated=State
        // mp.context.ThreadContext.unchanged=Remaining
        ThreadContext threadContext = ThreadContext.builder().build();

        CurrentLocation.setLocation("Owatonna", "Minnesota");
        try {
            // Run on same thread to confirm cleared context
            Runnable test = threadContext.contextualRunnable(() -> {
                assertEquals("Context type not left unchanged.", "Oskaloosa", CurrentLocation.getCity());
                assertEquals("Context type not propagated.", "Minnesota", CurrentLocation.getState());
                CurrentLocation.setLocation("Wasau", "Wisconsin");
            });

            CurrentLocation.setLocation("Oskaloosa", "Iowa");

            test.run();

            assertEquals("Context type not restored.", "Wasau", CurrentLocation.getCity());
            assertEquals("Context type not restored.", "Iowa", CurrentLocation.getState());
        } finally {
            CurrentLocation.clear();
        }
    }

    /**
     * Verifies that a managed executor configured with cleared=SECURITY clears security context for async actions/tasks.
     */
    public void testSecurityContextCleared(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        ExecutorService executor = ManagedExecutor.builder()
                        .cleared(ThreadContext.SECURITY)
                        .build();
        try {
            Future<String> getUserName = executor.submit(() -> {
                Principal principal = req.getUserPrincipal();
                return principal == null ? null : principal.getName();
            });

            String user = getUserName.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertNull(user);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Verifies that a managed executor configured with cleared=ALL_REMAINING clears security context for async actions/tasks.
     */
    public void testSecurityContextClearedWhenAllRemaining(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        CompletableFuture<String> getUserName = maxQueued3Executor.completedFuture(20).thenApplyAsync(unused -> {
            Principal principal = req.getUserPrincipal();
            return principal == null ? null : principal.getName();
        });

        String user = getUserName.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNull(user);
    }

    /**
     * Verifies that a managed executor configured with propagated=ALL_REMAINING propagates security context to async actions/tasks.
     * Prerequisite: must invoke this test method as "user1"
     */
    public void testSecurityContextPropagatedWhenAllRemaining(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        ManagedExecutor executor = ManagedExecutor.builder()
                        .propagated(ThreadContext.ALL_REMAINING)
                        .build();
        try {
            CompletableFuture<String> getUserName = executor.supplyAsync(() -> {
                Principal principal = req.getUserPrincipal();
                return principal == null ? null : principal.getName();
            });

            String user = getUserName.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals("user1", user);
        } finally {
            executor.shutdownNow();
        }
    }
}
