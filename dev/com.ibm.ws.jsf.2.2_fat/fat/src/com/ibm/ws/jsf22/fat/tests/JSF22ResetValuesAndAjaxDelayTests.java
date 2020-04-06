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

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsf22TracingServer that use HtmlUnit. jsf22TracingServer
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22ResetValuesAndAjaxDelayTests {
    public TestName name = new TestName();

    String contextRoot = "TestJSF22Ajax";

    @Server("jsf22TracingServer")
    public static LibertyServer jsf22TracingServer;

    protected static final Class<?> c = JSF22ResetValuesAndAjaxDelayTests.class;

    private static BrowserVersion browser = BrowserVersion.CHROME;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf22TracingServer, "TestJSF22Ajax.war", "com.ibm.ws.jsf22.fat.ajax.ajaxDelay", "com.ibm.ws.jsf22.fat.ajax.resetValue");

        jsf22TracingServer.startServer(JSF22ResetValuesAndAjaxDelayTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf22TracingServer != null && jsf22TracingServer.isStarted()) {
            jsf22TracingServer.stopServer();
        }
    }

    /**
     * Test the ajax resetValues attribute and also the f:resetValues component
     *
     * @throws Exception
     */
    @Test
    public void testResetValues() throws Exception {
        try (WebClient webClient = new WebClient(browser)) {

            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            URL url = JSFUtils.createHttpUrl(jsf22TracingServer, contextRoot, "resetValuesTest.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22Ajax/resetValuesTest.jsf");
            HtmlElement link = (HtmlElement) page.getElementById("form1:link1");
            page = link.click();

            HtmlElement checkValue = (HtmlElement) page.getElementById("form1:input1");

            Log.info(c, name.getMethodName(), "The input1 field should have a value of 1, actual: " + checkValue.asText());
            assertEquals("1", checkValue.asText());

            HtmlElement saveButton = (HtmlElement) page.getElementById("form1:saveButton");
            page = saveButton.click();

            HtmlElement checkMessage = (HtmlElement) page.getElementById("form1:messages");
            Log.info(c, name.getMethodName(), "On save, the validation should have failed.  Message displayed: " + checkMessage.asText());
            assertNotNull("A validation error should have been displayed", checkMessage.asText());

            //click the link again, the value should still increment which means the Ajax reset is working
            page = link.click();
            checkValue = (HtmlElement) page.getElementById("form1:input1");

            Log.info(c, name.getMethodName(), "The input1 field should have a value of 2, actual: " + checkValue.asText());
            assertEquals("2", checkValue.asText());

            //click the resetButton and ensure the fields are reset to 0 each, which means the f:resetValues component is working.
            HtmlElement resetButton = (HtmlElement) page.getElementById("form1:resetButton");
            page = resetButton.click();

            checkValue = (HtmlElement) page.getElementById("form1:input1");

            Log.info(c, name.getMethodName(), "The input1 field should have been reset to 0, actual: " + checkValue.asText());
            assertEquals("0", checkValue.asText());

            HtmlElement checkValue2 = (HtmlElement) page.getElementById("form1:input2");

            Log.info(c, name.getMethodName(), "The input2 field should have been reset to 0, actual: " + checkValue2.asText());
            assertEquals("0", checkValue2.asText());
        }
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

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22Ajax/ajaxDelayTest.jsf");
            URL url = JSFUtils.createHttpUrl(jsf22TracingServer, contextRoot, "ajaxDelayTest.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Returned from navigating to: /TestJSF22Ajax/ajaxDelayTest.jsf and setting mark in logs.");

            Log.info(c, name.getMethodName(), "Sleeping for 1 second");
            Thread.sleep(1000);

            Log.info(c, name.getMethodName(), "Getting input field");
            HtmlTextInput input = (HtmlTextInput) page.getElementById("form1:name");

            Log.info(c, name.getMethodName(), "Typing value 'joh'");
            input.type("joh", false, false, false);

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
     */

    @Test
    public void testAjaxZeroDelay() throws Exception {
        try (WebClient webClient = new WebClient(browser)) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            jsf22TracingServer.setMarkToEndOfLog();

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22Ajax/ajaxZeroDelayTest.jsf");
            URL url = JSFUtils.createHttpUrl(jsf22TracingServer, contextRoot, "ajaxZeroDelayTest.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Returned from navigating to: /TestJSF22Ajax/ajaxDelayTest.jsf and setting mark in logs.");

            Log.info(c, name.getMethodName(), "Sleeping for 1 second");
            Thread.sleep(1000);

            Log.info(c, name.getMethodName(), "Getting input field");
            HtmlTextInput input = (HtmlTextInput) page.getElementById("form1:name");

            Log.info(c, name.getMethodName(), "Typing value 'joh'");
            input.type("joh", false, false, false);

            Log.info(c, name.getMethodName(), "Checking logs for bean call entry");
            int numOfMethodCalls = jsf22TracingServer.waitForMultipleStringsInLogUsingMark(3, "AjaxDelayTest getMatchingEmployees");

            Log.info(c, name.getMethodName(), "The bean method should have been called three times, actual amount is: " + numOfMethodCalls);
            assertEquals(3, numOfMethodCalls);
        }
    }

    /**
     * Test an Ajax request with a delay of none and make sure the method is called on each keyup (3 times).
     *
     * @throws Exception
     */

    /*
     * CURRENTLY DOES NOT WORK IN MYFACES. ALSO THERE'S AN HTMLUNIT BUG BECAUSE "NONE" IS NOT ALLOWED AS AN ATTRIBUTE VALUE.
     * WHEN DEFECTS 168751 AND 168757 ARE RESOLVED, UNCOMMENT THIS TEST.
     */

    //   @Test
    //   public void testAjaxDelayNone() throws Exception {
    //   WebClient webClient = new WebClient();
    //   webClient.setAjaxController(new NicelyResynchronizingAjaxController());

    //   URL url = JSFUtils.createHttpUrl(jsf22TracingServer, contextRoot, "ajaxDelayTestNone.jsf");
    //   HtmlPage page = (HtmlPage) webClient.getPage(url);

    //   Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22Ajax/ajaxDelayTestNone.jsf");
    //   jsf22TracingServer.setMarkToEndOfLog();
    //   HtmlTextInput input = (HtmlTextInput) page.getElementById("form1:name");
    //   input.type("joh", false, false, false);

    //   int numOfMethodCalls = jsf22TracingServer.waitForMultipleStringsInLogUsingMark(3, "AjaxDelayTest getMatchingEmployees");
    //   jsf22TracingServer.setMarkToEndOfLog();

    //   Log.info(c, name.getMethodName(), "The bean method should have been called three times, actual amount is: " + numOfMethodCalls);
    //   assertEquals(3, numOfMethodCalls);
    //  }

}