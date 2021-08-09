/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.context.internal;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.concurrent.RejectedExecutionException;

import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.uow.embeddable.EmbeddableUOWTokenImpl;
import com.ibm.ws.uow.embeddable.UOWManager;
import com.ibm.ws.uow.embeddable.UOWManagerFactory;
import com.ibm.ws.uow.embeddable.UOWToken;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * A special transaction context implementation for MicroProfile Concurrency that propagates a
 * transaction to another thread on the condition that it is only active on one thread at a time.
 */
public class SerialTransactionContextImpl implements ThreadContext {
    private static final long serialVersionUID = 1;

    /**
     * Unit of work that was on the thread of execution prior to invoking the contextual task.
     */
    private UOWToken suspendedUOW;

    private final Transaction tx;

    SerialTransactionContextImpl() {
        try {
            tx = EmbeddableTransactionManagerFactory.getTransactionManager().getTransaction();
        } catch (SystemException x) {
            throw new RejectedExecutionException(x);
        }
    }

    @Override
    public ThreadContext clone() {
        try {
            SerialTransactionContextImpl copy = (SerialTransactionContextImpl) super.clone();
            return copy;
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void taskStarting() throws RejectedExecutionException {
        // Suspend whatever is currently on the thread.
        UOWManager uowManager = UOWManagerFactory.getUOWManager();
        try {
            suspendedUOW = uowManager.suspend();
        } catch (com.ibm.ws.uow.embeddable.SystemException e) {
            suspendedUOW = null;
        }

        boolean resumed = false;
        try {
            EmbeddableTransactionManagerFactory.getTransactionManager().resume(tx);
            resumed = true;
        } catch (InvalidTransactionException x) {
            throw new RejectedExecutionException(x);
        } catch (SystemException x) {
            throw new RejectedExecutionException(x);
        } catch (IllegalStateException x) {
            if (tx != null) {
                try {
                    tx.setRollbackOnly();
                } catch (IllegalStateException isx) {
                } catch (SystemException sysx) {
                }
            }
            throw x;
        } finally {
            if (!resumed && suspendedUOW != null)
                try {
                    uowManager.resume(suspendedUOW);
                } catch (com.ibm.ws.uow.embeddable.SystemException x) {
                    // A prior error is already being raised, so ignore apart from logging to FFDC
                }
        }
    }

    @Override
    public void taskStopping() {
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        Throwable exception = null;

        // Suspend the transaction that we propagated to the thread if it is still active
        try {
            if (tx.equals(tm.getTransaction()))
                tm.suspend();
        } catch (Throwable x) {
            exception = x;
        }

        // Cleanup any unresolved transactions.
        UOWCurrent uowCurrent = EmbeddableTransactionManagerFactory.getUOWCurrent();
        switch (uowCurrent.getUOWType()) {
            case UOWCurrent.UOW_GLOBAL:
                // Rollback global transactions.
                try {
                    tm.rollback();
                    if (exception == null)
                        exception = new Exception("Global transaction rolled back.");
                } catch (Exception e) {
                    if (exception == null)
                        exception = e;
                }
                break;

            case UOWCurrent.UOW_LOCAL:
                // Commit local transaction.
                try {
                    LocalTransactionCurrent ltCurrent = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
                    ltCurrent.end(LocalTransactionCoordinator.EndModeCommit);
                } catch (Exception e) {
                    if (exception == null)
                        exception = e;
                }
                break;

            case UOWCurrent.UOW_NONE:
                break;
            default:
                if (exception == null)
                    exception = new Exception("Invalid transaction type: " + uowCurrent.getUOWType());
                break;
        }

        // Resume the original transaction if it hasn't already committed or rolled back.
        try {
            if (suspendedUOW != null) {
                Transaction tran = suspendedUOW instanceof EmbeddableUOWTokenImpl ? ((EmbeddableUOWTokenImpl) suspendedUOW).getTransaction() : null;
                int status = tran == null ? Status.STATUS_UNKNOWN : tran.getStatus();
                if (status != Status.STATUS_NO_TRANSACTION && status != Status.STATUS_COMMITTED && status != Status.STATUS_ROLLEDBACK) {
                    UOWManager uowManager = UOWManagerFactory.getUOWManager();
                    uowManager.resume(suspendedUOW);
                }
                suspendedUOW = null;
            }
        } catch (Throwable e) {
            exception = e;
        }

        // Throw any pending exception.
        if (exception != null) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder(100).append(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())).append(" tx=").append(tx);
        return sb.toString();
    }

    private void writeObject(ObjectOutputStream outStream) throws IOException {
        throw new NotSerializableException();
    }
}