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

import javax.transaction.Transaction;

import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.EJBKey;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * <code> RequiresNew </code> implements TX_REQUIRES_NEW semantics.
 * Any active global transaction will be suspended and a new global
 * will begin. The suspended transaction will retain its locks and
 * the new transaction will attempt to acquire new locks.
 **/

final class RequiresNew extends TranStrategy {
    //d121558
    private static final TraceComponent tc = Tr.register(RequiresNew.class, "EJBContainer", "com.ibm.ejs.container.container");

    RequiresNew(TransactionControlImpl txCtrl) {
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

        Transaction suspended = null; //LIDB1673.2.1.5

        // Suspend a local tran, if it exists (null is returned if not).
        // This should only be required if a global tran is not present,
        // but this code tolerates scenarios where applications use
        // internals to begin/resume global trans on threads that already
        // have a local transaction.                                   PI10351
        LocalTransactionCoordinator savedLocalTx = suspendLocalTx();

        if (globalTxExists(false)) {
            suspended = suspendGlobalTx(TransactionControlImpl.TIMEOUT_CLOCK_STOP);
        }

        try {
            beginGlobalTx(key, methodInfo);
        } catch (CSIException ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Begin of global tx failed", ex);
            }

            // LIDB2446: have to restore any suspended global/local tx here
            try
            {
                if (suspended != null)
                {
                    resumeGlobalTx(suspended,
                                   TransactionControlImpl.TIMEOUT_CLOCK_STOP);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Resumed suspended global tran after " +
                                     "start of new global tran failed");
                }
                else if (savedLocalTx != null)
                {
                    resumeLocalTx(savedLocalTx);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Resumed suspended local tran after " +
                                     "start of new global tran failed");
                }
            } catch (Throwable ex2) {
                if (!(ex2 instanceof CSIException))
                    FFDCFilter.processException(ex2, "com.ibm.ejs.csi.RequiresNew.preInvoke", "95", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Saved local/global tx resume failed", ex2);
                }
            }

            throw ex;
        }

        if (entryEnabled) {
            Tr.exit(tc, "preInvoke");
        }

        TxCookieImpl cookie = new TxCookieImpl(true, false, this, suspended);
        // LIDB2446 add suspended ltc to cookie
        cookie.suspendedLocalTx = savedLocalTx;
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

        super.postInvoke(key, txCookie, methodInfo);

        if (entryEnabled) {
            Tr.exit(tc, "postInvoke");
        }
    }
} // RequiresNew
