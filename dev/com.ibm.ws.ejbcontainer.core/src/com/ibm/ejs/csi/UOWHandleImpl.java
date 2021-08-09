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
 *  <code>UOWHandleImpl</code> instances are used by <code>UOWControl</code> implementations
 *  suspend and resume methods to convey context management information accross a suspend
 *  and resume call.
 */

package com.ibm.ejs.csi;

import javax.transaction.Transaction;

import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;

public class UOWHandleImpl implements UOWHandle {

    /**
     * Suspended global transaction, if any.
     */
    protected final Transaction suspendedGlobalTx;

    /**
     * Suspended local transaction if any.
     */
    protected final LocalTransactionCoordinator suspendedLocalTx;

    /**
     * Suspended ActivitySession, if any.
     */
//    protected final ActivitySession suspendedActivitySession;

    /**
     * Create new UOWHandleImpl instance to hold a suspended global tx.
     */
    UOWHandleImpl(Transaction suspendedGlobalTx) {
//        this.suspendedActivitySession = null;
        this.suspendedLocalTx = null;
        this.suspendedGlobalTx = suspendedGlobalTx;
    } //ctor

    /**
     * Create new UOWHandleImpl instance to hold a suspended local tx.
     */
    UOWHandleImpl(LocalTransactionCoordinator ltc) {
//        this.suspendedActivitySession = null;
        this.suspendedLocalTx = ltc;
        this.suspendedGlobalTx = null;
    } //ctor

    /**
     * Create new UOWHandleImpl instance to hold a suspended ActivitySession.
     */
//    UOWHandleImpl(ActivitySession as) {
//        this.suspendedActivitySession = as;
//        this.suspendedLocalTx = null;
//        this.suspendedGlobalTx = null;
//    } //ctor
}// UOWHandleImpl
