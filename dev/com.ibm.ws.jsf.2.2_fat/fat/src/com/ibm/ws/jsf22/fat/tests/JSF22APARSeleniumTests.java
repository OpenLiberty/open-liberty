/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.tests;

import static org.junit.Assert.assertEquals;
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
import com.ibm.ws.jsf22.fat.FATSuite;
import com.ibm.ws.jsf22.fat.JSFUtils;
import com.ibm.ws.jsf22.fat.selenium_util.CustomDriver;
import com.ibm.ws.jsf22.fat.selenium_util.ExtendedWebDriver;
import com.ibm.ws.jsf22.fat.selenium_util.WebPage;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsf22APARSeleniumServer that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22APARSeleniumTests {
    @Rule
    public TestName name = new TestName();

    protected static final Class<?> c = JSF22APARSeleniumTests.class;

    @Server("jsf22APARSeleniumServer")
    public static LibertyServer jsf22APARSeleniumServer;

    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(FATSuite.getChromeImage()).withCapabilities(new ChromeOptions())
                    .withAccessToHost(true)
                    .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"

    private static ExtendedWebDriver driver;

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(jsf22APARSeleniumServer, "PH55398.war", "com.ibm.ws.jsf22.fat.PH55398.bean");

        jsf22APARSeleniumServer.startServer(c.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(jsf22APARSeleniumServer.getHttpDefaultPort(), jsf22APARSeleniumServer.getHttpDefaultSecurePort());

        driver = new CustomDriver(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf22APARSeleniumServer != null && jsf22APARSeleniumServer.isStarted()) {
            jsf22APARSeleniumServer.stopServer();
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
     * Test:  Hit the following link: 
     *          /PH55398
     *  See https://issues.apache.org/jira/browse/MYFACES-4606
     *  Tests checks that the issuing element is not visible in the request. 
     *  In other words, request parameters should contain the id:value info      
     *  for both ajax and non ajax requests.      
     * @throws Exception
     */
    @Test
    public void testPH55398() throws Exception {
            String url = JSFUtils.createSeleniumURLString(jsf22APARSeleniumServer, "PH55398", "index.xhtml");
            WebPage page = new WebPage(driver);
         
            page.get(url);
            page.waitForPageToLoad();

            WebElement ajaxButton = page.findElement(By.id("form1:ajaxbtn"));
            ajaxButton.click();
            page.waitReqJs();

            String paramValues = page.findElement(By.id("paramvalues")).getText();

            // check for form1:ajaxbtn : Ajax Submit
            assertTrue(paramValues.contains("form1:ajaxbtn : Ajax Submit"));

            WebElement nonAjaxButton = page.findElement(By.id("form1:nonajaxbtn"));
            nonAjaxButton.click();
            page.waitReqJs();

            paramValues = page.findElement(By.id("paramvalues")).getText();
            
            // check for form1:nonajaxbtn : Non Ajax Submit
            assertTrue(paramValues.contains("form1:nonajaxbtn : Non Ajax Submit"));
    }

    /*
    * Follow up to MYFACES-4606 and also tested via TCK's Issue2255IT (unintentionally)
    * Mozilla Checkbox Documentation: 
    * "If a checkbox is unchecked when its form is submitted, neither the name nor the value is submitted to the server."
    */
    @Test
    public void testPH55398_checkbox() throws Exception {
            String url = JSFUtils.createSeleniumURLString(jsf22APARSeleniumServer, "PH55398", "checkbox.xhtml");
            WebPage page = new WebPage(driver);
         
            page.get(url);
            page.waitForPageToLoad();

            WebElement ajaxButton = page.findElement(By.id("form1:checkbox"));
            ajaxButton.click();
            page.waitReqJs();

            String output = page.findElement(By.id("form1:output")).getText();

            assertEquals("true", page.findElement(By.id("form1:output")).getText());

            ajaxButton = page.findElement(By.id("form1:checkbox"));
            ajaxButton.click();
            page.waitReqJs();

            output = page.findElement(By.id("form1:output")).getText();
            assertEquals("false", page.findElement(By.id("form1:output")).getText());

    }
    
}
