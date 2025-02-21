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

import static org.junit.Assert.assertEquals;

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
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.Assert;
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

    protected String getContainerCollectorMetrics(GenericContainer<?> container) {
        String containerCollectorMetrics = requestContainerHttpServlet("/metrics", container.getHost(), container.getMappedPort(8889), HttpMethod.GET, null);
        Log.info(c, "getContainerCollectorMetrics", containerCollectorMetrics);
        return containerCollectorMetrics;
    }

    /**
     *
     * @param vendorMetricsOutput Provide /metrics output to use for regex matching
     * @param route               Specified http route to check in regex matching.
     * @param responseStatus      Specified response status to check in regex matching.
     * @param requestMethod       Specified requestMethod to check in regex matching.
     * @return
     */
    protected boolean validateMpMetricsHttp(String vendorMetricsOutput, String route, String responseStatus, String requestMethod) {
        return validateMpMetricsHttp(vendorMetricsOutput, route, responseStatus, requestMethod, null);
    }

    /**
     *
     * @param vendorMetricsOutput Provide /metrics output to use for regex matching
     * @param route               Specified http route to check in regex matching.
     * @param responseStatus      Specified response status to check in regex matching.
     * @param requestMethod       Specified requestMethod to check in regex matching.
     * @param errorType           Specified errorType to check in regex matching.
     * @return
     */
    protected boolean validateMpMetricsHttp(String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType) {
        return validateMpMetricsHttp(vendorMetricsOutput, route, responseStatus, requestMethod, errorType, null, null);
    }

    /**
     *
     * @param vendorMetricsOutput Provide /metrics output to use for regex matching
     * @param route               Specified http route to check in regex matching.
     * @param responseStatus      Specified response status to check in regex matching.
     * @param requestMethod       Specified requestMethod to check in regex matching.
     * @param errorType           Specified errorType to check in regex matching.
     * @param expectedCount       Specify the regex to match with or use ">number" (i.e., ">5") to check if value is greater than number. If null will default to checking greater
     *                                than zero
     * @param expectedSum         Specify the regex to match for the sum value of the http metric
     * @return
     */
    protected boolean validateMpMetricsHttp(String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType, String expectedCount,
                                            String expectedSum) {
        if (errorType == null) {
            errorType = "";
        }

        String countMatchString = "http_server_request_duration_seconds_count\\{error_type=\"" + errorType
                                  + "\",http_request_method=\""
                                  + requestMethod
                                  + "\",http_response_status_code=\"" + responseStatus
                                  + ((route != null) ? ("\",http_route=\"" + route) : "\",http_route=\"")
                                  + "\",mp_scope=\"vendor\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\",\\} ";

        String sumMatchString = "http_server_request_duration_seconds_sum\\{error_type=\"" + errorType
                                + "\",http_request_method=\""
                                + requestMethod
                                + "\",http_response_status_code=\"" + responseStatus
                                + ((route != null) ? ("\",http_route=\"" + route) : "\",http_route=\"")
                                + "\",mp_scope=\"vendor\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\",\\} ";

        return validatePrometheusHTTPMetricCount(vendorMetricsOutput, route, responseStatus, requestMethod, errorType, expectedCount, countMatchString) &&
               validatePrometheusHTTPMetricSum(vendorMetricsOutput, route, responseStatus, requestMethod, errorType, expectedSum, sumMatchString);
    }

    /**
     *
     * @param appName             Specified app name (or service name)
     * @param vendorMetricsOutput Provide /metrics output to use for regex matching
     * @param route               Specified http route to check in regex matching.
     * @param responseStatus      Specified response status to check in regex matching.
     * @param requestMethod       Specified requestMethod to check in regex matching.
     * @return
     */
    protected boolean validateMpTelemetryHttp(String appName, String vendorMetricsOutput, String route, String responseStatus, String requestMethod) {
        return validateMpTelemetryHttp(appName, vendorMetricsOutput, route, responseStatus, requestMethod, null);
    }

    /**
     *
     * @param appName             Specified app name (or service name)
     * @param vendorMetricsOutput Provide /metrics output to use for regex matching
     * @param route               Specified http route to check in regex matching.
     * @param responseStatus      Specified response status to check in regex matching.
     * @param requestMethod       Specified requestMethod to check in regex matching.
     * @param errorType           Specified errorType to check in regex matching.
     * @return
     */
    protected boolean validateMpTelemetryHttp(String appName, String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType) {
        return validateMpTelemetryHttp(appName, vendorMetricsOutput, route, responseStatus, requestMethod, errorType, null, null);
    }

    /**
     *
     * @param appName             Specified app name (or service name)
     * @param vendorMetricsOutput Provide /metrics output to use for regex matching
     * @param route               Specified http route to check in regex matching.
     * @param responseStatus      Specified response status to check in regex matching.
     * @param requestMethod       Specified requestMethod to check in regex matching.
     * @param errorType           Specified errorType to check in regex matching.
     * @param expectedCount       Specify the regex to match with or use ">number" (i.e., ">5") to check if value is greater than number. If null will default to checking greater
     *                                than zero
     * @param expectedSum         Specify the regex to match for the sum value of the http metric
     * @return
     */
    protected boolean validateMpTelemetryHttp(String appName, String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType,
                                              String expectedCount, String expectedSum) {
        String countMatchString = null;
        String sumMatchString = null;

        /*
         * Otel Prometheus output not bound by the Prometheus client issue where same labels are needed
         */
        if (errorType == null) {
            countMatchString = "http_server_request_duration_seconds_count\\{http_request_method=\""
                               + requestMethod
                               + "\",http_response_status_code=\"" + responseStatus
                               + ((route != null) ? ("\",http_route=\"" + route) : "")
                               + "\",instance=\"[a-zA-Z0-9-]*\""
                               + ",job=\"" + appName
                               + "\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\"\\} ";

            sumMatchString = "http_server_request_duration_seconds_sum\\{http_request_method=\""
                             + requestMethod
                             + "\",http_response_status_code=\"" + responseStatus
                             + ((route != null) ? ("\",http_route=\"" + route) : "")
                             + "\",instance=\"[a-zA-Z0-9-]*\""
                             + ",job=\"" + appName
                             + "\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\"\\} ";
        } else {
            countMatchString = "http_server_request_duration_seconds_count\\{error_type=\"" + errorType
                               + "\",http_request_method=\""
                               + requestMethod
                               + "\",http_response_status_code=\"" + responseStatus
                               + ((route != null) ? ("\",http_route=\"" + route) : "")
                               + "\",instance=\"[a-zA-Z0-9-]*\""
                               + ",job=\"" + appName
                               + "\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\"\\} ";

            sumMatchString = "http_server_request_duration_seconds_sum\\{error_type=\"" + errorType
                             + "\",http_request_method=\""
                             + requestMethod
                             + "\",http_response_status_code=\"" + responseStatus
                             + ((route != null) ? ("\",http_route=\"" + route) : "")
                             + "\",instance=\"[a-zA-Z0-9-]*\""
                             + ",job=\"" + appName
                             + "\",network_protocol_version=\"1\\.[01]\",server_address=\"localhost\",server_port=\"[0-9]+\",url_scheme=\"http\"\\} ";
        }

        return validatePrometheusHTTPMetricCount(vendorMetricsOutput, route, responseStatus, requestMethod, errorType, expectedCount, countMatchString)
               && validatePrometheusHTTPMetricSum(vendorMetricsOutput, route, responseStatus, requestMethod, errorType, expectedSum, sumMatchString);
    }

    private boolean validatePrometheusHTTPMetricCount(String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType, String expectedCount,
                                                      String matchString) {
        boolean isGreaterThanCountCheck = false;
        int greaterThanVal = 0;

        /*
         * Account for both MP Metrics (double)
         * and OTel Collector output (integer)
         */
        if (expectedCount == null || expectedCount.startsWith(">")) {

            /*
             * If the greater than check was specified, verify it is legitimate
             * otherwise, we'll just check it's greater than 0 (i.e., default).
             */
            if (expectedCount != null && expectedCount.startsWith(">") && expectedCount.matches(">[0-9]+")) {
                greaterThanVal = Integer.valueOf(expectedCount.split(">")[1]);
            }

            //Whether null or checking value is greater than 'x', we'll use same regex.
            expectedCount = "[0-9]+[.]?[0-9]*";
            isGreaterThanCountCheck = true;

        }

        matchString += expectedCount;

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
                    if (isGreaterThanCountCheck) {
                        String[] split = line.split(" "); // should be only one space at the very end.
                        assertEquals("Error. Expected 2 indexes from split " + Arrays.toString(split), split.length, 2);

                        double countVal = Double.parseDouble(split[1].trim());

                        if (countVal <= greaterThanVal) {
                            Log.info(c, "validatePrometheusHTTPMetricCount", String.format("Expected count value to be greater than [%d]", greaterThanVal));
                            return false;
                        }
                    }

                    return true;
                }
            }
        }
        Log.info(c, "validatePrometheusHTTPMetricCount", String.format("Could not find matching count entry"));
        return false;
    }

    private boolean validatePrometheusHTTPMetricSum(String vendorMetricsOutput, String route, String responseStatus, String requestMethod, String errorType, String expectedSum,
                                                    String matchString) {

        boolean isDefaultCountCheck = false;

        /*
         * For Open Telemetry, a zero would be 0.
         * The existence of a period indicates that something has been recorded
         */
        if (expectedSum == null) {
            expectedSum = "[0-9]+\\.[0-9]*[eE]?-?[0-9]+";
            isDefaultCountCheck = true;
        }

        matchString += expectedSum;

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

                        double sumVal = Double.parseDouble(split[1].trim());

                        if (sumVal <= 0) {
                            Log.info(c, "validatePrometheusHTTPMetricSum", String.format("Expected sum value to be greater than 0"));
                            return false;
                        }
                    }

                    return true;
                }
            }
        }
        Log.info(c, "validatePrometheusHTTPMetricSum", String.format("Could not find matching sum entry"));

        return false;
    }

    protected boolean checkMBeanRegistered(LibertyServer server, String objectName) throws Exception {
        //Get request automatically checks registration
        boolean result = false;

        String response = requestHttpServlet("/MBeanGetter/MBeanGetterServlet", server, HttpMethod.GET, "objectname=" + objectName);
        response = response.trim();
        if (response.equalsIgnoreCase("true")) {
            result = true;
        } else {
            result = false;
            Log.info(c, "checkMBeanRegistered", "Checking for Mbean registration failed. Here is the list of registered HTTP Mbeans: \n" + response);
        }

        return result;

    }

    /**
     * Waits one second before checking the condition. Will wait 1 second for every retry amount. Uses the defaultof 5 seconds.
     *
     * @param condition condition being evaluated
     * @throws InterruptedException
     */
    protected void assertTrueRetryWithTimeout(Supplier<Boolean> condition) throws InterruptedException {
        assertTrueRetryWithTimeout(condition, 8);
    }

    /**
     * Waits one second before checking the condition. Will wait 1 second for every retry amount.
     *
     * @param condition  condition being evaluated
     * @param maxTimeOut in seconds
     * @throws InterruptedException
     */
    protected void assertTrueRetryWithTimeout(Supplier<Boolean> condition, int maxRetries) throws InterruptedException {
        for (int x = 0; x <= maxRetries; x++) {
            TimeUnit.SECONDS.sleep(1);

            if (condition.get() == true) {
                Log.info(c, "assertTrueRetryWithTimeout", String.format("It took %d retries and %d seconds of waiting to be succesful)", x, (x + 1)));
                return;
            }
            Log.info(c, "assertTrueRetryWithTimeout", "Failed to succeed on try number " + x);
        }
        Assert.fail(String.format("We've gone through the maximum retries [%d] and have waited a total of %d seconds", maxRetries, (maxRetries + 1)));
    }
}
