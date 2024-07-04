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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

/**
 *
 */
public abstract class BaseTestClass {

    protected Class<?> c = this.getClass();

    protected static final String PATH_TO_AUTOFVT_TESTFILES = "lib/LibertyFATTestFiles/";

    protected static final String IMAGE_NAME = ImageNameSubstitutor.instance() //
                    .apply(DockerImageName.parse("otel/opentelemetry-collector-contrib:0.103.0")).asCanonicalNameString();

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
            Log.info(c, "requestHttpSecureServlet", sURL);
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
            Log.info(c, "requestHttpServlet", "Encountered IO exception " + e);
            return null;
        } catch (Exception e) {
            Log.info(c, "requestHttpServlet", "Encountered an exception " + e);
            return null;
        } finally {
            if (con != null)
                con.disconnect();
        }

    }

    protected String requestContainerHttpServlet(String servletPath, String host, int port, String requestMethod, String query) {
        HttpURLConnection con = null;
        try {
            String sURL = "http://" + host + ":"
                          + port + servletPath
                          + ((query != null) ? ("?" + query) : "");

            Log.info(c, "requestContainerHttpServlet", sURL);

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
            return lines.toString();
        } catch (IOException e) {
            Log.info(c, "requestContainerHttpServlet", "Encountered IO exception " + e);
            return null;
        } catch (Exception e) {
            Log.info(c, "requestContainerHttpServlet", "Encountered an exception " + e);
            return null;
        } finally {
            if (con != null)
                con.disconnect();
        }

    }

    protected String getVendorMetrics(LibertyServer server) throws Exception {

        TimeUnit.MILLISECONDS.sleep(500);

        String vendorMetricsOutput = requestHttpServlet("/metrics?scope=vendor", server, HttpMethod.GET);
        Log.info(c, "getVendorMetrics", vendorMetricsOutput);
        return vendorMetricsOutput;
    }

    protected String getContainerCollectorMetrics(GenericContainer<?> container) throws Exception {
        String containerCollectorMetrics = requestContainerHttpServlet("/metrics", container.getHost(), container.getMappedPort(8889), HttpMethod.GET, null);
        Log.info(c, "getContainerCollectorMetrics", containerCollectorMetrics);
        return containerCollectorMetrics;
    }

    /*
     * MP Metrics
     */
    protected boolean validateMpMetricsHttp(String vendorMetricsOutput, String route, String responseStatus, String requestMethod) {
        return validateMpMetricsHttp(vendorMetricsOutput, route, responseStatus, requestMethod, null);
    }

    protected boolean validateMpMetricsHttp(String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType) {
        return validateMpMetricsHttp(vendorMetricsOutput, route, responseStatus, requestMethod, errorType, null);
    }

    protected boolean validateMpMetricsHttp(String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType, String count) {
        if (errorType == null) {
            errorType = "";
        }

        String countMatchString = "http_server_request_duration_seconds_count\\{error_type=\"" + errorType
                                  + "\",http_request_method=\""
                                  + requestMethod
                                  + "\",http_response_status_code=\"" + responseStatus
                                  + "\",http_route=\"" + route
                                  + "\",mp_scope=\"vendor\",network_protocol_name=\"HTTP\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\",\\} ";

        String sumMatchString = "http_server_request_duration_seconds_sum\\{error_type=\"" + errorType
                                + "\",http_request_method=\""
                                + requestMethod
                                + "\",http_response_status_code=\"" + responseStatus
                                + "\",http_route=\"" + route
                                + "\",mp_scope=\"vendor\",network_protocol_name=\"HTTP\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\",\\} ";

        return validatePrometheusHTTPMetricCount(vendorMetricsOutput, route, responseStatus, requestMethod, errorType, count, countMatchString) &&
               validatePrometheusHTTPMetricSum(vendorMetricsOutput, route, responseStatus, requestMethod, errorType, count, sumMatchString);
    }

    /*
     * MP Telemetry
     */
    protected boolean validateMpTelemetryHttp(String appName, String vendorMetricsOutput, String route, String responseStatus, String requestMethod) {
        return validateMpTelemetryHttp(appName, vendorMetricsOutput, route, responseStatus, requestMethod, null);
    }

    protected boolean validateMpTelemetryHttp(String appName, String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType) {
        return validateMpTelemetryHttp(appName, vendorMetricsOutput, route, responseStatus, requestMethod, errorType, null);
    }

    protected boolean validateMpTelemetryHttp(String appName, String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType,
                                              String count) {
        String countMatchString = null;
        String sumMatchString = null;

        /*
         * Otel Prometheus output not bound by the Prometheus client issue where same labels are needed
         */
        if (errorType == null) {
            countMatchString = "http_server_request_duration_seconds_count\\{http_request_method=\""
                               + requestMethod
                               + "\",http_response_status_code=\"" + responseStatus
                               + "\",http_route=\"" + route
                               + "\",job=\"" + appName
                               + "\",network_protocol_name=\"HTTP\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\"\\} ";

            sumMatchString = "http_server_request_duration_seconds_sum\\{http_request_method=\""
                             + requestMethod
                             + "\",http_response_status_code=\"" + responseStatus
                             + "\",http_route=\"" + route
                             + "\",job=\"" + appName
                             + "\",network_protocol_name=\"HTTP\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\"\\} ";
        } else {
            countMatchString = "http_server_request_duration_seconds_count\\{error_type=\"" + errorType
                               + "\",http_request_method=\""
                               + requestMethod
                               + "\",http_response_status_code=\"" + responseStatus
                               + "\",http_route=\"" + route
                               + "\",job=\"" + appName
                               + "\",network_protocol_name=\"HTTP\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\"\\} ";

            sumMatchString = "http_server_request_duration_seconds_sum\\{error_type=\"" + errorType
                             + "\",http_request_method=\""
                             + requestMethod
                             + "\",http_response_status_code=\"" + responseStatus
                             + "\",http_route=\"" + route
                             + "\",job=\"" + appName
                             + "\",network_protocol_name=\"HTTP\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\"\\} ";
        }

        return validatePrometheusHTTPMetricCount(vendorMetricsOutput, route, responseStatus, requestMethod, errorType, count, countMatchString)
               && validatePrometheusHTTPMetricSum(vendorMetricsOutput, route, responseStatus, requestMethod, errorType, count, sumMatchString);
    }

    /*
     * For all
     */
    private boolean validatePrometheusHTTPMetricCount(String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType, String count,
                                                      String matchString) {
        boolean isDefaultCountCheck = false;

        /*
         * Account for both MP Metrics (double)
         * and OTel Collector output (integer)
         */
        if (count == null) {
            count = "[0-9]+[.]?[0-9]*";
            isDefaultCountCheck = true;
        }

        matchString += count;

        Log.info(c, "validatePrometheusHTTPMetricCount", "Trying to match: " + matchString);
        try (Scanner sc = new Scanner(vendorMetricsOutput)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                /*
                 * Skip things we don't care about for perfomance
                 */
                if (!line.startsWith("http_server_request_duration_seconds_count")) {
                    continue;
                }
                Log.info(c, "validatePrometheusHTTPMetricCount", "Potential match with: " + line);
                if (line.matches(matchString)) {
                    Log.info(c, "validatePrometheusHTTPMetricCount", "Matched with: " + line);

                    /*
                     * If no custom count regex was supplied.
                     * We will check if value is greater than 0
                     */
                    if (isDefaultCountCheck) {
                        String[] split = line.split(" "); // should be only one space at the very end.
                        assertEquals("Error. Expected 2 indexes from split " + Arrays.toString(split), split.length, 2);

                        double countVal = Double.parseDouble(split[1].trim());
                        assertTrue("Expected count value to be greater than 0", countVal > 0);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private boolean validatePrometheusHTTPMetricSum(String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType, String count,
                                                    String matchString) {

        boolean isDefaultCountCheck = false;

        /*
         * For Open Telemetry, a zero would be 0.
         * The existence of a period indicates that something has been recorded
         */
        if (count == null) {
            count = "[0-9]+\\.[0-9]*[eE]?-?[0-9]+";
            isDefaultCountCheck = true;
        }

        matchString += count;

        Log.info(c, "validatePrometheusHTTPMetricSum", "Trying to match: " + matchString);
        try (Scanner sc = new Scanner(vendorMetricsOutput)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                /*
                 * Skip things we don't care about for perfomance
                 */
                if (!line.startsWith("http_server_request_duration_seconds_sum")) {
                    continue;
                }
                Log.info(c, "validatePrometheusHTTPMetricSum", "Potential match with: " + line);
                if (line.matches(matchString)) {
                    Log.info(c, "validatePrometheusHTTPMetricSum", "Matched with: " + line);

                    /*
                     * If no custom count regex was supplied.
                     * We will check if value is greater than 0
                     */
                    if (isDefaultCountCheck) {
                        String[] split = line.split(" "); // should be only one space at the very end.
                        assertEquals("Error. Expected 2 indexes from split " + Arrays.toString(split), split.length, 2);

                        double countVal = Double.parseDouble(split[1].trim());
                        assertTrue("Expected sum value to be greater than 0", countVal > 0);
                    }

                    return true;
                }
            }
        }

        return false;
    }
}
