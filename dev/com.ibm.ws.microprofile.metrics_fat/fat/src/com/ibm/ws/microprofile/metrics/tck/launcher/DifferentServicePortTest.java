/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.tck.launcher;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class DifferentServicePortTest {

    private static Class<?> c = DifferentServicePortTest.class;
    private static final String EARLY_ACCESS = "EARLY_ACCESS";
    private static Boolean isBeta;

    @Server("ServicePortServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        isBeta = isBeta();
        trustAll();
        server.startServer();
        String line = server.waitForStringInLog("CWWKF0011I");
        Assert.assertNotNull("CWWKF0011I is not found", line);
        //check Beta
    }

    private static void trustAll() throws Exception {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(
                            null,
                            new TrustManager[] {
                                                 new X509TrustManager() {
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
                                                 }
                            },
                            new SecureRandom());
            SSLContext.setDefault(sslContext);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            Log.error(c, "trustAll", e);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        if ((server != null) && (server.isStarted())) {
            try {
                server.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testDefaultPort() throws Exception {
        String testName = "testDefaultPort";

        Log.info(c, testName, "entry");
        setConfig("defaultPort.xml");
        Assert.assertNotNull("CWWKT0016I NOT FOUND", server.waitForStringInLogUsingMark(".*CWWKT0016I.*metrics.*", 60000));
        Assert.assertNotNull("CWWKO0219I NOT FOUND", server.waitForStringInLog(".*CWWKO0219I.*defaultHttpEndpoint-ssl.*", 60000));
        getHttpsServlet("/metrics", server.getHttpDefaultSecurePort());
    }

    @Test
    public void testAdminPort() throws Exception {
        Assume.assumeTrue(isBeta);
        String testName = "testAdminPort";

        Log.info(c, testName, "entry");
        setConfig("adminPort.xml");
        Assert.assertNotNull("CWWKT0016I NOT FOUND", server.waitForStringInLogUsingMark(".*CWWKT0016I.*metrics.*", 60000));
        Assert.assertNotNull("CWWKO0219I NOT FOUND", server.waitForStringInLog(".*CWWKO0219I.*adminHttpEndpoint-ssl.*", 60000));
        getHttpsServlet("/metrics", 9445);
    }

    @Test
    public void testOpsPort() throws Exception {
        Assume.assumeTrue(isBeta);
        String testName = "testOpsPort";

        Log.info(c, testName, "entry");
        setConfig("opsPort.xml");
        Assert.assertNotNull("CWWKT0016I NOT FOUND", server.waitForStringInLog(".*CWWKT0016I.*metrics.*", 60000));
        Assert.assertNotNull("CWWKO0219I NOT FOUND", server.waitForStringInLog(".*CWWKO0219I.*opsHttpEndpoint-ssl.*", 60000));
        getHttpsServlet("/metrics", 9446);
    }

    private String getHttpsServlet(String servletPath, Integer port) throws Exception {
        HttpsURLConnection con = null;
        try {
            String sURL = "https://" + server.getHostname() + ":" + port + servletPath;
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
            String authorization = "Basic " + Base64.getEncoder().encodeToString(("theUser:thePassword").getBytes(StandardCharsets.UTF_8)); //Java 8
            con.setRequestProperty("Authorization", authorization);
            con.setRequestMethod("GET");

            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            line = br.readLine();
            Log.info(c, "getHttpsServlet", line);
            assertTrue(line != null && line.length() > 0);

            Log.info(c, "getHttpsServlet", sURL);
            return lines.toString();
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    private static String setConfig(String fileName) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(fileName);
        return server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
    }

    private static boolean isBeta() {
        try {
            final Map<String, ProductInfo> productInfos = ProductInfo.getAllProductInfo(new File(server.getInstallRoot()));
            for (ProductInfo info : productInfos.values()) {
                if (EARLY_ACCESS.equals(info.getEdition())) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.debug(c, "Exception getting InstalledProductInfo: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

}
