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

import javax.transaction.xa.XAResource;

import com.ibm.ejs.ras.Tr;

/**
 * Implementation class for XAResource.
 * <p>
 */
public class RecoverableXAResourceImpl extends XAResourceImpl {

    private static int _recoveryToken = 1;

    /**
     * @param xaRes
     * @param mc
     */
    public RecoverableXAResourceImpl(XAResource xaRes, ManagedConnectionImpl mc) {

        super(xaRes, mc);

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "<init>", new Object[] { xaRes, mc });
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "<init>");
        }

    }

    public int getXARecoveryToken() {
        return _recoveryToken++;
    }

} // end class RecoverableXAResourceImpl
