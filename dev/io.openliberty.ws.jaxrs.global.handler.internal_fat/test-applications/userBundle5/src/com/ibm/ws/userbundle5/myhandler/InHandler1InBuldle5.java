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

import java.io.PrintStream;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class InHandler1InBuldle5 implements Handler {
    private static PrintStream out = System.out;

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        // TODO Auto-generated method stub
        System.out.println("in InHandler1 handleFault method");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        if (msgctxt.getFlowType().equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN)) {

            System.out.println("########################################");
            HttpServletRequest httpRequest = msgctxt.getHttpServletRequest();
            System.out.println("in InHandler1 handlemessage method");
            if (httpRequest != null) {
                System.out.println("get Encoding in InHandler1InBuldle5 " + httpRequest.getCharacterEncoding());
                System.out.println("get LocalAddr in InHandler1InBuldle5 " + httpRequest.getLocalAddr());
            }

            //get WSDL_OPERATION and WSDL_SERVICE
            SOAPMessageContext soapmsgctxt = msgctxt.adapt(SOAPMessageContext.class);
            System.out.println("get OperationName in InHandler1: " + soapmsgctxt.get(SOAPMessageContext.WSDL_OPERATION));
            System.out.println("get ServiceName in InHandler1: " + soapmsgctxt.get(SOAPMessageContext.WSDL_SERVICE));

            System.out.println("#####################iiiiiiiiiiiiiiiiiiii###################");
            System.out.println("get OperationName in InHandler1: " + msgctxt.getProperty(javax.xml.ws.handler.MessageContext.WSDL_OPERATION).toString());
            System.out.println("get ServiceName in InHandler1: " + msgctxt.getProperty(javax.xml.ws.handler.MessageContext.WSDL_SERVICE).toString());
            System.out.println("######################iiiiiiiiiiiiiiiiiiiiii##################");

            //print out soapMessage and check arg0 in the incoming soap message has been modified to 50000
            if (msgctxt.isServerSide())
            {
                try {
                    SOAPMessage message = soapmsgctxt.getMessage();

                    out.println("\nInbound message in InHandler1" + "\n");
                    message.writeTo(out);
                    out.println("");
                    if (checkArg0(message))
                    {
                        out.println("arg0 has been modified to 5000");
                    }
                } catch (Exception e) {
                    out.println("Exception in handler: " + e);
                }

            }
        }
    }

    private boolean checkArg0(SOAPMessage message)

    {
        try {
            SOAPBody soapBody = message.getSOAPBody();
            String arg0 = soapBody.getElementsByTagName("arg0").item(0).getFirstChild().getTextContent();
            return arg0.equalsIgnoreCase("5000");
        } catch (SOAPException e) {

            return false;
        }

    }

}
