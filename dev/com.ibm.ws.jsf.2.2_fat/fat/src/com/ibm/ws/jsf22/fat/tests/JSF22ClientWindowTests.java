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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.After;
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

import com.ibm.websphere.simplicity.ShrinkHelper;
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
 * Tests to execute on the jsfTestServer2 that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22ClientWindowTests {
    @Rule
    public TestName name = new TestName();

    private static final String APP_NAME = "JSF22ClientWindow";
    private static final String APP_NAME_FACES40 = "JSF22ClientWindowFaces40";
    private static boolean isEE10;

    protected static final Class<?> c = JSF22ClientWindowTests.class;

    @Server("jsfTestServer2")
    public static LibertyServer jsfTestServer2;

    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(FATSuite.getChromeImage()).withCapabilities(new ChromeOptions())
                    .withAccessToHost(true)
                    .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"


    private static ExtendedWebDriver driver;

    @BeforeClass
    public static void setup() throws Exception {
        isEE10 = JakartaEEAction.isEE10OrLaterActive();

        if (isEE10) {
            ShrinkHelper.defaultDropinApp(jsfTestServer2, APP_NAME_FACES40 + ".war",
                                          "com.ibm.ws.jsf22.fat.clientwindow.faces40");
        } else {
            ShrinkHelper.defaultDropinApp(jsfTestServer2, APP_NAME + ".war",
                                          "com.ibm.ws.jsf22.fat.clientwindow.jsf22");
        }

        jsfTestServer2.startServer(c.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(jsfTestServer2.getHttpDefaultPort(), jsfTestServer2.getHttpDefaultSecurePort());

        driver = new CustomDriver(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer2 != null && jsfTestServer2.isStarted()) {
            jsfTestServer2.stopServer();
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
     * Check the ClientWindow ID retrieved from javascript when the page loads.
     * Then click a link and check that the client window id is set in the request parameter.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestSimpleLink() throws Exception {
        String contextRoot = isEE10 ? APP_NAME_FACES40 : APP_NAME;

        String url = JSFUtils.createSeleniumURLString(jsfTestServer2, contextRoot, "index.jsf");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        String clientWindowJS = page.findElement(By.id("clientWindowDisplay")).getText();

        page.findElement(By.id("testForm:link1")).click();
        // Look for the correct results
        assertTrue(page.isInPageTextReduced("Window ID from parameter: " + clientWindowJS));

        // check that the client window ids match
        String outputText = page.findElement(By.id("testFormPage2:windowIdParam")).getText();
        assertTrue(clientWindowJS.equals(outputText));
    }

    /**
     * Check the ClientWindow ID retrieved from javascript when the page loads.
     * Then click a link, which will open a new window, and check that the client window id is set in the request parameter.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestSimpleLinkNewWindow() throws Exception {
        String contextRoot = isEE10 ? APP_NAME_FACES40 : APP_NAME;

        String url = JSFUtils.createSeleniumURLString(jsfTestServer2, contextRoot, "index.jsf");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        String clientWindowJS = page.findElement(By.id("clientWindowDisplay")).getText();

        String currentHandle = driver.getWindowHandle();

        page.findElement(By.id("testForm:link2")).click(); // opens new tab
        ArrayList<String> wid = new ArrayList<String>(driver.getWindowHandles());
        // switch to the new tab
        driver.close(); // close current window
        wid.remove(currentHandle);
        driver.switchTo().window(wid.get(0));
        // Look for the correct results
        assertTrue(page.isInPageTextReduced("Window ID from parameter: " + clientWindowJS));

        // check that the client window ids match
        String outputText = page.findElement(By.id("testFormPage2:windowIdParam")).getText();
        assertTrue(clientWindowJS.equals(outputText));
    }

    /**
     * Click a link that has the disableClientWindow attribute set to true
     * and check that the client window id is not set in the request parameter.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestDisabledLink() throws Exception {
        String contextRoot = isEE10 ? APP_NAME_FACES40 : APP_NAME;

        String url = JSFUtils.createSeleniumURLString(jsfTestServer2, contextRoot, "index.jsf");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();
        String currentHandle = driver.getWindowHandle();

        page.findElement(By.id("testForm:linkDisabled1")).click();
        ArrayList<String> wid = new ArrayList<String>(driver.getWindowHandles());
        // switch to the new tab
        driver.close(); // close current window
        wid.remove(currentHandle);
        driver.switchTo().window(wid.get(0));

        String outputText = page.findElement(By.id("form2Disabled:outputPassed")).getText();
        assertTrue("Test Passed".equals(outputText));
    }

    /**
     * Fill out two fields and click an Ajax command button.
     * Once the target panel and fields were updated (in the same page), check that the values are correct.
     * Also get the clientWindow id from the bean, which uses the ExternalContext to retrieve it,
     * then compare it to the one set on the original page.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestAjax() throws Exception {
        String contextRoot = isEE10 ? APP_NAME_FACES40 : APP_NAME;

        String url = JSFUtils.createSeleniumURLString(jsfTestServer2, contextRoot, "index.jsf");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        String clientWindowJS = page.findElement(By.id("clientWindowDisplay")).getText();

        // fill out fields
        page.findElement(By.id("testForm:firstName")).sendKeys("John");
        page.findElement(By.id("testForm:lastName")).sendKeys("Doe");

        // Click link to execute the methods and update the page
        page.findElement(By.id("testForm:buttonAjax1")).click();
        page.waitReqJs();

        String firstName = page.findElement(By.id("testForm:ajaxFirstName")).getText();
        String lastName = page.findElement(By.id("testForm:ajaxLastName")).getText();
        String ajaxWindowId = page.findElement(By.id("testForm:ajaxWindowId")).getText();

        // Look for the correct results
        assertTrue(firstName.equals("John"));
        assertTrue(lastName.equals("Doe"));
        assertTrue(ajaxWindowId.equals(clientWindowJS));

    }

    /**
     * Fill out two fields and click a command button.
     * On the resulting page (page2.xhtml), check that the bean was updated.
     * Also get the clientWindow id from the bean, which uses the ExternalContext to retrieve it,
     * then compare it to the one set on the original page.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestCommandButton() throws Exception {
        String contextRoot = isEE10 ? APP_NAME_FACES40 : APP_NAME;

        String url = JSFUtils.createSeleniumURLString(jsfTestServer2, contextRoot, "index.jsf");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        String clientWindowJS = page.findElement(By.id("clientWindowDisplay")).getText();

        // fill out fields
        page.findElement(By.id("testForm:firstName")).sendKeys("Bill");
        page.findElement(By.id("testForm:lastName")).sendKeys("Smith");

        // Click link to execute the methods and update the page
        page.findElement(By.id("testForm:submitCommandButton1")).click();
        page.waitReqJs();

        String firstName = page.findElement(By.id("testFormPage2:firstName")).getText();
        String lastName = page.findElement(By.id("testFormPage2:lastName")).getText();
        String windowIdBean = page.findElement(By.id("testFormPage2:windowIdBean")).getText();

        // Look for the correct results
        assertTrue(firstName.equals("Bill"));
        assertTrue(lastName.equals("Smith"));
        assertTrue(windowIdBean.equals(clientWindowJS));
    }

    /**
     * Fill out two fields and click a command link.
     * On the resulting page (page2.xhtml), check that the bean was updated.
     * Also get the clientWindow id from the bean, which uses the ExternalContext to retrieve it,
     * then compare it to the one set on the original page.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestCommandLink() throws Exception {
        String contextRoot = isEE10 ? APP_NAME_FACES40 : APP_NAME;

        String url = JSFUtils.createSeleniumURLString(jsfTestServer2, contextRoot, "index.jsf");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        String clientWindowJS = page.findElement(By.id("clientWindowDisplay")).getText();

        // fill out fields
        page.findElement(By.id("testForm:firstName")).sendKeys("Jane");
        page.findElement(By.id("testForm:lastName")).sendKeys("Jones");

        // Click link to execute the methods and update the page
        page.findElement(By.id("testForm:commandLink1")).click();
        page.waitReqJs();

        String firstName = page.findElement(By.id("testFormPage2:firstName")).getText();
        String lastName = page.findElement(By.id("testFormPage2:lastName")).getText();
        String windowIdBean = page.findElement(By.id("testFormPage2:windowIdBean")).getText();

        // Look for the correct results
        assertTrue(firstName.equals("Jane"));
        assertTrue(lastName.equals("Jones"));
        assertTrue(windowIdBean.equals(clientWindowJS));
    }

    /**
     * Fill out two fields and click a button.
     * On the resulting page (page2.xhtml), check that the bean was updated.
     * Also get the clientWindow id from the bean, which uses the ExternalContext to retrieve it,
     * then compare it to the one set on the original page.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestButton() throws Exception {
        String contextRoot = isEE10 ? APP_NAME_FACES40 : APP_NAME;

        String url = JSFUtils.createSeleniumURLString(jsfTestServer2, contextRoot, "index.jsf");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        String clientWindowJS = page.findElement(By.id("clientWindowDisplay")).getText();

        page.findElement(By.id("testForm:button1")).click();
        page.waitForPageToLoad();

        String windowIdParam = page.findElement(By.id("testFormPage2:windowIdParam")).getText();

        String windowIdBean = page.findElement(By.id("testFormPage2:windowIdBean")).getText();

        assertTrue(windowIdBean.equals(clientWindowJS));
        assertTrue(windowIdBean.equals(windowIdParam));

    }

    /**
     * Fill out two fields and click a button with the disableClientWindow set to true.
     * On the resulting page (page2.xhtml), check that the bean was updated.
     * Also get the clientWindow id from the bean, which uses the ExternalContext to retrieve it,
     * then compare it to the one set on the original page.
     * Most importantly, make sure the client window attribute (jfwid) was not set on the request.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestButtonDisabled() throws Exception {
        String contextRoot = isEE10 ? APP_NAME_FACES40 : APP_NAME;

        String url = JSFUtils.createSeleniumURLString(jsfTestServer2, contextRoot, "index.jsf");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        String clientWindowJS = page.findElement(By.id("clientWindowDisplay")).getText();

        page.findElement(By.id("testForm:button2Disabled")).click();
        // Look for the "Test Passed". The page2Disabled.xhtml page has the logic to compare the IDs.
        assertTrue(page.isInPageTextReduced("Test Passed"));

        // We still should be able to get the id from the ExternalContext (in the bean), check to make sure that it isn't null
        String windowIdBean = page.findElement(By.id("form2Disabled:outputWindowIdBean")).getText();

        assertTrue(windowIdBean != null && windowIdBean != "");
    }

    /**
     * Load two base level pages, then click links in each one.
     * Ensure that the client window ids do not match.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestMultipleBasePages() throws Exception {
        String contextRoot = isEE10 ? APP_NAME_FACES40 : APP_NAME;

        String url = JSFUtils.createSeleniumURLString(jsfTestServer2, contextRoot, "index.jsf");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        page.findElement(By.id("testForm:link1")).click();
        page.waitForPageToLoad();

        String windowIdParam = page.findElement(By.id("testFormPage2:windowIdParam")).getText();

        String url2 = JSFUtils.createSeleniumURLString(jsfTestServer2, contextRoot, "index2.jsf");
        WebPage page2 = new WebPage(driver);
        page2.get(url2);
        page2.waitForPageToLoad();

        page2.findElement(By.id("testForm:link1")).click();
        page.waitForPageToLoad();

        String windowIdParam2 = page.findElement(By.id("testFormPage2:windowIdParam")).getText();
        // check that the client window ids do not match
        assertFalse(windowIdParam.equals(windowIdParam2));
    }
}
