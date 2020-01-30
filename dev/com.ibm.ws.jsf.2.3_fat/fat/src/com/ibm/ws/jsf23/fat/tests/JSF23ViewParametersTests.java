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
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * JSF 2.3 test cases for testing the NotNull annotation in bean properties that corresponds to f:viewParam values.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF23ViewParametersTests {

    protected static final Class<?> c = JSF23ViewParametersTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23CDIBVServer")
    public static LibertyServer jsf23CDIBVServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23CDIBVServer, "JSF23ViewParameters.war", "com.ibm.ws.jsf23.fat.viewparameters.beans");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server.
        jsf23CDIBVServer.startServer(JSF23ViewParametersTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23CDIBVServer != null && jsf23CDIBVServer.isStarted()) {
            jsf23CDIBVServer.stopServer();
        }
    }

    /**
     * This test case ensures that the NotNull annotation on the backing bean is enforced when used with a viewParam component
     * If successful, the validation message will be displayed.
     * This test has the context parameter javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL set to true.
     *
     * @throws Exception
     */
    @Test
    public void testViewParamNotNull() throws Exception {
        String contextRoot = "JSF23ViewParameters";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIBVServer, contextRoot, "ViewParameters.xhtml?textParam1=&textParam2=test2&textParam3=test3");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The validation message was not displayed.", page.asText().contains("textParam1: must not be null"));
        }
    }

    /**
     * This test case ensures that the required attribute textParam2 viewParam takes precedence over the
     * NotNull annotation on the backing bean.
     * If successful, the validation message will be displayed.
     * This test has the context parameter javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL set to true.
     *
     * @throws Exception
     */
    @Test
    public void testViewParamRequiredAttribute() throws Exception {
        String contextRoot = "JSF23ViewParameters";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIBVServer, contextRoot, "ViewParameters.xhtml?textParam1=test1&textParam2=&textParam3=test3");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The validation message was not displayed.", page.asText().contains("textParam2: Validation Error: Value is required."));
        }
    }

    /**
     * This test case ensures that the viewParam textParam3 has no issues when a null value is submitted.
     * Since there is no NotNull annotation, nor is the required attribute set, the textParam3 can be set to null.
     * If successful, the values of textParam1 and textParam2 will be displayed.
     * This test has the context parameter javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL set to true.
     *
     * @throws Exception
     */
    @Test
    public void testViewParamAllowNull() throws Exception {
        String contextRoot = "JSF23ViewParameters";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIBVServer, contextRoot, "ViewParameters.xhtml?textParam1=test1&textParam2=test2&textParam3=");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The textParam3 was not set to null", page.asText().contains("test1|test2|"));
        }
    }
}
