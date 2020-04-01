/*
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsfTestServer1 that use HtmlUnit.
 * This particular class executes the tests found in the TestJSF2.2 application.
 * These tests are relatively standalone.
 */
@RunWith(FATRunner.class)
public class JSFSimpleHtmlUnit {
    @Rule
    public TestName name = new TestName();

    protected static final Class<?> c = JSFSimpleHtmlUnit.class;

    String contextRoot = "JSF22SimpleHTML";

    @Server("jsfTestServer1")
    public static LibertyServer jsfTestServer1;

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(jsfTestServer1, "JSF22SimpleHTML.war", "com.ibm.ws.jsf22.fat.simple.bean", "com.ibm.ws.jsf22.fat.simple.cforeach",
                                      "com.ibm.ws.jsf22.fat.simple.externalContext");

        jsfTestServer1.startServer(JSFServerTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer1 != null && jsfTestServer1.isStarted()) {
            jsfTestServer1.stopServer();
        }
    }

    /*
     * A sample HtmlUnit test case for JSF. Just ensure that the basic application is reachable.
     *
     * @throws Exception
     */
    @Test
    public void sampleTest() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            assertTrue(page.asText().contains("Hello World"));
        }
    }

    /**
     * 167256: create testcase for UIViewParameter.getSubmittedValue() i.e. JAVASERVERFACES_SPEC_PUBLIC-1063 spec clarification
     *
     * Testcase will test first setting the value and then get the submittedValue. Make sure it is of type Object.
     */
    @Test
    public void testEditableValueHoldergetSubmittedValue() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testValue.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            // Log.info(c, name.getMethodName(), "testEditableValueHoldergetSubmittedValue:: page " + page.asXml());

            // Click the commandButton to execute the methods and update the page
            HtmlElement button = (HtmlElement) page.getElementById("testForm:test");
            page = button.click();

            Log.info(c, name.getMethodName(), "testEditableValueHoldergetSubmittedValue:: page now " + page.asXml());
            assertTrue(page.asXml().contains("getSubmittedValue PASS"));
        }
    }

    /**
     * 167254: create testcase for JAVASERVERFACES_SPEC_PUBLIC-1064 spec clarification regd CDATA section
     *
     * Testcase will check the data within the CDATA section is not consumed.
     */
    @Test
    public void testDatainCdataSectionWorks() throws Exception {
        try (WebClient webClient = new WebClient()) {
            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testCdata.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            Log.info(c, name.getMethodName(), "testEditableValueHoldergetSubmittedValue:: page --> " + page.asText());

            assertTrue(page.asText().contains("Hello World!"));
        }
    }

    /**
     * Test the c:forEach tag using an object with overriden equals method
     * and with an object that is not serializable.
     *
     * Ensure that c:forEach does not use an outdated reference to the object
     * by checking the response of the page.
     *
     * @throws Exception
     */
    @Test
    public void testCForEachWithCustomEqualsAndNonSerializableObjects() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "forEach-equals.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Page without clicking the button of the form called Change Items
            assertTrue(page.asText().contains("ForEach equals and non serializable behavior"));

            assertTrue(page.asText().contains("Overriden equals c:forEach: 0 1 2 3 4 0 1 2 3 4"));
            assertTrue(page.asText().contains("Overriden equals ui:repeat: 0 1 2 3 4 0 1 2 3 4"));
            assertTrue(page.asText().contains("Serializable Object c:forEach: 0 1 2 3 4 0 1 2 3 4"));
            assertTrue(page.asText().contains("Serializable Object ui:repeat: 0 1 2 3 4 0 1 2 3 4"));
            assertTrue(page.asText().contains("Non-Serializable Object c:forEach: 0 1 2 3 4 0 1 2 3 4"));
            assertTrue(page.asText().contains("Non-Serializable Object ui:repeat: 0 1 2 3 4 0 1 2 3 4"));

            // Get the form that we are dealing
            HtmlForm changedItemsForm1 = page.getFormByName("testCForEachForm");

            // Get the button to change the items
            HtmlSubmitInput changeItemsButton1 = changedItemsForm1.getInputByName("changeItemsButton");

            // Now submit the form by clicking the button for the first time
            HtmlPage changedItems1 = changeItemsButton1.click();

            // Page after clicking the button Change Items for the first time
            assertTrue(changedItems1.asText().contains("Overriden equals c:forEach: 0 1 2 3 4 0 2 4 6 8"));
            assertTrue(changedItems1.asText().contains("Overriden equals ui:repeat: 0 1 2 3 4 0 2 4 6 8"));
            assertTrue(changedItems1.asText().contains("Serializable Object c:forEach: 0 1 2 3 4 0 2 4 6 8"));
            assertTrue(changedItems1.asText().contains("Serializable Object ui:repeat: 0 1 2 3 4 0 2 4 6 8"));
            assertTrue(changedItems1.asText().contains("Non-Serializable Object c:forEach: 0 1 2 3 4 0 2 4 6 8"));
            assertTrue(changedItems1.asText().contains("Non-Serializable Object ui:repeat: 0 1 2 3 4 0 2 4 6 8"));

            // Get the form that we are dealing
            HtmlForm changedItemsForm2 = changedItems1.getFormByName("testCForEachForm");

            // Get the button to change the items
            HtmlSubmitInput changeItemsButton2 = changedItemsForm2.getInputByName("changeItemsButton");

            // Now submit the form by clicking the button
            HtmlPage changedItems2 = changeItemsButton2.click();

            // Page after clicking the button Change Items for the second time
            assertTrue(changedItems2.asText().contains("Overriden equals c:forEach: 0 1 2 3 4 0 3 6 9 12"));
            assertTrue(changedItems2.asText().contains("Overriden equals ui:repeat: 0 1 2 3 4 0 3 6 9 12"));
            assertTrue(changedItems2.asText().contains("Serializable Object c:forEach: 0 1 2 3 4 0 3 6 9 12"));
            assertTrue(changedItems2.asText().contains("Serializable Object ui:repeat: 0 1 2 3 4 0 3 6 9 12"));
            assertTrue(changedItems2.asText().contains("Non-Serializable Object c:forEach: 0 1 2 3 4 0 3 6 9 12"));
            assertTrue(changedItems2.asText().contains("Non-Serializable Object ui:repeat: 0 1 2 3 4 0 3 6 9 12"));
        }
    }

    /**
     * Test h:commandButton when there is one composite component with
     * comments on interface section.
     *
     * Ensure that after clicking the button for the first time, the action is performed.
     *
     * Action wasn't being performed on first click
     *
     * @throws Exception
     */
    @Test
    public void testHCommandButtonWithCompositeComponentOnFirstClick() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testCommandButton.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            assertTrue(page.asText().contains("Testing h:commandButton on first click"));

            // Get the form that we are dealing
            HtmlForm testForm = page.getFormByName("testForm");

            // Get the button to be clicked
            HtmlSubmitInput commandButton = testForm.getInputByValue("Execute_Action");

            // Click the button
            HtmlPage resultPage = commandButton.click();

            // Page and response after clicking the button one time
            assertTrue(resultPage.asText().contains("Action was performed on first click!"));
        }
    }

    /**
     * Create a testcase for JAVASERVERFACES_SPEC_PUBLIC-1004 , default size is 1024 as compared to -1 in previous versions
     *
     * @throws Exception
     */
    @Test
    public void check_default_FACELETS_BUFFER_SIZE() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Make a request to a dummy page to ensure that MyFaces initializes if it has not done so already
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "dummy.jsf");

            String msg = "No context init parameter 'javax.faces.FACELETS_BUFFER_SIZE' found, using default value '1024'";
            // Check the trace.log
            // There should be a match so fail if there is not.
            assertFalse(msg, jsfTestServer1.findStringsInLogs(msg).isEmpty());
            Log.info(c, name.getMethodName(), "check_default_FACELETS_BUFFER_SIZE :: Found expected msg in log -->" + msg);
        }
    }

    /**
     * Create a testcase 169346: Port MYFACES-3949, javax.faces.ViewState autocomplete
     *
     * @throws Exception
     */
    @Test
    public void check_defaultLogging_AUTOCOMPLETE_OFF_VIEW_STATE() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Make a request to a dummy page to ensure that MyFaces initializes if it has not done so already
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "dummy.jsf");
            webClient.getPage(url);

            String msg = "No context init parameter 'org.apache.myfaces.AUTOCOMPLETE_OFF_VIEW_STATE' found, using default value 'true'";
            // Check the trace.log
            // There should be a match so fail if there is not.
            assertFalse(msg, jsfTestServer1.findStringsInLogs(msg).isEmpty());
            Log.info(c, name.getMethodName(), "check_defaultLogging_AUTOCOMPLETE_OFF_VIEW_STATE :: Found expected msg in log -->" + msg);
        }
    }

    /**
     * Test for 170574: CTS: JSF 2.2: jsf/api/javax_faces/context/externalcontext/URLClient_extContextGetInitParameterNPETest.jtr
     *
     * Make sure the exception is thrown
     *
     * @throws Exception
     */
    @Test
    public void testifExceptionThrown_nullInitParam() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "getInitParam.jsf");
            HtmlPage page = webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Response: " + page.asText());

            assertTrue(page.asText().contains("Check NPE: true"));
        }
    }

    /**
     * Test the c:forEach tag with varStatus assigned
     *
     * Ensure that a PropertyNotFoundExcpetion is not thrown
     *
     * @throws Exception
     */
    @Test
    public void testCForEachWithVarStatusAssigned() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "forEach-varStatus.jsf");
            HtmlPage page = webClient.getPage(url);

            // Page without clicking the button of the form called Change Items
            assertTrue(page.asText().contains("forEach varStatus assigned"));

            // Click button
            HtmlForm changedItemsForm1 = page.getFormByName("testCForEachForm");
            HtmlSubmitInput changeItemsButton1 = changedItemsForm1.getInputByName("changeItemsButton");
            HtmlPage changedItems1 = changeItemsButton1.click();

            String msgToSearchFor = "PropertyNotFoundException";
            List<String> msg = jsfTestServer1.findStringsInLogs(msgToSearchFor);

            assertFalse("PropertyNotFoundException found in logs ", !msg.isEmpty());
        }
    }
}
