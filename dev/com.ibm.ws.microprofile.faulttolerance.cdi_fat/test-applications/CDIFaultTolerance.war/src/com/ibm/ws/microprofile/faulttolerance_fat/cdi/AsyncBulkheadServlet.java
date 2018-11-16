/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBulkheadBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBulkheadBean2;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Test
 */
@WebServlet("/asyncbulkhead")
public class AsyncBulkheadServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    AsyncBulkheadBean bean1;
    @Inject
    AsyncBulkheadBean bean2;
    @Inject
    AsyncBulkheadBean bean3;
    @Inject
    AsyncBulkheadBean2 bean4;

    @Test
    public void testAsyncBulkheadSmall() throws InterruptedException, ExecutionException, TimeoutException {
        //connectA has a poolSize of 2
        //first two should be run straight away, in parallel, each around 5 seconds
        Future<Boolean> future1 = bean1.connectA("One");
        //These sleep statements are fine tuning to ensure this test functions.
        //The increments are small enough that it shuld not impact the logic of this test.
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Boolean> future2 = bean1.connectA("Two");
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);

        //next two should wait until the others have finished
        Future<Boolean> future3 = bean1.connectA("Three");
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Boolean> future4 = bean1.connectA("Four");
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);

        //total time should be just over 10s
        Thread.sleep((TestConstants.WORK_TIME * 2) + TestConstants.TEST_TIME_UNIT);

        if (!future1.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future1 did not complete properly");
        }
        if (!future2.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future2 did not complete properly");
        }
        if (!future3.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future3 did not complete properly");
        }
        if (!future4.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future4 did not complete properly");
        }

    }

    @Test
    public void testAsyncBulkheadQueueFull() throws InterruptedException, ExecutionException, TimeoutException {
        //connectA has a poolSize of 2
        //first two should be run straight away, in parallel, each around 5 seconds
        Future<Boolean> future1 = bean2.connectA("One");
        Future<Boolean> future2 = bean2.connectA("Two");
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Boolean> future3 = bean2.connectA("Three");
        Future<Boolean> future4 = bean2.connectA("Four");

        try {
            Future<Boolean> future5 = bean2.connectA("Five");
            future5.get(TestConstants.TIMEOUT, TimeUnit.MILLISECONDS);
            throw new AssertionError("BulkheadException not thrown");
        } catch (BulkheadException e) {
            // expected for 1.0
        } catch (ExecutionException e) {
            // expected for 2.0
            assertThat(e.getCause(), instanceOf(BulkheadException.class));
        }

    }

    @Test
    public void testAsyncBulkheadTimeout() throws InterruptedException, TimeoutException, ExecutionException, AssertionError {
        //connectB has a poolSize of 2 but a timeout of 2s
        //first two should be run straight away, in parallel, but should timeout after 2s
        Future<Boolean> future1 = bean3.connectB("One"); //without timeout would take 5s
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Boolean> future2 = bean3.connectB("Two"); //without timeout would take 5s
        Thread.sleep(TestConstants.TIMEOUT); //sleep until timeout has occurred

        //next two should run straight away and complete quickly
        Future<Boolean> future3 = bean3.connectB("Three");
        Future<Boolean> future4 = bean3.connectB("Four");
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT * 2);

        try {
            future1.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS);
            throw new AssertionError("Future1 did not timeout properly");
        } catch (ExecutionException e) {
            //expected
            assertThat(e.getCause(), instanceOf(org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class));
        }

        try {
            future2.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS);
            throw new AssertionError("Future2 did not timeout properly");
        } catch (ExecutionException e) {
            //expected
            assertThat(e.getCause(), instanceOf(org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class));
        }

        if (!future3.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future3 did not complete properly");
        }
        if (!future4.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future4 did not complete properly");
        }
    }

    /**
     * Test overriding method level Bulkhead annotation attributes through config.
     *
     * The test operates in exactly the same manner as testAsyncBulkheadSmall but assumes that the
     * Bulkead attributes set on bean1's connectC method are overridden by config.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    @Test
    public void testAsyncBulkheadSmallConfig() throws InterruptedException, ExecutionException, TimeoutException {
        //connectC has a poolSize of 2
        //first two should be run straight away, in parallel, each around 5 seconds
        Future<Boolean> future1 = bean1.connectC("One");
        //These sleep statements are fine tuning to ensure this test functions.
        //The increments are small enough that it shuld not impact the logic of this test.
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Boolean> future2 = bean1.connectC("Two");
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);

        //next two should wait until the others have finished
        Future<Boolean> future3 = bean1.connectC("Three");
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Boolean> future4 = bean1.connectC("Four");
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);

        //total time should be just over 10s
        Thread.sleep((TestConstants.WORK_TIME * 2) + TestConstants.TEST_TIME_UNIT);

        if (!future1.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future1 did not complete properly");
        }
        if (!future2.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future2 did not complete properly");
        }
        if (!future3.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future3 did not complete properly");
        }
        if (!future4.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future4 did not complete properly");
        }

    }

    /**
     * Test overriding class level Bulkhead annotation attributes through config.
     *
     * The test operates in exactly the same manner as testAsyncBulkheadSmall but assumes that the
     * Bulkead attributes set on bean4 are overridden by config.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    @Test
    public void testAsyncBulkheadSmallClassScopeConfig() throws InterruptedException, ExecutionException, TimeoutException {
        //connectC has a poolSize of 2
        //first two should be run straight away, in parallel, each around 5 seconds
        Future<Boolean> future1 = bean4.connectA("One");
        //These sleep statements are fine tuning to ensure this test functions.
        //The increments are small enough that it shuld not impact the logic of this test.
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Boolean> future2 = bean4.connectA("Two");
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);

        //next two should wait until the others have finished
        Future<Boolean> future3 = bean4.connectA("Three");
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);
        Future<Boolean> future4 = bean4.connectA("Four");
        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);

        //total time should be just over 10s
        Thread.sleep((TestConstants.WORK_TIME * 2) + TestConstants.TEST_TIME_UNIT);

        if (!future1.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future1 did not complete properly");
        }
        if (!future2.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future2 did not complete properly");
        }
        if (!future3.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future3 did not complete properly");
        }
        if (!future4.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Future4 did not complete properly");
        }

    }
}
