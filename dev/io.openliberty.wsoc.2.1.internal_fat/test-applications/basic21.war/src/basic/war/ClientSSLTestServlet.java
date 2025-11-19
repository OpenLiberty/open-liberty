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
package basic.war;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;

/*
 * Not used by FAT, but kept for testing / troubleshooting.
 * Just hit this servlet endpoint via HTTPS to test ssl connection
 */
public class ClientSSLTestServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        boolean success = true;
        String reasonForFailure = "";

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
            reasonForFailure = e.getMessage();
        }

        ClientEndpointConfig cec  = ClientEndpointConfig.Builder.create().sslContext(sslContext).build();

        try{
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(new EchoClientEP(), cec, buildFullURI(req));
        } catch(Exception ex){
            ex.printStackTrace();
            success = false;
            reasonForFailure = ex.getMessage();
        }

        PrintWriter printWriter = resp.getWriter();

        if(success){
            printWriter.print("SUCCESS! Verify logs for messages.");
        } else {
            printWriter.print("FAILURE! -> " + reasonForFailure);
        }

        printWriter.close();
    }

        //Trust all as it's easily to work with during testing
    private static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] { null };
        }

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
    } };

    /* Copied from com.ibm.ws.wsoc.HandshakeProcessor.java  */
    private URI buildFullURI(HttpServletRequest req) throws Exception {
        StringBuilder builder = new StringBuilder();

        String url = req.getRequestURL().toString();
        String https = "https";
        String http = "http";

        if (url.startsWith(https)) {
            url = "wss" + url.substring(https.length(), url.length());
        } else if (url.startsWith(http)) {
            url = "ws" + url.substring(http.length(), url.length());
        }

        if (!(url.startsWith("ws:") || url.startsWith("wss:"))) {
            throw new Exception("Scheme is not of type ws or wss.");
        }

        //Server Endpoint is echo
        url = url.replace("ClientSSLTestServlet", "echo");

        builder.append(url);

        return new URI(builder.toString());

    }

}
