/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.fat.mock.endpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet acts as a Mock JAX-WS Endpoint that returns a "malformed" response that will cause the JAXB Unmarshaller on the
 * client side to fail when processed.
 *
 * We take the inbound request, convert it to a string, and check for the "name" element, which will tell us which test is being invoked,
 * based on that we will send a specific response, designed to cause a JAXB Unmarshaller failure.
 *
 * We set the publish servlet at http://127.0.0.1:8010/testHandlerProvider/SayHelloServiceWithHandler which
 * allows the JAX-WS client in IgnoreUnexpectedElementTestServiceClient invoke this "Web Service".
 *
 * When enableSchemaValidation=false, these malformed responses may or may not still cause additional failures since the default schema validation will be disabled
 * but the JAX-WS runtime still might not be able to process the message
 */
//Publishing the Servlet to the endpoint of a pre-existing Web Serbvice so we can use the client without modification
@WebServlet("/SayHelloServiceWithHandler")
public class MockJaxwsEndpointServlet extends HttpServlet {

    Logger LOG = Logger.getLogger("MockJaxwsEndpointServlet.class");

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Convert inbound request payload to a string
        BufferedReader requestBufferedReader = request.getReader();
        String requestPayload = new String();
        for (String payLoadLine; (payLoadLine = requestBufferedReader.readLine()) != null; requestPayload += payLoadLine);

        LOG.info("Received request: " + requestPayload);

        // Now that we've got the request, we can check the value of the "name" parameter sent by the client
        // This will allow us to know which test we are running, and which response to send back to the client.

        response.setStatus(200);
        response.setContentType("text/xml");
        response.setHeader("Address", "http://127.0.0.1:8010/testHandlerProvider/SayHelloService");
        response.setHeader("ServiceName", "SayHelloService");
        response.setHeader("PortName", "http://127.0.0.1:8010/testHandlerProvider/SayHelloService");
        PrintWriter writer = response.getWriter();
        if (requestPayload.contains("AddedElement")) {
            writer.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                          + "  <soap:Body>\n"
                          + "    <ns2:sayHelloResponse xmlns:ns2=\"http://jaxws.samples.ibm.com.handler/\">\n"
                          + "      <return>Hello, AddedElement</return>\n"
                          + "      <arg0>Hello again, AddedElement</arg0>\n" // Additional element in the response
                          + "    </ns2:sayHelloResponse>\n"
                          + "  </soap:Body>\n"
                          + "</soap:Envelope>");
        } else if (requestPayload.contains("WrongElementName")) {
            writer.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                          + "  <soap:Body>\n"
                          + "    <ns2:sayHelloResponse xmlns:ns2=\"http://jaxws.samples.ibm.com.handler/\">\n"
                          + "      <wrongElement>Hello, WrongElementName</wrongElement>\n" // change return to wrongElement
                          + "    </ns2:sayHelloResponse>\n"
                          + "  </soap:Body>\n"
                          + "</soap:Envelope>");

        } else if (requestPayload.contains("WrongNamespace")) {
            writer.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                          + "  <soap:Body>\n"
                          + "    <ns2:sayHelloResponse xmlns:ns2=\"http://jaxws.samples.ibm.com.handler/\">\n" // change namespace to wrong.namespacee
                          + "      <ns2:return xmlns:ns2=\"http://wrong.namespace/\">Hello, WrongElementNamespace</ns2:return>\n"
                          + "    </ns2:sayHelloResponse>\n"
                          + "  </soap:Body>\n"
                          + "</soap:Envelope>");

        } else if (requestPayload.contains("WrongElementContent")) {
            writer.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                          + "  <soap:Body>\n"
                          + "    <ns2:sayHelloResponse xmlns:ns2=\"http://jaxws.samples.ibm.com.handler/\">\n"
                          + "      <return>"
                          + "           <WrongElementContent>Hello again, AddedElement</WrongElementContent>\n" // Additional child elements of return
                          + "      </return>\n"
                          + "    </ns2:sayHelloResponse>\n"
                          + "  </soap:Body>\n"
                          + "</soap:Envelope>");
        }

    }
}
