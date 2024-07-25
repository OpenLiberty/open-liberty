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
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
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

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

@RunWith(FATRunner.class)
public class ContainerJSPApplicationTest extends BaseTestClass {

    private static Class<?> c = ContainerJSPApplicationTest.class;

    @Server("ContainerJSPServer")
    public static LibertyServer server;

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
        WebArchive testWAR = ShrinkWrap
                        .create(WebArchive.class, "jspApp.war")
                        .addAsWebInfResource(new File("test-applications/JspApp/resource/WEB-INF/web.xml"))
                        .add(new FileAsset(new File("test-applications/JspApp/resource/configured.jsp")), "/configured.jsp")
                        .add(new FileAsset(new File("test-applications/JspApp/resource/unconfigured.jsp")), "/unconfigured.jsp")
                        .add(new FileAsset(new File("test-applications/JspApp/resource/default.html")), "/default.html")
                        .add(new FileAsset(new File("test-applications/JspApp/resource/Testhtml.html")), "Testhtml.html")
                        .addPackage(
                                    "io.openliberty.http.monitor.fat.jspApp")
                        .addAsManifestResource(new File("publish/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties");

        // test-applications\JspApp\src\io\openliberty\http\monitor\fat\jspApp\resource
        ShrinkHelper.exportDropinAppToServer(server, testWAR,
                                             DeployOptions.SERVER_ONLY);

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
            server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWPMI2006W", "CWMMC0013E", "CWWKG0033W", "SRVE0315E");
        }
    }

    @Test
    public void jsp_noWebXmlJSP() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.JSP_CONTEXT_ROOT + "/unconfigured.jsp";
        String expectedRoute = Constants.JSP_CONTEXT_ROOT + "/\\*.jsp";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        TimeUnit.SECONDS.sleep(4);
        assertTrue(validateMpTelemetryHttp(Constants.JSP_APP, getContainerCollectorMetrics(container), expectedRoute, responseStatus, requestMethod));

    }

    @Test
    public void jsp_configredWebXmlJSP() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.JSP_CONTEXT_ROOT + "/configured";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        TimeUnit.SECONDS.sleep(4);
        assertTrue(validateMpTelemetryHttp(Constants.JSP_APP, getContainerCollectorMetrics(container), route, responseStatus, requestMethod));

    }

    @Test
    public void jsp_defaultHTML() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.JSP_CONTEXT_ROOT;
        String expectedRoute = Constants.JSP_CONTEXT_ROOT + "/";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        TimeUnit.SECONDS.sleep(4);
        assertTrue(validateMpTelemetryHttp(Constants.JSP_APP, getContainerCollectorMetrics(container), expectedRoute, responseStatus, requestMethod));

        route = Constants.JSP_CONTEXT_ROOT + "/Testhtml.html";
        expectedRoute = Constants.JSP_CONTEXT_ROOT + "/\\*";
        requestMethod = HttpMethod.GET;
        responseStatus = "200";

        res = requestHttpServlet(route, server, requestMethod);
        //Allow time for the collector to receive and expose metrics
        TimeUnit.SECONDS.sleep(4);
        assertTrue(validateMpTelemetryHttp(Constants.JSP_APP, getContainerCollectorMetrics(container), expectedRoute, responseStatus, requestMethod));
    }

}
