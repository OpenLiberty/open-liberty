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
package test.concurrent.work.app;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.app.FATServlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import test.concurrent.work.Work;
import test.concurrent.work.WorkCompletedException;
import test.concurrent.work.WorkItem;
import test.concurrent.work.WorkManager;
import test.concurrent.work.WorkRejectedException;

@SuppressWarnings("serial")
@WebServlet("/*")
public class WorkTestServlet extends FATServlet {
    /**
     * Interval in milliseconds between polling for work to complete.
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Maximum number of nanoseconds to wait for work to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource(lookup = "wm/executor", name = "java:module/env/wm/executorServiceRef")
    private ExecutorService executorService;

    @Resource(lookup = "wm/scheduledExecutor", name = "java:app/env/wm/scheduledExecutorServiceRef")
    private ScheduledExecutorService scheduledExecutorService;

    @Resource(lookup = "wm/executor", name = "java:global/env/wm/wmExecutorRef")
    private WorkManager wmExecutor;

    @Resource(lookup = "wm/scheduledExecutor")
    private WorkManager wmScheduledExecutor;

    // Defined in web.xml: java:comp/env/wm/scheduledExecutorRef

    /**
     * Direct lookup of a configured managedExecutorService as a WorkManager.
     */
    @Test
    public void testDirectLookupManagedExecutorServiceAsWorkManager(HttpServletRequest request, HttpServletResponse response) throws Exception {
        WorkManager wm = InitialContext.doLookup("wm/executor");
        assertNotNull(wm);
        assertTrue(wm.toString(), wm instanceof ExecutorService);
        assertTrue(wm.toString(), wm instanceof ManagedExecutorService);

        // Verify that work runs when scheduled
        CompletableFuture<Boolean> result = new CompletableFuture<Boolean>();
        wm.schedule(() -> result.complete(true));
        assertTrue(result.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Direct lookup of a configured managedScheduledExecutorService as a WorkManager.
     */
    @Test
    public void testDirectLookupManagedScheduledExecutorServiceAsWorkManager(HttpServletRequest request, HttpServletResponse response) throws Exception {
        WorkManager wm = InitialContext.doLookup("wm/scheduledExecutor");
        assertNotNull(wm);
        assertTrue(wm.toString(), wm instanceof ScheduledExecutorService);
        assertTrue(wm.toString(), wm instanceof ManagedScheduledExecutorService);

        // Verify that work runs with the submitting application's context when scheduled
        CompletableFuture<WorkManager> result = new CompletableFuture<WorkManager>();
        wm.schedule(() -> {
            try {
                // Requires JEE metadata context of the application:
                result.complete(InitialContext.doLookup("java:app/env/wm/scheduledExecutorServiceRef"));
            } catch (NamingException x) {
                throw new CompletionException(x);
            }
        });
        assertNotNull(result.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Inject a configured managedExecutorService and cast it to WorkManager
     */
    @Test
    public void testInjectedManagedExecutorIsCastableToWorkManager(HttpServletRequest request, HttpServletResponse response) throws Exception {
        assertNotNull(executorService);
        assertTrue(executorService.toString(), executorService instanceof ManagedExecutorService);

        WorkManager wm = (WorkManager) executorService;

        CountDownLatch remaining = new CountDownLatch(1);
        Work work = () -> remaining.countDown();
        WorkItem item = wm.schedule(work);
        Work w;
        for (long start = System.nanoTime(); (w = item.getResult()) == null && System.nanoTime() - start < TIMEOUT_NS; )
            Thread.sleep(POLL_INTERVAL);
        assertEquals(work, w);
        assertEquals(0, remaining.getCount());
    }

    /**
     * Inject a configured managedScheduledExecutorService and cast it to WorkManager
     */
    @Test
    public void testInjectedManagedScheduledExecutorIsCastableToWorkManager(HttpServletRequest request, HttpServletResponse response) throws Exception {
        assertNotNull(scheduledExecutorService);
        assertTrue(scheduledExecutorService.toString(), scheduledExecutorService instanceof ManagedScheduledExecutorService);

        WorkManager wm = (WorkManager) scheduledExecutorService;

        Work work = () -> { throw new ArrayIndexOutOfBoundsException("This error is intentionally raised by the work."); };
        WorkItem item = wm.schedule(work);
        try {
            for (long start = System.nanoTime(); item.getResult() == null && System.nanoTime() - start < TIMEOUT_NS; )
                Thread.sleep(POLL_INTERVAL);

            fail("Work " + work + " did not run and raise expected error within the allotted interval.");
        } catch (WorkCompletedException x) {
            if (x.getCause() == null || !(x.getCause() instanceof ArrayIndexOutOfBoundsException))
                throw x;
        }
    }

    /**
     * Inject a configured managedExecutorService as a WorkManager.
     */
    @Test
    public void testInjectManagedExecutorAsWorkManager(HttpServletRequest request, HttpServletResponse response) throws Exception {
        assertNotNull(wmExecutor);
        assertTrue(wmExecutor.toString(), wmExecutor instanceof ExecutorService);
        assertTrue(wmExecutor.toString(), wmExecutor instanceof ManagedExecutorService);

        Work work = () -> {
            try {
                // requires the classloader context:
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                assertEquals(WorkTestServlet.class, loader.loadClass(WorkTestServlet.class.getName()));

                // requires the JEE metadata context, which we intentionally didn't configure
                Object lookupResult = InitialContext.doLookup("java:module/env/wm/executorServiceRef");
                fail("Without JEE metadata context, it should not be possible to look up " + lookupResult);
            } catch (ClassNotFoundException x) {
                throw new CompletionException(x);
            } catch (NamingException x) {
                throw new CompletionException(x);
            }
        };

        WorkItem item = wmExecutor.schedule(work);
        try {
            for (long start = System.nanoTime(); item.getResult() == null && System.nanoTime() - start < TIMEOUT_NS; )
                Thread.sleep(POLL_INTERVAL);

            fail("Work " + work + " did not run and raise expected error within the allotted interval.");
        } catch (WorkCompletedException x) {
            if (x.getCause() == null || !(x.getCause() instanceof CompletionException)
                    || !(x.getCause().getCause() instanceof NamingException))
                throw x;
        }
    }

    /**
     * Inject a configured managedScheduledExecutorService as a WorkManager.
     */
    @Test
    public void testInjectManagedScheduledExecutorAsWorkManager(HttpServletRequest request, HttpServletResponse response) throws Exception {
        assertNotNull(wmScheduledExecutor);
        assertTrue(wmScheduledExecutor.toString(), wmScheduledExecutor instanceof ScheduledExecutorService);
        assertTrue(wmScheduledExecutor.toString(), wmScheduledExecutor instanceof ManagedScheduledExecutorService);

        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();

        Work work = () -> {
            try {
                // requires the JEE metadata context:
                results.add(InitialContext.doLookup("java:module/env/wm/executorServiceRef"));

                // requires the classloader context:
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                results.add(loader.loadClass(WorkTestServlet.class.getName()));
            } catch (ClassNotFoundException x) {
                throw new CompletionException(x);
            } catch (NamingException x) {
                throw new CompletionException(x);
            }
        };

        WorkItem item = wmScheduledExecutor.schedule(work);

        Object lookupResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        if (lookupResult == null)
            fail("Work did not run within allotted interval. Result is now: " + item.getResult());
        assertNotNull(lookupResult);

        Object classLoadResult = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        if (classLoadResult == null)
            fail("Work did not finish within allotted interval. Result is now: " + item.getResult());
        assertEquals(WorkTestServlet.class, classLoadResult);

        Work w;
        for (long start = System.nanoTime(); (w = item.getResult()) == null && System.nanoTime() - start < TIMEOUT_NS; )
            Thread.sleep(POLL_INTERVAL);
        assertEquals(work, w);
    }

    /**
     * Look up the default instance java:comp/DefaultManagedExecutorService as a WorkManager.
     */
    @Test
    public void testLookUpDefaultManagedExecutorServiceAsWorkManager(HttpServletRequest request, HttpServletResponse response) throws Exception {
        WorkManager wm = InitialContext.doLookup("java:comp/DefaultManagedExecutorService");
        assertNotNull(wm);
        assertTrue(wm.toString(), wm instanceof ExecutorService);
        assertTrue(wm.toString(), wm instanceof ManagedExecutorService);

        // Schedule 3 work items to run at the same time
        AtomicReference<Object> resultRef = new AtomicReference<Object>();
        CyclicBarrier allWorkRunning = new CyclicBarrier(3, () -> {
            try {
                // Requires JEE metadata context of the test app:
                resultRef.getAndSet(InitialContext.doLookup("java:app/env/wm/scheduledExecutorServiceRef"));
            } catch (NamingException x) {
                resultRef.getAndSet(x);
            }
        });

        Work work = () -> {
            try {
                allWorkRunning.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            } catch (BrokenBarrierException | InterruptedException | TimeoutException x) {
                throw new CompletionException(x);
            }
        };

        WorkItem item1 = wm.schedule(work);
        WorkItem item2 = wm.schedule(work);
        WorkItem item3 = wm.schedule(work);

        for (long start = System.nanoTime(); //
                (item1.getResult() == null || item2.getResult() == null || item3.getResult() == null) && System.nanoTime() - start < TIMEOUT_NS; )
            Thread.sleep(POLL_INTERVAL);

        assertEquals(work, item1.getResult());
        assertEquals(work, item2.getResult());
        assertEquals(work, item3.getResult());

        Object result = resultRef.get();
        assertNotNull(result);
        if (result instanceof Throwable)
            throw new Exception((Throwable) result);
        assertTrue(result.toString(), result instanceof WorkManager);
    }

    /**
     * Look up the default instance java:comp/DefaultManagedScheduledExecutorService as a WorkManager.
     */
    @Test
    public void testLookUpDefaultManagedScheduledExecutorServiceAsWorkManager(HttpServletRequest request, HttpServletResponse response) throws Exception {
        WorkManager wm = InitialContext.doLookup("java:comp/DefaultManagedScheduledExecutorService");
        assertNotNull(wm);
        assertTrue(wm.toString(), wm instanceof ScheduledExecutorService);
        assertTrue(wm.toString(), wm instanceof ManagedScheduledExecutorService);

        LinkedBlockingQueue<Object> resultQueue = new LinkedBlockingQueue<Object>();

        Work lookupWork = () -> {
            try {
                // Requires JEE metadata context:
                resultQueue.add(InitialContext.doLookup("java:comp/env/wm/scheduledExecutorRef"));
            } catch (NamingException x) {
                resultQueue.add(x);
            }
        };

        Work schedulingWork = () -> {
            try {
                wm.schedule(lookupWork);
            } catch (WorkRejectedException x) {
                throw new CompletionException(x);
            }
        };

        wm.schedule(schedulingWork);

        Object result = resultQueue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(result);
        if (result instanceof Throwable)
            throw new Exception((Throwable) result);
        assertTrue(result.toString(), result instanceof WorkManager);
    }

    /**
     * Perform a lookup of a managedExecutorService using an annotatively-defined resource reference
     * that specifies the resource type as WorkManager.
     */
    @Test
    public void testWorkManagerResourceReferenceLookupOfManagedExecutorService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        WorkManager wm = InitialContext.doLookup("java:global/env/wm/wmExecutorRef");
        assertNotNull(wm);
        assertTrue(wm.toString(), wm instanceof ExecutorService);
        assertTrue(wm.toString(), wm instanceof ManagedExecutorService);

        Phaser workStarted = new Phaser(1);

        // Attempt to exceed the configured maxQueueSize of 2

        // First, schedule work to use up the single thread (max concurrency is 1) that is available to the work manager
        CountDownLatch blocker = new CountDownLatch(1);
        Work blockingWork = () -> {
            try {
                workStarted.arrive();
                assertTrue(blocker.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS));
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        };
        WorkItem blockingItem = wm.schedule(blockingWork);

        // Queue up 2 work items
        Work blockedWork = () -> workStarted.arrive();
        WorkItem blockedItem1 = wm.schedule(blockedWork);
        assertEquals(1, workStarted.awaitAdvanceInterruptibly(0, TIMEOUT_NS, TimeUnit.NANOSECONDS));
        WorkItem blockedItem2 = wm.schedule(blockedWork);

        // Attempt to queue up 3rd work item, which should be rejected, per maxQueueSize=2 config
        try {
            WorkItem blockedWork3 = wm.schedule(blockedWork);
            fail("Should not be able to queue 3rd work item: " + blockedWork3);
        } catch (WorkRejectedException x) {
            // expected
        }

        // unblock and expect queued items to run
        blocker.countDown();
        workStarted.awaitAdvanceInterruptibly(1, TIMEOUT_NS, TimeUnit.NANOSECONDS);
        workStarted.awaitAdvanceInterruptibly(2, TIMEOUT_NS, TimeUnit.NANOSECONDS);

        Work w;
        for (long start = System.nanoTime(); (w = blockedItem2.getResult()) == null && System.nanoTime() - start < TIMEOUT_NS; )
            Thread.sleep(POLL_INTERVAL);
        assertEquals(blockedWork, w);
        assertEquals(blockedWork, blockedItem1.getResult());
        assertEquals(blockingWork, blockingItem.getResult());
        assertEquals(3, workStarted.getPhase());
    }

    /**
     * Perform a lookup of a managedExecutorService using deployment descriptor defined resource reference
     * that specifies the resource type as WorkManager.
     */
    @Test
    public void testWorkManagerResourceReferenceLookupOfManagedScheduledExecutorService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        WorkManager wm = InitialContext.doLookup("java:comp/env/wm/scheduledExecutorRef");
        assertNotNull(wm);
        assertTrue(wm.toString(), wm instanceof ScheduledExecutorService);
        assertTrue(wm.toString(), wm instanceof ManagedScheduledExecutorService);

        Phaser workStarted = new Phaser(1);

        // Attempt to exceed the configured maxQueueSize of 1

        // First, schedule work to use up the two threads (max concurrency is 2) that are available to the work manager
        CountDownLatch blocker = new CountDownLatch(1);
        Work blockingWork = () -> {
            try {
                workStarted.arrive();
                assertTrue(blocker.await(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS));
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
        };
        WorkItem blockingItem1 = wm.schedule(blockingWork);
        assertEquals(1, workStarted.awaitAdvanceInterruptibly(0, TIMEOUT_NS, TimeUnit.NANOSECONDS));
        WorkItem blockingItem2 = wm.schedule(blockingWork);
        assertEquals(2, workStarted.awaitAdvanceInterruptibly(1, TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Queue up 1 more work item
        Work blockedWork = () -> workStarted.arrive();
        WorkItem blockedItem1 = wm.schedule(blockedWork);

        // Attempt to queue up 2nd work item, which should be rejected, per maxQueueSize=1 config
        try {
            WorkItem blockedWork2 = wm.schedule(blockedWork);
            fail("Should not be able to queue 3rd work item: " + blockedWork2);
        } catch (WorkRejectedException x) {
            // expected
        }

        // unblock and expect queued item to run
        blocker.countDown();
        assertEquals(3, workStarted.awaitAdvanceInterruptibly(2, TIMEOUT_NS, TimeUnit.NANOSECONDS));

        Work w;
        for (long start = System.nanoTime();
                ((w = blockedItem1.getResult()) == null ||
                 blockingItem1.getResult() == null ||
                 blockingItem2.getResult() == null)
                && System.nanoTime() - start < TIMEOUT_NS; )
            Thread.sleep(POLL_INTERVAL);
        assertEquals(blockedWork, w);
        assertEquals(blockingWork, blockingItem1.getResult());
        assertEquals(blockingWork, blockingItem2.getResult());
        assertEquals(3, workStarted.getPhase());
    }
}
