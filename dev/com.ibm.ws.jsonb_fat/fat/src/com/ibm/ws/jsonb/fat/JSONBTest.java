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

import static com.ibm.ws.jsonb.fat.FATSuite.PROVIDER_JOHNZON;
import static com.ibm.ws.jsonb.fat.FATSuite.PROVIDER_JOHNZON_JSONP;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.jsonbtest.JSONBTestServlet;
import web.jsonbtest.JohnzonTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 1.8)
public class JSONBTest extends FATServletClient {
    private static final String appName = "jsonbapp";

    @Server("com.ibm.ws.jsonb.fat")
    @TestServlets({
                    @TestServlet(servlet = JSONBTestServlet.class, path = appName + "/JSONBTestServlet"),
                    @TestServlet(servlet = JohnzonTestServlet.class, path = appName + "/JohnzonTestServlet")
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, appName + ".war")
                        .addPackage("web.jsonbtest");
        ShrinkHelper.exportAppToServer(server, app);

        server.addInstalledAppForValidation(appName);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testJsonbFromUserFeature() throws Exception {
        // Due to a JSON-P 1.1 spec bug, first-touch cannot be from the user feature, see:
        // https://github.com/javaee/jsonp/issues/56
        runTest(server, appName + "/JSONBTestServlet", "testJsonbDemo");

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
        assertTrue(found, found.contains(PROVIDER_JOHNZON));
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST1.1: JsonProvider obtained from declarative services"));
        assertTrue(found, found.contains(PROVIDER_JOHNZON_JSONP));
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
