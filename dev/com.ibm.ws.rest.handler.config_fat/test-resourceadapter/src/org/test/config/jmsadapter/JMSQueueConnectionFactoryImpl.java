/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package org.test.config.jmsadapter;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

public class JMSQueueConnectionFactoryImpl extends JMSConnectionFactoryImpl implements QueueConnectionFactory {
    @Override
    public QueueConnection createQueueConnection() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueueConnection createQueueConnection(String user, String password) throws JMSException {
        throw new UnsupportedOperationException();
    }
}
