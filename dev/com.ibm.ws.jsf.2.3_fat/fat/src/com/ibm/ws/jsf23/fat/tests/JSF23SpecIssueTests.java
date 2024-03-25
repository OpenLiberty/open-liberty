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

import static componenttest.annotation.SkipForRepeat.EE10_OR_LATER_FEATURES;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
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

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.FATSuite;
import com.ibm.ws.jsf23.fat.JSFUtils;
import com.ibm.ws.jsf23.fat.selenium_util.CustomDriver;
import com.ibm.ws.jsf23.fat.selenium_util.ExtendedWebDriver;
import com.ibm.ws.jsf23.fat.selenium_util.WebPage;


import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * JSF 2.3 spec issue tests.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JSF23SpecIssueTests {

    protected static final Class<?> c = JSF23SpecIssueTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23SpecIssueServer")
    public static LibertyServer server;

    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(FATSuite.getChromeImage()).withCapabilities(new ChromeOptions())
                    .withAccessToHost(true)
                    .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "JSF23Spec1300.war", "com.ibm.ws.jsf23.fat.spec1300");
        ShrinkHelper.defaultDropinApp(server, "JSF23Spec217.war", "com.ibm.ws.jsf23.fat.spec217");
        ShrinkHelper.defaultDropinApp(server, "JSF23Spec790.war", "com.ibm.ws.jsf23.fat.spec790");
        ShrinkHelper.defaultDropinApp(server, "ResourceRendering.war", "com.ibm.ws.jsf23.fat.resourcerendering");
        ShrinkHelper.defaultDropinApp(server, "JSF23CDITests.war",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans.factory",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.client.window");

        // Create the JSF23Spec1433 JAR
        JavaArchive spec1433Jar = ShrinkWrap.create(JavaArchive.class, "JSF23Spec1433.jar");
        spec1433Jar.addPackage("com.ibm.ws.jsf23.fat.spec1433");

        // Create the JSF23Spec1433True WAR and add the JSF23Spec1433 JAR to it
        WebArchive spec1433TrueWar = ShrinkWrap.create(WebArchive.class, "JSF23Spec1433True.war");
        spec1433TrueWar.addAsLibrary(spec1433Jar);
        ShrinkHelper.addDirectory(spec1433TrueWar, "test-applications/" + "JSF23Spec1433True.war" + "/resources");
        ShrinkHelper.exportToServer(server, "dropins", spec1433TrueWar);

        // Create the JSF231433SpecFalse WAR and add the JSF23Spec1433 JAR to it
        WebArchive spec1433FalseWar = ShrinkWrap.create(WebArchive.class, "JSF23Spec1433False.war");
        spec1433FalseWar.addAsLibrary(spec1433Jar);
        ShrinkHelper.addDirectory(spec1433FalseWar, "test-applications/" + "JSF23Spec1433False.war" + "/resources");
        ShrinkHelper.exportToServer(server, "dropins", spec1433FalseWar);

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server
        server.startServer(c.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());
    }

    @Before
    public void startServer() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer(JSF23SpecIssueTests.class.getSimpleName() + ".log");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test for the following JSF 2.3 Specification Issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/1300
     *
     * First we drive a request to the application and ensure a page that uses
     * a ViewScoped bean loads. This request should ensure that the PostConstructViewMapEvent
     * is published.
     *
     * The application uses a custom ViewRoot component and has a listener for the
     * PostConstructViewMapEvent.
     *
     * The PostConstructViewMapEvent JavaDoc states that the source class for the event
     * will be javax.faces.component.UIViewRoot.
     *
     * Since the application is using a custom ViewRoot and the specification does not
     * allow Application.publishEvent to go up the type hierarchy a sourceBaseType must
     * be used to ensure the listener is invoked even when the UIViewRoot is extended.
     *
     * The source class expected would be UIViewRoot but the actual source class is
     * CustomViewRoot. Using the sourceBaseType solves the issue.
     *
     * MyFaces solution: https://issues.apache.org/jira/browse/MYFACES-4158
     *
     * @throws Exception
     */
    @Test
    public void testSpecIssue1300_MyFaces_4158() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec1300";
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure the page loaded correctly
            String pageText = "JSF 2.3 Spec Issue 1300";
            assertTrue("The page did not contain the following text: " + pageText, page.asText().contains(pageText));

            // No look at the logs to ensure that the PostConstructViewMapEventListener was invoked
            String listenerString = "PostConstructViewMapEventListener processEvent invoked for PostConstructViewMapEvent!!";
            String listenerInvoked = server.waitForStringInLog(listenerString);

            // Ensure the string was found, otherwise the test should fail.
            assertNotNull("The following string was not found in the logs: " + listenerString, listenerInvoked);
        }
    }

    /**
     * A test for the following JSF 2.3 Specification issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/217
     *
     * There are two parts to this test.
     *
     * First we test the h:dataTable rowClass attribute to ensure that
     * the class attribute is rendered correctly. The even rows should
     * have a class attribute with a value of "testStyleRowEven" and
     * the odd rows with a value of "testStyleRowOdd".
     *
     * Second we test the h:column styleClass attribute to ensure that
     * the class attribute is rendered correctly. Each column should have
     * a class attribute with a value of "testStyleCol".
     *
     * @throws Exception
     */
    @Test
    public void testSpecIssue217() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec217";
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // First make sure that the h:dataTable rowClass attribute works correctly
            HtmlTable table = (HtmlTable) page.getElementById("table");
            String evenClass = "testStyleRowEven";
            String oddClass = "testStyleRowOdd";

            // Need to look at the second and third row as the first row (0) is the header.
            HtmlTableRow row1 = table.getRow(1);
            HtmlTableRow row2 = table.getRow(2);
            Log.info(c, name.getMethodName(), row1.toString());
            Log.info(c, name.getMethodName(), row2.toString());
            String row1Class = row1.getAttribute("class");
            String row2Class = row2.getAttribute("class");

            assertTrue("The class attribute for row 1 should have been: " + evenClass + " but was: " + row1Class, row1Class.equals(evenClass));
            assertTrue("The class attribute for row 2 should have been: " + oddClass + " but was: " + row2Class, row2Class.equals(oddClass));

            // Second make sure that the h:column styleClass attribute works correctly
            String columnClassExpected = "testStyleCol";
            String columnClass;
            List<HtmlTableRow> rows = table.getRows();
            HtmlTableCell cell;

            // Skip row 0 since that is the header
            for (int i = 1; i < rows.size(); i++) {
                cell = rows.get(i).getCell(0);
                columnClass = cell.getAttribute("class");
                Log.info(c, name.getMethodName(), cell.asXml());
                Log.info(c, name.getMethodName(), columnClass);
                assertTrue("The cell had an incorrect column style class. It was expected to be: " + columnClassExpected +
                           " but was: " + columnClass, columnClass.equals(columnClassExpected));
            }
        }
    }

    /**
     * Test ClientWindowFactory spec issue: https://github.com/javaee/javaserverfaces-spec/issues/1241
     * We should be able to use <client-window-factory></client-window-factory> in the faces-config.xml
     *
     * Test that field and method injection is possible on ClientWindowFactory JSF artifact.
     *
     * @throws Exception
     */
    @Test
    public void testSpecIssue1241AndClientWindowFactoryInjection() throws Exception {

        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23CDITests";
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "FactoryInfo.jsf");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Format:  Class | Method | Method injected class: Field injected class : <option> constructor injected class
            String expectedText = "CustomClientWindowFactory|getClientWindow|FactoryDepBean:FactoryAppBean:PostConstructCalled";
            assertTrue("Did not find " + expectedText + " in response", page.asText().contains(expectedText));

            // Stop the server but don't archive the logs (this should shutdown the app and trigger preDestroy)
            server.stopServer(false);

            // Verify that PreDestroy is being called
            assertTrue("PreDestroy was not called",
                       server.findStringsInLogs("CustomClientWindowFactory preDestroy called").size() == 1);

            // Archive the logs
            server.postStopServerArchive();
        }
    }

    /**
     * A test for the following JSF 2.3 Specification issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/790
     *
     * Test that cross form submit can be done using AJAX.
     *
     * First we drive a request to the application.
     *
     * First part:
     * Input a value to form 'a' and submit that value. Verify that the input value
     * of the second form 'b' was re-rendered.
     *
     * Second part:
     * Input a value to form 'b' and submit that value. Verify that the input value
     * of the first form 'a' was re-rendered.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat(EE10_OR_LATER_FEATURES)
    public void testSpecIssue790Test1() throws Exception {
        try (WebClient webClient = new WebClient()) {
            // Use a synchronizing ajax controller to allow proper ajax updating
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());

            // Construct the URL for the test
            String contextRoot = "JSF23Spec790";
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "test1.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Get the input text of the first form and set a value
            HtmlTextInput textInput1 = (HtmlTextInput) page.getElementById("a:input1");
            textInput1.setValueAttribute("test");

            // Get the button to click from the first form
            HtmlSubmitInput submitButton = (HtmlSubmitInput) page.getElementById("a:submitButton1");
            page = submitButton.click();

            String resultingPage = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), page.asXml());

            // Verify that the input text from the second form contains the same value as the input text of the first form.
            assertTrue("Input text of the second form does not contain '" + textInput1.getValueAttribute() + "'",
                       resultingPage.contains("Enter a new value for input2: test"));

            // Get the input text for the second form and set a value
            HtmlTextInput textInput2 = (HtmlTextInput) page.getElementById("b:input2");
            textInput2.setValueAttribute("testagain");

            // Get the commandLink to click from the second form
            HtmlAnchor commandLink1 = (HtmlAnchor) page.getElementById("b:commandLink1");
            page = commandLink1.click();

            resultingPage = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), page.asXml());

            // Verify that the input text from the first form contains the same value as the input text of the second form.
            assertTrue("Input text of the first form does not contain '" + textInput2.getValueAttribute() + "'",
                       resultingPage.contains("Enter a new value for input1: testagain"));
        }
    }

    /**
     * A test for the following JSF 2.3 Specification issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/790
     *
     * Test that cross form submit can be done using AJAX.
     *
     * First we drive a request to the application.
     *
     * Input a value to form 'a' and submit that value. Verify that the output text value
     * of the second form 'b' was re-rendered.
     *
     * @throws Exception
     */
    @Test
    public void testSpecIssue790Test2() throws Exception {
        String contextRoot = "JSF23Spec790";
        ExtendedWebDriver driver = new CustomDriver(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));

        String url = JSFUtils.createSeleniumURLString(server, contextRoot, "test2.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        WebElement input = page.findElement(By.id("a:input1"));
        input.sendKeys("test");

        page.findElement(By.id("a:submitButton1")).click();
        page.waitReqJs();

        Log.info(c, name.getMethodName(), page.getPageSource());

        // Verify that the input text from the second form contains the same value as the input text of the first form.
        assertTrue("Input text of the second form is not correct",
                   page.isInPageTextReduced("This is the value from outputText of form 'b': test"));
    }

    /**
     * This test case uses an application that has the following context-parameter enabled:
     * javax.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE
     *
     * With the context-parameter enabled, and input fields with required set to true
     * validation should occur even when getSubmittedValue returns null. The only way
     * I've been able to get a getSubmittedValue to be null is to hack the response a bit.
     *
     * Setting the input1 name to "" and then submitting the page does the trick.
     *
     * When no values are submitted in either field we should see both requiredMessage values
     * displayed.
     *
     * @throws Exception
     */
    @Test
    public void testSpecIssue1433_PerformValidationRequiredTrueEnabled_EmptySubmit() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec1433True";
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlInput input1 = (HtmlInput) page.getElementById("input1");
            input1.setAttribute("name", "");

            page = page.getElementById("button1").click();

            String pageText = page.asText();
            String expectedText1 = "Value one is required please enter value one!";
            String expectedText2 = "Value two is required please enter value two!";

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The page did not contain: " + expectedText1, pageText.contains(expectedText1));
            assertTrue("The page did not contain: " + expectedText2, pageText.contains(expectedText2));
        }
    }

    /**
     * This test case uses an application that has the following context-parameter enabled:
     * javax.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE
     *
     * With the context-parameter enabled, and input fields with required set to true
     * validation should occur even when getSubmittedValue returns null. The only way
     * I've been able to get a getSubmittedValue to be null is to hack the response a bit.
     *
     * Setting the input1 name to "" and then submitting the page does the trick.
     *
     * When values are submitted in both fields we should see the requiredMessage for
     * the first input only since getSubmittedValue for it was null since we changed
     * the name attribute before we submitted.
     *
     * @throws Exception
     */
    @Test
    public void testSpecIssue1433_PerformValidationRequiredTrueEnabled_ValueSubmit() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec1433True";
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlInput input1 = (HtmlInput) page.getElementById("input1");
            input1.setAttribute("name", "");
            input1.setValueAttribute("test");

            HtmlInput input2 = (HtmlInput) page.getElementById("input2");
            input2.setValueAttribute("test");

            page = page.getElementById("button1").click();

            String pageText = page.asText();
            String expectedText = "Value one is required please enter value one!";
            String unexpectedText = "Value two is required please enter value two!";

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The page did not contain: " + expectedText, pageText.contains(expectedText));
            assertTrue("The page contains unexpected text: " + unexpectedText, !pageText.contains(unexpectedText));
        }
    }

    /**
     * This test case uses an application that has the following context-parameter disabled:
     * javax.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE
     *
     * With the context-parameter disabled, and input fields with required set to true
     * validation should not occur even when getSubmittedValue returns null. The only way
     * I've been able to get a getSubmittedValue to be null is to hack the response a bit.
     *
     * Setting the input1 name to "" and then submitting the page does the trick.
     *
     * When no values are submitted in either field we should see the second input
     * requiredMessage since no validation was done on the first input which had the
     * name attribute set to "".
     *
     * @throws Exception
     */
    @Test
    public void testSpecIssue1433_PerformValidationRequiredTrueDisabled_EmptySubmit() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec1433False";
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlInput input1 = (HtmlInput) page.getElementById("input1");
            input1.setAttribute("name", "");

            page = page.getElementById("button1").click();

            String pageText = page.asText();
            String expectedText = "Value two is required please enter value two!";
            String unexpectedText = "Value one is required please enter value one!";

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The page did not contain: " + expectedText, pageText.contains(expectedText));
            assertTrue("The page contains unexpected text: " + unexpectedText, !pageText.contains(unexpectedText));
        }
    }

    /**
     * This test case uses an application that has the following context-parameter disabled:
     * javax.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE
     *
     * With the context-parameter disabled, and input fields with required set to true
     * validation should not occur even when getSubmittedValue returns null. The only way
     * I've been able to get a getSubmittedValue to be null is to hack the response a bit.
     *
     * Setting the input1 name to "" and then submitting the page does the trick.
     *
     * When values are submitted in both fields we should not see a requiredMessage for
     * either input as a value was entered for each field and validation won't be
     * performed on the hacked input since the parameter is disabled.
     *
     * @throws Exception
     */
    @Test
    public void testSpecIssue1433_PerformValidationRequiredTrueDisabled_ValueSubmit() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec1433False";
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlInput input1 = (HtmlInput) page.getElementById("input1");
            input1.setValueAttribute("test");

            HtmlInput input2 = (HtmlInput) page.getElementById("input2");
            input2.setValueAttribute("test");

            page = page.getElementById("button1").click();

            String pageText = page.asText();
            String unexpectedText1 = "Value one is required please enter value one!";
            String unexpectedText2 = "Value two is required please enter value two!";

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure that we don't see any validation messages in this case
            assertTrue("Page contains unexpected text: " + unexpectedText1, !pageText.contains(unexpectedText1));
            assertTrue("Page contains unexpected text: " + unexpectedText2, !pageText.contains(unexpectedText2));
        }
    }

    /**
     * Test the new JSF 2.3 API methods described in the following two spec issues:
     * https://github.com/javaee/javaserverfaces-spec/issues/1404
     * https://github.com/javaee/javaserverfaces-spec/issues/1423
     *
     * ResourceHandler.markResourceRendered()
     * ResourceHandler.isResourceRendered()
     * UIViewRoot.getComponentResources()
     *
     * @throws Exception
     */
    @Test
    public void testSpecIssue1404And1423_JSF23NewAPIMethods() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "ResourceRendering";
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlSubmitInput submitButton = (HtmlSubmitInput) page.getElementById("form1:submitButton");
            page = submitButton.click();

            String pageText = page.asText();
            String expectedText1 = "Message from CustomComponent: isResourceRendered library=css name=test-style.css --> false";
            String expectedText2 = "Message from CustomComponent: isResourceRendered library=css name=test-style.css --> true";
            String expectedText3 = "Message from CustomComponent: getComponentResources List size --> 2";
            String expectedText4 = "Message from MyBean: isResourceRendered library=css name=test-style.css --> true";
            String expectedText5 = "Message from MyBean: isResourceRendered library=css name=another.css --> false";
            String expectedText6 = "Message from MyBean: isResourceRendered library=another name=test-style.css --> false";

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure that we don't see any validation messages in this case
            assertTrue("Page does not contain expected text: " + expectedText1, pageText.contains(expectedText1));
            assertTrue("Page does not contain expected text: " + expectedText2, pageText.contains(expectedText2));
            assertTrue("Page does not contain expected text: " + expectedText3, pageText.contains(expectedText3));
            assertTrue("Page does not contain expected text: " + expectedText4, pageText.contains(expectedText4));
            assertTrue("Page does not contain expected text: " + expectedText5, pageText.contains(expectedText5));
            assertTrue("Page does not contain expected text: " + expectedText6, pageText.contains(expectedText6));
        }
    }
}
