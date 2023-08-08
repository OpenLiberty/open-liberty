/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.policyattachments.client1.service1;

import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

/**
 * The purpose of this SOAPHandler is to modify the ReplyTo SOAPHeaders default Address value from
 * to com.ibm.ws.policyattachments.client1.service1.HelloService so that we can test if the Policy
 * Attachment is being applied and requiring both the Service and Client to require NonAnonymous style
 * responses.
 *
 * NOTE: Because of the way apps are packaged with ShrinkHelper the handler-chain.xml file for this Handler is here:
 *
 * test-applications/policyAttachmentsClient1/resources/WEB-INF/classes/com/ibm/ws/policyattachments/client1/service1/handler-chain.xml
 */
public class HelloClientReplyToHandler implements SOAPHandler<SOAPMessageContext> {

    @Override
    public void close(MessageContext arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        // TODO Auto-generated method stub
        System.out.println("Server executing SOAP Handler");

        Boolean outBoundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        // if this is an incoming message from the client
        if (outBoundProperty) {
            SOAPMessage msg = context.getMessage();
            SOAPHeader header;
            try {
                header = msg.getSOAPHeader();

                QName replyToQn = new QName("http://www.w3.org/2005/08/addressing", "ReplyTo");

                SOAPElement replyTo = null;
                Iterator<Node> elements = header.getChildElements();
                if (elements != null) {
                    while (elements.hasNext()) {

                        SOAPElement element = (SOAPElement) elements.next();

                        Name name = element.getElementName();

                        System.out.println("Searching for ReplyTo header: " + name.getLocalName());

                        if (name.getLocalName() == "ReplyTo") {

                            System.out.println("Found the ReplyTo header");
                            SOAPElement oldReplyTo = element;

                            oldReplyTo.removeContents();

                            QName addressQn = new QName("http://www.w3.org/2005/08/addressing", "Address");
                            SOAPElement address = oldReplyTo.addChildElement(addressQn);

                            address.setTextContent("http://localhost:8010/policyAttachmentsClient1/HelloService3");

                            return true;
                        }
                    }
                }

            } catch (SOAPException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                e.printStackTrace();
            }
        }
        return true;

    }

    @Override
    public Set getHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean handleFault(SOAPMessageContext arg0) {
        // TODO Auto-generated method stub
        return false;
    }
}
