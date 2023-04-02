/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.ann.ejb;

import static javax.annotation.Resource.AuthenticationType.APPLICATION;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

/**
 * This is a Message Driven Bean (MCM04) that on receipt of a message prints the messageid and the contents
 * of the message to the standard out of the Application Server
 */
//add annotation for activationConfig props
@MessageDriven(activationConfig = {
                                    @ActivationConfigProperty(propertyName = "destination", propertyValue = "select"),
                                    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                                    @ActivationConfigProperty(propertyName = "SubscriptionDurability", propertyValue = "Durable"),
                                    @ActivationConfigProperty(propertyName = "clientId", propertyValue = "C0002"),
                                    @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "subscription2Ann"),
                                    @ActivationConfigProperty(propertyName = "MessageSelector", propertyValue = "JMSType='MCM02'")
},
               name = "MDBBeanMessageSelector")
public class MDBBeanMessageSelector implements MessageListener {
    private final static String CLASSNAME = MDBBeanMessageSelector.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // d459309, add authenticationType=APPLICATION
    @Resource(name = "jms/TestQCF", authenticationType = APPLICATION, shareable = true)
    private QueueConnectionFactory replyQueueFactory;

    @Resource(name = "jms/TestResultQueue")
    private Queue replyQueue;

    final static String BeanName = "MDBBeanMessageSelector";
    final static String replyMessage = "testMessageSelector passed";

    /**
     * The onMessage method extracts the text from the message and the messageid and passes that data into
     * the handleMessage methods.
     *
     * @param msg javax.jms.Message This should be a TextMessage.
     */
    @Override
    public void onMessage(Message msg) {
        String text = null;

        try {
            svLogger.info(BeanName + " message bean onMessage() method called");

            text = ((TextMessage) msg).getText();
            svLogger.info("onMessage() text received: " + text);

            if (msg.getJMSType().equalsIgnoreCase("MCM02")) {
                FATMDBHelper.putQueueMessage(replyMessage, replyQueueFactory, replyQueue);
            } else {
                FATMDBHelper.putQueueMessage("Should not get this message.  Message Selector failed.", replyQueueFactory, replyQueue);
            }
        } catch (Exception e) {
            svLogger.info("Exception thrown while in onMessage: " + e.toString());
            e.printStackTrace();
        }

        return;
    }
}