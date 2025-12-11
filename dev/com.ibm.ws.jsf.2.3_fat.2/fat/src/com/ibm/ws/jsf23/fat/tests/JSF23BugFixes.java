/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.Before;
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
 * Tests for Bug Fixes in JSF 2.3
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JSF23BugFixes {

    protected static final Class<?> c = JSF23BugFixes.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23BugFixServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "MYFACES-4709.war", "com.ibm.ws.jsf23.fat.myfaces4709");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server
        server.startServer(c.getSimpleName() + ".log");

    }

    @Before
    public void startServer() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer(JSF23BugFixes.class.getSimpleName() + ".log");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * https://github.com/OpenLiberty/open-liberty/issues/27496
     * 
     * Ensure Liberty picks up the FacesConfig annotation via HandlesTypes.
     * Note - Liberty extends MyFacesContainerInitializer via WASMyFacesContainerInitializer
     * 
     * The app has no mappings to the Faces Servlet, so they will need to be added automatically due to the FacesConfig annotation.
     * 
     * @throws Exception
     */
    @Test
    public void testMyFaces4709() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "MYFACES-4709";
            URL url = JSFUtils.createHttpUrl(server, contextRoot, "index.faces");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure the page loaded correctly
            String pageText = "Hello World";
            assertTrue("The page did not contain the following text: " + pageText, page.asText().contains(pageText));
        }
    }

}
