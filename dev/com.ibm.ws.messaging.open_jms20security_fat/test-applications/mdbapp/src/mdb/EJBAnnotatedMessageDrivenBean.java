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

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven(name = "MDBQueue", activationConfig = {
                                                       @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),

                                                       @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "queue/test"),

                                                       @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),

                                                       @ActivationConfigProperty(propertyName = "connectionFactoryLookup", propertyValue = "newQCF"),

                                                       @ActivationConfigProperty(propertyName = "userName", propertyValue = "user1"),

                                                       @ActivationConfigProperty(propertyName = "password", propertyValue = "user1pwd")

})
public class EJBAnnotatedMessageDrivenBean implements MessageListener {

    @Override
    public void onMessage(Message message) {

        try {
            TextMessage msg = (TextMessage) message;
            System.out.println((new StringBuilder()).append(message).toString());
            System.out.println("Message received on Annotated MDB: "
                               + msg.getText());
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
