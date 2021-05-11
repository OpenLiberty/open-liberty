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

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@SkipForRepeat({ "jaxws-2.3", SkipForRepeat.EE9_FEATURES })
public class EJBWSBasicTest {

    @Server("com.ibm.ws.jaxws.ejb.fat.ejbwsbasic")
    public static LibertyServer server;

    private static final String ejbwsbasicjar = "EJBWSBasic";
    private static final String ejbwsbasicclientwar = "EJBWSBasicClient";
    private static final String ejbwsbasicear = "EJBWSBasic";

    private static final String SERVLET_PATH = "/EJBWSBasicClient/EJBBasicClientServlet";

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void beforeAllTests() throws Exception {

        JavaArchive jar = ShrinkHelper.buildJavaArchive(ejbwsbasicjar + ".jar", "com.ibm.ws.jaxws.ejbbasic.*");

        WebArchive war = ShrinkWrap.create(WebArchive.class, ejbwsbasicclientwar + ".war").addPackages(true, "com.ibm.ws.jaxws.ejbbasic");
        ShrinkHelper.addDirectory(war, "test-applications/EJBWSBasicClient/resources/");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ejbwsbasicear + ".ear").addAsModule(jar).addAsModule(war);

        ShrinkHelper.exportDropinAppToServer(server, ear);

        try {
            server.startServer();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        Assert.assertNotNull("The application EJBWSBasic did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*EJBWSBasic"));
        /*
         * Assert.assertNotNull("The application EJBWSBasicClient did not appear to have started",
         * server.waitForStringInLog("CWWKZ0001I.*EJBWSBasicClient"));
         */
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testQueryUser() throws Exception {
        runTest("PASS");
    }

    /*
     * TODO: Investigate why the correct custom exception (UserNotFoundException) is being thrown, but
     * is now being wrapped in an InvocationTargetException
     */
    @Mode(TestMode.FULL)
    //@Test
    public void testUserNotFoundException() throws Exception {
        runTest("PASS");
    }

    @Mode(TestMode.FULL)
    // @Test
    public void testListUsers() throws Exception {
        runTest("PASS");
    }

    //@Test
    public void testQueryUserBasicAsyncHandler() throws Exception {
        runTest("PASS");
    }

    //@Test
    public void testQueryUserBasicAsyncResponse() throws Exception {
        runTest("PASS");
    }

    @Mode(TestMode.FULL)
    //@Test
    public void testQueryUserBasicAsyncHandler_EJB() throws Exception {
        runTest("PASS");
    }

    @Mode(TestMode.FULL)
    //@Test
    public void testQueryUserBasicAsyncResponse_EJB() throws Exception {
        runTest("PASS");
    }

    @Mode(TestMode.FULL)
    //@Test
    public void testInConsistentNamespace() throws Exception {
        runTest("PASS");
    }

    protected void runTest(String responseString) throws Exception {

        // Strip the Test Rerun id's out of the method name
        String testMethod = ((testName.getMethodName()).replace("_jaxws-2.3",
                                                                "")).replace("_EE9_FEATURES",
                                                                             "");

        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append(SERVLET_PATH).append("?testMethod=").append(testMethod);
        String urlStr = sBuilder.toString();
        Log.info(this.getClass(), testMethod, "Calling Application with URL=" + urlStr);

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, 10);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        assertTrue("The excepted response must contain " + responseString + " while " + line + " is received", line.contains(responseString));
    }
}
