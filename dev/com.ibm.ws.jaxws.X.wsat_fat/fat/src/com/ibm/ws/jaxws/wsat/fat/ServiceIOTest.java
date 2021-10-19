/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.wsat.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;


import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class ServiceIOTest {
    @Server("clientout_server")
    public static LibertyServer server;

    private final static int REQUEST_TIMEOUT = 60;
    

    public static String serviceApp = "simpleTestService";

    @BeforeClass
    public static void setup() throws Exception {
    
        WebArchive simpleTestService = ShrinkWrap.create(WebArchive.class, serviceApp + ".war")
                        .addPackages(false, "com.ibm.ws.jaxws.wsat.testservice")
                        .addPackages(false, "com.ibm.ws.jaxws.wsat.testservice.client")
                        .addPackages(false, "com.ibm.ws.jaxws.wsat.testservice.impl");

        ShrinkHelper.addDirectory(simpleTestService, "test-applications/" + serviceApp + "/resources");
        
        ShrinkHelper.exportDropinAppToServer(server, simpleTestService);
    
    }
    
    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(); // trust stop server to ensure server has
                                 // stopped
        }
    }

    @Test
    public void testClientOutBoundException() throws Exception {
        server.startServer();
        StringBuilder sBuilder = new StringBuilder("http://")
                        .append(server.getHostname()).append(":")
                        .append(server.getHttpDefaultPort())
                        .append("/simpleTestService/SimpleServiceServlet")
                        .append("?port=").append(server.getHttpDefaultPort())
                        .append("&hostname=").append(server.getHostname())
                        .append("&service=").append("SimpleImplService")
                        .append("&war=simpleTestService");
        server.waitForStringInLog("CWWKZ0001I.*simpleTestService");
        String urlStr = sBuilder.toString();
        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr),
                                                            HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertNotNull(line);
        assertTrue(line.equals("WS-AT Feature is not installed"));
    }

    @Test
    public void testServerInBoundException() throws Exception {
        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*simpleTestService");
        final String serviceName = "SimpleImplService";
        final String NAMESPACE = "http://impl.testservice.wsat.jaxws.ws.ibm.com/";

        QName qname = new QName(NAMESPACE, serviceName);
        Service service = Service.create(qname);

        QName portType = new QName(NAMESPACE, "SimplePort");
        service.addPort(portType, SOAPBinding.SOAP11HTTP_BINDING,
                        new StringBuilder("http://").append(server.getHostname())
                                        .append(":").append(server.getHttpDefaultPort())
                                        .append("/").append("simpleTestService").append("/")
                                        .append(serviceName).toString());
        Dispatch<SOAPMessage> dispatch = service.createDispatch(portType,
                                                                SOAPMessage.class, Service.Mode.MESSAGE);
        try {
            SOAPMessage message = MessageFactory.newInstance(
                                                             SOAPConstants.SOAP_1_1_PROTOCOL).createMessage();

            SOAPHeader header = message.getSOAPPart().getEnvelope().getHeader();
            SOAPElement ele = header.addChildElement(new QName(
                            "http://docs.oasis-open.org/ws-tx/wscoor/2006/06",
                            "CoordinationType", "wscoor"));
            ele.addAttribute(new QName(
                            "http://schemas.xmlsoap.org/soap/envelope/",
                            "mustUnderstand", "SOAP-ENV"), "1");
            ele.setTextContent("http://docs.oasis-open.org/ws-tx/wsat/2006/06");
            SOAPBody b = message.getSOAPBody();
            SOAPElement bele = b.addChildElement(new QName(
                            "http://testservice.wsat.jaxws.ws.ibm.com/", "echo", "a"));
            bele.addChildElement(new QName("arg0")).setTextContent("Hello");
            dispatch.invoke(message);
        } catch (SOAPFaultException e) {
            assertNotNull(e.getMessage());
            System.out.println("The expect exception is: " + e.getMessage());
            assertTrue(e.getMessage().contains("WS-AT Feature is not installed"));
            return;
        }
        fail("This webservice should throw exceptions other than pass");
    }

    @Test
    public void testServerOutBoundException() throws Exception {
        server.startServer();
        StringBuilder sBuilder = new StringBuilder("http://")
                        .append(server.getHostname()).append(":")
                        .append(server.getHttpDefaultPort())
                        .append("/simpleTestService/SimpleServiceServlet")
                        .append("?port=").append(server.getHttpDefaultPort())
                        .append("&hostname=").append(server.getHostname())
                        .append("&service=").append("SimpleEcho")
                        .append("&war=simpleTestService");
        server.waitForStringInLog("CWWKZ0001I.*simpleTestService");
        // Adding an additional log check, as Servlet doesn't seem to have deploy by the time 
        // the test is run on certain builds.
        server.waitForStringInLog("SRVE0242I.*simpleTestService");
        
        String urlStr = sBuilder.toString();
        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr),
                                                            HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertNotNull(line);
        assertTrue(line.equals("WS-AT Feature is not installed"));
    }

}
