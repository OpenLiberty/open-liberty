package com.ibm.ws.jtaextensions;

/*******************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.Status;
import javax.transaction.Synchronization;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.websphere.jtaextensions.SynchronizationCallback;

//
// This class wrappers a SynchronizationCallback as a javax.transaction.Synchronization type
// allowing it to be registered as a Synchronization with a transaction.
//
// Used by ExtendedJTATransactionImpl to allow SynchronizationCallback registration
// on a per-transaction basis.
//
public final class SynchronizationCallbackWrapper implements Synchronization
{
    private static final TraceComponent tc = Tr.register(SynchronizationCallbackWrapper.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private final SynchronizationCallback _syncCallback;
    private final int _localId;
    private final byte[] _globalId;

    public SynchronizationCallbackWrapper(SynchronizationCallback syncCallback, int localId, byte[] globalId)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "SynchronizationCallbackWrapper", new Object[] { syncCallback, localId, globalId });

        _syncCallback = syncCallback;
        _localId = localId;
        _globalId = globalId;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "SynchronizationCallbackWrapper");
    }

    @Override
    public void beforeCompletion()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "beforeCompletion");

        _syncCallback.beforeCompletion(_localId, _globalId);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "beforeCompletion");
    }

    @Override
    public void afterCompletion(int status)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "afterCompletion", status);

        _syncCallback.afterCompletion(_localId, _globalId, (status == Status.STATUS_COMMITTED));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "afterCompletion");
    }
}
