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

import java.io.BufferedReader;
import java.io.IOException;
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

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

/**
 *
 */
public abstract class BaseTestClass {

    protected Class<?> c = this.getClass();

    protected static void trustAll() throws Exception {
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
            Log.error(BaseTestClass.class, "trustAll", e);
        }
    }

    protected String requestHttpSecureServlet(String servletPath, LibertyServer server, String requestMethod) throws Exception {

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
            con.setRequestMethod(requestMethod);

            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                if (!line.startsWith("#"))
                    lines.append(line).append(sep);
            }
            Log.info(c, "requestHttpSecureServlet", sURL);
            return lines.toString();
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    protected String requestHttpServlet(String servletPath, LibertyServer server, String requestMethod) {
        return requestHttpServlet(servletPath, server, requestMethod, null);
    }

    protected String requestHttpServlet(String servletPath, LibertyServer server, String requestMethod, String query) {
        HttpURLConnection con = null;
        try {
            String sURL = "http://" + server.getHostname() + ":"
                          + server.getHttpDefaultPort() + servletPath
                          + ((query != null) ? ("?" + query) : "");
            URL checkerServletURL = new URL(sURL);
            con = (HttpURLConnection) checkerServletURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod(requestMethod);
            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }
            Log.info(c, "requestHttpServlet", sURL);
            return lines.toString();
        } catch (IOException e) {
            Log.info(c, "requestHttpServlet", "Encountered exceptoin " + e);
            return null;
        } finally {
            if (con != null)
                con.disconnect();
        }

    }

    protected String getVendorMetrics(LibertyServer server) throws Exception {
        String vendorMetricsOutput = requestHttpSecureServlet("/metrics?scope=vendor", server, HttpMethod.GET);
        Log.info(c, "getVendorMetrics", vendorMetricsOutput);
        return vendorMetricsOutput;
    }

    protected boolean validatePrometheusHTTPMetric(String vendorMetricsOutput, String route, String responseStatus, String requestMethod) {
        return validatePrometheusHTTPMetric(vendorMetricsOutput, route, responseStatus, requestMethod, null, null);
    }

    protected boolean validatePrometheusHTTPMetricWithErrorType(String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType) {
        return validatePrometheusHTTPMetric(vendorMetricsOutput, route, responseStatus, requestMethod, null, errorType);
    }

    protected boolean validatePrometheusHTTPMetric(String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String count, String errorType) {

        if (count == null) {
            count = "[0-9]+\\.[0-9]+";
        }

        if (errorType == null) {
            errorType = "";
        }
        String matchString = "http_server_request_duration_seconds_count\\{error_type=\"" + errorType + "\",http_route=\"" + route
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
