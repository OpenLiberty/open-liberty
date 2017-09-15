/*******************************************************************************
 * Copyright (c) 2002, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 *  <code>UOWCookie</code> instances are used by <code>UOWControlImpl</code> and
 *  <code>TransactionControlImpl</code> to convey context management information
 *  from preinvoke calls to the matching postinvoke calls.
 */

package com.ibm.ejs.csi;

import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;

public interface UOWCookie
{
    /**
     * Returns true if preInvoke placed a local transaction context
     * on the current thread
     */
    boolean isLocalTx();

    /**
     * Returns true if preInvoke began a new local or global transaction
     */
    boolean beganTx();

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
    SynchronizationRegistryUOWScope getTransactionalUOW();

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
    void setTransactionalUOW(SynchronizationRegistryUOWScope uowCoordinator);

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
    SynchronizationRegistryUOWScope getSuspendedTransactionalUOW();
}
