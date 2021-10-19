/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el.fat.tests;

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
    public static LibertyServer elServer;

    private static final String ServiceLookup_AppName = "ELServicesLookup";

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(elServer, "ELServicesLookup.war", "com.ibm.ws.el30.fat.servicelookup");

        elServer.startServer(EL30MiscTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (elServer != null && elServer.isStarted()) {
            elServer.stopServer();
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

        String url = ELUtils.createHttpUrlString(elServer, ServiceLookup_AppName, "TestServiceLookup");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());
        assertTrue("The response did not contain: Lookup works!", response.getText().contains("Lookup works!"));
    }

    /**
     * Comments in the META-INF/services/javax.el.ExpressionFactory files should be ignored
     *
     * The BZ 64097 patch was applied to both EL-3.0 and th EL-2.2 API.
     *
     * This one tests the service look using the jsp-2.2 feature because it relies on the el 2.2 api. This test was placed here for two mains reasons:
     * - There's no el 2.2 bucket.
     * - This scenario tests EL, so we chose not to place this in the the jsp FATs.
     * - We didn't want to duplicate the test application in multiple buckets
     *
     * https://github.com/OpenLiberty/open-liberty/pull/18424
     *
     * @throws Exception
     */
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    @Test
    public void testEL22ServiceLookup() throws Exception {


        elServer.saveServerConfiguration();

        ServerConfiguration configuration = elServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        elServer.setMarkToEndOfLog();
        elServer.setServerConfigurationFile("serverConfigs/jsp22server.xml");
        elServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(ServiceLookup_AppName), true, "CWWKT0016I:.*ELServicesLookup.*");

        configuration = elServer.getServerConfiguration();
        LOG.info("Updated server configuration: " + configuration);

        try {    
            WebConversation wc = new WebConversation();
    
            wc.setExceptionsThrownOnErrorStatus(false);
    
            String url = ELUtils.createHttpUrlString(elServer, ServiceLookup_AppName, "EL22Test.jsp");
            LOG.info("url: " + url);
    
            WebRequest request = new GetMethodWebRequest(url);
            WebResponse response = wc.getResponse(request);
            LOG.info("Servlet response : " + response.getText());
    
            assertEquals("Expected " + 200 + " status code was not returned!",
                         200, response.getResponseCode());
            assertTrue("The response did not contain: Lookup works!", response.getText().contains("Lookup works!"));
            
        } finally {
            elServer.setMarkToEndOfLog();
            elServer.restoreServerConfiguration();
            // Wait for the application that is still deployed to start.
            elServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(ServiceLookup_AppName), true, "CWWKT0016I:.*ELServicesLookup.*");
        }
    }
}
