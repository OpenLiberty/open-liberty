/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.grpc.tls;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 * This utility is used to create a TrustManagerFactory that can be used to configure an SslContext so that it will
 * trust Liberty's self-signed cert.
 *
 * We should prefer getLibertyTrustedManagerFactory() over getNaiveTlsTrustManagerFactory().
 */
public class GrpcTestTlsUtils {

    /**
     * Borrowed from LibertyServer.getJMXRestConnector()
     *
     * Returns a TrustManagerFactory that is configured to trust Liberty's default certificate
     */
    public static TrustManagerFactory getLibertyTrustedManagerFactory(String serverRoot) {
        TrustManagerFactory trustManagerFactory = null;
        try {
            // Load the keystore file from the file system.
            KeyStore keyStore = KeyStore.getInstance("JKS");
            String path = serverRoot + "/resources/security/key.jks";
            File keyFile = new File(path);
            if (!keyFile.exists()) {
                path = serverRoot + "/resources/security/key.p12";
                keyFile = new File(path);
            }

            FileInputStream is = new FileInputStream(keyFile);
            byte[] fileBytes = new byte[(int) keyFile.length()];
            is.read(fileBytes);

            // Load the file to the Keystore object as type JKS (default).
            // If the load fails with an IOException, try to load it as type PKCS12.
            // Note that in java 9, dynamically generated keystores using java's keytool will be of type PKCS12 because
            // that is the new default. See this link for more information: http://openjdk.java.net/jeps/229
            // The code below will handle paltform differences and JDK level differences.
            try {
                keyStore.load(new ByteArrayInputStream(fileBytes), "passw0rd".toCharArray());
            } catch (IOException ioe) {
                keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(new ByteArrayInputStream(fileBytes), "passw0rd".toCharArray());
            }

            is.close();
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e) {
            e.printStackTrace();
        }
        return trustManagerFactory;
    }

    /**
     * Get a TrustManagerFactory which can be used to trust all TLS certs
     *
     * @return TrustManagerFactory
     */
    public static TrustManagerFactory getNaiveTlsTrustManagerFactory() {
        return new NaiveTrustManagerFactory(new NaiveTrustManagerFactorySpi(), null, "TLS");
    }

    private static TrustManager[] getNaiveTrustManager() {
        return new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate[] dummyCert = new X509Certificate[0];
                return dummyCert;
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws CertificateException {
            }
        }
        };
    }

    private static class NaiveTrustManagerFactorySpi extends javax.net.ssl.TrustManagerFactorySpi {

        @Override
        protected void engineInit(KeyStore ks) throws KeyStoreException {
        }

        @Override
        protected void engineInit(ManagerFactoryParameters spec) throws InvalidAlgorithmParameterException {
        }

        @Override
        protected TrustManager[] engineGetTrustManagers() {
            return getNaiveTrustManager();
        }
    }

    private static class NaiveTrustManagerFactory extends TrustManagerFactory {
        protected NaiveTrustManagerFactory(TrustManagerFactorySpi factorySpi, Provider provider, String algorithm) {
            super(factorySpi, provider, algorithm);
        }
    }
}
