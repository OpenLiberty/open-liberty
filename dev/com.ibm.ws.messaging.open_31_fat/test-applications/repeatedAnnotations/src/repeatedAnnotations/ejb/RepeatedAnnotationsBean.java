/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package repeatedAnnotations.ejb;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactoryDefinitions;
import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinitions;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;

@Stateless
@JMSConnectionFactoryDefinitions({
    @JMSConnectionFactoryDefinition(name ="java:comp/env/injectedConnectionFactory1", className = "javax.jms.JMSConnectionFactory",
                                    properties = {"remoteServerAddress=localhost:${bvt.prop.jms}:BootstrapBasicMessaging"}),
    @JMSConnectionFactoryDefinition(name ="java:comp/env/injectedConnectionFactory2", className = "javax.jms.JMSConnectionFactory",
                                    properties = {"remoteServerAddress=localhost:${bvt.prop.jms}:BootstrapBasicMessaging"})
})
@JMSDestinationDefinitions({
    @JMSDestinationDefinition(name="java:comp/env/injectedQueue",interfaceName="javax.jms.Queue",destinationName="Queue1"),
    @JMSDestinationDefinition(name="java:comp/env/injectedTopic",interfaceName="javax.jms.Topic",destinationName="Topic1")
})

//@JMSConnectionFactoryDefinition(name = "java:comp/env/injectedConnectionFactory1", className = "javax.jms.JMSQueueConnectionFactory")
//    @JMSConnectionFactoryDefinition(name ="java:comp/env/injectedConnectionFactory2", className = "javax.jms.JMSConnectionFactory",
//                                    properties = {"remoteServerAddress=localhost:${bvt.prop.jms}:BootstrapBasicMessaging"})
//@JMSDestinationDefinition(name = "java:comp/env/injectedQueue", interfaceName = "javax.jms.Queue", destinationName = "Queue1")
//    @JMSDestinationDefinition(name="java:comp/env/injectedTopic",interfaceName="javax.jms.Topic",destinationName="Topic1")

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
