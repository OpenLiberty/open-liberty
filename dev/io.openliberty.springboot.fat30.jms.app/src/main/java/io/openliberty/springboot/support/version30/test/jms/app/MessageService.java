/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.springboot.support.version30.test.jms.app;

import javax.naming.NamingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import jakarta.jms.Queue;

@Service
public class MessageService {
    private final JmsTemplate jmsTemplate;
    
    private final Queue queue;

    @Autowired
    public MessageService(JmsTemplate template, Queue queue) {
        this.jmsTemplate = template;
        this.queue = queue;
    }
    
    public String send(String message) throws NamingException {
        try {
    	    System.out.println("Sending message: " + message);
    	    jmsTemplate.convertAndSend(queue, message);
            return "Message Sent: "  + message;
        } catch (JmsException ex) {
            ex.printStackTrace();
            return "Error occured during sending the message: "+ message;
        }
    }
}
