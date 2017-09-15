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
import com.ibm.websphere.csi.EJBKey;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * <code> Supports </code> implements TX_SUPPORTS semantics. If
 * there is a current global transaction context, the bean becomes
 * a participant. Otherwise, behave like TX_NOT_SUPPORTED (i.e.
 * begin a local transaction and commit it at postInvoke).
 **/

final class Supports extends TranStrategy {
    //d121558
    private static final TraceComponent tc = Tr.register(Supports.class, "EJBContainer", "com.ibm.ejs.container.container");

    Supports(TransactionControlImpl txCtrl) {
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

        // LIDB2446: beginLocalTx returns cookie
        TxCookieImpl cookie = null;
        if (!globalTxExists(true)) {
            cookie = beginLocalTx(key, methodInfo, null);
        }
        else {
            cookie = new TxCookieImpl(false, false, this, null);

            // Suspend a local tran, if it exists (null is returned if not).
            // This should not be required since a global tran is present,
            // but this code tolerates scenarios where applications use
            // internals to begin/resume global trans on threads that already
            // have a local transaction.                               PI10351
            cookie.suspendedLocalTx = suspendLocalTx();
        }

        if (entryEnabled) {
            Tr.exit(tc, "preInvoke");
        }

        return cookie;
    }

} // Supports
