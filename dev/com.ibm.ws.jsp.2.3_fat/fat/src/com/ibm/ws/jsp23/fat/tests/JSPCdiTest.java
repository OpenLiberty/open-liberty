/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsp23.fat.JSPUtils;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jspCdiServer that use HttpUnit.
 */

@SkipForRepeat("EE9_FEATURES")
@RunWith(FATRunner.class)
public class JSPCdiTest {
    private static final Logger LOG = Logger.getLogger(JSPCdiTest.class.getName());
    private static final String TESTINJECTION_APP_NAME = "TestInjection";

    @Server("jspCdiServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server,
                                      TESTINJECTION_APP_NAME + ".war",
                                      "com.ibm.ws.jsp23.fat.testinjection.beans",
                                      "com.ibm.ws.jsp23.fat.testinjection.interceptors",
                                      "com.ibm.ws.jsp23.fat.testinjection.listeners",
                                      "com.ibm.ws.jsp23.fat.testinjection.servlets",
                                      "com.ibm.ws.jsp23.fat.testinjection.tagHandler");

        server.startServer(JSPCdiTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * A sample HttpUnit test case for JSP. Just ensure that the basic application is reachable.
     *
     * @throws Exception
     */
    @Test
    public void helloWorldTest() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, TESTINJECTION_APP_NAME, "SimpleTestServlet");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());
        assertTrue("The response did not contain: Hello World", response.getText().contains("Hello World"));
    }

    /**
     * Test Tag Handlers with CDI.
     *
     * @throws Exception
     */
    @Test
    public void testTag1() throws Exception {
        // Each entry in the array is an expected output in the response
        String[] expectedInResponse = {
                                        "<b>Test 1:</b> Test Start",
                                        "<b>Test 2:</b> Message: DependentBean Hit SessionBean Hit RequestBean Hit ...constructor injection OK ...interceptor OK",
        };

        this.verifyStringsInResponse(TESTINJECTION_APP_NAME, "Tag1.jsp", expectedInResponse);
    }

    /**
     * Test Tag Handlers with CDI Method Injection.
     *
     * @throws Exception
     */
    @Test
    public void testTag2() throws Exception {
        // Each entry in the array is an expected output in the response
        String[] expectedInResponse = {
                                        "<b>Test 1:</b> Test Start",
                                        "<b>Test 2:</b> Message: BeanCounters are OK"
        };

        this.verifyStringsInResponse(TESTINJECTION_APP_NAME, "Tag2.jsp", expectedInResponse);
    }

    /**
     * Test the tag library event listeners injected using CDI.
     * Constructor injection.
     *
     * @throws Exception
     */
    @Test
    public void testTagLibraryEventListenerCI() throws Exception {
        final int iterations = 5;

        DefaultHttpClient client = new DefaultHttpClient();
        DefaultHttpClient client2 = new DefaultHttpClient(); //Used to test @SessionScoped

        String url = JSPUtils.createHttpUrlString(server, TESTINJECTION_APP_NAME, "TagLibraryEventListenerCI.jsp?increment=true");
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);
        HttpResponse response;

        for (int i = 1; i <= iterations; i++) {
            String[] expectedInResponse = {
                                            "<li>TestConstructorInjection index: " + i + "</li>",
                                            "<li>TestConstructorInjectionRequest index: " + 1 + "</li>",
                                            "<li>TestConstructorInjectionApplication index: " + i + "</li>",
                                            "<li>TestConstructorInjectionSession index: " + i + "</li>"
            };
            response = client.execute(getMethod);
            assertEquals("Expected " + 200 + " status code was not returned!",
                         200, response.getStatusLine().getStatusCode());

            String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
            LOG.info("Response content: " + content);
            org.apache.http.util.EntityUtils.consume(response.getEntity());

            for (String expectedResponse : expectedInResponse) {
                assertTrue("The response did not contain: " + expectedResponse, content.contains(expectedResponse));
            }
        }

        //Testing if a new TestConstructorInjectionSession was injected when another session is used
        String[] expectedInResponse = {
                                        "<li>TestConstructorInjection index: " + (iterations + 1) + "</li>",
                                        "<li>TestConstructorInjectionRequest index: " + 1 + "</li>",
                                        "<li>TestConstructorInjectionApplication index: " + (iterations + 1) + "</li>",
                                        "<li>TestConstructorInjectionSession index: " + 1 + "</li>"
        };

        response = client2.execute(getMethod);
        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getStatusLine().getStatusCode());

        String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
        LOG.info("Response content: " + content);
        org.apache.http.util.EntityUtils.consume(response.getEntity());

        for (String expectedResponse : expectedInResponse) {
            assertTrue("The response did not contain: " + expectedResponse, content.contains(expectedResponse));
        }
    }

    /**
     * Test the tag library event listeners injected using CDI.
     * Field injection.
     *
     * @throws Exception
     */
    @Test
    public void testTagLibraryEventListenerFI() throws Exception {
        final int iterations = 5;

        DefaultHttpClient client = new DefaultHttpClient();
        DefaultHttpClient client2 = new DefaultHttpClient(); //Used to test @SessionScoped

        String url = JSPUtils.createHttpUrlString(server, TESTINJECTION_APP_NAME, "TagLibraryEventListenerFI.jsp?increment=true");
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);
        HttpResponse response;

        for (int i = 1; i <= iterations; i++) {
            String[] expectedInResponse = {
                                            "<li>TestFieldInjection index: " + i + "</li>",
                                            "<li>TestFieldInjectionRequest index: " + 1 + "</li>",
                                            "<li>TestFieldInjectionApplication index: " + i + "</li>",
                                            "<li>TestFieldInjectionSession index: " + i + "</li>"
            };

            response = client.execute(getMethod);
            assertEquals("Expected " + 200 + " status code was not returned!",
                         200, response.getStatusLine().getStatusCode());

            String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
            LOG.info("Response content: " + content);
            org.apache.http.util.EntityUtils.consume(response.getEntity());

            for (String expectedResponse : expectedInResponse) {
                assertTrue("The expected response String was not found" + expectedResponse, content.contains(expectedResponse));
            }
        }

        //Testing if a new TestFieldInjectionSession was injected when another session is used
        String[] expectedInResponse = {
                                        "<li>TestFieldInjection index: " + (iterations + 1) + "</li>",
                                        "<li>TestFieldInjectionRequest index: " + 1 + "</li>",
                                        "<li>TestFieldInjectionApplication index: " + (iterations + 1) + "</li>",
                                        "<li>TestFieldInjectionSession index: " + 1 + "</li>"
        };

        response = client2.execute(getMethod);
        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getStatusLine().getStatusCode());

        String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
        LOG.info("Response content: " + content);
        org.apache.http.util.EntityUtils.consume(response.getEntity());

        for (String expectedResponse : expectedInResponse) {
            assertTrue("The response did not contain: " + expectedResponse, content.contains(expectedResponse));
        }
    }

    /**
     * Test the tag library event listeners injected using CDI.
     * Method injection.
     *
     * @throws Exception
     */
    @Test
    public void testTagLibraryEventListenerMI() throws Exception {
        final int iterations = 5;

        DefaultHttpClient client = new DefaultHttpClient();
        DefaultHttpClient client2 = new DefaultHttpClient(); //Used to test @SessionScoped

        String url = JSPUtils.createHttpUrlString(server, TESTINJECTION_APP_NAME, "TagLibraryEventListenerMI.jsp?increment=true");
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);
        HttpResponse response;

        for (int i = 1; i <= iterations; i++) {
            String[] expectedInResponse = {
                                            "<li>TestMethodInjection index: " + i + "</li>",
                                            "<li>TestMethodInjectionRequest index: " + 1 + "</li>",
                                            "<li>TestMethodInjectionApplication index: " + i + "</li>",
                                            "<li>TestMethodInjectionSession index: " + i + "</li>"
            };

            response = client.execute(getMethod);
            assertEquals("Expected " + 200 + " status code was not returned!",
                         200, response.getStatusLine().getStatusCode());

            String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
            LOG.info("Response content: " + content);
            org.apache.http.util.EntityUtils.consume(response.getEntity());

            for (String expectedResponse : expectedInResponse) {
                assertTrue("The response did not contain: " + expectedResponse, content.contains(expectedResponse));
            }
        }

        //Testing if a new TestMethodInjectionSession was injected when another session is used
        String[] expectedInResponse = {
                                        "<li>TestMethodInjection index: " + (iterations + 1) + "</li>",
                                        "<li>TestMethodInjectionRequest index: " + 1 + "</li>",
                                        "<li>TestMethodInjectionApplication index: " + (iterations + 1) + "</li>",
                                        "<li>TestMethodInjectionSession index: " + 1 + "</li>"
        };

        response = client2.execute(getMethod);
        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getStatusLine().getStatusCode());

        String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
        LOG.info("Response content: " + content);
        org.apache.http.util.EntityUtils.consume(response.getEntity());

        for (String expectedResponse : expectedInResponse) {
            assertTrue("The response did not contain: " + expectedResponse, content.contains(expectedResponse));
        }
    }

    private void verifyStringsInResponse(String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(JSPUtils.createHttpUrlString(server, contextRoot, path));
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
