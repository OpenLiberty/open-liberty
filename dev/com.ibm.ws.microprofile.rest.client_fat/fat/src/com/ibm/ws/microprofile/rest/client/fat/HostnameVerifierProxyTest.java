/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.rest.client.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.NottableString.not;
import static org.mockserver.model.NottableString.string;


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import mpRestClient20.hostnameVerifier.HostnameVerifierTestServlet;

/**
 * This test should only be run with mpRestClient-2.0 and above.
 */
@RunWith(FATRunner.class)
public class HostnameVerifierProxyTest extends AbstractTest {

    private final static Class<?> c = HostnameVerifierProxyTest.class;

    final static String SERVER_NAME = "mpRestClient20.proxy";
    @Server(SERVER_NAME)
    
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, 
                                                             MicroProfileActions.MP40, // 2.0
                                                             MicroProfileActions.MP50, // 3.0
                                                             MicroProfileActions.MP60);// 3.0+EE10
    
    private final static String appname = "hostnameVerifierProxyApp";
    private final static String target = appname + "/HostnameVerifierTestServlet";

    @TestServlet(servlet = HostnameVerifierTestServlet.class, contextRoot = appname)

    private static int proxyPort;
    private static ClientAndServer proxy;


    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, appname, "mpRestClient20.hostnameVerifier");
        
        System.setProperty("javax.net.ssl.keyStore", "publish/servers/mpRestClient20.proxy/resources/security/key.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "passw0rd");
        System.setProperty("javax.net.ssl.trustStore", "publish/servers/mpRestClient20.proxy/resources/security/trust.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "passw0rd");

        ConfigurationProperties.useBouncyCastleForKeyAndCertificateGeneration(true);
        ConfigurationProperties.forwardProxyTLSX509CertificatesTrustManagerType("ANY");
        ConfigurationProperties.proxyAuthenticationUsername("jaxrsUser");
        ConfigurationProperties.proxyAuthenticationPassword("myPa$$word");
        ConfigurationProperties.proxyAuthenticationRealm("foo");
        ConfigurationProperties.attemptToProxyIfNoMatchingExpectation(true);

        proxyPort = Integer.getInteger("member_3.http");
        proxy = ClientAndServer.startClientAndServer(proxyPort);
        
        System.out.println("Jim... proxy address = " + proxy.getRemoteAddress());

        // Make sure we don't fail because we try to start an
        // already started server
        try {
        server.startServer(true);
        } catch (Exception e) {
        System.out.println(e.toString());
        }

    }

    @AfterClass
    public static void afterClass() throws Exception {
        proxy.stop();
        if (server != null) {
            server.stopServer();
        }

    }
    
    @Before
    public void preTest() {
        serverRef = server;
        Log.info(c, "preTest", "Mock Server Proxy listening on port " + proxyPort);
    }

    @After
    public void afterTest() {
        Log.info(c, "afterTest", "Proxy Server log messages: " + proxy.retrieveLogMessages(request()));
        proxy.reset();
        serverRef = null;
    }
    
    
}