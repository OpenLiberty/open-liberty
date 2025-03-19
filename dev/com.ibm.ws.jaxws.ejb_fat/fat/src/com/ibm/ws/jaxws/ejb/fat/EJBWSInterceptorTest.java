/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxws.ejb.fat;

import static org.junit.Assert.assertEquals;
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

import componenttest.annotation.ExpectedFFDC;
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
    private static String EXCEPTION_SERVICE_ADDRESS = null;

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
        EXCEPTION_SERVICE_ADDRESS = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/EJBWSInterceptor/SayHelloWithInterceptorExceptionService").toString();

        System.out.println("~~SERVICE_ADDRESS: " + SERVICE_ADDRESS);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            // CNTR0020E : testInterceptorException : EJB threw unexpected exception
            server.stopServer("CNTR0020E");
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

    // This test currently hard fails due to unexpected NCDFE, once this is fix, test should be updated to check for expected
    // UserNotFoundException_Exception
    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testInterceptorException() throws Exception {
        String encodedServiceAddress = URLEncoder.encode(EXCEPTION_SERVICE_ADDRESS, "utf-8");

        String servletUrl = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append(SERVLET_PATH).append("?url=").append(encodedServiceAddress).toString();
        System.out.println("~~servletUrl: " + servletUrl);

        String expectedValue = "EJBException: See nested exception; nested exception is: com.ibm.ws.jaxws.ejbinterceptor.UserNotFoundException_Exception: UserNotFound while invoking public java.lang.String com.ibm.ws.jaxws.ejbinterceptor.SayHelloWithInterceptorException.hello(java.lang.String) with params [EJBWSInterceptor].";
        String expectedTraceOutout = "com.ibm.ws.jaxws.ejbinterceptor.SayHelloInterceptorWithException intercepted the method";

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(servletUrl), HttpURLConnection.HTTP_OK, 10);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        line = br.readLine();
        if (line != null) {
            int index = line.indexOf("EJBException");
            if (index > 0) {
                line = line.substring(index);
            }
        }

        assertNotNull("The expected output in server log is " + expectedTraceOutout, server.waitForStringInLog(expectedTraceOutout));
        assertEquals("Unexpected response", expectedValue, line);
    }

}
