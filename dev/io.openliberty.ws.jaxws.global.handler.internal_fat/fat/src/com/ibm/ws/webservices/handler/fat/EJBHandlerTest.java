/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.webservices.handler.fat;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class EJBHandlerTest {

    @Server("EJBHandler")
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("EJBHandler");

    private static final String EJB_CLIENT_HANDLER_SOAP_MESSAGE = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ejb=\"http://ejbHandler.jaxws.ws.ibm.com/\"><soapenv:Header/>"
                                                                  + "<soapenv:Body><ejb:echoClient><arg0>client</arg0></ejb:echoClient></soapenv:Body></soapenv:Envelope>";

    private static final String EJB_SERVER_HANDLER_SOAP_MESSAGE = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ejb=\"http://ejbHandler.jaxws.ws.ibm.com/\"><soapenv:Header/>"
                                                                  + "<soapenv:Body><ejb:echoServer><arg0>server</arg0></ejb:echoServer></soapenv:Body></soapenv:Envelope>";

    private static String CLIENT_HANDLER_ENDPOINT_URL;

    private static String SERVER_HANDLER_ENDPOINT_URL;

    private static QName DUMMY_SERVICE_QNAME = new QName("http://", "service");

    private static QName DUMMY_PORT_QNAME = new QName("http://", "port");

    @BeforeClass
    public static void beforeAllTests() throws Exception {
        //server.installUserBundle("TestHandler1_1.0.0.201311011652");
        ShrinkHelper.defaultUserFeatureArchive(server, "userBundle2", "com.ibm.ws.userbundle2.myhandler");
        TestUtils.installUserFeature(server, "TestHandler1Feature1");

        JavaArchive ejbJar = ShrinkHelper.buildJavaArchive("EJBHandler", "com.ibm.ws.jaxws.ejbHandler", "com.ibm.ws.jaxws.ejbHandler.client");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "EJBHandler.ear");
        ear.addAsModule(ejbJar);
        ShrinkHelper.exportDropinAppToServer(server, ear);

        server.startServer();
        Assert.assertNotNull("The application EJBHandler did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*EJBHandler"));
        //EJBHandler does not require app security things, the feature is just added there to make sure, our functions could work
        //with security enabled.
        Assert.assertNotNull("LTPA keys in not created",
                             server.waitForStringInLog("CWWKS4104A.*LTPA"));

        CLIENT_HANDLER_ENDPOINT_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/EJBHandler/EJBHandlerClientBeanService";

        SERVER_HANDLER_ENDPOINT_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/EJBHandler/EJBHandlerServerBeanService";
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
        server.uninstallUserFeature("TestHandler1Feature1");
        server.uninstallUserBundle("userBundle2");
    }

    @Test
    public void testEJBServerHandlerBeanWithGlobalHandlers() throws Exception {

        Service service = Service.create(DUMMY_SERVICE_QNAME);
        service.addPort(DUMMY_PORT_QNAME, SOAPBinding.SOAP11HTTP_BINDING, SERVER_HANDLER_ENDPOINT_URL);
        Dispatch<SOAPMessage> dispatch = service.createDispatch(DUMMY_PORT_QNAME, SOAPMessage.class, Service.Mode.MESSAGE);
        SOAPMessage requestSOAPMessage = createRequestSOAPMessage(EJB_SERVER_HANDLER_SOAP_MESSAGE);

        dispatch.invoke(requestSOAPMessage);

        assertStatesExsited(5000, new String[] {
                                                "handle inbound message in TestHandler2InBundle2",
                                                "handle inbound message in TestHandler1InBundle2",
                                                "com.ibm.ws.jaxws.ejbHandler.TestLogicalHandler:init param \"arg0\" = testInitParam",
                                                "com.ibm.ws.jaxws.ejbHandler.TestLogicalHandler:postConstruct is invoked",
                                                "com.ibm.ws.jaxws.ejbHandler.TestSOAPHandler:postConstruct is invoked",
                                                "com.ibm.ws.jaxws.ejbHandler.EJBHandlerServerBean:LogicalHandlerSayHiBean.sayHi:Hi, ivan",
                                                "com.ibm.ws.jaxws.ejbHandler.EJBHandlerServerBean:SOAPHandlerSayHiBean.sayHi:Hi, ivan",
                                                "com.ibm.ws.jaxws.ejbHandler.EJBHandlerServerBean:EchoBean.echo:ivan",
                                                "com.ibm.ws.jaxws.ejbHandler.TestLogicalHandler:handle outbound message",
                                                "com.ibm.ws.jaxws.ejbHandler.TestLogicalHandler:LogicalHandlerSayHiBean.sayHi:Hi, ivan",
                                                "com.ibm.ws.jaxws.ejbHandler.TestLogicalHandler:EchoBean.echo:ivan",
                                                "com.ibm.ws.jaxws.ejbHandler.TestLogicalHandler:SOAPHandlerSayHiBean.soapHandlerSayHi:Hi, ivan",
                                                "com.ibm.ws.jaxws.ejbHandler.TestSOAPHandler: handle outbound message",
                                                "com.ibm.ws.jaxws.ejbHandler.TestSOAPHandler: sayHi = Hi, ivan",
                                                "com.ibm.ws.jaxws.ejbHandler.TestSOAPHandler: echo = ivan",
                                                "com.ibm.ws.jaxws.ejbHandler.TestSOAPHandler: logicalHandlerSayHi = Hi, ivan",
                                                "com.ibm.ws.jaxws.ejbHandler.TestSOAPHandler is closed",
                                                "com.ibm.ws.jaxws.ejbHandler.TestLogicalHandler is closed",
                                                "handle outbound message in TestHandler1InBundle2",
                                                "handle outbound message in TestHandler2InBundle2" });
    }

    @Test
    public void testEJBClientHandlerBeanWithGlobalHandlers() throws Exception {

        Service service = Service.create(DUMMY_SERVICE_QNAME);
        service.addPort(DUMMY_PORT_QNAME, SOAPBinding.SOAP11HTTP_BINDING, CLIENT_HANDLER_ENDPOINT_URL);
        Dispatch<SOAPMessage> dispatch = service.createDispatch(DUMMY_PORT_QNAME, SOAPMessage.class, Service.Mode.MESSAGE);
        SOAPMessage requestSOAPMessage = createRequestSOAPMessage(EJB_CLIENT_HANDLER_SOAP_MESSAGE);

        dispatch.invoke(requestSOAPMessage);

        assertStatesExsited(5000, new String[] {
                                                "handle inbound message in TestHandler2InBundle2",
                                                "handle inbound message in TestHandler1InBundle2",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientLogicalHandler:init param \"arg0\" = testInitParam",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientLogicalHandler:postConstruct is invoked",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientSOAPHandler:postConstruct is invoked",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientLogicalHandler:handle outbound message",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientLogicalHandler:LogicalHandlerSayHiBean.sayHi:Hi, ivan",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientLogicalHandler:EchoBean",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientLogicalHandler:SOAPHandlerSayHiBean.soapHandlerSayHi:Hi, ivan",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientSOAPHandler: handle outbound message",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientSOAPHandler: sayHi = Hi, ivan",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientSOAPHandler:EchoBean",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientSOAPHandler: logicalHandlerSayHi = Hi, ivan",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientSOAPHandler is closed",
                                                ".*com.ibm.ws.jaxws.ejbHandler.TestClientLogicalHandler is closed",
                                                "handle outbound message in TestHandler1InBundle2",
                                                "handle outbound message in TestHandler2InBundle2" });
    }

    private SOAPMessage createRequestSOAPMessage(String messageText) throws SOAPException {
        SOAPMessage requestSoapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage();
        requestSoapMessage.getSOAPPart().setContent(new StreamSource(new StringReader(messageText)));
        requestSoapMessage.saveChanges();
        return requestSoapMessage;
    }

    private void assertStatesExsited(long timeout, String... states) {
        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLog(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }
}
