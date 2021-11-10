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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class HandlerChainTest {

    private static final String CLIENT_APP_LOCATION_DROPINS = "dropins/testHandlerClient.war";
    private static final String CLIENT_APP_WITHOUTXML_LOCATION_DROPINS = "dropins/testHandlerClientWithoutXML.war";

    @Server("AnnotatedHandlerChainTestServer")
    public static LibertyServer annotatedServer;

    @Server("XmlOverrideHandlerChainTestServer")
    public static LibertyServer xmlServer;

//    private final static QName serviceQName = new QName("http://jaxws.samples.ibm.com/", "TemperatureConverterService");
//    private final static QName portQName = new QName("http://jaxws.samples.ibm.com/", "TemperatureConverterPort");

    private final static int REQUEST_TIMEOUT = 10;

    private static WebArchive testHandlerClient = null;
    private static WebArchive testHandlerClientWithoutXML = null;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(annotatedServer, "testHandlerProvider", "com.ibm.samples.jaxws.testhandlerprovider",
                                      "com.ibm.samples.jaxws.testhandlerprovider.handler",
                                      "com.ibm.samples.jaxws.testhandlerprovider.service");

        ShrinkHelper.defaultDropinApp(annotatedServer, "testHandlerClientWithoutXML", "com.ibm.samples.jaxws.client",
                                      "com.ibm.samples.jaxws.client.handler",
                                      "com.ibm.samples.jaxws.client.nowebxml.servlet");

        ShrinkHelper.defaultDropinApp(xmlServer, "testHandlerProvider", "com.ibm.samples.jaxws.testhandlerprovider",
                                      "com.ibm.samples.jaxws.testhandlerprovider.handler",
                                      "com.ibm.samples.jaxws.testhandlerprovider.service");

        ShrinkHelper.defaultDropinApp(xmlServer, "testHandlerClient", "com.ibm.samples.jaxws.client",
                                      "com.ibm.samples.jaxws.client.handler",
                                      "com.ibm.samples.jaxws.client.servlet");
    }

    @After
    public void tearDown() throws Exception {
        if (annotatedServer != null && annotatedServer.isStarted()) {
            annotatedServer.stopServer();
        }
        if (xmlServer != null && xmlServer.isStarted()) {
            xmlServer.stopServer();
        }
    }

    /*
     * Commenting this test case out because it fails in JAVA9, but it fails not because of an issue with the jaxws-2.2 feature or the cxf 2.6.2 code.
     * It's failing because this test is doing a Dispatch.invoke from the test case itself rather than from a Client, and some how it's picking up the CXF
     * binaries (that aren't provided by the jaxws-2.2 feature) on the class path from a completely different test app called pureCXFTest. That test app has
     * all the CXF binaries.
     * TODO:
     * Either this test case is totally invalid because it's testing a Dispatch invocation from the test case rather than from a client web app, or its failing
     * because it's picking up a different CXF impl from the pureCXFTest client app that doesn't need to even test an old version of 2.6.2 anyway.
     *
     * @Test
     * public void testProviderHandlerChain() throws Exception {
     * server.startServer("HandlerChainProvider.log");
     * installApplications(true);
     *
     * // Create the dispatch
     * StringBuilder sBuilder = new
     * StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/testHandlerProvider/TemperatureConverterService");
     * String endpointUrl = sBuilder.toString();
     * Service service = Service.create(serviceQName);
     * service.addPort(portQName, SOAPBinding.SOAP11HTTP_BINDING, endpointUrl);
     * Dispatch<SOAPMessage> dispatch = service.createDispatch(portQName, SOAPMessage.class, Service.Mode.MESSAGE);
     *
     * // Send the request
     * String responseStr = null;
     * try {
     * // Create SOAP message and extract body
     * MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
     * SOAPMessage request = mf.createMessage();
     * SOAPPart part = request.getSOAPPart();
     * SOAPBody body = part.getEnvelope().getBody();
     *
     * // Build operation element by element
     * SOAPElement operation = body.addChildElement(
     * "celsiusToFahrenheit", "tns", "http://jaxws.samples.ibm.com/");
     * SOAPElement value = operation.addChildElement("temperature");
     *
     * Log.info(this.getClass(), "testProviderHandlerChain()", "SOAPBody: " + body.toString());
     * Log.info(this.getClass(), "testProviderHandlerChain()", "operation: " + operation.toString() + " value: " + value);
     *
     * value.addTextNode("45.3");
     * request.saveChanges();
     *
     * // Dispatch message and process response
     * SOAPMessage response = dispatch.invoke(request);
     * SOAPBody respBody = response.getSOAPBody();
     * responseStr = respBody.getFirstChild().getFirstChild().getTextContent();
     *
     * assertTrue("The response should not be null", responseStr != null);
     * } catch (SOAPException se) {
     * throw se;
     * }
     * // Uninstall Applications
     * uninstallApplications();
     *
     * // Test invoke sequence
     * assertStatesExistedFromMark(true, 5000, new String[] {
     * "com.ibm.samples.jaxws.handler.TestSOAPHandler: handle inbound message",
     * "com.ibm.samples.jaxws.handler.TestLogicalHandler: handle inbound message",
     * "com.ibm.samples.jaxws.handler.TestLogicalHandler: handle outbound message",
     * "com.ibm.samples.jaxws.handler.TestSOAPHandler: handle outbound message" });
     * // Test initParams
     * assertStatesExsited(5000, ".*init param \"arg0\" = testInitParam");
     *
     * // Test postConstruct and preDestroy
     * assertStatesExsited(5000, new String[] {
     * "com.ibm.samples.jaxws.handler.TestLogicalHandler: postConstruct is invoked",
     * "com.ibm.samples.jaxws.handler.TestSOAPHandler: postConstruct is invoked",
     * "com.ibm.samples.jaxws.handler.TestLogicalHandler: PreDestroy is invoked",
     * "com.ibm.samples.jaxws.handler.TestSOAPHandler: PreDestroy is invoked"
     * });
     * }
     */

    @Test
    public void testClientAnnotatedHandlerChain() throws Exception {

        annotatedServer.startServer("HandlerChainClientAnnotation.log");

        // Visit the URL:  http://hostName:hostPort/testHandlerClientWithoutXML/TestServiceServlet?target=user
        assertResponseNotNull(annotatedServer, "testHandlerClientWithoutXML/TestServiceServlet?target=user");

        // Uninstall the applications
        uninstallApplications(annotatedServer);

        // Test invoke sequence
        assertStatesExistedFromMark(annotatedServer, true, 5000, new String[] {
                                                                                "com.ibm.samples.jaxws.client.handler.TestLogicalHandler: handle outbound message",
                                                                                "com.ibm.samples.jaxws.client.handler.TestSOAPHandler: handle outbound message",
                                                                                "com.ibm.samples.jaxws.client.handler.TestSOAPHandler: handle inbound message",
                                                                                "com.ibm.samples.jaxws.client.handler.TestLogicalHandler: handle inbound message",
                                                                                "com.ibm.samples.jaxws.client.handler.TestSOAPHandler is closed",
                                                                                "com.ibm.samples.jaxws.client.handler.TestLogicalHandler is closed" });
        // Test initParams
        assertStatesExsited(annotatedServer, 5000, new String[] {
                                                                  ".*init param \"arg0\" = testInitParamClient",
                                                                  ".*init param \"soapArg0\" = testInitParamInSoapClient" });

        // Test postConstruct and preDestroy
        assertStatesExsited(annotatedServer, 5000, new String[] {
                                                                  "com.ibm.samples.jaxws.client.handler.TestLogicalHandler: postConstruct is invoked",
                                                                  "com.ibm.samples.jaxws.client.handler.TestSOAPHandler: postConstruct is invoked",
                        //PreDestroy for the handlerChain on the client side is not supported, as it is not a managed environment
                        //"com.ibm.samples.jaxws.client.handler.TestLogicalHandler: PreDestroy is invoked",
                        //"com.ibm.samples.jaxws.client.handler.TestSOAPHandler: PreDestroy is invoked"
        });
    }

    @Test
    public void testClientXMLOverrideHandlerChain() throws Exception {

        xmlServer.startServer("HandlerChainClientXMLOverride.log");

        // Visit the URL:  http://hostName:hostPort/testHandlerClient/TestServiceServlet?target=user
        assertResponseNotNull(xmlServer, "testHandlerClient/TestServiceServlet?target=user");

        // Visit the URL:  http://hostName:hostPort/testHandlerClient/TestPortServlet?target=user
        assertResponseNotNull(xmlServer, "testHandlerClient/TestPortServlet?target=user");

        // Uninstall the applications
        uninstallApplications(xmlServer);

        // Test invoke sequence
        assertStatesExistedFromMark(xmlServer, true, 5000, new String[] {
                                                                          "com.ibm.samples.jaxws.client.handler.TestLogicalHandler: handle outbound message",
                                                                          "com.ibm.samples.jaxws.client.handler.TestSOAPHandler: handle outbound message",
                                                                          "com.ibm.samples.jaxws.client.handler.TestSOAPHandler: handle inbound message",
                                                                          "com.ibm.samples.jaxws.client.handler.TestLogicalHandler: handle inbound message",
                                                                          "com.ibm.samples.jaxws.client.handler.TestSOAPHandler is closed",
                                                                          "com.ibm.samples.jaxws.client.handler.TestLogicalHandler is closed",
                                                                          "com.ibm.samples.jaxws.client.handler.TestLogicalHandler: handle outbound message",
                                                                          "com.ibm.samples.jaxws.client.handler.TestSOAPHandler: handle outbound message",
                                                                          "com.ibm.samples.jaxws.client.handler.TestSOAPHandler: handle inbound message",
                                                                          "com.ibm.samples.jaxws.client.handler.TestLogicalHandler: handle inbound message",
                                                                          "com.ibm.samples.jaxws.client.handler.TestSOAPHandler is closed",
                                                                          "com.ibm.samples.jaxws.client.handler.TestLogicalHandler is closed" });
        // Test initParams
        assertStatesExsited(xmlServer, 5000, new String[] {
                                                            ".*init param \"soapArg0\" = testServiceInitParamInSoapFromRes",
                                                            ".*init param \"arg0\" = testServiceInitParamFromRes",
                                                            ".*init param \"soapArg0\" = testServiceInitParamInSoapFromWSRef",
                                                            ".*init param \"arg0\" = testServiceInitParamFromWSRef",
                                                            ".*init param \"soapArg0\" = testPortInitParamInSoapFromWSRef",
                                                            ".*init param \"arg0\" = testPortInitParamFromWSRef",
                                                            ".*init param \"soapArg0\" = testPortInitParamInSoapFromRes",
                                                            ".*init param \"arg0\" = testPortInitParamFromRes" });

        // Test postConstruct and preDestroy
        assertStatesExsited(xmlServer, 5000, new String[] {
                                                            "com.ibm.samples.jaxws.client.handler.TestSOAPHandler: postConstruct is invoked",
                                                            "com.ibm.samples.jaxws.client.handler.TestLogicalHandler: postConstruct is invoked",
        });

    }

    private void assertResponseNotNull(LibertyServer server, String pathAndParams) throws ProtocolException, IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + pathAndParams);
        Log.info(this.getClass(), "assertResponseNotNull", "Calling Application with URL=" + url.toString());

        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        //If there is output then the Application automatically installed correctly
        assertNotNull("The content of the response is null", line);
    }

    private void assertStatesExistedFromMark(LibertyServer server, boolean needReset, long timeout, String... states) {
        if (needReset) {
            server.resetLogOffsets();
        }

        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLogUsingMark(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }

    private void assertStatesExsited(LibertyServer server, long timeout, String... states) {
        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLog(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }

    private void uninstallApplications(LibertyServer server) throws Exception {
        try {
            server.removeDropinsApplications("testHandlerClient.war");
        } catch (FileNotFoundException e) {
            Log.warning(this.getClass(), e.getMessage());
        }

        try {
            server.removeDropinsApplications("testHandlerClientWithoutXML.war");
        } catch (FileNotFoundException e) {
            Log.warning(this.getClass(), e.getMessage());
        }

    }

}
