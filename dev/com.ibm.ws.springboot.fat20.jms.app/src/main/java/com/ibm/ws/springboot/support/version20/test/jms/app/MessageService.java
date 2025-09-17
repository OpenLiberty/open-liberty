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
package com.ibm.ws.springboot.support.version20.test.jms.app;

import javax.naming.NamingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    private final JmsTemplate jmsTemplate;
	
    private String queue = "Q1";

    @Autowired
    public MessageService(JmsTemplate template) {
        this.jmsTemplate = template;
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
