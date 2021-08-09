/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mdb;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven
public class RDC2MessageDrivenBean implements MessageListener {
    static int i = 0;

    @Resource
    MessageDrivenContext ejbcontext;

    @SuppressWarnings("unused")
    @Resource
    private void setMessageDrivenContext(EJBContext ejbcontext) {
        System.out.println("TODO: remove this if we don't need it: setMessageDrivenContext invoked");
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("TODO: remove this if we don't need it: postConstruct invoked");
    }

    /*
     * onMessage Method for testing RedeliveryCount.
     * Runtime Exception is thrown to ensure redelivery of message.
     * Redelivery will be hapened till max value=5.
     */
    @Override
    public void onMessage(Message message) {
        i++;
        try {
            if (i == 1) {
                System.out.println("Message=1: Clearing message body");
                message.clearBody();
            }
            String text = ((TextMessage) message).getText();
            System.out.println("Message=" + i
                               + ",JMSXDeliveryCount=" + message.getIntProperty("JMSXDeliveryCount")
                               + ",JMSRedelivered=" + message.getBooleanProperty("JMSRedelivered")
                               + ",text=" + text);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
        throw new RuntimeException();
    }
}
