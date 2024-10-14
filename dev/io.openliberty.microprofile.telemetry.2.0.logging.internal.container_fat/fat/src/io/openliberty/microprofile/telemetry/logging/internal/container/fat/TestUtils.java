/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal.container.fat;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class TestUtils {

    private static Class<?> c = TestUtils.class;
    protected static final String PATH_TO_AUTOFVT_TESTFILES = "lib/LibertyFATTestFiles/";

    protected static final String IMAGE_NAME = ImageNameSubstitutor.instance() //
                    .apply(DockerImageName.parse("otel/opentelemetry-collector-contrib:0.103.0")).asCanonicalNameString();

    public static void runApp(LibertyServer server, String type) throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/MpTelemetryLogApp";
        if (type.equals("logs")) {
            url = url + "/LogURL";
        } else if (type.equals("ffdc1")) {
            url = url + "?isFFDC=true";
        }

        Log.info(c, "runApp", "---> Running the application with url : " + url);

        try {
            runGetMethod(url);
        } catch (Exception e) {
            Log.info(c, "runApp", " ---> Exception : " + e.getMessage());
        }
    }

    protected static String runGetMethod(String urlStr) throws Exception {
        Log.info(c, "runGetMethod", "URL = " + urlStr);
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            return lines.toString();
        } finally {
            con.disconnect();
        }
    }

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
            Log.error(TestUtils.class, "trustAll", e);
        }
    }

    /*
     * Ensure that the container was successfully started. This is done by checking the container logs for the message provided,
     * and will check check the logs every 5 seconds for a total of 60s.
     */
    public static void isContainerStarted(String subString, GenericContainer<?> container) {
        long endTime = System.currentTimeMillis() + Duration.ofSeconds(60).toMillis();
        boolean messageFound = false;

        while (System.currentTimeMillis() < endTime) {
            String logs = container.getLogs();

            if (logs.contains(subString)) {
                messageFound = true;
                break;
            } else {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    Log.info(c, "isContainerStarted", e.getMessage());
                }
            }
        }
        assertTrue("Container failed to startup successfully.", messageFound);
    }

    /*
     * Checks to see if a log string contains a given message. If not, the container log file will be printed. This was done to reduce the output.txt size.
     */
    static boolean assertLogContains(String methodName, String logs, String message) {
        boolean messageExists = logs.contains(message);

        if (!messageExists)
            Log.info(c, methodName, logs);

        return messageExists;
    }

    /*
     * Checks to if the two given log sizes are equal to each other. If not, the container log file will be printed. This was done to reduce the output.txt size.
     */
    static boolean compareLogSizes(String methodName, String logs, long expected, long actual) {
        boolean logSizesMatch = (expected == actual);

        if (!logSizesMatch) {
            Log.info(c, methodName, methodName + " -- test failed");
            Log.info(c, methodName, expected + "=expected, " + actual + "=actual -- occurances found");
            Log.info(c, methodName, logs);
            Log.info(c, methodName, "LOG COMPLETED");

        }

        return logSizesMatch;
    }
}
