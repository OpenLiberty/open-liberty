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

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * A test class to test UISelectMany with <h:selectManyCheckbox/> component and multiple data structures.
 *
 * Spec issue: https://github.com/javaee/javaserverfaces-spec/issues/1422
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF23UISelectManyTests {

    protected static final Class<?> c = JSF23UISelectManyTests.class;
    String contextRoot = "JSF23UISelectMany";

    @Rule
    public TestName name = new TestName();

    @Server("jsf23CDIServer")
    public static LibertyServer jsf23CDIServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "JSF23UISelectMany.war", "com.ibm.ws.jsf23.fat.uiselectmany");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server.
        jsf23CDIServer.startServer(JSF23UISelectManyTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23CDIServer != null && jsf23CDIServer.isStarted()) {
            jsf23CDIServer.stopServer();
        }
    }

    /**
     * This test case will ensure that <h:selectManyCheckbox/> component can be populated with Enum.
     * Also verify the checkboxs can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testUISelectMany_Enum() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "selectManyCheckboxEnum.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectManyCheckbox: Enum"));

            // Get the checkboxs
            HtmlCheckBoxInput checkbox0 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:0");
            HtmlCheckBoxInput checkbox2 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:2");
            HtmlCheckBoxInput checkbox4 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:4");
            // Mark them as checked
            checkbox0.setChecked(true);
            checkbox2.setChecked(true);
            checkbox4.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the checkboxs again after submitting the form
            checkbox0 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:0");
            checkbox2 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:2");
            checkbox4 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:4");

            // Verify that they remained checked
            assertTrue("Checkbox 0 is not checked.", checkbox0.isChecked());
            assertTrue("Checkbox 2 is not checked.", checkbox2.isChecked());
            assertTrue("Checkbox 4 is not checked.", checkbox4.isChecked());

            assertTrue("Selected values were not found.", resultingPage.contains("Selected Values: [A, C, E]"));
        }
    }

    /**
     * This test case will ensure that <h:selectManyCheckbox/> component can be populated with SelectItems.
     * Also verify the checkboxs can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testUISelectMany_SelectItems() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "selectManyCheckboxSelectItems.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectManyCheckbox: selectItems"));

            // Get the checkboxs
            HtmlCheckBoxInput checkbox1 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:1");
            HtmlCheckBoxInput checkbox2 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:2");
            // Mark them as checked
            checkbox1.setChecked(true);
            checkbox2.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the checkboxs again after submitting the form
            checkbox1 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:1");
            checkbox2 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:2");

            // Verify that they remained checked
            assertTrue("Checkbox 1 is not checked.", checkbox1.isChecked());
            assertTrue("Checkbox 2 is not checked.", checkbox2.isChecked());

            assertTrue("Selected items were not found.", resultingPage.contains("Selected Items: [Item2, Item3]"));
        }
    }

    /**
     * This test case will ensure that <h:selectManyCheckbox/> component can be populated with Static values.
     * Also verify the checkboxs can be checked, the value is propagated to the bean and
     * it stays checked after submitting the form.
     *
     * @throws Exception
     */
    @Test
    public void testUISelectMany_Static() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "selectManyCheckboxStatic.xhtml");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            String resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            assertTrue("The page was not rendered correctly.", resultingPage.contains("JSF 2.3 SelectManyCheckbox: Static"));

            // Get the checkboxs
            HtmlCheckBoxInput checkbox0 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:0");
            HtmlCheckBoxInput checkbox1 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:1");
            HtmlCheckBoxInput checkbox3 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:3");
            // Mark them as checked
            checkbox0.setChecked(true);
            checkbox1.setChecked(true);
            checkbox3.setChecked(true);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) testPage.getElementById("submitButton");
            testPage = submitButton.click();

            resultingPage = testPage.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Get the checkboxs again after submitting the form
            checkbox0 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:0");
            checkbox1 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:1");
            checkbox3 = (HtmlCheckBoxInput) testPage.getElementById("checkboxId:3");

            // Verify that they remained checked
            assertTrue("Checkbox 0 is not checked.", checkbox0.isChecked());
            assertTrue("Checkbox 1 is not checked.", checkbox1.isChecked());
            assertTrue("Checkbox 3 is not checked.", checkbox3.isChecked());

            assertTrue("Static selected items were not found.", resultingPage.contains("Static Selected Items: [1, 2, 4]"));
        }
    }
}
