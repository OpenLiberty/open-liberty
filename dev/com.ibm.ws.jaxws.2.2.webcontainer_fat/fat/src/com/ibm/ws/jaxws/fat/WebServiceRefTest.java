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

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This is to test the @WebServiceRef annotation works in the JAX-WS client
 */
@RunWith(FATRunner.class)
public class WebServiceRefTest {

    @Server("WebServiceRefTestServer")
    public static LibertyServer server;
    private static String BASE_URL;
    private static final int CONN_TIMEOUT = 5;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "helloClient", "com.ibm.ws.jaxws.test.wsr.client",
                                      "com.ibm.ws.jaxws.test.wsr.server.stub",
                                      "com.ibm.ws.jaxws.fat.util");
        ShrinkHelper.defaultDropinApp(server, "helloClientDDMerge", "com.ibm.ws.jaxws.test.wsr.clientddmerge",
                                      "com.ibm.ws.jaxws.test.wsr.server.stub",
                                      "com.ibm.ws.jaxws.fat.util");
        ShrinkHelper.defaultDropinApp(server, "helloClientServiceResource", "com.ibm.ws.jaxws.test.wsr.clientserviceresource",
                                      "com.ibm.ws.jaxws.test.wsr.server.stub",
                                      "com.ibm.ws.jaxws.fat.util");
        ShrinkHelper.defaultDropinApp(server, "helloServer", "com.ibm.ws.jaxws.test.wsr.server",
                                      "com.ibm.ws.jaxws.test.wsr.server.impl",
                                      "com.ibm.ws.jaxws.fat.util");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("WebServiceRefTest.log");
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*helloServer");
        server.waitForStringInLog("CWWKZ0001I.*helloClient");
        server.waitForStringInLog("CWWKZ0001I.*helloClientDDMerge");
        server.waitForStringInLog("CWWKZ0001I.*helloClientServiceResource");

        BASE_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * TestDescription: Using WebServiceRef annotation in client to reference JAX-WS Service or Port type instance.
     * Condition:
     * - Client servlets uses @WebServiceRef annotation
     * - No <service-ref> in web.xml
     * Result:
     * - response contains "Hello World"
     */
    @Test
    public void testWebServiceRefAnnoatation() throws Exception {
        String clientUrlStr = BASE_URL + "/helloClient";

        checkClientHelloWorld(clientUrlStr, "ServiceInjectionNormalServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionObjectMemberServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionObjectTypeServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionServiceMemberServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionServiceTypeServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionMultiTargetsServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionClassLevelServlet");

        checkClientHelloWorld(clientUrlStr, "PortTypeInjectionNormalServlet");
        checkClientHelloWorld(clientUrlStr, "PortTypeInjectionObjectTypeServlet");
        checkClientHelloWorld(clientUrlStr, "PortTypeInjectionClassLevelServlet");

    }

    /**
     * TestDescription: Using WebServiceRef annotation along with deployment descriptor in client to reference JAX-WS Service or Port type instance.
     * Condition:
     * - Client servlets uses @WebServiceRef annotation, but the information is not complete.
     * - Define <service-ref> in web.xml
     * Result:
     * - response contains "Hello World"
     */
    @Test
    public void testWebServiceRefDDMerge() throws Exception {
        String clientUrlStr = BASE_URL + "/helloClientDDMerge";

        checkClientHelloWorld(clientUrlStr, "ServiceInjectionClassLevelServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionMemberLevelServlet");

        checkClientHelloWorld(clientUrlStr, "PortTypeInjectionClassLevelServlet");
        checkClientHelloWorld(clientUrlStr, "PortTypeInjectionMemberLevelServlet");

    }

    /**
     * TestDescription: Using Resource annotation along with deployment descriptor in client to reference JAX-WS Service instance.
     * Condition:
     * - Client servlets uses @Resource annotation to reference a JAX-WS Service.
     * - Define <service-ref> in web.xml
     * Result:
     * - response contains "Hello World"
     */
    @Test
    public void testWebServiceRefServiceResource() throws Exception {
        String clientUrlStr = BASE_URL + "/helloClientServiceResource";

        checkClientHelloWorld(clientUrlStr, "ServiceInjectionClassLevelServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionMemberLevelServlet");

    }

    private void checkClientHelloWorld(String clientUrlStr, String servletName) throws Exception {
        URL url = new URL(clientUrlStr + "/" + servletName + "?target=World");
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertTrue("The Web service can not be invoked successfully from " + clientUrlStr + "/" + servletName, line.contains("Hello World"));
    }

}
