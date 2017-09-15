/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jmsra.impl;

import javax.jms.ConnectionFactory;

import com.ibm.ws.sib.api.jms.JmsRAFactoryFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedTopicConnectionFactory;

/**
 * Managed connection factory for the publish-subscribe domain.
 */
public final class JmsJcaManagedTopicConnectionFactoryImpl extends
        JmsJcaManagedConnectionFactoryImpl implements
        JmsJcaManagedTopicConnectionFactory {

    static final String TOPIC_CONN_FACTORY_TYPE = "javax.jms.TopicConnectionFactory";

    private static final long serialVersionUID = 645068421741658829L;

    ConnectionFactory createJmsConnFactory(
            final JmsRAFactoryFactory jmsFactory,
            final JmsJcaConnectionFactoryImpl connectionFactory) {

        return jmsFactory.createTopicConnectionFactory(connectionFactory);

    }

    ConnectionFactory createJmsConnFactory(
            final JmsRAFactoryFactory jmsFactory,
            final JmsJcaConnectionFactoryImpl connectionFactory,
            final JmsJcaManagedConnectionFactory managedConnectionFactory) {

        return jmsFactory.createTopicConnectionFactory(connectionFactory,
                (JmsJcaManagedTopicConnectionFactory) managedConnectionFactory);

    }

    /**
     * Returns the connection type.
     * 
     * @return the connection type
     */
    String getConnectionType() {
        return TOPIC_CONN_FACTORY_TYPE;
    }

}
