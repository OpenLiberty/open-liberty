/*
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package jmsmdb.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.inject.Inject;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.TextMessage;

@MessageDriven
public class RDCMessageDrivenBean implements MessageListener {
    static int i = 0;

    @Resource
    MessageDrivenContext ejbcontext;

    @Inject
    @JMSConnectionFactory("jndi_JMS_BASE_QCF")
    JMSContext context;

    @Resource(name = "jndi_INPUT_Q")
    Queue replyQueue;

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
            System.out.println("RDCMessageDrivenBean: The Message No = " + i);

            if (i == 1) {
                System.out.println("Clearing message body");
                message.clearBody();
                throw new RuntimeException();
            }
            if (i == 2) {
                String text = ((TextMessage) message).getText();
                boolean redelivered = message.getBooleanProperty("JMSRedelivered");
                int deliveryCount = message.getIntProperty("JMSXDeliveryCount");
                System.out.println("The message text upon redelivery is " + text);
                System.out.println("JMSRedelivered value is set " + " " + message.getBooleanProperty("JMSRedelivered"));
                System.out.println("JMSXDeliveryCount value is " + (message.getIntProperty("JMSXDeliveryCount")));

                sendToReplyQueue(text, redelivered, deliveryCount);
            }

        } catch (JMSException x) {
            x.printStackTrace();
        }

    }

    public void sendToReplyQueue(String text, boolean redelivered, int deliveryCount) {

        try {

            JMSProducer sender = context.createProducer();

            MapMessage reply = context.createMapMessage();
            reply.setStringProperty("msgText", text);
            reply.setBooleanProperty("redelivered", redelivered);
            reply.setIntProperty("deliveryCount", deliveryCount);
            System.out.println("MDB sending reply: " + reply);
            sender.send(replyQueue, reply);
            System.out.println("MDB sent reply");

        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}
