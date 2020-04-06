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

import java.net.URL;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22IncludeTest {
    @Rule
    public TestName name = new TestName();

    protected static final Class<?> c = JSF22IncludeTest.class;

    @Server("jsf22IncludeTestServer")

    public static LibertyServer jsf22IncludeTestServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf22IncludeTestServer, "TestJSF2.2.war", "com.ibm.ws.fat.jsf22.fat.testjsf.*");
        jsf22IncludeTestServer.startServer(JSF22IncludeTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf22IncludeTestServer != null && jsf22IncludeTestServer.isStarted()) {
            jsf22IncludeTestServer.stopServer();
        }
    }

    /**
     * Test if the page is properly rendered when using a jsp:include element
     * This is a replacement for the JSF 2.0 test PM25955, which has been disabled
     * in the JSF 2.2 run of the JSF 2.0 bucket
     *
     * @throws Exception
     */
    @Test
    public void testJSPInclude() throws Exception {
        String contextRoot = "TestJSF2.2";

        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22IncludeTestServer, contextRoot, "IncludeTest.jsf");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF2.2/IncludeTest.jsf");

            int statusCode = page.getWebResponse().getStatusCode();

            Log.info(c, name.getMethodName(), "Checking the satus code, 200 expected : " + statusCode);
            // Check the status code
            if (statusCode != 200) {
                Assert.fail("Test failed! Status Code: " + statusCode + " Page contents: " + page.asXml());
            }

            Log.info(c, name.getMethodName(), "Checking to make sure the include was properly rendered");
            // Make sure the right text is output
            if (!page.asXml().contains("some text")) {
                Assert.fail("The wrong text was printed! Status Code: " + statusCode + " Page contents: " + page.asXml());
            }

            Log.info(c, name.getMethodName(), "Ensuring ViewState had a proper ID generated");
            //Make sure the ViewState elements were generated with the proper IDs
            //This is the specific ID we want to look for because it has changed since our 2.0 code
            //which generates them with different IDs
            //This test is in lieu of PM25955 from the JSF 2.0 bucket
            List<DomElement> viewStateElements = page.getElementsByName("javax.faces.ViewState");
            for (DomElement element : viewStateElements) {
                String id = element.getId();

                if (!id.startsWith("j_id__v_")) {
                    Assert.fail("ViewState elements were not created with the correct id attribute! id: " + id + " Page contents: " + page.asXml());
                }
            }
        }
    }
}
