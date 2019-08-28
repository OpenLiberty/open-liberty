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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.ResourceAllocationException;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

import org.test.validator.adapter.ConnectionSpecImpl;

public class JMSConnectionFactoryImpl implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory {
    final ConnectionManager cm;
    final ManagedJMSConnectionFactoryImpl mcf;

    JMSConnectionFactoryImpl(ConnectionManager cm, ManagedJMSConnectionFactoryImpl mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @Override
    public Connection createConnection() throws JMSException {
        return createConnection(null, null);
    }

    @Override
    public JMSConnectionImpl createConnection(String user, String password) throws JMSException {
        ConnectionSpecImpl conSpec = new ConnectionSpecImpl();
        conSpec.setConnectionImplClass(JMSConnectionImpl.class.getName());
        try {
            return (JMSConnectionImpl) cm.allocateConnection(mcf, conSpec.createConnectionRequestInfo());
        } catch (ResourceException x) {
            throw (JMSException) new ResourceAllocationException(x.getMessage(), x.getErrorCode()).initCause(x);
        }
    }

    @Override
    public JMSContext createContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JMSContext createContext(String user, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JMSContext createContext(String user, String password, int sessionMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueueConnection createQueueConnection() throws JMSException {
        return createQueueConnection(null, null);
    }

    @Override
    public QueueConnection createQueueConnection(String user, String password) throws JMSException {
        return createConnection(user, password);
    }

    @Override
    public TopicConnection createTopicConnection() throws JMSException {
        return createTopicConnection(null, null);
    }

    @Override
    public TopicConnection createTopicConnection(String user, String password) throws JMSException {
        return createConnection(user, password);
    }
}
