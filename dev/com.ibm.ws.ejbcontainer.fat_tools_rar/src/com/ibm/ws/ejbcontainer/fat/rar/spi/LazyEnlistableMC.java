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

import java.sql.Connection;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.LazyEnlistableManagedConnection;
import javax.security.auth.Subject;
import javax.sql.PooledConnection;

/**
 * This managed connection supports both lazy enlistable optimization and laze associatable
 * optimization.
 */
public class LazyEnlistableMC extends ManagedConnectionImpl implements LazyEnlistableManagedConnection {
    private final static String CLASSNAME = LazyEnlistableMC.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * Constructor for LazyEnlistableMC.
     *
     * @param mcf
     * @param pconn
     * @param conn
     * @param sub
     * @param cxRequestInfo
     * @throws ResourceException
     */
    public LazyEnlistableMC(ManagedConnectionFactoryImpl mcf, PooledConnection pconn, Connection conn, Subject sub,
                            ConnectionRequestInfoImpl cxRequestInfo) throws ResourceException {
        super(mcf, pconn, conn, sub, cxRequestInfo);
        svLogger.entering(CLASSNAME, "<init>", this);
    }

    /**
     * @return boolean Whether the MC supports Lazy Associatable optimization.
     */
    @Override
    public boolean isLazyAssociatable() {
        return false;
    }

    /**
     * @return boolean Whether the MC supports Lazy Enlistable optimization.
     */
    @Override
    public boolean isLazyEnlistable() {
        return true;
    }
}