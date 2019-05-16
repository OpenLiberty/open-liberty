/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejb.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class EJBInWarServiceTest {
    private static final String SERVLET_PATH = "/EJBInWarServiceClient/EJBWebServiceServlet";

    private final static int REQUEST_TIMEOUT = 10;

    @Server("com.ibm.ws.jaxws.ejb.fat.ejbinwarservice")
    public static LibertyServer server;

    private static final String ejbinwarservicewar = "EJBInWarService";
    private static final String ejbinwarservicewarclient = "EJBInWarServiceClient";

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeAllTests() throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ejbinwarservicewar + ".war").addPackages(true, "com.ibm.ws.jaxws.ejbinwar.ejb");
        ShrinkHelper.addDirectory(war, "test-applications/EJBInWarService/resources/");
        ShrinkHelper.exportDropinAppToServer(server, war);

        WebArchive warclient = ShrinkWrap.create(WebArchive.class, ejbinwarservicewarclient + ".war").addPackages(true, "com.ibm.ws.jaxws.ejbinwar");
        ShrinkHelper.addDirectory(warclient, "test-applications/EJBInWarServiceClient/resources/");
        ShrinkHelper.exportDropinAppToServer(server, warclient);

        try {
            server.startServer();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        assertNotNull("The application EJBInWarService.war did not appear to have started",
                      server.waitForStringInLog("CWWKZ0001I.*EJBInWarService"));
        assertNotNull("The application EJBInWarServiceClient.war did not appear to have started",
                      server.waitForStringInLog("CWWKZ0001I.*EJBInWarServiceClient"));
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testSayHelloFromStateless() throws Exception {
        runTest("Hello, user from SayHelloStatelessBean");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSayHelloFromSingle() throws Exception {
        runTest("Hello, user from SayHelloSingletonBean");
    }

    @Test
    public void testInvokeOtherFromStateless() throws Exception {
        runTest("Hello, Anonym from SayHelloStatelessBean");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testInvokeOtherFromSingle() throws Exception {
        runTest("Hello, StatelessSessionBeanClient from SayHelloStatelessBean");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSayHelloFromPojo() throws Exception {
        runTest("Hello, user from SayHelloPojoBean");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testInvokeOtherFromPojo() throws Exception {
        runTest("Hello, Anonym from SayHelloPojoBean");
    }

    protected void runTest(String responseString) throws ProtocolException, MalformedURLException, IOException {
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append(SERVLET_PATH).append("?testMethod=").append(testName.getMethodName()).append("&hostName=").append(server.getHostname());
        String urlStr = sBuilder.toString();
        Log.info(this.getClass(), testName.getMethodName(), "Calling Application with URL=" + urlStr);

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        assertTrue("The excepted response must contain " + responseString, line.contains(responseString));
    }
}
