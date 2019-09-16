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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws.jmx.test.fat.util.ClientConnector;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class WebServiceMonitorTest {

    private static final String REQUEST_STR = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:jax=\"http://monitor.jaxws.ws.ibm.com/\">" +
                                              "<soapenv:Header/>" +
                                              "<soapenv:Body>" +
                                              "<jax:dollarToRupees>" +
                                              "<arg0>4</arg0>" +
                                              "</jax:dollarToRupees>" +
                                              "</soapenv:Body>" +
                                              "</soapenv:Envelope>";

    private final static int REQUEST_TIMEOUT = 15;

    @Server("WebServiceMonitorTestServer")
    public static LibertyServer server;

    private MBeanServerConnection mbsc;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "testWebServiceMonitor", "com.ibm.ws.jaxws.monitor");

        String thisMethod = "setUp()";
        // Ignore unsigned certificate: Copied from com.ibm.ws.channel.ssl.client.test.SimpleHttpsClientTest
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        } };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier hv = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostName, SSLSession session) {
                System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);

    }

    @Before
    public void startServer() throws Exception {

        server.startServer("WebServiceMonitorTest.log");
        server.waitForStringInLog("CWWKZ0001I.*testWebServiceMonitor");

        //Check to make sure jks has been created for restConnector
        server.waitForStringInLog("CWPKI0803A.*");
        //Check to see if Rest service is up
        server.waitForStringInLog("CWWKX0103I.*");

        ClientConnector cc = new ClientConnector(server.getServerRoot(), server.getHostname(), server.getHttpDefaultSecurePort());
        mbsc = cc.getMBeanServer();
        if (mbsc == null) {
            fail("The MBeanServer connection is null! The test fails.");
        }
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testMonitorEnabled() throws Exception {
        String serviceName = "ConverterService";
        accessServiceWSDL(serviceName);
        String urlAddress = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/testWebServiceMonitor/" + serviceName;

        // The first time to invoke web service
        postSOAPMessage(urlAddress, REQUEST_STR);
        // The second time to invoke web service
        String respStr = postSOAPMessage(urlAddress, REQUEST_STR);
        assertTrue("Web service response is not correct.", respStr.contains("<return>162.32</return>"));

        int perfromanceCount = getPerformanceCountAndVerifyInvocationTimes(2);
        assertTrue("The monitor feature is not enabled in Liberty.", (perfromanceCount > 0));
    }

    //Need to restart the server after removing the monitor feature, which causes by a liberty runtime issue
    public void testMonitorDisabledDynamiclly() throws Exception {
        String serviceName = "ConverterService";
        accessServiceWSDL(serviceName);
        String urlAddress = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/testWebServiceMonitor/" + serviceName;
        String respStr = postSOAPMessage(urlAddress, REQUEST_STR);
        assertTrue("Web service response is not correct.", respStr.contains("<return>162.32</return>"));

        int perfromanceCount = getPerformanceCountAndVerifyInvocationTimes(1);
        assertTrue("The monitor feature is not enabled in Liberty.", (perfromanceCount > 0));

        // Disable monitor feature dynamiclly.
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles + "WebServiceMonitorTest/clearCache/server.xml"));
        server.waitForStringInLog("CWWKZ0009I.*testWebServiceMonitor");
        server.waitForStringInLog("CWWKZ0003I.*testWebServiceMonitor");
        server.waitForStringInLog("CWWKF0008I.*Feature update completed");

        respStr = postSOAPMessage(urlAddress, REQUEST_STR);
        assertTrue("Web service response is not correct.", respStr.contains("<return>162.32</return>"));

        int disableCount = getPerformanceCountAndVerifyInvocationTimes(0);
        assertTrue("The monitor feature is still enabled in Liberty.", (disableCount == 0));
    }

    private int getPerformanceCountAndVerifyInvocationTimes(int excpectedCount) throws Exception {
        Set<ObjectInstance> mbeans = mbsc.queryMBeans(new ObjectName("org.apache.cxf:*"), null);
        int perfromanceCount = 0;
        for (ObjectInstance mbean : mbeans) {
            ObjectName objectName = mbean.getObjectName();
            if (objectName.toString().contains("Performance")) {
                MBeanInfo mBeanInfo = mbsc.getMBeanInfo(objectName);
                MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
                for (MBeanAttributeInfo mBeanAttr : attributes) {
                    String attrName = mBeanAttr.getName();
                    if (attrName.equals("NumInvocations")) {
                        Object attr = mbsc.getAttribute(objectName, attrName);
                        long invoNum = Long.parseLong(attr.toString());
                        assertEquals("Invocations num is " + invoNum + " not " + excpectedCount, excpectedCount, invoNum);
                    }
                }
                perfromanceCount++;
            }
        }
        return perfromanceCount;
    }

    public void accessServiceWSDL(String service) throws ProtocolException, IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/testWebServiceMonitor/" + service + "?wsdl");
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        br.readLine();
        br.close();
    }

    public String postSOAPMessage(String urlAddress, String content) throws IOException {
        URL url = new URL(urlAddress);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        httpConn.setInstanceFollowRedirects(false);
        httpConn.setRequestMethod("POST");
        httpConn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
        httpConn.setRequestProperty("charset", "utf-8");
        httpConn.setRequestProperty("Content-Length", "" + Integer.toString(content.getBytes().length));
        httpConn.setUseCaches(false);
        httpConn.setConnectTimeout(REQUEST_TIMEOUT * 1000);

        DataOutputStream dataOutputStream = new DataOutputStream(httpConn.getOutputStream());
        dataOutputStream.writeBytes(content);
        dataOutputStream.flush();
        dataOutputStream.close();

        InputStream is = httpConn.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        bufferedReader.close();

        httpConn.disconnect();
        return stringBuilder.toString();
    }
}
