/*******************************************************************************
 * Copyright (c) 2021,2023 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import java.io.FileNotFoundException;
import java.util.logging.Logger;

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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class HandlerChainTest {
    private static final String PROVIDER_APP_LOCATION = "HandlerChainTest/testHandlerProvider.war.xml";
    private static final String CLIENT_APP_LOCATION = "HandlerChainTest/testHandlerClient.war.xml";
    private static final String CLIENT_APP_WITHOUTXML_LOCATION = "HandlerChainTest/testHandlerClientWithoutXML.war.xml";

    private static final String PROVIDER_APP_LOCATION_DROPINS = "dropins/testHandlerProvider.war.xml";
    private static final String CLIENT_APP_LOCATION_DROPINS = "dropins/testHandlerClient.war.xml";
    private static final String CLIENT_APP_WITHOUTXML_LOCATION_DROPINS = "dropins/testHandlerClientWithoutXML.war.xml";

    public static LibertyServer server = null;
    
    //Alternating servers to avoid un-registration of incomingObserver
    @Server("HandlerChainTestServer")
    public static LibertyServer serverAlternate1;
    
    @Server("HandlerChainTestServerAlternate")
    public static LibertyServer serverAlternate2;

    private final static QName serviceQName = new QName("http://jaxws.samples.ibm.com/", "TemperatureConverterService");
    private final static QName portQName = new QName("http://jaxws.samples.ibm.com/", "TemperatureConverterPort");

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
    
    @BeforeClass
    public static void setup() throws Exception {
        // Alternate server to prevent deregistration of incomingObserver from next EE repetition
        if(JakartaEEAction.isEE11Active()) {
            server = serverAlternate1;
        } else if(JakartaEEAction.isEE10Active()) {
            server = serverAlternate2;
        } else if(JakartaEEAction.isEE9Active()) {
            server = serverAlternate1;
        } else if(EE8FeatureReplacementAction.isActive()) {
            server = serverAlternate2;
        } else {
            server = serverAlternate1;
        }

        ShrinkHelper.defaultUserFeatureArchive(server, "userBundle", "com.ibm.ws.userbundle.myhandler");
        TestUtils.installUserFeature(server, "MyHandlerFeature");
        ShrinkHelper.defaultDropinApp(server, "testHandlerClient", "com.ibm.samples.jaxws.client", "com.ibm.samples.jaxws.client.handler", "com.ibm.samples.jaxws.client.servlet");
        ShrinkHelper.defaultDropinApp(server, "testHandlerClientWithoutXML", "com.ibm.samples.jaxws.client", "com.ibm.samples.jaxws.client.handler", "com.ibm.samples.jaxws.client.servlet");
        ShrinkHelper.defaultDropinApp(server, "testHandlerProvider", "com.ibm.samples.jaxws", "com.ibm.samples.jaxws.handler", "com.ibm.samples.jaxws.service");
        
        server.startServer("HandlerChainProvider.log");
        
        server.waitForMultipleStringsInLog(3, "CWWKZ0001I");
    }
    
    @Test
    public void testProviderHandlerChainWithGlobalHandlers() throws Exception {

        // Create the dispatch
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).
                        append(":").append(server.getHttpDefaultPort()).append("/testHandlerProvider/TemperatureConverterService");

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
        
        // Test invoke sequence
        assertStatesExistedFromMark(true, new String[] {
                                                              "com.ibm.samples.jaxws.handler.TestSOAPHandler: handle inbound message",
                                                              "com.ibm.samples.jaxws.handler.TestLogicalHandler: handle inbound message",
                                                              "com.ibm.samples.jaxws.handler.TestLogicalHandler: handle outbound message",
                                                              "com.ibm.samples.jaxws.handler.TestSOAPHandler: handle outbound message" });
        // Test initParams
        assertStatesExisted(".*init param \"arg0\" = testInitParam");

        // Test postConstruct 
        assertStatesExisted(new String[] {
                                                "com.ibm.samples.jaxws.handler.TestLogicalHandler: postConstruct is invoked",
                                                "com.ibm.samples.jaxws.handler.TestSOAPHandler: postConstruct is invoked"});

        // Stop server, with option to preserve logs 
        server.stopServer(false, true, null);

        // Test preDestroy  
        assertStatesExisted(new String[] {
                                                "com.ibm.samples.jaxws.handler.TestLogicalHandler: PreDestroy is invoked",
                                                "com.ibm.samples.jaxws.handler.TestSOAPHandler: PreDestroy is invoked"
        });

        //check the call sequence: service ranking: Flow
        //in InHandler1 handlemessage() method!! : 3 : IN
        //in INHandler2 handlemessage() method!! : 2 : IN
        //in InHandler3 handlemessage() method!  : 1 : IN
        assertStatesExistedFromMark(true, new String[] {
                                                              "in InHandler1 handlemessage method",
                                                              "in INHandler2 handlemessage method",
                                                              "in InHandler3 handlemessage method",
        });

    }

    private void assertStatesExistedFromMark(boolean needReset, String... states) {
        if (needReset) {
            server.resetLogMarks();
        }

        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLogUsingMark(state);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }

    private void assertStatesExisted(String... states) {
        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLog(state);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }
}