/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.fat;

import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

/**
 * This just tests hitting the splash page for the server.
 */
@RunWith(FATRunner.class)
public class NoAppTest extends BaseTestClass {

    private static Class<?> c = NoAppTest.class;

    @Server("SimpleRestServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests rt = FATSuite.testRepeatMPTMetrics5("SimpleRestServer");

    @BeforeClass
    public static void beforeClass() throws Exception {
        trustAll();

        WebArchive testWAR = ShrinkWrap
                        .create(WebArchive.class, "MBeanGetter.war")
                        .addPackage(
                                    "io.openliberty.http.monitor.fat.mbeanGetter");

        ShrinkHelper.exportDropinAppToServer(server, testWAR,
                                             DeployOptions.SERVER_ONLY);

        server.startServer();
        server.waitForSSLStart();

        //Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        server.setMarkToEndOfLog();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        //catch if a server is still running.
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void noApp_splashPage() throws Exception {

        assertTrue(server.isStarted());

        String route = "/";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    public void noApp_nonExistent() throws Exception {

        assertTrue(server.isStarted());

        String route = "/madeThisUp";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "404";
        String expectedRoute = "/";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRoute, responseStatus, requestMethod));

        String route2 = "/anotherMadeThisUp";
        String res2 = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRoute, responseStatus, requestMethod, null, ">1", null));

    }

}
