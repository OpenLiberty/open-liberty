/*******************************************************************************
 * Copyright (c) 2009, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.embeddable.impl;

import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionRolledbackException;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.jta.impl.TranManagerImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.tx.UOWEventListener;

public class EmbeddableTranManagerImpl extends TranManagerImpl {
    private static final TraceComponent tc = Tr.register(EmbeddableTranManagerImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    @Override
    public void begin() throws NotSupportedException, SystemException {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "begin (SPI)");

        if (tx != null) {
            if (tx.getTxType() != UOWCoordinator.TXTYPE_NONINTEROP_GLOBAL) {
                Tr.error(tc, "WTRN0017_UNABLE_TO_BEGIN_NESTED_TRANSACTION");
                final NotSupportedException nse = new NotSupportedException("Nested transactions are not supported.");

                FFDCFilter.processException(nse, "com.ibm.tx.jta.embeddable.impl.EmbeddableTranManagerImpl.begin", "63", this);
                if (traceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "begin (SPI)", nse);
                throw nse;
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "the tx is NONINTEROP_GLOBAL it may safely be treated as null");
            }
        }

        // this is a CMT, so look for Component timeout
        int timeout = ConfigurationProviderManager.getConfigurationProvider().getRuntimeMetaDataProvider().getTransactionTimeout();
        if (timeout == -1) {
            timeout = txTimeout;
        }

        if (timeout == 0) {
            timeout = ConfigurationProviderManager.getConfigurationProvider().getTotalTransactionLifetimeTimeout();
        }

        tx = createNewTransaction(timeout);

        invokeEventListener(tx, UOWEventListener.POST_BEGIN, null);

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "begin (SPI)");
    }

    @Override
    protected EmbeddableTransactionImpl createNewTransaction(int timeout) throws SystemException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "createNewTransaction", timeout);

        final EmbeddableTransactionImpl tx = new EmbeddableTransactionImpl(timeout);
        tx.setMostRecentThread(Thread.currentThread());

        return tx;
    }

    /**
     * Complete processing of passive transaction timeout.
     */
    public void completeTxTimeout() throws TransactionRolledbackException {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "completeTxTimeout");

        if (tx != null && tx.isTimedOut()) {
            if (traceOn && tc.isEventEnabled())
                Tr.event(tc, "Transaction has timed out. The transaction will be rolled back now");
            Tr.info(tc, "WTRN0041_TXN_ROLLED_BACK", tx.getTranName());
            ((EmbeddableTransactionImpl) tx).rollbackResources();

            final TransactionRolledbackException rbe = new TransactionRolledbackException("Transaction is ended due to timeout");

            FFDCFilter.processException(rbe, "com.ibm.tx.jta.embeddable.impl.EmbeddableTranManagerImpl.completeTxTimeout", "100", this);
            if (traceOn && tc.isEntryEnabled())
                Tr.exit(tc, "completeTxTimeout", rbe);
            throw rbe;
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "completeTxTimeout");
    }

    @Override
    public synchronized void resume(Transaction tx) throws InvalidTransactionException, IllegalStateException {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "synchronized resume", new Object[] { this, tx });

        // no-op if tx is null
        if (tx instanceof EmbeddableTransactionImpl) {
            final EmbeddableTransactionImpl t = (EmbeddableTransactionImpl) tx;

            if (!t.isResumable()) {
                final Thread thread = t.getThread(); // avoid race condition where value becomes null after first check
                final IllegalStateException ise;
                if (thread != null) {
                    ise = new IllegalStateException(Tr.formatMessage(tc, "WTRN0156_CANNOT_RESUME", String.format("%08X", thread.getId())));
                } else {
                    ise = new IllegalStateException(Tr.formatMessage(tc, "WTRN0157_CANNOT_RESUME"));
                }

                if (traceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "synchronized resume", ise);
                throw ise;
            }

            super.resume(t);

            t.setThread(Thread.currentThread());
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "synchronized resume");
    }

    @Override
    public synchronized Transaction suspend() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "synchronized suspend", this);

        final Transaction t = super.suspend();

        if (t instanceof EmbeddableTransactionImpl) {
            ((EmbeddableTransactionImpl) t).setThread(null);
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "synchronized suspend", t);
        return t;
    }

    /**
     * Atomically resume and add an association for a transaction being imported.
     * This method delegates to resume() then calls addAssociation() with proper error recovery.
     */
    public synchronized void resumeForImport(Transaction tx) throws InvalidTransactionException, IllegalStateException {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "synchronized resumeForImport", new Object[] { this, tx });

        // Step 1: Resume the transaction (delegates to existing resume method)
        // This handles all validation, state checks, thread association, and event listeners
        try {
            resume(tx);
            
            if (traceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Transaction resumed successfully");
        } catch (InvalidTransactionException | IllegalStateException e) {
            // Resume failed, nothing to clean up
            if (traceOn && tc.isEntryEnabled())
                Tr.exit(tc, "synchronized resumeForImport", e);
            throw e;
        }

        // Step 2: Add association (this can throw TRANSACTION_ROLLEDBACK)
        // Note: Like resume(), we silently handle non-EmbeddableTransactionImpl transactions
        if (tx instanceof EmbeddableTransactionImpl) {
            final EmbeddableTransactionImpl t = (EmbeddableTransactionImpl) tx;
            try {
                t.addAssociation();
                
                if (traceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Association added successfully");
                    
                if (traceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "synchronized resumeForImport - success");
            } catch (RuntimeException e) {
                // addAssociation failed (typically TRANSACTION_ROLLEDBACK) - must suspend to restore state
                if (traceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "addAssociation failed, suspending transaction to restore state", e);
                
                try {
                    suspend();
                } catch (Exception suspendEx) {
                    // Log but don't throw - we're already in error handling
                    FFDCFilter.processException(suspendEx, "com.ibm.tx.jta.embeddable.impl.EmbeddableTranManagerImpl.resumeForImport", "XXX", this);
                    if (traceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Failed to suspend after addAssociation failure", suspendEx);
                }
                
                // Re-throw the original exception from addAssociation
                FFDCFilter.processException(e, "com.ibm.tx.jta.embeddable.impl.EmbeddableTranManagerImpl.resumeForImport", "XXX", this);
                if (traceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "synchronized resumeForImport", e);
                throw e;
            }
        }
        // else: tx is null or not EmbeddableTransactionImpl - resume() handled it, we're done
        // This matches the behavior of resume() which doesn't throw for non-EmbeddableTransactionImpl
        
        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "synchronized resumeForImport");
    }
}
