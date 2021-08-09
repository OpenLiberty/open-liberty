/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.spi;

import java.sql.SQLException;

import javax.resource.ResourceException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class RecoverableManagedConnectionImpl extends ManagedConnectionImpl {

    private static final TraceComponent tc = Tr
                    .register(ManagedConnectionImpl.class);

    /**
     * Constructor
     */
    public RecoverableManagedConnectionImpl(ManagedConnectionFactoryImpl mcf,
                                            javax.sql.PooledConnection pconn, java.sql.Connection conn,
                                            Subject sub, ConnectionRequestInfoImpl cxRequestInfo) throws ResourceException {

        super(mcf, pconn, conn, sub, cxRequestInfo);

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "<init>", new Object[] { mcf, conn, cxRequestInfo,
                                                  sub, cxRequestInfo });
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "<init>", this);
        }

    } // end ctor

    /**
     * Returns a javax.transaction.xa.XAresource instance. An application server
     * enlists this XAResource instance with the Transaction Manager if the
     * ManagedConnection instance is being used in a JTA transaction that is
     * being coordinated by the Transaction Manager.
     *
     * @return a XAResource - if the dataSource specified for this
     *         ManagedConnection is of type XADataSource, then an XAResource
     *         from the physical connection is returned wrappered in our
     *         WSRdbXaResourceImpl. If the dataSource is of type
     *         ConnectionPoolDataSource, then our wrapper
     *         WSRdbOnePhaseXaResourceImpl is returned as the connection will
     *         not be capable of returning an XAResource as it is not two phase
     *         capable.
     *
     * @exception ResourceException
     *                - Possible causes for this exception are: 1) failed to get
     *                an XAResource from the XAConnection object.
     */

    @Override
    public XAResource getXAResource() throws ResourceException {

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getXAResource", this);
        }

        if (xares != null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Returning existing XAResource", xares);
            }
        } else if (is2Phase) {
            try {
                XAResource xa = ((javax.sql.XAConnection) poolConn)
                                .getXAResource();
                // note this returns a RecoverableXAResourceImpl instead of
                // a XAResourceImpl which the parent class does
                xares = new RecoverableXAResourceImpl(xa, this);

            } catch (SQLException se) {
                if (tc.isEntryEnabled()) {
                    Tr
                                    .exit(tc,
                                          "getXAResource - failed trying to create XAResource, throwing exception");
                }
                throw new ResourceException(se.getMessage());
            }
        } else {
            // note this returns a RecoverableOnePhaseXAResourceImpl instead of
            // a OnePhaseXAResourceImpl which the parent class does
            xares = new RecoverableOnePhaseXAResourceImpl(sqlConn, this);
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getXAResource");
        }

        return xares;

    } // end class Recoverable ManagedConnectionImpl

} // end class RecoverableManagedConnectionImpl
