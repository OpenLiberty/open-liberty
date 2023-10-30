/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package jmscontextinject.ejb;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSDestinationDefinition;

@Stateless
@JMSConnectionFactoryDefinition(name ="java:comp/env/injectedConnectionFactory", className = "javax.jms.JMSConnectionFactory",
                                properties = {"remoteServerAddress=localhost:${bvt.prop.jms.1}:BootstrapBasicMessaging"} )
@JMSDestinationDefinition(name="java:comp/env/injectedQueue",interfaceName="javax.jms.Queue",destinationName="QUEUE1")

public class JMSDefinitionBean {

    @Resource(lookup="java:comp/env/injectedConnectionFactory")
    ConnectionFactory connectionFactory;
    @Resource(lookup="java:comp/env/injectedQueue")
    Queue queue;
   
    public void sendMessage(String text) throws JMSException {
        System.out.println("Sending message [ " + text + " ]");

        try (JMSContext jmsContext = connectionFactory.createContext()){
            Message sentMessage = jmsContext.createTextMessage(text);
            jmsContext.createProducer().send(queue, sentMessage);
            System.out.println("Sent message [ " + text + " ]");
        }
    }
}
