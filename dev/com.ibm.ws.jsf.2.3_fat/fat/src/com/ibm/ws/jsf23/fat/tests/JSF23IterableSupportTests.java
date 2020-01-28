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
import componenttest.topology.impl.LibertyServer;

/**
 * A set of test cases to ensure Iterable is supported by
 * <ui:repeat/> as well as UIData.
 *
 */
@RunWith(FATRunner.class)
public class JSF23IterableSupportTests {

    protected static final Class<?> c = JSF23IterableSupportTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23CDIServer")
    public static LibertyServer jsf23CDIServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "IterableSupport.war", "com.ibm.ws.jsf23.fat.iterable");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server
        jsf23CDIServer.startServer(JSF23IterableSupportTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23CDIServer != null && jsf23CDIServer.isStarted()) {
            jsf23CDIServer.stopServer();
        }
    }

    /**
     * A test to ensure that <ui:repeat/> supports Iterable.
     *
     * @throws Exception
     */
    @Test
    public void testUIRepeatIterableSupport() throws Exception {
        String contextRoot = "IterableSupport";
        try (WebClient webClient = new WebClient()) {
            String value;

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "uiRepeatSupportForIterable.jsf");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testPage.asText());
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Ensure the first value returned by the Iterable's iterator is correct.
            value = testPage.getElementById("repeat:0:iterableValue").getTextContent();

            assertTrue("The first value returned by the Iterable's iterator was incorrect.", value.equals("5"));

            // Ensure the second value returned by the Iterable's iterator is correct.
            value = testPage.getElementById("repeat:1:iterableValue").getTextContent();

            assertTrue("The second value returned by the Iterable's iterator was incorrect.", value.equals("6"));
        }
    }

    /**
     * A test to ensure that UIData supports Iterable.
     *
     * @throws Exception
     */
    @Test
    public void testUIDataIterableSupport() throws Exception {
        String contextRoot = "IterableSupport";
        try (WebClient webClient = new WebClient()) {
            String value;

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "uiDataSupportForIterable.jsf");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testPage.asText());
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Ensure the first value returned by the Iterable's iterator is correct.
            value = testPage.getElementById("table:0:iterableValue").getTextContent();

            assertTrue("The first value returned by the Iterable's iterator was incorrect.", value.equals("5"));

            // Ensure the second value returned by the Iterable's iterator is correct.
            value = testPage.getElementById("table:1:iterableValue").getTextContent();

            assertTrue("The second value returned by the Iterable's iterator was incorrect.", value.equals("6"));
        }
    }
}
