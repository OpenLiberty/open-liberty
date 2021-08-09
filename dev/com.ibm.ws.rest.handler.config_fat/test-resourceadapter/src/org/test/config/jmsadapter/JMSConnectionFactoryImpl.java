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
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;

public class JMSConnectionFactoryImpl implements ConnectionFactory {
    @Override
    public Connection createConnection() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection createConnection(String user, String password) throws JMSException {
        throw new UnsupportedOperationException();
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
}
