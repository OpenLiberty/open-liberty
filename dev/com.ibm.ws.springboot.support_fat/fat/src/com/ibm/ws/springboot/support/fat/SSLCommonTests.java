/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public abstract class SSLCommonTests extends AbstractSpringTests {

    public String getKeyStorePath() {
        return null;
    }

    public String getKeyStorePassword() {
        return null;
    }

    public String getTrustStorePath() {
        return null;
    }

    public String getTrustStorePassword() {
        return null;
    }

    protected void testSSLApplication() throws Exception {
        final String ksPath = getKeyStorePath();
        final String ksPassword = getKeyStorePassword();
        final String tsPath = getTrustStorePath();
        final String tsPassword = getTrustStorePassword();

        assertNotNull("The application was not installed", server
                        .waitForStringInLog("CWWKZ0001I:.*"));

        // NOTE we set the port to the expected port according to the test application.properties
        server.setHttpDefaultSecurePort(8081);
        String result = sendHttpsGet("/", server, ksPath, ksPassword, tsPath, tsPassword);
        assertNotNull(result);
        assertEquals("Expected response not found.", "HELLO SPRING BOOT!!", result);
    }

    protected String sendHttpsGet(String url, LibertyServer server, String ksPath, String ksPassword, String tsPath, String tsPassword) throws Exception {

        String result = null;
        SSLContext sslcontext = SSLContext.getInstance("SSL");

        establishSSLcontext(sslcontext, server, ksPath, ksPassword, tsPath, tsPassword);

        URL requestUrl = getURL(url, server);

        HttpsURLConnection httpsConn = (HttpsURLConnection) requestUrl.openConnection();
        httpsConn.setHostnameVerifier(new MyHostnameVerifier());
        httpsConn.setSSLSocketFactory(sslcontext.getSocketFactory());
        httpsConn.setRequestMethod("GET");
        httpsConn.setDoOutput(false);
        httpsConn.setDoInput(true);

        int code = httpsConn.getResponseCode();
        assertEquals("Expected response code not found.", 200, code);

        BufferedReader in = new BufferedReader(new InputStreamReader(httpsConn.getInputStream()));
        String temp = in.readLine();

        while (temp != null) {
            if (result != null)
                result += temp;
            else
                result = temp;
            temp = in.readLine();
        }
        return result;
    }

    private void establishSSLcontext(SSLContext sslcontext, LibertyServer server, String ksPath, String ksPassword, String tsPath, String tsPassword) throws Exception {
        InputStream ksStream = null;
        InputStream tsStream = null;

        try {
            KeyManager keyManagers[] = null;

            if (ksPath != null) {
                KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                File ksFile = new File(ksPath);
                KeyStore keyStore = KeyStore.getInstance("JKS");

                ksStream = new FileInputStream(ksFile);
                keyStore.load(ksStream, ksPassword.toCharArray());

                kmFactory.init(keyStore, ksPassword.toCharArray());
                keyManagers = kmFactory.getKeyManagers();
            }
            TrustManager[] trustManagers = null;

            if (tsPath != null) {
                TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                File tsFile = new File(tsPath);
                KeyStore trustStore = KeyStore.getInstance("JKS");

                tsStream = new FileInputStream(tsFile);
                trustStore.load(tsStream, tsPassword.toCharArray());

                tmFactory.init(trustStore);
                trustManagers = tmFactory.getTrustManagers();
            }
            if (trustManagers == null) {
                trustManagers = getTrustManager();
            }

            sslcontext.init(keyManagers, trustManagers, null);
        } finally {
            if (ksStream != null) {
                ksStream.close();
            }
            if (tsStream != null) {
                tsStream.close();
            }
        }
    }

    private TrustManager[] getTrustManager() {
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

    private URL getURL(String path, LibertyServer server) throws MalformedURLException {
        return new URL("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + path);

    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("server.ssl.key-store", "classpath:server-keystore.jks");
        properties.put("server.ssl.key-store-password", "secret");
        properties.put("server.ssl.key-password", "secret");
        properties.put("server.ssl.trust-store", "classpath:server-truststore.jks");
        properties.put("server.ssl.trust-store-password", "secret");
        return properties;
    }

}

class MyHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }
}
