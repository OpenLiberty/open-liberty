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

import java.util.logging.Logger;

import javax.transaction.xa.XAResource;

/**
 * Implementation class for XAResource. <p>
 */
public class RecoverableXAResourceImpl extends XAResourceImpl {
    private final static String CLASSNAME = RecoverableXAResourceImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static int _recoveryToken = 1;

    /**
     * @param xaRes
     * @param mc
     */
    public RecoverableXAResourceImpl(XAResource xaRes, ManagedConnectionImpl mc) {
        super(xaRes, mc);
        svLogger.entering(CLASSNAME, "<init>", new Object[] { xaRes, mc });
        svLogger.exiting(CLASSNAME, "<init>");
    }

    public int getXARecoveryToken() {
        return _recoveryToken++;
    }
}