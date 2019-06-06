/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.wsfeatures.handler;

import java.util.Iterator;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class MessageInfoHandler implements Handler<SOAPMessageContext> {

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
     * @see javax.xml.ws.handler.Handler#handleFault(javax.xml.ws.handler.MessageContext)
     */
    @Override
    public boolean handleFault(SOAPMessageContext context) {
        System.out.println(this.getClass().getName() + " handling fault");
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.ws.handler.Handler#handleMessage(javax.xml.ws.handler.MessageContext)
     */
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean isOut = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (isOut == true) {
            return true;
        }

        System.out.println("===============================================================");
        System.out.println("[CONTENT-TYPE] : " + context.get("Content-Type"));

        System.out.println("[SOAPMessage] : ");
        try {
            System.out.println(context.getMessage().getSOAPHeader().getTextContent());
            Iterator<SOAPHeaderElement> elements = context.getMessage().getSOAPHeader().extractAllHeaderElements();
            while (elements.hasNext()) {
                SOAPHeaderElement se = elements.next();
                System.out.println(se.getNodeName() + ":" + se.getNodeValue() + ";" + se.getElementQName());

            }

        } catch (SOAPException e) {
            e.printStackTrace();
        }
        System.out.println("===============================================================");

        return true;
    }

}
