/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Faces 4.0 UIViewRoot.getDoctype test.
 *
 */
@RunWith(FATRunner.class)
public class UIViewRootGetDoctypeTest {
    private static final Logger LOG = Logger.getLogger(UIViewRootGetDoctypeTest.class.getName());
    private static final String APP_NAME = "UIViewRootGetDoctypeTest";

    @Server("faces40_UIViewRootGetDoctypeTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.org.apache.faces40.fat.uiViewRoot.getDoctype.bean");

        server.startServer(UIViewRootGetDoctypeTest.class.getSimpleName() + ".log");
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
     * This test drives a request to page that contains an HTML 5 DOCTYPE. The page
     * calls a bean method that gets the Doctype and returns true if it was correct.
     * The bean uses the new Faces 4.0 UIViewRoot.getDocType() API.
     *
     * In addition, the test verifies that the HTML 5 DOCTYPE was also contained within the
     * response.
     *
     * @throws Exception
     */
    @Test
    public void testUIViewRootGetDoctype() throws Exception {
        String expectedDoctype = "<!DOCTYPE html>";

        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/UIViewRootGetDoctype.xhtml");

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            LOG.info(page.asXml());

            String outputTextValue = page.getElementById("form:docTypeOutput").getTextContent();

            assertTrue("The UIViewRootGetDoctype.xhtml page uses the HTML 5 DOCTYPE and it was not found in the response: " + page.asText(),
                       page.getWebResponse().getContentAsString().contains(expectedDoctype));

            assertTrue("The DoctypeBean should return true when isDoctypeHtml5() is called and an HTML 5 DOCTYPE is used but returned: " + outputTextValue,
                       Boolean.parseBoolean(outputTextValue) == true);
        }
    }
}
