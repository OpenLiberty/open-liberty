/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/package com.ibm.ws.userbundle.myhandler;

import java.io.ByteArrayInputStream;
import java.io.PrintStream;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class OutHandler1 implements Handler {
    private static PrintStream out = System.out;

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        // TODO Auto-generated method stub
        System.out.println("in OutHandler1 handleFault method");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        if (msgctxt.getFlowType().equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
            HttpServletRequest httpRequest = msgctxt.getHttpServletRequest();
            System.out.println("in OutHandler1 handlemessage method");
            if (httpRequest != null) {
                System.out.println(httpRequest.getCharacterEncoding());
                System.out.println(httpRequest.getLocalAddr());
            }

            //get WSDL_OPERATION and WSDL_SERVICE
            SOAPMessageContext soapmsgctxt = msgctxt.adapt(SOAPMessageContext.class);
            System.out.println("OperationName: " + soapmsgctxt.get(SOAPMessageContext.WSDL_OPERATION));
            System.out.println("ServiceName: " + soapmsgctxt.get(SOAPMessageContext.WSDL_SERVICE));

            //print out soapMessage
            try {
                SOAPMessage message = soapmsgctxt.getMessage();

                out.println("\nInbound message in OutHandler1" + "\n");
                message.writeTo(out);
                out.println("");
            } catch (Exception e) {
                out.println("Exception in handler: " + e);
            }

            //reset SoapMessage
            SOAPMessage msg = soapmsgctxt.getMessage();
            String soapMessageinText1 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                        "<soapenv:Header xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">" +
                                        "</soapenv:Header>" +
                                        "<soapenv:Body>" +
                                        "<INPUTMSG xmlns=\"http://www.IMSPHBKI.com/schemas/IMSPHBKIInterface\">" +
                                        "<in_ll>32</in_ll>" +
                                        "<in_zz>0</in_zz>" +
                                        "<in_trcd>IVTNO</in_trcd>" +
                                        "<in_cmd>display</in_cmd>" +
                                        "<in_name1>JOHN DOE</in_name1>" +
                                        "<in_name2></in_name2>" +
                                        "<in_extn></in_extn>" +
                                        "<in_zip></in_zip>" +
                                        "</INPUTMSG>" +
                                        "</soapenv:Body>" +
                                        "</soapenv:Envelope>";
            String soapMessageinText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"/><soap:Body><ns2:sayHelloResponse xmlns:ns2=\"http://jaxws2.com\"><return>added by global handler!</return></ns2:sayHelloResponse></soap:Body></soap:Envelope>";
            msg = MessageFactory.newInstance().createMessage(new MimeHeaders(), new ByteArrayInputStream(soapMessageinText1.getBytes("UTF-8")));
            soapmsgctxt.setMessage(msg);
            //get Property
            System.out.println("get Age in OutHandler1:" + msgctxt.getProperty("age"));

        }
    }

}
