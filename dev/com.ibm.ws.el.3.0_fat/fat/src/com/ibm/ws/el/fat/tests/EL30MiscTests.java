/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.el.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE9_OR_LATER_FEATURES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.el.fat.ELUtils;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * EL Misc Tests
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EL30MiscTests {

    private static final Logger LOG = Logger.getLogger(EL30MiscTests.class.getName());

    @Server("elMiscServer")
    public static LibertyServer server;

    private static final String ServiceLookup_AppName = "ELServicesLookup";

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "ELServicesLookup.war", "com.ibm.ws.el30.fat.servicelookup");

        server.startServer(EL30MiscTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Comments in the META-INF/services/javax.el.ExpressionFactory files should be ignored
     *
     * https://github.com/OpenLiberty/open-liberty/pull/18424
     *
     * @throws Exception
     */
    @Test
    public void testEL30ServiceLookup() throws Exception {
        WebConversation wc = new WebConversation();

        String url = ELUtils.createHttpUrlString(server, ServiceLookup_AppName, "TestServiceLookup");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());
        assertTrue("The response did not contain: Lookup works!", response.getText().contains("Lookup works!"));
    }

    /**
     * Comments in the META-INF/services/javax.el.ExpressionFactory files should be ignored.
     *
     * The BZ 64097 patch was applied to both EL 3.0 and the EL 2.2 API.
     *
     * This one tests the service lookup using the jsp-2.2 feature because it relies on the EL 2.2 API. This test was placed here for three reasons:
     *
     * 1) There's no EL 2.2 test bucket.
     * 2) This scenario tests EL, so we chose not to place this in the the JSP FATs.
     * 3) We didn't want to duplicate the test application in multiple buckets.
     *
     * https://github.com/OpenLiberty/open-liberty/pull/18424
     *
     * @throws Exception
     */
    @SkipForRepeat(EE9_OR_LATER_FEATURES)
    @Test
    public void testEL22ServiceLookup() throws Exception {

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("serverConfigs/jsp22server.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(ServiceLookup_AppName), true, "CWWKT0016I:.*ELServicesLookup.*");

        configuration = server.getServerConfiguration();
        LOG.info("Updated server configuration: " + configuration);

        try {
            WebConversation wc = new WebConversation();

            wc.setExceptionsThrownOnErrorStatus(false);

            String url = ELUtils.createHttpUrlString(server, ServiceLookup_AppName, "EL22Test.jsp");
            LOG.info("url: " + url);

            WebRequest request = new GetMethodWebRequest(url);
            WebResponse response = wc.getResponse(request);
            LOG.info("Servlet response : " + response.getText());

            assertEquals("Expected " + 200 + " status code was not returned!",
                         200, response.getResponseCode());
            assertTrue("The response did not contain: Lookup works!", response.getText().contains("Lookup works!"));

        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            // Wait for the application that is still deployed to start.
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(ServiceLookup_AppName), true, "CWWKT0016I:.*ELServicesLookup.*");
        }
    }
}
