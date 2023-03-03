/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertTrue;
import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;

import java.net.URL;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * JSF 2.3 test cases for the tag h:commandScript.
 */
@RunWith(FATRunner.class)
public class JSF23CommandScriptTests {

    protected static final Class<?> c = JSF23CommandScriptTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23CommandScriptServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "CommandScript.war",
                                      "com.ibm.ws.jsf23.fat.commandscript.beans",
                                      "com.ibm.ws.jsf23.fat.commandscript.listener");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server
        server.startServer(JSF23CommandScriptTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * This test case ensures that the commandScript is called when the page loads because of the autorun attribute.
     * This test also verifies that a default value for the execute attribute will be used since the attribute is not specified.
     * If successful, the bean will be called and a success message will be displayed.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat(EE10_FEATURES)  // Skipped due to HTMLUnit / JavaScript Incompatabilty (New JS in RC5)
    public void testCommandScriptAutorunDefaultExecute() throws Exception {
        String contextRoot = "CommandScript";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "JSF23CommandScriptAutorunDefaultExecute.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);
            webClient.waitForBackgroundJavaScript(10000);
            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the commandScript code works properly the success message will be displayed on the page.
            assertTrue("The commandScript test failed, success not displayed.", page.asText().contains("The value of output is: success"));
        }
    }

    /**
     * This test case ensures that the commandScript is called when the page loads because of the autorun attribute.
     * If successful, the bean will be called and a success message will be displayed.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat(EE10_FEATURES)  // Skipped due to HTMLUnit / JavaScript Incompatabilty (New JS in RC5)
    public void testCommandScriptAutorun() throws Exception {
        String contextRoot = "CommandScript";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "JSF23CommandScriptAutorun.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);
            webClient.waitForBackgroundJavaScript(10000);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the commandScript code works properly the success message will be displayed on the page.
            assertTrue("The commandScript test failed, success not displayed.", page.asText().contains("The value of output is: success"));
        }
    }

    /**
     * This test case ensures that the commandScript is called when the page loads because of the autorun attribute.
     * Also, the f:actionListener is nested and the test verifies that the listener is called.
     * If successful, the bean will be called and a success message will be displayed.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat(EE10_FEATURES)  // Skipped due to HTMLUnit / JavaScript Incompatabilty (New JS in RC5)
    public void testCommandScriptActionListener() throws Exception {
        String contextRoot = "CommandScript";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "JSF23CommandScriptActionListener.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);
            webClient.waitForBackgroundJavaScript(10000);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the commandScript code works properly the success message will be displayed on the page.
            assertTrue("The commandScript test failed, success not displayed.", page.asText().contains("The value of output is: success"));

            //verify that the message from the listener is in the log file.
            List<String> result = server.findStringsInLogs("CommandScriptActionListener.processAction called");
            assertTrue("The ActionListener was not called.", result.size() == 1);
        }
    }

    /**
     * This test case ensures that the commandScript is called when the page loads because of the autorun attribute.
     * Also, the actionListener is added to the commandScript tag and the test verifies that the method is called.
     * If successful, the bean will be called and a success message will be displayed.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat(EE10_FEATURES)  // Skipped due to HTMLUnit / JavaScript Incompatabilty (New JS in RC5)
    public void testCommandScriptActionListenerAttr() throws Exception {
        String contextRoot = "CommandScript";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "JSF23CommandScriptActionListenerAttr.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);
            webClient.waitForBackgroundJavaScript(10000);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the commandScript code works properly the success message will be displayed on the page.
            assertTrue("The commandScript test failed, success not displayed.", page.asText().contains("The value of output is: success"));

            //verify that the message from the listener is in the log file.
            List<String> result = server.findStringsInLogs("performAction called");
            assertTrue("The ActionListener was not called.", result.size() == 1);
        }
    }

    /**
     * This test case ensures that the commandScript is called when the page loads because of the autorun attribute.
     * Also, there is a nested f:param component. The test will ensure the parameters are passed to the bean.
     * If successful, the bean will be called and param values will be displayed.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat(EE10_FEATURES)  // Skipped due to HTMLUnit / JavaScript Incompatabilty (New JS in RC5)
    public void testCommandScriptParam() throws Exception {
        String contextRoot = "CommandScript";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "JSF23CommandScriptParam.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);
            webClient.waitForBackgroundJavaScript(10000);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the commandScript code works properly the parameter values will be displayed on the page.
            assertTrue("The commandScript test failed, parameter values not displayed.", page.asText().contains("The value of output is: Value1 Value2"));
        }
    }

    /**
     * This test case ensures that the commandScript is called when the button is clicked.
     * If successful, the bean will add a message that will be displayed on the page.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat(EE10_FEATURES)  // Skipped due to HTMLUnit / JavaScript Incompatabilty (New JS in RC5)
    public void testCommandScriptButton() throws Exception {
        String contextRoot = "CommandScript";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "JSF23CommandScriptButton.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Now click the submit button
            page.getElementById("button1").click();
            webClient.waitForBackgroundJavaScript(10000);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the commandScript code works properly the parameter values will be displayed on the page.
            assertTrue("The commandScript test failed, parameter values not displayed.", page.asText().contains("submitForm called"));
        }
    }
}
