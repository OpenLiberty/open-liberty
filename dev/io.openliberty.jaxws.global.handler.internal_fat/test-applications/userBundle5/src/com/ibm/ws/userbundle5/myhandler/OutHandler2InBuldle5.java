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

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.DOMException;
import org.w3c.dom.NodeList;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;

public class OutHandler2InBuldle5 implements Handler {
    private static PrintStream out = System.out;

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        // add you handler logic here
        System.out.println("in OutHandler2 handleFault method");

    }

    @Override
    // Print soap message only to check newly added soap header
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        System.out.println("########################################");

        System.out.println("in OutHandler2 handlemessage method");

        SOAPMessageContext soapmsgctxt = msgctxt.adapt(SOAPMessageContext.class);

        //print out soapMessage and check to see whether it has the newly added soap header
        if (msgctxt.isClientSide()) {
            try {
                SOAPMessage message = soapmsgctxt.getMessage();

                out.println("\nInbound message in OutHandler2:" + "\n");
                message.writeTo(out);
                out.println("");
                if (containsNewHeader(message))
                {
                    out.println("can see the soap header added by OutHandler1InBuldle5 in OutHandler2InBuldle5");
                }
            } catch (Exception e) {
                out.println("Exception in handler: " + e);
            }

        }

    }

    private boolean containsNewHeader(SOAPMessage message)
    {
        try {
            NodeList nodeList = message.getSOAPHeader().getElementsByTagNameNS("http://www.webservice.com", "licenseInfo");
            String value = nodeList.item(0).getFirstChild().getTextContent();
            return value.equalsIgnoreCase("12345");
        } catch (DOMException e) {

            return false;
        } catch (SOAPException e) {

            return false;
        }

    }
}
