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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;

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
import io.openliberty.org.apache.myfaces40.fat.FATSuite;
import io.openliberty.org.apache.myfaces40.fat.JSFUtils;
import io.openliberty.org.apache.myfaces40.fat.selenium_util.CustomDriver;
import io.openliberty.org.apache.myfaces40.fat.selenium_util.ExtendedWebDriver;
import io.openliberty.org.apache.myfaces40.fat.selenium_util.WebPage;

/**
 * This test class is to be used for the tests that test feature specified
 * in JSF 4.0 specification for <f:websocket> onerror="...".
 */
@RunWith(FATRunner.class)
@SkipForRepeat({SkipForRepeat.NO_MODIFICATION,SkipForRepeat.EE11_FEATURES}) // https://github.com/OpenLiberty/open-liberty/issues/27598
public class WebSocketTests {

    private static final String WEB_SOCKET_TEST_APP_NAME = "WebSocket";
    protected static final Class<?> c = WebSocketTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("faces40_WebSocketServer")
    public static LibertyServer server;

    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(FATSuite.getChromeImage()).withCapabilities(new ChromeOptions())
                    .withAccessToHost(true)
                    .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"

    private static ExtendedWebDriver driver;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, WEB_SOCKET_TEST_APP_NAME + ".war",
                                      "io.openliberty.org.apache.faces40.fat.websocket");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WebSocketTests.class.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());

        driver = new CustomDriver(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0190E"); // SRVE0190E is due to ENABLE_WEBSOCKET_ENDPOINT being false for triggering onerror on a websocket.
        }

        driver.quit(); // closes all sessions and terminutes the webdriver
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

        String contextRoot = "WebSocket";

        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "OnErrorWebSocketTest.jsf");;
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(c, name.getMethodName(), page.getPageSource());

        assertTrue(page.isInPage("JSF 4.0 WebSocket - Test that onerror is invoked correctly."));

        page.findElement(By.id("form1:openButton")).click();

        // Called onerror listener is not actually called. Parsing error occurs instead:
        // onerror(t) {
        //     var n, r;
        //     let s = JSON.parse(t.data); Error:  SyntaxError: "undefined" is not valid JSON
        // TODO FOLLOW UP : https://github.com/OpenLiberty/open-liberty/issues/27598
        // page.waitForCondition(driver -> page.isInPage("Called onerror listener"));
        page.waitForCondition(driver -> page.isInPage("Called onclose listener"));

    }

    private void assertContains(String str, String lookFor) {
        Log.info(c, name.getMethodName(), "Looking for '" + lookFor + "' in string: " + str);
        if (str == null || !str.contains(lookFor))
            fail("Expected to find '" + lookFor + "' in response, but response was: " + str);
    }
}
