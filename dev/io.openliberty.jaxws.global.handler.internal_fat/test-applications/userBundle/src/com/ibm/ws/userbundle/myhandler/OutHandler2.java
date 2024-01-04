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

import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Node;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;

public class OutHandler2 implements Handler {
    private static PrintStream out = System.out;

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        // TODO Auto-generated method stub
        System.out.println("in OutHandler2 handleFault method");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        System.out.println("in OutHandler2 handlemessage method");
        SOAPMessageContext soapmsgctxt = msgctxt.adapt(SOAPMessageContext.class);
        //print out soapMessage
        try {
            SOAPMessage message = soapmsgctxt.getMessage();

            out.println("\nInbound message in OutHandler2:" + "\n");
            message.writeTo(out);
            out.println("");
        } catch (Exception e) {
            out.println("Exception in handler: " + e);
        }
        // modify payLoad
        LogicalMessageContext logicalmsgctxt = msgctxt.adapt(LogicalMessageContext.class);
        LogicalMessage msg = logicalmsgctxt.getMessage();
        Source payload = msg.getPayload();

        Source newSource = getDOMSource(payload, "OutHandler2_Outbound:");

        if (newSource != null) {
            msg.setPayload(newSource);
        }

        //print out soapMessage
        try {
            SOAPMessage message = soapmsgctxt.getMessage();

            out.println("\nInbound message:" + "\n");
            message.writeTo(out);
            out.println("");
        } catch (Exception e) {
            out.println("Exception in handler: " + e);
        }

    }

    private Source getDOMSource(Source source, String str) {
        DOMSource newSource = new DOMSource();

        try {
            DOMResult dom = new DOMResult();

            Transformer transformer =
                            TransformerFactory.newInstance().newTransformer();

            transformer.transform(source, dom);

            /* #document node */
            Node node = dom.getNode();
            System.out.println(";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;");
            /* celsiusToFahrenheitResponse node */
            Node root = node.getFirstChild();
            /* return node */
            Node child = root.getFirstChild();
            System.out.println(child.getLocalName());
            // This is the request or response string
            String curStr = child.getTextContent();

            System.out.println(curStr);

            child.setTextContent(curStr + str);

            newSource.setNode(node);
            return newSource;
        } catch (Throwable e) {
            System.err.println(e.getClass().getName() + " was caught in WASLogicalHandler:getDOMSource()\n");
            e.printStackTrace();
            return null;
        }
    }

}
