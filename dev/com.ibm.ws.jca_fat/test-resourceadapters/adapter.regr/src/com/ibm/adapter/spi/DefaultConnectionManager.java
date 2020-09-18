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

package com.ibm.adapter.spi;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * DefaultConnectionManager class.
 */
public class DefaultConnectionManager implements ConnectionManager {

    private static final TraceComponent tc = Tr.register(DefaultConnectionManager.class);

    private ManagedConnection mc = null;
    private ConnectionRequestInfo cri = null;
    private ManagedConnectionFactory mcf = null;

    public DefaultConnectionManager() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);

    }

    /**
     * @see javax.resource.spi.ConnectionManager#allocateConnection(ManagedConnectionFactory, ConnectionRequestInfo)
     */
    @Override
    public Object allocateConnection(
                                     ManagedConnectionFactory mcf,
                                     ConnectionRequestInfo cri) throws ResourceException {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "allocateConnection", new Object[] { mcf, cri });

        this.mcf = mcf;
        this.cri = cri;

        if (mc == null) {
            mc = mcf.createManagedConnection(null, cri);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "allocateConnection");

        return mc.getConnection(null, cri);
    }

}
