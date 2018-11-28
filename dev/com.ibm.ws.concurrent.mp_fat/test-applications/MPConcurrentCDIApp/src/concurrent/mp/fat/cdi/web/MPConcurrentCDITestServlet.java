/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.mp.fat.cdi.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;
import org.eclipse.microprofile.concurrent.ThreadContext;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MPConcurrentCDITestServlet")
public class MPConcurrentCDITestServlet extends FATServlet {

    static final int TIMEOUT_MIN = 2;

    @Inject
    ConcurrencyBean bean;

    @Inject
    ManagedExecutor noAnno;

    @Inject
    @ManagedExecutorConfig
    ManagedExecutor defaultAnno;

    @Inject
    @ManagedExecutorConfig(maxAsync = -1, maxQueued = -1, propagated = ThreadContext.ALL_REMAINING, cleared = ThreadContext.TRANSACTION)
    ManagedExecutor defaultAnnoVerbose;

    @Inject
    @ManagedExecutorConfig(maxAsync = 5)
    ManagedExecutor maxAsync5;

    @Inject
    @ManagedExecutorConfig(maxAsync = 2, maxQueued = 2)
    ManagedExecutor max2;

    @Inject
    @ManagedExecutorConfig(propagated = {}, cleared = { ThreadContext.APPLICATION })
    ManagedExecutor noAppCtx;

    @Inject
    @ManagedExecutorConfig(propagated = { ThreadContext.APPLICATION, ThreadContext.TRANSACTION }, cleared = {})
    ManagedExecutor propagatedAB;

    @Inject
    @ManagedExecutorConfig(propagated = { ThreadContext.TRANSACTION, ThreadContext.APPLICATION }, cleared = {})
    ManagedExecutor propagatedBA;

    @Test
    public void testMEDefaultsEqual() {
        assertEquals(noAnno, defaultAnno);
        assertEquals(noAnno, bean.getNoAnno());
        assertEquals(noAnno, defaultAnno);
        assertEquals(noAnno, bean.getDefaultAnno());
        assertEquals(noAnno, defaultAnnoVerbose);
    }

    @Test
    public void testMEConfiguredEqual() {
        assertEquals(maxAsync5, bean.getMaxAsync5());
    }

    @Test
    public void testMEDifferent() {
        assertNotSame(noAnno, maxAsync5);
        assertNotSame(noAnno, propagatedAB);
    }

    @Test
    public void testMEPropogationEqual() {
        assertEquals(propagatedAB, propagatedBA);
    }

    @Test
    public void testBasicMEWorks() throws Exception {
        CompletableFuture<String> cf = noAnno.supplyAsync(() -> {
            try {
                System.out.println("testBasicMEWorks: Performing lookup of 'foo'");
                // TODO: This confirms JEE context is on the thread, need to find an operation that
                // confirms 'application' context is on the thread
                return InitialContext.doLookup("foo");
            } catch (NamingException e) {
                e.printStackTrace();
                return e.getMessage();
            }
        });
        String result = cf.get(TIMEOUT_MIN, TimeUnit.MINUTES);
        System.out.println("testBasicMEWorks: result=" + result);
        assertEquals("bar", result);
    }

    // @Test
    // TODO: disable this test until I figure out a way to observe the absence
    // of application context, or the ability to wipe JEE context
    public void testNoAppContext() throws Exception {
        CompletableFuture<Boolean> cf1 = noAppCtx.supplyAsync(() -> {
            try {
                System.out.println("testNoAppContext: enter");
                InitialContext.doLookup("foo");
                fail("Should not be able to perform a JNDI lookup without application context.");
                return false;
            } catch (NamingException expected) {
                return true;
            }
        });
        assertEquals("Should not be able to perform JNDI lookup without app context",
                     true, cf1.get(TIMEOUT_MIN, TimeUnit.MINUTES));
    }

    /**
     * Using an executor configured with maxAsync=2 and maxQueued=2, use blocking tasks to fill up
     * the queue by submitting 2 tasks that run and block, then 2 tasks that sit in the queue. When
     * a 5th task is submitted it should be rejected because it exceeds the max queue size.
     */
    @Test
    public void testMaxQueueSizeExceededAndReject() throws Exception {
        CountDownLatch beginLatch = new CountDownLatch(2);
        CountDownLatch continueLatch = new CountDownLatch(1);

        // max concurrency: 2, max queue size: 2, runIfQueueFull: false
        CompletableFuture<Integer> cf0 = max2.supplyAsync(() -> 144);
        CompletableFuture<Integer> cf1, cf2, cf3, cf4, cf5, cf6;
        try {
            // Create 2 async stages that will block both max concurrency permits, and wait for both to start running
            cf1 = cf0.thenApplyAsync(new BlockableIncrementFunction("testMaxQueueSizeExceededAndReject1", beginLatch, continueLatch));
            cf2 = cf0.thenApplyAsync(new BlockableIncrementFunction("testMaxQueueSizeExceededAndReject2", beginLatch, continueLatch));
            assertTrue(beginLatch.await(TIMEOUT_MIN, TimeUnit.MINUTES));

            // Create 2 async stages to fill the queue
            cf3 = cf0.thenApplyAsync(new BlockableIncrementFunction("testMaxQueueSizeExceededAndReject3", null, null));
            cf4 = cf0.thenApplyAsync(new BlockableIncrementFunction("testMaxQueueSizeExceededAndReject4", null, null));

            // Attempt to create async stage which it will not be possible to submit due exceeding queue capacity
            cf5 = cf0.thenApplyAsync(new BlockableIncrementFunction("testMaxQueueSizeExceededAndReject5", null, null));
            try {
                Integer i = cf5.get(TIMEOUT_MIN, TimeUnit.MINUTES);
                fail("Should not be able to submit task for cf5. Instead result is: " + i);
            } catch (ExecutionException x) {
                if (x.getCause() instanceof RejectedExecutionException) {
                    String message = x.getCause().getMessage();
                    if (message == null
                        || !message.contains("CWWKE1201E")
                        || !message.contains("PolicyExecutorProvider-ManagedExecutor")
                        || !message.contains("maxQueueSize")
                        || !message.contains(" 2 ")) // the maximum queue size
                        throw x;
                } else
                    throw x;
            }

            // Create an async stage that will be a delayed submit (after cf3 runs)
            cf6 = cf3.thenApplyAsync(new BlockableIncrementFunction("testMaxQueueSizeExceededAndReject6", null, null));

            // Confirm that asynchronous stages are not complete:
            try {
                cf3.get(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException x) {
            }

            assertFalse(cf1.isDone());
            assertFalse(cf2.isDone());
            assertFalse(cf3.isDone());
            assertFalse(cf4.isDone());
            assertTrue(cf5.isDone());
            assertTrue(cf5.isCompletedExceptionally());
            assertFalse(cf5.isCancelled());
            assertFalse(cf6.isDone());
        } finally {
            // Allow the async stages to complete
            continueLatch.countDown();
        }

        // Confirm that all asynchronous stages complete, once unblocked:
        assertEquals(Integer.valueOf(145), cf1.get(TIMEOUT_MIN, TimeUnit.MINUTES));
        assertEquals(Integer.valueOf(145), cf2.get(TIMEOUT_MIN, TimeUnit.MINUTES));
        assertEquals(Integer.valueOf(145), cf3.get(TIMEOUT_MIN, TimeUnit.MINUTES));
        assertEquals(Integer.valueOf(145), cf4.get(TIMEOUT_MIN, TimeUnit.MINUTES));
        assertEquals(Integer.valueOf(146), cf6.get(TIMEOUT_MIN, TimeUnit.MINUTES));

        assertTrue(cf1.isDone());
        assertTrue(cf2.isDone());
        assertTrue(cf3.isDone());
        assertTrue(cf4.isDone());
        assertTrue(cf6.isDone());

        assertFalse(cf1.isCompletedExceptionally());
        assertFalse(cf2.isCompletedExceptionally());
        assertFalse(cf3.isCompletedExceptionally());
        assertFalse(cf4.isCompletedExceptionally());
        assertFalse(cf6.isCompletedExceptionally());
    }

}
