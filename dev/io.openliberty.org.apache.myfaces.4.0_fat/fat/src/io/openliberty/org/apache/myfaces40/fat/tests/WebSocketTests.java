/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces40.fat.JSFUtils;

/**
 * This test class is to be used for the tests that test feature specified
 * in JSF 4.0 specification for <f:websocket> onerror="...".
 */
@RunWith(FATRunner.class)
@SkipForRepeat({SkipForRepeat.NO_MODIFICATION,SkipForRepeat.EE11_FEATURES}) // Skipped due to HTMLUnit / JavaScript incompatibility (New JS in RC5)
public class WebSocketTests {

    private static final String WEB_SOCKET_TEST_APP_NAME = "WebSocket";
    protected static final Class<?> c = WebSocketTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("faces40_WebSocketServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, WEB_SOCKET_TEST_APP_NAME + ".war",
                                      "io.openliberty.org.apache.faces40.fat.websocket");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WebSocketTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0190E"); // SRVE0190E is due to ENABLE_WEBSOCKET_ENDPOINT being false for triggering onerror on a websocket.
        }
    }

    @Before
    public void setupPerTest() throws Exception {
        server.setMarkToEndOfLog();
    }

    /**
     * Test to ensure that the <f:websocket> onerror listener works properly.
     *
     * @throws Exception
     */
    @Test
    public void testOnErrorWebsocket() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            // Construct the URL for the test
            String contextRoot = "WebSocket";
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "OnErrorWebSocketTest.jsf");

            HtmlPage testOnErrorWebSocketPage = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testOnErrorWebSocketPage.asText());
            Log.info(c, name.getMethodName(), testOnErrorWebSocketPage.asXml());

            // Verify that the page contains the expected messages.
            assertContains(testOnErrorWebSocketPage.asText(),
                           "JSF 4.0 WebSocket - Test that onerror is invoked correctly.");

            // Get the form that we are dealing with
            HtmlForm form = testOnErrorWebSocketPage.getFormByName("form1");

            // Get the button that opens the push connection
            HtmlSubmitInput openButton = form.getInputByName("form1:openButton");

            // Now click the open button. This should result in an error since 'jakarta.faces.ENABLE_WEBSOCKET_ENDPOINT' is set to 'false'
            HtmlPage openPage = openButton.click();

            assertTrue(JSFUtils.waitForPageResponse(openPage, "Called onerror listener"));
            assertTrue(JSFUtils.waitForPageResponse(openPage, "Called onclose listener"));
        }
    }

    private void assertContains(String str, String lookFor) {
        Log.info(c, name.getMethodName(), "Looking for '" + lookFor + "' in string: " + str);
        if (str == null || !str.contains(lookFor))
            fail("Expected to find '" + lookFor + "' in response, but response was: " + str);
    }
}
