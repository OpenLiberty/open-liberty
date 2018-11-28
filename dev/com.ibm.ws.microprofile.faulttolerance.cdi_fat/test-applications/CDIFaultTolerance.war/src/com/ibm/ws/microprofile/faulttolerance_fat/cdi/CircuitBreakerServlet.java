package com.ibm.ws.microprofile.faulttolerance_fat.cdi;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.CircuitBreakerBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.CircuitBreakerBean2;
import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Test
 */
@WebServlet("/circuitbreaker")
public class CircuitBreakerServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    CircuitBreakerBean bean;

    @Inject
    CircuitBreakerBean2 classScopeConfigBean;

    /**
     * Test the operation of the requestVolumeThreshold on a CircuitBreaker configured on a synchronous service
     * that is configured with a Timeout.
     *
     * @throws InterruptedException
     */
    @Test
    public void testCBFailureThresholdWithTimeout() throws InterruptedException {

        // FaultTolerance object with circuit breaker, should fail 3 times
        for (int i = 0; i < 3; i++) {
            try {
                bean.serviceA();
                throw new AssertionError("TimeoutException not caught");
            } catch (TimeoutException e) {
                //expected
            }
        }

        try {
            bean.serviceA();
            throw new AssertionError("CircuitBreakerOpenException not caught");
        } catch (CircuitBreakerOpenException e) {
            //expected
        }

        //allow time for the circuit to re-close
        Thread.sleep(5000);

        String res = bean.serviceA();
        if (!"serviceA: 4".equals(res)) {
            throw new AssertionError("Bad Result: " + res);
        }
    }

    /**
     * Test the operation of the requestVolumeThreshold on a CircuitBreaker configured on a synchronous service.
     *
     * @throws InterruptedException
     * @throws ConnectException
     */
    @Test
    public void testCBFailureThresholdWithException() throws InterruptedException, ConnectException {

        // FaultTolerance object with circuit breaker, should fail 3 times
        for (int i = 0; i < 3; i++) {
            try {
                bean.serviceB();
                throw new AssertionError("ConnectException not caught");
            } catch (ConnectException e) {
                if (!e.getMessage().equals("ConnectException: serviceB exception: " + (i + 1))) {
                    throw new AssertionError("ConnectException bad message: " + e.getMessage());
                }
            }
        }

        try {
            bean.serviceB();
            throw new AssertionError("CircuitBreakerOpenException not caught");
        } catch (CircuitBreakerOpenException e) {
            //expected
        }

        //allow time for the circuit to re-close
        Thread.sleep(3000);

        String res = bean.serviceB();
        if (!"serviceB: 4".equals(res)) {
            throw new AssertionError("Bad Result: " + res);
        }
    }

    /**
     * Test the behaviour of an asynchronous service configured with a CircuitBreaker.
     *
     * @throws Exception
     */
    @Test
    public void testCBAsync() throws Exception {
        for (int i = 0; i < 3; i++) {
            try {
                bean.serviceC().get();
                fail("Exception not thrown");
            } catch (ExecutionException e) {
                //assertThat("Execution exception cause", e.getCause(), instanceOf(ConnectException.class));
            }
        }

        // Circuit should now be open

        try {
            bean.serviceC().get();
            fail("Exception not thrown");
        } catch (CircuitBreakerOpenException e) {
            // Expected on 1.1
        } catch (ExecutionException e) {
            // Expected on 2.0
            assertThat(e.getCause(), instanceOf(CircuitBreakerOpenException.class));
        }
    }

    /**
     * Test the behaviour of an asynchronous service configured with a CircuitBreaker and Fallback method.
     *
     * @throws Exception
     */
    @Test
    public void testCBAsyncFallback() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(bean.serviceD().get(), is("serviceDFallback"));
        }

        assertThat(bean.getExecutionCounterD(), is(3));

        // Circuit should now be open
        Future<String> future = bean.serviceD();
        String result = future.get();

        // CB is open, expect to fall back
        assertThat(result, is("serviceDFallback"));

        // However, we don't expect the call to have reached the serviceD method
        assertThat(bean.getExecutionCounterD(), is(3));
    }

    /**
     * Test the behaviour of a service configured with a CircuitBreaker and Fallback method. This is the
     * synchronous version of testCBAsyncFallback.
     *
     * @throws Exception
     */
    @Test
    public void testCBSyncFallback() throws Exception {
        String result = null;
        for (int i = 0; i < 3; i++) {
            try {
                result = bean.serviceE();
                assertThat(result, is("serviceEFallback"));
            } catch (ConnectException e) {
                // We should have fallen back, assert if a ConnectException is thrown
                throw new AssertionError("ConnectException not expected");
            } catch (Exception ue) {
                // We should have fallen back, assert if an unexpected Exception is thrown
                throw new AssertionError("Unexpected exception " + ue);
            }
        }

        // Confirm that the service was called 3 times.
        assertThat(bean.getExecutionCounterE(), is(3));

        // Circuit should now be open. Attempt to call serviceE once more.
        try {
            result = bean.serviceE();
        } catch (ConnectException e) {
            // We should have fallen back, assert if a ConnectException is thrown
            throw new AssertionError("ConnectException not expected");
        } catch (Exception ue) {
            // We should have fallen back, assert if an unexpected Exception is thrown
            throw new AssertionError("Unexpected exception " + ue);
        }

        // CB is open, expect to fall back
        assertThat(result, is("serviceEFallback"));

        // However, we don't expect the call to have reached the serviceE method because the Circuit is open
        // so the counter should not have been further incremented.
        assertThat(bean.getExecutionCounterE(), is(3));
    }

    /**
     * Test the behaviour of a service configured with a CircuitBreaker and Retry annotation. In this test
     * maxRetries is set sufficiently high that the Circuit will open before maxRetries is reached and
     * a CircuitBreakerOpenException will be caught.
     *
     * @throws Exception
     */
    @Test
    public void testCBSyncRetryCircuitOpens() throws Exception {
        String result = null;

        try {
            result = bean.serviceF();
            throw new AssertionError("serviceF should be retried");
        } catch (CircuitBreakerOpenException cboe) {
            // Confirm that the service was called 3 times before this exception was thrown.
            assertThat(bean.getExecutionCounterF(), is(3));
        } catch (ConnectException e) {
            // We should have retried or thrown a CircuitBreakerOpenException, assert if a ConnectException is thrown
            throw new AssertionError("ConnectException not expected");
        } catch (Exception ue) {
            // We should have retried or thrown a CircuitBreakerOpenException, assert if an unexpected Exception is thrown
            throw new AssertionError("Unexpected exception " + ue);
        }
    }

    /**
     * Test the behaviour of a service configured with a CircuitBreaker and Retry annotation. In this test
     * maxRetries is set sufficiently low that the Circuit will remain closed after maxRetries is reached and
     * a ConnectionException will be caught.
     *
     * @throws Exception
     */
    @Test
    public void testCBSyncRetryCircuitClosed() throws Exception {
        String result = null;

        try {
            result = bean.serviceG();
            throw new AssertionError("serviceG should be retried");
        } catch (CircuitBreakerOpenException cboe) {
            throw new AssertionError("CircuitBreakerOpenException not expected");
        } catch (ConnectException e) {
            // Confirm that the service was called 2 times before this exception was thrown.
            assertThat(bean.getExecutionCounterG(), is(2));
        } catch (Exception ue) {
            // We should have retried or thrown a ConnectionException, assert if an unexpected Exception is thrown
            throw new AssertionError("Unexpected exception " + ue);
        }
    }

    /**
     * The Asynchronous equivalent of testCBSyncRetryCircuitOpens, to test the behaviour of a service configured
     * with CircuitBreaker, Retry and Asynchronous annotations. In this test maxRetries is set sufficiently high
     * that the Circuit will open before maxRetries is reached and a CircuitBreakerOpenException will be thrown.
     *
     * @throws Exception
     */
    @Test
    public void testCBAsyncRetryCircuitOpens() throws Exception {
        Future<String> future = null;
        try {
            future = bean.serviceH();
        } catch (Exception ue) {
            // Assert if an unexpected Exception is thrown
            throw new AssertionError("Unexpected exception " + ue);
        }

        try {
            String result = future.get();
            throw new AssertionError("serviceH should be retried");
        } catch (ExecutionException ee) {
            // The Service will be retried three times. On the third retry a CircuitBreakerOpenException
            // will be thrown. This will be wrapped by a java.util.ExecutionException and caught here.
            assertTrue("Cause was not CircuitBreakerOpenException", ee.getCause().toString().contains("CircuitBreakerOpenException"));
            // Confirm that the service was called 3 times before this exception was thrown.
            assertThat(bean.getExecutionCounterH(), is(3));
        } catch (Exception ue) {
            // We should have retried or thrown an ExecutionException, assert if an unexpected Exception is thrown
            throw new AssertionError("Unexpected exception " + ue);
        }
    }

    /**
     * The Asynchronous equivalent of testCBSyncRetryCircuitClosed, to test the behaviour of a service configured
     * with CircuitBreaker, Retry and Asynchronous annotations. In this test maxRetries is set sufficiently low
     * that the Circuit will remain closed after maxRetries is reached and a ConnectionException will be thrown.
     *
     * @throws Exception
     */
    @Test
    public void testCBAsyncRetryCircuitClosed() throws Exception {
        Future<String> future = null;
        try {
            future = bean.serviceI();
        } catch (Exception ue) {
            // Assert if an unexpected Exception is thrown
            throw new AssertionError("Unexpected exception " + ue);
        }

        try {
            String result = future.get();
            throw new AssertionError("serviceI should be retried");
        } catch (ExecutionException ee) {
            // The Service will be retried just once. After maxRetries is exceeded a ConnectException
            // will be thrown. This will be wrapped by a java.util.ExecutionException and caught here.
            assertTrue("Cause was not ConnectException", ee.getCause().toString().contains("ConnectException"));
            // Confirm that the service was called 2 times before this exception was thrown.
            assertThat(bean.getExecutionCounterI(), is(2));
        } catch (Exception ue) {
            // We should have retried or thrown a ConnectException, assert if an unexpected Exception is thrown
            throw new AssertionError("Unexpected exception " + ue);
        }
    }

    /**
     * An adaption of testCBFailureThresholdWithException, to test the CircuitBreaker behaviour is as expected
     * when the service initially succeeds but then begins to fail.
     *
     * @throws InterruptedException
     * @throws ConnectException
     */
    @Test
    public void testCBFailureThresholdWithRoll() throws InterruptedException, ConnectException {

        // FaultTolerance object with circuit breaker, should fail 3 times
        for (int i = 0; i < 5; i++) {
            try {
                bean.serviceJ();
                if (i > 1)
                    throw new AssertionError("ConnectException not caught");
            } catch (ConnectException e) {
                if (!e.getMessage().equals("ConnectException: serviceJ exception: " + (i + 1))) {
                    throw new AssertionError("ConnectException bad message: " + e.getMessage());
                }
            }
        }

        // The service should have failed 3 times in succession, so the Circuit should be Open.
        try {
            bean.serviceJ();
            throw new AssertionError("CircuitBreakerOpenException not caught");
        } catch (CircuitBreakerOpenException e) {
            //expected
        }

        //allow time for the circuit to re-close
        Thread.sleep(3000);

        String res = bean.serviceJ();
        if (!"serviceJ: 6".equals(res)) {
            throw new AssertionError("Bad Result: " + res);
        }
    }

    /**
     * Test method level override of requestVolumeThreshold on a CircuitBreaker configured on a synchronous service.
     *
     * The method annotation has a higher threshold (5) than that provided by configuration where it is set to 3.
     *
     * @throws InterruptedException
     * @throws ConnectException
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Test
    public void testCBFailureThresholdConfig(HttpServletRequest request,
                                             HttpServletResponse response) throws ServletException, IOException, InterruptedException, ConnectException {

        // FaultTolerance object with circuit breaker, should fail 3 times
        for (int i = 0; i < 3; i++) {
            try {
                bean.serviceK();
                throw new AssertionError("ConnectException not caught");
            } catch (ConnectException e) {
                if (!e.getMessage().equals("ConnectException: serviceK exception: " + (i + 1))) {
                    throw new AssertionError("ConnectException bad message: " + e.getMessage());
                }
            }
        }

        try {
            bean.serviceK();
            throw new AssertionError("CircuitBreakerOpenException not caught");
        } catch (CircuitBreakerOpenException e) {
            //expected
        }
    }

    /**
     * Test class level override of requestVolumeThreshold on a CircuitBreaker configured on a synchronous service.
     *
     * The class annotation has a higher threshold (5) than that provided by configuration where it is set to 3.
     *
     * @throws InterruptedException
     * @throws ConnectException
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Test
    public void testCBFailureThresholdClassScopeConfig(HttpServletRequest request,
                                                       HttpServletResponse response) throws ServletException, IOException, InterruptedException, ConnectException {

        // FaultTolerance object with circuit breaker, should fail 3 times
        for (int i = 0; i < 3; i++) {
            try {
                classScopeConfigBean.serviceA();
                throw new AssertionError("ConnectException not caught");
            } catch (ConnectException e) {
                if (!e.getMessage().equals("ConnectException: serviceA exception: " + (i + 1))) {
                    throw new AssertionError("ConnectException bad message: " + e.getMessage());
                }
            }
        }

        try {
            classScopeConfigBean.serviceA();
            throw new AssertionError("CircuitBreakerOpenException not caught");
        } catch (CircuitBreakerOpenException e) {
            //expected
        }
    }

    /**
     * Test that the delay param of a CircuitBreaker can be overridden via config
     *
     * @throws InterruptedException
     * @throws ConnectException
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Test
    public void testCBDelayConfig(HttpServletRequest request,
                                  HttpServletResponse response) throws ServletException, IOException, InterruptedException, ConnectException {

        // FaultTolerance object with circuit breaker, should fail 3 times
        for (int i = 0; i < 3; i++) {
            try {
                bean.serviceL();
                throw new AssertionError("ConnectException not caught");
            } catch (ConnectException e) {
                if (!e.getMessage().equals("ConnectException: serviceL exception: " + (i + 1))) {
                    throw new AssertionError("ConnectException bad message: " + e.getMessage());
                }
            }
        }

        try {
            bean.serviceL();
            throw new AssertionError("CircuitBreakerOpenException not caught");
        } catch (CircuitBreakerOpenException e) {
            //expected
        }

        //after three seconds, if the config override has not taken effect then the circuit still be open
        Thread.sleep(3000);

        String res = bean.serviceL();
        if (!"serviceL: 4".equals(res)) {
            throw new AssertionError("Bad Result: " + res);
        }
    }

}
