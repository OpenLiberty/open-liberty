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
package com.ibm.ws.userbundle.myhandler;

import java.io.PrintStream;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;

public class InHandler3 implements Handler {

    private static PrintStream out = System.out;

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {

        System.out.println("in InHandler3 handleFault() method");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        System.out.println("in InHandler3 handlemessage method");

        // modify soapMessage
        SOAPMessageContext soapmsgctxt = msgctxt.adapt(SOAPMessageContext.class);

        //print out soapMessage before modification
        try {
            SOAPMessage message = soapmsgctxt.getMessage();

            out.println("\nInbound message:" + "\n");
            message.writeTo(out);
            out.println("");
        } catch (Exception e) {
            out.println("Exception in handler: " + e);
        }

        //print out soapMessage after modification
        try {
            SOAPMessage message = soapmsgctxt.getMessage();
            SOAPPart part = message.getSOAPPart();
            SOAPEnvelope env = part.getEnvelope();
            SOAPBody body = env.getBody();
            SOAPElement sel = (SOAPElement) body.getChildElements().next();
            ((SOAPElement) (sel.getChildElements().next())).setValue("500000");

            out.println("\nInbound message:" + "\n");
            message.writeTo(out);
            out.println("");
        } catch (Exception e) {
            out.println("Exception in handler: " + e);
        }

    }

}
