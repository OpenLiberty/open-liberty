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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

@RunWith(FATRunner.class)
public class ServletApplicationTest extends BaseTestClass {

    private static Class<?> c = ServletApplicationTest.class;

    static final String SIMPLE_APP_CONTEXT_ROOT = "/ServletApp";

    static final String WILDCARD_APP_CONTEXT_ROOT = "/WildCardServlet";

    static final String SIMPLE_SERVLET_URL = SIMPLE_APP_CONTEXT_ROOT + "/simpleServlet";

    static final String FAIL_SERVLET_URL = SIMPLE_APP_CONTEXT_ROOT + "/failServlet";

    static final String SUB1_SERVLET_URL = SIMPLE_APP_CONTEXT_ROOT + "/sub";

    static final String SUB2_SERVLET_URL = SIMPLE_APP_CONTEXT_ROOT + "/sub/sub";

    @Server("SimpleServletServer")
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        trustAll();
        WebArchive simpleSerletWAR = ShrinkWrap
                        .create(WebArchive.class, "ServletApp.war")
                        .addPackage(
                                    "io.openliberty.http.monitor.fat.servletApp");

        WebArchive wildCardServletWAR = ShrinkWrap
                        .create(WebArchive.class, "WildCardServlet.war")
                        .addPackage(
                                    "io.openliberty.http.monitor.fat.wildCardServletApp");

        ShrinkHelper.exportDropinAppToServer(server, simpleSerletWAR,
                                             DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportDropinAppToServer(server, wildCardServletWAR, DeployOptions.SERVER_ONLY);

        server.startServer();

        //Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        server.setMarkToEndOfLog();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        //catch if a server is still running.
        if (server != null && server.isStarted()) {
            server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWPMI2006W", "CWMMC0013E", "CWWKG0033W", "SRVE0190E", "SRVE0315E", "SRVE0777E");
        }
    }

    @Test
    public void s1_simplePathGet() throws Exception {

        assertTrue(server.isStarted());

        String route = SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    public void s1_simplePathPost() throws Exception {
        assertTrue(server.isStarted());

        String route = SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.POST;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    public void s1_simplePathPut() throws Exception {

        assertTrue(server.isStarted());

        String route = SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.PUT;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    public void s1_simplePathDelete() throws Exception {

        assertTrue(server.isStarted());

        String route = SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.DELETE;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    public void s1_simplePathOptions() throws Exception {

        assertTrue(server.isStarted());

        String route = SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.OPTIONS;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    public void s1_simplePathHead() throws Exception {

        assertTrue(server.isStarted());

        String route = SIMPLE_SERVLET_URL;
        String requestMethod = HttpMethod.HEAD;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    @AllowedFFDC
    public void s1_failDivZero() throws Exception {

        assertTrue(server.isStarted());

        String route = FAIL_SERVLET_URL;
        String requestMethod = HttpMethod.GET;
        String responseStatus = "500";
        String errorType = responseStatus;

        String res = requestHttpServlet(route, server, requestMethod, "failMode=zero");

        assertTrue(validatePrometheusHTTPMetricWithErrorType(getVendorMetrics(server), route, responseStatus, requestMethod, errorType));

    }

    @Test
    @AllowedFFDC
    public void s1_failNonExistentPath() throws Exception {

        assertTrue(server.isStarted());

        String route = SIMPLE_APP_CONTEXT_ROOT + "/nonExistent";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "404";
        String resolvedRoute = SIMPLE_APP_CONTEXT_ROOT + "/\\*";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), resolvedRoute, responseStatus, requestMethod));

    }

    @Test
    public void s1_failCustom() throws Exception {

        assertTrue(server.isStarted());

        String route = FAIL_SERVLET_URL;
        String requestMethod = HttpMethod.GET;
        String responseStatus = "456";

        String res = requestHttpServlet(route, server, requestMethod, "failMode=custom");

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    @AllowedFFDC
    public void s1_failIOE() throws Exception {

        assertTrue(server.isStarted());

        String route = FAIL_SERVLET_URL;
        String requestMethod = HttpMethod.GET;
        String responseStatus = "500";
        String errorType = responseStatus;

        String res = requestHttpServlet(route, server, requestMethod, "failMode=io");

        assertTrue(validatePrometheusHTTPMetricWithErrorType(getVendorMetrics(server), route, responseStatus, requestMethod, errorType));

    }

    @Test
    @AllowedFFDC
    public void s1_failIAE() throws Exception {

        assertTrue(server.isStarted());

        String route = FAIL_SERVLET_URL;
        String requestMethod = HttpMethod.GET;
        String responseStatus = "500";
        String errorType = responseStatus;

        String res = requestHttpServlet(route, server, requestMethod, "failMode=iae");

        assertTrue(validatePrometheusHTTPMetricWithErrorType(getVendorMetrics(server), route, responseStatus, requestMethod, errorType));

    }

    @Test
    public void s1_rootWildCard() throws Exception {
        // path -> <app>/*

        assertTrue(server.isStarted());

        String route = WILDCARD_APP_CONTEXT_ROOT + "/anythingGoes";
        String expectedRoute = WILDCARD_APP_CONTEXT_ROOT + "/\\*";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), expectedRoute, responseStatus, requestMethod));

    }

    @Test
    public void s1_subWildCardCard() throws Exception {
        // path -> <app>/sub/*

        assertTrue(server.isStarted());

        String route = WILDCARD_APP_CONTEXT_ROOT + "/sub/aloha";
        String expectedRoute = WILDCARD_APP_CONTEXT_ROOT + "/sub/\\*";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), expectedRoute, responseStatus, requestMethod));

    }

    @Test
    public void s1_subSubWildCardCard() throws Exception {
        // path -> <app>/sub/sub/*

        assertTrue(server.isStarted());

        String route = WILDCARD_APP_CONTEXT_ROOT + "/sub/sub/bonjourno";
        String expectedRoute = WILDCARD_APP_CONTEXT_ROOT + "/sub/sub/\\*";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validatePrometheusHTTPMetric(getVendorMetrics(server), expectedRoute, responseStatus, requestMethod));

    }

}
