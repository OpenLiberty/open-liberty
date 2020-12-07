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
//  Date       pgmr       reason   Description
//  --------   -------    ------   ---------------------------------
//  01/07/03   jitang	  d155877  create
//  ----------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.spi;

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