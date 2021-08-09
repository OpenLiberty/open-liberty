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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jaxws.ejbwscontext.EchoInfo;
import com.ibm.ws.jaxws.ejbwscontext.EchoInfoI;
import com.ibm.ws.jaxws.ejbwscontext.EchoInfoInterface;
import com.ibm.ws.jaxws.ejbwscontext.EchoInfoService;
import com.ibm.ws.jaxws.ejbwscontext.GetInfo;
import com.ibm.ws.jaxws.ejbwscontext.GetInfoResponse;
import com.ibm.ws.jaxws.ejbwscontext.ObjectFactory;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class EJBWSContextTest {

    @Server("com.ibm.ws.jaxws.ejb.fat.ejbwscontext")
    public static LibertyServer server;

    private static final String ejbwscontextjar = "EJBWSContext";
    private static final String ejbwscontextclientwar = "EJBWSContextClient";
    private static final String ejbwscontextear = "EJBWSContext";

    private static final String SERVLET_PATH = "/EJBWSContextClient/EJBWSContextTestServlet";

    @BeforeClass
    public static void beforeAllTests() throws Exception {

        JavaArchive jar = ShrinkHelper.buildJavaArchive(ejbwscontextjar + ".jar").addClasses(EchoInfo.class, EchoInfoInterface.class);

        ShrinkHelper.addDirectory(jar, "test-applications/EJBWSContext/resources/");

        Class<?> c = Class.forName("com.ibm.ws.jaxws.ejbwscontext.package-info");
        WebArchive war = ShrinkWrap.create(WebArchive.class,
                                           ejbwscontextclientwar + ".war").addClasses(EchoInfoI.class, EchoInfoService.class, GetInfo.class, GetInfoResponse.class,
                                                                                      ObjectFactory.class, c).addPackages(true, "com.ibm.ws.jaxws.ejbwscontext.client");
        ShrinkHelper.addDirectory(war, "test-applications/EJBWSContextClient/resources/");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ejbwscontextear + ".ear").addAsModule(jar).addAsModule(war);

        ShrinkHelper.exportAppToServer(server, ear);

        try {
            server.startServer();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        Assert.assertNotNull("The application EJBWSContext did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*EJBWSContext"));
        Assert.assertNotNull("Security service did not report it was ready",
                             server.waitForStringInLog("CWWKS0008I"));
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testPrin() throws Exception {
        runTest("PRIN", "@OK: Principal");
    }

    @Test
    public void testRole() throws Exception {
        runTest("ROLE", "@OK: role_1 allowed");
    }

    @Test
    public void testMsgContextKeySize() throws Exception {
        runTest("MCKEYSIZE", "@OK: MessageContext key size");
    }

    @Test
    public void testMsgContextFieldsGetter() throws Exception {
        runTest("MCFIELDS", "@OK: Common Fields Loaded");
    }

    protected void runTest(String action, String targetString) throws Exception {
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append(SERVLET_PATH).append("?action=").append(action).append("&user=user1&pwd=u1pwd");
        String urlStr = sBuilder.toString();
        Log.info(this.getClass(), action, "request URL=" + urlStr);

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, 10);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        assertTrue("The excepted response should contain " + targetString + " while " + line + " is received", line.contains(targetString));
    }
}
