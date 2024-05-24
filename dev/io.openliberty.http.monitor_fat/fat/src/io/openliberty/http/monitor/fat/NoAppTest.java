/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.fat;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

@RunWith(FATRunner.class)
public class NoAppTest extends BaseTestClass {

    private static Class<?> c = NoAppTest.class;

    @Server("SimpleRestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        trustAll();

        server.startServer();

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

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

}
