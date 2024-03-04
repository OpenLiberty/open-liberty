/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces41.fat.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces41.fat.JSFUtils;

/**
 * Faces 4.1: Deprecate full state saving (FSS)
 * https://github.com/jakartaee/faces/issues/1829
 * 
 * The following two are deprecated:
 *  - jakarta.faces.PARTIAL_STATE_SAVING
 *  - jakarta.faces.FULL_STATE_SAVING_VIEW_IDS
 * 
 * Faces 4.1: jakarta.faces.FACELETS_REFRESH_PERIOD default when ProjectStage is Development
 * https://github.com/jakartaee/faces/issues/1821
 * 
 * When the project stage is development, the default refresh period should be 0. 
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ConfigTests {

    private static final String APP_NAME = "Config_Spec1764";
    protected static final Class<?> c = ConfigTests.class;

    private static final Logger LOG = Logger.getLogger(ContentLengthTest.class.getName());

    @Rule
    public TestName name = new TestName();

    @Server("faces41_configServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(ConfigTests.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer(); 
        }

    }

    /**
     * Verify the logs state that jakarta.faces.PARTIAL_STATE_SAVING is deprecated.
     *
     * @throws Exception
     */
    @Test
    public void verifyParitalStateSavingIsDeprecated() throws Exception {
        assertEquals(1, server.findStringsInLogs("The configuration 'jakarta.faces.PARTIAL_STATE_SAVING' is deprecated").size());
    }

    /**
     * Verify that jakarta.faces.FACELETS_REFRESH_PERIOD is 0 in Development. 
     *
     * @throws Exception
     */
    @Test
    public void verifyFaceletRefreshPeriodIsZeroinDevelopment() throws Exception {
        // 1) Check Log Message
        assertEquals(1, server.findStringsInLogs("No context init parameter 'jakarta.faces.FACELETS_REFRESH_PERIOD' found, using default value '0, -1 in Production'").size());


        // 2) Update the index.xhtml to verify the changes are picked up 
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(server, APP_NAME, "index.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            LOG.info(page.asXml());

            assertTrue("Error! Text not Found!", page.asText().contains("Value: 123"));

            String filetoUpdate = "index.xhtml";
            String newLocation = "apps/expanded/" + APP_NAME + ".war";
            server.copyFileToLibertyServerRoot(newLocation, filetoUpdate);

            page = (HtmlPage) webClient.getPage(url);

            LOG.info(page.asXml());

            // Page should update on new request
            assertTrue("Page was not refreshed!", page.asText().contains("Value: 456"));

        }

    }

    /**
     *  Verify that jakarta.faces.FULL_STATE_SAVING_VIEW_IDS is marked as deprecated in the logs
     *
     * @throws Exception
     */
    // @Test
    public void verifyFullStateSavingViewIdsIsDeprecated() throws Exception {
        // TO DO -- Warning will be added in next MyFaces Release (RC2)
    }



}
