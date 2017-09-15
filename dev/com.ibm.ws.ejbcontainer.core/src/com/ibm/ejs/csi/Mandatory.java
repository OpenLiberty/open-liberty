/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.csi;

import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CSITransactionRequiredException;
import com.ibm.websphere.csi.EJBKey;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * <code> Mandatory </code> implements TX_MANDATORY semantics. A global
 * transaction must already be active or else exception is thrown. No
 * local transaction is started.
 **/

final class Mandatory extends TranStrategy {

    private static final TraceComponent tc = Tr.register(Mandatory.class
                                                         , "EJBContainer"
                                                         , "com.ibm.ejs.container.container"); //p111002.4

    Mandatory(TransactionControlImpl txCtrl) {
        super(txCtrl);
    }

    @Override
    TxCookieImpl preInvoke(EJBKey key, EJBMethodInfoImpl methodInfo)
                    throws CSIException
    {
        final boolean entryEnabled =
                        TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (entryEnabled) {
            Tr.entry(tc, "preInvoke");
        }

        if (!globalTxExists(true)) {
            throw new CSITransactionRequiredException("global tx required");
        }

        TxCookieImpl cookie = new TxCookieImpl(false, false, this, null);

        // Suspend a local tran, if it exists (null is returned if not).
        // This should never be required for the Mandatory strategy,
        // but this code tolerates scenarios where applications use
        // internals to begin/resume global trans on threads that already
        // have a local transaction.                                   PI10351
        cookie.suspendedLocalTx = suspendLocalTx();

        if (entryEnabled) {
            Tr.exit(tc, "preInvoke");
        }

        return cookie;
    }

    @Override
    void postInvoke(EJBKey key, TxCookieImpl txCookie, EJBMethodInfoImpl methodInfo)
                    throws CSIException
    {
        final boolean entryEnabled =
                        TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (entryEnabled) {
            Tr.entry(tc, "postInvoke");
        }

        if (txCookie.beginner) {

            //d135218 - switch to Tr.error instead of fatal
            Tr.error(tc, "PROTOCOL_ERROR_IN_CONTAINER_TRANSACTION_CNTR0050E"); //p111002.4
        }

        super.postInvoke(key, txCookie, methodInfo);

        if (entryEnabled) {
            Tr.exit(tc, "postInvoke");
        }
    }
} // Mandatory
