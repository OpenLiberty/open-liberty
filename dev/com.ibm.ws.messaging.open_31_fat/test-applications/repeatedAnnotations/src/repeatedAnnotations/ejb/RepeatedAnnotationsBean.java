/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package repeatedAnnotations.ejb;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConnectionFactoryDefinitions;
import jakarta.jms.JMSConnectionFactoryDefinition;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinitions;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.Topic;

@Stateless
@JMSConnectionFactoryDefinitions({
    @JMSConnectionFactoryDefinition(name ="java:comp/env/injectedConnectionFactory1", className = "jakarta.jms.JMSConnectionFactory",
                                    properties = {"remoteServerAddress=localhost:${bvt.prop.jms}:BootstrapBasicMessaging"}),
    @JMSConnectionFactoryDefinition(name ="java:comp/env/injectedConnectionFactory2", className = "jakarta.jms.JMSConnectionFactory",
                                    properties = {"remoteServerAddress=localhost:${bvt.prop.jms}:BootstrapBasicMessaging"})
})
@JMSDestinationDefinitions({
    @JMSDestinationDefinition(name="java:comp/env/injectedQueue",interfaceName="jakarta.jms.Queue",destinationName="Queue1"),
    @JMSDestinationDefinition(name="java:comp/env/injectedTopic",interfaceName="jakarta.jms.Topic",destinationName="Topic1")
})

//@JMSConnectionFactoryDefinition(name = "java:comp/env/injectedConnectionFactory1", className = "jakarta.jms.JMSQueueConnectionFactory")
//    @JMSConnectionFactoryDefinition(name ="java:comp/env/injectedConnectionFactory2", className = "jakarta.jms.JMSConnectionFactory",
//                                    properties = {"remoteServerAddress=localhost:${bvt.prop.jms}:BootstrapBasicMessaging"})
//@JMSDestinationDefinition(name = "java:comp/env/injectedQueue", interfaceName = "jakarta.jms.Queue", destinationName = "Queue1")
//    @JMSDestinationDefinition(name="java:comp/env/injectedTopic",interfaceName="jakarta.jms.Topic",destinationName="Topic1")

public class RepeatedAnnotationsBean {

    @Resource(lookup = "java:comp/env/injectedConnectionFactory1")
    ConnectionFactory connectionFactory1;
    @Resource(lookup = "java:comp/env/injectedQueue")
    Queue queue;

    public void sendQueueMessage(String text) throws JMSException {
        try (JMSContext jmsContext = connectionFactory1.createContext()) {
            Message sentMessage = jmsContext.createTextMessage(text);
            jmsContext.createProducer().send(queue, sentMessage);
        }
    }

    @Resource(lookup = "java:comp/env/injectedConnectionFactory2")
    ConnectionFactory connectionFactory2;
    @Resource(lookup = "java:comp/env/injectedTopic")
    Topic topic;

    public void sendTopicMessage(String text) throws JMSException {
        try (JMSContext jmsContext = connectionFactory2.createContext()) {
            Message sentMessage = jmsContext.createTextMessage(text);
            jmsContext.createProducer().send(topic, sentMessage);
        }
    }
}
