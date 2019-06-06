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
package com.ibm.ws.test.wsfeatures.client.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class TestSOAPHandler implements SOAPHandler<SOAPMessageContext> {
    @Resource(name = "soapArg0")
    private String initParam;

    @PostConstruct
    public void initialize() {
        System.out.println(this.getClass().getName() + ": init param \"soapArg0\" = " + initParam);
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

        if (isOut == true) {
            return true;
        }

        System.out.println("===============================================================");

        try {
            System.out.println(context.getMessage().getSOAPHeader().getTextContent());
            Iterator<SOAPHeaderElement> elements = context.getMessage().getSOAPHeader().extractAllHeaderElements();

            String addrNs = "http://www.w3.org/2005/08/addressing";
            List<QName> addrHeadersExpected = new ArrayList<QName>();
            addrHeadersExpected.add(new QName(addrNs, "Action"));
            addrHeadersExpected.add(new QName(addrNs, "MessageID"));
            addrHeadersExpected.add(new QName(addrNs, "To"));
            addrHeadersExpected.add(new QName(addrNs, "RelatesTo"));

            List<QName> actualHeaders = new ArrayList<QName>();
            while (elements.hasNext()) {
                SOAPHeaderElement se = elements.next();
                System.out.println("ImageServiceImplServiceTwo.uploadImage response header:" + se.getElementQName());
                actualHeaders.add(se.getElementQName());
            }

            if (!addrHeadersExpected.containsAll(actualHeaders)) {
                // Do something to fail test
                System.out.println("Did not receive any or all expected WS-Addressing headers in response");
                throw new SOAPException("Did not receive any or all expected WS-Addressing headers in response");
            }

        } catch (SOAPException e) {
            e.printStackTrace();
        }
        System.out.println("===============================================================");

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
