/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package jmsConsumer.mdb;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven
public class RDC2MessageDrivenBean implements MessageListener {
    
    static AtomicLong invocationCount = new AtomicLong(0);  
    
    /*
     * onMessage Method for testing RedeliveryCount.
     * Runtime Exception is thrown to ensure redelivery of the message.
     * 
     * Message redelivery will be happen until maxRedeliveryCount is reached, 
     * default is 5, configured to 2 for QUEUE1.
     */
    @Override
    public void onMessage(Message message) {
        
        long i = invocationCount.incrementAndGet();
        try {
            String text = ((TextMessage) message).getText();
            System.out.println("Message=" + i
                               + ",JMSXDeliveryCount=" + message.getIntProperty("JMSXDeliveryCount")
                               + ",JMSRedelivered=" + message.getBooleanProperty("JMSRedelivered")
                               + ",text=" + text);
        } catch (Exception exception) {
            throw new RuntimeException("Rethrow" ,exception);
        }
        throw new RuntimeException("Test RuntimeException");
    }
}