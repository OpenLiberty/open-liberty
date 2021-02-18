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
package com.ibm.ws.webservices.handler.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import java.io.FileNotFoundException;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class HandlerChainTest {
    private static final String PROVIDER_APP_LOCATION = "HandlerChainTest/testHandlerProvider.war.xml";
    private static final String CLIENT_APP_LOCATION = "HandlerChainTest/testHandlerClient.war.xml";
    private static final String CLIENT_APP_WITHOUTXML_LOCATION = "HandlerChainTest/testHandlerClientWithoutXML.war.xml";

    private static final String PROVIDER_APP_LOCATION_DROPINS = "dropins/testHandlerProvider.war.xml";
    private static final String CLIENT_APP_LOCATION_DROPINS = "dropins/testHandlerClient.war.xml";
    private static final String CLIENT_APP_WITHOUTXML_LOCATION_DROPINS = "dropins/testHandlerClientWithoutXML.war.xml";

    @Server("HandlerChainTestServer")
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("HandlerChainTestServer");

    private final static QName serviceQName = new QName("http://jaxws.samples.ibm.com/", "TemperatureConverterService");
    private final static QName portQName = new QName("http://jaxws.samples.ibm.com/", "TemperatureConverterPort");

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.removeDropinsApplications("testHandlerClient.war", "testHandlerProvider.war");
            server.stopServer();
        }
    }

    @Test
    public void testProviderHandlerChainWithGlobalHandlers() throws Exception {
        //server.installUserBundle("MyHandler_1.0.0.201311011651");
        ShrinkHelper.defaultUserFeatureArchive(server, "userBundle", "com.ibm.ws.userbundle.myhandler");
        TestUtils.installUserFeature(server, "MyHandlerFeature");
        server.startServer("HandlerChainProvider.log");
        ShrinkHelper.defaultDropinApp(server, "testHandlerClient", "com.ibm.samples.jaxws.client", "com.ibm.samples.jaxws.client.handler", "com.ibm.samples.jaxws.client.servlet");
        ShrinkHelper.defaultDropinApp(server, "testHandlerClientWithoutXML", "com.ibm.samples.jaxws.client", "com.ibm.samples.jaxws.client.handler", "com.ibm.samples.jaxws.client.servlet");
        ShrinkHelper.defaultDropinApp(server, "testHandlerProvider", "com.ibm.samples.jaxws", "com.ibm.samples.jaxws.handler", "com.ibm.samples.jaxws.service");

        // Create the dispatch
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).
                        append(":").append(server.getHttpDefaultPort()).append("/testHandlerProvider/TemperatureConverterService");

//        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).
//                        append(":").append("8010").append("/testHandlerProvider/TemperatureConverterService");

        String endpointUrl = sBuilder.toString();
        Service service = Service.create(serviceQName);
        service.addPort(portQName, SOAPBinding.SOAP11HTTP_BINDING, endpointUrl);
        Dispatch<SOAPMessage> dispatch = service.createDispatch(portQName, SOAPMessage.class, Service.Mode.MESSAGE);

        // Send the request
        String responseStr = null;
        try {
            // Create SOAP message and extract body
            MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            SOAPMessage request = mf.createMessage();
            SOAPPart part = request.getSOAPPart();
            SOAPBody body = part.getEnvelope().getBody();

            // Build operation element by element
            SOAPElement operation = body.addChildElement(
                                                         "celsiusToFahrenheit", "tns", "http://jaxws.samples.ibm.com/");
            SOAPElement value = operation.addChildElement("temperature");
            value.addTextNode("45.3");
            request.saveChanges();

            // Dispatch message and process response
            SOAPMessage response = dispatch.invoke(request);
            SOAPBody respBody = response.getSOAPBody();
            responseStr = respBody.getFirstChild().getFirstChild().getTextContent();

            assertTrue("The response should not be null", responseStr != null);
        } catch (SOAPException se) {
            se.printStackTrace();
            throw se;
        }
        // Uninstall Applications
        server.removeDropinsApplications("testHandlerClient.war", "testHandlerClientWithoutXML.war", "testHandlerProvider.war");

        // Test invoke sequence
        assertStatesExistedFromMark(true, 5000, new String[] {
                                                              "com.ibm.samples.jaxws.handler.TestSOAPHandler: handle inbound message",
                                                              "com.ibm.samples.jaxws.handler.TestLogicalHandler: handle inbound message",
                                                              "com.ibm.samples.jaxws.handler.TestLogicalHandler: handle outbound message",
                                                              "com.ibm.samples.jaxws.handler.TestSOAPHandler: handle outbound message" });
        // Test initParams
        assertStatesExsited(5000, ".*init param \"arg0\" = testInitParam");

        // Test postConstruct and preDestroy
        assertStatesExsited(5000, new String[] {
                                                "com.ibm.samples.jaxws.handler.TestLogicalHandler: postConstruct is invoked",
                                                "com.ibm.samples.jaxws.handler.TestSOAPHandler: postConstruct is invoked",
                                                "com.ibm.samples.jaxws.handler.TestLogicalHandler: PreDestroy is invoked",
                                                "com.ibm.samples.jaxws.handler.TestSOAPHandler: PreDestroy is invoked"
        });

        //check the call sequence: service ranking: Flow
        //in InHandler1 handlemessage() method!! : 3 : IN
        //in INHandler2 handlemessage() method!! : 2 : IN
        //in InHandler3 handlemessage() method!  : 1 : IN
        assertStatesExistedFromMark(true, 5000, new String[] {
                                                              "in InHandler1 handlemessage method",
                                                              "in INHandler2 handlemessage method",
                                                              "in InHandler3 handlemessage method",
        });

        //API CHECK:

    }

    private void assertStatesExistedFromMark(boolean needReset, long timeout, String... states) {
        if (needReset) {
            server.resetLogMarks();
        }

        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLogUsingMark(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
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