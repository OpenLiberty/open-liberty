/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.fat;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

/**
 * This just tests hitting the splash page for the server.
 */
@RunWith(FATRunner.class)
public class ContainerNoAppTest extends BaseTestClass {

    private static Class<?> c = ContainerNoAppTest.class;

    @Server("ContainerJustServer")
    public static LibertyServer server;

    @ClassRule //FileSystemBind, path is relative to AutoFVT folder.
    public static GenericContainer<?> container = new GenericContainer<>("otel/opentelemetry-collector-contrib:0.103.0")
                    .withLogConsumer(new SimpleLogConsumer(ContainerServletApplicationTest.class, "opentelemetry-collector-contrib"))
                    .withFileSystemBind("config.yaml", "/etc/otelcol-contrib/config.yaml", BindMode.READ_ONLY)
                    .withExposedPorts(8888, 8889, 4317);

    @BeforeClass
    public static void beforeClass() throws Exception {
        trustAll();
        server.addEnvVar("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", "http://" + container.getHost() + ":" + container.getMappedPort(4317));
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
    public void c_noApp_splashPage() throws Exception {

        assertTrue(server.isStarted());

        String route = "/";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        TimeUnit.SECONDS.sleep(4);
        assertTrue(validateMpTelemetryHttp(Constants.RUNTIME_INSTANCE_SERVICE, getContainerCollectorMetrics(container), route, responseStatus, requestMethod));

    }

}
