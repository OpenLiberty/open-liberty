/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;

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

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.FATSuite;
import com.ibm.ws.jsf22.fat.JSFUtils;
import com.ibm.ws.jsf22.fat.selenium_util.CustomDriver;
import com.ibm.ws.jsf22.fat.selenium_util.ExtendedWebDriver;
import com.ibm.ws.jsf22.fat.selenium_util.WebPage;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsf22TracingServer that use HtmlUnit. jsf22TracingServer
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22ResetValuesAndAjaxDelayTests {
    private static final String APP_NAME = "TestJSF22Ajax";
    public TestName name = new TestName();

    @Server("jsf22TracingServer")
    public static LibertyServer jsf22TracingServer;

    protected static final Class<?> c = JSF22ResetValuesAndAjaxDelayTests.class;

    private static BrowserVersion browser = BrowserVersion.CHROME;

    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(FATSuite.getChromeImage()).withCapabilities(new ChromeOptions())
                    .withAccessToHost(true)
                    .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"

   private static ExtendedWebDriver driver;

    @BeforeClass
    public static void setup() throws Exception {
        boolean isEE10 = JakartaEEAction.isEE10OrLaterActive();

        ShrinkHelper.defaultDropinApp(jsf22TracingServer, APP_NAME + ".war",
                                      isEE10 ? "com.ibm.ws.jsf22.fat.ajax.ajaxDelay.faces40" : "com.ibm.ws.jsf22.fat.ajax.ajaxDelay.jsf22",
                                      isEE10 ? "com.ibm.ws.jsf22.fat.ajax.resetValue.faces40" : "com.ibm.ws.jsf22.fat.ajax.resetValue.jsf22");

        jsf22TracingServer.startServer(c.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(jsf22TracingServer.getHttpDefaultPort(), jsf22TracingServer.getHttpDefaultSecurePort());

        driver = new CustomDriver(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf22TracingServer != null && jsf22TracingServer.isStarted()) {
            jsf22TracingServer.stopServer();
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
     * Test the ajax resetValues attribute and also the f:resetValues component
     *
     * @throws Exception
     */
    @Test
    public void testResetValues() throws Exception {
        String url = JSFUtils.createSeleniumURLString(jsf22TracingServer, APP_NAME, "resetValuesTest.jsf");
        WebPage page = new WebPage(driver);
        Log.info(c, name.getMethodName(), "Navigating to: /" + APP_NAME + "/resetValuesTest.jsf");
        page.get(url);
        page.waitForPageToLoad();

        page.findElement(By.id("form1:link1")).click();
        page.waitReqJs();

        String input1Value = page.findElement(By.id("form1:input1")).getAttribute("value");
        Log.info(c, name.getMethodName(), "The input1 field should have a value of 1, actual: " + input1Value);
        assertEquals("1", input1Value);

        page.findElement(By.id("form1:saveButton")).submit();
        page.waitForCondition(webDriver -> page.isInPage("Validation Error: Value is less than allowable minimum"));

        String message = page.findElement(By.id("form1:messages")).getText();
        Log.info(c, name.getMethodName(), "On save, the validation should have failed.  Message displayed: " + message);
        assertNotNull("A validation error should have been displayed", message);

        // click the link again, the value should still increment which means the Ajax reset is working
        page.findElement(By.id("form1:link1")).click();
        page.waitReqJs();

        input1Value = page.findElement(By.id("form1:input1")).getAttribute("value");
        Log.info(c, name.getMethodName(), "The input1 field should have a value of 2, actual: " + input1Value);
        assertEquals("2", input1Value);

        // click the resetButton and ensure the fields are reset to 0 each, which means the f:resetValues component is working.
        page.findElement(By.id("form1:resetButton")).click();
        page.waitReqJs();

        input1Value = page.findElement(By.id("form1:input1")).getAttribute("value");
        Log.info(c, name.getMethodName(), "The input1 field should have been reset to 0, actual: " + input1Value);
        assertEquals("0", input1Value);

        String input2Value = page.findElement(By.id("form1:input2")).getAttribute("value");
        Log.info(c, name.getMethodName(), "The input2 field should have been reset to 0, actual: " + input2Value);
        assertEquals("0", input2Value);
    }

    /**
     * Test an Ajax request with a delay of 200 and make sure the method is only called once in that time.
     *
     * @throws Exception
     */
    @Test
    public void testAjaxDelay() throws Exception {
        try (WebClient webClient = new WebClient(browser)) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            jsf22TracingServer.setMarkToEndOfLog();

            Log.info(c, name.getMethodName(), "Navigating to: /" + APP_NAME + "/ajaxDelayTest.jsf");
            URL url = JSFUtils.createHttpUrl(jsf22TracingServer, APP_NAME, "ajaxDelayTest.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Returned from navigating to: /" + APP_NAME + "/ajaxDelayTest.jsf and setting mark in logs.");

            Log.info(c, name.getMethodName(), "Sleeping for 1 second");
            Thread.sleep(1000);

            Log.info(c, name.getMethodName(), "Getting input field");
            HtmlTextInput input = (HtmlTextInput) page.getElementById("form1:name");

            Log.info(c, name.getMethodName(), "Typing value 'joh'");
            input.type("joh");

            Log.info(c, name.getMethodName(), "Checking logs for bean call entry");
            int numOfMethodCalls = jsf22TracingServer.waitForMultipleStringsInLogUsingMark(1, "AjaxDelayTest getMatchingEmployees");

            Log.info(c, name.getMethodName(), "The bean method should have been called once, actual amount is: " + numOfMethodCalls);
            assertEquals(1, numOfMethodCalls);
        }
    }

    /**
     * Test an Ajax request with a delay of zero (0) and make sure the method is called on each keyup (3 times).
     *
     * @throws Exception
     * 
     * Updated to use Selenium
     */
    @Test
    public void testAjaxZeroDelay() throws Exception {
            String url = JSFUtils.createSeleniumURLString(jsf22TracingServer, APP_NAME, "ajaxZeroDelayTest.jsf");
            WebPage page = new WebPage(driver);

            jsf22TracingServer.setMarkToEndOfLog();

            page.get(url);
            page.waitForPageToLoad();

            Log.info(c, name.getMethodName(), "Returned from navigating to: /" + APP_NAME + "/ajaxZeroDelayTest.jsf and setting mark in logs.");

            Log.info(c, name.getMethodName(), "Getting input field");
            WebElement input = page.findElement(By.id("form1:name"));

            Log.info(c, name.getMethodName(), "Typing value 'joh'");
            // typing each character at a time to trigger the keyup event each time
            input.sendKeys("j");
            input.sendKeys("o");
            input.sendKeys("h");

            page.waitForCondition(webDriver -> page.isInPage("john doe"));  // Wait for text to appear rather than some default time 

            Log.info(c, name.getMethodName(), "Checking logs for bean call entry");
            int numOfMethodCalls = jsf22TracingServer.waitForMultipleStringsInLogUsingMark(4, "AjaxDelayTest getMatchingEmployees");

            Log.info(c, name.getMethodName(), "The bean method should have been called four times, actual amount is: " + numOfMethodCalls);
            assertEquals(4, numOfMethodCalls);
    }

    /**
     * Test an Ajax request with a delay of none and make sure the method is called on each keyup (3 times).
     * Previously disabled on HTMLUnit, but updated to run on Selenium 
     * @throws Exception
     */
      @Test
      public void testAjaxDelayNone() throws Exception {
            String url = JSFUtils.createSeleniumURLString(jsf22TracingServer, APP_NAME, "ajaxDelayTestNone.jsf");
            WebPage page = new WebPage(driver);

            jsf22TracingServer.setMarkToEndOfLog();

            page.get(url);
            page.waitForPageToLoad();

            Log.info(c, name.getMethodName(), "Returned from navigating to: /" + APP_NAME + "/ajaxDelayTestNone.jsf and setting mark in logs.");

            Log.info(c, name.getMethodName(), "Getting input field");
            WebElement input = page.findElement(By.id("form1:name"));

            Log.info(c, name.getMethodName(), "Typing value 'joh'");
            // typing each character at a time to trigger the keyup event each time
            input.sendKeys("j");
            input.sendKeys("o");
            input.sendKeys("h");

            page.waitForCondition(webDriver -> page.isInPage("john doe"));  // Wait for text to appear rather than some default time 

            Log.info(c, name.getMethodName(), "Checking logs for bean call entry");
            int numOfMethodCalls = jsf22TracingServer.waitForMultipleStringsInLogUsingMark(3, "AjaxDelayTest getMatchingEmployees");

            Log.info(c, name.getMethodName(), "The bean method should have been called three times, actual amount is: " + numOfMethodCalls);
            assertEquals(3, numOfMethodCalls);
     }

}
