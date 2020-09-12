/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jmscontextinject.ejb;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSSessionMode;
import javax.jms.Message;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Stateless
public class SampleSecureStatelessBean {

    @Inject
    @JMSConnectionFactory("java:comp/env/jndi_JMS_BASE_QCF")
    @JMSSessionMode(JMSContext.SESSION_TRANSACTED)
    JMSContext jmscontext;

    public String hello() {
        return "EJBMessage";
    }

    public void sendMessage(String text) {
        System.out.println("Sending message [ " + text + " ]");

        try {
            Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q1");
            Message msg = jmscontext.createTextMessage(text);
            jmscontext.createProducer().send(queue, msg);
            System.out.println("Sent message [ " + text + " ]");

        } catch ( NamingException ex ) {
            ex.printStackTrace();
        }
    }
}
