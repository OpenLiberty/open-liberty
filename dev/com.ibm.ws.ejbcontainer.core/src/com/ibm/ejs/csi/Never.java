/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 * <code> Never </code> implements TX_NEVER semantics. If an inbound
 * global transaction exists, a CSIException will be thrown. The global
 * tx will be marked for rollback in TransactionControlImpl.postInvoke()
 * because there wil be no TxCookie associated with this tx. If no global
 * transaction exists on entry, a local tx is started instead.
 **/

final class Never extends TranStrategy
{
    //d121558
    private static final TraceComponent tc = Tr.register(Never.class, "EJBContainer", "com.ibm.ejs.container.container");

    Never(TransactionControlImpl txCtrl) {
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

        if (globalTxExists(false)) {
            throw new CSIException("TX_NEVER method called within a global tx");
        }

        // Normal case; begin a local tx
        // LIDB2446 beginLocalTx returns cookie
        TxCookieImpl cookie = beginLocalTx(key, methodInfo, null);

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

        //------------------------------------------------
        // If we get back here preInvoke must have started
        // a local tx so have TranStrategy end it now.
        //------------------------------------------------

        // LIDB2446: May be sharing an LTC
        // ASSERT.isTrue(txCookie.beginner);

        super.postInvoke(key, txCookie, methodInfo);

        if (entryEnabled) {
            Tr.exit(tc, "postInvoke");
        }

    }

} // Never
