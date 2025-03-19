/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.metrics50.internal.tck.launcher;

import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class LibraryRefTest {

    private static Class<?> c = LibraryRefTest.class;

    @Server("MicrometerPrometheus")
    public static LibertyServer serverMicrometerPrometheus;

    @Server("MicrometerUseless")
    public static LibertyServer serverMicrometerUseless;

    @Server("NonExistentLibrary")
    public static LibertyServer serverNonExistentLibrary;

    @Server("NoMicrometerCore")
    public static LibertyServer serverNoMicrometerCore;

    public static LibertyServer server;

    public static boolean isJava11014 = false;

    @BeforeClass
    public static void setUp() throws Exception {
        trustAll();
        //Check what JVM the server is running on the (remote) machine
        server = serverMicrometerPrometheus;
        isJava11014();
    }

    @Before
    /*
     * Check that Java is not 11.0.14; known to cause javacores
     * Skip if it is the case.
     */
    public void checkJava() {
        if (isJava11014) {
            Log.info(c, "checkJava", "Detected Java to be 11.0.14. SKIPPING");
        }

        assumeTrue(!isJava11014);
    }

    @After
    public void after() throws Exception {
        //catch if a server is still running.
        if (server != null && server.isStarted()) {
            server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWPMI2006W", "CWMMC0013E", "CWWKG0033W");
        }
    }

    /*
     * Java 11.0.14 known to cause issues.
     */
    private static void isJava11014() throws IOException {
        JavaInfo javaInfo = JavaInfo.forServer(server);

        Log.info(c, "isJava11014",
                 "Major.minor.micro : [" + JavaInfo.forServer(server).majorVersion()
                                   + "."
                                   + JavaInfo.forServer(server).minorVersion()
                                   + "." + JavaInfo.forServer(server).microVersion()
                                   + "]");
        if (javaInfo.majorVersion() == 11 && javaInfo.microVersion() == 14)
            isJava11014 = true;
    }

    /*
     * Not actually a test regarding metrics and libraryRef attribute.
     * This is emitted by the Config component when it can't find a matching library reference.
     * Might as well test it anyways.
     */
    @Test
    public void nonExistentLibrary() throws Exception {
        server = serverNonExistentLibrary;
        server.startServer();
        server.waitForSSLStart();

        //CWWKG0033W The value [<value>] specified for the reference attribute [libraryRef] was not found in the configuration.
        Assert.assertNotNull("CWWKG0033W Not found", server.waitForStringInLogUsingMark("CWWKG0033W"));

    }

    /*
     * Provided a libray that is missing the micrometer core jar.
     * This emits a CWMMC0013E
     * Also an FFDC is created which tells us what is missing.
     */
    @Test
    @AllowedFFDC
    public void noMicrometerCore() throws Exception {
        server = serverNoMicrometerCore;
        server.startServer();
        server.waitForSSLStart();

        //CWMMC0014I emits that metrics is using libraryRef
        Assert.assertNotNull("CWMMC0014I Not found", server.waitForStringInLogUsingMark("CWMMC0014I"));

        //Realize that we don't have the classes necessary (i.e. micrometer core)

        //CWMMC0013E The MicroProfile Metrics feature was unable to initialize. A class that is required for a user-provided Micrometer Library is missing.
        Assert.assertNotNull("CWMMC0013E Not found", server.waitForStringInLogUsingMark("CWMMC0013E"));

    }

    /*
     * This MicrometerPrometheus is configured to use external Micrometer Libraries.
     * Configured via the libraryRef attribute of mpMetrics
     * The <library> referenced contains Micrometer Core, Prometheus registry, and its dependencies
     *
     * Note: see build.gradle
     */
    @Test
    public void externalPrometheusMicrometer() throws Exception {

        server = serverMicrometerPrometheus;

        String installRoot = server.getInstallRoot();
        String prometheusLibPath = installRoot + "/usr/shared/resources/prometheusLib";
        String micrometerPath = installRoot + "/usr/shared/resources/micrometercore";

        Log.info(c, "externalPrometheusMicrometer", "Prom library directory: " + prometheusLibPath);
        Log.info(c, "externalPrometheusMicrometer", "Micrometer library directory: " + micrometerPath);
        try {
            File f = new File(prometheusLibPath);

            if (f.isDirectory()) {
                for (File ff : f.listFiles()) {
                    Log.info(c, "externalPrometheusMicrometer", "Prom lib files found: " + ff.getName());
                }
            } else {
                Log.info(c, "externalPrometheusMicrometer", "Not a directory: " + prometheusLibPath);
            }

            File f2 = new File(micrometerPath);
            if (f.isDirectory()) {
                for (File ff : f2.listFiles()) {
                    Log.info(c, "externalPrometheusMicrometer", "Micrometer lib files found: " + ff.getName());
                }
            } else {
                Log.info(c, "externalPrometheusMicrometer", "Not a directory: " + micrometerPath);
            }

        } catch (Exception e) {
            Log.info(c, "externalPrometheusMicrometer", "Encountered exception while trying to list files of shared library " + e);
        }

        server.startServer();
        server.waitForSSLStart();

        //CWMMC0014I emits that metrics is using libraryRef
        Assert.assertNotNull("CWMMC0014I Not found", server.waitForStringInLogUsingMark("CWMMC0014I"));

        //CWWKF0011I server ready
        Assert.assertNotNull("CWWKF0011I Not found", server.waitForStringInLogUsingMark("CWWKF0011I"));

        server.resetLogMarks();

        Assert.assertNotNull("CWWKO0219I Not found", server.waitForStringInLogUsingMark("CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl"));

        //Check SR implementation log that Promethues Registry created
        //Note that SR makes THIS explicit log for Prometheus, other meter registries are logged differently following a template
        String line = server.waitForStringInTrace("Prometheus MeterRegistry created");
        Assert.assertNotNull("Expected \"Prometheus MeterRegistry created\"", line);

        String exceptionString = null;
        try {
            String output = getHttpsServlet("/metrics");
            Log.info(c, "externalPrometheusMicrometer", output);
            Assert.assertNotNull("Results of /metrics output should not have been null", output);

            //just do simple check for jvm.uptime metric
            boolean containsMetrics = output.contains("jvm_uptime_seconds{mp_scope=\"base\",");
            Assert.assertTrue("Expected to see the always present base metric jvm.uptime from /metriccs output", containsMetrics);

        } catch (ConnectException exception) {
            exceptionString = exception.toString();
            Log.error(c, "externalPrometheusMicrometer", exception);
        }
        Assert.assertNull("Was not expecting ConnectException", exceptionString);
    }

    /*
     * This MicrometerPrometheus is configured to use no external Micrometer Libraries.
     * Albeit the libraryRef is configured and does point to a micrometer core.
     * The Micrometer Core is so that SR can actually initialize so that we can
     * check that SmallRye does not emit the message indicating a registry is registered to global
     * registry: "created and registered to the Micrometer global registry" which originates from the
     * SharedMetricRegistires.java class
     *
     */
    @Test
    public void externalMicrometerUselessJar() throws Exception {
        server = serverMicrometerUseless;
        server.startServer();
        server.waitForSSLStart();

        //CWMMC0014I emits that metrics is using libraryRef
        Assert.assertNotNull("CWMMC0014I Not found", server.waitForStringInLogUsingMark("CWMMC0014I"));

        //CWWKF0011I server ready
        Assert.assertNotNull("CWWKF0011I Not found", server.waitForStringInLogUsingMark("CWWKF0011I"));

        /*
         * Check that this SR log is not emmitted.
         * This is emitted for every Meter Registry detected.
         * Since we aren't using any real registries, we don't expect this.
         */
        String line = server.waitForStringInTrace("created and registered to the Micrometer global registry", 10000);
        Assert.assertNull("Expected not to see \"created and registered to the Micrometer global registry\" in trace.", line);
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

}
