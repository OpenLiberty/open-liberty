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

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

/**
 * This just tests hitting the splash page for the server.
 */
@RunWith(FATRunner.class)
@SkipForRepeat(MicroProfileActions.MP70_EE11_APP_MODE_ID)
public class ContainerNoAppTest extends BaseTestClass {

    private static Class<?> c = ContainerNoAppTest.class;

    private static final String SERVER_NAME = "ContainerJustServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests rt = FATSuite.allMPRepeatsWithMPTel20OrLater(SERVER_NAME);

    //TODO switch to use ghcr.io/open-telemetry/opentelemetry-collector-releases/opentelemetry-collector-contrib:0.117.0
    //TODO remove withDockerfileFromBuilder and instead create a dockerfile
    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> builder.from(IMAGE_NAME)
                                    .copy("/etc/otelcol-contrib/config.yaml", "/etc/otelcol-contrib/config.yaml"))
                    .withFileFromFile("/etc/otelcol-contrib/config.yaml", new File(PATH_TO_AUTOFVT_TESTFILES + "config.yaml")))
                    .withLogConsumer(new SimpleLogConsumer(ContainerServletApplicationTest.class, "opentelemetry-collector-contrib"))
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
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(Constants.OTEL_SERVICE_NOT_SET, getContainerCollectorMetrics(container), route, responseStatus, requestMethod));

    }

    @Test
    public void c_noApp_nonExistent() throws Exception {

        assertTrue(server.isStarted());

        String route = "/madeThisUp";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "404";
        String expectedRoute = "/";

        String res = requestHttpServlet(route, server, requestMethod);

        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(Constants.OTEL_SERVICE_NOT_SET, getContainerCollectorMetrics(container), expectedRoute, responseStatus,
                                                                 requestMethod));

        String route2 = "/anotherMadeThisUp";
        String res2 = requestHttpServlet(route, server, requestMethod);

        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(Constants.OTEL_SERVICE_NOT_SET, getContainerCollectorMetrics(container), expectedRoute, responseStatus,
                                                                 requestMethod, null,
                                                                 ">1", null));

    }

}
