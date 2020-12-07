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
import javax.resource.spi.LazyEnlistableManagedConnection;
import javax.security.auth.Subject;
import javax.sql.PooledConnection;

/**
 * This managed connection supports both lazy enlistable optimization and lazy associatable
 * optimization.
 */
public class LazyEnlistableLazyAssociatableMC extends LazyAssociatableMC implements LazyEnlistableManagedConnection {
    private final static String CLASSNAME = LazyEnlistableLazyAssociatableMC.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * Constructor for LazyEnlistableLazyAssociatableMC.
     * 
     * @param mcf
     * @param pconn
     * @param conn
     * @param sub
     * @param cxRequestInfo
     * @throws ResourceException
     */
    public LazyEnlistableLazyAssociatableMC(ManagedConnectionFactoryImpl mcf, PooledConnection pconn, Connection conn, Subject sub, ConnectionRequestInfoImpl cxRequestInfo) throws ResourceException {
        super(mcf, pconn, conn, sub, cxRequestInfo);
        svLogger.entering(CLASSNAME, "<init>", this);
    }

    /**
     * @return boolean Whether the MC supports Lazy Enlistable optimization.
     */
    @Override
    public boolean isLazyEnlistable() {
        return true;
    }
}