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
//  06/16/03   jitang     LIDB2110.31  create - Provide J2C 1.5 resource adapter
//
//  --------------------------------------------------------------------
package com.ibm.websphere.ejbcontainer.test.rar.spi;

import java.sql.Connection;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.DissociatableManagedConnection;
import javax.security.auth.Subject;
import javax.sql.PooledConnection;

import com.ibm.websphere.ejbcontainer.test.rar.core.Reassociateable;

/**
 * This managed connection supports both lazy enlistable optimization and lazy associatable
 * optimization.
 */
public class LazyAssociatableMC extends ManagedConnectionImpl implements DissociatableManagedConnection {
    private final static String CLASSNAME = LazyAssociatableMC.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * Constructor for LazyAssociatableMC.
     * 
     * @param mcf
     * @param pconn
     * @param conn
     * @param sub
     * @param cxRequestInfo
     * @throws ResourceException
     */
    public LazyAssociatableMC(ManagedConnectionFactoryImpl mcf, PooledConnection pconn, Connection conn, Subject sub, ConnectionRequestInfoImpl cxRequestInfo) throws ResourceException {
        super(mcf, pconn, conn, sub, cxRequestInfo);
        svLogger.entering(CLASSNAME, "<init>", this);
    }

    /**
     * @return boolean Whether the MC supports Lazy Associatable optimization.
     */
    @Override
    public boolean isLazyAssociatable() {
        return true;
    }

    /**
     * @return boolean Whether the MC supports Lazy Enlistable optimization.
     */
    @Override
    public boolean isLazyEnlistable() {
        return false;
    }

    /**
     * Dissociate all connection handles from this ManagedConnection, transitioning the handles
     * to an inactive state where are not associated with any ManagedConnection. Processing
     * continues when errors occur. All errors are logged, and the first error is save to be
     * thrown when processing completes. [Method added in LIDB2110.16]
     * 
     * @throws ResourceException the first error to occur while dissociating the handles.
     */
    @Override
    public synchronized void dissociateConnections() throws ResourceException {
        svLogger.entering(CLASSNAME, "dissociateConnections", this);

        // The first exception to occur while dissociating connection handles.
        ResourceException firstX = null;

        // Indicate that we are cleaning up handles, so we know not to send events for
        // operations done in the cleanup. [d138049]
        cleaningUpHandles = true;

        for (int i = handlesInUse.size() - 1; i >= 0; i--)
            try {
                // The handle.dissociate method will signal us back to remove our reference to
                // the handle.
                ((Reassociateable) handlesInUse.get(i)).dissociate();
            } catch (ResourceException dissociationX) {
                if (firstX == null)
                    firstX = dissociationX;
            }

        cleaningUpHandles = false;

        if (firstX == null) {
            svLogger.exiting(CLASSNAME, "dissociateConnections");
        }
        else {
            svLogger.exiting(CLASSNAME, "dissociateConnections", firstX);
            throw firstX;
        }
    }
}