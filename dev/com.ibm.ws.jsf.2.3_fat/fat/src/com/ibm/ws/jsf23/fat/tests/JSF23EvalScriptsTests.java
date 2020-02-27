/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.JSFUtils;

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

    @Server("jsf23CDIServer")
    public static LibertyServer jsf23CDIServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "EvalScripts.war", "com.ibm.ws.jsf23.fat.evalscripts.beans");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server
        jsf23CDIServer.startServer(JSF23EvalScriptsTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23CDIServer != null && jsf23CDIServer.isStarted()) {
            jsf23CDIServer.stopServer();
        }
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
        try (WebClient webClient = new WebClient()) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "EvalScriptsSimple.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Get the form that we are dealing with
            HtmlForm form = page.getFormByName("form1");

            // Get the submit button
            HtmlSubmitInput submitButton = form.getInputByName("form1:button1");

            //click the button
            submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the getEvalScripts worked, the javascript should have populated the outputText field.
            assertTrue("Test failed. The javascript code from getEvalScripts was not called.", page.asText().contains("Test Passed!"));
        }
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
        try (WebClient webClient = new WebClient()) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "EvalScriptsList.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Get the form that we are dealing with
            HtmlForm form = page.getFormByName("form1");

            // Get the submit button
            HtmlSubmitInput submitButton = form.getInputByName("form1:button1");

            //click the button
            submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the getEvalScripts worked, the javascript should have populated the three outputText fields.
            assertTrue("Test failed. The javascript code from getEvalScripts was not called.", page.asText().contains("Text Value 1,Text Value 2,Text Value 3"));
        }
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
        try (WebClient webClient = new WebClient()) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "EvalScriptsFunction.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Get the form that we are dealing with
            HtmlForm form = page.getFormByName("form1");

            // Get the submit button
            HtmlSubmitInput submitButton = form.getInputByName("form1:button1");

            //click the button
            submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the getEvalScripts worked, the javascript should have populated the outputText field.
            assertTrue("Test failed. The javascript code from getEvalScripts was not called.", page.asText().contains("Function Called!"));
        }
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
        try (WebClient webClient = new WebClient()) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "EvalScriptsMultiFieldUpdate.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Get the form that we are dealing with
            HtmlForm form = page.getFormByName("form1");

            // Get the input text and submit button
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("form1:inputText1");
            HtmlSubmitInput submitButton = form.getInputByName("form1:button1");

            // Fill the input text
            inputText.setValueAttribute("test");

            //click the button
            submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the getEvalScripts worked, the javascript should have populated the outputText field and ajax should have update the outputText4.
            assertTrue("Test failed. The javascript code from getEvalScripts was not called.", page.asText().contains("Test Passed!,test"));
        }
    }
}
