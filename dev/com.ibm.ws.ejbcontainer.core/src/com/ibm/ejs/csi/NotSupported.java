/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * <code> NotSupported </code> implements TX_NOT_SUPPORTED semantics.
 * Any inbound global transaction will be suspended. The suspended
 * transaction will continue to hold its locks. A local transaction
 * will be started and commited at postInvoke.
 **/

final class NotSupported
                extends TranStrategy
{
    //d121558
    private static final TraceComponent tc = Tr.register(NotSupported.class, "EJBContainer", "com.ibm.ejs.container.container");

    NotSupported(TransactionControlImpl txCtrl) {
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

        if (globalTxExists(false)) {
            suspended = suspendGlobalTx(TransactionControlImpl.TIMEOUT_CLOCK_STOP);
        }

        // LIDB2446 beginLocalTx returns cookie
        TxCookieImpl cookie = null;
        try
        {
            cookie = beginLocalTx(key, methodInfo, suspended);
        } catch (CSIException ex)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Begin of local tx failed", ex);

            try
            {
                if (suspended != null)
                {
                    resumeGlobalTx(suspended,
                                   TransactionControlImpl.TIMEOUT_CLOCK_STOP);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Resumed suspended global tran after " +
                                     "start of new local tran failed");
                }
            } catch (Throwable ex2)
            {
                if (!(ex2 instanceof CSIException))
                    FFDCFilter.processException(ex2, "com.ibm.ejs.csi.NotSupported.preInvoke", "98", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Saved global tx resume failed", ex2);
            }

            throw ex;
        }

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

        //-----------------------------------------------------
        // We always began a local transaction, now commit it.
        //-----------------------------------------------------

        // LIDB2446: May be sharing an LTC
        // ASSERT.isTrue(txCookie.beginner);

        super.postInvoke(key, txCookie, methodInfo);

        if (entryEnabled) {
            Tr.exit(tc, "postInvoke");
        }
    }
} // NotSupported
