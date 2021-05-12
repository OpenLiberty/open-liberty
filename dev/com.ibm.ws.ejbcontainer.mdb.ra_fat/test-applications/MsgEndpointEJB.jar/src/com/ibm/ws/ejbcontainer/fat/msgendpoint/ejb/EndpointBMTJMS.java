/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb;

import javax.annotation.Resource;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;

public class EndpointBMTJMS implements MessageListener {
    @Resource
    MessageDrivenContext myMessageDrivenCtx;

    /**
     * @see javax.jms.MessageListener#onMessage(Message)
     */
    @Override
    public void onMessage(Message message) {
        if (AdapterUtil.inGlobalTransaction()) {
            System.out.println("EndpointBMTJMS is in a global transaction");
        } else {
            System.out.println("EndpointBMTJMS is in a local transaction");
        }

        try {
            System.out.println("--onMessage: EndpointBMTJMS got the message :" + ((TextMessage) message).getText());
        } catch (Exception e) {
        }
    }

    @Resource
    public void setMessageDrivenContext(MessageDrivenContext ctx) {
        // Nothing to do
    }

    /**
     * This method is called when the Message Driven Bean is created. It currently does nothing.
     *
     * @exception javax.ejb.CreateException
     * @exception javax.ejb.EJBException
     */
    public void ejbCreate() {
        System.out.println("--EndpointBMTJMS: ejbCreate");
    }

    /**
     * This method is called when the Message Driven Bean is removed from the server.
     *
     * @exception javax.ejb.EJBException
     */
    public void ejbRemove() {
    }
}
