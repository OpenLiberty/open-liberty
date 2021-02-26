/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tra.outbound.base;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tra.SimpleRAImpl;

/**
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
@SuppressWarnings("serial")
public class ConnectionManagerBase implements ConnectionManager {

    private static final TraceComponent tc = Tr.register(ConnectionManagerBase.class, SimpleRAImpl.RAS_GROUP, null);

    /**
     * @see javax.resource.spi.ConnectionManager#allocateConnection(javax.resource.spi.ManagedConnectionFactory, javax.resource.spi.ConnectionRequestInfo)
     */
    @Override
    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo reqInfo) throws ResourceException {

        final String methodName = "allocateConnection";
        Tr.entry(tc, methodName, new Object[] { mcf, reqInfo });

        ManagedConnection mc = mcf.createManagedConnection(null, reqInfo); // null javax.security.auth.Subject

        // Returns a new application-level connection handle.
        Object connection = mc.getConnection(null, reqInfo); // null javax.security.auth.Subject

        Tr.exit(tc, methodName, connection);
        return connection;
    }

}
