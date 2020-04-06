/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * CDI Test
 * 
 * Perform tests of interception.
 */
@MinimumJavaLevel(javaLevel = 7)
public class CDIServletInterceptorTest extends LoggingTest {

    // Server instance ...

    /** A single shared server used by all of the tests. */
    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_cdiServletInterceptorServer");

    /**
     * Log text at the info level. Use the shared server to perform the logging.
     * 
     * @param text Text which is to be logged.
     */
    public static void logInfo(String text) {
        SHARED_SERVER.logInfo(text);
    }

    /**
     * Wrapper for {@link #createWebBrowserForTestCase()} with relaxed protection.
     * 
     * @return A web brower.
     */
    protected WebBrowser createWebBrowser() {
        return createWebBrowserForTestCase();
    }

    /**
     * Perform a request to the the server instance and verify that the
     * response has expected text. Throw an exception if the expected
     * text is not present or if the unexpected text is present.
     * 
     * The request path is used to create a request URL via {@link SharedServer.getServerUrl}.
     * 
     * Both the expected text and the unexpected text are tested using a contains
     * test. The test does not look for an exact match.
     * 
     * @param webBrowser Simulated web browser instance through which the request is made.
     * @param requestPath The path which will be requested.
     * @param expectedResponses Expected response text. All elements are tested.
     * @param unexpectedResponses Unexpected response text. All elements are tested.
     * @return The encapsulated response.
     * 
     * @throws Exception Thrown if the expected response text is not present or if the
     *             unexpected response text is present.
     */
    protected WebResponse verifyResponse(WebBrowser webBrowser, String resourceURL, String[] expectedResponses, String[] unexpectedResponses) throws Exception {
        return SHARED_SERVER.verifyResponse(webBrowser, resourceURL, expectedResponses, unexpectedResponses); // throws Exception
    }

    /** Standard failure text. Usually unexpected. */
    public static final String[] FAILED_RESPONSE = new String[] { "FAILED" };

    // URL values for the servlet interceptor servlet ...

    public static final String COUNTER_INTERCEPTOR_CONTEXT_ROOT = "/CDI12TestV2Counter";
    public static final String COUNTER_INTERCEPTOR_URL_FRAGMENT = "/CDICounter";
    public static final String COUNTER_INTERCEPTOR_URL = COUNTER_INTERCEPTOR_CONTEXT_ROOT + COUNTER_INTERCEPTOR_URL_FRAGMENT;

    public static final String OPERATION_INCREMENT = "increment";
    public static final String OPERATION_DECREMENT = "decrement";
    public static final String OPERATION_GET_COUNT = "getCount";
    public static final String OPERATION_DISPLAY_LOG = "displayLog";

    // Operation selection ...

    public static final String OPERATION_PARAMETER_NAME = "operation";

    /**
     * Generate the url for a request to the counter servlet.
     * 
     * @param operationName The operation of the request.
     * 
     * @return The request url.
     */
    public String getCounterURL(String operationName) {
        return COUNTER_INTERCEPTOR_URL + "?" + OPERATION_PARAMETER_NAME + "=" + operationName;
    }

    // The test has two beans: the servlet counter, which is session scoped,
    // and the application log, which is application scoped.
    //
    // The test places an interceptor on the counter bean operations
    // 'increment', 'decrement', and 'getCount'.  The interceptor
    // logs the operation to the application log.
    //
    // The responses are expected to show the counter state updated
    // relative to each of the sessions, with the counter clearing
    // between sessions.
    //
    // The responses are expected to show, first, that the interceptor
    // is firing and the log is being populated, and second, that the
    // same log is shared between the sessions.

    // @formatter:off
    
    public static final String[] EXPECTED_S1_INITIAL_GET = {
        ":Servlet:Entry:",
        "Count [ 0 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_S1_OP1 = {
        ":Servlet:Entry:",
        "Increment [ 1 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_S1_OP2 = {
        ":Servlet:Entry:",
        "Decrement [ 0 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_S1_OP3 = {
        ":Servlet:Entry:",
        "Increment [ 1 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_S1_FINAL_GET = {
        ":Servlet:Entry:",
        "Count [ 1 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_S1_LOG = {
        ":Servlet:Entry:",
        "Application Log [",
        "[ ServletServiceInterceptor: logService: Counter [ getCount ]",
        "[ ServletServiceInterceptor: logService: Counter [ increment ]",
        "[ ServletServiceInterceptor: logService: Counter [ decrement ]",
        "[ ServletServiceInterceptor: logService: Counter [ increment ]",
        "[ ServletServiceInterceptor: logService: Counter [ getCount ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_S2_INITIAL_GET = {
        ":Servlet:Entry:",
        "Count [ 0 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_S2_OP1 = {
        ":Servlet:Entry:",
        "Decrement [ -1 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_S2_OP2 = {
        ":Servlet:Entry:",
        "Increment [ 0 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_S2_OP3 = {
        ":Servlet:Entry:",
        "Decrement [ -1 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_S2_FINAL_GET = {
        ":Servlet:Entry:",
        "Count [ -1 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_S2_LOG = {
        ":Servlet:Entry:",
        "Application Log [",
        "ServletServiceInterceptor: logService: Counter [ getCount ]",
        "[ ServletServiceInterceptor: logService: Counter [ increment ]",
        "[ ServletServiceInterceptor: logService: Counter [ decrement ]",
        "[ ServletServiceInterceptor: logService: Counter [ increment ]",
        "[ ServletServiceInterceptor: logService: Counter [ getCount ]",
        "[ ServletServiceInterceptor: logService: Counter [ getCount ]",
        "[ ServletServiceInterceptor: logService: Counter [ decrement ]",
        "[ ServletServiceInterceptor: logService: Counter [ increment ]",
        "[ ServletServiceInterceptor: logService: Counter [ decrement ]",
        "[ ServletServiceInterceptor: logService: Counter [ getCount ]",
        ":Servlet:Exit:"
    };
    
    // @formatter:on

    // @formatter:off
    
    /**
     * Perform tests of interception.
     * 
     * @throws Exception Thrown in case of an error running the tests.
     */
    @Test
    @Mode(TestMode.LITE)
    public void testCDIInterceptor() throws Exception {
        WebBrowser firstSessionBrowser = createWebBrowserForTestCase();

        verifyResponse(firstSessionBrowser,
                       getCounterURL(OPERATION_GET_COUNT),
                       EXPECTED_S1_INITIAL_GET, FAILED_RESPONSE);

        verifyResponse(firstSessionBrowser,
                       getCounterURL(OPERATION_INCREMENT),
                       EXPECTED_S1_OP1, FAILED_RESPONSE);
        verifyResponse(firstSessionBrowser,
                       getCounterURL(OPERATION_DECREMENT),
                       EXPECTED_S1_OP2, FAILED_RESPONSE);
        verifyResponse(firstSessionBrowser,
                       getCounterURL(OPERATION_INCREMENT),
                       EXPECTED_S1_OP3, FAILED_RESPONSE);
        verifyResponse(firstSessionBrowser,
                       getCounterURL(OPERATION_GET_COUNT),
                       EXPECTED_S1_FINAL_GET, FAILED_RESPONSE);

        verifyResponse(firstSessionBrowser,
                       getCounterURL(OPERATION_DISPLAY_LOG),
                       EXPECTED_S1_LOG, FAILED_RESPONSE);

        //

        WebBrowser secondSessionBrowser = createWebBrowserForTestCase();

        verifyResponse(secondSessionBrowser,
                       getCounterURL(OPERATION_GET_COUNT),
                       EXPECTED_S2_INITIAL_GET, FAILED_RESPONSE);

        verifyResponse(secondSessionBrowser,
                       getCounterURL(OPERATION_DECREMENT),
                       EXPECTED_S2_OP1, FAILED_RESPONSE);
        verifyResponse(secondSessionBrowser,
                       getCounterURL(OPERATION_INCREMENT),
                       EXPECTED_S2_OP2, FAILED_RESPONSE);
        verifyResponse(secondSessionBrowser,
                       getCounterURL(OPERATION_DECREMENT),
                       EXPECTED_S2_OP3, FAILED_RESPONSE);
        verifyResponse(secondSessionBrowser,
                       getCounterURL(OPERATION_GET_COUNT),
                       EXPECTED_S2_FINAL_GET, FAILED_RESPONSE);

        verifyResponse(secondSessionBrowser,
                       getCounterURL(OPERATION_DISPLAY_LOG),
                       EXPECTED_S2_LOG, FAILED_RESPONSE);
    }
}
