/*******************************************************************************
 * Copyright (c) 1998, 2012 IBM Corporation and others.
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
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;

/**
 * <code>TxCookieImpl</code> provides context information passed between
 * preInvoke and postInvoke.
 **/

final class TxCookieImpl
                implements UOWCookie
{
    /**
     * Did we begin the transaction this cookie associated wtih?
     */
    protected final boolean beginner;

    /**
     * Is this a local transaction?
     */
    protected final boolean isLocal;

    /**
     * Transaction strategy associated with this cookie.
     */
    protected final TranStrategy txStrategy;

    /**
     * Suspended global transaction if any.
     */

    protected final Transaction suspendedTx; //LIDB1673.2.1.5

    /**
     * Suspended local transaction if any.
     */
    protected LocalTransactionCoordinator suspendedLocalTx;

    /**
     * Method cookie associated with.
     */
    protected EJBMethodInfoImpl methodInfo;

    /**
     * Transactional UOWCoordinator associated with this Cookie.
     */
    // d139352-2
    protected SynchronizationRegistryUOWScope ivCoordinator;

    /**
     * Create new cookie instance.
     */
    TxCookieImpl(boolean beginner, boolean isLocal,
                 TranStrategy txStrategy, Transaction suspendedGlobalTx) //LIDB1673.2.1.5

    {
        this.beginner = beginner;
        this.isLocal = isLocal;
        this.txStrategy = txStrategy;
        this.suspendedTx = suspendedGlobalTx;
        this.suspendedLocalTx = null;
        this.methodInfo = null;
        this.ivCoordinator = null;

    } // TxCookieImpl

    /**
     * Return true iff this cookie associated with newly begun transaction.
     */
    public boolean beganTx()
    {
        return beginner;

    } // beganTx

    /**
     * Returns true if preInvoke placed a local transaction context
     * on the current thread
     */
    public boolean isLocalTx()
    {
        return isLocal;
    } // isLocalTx

    /**
     * Obtain UOW identifier which corresponds to whatever transaction
     * context is associated with the UOW Cookie. This object will be an
     * implementation of one of the following two interfaces. Each of
     * these objects is a type of UOW that could be used to trigger
     * activation / passivation. Also, each of these objects supports
     * registration of a javax.activity.Synchronization.
     * 
     * 1) Transaction (if there is one) OR
     * 2) LocalTransactionCoordinator (if there is one) OR
     * 3) null
     * 
     * @return UOW identifier for the corresponding transaction.
     */
    // d139352-2
    public SynchronizationRegistryUOWScope getTransactionalUOW()
    {
        return ivCoordinator;
    }

    /**
     * Sets the UOW identifier which corresponds to whatever transaction
     * context is to be associated with the UOW Cookie. This object should
     * be an implementation of one of the following two interfaces. Each of
     * these objects is a type of UOW that could be used to trigger
     * activation / passivation. Also, each of these objects supports
     * registration of a javax.activity.Synchronization.
     * 
     * 1) Transaction (if there is one) OR
     * 2) LocalTransactionCoordinator (if there is one) OR
     * 3) null
     * 
     * @param uowCoordinator identifier (Coordinator) for a transaction.
     */
    // d139352-2
    public void setTransactionalUOW(SynchronizationRegistryUOWScope uowCoordinator)
    {
        ivCoordinator = uowCoordinator;
    }

    /**
     * Obtain UOW identifier of whatever transaction context was suspended when
     * the transaction context associated with the UOW Cookie was started. <p>
     * 
     * This object should be an implementation of one of the following two
     * interfaces. Each of these objects is a type of UOW that could be used
     * to trigger activation / passivation. Also, each of these objects
     * supports registration of a javax.activity.Synchronization.
     * 
     * 1) Transaction (if there is one) OR
     * 2) LocalTransactionCoordinator (if there is one) OR
     * 3) null
     * 
     * @return UOW identifier for the corresponding suspended transaction.
     */
    // d704504
    public SynchronizationRegistryUOWScope getSuspendedTransactionalUOW()
    {
        if (suspendedTx != null) {
            return (SynchronizationRegistryUOWScope) suspendedTx;
        }

        return (SynchronizationRegistryUOWScope) suspendedLocalTx;
    }

} // TxCookieImpl

