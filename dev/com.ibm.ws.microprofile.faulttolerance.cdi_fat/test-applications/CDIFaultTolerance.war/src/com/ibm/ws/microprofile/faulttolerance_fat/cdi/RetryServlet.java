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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.RetryBeanB;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.RetryBeanC;
import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;
import com.ibm.ws.microprofile.faulttolerance_fat.util.DisconnectException;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Test
 */
@WebServlet("/retry")
public class RetryServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    RetryBeanB beanB;

    @Inject
    RetryBeanC beanC;

    public void testRetry(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //should be retried 3 times as per default
        try {
            beanB.connectB();
            throw new AssertionError("Exception not thrown");
        } catch (ConnectException e) {
            String expected = "ConnectException: RetryBeanB Connect: 4";
            String actual = e.getMessage();
            if (!expected.equals(actual)) {
                throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
            }
        }
    }

    public void testInheritedAnnotations(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //should be retried 2 times due to class level annotation on beanB
        try {
            beanB.connectA();
            throw new AssertionError("Exception not thrown");
        } catch (ConnectException e) {
            String expected = "ConnectException: RetryBeanA Connect: 3";
            String actual = e.getMessage();
            if (!expected.equals(actual)) {
                throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
            }
        }
        //should be retried 4 times due to method level annotation in beanA
        try {
            beanB.disconnectA();
            throw new AssertionError("Exception not thrown");
        } catch (DisconnectException e) {
            String expected = "DisconnectException: RetryBeanA Disconnect: 5";
            String actual = e.getMessage();
            if (!expected.equals(actual)) {
                throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
            }
        }
    }

    /**
     * Test that the abortOn parameter is handled correctly
     */
    public void testRetryAbortOn(HttpServletRequest request, HttpServletResponse response) {
        try {
            beanC.connectC();
        } catch (ConnectException e) {
            // Connect count should be 1 because abortOn is set to include ConnectException
            assertThat("Exception message", e.getMessage(), is("ConnectException: RetryBeanC Connect: 1"));
        }
    }

    /**
     * Test that the abortOn parameter is handled correctly on an asynchronous call
     */
    public void testRetryAbortOnAsync(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            beanC.connectCAsync().get();
        } catch (ExecutionException e) {
            // Connect count should be 1 because abortOn is set to include ConnectException
            assertThat("Exception message", e.getCause().getMessage(), containsString("ConnectException: RetryBeanC Connect: 1"));
        }
    }

    /**
     * Test that we can configure the abortOn parameter using config
     * <p>
     * Worth testing specifically because it takes an array of Classes
     */
    public void testRetryAbortOnConfig(HttpServletRequest request, HttpServletResponse response) {
        try {
            beanC.connectC2();
        } catch (ConnectException e) {
            // Connect count should be 1 because abortOn is set to include ConnectException
            assertThat("Exception message", e.getMessage(), is("ConnectException: RetryBeanC Connect: 1"));
        }
    }

    /**
     * This test should only pass if MP_Fault_Tolerance_NonFallback_Enabled is set to false
     */
    public void testRetryDisabled(HttpServletRequest request, HttpServletResponse response) {
        try {
            beanB.connectB();
            fail("Exception not thrown");
        } catch (ConnectException e) {
            // If Retry is disabled, then the call should *not* be retried, so there's only one connect attempt
            assertThat("Exception message", e.getMessage(), is("ConnectException: RetryBeanB Connect: 1"));
        }
    }

    public void testRetryForever(HttpServletRequest request, HttpServletResponse response) throws Exception {
        beanC.connectCForever();
        assertThat(beanC.getConnectCount(), is(5));
    }

    public void testRetryDurationZero(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            beanC.connectCDurationZero();
            fail("Exception not thrown");
        } catch (ConnectException e) {
            // Expected
        }
        assertThat(beanC.getConnectCount(), is(6));
    }

}
