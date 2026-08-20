/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop.transaction.nodistributedtransactions;

import javax.transaction.Transaction;

import org.omg.CORBA.SystemException;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TRANSACTION_ROLLEDBACK;
import org.omg.CosTransactions.otid_t;
import org.omg.CosTransactions.PropagationContext;

import com.ibm.tx.jta.embeddable.impl.EmbeddableTransactionImpl;
import com.ibm.tx.remote.DistributableTransaction;
import com.ibm.ws.Transaction.UOWCoordinator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionHandlerContext;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

/**
 * Fallback implementation of TransactionImportHandler for scenarios without distributed transactions.
 *
 * This handler is used when no specific transaction protocol (like WS-AT or OTS) is available
 * or configured. It handles basic transaction propagation contexts and creates non-interop
 * transactions when needed.
 *
 * This implementation serves as a fallback and ensures the system can function even when
 * no protocol-specific handlers are registered.
 *
 * This class is directly instantiated (not OSGi managed) and receives services
 * via the TransactionHandlerContext parameter, matching the pattern used by
 * NoDTxTransactionExporter on the client side.
 */
public class NoDtxTransactionImportHandler {
    
    private static final TraceComponent tc = Tr.register(NoDtxTransactionImportHandler.class, "IIOP", null);
	
	private final ThreadLocal<DistributableTransaction> _threadImportedTran = new ThreadLocal<DistributableTransaction>();

    /**
     * Attempts to import a transaction from the propagation context.
     * Returns true if it successfully handled the context (even if no transaction was found),
     * false if this handler cannot process this type of context.
     *
     * @param propagationContext CORBA PropagationContext containing transaction data
     * @param txHandlerContext context providing access to shared services
     * @return true if handled, false otherwise
     * @throws TransactionImportFailed if an error occurs
     */
    public boolean importTransaction(PropagationContext propagationContext, TransactionHandlerContext txHandlerContext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "NoDtxTransactionImportHandler.importTransaction()");
        }
        
        // Check if we can handle this propagation context
        if (propagationContext == null || propagationContext.implementation_specific_data == null ||
            propagationContext.current == null || propagationContext.current.otid == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cannot handle: No propagation context or implementation_specific_data");
            }
            return false;
        }

        if (propagationContext.implementation_specific_data.type().kind() != TCKind.tk_boolean) {
            return false;
        }

        // We can handle this context, now process it
        otid_t otid = propagationContext.current.otid;
        byte [] tid = otid.tid;

        try {
            if (tid == null || tid.length == 0) {
                // Create non-interop transaction
                setupNonInterOpTransaction(txHandlerContext);
            } else {
                // Use the new lookupTransaction method that takes byte[] directly
                DistributableTransaction tx = txHandlerContext.getRemoteTransactionController().lookupTransaction(tid);

                if (tx != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "NoDtxTransactionImportHandler found existing transaction: {0}", tx);
                    }
                    ((EmbeddableWebSphereTransactionManager)txHandlerContext.getTransactionManager()).resumeForImport((Transaction) tx);
                    _threadImportedTran.set(tx);
                } else {
                    // We do NOT import this transaction, treat as Non-Interop 
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "NoDtxTransactionImportHandler did not find existing transaction for tid");
                    }
                    setupNonInterOpTransaction(txHandlerContext);
                }
            }
        } catch (SystemException se) {
            throw se;
        } catch (Exception e) {
            throw (TRANSACTION_ROLLEDBACK) new TRANSACTION_ROLLEDBACK(e.getMessage()).initCause(e);
        }

        return true; // We handled this context
    }

    /**
     * No-op cleanup method.
     *
     * @param context context providing access to shared services
     * @throws TransactionImportFailed never thrown
     */
    public void unimportTransaction(TransactionHandlerContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "NoDtxTransactionImportHandler.unimportTransaction() - no-op");
        }

        try {
            // Suspend the transaction on the thread - potentially may not be the same one we imported so use the ThreadLocal for the removeAssociation.
            context.getTransactionManager().suspend();
        } catch (javax.transaction.SystemException e) {
            // Never happens in our TM
        }

        DistributableTransaction tx = _threadImportedTran.get();
        if (tx != null) {
            tx.removeAssociation();
        }
    }

    private void setupNonInterOpTransaction(TransactionHandlerContext txHandlerContext) {

        try {
        EmbeddableTransactionImpl theTx = new EmbeddableTransactionImpl(UOWCoordinator.TXTYPE_NONINTEROP_GLOBAL, 0);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "NoDtxTransactionImportHandler detected Non-Interop transaction - created TXTYPE_NONINTEROP_GLOBAL transaction: {0}", theTx);
        }

        theTx.suspendAssociation(); // Or implement setNonInterOp to prevent Inactivity timer.
        theTx.addAssociation();
        ((EmbeddableWebSphereTransactionManager)txHandlerContext.getTransactionManager()).resumeForImport(theTx);
        _threadImportedTran.set(theTx);
        } catch (Exception e) {
            // Should never get here but if we do, make the request fail and indicate teh transaction should rollback
            throw (TRANSACTION_ROLLEDBACK) new TRANSACTION_ROLLEDBACK().initCause(e);
        }
    }

}

// Made with Bob
