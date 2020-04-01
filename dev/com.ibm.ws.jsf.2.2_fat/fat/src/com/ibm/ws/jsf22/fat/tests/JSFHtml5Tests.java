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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsfTestServer1 that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSFHtml5Tests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22HTML5";

    protected static final Class<?> c = JSFHtml5Tests.class;

    @Server("jsfTestServer1")
    public static LibertyServer jsfTestServer1;

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(jsfTestServer1, "JSF22HTML5.war", "com.ibm.ws.jsf22.fat.html5.*");

        jsfTestServer1.startServer(JSFHtml5Tests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer1 != null && jsfTestServer1.isStarted()) {
            jsfTestServer1.stopServer();
        }
    }

    /**
     * Test if the input text box is properly processed by JSF
     * Here is what the tag in the xhtml will look like:
     * <input type="text" jsf:id="testFieldOne"
     * placeholder="TestData"
     * jsf:value="#{html5TestBean.inputTestValue}"
     * data-test="DataTested"
     * type="email" />
     *
     * @throws Exception
     */
    @Test
    public void testHtml5TextBox() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "Html5TextBox.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF2.2/Html5TextBox.jsf");

            //Test whether the h:inputText was translated to an <input> element
            DomElement inputElement = page.getElementById("html5jsfTest:testFieldOne");

            Log.info(c, name.getMethodName(), "Test whether the pass through element was properly added to the <input> element");
            //Test that the placeholder pass through element was properly added as an attribute to <input>
            String inputPlaceholder = inputElement.getAttribute("placeholder");
            Log.info(c, name.getMethodName(), "<input> placeholder attribute : " + inputPlaceholder);
            assertEquals("TestData", inputPlaceholder);

            Log.info(c, name.getMethodName(), "Test whether the pass through element was properly added to the <input> element");
            //Test that the type pass through element was properly added as an attribute to <input>
            String inputType = inputElement.getAttribute("type");
            Log.info(c, name.getMethodName(), "<input> type attribute : " + inputType);
            assertEquals("email", inputType);

            Log.info(c, name.getMethodName(), "Test whether the pass through attributes were properly added to the <input> element");
            //Test that the type pass through attributes was properly added as an attribute to <input>
            String inputDataTest = inputElement.getAttribute("data-test");
            Log.info(c, name.getMethodName(), "<input> data-test attribute : " + inputDataTest);
            assertEquals("DataTested", inputDataTest);
        }
    }

    /**
     * Test if the <h:inputText> tag is translated into a <input> tag by JSF
     * This JSF tag will utilize the passthroughElement feature new to JSF 2.2
     * Here is what the tag in the xhtml will look like:
     * <h:inputText id="testFieldOne">
     * <f:passThroughAttribute name="placeholder" value="TestData" />
     * <f:passThroughAttribute name="type" value="email" />
     * </h:inputText>
     *
     * @throws Exception
     */
    @Test
    public void testHtml5PassThroughElement() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "Html5PassThroughElement.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF2.2/Html5TextBoxPassThroughElement.jsf");

            //Test whether the h:inputText was translated to an <input> element
            DomElement inputElement = page.getElementById("html5jsfTest:testFieldOne");

            Log.info(c, name.getMethodName(), "Test whether the pass through element was properly added to the <input> element");
            //Test that the placeholder pass through element was properly added as an attribute to <input>
            String inputPlaceholder = inputElement.getAttribute("placeholder");
            Log.info(c, name.getMethodName(), "<input> placeholder attribute : " + inputPlaceholder);
            assertEquals("TestData", inputPlaceholder);

            Log.info(c, name.getMethodName(), "Test whether the pass through element was properly added to the <input> element");
            //Test that the type pass through element was properly added as an attribute to <input>
            String inputType = inputElement.getAttribute("type");
            Log.info(c, name.getMethodName(), "<input> type attribute : " + inputType);
            assertEquals("email", inputType);

            Log.info(c, name.getMethodName(), "Test whether the pass through attributes were properly added to the <input> element");
            //Test that the type pass through attributes was properly added as an attribute to <input>
            String inputDataTest = inputElement.getAttribute("data-test");
            Log.info(c, name.getMethodName(), "<input> data-test attribute : " + inputDataTest);
            assertEquals("DataTested", inputDataTest);
        }
    }

    /**
     * Test if the <h:inputText> tag is translated into a HTML 5 <input> tag by JSF
     * This JSF tag will utilize the passthroughElement feature new to JSF 2.2
     * Here is what the tag in the xhtml will look like:
     * <h:inputText id="testFieldOne">
     * <f:passThroughAttributes value="#{html5TestBean.testPassthroughAttributesList}" />
     * </h:inputText>
     * These are the attributes added in the bean:
     * obj.put("placeholder", "TestData");
     * obj.put("type", "email");
     * obj.put("data-test", "DataTested");
     *
     * @throws Exception
     */
    @Test
    public void testHtml5PassThroughAttributes() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "Html5PassThroughAttributes.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF2.2/Html5PassThroughAttributes.jsf");

            //Test whether the h:inputText was translated to an <input> element
            DomElement inputElement = page.getElementById("html5jsfTest:testFieldOne");

            Log.info(c, name.getMethodName(), "Test whether the pass through attributes were properly added to the <input> element");
            //Test that the placeholder pass through attributes were properly added as an attribute to <input>
            String inputPlaceholder = inputElement.getAttribute("placeholder");
            Log.info(c, name.getMethodName(), "<input> placeholder attribute : " + inputPlaceholder);
            assertEquals("TestData", inputPlaceholder);

            Log.info(c, name.getMethodName(), "Test whether the pass through attributes were properly added to the <input> element");
            //Test that the type pass through attributes was properly added as an attribute to <input>
            String inputType = inputElement.getAttribute("type");
            Log.info(c, name.getMethodName(), "<input> type attribute : " + inputType);
            assertEquals("email", inputType);

            Log.info(c, name.getMethodName(), "Test whether the pass through attributes were properly added to the <input> element");
            //Test that the type pass through attributes were properly added as an attribute to <input>
            String inputDataTest = inputElement.getAttribute("data-test");
            Log.info(c, name.getMethodName(), "<input> data-test attribute : " + inputDataTest);
            assertEquals("DataTested", inputDataTest);
        }
    }

    /**
     * Test if the <h:inputText> tag is translated into a HTML 5 <input> tag by JSF
     * This JSF tag will utilize the passthroughElement feature new to JSF 2.2
     * Here is what the tag in the xhtml will look like:
     * <h:inputText id="testFieldOne"
     * p:placeholder="TestData"
     * p:type="email"
     * p:data-test="DataTested" />
     * The p: attributes are the pass through attributes
     *
     * @throws Exception
     */
    @Test
    public void testHtml5PassThroughAttribute() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "Html5PassThroughAttribute.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF2.2/Html5PassThroughAttribute.jsf");

            //Test whether the h:inputText was translated to an <input> element
            DomElement inputElement = page.getElementById("html5jsfTest:testFieldOne");

            Log.info(c, name.getMethodName(), "Test whether the pass through attribute was properly added to the <input> element");
            //Test that the placeholder pass through attribute was properly added as an attribute to <input>
            String inputPlaceholder = inputElement.getAttribute("placeholder");
            Log.info(c, name.getMethodName(), "<input> placeholder attribute : " + inputPlaceholder);
            assertEquals("TestData", inputPlaceholder);

            Log.info(c, name.getMethodName(), "Test whether the pass through attribute was properly added to the <input> element");
            //Test that the type pass through attribute was properly added as an attribute to <input>
            String inputType = inputElement.getAttribute("type");
            Log.info(c, name.getMethodName(), "<input> type attribute : " + inputType);
            assertEquals("email", inputType);

            Log.info(c, name.getMethodName(), "Test whether the pass through attribute was properly added to the <input> element");
            //Test that the type pass through attribute was properly added as an attribute to <input>
            String inputDataTest = inputElement.getAttribute("data-test");
            Log.info(c, name.getMethodName(), "<input> data-test attribute : " + inputDataTest);
            assertEquals("DataTested", inputDataTest);
        }
    }

    /**
     * This testcase will test the fix for 167452: Update HTML5 tests to test "id" attribute
     * which must be supported for all component elements
     *
     *
     * @throws Exception
     */
    @Test
    public void testHtml5_ID() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "Html5TestID.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "testHtml5_ID:: page " + page.asXml());

            HtmlElement head = page.getHtmlElementById("testHeadID");
            Log.info(c, name.getMethodName(), "testHtml5_ID:: head " + head.asXml());
            assertTrue(head.asXml().contains("Test id"));

            HtmlElement body = page.getHtmlElementById("testBodyID");
            Log.info(c, name.getMethodName(), "testHtml5_ID:: body " + body.asXml());
            assertTrue(body.asXml().contains("testForm"));
        }
    }

    /**
     * This testcase is added to test fix for 169345: Port MYFACES-3947, Passthrough Element textarea doesn't work.
     * If the fix is not in place then the page will not be rendered and following message can be seen in response
     *
     * /PasthroughElementTextarea.xhtml at line 15 and column 62 <textarea>
     * Tag Library supports namespace: http://xmlns.jcp.org/jsf/html, but no tag was defined for name: inputTextArea
     *
     * @throws Exception
     */
    @Test
    public void testHtml5_PasthroughTextarea() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "PasthroughElementTextarea.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "testHtml5_PasthroughTextarea:: page " + page.asXml());
            assertTrue(page.asText().contains("PasthroughElementTextarea page rendered"));

            // Get the form that we are dealing with and within that form
            HtmlForm form = page.getFormByName("complainForm");
            HtmlTextArea wordTextArea = form.getTextAreaByName("complainForm:message");
            //HtmlTextArea wordTextArea = form.getTextAreaByName("comments");
            wordTextArea.setText("Textarea goes to bean");

            Log.info(c, name.getMethodName(), "TextArea value : " + wordTextArea.getText() + ", XML : " + wordTextArea.asXml());

            HtmlElement button = form.getButtonByName("complainForm:mybutton");
            HtmlPage messagePage = button.click();
            Log.info(c, name.getMethodName(), "Page after the submit click , testHtml5_PasthroughTextarea page:: " + messagePage.asXml());

            String msgToSearchFor = "Textarea goes to bean";
            Log.info(c, name.getMethodName(), "Looking for : " + msgToSearchFor);
            // Check the log file
            String istextareaValueinBean = jsfTestServer1.waitForStringInLog(msgToSearchFor);

            Log.info(c, name.getMethodName(), "Message found after searching logs : " + istextareaValueinBean);
            // There should be a match so fail if there is not.
            assertNotNull("The following message was not found in the trace log: " + msgToSearchFor, istextareaValueinBean);
        }
    }
}
