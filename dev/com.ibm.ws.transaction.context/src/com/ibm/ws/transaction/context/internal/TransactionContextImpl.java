/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.concurrent.RejectedExecutionException;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.uow.embeddable.UOWManager;
import com.ibm.ws.uow.embeddable.UOWManagerFactory;
import com.ibm.ws.uow.embeddable.UOWToken;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * Transaction context implementation.
 */
public class TransactionContextImpl implements ThreadContext {

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = -6094017242267061944L;

    /**
     * Fields to serialize
     */
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {
    };

    /**
     * Unit of work that was on the thread of execution prior to invoking the contextual task.
     */
    private transient UOWToken suspendedUOW;

    /**
     * Indicates that prior to invoking a task, any transaction that is present on the thread of execution
     * should be suspended (and resumed afterwards). After suspending the transaction on the thread of execution,
     * by default an LTC is put in place for the task to run under.
     * In the future, we leave open the possibility that some other action could be taken in place of the LTC.
     */
    transient boolean suspendTranOfExecutionThread;

    TransactionContextImpl(boolean suspendTranOfExecuctionThread) {
        this.suspendTranOfExecutionThread = suspendTranOfExecuctionThread;
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext clone() {
        try {
            TransactionContextImpl copy = (TransactionContextImpl) super.clone();
            return copy;
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void taskStarting() throws RejectedExecutionException {
        if (suspendTranOfExecutionThread) {
            // Suspend whatever is currently on the thread.
            try {
                UOWManager uowManager = UOWManagerFactory.getUOWManager();
                suspendedUOW = uowManager.suspend();
            } catch (com.ibm.ws.uow.embeddable.SystemException e) {
            }

            // begin a local transaction.
            LocalTransactionCurrent ltCurrent = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
            ltCurrent.begin();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void taskStopping() {
        if (suspendTranOfExecutionThread) {
            Throwable exception = null;

            // Cleanup any unresolved transactions.
            UOWCurrent uowCurrent = EmbeddableTransactionManagerFactory.getUOWCurrent();
            switch (uowCurrent.getUOWType()) {
                case UOWCurrent.UOW_GLOBAL:
                    // Rollback global transactions.
                    try {
                        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
                        tm.rollback();
                    } catch (Exception e) {
                        exception = e;
                    }
                    break;

                case UOWCurrent.UOW_LOCAL:
                    // Commit local transaction.
                    try {
                        LocalTransactionCurrent ltCurrent = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
                        ltCurrent.end(LocalTransactionCoordinator.EndModeCommit);
                    } catch (Exception e) {
                        exception = e;
                    }
                    break;

                case UOWCurrent.UOW_NONE:
                    break;
                default:
                    exception = new Exception("Invalid transaction type: " + uowCurrent.getUOWType());
                    break;
            }

            // Resume the original transaction.
            try {
                if (suspendedUOW != null) {
                    UOWManager uowManager = UOWManagerFactory.getUOWManager();
                    uowManager.resume(suspendedUOW);
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
    }

    /**
     * Reads and deserializes the input object.
     *
     * @param in The object to deserialize.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.readFields();
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder(100).append(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())).append(" suspend=").append(suspendTranOfExecutionThread);
        return sb.toString();
    }

    /**
     * Serialized the given object.
     *
     * @param outStream The stream to write the serialized data.
     *
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream outStream) throws IOException {
        outStream.putFields();
        outStream.writeFields();
    }
}