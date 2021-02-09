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