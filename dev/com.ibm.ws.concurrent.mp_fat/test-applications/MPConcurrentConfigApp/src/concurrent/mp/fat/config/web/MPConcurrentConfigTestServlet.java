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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;
import org.eclipse.microprofile.concurrent.NamedInstance;
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
    @ManagedExecutorConfig(maxAsync = 1)
    ManagedExecutor annotatedExecutorWithMPConfigOverride; // MP Config sets maxAsync=2, maxQueued=3

    @Inject
    @NamedInstance("containerExecutorReturnedByAppProducer")
    ManagedExecutor containerExecutorReturnedByAppProducer;

    @Inject
    ManagedExecutor executorWithMPConfigOverride; // MP Config sets cleared=City, propagated=Application,State

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
                fail("Should not be able to run more than 1 task or have more then 3 queued. " + future);
            } catch (RejectedExecutionException x) {
                // Pass - intentionally exceeded queue capacity in order to test maxQueued constraint
            }

            // unblock the running task
            status.exchange("CONTINUE");

            assertEquals(future0.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), "CONTINUE");
            assertEquals(cf1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), Integer.valueOf(111));
            assertEquals(cf2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), Integer.valueOf(222));
            assertEquals(cf3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS), Integer.valueOf(333));
        } finally {
            if (future0.isDone())
                future0.cancel(true);
        }
    }
}
