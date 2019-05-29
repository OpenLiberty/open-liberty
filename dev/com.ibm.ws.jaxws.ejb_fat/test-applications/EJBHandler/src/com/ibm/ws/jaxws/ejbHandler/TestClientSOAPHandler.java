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
package com.ibm.ws.jaxws.ejbHandler;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.ibm.ws.jaxws.ejbHandler.client.EchoBean;

/**
 *
 */
public class TestClientSOAPHandler implements SOAPHandler<SOAPMessageContext> {

    @EJB(name = "SOAPHandlerSayHiBean")
    private SOAPHandlerSayHi sayHi;

    public TestClientSOAPHandler() {
    }

    @PostConstruct
    public void initialize() {
        System.out.println(this.getClass().getName() + ":postConstruct is invoked");
    }

    @PreDestroy
    public void shutdown() {
        System.out.println(this.getClass().getName() + ":PreDestroy is invoked");
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.ws.handler.Handler#handleMessage(javax.xml.ws.handler.MessageContext)
     */
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean isOut = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        String echoBeanOutput;
        try {
            InitialContext jndiContext = new InitialContext();
            EchoBean echoBean = (EchoBean) jndiContext.lookup("java:comp/env/EchoBean");
            if (echoBean == null) {
                echoBeanOutput = "Unable to lookup EchoBean from handler";
            } else {
                //NO invocation, as no way to configure the endpoint URL
                echoBeanOutput = getClass().getName() + ":EchoBean";
            }
        } catch (NamingException e) {
            echoBeanOutput = "Unable to lookup EchoBean from handler " + e.getMessage();
        }

        String logicalHandlerSayHiOutput;
        try {
            InitialContext jndiContext = new InitialContext();
            LogicalHandlerSayHi logicalHandlerSayHi = (LogicalHandlerSayHi) jndiContext.lookup("java:comp/env/LogicalHandlerSayHiBean");
            if (logicalHandlerSayHi == null) {
                logicalHandlerSayHiOutput = "Unable to lookup SOAPHandlerSayHi from handler";
            } else {
                logicalHandlerSayHiOutput = getClass().getName() + ": logicalHandlerSayHi = " + logicalHandlerSayHi.sayHi("ivan");
            }
        } catch (NamingException e) {
            logicalHandlerSayHiOutput = "Unable to lookup LogicalHandlerSayHi from handler " + e.getMessage();
        }

        if (!isOut) {
            System.out.println(getClass().getName() + ": handle inbound message");
        } else {
            System.out.println(getClass().getName() + ": handle outbound message");
        }

        System.out.println(getClass().getName() + ": sayHi = " + sayHi.sayHi("ivan"));
        System.out.println(echoBeanOutput);
        System.out.println(logicalHandlerSayHiOutput);

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
