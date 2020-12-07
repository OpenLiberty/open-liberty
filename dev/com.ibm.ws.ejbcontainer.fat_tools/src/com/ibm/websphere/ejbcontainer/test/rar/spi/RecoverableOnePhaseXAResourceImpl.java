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
import java.util.logging.Logger;

public class RecoverableOnePhaseXAResourceImpl extends OnePhaseXAResourceImpl {
    private final static String CLASSNAME = RecoverableOnePhaseXAResourceImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static int _recoveryToken = 1;

    /**
     * @param conn
     * @param mc
     */
    public RecoverableOnePhaseXAResourceImpl(Connection conn, ManagedConnectionImpl mc) {
        super(conn, mc);
        svLogger.entering(CLASSNAME, "<init>");
        svLogger.exiting(CLASSNAME, "<init>");
    }

    public int getXARecoveryToken() {
        return _recoveryToken++;
    }
}