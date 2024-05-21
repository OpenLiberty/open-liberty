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

import java.util.Scanner;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

@RunWith(FATRunner.class)
public class RestApplicationTest extends BaseTestClass {

    private static Class<?> c = RestApplicationTest.class;

    @Server("RestServer")
    public static LibertyServer server;

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
    }

    @AfterClass
    public static void afterClass() throws Exception {
        //catch if a server is still running.
        if (server != null && server.isStarted()) {
            server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWPMI2006W", "CWMMC0013E", "CWWKG0033W");
        }
    }

    @Test
    public void normalPathGet() throws Exception {
        final String method = "normalPathGet";

        assertTrue(server.isStarted());

        //Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        server.setMarkToEndOfLog();
        String route = "/RestApp/resource/normalPathGet";
        String requestMethod = "GET";
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, HttpMethod.GET);

        String vendorMetricsOutput = requestHttpSecureServlet("/metrics?scope=vendor", server, HttpMethod.GET);
        Log.info(c, method, vendorMetricsOutput);

        assertTrue(validatePrometheusHTTPMetric(vendorMetricsOutput, route, responseStatus, requestMethod));

    }

    @Test
    public void normalPathPost() throws Exception {
        final String method = "normalPathGet";

        assertTrue(server.isStarted());

        //Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        server.setMarkToEndOfLog();
        String route = "/RestApp/resource/normalPathPost";
        String requestMethod = "POST";
        String responseStatus = "200";

        String res = requestHttpServlet(route, server, HttpMethod.POST);

        String vendorMetricsOutput = requestHttpSecureServlet("/metrics?scope=vendor", server, HttpMethod.GET);
        Log.info(c, method, vendorMetricsOutput);

        assertTrue(validatePrometheusHTTPMetric(vendorMetricsOutput, route, responseStatus, requestMethod));

    }

    private boolean validatePrometheusHTTPMetric(String vendorMetricsOutput, String route, String responseStatus, String requestMethod) {
        return validatePrometheusHTTPMetric(vendorMetricsOutput, route, responseStatus, requestMethod, null);
    }

    private boolean validatePrometheusHTTPMetric(String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String count) {

        if (count == null) {
            count = "[0-9]+\\.[0-9]+";
        }

        String matchString = "http_server_request_duration_seconds_count\\{error_type=\"\",http_route=\"" + route
                             + "\",http_scheme=\"http\",mp_scope=\"vendor\",network_name=\"HTTP\",network_version=\"1\\.[01]\",request_method=\"" + requestMethod
                             + "\",response_status=\"" + responseStatus + "\",server_name=\"localhost\",server_port=\"[0-9]+\",\\} " + count;

        try (Scanner sc = new Scanner(vendorMetricsOutput)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                /*
                 * Skip things we don't care about for perfomance
                 */
                if (!line.startsWith("http_server_request_duration_seconds_count")) {
                    continue;
                }

                if (line.matches(matchString)) {
                    Log.info(c, "validatePrometheusHTTPMetric", "Matched With line: " + line);
                    return true;
                }
            }
        }

        return false;
    }

}
