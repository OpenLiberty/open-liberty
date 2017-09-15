package com.ibm.ws.tx.jta.embeddable;

/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.jta.embeddable.impl.EmbeddableTransactionImpl;
import com.ibm.tx.jta.impl.JCARecoveryData;
import com.ibm.tx.jta.impl.TranManagerSet;
import com.ibm.tx.ltc.impl.LocalTranCurrentSet;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ffdc.FFDCFilter;

public final class EmbeddableJCATranWrapperImpl extends com.ibm.tx.jta.impl.JCATranWrapperImpl
{
    private static final TraceComponent tc = Tr.register(EmbeddableJCATranWrapperImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /**
     * 
     * Create a new transaction wrapper and transaction
     * 
     * @param timeout
     * @param xid
     * @param jcard
     */
    public EmbeddableJCATranWrapperImpl(int timeout, Xid xid, JCARecoveryData jcard)
    {
        _tranManager = (TranManagerSet) EmbeddableTransactionManagerFactory.getTransactionManager();

        suspend(); // suspend and save any LTC before we create the global txn

        _txn = new EmbeddableTransactionImpl(timeout, xid, jcard);

        _prepared = false;

        _associated = true;
    }

    /**
     * Suspend any transaction context off the thread - save the LTC in the wrapper
     */
    @Override
    public void suspend()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "suspend");

        _suspendedUOWType = ((UOWCurrent) _tranManager).getUOWType();

        switch (_suspendedUOWType)
        {
            case UOWCurrent.UOW_LOCAL:

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "suspending (local)");
                }

                _suspendedUOW = LocalTranCurrentSet.instance().suspend();

                break;

            case UOWCurrent.UOW_GLOBAL:

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "suspending (global)");
                }

                _suspendedUOW = _tranManager.suspend();

                break;

            case UOWCurrent.UOW_NONE:

                _suspendedUOW = null;
                break;

            default:
                break;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "suspend", _suspendedUOWType);
    }

    /**
     * 
     */
    @Override
    public void resume()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "resume");

        switch (_suspendedUOWType)
        {
            case UOWCurrent.UOW_LOCAL:
                LocalTranCurrentSet.instance().resume((LocalTransactionCoordinator) _suspendedUOW);
                break;

            case UOWCurrent.UOW_GLOBAL:
                try
                {
                    _tranManager.resume((Transaction) _suspendedUOW);
                } catch (Exception e) {
                    FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.EmbeddableJCATranWrapperImpl.resume", "135", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Failed to resume", new Object[] { _suspendedUOW, e });
                }
                break;

            default:
                break;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "resume");
    }
}