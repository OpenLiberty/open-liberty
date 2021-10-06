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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;
import com.ibm.ws.jaxws.fat.util.TestUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class WsBndServiceRefOverrideTest_Lite {

    public static final int CONN_TIMEOUT = 5;

    @Server("WsBndServiceRefOverrideTestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ExplodedShrinkHelper.explodedDropinApp(server, "wsBndServiceRefOverride", "com.ibm.ws.test.overriddenuri.client",
                                               "com.ibm.ws.test.overriddenuri.client.servlet",
                                               "com.ibm.ws.test.overriddenuri.server");
    }

    @Before
    public void setup() throws Exception {
        cleanupTestedBindingFile();
    }

    @After
    public void tearDown() throws Exception {
        if (server == null) {
            return;
        }

        if (server.isStarted()) {
            server.stopServer();
        }

        cleanupTestedBindingFile();
    }

    private void cleanupTestedBindingFile() throws Exception {
        server.deleteFileFromLibertyServerRoot("dropins/wsBndServiceRefOverride.war/WEB-INF/ibm-ws-bnd.xml");
        server.deleteFileFromLibertyServerRoot("dropins/wsBndServiceRefOverride.war/WEB-INF/ibm-webservicesclient-bnd.xmi");
    }

    @Test
    public void testFaultEndpointUri() throws Exception {
        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wsBndServiceRefOverride");
        String result = getServletResponse(getServletAddr());

        assertFalse("The service should not be accessed.", "Hello".equals(result));
    }

    @Test
    public void testXmlValidEndpointUri() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "WsBndServiceRefOverrideTest", "ibm-ws-bnd_testXmlValidEndpointUri.xml",
                                      "dropins/wsBndServiceRefOverride.war/WEB-INF/", "ibm-ws-bnd.xml");
        TestUtils.replaceServerFileString(server, "dropins/wsBndServiceRefOverride.war/WEB-INF/ibm-ws-bnd.xml", "#ENDPOINT_ADDRESS#", getDefaultEndpointAddr());

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wsBndServiceRefOverride");
        String result = getServletResponse(getServletAddr());
        assertTrue("The returned result is not expected: " + getDefaultEndpointAddr() + "," + result, "Hello".equals(result));
    }

    /**
     * Test the invalid address defined in port element, to override the valid address in serviceRef.
     *
     * @throws Exception
     */
    @Test
    public void testXmlOverrideValidServiceRefWithInvalidPortAddress() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "WsBndServiceRefOverrideTest", "ibm-ws-bnd_testXmlOverrideValidServiceRefWithInvalidPortAddress.xml",
                                      "dropins/wsBndServiceRefOverride.war/WEB-INF/", "ibm-ws-bnd.xml");
        TestUtils.replaceServerFileString(server, "dropins/wsBndServiceRefOverride.war/WEB-INF/ibm-ws-bnd.xml", "#ENDPOINT_ADDRESS#", getDefaultEndpointAddr());

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wsBndServiceRefOverride");
        String result = getServletResponse(getServletAddr());
        assertFalse("The returned result is not expected: " + getDefaultEndpointAddr() + "," + result, "Hello".equals(result));
    }

    /**
     * Test the valid address in serviceRef, and define port under serviceRef.
     *
     * @throws Exception
     */
    @Test
    public void testXmlValidServiceRefAddressWithoutPort() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "WsBndServiceRefOverrideTest", "ibm-ws-bnd_testXmlValidServiceRefAddressWithoutPort.xml",
                                      "dropins/wsBndServiceRefOverride.war/WEB-INF/", "ibm-ws-bnd.xml");
        TestUtils.replaceServerFileString(server, "dropins/wsBndServiceRefOverride.war/WEB-INF/ibm-ws-bnd.xml", "#ENDPOINT_ADDRESS#", getDefaultEndpointAddr());

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wsBndServiceRefOverride");
        String result = getServletResponse(getServletAddr());
        assertTrue("The returned result is not expected: " + getDefaultEndpointAddr() + "," + result, "Hello".equals(result));
    }

    /**
     * Test the xml without defining the overridden endpoint address, should be failed.
     *
     * @throws Exception
     */
    @Test
    public void testXmlWithoutOverriddenEndpointAddress() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "WsBndServiceRefOverrideTest", "ibm-ws-bnd_testXmlWithoutOverriddenEndpointAddress.xml",
                                      "dropins/wsBndServiceRefOverride.war/WEB-INF/", "ibm-ws-bnd.xml");
        TestUtils.replaceServerFileString(server, "dropins/wsBndServiceRefOverride.war/WEB-INF/ibm-ws-bnd.xml", "#ENDPOINT_ADDRESS#", getDefaultEndpointAddr());

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wsBndServiceRefOverride");
        String result = getServletResponse(getServletAddr());
        assertFalse("The returned result is not expected: " + getDefaultEndpointAddr() + "," + result, "Hello".equals(result));
    }

    /**
     * Test the wsdl location override in service-ref, and no address defined in port element.
     *
     * @throws Exception
     */
    @Test
    public void testWsdlLocationOverride() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "WsBndServiceRefOverrideTest", "ibm-ws-bnd_testWsdlLocationOverride.xml",
                                      "dropins/wsBndServiceRefOverride.war/WEB-INF/", "ibm-ws-bnd.xml");
        String wsdlAddr = getDefaultEndpointAddr() + "?wsdl";
        TestUtils.replaceServerFileString(server, "dropins/wsBndServiceRefOverride.war/WEB-INF/ibm-ws-bnd.xml", "#WSDL_LOCATION#", wsdlAddr);

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wsBndServiceRefOverride");
        String result = getServletResponse(getServletAddr());
        assertTrue("WSDL Location Override is not working, and the result is not expected: " + result, "Hello".equals(result));
    }

    /**
     * Test the LoggingInOutInterceptor Prop defined in service-ref
     *
     * @throws Exception
     *
     *                       LoggingInOutInterceptors are replaced by LoggingFeature for jaxws-2.3 and xmlWS-3.0. This test will be skipped
     */
    @Test
    @SkipForRepeat({ "jaxws-2.3", JakartaEE9Action.ID })
    public void testLoggingInOutInterceptorProp() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "WsBndServiceRefOverrideTest", "ibm-ws-bnd_testLoggingInOutInterceptorProp.xml",
                                      "dropins/wsBndServiceRefOverride.war/WEB-INF/", "ibm-ws-bnd.xml");
        String wsdlAddr = getDefaultEndpointAddr() + "?wsdl";
        TestUtils.replaceServerFileString(server, "dropins/wsBndServiceRefOverride.war/WEB-INF/ibm-ws-bnd.xml", "#WSDL_LOCATION#", wsdlAddr);
        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wsBndServiceRefOverride");
        getServletResponse(getServletAddr());
        List<String> dumpInMessages = server.findStringsInLogs("Inbound Message");
        List<String> dumpOutMessages = server.findStringsInLogs("Outbound Message");
        assertTrue("Can't find inBoundMessage, the return inboundmessage is: " + dumpInMessages.toString(), !dumpInMessages.isEmpty());
        assertTrue("Can't find outBoundMessage, the return outboundmessage is: " + dumpOutMessages.toString(), !dumpOutMessages.isEmpty());

    }

    protected String getServletResponse(String servletUrl) throws Exception {
        URL url = new URL(servletUrl);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String result = br.readLine();
        return result;
    }

    protected String getDefaultEndpointAddr() {
        return new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/wsBndServiceRefOverride/SimpleEchoService").toString();
    }

    protected String getServletAddr() {
        return new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/wsBndServiceRefOverride/TestOverriddenEndpointUriServlet").toString();
    }

}
