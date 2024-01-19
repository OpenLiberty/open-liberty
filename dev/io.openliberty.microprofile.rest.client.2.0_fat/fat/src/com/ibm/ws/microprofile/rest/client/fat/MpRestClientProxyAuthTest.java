/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

import static org.mockserver.model.HttpRequest.request;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient.proxyauth.testservlet.MpRestClientProxyAuthClientTestServlet;

@RunWith(FATRunner.class)
public class MpRestClientProxyAuthTest extends FATServletClient {

    private final static Class<?> c = MpRestClientProxyAuthTest.class;

    private final static String appname = "MpRestClientProxyAuth";

    @Server("mpRestClientProxyAuthTest")
    @TestServlet(servlet = MpRestClientProxyAuthClientTestServlet.class, contextRoot = appname)
    public static LibertyServer server;

    private static int proxyPort;
    private static ClientAndServer proxy;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, appname, "mpRestClient.proxyauth.app",
                                                       "mpRestClient.proxyauth.restclient",
                                                       "mpRestClient.proxyauth.testservlet");

        System.setProperty("javax.net.ssl.keyStore", "publish/servers/mpRestClientProxyAuthTest/resources/security/key.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "passw0rd");
        System.setProperty("javax.net.ssl.trustStore", "publish/servers/mpRestClientProxyAuthTest/resources/security/trust.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "passw0rd");

        ConfigurationProperties.useBouncyCastleForKeyAndCertificateGeneration(true);
        ConfigurationProperties.forwardProxyTLSX509CertificatesTrustManagerType("ANY");
        ConfigurationProperties.proxyAuthenticationUsername("mpRestClientUser");
        ConfigurationProperties.proxyAuthenticationPassword("myPa$$word");
        ConfigurationProperties.proxyAuthenticationRealm("foo");
        ConfigurationProperties.attemptToProxyIfNoMatchingExpectation(true);

        proxyPort = Integer.getInteger("member_3.https");
        proxy = ClientAndServer.startClientAndServer(proxyPort);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        System.out.println(proxy.retrieveRecordedRequests(request()));
        proxy.stop();
        if (server != null) {
            server.stopServer();
        }
    }

    @Before
    public void preTest() {
        if (proxy.isRunning()) {
            Log.info(c, "preTest", "Mock Server running at: " + proxy.getLocalPort());
        }
    }

    @After
    public void afterTest() {
        Log.info(c, "afterTest", "Proxy Server log messages: " + proxy.retrieveLogMessages(request()));
        proxy.reset();
    }
}
