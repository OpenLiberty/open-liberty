/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package com.ibm.ws.jsonb.fat;

import static com.ibm.ws.jsonb.fat.FATSuite.CDI_APP;
import static com.ibm.ws.jsonb.fat.FATSuite.JSONB_APP;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jsonb.cdi.web.JsonbCDITestServlet;
import web.jsonbtest.JSONBTestServlet;

@RunWith(FATRunner.class)
public class JSONBContainerTest extends FATServletClient {

    @Server("com.ibm.ws.jsonb.container.fat")
    @TestServlets({
                    @TestServlet(servlet = JSONBTestServlet.class, contextRoot = JSONB_APP),
                    @TestServlet(servlet = JsonbCDITestServlet.class, contextRoot = CDI_APP)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(JSONBContainerTest.class, "setUp", "=====> Start JSONBContainerTest");

        FATSuite.configureImpls(server);
        FATSuite.jsonbApp(server);
        FATSuite.cdiApp(server);

        server.startServer();

        if (JakartaEE10Action.isActive()) { //TODO possibly back port this info message to EE9 and EE8
            assertTrue(!server.findStringsInLogsAndTrace("CWWKJ0350I").isEmpty());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
        Log.info(JSONBContainerTest.class, "tearDown", "<===== Stop JSONBContainerTest");
    }

    // Test a user feature with a service component that injects JsonbProvider (from the bell)
    // as a declarative service. Validate the expected provider is used, and that it can successfully
    // marshall/unmarshall to/from classes from the bundle.
    @Test
    public void testUserFeature() throws Exception {
        String found;
        server.resetLogMarks();
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST1: JsonbProvider obtained from declarative services"));
        assertTrue(found, found.contains(FATSuite.getJsonbProviderClassName()));
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST2"));
        assertTrue(found, found.contains("success"));
        assertTrue(found, found.contains("\"Rochester\""));
        assertTrue(found, found.contains("\"Minnesota\""));
        assertTrue(found, found.contains("55901"));
        assertTrue(found, found.contains("410"));
    }

    @Test
    // Verify that the jsonb-x.x and jsonbContainer-x.x features can be used together to specify Yasson
    public void testJsonAndYasson() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        ServerConfiguration configClone = config.clone();

        if (JakartaEE10Action.isActive()) {
            config.getFeatureManager().getFeatures().add("jsonb-3.0");
        } else if (JakartaEE9Action.isActive()) {
            config.getFeatureManager().getFeatures().add("jsonb-2.0");
        } else {
            config.getFeatureManager().getFeatures().add("jsonb-1.0");
        }

        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JSONB_APP));

        // Run a test to verify that jsonb is still usable
        runTest(server, JSONB_APP + "/JSONBTestServlet", "testJsonbDeserializer");

        server.updateServerConfiguration(configClone);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JSONB_APP));
    }

    @Test
    public void testApplicationClasses() throws Exception {
        runTest(server, JSONB_APP + "/JSONBTestServlet", getTestMethodSimpleName() + "&JsonbProvider=" + FATSuite.getJsonbProviderClassName());
    }

    @Test
    public void testJsonbAdapter() throws Exception {
        runTest(server, JSONB_APP + "/JSONBTestServlet", getTestMethodSimpleName() + "&JsonbProvider=" + FATSuite.getJsonbProviderClassName());
    }

    @Test
    public void testJsonbProviderAvailable() throws Exception {
        runTest(server, JSONB_APP + "/JSONBTestServlet", getTestMethodSimpleName() + "&JsonbProvider=" + FATSuite.getJsonbProviderClassName());
    }

    @Test
    public void testJsonbProviderNotAvailable() throws Exception {
        runTest(server, JSONB_APP + "/JSONBTestServlet", getTestMethodSimpleName() + "&JsonbProvider=" + FATSuite.getJsonbProviderClassName(false));
    }

    @Test
    public void testThreadContextClassLoader() throws Exception {
        runTest(server, JSONB_APP + "/JSONBTestServlet", getTestMethodSimpleName() + "&JsonbProvider=" + FATSuite.getJsonbProviderClassName());
    }
}
