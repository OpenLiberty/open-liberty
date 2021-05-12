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
package mdbapp.ejb;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class EJBMessageDrivenBean implements MessageListener {

    @Override
    public void onMessage(Message message) {
        try {
            TextMessage msg = (TextMessage) message;
            System.out.println((new StringBuilder()).append(message).toString());
            System.out.println("Message received on EJB MDB: " + msg.getText());
        } catch ( JMSException e ) {
            e.printStackTrace();
        }
    }
}
