/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.topology.impl.LibertyServer;

/**
 * This test class is to be used for the tests that test feature specified
 * in JSF 2.3 specification under the Section 10.4.1.7 “<f:websocket>”.
 */
@RunWith(FATRunner.class)
@SkipForRepeat(EE10_FEATURES)
public class JSF23WebSocketTests {

    protected static final Class<?> c = JSF23WebSocketTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23WebSocketServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "WebSocket.war", "com.ibm.ws.jsf23.fat.websocket");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(JSF23WebSocketTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Before
    public void setupPerTest() throws Exception {
        server.setMarkToEndOfLog();
    }

    /**
     * Test to ensure that the <f:websocket> component actually works properly.
     * The test will ensure that a message is pushed from server to client.
     * Tests that onopen listener is triggered automatically.
     * Tests that onclose listener is triggered when push connection is closed.
     *
     * @throws Exception
     */
    @Test
    public void testPushWebsocket() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            // Construct the URL for the test
            String contextRoot = "WebSocket";
            URL url;

            if (JakartaEE10Action.isActive()) {
                url = JSFUtils.createHttpUrl(server, contextRoot, "faces40/PushWebSocketTest.jsf");
            } else {
                url = JSFUtils.createHttpUrl(server, contextRoot, "PushWebSocketTest.jsf");
            }

            HtmlPage testPushWebSocketPage = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testPushWebSocketPage.asText());
            Log.info(c, name.getMethodName(), testPushWebSocketPage.asXml());

            // Verify that the page contains the expected messages.
            assertContains(testPushWebSocketPage.asText(), "JSF 2.3 WebSocket - Test message pushed from server to client");
            assertContains(testPushWebSocketPage.asText(), "Called onopen listener");

            String result1 = server.waitForStringInLogUsingMark("Channel myChannel was opened successfully!");

            // Verify that the correct message is found in the logs
            assertNotNull("Message not found. Channel was not opened succesfully.", result1);

            // Get the form that we are dealing with
            HtmlForm form = testPushWebSocketPage.getFormByName("form1");

            // Get the button to click
            HtmlSubmitInput sendButton = form.getInputByName("form1:sendButton");

            // Now click the button and get the resulted page.
            HtmlPage resultPage = sendButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultPage.asText());
            Log.info(c, name.getMethodName(), resultPage.asXml());

            // Verify that the page contains the expected messages.
            assertContains(resultPage.asText(), "JSF 2.3 WebSocket - Test message pushed from server to client");
            assertTrue(JSFUtils.waitForPageResponse(resultPage, "Message from the server via push!"));
            assertTrue(JSFUtils.waitForPageResponse(resultPage, "Called onclose listener"));

            String result2 = server.waitForStringInLogUsingMark("Channel myChannel was closed successfully!");

            // Verify that the correct message is found in the logs
            assertNotNull("Message not found. Channel was not closed succesfully.", result2);
        }
    }

    /**
     * Test to ensure that the <f:websocket> component actually works properly.
     * The test will ensure push connection is opened and closed on client manually.
     * Both onopen and onclose listeners should be triggered.
     *
     * @throws Exception
     */
    @Test
    public void testOpenAndCloseWebsocket() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            // Construct the URL for the test
            String contextRoot = "WebSocket";
            URL url;

            if (JakartaEE10Action.isActive()) {
                url = JSFUtils.createHttpUrl(server, contextRoot, "faces40/OpenCloseWebSocketTest.jsf");
            } else {
                url = JSFUtils.createHttpUrl(server, contextRoot, "OpenCloseWebSocketTest.jsf");
            }

            HtmlPage testOpenCloseWebSocketPage = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testOpenCloseWebSocketPage.asText());
            Log.info(c, name.getMethodName(), testOpenCloseWebSocketPage.asXml());

            // Verify that the page contains the expected messages.
            assertContains(testOpenCloseWebSocketPage.asText(),
                           "JSF 2.3 WebSocket - Test that onopen and onclose listener can be triggered manually, that is, when connected attribute is set to false");

            // Get the form that we are dealing with
            HtmlForm form = testOpenCloseWebSocketPage.getFormByName("form1");

            // Get the buttons that open and close the push connection
            HtmlSubmitInput openButton = form.getInputByName("form1:openButton");
            HtmlSubmitInput closeButton = form.getInputByName("form1:closeButton");

            // Now click the open button and get the resulted page.
            HtmlPage openPage = openButton.click();

            // Use JSFUtils as this fails intermittently waiting for background JavaScript.
            assertTrue(JSFUtils.waitForPageResponse(openPage, "Called onopen listener"));

            String result1 = server.waitForStringInLogUsingMark("Channel myChannel was opened successfully!");

            // Verify that the correct message is found in the logs
            assertNotNull("Message not found. Channel was not opened succesfully.", result1);

            // Now click the close button and get the resulted page.
            HtmlPage closePage = closeButton.click();

            // Use JSFUtils as this fails intermittently waiting for background JavaScript.
            assertTrue(JSFUtils.waitForPageResponse(closePage, "Called onclose listener"));

            String result2 = server.waitForStringInLogUsingMark("Channel myChannel was closed successfully!");

            // Verify that the correct message is found in the logs
            assertNotNull("Message not found. Channel was not closed succesfully.", result2);
        }
    }

    private void assertContains(String str, String lookFor) {
        Log.info(c, name.getMethodName(), "Looking for '" + lookFor + "' in string: " + str);
        if (str == null || !str.contains(lookFor))
            fail("Expected to find '" + lookFor + "' in response, but response was: " + str);
    }
}
