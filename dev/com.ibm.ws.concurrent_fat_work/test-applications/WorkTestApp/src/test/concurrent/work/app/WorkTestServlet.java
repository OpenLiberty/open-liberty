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

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    @Resource(lookup = "wm/scheduledExecutor", name = "java:comp/env/wm/wmScheduledExecutorRef")
    private WorkManager wmScheduledExecutor;

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
}
