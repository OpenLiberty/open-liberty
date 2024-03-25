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

import java.net.URL;

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
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
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
 * A test class to test <h:selectOneRadio/> component with defined groups.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF23SelectOneRadioGroupTests {

    protected static final Class<?> c = JSF23SelectOneRadioGroupTests.class;
    String contextRoot = "JSF23SelectOneRadioGroup";

    @Rule
    public TestName name = new TestName();

    @Server("jsf23SelectOneRadioGroupServer")
    public static LibertyServer server;

    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(FATSuite.getChromeImage()).withCapabilities(new ChromeOptions())
                    .withAccessToHost(true)
                    .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"
    
    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "JSF23SelectOneRadioGroup.war", "com.ibm.ws.jsf23.fat.selectoneradio");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server.
        server.startServer(c.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * This test case will ensure that <h:selectOneRadio/> component can be used with AJAX requests.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after ajax request.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_AjaxRequest() throws Exception {
        ExtendedWebDriver driver = new CustomDriver(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));

        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "selectOneRadioGroupAjaxRequest.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        assertTrue("The page was not rendered correctly.", page.isInPage("JSF 2.3 SelectOneRadio Ajax Request"));

        page.findElement(By.id("f3:radio1A")).click();
        page.waitReqJs();

        // Log the page for debugging if necessary in the future.
        Log.info(c, name.getMethodName(), page.getPageSource());
        assertTrue("Radio button 1 is not checked.", page.findElement(By.id("f3:radio1A")).isEnabled());

        assertTrue("Selected value was not found.", page.isInPage("Selected Value: staticValue2"));

        page.findElement(By.id("f3:radio0")).click();
        page.waitReqJs();

        Log.info(c, name.getMethodName(), page.getPageSource());

        // Verify that it is checked
        assertTrue("Radio button 0 is not checked.", page.findElement(By.id("f3:radio0")).isEnabled());

        assertTrue("Selected value was not found.", page.isInPage("Selected Value: staticValue1"));
    }

    /**
     * This test case will ensure that <h:selectOneRadio/> component can be populated with default ID.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_defaultID() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupDefaultID.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: DefaultID"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("j_id_9");
            // Mark it as checked
            radioButton1.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio button again after submitting the form
            radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("j_id_9");

            // Verify that it is checked
            assertTrue("Radio button is not checked.", radioButton1.isChecked());

            assertTrue("Selected value was not found.", resultingPage.contains("Selected Value: staticValue1"));
        }
    }

    /**
     * This test case will use <h:selectOneRadio/> component individually with assigned IDs.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_individual() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupIndividual.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: individual usage"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton2 = (HtmlRadioButtonInput) testPage.getElementById("radio2");
            // Mark it as checked
            radioButton2.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio button again after submitting the form
            radioButton2 = (HtmlRadioButtonInput) testPage.getElementById("radio2");

            // Verify that it is checked
            assertTrue("Radio button is not checked.", radioButton2.isChecked());

            assertTrue("Selected value was not found.", resultingPage.contains("Selected Value: Value3"));
        }
    }

    /**
     * This test case will use <h:selectOneRadio/> component in multiple interwoven groups.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_interweaveGroups() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupInterweaveGroups.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: InterweaveGroups"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("radio1");
            // Mark it as checked
            radioButton1.setChecked(true);

            // Get the another radio button
            HtmlRadioButtonInput radioButton5 = (HtmlRadioButtonInput) testPage.getElementById("radio5");
            // Mark it as checked
            radioButton5.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio buttons again after submitting the form
            radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("radio1");
            radioButton5 = (HtmlRadioButtonInput) testPage.getElementById("radio5");

            // Verify that they are checked
            assertTrue("Radio button 'radio1' is not checked.", radioButton1.isChecked());
            assertTrue("Radio button 'radio5' is not checked.", radioButton5.isChecked());

            assertTrue("Selected value from testGroup was not found.", resultingPage.contains("Selected Value testGroup: Value2"));
            assertTrue("Selected value from testGroup1 was not found.", resultingPage.contains("Selected Value testGroup1: Value6"));
        }
    }

    /**
     * This test case will use <h:selectOneRadio/> component in multiple groups.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_multipleGroups() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupMultipleGroups.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: MultipleGroups"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("radio1");
            // Mark it as checked
            radioButton1.setChecked(true);

            // Get the another radio button
            HtmlRadioButtonInput radioButton5 = (HtmlRadioButtonInput) testPage.getElementById("radio5");
            // Mark it as checked
            radioButton5.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio buttons again after submitting the form
            radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("radio1");
            radioButton5 = (HtmlRadioButtonInput) testPage.getElementById("radio5");

            // Verify that they are checked
            assertTrue("Radio button 'radio1' is not checked.", radioButton1.isChecked());
            assertTrue("Radio button 'radio5' is not checked.", radioButton5.isChecked());

            assertTrue("Selected value from testGroup was not found.", resultingPage.contains("Selected Value testGroup: Value2"));
            assertTrue("Selected value from testGroup1 was not found.", resultingPage.contains("Selected Value testGroup1: Value6"));
        }
    }

    /**
     * This test case will use <h:selectOneRadio/> component with multiple selectItem components.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_multipleSelectItem() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupMultipleSelectItem.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: Multiple selectItem"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("radio1");
            // Mark it as checked
            radioButton1.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio button again after submitting the form
            radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("radio1");

            // Verify that it is checked
            assertTrue("Radio button is not checked.", radioButton1.isChecked());

            assertTrue("Selected value was not found.", resultingPage.contains("Selected Value: Value2"));
        }
    }

    /**
     * This test case will use <h:selectOneRadio/> component with only one value expression in the first element.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_oneValue() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupOneValue.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: OneValue"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("radio1");
            // Mark it as checked
            radioButton1.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio button again after submitting the form
            radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("radio1");

            // Verify that it is checked
            assertTrue("Radio button is not checked", radioButton1.isChecked());

            assertTrue("Selected value was not found.", resultingPage.contains("Selected Value: Value2"));
        }
    }

    /**
     * This test case will use <h:selectOneRadio/> component with only one value expression in the first element.
     * It also has just one selectItems in the first element.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_selectItems() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupSelectItems.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: selectItems"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton2 = (HtmlRadioButtonInput) testPage.getElementById("radio2");
            // Mark it as checked
            radioButton2.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio button again after submitting the form
            radioButton2 = (HtmlRadioButtonInput) testPage.getElementById("radio2");

            // Verify that it is checked
            assertTrue("Radio button is not checked.", radioButton2.isChecked());

            assertTrue("Selected value was not found.", resultingPage.contains("Selected Value: Value3"));
        }
    }

    /**
     * This test case will use <h:selectOneRadio/> component with a collection in selectItems.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_selectItemsCollection() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupSelectItemsCollection.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: selectItems Collection"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton2 = (HtmlRadioButtonInput) testPage.getElementById("radio2");
            // Mark it as checked
            radioButton2.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio button again after submitting the form
            radioButton2 = (HtmlRadioButtonInput) testPage.getElementById("radio2");

            // Verify that it is checked
            assertTrue("Radio button is not checked.", radioButton2.isChecked());

            assertTrue("Selected value was not found.", resultingPage.contains("Selected Value: Value3"));
        }
    }

    /**
     * This test case will use <h:selectOneRadio/> component with a collection in selectItems.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_selectItemsOverride() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupSelectItemsOverride.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: selectItems Override"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton2 = (HtmlRadioButtonInput) testPage.getElementById("radio2");
            // Mark it as checked
            radioButton2.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio button again after submitting the form
            radioButton2 = (HtmlRadioButtonInput) testPage.getElementById("radio2");

            // Verify that it is checked
            assertTrue("Radio button is not checked.", radioButton2.isChecked());

            assertTrue("Selected value was not found.", resultingPage.contains("Selected Value: ValueOverride"));
        }
    }

    /**
     * This test case will use <h:selectOneRadio/> component with static values.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_staticValue() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupStaticValue.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: Static Value"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("radio1");
            // Mark it as checked
            radioButton1.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio button again after submitting the form
            radioButton1 = (HtmlRadioButtonInput) testPage.getElementById("radio1");

            // Verify that it is checked
            assertTrue("Radio button is not checked.", radioButton1.isChecked());

            assertTrue("Selected value was not found.", resultingPage.contains("Selected Value: staticValue2"));
        }
    }

    /**
     * This test case will ensure that the <h:dataTable/> component can be used
     * to add a <selectOneRadio/> component to the page in a single group
     * for a set of values.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_table() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupTable.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: used in a table"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton2 = (HtmlRadioButtonInput) testPage.getElementById("table1:2:radio");
            // Mark it as checked
            radioButton2.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio button again after submitting the form
            radioButton2 = (HtmlRadioButtonInput) testPage.getElementById("table1:2:radio");

            // Verify that it is checked
            assertTrue("Radio button is not checked.", radioButton2.isChecked());

            assertTrue("Selected value was not found.", resultingPage.contains("Selected Value: Value3"));
        }
    }

    /**
     * This test case will ensure that the <ui:repeat/> component can be used
     * to add a <selectOneRadio/> component to the page in a single group
     * for a set of values.
     * Also verify that a radio button can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testSelectOneRadioGroup_uiRepeat() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "selectOneRadioGroupUIRepeat.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectOneRadio Group Attribute: used with ui:repeat"));

            // Get the a radio button
            HtmlRadioButtonInput radioButton0 = (HtmlRadioButtonInput) testPage.getElementById("ui_repeat_id:0:radio");
            // Mark it as checked
            radioButton0.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the radio button again after submitting the form
            radioButton0 = (HtmlRadioButtonInput) testPage.getElementById("ui_repeat_id:0:radio");

            // Verify that it is checked
            assertTrue("Radio button is not checked.", radioButton0.isChecked());

            assertTrue("Selected value was not found.", resultingPage.contains("Selected Value: Value1"));
        }
    }

    /**
     * This test case will verify that rendering of <h:selectOneRadio/> component is correct.
     *
     * Case: When the ID of the first component ends with a number, but then the next selectOneRadio doesn’t contain
     * the next number in sequence, and they are part of the same group.
     *
     * For more information: https://issues.apache.org/jira/browse/MYFACES-4169
     *
     * @throws Exception
     */
    @Test
    public void testMyFaces4169RendingIssue_IDEndingWithNumberNextWithLetter() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "myFaces4169IDEndingWithNumberNextWithLetter.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asXml();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testPage.asText());
            Log.info(c, name.getMethodName(), resultingPage);

            assertTrue("ID of first radio button is incorrect.", resultingPage.contains("id=\"radio0\""));
            assertTrue("ID of second radio button is incorrect.", resultingPage.contains("id=\"radio1A\""));
            assertTrue("ID of third radio button is incorrect.", resultingPage.contains("id=\"radio1B\""));
        }
    }

    /**
     * This test case will verify that rendering of <h:selectOneRadio/> component is correct.
     *
     * Case: When ID contains a number at end that it is greater than the number of selectItems an IndexOutOfBound should not be thrown
     * and rendering of the <h:selectOneRadio/> components should be correct.
     *
     * For more information: https://issues.apache.org/jira/browse/MYFACES-4169
     *
     * @throws Exception
     */
    @Test
    public void testMyFaces4169RendingIssue_IDNumberGreaterThanSelectItems() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "myFaces4169IDNumberGreaterThanSelectItems.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asXml();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testPage.asText());
            Log.info(c, name.getMethodName(), resultingPage);

            assertTrue("ID of first radio button is incorrect.", resultingPage.contains("id=\"radio8\""));
            assertTrue("ID of second radio button is incorrect.", resultingPage.contains("id=\"radio1\""));
            assertTrue("ID of third radio button is incorrect.", resultingPage.contains("id=\"radio2\""));
        }
    }

    /**
     * This test case will verify that rendering of <h:selectOneRadio/> component is correct.
     *
     * Case: When ID contains a number at the end, and that number is not 0, for example “radio1”,
     * it should not select the second item value in the list, it should start with the first one.
     *
     * For more information: https://issues.apache.org/jira/browse/MYFACES-4169
     *
     * @throws Exception
     */
    @Test
    public void testMyFaces4169RendingIssue_IDStartingWithIndex1() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "myFaces4169IDStartingWithIndex1.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asXml();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testPage.asText());
            Log.info(c, name.getMethodName(), resultingPage);

            assertTrue("Value of first radio button is incorrect.", resultingPage.contains("value=\"radio1:staticValue1\""));
            assertTrue("Value of second radio button is incorrect.", resultingPage.contains("value=\"radio2:staticValue2\""));
        }
    }

    /**
     * This test case will verify that rendering of <h:selectOneRadio/> component is correct.
     *
     * Case: When ID does not contain a number at the end, the rendered markup should not contains the same ID
     * in all the radio components
     *
     * For more information: https://issues.apache.org/jira/browse/MYFACES-4169
     *
     * @throws Exception
     */
    @Test
    public void testMyFaces4169RendingIssue_IDWithoutNumber() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "myFaces4169IDWithoutNumberAtTheEnd.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asXml();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testPage.asText());
            Log.info(c, name.getMethodName(), resultingPage);

            assertTrue("ID of first radio button is incorrect.", resultingPage.contains("id=\"radioOther\""));
            assertTrue("ID of second radio button is incorrect.", resultingPage.contains("id=\"radioOtherA\""));
            assertTrue("ID of second radio button is incorrect.", resultingPage.contains("id=\"radioOtherB\""));
        }
    }

}
