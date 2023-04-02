/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.transaction.context.internal;

import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.util.concurrent.RejectedExecutionException;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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
    private static final TraceComponent tc = Tr.register(TransactionContextImpl.class);

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = -6094017242267061944L;

    /**
     * Values for serializable fields.
     * A single character is used for each to reduce the space required.
     */
    private static final String CLEARED = "C",
                    UNCHANGED = "U";

    /**
     * Names for serializable fields.
     * A single character is used for each to reduce the space required.
     */
    private static final String TYPE = "T";

    /**
     * Fields to serialize.
     */

    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {
                                                                                                new ObjectStreamField(TYPE, String.class)
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
    transient Boolean suspendTranOfExecutionThread;

    TransactionContextImpl(boolean suspendTranOfExecuctionThread) {
        this.suspendTranOfExecutionThread = suspendTranOfExecuctionThread;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
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
    @Trivial // method name is misleading in trace
    public void taskStarting() throws RejectedExecutionException {
        if (suspendTranOfExecutionThread) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "clear");

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
    @Trivial // method name is misleading in trace
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

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "restore   " + suspendedUOW);

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
        GetField fields = in.readFields();

        Object type = fields.get(TYPE, null);
        if (CLEARED.equals(type))
            suspendTranOfExecutionThread = true;
        else if (UNCHANGED.equals(type))
            suspendTranOfExecutionThread = false;
        // else null value will cause TransactionContextProviderImpl to recompute from the execution property
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder(100).append(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode()));
        if (suspendTranOfExecutionThread)
            sb.append(" suspend=true");
        else
            sb.append(" unchanged");
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
        PutField fields = outStream.putFields();
        fields.put(TYPE, suspendTranOfExecutionThread ? CLEARED : UNCHANGED);
        outStream.writeFields();
    }
}