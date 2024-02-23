/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.After;
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

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.FATSuite;
import com.ibm.ws.jsf23.fat.JSFUtils;
import com.ibm.ws.jsf23.fat.selenium_util.CustomDriver;
import com.ibm.ws.jsf23.fat.selenium_util.ExtendedWebDriver;
import com.ibm.ws.jsf23.fat.selenium_util.WebPage;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * JSF 2.3 test cases for the tag h:commandScript.
 */
@RunWith(FATRunner.class)
public class JSF23CommandScriptTests {

    protected static final Class<?> c = JSF23CommandScriptTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23CommandScriptServer")
    public static LibertyServer server;

    private String contextRoot = "CommandScript";

    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(FATSuite.getChromeImage()).withCapabilities(new ChromeOptions())
                    .withAccessToHost(true)
                    .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"

    private static ExtendedWebDriver driver;


    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "CommandScript.war",
                                      "com.ibm.ws.jsf23.fat.commandscript.beans",
                                      "com.ibm.ws.jsf23.fat.commandscript.listener");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server
        server.startServer(c.getSimpleName() + ".log");
        Testcontainers.exposeHostPorts(server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());

        driver = new CustomDriver(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
        driver.quit(); // closes all sessions and terminutes the webdriver
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
     * This test case ensures that the commandScript is called when the page loads because of the autorun attribute.
     * This test also verifies that a default value for the execute attribute will be used since the attribute is not specified.
     * If successful, the bean will be called and a success message will be displayed.
     *
     * @throws Exception
     */
    @Test
    public void testCommandScriptAutorunDefaultExecute() throws Exception {

        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "JSF23CommandScriptAutorunDefaultExecute.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(c, name.getMethodName(), page.getPageSource());

        //if the commandScript code works properly the success message will be displayed on the page.
        assertTrue("The commandScript test failed, success not displayed.", page.isInPage("The value of output is: success"));

    }

    /**
     * This test case ensures that the commandScript is called when the page loads because of the autorun attribute.
     * If successful, the bean will be called and a success message will be displayed.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCommandScriptAutorun() throws Exception {

        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "JSF23CommandScriptAutorun.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(c, name.getMethodName(), page.getPageSource());

        //if the commandScript code works properly the success message will be displayed on the page.
        assertTrue("The commandScript test failed, success not displayed.", page.isInPage("The value of output is: success"));
    }

    /**
     * This test case ensures that the commandScript is called when the page loads because of the autorun attribute.
     * Also, the f:actionListener is nested and the test verifies that the listener is called.
     * If successful, the bean will be called and a success message will be displayed.
     *
     * @throws Exception
     */
    @Test
    public void testCommandScriptActionListener() throws Exception {

        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "JSF23CommandScriptActionListener.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(c, name.getMethodName(), page.getPageSource());

        //if the commandScript code works properly the success message will be displayed on the page.
        assertTrue("The commandScript test failed, success not displayed.", page.isInPage("The value of output is: success"));

        //verify that the message from the listener is in the log file.
        List<String> result = server.findStringsInLogs("CommandScriptActionListener.processAction called");
        assertTrue("The ActionListener was not called.", result.size() == 1);
    }

    /**
     * This test case ensures that the commandScript is called when the page loads because of the autorun attribute.
     * Also, the actionListener is added to the commandScript tag and the test verifies that the method is called.
     * If successful, the bean will be called and a success message will be displayed.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCommandScriptActionListenerAttr() throws Exception {

        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "JSF23CommandScriptActionListenerAttr.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(c, name.getMethodName(), page.getPageSource());

        //if the commandScript code works properly the success message will be displayed on the page.
        assertTrue("The commandScript test failed, success not displayed.", page.isInPage("The value of output is: success"));

        //verify that the message from the listener is in the log file.
        List<String> result = server.findStringsInLogs("performAction called");
        assertTrue("The ActionListener was not called.", result.size() == 1);
    }

    /**
     * This test case ensures that the commandScript is called when the page loads because of the autorun attribute.
     * Also, there is a nested f:param component. The test will ensure the parameters are passed to the bean.
     * If successful, the bean will be called and param values will be displayed.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCommandScriptParam() throws Exception {

        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "JSF23CommandScriptParam.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(c, name.getMethodName(), page.getPageSource());

        //if the commandScript code works properly the success message will be displayed on the page.
        assertTrue("The commandScript test failed, success not displayed.", page.isInPage("The value of output is: Value1 Value2"));
    }

    /**
     * This test case ensures that the commandScript is called when the button is clicked.
     * If successful, the bean will add a message that will be displayed on the page.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCommandScriptButton() throws Exception {

        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "JSF23CommandScriptButton.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        page.findElement(By.id("button1")).click();
        page.waitReqJs();
        Log.info(c, name.getMethodName(), page.getPageSource());

        //if the commandScript code works properly the success message will be displayed on the page.
        assertTrue("The commandScript test failed, success not displayed.", page.isInPage("submitForm called"));
    }
}
