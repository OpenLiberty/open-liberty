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

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public abstract class SSLCommonTests extends AbstractSpringTests {

    private static final String TEST_CLIENT_AUTH_NEED = "testClientAuthNeedWithClientSideKeyStore";

    public String getKeyStorePath(String methodName) {
        if (methodName.startsWith(TEST_CLIENT_AUTH_NEED)) {
            try {
                RemoteFile ksRemoteFile = server.getFileFromLibertyServerRoot("client-keystore.jks");
                return ksRemoteFile.getAbsolutePath();
            } catch (Exception e) {
                throw new IllegalStateException("Key Store file not found", e);
            }
        }
        return null;
    }

    public String getKeyStorePassword(String methodName) {
        if (methodName.startsWith(TEST_CLIENT_AUTH_NEED)) {
            return "secret";
        }
        return null;
    }

    public String getTrustStorePath(String methodName) {
        if (methodName.startsWith(TEST_CLIENT_AUTH_NEED)) {
            try {
                RemoteFile tsRemoteFile = server.getFileFromLibertyServerRoot("client-truststore.jks");
                return tsRemoteFile.getAbsolutePath();
            } catch (Exception e) {
                throw new IllegalStateException("Trust Store file not found", e);
            }
        }
        return null;
    }

    public String getTrustStorePassword(String methodName) {
        if (methodName.startsWith(TEST_CLIENT_AUTH_NEED)) {
            return "secret";
        }
        return null;
    }

    protected void testSSLApplication() throws Exception {
        String methodName = testName.getMethodName();
        if (methodName == null) {
            return;
        }
        final String ksPath = getKeyStorePath(methodName);
        final String ksPassword = getKeyStorePassword(methodName);
        final String tsPath = getTrustStorePath(methodName);
        final String tsPassword = getTrustStorePassword(methodName);

        String result = sendHttpsGet("/", server, ksPath, ksPassword, tsPath, tsPassword);
        assertNotNull(result);
        assertEquals("Expected response not found.", "HELLO SPRING BOOT!!", result);
    }

    public static String sendHttpsGet(String url, LibertyServer server) throws Exception {
        return sendHttpsGet(url, server, null, null, null, null);
    }

    public static String sendHttpsGet(String url, LibertyServer server, String ksPath, String ksPassword, String tsPath, String tsPassword) throws Exception {

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

    private static void establishSSLcontext(SSLContext sslcontext, LibertyServer server, String ksPath, String ksPassword, String tsPath, String tsPassword) throws Exception {
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

    private static URL getURL(String path, LibertyServer server) throws MalformedURLException {
        return new URL("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + path);

    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        String methodName = testName.getMethodName();
        Map<String, String> properties = new HashMap<>();
        properties.put("server.ssl.key-store", "classpath:server-keystore.jks");
        properties.put("server.ssl.key-store-password", "secret");
        properties.put("server.ssl.key-password", "secret");
        properties.put("server.ssl.trust-store", "classpath:server-truststore.jks");
        properties.put("server.ssl.trust-store-password", "secret");
        if (methodName != null) {
            if (methodName.contains("Need")) {
                properties.put("server.ssl.client-auth", "NEED");
            } else if (methodName.contains("Want")) {
                properties.put("server.ssl.client-auth", "WANT");
            }
        }
        return properties;
    }
}

class MyHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }
}
