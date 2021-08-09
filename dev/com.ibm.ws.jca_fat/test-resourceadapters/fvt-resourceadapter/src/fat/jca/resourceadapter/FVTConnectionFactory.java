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
package fat.jca.resourceadapter;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;

public class FVTConnectionFactory implements ConnectionFactory {

    private final ConnectionManager cm;
    private final FVTManagedConnectionFactory mcf;

    public FVTConnectionFactory(ConnectionManager cm, FVTManagedConnectionFactory mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @Override
    public Connection createConnection() throws JMSException {
        return createConnection(mcf.getUserName(), mcf.getPassword());
    }

    @Override
    public Connection createConnection(String user, String password) throws JMSException {
        try {
            ConnectionRequestInfo cri = new FVTConnectionRequestInfo(user, password);
            return ((FVTConnection) cm.allocateConnection(mcf, cri)).init(cm);
        } catch (ResourceException x) {
            throw (JMSException) new JMSException(x.getMessage()).initCause(x);
        }
    }
}
