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
package com.ibm.ws.microprofile.faulttolerance_fat.cdi;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.CircuitBreakerBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.FallbackBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.FallbackBeanWithoutRetry;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.RetryBeanB;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.TimeoutBean;
import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

import componenttest.app.FATServlet;

/**
 * These tests should only pass if MP_Fault_Tolerance_NonFallback_Enabled is set to false
 */
@WebServlet("/annotations-disabled")
public class AnnotationsDisabledServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    AsyncBean asyncBean;

    @Inject
    CircuitBreakerBean cbBean;

    @Inject
    RetryBeanB retryBean;

    @Inject
    TimeoutBean timeoutBean;

    @Inject
    FallbackBean fallbackBean;

    @Inject
    FallbackBeanWithoutRetry fallbackBeanWithoutRetry;

    @Test
    public void testAsyncDisabled() throws Exception {
        long start = System.currentTimeMillis();
        Future<Connection> future = asyncBean.connectA();
        long end = System.currentTimeMillis();
        long duration = end - start;

        // Ensure that this method was executed synchronously
        assertThat("Call duration", duration, greaterThan(TestConstants.WORK_TIME - TestConstants.TEST_TWEAK_TIME_UNIT));
        assertThat("Call result", future.get(), is(notNullValue()));
        assertThat("Call result", future.get().getData(), equalTo(AsyncBean.CONNECT_A_DATA));
    }

    @Test
    public void testCBDisabled(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // FaultTolerance object with circuit breaker, should fail 3 times
        for (int i = 0; i < 3; i++) {
            try {
                cbBean.serviceB();
                throw new AssertionError("ConnectException not caught");
            } catch (ConnectException e) {
                if (!e.getMessage().equals("ConnectException: serviceB exception: " + (i + 1))) {
                    throw new AssertionError("ConnectException bad message: " + e.getMessage());
                }
            }
        }

        // If circuit breaker is enabled, the next method should throw a CircuitBreakerOpenException
        // If circuit breaker is disabled, it should succeed.
        cbBean.serviceB();
    }

    @Test
    public void testRetryDisabled() {
        try {
            retryBean.connectB();
            fail("Exception not thrown");
        } catch (ConnectException e) {
            // If Retry is disabled, then the call should *not* be retried, so there's only one connect attempt
            assertThat("Exception message", e.getMessage(), is("ConnectException: RetryBeanB Connect: 1"));
        }
    }

    @Test
    public void testTimeoutDisabled(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            timeoutBean.connectA();
            fail("No exception thrown");
        } catch (ConnectException e) {
            // expected, as Timeout should be disabled
        } catch (TimeoutException e) {
            // Not expected! rethrow
            throw e;
        }
    }

    /**
     * Retry is disabled but fallback should still work
     */
    @Test
    public void testFallbackRetryDisabled() throws ConnectException {
        Connection connection = fallbackBean.connectA();
        String data = connection.getData();
        assertThat(data, equalTo("Fallback for: connectA - data!"));
        // Connect count should only be 1 since retry is disabled
        assertThat("Call count", fallbackBean.getConnectCountA(), is(1));
    }

    /**
     * Fallback should work as usual
     */
    @Test
    public void testFallbackWithoutRetry() throws ConnectException {
        //should fallback immediately
        Connection connection = fallbackBeanWithoutRetry.connectA();
        String data = connection.getData();
        assertThat(data, equalTo("Fallback for: connectA - data!"));
        assertThat("Call count", fallbackBeanWithoutRetry.getConnectCountA(), is(1));
    }

}
