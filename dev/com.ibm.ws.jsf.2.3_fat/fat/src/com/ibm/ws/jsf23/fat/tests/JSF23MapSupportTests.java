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
 * A test class to test <ui:repeat/> and UIData with a Map.
 */
@RunWith(FATRunner.class)
public class JSF23MapSupportTests {

    protected static final Class<?> c = JSF23MapSupportTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23Server")
    public static LibertyServer jsf23Server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23Server, "ImportConstantsTag.war", "com.ibm.ws.jsf23.fat.constants");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server.
        jsf23Server.startServer(JSF23MapSupportTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23Server != null && jsf23Server.isStarted()) {
            jsf23Server.stopServer();
        }
    }

    /**
     * Test to ensure that <ui:repeat/> supports Maps.
     *
     * @throws Exception
     */
    @Test
    public void testUIRepeatMapSupport() throws Exception {
        String contextRoot = "ImportConstantsTag";
        try (WebClient webClient = new WebClient()) {
            String key;
            String value;

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "uiRepeatSupportForMap.jsf");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testPage.asText());
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Ensue the first key,value pair of the Map is correct.
            key = testPage.getElementById("repeat:0:key").getTextContent();
            value = testPage.getElementById("repeat:0:value").getTextContent();

            assertTrue("The first key,value pair was not correct.", key.equals("TEST_CONSTANTS_1") && value.equals("Testing "));

            // Ensure the second key,value pair of the Map is correct.
            key = testPage.getElementById("repeat:1:key").getTextContent();
            value = testPage.getElementById("repeat:1:value").getTextContent();

            assertTrue("The second key,value pair was not correct.", key.equals("TEST_CONSTANTS_2") && value.equals("a "));

            // Ensure the third key,value pair of the Map is correct.
            key = testPage.getElementById("repeat:2:key").getTextContent();
            value = testPage.getElementById("repeat:2:value").getTextContent();

            assertTrue("The third key,value pair was not correct.", key.equals("TEST_CONSTANTS_3") && value.equals("class!"));
        }
    }

    /**
     * Test to ensure that UIData supports Maps
     *
     * @throws Exception
     */
    @Test
    public void testUIDataMapSupport() throws Exception {
        String contextRoot = "ImportConstantsTag";
        try (WebClient webClient = new WebClient()) {
            String key;
            String value;

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "uiDataSupportForMap.jsf");

            HtmlPage testPage = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testPage.asText());
            Log.info(c, name.getMethodName(), testPage.asXml());

            // Ensue the first key,value pair of the Map is correct.
            key = testPage.getElementById("table:0:key").getTextContent();
            value = testPage.getElementById("table:0:value").getTextContent();

            assertTrue("The first key,value pair was not correct.", key.equals("TEST_CONSTANTS_1") && value.equals("Testing "));

            // Ensure the second key,value pair of the Map is correct.
            key = testPage.getElementById("table:1:key").getTextContent();
            value = testPage.getElementById("table:1:value").getTextContent();

            assertTrue("The second key,value pair was not correct.", key.equals("TEST_CONSTANTS_2") && value.equals("a "));

            // Ensure the third key,value pair of the Map is correct.
            key = testPage.getElementById("table:2:key").getTextContent();
            value = testPage.getElementById("table:2:value").getTextContent();

            assertTrue("The third key,value pair was not correct.", key.equals("TEST_CONSTANTS_3") && value.equals("class!"));
        }
    }
}
