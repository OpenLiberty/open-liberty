/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/*
 * This test is responsible for testing whether or not HTTPConduit configuration set in the ibm-ws-bnd.xml file
 * is picked up and applied to CXF via our integration layer. The test application requires access to CXF internals
 * so a jaxwsTest-2.3 feature is added to the Liberty image in order to expose those APIs.
 *
 * Lite mode - Runs matching serviceRef, port, and serviceRef + port configurations
 *
 * @TJJ: If changes don't resolve the timeout issue then setting marks at begining of test method
 * and using server.findStringInLogUsingMark() should work but it will impact performance by about 100%
 *
 */
@RunWith(FATRunner.class)
public class HttpConduitPropertiesTest {

    private static final int CONN_TIMEOUT = 10;

    @Server("HttpConduitPropertiesTestServer")
    public static LibertyServer server;

    // WAIT TIME to be used to wait for copying ibm-ws-bnd.xml to complete before invoking Web Service
    private static int BND_FILE_COPY_WAITTIME = 1000;

    private static int CONFIG_REFRESH_COUNT = 0;

    private static String defaultSimpleEchoServiceEndpointAddr;
    private static String defaultSimpleEchoServiceEndpointAddr2;
    private static String defaultHelloServiceEndpointAddr;
    private static String testServletURL;
    private static String testServletURL2;
    private static String testServletURLForHelloService;
    private static String receiveTimeoutTestServletURL;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultApp(server, "httpConduitProperties", "com.ibm.jaxws.properties.echo",
                                "com.ibm.jaxws.properties.echo.client",
                                "com.ibm.jaxws.properties.hello",
                                "com.ibm.jaxws.properties.hello.client",
                                "com.ibm.jaxws.properties.interceptor",
                                "com.ibm.jaxws.properties.servlet");

        // copy httpConduitProperties and make httpConduitProperties2
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot() + "/apps", "httpConduitProperties2.war",
                                               server.getServerRoot() + "/apps/httpConduitProperties.war");

        server.copyFileToLibertyInstallRoot("lib/features", "HttpConduitPropertiesTest/jaxwsTest-2.2.mf");
        server.copyFileToLibertyInstallRoot("lib/features", "HttpConduitPropertiesTest/jaxwsTest-2.3.mf");
        server.copyFileToLibertyInstallRoot("lib/features", "HttpConduitPropertiesTest/xmlwsTest-3.0.mf");
        server.copyFileToLibertyInstallRoot("lib/features", "HttpConduitPropertiesTest/xmlwsTest-4.0.mf");

        testServletURL = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/httpConduitProperties/TestServlet?target=SimpleEchoService").toString();
        testServletURL2 = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/httpConduitProperties2/TestServlet?target=SimpleEchoService").toString();
        testServletURLForHelloService = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/httpConduitProperties/TestServlet?target=HelloService").toString();
        receiveTimeoutTestServletURL = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/httpConduitProperties/ReceiveTimeoutTestServlet").toString();

        // Start server once, rather than force server start stop on every test execution
        server.startServer();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        server.deleteFileFromLibertyInstallRoot("lib/features/jaxwsTest-2.2.mf");
        server.deleteFileFromLibertyInstallRoot("lib/features/jaxwsTest-2.3.mf");
        server.deleteFileFromLibertyInstallRoot("lib/features/xmlwsTest-3.0.mf");
        server.deleteFileFromLibertyInstallRoot("lib/features/xmlwsTest-4.0.mf");

        if (server.isStarted())
            server.stopServer();
    }

    @Test
    public void testPropertiesForMatchedServiceRef() throws Exception {

        waitForServerConfigChange("testPropertiesForMatchedServiceRef.xml");

        Map<String, String> propertyMap = getServletResponse(testServletURL);

        // Wait for MBean to start after invoking servlet.
        server.waitForStringInLog("registered org.apache.cxf:bus.id=httpConduitProperties");

        String clientConnectionTimeOut = propertyMap.get("client.ConnectionTimeout");
        String clientChunkingThreshold = propertyMap.get("client.ChunkingThreshold");

        String authorizationUserName = propertyMap.get("authorization.UserName");
        String authorizationAuthorization = propertyMap.get("authorization.Authorization");

        String proxyAuthorizationUserName = propertyMap.get("proxyAuthorization.UserName");
        String proxyAuthorizationAuthorization = propertyMap.get("proxyAuthorization.Authorization");

        assertTrue("The expected client.ConnectionTimeOut should be '1739', but the actual is '" + clientConnectionTimeOut + "'", "1739".equals(clientConnectionTimeOut));
        assertTrue("The expected client.ChunkingThreshold should be '2317', but the actual is '" + clientChunkingThreshold + "'", "2317".equals(clientChunkingThreshold));

        assertTrue("The expected authorization.UserName should be 'tester', but the actual is '" + authorizationUserName + "'", "tester".equals(authorizationUserName));
        assertTrue("The expected authorization.Authorization should be 'ABCD', but the actual is '" + authorizationAuthorization + "'", "ABCD".equals(authorizationAuthorization));

        assertTrue("The expected proxyAuthorization.UserName should be 'proxyTestUser', but the actual is '" + proxyAuthorizationUserName + "'",
                   "proxyTestUser".equals(proxyAuthorizationUserName));
        assertTrue("The expected proxyAuthorization.Authorization should be 'ProxyABCD', but the actual is '" + proxyAuthorizationAuthorization + "'",
                   "ProxyABCD".equals(proxyAuthorizationAuthorization));
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPropertiesForNoMatchedServiceRef() throws Exception {

        waitForServerConfigChange("testPropertiesForNoMatchedServiceRef.xml");

        Map<String, String> propertyMap = getServletResponse(testServletURL);

        // Wait for MBean to start after invoking servlet.
        server.waitForStringInLog("registered org.apache.cxf:bus.id=httpConduitProperties");

        String clientConnectionTimeOut = propertyMap.get("client.ConnectionTimeout");
        String clientChunkingThreshold = propertyMap.get("client.ChunkingThreshold");

        String authorizationUserName = propertyMap.get("authorization.UserName");
        String authorizationAuthorization = propertyMap.get("authorization.Authorization");

        String proxyAuthorizationUserName = propertyMap.get("proxyAuthorization.UserName");
        String proxyAuthorizationAuthorization = propertyMap.get("proxyAuthorization.Authorization");

        assertFalse("The expected client.ConnectionTimeOut should not be '1739', but the actual is '" + clientConnectionTimeOut + "'", "1739".equals(clientConnectionTimeOut));
        assertFalse("The expected client.ChunkingThreshold should not be '2317', but the actual is '" + clientChunkingThreshold + "'", "2317".equals(clientChunkingThreshold));

        assertFalse("The expected authorization.UserName should be 'tester', but the actual is '" + authorizationUserName + "'", "tester".equals(authorizationUserName));
        assertFalse("The expected authorization.Authorization should be 'ABCD', but the actual is '" + authorizationAuthorization + "'", "ABCD".equals(authorizationAuthorization));

        assertFalse("The expected proxyAuthorization.UserName should be 'proxyTestUser', but the actual is '" + proxyAuthorizationUserName + "'",
                    "proxyTestUser".equals(proxyAuthorizationUserName));
        assertFalse("The expected proxyAuthorization.Authorization should be 'ProxyABCD', but the actual is '" + proxyAuthorizationAuthorization + "'",
                    "ProxyABCD".equals(proxyAuthorizationAuthorization));
    }

    @Test
    public void testPropertiesForMatchedPort() throws Exception {

        waitForServerConfigChange("testPropertiesForMatchedPort.xml");

        Map<String, String> propertyMap = getServletResponse(testServletURL);

        // Wait for MBean to start after invoking servlet.
        server.waitForStringInLog("registered org.apache.cxf:bus.id=httpConduitProperties");

        String clientConnectionTimeOut = propertyMap.get("client.ConnectionTimeout");
        String clientChunkingThreshold = propertyMap.get("client.ChunkingThreshold");

        String authorizationUserName = propertyMap.get("authorization.UserName");
        String authorizationAuthorization = propertyMap.get("authorization.Authorization");

        String proxyAuthorizationUserName = propertyMap.get("proxyAuthorization.UserName");
        String proxyAuthorizationAuthorization = propertyMap.get("proxyAuthorization.Authorization");

        assertTrue("The expected client.ConnectionTimeOut should be '1739', but the actual is '" + clientConnectionTimeOut + "'", "1739".equals(clientConnectionTimeOut));
        assertTrue("The expected client.ChunkingThreshold should be '2317', but the actual is '" + clientChunkingThreshold + "'", "2317".equals(clientChunkingThreshold));

        assertTrue("The expected authorization.UserName should be 'tester', but the actual is '" + authorizationUserName + "'", "tester".equals(authorizationUserName));
        assertTrue("The expected authorization.Authorization should be 'ABCD', but the actual is '" + authorizationAuthorization + "'", "ABCD".equals(authorizationAuthorization));

        assertTrue("The expected proxyAuthorization.UserName should be 'proxyTestUser', but the actual is '" + proxyAuthorizationUserName + "'",
                   "proxyTestUser".equals(proxyAuthorizationUserName));
        assertTrue("The expected proxyAuthorization.Authorization should be 'ProxyABCD', but the actual is '" + proxyAuthorizationAuthorization + "'",
                   "ProxyABCD".equals(proxyAuthorizationAuthorization));
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPropertiesForNoMatchedPort() throws Exception {

        waitForServerConfigChange("testPropertiesForNoMatchedPort.xml");

        Map<String, String> propertyMap = getServletResponse(testServletURL);

        // Wait for MBean to start after invoking servlet.
        server.waitForStringInLog("registered org.apache.cxf:bus.id=httpConduitProperties");

        String clientConnectionTimeOut = propertyMap.get("client.ConnectionTimeout");
        String clientChunkingThreshold = propertyMap.get("client.ChunkingThreshold");

        String authorizationUserName = propertyMap.get("authorization.UserName");
        String authorizationAuthorization = propertyMap.get("authorization.Authorization");

        String proxyAuthorizationUserName = propertyMap.get("proxyAuthorization.UserName");
        String proxyAuthorizationAuthorization = propertyMap.get("proxyAuthorization.Authorization");

        assertFalse("The expected client.ConnectionTimeOut should be '1739', but the actual is '" + clientConnectionTimeOut + "'", "1739".equals(clientConnectionTimeOut));
        assertFalse("The expected client.ChunkingThreshold should be '2317', but the actual is '" + clientChunkingThreshold + "'", "2317".equals(clientChunkingThreshold));

        assertFalse("The expected authorization.UserName should be 'tester', but the actual is '" + authorizationUserName + "'", "tester".equals(authorizationUserName));
        assertFalse("The expected authorization.Authorization should be 'ABCD', but the actual is '" + authorizationAuthorization + "'", "ABCD".equals(authorizationAuthorization));

        assertFalse("The expected proxyAuthorization.UserName should be 'proxyTestUser', but the actual is '" + proxyAuthorizationUserName + "'",
                    "proxyTestUser".equals(proxyAuthorizationUserName));
        assertFalse("The expected proxyAuthorization.Authorization should be 'ProxyABCD', but the actual is '" + proxyAuthorizationAuthorization + "'",
                    "ProxyABCD".equals(proxyAuthorizationAuthorization));
    }

    @Test
    public void testPropertiesForMatchedServiceRefAndPort() throws Exception {

        waitForServerConfigChange("testPropertiesForMatchedServiceRefAndPort.xml");

        Map<String, String> propertyMap = getServletResponse(testServletURL);

        // Wait for MBean to start after invoking servlet.
        server.waitForStringInLog("registered org.apache.cxf:bus.id=httpConduitProperties");

        String clientConnectionTimeOut = propertyMap.get("client.ConnectionTimeout");
        String clientChunkingThreshold = propertyMap.get("client.ChunkingThreshold");

        String authorizationUserName = propertyMap.get("authorization.UserName");
        String authorizationAuthorization = propertyMap.get("authorization.Authorization");

        String proxyAuthorizationUserName = propertyMap.get("proxyAuthorization.UserName");
        String proxyAuthorizationAuthorization = propertyMap.get("proxyAuthorization.Authorization");

        assertTrue("The expected client.ConnectionTimeOut should be '1189', but the actual is '" + clientConnectionTimeOut + "'", "1189".equals(clientConnectionTimeOut));
        assertTrue("The expected client.ChunkingThreshold should be '2317', but the actual is '" + clientChunkingThreshold + "'", "2317".equals(clientChunkingThreshold));

        assertTrue("The expected authorization.UserName should be 'portTester', but the actual is '" + authorizationUserName + "'", "portTester".equals(authorizationUserName));
        assertTrue("The expected authorization.Authorization should be 'portAbc', but the actual is '" + authorizationAuthorization + "'",
                   "portAbc".equals(authorizationAuthorization));

        assertTrue("The expected proxyAuthorization.UserName should be 'portProxyTestUser', but the actual is '" + proxyAuthorizationUserName + "'",
                   "portProxyTestUser".equals(proxyAuthorizationUserName));
        assertTrue("The expected proxyAuthorization.Authorization should be 'ProxyPortAbc', but the actual is '" + proxyAuthorizationAuthorization + "'",
                   "ProxyPortAbc".equals(proxyAuthorizationAuthorization));
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPropertiesForMultipleSampleApp() throws Exception {

        waitForServerConfigChange("testPropertiesForMultipleSampleApp.xml");
        // Checks for httpConduitProperties2 restart
        server.waitForStringInLog("CWWKZ0001I.*httpConduitProperties2");

        Map<String, String> propertyMap1 = getServletResponse(testServletURL);

        Map<String, String> propertyMap2 = getServletResponse(testServletURL2);

        // Wait for MBean to start after invoking servlet.
        server.waitForStringInLog("registered org.apache.cxf:bus.id=httpConduitProperties");

        String clientConnectionTimeOut1 = propertyMap1.get("client.ConnectionTimeout");
        String clientChunkingThreshold1 = propertyMap1.get("client.ChunkingThreshold");

        String authorizationUserName1 = propertyMap1.get("authorization.UserName");
        String authorizationAuthorization1 = propertyMap1.get("authorization.Authorization");

        String proxyAuthorizationUserName1 = propertyMap1.get("proxyAuthorization.UserName");
        String proxyAuthorizationAuthorization1 = propertyMap1.get("proxyAuthorization.Authorization");

        String clientConnectionTimeOut2 = propertyMap2.get("client.ConnectionTimeout");
        String clientChunkingThreshold2 = propertyMap2.get("client.ChunkingThreshold");

        String authorizationUserName2 = propertyMap2.get("authorization.UserName");
        String authorizationAuthorization2 = propertyMap2.get("authorization.Authorization");

        String proxyAuthorizationUserName2 = propertyMap2.get("proxyAuthorization.UserName");
        String proxyAuthorizationAuthorization2 = propertyMap2.get("proxyAuthorization.Authorization");

        assertTrue("The expected client.ConnectionTimeOut should be '1739' in httpConduitProperties, but the actual is '" + clientConnectionTimeOut1 + "'",
                   "1739".equals(clientConnectionTimeOut1));
        assertTrue("The expected client.ChunkingThreshold should be '2317' in httpConduitProperties, but the actual is '" + clientChunkingThreshold1 + "'",
                   "2317".equals(clientChunkingThreshold1));

        assertTrue("The expected authorization.UserName should be 'tester' in httpConduitProperties, but the actual is '" + authorizationUserName1 + "'",
                   "tester".equals(authorizationUserName1));
        assertTrue("The expected authorization.Authorization should be 'ABCD' in httpConduitProperties, but the actual is '" + authorizationAuthorization1 + "'",
                   "ABCD".equals(authorizationAuthorization1));

        assertTrue("The expected proxyAuthorization.UserName should be 'proxyTestUser' in httpConduitProperties, but the actual is '" + proxyAuthorizationUserName1 + "'",
                   "proxyTestUser".equals(proxyAuthorizationUserName1));
        assertTrue("The expected proxyAuthorization.Authorization should be 'ProxyABCD' in httpConduitProperties, but the actual is '" + proxyAuthorizationAuthorization1 + "'",
                   "ProxyABCD".equals(proxyAuthorizationAuthorization1));

        assertTrue("The expected client.ConnectionTimeOut should be '1189' in httpConduitProperties2, but the actual is '" + clientConnectionTimeOut2 + "'",
                   "1189".equals(clientConnectionTimeOut2));
        assertTrue("The expected client.ChunkingThreshold should be '3546' in httpConduitProperties2, but the actual is '" + clientChunkingThreshold2 + "'",
                   "3546".equals(clientChunkingThreshold2));

        assertTrue("The expected authorization.UserName should be 'portTester' in httpConduitProperties2, but the actual is '" + authorizationUserName2 + "'",
                   "portTester".equals(authorizationUserName2));
        assertTrue("The expected authorization.Authorization should be 'portAbc' in httpConduitProperties2, but the actual is '" + authorizationAuthorization2 + "'",
                   "portAbc".equals(authorizationAuthorization2));

        assertTrue("The expected proxyAuthorization.UserName should be 'portProxyTestUser' in httpConduitProperties2, but the actual is '" + proxyAuthorizationUserName2 + "'",
                   "portProxyTestUser".equals(proxyAuthorizationUserName2));
        assertTrue("The expected proxyAuthorization.Authorization should be 'ProxyPortAbc' in httpConduitProperties2, but the actual is '" + proxyAuthorizationAuthorization2 + "'",
                   "ProxyPortAbc".equals(proxyAuthorizationAuthorization2));

    }

    @Mode(TestMode.FULL)
    @Test
    public void testPropertiesForTwoServiceRefInOneApp() throws Exception {

        waitForServerConfigChange("testPropertiesForTwoServiceRefInOneApp.xml");

        Map<String, String> echoServiceProperties = getServletResponse(testServletURL);
        Map<String, String> helloServiceProperties = getServletResponse(testServletURLForHelloService);

        // Wait for MBean to start after invoking servlet.
        server.waitForStringInLog("registered org.apache.cxf:bus.id=httpConduitProperties");

        String clientConnectionTimeOut = echoServiceProperties.get("client.ConnectionTimeout");
        String clientChunkingThreshold = echoServiceProperties.get("client.ChunkingThreshold");

        String authorizationUserName = echoServiceProperties.get("authorization.UserName");
        String authorizationAuthorization = echoServiceProperties.get("authorization.Authorization");

        String proxyAuthorizationUserName = echoServiceProperties.get("proxyAuthorization.UserName");
        String proxyAuthorizationAuthorization = echoServiceProperties.get("proxyAuthorization.Authorization");

        String helloServiceClientConnectionTimeOut = helloServiceProperties.get("client.ConnectionTimeout");
        String helloServiceClientChunkingThreshold = helloServiceProperties.get("client.ChunkingThreshold");

        assertTrue("The expected client.ConnectionTimeOut should be '1739', but the actual is '" + clientConnectionTimeOut + "'", "1739".equals(clientConnectionTimeOut));
        assertTrue("The expected client.ChunkingThreshold should be '2317', but the actual is '" + clientChunkingThreshold + "'", "2317".equals(clientChunkingThreshold));

        assertTrue("The expected authorization.UserName should be 'tester', but the actual is '" + authorizationUserName + "'", "tester".equals(authorizationUserName));
        assertTrue("The expected authorization.Authorization should be 'ABCD', but the actual is '" + authorizationAuthorization + "'", "ABCD".equals(authorizationAuthorization));

        assertTrue("The expected proxyAuthorization.UserName should be 'proxyTestUser', but the actual is '" + proxyAuthorizationUserName + "'",
                   "proxyTestUser".equals(proxyAuthorizationUserName));
        assertTrue("The expected proxyAuthorization.Authorization should be 'ProxyABCD', but the actual is '" + proxyAuthorizationAuthorization + "'",
                   "ProxyABCD".equals(proxyAuthorizationAuthorization));

        assertTrue("The expected client.ConnectionTimeOut should be '5432', but the actual is '" + helloServiceClientConnectionTimeOut + "'",
                   "5432".equals(helloServiceClientConnectionTimeOut));
        assertTrue("The expected client.ChunkingThreshold should be '6547', but the actual is '" + helloServiceClientChunkingThreshold + "'",
                   "6547".equals(helloServiceClientChunkingThreshold));
    }

    @Test
    public void testReceiveTimeout() throws Exception {

        waitForServerConfigChange("testReceiveTimeout.xml");

        String msg = getServletResponseMessage(receiveTimeoutTestServletURL);
        // Wait for MBean to start after invoking servlet.
        server.waitForStringInLog("registered org.apache.cxf:bus.id=httpConduitProperties");
        assertTrue("The Read time out exception should be thrown, but the actual is '" + msg + "'",
                   msg.contains("SocketTimeoutException"));
    }

    private Map<String, String> getServletResponse(String servletUrl) throws Exception {
        URL url = new URL(servletUrl);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String result = br.readLine();
        String[] keyValues = result.split(",");

        Map<String, String> propertyMap = new HashMap<String, String>();
        for (String keyValue : keyValues) {
            String[] param = keyValue.trim().split("=");
            if (param.length == 2) {
                propertyMap.put(param[0].trim(), param[1].trim());
            }
        }

        return propertyMap;
    }

    private String getServletResponseMessage(String servletUrl) throws Exception {
        URL url = new URL(servletUrl);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String result = br.readLine();

        if (result == null) // Prevent null return
            result = "";

        return result;
    }

    // Replace server configuration and wait for update to finish
    private void waitForServerConfigChange(String serverXMLName) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("HttpConduitPropertiesTest/configFiles/" + serverXMLName);
        server.waitForConfigUpdateInLogUsingMark(null, false, new String[0]);
    }
}
