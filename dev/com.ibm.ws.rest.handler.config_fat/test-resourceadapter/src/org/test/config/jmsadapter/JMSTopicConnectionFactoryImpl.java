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

import javax.jms.JMSException;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

public class JMSTopicConnectionFactoryImpl extends JMSConnectionFactoryImpl implements TopicConnectionFactory {
    @Override
    public TopicConnection createTopicConnection() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TopicConnection createTopicConnection(String user, String password) throws JMSException {
        throw new UnsupportedOperationException();
    }
}
