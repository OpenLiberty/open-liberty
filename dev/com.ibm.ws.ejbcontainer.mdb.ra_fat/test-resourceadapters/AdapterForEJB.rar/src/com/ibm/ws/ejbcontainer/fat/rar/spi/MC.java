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
import javax.security.auth.Subject;
import javax.sql.PooledConnection;

/**
 * This managed connection supports both lazy enlistable optimization and laze associatable
 * optimization.
 */
public class MC extends ManagedConnectionImpl {
    private final static String CLASSNAME = MC.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * Constructor for MC.
     *
     * @param mcf
     * @param pconn
     * @param conn
     * @param sub
     * @param cxRequestInfo
     * @throws ResourceException
     */
    public MC(ManagedConnectionFactoryImpl mcf, PooledConnection pconn, Connection conn, Subject sub, ConnectionRequestInfoImpl cxRequestInfo) throws ResourceException {
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
        return false;
    }
}