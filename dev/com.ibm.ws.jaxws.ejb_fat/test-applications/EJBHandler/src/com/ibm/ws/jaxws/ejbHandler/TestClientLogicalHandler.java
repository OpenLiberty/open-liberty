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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;

import com.ibm.ws.jaxws.ejbHandler.client.EchoBean;

/**
 *
 */
public class TestClientLogicalHandler implements LogicalHandler<LogicalMessageContext> {

    @EJB(name = "LogicalHandlerSayHiBean")
    private LogicalHandlerSayHi sayHi;

    @Resource(name = "arg0")
    private String testArg0;

    @PostConstruct
    public void initialize() {
        System.out.println(this.getClass().getName() + ":init param \"arg0\" = " + testArg0);
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
    public boolean handleMessage(LogicalMessageContext context) {

        boolean isOut = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (!isOut) {
            System.out.println(this.getClass().getName() + ":handle inbound message");
        } else {
            System.out.println(this.getClass().getName() + ":handle outbound message");
        }
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

        String soapHandlerSayHiOutput;
        try {
            InitialContext jndiContext = new InitialContext();
            SOAPHandlerSayHi soapHandlerSayHi = (SOAPHandlerSayHi) jndiContext.lookup("java:comp/env/SOAPHandlerSayHiBean");
            if (soapHandlerSayHi == null) {
                soapHandlerSayHiOutput = "Unable to lookup SOAPHandlerSayHi from handler";
            } else {
                soapHandlerSayHiOutput = getClass().getName() + ":SOAPHandlerSayHiBean.soapHandlerSayHi:" + soapHandlerSayHi.sayHi("ivan");
            }
        } catch (NamingException e) {
            soapHandlerSayHiOutput = "Unable to lookup SOAPHandlerSayHi from handler " + e.getMessage();
        }

        System.out.println(getClass().getName() + ":LogicalHandlerSayHiBean.sayHi:" + sayHi.sayHi("ivan"));
        System.out.println(echoBeanOutput);
        System.out.println(soapHandlerSayHiOutput);

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.ws.handler.Handler#handleFault(javax.xml.ws.handler.MessageContext)
     */
    @Override
    public boolean handleFault(LogicalMessageContext context) {
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

}
