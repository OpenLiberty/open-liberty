/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test the new Faces 4.0 URNs that have not already been tested in other
 * tests in this FAT bucket.
 */
@RunWith(FATRunner.class)
public class Faces40URNTest {

    private static final Logger LOG = Logger.getLogger(Faces40URNTest.class.getName());
    private static final String APP_NAME = "Faces40URN";

    @Server("faces40_URN")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.org.apache.faces40.fat.urn.component");

        server.startServer(Faces40URNTest.class.getSimpleName() + ".log");
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
     * Test new Faces 4.0 URNs.
     *
     * @throws Exception
     */
    @Test
    public void testFaces40URN() throws Exception {
        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/Faces40URN.xhtml");
        String formId = "URNTestFacesForm";
        String inputTextName = "URNTestFacesForm:URNTestFacesInputText";
        String inputEmailName = "URNTestFacesForm:URNTestFacesInputEmail";

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            LOG.info(page.asText());
            LOG.info(page.asXml());

            HtmlForm form = (HtmlForm) page.getElementById(formId);

            // Verify the jakarta.faces URN.
            assertTrue("A form with id: " + formId + " was not found.", form != null);

            HtmlInput inputText = form.getInputByName(inputTextName);
            HtmlInput inputEmail = form.getInputByName(inputEmailName);

            // Verify the jakarta.faces.passthrough URN.
            assertTrue("The " + inputEmailName + " input did not have a type attribute of email. The type attribute value was: " + inputEmail.getTypeAttribute(),
                       inputEmail.getTypeAttribute().equals("email"));

            // Verify the jakarta.tags.functions URN.
            assertTrue("The " + inputTextName + " input did not have a value of true. The value attribute was: " + inputText.getValueAttribute(),
                       inputText.getValueAttribute().equals("true"));

            // Verify the jakarta.faces.component URN
            assertTrue("The TestComponent did not output: hello world in all lowercase letters.", page.asText().contains("hello world"));
        }
    }
}
