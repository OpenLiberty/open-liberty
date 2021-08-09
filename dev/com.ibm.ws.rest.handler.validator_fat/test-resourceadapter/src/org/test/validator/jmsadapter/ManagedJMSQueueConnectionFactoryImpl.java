/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.validator.jmsadapter;

import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;

/**
 * Mock managed connection factory which is only used to supply configuration.
 */
@ConnectionDefinition(connectionFactory = QueueConnectionFactory.class,
                      connectionFactoryImpl = JMSConnectionFactoryImpl.class,
                      connection = QueueConnection.class,
                      connectionImpl = JMSConnectionImpl.class)
public class ManagedJMSQueueConnectionFactoryImpl extends ManagedJMSConnectionFactoryImpl {
    private static final long serialVersionUID = 1L;

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(null);
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new JMSConnectionFactoryImpl(cm, this);
    }
}
