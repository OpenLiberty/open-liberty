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
package com.ibm.ws.jaxws.cdi.testservice.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

import com.ibm.ws.jaxws.cdi.beans.Student;

@WebServlet("/SimpleServiceServlet")
public class SimpleServiceServlet extends HttpServlet {
    /**
     * 
     */
    private static final long serialVersionUID = -8305921017784778199L;

    private static final String NAMESPACE = "http://impl.service.cdi.jaxws.ws.ibm.com/";

    private String hostname;

    private String port;

    @Inject
    private Student student;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        String msg = request.getParameter("arg0");
        PrintWriter writer = response.getWriter();
        port = request.getParameter("port");
        hostname = request.getParameter("hostname");
        String serviceName = request.getParameter("service");
        String warName = request.getParameter("war").replace(".war", "");

        if (hostname == null || hostname.isEmpty() || port == null
            || port.isEmpty() || warName == null || warName.isEmpty()
            || serviceName == null || serviceName.isEmpty()) {
            writer.println("Parameters named port, hostname, service and war are all required.");
            writer.flush();
            writer.close();
            return;
        }

        if (serviceName.equals("SimpleImplService")) {
            invokeSimpleImplService(msg, warName, serviceName, writer);
        } else if (serviceName.equals("SimpleImplProviderService")) {
            invokeSimpleImplProviderService(msg, warName, serviceName, writer);
        } else {
            writer.println("Not supported service: " + serviceName);
            writer.flush();
            writer.close();
        }
    }

    private void invokeSimpleImplProviderService(String msg, String warName, String serviceName,
                                                 PrintWriter writer) {

        StringBuilder sBuilder = new StringBuilder("http://").append(hostname).append(":").append(port).append("/").append(warName).append("/").append(serviceName).append("?wsdl");
        // System.out.println(sBuilder.toString());

        try {
            QName qname = new QName(NAMESPACE, serviceName);
            URL wsdlLocation = new URL(sBuilder.toString());
            Service service = Service.create(wsdlLocation, qname);

            QName portType = new QName(NAMESPACE, "SimpleImplProviderPort");
            Dispatch<SOAPMessage> dispatch = service.createDispatch(portType,
                                                                    SOAPMessage.class, Service.Mode.MESSAGE);
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage message = messageFactory.createMessage();
            SOAPPart soappart = message.getSOAPPart();
//            <soapenv:Body>
//            <tns2:invoke>
//            <arg0>
//            <contentDescription>strfgsdfg</contentDescription>
//            </arg0>
//            </tns2:invoke>
//            </soapenv:Body>
            SOAPEnvelope envelope = soappart.getEnvelope();
            envelope.setAttribute("xmlns:tns",
                                  "http://impl.service.cdi.jaxws.ws.ibm.com/");

            SOAPBody body = envelope.getBody();
            SOAPElement element = body.addChildElement(body.createQName("invoke",
                                                                        "tns"));
            element.addChildElement(new QName("arg0")).setTextContent(msg);

            message.writeTo(System.out);
            SOAPMessage msgRtn = dispatch.invoke(message);

            msgRtn.writeTo(System.out);

            String rtnStr = msgRtn.getSOAPBody().getTextContent();

            writer.println(rtnStr);
        } catch (Exception e) {
            writer.println(e.getMessage());
        } finally {
            writer.flush();
            writer.close();
        }

    }

    private void invokeSimpleImplService(String msg, String warName, String serviceName,
                                         PrintWriter writer) throws ServletException, IOException {

        StringBuilder sBuilder = new StringBuilder("http://").append(hostname).append(":").append(port).append("/").append(warName).append("/").append(serviceName).append("?wsdl");
        // System.out.println(sBuilder.toString());
        QName qname = new QName(NAMESPACE, serviceName);
        URL wsdlLocation = new URL(sBuilder.toString());
        Service service = Service.create(wsdlLocation, qname);

        QName portType = new QName(NAMESPACE, "SimpleImplPort");
        Dispatch<SOAPMessage> dispatch = service.createDispatch(portType,
                                                                SOAPMessage.class, Service.Mode.MESSAGE);
        try {
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage message = messageFactory.createMessage();
            SOAPPart soappart = message.getSOAPPart();

            SOAPEnvelope envelope = soappart.getEnvelope();
            envelope.setAttribute("xmlns:q0",
                                  "http://impl.service.cdi.jaxws.ws.ibm.com/");

            SOAPBody body = envelope.getBody();
            SOAPElement element = body.addChildElement(body.createQName("echo",
                                                                        "q0"));
            element.addChildElement(new QName("arg0")).setTextContent(msg);

            // message.writeTo(System.out);
            SOAPMessage msgRtn = dispatch.invoke(message);

            msgRtn.writeTo(System.out);

            String rtnStr = msgRtn.getSOAPBody().getTextContent();

            writer.println(rtnStr);
        } catch (Exception e) {
            writer.println(e.getMessage());
        } finally {
            writer.flush();
            writer.close();
        }

    }

}
