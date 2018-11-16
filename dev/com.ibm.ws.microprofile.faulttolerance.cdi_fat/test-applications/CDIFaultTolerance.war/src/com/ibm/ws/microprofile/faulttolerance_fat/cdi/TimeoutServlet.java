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

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Test;

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

    @Test
    public void testTimeout() throws ConnectException {
        //should timeout after a second as per default
        long start = System.currentTimeMillis();
        try {
            bean.connectA();
            throw new AssertionError("TimeoutException not thrown");
        } catch (TimeoutException e) {
            //expected!
            long timeout = System.currentTimeMillis();
            long duration = timeout - start;
            if (duration > 3000) { //the default timeout is 1000ms, if it takes 3000ms to fail then there is something wrong
                throw new AssertionError("TimeoutException not thrown quickly enough: " + duration);
            }
        }
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
            bean.busyWait(1000); // Busy wait time is greater than timeout (=500)
            fail("No exception thrown");
        } catch (TimeoutException e) {
            if (Thread.interrupted()) {
                fail("Thread was in interrupted state upon return");
            }
            // This wait is to ensure our thread doesn't get interrupted later, after the method has finished
            Thread.sleep(1000);
        }
    }

    @Test
    public void testNonInterruptableDoesntTimeout() throws Exception {
        bean.busyWait(10); // Busy wait time is less than timeout (=500)

        if (Thread.interrupted()) {
            fail("Thread was in interrupted state upon return");
        }

        Thread.sleep(2000); // Wait to ensure that our thread isn't interrupted after the method has finished
    }

    /**
     * Test method level override of Timeout value on a synchronous service.
     *
     * A timeout will not occur unless the configuration overrides the value set on the connectG method.
     */
    @Test
    public void testTimeoutConfig() throws ConnectException {
        //should timeout after a second as per default
        long start = System.currentTimeMillis();
        try {
            bean.connectG();
            throw new AssertionError("TimeoutException not thrown");
        } catch (TimeoutException e) {
            //expected!
            long timeout = System.currentTimeMillis();
            long duration = timeout - start;
            if (duration > 1000) { //the configured timeout is 500ms, if it takes 1000ms to fail then there is something wrong
                throw new AssertionError("TimeoutException not thrown quickly enough: " + duration);
            }
        }
    }

    /**
     * Test method level override of Timeout value on a synchronous service.
     *
     * A timeout will not occur unless the configuration overrides the value set on the classScopedConfigBean.
     */
    @Test
    public void testTimeoutClassScopeConfig() throws ConnectException {
        //should timeout after a second as per default
        long start = System.currentTimeMillis();
        try {
            classScopedConfigBean.connectA();
            throw new AssertionError("TimeoutException not thrown");
        } catch (TimeoutException e) {
            //expected!
            long timeout = System.currentTimeMillis();
            long duration = timeout - start;
            if (duration > 1000) { //the configured timeout is 500ms, if it takes 1000ms to fail then there is something wrong
                throw new AssertionError("TimeoutException not thrown quickly enough: " + duration);
            }
        }
    }
}
