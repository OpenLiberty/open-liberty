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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

@RunWith(FATRunner.class)
public class JSFApplicationTest extends BaseTestClass {

    private static Class<?> c = JSFApplicationTest.class;

    @Server("JSFServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests rt = FATSuite.testRepeatMPTMetrics5("JSFServer");

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
            server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWPMI2006W", "CWMMC0013E", "CWWKG0033W", "SRVE0315E");
        }
    }

    @Test
    public void jsf_testNormalHtml() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.JSF_CONTEXT_ROOT + "/normal.html";
        String expectedRoute = Constants.JSF_CONTEXT_ROOT + "/\\*";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRoute, responseStatus, requestMethod));

    }

    @Test
    public void jsf_testXHTMLext() throws Exception {

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

        try (WebClient webClient = new WebClient()) {

            String urlXHTML = "http://" + server.getHostname() + ":"
                              + server.getHttpDefaultPort() + routeXHTML;
            HtmlPage page = (HtmlPage) webClient.getPage(urlXHTML);

            //Expected count 3 : 1 xhtml, 2 resource
            assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRouteXHTML, responseStatus, requestMethod, null, ">2", null));

            String urlJSF = "http://" + server.getHostname() + ":"
                            + server.getHttpDefaultPort() + routeJSF;
            page = (HtmlPage) webClient.getPage(urlJSF);

            //Expected count 5 : + 2 resource loading (since xhtml loaded resources first, they continue to load form the /[jakarta.javax].faces.resource/*.xhtml path
            assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRouteXHTML, responseStatus, requestMethod, null, ">4", null));
            //We expect 1, default signature just checks for greater than 0
            assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRouteJSF, responseStatus, requestMethod));

            String urlFaces = "http://" + server.getHostname() + ":"
                              + server.getHttpDefaultPort() + routeFaces;
            page = (HtmlPage) webClient.getPage(urlFaces);

            //Expected count 7 : + 2 resource loading (since xhtml loaded resources first, they continue to load form the /[jakarta.javax].faces.resource/*.xhtml path
            assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRouteXHTML, responseStatus, requestMethod, null, ">6", null));
            //We expect 1, default signature just checks for greater than 0
            assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRouteFaces, responseStatus, requestMethod));

            String urlFacesNode = "http://" + server.getHostname() + ":"
                                  + server.getHttpDefaultPort() + routeFacesNode;
            page = (HtmlPage) webClient.getPage(urlFacesNode);

            //Expected count 9 : + 2 resource loading (since xhtml loaded resources first, they continue to load form the /[jakarta.javax].faces.resource/*.xhtml path
            assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRouteXHTML, responseStatus, requestMethod, null, ">8", null));
            //We expect 1, default signature just checks for greater than 0
            assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRouteFacesNode, responseStatus, requestMethod));

        }
    }

}
