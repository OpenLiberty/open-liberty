/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class HttpsRequest extends HttpRequest {

    private static final Class<?> c = HttpsRequest.class;

    private final boolean isHTTPS;
    private boolean allowInsecure = false;
    private SSLSocketFactory sf = null;

    private static String concat(String... pathParts) {
        String base = "";
        for (String part : pathParts)
            base += part;
        return base;
    }

    public HttpsRequest(String url) {
        super(url);
        this.isHTTPS = url.startsWith("https://");
    }

    public HttpsRequest(LibertyServer server, String... pathParts) {
        this("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + concat(pathParts));
    }

    public HttpsRequest sslSocketFactory(SocketFactory sslSf) {
        if (!isHTTPS)
            throw new IllegalArgumentException("Cannot set an SSLSocketFactory on an HTTP connection");
        if (!(sslSf instanceof SSLSocketFactory))
            throw new IllegalArgumentException("Socket factory must be an instanceof SSLSocketFactory, but was: " + sslSf);
        this.sf = (SSLSocketFactory) sslSf;
        return this;
    }

    public HttpsRequest allowInsecure() {
        this.allowInsecure = true;
        return this;
    }

    @Override
    void configureConnection(HttpURLConnection con) {
        if (allowInsecure && sf != null)
            throw new IllegalStateException("Cannot set allowInsecure=true and sslSocketFactory because " +
                                            " allowInsecure=true installs its own sslSocketFactory.");
        if (allowInsecure && isHTTPS) {
            //All hosts are valid
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            // Install the all-trusting host verifier
            ((HttpsURLConnection) con).setHostnameVerifier(allHostsValid);

            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            } };
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new SecureRandom());

                ((HttpsURLConnection) con).setSSLSocketFactory(sc.getSocketFactory());
            } catch (GeneralSecurityException e) {
                Log.error(c, "run", e, "CheckServerAvailability hit an error when trying to ignore certificates.");
                e.printStackTrace();
            }
        }
        if (sf != null && isHTTPS) {
            ((HttpsURLConnection) con).setSSLSocketFactory(sf);
        }
    }
}
