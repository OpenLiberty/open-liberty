/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxws.security.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This class tests the security of ejb based web services, which are packaged
 * in a jar package. - no security constraints - security constraints are
 * configured in ibm-ws-bnd.xml
 */
@RunWith(FATRunner.class)
public class EJBInJarServiceSecurityTest {

    @Server("EJBInJarSecurityServer")
    public static LibertyServer server;

    private final static int REQUEST_TIMEOUT = 10;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("EJBInJarServiceSecurityClient", "com.ibm.samples.jaxws",
                                                      "com.ibm.samples.servlet");

        ExplodedShrinkHelper.explodedEarApp(server, war, "EJBInJarServiceSecurityClient", "EJBInJarServiceSecurity",
                                            false);

        ExplodedShrinkHelper.explodedJarToDestination(server, "apps/EJBInJarServiceSecurity.ear",
                                                      "EJBInJarServiceSecurity", "com.ibm.samples.jaxws", "com.ibm.sample.ejb");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("WebServiceRefTest.log");
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*EJBInJarServiceSecurity");

        // BASE_URL = "http://" + server.getHostname() + ":" +
        // server.getHttpDefaultPort();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Before
    public void beforeEachTest() throws Exception {

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
    public void test_ejbws_in_jar_security_with_bndfile() throws Exception {

        // add ibm-ws-bnd.xml
        if (!server.fileExistsInLibertyServerRoot(
                                                  "apps/EJBInJarServiceSecurity.ear/EJBInJarServiceSecurity.jar/META-INF/ibm-ws-bnd.xml")) {
            server.copyFileToLibertyServerRoot(
                                               "apps/EJBInJarServiceSecurity.ear/EJBInJarServiceSecurity.jar/META-INF/ibm-ws-bnd.xml",
                                               "EJBWSSecurityFileStore/ibm-ws-bnd.xml");
        }

        if (server != null && !server.isStarted()) {
            // delete whatever ejb jar
            server.startServer();

            checkAppsReady();
            server.setMarkToEndOfLog();
        }

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
    }

    @Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
    @Test
    @AllowedFFDC({ "java.rmi.AccessException" })
    public void test_ejbws_in_jar_security_without_bndfile() throws Exception {

        // delete ibm-ws-bnd.xml
        if (server.fileExistsInLibertyServerRoot(
                                                 "apps/EJBInJarServiceSecurity.ear/EJBInJarServiceSecurity.jar/META-INF/ibm-ws-bnd.xml"))
            server.deleteFileFromLibertyServerRoot(
                                                   "apps/EJBInJarServiceSecurity.ear/EJBInJarServiceSecurity.jar/META-INF/ibm-ws-bnd.xml");

        if (server != null && !server.isStarted()) {
            // delete whatever ejb jar
            server.startServer();

            checkAppsReady();
            server.setMarkToEndOfLog();
        }

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

    private void checkAppsReady() {
        Assert.assertNotNull("The application EJBInJarServiceSecurity did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*EJBInJarServiceSecurity"));
        Assert.assertNotNull("Security service did not report it was ready", server.waitForStringInLog("CWWKS0008I"));
    }
}
