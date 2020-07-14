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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class EJBWSInterceptorTest {

    @Server("com.ibm.ws.jaxws.ejb.fat.ejbwsinterceptor")
    public static LibertyServer server;

    private static final String ejbwsinterceptorjar = "EJBWSInterceptor";
    private static final String ejbwsinterceptorclientwar = "EJBWSInterceptorClient";
    private static final String ejbwsinterceptorear = "EJBWSInterceptor";

    private static String SERVICE_ADDRESS = null;

    private static final String SERVLET_PATH = "/EJBWSInterceptorClient/EJBWSInterceptorTestServlet";

    @Before
    public void before() throws Exception {

        JavaArchive jar = ShrinkHelper.buildJavaArchive(ejbwsinterceptorjar + ".jar", "com.ibm.ws.jaxws.ejbinterceptor");

        WebArchive war = ShrinkWrap.create(WebArchive.class, ejbwsinterceptorclientwar + ".war").addPackages(true, "com.ibm.ws.jaxws.ejbinterceptor.client");
        ShrinkHelper.addDirectory(war, "test-applications/EJBWSInterceptorClient/resources/");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ejbwsinterceptorear + ".ear").addAsModule(war).addAsModule(jar);

        ShrinkHelper.exportDropinAppToServer(server, ear);

        try {
            server.startServer();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        SERVICE_ADDRESS = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/EJBWSInterceptor/SayHelloService").toString();
        System.out.println("~~SERVICE_ADDRESS: " + SERVICE_ADDRESS);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void testInterceptor() throws Exception {
        String encodedServiceAddress = URLEncoder.encode(SERVICE_ADDRESS, "utf-8");

        String servletUrl = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append(SERVLET_PATH).append("?url=").append(encodedServiceAddress).toString();
        System.out.println("~~servletUrl: " + servletUrl);

        String expectedValue = "hello, EJBWSInterceptor";
        String expectedTraceOutout = "com.ibm.ws.jaxws.ejbinterceptor.SayHelloInterceptor intercepted the method";

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(servletUrl), HttpURLConnection.HTTP_OK, 10);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        assertTrue("The response should be '" + expectedValue + "', while the actual is '" + line + "'", expectedValue.equals(line));
        assertNotNull("The expected output in server log is " + expectedTraceOutout, server.waitForStringInLog(expectedTraceOutout));
    }

}
