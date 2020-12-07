// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date       pgmr       reason       Description
//  --------   -------    ------       ---------------------------------
//  03/30/04   kak        LIDB2110-69  create
//  ----------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.security.auth.Subject;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

public class RecoverableManagedConnectionImpl extends ManagedConnectionImpl {
    private final static String CLASSNAME = RecoverableManagedConnectionImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * Constructor
     */
    public RecoverableManagedConnectionImpl(ManagedConnectionFactoryImpl mcf, PooledConnection pconn, Connection conn, Subject sub, ConnectionRequestInfoImpl cxRequestInfo) throws ResourceException {
        super(mcf, pconn, conn, sub, cxRequestInfo);
        svLogger.entering(CLASSNAME, "<init>", new Object[] { mcf, conn, cxRequestInfo, sub, cxRequestInfo });
        svLogger.exiting(CLASSNAME, "<init>", this);
    }

    /**
     * Returns a javax.transaction.xa.XAresource instance. An application server
     * enlists this XAResource instance with the Transaction Manager if the
     * ManagedConnection instance is being used in a JTA transaction that is
     * being coordinated by the Transaction Manager.
     * 
     * @return a XAResource - if the dataSource specified for this ManagedConnection
     *         is of type XADataSource, then an XAResource from the physical connection is returned
     *         wrappered in our WSRdbXaResourceImpl. If the dataSource is of type ConnectionPoolDataSource,
     *         then our wrapper WSRdbOnePhaseXaResourceImpl is returned as the connection will not be
     *         capable of returning an XAResource as it is not two phase capable.
     * 
     * @exception ResourceException - Possible causes for this exception are:
     *                1) failed to get an XAResource from the XAConnection object.
     */

    @Override
    public XAResource getXAResource() throws ResourceException {
        svLogger.entering(CLASSNAME, "getXAResource", this);

        if (xares != null) {
            svLogger.info("Returning existing XAResource: " + xares);
        }
        else if (is2Phase) {
            try {
                XAResource xa = ((XAConnection) poolConn).getXAResource();
                // note this returns a RecoverableXAResourceImpl instead of
                // a XAResourceImpl which the parent class does
                xares = new RecoverableXAResourceImpl(xa, this);
            } catch (SQLException se) {
                svLogger.exiting(CLASSNAME, "getXAResource - failed trying to create XAResource, throwing exception");
                throw new ResourceException(se.getMessage());
            }
        }
        else {
            // note this returns a RecoverableOnePhaseXAResourceImpl instead of
            // a OnePhaseXAResourceImpl which the parent class does
            xares = new RecoverableOnePhaseXAResourceImpl(sqlConn, this);
        }

        svLogger.exiting(CLASSNAME, "getXAResource");
        return xares;
    }
}