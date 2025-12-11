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

import javax.ws.rs.HttpMethod;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 *
 * Tests the different Request methods to the restful resource.
 * Tests throwing exceptions.
 * Tests path parameters and query parameters.
 */
@RunWith(FATRunner.class)
public class RestApplicationTest extends BaseTestClass {

    private static Class<?> c = RestApplicationTest.class;

    @Server("SimpleRestServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests rt = FATSuite.testRepeatMPTMetrics5("SimpleRestServer");

    @BeforeClass
    public static void beforeClass() throws Exception {
        trustAll();
        WebArchive testWAR = ShrinkWrap
                        .create(WebArchive.class, "RestApp.war")
                        .addPackage(
                                    "io.openliberty.http.monitor.fat.restApp");

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
    public void r1_normalPathGet() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_RESOURCE_URL + "/pathGet";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    public void r1_normalPathPost() throws Exception {
        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_RESOURCE_URL + "/pathPost";
        String requestMethod = HttpMethod.POST;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    public void r1_normalPathPut() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_RESOURCE_URL + "/pathPut";
        String requestMethod = HttpMethod.PUT;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    public void r1_normalPathDelete() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_RESOURCE_URL + "/pathDelete";
        String requestMethod = HttpMethod.DELETE;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    public void r1_normalPathOptions() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_RESOURCE_URL + "/pathOptions";
        String requestMethod = HttpMethod.OPTIONS;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), route, responseStatus, requestMethod));

    }

    @Test
    public void r1_normalPathHead() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_RESOURCE_URL + "/pathHead";
        String requestMethod = HttpMethod.HEAD;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), route, responseStatus, requestMethod));
    }

    @Test
    @AllowedFFDC
    public void r1_failDivZero() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.FAIL_RESOURCE_URL + "/zero";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "500";
        String errorType = responseStatus;

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), route, responseStatus, requestMethod, errorType));

    }

    @Test
    public void r1_nonExistentPath() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.SIMPLE_RESOURCE_URL + "/fakePath";
        String resolvedRoute = Constants.REST_APP_URL + "/\\*";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "404";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), resolvedRoute, responseStatus, requestMethod));

    }

    @Test
    @AllowedFFDC
    public void r1_failThrowIO() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.FAIL_RESOURCE_URL + "/io";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "500";
        String errorType = responseStatus;

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), route, responseStatus, requestMethod, errorType));

    }

    @Test
    @AllowedFFDC
    public void r1_failThrowIAE() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.FAIL_RESOURCE_URL + "/iae";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "500";
        String errorType = responseStatus;

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), route, responseStatus, requestMethod, errorType));

    }

    @Test
    public void r1_params_any() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.PARAM_RESOURCE_URL + "/awooga";
        String expectedRoute = Constants.PARAM_RESOURCE_URL + "/\\{anything\\}";

        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRoute, responseStatus, requestMethod));

    }

    @Test
    public void r1_params_getName() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.PARAM_RESOURCE_URL + "/name/watson";
        String expectedRoute = Constants.PARAM_RESOURCE_URL + "/name/\\{name\\}";

        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRoute, responseStatus, requestMethod));

    }

    @Test
    public void r1_params_postName() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.PARAM_RESOURCE_URL + "/name/watson";
        String expectedRoute = Constants.PARAM_RESOURCE_URL + "/name/\\{name\\}";

        String requestMethod = HttpMethod.POST;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRoute, responseStatus, requestMethod));

    }

    @Test
    public void r1_params_query() throws Exception {

        /*
         * Query params aren't part of the rout.
         * so we jsut expect "query"
         */
        assertTrue(server.isStarted());

        String route = Constants.PARAM_RESOURCE_URL + "/query?useless=this";
        String expectedRoute = Constants.PARAM_RESOURCE_URL + "/query";

        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        Log.info(c, " r1_params_query", "the response is " + res);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRoute, responseStatus, requestMethod));

    }

    @Test
    public void r1_params_queryParam() throws Exception {

        /*
         * Query params aren't part of the rout.
         * so we jsut expect "query"
         */
        assertTrue(server.isStarted());

        String route = Constants.PARAM_RESOURCE_URL + "/pq/peeqeee?useless=this";
        String expectedRoute = Constants.PARAM_RESOURCE_URL + "/pq/\\{pq\\}";

        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, requestMethod);

        assertTrue(validateMpMetricsHttp(getVendorMetrics(server), expectedRoute, responseStatus, requestMethod));

    }

}
