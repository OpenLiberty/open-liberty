/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsonb.fat;

import static com.ibm.ws.jsonb.fat.FATSuite.CDI_APP;
import static com.ibm.ws.jsonb.fat.FATSuite.JSONB_APP;
import static com.ibm.ws.jsonb.fat.FATSuite.PROVIDER_GLASSFISH_JSONP;
import static com.ibm.ws.jsonb.fat.FATSuite.PROVIDER_YASSON;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jsonb.cdi.web.JsonbCDITestServlet;
import web.jsonbtest.JSONBTestServlet;
import web.jsonbtest.YassonTestServlet;

@RunWith(FATRunner.class)
@SkipForRepeat(EE9_FEATURES) // TODO: Enable this once cdi-3.0 is available, https://github.com/OpenLiberty/open-liberty/issues/11633
public class JSONBTest extends FATServletClient {

    @Server("com.ibm.ws.jsonb.fat")
    @TestServlets({
                    @TestServlet(servlet = JSONBTestServlet.class, contextRoot = JSONB_APP),
                    @TestServlet(servlet = YassonTestServlet.class, contextRoot = JSONB_APP),
                    @TestServlet(servlet = JsonbCDITestServlet.class, contextRoot = CDI_APP)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        FATSuite.jsonbApp(server);
        FATSuite.cdiApp(server);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testJsonbFromUserFeature() throws Exception {
        // Add the jsonb user feature, which will make 'ServiceThatRequiresJsonb' activate
        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        config.getFeatureManager().getFeatures().add("usr:testFeatureUsingJsonb-1.0");
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.emptySet());

        // Scrape messages.log to verify that 'ServiceThatRequiresJsonb' has activated
        // using Johnzon for jsonp and Johnzon for jsonb
        String found;
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST1: JsonbProvider obtained from declarative services"));
        assertTrue(found, found.contains(PROVIDER_YASSON));
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST1.1: JsonProvider obtained from declarative services"));
        assertTrue(found, found.contains(PROVIDER_GLASSFISH_JSONP));
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST2"));
        assertTrue(found, found.contains("success"));
        assertTrue(found, found.contains("\"Rochester\""));
        assertTrue(found, found.contains("\"Minnesota\""));
        assertTrue(found, found.contains("55901"));
        assertTrue(found, found.contains("410"));

        // Clean up the test by removing the jsonb-1.0 feature
        config.getFeatureManager().getFeatures().remove("usr:testFeatureUsingJsonb-1.0");
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.emptySet());
    }
}
