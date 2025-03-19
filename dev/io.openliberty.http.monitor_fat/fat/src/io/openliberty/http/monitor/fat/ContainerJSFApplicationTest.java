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
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

@RunWith(FATRunner.class)
public class ContainerJSFApplicationTest extends BaseTestClass {

    private static Class<?> c = ContainerJSFApplicationTest.class;

    private static final String SERVER_NAME = "ContainerJSFServer";
    private static final String SERVICE_NAME = FATSuite.getAppNameOrUnknownService(Constants.JSF_APP);

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
        WebArchive testWAR = ShrinkWrap
                        .create(WebArchive.class, "jsfApp.war")
                        .addAsWebInfResource(new File("test-applications/JsfApp/resources/WEB-INF/web.xml"))
                        .addAsWebInfResource(new File("test-applications/JsfApp/resources/WEB-INF/faces-config.xml"))
                        .addAsWebInfResource(new File("test-applications/JsfApp/resources/WEB-INF/beans.xml"))
                        .add(new FileAsset(new File("test-applications/JsfApp/resources/index.xhtml")), "/index.xhtml")
                        .add(new FileAsset(new File("test-applications/JsfApp/resources/normal.html")), "/normal.html")
                        .add(new FileAsset(new File("test-applications/JsfApp/resources/resources/css/style.css")), "/resources/css/style.css")
                        .add(new FileAsset(new File("test-applications/JsfApp/resources/resources/js/script.js")), "/resources/js/script.js")
                        .addPackage(
                                    "io.openliberty.http.monitor.fat.jsfApp");

        testWAR = FATSuite.setTelProperties(testWAR, server);

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
    public void cjsf_testNormalHtml() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.JSF_CONTEXT_ROOT + "/normal.html";
        String expectedRoute = Constants.JSF_CONTEXT_ROOT + "/\\*";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), expectedRoute, responseStatus,
                                                                 requestMethod));
    }

    @Test
    public void cjsf_testXHTMLext() throws Exception {

        assertTrue(server.isStarted());

        String routeXHTML = Constants.JSF_CONTEXT_ROOT + "/index.xhtml";
        String routeJSF = Constants.JSF_CONTEXT_ROOT + "/index.jsf";
        String routeFaces = Constants.JSF_CONTEXT_ROOT + "/index.faces";
        String routeFacesNode = Constants.JSF_CONTEXT_ROOT + "/faces/index.xhtml";

        String expectedRouteXHTML = Constants.JSF_CONTEXT_ROOT + "/\\*.xhtml";
        String expectedRouteJSF = Constants.JSF_CONTEXT_ROOT + "/\\*.jsf";
        String expectedRouteFaces = Constants.JSF_CONTEXT_ROOT + "/\\*.faces";
        String expectedRouteFacesNode = Constants.JSF_CONTEXT_ROOT + "/faces/\\*";

        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        /*
         * With JSF 2.2 (and only JSF 2.2) using the same WebClient object to hit all the URLs
         * caches the resources. This means the first URL request will cause the 2 HTTP requests to load the JSF resource.
         *
         * However, with JSF 2.3 and above (and then facex-x.x), using the same WebClient object doe not cache
         * and any subsequent hits will load the resources over and over.
         *
         * For uniformity across the FAT test repeats, we'll use new WebClient object for each request (that uses a different extension/mapping)
         */

        /*
         * Load with .xhtml extension
         */
        WebClient webClient1 = new WebClient();
        String urlXHTML = "http://" + server.getHostname() + ":"
                          + server.getHttpDefaultPort() + routeXHTML;
        HtmlPage page = (HtmlPage) webClient1.getPage(urlXHTML);

        //Expected count 3 : 1 xhtml, 2 resource
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), expectedRouteXHTML, responseStatus, requestMethod, null,
                                                                 ">2",
                                                                 null));

        /*
         * Load with .jsf extension
         */
        WebClient webClient2 = new WebClient();
        String urlJSF = "http://" + server.getHostname() + ":"
                        + server.getHttpDefaultPort() + routeJSF;
        page = (HtmlPage) webClient2.getPage(urlJSF);

        //Expected count 5 : + 2 resource loading (since xhtml loaded resources first, they continue to load form the /[jakarta.javax].faces.resource/*.xhtml path
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), expectedRouteXHTML, responseStatus, requestMethod, null,
                                                                 ">4",
                                                                 null));
        //We expect 1, default signature just checks for greater than 0
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), expectedRouteJSF, responseStatus, requestMethod));

        /*
         * Load with .faces extension
         */
        WebClient webClient3 = new WebClient();
        String urlFaces = "http://" + server.getHostname() + ":"
                          + server.getHttpDefaultPort() + routeFaces;
        page = (HtmlPage) webClient3.getPage(urlFaces);

        //Expected count 7 : + 2 resource loading (since xhtml loaded resources first, they continue to load form the /[jakarta.javax].faces.resource/*.xhtml path
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), expectedRouteXHTML, responseStatus, requestMethod, null,
                                                                 ">6",
                                                                 null));
        //We expect 1, default signature just checks for greater than 0
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), expectedRouteFaces, responseStatus, requestMethod));

        /*
         * Load with /faces/* mapping
         */
        WebClient webClient4 = new WebClient();
        String urlFacesNode = "http://" + server.getHostname() + ":"
                              + server.getHttpDefaultPort() + routeFacesNode;
        page = (HtmlPage) webClient4.getPage(urlFacesNode);

        //Expected count 9 : + 2 resource loading (since xhtml loaded resources first, they continue to load form the /[jakarta.javax].faces.resource/*.xhtml path
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), expectedRouteXHTML, responseStatus, requestMethod, null,
                                                                 ">8",
                                                                 null));
        //We expect 1, default signature just checks for greater than 0
        assertTrueRetryWithTimeout(() -> validateMpTelemetryHttp(SERVICE_NAME, getContainerCollectorMetrics(container), expectedRouteFacesNode, responseStatus, requestMethod));

    }

}
