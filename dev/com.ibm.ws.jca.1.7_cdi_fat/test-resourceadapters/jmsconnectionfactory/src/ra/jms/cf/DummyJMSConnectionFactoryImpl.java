/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package ra.jms.cf;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;

public class DummyJMSConnectionFactoryImpl implements ConnectionFactory {
    @Override
    public Connection createConnection() throws JMSException {
        return null;
    }

    @Override
    public Connection createConnection(String user, String password) throws JMSException {
        return null;
    }

    @Override
    public JMSContext createContext() {
        return null;
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        return null;
    }

    @Override
    public JMSContext createContext(String user, String password) {
        return null;
    }

    @Override
    public JMSContext createContext(String user, String password, int sessionMode) {
        return null;
    }
}
