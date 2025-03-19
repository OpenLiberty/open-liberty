/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * CDI Test
 *
 * Perform tests of interception.
 */
@RunWith(FATRunner.class)
public class CDIServletInterceptorTest {

    private static final Logger LOG = Logger.getLogger(CDIServletInterceptorTest.class.getName());

    private static final String CDI12_TEST_V2_JAR_NAME = "CDI12TestV2";
    private static final String CDI12_TEST_V2_COUNTER_APP_NAME = "CDI12TestV2Counter";

    // Server instance
    @Server("servlet31_cdiServletInterceptorServer")
    public static LibertyServer LS;

    /**
     * Log text at the info level.
     *
     * @param text Text which is to be logged.
     */
    public static void logInfo(String text) {
        LOG.info(text);
    }

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the CDI12TestV2 jar to add to the war app as a lib
        JavaArchive CDI12TestV2Jar = ShrinkHelper.buildJavaArchive(CDI12_TEST_V2_JAR_NAME + ".jar",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.log",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2");
        CDI12TestV2Jar = (JavaArchive) ShrinkHelper.addDirectory(CDI12TestV2Jar, "test-applications/CDI12TestV2.jar/resources");
        // Build the war app CDI12TestV2Counter.war and add the dependencies
        WebArchive CDI12TestV2CounterApp = ShrinkHelper.buildDefaultApp(CDI12_TEST_V2_COUNTER_APP_NAME + ".war",
                                                                        "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2counter.war.cdi.interceptors.servlets");
        CDI12TestV2CounterApp = (WebArchive) ShrinkHelper.addDirectory(CDI12TestV2CounterApp, "test-applications/CDI12TestV2Counter.war/resources");
        CDI12TestV2CounterApp = CDI12TestV2CounterApp.addAsLibrary(CDI12TestV2Jar);

        // Export the application.
        ShrinkHelper.exportDropinAppToServer(LS, CDI12TestV2CounterApp);

        // Start the server and use the class name so we can find logs easily.
        LS.startServer(CDIServletInterceptorTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (LS != null && LS.isStarted()) {
            LS.stopServer();
          }
    }

    // URL values for the servlet interceptor servlet ...

    public static final String COUNTER_INTERCEPTOR_CONTEXT_ROOT = "/CDI12TestV2Counter";
    public static final String COUNTER_INTERCEPTOR_URL_FRAGMENT = "/CDICounter";

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
        return COUNTER_INTERCEPTOR_URL_FRAGMENT + "?" + OPERATION_PARAMETER_NAME + "=" + operationName;
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
        HttpClient session1 = new HttpClient();
        verifyStringsInResponse(session1, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_GET_COUNT), EXPECTED_S1_INITIAL_GET);
        verifyStringsInResponse(session1, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_INCREMENT), EXPECTED_S1_OP1);
        verifyStringsInResponse(session1, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_DECREMENT), EXPECTED_S1_OP2);
        verifyStringsInResponse(session1, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_INCREMENT), EXPECTED_S1_OP3);
        verifyStringsInResponse(session1, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_GET_COUNT), EXPECTED_S1_FINAL_GET);
        verifyStringsInResponse(session1, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_DISPLAY_LOG), EXPECTED_S1_LOG);

        HttpClient session2 = new HttpClient();
        verifyStringsInResponse(session2, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_GET_COUNT), EXPECTED_S2_INITIAL_GET);
        verifyStringsInResponse(session2, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_DECREMENT), EXPECTED_S2_OP1);
        verifyStringsInResponse(session2, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_INCREMENT), EXPECTED_S2_OP2);
        verifyStringsInResponse(session2, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_DECREMENT), EXPECTED_S2_OP3);
        verifyStringsInResponse(session2, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_GET_COUNT), EXPECTED_S2_FINAL_GET);
        verifyStringsInResponse(session2, COUNTER_INTERCEPTOR_CONTEXT_ROOT, getCounterURL(OPERATION_DISPLAY_LOG), EXPECTED_S2_LOG);
    }

    private void verifyStringsInResponse(HttpClient client, String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        GetMethod get = new GetMethod("http://" + LS.getHostname() + ":" + LS.getHttpDefaultPort() + contextRoot + path);
        int responseCode = client.executeMethod(get);
        String responseBody = get.getResponseBodyAsString();
        LOG.info("Response : " + responseBody);
  
        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, responseCode);
  
        for (String expectedResponse : expectedResponseStrings) {
            assertTrue("The response did not contain: " + expectedResponse, responseBody.contains(expectedResponse));
        }
    }
}
