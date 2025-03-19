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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This class tests the security constraints of POJO web services, which are
 * packaged in a war package. - no security constraints - security constraints
 * are configured in web.xml - security constraints are configured in
 * ibm-ws-bnd.xml
 */
@RunWith(FATRunner.class)
public class POJOServiceSecurityTest {
    @Server("POJOServiceSecurityServer")
    public static LibertyServer server;

    private final static int REQUEST_TIMEOUT = 10;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeAllTests() throws Exception {

        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        ExplodedShrinkHelper.explodedDropinApp(server, "POJOServiceSecurityClient", "com.ibm.samples.jaxws2",
                                               "com.ibm.samples.servlets");

        ExplodedShrinkHelper.explodedApp(server, "POJOServiceSecurity", "com.ibm.sample");
        server.startServer();
        Assert.assertNotNull("The application POJOServiceSecurity did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*POJOServiceSecurity"));
        Assert.assertNotNull("The application POJOServiceSecurityClient did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*POJOServiceSecurityClient"));
        Assert.assertNotNull("Security service did not report it was ready", server.waitForStringInLog("CWWKS0008I"));
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWPKI0823E");
        }
    }

    @Test
    @AllowedFFDC({ "java.rmi.AccessException" })
    public void test_pojows_security_with_bndfile() throws Exception {
        runTest("user1", "user2pwd", "SayHelloServiceOne", "Hello user1 from SayHelloServiceOne.", false);
        runTest("user1", "user1pwd", "SayHelloServiceOne", "Hello user1 from SayHelloServiceOne.", true);
        runTest("user2", "user2pwd", "SayHelloServiceOne", "Hello user2 from SayHelloServiceOne.", false);
        runTest("user3", "user3pwd", "SayHelloServiceOne", "Hello user3 from SayHelloServiceOne.", false);
        runTest("user4", "user4pwd", "SayHelloServiceOne", "Hello user4 from SayHelloServiceOne.", false);
    }

    @Test
    @AllowedFFDC({ "java.rmi.AccessException" })
    public void test_pojows_security_with_webxml() throws Exception {
        runTest("user1", "user2pwd", "SayHelloServiceTwo", "Hello user1 from SayHelloServiceTwo.", false);
        runTest("user1", "user1pwd", "SayHelloServiceTwo", "Hello user1 from SayHelloServiceTwo.", false);
        runTest("user2", "user2pwd", "SayHelloServiceTwo", "Hello user2 from SayHelloServiceTwo.", true);
        runTest("user3", "user3pwd", "SayHelloServiceTwo", "Hello user3 from SayHelloServiceTwo.", false);
        runTest("user4", "user4pwd", "SayHelloServiceTwo", "Hello user4 from SayHelloServiceTwo.", false);
    }

    @Test
    public void test_pojows_without_security() throws Exception {
        runTest("user1", "user2pwd", "SayHelloServiceThree", "Hello user1 from SayHelloServiceThree.", true);
        runTest("user1", "user1pwd", "SayHelloServiceThree", "Hello user1 from SayHelloServiceThree.", true);
        runTest("user2", "user2pwd", "SayHelloServiceThree", "Hello user2 from SayHelloServiceThree.", true);
        runTest("user3", "user3pwd", "SayHelloServiceThree", "Hello user3 from SayHelloServiceThree.", true);
        runTest("user4", "user4pwd", "SayHelloServiceThree", "Hello user4 from SayHelloServiceThree.", true);
    }

    protected void runTest(String username, String password, String serviceName, String responseString,
                           boolean expected) throws ProtocolException, MalformedURLException, IOException {
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/POJOServiceSecurityClient/SayHelloServlet").append("?username=").append(username).append("&password=").append(password).append("&service=").append(serviceName).append("&war=POJOServiceSecurity");
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

}
