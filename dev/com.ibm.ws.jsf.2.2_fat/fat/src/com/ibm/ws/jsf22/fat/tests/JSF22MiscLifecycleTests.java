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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlHiddenInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsfTestServer1 that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22MiscLifecycleTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22ActionListener";

    protected static final Class<?> c = JSF22MiscLifecycleTests.class;

    @Server("jsfTestServer1")
    public static LibertyServer jsfTestServer1;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsfTestServer1, "JSF22ActionListener.war", "com.ibm.ws.jsf22.fat.actionlistener.*");

        jsfTestServer1.startServer(JSF22MiscLifecycleTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer1 != null && jsfTestServer1.isStarted()) {
            jsfTestServer1.stopServer();
        }
    }

    /**
     * Test a simple form that has an ActionListenerWrapper attached to the commandButton.
     * Check the logs for the messages printed out in the ActionListenerWrapper and ActionListener classes.
     * https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-566
     *
     * @throws Exception
     */
    @Test
    public void testActionListenerWrapper() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testActionListener.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /JSF22ActionListener/testActionListener.jsf");
            HtmlTextInput input = (HtmlTextInput) page.getElementById("testForm:value2");
            input.type("2", false, false, false);

            HtmlElement button = (HtmlElement) page.getElementById("testForm:button1");
            page = button.click();

            List<String> actionListenerWrapper = jsfTestServer1.findStringsInLogs("TestActionListenerWrapper.getWrapped()");
            List<String> actionListener = jsfTestServer1.findStringsInLogs("TestActionListener.processAction()");

            assertNotNull(actionListenerWrapper);
            assertNotNull(actionListener);
        }
    }

    /**
     * Test a simple form that clears out a value then clicks submit. One field is required so there is a validation
     * message that is returned. Simply check that the values are in the input fields are still empty.
     *
     * @throws Exception
     */
    @Test
    public void testUIOutputNull() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "simpleForm.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /JSF22ActionListener/simpleForm.jsf");
            HtmlTextInput input = (HtmlTextInput) page.getElementById("testForm:value2");
            input.setText("");

            HtmlElement button = (HtmlElement) page.getElementById("testForm:button1");
            page = button.click();

            HtmlElement value1 = (HtmlElement) page.getElementById("testForm:value1");
            HtmlElement value2 = (HtmlElement) page.getElementById("testForm:value2");
            Log.info(c, name.getMethodName(), "Value for value1 field: " + value1);
            Log.info(c, name.getMethodName(), "Value for value2 field: " + value2);

            assertEquals("", value1.asText());
            assertEquals("", value2.asText());
        }
    }

    /**
     * Test a basic JSF page that the ViewState hidden field has an id that is not javax.faces.ViewState
     * This is from jira https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-220
     *
     * @throws Exception
     */
    @Test
    public void testViewStateId() throws Exception {
        try (WebClient webClient = new WebClient()) {

            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "simpleForm.jsf");

            Log.info(c, name.getMethodName(), "Navigating to: /JSF22ActionListener/simpleForm.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            Log.info(c, name.getMethodName(), "Page output: " + page.asXml());

            HtmlHiddenInput hidden;
            if (JakartaEE9Action.isActive()) {
                hidden = (HtmlHiddenInput) page.getElementByName("jakarta.faces.ViewState");
                Log.info(c, name.getMethodName(), "The ViewState hidden field has an id of: " + hidden.getAttribute("id"));
                assertFalse("jakarta.faces.ViewState".equals(hidden.getAttribute("id")));
            } else {
                hidden = (HtmlHiddenInput) page.getElementByName("javax.faces.ViewState");
                Log.info(c, name.getMethodName(), "The ViewState hidden field has an id of: " + hidden.getAttribute("id"));
                assertFalse("javax.faces.ViewState".equals(hidden.getAttribute("id")));
            }
        }
    }
}
