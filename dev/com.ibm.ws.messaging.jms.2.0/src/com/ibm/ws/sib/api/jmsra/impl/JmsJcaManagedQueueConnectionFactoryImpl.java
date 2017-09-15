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
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedQueueConnectionFactory;

/**
 * Managed connection factory for point-to-point domain.
 */
public final class JmsJcaManagedQueueConnectionFactoryImpl extends
        JmsJcaManagedConnectionFactoryImpl implements
        JmsJcaManagedQueueConnectionFactory {

    static final String QUEUE_CONN_FACTORY_TYPE = "javax.jms.QueueConnectionFactory";

    private static final long serialVersionUID = 114300589728595741L;

    ConnectionFactory createJmsConnFactory(
            final JmsRAFactoryFactory jmsFactory,
            final JmsJcaConnectionFactoryImpl connectionFactory) {

        return jmsFactory.createQueueConnectionFactory(connectionFactory);

    }

    ConnectionFactory createJmsConnFactory(
            final JmsRAFactoryFactory jmsFactory,
            final JmsJcaConnectionFactoryImpl connectionFactory,
            final JmsJcaManagedConnectionFactory managedConnectionFactory) {

        return jmsFactory.createQueueConnectionFactory(connectionFactory,
                (JmsJcaManagedQueueConnectionFactory) managedConnectionFactory);

    }

    /**
     * Returns the connection type.
     * 
     * @return the connection type
     */
    String getConnectionType() {
        return QUEUE_CONN_FACTORY_TYPE;
    }

}
