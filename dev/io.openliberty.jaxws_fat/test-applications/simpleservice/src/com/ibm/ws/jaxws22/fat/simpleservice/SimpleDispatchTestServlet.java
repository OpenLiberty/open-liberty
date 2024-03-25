/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws22.fat.simpleservice;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service.Mode;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.junit.Test;

import componenttest.app.FATServlet;
import fats.cxf.basic.jaxws.SOAPService1;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SimpleDispatchTestServlet")
public class SimpleDispatchTestServlet extends FATServlet {

    private static final String thisClass = "SimpleDispatchTestServlet";
    private static final Logger LOG = Logger.getLogger("HolderTestLogger");

    private static final String SERVICE_NS = "http://jaxws.basic.cxf.fats";

    private static URL wsdlURL;
    private static String serviceClientUrl = "";

    private final StringReader reqMsgSimpleClient = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://jaxws.basic.cxf.fats/types\"><invoke>Simple FVT Service Client</invoke></soapenv:Body></soapenv:Envelope>");
    private static final StringReader reqMsgSvcClient = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://jaxws.basic.cxf.fats/types\"><invoke>JAXWS FVT Version: 2.0</invoke></soapenv:Body></soapenv:Envelope>");

    QName serviceName1 = new QName(SERVICE_NS, "SOAPService1");
    QName portName1 = new QName(SERVICE_NS, "SoapPort1");

    // Construct a single instance of the service client
    static {
        try {
            wsdlURL = new URL(new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/simpleservice/SOAPService1?wsdl").toString());

            serviceClientUrl = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") +
                               "/cxfclient/SimpleServiceClient";

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service using a dispatch client.
     * And a Pre-generated SOAPMessage
     *
     */
    @Test
    public void testSimpleClient() throws Exception {

        String thisMethod = "tesSimpleClient";
        String expectedResponse = "This is JAXWS FVT CXF Web Service.";

        QName serviceName1 = new QName(SERVICE_NS, "SOAPService1");
        QName portName1 = new QName(SERVICE_NS, "SoapPort1");

        SOAPService1 service1 = new SOAPService1(wsdlURL, serviceName1);

        Source src = new StreamSource(reqMsgSimpleClient);
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq1 = factory.createMessage();
        soapReq1.getSOAPPart().setContent(src);
        soapReq1.saveChanges();

        Dispatch<SOAPMessage> dispSOAPMsg = service1.createDispatch(portName1,
                                                                    SOAPMessage.class, Mode.MESSAGE);

        LOG.info(thisClass + " " + thisMethod + " " + "Invoking server through Dispatch interface using SOAPMessage with: " + soapReq1);

        SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq1);
        LOG.info(thisClass + " " + thisMethod + " " + "CXF Client Response SOAP Body contents: " + soapResp.getSOAPBody().getTextContent());

        String respText = soapResp.getSOAPBody().getTextContent();

        assertTrue("The testCxfClientToCxfWebSvc test failed",
                   respText.contains(expectedResponse));

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes
     * a simple jax-ws cxf web service.
     *
     */
    @Test
    public void testSvcClient() throws Exception {

        String expectedResponse = "This is JAXWS FVT CXF Web Service.";

        SOAPService1 service1 = new SOAPService1(wsdlURL, serviceName1);

        Source src = new StreamSource(reqMsgSvcClient);
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq1 = factory.createMessage();
        soapReq1.getSOAPPart().setContent(src);
        soapReq1.saveChanges();

        Dispatch<SOAPMessage> dispSOAPMsg = service1.createDispatch(portName1,
                                                                    SOAPMessage.class, Mode.MESSAGE);

        SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq1);

        String answer = soapResp.getSOAPBody().getTextContent();

        assertTrue("The testSvcClient test failed",
                   answer.contains(expectedResponse));

        return;

    }

}
