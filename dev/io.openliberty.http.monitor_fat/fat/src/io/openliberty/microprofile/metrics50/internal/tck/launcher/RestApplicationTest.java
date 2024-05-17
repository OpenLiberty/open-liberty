/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.metrics50.internal.tck.launcher;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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

@RunWith(FATRunner.class)
public class RestApplicationTest {

    private static Class<?> c = RestApplicationTest.class;

    @Server("RestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

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

        String res = getHttpServlet(route);

        String vendorMetricsOutput = getHttpServlet("/metrics?scope=vendor");
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

        String res = postHttpServlet(route);

        String vendorMetricsOutput = getHttpServlet("/metrics?scope=vendor");
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
                Log.info(c, "validatePrometheusHTTPMetric", "line is " + line);

                if (line.matches(matchString)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void trustAll() throws Exception {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } }, new SecureRandom());
            SSLContext.setDefault(sslContext);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            Log.error(c, "trustAll", e);
        }
    }

    private String getHttpsServlet(String servletPath) throws Exception {
        HttpsURLConnection con = null;
        try {
            String sURL = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + servletPath;
            Log.info(c, "getHttpsServlet", sURL);
            URL checkerServletURL = new URL(sURL);
            con = (HttpsURLConnection) checkerServletURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
            String authorization = "Basic "
                                   + Base64.getEncoder().encodeToString(("theUser:thePassword").getBytes(StandardCharsets.UTF_8)); // Java
                                                                                                                                                                   // 8
            con.setRequestProperty("Authorization", authorization);
            con.setRequestProperty("Accept", "text/plain");
            con.setRequestMethod("GET");

            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                if (!line.startsWith("#"))
                    lines.append(line).append(sep);
            }
            Log.info(c, "getHttpsServlet", sURL);
            return lines.toString();
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    private String getHttpServlet(String servletPath) throws Exception {
        HttpURLConnection con = null;
        try {
            String sURL = "http://" + server.getHostname() + ":"
                          + server.getHttpDefaultPort() + servletPath;
            URL checkerServletURL = new URL(sURL);
            con = (HttpURLConnection) checkerServletURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }
            Log.info(c, "getHttpServlet", sURL);
            return lines.toString();
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    private String postHttpServlet(String servletPath) throws Exception {
        HttpURLConnection con = null;
        try {
            String sURL = "http://" + server.getHostname() + ":"
                          + server.getHttpDefaultPort() + servletPath;
            URL checkerServletURL = new URL(sURL);
            con = (HttpURLConnection) checkerServletURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("POST");
            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }
            Log.info(c, "postHttpServlet", sURL);
            return lines.toString();
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

}
