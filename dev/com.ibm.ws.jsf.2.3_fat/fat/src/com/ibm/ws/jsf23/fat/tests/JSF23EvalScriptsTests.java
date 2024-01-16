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

import org.junit.After;
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
 * JSF 2.3 test cases for the spec issue 1412
 * These test involve javascript calls which are added javax.faces.partialViewContext.getEvalScripts()
 */
@RunWith(FATRunner.class)
public class JSF23EvalScriptsTests {

    protected static final Class<?> c = JSF23EvalScriptsTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23EvalScriptsServer")
    public static LibertyServer server;

    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(FATSuite.getChromeImage()).withCapabilities(new ChromeOptions())
                    .withAccessToHost(true)
                    .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"

    private static ExtendedWebDriver driver;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "EvalScripts.war", "com.ibm.ws.jsf23.fat.evalscripts.beans");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server
        server.startServer(c.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());

        driver = new CustomDriver(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));
    }

    /*
     * Clear cookies for the selenium webdriver, so that session don't carry over between tests
     */
    @After
    public void clearCookies()
    {
        driver.getRemoteWebDriver().manage().deleteAllCookies();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
        driver.quit(); // closes all sessions and terminutes the webdriver
    }

    /**
     * This test case makes a simple method call to the bean via the button click.
     * The method will call getEvalScripts() and the rendered page will update one outputText component
     *
     * @throws Exception
     */
    @Test
    public void testEvalScriptsSimple() throws Exception {
        String contextRoot = "EvalScripts";
        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "EvalScriptsSimple.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        page.findElement(By.id("form1:button1")).click();
        page.waitForCondition(driver -> page.isInPage("Test Passed!"));

        Log.info(c, name.getMethodName(), page.getPageSource());

        //if the commandScript code works properly the success message will be displayed on the page.
        assertTrue("The commandScript test failed, success not displayed.", page.isInPage("Test Passed!"));
    }

    /**
     * This test case makes a simple method call to the bean via the button click.
     * The method will call getEvalScripts() and the rendered page will update three outputText components
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEvalScriptsList() throws Exception {
        String contextRoot = "EvalScripts";
        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "EvalScriptsList.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(c, name.getMethodName(), page.getPageSource());

        WebElement element =  page.findElement(By.id("form1:button1"));
        page.findElement(By.id("form1:button1")).click();

        page.waitForCondition(driver -> page.isInPageTextReduced("Text Value 1"));

        Log.info(c, name.getMethodName(), page.getPageSource());

        //if the commandScript code works properly the success message will be displayed on the page.
        assertTrue("The commandScript test failed, success not displayed.", page.isInPageTextReduced("Text Value 1,Text Value 2,Text Value 3"));
    }

    /**
     * This test case makes a simple method call to the bean via the button click.
     * The method will call getEvalScripts() and the will contain a javascript call to a function.
     * The javascript function should update an outputText component on the rendered page.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEvalScriptsFunction() throws Exception {
        String contextRoot = "EvalScripts";
        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "EvalScriptsFunction.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        page.findElement(By.id("form1:button1")).click();
        page.waitForCondition(driver -> page.isInPage("Function Called!"));

        Log.info(c, name.getMethodName(), page.getPageSource());

        //if the commandScript code works properly the success message will be displayed on the page.
        assertTrue("The commandScript test failed, success not displayed.", page.isInPage("Function Called!"));
    }

    /**
     * This test case makes a simple method call to the bean via the button click.
     * The method will call getEvalScripts() and the will contain a javascript call that will update the outputText1 field.
     * The test will also enter a value in a textInput will update the bean. The Ajax call will update the outputText4 field
     * with the value from the inputText.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEvalScriptsMultiField() throws Exception {
        String contextRoot = "EvalScripts";
        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "EvalScriptsMultiFieldUpdate.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        WebElement input = page.findElement(By.id("form1:inputText1"));
        input.sendKeys("test");

        Log.info(c, name.getMethodName(), page.getPageSource());

        page.findElement(By.id("form1:button1")).click();
        page.waitForCondition(driver -> page.isInPageTextReduced("Test Passed!,test"));

        Log.info(c, name.getMethodName(), page.getPageSource());

        //if the commandScript code works properly the success message will be displayed on the page.
        assertTrue("Test failed. The javascript code from getEvalScripts was not called.", page.isInPageTextReduced("Test Passed!,test"));
    }
}
