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

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

/**
 *
 */
@RunWith(FATRunner.class)
public class ContainerServletApplicationTest extends BaseTestClass {

    private static Class<?> c = ContainerServletApplicationTest.class;

    private static final String SERVER_NAME = "ContainerServletServer";
    private static final String SERVICE_NAME = FATSuite.getAppNameOrUnknownService(Constants.SERVLET_APP);
    private static final String WILDCARD_SERVICE_NAME = FATSuite.getAppNameOrUnknownService(Constants.WILDCARD_SERVLET_APP);

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
        WebArchive simpleSerletWAR = ShrinkWrap
                        .create(WebArchive.class, "ServletApp.war")
                        .addPackage(
                                    "io.openliberty.http.monitor.fat.servletApp");

        simpleSerletWAR = FATSuite.setTelProperties(simpleSerletWAR, server);

        WebArchive wildCardServletWAR = ShrinkWrap
                        .create(WebArchive.class, "WildCardServlet.war")
                        .addPackage(
                                    "io.openliberty.http.monitor.fat.wildCardServletApp");

        wildCardServletWAR = FATSuite.setTelProperties(wildCardServletWAR, server);

        ShrinkHelper.exportDropinAppToServer(server, simpleSerletWAR,
                                             DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportDropinAppToServer(server, wildCardServletWAR, DeployOptions.SERVER_ONLY);

        server.addEnvVar("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", "http://" + container.getHost() + ":" + container.getMappedPort(4317));

        server.startServer();

        //Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        server.setMarkToEndOfLog();

    }

    @Test
    public void cs1_simplePathGet() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), route, responseStatus,
                                                                 requestMethod));

    }

    @Test
    public void cs1_simplePathPost() throws Exception {
        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.POST;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), route, responseStatus,
                                                                 requestMethod));

    }

    @Test
    public void cs1_simplePathPut() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.PUT;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), route, responseStatus,
                                                                 requestMethod));

    }

    @Test
    public void cs1_simplePathDelete() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.DELETE;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), route, responseStatus,
                                                                 requestMethod));

    }

    @Test
    public void cs1_simplePathOptions() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.OPTIONS;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), route, responseStatus,
                                                                 requestMethod));

    }

    @Test
    public void cs1_simplePathHead() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.HEAD;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), route, responseStatus,
                                                                 requestMethod));

    }

    @Test
    @AllowedFFDC
    public void cs1_failDivZero() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.FAIL_SERVLET_URL;
        String requestMethod = HttpMethod.GET;
        String responseStatus = "500";
        String errorType = responseStatus;

        String res = requestHttpServlet(route, server, requestMethod, "failMode=zero");
        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), route, responseStatus, requestMethod,
                                                                 errorType));

    }

    @Test
    @AllowedFFDC
    public void cs1_failNonExistentPath() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SERVLET_CONTEXT_ROOT + "/nonExistent";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "404";
        String expectedRoute = Constants.SERVLET_CONTEXT_ROOT + "/\\*";

        String res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), expectedRoute, responseStatus,
                                                                 requestMethod));

    }

    @Test
    public void cs1_failCustom() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.FAIL_SERVLET_URL;
        String requestMethod = HttpMethod.GET;
        String responseStatus = "456";

        String res = requestHttpServlet(route, server, requestMethod, "failMode=custom");
        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), route, responseStatus,
                                                                 requestMethod));

    }

    @Test
    @AllowedFFDC
    public void cs1_failIOE() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.FAIL_SERVLET_URL;
        String requestMethod = HttpMethod.GET;
        String responseStatus = "500";
        String errorType = responseStatus;

        String res = requestHttpServlet(route, server, requestMethod, "failMode=io");
        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), route, responseStatus, requestMethod,
                                                                 errorType));

    }

    @Test
    @AllowedFFDC
    public void cs1_failIAE() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.FAIL_SERVLET_URL;
        String requestMethod = HttpMethod.GET;
        String responseStatus = "500";
        String errorType = responseStatus;

        String res = requestHttpServlet(route, server, requestMethod, "failMode=iae");
        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), route, responseStatus, requestMethod,
                                                                 errorType));

    }

    @Test
    public void cs1_rootWildCard() throws Exception {
        // path -> <app>/*

        assertTrue(server.isStarted());

        String route = Constants.WILDCARD_APP_CONTEXT_ROOT + "/anythingGoes";
        String expectedRoute = Constants.WILDCARD_APP_CONTEXT_ROOT + "/\\*";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(WILDCARD_SERVICE_NAME, getContainerCollectorMetrics(container), expectedRoute, responseStatus, requestMethod));

    }

    @Test
    public void cs1_subWildCardCard() throws Exception {
        // path -> <app>/sub/*

        assertTrue(server.isStarted());

        String route = Constants.WILDCARD_APP_CONTEXT_ROOT + "/sub/aloha";
        String expectedRoute = Constants.WILDCARD_APP_CONTEXT_ROOT + "/sub/\\*";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(WILDCARD_SERVICE_NAME, getContainerCollectorMetrics(container), expectedRoute, responseStatus, requestMethod));

    }

    @Test
    public void cs1_subSubWildCardCard() throws Exception {
        // path -> <app>/sub/sub/*

        assertTrue(server.isStarted());

        String route = Constants.WILDCARD_APP_CONTEXT_ROOT + "/sub/sub/bonjourno";
        String expectedRoute = Constants.WILDCARD_APP_CONTEXT_ROOT + "/sub/sub/\\*";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(WILDCARD_SERVICE_NAME, getContainerCollectorMetrics(container), expectedRoute, responseStatus, requestMethod));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        //catch if a server is still running.
        if (server != null && server.isStarted()) {
            server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWPMI2006W", "CWMMC0013E", "CWWKG0033W", "SRVE0190E", "SRVE0315E", "SRVE0777E");
        }
    }
}
