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
package org.test.config.jmsadapter;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.resource.spi.ConnectionManager;

public class JMSTopicConnectionFactoryImpl extends JMSConnectionFactoryImpl implements TopicConnectionFactory {
    final ConnectionManager cm;
    final ManagedJMSTopicConnectionFactoryImpl mcf;

    JMSTopicConnectionFactoryImpl(ConnectionManager cm, ManagedJMSTopicConnectionFactoryImpl mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @Override
    public Connection createConnection() throws JMSException {
        return createTopicConnection();
    }

    @Override
    public Connection createConnection(String user, String password) throws JMSException {
        return createTopicConnection(user, password);
    }

    @Override
    public TopicConnection createTopicConnection() throws JMSException {
        return createTopicConnection(null, null);
    }

    @Override
    public TopicConnection createTopicConnection(String user, String password) throws JMSException {
        return new JMSTopicConnectionImpl(this);
    }
}
