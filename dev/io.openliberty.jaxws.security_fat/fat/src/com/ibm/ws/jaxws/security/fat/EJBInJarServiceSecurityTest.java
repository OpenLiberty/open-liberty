/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.security.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This abstract class provides a super class for two tests split by their usage of ibm-ws.bnd.xml or not
 */
public abstract class EJBInJarServiceSecurityTest {

    @Server("EJBInJarSecurityServer")
    public static LibertyServer server;

    @Rule
    public TestName testName = new TestName();

    private final static int REQUEST_TIMEOUT = 10;

    protected static void init() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("EJBInJarServiceSecurityClient", "com.ibm.samples.jaxws",
                                                      "com.ibm.samples.servlet");

        ExplodedShrinkHelper.explodedEarApp(server, war, "EJBInJarServiceSecurityClient", "EJBInJarServiceSecurity",
                                            false);

        ExplodedShrinkHelper.explodedJarToDestination(server, "apps/EJBInJarServiceSecurity.ear",
                                                      "EJBInJarServiceSecurity", "com.ibm.samples.jaxws", "com.ibm.sample.ejb");
    }

    public static void startServer(String logName) throws Exception {
        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        if (server != null && !server.isStarted()) {
            server.startServer(logName);
            checkAppsReady();
            server.setMarkToEndOfLog();
        }
    }

    public static void stopServer() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    protected void runTest(String username, String password, String serviceName, String responseString,
                           boolean expected) throws ProtocolException, MalformedURLException, IOException {
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/EJBInJarServiceSecurityClient/SayHelloServlet").append("?username=").append(username).append("&password=").append(password).append("&service=").append(serviceName).append("&war=EJBInJarServiceSecurity");
        String urlStr = sBuilder.toString();
        Log.info(this.getClass(), testName.getMethodName(), "Calling Application with URL=" + urlStr);

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK,
                                                            REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        if (expected) {
            assertTrue("The excepted response must contain " + responseString + ", but the real response is " + line,
                       line.contains(responseString));
        } else {
            assertFalse(
                        "The excepted response must not contain " + responseString + ", but the real response is " + line,
                        line.contains(responseString));
        }
    }

    private static void checkAppsReady() {
        Assert.assertNotNull("The application EJBInJarServiceSecurity did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*EJBInJarServiceSecurity"));
        Assert.assertNotNull("Security service did not report it was ready", server.waitForStringInLog("CWWKS0008I"));
    }
}
