/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.tests;

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
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests that the correct MyFaces packages are accessible when an application is configured to use third-party APIs
 */
@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class JSF22ThirdPartyApiTests {

    protected static final Class<?> c = JSF22ThirdPartyApiTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf22ThirdPartyAPIServer")
    public static LibertyServer jsf22ThirdPartyApiServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultApp(jsf22ThirdPartyApiServer, "JSF22ThirdPartyApi.war", "com.ibm.ws.jsf22.fat.thirdpartyapi");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server.
        jsf22ThirdPartyApiServer.startServer(JSF22ThirdPartyApiTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf22ThirdPartyApiServer != null && jsf22ThirdPartyApiServer.isStarted()) {
            jsf22ThirdPartyApiServer.stopServer();
        }
    }

    /**
     * Test that an application with the "third-party" classloader visibility enabled has access to the jsf-2.3 org.apache.myfaces packages.
     *
     * @throws Exception
     */
    @Test
    public void testJSFThirdPartyAPIAccess() throws Exception {
        String contextRoot = "JSF22ThirdPartyApi";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf22ThirdPartyApiServer, contextRoot, "JSF22ThirdPartyAPI.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The MyFaces API classes were not accessible by the application:\n" + page.asText(), page.asXml().contains("test passed!"));
        }
    }

    /**
     * Test that the same app cannot access those org.apache.myfaces packages which have not been exposed
     *
     * @throws Exception
     */
    @Test
    public void testJSFThirdPartyAPIAccessFails() throws Exception {
        String contextRoot = "JSF22ThirdPartyApi";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf22ThirdPartyApiServer, contextRoot, "JSF22ThirdPartyAPIFailure.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("org.apache.myfaces classes were accessible when they should not have been:\n" + page.asText(), page.asXml().contains("test passed!"));
        }
    }
}
