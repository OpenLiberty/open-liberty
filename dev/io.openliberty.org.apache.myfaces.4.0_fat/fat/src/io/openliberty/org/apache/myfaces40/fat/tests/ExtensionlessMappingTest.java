/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Faces 4.0 Extensionless Mapping tests.
 *
 */
@RunWith(FATRunner.class)
public class ExtensionlessMappingTest {
    private static final Logger LOG = Logger.getLogger(ExtensionlessMappingTest.class.getName());
    private static final String APP_NAME = "ExtensionlessMappingTest";
    private static final String APP_NAME_NO_FACES_SERVLET = "ExtensionlessMappingNoFacesServletTest";

    @Server("faces40_ExtensionlessMappingTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");
        ShrinkHelper.defaultDropinApp(server, APP_NAME_NO_FACES_SERVLET + ".war");

        server.startServer(ExtensionlessMappingTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that when jakarta.faces.AUTOMATIC_EXTENSIONLESS_MAPPING is set to true
     * that a Facelet can be requested without providing an extension.
     *
     * For example: ExtensionlessMapping.xhtml can be requested using /ExtensionlessMapping
     * rather than /ExtensionlessMapping.xhtml
     *
     * This test also defines a FacesServlet in the web.xml
     *
     * @throws Exception
     */
    @Test
    public void testExtensionlessMapping() throws Exception {
        String expectedOutput = "HELLO FROM EXTENSIONLESS MAPPING TEST!";

        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/ExtensionlessMapping");

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            LOG.info(page.asText());
            LOG.info(page.asXml());

            String outputTextValue = page.getElementById("form:output1").getTextContent();

            assertTrue("The expected output: " + expectedOutput + "  was not found on the page: " + page.asText(),
                       expectedOutput.equals(outputTextValue));
        }
    }

    /**
     * Test that when jakarta.faces.AUTOMATIC_EXTENSIONLESS_MAPPING is set to true
     * that a Facelet can be requested without providing an extension.
     *
     * For example: ExtensionlessMappingNoFacesServlet.xhtml can be requested using /ExtensionlessMappingNoFacesServlet
     * rather than /ExtensionlessMappingNoFacesServlet.xhtml
     *
     * This test does not define a FacesServlet in the web.xml so MyFaces will programmatically
     * add the FacesServlet.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testExtnesionlessMappingNoFacesServletDefined() throws Exception {
        String expectedOutput = "HELLO FROM EXTENSIONLESS MAPPING TEST WITH NO FACES SERVLET DEFINED!";

        URL url = HttpUtils.createURL(server, "/" + APP_NAME_NO_FACES_SERVLET + "/ExtensionlessMappingNoFacesServlet");

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            LOG.info(page.asText());
            LOG.info(page.asXml());

            String outputTextValue = page.getElementById("form:output1").getTextContent();

            assertTrue("The expected output: " + expectedOutput + "  was not found on the page: " + page.asText(),
                       expectedOutput.equals(outputTextValue));
        }
    }
}
