/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests.bugfixes;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;


import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.org.apache.myfaces40.fat.FATSuite;

import io.openliberty.org.apache.myfaces40.fat.JSFUtils;
import io.openliberty.org.apache.myfaces40.fat.selenium_util.CustomDriver;
import io.openliberty.org.apache.myfaces40.fat.selenium_util.ExtendedWebDriver;
import io.openliberty.org.apache.myfaces40.fat.selenium_util.WebPage;
/**
 * https://github.com/OpenLiberty/open-liberty/issues/28118
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MyFaces4658Test {

    protected static final Class<?> c = MyFaces4658Test.class;

    private static final String APP_NAME = "MYFACES-4658";

    @Server("faces40_myfaces4658")
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(FATSuite.getChromeImage()).withCapabilities(new ChromeOptions())
                    .withAccessToHost(true)
                    .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"
    
    private static ExtendedWebDriver driver;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war",
                                      "io.openliberty.org.apache.faces40.fat.myfaces4658");

        server.startServer(MyFaces4658Test.class.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());

        driver = new CustomDriver(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }

        driver.quit(); // closes all sessions and terminutes the webdriver
    }

    /*
     * https://issues.apache.org/jira/browse/MYFACES-4658 
     * 
     * Verifies the faces.util.chain exits when false is returned in any call (i.e confirm alert is cancelled)
     * 
     * Backing bean should only be invoked when confirm alert is accepted 
     */
    @Test
    public void testMyFaces4658() throws Exception {

        server.setMarkToEndOfLog();

        String url = JSFUtils.createSeleniumURLString(server, APP_NAME, "index.xhtml");;
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(c, name.getMethodName(), page.getPageSource());

        assertTrue(page.isInPage("MYFACES-4658")); // found in title

        page.findElement(By.id("form:button")).click();

        driver.switchTo().alert().dismiss();

        page.waitReqJs(); // wait for AJAX

        List<String> messages = server.findStringsInLogsUsingMark("CONFIRMED!!!!", server.getConsoleLogFile());

        Log.info(c, name.getMethodName(), messages.size() == 1 ? "Found message in log: " + messages.get(0).toString() : "messages list is empty");

        server.resetLogMarks();

        assertTrue("Message found in the log when it should not have been found!", messages.size() == 0);


    }
}
