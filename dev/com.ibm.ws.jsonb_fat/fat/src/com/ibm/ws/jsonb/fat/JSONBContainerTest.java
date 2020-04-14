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
import static com.ibm.ws.jsonb.fat.FATSuite.PROVIDER_JOHNZON;
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
import web.jsonbtest.JohnzonTestServlet;

@RunWith(FATRunner.class)
@SkipForRepeat(EE9_FEATURES) // TODO: Enable this once cdi-3.0 is available
public class JSONBContainerTest extends FATServletClient {

    @Server("com.ibm.ws.jsonb.container.fat")
    @TestServlets({
                    @TestServlet(servlet = JSONBTestServlet.class, contextRoot = JSONB_APP),
                    @TestServlet(servlet = JohnzonTestServlet.class, contextRoot = JSONB_APP),
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

    // Test a user feature with a service component that injects JsonbProvider (from the bell)
    // as a declarative service. Validate the expected provider is used, and that it can succesfully
    // marshall/unmarshall to/from classes from the bundle.
    @Test
    public void testUserFeature() throws Exception {
        String found;
        server.resetLogMarks();
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST1: JsonbProvider obtained from declarative services"));
        assertTrue(found, found.contains(PROVIDER_JOHNZON));
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST2"));
        assertTrue(found, found.contains("success"));
        assertTrue(found, found.contains("\"Rochester\""));
        assertTrue(found, found.contains("\"Minnesota\""));
        assertTrue(found, found.contains("55901"));
        assertTrue(found, found.contains("410"));
    }

    @Test
    // Verify that the jsonb-1.0 and jsonbContainer-1.0 features can be used together to to specify Yasson
    public void testJsonAndYasson() throws Exception {
        // Add the jsonb-1.0 feature to server.xml
        ServerConfiguration config = server.getServerConfiguration();
        config.getFeatureManager().getFeatures().add("jsonb-1.0");
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JSONB_APP));

        // Run a test to verify that jsonb is still usable
        runTest(server, JSONB_APP + "/JSONBTestServlet", "testJsonbDeserializer&JsonbProvider=" + PROVIDER_JOHNZON);
    }
}
