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

import org.junit.After;
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
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This class tests the security of ejb based web services, which are packaged
 * in a war package. - no security constraints - security constraints are
 * configured in web.xml - security constraints are configured in ibm-ws-bnd.xml
 */
@RunWith(FATRunner.class)
@Mode(Mode.TestMode.FULL)
public class EJBInWarServiceSecurityTest {

    @Server("EJBInWarSecurityServer")
    public static LibertyServer server;

    private final static int REQUEST_TIMEOUT = 10;
    private static String wsdlUrl;

    private static String securedWsdlUrl;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));

        ExplodedShrinkHelper.explodedApp(server, "EJBInWarServiceSecurity", "com.ibm.samples.jaxws",
                                         "com.ibm.samples.servlet");

        ExplodedShrinkHelper.explodedJarToDestination(server, "apps/EJBInWarServiceSecurity.war/WEB-INF/lib",
                                                      "EJBInJarServiceSecurity", "com.ibm.samples.jaxws", "com.ibm.sample.ejb");

        server.copyFileToLibertyServerRoot("apps/EJBInWarServiceSecurity.war/WEB-INF/wsdl",
                                           "EJBInWarServiceSecurity/WEB-INF/wsdl/SecuredSayHelloService.wsdl");

        server.copyFileToLibertyServerRoot("apps/EJBInWarServiceSecurity.war/WEB-INF/wsdl",
                                           "EJBInWarServiceSecurity/WEB-INF/wsdl/SayHelloService.wsdl");

        wsdlUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                  + "/EJBInWarServiceSecurity/SayHelloService?wsdl";

        securedWsdlUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                         + "/EJBInWarServiceSecurity/SecuredSayHelloService?wsdl";

    }

    @After
    public void afterEachTest() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
    @Test
    @AllowedFFDC({ "java.rmi.AccessException" })
    public void test_ejbws_in_war_security_with_webxml_configuration() throws Exception {
        // delete whatever web.xml
        server.deleteFileFromLibertyServerRoot("apps/EJBInWarServiceSecurity.war/WEB-INF/web.xml");
        // add web.xml with security constraint
        server.copyFileToLibertyServerRoot("apps/EJBInWarServiceSecurity.war/WEB-INF",
                                           "EJBWSSecurityFileStore/secure/web.xml");
        server.startServer();

        checkAppsReady();
        server.setMarkToEndOfLog();

        runTest("user1", "user2pwd", "SayHelloService", "Hello user1 from ejb web service.", false);
        runTest("user1", "user1pwd", "SayHelloService", "Hello user1 from ejb web service.", false);
        runTest("user2", "user2pwd", "SayHelloService", "Hello user2 from ejb web service.", true);

        runTest("user3", "user3pwd", "SayHelloService", "Hello user3 from ejb web service.", true);
        runTest("user4", "user4pwd", "SayHelloService", "Hello user4 from ejb web service.", false);
        runTest("user1", "user2pwd", "SecuredSayHelloService", "Hello user1 from secured ejb web service.", false);
        runTest("user1", "user1pwd", "SecuredSayHelloService", "Hello user1 from secured ejb web service.", false);
        runTest("user2", "user2pwd", "SecuredSayHelloService", "Hello user2 from secured ejb web service.", true);
        runTest("user3", "user3pwd", "SecuredSayHelloService", "Hello user3 from secured ejb web service.", false);
        runTest("user4", "user4pwd", "SecuredSayHelloService", "Hello user4 from secured ejb web service.", false);

        server.stopServer();
    }

    @Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
    @Test
    @AllowedFFDC({ "java.rmi.AccessException" })
    public void test_ejbws_in_war_security_without_webxml_configuration() throws Exception {
        // delete whatever web.xml
        server.deleteFileFromLibertyServerRoot("apps/EJBInWarServiceSecurity.war/WEB-INF/web.xml");
        // add web.xml without security constraint
        server.copyFileToLibertyServerRoot("apps/EJBInWarServiceSecurity.war/WEB-INF",
                                           "EJBWSSecurityFileStore/nonsecure/web.xml");
        server.startServer();

        checkAppsReady();
        server.setMarkToEndOfLog();

        runTest("user1", "user2pwd", "SayHelloService", "Hello user1 from ejb web service.", true);
        runTest("user1", "user1pwd", "SayHelloService", "Hello user1 from ejb web service.", true);
        runTest("user2", "user2pwd", "SayHelloService", "Hello user2 from ejb web service.", true);
        runTest("user3", "user3pwd", "SayHelloService", "Hello user3 from ejb web service.", true);
        runTest("user4", "user4pwd", "SayHelloService", "Hello user4 from ejb web service.", true);
        runTest("user1", "user2pwd", "SecuredSayHelloService", "Hello user1 from secured ejb web service.", false);
        runTest("user1", "user1pwd", "SecuredSayHelloService", "Hello user1 from secured ejb web service.", true);
        runTest("user2", "user2pwd", "SecuredSayHelloService", "Hello user2 from secured ejb web service.", true);
        runTest("user3", "user3pwd", "SecuredSayHelloService", "Hello user3 from secured ejb web service.", false);
        runTest("user4", "user4pwd", "SecuredSayHelloService", "Hello user4 from secured ejb web service.", false);

        server.stopServer();
    }

    protected void runTest(String username, String password, String serviceName, String responseString,
                           boolean expected) throws ProtocolException, MalformedURLException, IOException {
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/EJBInWarServiceSecurity/SayHelloServlet").append("?username=").append(username).append("&password=").append(password).append("&service=").append(serviceName).append("&war=EJBInWarServiceSecurity");
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

    private void checkAppsReady() {
        Assert.assertNotNull("The application EJBInWarServiceSecurity did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*EJBInWarServiceSecurity"));
        Assert.assertNotNull("Security service did not report it was ready", server.waitForStringInLog("CWWKS0008I"));

        server.waitForStringInLog("CWWKO0219I:.*-ssl");

    }

}
