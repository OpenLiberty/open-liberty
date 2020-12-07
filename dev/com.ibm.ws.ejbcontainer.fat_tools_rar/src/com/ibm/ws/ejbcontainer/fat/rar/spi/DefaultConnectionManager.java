/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.spi;

import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

/**
 * DefaultConnectionManager class.
 */
public class DefaultConnectionManager implements ConnectionManager {
    private final static String CLASSNAME = DefaultConnectionManager.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private ManagedConnection mc = null;
    private ConnectionRequestInfo cri = null;
    private ManagedConnectionFactory mcf = null;

    public DefaultConnectionManager() {
        svLogger.entering(CLASSNAME, "<init>");
        svLogger.exiting(CLASSNAME, "<init>", this);
    }

    /**
     * @see javax.resource.spi.ConnectionManager#allocateConnection(ManagedConnectionFactory, ConnectionRequestInfo)
     */
    @Override
    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cri) throws ResourceException {
        svLogger.entering(CLASSNAME, "allocateConnection", new Object[] { mcf, cri });

        this.mcf = mcf;
        this.cri = cri;

        if (mc == null) {
            mc = mcf.createManagedConnection(null, cri);
        }

        svLogger.exiting(CLASSNAME, "allocateConnection");
        return mc.getConnection(null, cri);
    }
}