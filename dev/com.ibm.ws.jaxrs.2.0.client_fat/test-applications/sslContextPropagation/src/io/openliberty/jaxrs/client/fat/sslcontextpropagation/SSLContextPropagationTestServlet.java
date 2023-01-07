/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jaxrs.client.fat.sslcontextpropagation;


import static org.junit.Assert.assertEquals;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SSLContextPropagationTestServlet")
public class SSLContextPropagationTestServlet extends FATServlet {

    private static final String SERVER_CONTEXT_ROOT = "https://localhost:" + Integer.getInteger("bvt.prop.HTTP_default.secure") + "/simpleSSL/";

    private static Client client;

    @Override
    public void after() {
        client.close();
    }

    @Test
    public void testClientBuilderSSLContextPropagation() throws NoSuchAlgorithmException, KeyManagementException {

        // create a custom SSLContext
        SSLContext sslcontext = SSLContext.getInstance("TLSv1.1");
        sslcontext.init(null, new TrustManager[] { new X509TrustManager() {
          @Override
          public void checkClientTrusted(X509Certificate[] arg0, String arg1)
              throws CertificateException {
          }
          @Override
          public void checkServerTrusted(X509Certificate[] arg0, String arg1)
              throws CertificateException {
          }
          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }

        } }, null);

        ClientBuilder builder = ClientBuilder.newBuilder();
        builder.sslContext(sslcontext).hostnameVerifier((s1, s2) -> true);

        // build the client
        client = builder.build();

        // verify that the client's SSLContext isn't reset to the default SSLContext in the server.xml
        SSLContext sslContext = client.getSslContext();
        assertEquals("TLSv1.1", sslContext.getProtocol());
    }
}