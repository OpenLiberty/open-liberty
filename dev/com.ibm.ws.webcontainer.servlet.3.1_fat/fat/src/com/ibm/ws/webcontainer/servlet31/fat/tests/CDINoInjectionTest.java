/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
import com.ibm.ws.fat.util.SharedServer;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * CDI Test
 *
 * Verify that we get an expected response from a CDI server with no injection
 */
@RunWith(FATRunner.class)
public class CDINoInjectionTest {

    private static final Logger LOG = Logger.getLogger(CDINoInjectionTest.class.getName());

    @Server("servlet31_cdiNoInjectionServer")
    public static LibertyServer server;

    private static final String CDI12_TEST_V2_JAR_NAME = "CDI12TestV2";
    private static final String CDI12_TEST_V2_NO_INJECTION_APP_NAME = "CDI12TestV2NoInjection";

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
     * @param webBrowser          Simulated web browser instance through which the request is made.
     * @param requestPath         The path which will be requested.
     * @param expectedResponses   Expected response text. All elements are tested.
     * @param unexpectedResponses Unexpected response text. All elements are tested.
     * @return The encapsulated response.
     *
     * @throws Exception Thrown if the expected response text is not present or if the
     *                       unexpected response text is present.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the CDI12TestV2 jar to add to the war app as a lib
        JavaArchive cdi12TestV2Jar = ShrinkHelper.buildJavaArchive(CDI12_TEST_V2_JAR_NAME + ".jar",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.log",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2");
        cdi12TestV2Jar = (JavaArchive) ShrinkHelper.addDirectory(cdi12TestV2Jar, "test-applications/CDI12TestV2.jar/resources");
        // Build the war app CDI12TestV2NoInjection.war and add the dependencies
        WebArchive cdi12TestV2NoInjectionApp = ShrinkHelper.buildDefaultApp(CDI12_TEST_V2_NO_INJECTION_APP_NAME + ".war",
                                                                            "com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2noinjection.war.cdi.servlets");
        cdi12TestV2NoInjectionApp = cdi12TestV2NoInjectionApp.addAsLibraries(cdi12TestV2Jar);

        // Export the application.
        ShrinkHelper.exportDropinAppToServer(server, cdi12TestV2NoInjectionApp);

        // Start the server and use the class name so we can find logs easily.
        server.startServer(CDINoInjectionTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    // Servlet cases ...

    public static final String SERVLET_NO_INJECTION_CONTEXT_ROOT = "/CDI12TestV2NoInjection";
    public static final String SERVLET_NO_INJECTION_URL_FRAGMENT = "/CDINoInjection";

    public static final String[] SERVLET_NO_INJECTION_EXPECTED = {
                                                                   "Servlet Hello! No Injection",
                                                                   "Filter Hello! No Injection",
                                                                   "Listener Hello! No Injection"
    };

    @Test
    @Mode(TestMode.LITE)
    public void testCDIEnabledNoInjection() throws Exception {
        verifyStringsInResponse(SERVLET_NO_INJECTION_CONTEXT_ROOT,
                                SERVLET_NO_INJECTION_URL_FRAGMENT,
                                SERVLET_NO_INJECTION_EXPECTED);
    }

    private void verifyStringsInResponse(String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + contextRoot + path);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());

        String responseText = response.getText();

        for (String expectedResponse : expectedResponseStrings) {
            assertTrue("The response did not contain: " + expectedResponse, responseText.contains(expectedResponse));
        }
    }
}
