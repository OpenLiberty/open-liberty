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
 * A set of test cases for the @FacesDataModel feature of JSF 2.3
 *
 */
@RunWith(FATRunner.class)
public class JSF23FacesDataModelTests {

    protected static final Class<?> c = JSF23FacesDataModelTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23CDIServer")
    public static LibertyServer jsf23CDIServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "FacesDataModel.war", "com.ibm.ws.jsf23.fat.datamodel");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server.
        jsf23CDIServer.startServer(JSF23FacesDataModelTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23CDIServer != null && jsf23CDIServer.isStarted()) {
            jsf23CDIServer.stopServer();
        }
    }

    /**
     * This is a test to ensure that we can register a custom DataModel using
     * the @FacesDataModel annotation. The test ensures that this custom DataModel
     * works with the <ui:repeat/> tag.
     *
     * @throws Exception
     */
    @Test
    public void testFacesDataModelUIRepeat() throws Exception {
        String contextRoot = "FacesDataModel";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "UIRepeatFacesDataModel.jsf");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure the proper values were output on the page
            String testValue1 = page.getElementById("repeat:0:output").getTextContent();
            String testValue2 = page.getElementById("repeat:1:output").getTextContent();
            String testValue3 = page.getElementById("repeat:2:output").getTextContent();
            String testValue4 = page.getElementById("repeat:3:output").getTextContent();

            Log.info(c, name.getMethodName(), "testValue1: " + testValue1);
            Log.info(c, name.getMethodName(), "testValue2: " + testValue2);
            Log.info(c, name.getMethodName(), "testValue3: " + testValue3);
            Log.info(c, name.getMethodName(), "testValue4: " + testValue4);

            assertTrue("The FacesDataModel annotation did not work correctly.",
                       testValue1.equals("test1") && testValue2.equals("test2") &&
                                                                                testValue3.equals("test3") && testValue4.equals("test4"));
        }
    }

    /**
     * This is a test to ensure that we can register a custom DataModel using
     * the @FacesDataModel annotation. The test ensures that this custom DataModel
     * works with the <h:dataTable/> tag.
     *
     * @throws Exception
     */
    @Test
    public void testFacesDataModelUIData() throws Exception {
        String contextRoot = "FacesDataModel";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "UIDataFacesDataModel.jsf");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure the proper values were output on the page
            String testValue1 = page.getElementById("table:0:output").getTextContent();
            String testValue2 = page.getElementById("table:1:output").getTextContent();
            String testValue3 = page.getElementById("table:2:output").getTextContent();
            String testValue4 = page.getElementById("table:3:output").getTextContent();

            Log.info(c, name.getMethodName(), "testValue1: " + testValue1);
            Log.info(c, name.getMethodName(), "testValue2: " + testValue2);
            Log.info(c, name.getMethodName(), "testValue3: " + testValue3);
            Log.info(c, name.getMethodName(), "testValue4: " + testValue4);

            assertTrue("The FacesDataModel annotation did not work correctly.",
                       testValue1.equals("test1") && testValue2.equals("test2") &&
                                                                                testValue3.equals("test3") && testValue4.equals("test4"));
        }
    }

    /**
     * This is a test to ensure that we can register a custom DataModel using
     * the @FacesDataModel annotation. The test ensures that this custom DataModel
     * works with the <h:dataTable/> tag. In addition this test is checking to ensure
     * that the proper DataModel is used since TestValuesChild extends TestValues and
     * both have custom DataModels. The test ensure that the child DataModel is used and not
     * the parent DataModel.
     *
     * @throws Exception
     */
    @Test
    public void testFacesDataModelChildUIData() throws Exception {
        String contextRoot = "FacesDataModel";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "UIDataFacesDataModelChild.jsf");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure the proper values were output on the page
            String testValue1 = page.getElementById("table:0:output").getTextContent();
            String testValue2 = page.getElementById("table:1:output").getTextContent();
            String testValue3 = page.getElementById("table:2:output").getTextContent();
            String testValue4 = page.getElementById("table:3:output").getTextContent();

            Log.info(c, name.getMethodName(), "testValue1: " + testValue1);
            Log.info(c, name.getMethodName(), "testValue2: " + testValue2);
            Log.info(c, name.getMethodName(), "testValue3: " + testValue3);
            Log.info(c, name.getMethodName(), "testValue4: " + testValue4);

            assertTrue("The FacesDataModel annotation did not work correctly.",
                       testValue1.equals("child: test1") && testValue2.equals("child: test2") &&
                                                                                testValue3.equals("child: test3") && testValue4.equals("child: test4"));
        }
    }

    /**
     * This is a test to ensure that we can register a custom DataModel using
     * the @FacesDataModel annotation. The test ensures that this custom DataModel
     * works with the <ui:repeat/> tag. In addition this test is checking to ensure
     * that the proper DataModel is used since TestValuesChild extends TestValues and
     * both have custom DataModels. The test ensure that the child DataModel is used and not
     * the parent DataModel.
     *
     * @throws Exception
     */
    @Test
    public void testFacesDataModelChildUIRepeat() throws Exception {
        String contextRoot = "FacesDataModel";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "UIRepeatFacesDataModelChild.jsf");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure the proper values were output on the page
            String testValue1 = page.getElementById("repeat:0:output").getTextContent();
            String testValue2 = page.getElementById("repeat:1:output").getTextContent();
            String testValue3 = page.getElementById("repeat:2:output").getTextContent();
            String testValue4 = page.getElementById("repeat:3:output").getTextContent();

            Log.info(c, name.getMethodName(), "testValue1: " + testValue1);
            Log.info(c, name.getMethodName(), "testValue2: " + testValue2);
            Log.info(c, name.getMethodName(), "testValue3: " + testValue3);
            Log.info(c, name.getMethodName(), "testValue4: " + testValue4);

            assertTrue("The FacesDataModel annotation did not work correctly.",
                       testValue1.equals("child: test1") && testValue2.equals("child: test2") &&
                                                                                testValue3.equals("child: test3") && testValue4.equals("child: test4"));
        }
    }
}
