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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class EJBHandlerTest {

    @Server("com.ibm.ws.jaxws.ejb.fat.ejbhandler")
    public static LibertyServer server;

    private static final String ejbhandlerear = "EJBHandler";

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

        JavaArchive jar = ShrinkHelper.buildJavaArchive(ejbhandlerear + ".jar", "com.ibm.ws.jaxws.ejbHandler",
                                                        "com.ibm.ws.jaxws.ejbHandler.client");
        ShrinkHelper.addDirectory(jar, "test-applications/EJBHandler/resources/");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ejbhandlerear + ".ear").addAsModule(jar);

        ShrinkHelper.exportDropinAppToServer(server, ear);

        try {
            server.startServer();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        Assert.assertNotNull("The application EJBHandler did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*EJBHandler"));
        //EJBHandler does not require app security things, the feature is just added there to make sure, our functions could work
        //with secuirty enabled.
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
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEJBServerHandlerBean() throws Exception {

        Service service = Service.create(DUMMY_SERVICE_QNAME);
        service.addPort(DUMMY_PORT_QNAME, SOAPBinding.SOAP11HTTP_BINDING, SERVER_HANDLER_ENDPOINT_URL);
        Dispatch<SOAPMessage> dispatch = service.createDispatch(DUMMY_PORT_QNAME, SOAPMessage.class, Service.Mode.MESSAGE);
        SOAPMessage requestSOAPMessage = createRequestSOAPMessage(EJB_SERVER_HANDLER_SOAP_MESSAGE);

        dispatch.invoke(requestSOAPMessage);

        assertStatesExsited(5000, new String[] {
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
                                                 "com.ibm.ws.jaxws.ejbHandler.TestLogicalHandler is closed" });
    }

    @Test
    public void testEJBClientHandlerBean() throws Exception {

        Service service = Service.create(DUMMY_SERVICE_QNAME);
        service.addPort(DUMMY_PORT_QNAME, SOAPBinding.SOAP11HTTP_BINDING, CLIENT_HANDLER_ENDPOINT_URL);
        Dispatch<SOAPMessage> dispatch = service.createDispatch(DUMMY_PORT_QNAME, SOAPMessage.class, Service.Mode.MESSAGE);
        SOAPMessage requestSOAPMessage = createRequestSOAPMessage(EJB_CLIENT_HANDLER_SOAP_MESSAGE);
        dispatch.invoke(requestSOAPMessage);

        assertStatesExsited(5000, new String[] {
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
                                                 ".*com.ibm.ws.jaxws.ejbHandler.TestClientLogicalHandler is closed" });
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
