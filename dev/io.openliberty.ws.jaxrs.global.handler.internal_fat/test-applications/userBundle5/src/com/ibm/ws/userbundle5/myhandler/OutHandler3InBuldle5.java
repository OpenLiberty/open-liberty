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
 *******************************************************************************/
package com.ibm.ws.userbundle5.myhandler;

import java.io.ByteArrayInputStream;
import java.io.PrintStream;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;

public class OutHandler3InBuldle5 implements Handler {

    private static PrintStream out = System.out;

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {

        System.out.println("in OutHandler3 handleFault() method");

    }

    @Override
    // adapt to soapMessageContext and modify soapmessage to set agr0 as 50000
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {
        System.out.println("########################################");

        System.out.println("in OutHandler3 handlemessage method");

        // modify soapMessage
        SOAPMessageContext soapmsgctxt = msgctxt.adapt(SOAPMessageContext.class);

        //print out soapMessage before modification
        try {
            SOAPMessage message = soapmsgctxt.getMessage();

            out.println("\nInbound message in OutHandler3:" + "\n");
            message.writeTo(out);
            out.println("");
        } catch (Exception e) {
            out.println("Exception in handler: " + e);
        }

        //modify arg0 as 5000 before invoking the services
        if (msgctxt.isClientSide()) {
            try {
                SOAPMessage message = soapmsgctxt.getMessage();
                SOAPPart part = message.getSOAPPart();
                SOAPEnvelope env = part.getEnvelope();
                SOAPBody body = env.getBody();
                SOAPElement sel = (SOAPElement) body.getChildElements().next();
                ((SOAPElement) (sel.getChildElements().next())).setValue("5000");

                out.println("\nInbound message:" + "\n");
                message.writeTo(out);
                out.println("");
            } catch (Exception e) {
                out.println("Exception in handler: " + e);
            }

            //get Property
            System.out.println("get age in OutHandler3InBuldle5 " + msgctxt.getProperty("age"));
            System.out.println("the globalHandlerMessageContext contains age property " + msgctxt.containsProperty("age"));

        }

        //check the return result and reset SoapMessage in server Side
        if (msgctxt.isServerSide()) {

            SOAPMessage message = soapmsgctxt.getMessage();
            SOAPPart part = message.getSOAPPart();
            SOAPEnvelope env = part.getEnvelope();
            SOAPBody body = env.getBody();
            SOAPElement sel = (SOAPElement) body.getChildElements().next();
            System.out.println(((SOAPElement) (sel.getChildElements().next())).getValue());

            SOAPMessage msg = soapmsgctxt.getMessage();

            String soapMessageinText = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"/><soap:Body><ns2:addNumbersResponse xmlns:ns2=\"http://provider.jaxws.ws.ibm.com/\"><return>Result = 3</return></ns2:addNumbersResponse></soap:Body></soap:Envelope>";

            msg = MessageFactory.newInstance().createMessage(new MimeHeaders(), new ByteArrayInputStream(soapMessageinText.getBytes("UTF-8")));
            soapmsgctxt.setMessage(msg);
        }

    }

}
