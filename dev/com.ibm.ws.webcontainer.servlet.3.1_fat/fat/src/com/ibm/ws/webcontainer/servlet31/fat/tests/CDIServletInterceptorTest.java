/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.util.logging.Logger;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * CDI Test
 *
 * Perform tests of interception.
 * Temporarily skipped for EE9 jakarta until cdi-3.0 feature is developed
 */
@RunWith(FATRunner.class)
@SkipForRepeat(EE9_FEATURES)
public class CDIServletInterceptorTest extends LoggingTest {
    
    private static final Logger LOG = Logger.getLogger(CDIServletInterceptorTest.class.getName());

    private static final String CDI12_TEST_V2_JAR_NAME = "CDI12TestV2";
    private static final String CDI12_TEST_V2_COUNTER_APP_NAME = "CDI12TestV2Counter";

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
        // Verify if the apps are in the server before trying to deploy them
        if (SHARED_SERVER.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(CDI12_TEST_V2_COUNTER_APP_NAME);
            LOG.info("addAppToServer : " + CDI12_TEST_V2_COUNTER_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
              ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), CDI12TestV2CounterApp);
          }
        SHARED_SERVER.startIfNotStarted();
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + CDI12_TEST_V2_COUNTER_APP_NAME);
    }
    
    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (SHARED_SERVER.getLibertyServer() != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer(null);
        }
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

    /* (non-Javadoc)
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }
}
