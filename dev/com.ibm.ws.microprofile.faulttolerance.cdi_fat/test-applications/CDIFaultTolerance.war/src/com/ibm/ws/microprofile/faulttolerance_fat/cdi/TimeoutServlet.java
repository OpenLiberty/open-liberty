/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.suite.BasicTest;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.TimeoutBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.TimeoutBean2;
import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Test
 */
@WebServlet("/timeout")
public class TimeoutServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    TimeoutBean bean;

    @Inject
    TimeoutBean2 classScopedConfigBean;

    /**
     * This method will test the connect() method of the bean passed in, checking that a timeout
     * is received within the time period passed in - if the call takes too long
     * it is repeated in order to try to determine if the problem is due to an implementation
     * error or just a blip in the underlying (virtual) hardware - as is seen occasionally.
     *
     * @param bean      the test bean (often a lambda)
     * @param maxMillis the limit of how long before a result or exception is expected
     * @return the result of the call
     * @throws ConnectException
     */
    private <T> T ignoreOutliers(TimedTestBean<T> bean, int maxMillis) throws ConnectException {
        long startNanos = System.nanoTime();
        T result = null; //Never actually used if tests are going according to plan!
        try {
            result = bean.connect();
            throw new AssertionError("TimeoutException not thrown");
        } catch (TimeoutException e) {
            //expected!
            long endNanos = System.nanoTime();
            long millisTaken = MILLISECONDS.convert(endNanos - startNanos, NANOSECONDS);
            if (millisTaken > maxMillis) {
                String msg = "TimeoutException not thrown quickly enough, duration was: " + millisTaken + "milliseconds, limit was " + maxMillis;

                int delayMillis = 5000;
                int sampleSize = 10;

                long averageTime = getSampleOfConnectTimesAverage(delayMillis, sampleSize, bean);

                if (averageTime > maxMillis) {
                    // We throw the same error we would have without de-glitching
                    throw new AssertionError(msg);
                }
            }
        }
        return result;
    }

    /**
     * This method waits for a period of time then calls connect on the targetBean
     * sampleSize number of times to calculate the average.
     *
     * @param delayMillis the initial delay, an attempt to coast past an underlying platform glitch
     * @param sampleSize  how many times to call the target method
     * @param targetBean  the bean with the target method
     */
    private <T> long getSampleOfConnectTimesAverage(int delayMillis, int sampleSize, TimedTestBean<T> targetBean) {

        long totalNanos = 0;
        long averageMillis = 0;

        try {
            Thread.sleep(delayMillis);
            long startNanos = System.nanoTime();
            for (int i = 0; i < sampleSize; i++) {
                try {
                    targetBean.connect();
                    throw new AssertionError("TimeoutException not thrown");
                } catch (TimeoutException e) {
                    // expected
                }
            }
            totalNanos = System.nanoTime() - startNanos;
            averageMillis = MILLISECONDS.convert(totalNanos / sampleSize, NANOSECONDS);
            return averageMillis;
        } catch (Exception e) {
            // This will flag the original error to be reported
            // as we cannot successfully de-glitch.
            return Long.MAX_VALUE;
        }

    }

    @BasicTest
    @Test
    public void testTimeout() throws ConnectException {
        //the default timeout is 1000ms, if it takes 3000ms to fail then there is something wrong
        ignoreOutliers(() -> bean.connectA(), 3000);
    }

    @Test
    public void testException() {
        //should just throw an exception (we're checking we get the right exception even thought it is async internally)
        try {
            bean.connectB();
        } catch (ConnectException e) {
            String expected = "ConnectException: A simple exception";
            String actual = e.getMessage();
            if (!expected.equals(actual)) {
                throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
            }
        }
    }

    /**
     * This test ensures that a timeout prompts a retry
     */
    @Test
    public void testTimeoutWithRetry() throws Exception {
        try {
            bean.connectC();
            fail("No exception thrown");
        } catch (TimeoutException e) {
            // Expected, ensure that the correct number of calls have been made
            assertThat("connectC calls", bean.getConnectCCalls(), is(8));
        }
    }

    @Test
    public void testTimeoutWithRetryAsync() throws Exception {
        try {
            bean.connectD().get();
            fail("No exception thrown");
        } catch (ExecutionException e) {
            // Expected, ensure that the cause is correct
            assertThat("Execution exception cause", e.getCause(), instanceOf(TimeoutException.class));
            // Ensure that the correct number of calls have been made
            assertThat("connectD calls", bean.getConnectDCalls(), is(8));
        }
    }

    @Test
    public void testTimeoutWithFallback() throws Exception {
        Connection result = bean.connectF();
        assertThat(result.getData(), is("Fallback for: connectF - data!"));
    }

    @Test
    public void testTimeoutZero() throws Exception {
        bean.connectE();
        // No TimeoutException expected
    }

    @Test
    public void testNonInterruptableTimeout() throws InterruptedException {
        try {
            bean.busyWaitTimeout(); // Busy wait which will time out
            fail("No exception thrown");
        } catch (TimeoutException e) {
            if (Thread.interrupted()) {
                fail("Thread was in interrupted state upon return");
            }
            // This wait is to ensure our thread doesn't get interrupted later, after the method has finished
            Thread.sleep(TestConstants.NEGATIVE_TIMEOUT);
        }
    }

    @Test
    public void testNonInterruptableDoesntTimeout() throws Exception {
        bean.busyWaitNoTimeout(); // Busy wait which will not time out

        if (Thread.interrupted()) {
            fail("Thread was in interrupted state upon return");
        }

        Thread.sleep(TestConstants.NEGATIVE_TIMEOUT); // Wait to ensure that our thread isn't interrupted after the method has finished
    }

    /**
     * Test method level override of Timeout value on a synchronous service.
     *
     * A timeout will not occur unless the configuration overrides the value set on the connectG method.
     */
    @Test
    public void testTimeoutConfig() throws ConnectException {
        //the configured timeout is 500ms,
        //if it takes 1000ms to fail then there is something wrong
        ignoreOutliers(() -> bean.connectG(), 1000);
    }

    /**
     * Test method level override of Timeout value on a synchronous service.
     *
     * A timeout will not occur unless the configuration overrides the value set on the classScopedConfigBean.
     */
    @Test
    public void testTimeoutClassScopeConfig() throws ConnectException {
        //the configured timeout is 500ms,
        //if it takes 1000ms to fail then there is something wrong
        ignoreOutliers(() -> classScopedConfigBean.connectA(), 1000);
    }

    @FunctionalInterface
    public interface TimedTestBean<T> {
        T connect() throws ConnectException;
    }

}
