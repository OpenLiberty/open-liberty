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
package io.openliberty.springboot.support.version40.test.jms.app;

import javax.naming.NamingException;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class MessageReceiver {
	
	private final String msgDestination = "DEV.QUEUE.1";
	
    @JmsListener(destination = msgDestination, containerFactory = "myListenerContainerFactory")
    public void receiveMessage(String message) throws NamingException {
    	System.out.println("Received message: " + message);	
    }
}
