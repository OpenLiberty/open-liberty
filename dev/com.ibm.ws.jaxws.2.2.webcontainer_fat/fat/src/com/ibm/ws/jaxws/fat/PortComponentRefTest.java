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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;
import com.ibm.ws.jaxws.fat.util.TestUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This Test is to verify the functionality of the <port-component-ref> is working.
 */
@RunWith(FATRunner.class)
@SkipForRepeat("jaxws-2.3")
public class PortComponentRefTest {

    @Server("PortComponentRefTestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ExplodedShrinkHelper.explodedDropinApp(server, "testPortComponentRefWeb", "com.ibm.ws.jaxws.test.pcr.pojo.server",
                                               "com.ibm.ws.jaxws.test.pcr.pojo.server.stub",
                                               "com.ibm.ws.jaxws.test.pcr.war.web.client");

        ExplodedShrinkHelper.explodedDropinApp(server, "testPortComponentRefEJBinWeb", "com.ibm.ws.jaxws.test.pcr.ejb.server",
                                               "com.ibm.ws.jaxws.test.pcr.ejb.server.stub",
                                               "com.ibm.ws.jaxws.test.pcr.ejbinwar.web.client");

        TestUtils.publishFileToServer(server, "PortComponentRefTest", "testPortComponentRefApplication.ear.xml", "dropins",
                                      "testPortComponentRefApplication.ear.xml");

        ExplodedShrinkHelper.explodedJarToDestination(server, "resources", "testPortComponentRefApplicationEJB", "com.ibm.ws.jaxws.test.pcr.app.ejb.client",
                                                      "com.ibm.ws.jaxws.test.pcr.app.ejb.server",
                                                      "com.ibm.ws.jaxws.test.pcr.app.ejb.server.stub");

        ExplodedShrinkHelper.explodedWarToDestination(server, "resources", "testPortComponentRefApplicationWeb", "com.ibm.ws.jaxws.test.pcr.app.web.client");

        server.startServer("PortComponentRefTest.log");

        // Replace the service's wsdl port address with the real URL,
        // because the client's <port-component-link> will find and use it so that it doesn't need explicitly specify the wsdl location in service-ref.
        TestUtils.replaceServerFileString(server, "dropins/testPortComponentRefWeb.war/wsdl/HelloService.wsdl", "#BASE_URL#", getBaseURL());
        TestUtils.replaceServerFileString(server, "dropins/testPortComponentRefEJBinWeb.war/wsdl/HelloService.wsdl", "#BASE_URL#", getBaseURL());
        TestUtils.replaceServerFileString(server, "resources/testPortComponentRefApplicationEJB.jar/META-INF/wsdl/HelloService.wsdl", "#BASE_URL#", getBaseURL());

        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*testPortComponentRefWeb");
        server.waitForStringInLog("CWWKZ0001I.*testPortComponentRefEJBinWeb");
        server.waitForStringInLog("CWWKZ0001I.*testPortComponentRefApplication");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server == null) {
            return;
        }

        if (server.isStarted()) {
            server.stopServer("CWWKS4000E");
        }

    }

    /**
     * TestDescription:
     * - Test <port-component-ref> could reference the Pojo Web Service in the same Web module.
     * Condition:
     * - A testPortComponentRefWeb.war in dropins
     * - The war contains a Pojo Web service
     * - The Web service has webservices.xml, and configured <wsdl-file>
     * - The war contains wsdl file
     * - The war contains a Servlet Web service Client
     * - The <port-component-ref> is configured in web.xml
     * Result:
     * - invoke the Web Service's sayHello method with parameter "World"
     * - response contains the "Hello World"
     */
    @Test
    public void testPortComponentRefPojoServiceWebClient() throws Exception {
        String result = TestUtils.getServletResponse(getBaseURL() + "/testPortComponentRefWeb/HelloClientServlet?target=World");
        assertTrue("Can not access the Web service in HelloClientServlet, the return result is: " + result, result.contains("Hello World"));
    }

    /**
     * TestDescription:
     * - Test <port-component-ref> could reference the EJB Based Web Service in the same Web module.
     * Condition:
     * - A testPortComponentRefEJBinWeb.war in dropins
     * - The war contains a EJB Web service
     * - The Web service has webservices.xml, and configured <wsdl-file>
     * - The war contains wsdl file
     * - The war contains a Servlet Web service Client
     * - The <port-component-ref> is configured in web.xml
     * Result:
     * - invoke the Web Service's sayHello method with parameter "World"
     * - response contains the "Hello World"
     */
    @Test
    public void testPortComponentRefEJBServiceWebClient() throws Exception {
        String result = TestUtils.getServletResponse(getBaseURL() + "/testPortComponentRefEJBinWeb/HelloClientServlet?target=World");
        assertTrue("Can not access the Web service in HelloClientServlet, the return result is: " + result, result.contains("Hello World"));
    }

    /**
     * TestDescription:
     * - Test <port-component-ref> could reference the EJB Based Web Service in the same EJB module.
     * Condition:
     * - A testPortComponentRefApplicationEJB.jar in testPortComponentRefApplication.ear, using loose config
     * - The EJB contains a EJB Web service
     * - The Web service has webservices.xml, and configured <wsdl-file>
     * - The EJB contains the wsdl file
     * - The EJB contains a EJB Web service Client
     * - The <port-component-ref> is configured in ejb-jar.xml
     * - A testPortComponentRefApplicationWeb.jar in testPortComponentRefApplication.ear, using loose config
     * - The Web application is will inject the EJB Web service Client to do test.
     * Result:
     * - invoke the Web Service's sayHello method with parameter "World" from the EJB Web service Client
     * - response contains the "Hello World"
     */
    @Test
    public void testPortComponentRefEJBServiceEJBClient() throws Exception {
        String result = TestUtils.getServletResponse(getBaseURL() + "/testPortComponentRefApplicationWeb/HelloClientServlet");
        assertTrue("Can not access the Web service in HelloClientServlet, the return result is: " + result, result.contains("Hello World"));
    }

    protected static String getBaseURL() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    }
}
