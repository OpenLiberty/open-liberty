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

import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.RetryBeanB;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.RetryBeanC;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.RetryBeanD;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.RetryBeanE;
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

    @Inject
    RetryBeanD beanD;

    @Inject
    RetryBeanE beanE;

    @Test
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

    @Test
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
    @Test
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
    @Test
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
    @Test
    public void testRetryAbortOnConfig(HttpServletRequest request, HttpServletResponse response) {
        try {
            beanC.connectC2();
        } catch (ConnectException e) {
            // Connect count should be 1 because abortOn is set to include ConnectException
            assertThat("Exception message", e.getMessage(), is("ConnectException: RetryBeanC Connect: 1"));
        }
    }

    @Test
    public void testRetryForever(HttpServletRequest request, HttpServletResponse response) throws Exception {
        beanC.connectCForever();
        assertThat(beanC.getConnectCount(), is(5));
    }

    @Test
    public void testRetryDurationZero(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            beanC.connectCDurationZero();
            fail("Exception not thrown");
        } catch (ConnectException e) {
            // Expected
        }
        assertThat(beanC.getConnectCount(), is(6));
    }

    /**
     * Test method level override of maxRetries attribute on Retry annotation on a synchronous service.
     *
     * The method will not be executed the expected number of times unless the configuration overrides the value
     * set on the connectCMaxRetries1 method.
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @Test
    public void testRetryMaxRetriesConfig(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            beanC.connectCMaxRetries1();
            fail("Exception not thrown");
        } catch (ConnectException e) {
            // Expected
        }
        assertThat(beanC.getConnectCount(), is(5));
    }

    /**
     * Test class level override of maxRetries attribute on Retry annotation on a synchronous service.
     *
     * The method will not be executed the expected number of times unless the configuration overrides the value
     * set on beanD.
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @Test
    public void testRetryMaxRetriesClassScopeConfig(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            beanD.connectDMaxRetries2();
            fail("Exception not thrown");
        } catch (ConnectException e) {
            // Expected
        }
        assertThat(beanD.getConnectCount(), is(5));
    }

    /**
     * Test that the Class-level annotation IS NOT be overridden by config at the method level
     * In issue #186, we planned to allow class level config overrides for method level annotations.
     * One of the TCK tests specifically tests that such overrides cannot be made and we will revert
     * the change to line up with the TCK. This behaviour should be revisited in a future release.
     *
     * In the meantime, under issue #542, the behaviour was reverted and this test will be reworked to
     * confirm the original behaviour.
     *
     * Retry/maxRetries is set to 6 for this method in the config
     */
    @Test
    public void testRetryMaxRetriesClassLevelConfigForMethodAnnotation() {
        try {
            beanE.connect();
            fail("Exception not thrown");
        } catch (ConnectException e) {
            // Expected
        }
        assertThat(beanE.getConnectCount(), is(3)); // would be 7 if config had overridden
    }

}
