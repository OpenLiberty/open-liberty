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
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;
import org.eclipse.microprofile.concurrent.NamedInstance;
import org.eclipse.microprofile.concurrent.ThreadContext;
import org.junit.Test;
import org.test.context.location.CurrentLocation;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MPConcurrentConfigTestServlet")
public class MPConcurrentConfigTestServlet extends FATServlet {
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Inject
    ConcurrencyConfigBean bean;

    @Inject
    @NamedInstance("applicationProducedExecutor")
    ManagedExecutor appProducedExecutor;

    @Inject
    @ManagedExecutorConfig(maxAsync = 1, cleared = { ThreadContext.TRANSACTION, ThreadContext.SECURITY })
    ManagedExecutor annotatedExecutorWithMPConfigOverride; // MP Config sets maxAsync=2, maxQueued=3

    @Inject
    @NamedInstance("containerExecutorReturnedByAppProducer")
    ManagedExecutor containerExecutorReturnedByAppProducer;

    @Inject
    @NamedInstance("containerExecutorWithNameAndConfig")
    @ManagedExecutorConfig(maxQueued = 9, cleared = ThreadContext.ALL_REMAINING, propagated = "State") // MP Config sets maxAsync=1, maxQueued=2
    ManagedExecutor containerExecutorWithNameAndConfig;

    @Inject
    ManagedExecutor executorWithMPConfigOverride; // MP Config sets cleared=City, propagated=Application,State

    @Inject
    @NamedInstance("incompleteStageForSecurityContextTests")
    CompletionStage<LinkedBlockingQueue<String>> incompleteStageForSecurityContextTests;

    /**
     * Demonstrates that MicroProfile Config can be used by the application to override config properties.
     */
    @Test
    public void testApplicationProducedManagedExecutorUsingMicroProfileConfig() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch blocker = new CountDownLatch(1);
        CompletableFuture<Boolean> cf0 = appProducedExecutor.supplyAsync(() -> {
            try {
                started.countDown();
                return blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
        });
        assertTrue(started.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        CompletableFuture<Integer> cf1 = appProducedExecutor.supplyAsync(() -> 1);
        CompletableFuture<Integer> cf2 = appProducedExecutor.supplyAsync(() -> 2);

        try {
            Future<?> future = appProducedExecutor.submit(() -> System.out.println("This should never run!"));
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
     * Use MicroProfile Config to override config properties of a ManagedExecutor injection point
     * that is annotated with ManagedExecutorConfig and does not have any qualifiers.
     */
    @Test
    public void testMPConfigOverridesAnnotatedManagedExecutor() throws Exception {
        assertNotNull(annotatedExecutorWithMPConfigOverride);

        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch blocker = new CountDownLatch(1);
        CompletableFuture<Boolean> cf0 = annotatedExecutorWithMPConfigOverride.supplyAsync(() -> {
            try {
                started.countDown();
                return blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
        });
        CompletableFuture<Boolean> cf1 = annotatedExecutorWithMPConfigOverride.supplyAsync(() -> {
            try {
                started.countDown();
                return blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
        });
        assertTrue(started.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        CompletableFuture<Integer> cf2 = annotatedExecutorWithMPConfigOverride.supplyAsync(() -> 20);
        CompletableFuture<Integer> cf3 = annotatedExecutorWithMPConfigOverride.supplyAsync(() -> 30);
        CompletableFuture<Integer> cf4 = annotatedExecutorWithMPConfigOverride.supplyAsync(() -> 40);

        try {
            Future<?> future = annotatedExecutorWithMPConfigOverride.submit(() -> System.out.println("This should not run!"));
            fail("Should not be able to run more than 2 tasks or have more then 3 queued. " + future);
        } catch (RejectedExecutionException x) {
            // Pass - intentionally exceeded queue capacity in order to test maxQueued constraint
        }

        blocker.countDown();

        assertEquals(cf0.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), Boolean.TRUE);
        assertEquals(cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), Boolean.TRUE);
        assertEquals(cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), Integer.valueOf(20));
        assertEquals(cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), Integer.valueOf(30));
        assertEquals(cf4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), Integer.valueOf(40));
    }

    /**
     * Use MicroProfile Config to override config properties of a ManagedExecutor injection point
     * that is not annotated with ManagedExecutorConfig and does not have any qualifiers.
     */
    @Test
    public void testMPConfigOverridesInjectedManagedExecutor() throws Exception {
        assertNotNull(executorWithMPConfigOverride);

        CurrentLocation.setLocation("Rochester", "Minnesota");
        try {
            // Run on another thread to confirm propagated context
            CompletableFuture<Void> cf1 = executorWithMPConfigOverride.runAsync(() -> {
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
        }
    }

    /**
     * Use MicroProfile Config to override config properties of a ManagedExecutor injection point
     * that is annotated with ManagedExecutorConfig and the NamedInstance qualifier.
     */
    @Test
    public void testMPConfigOverridesManagedExecutorWithConfigAndName() throws Exception {
        assertNotNull(containerExecutorWithNameAndConfig);

        CurrentLocation.setLocation("Stewartville", "Minnesota");
        try {
            // First, verify ManagedExecutorConfig attributes that are not overridden by MicroProfile Config

            // Run on same thread to confirm cleared context
            CompletableFuture<Void> cf1 = containerExecutorWithNameAndConfig.completedFuture(null).thenRun(() -> {
                assertEquals("Context type not cleared.", "", CurrentLocation.getCity());
                CurrentLocation.setLocation("Sparta", "Wisconsin");
            });
            cf1.join();

            // Run on another thread to confirm propagated context
            CompletableFuture<Void> cf2 = cf1.thenRun(() -> {
                assertEquals("Context type not propagated.", "Minnesota", CurrentLocation.getState());
            });
            cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            assertEquals("Context type not restored.", "Stewartville", CurrentLocation.getCity());
            assertEquals("Context type not restored.", "Minnesota", CurrentLocation.getState());

            // Verify that MicroProfile Config overrides of maxAsync/maxQueued are honored

            // schedule task to block and wait for it to start
            Exchanger<String> status = new Exchanger<String>();
            Future<String> future3 = containerExecutorWithNameAndConfig.submit(() -> status.exchange(status.exchange("READY")));
            try {
                status.exchange("WAITING", TIMEOUT_NS, TimeUnit.NANOSECONDS);

                CompletableFuture<Integer> cf4 = cf2.thenApplyAsync(unused -> 44);
                CompletableFuture<Integer> cf5 = cf2.thenApplyAsync(unused -> 55);

                try {
                    Future<?> future6 = containerExecutorWithNameAndConfig.submit(() -> System.out.println("This shouldn't be able to run!"));
                    fail("Should not be able to run more than 1 task or have more than 2 queued. " + future6);
                } catch (RejectedExecutionException x) {
                    // Pass - intentionally exceeded queue capacity in order to test maxQueued constraint
                }

                // unblock the running task
                status.exchange("CONTINUE");

                assertEquals("CONTINUE", future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertEquals(Integer.valueOf(44), cf4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertEquals(Integer.valueOf(55), cf5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            } finally {
                if (future3.isDone())
                    future3.cancel(true);
            }

        } finally {
            CurrentLocation.clear();
        }
    }

    /**
     * Use MicroProfile Config to override config properties of a ManagedExecutor injection point
     * that is a parameter of a Producer method. This is verified by confirming that maxQueued=3
     * from microprofile-config.properties is enforced.
     */
    @Test
    public void testMPConfigOverridesManagedExecutorConfigOnParameter() throws Exception {
        assertNotNull(containerExecutorReturnedByAppProducer);

        // schedule task to block and wait for it to start
        Exchanger<String> status = new Exchanger<String>();
        Future<String> future0 = containerExecutorReturnedByAppProducer.submit(() -> status.exchange(status.exchange("READY")));
        try {
            status.exchange("WAITING", TIMEOUT_NS, TimeUnit.NANOSECONDS);

            CompletableFuture<Integer> cf1 = containerExecutorReturnedByAppProducer.supplyAsync(() -> 111);
            CompletableFuture<Integer> cf2 = containerExecutorReturnedByAppProducer.supplyAsync(() -> 222);
            CompletableFuture<Integer> cf3 = containerExecutorReturnedByAppProducer.supplyAsync(() -> 333);

            try {
                Future<?> future = containerExecutorReturnedByAppProducer.submit(() -> System.out.println("This shouldn't be running!"));
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
     * Verifies that a managed executor configured with cleared=SECURITY clears security context for async actions/tasks.
     */
    public void testSecurityContextCleared(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Future<String> getUserName = annotatedExecutorWithMPConfigOverride.submit(() -> {
            Principal principal = req.getUserPrincipal();
            return principal == null ? null : principal.getName();
        });

        String user = getUserName.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNull(user);
    }

    /**
     * Verifies that a managed executor configured with cleared=ALL_REMAINING clears security context for async actions/tasks.
     */
    public void testSecurityContextClearedWhenAllRemaining(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        CompletableFuture<String> getUserName = containerExecutorWithNameAndConfig.completedFuture(20).thenApplyAsync(unused -> {
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
        CompletableFuture<String> getUserName = containerExecutorReturnedByAppProducer.supplyAsync(() -> {
            Principal principal = req.getUserPrincipal();
            return principal == null ? null : principal.getName();
        });

        String user = getUserName.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("user1", user);
    }
}
