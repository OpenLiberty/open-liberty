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
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * These test are to test the functionality of the <ui:repeat> begin and end attributes
 * new to JSF 2.3.
 *
 */
@RunWith(FATRunner.class)
public class JSF23UIRepeatConditionTests {

    protected static final Class<?> c = JSF23UIRepeatConditionTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23CDIServer")
    public static LibertyServer jsf23CDIServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "UIRepeatConditionCheck.war", "com.ibm.ws.jsf23.fat.uirepeat");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server.
        jsf23CDIServer.startServer(JSF23UIRepeatConditionTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23CDIServer != null && jsf23CDIServer.isStarted()) {
            jsf23CDIServer.stopServer();
        }
    }

    /**
     * A test to ensure that UIRepeat supports the begin and end attributes
     *
     * @throws Exception
     */
    @Test
    public void testUIRepeatCondition() throws Exception {
        String contextRoot = "UIRepeatConditionCheck";
        try (WebClient webClient = new WebClient()) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());

            // The initial value expected
            String expected = "0123456789";

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "UIRepeatBeginEnd.jsf");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlTextInput beginInput = (HtmlTextInput) page.getElementById("beginInput");
            HtmlTextInput endInput = (HtmlTextInput) page.getElementById("endInput");
            HtmlTextInput stepInput = (HtmlTextInput) page.getElementById("stepInput");

            String output = page.getElementById("panel1").getTextContent().replaceAll("\\s", "");

            // Test the inital output for the default values of begin = 0, end = 9 and step = 1
            assertTrue("The output should have been: " + expected + " but was: " + output, output.equals(expected));

            // Set step = 2 and ensure we get the proper output
            expected = "02468";
            stepInput.setValueAttribute("2");

            // Now click the submit button
            page = page.getElementById("button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            output = page.getElementById("panel1").getTextContent().replaceAll("\\s", "");

            // Ensure that the resulting output is correct
            assertTrue("The output should have been: " + expected + " but was: " + output, output.equals(expected));

            // Set step = 1, begin = 4 and end = 6 and ensure we get the proper output
            expected = "456";
            stepInput.setValueAttribute("1");
            beginInput.setValueAttribute("4");
            endInput.setValueAttribute("6");

            // Now click the submit button
            page = page.getElementById("button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            output = page.getElementById("panel1").getTextContent().replaceAll("\\s", "");

            // Ensure that the resulting output is correct
            assertTrue("The output should have been: " + expected + " but was: " + output, output.equals(expected));
        }
    }

    /**
     * Test to ensure that if end is specified to be too large an error occurs.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException" })
    public void testUIRepeatConditionErrorEndTooLarge() throws Exception {
        String contextRoot = "UIRepeatConditionCheck";
        String errorText = "end cannot be greater than collection size";

        try (WebClient webClient = new WebClient()) {

            // Ensure the test does not fail due to the error condition we are creating
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            jsf23CDIServer.addIgnoredErrors(Arrays.asList("SRVE0777E:.*", "SRVE0315E:.*"));

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "UIRepeatEndTooLarge.jsf");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The following error was not found on the page: " + errorText, page.asText().contains(errorText));
        }
    }
}
