
/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package fats.cxf.jaxws22.mtom.client;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

/**
 * A handler to capture the soap message so we can see if
 * various features are present in the message, such as mtom and addressing.
 */
public class MessageCaptureHandler implements SOAPHandler<SOAPMessageContext> {

    // a place to store the message in a non-thread-safe way to avoid the
    // hassle of putting them on the message context.
    public static SOAPMessage inboundmsg = null;
    public static SOAPMessage outboundmsg = null;

    @PostConstruct
    public void init() {}

    @PreDestroy
    public void destroy() {}

    /**
     * @see javax.xml.ws.handler.soap.Handler#getHeaders()
     */
    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    /**
     * @see javax.xml.rpc.handler.Handler#handleFault(MessageContext)
     */
    @Override
    public boolean handleFault(SOAPMessageContext smc) {
        return true;
    }

    @Override
    public void close(MessageContext mc) {

    }

    /**
     * @see javax.xml.ws.handler.Handler#handleMessage(MessageContext)
     */
    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        boolean isOutbound = ((Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)).booleanValue();

        SOAPMessage msg = smc.getMessage();

        // since this is just testing we will store the message in a Client static variable
        // for retrieval and examination by the testcases.
        // In production, this would be poor practice, we would want to use the messageContext.
        if (isOutbound) {
            outboundmsg = msg;
            CommonMTOMClient.outboundmsg = msg;
            CommonManagedMTOMClient.outboundmsg = msg; // crude, but effective.

        } else {
            inboundmsg = msg;
            CommonMTOMClient.inboundmsg = msg;
            CommonManagedMTOMClient.inboundmsg = msg;
        }

        return true;
    }

    /**
     * convenience method for tests to retrieve last inbound msg as a string
     * 
     * @return
     */
    public static String getInboundMsgAsString() {
        java.io.ByteArrayOutputStream baos = null;
        try {
            baos = new java.io.ByteArrayOutputStream();
            inboundmsg.writeTo(baos);

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return baos.toString();
    }

    /**
     * convenience method for tests to retrieve last outbound msg as a string
     * 
     * @return
     */
    public static String getOutboundMsgAsString() {
        java.io.ByteArrayOutputStream baos = null;
        try {
            baos = new java.io.ByteArrayOutputStream();
            outboundmsg.writeTo(baos);

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return baos.toString();
    }

}
