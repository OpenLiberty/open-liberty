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

import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test the new Faces 4.0 FacesContext.getLifecycle() API.
 */
@RunWith(FATRunner.class)
public class FacesContextGetLifecycleTest {
    private static final Logger LOG = Logger.getLogger(FacesContextGetLifecycleTest.class.getName());
    private static final String APP_NAME = "FacesContextGetLifecycle";

    @Server("faces40_facesContextGetLifecycleServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war",
                                      "io.openliberty.org.apache.myfaces40.fat.facesContext.getLifecycle.bean",
                                      "io.openliberty.org.apache.myfaces40.fat.facesContext.getLifecycle.phaseListener");

        server.startServer(FacesContextGetLifecycleTest.class.getSimpleName() + ".log");
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
     * Test the Faces 4.0 FacesContext.getLifecycle() API.
     *
     * @throws Exception
     */
    @Test
    public void testFacesContextGetLifecycleAPI() throws Exception {
        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/FacesContextGetLifecycle.xhtml");
        server.setMarkToEndOfLog();

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            LOG.info(page.asText());
            LOG.info(page.asXml());

            // Get the form.
            HtmlForm form = page.getFormByName("form");

            // Get the button and click it.
            HtmlSubmitInput addPhaseListener = form.getInputByName("form:addPhaseListener");
            page = addPhaseListener.click();

            // Verify the log entries from the PhaseListener that was added programmatically are found.
            assertNotNull("The PhaseListener was not invoked before the RENDER_RESPONSE phase.", server.waitForStringInLogUsingMark("beforePhase: RENDER_RESPONSE"));
            assertNotNull("The PhaseListener was not invoked after the RENDER_RESPONSE phase.", server.waitForStringInLogUsingMark("afterPhase: RENDER_RESPONSE"));
        }
    }
}
