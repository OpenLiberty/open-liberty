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

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
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
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class InHandler2InBuldle5 implements Handler {
    private static PrintStream out = System.out;

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        // TODO Auto-generated method stub
        System.out.println("in InHandler2 handleFault method");

    }

    @Override
    //// modify payload to set arg1 +5
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        if (msgctxt.isServerSide()) {

            System.out.println("########################################");
            System.out.println("in InHandler2 handlemessage method");
            SOAPMessageContext soapmsgctxt = msgctxt.adapt(SOAPMessageContext.class);
            //print out soapMessage
            try {
                SOAPMessage message = soapmsgctxt.getMessage();

                out.println("\nInbound message in InHandler2:" + "\n");
                message.writeTo(out);
                out.println("");
            } catch (Exception e) {
                out.println("Exception in handler: " + e);
            }
            if (msgctxt.getFlowType().equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN))
            {
                // modify payLoad
                LogicalMessageContext logicalmsgctxt = msgctxt.adapt(LogicalMessageContext.class);
                LogicalMessage msg = logicalmsgctxt.getMessage();
                Source payload = msg.getPayload();
                //increase arg1 to arg1 + 5
                Source newSource = getDOMSource(payload, 5);

                if (newSource != null) {
                    msg.setPayload(newSource);
                }

                //print out soapMessage
                try {
                    SOAPMessage message = soapmsgctxt.getMessage();

                    out.println("\nInbound message in InHandler2:" + "\n");
                    message.writeTo(out);
                    if (checkArg1(message))
                    {
                        out.println("arg1 has been modified to 7 in InHandler2InBuldle5");
                    }
                    out.println("");
                } catch (Exception e) {
                    out.println("Exception in handler: " + e);
                }
            }

        }
    }

    private Source getDOMSource(Source source, int incremental) {
        DOMSource newSource = new DOMSource();

        try {
            DOMResult dom = new DOMResult();

            Transformer transformer =
                            TransformerFactory.newInstance().newTransformer();

            transformer.transform(source, dom);

            /* #document node */
            Node node = dom.getNode();

            /* addNumbers node */
            Node root = node.getFirstChild();

            /* arg1 node */
            Node child = root.getFirstChild().getNextSibling();
            int curStr = Integer.parseInt(child.getTextContent());
            child.setTextContent(String.valueOf(curStr + incremental));

            newSource.setNode(node);
            return newSource;
        } catch (Throwable e) {
            System.err.println(e.getClass().getName() + " was caught in INHandler2:getDOMSource()\n");
            e.printStackTrace();
            return null;
        }
    }

    private boolean checkArg1(SOAPMessage message)

    {
        try {
            SOAPBody soapBody = message.getSOAPBody();
            String arg1 = soapBody.getElementsByTagName("arg1").item(0).getFirstChild().getTextContent();
            return arg1.equalsIgnoreCase("7");
        } catch (SOAPException e) {

            return false;
        }

    }
}