package com.ibm.ws.microprofile.faulttolerance_fat.cdi;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.CircuitBreakerBean;
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

    /**
     * @throws InterruptedException
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    public void testCBFailureThresholdWithTimeout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, InterruptedException {

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
     * @throws InterruptedException
     * @throws ConnectException
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    public void testCBFailureThresholdWithException(HttpServletRequest request,
                                                    HttpServletResponse response) throws ServletException, IOException, InterruptedException, ConnectException {

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
            bean.serviceC();
            fail("Exception not thrown");
        } catch (CircuitBreakerOpenException e) {
            // Expected
        }
    }

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
     * This test should only pass if MP_Fault_Tolerance_NonFallback_Enabled is set to false
     */
    public void testCBDisabled(HttpServletRequest request, HttpServletResponse response) throws Exception {
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

        // If circuit breaker is enabled, the next method should throw a CircuitBreakerOpenException
        // If circuit breaker is disabled, it should succeed.
        bean.serviceB();
    }

}
