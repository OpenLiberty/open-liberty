/*
 * Copyright (c) 2015,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package mdbapp1.ejb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven(
    name = "MDBTopic",
    activationConfig = {
        @ActivationConfigProperty(
            propertyName = "destinationType",
            propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(
            propertyName = "destinationLookup",
            propertyValue = "eis/topic2"),
        @ActivationConfigProperty(
            propertyName = "subscriptionDurability",
            propertyValue = "Durable"),
        @ActivationConfigProperty(
            propertyName = "subscriptionName",
            propertyValue = "SubName1"),
        @ActivationConfigProperty(
            propertyName = "connectionFactoryLookup",
            propertyValue = "eis/tcf"),
        @ActivationConfigProperty(
            propertyName = "acknowledgeMode",
            propertyValue = "Auto-acknowledge"),
    })
public class EJBAnnotatedTopicMessageDrivenBean implements MessageListener {
    @Override
    public void onMessage(Message message) {
        try {
            TextMessage msg = (TextMessage) message;
            System.out.println((new StringBuilder()).append(message).toString());
            System.out.println("Message received on Annotated Topic MDB: " + msg.getText());
        } catch ( JMSException e ) {
            e.printStackTrace();
        }
    }
}
