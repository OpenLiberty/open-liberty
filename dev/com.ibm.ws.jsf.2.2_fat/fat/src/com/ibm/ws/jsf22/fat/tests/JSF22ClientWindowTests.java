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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Tests to execute on the jsfTestServer2 that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22ClientWindowTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22ClientWindow";

    protected static final Class<?> c = JSF22ClientWindowTests.class;

    @Server("jsfTestServer2")
    public static LibertyServer jsfTestServer2;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsfTestServer2, "JSF22ClientWindow.war", "com.ibm.ws.jsf22.fat.clientwindow");

        jsfTestServer2.startServer(JSF22ClientWindowTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer2 != null && jsfTestServer2.isStarted()) {
            jsfTestServer2.stopServer();
        }
    }

    /**
     * Check the ClientWindow ID retrieved from javascript when the page loads.
     * Then click a link and check that the client window id is set in the request parameter.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestSimpleLink() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("index.xhtml did not render properly.");
            }
            //get the window id from javascript
            HtmlElement clientWindowElement = (HtmlElement) page.getElementById("clientWindowDisplay");
            String clientWindowJS = clientWindowElement.asText();

            // Click link to execute the methods and update the page
            HtmlElement link = (HtmlElement) page.getElementById("testForm:link1");
            page = link.click();

            HtmlElement output = (HtmlElement) page.getElementById("testFormPage2:windowIdParam");

            // Look for the correct results
            assertTrue(page.asText().contains("Window ID from parameter: " + output.asText()));

            //check that the client window ids match
            assertTrue(clientWindowJS.equals(output.asText()));
        }
    }

    /**
     * Check the ClientWindow ID retrieved from javascript when the page loads.
     * Then click a link, which will open a new window, and check that the client window id is set in the request parameter.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestSimpleLinkNewWindow() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("index.xhtml did not render properly.");
            }
            //get the window id from javascript
            HtmlElement clientWindowElement = (HtmlElement) page.getElementById("clientWindowDisplay");
            String clientWindowJS = clientWindowElement.asText();

            // Click link to execute the methods and update the page
            HtmlElement link = (HtmlElement) page.getElementById("testForm:link2");
            page = link.click();

            HtmlElement output = (HtmlElement) page.getElementById("testFormPage2:windowIdParam");

            // Look for the correct results
            assertTrue(page.asText().contains("Window ID from parameter: " + output.asText()));
            //check that the client window ids match
            assertTrue(clientWindowJS.equals(output.asText()));
        }
    }

    /**
     * Click a link that has the disableClientWindow attribute set to true
     * and check that the client window id is not set in the request parameter.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestDisabledLink() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("index.xhtml did not render properly.");
            }
            // Click link to execute the methods and update the page
            HtmlElement link = (HtmlElement) page.getElementById("testForm:linkDisabled1");
            page = link.click();

            // Look for the "Test Passed".  The page2Disabled.xhtml page has the logic to compare the IDs.
            assertTrue(page.asText().contains("Test Passed"));
        }
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
        try (WebClient webClient = new WebClient()) {
            // Use a synchronizing ajax controller to allow proper ajax updating
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("index.xhtml did not render properly.");
            }

            //get the window id from javascript
            HtmlElement clientWindowElement = (HtmlElement) page.getElementById("clientWindowDisplay");
            String clientWindowJS = clientWindowElement.asText();

            // fill out fields
            HtmlTextInput input = (HtmlTextInput) page.getElementById("testForm:firstName");
            input.type("John", false, false, false);

            HtmlTextInput input2 = (HtmlTextInput) page.getElementById("testForm:lastName");
            input2.type("Doe", false, false, false);

            // Click link to execute the methods and update the page
            HtmlElement button = (HtmlElement) page.getElementById("testForm:buttonAjax1");

            page = button.click();

            HtmlElement firstName = (HtmlElement) page.getElementById("testForm:ajaxFirstName");
            HtmlElement lastName = (HtmlElement) page.getElementById("testForm:ajaxLastName");
            HtmlElement ajaxWindowId = (HtmlElement) page.getElementById("testForm:ajaxWindowId");

            // Look for the correct results
            assertTrue(firstName.asText().equals("John"));
            assertTrue(lastName.asText().equals("Doe"));
            assertTrue(ajaxWindowId.asText().equals(clientWindowJS));
        }
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
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("index.xhtml did not render properly.");
            }

            //get the window id from javascript
            HtmlElement clientWindowElement = (HtmlElement) page.getElementById("clientWindowDisplay");
            String clientWindowJS = clientWindowElement.asText();

            // fill out fields
            HtmlTextInput input = (HtmlTextInput) page.getElementById("testForm:firstName");
            input.type("Bill", false, false, false);
            HtmlTextInput input2 = (HtmlTextInput) page.getElementById("testForm:lastName");
            input2.type("Smith", false, false, false);

            // Click link to execute the methods and update the page
            HtmlElement button = (HtmlElement) page.getElementById("testForm:submitCommandButton1");

            page = button.click();

            HtmlElement firstName = (HtmlElement) page.getElementById("testFormPage2:firstName");
            HtmlElement lastName = (HtmlElement) page.getElementById("testFormPage2:lastName");
            HtmlElement windowIdBean = (HtmlElement) page.getElementById("testFormPage2:windowIdBean");

            // Look for the correct results
            assertTrue(firstName.asText().equals("Bill"));
            assertTrue(lastName.asText().equals("Smith"));
            assertTrue(windowIdBean.asText().equals(clientWindowJS));
        }
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
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("index.xhtml did not render properly.");
            }

            //get the window id from javascript
            HtmlElement clientWindowElement = (HtmlElement) page.getElementById("clientWindowDisplay");
            String clientWindowJS = clientWindowElement.asText();

            // fill out fields
            HtmlTextInput input = (HtmlTextInput) page.getElementById("testForm:firstName");
            input.type("Jane", false, false, false);
            HtmlTextInput input2 = (HtmlTextInput) page.getElementById("testForm:lastName");
            input2.type("Jones", false, false, false);

            // Click link to execute the methods and update the page
            HtmlElement button = (HtmlElement) page.getElementById("testForm:commandLink1");

            page = button.click();

            HtmlElement firstName = (HtmlElement) page.getElementById("testFormPage2:firstName");
            HtmlElement lastName = (HtmlElement) page.getElementById("testFormPage2:lastName");
            HtmlElement windowIdBean = (HtmlElement) page.getElementById("testFormPage2:windowIdBean");

            // Look for the correct results
            assertTrue(firstName.asText().equals("Jane"));
            assertTrue(lastName.asText().equals("Jones"));
            assertTrue(windowIdBean.asText().equals(clientWindowJS));
        }
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
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("index.xhtml did not render properly.");
            }

            //get the window id from javascript
            HtmlElement clientWindowElement = (HtmlElement) page.getElementById("clientWindowDisplay");
            String clientWindowJS = clientWindowElement.asText();

            // Click link to execute the methods and update the page
            HtmlElement button = (HtmlElement) page.getElementById("testForm:button1");

            page = button.click();

            HtmlElement windowIdParam = (HtmlElement) page.getElementById("testFormPage2:windowIdParam");
            HtmlElement windowIdBean = (HtmlElement) page.getElementById("testFormPage2:windowIdBean");

            // Look for the correct results
            assertTrue(windowIdBean.asText().equals(clientWindowJS));
            assertTrue(windowIdBean.asText().equals(windowIdParam.asText()));
        }
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
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("index.xhtml did not render properly.");
            }

            // Click link to execute the methods and update the page
            HtmlElement button = (HtmlElement) page.getElementById("testForm:button2Disabled");

            page = button.click();

            HtmlElement windowIdBean = (HtmlElement) page.getElementById("form2Disabled:outputWindowIdBean");

            // Look for the "Test Passed".  The page2Disabled.xhtml page has the logic to compare the IDs.
            assertTrue(page.asText().contains("Test Passed"));
            //We still should be able to get the id from the ExternalContext (in the bean), check to make sure that it isn't null.
            assertTrue(windowIdBean.asText() != null);
        }
    }

    /**
     * Load two base level pages, then click links in each one.
     * Ensure that the client window ids do not match.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ClientWindow_TestMultipleBasePages() throws Exception {
        try (WebClient webClient = new WebClient()) {

            //index.xhtml link
            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("index.xhtml did not render properly.");
            }

            // Click link to execute the methods and update the page
            HtmlElement link = (HtmlElement) page.getElementById("testForm:link1");
            page = link.click();

            HtmlElement output1 = (HtmlElement) page.getElementById("testFormPage2:windowIdParam");

            //index2.xhtml link
            url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index2.jsf");
            HtmlPage page2 = (HtmlPage) webClient.getPage(url);

            if (page2 == null) {
                Assert.fail("index2.xhtml did not render properly.");
            }

            // Click link to execute the methods and update the page
            HtmlElement link2 = (HtmlElement) page2.getElementById("testForm:link1");
            page2 = link2.click();

            HtmlElement output2 = (HtmlElement) page2.getElementById("testFormPage2:windowIdParam");
            //check that the client window ids do not match
            assertFalse(output1.asText().equals(output2.asText()));
        }
    }
}
