/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.zos.tx;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.jta.impl.RegisteredSyncs;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

/**
 * Transaction manager object wrapper. It provides RRS capable RMs with the ability
 * to register a synchronization object directly with the transaction manager.
 * Transaction management through this object is not allowed.
 */
public class RRSTXSynchronizationManager implements TransactionManager {

    /**
     * Generic message text to place in exceptions.
     */
    private final static String errorMsg =
                    "Transaction management is not allowed through this interface";

    /**
     * Singleton object instance.
     */
    private final static RRSTXSynchronizationManager instance =
                    new RRSTXSynchronizationManager();

    /**
     * Reference to the real transaction manager.
     */
    final private TransactionManager tm;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private RRSTXSynchronizationManager() {
        tm = EmbeddableTransactionManagerFactory.getTransactionManager();
    }

    /**
     * This method signature is <i>Required</i> for use by RRS RM's.
     */
    public static TransactionManager getTransactionManager() {
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    public int getStatus() throws SystemException {
        return tm.getStatus();
    }

    /**
     * Get a reference to an object that represents the transaction
     * associated with the current thread. The object that is
     * return only supports enlistment of a <code>Synchronization</code>
     * by RRS connectors. It is special in that sync objects registered
     * through this object are "priority" syncs. In other words, they
     * are called as close to the RRS syncpoint as possible.
     * 
     * @return A transaction wrapper reference.
     */
    @FFDCIgnore(Exception.class)
    public Transaction getTransaction() throws SystemException {
        Transaction tx = null;
        RRSSynchronizerTransaction rrsTx = null;

        try {
            tx = tm.getTransaction();

            if (tx != null) {
                rrsTx = new RRSSynchronizerTransaction(tx);
            }
        } catch (Exception e) {
            // Absorb any exceptions.
        }

        return rrsTx;
    }

    /**
     * {@inheritDoc}
     */
    public void begin() throws SystemException {
        throw new SystemException(RRSTXSynchronizationManager.errorMsg);
    }

    /**
     * {@inheritDoc}
     */
    public void commit() throws SystemException {
        throw new SystemException(RRSTXSynchronizationManager.errorMsg);
    }

    /**
     * {@inheritDoc}
     */
    public void rollback() throws SystemException {
        throw new SystemException(RRSTXSynchronizationManager.errorMsg);
    }

    /**
     * {@inheritDoc}
     */
    public Transaction suspend() throws SystemException {
        throw new SystemException(RRSTXSynchronizationManager.errorMsg);
    }

    /**
     * {@inheritDoc}
     */
    public void resume(Transaction tx) throws SystemException {
        throw new SystemException(RRSTXSynchronizationManager.errorMsg);
    }

    /**
     * {@inheritDoc}
     */
    public void setRollbackOnly() throws SystemException {
        throw new SystemException(RRSTXSynchronizationManager.errorMsg);
    }

    /**
     * {@inheritDoc}
     */
    public void setTransactionTimeout(int seconds) throws SystemException {
        throw new SystemException(errorMsg);
    }

    /**
     * Nested class that handles registration of sync objects for
     * RRS resource managers.
     */
    private final class RRSSynchronizerTransaction implements javax.transaction.Transaction {
        private final Transaction tx;

        /**
         * Constructor.
         * 
         * @param tx The object representing the transaction.
         */
        public RRSSynchronizerTransaction(Transaction tx) {
            this.tx = tx;
        }

        /**
         * {@inheritDoc}
         */
        public void registerSynchronization(Synchronization sync)
                        throws SystemException, RollbackException {
            ((EmbeddableWebSphereTransactionManager) tm).registerSynchronization((UOWCoordinator) tx,
                                                                                 sync,
                                                                                 RegisteredSyncs.SYNC_TIER_RRS);
        }

        /**
         * {@inheritDoc}
         */
        public int getStatus() throws SystemException {
            return tx.getStatus();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return tx.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof RRSSynchronizerTransaction) {
                final RRSSynchronizerTransaction otherTx;
                otherTx = (RRSSynchronizerTransaction) o;
                return tx.equals(otherTx.tx);
            }

            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void commit() throws SystemException {
            throw new SystemException(errorMsg);
        }

        /**
         * {@inheritDoc}
         */
        public void rollback() throws SystemException {
            throw new SystemException(errorMsg);
        }

        /**
         * {@inheritDoc}
         */
        public void setRollbackOnly() throws SystemException {
            throw new SystemException(errorMsg);
        }

        /**
         * {@inheritDoc}
         */
        public boolean enlistResource(XAResource xares) throws SystemException {
            throw new SystemException(errorMsg);
        }

        /**
         * {@inheritDoc}
         */
        public boolean delistResource(XAResource xares, int flag) throws SystemException {
            throw new SystemException(errorMsg);
        }
    }
}
