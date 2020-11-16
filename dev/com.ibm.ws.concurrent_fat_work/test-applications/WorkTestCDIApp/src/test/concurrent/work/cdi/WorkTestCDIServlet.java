/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrent.work.cdi;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.Test;

import componenttest.app.FATServlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import test.concurrent.work.Work;
import test.concurrent.work.WorkCompletedException;
import test.concurrent.work.WorkItem;
import test.concurrent.work.WorkManager;
import test.concurrent.work.WorkRejectedException;

@ApplicationScoped
@SuppressWarnings("serial")
@WebServlet("/*")
public class WorkTestCDIServlet extends FATServlet {
    /**
     * Interval in milliseconds between polling for work to complete.
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Maximum number of nanoseconds to wait for work to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource(name = "wm/default") // Relies on default binding. Does not use CDI.
    private WorkManager wmDefault;

    @Inject
    private WorkManager wmInjectedByCDI;

    @Resource(lookup = "wm/schedExec")
    @Produces
    private WorkManager wmProduced;

    /**
     * CDI injection of a WorkManager that is obtained via a CDI producer.
     */
    @Test
    public void testInjectWorkManager(HttpServletRequest request, HttpServletResponse response) throws Exception {
        assertNotNull(wmInjectedByCDI);

        // Verify that work runs when scheduled
        CompletableFuture<Class<?>> result = new CompletableFuture<>();
        wmInjectedByCDI.schedule(() -> {
            try {
                // Requires class loader context
                result.complete(Thread.currentThread().getContextClassLoader().loadClass(WorkTestCDIServlet.class.getName()));
            } catch (ClassNotFoundException x) {
                throw new CompletionException(x);
            }
        });
        assertEquals(WorkTestCDIServlet.class, result.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * WorkManager obtained via resource injection also implements ManagedExecutor (from MicroProfile)
     * and ManagedExecutorService, and honors the configured max concurrency and start timeout.
     */
    @Test
    public void testWorkManagerIsCastableToMicroProfileManagedExecutor(HttpServletRequest request, HttpServletResponse response) throws Exception {
        assertNotNull(wmDefault);

        assertTrue(wmDefault.toString(), wmDefault instanceof ExecutorService);
        assertTrue(wmDefault.toString(), wmDefault instanceof ManagedExecutor);
        assertTrue(wmDefault.toString(), wmDefault instanceof ManagedExecutorService);

        // Verify that the correct instance is used (max concurrency is 2, startTimeout is 10s)
        CountDownLatch blocker = new CountDownLatch(1);
        Exchanger<String> status = new Exchanger<>();

        CompletableFuture<String> cf = ((ManagedExecutor) wmDefault).supplyAsync(() -> {
            try {
                assertTrue(blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                // Require both to be running at the same time:
                return status.exchange("task is running", TIMEOUT_NS, TimeUnit.NANOSECONDS);
            } catch (InterruptedException | TimeoutException x) {
                throw new CompletionException(x);
            }
        });

        LinkedBlockingQueue<String> workResults = new LinkedBlockingQueue<>();
        WorkItem item1 = wmDefault.schedule(() -> {
            try {
                assertTrue(blocker.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                // Require both to be running at the same time:
                workResults.add(status.exchange("work is running", TIMEOUT_NS, TimeUnit.NANOSECONDS));
            } catch (InterruptedException | TimeoutException x) {
                throw new CompletionException(x);
            }
        });

        WorkItem item2 = wmDefault.schedule(() -> workResults.add("more work is running"));

        // Wait for startTimeout to reject item2.
        try {
            for (long start = System.nanoTime(); item2.getResult() == null && System.nanoTime() - start < TIMEOUT_NS; )
                Thread.sleep(POLL_INTERVAL);
            fail("startTimeout exceeded without failing the work");
        } catch (WorkCompletedException x) {
            if (x.getCause() == null || //
                    !"StartTimeoutException".equals(x.getCause().getClass().getSimpleName()) || //
                    x.getCause().getMessage() == null || //
                    !x.getCause().getMessage().startsWith("CWWKE1205E")) // message for startTimeout exceeded
            throw x;
        }

        blocker.countDown();

        assertEquals("task is running", workResults.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals("work is running", cf.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNull(workResults.poll());
    }
}
