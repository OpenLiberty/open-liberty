/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxws.fat.stubclient.client;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;

import org.w3c.dom.DOMException;

/*
 * SimpleDispatchClientServlet is the test servlet that invokes the Web Service by
 * using a "dispatchMsg" http request parameter passed by the test method.
 * The servlet checks the value and uses it to determine which instance of the 
 * message that the dispatch client will use to invoke the Web Service
 */
@WebServlet("/SimpleDispatchClientServlet")
public class SimpleDispatchClientServlet extends HttpServlet {



    private static final long serialVersionUID = 4838332634689830661L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // name of the message the dispatch client will send to use for this test
        String dispatchMsg = req.getParameter("dispatchMsg");
        System.out.println("Test case will be using this dispatchMsg = " + dispatchMsg);

        QName qs = new QName("http://stubclient.fat.jaxws.openliberty.io/", "SimpleEchoService");
        QName qp = new QName("http://stubclient.fat.jaxws.openliberty.io/", "SimpleEchoPort");

        // Use the SimpleEchoService to generate the dispatch object
        SimpleEchoService echoService = new SimpleEchoService();
        // now create a dispatch object from it
        //Dispatch<StreamSource> dispatch = service.createDispatch(qp, StreamSource.class, Service.Mode.MESSAGE);

        // let's see if this has any chance of working....
        Dispatch<StreamSource> dispatch = echoService.createDispatch(
                                                                     qp,
                                                                     StreamSource.class,
                                                                     Service.Mode.MESSAGE);

        if (dispatch == null) {
            throw new RuntimeException("dispatch  is null!");
        }

        String msgString = "";
        // Pick which message to use based on the value provided by the request parameter
        if(dispatchMsg.contains("AddedElement")) {
            msgString = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                            + "  <soap:Body>\n"
                            + "    <ns2:echo xmlns:ns2=\"http://stubclient.fat.jaxws.openliberty.io/\">\n"
                            + "      <arg0>echo</arg0>\n"
                            + "      <arg1>AddedElement</arg1>\n"  // Additional element in the response
                            + "    </ns2:echo>\n"
                            + "  </soap:Body>\n"
                            + "</soap:Envelope>";
        } else if(dispatchMsg.contains("WrongElementName")) {
            msgString = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                            + "  <soap:Body>\n"
                            + "    <ns2:echo xmlns:ns2=\"http://stubclient.fat.jaxws.openliberty.io/\">\n"
                            + "      <wrongElement>Hello, WrongElementName</wrongElement>\n" // change arg0 to wrongElement
                            + "    </ns2:echo>\n"
                            + "  </soap:Body>\n"
                            + "</soap:Envelope>";
        }  else if (dispatchMsg.contains("WrongNamespace")) {
            msgString = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                            + "  <soap:Body>\n"
                            + "    <ns2:echo xmlns:ns2=\"http://stubclient.fat.jaxws.openliberty.io/\">\n"
                            + "      <wrongElement xmlns:ns2=\"http://wrong.namespace/\">Hello, WrongElementName</wrongElement>\n" // change arg0 to namespace
                            + "    </ns2:echo>\n"
                            + "  </soap:Body>\n"
                            + "</soap:Envelope>";

        } else if (dispatchMsg.contains("WrongElementContent")) {
            msgString = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                            + "  <soap:Body>\n"
                            + "    <ns2:echo xmlns:ns2=\"http://stubclient.fat.jaxws.openliberty.io/\">\n"
                            + "      <arg0>\n"
                            + "         <WrongElementContent>AddedElement</WrongElementContent>\n"  // Additional child element of arg0
                            + "      </arg0>\n"
                            + "    </ns2:echo>\n"
                            + "  </soap:Body>\n"
                            + "</soap:Envelope>";
        }


        if (msgString.equals("")) {
            throw new RuntimeException("msgString is null!");
        }

        String responseText = "";
        try {
            StreamSource response = dispatch.invoke(new StreamSource(new StringReader(msgString)));


            responseText = parseSourceResponse(response);
        } catch (Exception e) {
            responseText = e.getMessage();
        }

        if (responseText != null) {
            resp.getWriter().write(responseText);
        } else {
            resp.getWriter().write("Fail");
        }

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