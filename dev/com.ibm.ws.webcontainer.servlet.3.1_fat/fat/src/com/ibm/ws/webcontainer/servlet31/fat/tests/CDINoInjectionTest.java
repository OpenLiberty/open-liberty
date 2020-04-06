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
 * Verify that we get an expected response from a CDI server with no injection
 */
@MinimumJavaLevel(javaLevel = 7)
public class CDINoInjectionTest extends LoggingTest {

    // Server instance ...
    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_cdiNoInjectionServer");

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

    // Servlet cases ...

    public static final String SERVLET_NO_INJECTION_CONTEXT_ROOT = "/CDI12TestV2NoInjection";
    public static final String SERVLET_NO_INJECTION_URL_FRAGMENT = "/CDINoInjection";
    public static final String SERVLET_NO_INJECTION_URL = SERVLET_NO_INJECTION_CONTEXT_ROOT + SERVLET_NO_INJECTION_URL_FRAGMENT;

    public static final String[] SERVLET_NO_INJECTION_EXPECTED = {
                                                                  "Servlet Hello! No Injection",
                                                                  "Filter Hello! No Injection",
                                                                  "Listener Hello! No Injection"
    };

    @Test
    @Mode(TestMode.LITE)
    public void testCDIEnabledNoInjection() throws Exception {
        WebBrowser webBrowser = createWebBrowserForTestCase();
        verifyResponse(webBrowser,
                       SERVLET_NO_INJECTION_URL,
                       SERVLET_NO_INJECTION_EXPECTED, FAILED_RESPONSE);
    }
}
