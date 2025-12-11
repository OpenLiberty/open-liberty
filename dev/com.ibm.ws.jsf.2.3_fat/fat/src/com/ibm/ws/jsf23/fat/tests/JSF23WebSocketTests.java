/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.testcontainers.Testcontainers;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.FATSuite;
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.faces.fat.selenium.util.internal.ExtendedWebDriver;
import io.openliberty.faces.fat.selenium.util.internal.WebPage;

/**
 * This test class is to be used for the tests that test feature specified
 * in JSF 2.3 specification under the Section 10.4.1.7 “<f:websocket>”.
 */
@RunWith(FATRunner.class)
public class JSF23WebSocketTests {

    protected static final Class<?> c = JSF23WebSocketTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23WebSocketServer")
    public static LibertyServer server;

    private String contextRoot = "WebSocket";

    private static ExtendedWebDriver driver;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "WebSocket.war", "com.ibm.ws.jsf23.fat.websocket");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(c.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());

        driver = FATSuite.getWebDriver();
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

    /*
     * Clear cookies for the selenium webdriver, so that session don't carry over between tests
     */
    @After
    public void clearCookies()
    {
        driver.getRemoteWebDriver().manage().deleteAllCookies();
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
        String url;

        if (JakartaEEAction.isEE10OrLaterActive()) {
            url = JSFUtils.createSeleniumURLString(server, contextRoot, "faces40/PushWebSocketTest.jsf");
        } else {
            url = JSFUtils.createSeleniumURLString(server, contextRoot, "PushWebSocketTest.jsf");
        }

        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        // Log the page for debugging if necessary in the future.
        Log.info(c, name.getMethodName(), page.getPageSource());

        // Verify that the page contains the expected messages.
        assertTrue(page.isInPage("JSF 2.3 WebSocket - Test message pushed from server to client"));
        page.waitForCondition(driver -> page.isInPage("Called onopen listener"));

        // Verify that the correct message is found in the logs
        String result1 = server.waitForStringInLogUsingMark("Channel myChannel was opened successfully!");
        assertNotNull("Message not found. Channel was not opened succesfully.", result1);

        // Now click the button and get the resulted page.
        page.findElement(By.id("form1:sendButton")).click();
        page.waitForCondition(driver -> page.isInPage("Message from the server via push!")); // Wait for text to appear rather than some default time
        page.waitForCondition(driver -> page.isInPage("Called onclose listener"));

        // Log the page for debugging if necessary in the future.
        Log.info(c, name.getMethodName(), page.getPageSource());

        // Verify that the page contains the expected messages.
        assertTrue(page.isInPage("JSF 2.3 WebSocket - Test message pushed from server to client"));
        assertTrue(page.isInPage("Message from the server via push!"));
        assertTrue(page.isInPage("Called onclose listener"));

        // Verify that the correct message is found in the logs
        String result2 = server.waitForStringInLogUsingMark("Channel myChannel was closed successfully!");
        assertNotNull("Message not found. Channel was not closed succesfully.", result2);
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
        String url;

        if (JakartaEEAction.isEE10OrLaterActive()) {
            url = JSFUtils.createSeleniumURLString(server, contextRoot, "faces40/OpenCloseWebSocketTest.jsf");
        } else {
            url = JSFUtils.createSeleniumURLString(server, contextRoot, "OpenCloseWebSocketTest.jsf");
        }

        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        // Log the page for debugging if necessary in the future.
        Log.info(c, name.getMethodName(), page.getPageSource());

        // Verify that the page contains the expected messages.
        assertTrue(page.isInPage("JSF 2.3 WebSocket - Test that onopen and onclose listener can be triggered manually, that is, when connected attribute is set to false"));

        page.findElement(By.id("form1:openButton")).click();
        page.waitForCondition(driver -> page.isInPage("Called onopen listener"));
        assertTrue(page.isInPage("Called onopen listener"));

        String result1 = server.waitForStringInLogUsingMark("Channel myChannel was opened successfully!");
        // Verify that the correct message is found in the logs
        assertNotNull("Message not found. Channel was not opened succesfully.", result1);

        page.findElement(By.id("form1:closeButton")).click();
        page.waitForCondition(driver -> page.isInPage("Called onclose listener"));
        assertTrue(page.isInPage("Called onclose listener"));

        String result2 = server.waitForStringInLogUsingMark("Channel myChannel was closed successfully!");
        assertNotNull("Message not found. Channel was not closed succesfully.", result2);

    }

    private void assertContains(String str, String lookFor) {
        Log.info(c, name.getMethodName(), "Looking for '" + lookFor + "' in string: " + str);
        if (str == null || !str.contains(lookFor))
            fail("Expected to find '" + lookFor + "' in response, but response was: " + str);
    }
}
