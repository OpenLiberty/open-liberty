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

public class EndpointCMTJMS implements MessageListener {
    @Resource
    MessageDrivenContext myMessageDrivenCtx;

    @Override
    public void onMessage(Message message) {
        try {
            if (AdapterUtil.inGlobalTransaction()) {
                System.out.println("EndpointCMTJMS is in a global transaction");
            } else {
                System.out.println("EndpointCMTJMS is in a local transaction");
            }
            System.out.println("--onMessage: EndpointCMTJMS got the message :" + ((TextMessage) message).getText());
        } catch (Exception e) {
        }
    }

    public void onMessage(String message) {
        if (AdapterUtil.inGlobalTransaction()) {
            System.out.println("EndpointCMTJMS is in a global transaction");
        } else {
            System.out.println("EndpointCMTJMS is in a local transaction");
        }
        System.out.println("--onMessage: EndpointCMTJMS got the message :" + message);
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
        System.out.println("--EndpointCMTJMS: ejbCreate");
    }

    /**
     * This method is called when the Message Driven Bean is removed from the server.
     *
     * @exception javax.ejb.EJBException
     */
    public void ejbRemove() {
    }
}
