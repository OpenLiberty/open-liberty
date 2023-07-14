/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.samples.jaxws.testhandlerprovider.handler;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class TestSOAPHandlerAddedElement implements SOAPHandler<SOAPMessageContext> {

    @PostConstruct
    public void initialize() {
        System.out.println(this.getClass().getName() + ": postConstruct is invoked");
    }

    @PreDestroy
    public void shutdown() {
        System.out.println(this.getClass().getName() + ": PreDestroy is invoked");
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.ws.handler.Handler#handleMessage(javax.xml.ws.handler.MessageContext)
     */
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean isOut = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (!isOut) {
            System.out.println(this.getClass().getName() + ": handle inbound message");
        } else {
            // An element is added to SOAP Body the create  "javax.xml.ws.soap.SOAPFaultException: Unmarshalling Error: unexpected element"
            try {
                Node returnNode = (Node) context.getMessage().getSOAPBody().getFirstChild();
                Node an = context.getMessage().getSOAPBody().addChildElement("return1");
                an.setTextContent("dontsayhello");
                returnNode.appendChild(an);
                System.out.println(this.getClass().getName() + ": an unkown element added");

            } catch (SOAPException e) {
                e.printStackTrace();
            }
            System.out.println(this.getClass().getName() + ": handle outbound message");
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.ws.handler.Handler#handleFault(javax.xml.ws.handler.MessageContext)
     */
    @Override
    public boolean handleFault(SOAPMessageContext context) {
        System.out.println(this.getClass().getName() + ": handle fault message");
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.ws.handler.Handler#close(javax.xml.ws.handler.MessageContext)
     */
    @Override
    public void close(MessageContext context) {
        System.out.println(this.getClass().getName() + " is closed");

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.ws.handler.soap.SOAPHandler#getHeaders()
     */
    @Override
    public Set<QName> getHeaders() {
        return null;
    }

}
