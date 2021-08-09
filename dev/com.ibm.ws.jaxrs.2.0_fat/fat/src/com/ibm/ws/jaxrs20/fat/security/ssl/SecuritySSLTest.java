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
package com.ibm.ws.jaxrs20.fat.security.ssl;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class SecuritySSLTest {

    @Server("com.ibm.ws.jaxrs.fat.security.ssl")
    public static LibertyServer server;

    private static final String secwar = "security";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, secwar, "com.ibm.ws.jaxrs.fat.security.annotations",
                                      "com.ibm.ws.jaxrs.fat.security.ssl",
                                      "com.ibm.ws.jaxrs.fat.securitycontext",
                                      "com.ibm.ws.jaxrs.fat.securitycontext.xml");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
            assertNotNull("The server did not start", server.waitForStringInLog("CWWKF0011I"));
            assertNotNull("The Security Service should be ready", server.waitForStringInLog("CWWKS0008I"));
            assertNotNull("FeatureManager did not report update was complete", server.waitForStringInLog("CWWKF0008I"));
            assertNotNull("LTPA configuration should report it is ready", server.waitForStringInLog("CWWKS4105I"));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    public void testSecuritySSL() throws Exception {

        String url = "https://localhost:" + server.getHttpDefaultSecurePort() + "/security/ssltest/ssl/get";
        String getResult = SendHttpsGet(url);
        Log.info(this.getClass(), "testSecuritySSL", "The response: " + getResult);
        assertNotNull(getResult);

    }

    static public String SendHttpsGet(String url) {

        String result = null;

        try {
            //set SSLContext
            SSLContext sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(null, getTrustManager(), null);
            URL requestUrl = new URL(url);
            HttpsURLConnection httpsConn = (HttpsURLConnection) requestUrl.openConnection();
            httpsConn.setHostnameVerifier(new MyHostnameVerifier());

            httpsConn.setSSLSocketFactory(sslcontext.getSocketFactory());

            httpsConn.setRequestMethod("GET");
            httpsConn.setDoOutput(false);
            httpsConn.setDoInput(true);

            BufferedReader in = new BufferedReader(new InputStreamReader(httpsConn.getInputStream()));
            int code = httpsConn.getResponseCode();
            System.out.println("irisiris:" + code);
            Log.info(SecuritySSLTest.class, "SendHttpsGet", "The response code : " + code);

            String temp = in.readLine();

            while (temp != null) {
                if (result != null)
                    result += temp;
                else
                    result = temp;
                temp = in.readLine();
            }
            System.out.println(temp);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;

    }

    private static TrustManager[] getTrustManager() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(
                                           java.security.cert.X509Certificate[] certs, String authType) {}

            @Override
            public void checkServerTrusted(
                                           java.security.cert.X509Certificate[] certs, String authType) {}
        } };

        return trustAllCerts;
    }

}

class MyHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }
}
