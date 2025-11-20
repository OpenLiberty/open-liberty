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
package io.openliberty.wsoc.tests.all;

import java.security.SecureRandom;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.endpoints.client.basic.ClientHelper;
import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import jakarta.websocket.ClientEndpointConfig;

/**
 *  Verify Passed in SSLContext work in SSL Channel.
 */
public class SSLTest{

    private WsocTest wsocTest = null;

    public SSLTest(WsocTest test) {
        this.wsocTest = test;
    }

    public void testPassedInSSLContext() throws Exception {

        // Checks are performed within client endpoint because wsoc Impl uses HashMap which doesn't guarentee order.
        // Also avoids sending the properties to the client
        String uri = "/basic21/echo";

        String[] input1 = { "echoValue" };
        String[] output1 = { "echoValue" };

        SSLContext sslContext = null;

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }

        ClientEndpointConfig cec =  ClientEndpointConfig.Builder.create().sslContext(sslContext).build();

        WsocTestContext testdata = wsocTest.runWsocTest(new ClientHelper.BasicClientEP(input1), uri, cec, 1, Constants.getDefaultTimeout());
        List<Object> list = testdata.getMessage();
        for(Object o : list){
            System.out.println("received "  + o.toString());
        }
        testdata.reThrowException();


    }


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

}
