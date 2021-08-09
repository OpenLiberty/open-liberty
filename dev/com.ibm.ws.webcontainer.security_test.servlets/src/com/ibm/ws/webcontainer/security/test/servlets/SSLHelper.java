/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.test.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class SSLHelper {

    /**
     * Adds an SSL context to the HttpClient. No trust or client certificate is
     * established and a trust-all policy is assumed.
     *
     * @param client the HttpClient
     * @param port SSL port
     * @param server the LibertyServer
     */
    public static void establishSSLContext(HttpClient client, int port, LibertyServer server) {
        establishSSLContext(client, port, server, null, null, null, null, "TLS");
    }

    /**
     * Adds an SSL context to the HttpClient. If the given ksPath is not null
     * then adds it to the SSL context for client certificate authentication.
     * If the given tsPath is not null, then it is used for trust for the SSL
     * connection.
     *
     * @param client the HttpClient
     * @param port SSL port
     * @param server the LibertyServer
     * @param ksPath path to the keystore, may be null
     * @param ksPassword password for the keystore, may be null
     * @param tsPath path to the trust store, may be null
     * @param tsPassword password for the truststore, may be null
     */
    public static void establishSSLContext(HttpClient client,
                                           int port,
                                           LibertyServer server,
                                           String ksPath,
                                           String ksPassword,
                                           String tsPath,
                                           String tsPassword) {
        establishSSLContext(client, port, server, ksPath, ksPassword, tsPath, tsPassword, null);
    }

    /**
     * Adds an SSL context to the HttpClient. If the given ksPath is not null
     * then adds it to the SSL context for client certificate authentication.
     * If the given tsPath is not null, then it is used for trust for the SSL
     * connection.
     *
     * @param client the HttpClient
     * @param port SSL port
     * @param server the LibertyServer
     * @param ksPath path to the keystore, may be null
     * @param ksPassword password for the keystore, may be null
     * @param tsPath path to the trust store, may be null
     * @param tsPassword password for the truststore, may be null
     * @param sslProtocol the protocol to be used for the ssl connection
     */
    public static void establishSSLContext(HttpClient client,
                                           int port,
                                           LibertyServer server,
                                           String ksPath,
                                           String ksPassword,
                                           String tsPath,
                                           String tsPassword,
                                           String sslProtocol) {
        FileInputStream ksStream = null;
        FileInputStream tsStream = null;
        try {
            int sslPort;
            if (server != null) {
                sslPort = server.getHttpDefaultSecurePort();
            } else {
                sslPort = port;
            }

            try {
                KeyManager keyManagers[] = null;
                if (ksPath != null) {
                    KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

                    File ksFile = new File(ksPath);
                    KeyStore keyStore = null;
                    try {
                        keyStore = KeyStore.getInstance("JKS");
                        ksStream = new FileInputStream(ksFile);
                        keyStore.load(ksStream, ksPassword.toCharArray());
                    } catch (Exception e) {
                        try {
                            keyStore = KeyStore.getInstance("PKCS12");
                            ksStream = new FileInputStream(ksFile);
                            keyStore.load(ksStream, ksPassword.toCharArray());
                        } catch (Exception e1) {
                            throw e1;
                        }

                    }
                    kmFactory.init(keyStore, ksPassword.toCharArray());

                    keyManagers = kmFactory.getKeyManagers();
                }

                TrustManager[] trustManagers = null;

                if (tsPath != null) {
                    TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    File tsFile = new File(tsPath);

                    KeyStore trustStore = null;
                    try {
                        trustStore = KeyStore.getInstance("JKS");
                        tsStream = new FileInputStream(tsFile);
                        trustStore.load(tsStream, tsPassword.toCharArray());

                    } catch (Exception e) {
                        try {
                            trustStore = KeyStore.getInstance("PKCS12");
                            tsStream = new FileInputStream(tsFile);
                            trustStore.load(tsStream, tsPassword.toCharArray());
                        } catch (Exception e1) {
                            throw e1;
                        }

                    }

                    tmFactory.init(trustStore);

                    trustManagers = tmFactory.getTrustManagers();

                }
                if (trustManagers == null) {
                    trustManagers = new TrustManager[] { createTrustAllTrustManager() };
                }

                if (sslProtocol == null)
                    sslProtocol = "TLS";

                SSLContext ctx = SSLContext.getInstance(sslProtocol);
                ctx.init(keyManagers, trustManagers, null);
                SSLSocketFactory socketFactory = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                Scheme sch = new Scheme("https", sslPort, socketFactory);
                client.getConnectionManager().getSchemeRegistry().register(sch);
            } catch (Exception e) {
                throw new RuntimeException("Unable to establish SSLSocketFactory", e);
            }
        } finally {
            try {
                if (ksStream != null) {
                    ksStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (tsStream != null) {
                    tsStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Creates a socket by first creating a SSLContext with the given keystore and truststore
     * information, then getting the socket factory from that SSLContext and finally getting
     * creating a socket for the given url.
     */
    public static Socket getSocketForURL(URL url,
                                         String ksPath,
                                         String ksPassword,
                                         String tsPath,
                                         String tsPassword,
                                         String sslProtocol) {
        SSLSocket sslSocket = null;
        FileInputStream ksStream = null;
        FileInputStream tsStream = null;
        try {

            try {
                KeyManager keyManagers[] = null;
                if (ksPath != null) {
                    KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

                    File ksFile = new File(ksPath);
                    KeyStore keyStore = null;
                    try {
                        keyStore = KeyStore.getInstance("JKS");
                        ksStream = new FileInputStream(ksFile);
                        keyStore.load(ksStream, ksPassword.toCharArray());
                    } catch (Exception e) {
                        try {
                            keyStore = KeyStore.getInstance("PKCS12");
                            ksStream = new FileInputStream(ksFile);
                            keyStore.load(ksStream, ksPassword.toCharArray());
                        } catch (Exception e1) {
                            throw e1;
                        }

                    }
                    kmFactory.init(keyStore, ksPassword.toCharArray());

                    keyManagers = kmFactory.getKeyManagers();
                }

                TrustManager[] trustManagers = null;

                if (tsPath != null) {
                    TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    File tsFile = new File(tsPath);

                    KeyStore trustStore = null;
                    try {
                        trustStore = KeyStore.getInstance("JKS");
                        tsStream = new FileInputStream(tsFile);
                        trustStore.load(tsStream, tsPassword.toCharArray());

                    } catch (Exception e) {
                        try {
                            trustStore = KeyStore.getInstance("PKCS12");
                            tsStream = new FileInputStream(tsFile);
                            trustStore.load(tsStream, tsPassword.toCharArray());
                        } catch (Exception e1) {
                            throw e1;
                        }

                    }

                    tmFactory.init(trustStore);

                    trustManagers = tmFactory.getTrustManagers();

                }
                if (trustManagers == null) {
                    trustManagers = new TrustManager[] { createTrustAllTrustManager() };
                }

                if (sslProtocol == null)
                    sslProtocol = "TLS";

                SSLContext ctx = SSLContext.getInstance(sslProtocol);
                ctx.init(keyManagers, trustManagers, null);

                javax.net.ssl.SSLSocketFactory socketFactory = ctx.getSocketFactory();

                sslSocket = (SSLSocket) socketFactory.createSocket(url.getHost(), url.getPort());
                sslSocket.setUseClientMode(true);
                sslSocket.startHandshake();
            } catch (Exception e) {
                throw new RuntimeException("Unable to establish SSLSocket", e);
            }
        } finally {
            try {
                if (ksStream != null) {
                    ksStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (tsStream != null) {
                    tsStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sslSocket;
    }

    /**
     * @return
     */
    private static X509TrustManager createTrustAllTrustManager() {
        X509TrustManager tm = new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {}

            @Override
            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        return tm;
    }
}
