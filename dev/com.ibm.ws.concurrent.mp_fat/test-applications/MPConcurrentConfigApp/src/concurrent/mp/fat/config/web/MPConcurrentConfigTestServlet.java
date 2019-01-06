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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.NamedInstance;
import org.junit.Test;

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
}
