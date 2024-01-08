/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class EncodingTest {

    @Server("EncodingTestServer")
    public static LibertyServer server;

    private static String APP_NAME = "encodingApp";
    private static String WSDL_URL;
    private static String MOCK_SOAP_ENDPOINT;
    private final static int REQUEST_TIMEOUT = 10;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.ws.jaxws.test.wsr.server",
                                      "com.ibm.ws.jaxws.test.wsr.test.servlet",
                                      "com.ibm.ws.jaxws.test.wsr.server.impl",
                                      "com.ibm.ws.jaxws.test.wsr.server.stub",
                                      "com.ibm.ws.jaxws.fat.util");
        server.startServer();

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));
        String APP_URL = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/").append(APP_NAME).toString();
        WSDL_URL = APP_URL + "/PeopleService?wsdl";
        MOCK_SOAP_ENDPOINT = APP_URL + "/MockSoapEndPoint";
    }

    @After
    public void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }

    /*
     * Tests if response encoding is default(UTF-8) encoding when another encoding set explicitly
     */
    @Test
    public void UTF8EncodedResponseReturnTest() {
//        try {
//            PeopleService service = new PeopleService(new URL(WSDL_URL), new QName("http://server.wsr.test.jaxws.ws.ibm.com", "PeopleService"));
//            People bill = service.getBillPort();
//            BindingProvider bp = (BindingProvider) bill;
//
//            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, MOCK_SOAP_ENDPOINT);
//
//            String result = bill.hello(" Mr. \u6771\u42ac\u55b6\u696d\u90e8");
//
//            Log.info(EncodingTest.class, "UTF8EncodedResponseReturnTest", "~result: " + result);
//
//            String reqEncoding = (String) bp.getRequestContext().get(org.apache.cxf.message.Message.ENCODING);
//            Log.info(EncodingTest.class, "UTF8EncodedResponseReturnTest", "~request encoding: " + reqEncoding);
//
//            String respEncoding = (String) bp.getResponseContext().get(org.apache.cxf.message.Message.ENCODING);
//            Log.info(EncodingTest.class, "UTF8EncodedResponseReturnTest", "~response encoding: " + respEncoding);
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }

//        try {
//            Log.info(EncodingTest.class, "UTF8EncodedResponseReturnTest", "~WSDL_URL: " + WSDL_URL);
//            HttpURLConnection con = HttpUtils.getHttpConnection(new URL(WSDL_URL), HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
//            BufferedReader br = HttpUtils.getConnectionStream(con);
//            String line = br.readLine();
//            Log.info(EncodingTest.class, "UTF8EncodedResponseReturnTest", "~line: " + line);
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
//            e.printStackTrace();
//        }

        String soapEndpointUrl = "http://localhost:8010/encodingApp/PeopleService";
        String soapAction = "";

        callSoapWebService(soapEndpointUrl, soapAction);

    }

    /*
     * Tests if response encoding matches request encoding if it's set
     */
    @Test
    public void setEncodedResponseReturnTest() {

    }

    // =========================================================================

    private static void createSoapEnvelope(SOAPMessage soapMessage) throws SOAPException {
        SOAPPart soapPart = soapMessage.getSOAPPart();

        String myNamespace = "ns2";
        String myNamespaceURI = "http://server.wsr.test.jaxws.ws.ibm.com";

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration(myNamespace, myNamespaceURI);

        /*
         * Constructed SOAP Request Message:
         * <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:myNamespace="http://www.webserviceX.NET">
         * <SOAP-ENV:Header/>
         * <SOAP-ENV:Body>
         * <myNamespace:GetInfoByCity>
         * <myNamespace:USCity>New York</myNamespace:USCity>
         * </myNamespace:GetInfoByCity>
         * </SOAP-ENV:Body>
         * </SOAP-ENV:Envelope>
         */

        // SOAP Body
        SOAPBody soapBody = envelope.getBody();
        SOAPElement soapBodyElem = soapBody.addChildElement("hello", myNamespace);
        SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("arg0", myNamespace);
        soapBodyElem1.addTextNode("World '\\u6771\\u42ac\\u55b6\\u696d\\u90e8\'");
    }

    private static void callSoapWebService(String soapEndpointUrl, String soapAction) {
        try {
            // Create SOAP Connection
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            // Send SOAP Message to SOAP Server
            SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(soapAction), soapEndpointUrl);

            // Print the SOAP Response
            Log.info(EncodingTest.class, "callSoapWebService", "~Response SOAP Message:" + soapResponse);

            soapConnection.close();
        } catch (Exception e) {
            System.err.println("\nError occurred while sending SOAP Request to Server!\nMake sure you have the correct endpoint URL and SOAPAction!\n");
            e.printStackTrace();
        }
    }

    private static SOAPMessage createSOAPRequest(String soapAction) throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();

        createSoapEnvelope(soapMessage);

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", soapAction);

        soapMessage.saveChanges();

        /* Print the request message, just for debugging purposes */
        Log.info(EncodingTest.class, "createSOAPRequest", "~Request SOAP Message:" + soapMessage);

        return soapMessage;
    }

}
