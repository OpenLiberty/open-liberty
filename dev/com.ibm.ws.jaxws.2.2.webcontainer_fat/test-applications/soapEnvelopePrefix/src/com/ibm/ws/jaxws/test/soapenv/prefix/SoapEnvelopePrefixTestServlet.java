/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxws.test.soapenv.prefix;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.annotation.WebServlet;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Tests setting the default namespace of a soap envelope to the SOAP 1.1
 * Envelope namespace on a request. This helps us verify that CXF properly handles valid
 * and invalid XML for unmarshalling.
 *
 * Each test uses the response to verify if the XML contains the expected response or the
 * expected exception.
 *
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SoapEnvelopePrefixTestServlet")
public class SoapEnvelopePrefixTestServlet extends FATServlet {

    // Construct a single instance of the dispatch client
    private static URL WSDL_URL;
    private static QName qs;
    private static QName qp;
    private static Service service;
    private static Dispatch<StreamSource> dispatch;

    static {
        try {
            WSDL_URL = new URL(new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/soapEnvelopePrefix/PeopleService?wsdl").toString());

            //dispatch client --no need stubs
            qs = new QName("http://server.wsr.test.jaxws.ws.ibm.com", "PeopleService");
            qp = new QName("http://server.wsr.test.jaxws.ws.ibm.com", "BillPort");

            service = Service.create(qs);
            service.addPort(qp, SOAPBinding.SOAP11HTTP_BINDING, WSDL_URL.toString());

            // Uses Service.Mode.MESSAGE to prevent CXF from altering the outbound request Payload
            // now create a dispatch object from it
            dispatch = service.createDispatch(qp, StreamSource.class, Service.Mode.MESSAGE);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    // String constants to verify the response messages match what's expected
    private static final String SOAP11_UNMARSHAL_EXPECTED_EXCEPTION = "unexpected element (uri:\"http://schemas.xmlsoap.org/soap/envelope/\", local:\"arg0";
    private static final String EXPECTED_PASSING_RESPONSE = "from dispatch World";

    /*
     * A positive test where the default namespace is set to the SOAP 1.1 Envelope NS, but then redeclares the default namespace
     * to the " (empty) namespace in a child element making the xml valid.
     *
     * Default Envelope Namespace: SOAP 1.1 NS - http://schemas.xmlsoap.org/soap/envelope/
     *
     * Valid Element QNames:
     *
     * Envelope QName: {http://schemas.xmlsoap.org/soap/envelope/}Envelope
     * Body QName: {http://schemas.xmlsoap.org/soap/envelope/}Body
     * hello QName: {http://server.wsr.test.jaxws.ws.ibm.com}hello
     * arg0 QName: {}arg0
     */
    @Test
    public void testSoap11DefaultPrefixInEnvelopeValidXML() throws Exception {

        String msgString = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                           + "  <Body>\n"
                           + "    <ser:hello xmlns=\"\" xmlns:ser=\"http://server.wsr.test.jaxws.ws.ibm.com\">\n"
                           + "       <arg0>from dispatch World</arg0>\n"
                           + "    </ser:hello>\n"
                           + "  </Body>\n"
                           + "</Envelope>";

        if (dispatch == null) {
            throw new RuntimeException("dispatch  is null!");
        }

        StreamSource response = dispatch.invoke(new StreamSource(new StringReader(msgString)));
        String parsedResponse = parseSourceResponse(response);
        assertTrue("Expected parsed string to contain: " + EXPECTED_PASSING_RESPONSE + " but was actually: " + parsedResponse,
                   parsedResponse.contains(EXPECTED_PASSING_RESPONSE));
        //assertNull(notFoundString + " is expected to be in generated schema: " + schemaString, notFoundString);
    }

    /*
     * A positive test where the default namespace is set to the SOAP 1.1 Envelope NS, and the header then sets attributes that
     * inherit the default namespace
     *
     * Default Envelope Namespace: SOAP 1.1 NS - http://schemas.xmlsoap.org/soap/envelope/
     *
     * Valid Element QNames:
     *
     * Envelope QName: {http://schemas.xmlsoap.org/soap/envelope/}Envelope
     * Body QName: {http://schemas.xmlsoap.org/soap/envelope/}Body
     * hello QName: {http://server.wsr.test.jaxws.ws.ibm.com}hello
     * arg0 QName: {}arg0
     */
    @Test
    public void testSoap11DefaultAttributePrefixInHeaderValidXML() throws Exception {

        String msgString = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                           + "<Header boo=\"1\"/>"
                           + "  <Body>\n"
                           + "    <ser:hello xmlns=\"\" xmlns:ser=\"http://server.wsr.test.jaxws.ws.ibm.com\">\n"
                           + "       <arg0>from dispatch World</arg0>\n"
                           + "    </ser:hello>\n"
                           + "  </Body>\n"
                           + "</Envelope>";

        if (dispatch == null) {
            throw new RuntimeException("dispatch  is null!");
        }

        StreamSource response = dispatch.invoke(new StreamSource(new StringReader(msgString)));
        String parsedResponse = parseSourceResponse(response);
        assertTrue("Expected parsed string to contain: " + EXPECTED_PASSING_RESPONSE + " but was actually: " + parsedResponse,
                   parsedResponse.contains(EXPECTED_PASSING_RESPONSE));
        //assertNull(notFoundString + " is expected to be in generated schema: " + schemaString, notFoundString);
    }

    /*
     * A positive test where an additional optional eleement is added to the request, to ensure that service will ignore it and process the request successfully.
     * Additionally this test sends multiple requests that a unmarshaller obtained from the poll can also process the optional element in the second request.
     *
     * Default Envelope Namespace: SOAP 1.1 NS - http://schemas.xmlsoap.org/soap/envelope/
     *
     * Valid Element QNames:
     *
     * Envelope QName: {http://schemas.xmlsoap.org/soap/envelope/}Envelope
     * Body QName: {http://schemas.xmlsoap.org/soap/envelope/}Body
     * hello QName: {http://server.wsr.test.jaxws.ws.ibm.com}hello
     * arg0 QName: {}arg0
     */
    @Test
    public void testSoap11OptionalElementMashallerPool() throws Exception {

        String msgString = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                           + "  <Body>\n"
                           + "    <ser:hello xmlns=\"\" xmlns:ser=\"http://server.wsr.test.jaxws.ws.ibm.com\">\n"
                           + "       <arg0>from dispatch World</arg0>\n"
                           + "       <arg1>from dispatch World</arg1>\n"
                           + "    </ser:hello>\n"
                           + "  </Body>\n"
                           + "</Envelope>";

        if (dispatch == null) {
            throw new RuntimeException("dispatch  is null!");
        }

        StreamSource response = dispatch.invoke(new StreamSource(new StringReader(msgString)));
        String parsedResponse = parseSourceResponse(response);
        assertTrue("Expected parsed string to contain: " + EXPECTED_PASSING_RESPONSE + " but was actually: " + parsedResponse,
                   parsedResponse.contains(EXPECTED_PASSING_RESPONSE));

        StreamSource response1 = dispatch.invoke(new StreamSource(new StringReader(msgString)));
        String parsedResponse1 = parseSourceResponse(response1);
        assertTrue("Expected parsed string to contain: " + EXPECTED_PASSING_RESPONSE + " but was actually: " + parsedResponse1,
                   parsedResponse1.contains(EXPECTED_PASSING_RESPONSE));
        //assertNull(notFoundString + " is expected to be in generated schema: " + schemaString, notFoundString);
    }

    /*
     * A negative test where the default namespace is set to the SOAP 1.1 Envelope NS.
     *
     * Default Envelope Namespace: SOAP 1.1 NS - http://schemas.xmlsoap.org/soap/envelope/
     *
     * Valid Element QNames:
     *
     * Envelope QName: {http://schemas.xmlsoap.org/soap/envelope/}Envelope
     * Body QName: {http://schemas.xmlsoap.org/soap/envelope/}Body
     * hello QName: {http://server.wsr.test.jaxws.ws.ibm.com}hello
     *
     * Invalid Element QNames:
     *
     * arg0 QName: {http://schemas.xmlsoap.org/soap/envelope/}arg0
     */
    @Test
    public void testSoap11DefaultPrefixInEnvelopeInvalidXML() throws Exception {

        String msgString = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                           + "  <Body>\n"
                           + "    <ser:hello xmlns:ser=\"http://server.wsr.test.jaxws.ws.ibm.com\">\n"
                           + "       <arg0>from dispatch World</arg0>\n"
                           + "    </ser:hello>\n"
                           + "  </Body>\n"
                           + "</Envelope>";

        if (dispatch == null) {
            throw new RuntimeException("dispatch  is null!");
        }

        try {

            StreamSource response = dispatch.invoke(new StreamSource(new StringReader(msgString)));

        } catch (Exception e) {

            //tring parsedResponse = parseSourceResponse(response);
            assertTrue("Expected parsed string to contain: " + SOAP11_UNMARSHAL_EXPECTED_EXCEPTION + " but was actually: " + e.toString(),
                       e.toString().contains(SOAP11_UNMARSHAL_EXPECTED_EXCEPTION));
        }

        //assertNull(notFoundString + " is expected to be in generated schema: " + schemaString, notFoundString);
    }

    /*
     * Method for parsing response from StreamSource to a String
     */
    private String parseSourceResponse(Source response) {
        try {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.transform(response, result);
            String strResult = writer.toString();
            return strResult;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
