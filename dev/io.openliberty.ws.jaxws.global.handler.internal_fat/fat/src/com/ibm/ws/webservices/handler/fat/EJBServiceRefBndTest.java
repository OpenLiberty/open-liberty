/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package com.ibm.ws.webservices.handler.fat;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class EJBServiceRefBndTest {
    private static final int CONN_TIMEOUT = 5;
    @Server("EJBServiceRefBndTestServer")
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("EJBServiceRefBndTestServer");

    private static String echoServletURL = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/ejbServiceRefBndClient/EchoServlet").toString();

    private static String helloServletURL = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/ejbServiceRefBndClient/HelloServlet").toString();

    private static String testFeatureName;
    @BeforeClass
    public static void setup() throws Exception {
        testFeatureName = JakartaEEAction.isEE9Active() ? "xmlWSTest-3.0.mf" : (JakartaEEAction.isEE10OrLaterActive() ? "xmlWSTest-4.0.mf" :"jaxwsTest-2.2.mf");
        server.copyFileToLibertyInstallRoot("lib/features", "EJBServiceRefBndTest/" + testFeatureName);

        //server.installUserBundle("TestHandler1_1.0.0.201311011652");
        ShrinkHelper.defaultUserFeatureArchive(server, "userBundle2", "com.ibm.ws.userbundle2.myhandler");
        TestUtils.installUserFeature(server, "TestHandler1Feature1");
        JavaArchive ejbJar = ShrinkHelper.buildJavaArchive("ejbServiceRefBnd", "com.ibm.sample.bean",
                                                                               "com.ibm.sample.jaxws.echo.client",
                                                                               "com.ibm.sample.jaxws.hello.client",
                                                                               "com.ibm.sample.jaxws.hello.client.interceptor",
                                                                               "com.ibm.sample.util");
        WebArchive war = ShrinkHelper.buildDefaultApp("ejbServiceRefBndClient", "com.ibm.sample.web.servlet");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ejbServiceRefBndApp.ear");
        ear.addAsModule(ejbJar);
        ear.addAsModule(war);
        ShrinkHelper.exportDropinAppToServer(server, ear);
        
    }

    @AfterClass
    public static void cleanup() throws Exception {
        server.deleteFileFromLibertyInstallRoot("lib/features/" + testFeatureName);
    }

    @After
    public void tearDown() throws Exception {
        if (server == null) {
            return;
        }

        if (server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testEJBServiceRefPropertiesWithGLobalHandlers() throws Exception {
        
        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*ejbServiceRefBndApp");

        Map<String, String> echoServicePropertyMap = getServletResponse(echoServletURL);
        Map<String, String> helloServicePropertyMap = getServletResponse(helloServletURL);

        String echoClientConnectionTimeOut = echoServicePropertyMap.get("client.ConnectionTimeout");
        String echoClientChunkingThreshold = echoServicePropertyMap.get("client.ChunkingThreshold");

        String helloClientConnectionTimeOut = helloServicePropertyMap.get("client.ConnectionTimeout");
        String helloClientChunkingThreshold = helloServicePropertyMap.get("client.ChunkingThreshold");

        assertTrue("The expected client.ConnectionTimeOut should be '1739' for EchoBean, but the actual is '" + echoClientConnectionTimeOut + "'",
                   "1739".equals(echoClientConnectionTimeOut));
        assertTrue("The expected client.ChunkingThreshold should be '2317' for EchoBean, but the actual is '" + echoClientChunkingThreshold + "'",
                   "2317".equals(echoClientChunkingThreshold));

        assertTrue("The expected client.ConnectionTimeOut should be '1122' for HelloBean, but the actual is '" + helloClientConnectionTimeOut + "'",
                   "1122".equals(helloClientConnectionTimeOut));
        assertTrue("The expected client.ChunkingThreshold should not be '3344' for HelloBean, but the actual is '" + helloClientChunkingThreshold + "'",
                   "3344".equals(helloClientChunkingThreshold));
        assertStatesExsited(5000, new String[] {
                                                 "handle outbound message in TestHandler1InBundle2",
                                                 "handle outbound message in TestHandler2InBundle2" });
    }

    private Map<String, String> getServletResponse(String servletUrl) throws Exception {
        URL url = new URL(servletUrl);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String result = br.readLine();
        String[] keyValues = result.split(",");

        Map<String, String> propertyMap = new HashMap<String, String>();
        for (String keyValue : keyValues) {
            String[] param = keyValue.trim().split("=");
            if (param.length == 2) {
                propertyMap.put(param[0].trim(), param[1].trim());
            }
        }

        return propertyMap;
    }

    private void assertStatesExsited(long timeout, String... states) {
        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLog(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }

}
