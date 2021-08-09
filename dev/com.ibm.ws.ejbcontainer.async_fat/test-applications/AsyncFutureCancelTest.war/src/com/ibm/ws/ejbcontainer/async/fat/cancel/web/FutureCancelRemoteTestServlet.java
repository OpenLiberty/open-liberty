/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.cancel.web;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/AsyncFutureCancelRemoteTest")
public class FutureCancelRemoteTestServlet extends FATServlet {
    private static final String CLASS_NAME = FutureCancelRemoteTestServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    /**
     * Maximum number of times to attempt Future.cancel. There is no way to
     * guarantee that the server will not always dispatch the asynchronous
     * invocation before we can cancel it, so we attempt many calls in rapid
     * succession in that hope that one will cancel.
     *
     * This means that tests may "pass" without truly testing the objective,
     * but since we're constantly running tests in Liberty this is acceptable.
     * At least some of our test runs should successfully check the desired
     * scenario.
     */
    private static final int MAX_CANCEL_ATTEMPTS = 1000;

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    @EJB(beanName = "FutureCancelDriverRemoteBean")
    FutureCancelDriver driverBean;

    private static <T> T getWithTimeout(Future<T> future) throws Exception {
        // Use timed-get to avoid hanging the server.
        return future.get(MAX_ASYNC_WAIT, TimeUnit.SECONDS);
    }

    /**
     * This test verifies the behavior of a Future object that has been
     * successfully cancelled and the passed parameter mayInterruptIfRunning
     * is false.
     *
     * <p>An asynchronous method is called repeatedly until the resulting Future
     * object can be cancelled successfully. isCancelled, isDone, and
     * Future.get are called on the cancelled Future. All uncancelled futures
     * are waited upon for cleanup.
     *
     * The expected result is that calling an asynchronous method that sleeps
     * will eventually fill all available threads in the thread pool, and an
     * asynchronous method will eventually be buffered, which will allow it to
     * be cancelled successfully. Then, the isCancelled and isDone methods
     * should return true for that Future, and the get method should throw
     * CancellationException.
     */
    @Test
    public void testRemoteAsyncCancelledFalseParameter() throws Exception {
        svLogger.info("In testAsyncCancelledFalseParameter");

        List<Future<String>> uncancelledFutures = new ArrayList<Future<String>>();
        while (uncancelledFutures.size() < MAX_CANCEL_ATTEMPTS) {
            Future<String> future = driverBean.asyncCancelled();
            if (future.cancel(false)) {
                assertTrue("Future.isCancelled failed to return true", future.isCancelled());
                assertTrue("Future.isDone failed to return true", future.isDone());

                try {
                    getWithTimeout(future);
                    fail("Future.get failed to throw CancellationException");
                } catch (CancellationException e) {
                    svLogger.info("Caught expected exception: " + e);
                }

                break;
            }

            uncancelledFutures.add(future);
        }

        // Cleanup.
        for (Future<String> uncancelledFuture : uncancelledFutures) {
            getWithTimeout(uncancelledFuture);
        }
    }

    /**
     * This test verifies the behavior of a Future object that has been
     * successfully cancelled and the passed parameter mayInterruptIfRunning
     * is true.
     *
     * <p>An asynchronous method is called repeatedly until the resulting Future
     * object can be cancelled successfully. isCancelled, isDone are called, and
     * Future.get with timeout is called first and Future.get is called again
     * on the cancelled Future. All uncancelled futures are waited upon for cleanup.
     *
     * The expected result is that calling an asynchronous method that sleeps
     * will eventually fill all available threads in the thread pool, and an
     * asynchronous method will eventually be buffered, which will allow it to
     * be cancelled successfully. Then, the isCancelled and isDone methods
     * should return true for that Future, and the get method should throw
     * CancellationException.
     */
    @Test
    public void testRemoteAsyncCancelledTrueParameter() throws Exception {
        svLogger.info("In testAsyncCancelledTrueParameter");

        List<Future<String>> uncancelledFutures = new ArrayList<Future<String>>();
        while (uncancelledFutures.size() < MAX_CANCEL_ATTEMPTS) {
            Future<String> future = driverBean.asyncCancelled();
            if (future.cancel(true)) {
                assertTrue("Future.isCancelled failed to return true", future.isCancelled());
                assertTrue("Future.isDone failed to return true", future.isDone());

                try {
                    getWithTimeout(future);
                    fail("Future.get(timeout,TimeUnit) failed to throw CancellationException");
                } catch (CancellationException e) {
                    svLogger.info("Caught expected exception: " + e);
                }

                break;
            }

            uncancelledFutures.add(future);
        }

        // Cleanup.
        for (Future<String> uncancelledFuture : uncancelledFutures) {
            getWithTimeout(uncancelledFuture);
        }
    }

    /**
     * This test verifies the behavior of a Future object that has been
     * successfully cancelled and then an attempt is made to recancel the Future
     * the passed parameter mayInterruptIfRunning is false.
     *
     * <p>An asynchronous method is called repeatedly until the resulting Future
     * object can be cancelled successfully. isCancelled, isDone, and Future.get
     * with timeout are called on the cancelled Future. All uncancelled futures
     * are waited upon for cleanup.
     *
     * The expected result is that calling an asynchronous method that sleeps
     * will eventually fill all available threads in the thread pool, and an
     * asynchronous method will eventually be buffered, which will allow it to
     * be cancelled successfully. Then, the isCancelled and isDone methods
     * should return true for that Future, and the get method should throw
     * CancellationException.
     */
    @Test
    public void testRemoteAsyncRecancelledFalseParameter() throws Exception {
        svLogger.info("In testAsyncRecancelledFalseParameter");

        List<Future<String>> uncancelledFutures = new ArrayList<Future<String>>();
        while (uncancelledFutures.size() < MAX_CANCEL_ATTEMPTS) {
            Future<String> future = driverBean.asyncCancelled();
            if (future.cancel(false)) {
                assertTrue("Future.isCancelled failed to return true", future.isCancelled());
                assertTrue("Future.isDone failed to return true", future.isDone());

                try {
                    getWithTimeout(future);
                    fail("Future.get(timeout,TimeUnit) failed to throw CancellationException");
                } catch (CancellationException e) {
                    svLogger.info("Caught expected exception: " + e);
                }

                // Call future.cancel again
                assertFalse("Able to recancel a future", future.cancel(false));
                assertTrue("Recall Future.isCancelled failed to return true", future.isCancelled());
                assertTrue("Recall Future.isDone failed to return true", future.isDone());

                try {
                    getWithTimeout(future);
                    fail("Future.get failed to throw CancellationException");
                } catch (CancellationException e) {
                    svLogger.info("Caught expected exception: " + e);
                }

                break;
            }

            uncancelledFutures.add(future);
        }

        // Cleanup.
        for (Future<String> uncancelledFuture : uncancelledFutures) {
            getWithTimeout(uncancelledFuture);
        }
    }

    /**
     * This test verifies the behavior of a Future object that has been
     * successfully cancelled and then an attempt is made to recancel the Future
     * the passed parameter mayInterruptIfRunning is true.
     *
     * <p>An asynchronous method is called repeatedly until the resulting Future
     * object can be cancelled successfully. isCancelled, isDone are called, and
     * Future.get is called first and Future.get with timeout is called again
     * on the cancelled Future. All uncancelled futures are waited upon for cleanup.
     *
     * The expected result is that calling an asynchronous method that sleeps
     * will eventually fill all available threads in the thread pool, and an
     * asynchronous method will eventually be buffered, which will allow it to
     * be cancelled successfully. Then, the isCancelled and isDone methods
     * should return true for that Future, and the get method should throw
     * CancellationException.
     */
    @Test
    public void testRemoteAsyncRecancelledTrueParameter() throws Exception {
        List<Future<String>> uncancelledFutures = new ArrayList<Future<String>>();

        svLogger.info("In testAsyncRecancelledTrueParameter");

        while (uncancelledFutures.size() < MAX_CANCEL_ATTEMPTS) {
            Future<String> future = driverBean.asyncCancelled();
            if (future.cancel(true)) {
                assertTrue("Future.isCancelled failed to return true", future.isCancelled());
                assertTrue("Future.isDone failed to return true", future.isDone());

                try {
                    getWithTimeout(future);
                    fail("Future.get failed to throw CancellationException");
                } catch (CancellationException e) {
                    svLogger.info("Caught expected exception: " + e);
                }

                // Call future.cancel again
                assertFalse("Able to recancel a future", future.cancel(true));
                assertTrue("Recall Future.isCancelled failed to return true", future.isCancelled());
                assertTrue("Recall Future.isDone failed to return true", future.isDone());

                try {
                    getWithTimeout(future);
                    fail("Future.get(timeout,TimeUnit) failed to throw CancellationException");
                } catch (CancellationException e) {
                    svLogger.info("Caught expected exception: " + e);
                }

                break;
            }

            uncancelledFutures.add(future);
        }

        // Cleanup.
        for (Future<String> uncancelledFuture : uncancelledFutures) {
            getWithTimeout(uncancelledFuture);
        }
    }

    /**
     * This test verifies the behavior of a Future object that has been
     * unsuccessfully cancelled and the passed parameter
     * mayInterruptIfRunning is true.
     *
     * <p>An asynchronous method is called, then Future.isCancelled is called,
     * after that Future.cancel is called, but the object can't be cancelled
     * successfully and SessionContext.wasCancelCalled, isCancelled, isDone
     * are called called on the that Future.
     *
     * The expected result is that calling an asynchronous method that sleeps
     * to enough time after it started to execute. the asynchronous method
     * will not be cancelled, The isCancelled and isDone methods
     * should return false and SessionContext.wasCancelCalled should return
     * the same value of mayInterruptIfRunning that is true.
     */
    @Test
    public void testRemoteAsyncNotCancelledTrueParameter() throws Exception {
        svLogger.info("In testAsyncNotCancelledTrueParameter");

        driverBean.initializeAsyncNotCancelled();
        Future<String> future = driverBean.asyncNotCancelled(1);
        driverBean.awaitAsyncNotCancelled();
        assertFalse("Future.isCancelled failed to return false, before calling Future.cancel", future.isCancelled());

        assertFalse("Able to cancel a future", future.cancel(true));
        assertFalse("Future.isCancelled failed to return false", future.isCancelled());
        assertFalse("Future.isDone failed to return false", future.isDone());

        assertTrue("SessionContext.wasCancelCalled failed to return true", driverBean.awaitWasCancelCalled());
        getWithTimeout(future);
    }

    /**
     * This test verifies the behavior of a Future object that has been
     * unsuccessfully cancelled and the passed parameter
     * mayInterruptIfRunning is false.
     *
     * <p>An asynchronous method is called, then Future.isCancelled is called,
     * after that Future.cancel is called, but the object can't be cancelled
     * successfully and SessionContext.wasCancelCalled, isCancelled, isDone
     * are called called on the that Future.
     *
     * The expected result is that calling an asynchronous method that sleeps
     * to enough time after it started to execute. the asynchronous method
     * will not be cancelled, The isCancelled and isDone methods
     * should return false and SessionContext.wasCancelCalled should return
     * the same value of mayInterruptIfRunning that is false.
     */
    @Test
    public void testRemoteAsyncNotCancelledFalseParameter() throws Exception {
        svLogger.info("In testAsyncNotCancelledFalseParameter");

        driverBean.initializeAsyncNotCancelled();
        Future<String> future = driverBean.asyncNotCancelled(1);
        driverBean.awaitAsyncNotCancelled();
        assertFalse("Future.isCancelled failed to return false,before calling Future.cancel", future.isCancelled());

        assertFalse("Able to cancel a future", future.cancel(false));
        assertFalse("Future.isCancelled failed to return false", future.isCancelled());
        assertFalse("Future.isDone failed to return false", future.isDone());

        assertFalse("SessionContext.wasCancelCalled failed to return false", driverBean.awaitWasCancelCalled());
        getWithTimeout(future);
    }

    /**
     * This test verifies the behavior of a Future object that has been
     * unsuccessfully cancelled and the passed parameter
     * mayInterruptIfRunning is true.
     *
     * <p>An asynchronous method is called, then SessionContext.wasCancelCalled
     * is called, after that Future.cancel is called, but the object can't be
     * cancelled successfully and SessionContext.wasCancelCalled, isCancelled,
     * isDone are called called on the that Future.
     *
     * The expected result is that calling an asynchronous method that sleeps
     * to enough time after it started to execute. the asynchronous method
     * will not be cancelled, The isCancelled and isDone methods
     * should return false and the first SessionContext.wasCancelCalled should
     * return false and the second SessionContext.wasCancelCalled should return
     * the same value of mayInterruptIfRunning that is true.
     */
    @Test
    public void testRemoteAsyncNotCancelledTrueParameterWithTwoSessionContextWasCancelCalled() throws Exception {
        svLogger.info("In testAsyncNotCancelledTrueParameterWithTwoSessionContextiWasCancelCalled");

        driverBean.initializeAsyncNotCancelled();
        Future<String> future = driverBean.asyncNotCancelled(2);
        assertFalse("SessionContext.wasCancelCalled failed to return false,before calling Future.cancel", driverBean.awaitWasCancelCalled());

        assertFalse("Able to cancel a future", future.cancel(true));
        assertFalse("Future.isCancelled failed to return false", future.isCancelled());
        assertFalse("Future.isDone failed to return false", future.isDone());

        assertTrue("SessionContext.wasCancelCalled failed to return true", driverBean.awaitWasCancelCalled());
        getWithTimeout(future);
    }

    /**
     * This test verifies the behavior of a Future object that has been
     * unsuccessfully cancelled and the passed parameter
     * mayInterruptIfRunning is false.
     *
     * <p>An asynchronous method is called, then SessionContext.wasCancelCalled
     * is called, after that Future.cancel is called, but the object can't be
     * cancelled successfully and SessionContext.wasCancelCalled, isCancelled,
     * isDone are called called on the that Future.
     *
     * The expected result is that calling an asynchronous method that sleeps
     * to enough time after it started to execute. the asynchronous method
     * will not be cancelled, The isCancelled and isDone methods
     * should return false and the first SessionContext.wasCancelCalled should
     * return false and the second SessionContext.wasCancelCalled should return
     * the same value of mayInterruptIfRunning that is false.
     */
    @Test
    public void testRemoteAsyncNotCancelledFalseParameterWithTwoSessionContextWasCancelCalled() throws Exception {
        svLogger.info("In testAsyncNotCancelledFalseParameterWithTwoSessionContextiWasCancelCalled");

        driverBean.initializeAsyncNotCancelled();
        Future<String> future = driverBean.asyncNotCancelled(2);
        assertFalse("SessionContext.wasCancelCalled failed to return false,before calling Future.cancel", driverBean.awaitWasCancelCalled());

        assertFalse("Able to cancel a future", future.cancel(false));
        assertFalse("Future.isCancelled failed to return false", future.isCancelled());
        assertFalse("Future.isDone failed to return false", future.isDone());

        assertFalse("SessionContext.wasCancelCalled failed to return false", driverBean.awaitWasCancelCalled());
        getWithTimeout(future);
    }

    /**
     * This test verifies the behavior of a Future object that has been
     * unsuccessfully cancelled and the passed parameter
     * mayInterruptIfRunning is true.
     *
     * <p>An asynchronous method is called, then SessionContext.wasCancelCalled
     * is called, after that Future.cancel(true) is called, but the object can't
     * be cancelled successfully then the call sequence is
     * SessionContext.wasCancelCalled, Future.cancel(false),
     * SessionContext.wasCancelCalled, Future.cancel(true),
     * SessionContext.wasCancelCalled.
     *
     * The expected result is that calling an asynchronous method that sleeps
     * to enough time after it started to execute. the asynchronous method
     * will not be cancelled. the first SessionContext.wasCancelCalled should
     * return false, the second SessionContext.wasCancelCalled should return
     * true,the third SessionContext.wasCancelCalled should return false,
     * the fourth SessionContext.wasCancelCalled should return true.
     * All three Future.cancel should return false.
     */
    @Test
    public void testRemoteAsyncNotCancelledTrueFalseTrue() throws Exception {
        svLogger.info("In testAsyncNotCancelledTrueFalseTrue");

        driverBean.initializeAsyncNotCancelled();
        Future<String> future = driverBean.asyncNotCancelled(4);
        driverBean.awaitAsyncNotCancelled();
        assertFalse("SessionContext.wasCancelCalled failed to return false,before calling Future.cancel", driverBean.awaitWasCancelCalled());

        assertFalse("Able to cancel a future", future.cancel(true));
        assertTrue("SessionContext.wasCancelCalled failed to return true, for mayInterruptIfRunning is true in the first time call Future,cancel",
                   driverBean.awaitWasCancelCalled());

        assertFalse("Recall Future.cancel,able to cancel a future", future.cancel(false));
        assertFalse("SessionContext.wasCancelCalled failed to return false for mayInterruptIfRunning is false in the second time call Future,cancel",
                    driverBean.awaitWasCancelCalled());

        assertFalse("Third time call Future.cancel,able to cancel a future", future.cancel(true));
        assertTrue("SessionContext.wasCancelCalled failed to return true, for mayInterruptIfRunning is true in the third time call Future,cancel",
                   driverBean.awaitWasCancelCalled());

        getWithTimeout(future);
    }

    /**
     * This test verifies the behavior of a Future object that has been
     * unsuccessfully cancelled and the passed parameter
     * mayInterruptIfRunning is true.
     *
     * <p>An asynchronous method is called, then SessionContext.wasCancelCalled
     * is called, after that Future.cancel(false) is called, but the object can't
     * be cancelled successfully then the call sequence is
     * SessionContext.wasCancelCalled, Future.cancel(true),
     * SessionContext.wasCancelCalled, Future.cancel(false),
     * SessionContext.wasCancelCalled.
     *
     * The expected result is that calling an asynchronous method that sleeps
     * to enough time after it started to execute. the asynchronous method
     * will not be cancelled. the first SessionContext.wasCancelCalled should
     * return false, the second SessionContext.wasCancelCalled should return
     * false,the third SessionContext.wasCancelCalled should return true,
     * the fourth SessionContext.wasCancelCalled should return false.
     * All three Future.cancel should return false.
     */
    @Test
    public void testRemoteAsyncNotCancelledFalseTrueFalse() throws Exception {
        svLogger.info("In testAsyncNotCancelledFalseTrueFalse");

        driverBean.initializeAsyncNotCancelled();
        Future<String> future = driverBean.asyncNotCancelled(4);
        driverBean.awaitAsyncNotCancelled();
        assertFalse("SessionContext.wasCancelCalled failed to return false,before calling Future.cancel", driverBean.awaitWasCancelCalled());

        assertFalse("Able to cancel a future", future.cancel(false));
        assertFalse("SessionContext.wasCancelCalled failed to return false, for mayInterruptIfRunning is false in the first time call Future,cancel",
                    driverBean.awaitWasCancelCalled());

        assertFalse("Recall Future.cancel,able to cancel a future", future.cancel(true));
        assertTrue("SessionContext.wasCancelCalled failed to return true for mayInterruptIfRunning is true in the second time call Future,cancel",
                   driverBean.awaitWasCancelCalled());

        assertFalse("Third time call Future.cancel,able to cancel a future", future.cancel(false));
        assertFalse("SessionContext.wasCancelCalled failed to return false, for mayInterruptIfRunning is false in the third time call Future,cancel",
                    driverBean.awaitWasCancelCalled());

        getWithTimeout(future);
    }

    /**
     * This test verifies the behavior of SessionContext.wasCancelCalled
     * should throw IllegalStateException on a non-asynchronous method
     *
     * <p>An non-asynchronous method is called, that method calls
     * SessionContext.wasCancelCalled on the bean.
     *
     * The expected result is that calling a non-asynchronous method that calls
     * SessionContext.wasCancelCalled should catch a
     * java.lang.IllegalStateException on the bean.
     */
    @Test
    public void testRemoteSessionContextWasCancelCalledInSync() throws Exception {
        svLogger.info("In testSessionContextwasCancelCalledInSync");
        assertTrue("Get java.lang.IllegalStateException on bean, but not", driverBean.sessionContextWasCancelCalledInSync());
    }

    /**
     * This test verifies the behavior of SessionContext.wasCancelCalled
     * should throw IllegalStateException on a non-void method
     *
     * <p>A void asynchronous method is called, and that method attempts to
     * call SessionContext.wasCancelCalled.
     */
    @Test
    public void testRemoteSessionContextWasCancelCalledInVoid() throws Exception {
        driverBean.sessionContextWasCancelCalledInVoid();
        assertTrue("Expected java.lang.IllegalStateException from wasCancelCalled", driverBean.awaitWasCancelCalledInVoid());
    }
}