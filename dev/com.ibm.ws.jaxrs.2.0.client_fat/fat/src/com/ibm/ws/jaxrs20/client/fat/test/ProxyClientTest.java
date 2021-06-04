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
package com.ibm.ws.jaxrs20.client.fat.test;

import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs20.client.fat.proxy.HttpProxyServer;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class ProxyClientTest extends AbstractTest {

    @Server("jaxrs20.client.ProxyClientTest")
    public static LibertyServer server;

    private final static String appname = "jaxrsclientproxy";
    private final static String target = appname + "/ClientTestServlet";

    private final static String proxyPort = "8888";

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.jaxrsclientproxy.client",
                                                       "com.ibm.ws.jaxrs20.client.jaxrsclientproxy.service");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
            HttpProxyServer.startHttpProxyServer(Integer.valueOf(proxyPort));//
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKW0702E", "CWWKW0701E", "CWWKW1303W");
        }
        HttpProxyServer.stopHttpProxyServer(Integer.valueOf(proxyPort));//
        System.out.println("End!");
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testProxyWork() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "testProxyWork");
        p.put("proxyhost", serverRef.getHostname());
        p.put("proxyport", proxyPort);
        p.put("proxytype", "HTTP");
        this.runTestOnServer(target, "testProxy", p, "[Basic Resource]:testProxyWork");
    }

    @Test
    public void testProxyNotWork_WrongHost() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "testProxyNotWork_WrongHost");
        p.put("proxyhost", "wrongHost");
        p.put("proxyport", proxyPort);
        p.put("proxytype", "HTTP");
        this.runTestOnServer(target, "testProxy", p, "[Proxy Error]:");
    }

    @Test
    public void testProxyNotWork_EmptyHost() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "testProxyNotWork_EmptyHost");

        p.put("proxyport", proxyPort);
        p.put("proxytype", "HTTP");
        this.runTestOnServer(target, "testProxy", p, "[Basic Resource]:testProxyNotWork_EmptyHost");
    }

    @Test
    public void testProxyNotWork_WrongPort() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "testProxyNotWork_WrongPort");
        p.put("proxyhost", serverRef.getHostname());
        p.put("proxyport", "8889");
        p.put("proxytype", "HTTP");
        this.runTestOnServer(target, "testProxy", p, 
                             "[Proxy Error]:javax.ws.rs.ProcessingException: java.net.ConnectException: ConnectException", // <= EE8
                             "[Proxy Error]:jakarta.ws.rs.ProcessingException: RESTEASY004655: Unable to invoke request"); // EE9
    }

    @Test
    @AllowedFFDC("java.lang.NumberFormatException")
    public void testProxyNotWork_InvalidPort() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "testProxyNotWork_InvalidPort");
        p.put("proxyhost", serverRef.getHostname());
        p.put("proxyport", "invalidPort"); //JaxRS-2.0 Client set the value to default port 80
        p.put("proxytype", "HTTP");
        this.runTestOnServer(target, "testProxy", p, "[Proxy Error]:");
    }

    @Test
    public void testProxyNotWork_DefaultPort() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "testProxyNotWork_DefaultPort");
        p.put("proxyhost", serverRef.getHostname());
        //JaxRS-2.0 Client set the value to default port 80
        p.put("proxytype", "HTTP");
        this.runTestOnServer(target, "testProxy", p, "[Proxy Error]:");
    }

    @Test
    public void testProxyWork_DefaultType() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "testProxyWork_DefaultType");
        p.put("proxyhost", serverRef.getHostname());
        p.put("proxyport", proxyPort);

        this.runTestOnServer(target, "testProxy", p, "[Basic Resource]:testProxyWork_DefaultType");
    }

    @Test
    @AllowedFFDC("java.lang.IllegalArgumentException")
    public void testProxyWork_InvalidType() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "testProxyWork_InvalidType");
        p.put("proxyhost", serverRef.getHostname());
        p.put("proxyport", proxyPort);
        p.put("proxytype", "invalidType"); //JaxRS-2.0 Client set the proxy type to default HTTP
        this.runTestOnServer(target, "testProxy", p, "[Basic Resource]:testProxyWork_InvalidType", // <= EE8
                                                     "[Proxy Error]:jakarta.ws.rs.ProcessingException: RESTEASY004655: Unable to invoke request"); //EE9
    }
}