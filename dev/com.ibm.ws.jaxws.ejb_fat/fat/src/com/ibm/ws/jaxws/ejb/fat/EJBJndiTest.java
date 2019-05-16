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
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class EJBJndiTest {

    @Server("com.ibm.ws.jaxws.ejb.fat.ejbjndi")
    public static LibertyServer server;

    private static final String ejbjndiejbjar = "EJBJndiEJB";
    private static final String ejbjndiwebwar = "EJBJndiWeb";
    private static final String ejbjndiwebejbwar = "EJBJndiWebEJB";
    private static final String ejbjndicommon = "EJBJndiCommon";
    private static final String ejbjndiear = "EJBJndi";

    private static final String SERVLET_TARGET = "Servlet";

    private static final String EJB_TARGET = "EJB";

    private static final String SERVLET_PATH = "/EJBJndiWebEJB/MixedWaiterServlet";
    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void beforeAllTests() throws Exception {

        JavaArchive jar = ShrinkHelper.buildJavaArchive(ejbjndiejbjar + ".jar", "com.ibm.ws.jaxws.ejbjndi.ejb");

        WebArchive war = ShrinkWrap.create(WebArchive.class, ejbjndiwebwar + ".war").addPackage("com.ibm.ws.jaxws.ejbjndi.web");
        ShrinkHelper.addDirectory(war, "test-applications/EJBJndiWeb/resources/");

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, ejbjndiwebejbwar + ".war").addPackage("com.ibm.ws.jaxws.ejbjndi.webejb");
        ShrinkHelper.addDirectory(war2, "test-applications/EJBJndiWebEJB/resources/");

        JavaArchive jar2 = ShrinkHelper.buildJavaArchive(ejbjndicommon + ".jar", "com.ibm.ws.jaxws.ejbjndi.*");
        ShrinkHelper.addDirectory(jar2, "test-applications/EJBJndiCommon/resources/");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ejbjndiear + ".ear").addAsModule(jar).addAsModule(war).addAsModule(war2).addAsLibraries(jar2);
        //ear.add(new FileAsset(new File("../../../publish/files/EJBJndi/application.xml")), "/META-INF/application.xml");
        ear.add(new FileAsset(new File("lib/LibertyFATTestFiles/EJBJndi/application.xml")), "/META-INF/application.xml");

        ShrinkHelper.exportDropinAppToServer(server, ear);

        try {
            server.startServer();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        Assert.assertNotNull("The application EJBJndi did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*EJBJndi"));
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Test Description : Inject a comp scope WebServiceRef instance in the servlet
     */
    @Test
    public void testWebEJBCompServlet() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject a module scope WebServiceRef instance in the servlet
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBModuleServlet() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject an app scope WebServiceRef instance in the servlet
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBAppServlet() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject a global scope WebServiceRef instance in the servlet
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBGlobalServlet() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject an app scope WebServiceRef instance from another EJB module in the servlet
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBNonLocalAppEJBServlet() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject a global scope WebServiceRef instance from another EJB module in the servlet
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBNonLocalGlobalEJBServlet() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject an app scope WebServiceRef instance from another Web module in the servlet
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBNonLocalAppWebServlet() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject a global scope WebServiceRef instance from another Web module in the servlet
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBNonLocalGlobalWebServlet() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject a comp scope WebServiceRef instance from an EJB bean in the servlet
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBEJBCompServlet() throws Exception {
        runTest("PASS");
    }

    /*----------------------------------------------Testing Method for WebServiceRef in EJB Bean ---------------------------------------------*/

    /*
     * Test Description : Inject a comp scope WebServiceRef instance in an EJB Bean
     */
    @Test
    public void testWebEJBCompEJB() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject a global scope WebServiceRef instance in an EJB Bean
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBModuleEJB() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject a global scope WebServiceRef instance in an EJB Bean
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBAppEJB() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject a global scope WebServiceRef instance in an EJB Bean
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBGlobalEJB() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject an app scope WebServiceRef instance from another web module in the same EAR
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBNonLocalAppWebEJB() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject a global scope WebServiceRef instance from another web module in the same EAR
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBNonLocalGlobalWebEJB() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject an app scope WebServiceRef instance from another EJB module in the same EAR
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBNonLocalAppEJBEJB() throws Exception {
        runTest("PASS");
    }

    /*
     * Test Description : Inject a global scope WebServiceRef instance from another EJB module in the same EAR
     */
    @Mode(TestMode.FULL)
    @Test
    public void testWebEJBNonLocalGlobalEJBEJB() throws Exception {
        runTest("PASS");
    }

    protected void runTest(String responseString) throws Exception {

        String testMethod = testName.getMethodName();

        String target = null;
        String remoteTestMethod = null;
        if (testMethod.endsWith(SERVLET_TARGET)) {
            target = SERVLET_TARGET;
            remoteTestMethod = testMethod.substring(0, testMethod.length() - SERVLET_TARGET.length());
        } else if (testMethod.endsWith(EJB_TARGET)) {
            target = EJB_TARGET;
            remoteTestMethod = testMethod.substring(0, testMethod.length() - EJB_TARGET.length());
        }

        StringBuilder requestURLBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append(SERVLET_PATH).append("?type=").append(remoteTestMethod).append("&hostname=").append(server.getHostname()).append("&port=").append(server.getHttpDefaultPort()).append("&target=").append(target);

        String requestURL = requestURLBuilder.toString();
        Log.info(this.getClass(), testName.getMethodName(), "Calling Application with URL=" + requestURL);

        HttpURLConnection con = null;
        try {
            con = HttpUtils.getHttpConnection(new URL(requestURL), HttpURLConnection.HTTP_OK, 10);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();

            assertTrue("The excepted response must contain " + responseString + " while " + line + " is received", line.contains(responseString));
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (Exception e) {
                }
            }
        }
    }
}
